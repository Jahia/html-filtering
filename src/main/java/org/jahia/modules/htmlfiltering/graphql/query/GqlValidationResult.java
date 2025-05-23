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
import org.jahia.modules.htmlfiltering.PolicySanitizedHtmlResult;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * GraphQL representation of {@link PolicySanitizedHtmlResult}
 */
public class GqlValidationResult {

    private final PolicySanitizedHtmlResult policyExecutionResult;

    public GqlValidationResult(PolicySanitizedHtmlResult policyExecutionResult) {
        this.policyExecutionResult = policyExecutionResult;
    }

    @GraphQLField
    @GraphQLDescription("Returns true if the provided HTML has no unsafe tag or attribute. An unsafe tag (or attribute) " +
            "is either a tag not in allowed Tags, or in the disallowed tags from the OSGi configuration.")
    public boolean isSafe() {
        return policyExecutionResult.isValid();
    }

    @GraphQLField
    @GraphQLDescription("The sanitized HTML produced by the policy")
    public String getSanitizedHtml() {
        return policyExecutionResult.getSanitizedHtml();
    }

    @GraphQLField
    @GraphQLDescription("Removed tags")
    public Set<String> getRemovedTags() {
        return policyExecutionResult.getRejectedTags();
    }

    @GraphQLField
    @GraphQLDescription("Removed Attributes")
    public Set<GqlRemovedAttributes> getRemovedAttributes() {
        return policyExecutionResult.getRejectedAttributesByTag().entrySet().stream()
                .map(entry -> {
                    GqlRemovedAttributes gqlAttr = new GqlRemovedAttributes();
                    gqlAttr.setTag(entry.getKey());
                    gqlAttr.setAttributes(new HashSet<>(entry.getValue()));
                    return gqlAttr;
                })
                .collect(Collectors.toSet());
    }
}
