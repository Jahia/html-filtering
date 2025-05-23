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
package org.jahia.modules.htmlfiltering.graphql.query;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.modules.graphql.provider.dxm.node.NodeQueryExtensions;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;
import org.jahia.modules.htmlfiltering.Policy;
import org.jahia.modules.htmlfiltering.PolicyRegistry;

import javax.inject.Inject;

@GraphQLName("HTMLFilteringQuery")
@GraphQLDescription("HTML filtering query")
public class GqlHtmlFilteringQuery {


    private PolicyRegistry registry;

    @Inject
    @GraphQLOsgiService
    public void setRegistry(PolicyRegistry registry) {
        this.registry = registry;
    }

    @GraphQLField
    @GraphQLName("validate")
    @GraphQLDescription("Validate a given html from a resolved policy from a provided worskpace and site from its OSGi configuration, then returns sanitized HTML, removed tags and attributes")
    public GqlValidationResult validate(@GraphQLName("html") String html, @GraphQLName("workspace") NodeQueryExtensions.Workspace workspace, @GraphQLName("siteKey") String siteKey) {
        Policy policy = registry.resolvePolicy(siteKey, workspace.getValue());
        if (policy != null) {
            return new GqlValidationResult(policy.sanitize(html));
        }

        return null;
    }
}

