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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jahia.modules.htmlfiltering.Policy;
import org.jahia.modules.htmlfiltering.PolicyRegistry;
import org.jahia.modules.htmlfiltering.Strategy;
import org.jahia.modules.htmlfiltering.impl.config.ConfigBuilder;
import org.jahia.modules.htmlfiltering.impl.config.GlobalAbstractConfig;
import org.jahia.modules.htmlfiltering.impl.config.Config;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

@Component(immediate = true, service = {PolicyRegistry.class, ManagedServiceFactory.class},
        property = {
                "service.pid=org.jahia.modules.htmlfiltering.site",
                "service.description=HTML filtering config registry service to retrieve the policy to use for a given workspace of a given site",
                "service.vendor=Jahia Solutions Group SA"
        })
public final class PolicyRegistryImpl implements PolicyRegistry, ManagedServiceFactory {

    private static final Logger logger = LoggerFactory.getLogger(PolicyRegistryImpl.class);

    /**
     * Maps site keys to their corresponding {@link Config} configurations.
     */
    private final Map<String, Config> configsPerSiteKey = Collections.synchronizedMap(new HashMap<>());

    /**
     * Maps persistent identities (PIDs) to their corresponding site keys.
     */
    private final Map<String, String> sitesByPid = Collections.synchronizedMap(new HashMap<>());

    @Reference(target = "(service.pid=org.jahia.modules.htmlfiltering.global.custom)")
    private GlobalAbstractConfig globalCustomConfig;

    @Reference(target = "(service.pid=org.jahia.modules.htmlfiltering.global.default)")
    private GlobalAbstractConfig globalDefaultConfig;

    @Override
    public String getName() {
        return "HTML Filtering Registry Service";
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
        // extracting the site key from the configuration filename
        Object configurationPath = properties.get("felix.fileinstall.filename");
        if (configurationPath == null) {
            throw new ConfigurationException("felix.fileinstall.filename", "Missing configuration filename");
        }
        String configurationName = FilenameUtils.getBaseName(configurationPath.toString());
        String siteKey = StringUtils.substringAfter(configurationName, "-");

        logger.info("Updating html filtering configuration for {} (pid: {})", siteKey, pid);
        // build the config for the site
        Config config = null;
        try {
            config = ConfigBuilder.build(properties);
        } catch (ConfigurationException e) {
            logger.error("Unable to read the configuration for the site {}, unregistering it...", siteKey, e);
        }

        // update the maps
        configsPerSiteKey.put(siteKey, config);
        sitesByPid.put(pid, siteKey);
    }

    @Override
    public void deleted(String pid) {
        String siteKey = sitesByPid.remove(pid);
        configsPerSiteKey.remove(siteKey);

        logger.info("html-filtering config for {} (pid: {}) deleted.", siteKey, pid);
    }

    @Override
    public Policy resolvePolicy(String siteKey, String workspaceName) {
        // 1) site-specific configuration
        Config config = configsPerSiteKey.get(siteKey);
        if (config != null) {
            logger.debug("Site specific html-filtering config resolved for siteKey: {}", siteKey);
            return config.getPolicy(workspaceName);
        }

        // 2) global custom configuration
        config = globalCustomConfig.getHtmlFilteringConfig();
        if (config != null) {
            logger.debug("Global custom html-filtering config resolved for siteKey: {}", siteKey);
            return config.getPolicy(workspaceName);
        }

        // 3) global default configuration
        config = globalDefaultConfig.getHtmlFilteringConfig();
        if (config != null) {
            logger.debug("Global default html-filtering config resolved for siteKey: {}", siteKey);
            return config.getPolicy(workspaceName);
        }
        logger.debug("No html-filtering config resolved for siteKey: {}, workspaceName: {}", siteKey, workspaceName);
        return null;
    }

    @Override
    public Policy resolvePolicy(String siteKey, String workspaceName, Strategy strategy) {
        Policy policy = resolvePolicy(siteKey, workspaceName);
        if (policy != null && policy.getStrategy() == strategy) {
            return policy;
        }

        logger.debug("No html-filtering config resolved for siteKey: {}, workspaceName: {}, strategy: {}", siteKey, workspaceName, strategy.name());
        return null;
    }
}
