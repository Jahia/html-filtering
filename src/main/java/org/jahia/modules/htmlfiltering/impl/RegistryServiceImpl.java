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
import org.jahia.modules.htmlfiltering.RegistryService;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Component(immediate = true, service = {RegistryService.class, ManagedServiceFactory.class},
        property = {
                "service.pid=org.jahia.modules.htmlfiltering",
                "service.description=HTML filtering registry service to retrieve the policy to use for a given workspace of a given site",
                "service.vendor=Jahia Solutions Group SA"
        })
public final class RegistryServiceImpl implements RegistryService, ManagedServiceFactory {

    private static final Logger logger = LoggerFactory.getLogger(RegistryServiceImpl.class);

    /**
     * Maps site keys to their corresponding {@link SitePolicy} configurations.
     */
    private final Map<String, SitePolicy> policiesBySiteKey = Collections.synchronizedMap(new HashMap<>());

    /**
     * Maps persistent identities (PIDs) to their corresponding site keys.
     */
    private final Map<String, String> sitesByPid = Collections.synchronizedMap(new HashMap<>());

    @Reference(target = "(service.pid=org.jahia.modules.htmlfiltering.default)")
    private AbstractSitePolicyService defaultSitePolicyService;

    @Reference(target = "(service.pid=org.jahia.modules.htmlfiltering.fallback)")
    private AbstractSitePolicyService fallbackSitePolicyService;

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

        // build the site policy for the site
        SitePolicy sitePolicy = SitePolicy.SitePolicyBuilder.build(properties);

        // update the maps
        policiesBySiteKey.put(siteKey, sitePolicy);
        sitesByPid.put(pid, siteKey);

        logger.info("Site policy for {} (pid: {}) updated.", siteKey, pid);
    }

    @Override
    public void deleted(String pid) {
        String siteKey = sitesByPid.remove(pid);
        policiesBySiteKey.remove(siteKey);

        logger.info("Site policy for {} (pid: {}) deleted.", siteKey, pid);
    }

    @Override
    public Policy getPolicy(String siteKey, String workspaceName) {
        // 1) site-specific configuration
        SitePolicy sitePolicy = policiesBySiteKey.get(siteKey);
        if (sitePolicy != null) {
            logger.debug("Site policy found for siteKey: {}", siteKey);
            return sitePolicy.getPolicy(workspaceName);
        }

        // 2) default configuration
        sitePolicy = defaultSitePolicyService.getSitePolicy();
        if (sitePolicy != null) {
            logger.debug("Default site policy found");
            return sitePolicy.getPolicy(workspaceName);
        }

        // 3) fallback configuration
        sitePolicy = fallbackSitePolicyService.getSitePolicy();
        if (sitePolicy != null) {
            logger.debug("Fallback site policy found");
            return sitePolicy.getPolicy(workspaceName);
        }
        logger.debug("No site policy found for siteKey: {}, workspaceName: {}", siteKey, workspaceName);
        return null;
    }
}
