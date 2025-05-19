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
package org.jahia.modules.htmlfiltering.graphql.query.impl.html_filtering;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import org.jahia.modules.htmlfiltering.HtmlValidationResult;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * GraphQL representation of {@link HtmlValidationResult}
 */
public class GqlHtmlFilteringResult {

    private final String sanitizedHtml;
    private final Set<String> removedTags;
    private final Set<GqlRemovedAttributes> removedAttributes;
    private final boolean isValid;

    public GqlHtmlFilteringResult(HtmlValidationResult validationResult) {
        this.sanitizedHtml = validationResult.getSanitizedHtml();
        this.removedTags = validationResult.getRejectedTags();
        this.removedAttributes = validationResult.getRejectedAttributesByTag().entrySet().stream()
                .map(entry -> {
                    GqlRemovedAttributes gqlAttr = new GqlRemovedAttributes();
                    gqlAttr.setElement(entry.getKey());
                    gqlAttr.setAttributes(new HashSet<>(entry.getValue()));
                    return gqlAttr;
                })
                .collect(Collectors.toSet());
        this.isValid = validationResult.isValid();
    }

    @GraphQLField
    @GraphQLDescription("Check the validation of the sanitazation, Returns true is the provided HTLM has no removed tags or attributes")
    public boolean isValid() {
        return isValid;
    }

    @GraphQLField
    @GraphQLDescription("The sanitized HTML produced by the policy")
    public String getSanitizedHtml() {
        return sanitizedHtml;
    }

    @GraphQLField
    @GraphQLDescription("Removed tags")
    public Set<String> getRemovedTags() {
        return removedTags;
    }

    @GraphQLField
    @GraphQLDescription("Removed Attributes")
    public Set<GqlRemovedAttributes> getRemovedAttributes() {
        return removedAttributes;
    }
}
