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

import org.jahia.api.Constants;
import org.jahia.modules.htmlfiltering.Policy;

public final class Config {

    private final Policy editWorkspacePolicy;
    private final Policy liveWorkspacePolicy;

    public Config(Policy editWorkspacePolicy, Policy liveWorkspacePolicy) {
        this.editWorkspacePolicy = editWorkspacePolicy;
        this.liveWorkspacePolicy = liveWorkspacePolicy;
    }

    public Policy getPolicy(String workspaceName) {
        if (Constants.EDIT_WORKSPACE.equals(workspaceName)) {
            return editWorkspacePolicy;
        }
        if (!Constants.LIVE_WORKSPACE.equals(workspaceName)) {
            throw new IllegalArgumentException("Invalid workspace name: " + workspaceName);
        }
        return liveWorkspacePolicy;
    }

    public Policy getEditWorkspacePolicy() {
        return editWorkspacePolicy;
    }

    public Policy getLiveWorkspacePolicy() {
        return liveWorkspacePolicy;
    }
}
