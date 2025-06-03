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
package org.jahia.modules.htmlfiltering;

/**
 * The {@link PolicyResolver} interface provides a mechanism to manage and retrieve
 * HTML filtering policies for specific sites, workspaces, strategies.
 * <p>
 * This service allows retrieving a {@link Policy} for a specific site and workspace,
 * enabling tailored HTML content filtering and validation based on the given context.
 */
public interface PolicyResolver {


    /**
     * Retrieves the HTML filtering policy for the specified site and workspace.
     * <p>
     * Policy resolution strategy (by priority):
     * <ol>
     *   <li>Site-specific configuration: <code>org.jahia.modules.htmlfiltering.site-&lt;site key&gt;.yml</code> found in any OSGi bundle</li>
     *   <li>Global custom configuration: <code>org.jahia.modules.htmlfiltering.global.custom.yml</code> found in any OSGi bundle, that can be used to configure multiple sites with the same configuration if they don't have a site-specific configuration</li>
     *   <li>Global default configuration: <code>org.jahia.modules.htmlfiltering.global.default.yml</code> provided by the <i>HTML Filtering</i> bundle</li>
     * </ol>
     * <p>
     * <strong>Note:</strong> If the requested workspace does not exist for the given site, the default "live" workspace is used.
     * The global default configuration should not be overwritten as module updates would replace it.
     *
     * @param siteKey       the unique identifier for the site
     * @param workspaceName the name of the workspace
     * @return the policy applicable to the given site and workspace, or <code>null</code> if no policy is found
     */
    Policy resolvePolicy(String siteKey, String workspaceName);

    /**
     * Same as {@link #resolvePolicy(String, String)} but allows to specify a required strategy.
     * <p>
     * @param siteKey       the unique identifier for the site
     * @param workspaceName the name of the workspace
     * @param strategy      the strategy to be used for the policy
     * @return the policy applicable to the given site, workspace and strategy, or <code>null</code> if no policy is found
     */
    Policy resolvePolicy(String siteKey, String workspaceName, Strategy strategy);
}
