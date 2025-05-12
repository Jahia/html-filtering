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

public interface RegistryService {
    /**
     * Get the policy for the given site and workspace.
     * If the workspace does not exist for the given site, the default "live" workspace is used.
     *
     * @param siteKey       the site key
     * @param workspaceName the workspace name
     * @return the policy or null if no policy is found for the given site and workspace
     */
    Policy getPolicy(String siteKey, String workspaceName);
}
