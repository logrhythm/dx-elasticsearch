/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.index.similarity;

import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.SimilarityBase;
import org.elasticsearch.Version;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.IndexSettingsModule;

import java.util.Collections;

import static org.hamcrest.Matchers.instanceOf;

public class SimilarityServiceTests extends ESTestCase {
    public void testDefaultSimilarity() {
        Settings settings = Settings.builder().build();
        IndexSettings indexSettings = IndexSettingsModule.newIndexSettings("test", settings);
        SimilarityService service = new SimilarityService(indexSettings, null, Collections.emptyMap());
        assertThat(service.getDefaultSimilarity(), instanceOf(BM25Similarity.class));
    }

    // Tests #16594
    public void testOverrideBuiltInSimilarity() {
        Settings settings = Settings.builder().put("index.similarity.BM25.type", "classic").build();
        IndexSettings indexSettings = IndexSettingsModule.newIndexSettings("test", settings);
        try {
            new SimilarityService(indexSettings, null, Collections.emptyMap());
            fail("can't override bm25");
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), "Cannot redefine built-in Similarity [BM25]");
        }
    }

    public void testOverrideDefaultSimilarity() {
        Settings settings = Settings.builder().put("index.similarity.default.type", "boolean")
                .build();
        IndexSettings indexSettings = IndexSettingsModule.newIndexSettings("test", settings);
        SimilarityService service = new SimilarityService(indexSettings, null, Collections.emptyMap());
        assertTrue(service.getDefaultSimilarity() instanceof BooleanSimilarity);
    }

    public void testSimilarityValidation() {
        Similarity negativeScoresSim = new SimilarityBase() {
            @Override
            public String toString() {
                return "negativeScoresSim";
            }
            @Override
            protected float score(BasicStats stats, float freq, float docLen) {
                return -1;
            }
        };
        SimilarityService.validateSimilarity(Version.V_6_5_0, negativeScoresSim);
        assertWarnings("Similarities should not return negative scores:\n-1.0 = score(, doc=0, freq=1.0), computed from:\n");

        Similarity decreasingScoresWithFreqSim = new SimilarityBase() {
            @Override
            public String toString() {
                return "decreasingScoresWithFreqSim";
            }
            @Override
            protected float score(BasicStats stats, float freq, float docLen) {
                return 1 / (freq + docLen);
            }
        };
        SimilarityService.validateSimilarity(Version.V_6_5_0, decreasingScoresWithFreqSim);
        assertWarnings("Similarity scores should not decrease when term frequency increases:\n0.04761905 = score(, doc=0, freq=1.0), " +
                "computed from:\n\n0.045454547 = score(, doc=0, freq=2.0), computed from:\n");

        Similarity increasingScoresWithNormSim = new SimilarityBase() {
            @Override
            public String toString() {
                return "increasingScoresWithNormSim";
            }
            @Override
            protected float score(BasicStats stats, float freq, float docLen) {
                return freq + docLen;
            }
        };
        SimilarityService.validateSimilarity(Version.V_6_5_0, increasingScoresWithNormSim);
        assertWarnings("Similarity scores should not increase when norm increases:\n2.0 = score(, doc=0, freq=1.0), " +
                "computed from:\n\n3.0 = score(, doc=0, freq=1.0), computed from:\n");
    }

}
