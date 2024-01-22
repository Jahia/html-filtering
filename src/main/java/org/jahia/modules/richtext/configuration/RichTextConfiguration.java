/*
 * Copyright (C) 2002-2024 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.modules.richtext.configuration;

import org.apache.commons.lang.StringUtils;
import org.jahia.api.settings.SettingsBean;
import org.jahia.modules.richtext.RichTextConfigurationInterface;
import org.jahia.modules.richtext.configuration.parse.Parser;
import org.jahia.modules.richtext.configuration.parse.PropsToJsonParser;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.owasp.html.PolicyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component(service = {RichTextConfigurationInterface.class, ManagedServiceFactory.class}, property = {
        "service.pid=org.jahia.modules.richtext.config",
        "service.description=RichText configuration service",
        "service.vendor=Jahia Solutions Group SA"
}, immediate = true)
public class RichTextConfiguration implements RichTextConfigurationInterface, ManagedServiceFactory {

    private static final Logger logger = LoggerFactory.getLogger(RichTextConfiguration.class);
    private static final String CONFIG_FILE_NAME_BASE = "org.jahia.modules.richtext.config-";
    private Map<String, JSONObject> configs = new HashMap<>();
    private Map<String, String> siteKeyToPid = new HashMap<>();

    @Activate
    public void activate(BundleContext context) {
        logger.info("Activate RichTextConfiguration service");
    }

    @Override
    public String getName() {
        return "RichTextConfiguration service";
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> dictionary) throws ConfigurationException {
        List<String> keys = Collections.list(dictionary.keys());
        Map<String, Object> dictCopy = keys.stream()
                .filter(key -> key.startsWith("htmlFiltering."))
                .collect(Collectors.toMap(Function.identity(), dictionary::get));

        String siteKey = StringUtils.substringBefore(StringUtils.substringAfter((String) dictionary.get("felix.fileinstall.filename"), CONFIG_FILE_NAME_BASE), ".");

        if (!dictCopy.isEmpty()) {
            configs.put(pid, new PropsToJsonParser().parse(dictCopy));
            siteKeyToPid.put(siteKey, pid);
            logger.info(String.format("Setting htmlFiltering config for site %s: %s", siteKey, configs.get(pid).toString()));
        } else {
            logger.warn(String.format("Could not find htmlFiltering object for site: %s", siteKey));
        }
    }

    @Override
    public void deleted(String pid) {
        configs.remove(pid);
    }

    @Override
    public String getCKEditor5Config(String siteKey) {
        return null;
    }

    @Override
    public String getCKEditor4Config(String siteKey) {
        return null;
    }

    @Override
    public PolicyFactory getOwaspPolicyFactory(String siteKey) {
        if (configExists(siteKey)) {
            JSONObject config = configs.get(siteKeyToPid.get(siteKey));
            return Parser.parseToPolicy(config);
        }
        return null;
    }

    @Override
    public PolicyFactory getDefaultOwaspPolicyFactory() {
        return getOwaspPolicyFactory(DEFAULT_POLICY_KEY);
    }

    @Override
    public PolicyFactory getMergedOwaspPolicyFactory(String... siteKeys) {
        JSONObject mergedPolicy = getMergedJSONPolicy(siteKeys);

        if (!mergedPolicy.isEmpty()) {
            return Parser.parseToPolicy(mergedPolicy);
        }

        return null;
    }

    @Override
    public JSONObject getMergedJSONPolicy(String... siteKeys) {
        JSONObject mergedPolicy = new JSONObject();

        for (String key : siteKeys) {
            if (configExists(key)) {
                mergeJsonObject(mergedPolicy, configs.get(siteKeyToPid.get(key)));
            }
        }

        return mergedPolicy;
    }

    @Override
    public boolean configExists(String siteKey) {
        if (siteKeyToPid.containsKey(siteKey)) {
            String pid = siteKeyToPid.get(siteKey);
            return configs.containsKey(pid);
        }
        return false;
    }

    @Override
    public boolean htmlSanitizerDryRun(String siteKey) {
        JSONObject f = new JSONObject();

        if (configExists(siteKey)) {
            f = configs.get(siteKeyToPid.get(siteKey)).getJSONObject("htmlFiltering");
        } else if ((configExists(DEFAULT_POLICY_KEY))) {
            f = configs.get(siteKeyToPid.get(DEFAULT_POLICY_KEY)).getJSONObject("htmlFiltering");
        }

        return f.has("htmlSanitizerDryRun") && f.getBoolean("htmlSanitizerDryRun");
    }

    private void mergeJsonObject(JSONObject target, JSONObject source) {
        for (String key : source.keySet()) {
            if (source.get(key) instanceof JSONObject) {
                if (target.has(key) && target.get(key) instanceof JSONObject) {
                    mergeJsonObject(target.getJSONObject(key), source.getJSONObject(key));
                } else {
                    target.put(key, new JSONObject(source.getJSONObject(key).toString()));
                }
            } else if (source.get(key) instanceof JSONArray) {
                if (target.has(key) && target.get(key) instanceof JSONArray) {
                    JSONArray targetArray = target.getJSONArray(key);
                    JSONArray sourceArray = source.getJSONArray(key);
                    for (int i = 0; i < sourceArray.length(); i++) {
                        targetArray.put(sourceArray.get(i));
                    }
                } else {
                    target.put(key, new JSONArray(source.getJSONArray(key).toString()));
                }
            } else {
                target.put(key, source.get(key));
            }
        }
    }
}
