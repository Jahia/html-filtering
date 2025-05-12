package org.jahia.modules.htmlfiltering;/*
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
