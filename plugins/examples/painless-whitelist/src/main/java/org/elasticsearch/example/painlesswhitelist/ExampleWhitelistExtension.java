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

package org.elasticsearch.example.painlesswhitelist;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.elasticsearch.painless.spi.PainlessExtension;
import org.elasticsearch.painless.spi.Whitelist;
import org.elasticsearch.painless.spi.WhitelistInstanceBinding;
import org.elasticsearch.painless.spi.WhitelistLoader;
import org.elasticsearch.script.ScriptContext;
import org.elasticsearch.script.SearchScript;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** An extension of painless which adds a whitelist. */
public class ExampleWhitelistExtension implements PainlessExtension {

    private static final Whitelist WHITELIST =
        WhitelistLoader.loadFromResourceFiles(ExampleWhitelistExtension.class, "example_whitelist.txt");

    @Override
    public Map<ScriptContext<?>, List<Whitelist>> getContextWhitelists() {
        ExampleWhitelistedInstance ewi = new ExampleWhitelistedInstance(1);
        WhitelistInstanceBinding addValue = new WhitelistInstanceBinding("example addValue", ewi,
            "addValue", "int", Collections.singletonList("int"));
        WhitelistInstanceBinding getValue = new WhitelistInstanceBinding("example getValue", ewi,
            "getValue", "int", Collections.emptyList());
        Whitelist instanceWhitelist = new Whitelist(ewi.getClass().getClassLoader(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), Arrays.asList(addValue, getValue));

        return Collections.singletonMap(SearchScript.CONTEXT, Arrays.asList(WHITELIST, instanceWhitelist));
    }
}
