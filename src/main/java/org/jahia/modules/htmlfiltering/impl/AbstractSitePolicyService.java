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

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.concurrent.atomic.AtomicReference;

abstract class AbstractSitePolicyService implements ManagedService {

    // Uses getClass() to ensure logs show the correct implementing class name.
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final AtomicReference<SitePolicy> sitePolicyReference = new AtomicReference<>();

    SitePolicy getSitePolicy() {
        return sitePolicyReference.get();
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        if (properties == null) {
            sitePolicyReference.set(null);
            logger.info("Resetting site policy");
        } else {
            logger.info("Updating site policy with props: {}", properties);
            SitePolicy sitePolicy = SitePolicy.SitePolicyBuilder.build(properties);
            sitePolicyReference.set(sitePolicy);
        }
    }
}
