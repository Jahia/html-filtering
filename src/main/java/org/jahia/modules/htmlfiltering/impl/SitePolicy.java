/*
 * Copyright (C) 2002-2025 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.htmlfiltering.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import org.apache.commons.lang3.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.htmlfiltering.Policy;
import org.jahia.modules.htmlfiltering.configuration.SiteCfg;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class SitePolicy {

    private static final Logger logger = LoggerFactory.getLogger(SitePolicy.class);
    private final Policy editWorkspacePolicy;
    private final Policy liveWorkspacePolicy;

    SitePolicy(SiteCfg siteConfiguration) {
        // compile the regex patterns for the allowed and disallowed rule sets (shared for both workspaces)
        Map<String, Pattern> formatPatterns = new HashMap<>();
        if (siteConfiguration.getFormatDefinitions() != null) {
            siteConfiguration.getFormatDefinitions().forEach((formatName, formatRegex) -> {
                formatPatterns.put(formatName, Pattern.compile(formatRegex)); // TODO properly throw meaningful exception if invalid regex
                logger.debug("Compiled format {} regex: {}", formatName, formatRegex);
            });
        }
        try {
            editWorkspacePolicy = new PolicyImpl(formatPatterns, siteConfiguration.getEditWorkspace());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unable to create the policy for the 'editWorkspace', reason: " + e.getMessage(), e);
        }
        try {
            liveWorkspacePolicy = new PolicyImpl(formatPatterns, siteConfiguration.getLiveWorkspace());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unable to create the policy for the 'liveWorkspace', reason: " + e.getMessage(), e);
        }

    }

    Policy getPolicy(String workspaceName) {
        if (Constants.EDIT_WORKSPACE.equals(workspaceName)) {
            return editWorkspacePolicy;
        }
        if (!Constants.LIVE_WORKSPACE.equals(workspaceName)) {
            logger.warn("Unknown workspace name: {}, fallback to using live workspace policy", workspaceName);
        }
        return liveWorkspacePolicy;
    }

    static class SitePolicyBuilder {
        private final static JavaPropsMapper javaPropsMapper = JavaPropsMapper.builder()
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
                .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
                .build();

        static SitePolicy build(Dictionary<String, ?> properties) throws ConfigurationException {
            // convert the dictionary to a map and keep only the props starting with "htmlFiltering."
            List<String> keys = Collections.list(properties.keys());
            Map<String, Object> propertiesMap = keys.stream()
                    .filter(key -> key.startsWith("htmlFiltering."))
                    .collect(Collectors.toMap(k -> StringUtils.substringAfter(k, "htmlFiltering."), properties::get));

            // convert the map to a configuration object
            SiteCfg siteConfiguration;
            try {
                String propertiesMapAsString = javaPropsMapper.writeValueAsString(propertiesMap);
                siteConfiguration = javaPropsMapper.readValue(propertiesMapAsString, SiteCfg.class);
            } catch (JsonProcessingException e) {
                throw new ConfigurationException(null, "Unable to read the properties: " + properties, e);
            }
            logger.debug("Site configuration loaded: {}", siteConfiguration);

            return new SitePolicy(siteConfiguration);
        }
    }
}
