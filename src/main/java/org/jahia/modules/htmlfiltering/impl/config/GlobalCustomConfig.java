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
package org.jahia.modules.htmlfiltering.impl.config;

import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = {ManagedService.class, GlobalAbstractConfig.class},
        property = {
                "service.pid=org.jahia.modules.htmlfiltering.global.custom",
                "service.description=HTML filtering global custom config service to retrieve the global custom config to use for sites that do not have a site-specific configuration",
                "service.vendor=Jahia Solutions Group SA"
        })
public final class GlobalCustomConfig extends GlobalAbstractConfig {

        // This class is intentionally empty. It serves as a marker for the global default configuration.
        // The actual implementation is in the parent class HtmlFilteringAbstractGlobalConfig.
        // The properties defined in the @Component annotation are used for OSGi service registration.
}
