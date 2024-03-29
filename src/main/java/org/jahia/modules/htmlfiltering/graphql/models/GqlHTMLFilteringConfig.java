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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@GraphQLDescription("Model for HTML Filtering")
public class GqlHTMLFilteringConfig implements HTMLFilteringConfigInterface {

    private Set<String> protocols = new HashSet<>();
    private Set<String> elements = new HashSet<>();
    private List<GqlHTMLFilteringConfigAttribute> attributes = new ArrayList<>();
    private GqlHTMLFilteringDisallowedConfig disallow = new GqlHTMLFilteringDisallowedConfig();

    @GraphQLField
    @GraphQLName("protocols")
    @GraphQLDescription("Protocols")
    public Set<String> getProtocols() {
        return protocols;
    }

    @GraphQLField
    @GraphQLName("elements")
    @GraphQLDescription("HTML elements")
    public Set<String> getElements() {
        return elements;
    }

    @GraphQLField
    @GraphQLName("attributes")
    @GraphQLDescription("HTML attributes")
    public List<GqlHTMLFilteringConfigAttribute> getAttributes() {
        return attributes;
    }

    @GraphQLField
    @GraphQLName("disallow")
    @GraphQLDescription("Disallowed html elements and attributes")
    public GqlHTMLFilteringDisallowedConfig getDisallow() {
        return disallow;
    }
}
