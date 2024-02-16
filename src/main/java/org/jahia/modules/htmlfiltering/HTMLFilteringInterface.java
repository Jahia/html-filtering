/*
 * Copyright (C) 2002-2024 Jahia Solutions Group SA. All rights reserved.
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

import org.json.JSONObject;
import org.owasp.html.PolicyFactory;

public interface HTMLFilteringInterface {

    public static String DEFAULT_POLICY_KEY = "default";

    public String getCKEditor5Config(String siteKey);

    public String getCKEditor4Config(String siteKey);

    public PolicyFactory getOwaspPolicyFactory(String siteKey);

    public PolicyFactory getDefaultOwaspPolicyFactory();

    public PolicyFactory getMergedOwaspPolicyFactory(String... siteKeys);

    public JSONObject getMergedJSONPolicy(String... siteKeys);

    public boolean configExists(String siteKey);

    public boolean htmlSanitizerDryRun(String siteKey);
}
