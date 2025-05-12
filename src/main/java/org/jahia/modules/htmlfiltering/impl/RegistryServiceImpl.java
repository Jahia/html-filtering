/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2025 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.htmlfiltering.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component(immediate = true, service = {RegistryService.class, ManagedServiceFactory.class},
        property = {
                "service.pid=org.jahia.modules.htmlfiltering",
                "service.description=TODO", // TODO
                "service.vendor=Jahia Solutions Group SA"
        })
public final class RegistryServiceImpl implements RegistryService, ManagedServiceFactory {

    private static final Logger logger = LoggerFactory.getLogger(RegistryServiceImpl.class);

    private final JavaPropsMapper javaPropsMapper = JavaPropsMapper.builder()
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
            .build();

    private final Map<String, SitePolicy> policiesBySite = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, SitePolicy> policiesByPid = Collections.synchronizedMap(new HashMap<>());

    @Override
    public String getName() {
        // ${TODO} Auto-generated method stub
        return "";
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
        // TODO get rid of "default" in the default config name
        logger.info("Updating configuration for pid {}", pid);

        // extracting the site key from the configuration filename
        Object configurationPath = properties.get("felix.fileinstall.filename");
        if (configurationPath == null) {
            throw new ConfigurationException("felix.fileinstall.filename", "Missing configuration filename");
        }
        Path configurationFilename = Paths.get(configurationPath.toString()).getFileName();
        String configurationName = FilenameUtils.getBaseName(configurationPath.toString());
        String siteKey = StringUtils.substringAfter(configurationName, "-");
        if (siteKey == null) {
            throw new ConfigurationException("siteKey", "Missing siteKey in configuration filename: " + configurationFilename);
        }


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

        policiesBySite.put(siteKey, new SitePolicy(siteConfiguration));
    }

    @Override
    public void deleted(String pid) {
        // ${TODO} Auto-generated method stub

    }

    @Override
    public Policy getPolicy(String siteKey, String workspaceName) {
        SitePolicy sitePolicy = policiesBySite.get(siteKey);
        if (sitePolicy != null) {
            return sitePolicy.getPolicy(workspaceName);
        }

        // get the default policy if defined
        SitePolicy defaultSitePolicy = policiesBySite.get("default");
        if (defaultSitePolicy != null) {
            return defaultSitePolicy.getPolicy(workspaceName);
        }
        logger.debug("No policy defined for siteKey: {}", siteKey);
        return null;
    }

    private static final class SitePolicy {

        Policy editWorkspacePolicy;
        Policy liveWorkspacePolicy;

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
