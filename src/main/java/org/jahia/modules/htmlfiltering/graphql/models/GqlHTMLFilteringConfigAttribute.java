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
package org.jahia.modules.htmlfiltering.graphql.models;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.HashSet;
import java.util.Set;

@GraphQLDescription("Model for HTML Filtering attribute")
public class GqlHTMLFilteringConfigAttribute {

    private String attribute;
    private Set<String> elements = new HashSet<>();
    private String pattern;

    @GraphQLField
    @GraphQLName("attribute")
    @GraphQLDescription("Html attribute")
    public String getAttribute() {
        return attribute;
    }

    @GraphQLField
    @GraphQLName("elements")
    @GraphQLDescription("Elements for which attribute is applied")
    public Set<String> getElements() {
        return elements;
    }

    @GraphQLField
    @GraphQLName("pattern")
    @GraphQLDescription("Pattern used to validate attribute value")
    public String getPattern() {
        return pattern;
    }

    @GraphQLField
    @GraphQLName("isGlobal")
    @GraphQLDescription("Indicates if attribute is configured globally or for specific elements")
    public Boolean isGlobal() {
        return elements.isEmpty();
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
}
