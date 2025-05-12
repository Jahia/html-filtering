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
package org.jahia.modules.htmlfiltering.configuration;

import java.util.Map;

public class SiteCfg {
    private WorkspaceCfg editWorkspace;
    private WorkspaceCfg liveWorkspace;
    private Map<String, String> formatDefinitions;

    public WorkspaceCfg getEditWorkspace() {
        return editWorkspace;
    }

    public void setEditWorkspace(WorkspaceCfg editWorkspace) {
        this.editWorkspace = editWorkspace;
    }

    public WorkspaceCfg getLiveWorkspace() {
        return liveWorkspace;
    }

    public void setLiveWorkspace(WorkspaceCfg liveWorkspace) {
        this.liveWorkspace = liveWorkspace;
    }

    public Map<String, String> getFormatDefinitions() {
        return formatDefinitions;
    }

    public void setFormatDefinitions(Map<String, String> formatDefinitions) {
        this.formatDefinitions = formatDefinitions;
    }
}
