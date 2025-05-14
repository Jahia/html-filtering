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

import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = {ManagedService.class, AbstractSitePolicyService.class},
        property = {
                "service.pid=org.jahia.modules.htmlfiltering.fallback",
                "service.description=HTML filtering fallback policy service to retrieve the fallback policy",
                "service.vendor=Jahia Solutions Group SA"
        })
public final class FallbackSitePolicyServiceImpl extends AbstractSitePolicyService {

}
