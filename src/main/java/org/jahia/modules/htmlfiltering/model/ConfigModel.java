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
package org.jahia.modules.htmlfiltering.model;

import org.jahia.modules.htmlfiltering.model.validation.constraints.ValidFormatDefinitions;
import org.jahia.modules.htmlfiltering.model.validation.constraints.ValidFormatReference;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * Represents the HTML filtering configuration for a given site.
 * A configuration can be defined for a given site by creating a file named <code>org.jahia.modules.htmlfiltering.site-&lt;siteKey&gt;.yml</code> in the <code>META-INF/configurations/</code> folder of a bundle.
 */
@ValidFormatReference
public class ConfigModel {

    @NotNull
    @Valid
    private PolicyModel editWorkspace;
    @NotNull
    @Valid
    private PolicyModel liveWorkspace;
    @ValidFormatDefinitions
    private Map<String, String> formatDefinitions;

    public PolicyModel getEditWorkspace() {
        return editWorkspace;
    }

    public void setEditWorkspace(PolicyModel editWorkspace) {
        this.editWorkspace = editWorkspace;
    }

    public PolicyModel getLiveWorkspace() {
        return liveWorkspace;
    }

    public void setLiveWorkspace(PolicyModel liveWorkspace) {
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
        return "ConfigModel{" +
                "editWorkspace=" + editWorkspace +
                ", liveWorkspace=" + liveWorkspace +
                ", formatDefinitions=" + formatDefinitions +
                '}';
    }
}
