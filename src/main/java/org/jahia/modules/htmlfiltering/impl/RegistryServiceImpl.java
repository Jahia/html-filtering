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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.htmlfiltering.Policy;
import org.jahia.modules.htmlfiltering.RegistryService;
import org.jahia.modules.htmlfiltering.configuration.SiteCfg;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component(immediate = true, service = {RegistryService.class, ManagedServiceFactory.class},
        property = {
                "service.pid=org.jahia.modules.htmlfiltering",
                "service.description=HTML filtering registry service to retrieve the policy to use for a given workspace of a given site",
                "service.vendor=Jahia Solutions Group SA"
        })
public final class RegistryServiceImpl implements RegistryService, ManagedServiceFactory {

    private static final Logger logger = LoggerFactory.getLogger(RegistryServiceImpl.class);

    private final JavaPropsMapper javaPropsMapper = JavaPropsMapper.builder()
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
            .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
            .build();

    /**
     * Configuration PID for the default site policy
     */
    private final AtomicReference<String> defaultPid = new AtomicReference<>();

    /**
     * A thread-safe map that associates persistent identities (PIDs) with {@link SitePolicy} instances.
     * <p>
     * This map is used to store and manage the configuration policies for specific site configurations
     * in a synchronized manner. Each PID corresponds to a unique site configuration whose edit and live
     * workspace policies are encapsulated by the {@link SitePolicy} class.
     * <p>
     * The map ensures thread-safe access and modifications by using a synchronized wrapper around
     * a {@code HashMap}.
     */
    private final Map<String, SitePolicy> policyByPid = Collections.synchronizedMap(new HashMap<>());

    /**
     * A map that associates a site key with its corresponding PID (persistent identity).
     * This is used for managing and referencing the configurations associated with each site in the system.
     */
    private final Map<String, String> pidBySite = new HashMap<>();

    @Override
    public String getName() {
        return "HTML Filtering Registry Service";
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
        logger.debug("Updating configuration for pid {}", pid);

        // extracting the site key from the configuration filename
        Object configurationPath = properties.get("felix.fileinstall.filename");
        if (configurationPath == null) {
            throw new ConfigurationException("felix.fileinstall.filename", "Missing configuration filename");
        }
        logger.info("Updating configuration file: {} (pid: {})", configurationPath, pid);
        String configurationName = FilenameUtils.getBaseName(configurationPath.toString());
        String siteKey = StringUtils.substringAfter(configurationName, "-");

        SitePolicy sitePolicy = createSitePolicy(properties, configurationPath);

        policyByPid.put(pid, sitePolicy);
        if (StringUtils.isEmpty(siteKey)) {
            defaultPid.set(pid);
            logger.debug("Default site policy updated.");
        } else {
            pidBySite.put(siteKey, pid);
            logger.debug("Site policy for '{}' updated.", siteKey);
        }
    }

    @Override
    public void deleted(String pid) {
        logger.debug("Deleting configuration for pid {}", pid);
        policyByPid.remove(pid);
        if (defaultPid.compareAndSet(pid, null)) {
            logger.info("Default site policy deleted.");
        } else {
            logger.info("Deleting site policy for site '{}'", pidBySite.remove(pid));
        }
    }

    private SitePolicy createSitePolicy(Dictionary<String, ?> properties, Object configurationPath) throws ConfigurationException {
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
            throw new ConfigurationException(null, "Unable to read the configuration file: " + configurationPath, e);
        }
        logger.debug("Site configuration loaded: {}", siteConfiguration);

        return new SitePolicy(siteConfiguration);
    }

    @Override
    public Policy getPolicy(String siteKey, String workspaceName) {
        String pid = pidBySite.get(siteKey);
        if (pid == null) {
            logger.debug("No pid found for siteKey: {}, using default policy", siteKey);
            pid = defaultPid.get();
        }
        logger.debug("pid for site {}: {}", siteKey, pid);
        SitePolicy policy = policyByPid.get(pid);
        if (policy == null) {
            logger.debug("No policy available for siteKey: {}", siteKey);
            return null;
        }
        return policy.getPolicy(workspaceName);
    }

    private static final class SitePolicy {

        private final Policy editWorkspacePolicy;
        private final Policy liveWorkspacePolicy;

        private SitePolicy(SiteCfg siteConfiguration) {
            // compile the regex patterns for the allowed and disallowed rule sets (shared for both workspaces)
            Map<String, Pattern> formatPatterns = new HashMap<>();
            if (siteConfiguration.getFormatDefinitions() != null) {
                siteConfiguration.getFormatDefinitions().forEach((formatName, formatRegex) -> {
                    formatPatterns.put(formatName, Pattern.compile(formatRegex)); // TODO properly throw meaningful exception if invalid regex
                    logger.debug("Compiled format {} regex: {}", formatName, formatRegex);
                });
            }
            editWorkspacePolicy = new PolicyImpl(formatPatterns, siteConfiguration.getEditWorkspace());
            liveWorkspacePolicy = new PolicyImpl(formatPatterns, siteConfiguration.getLiveWorkspace());
        }

        private Policy getPolicy(String workspaceName) {
            if (Constants.EDIT_WORKSPACE.equals(workspaceName)) {
                return editWorkspacePolicy;
            }
            if (!Constants.LIVE_WORKSPACE.equals(workspaceName)) {
                logger.warn("Unknown workspace name: {}, fallback to using live workspace policy", workspaceName);
            }
            return liveWorkspacePolicy;
        }
    }

}
