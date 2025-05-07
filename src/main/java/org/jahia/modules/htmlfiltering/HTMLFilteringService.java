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
package org.jahia.modules.htmlfiltering;

import org.apache.commons.lang3.StringUtils;
import org.jahia.modules.htmlfiltering.configuration.parse.Parser;
import org.jahia.modules.htmlfiltering.configuration.parse.PropsToJsonParser;
import org.jahia.services.content.JCRNodeWrapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.annotations.Component;
import org.owasp.html.HtmlChangeListener;
import org.owasp.html.PolicyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component(service = {HTMLFilteringService.class, ManagedServiceFactory.class}, property = {
        "service.pid=org.jahia.modules.htmlfiltering.config",
        "service.description=HTML Filtering service",
        "service.vendor=Jahia Solutions Group SA"
}, immediate = true)
public class HTMLFilteringService implements ManagedServiceFactory {

    public static final String DEFAULT_POLICY_KEY = "default";

    private static final Logger logger = LoggerFactory.getLogger(HTMLFilteringService.class);
    private static final String CONFIG_FILE_NAME_BASE = "org.jahia.modules.htmlfiltering.config-";
    private final Map<String, JSONObject> configs = new HashMap<>();
    private final Map<String, String> siteKeyToPid = new HashMap<>();
    private boolean validate = false;

    @Override
    public String getName() {
        return "HTMLFiltering service";
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
            logger.info("Setting htmlFiltering config for site {}: {}", siteKey, configs.get(pid));
        } else {
            logger.warn("Could not find htmlFiltering object for site: {}", siteKey);
        }

        // Simple handling of sanitize / validate
        validate = Boolean.parseBoolean((String) dictionary.get("validate"));
    }

    @Override
    public void deleted(String pid) {
        configs.remove(pid);
    }

    public Map<String, SanitizedContent> sanitizeNode(JCRNodeWrapper node) {
        Map<String, SanitizedContent> results = new HashMap<>();
        logger.info("Validating node {}", node.getPath());
        final String siteKey;
        try {
            siteKey = node.getResolveSite().getSiteKey();
        } catch (RepositoryException e) {
            logger.warn("Unable to resolve site because {}, node can't be sanitized", e.getMessage());
            return results;
        }

        // Resolve properties to parse
        try {
            node.getPropertiesAsString().forEach((name, value) -> {
                try {
                    if (node.getProperty(name).getDefinition().getRequiredType() == PropertyType.STRING) {
                        SanitizedContent sanitizedContent = validate(value, siteKey);
                        if (sanitizedContent.getRemovedTags().isEmpty() && sanitizedContent.getRemovedAttributes().isEmpty()) {
                            return;
                        }
                        results.put(name, sanitizedContent);
                    }
                } catch (RepositoryException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }

        return results;
    }

    // Todo: Add nodetypes handling
    public SanitizedContent validate(String content, String siteKey) {

        PolicyFactory policyFactory = getMergedOwaspPolicyFactory(HTMLFilteringService.DEFAULT_POLICY_KEY, siteKey);

        if (policyFactory == null) {
            return null;
        }
        SanitizedContent result = new SanitizedContent();
        result.setSanitizedContent(policyFactory.sanitize(content, new HtmlChangeListener<SanitizedContent>() {
            @Override
            public void discardedTag(@Nullable SanitizedContent o, String tag) {
                o.getRemovedTags().add(tag);
            }

            @Override
            public void discardedAttributes(@Nullable SanitizedContent o, String tag, String... attrs) {
                o.getRemovedAttributes().computeIfAbsent(tag, k -> new HashSet<>()).addAll(Arrays.asList(attrs));
            }
        }, result));
        return result;
    }

    public JSONObject getMergedJSONPolicy(String... siteKeys) {
        JSONObject mergedPolicy = new JSONObject();

        for (String key : siteKeys) {
            if (configExists(key)) {
                mergeJSONObject(mergedPolicy, configs.get(siteKeyToPid.get(key)));
            }
        }

        return mergedPolicy;
    }

    public boolean configExists(String siteKey) {
        if (siteKeyToPid.containsKey(siteKey)) {
            String pid = siteKeyToPid.get(siteKey);
            return configs.containsKey(pid);
        }
        return false;
    }

    private PolicyFactory getMergedOwaspPolicyFactory(String... siteKeys) {
        JSONObject mergedPolicy = getMergedJSONPolicy(siteKeys);

        if (!mergedPolicy.isEmpty()) {
            return Parser.parseToPolicy(mergedPolicy);
        }

        return null;
    }

    private void mergeJSONObject(JSONObject target, JSONObject source) {
        for (String key : source.keySet()) {
            if (source.get(key) instanceof JSONObject) {
                mergeSubJSONObject(target, source, key);
            } else if (source.get(key) instanceof JSONArray) {
                mergeJSONArray(target, source, key);
            } else {
                target.put(key, source.get(key));
            }
        }
    }

    private static void mergeJSONArray(JSONObject target, JSONObject source, String key) {
        if (target.has(key) && target.get(key) instanceof JSONArray) {
            JSONArray targetArray = target.getJSONArray(key);
            JSONArray sourceArray = source.getJSONArray(key);
            for (int i = 0; i < sourceArray.length(); i++) {
                targetArray.put(sourceArray.get(i));
            }
        } else {
            target.put(key, new JSONArray(source.getJSONArray(key).toString()));
        }
    }

    private void mergeSubJSONObject(JSONObject target, JSONObject source, String key) {
        if (target.has(key) && target.get(key) instanceof JSONObject) {
            mergeJSONObject(target.getJSONObject(key), source.getJSONObject(key));
        } else {
            target.put(key, new JSONObject(source.getJSONObject(key).toString()));
        }
    }

    public boolean validate() {
        return validate;
    }
}
