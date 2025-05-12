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

    @Override
    public String toString() {
        return "SiteCfg{" +
                "editWorkspace=" + editWorkspace +
                ", liveWorkspace=" + liveWorkspace +
                ", formatDefinitions=" + formatDefinitions +
                '}';
    }
}
