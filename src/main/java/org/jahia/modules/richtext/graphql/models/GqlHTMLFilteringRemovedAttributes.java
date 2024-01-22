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
package org.jahia.modules.richtext.graphql.models;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.HashSet;
import java.util.Set;

@GraphQLDescription("Model for HTML filtering remove attributes")
public class GqlHTMLFilteringRemovedAttributes {

    private String element;
    private Set<String> attributes = new HashSet<>();

    @GraphQLField
    @GraphQLName("element")
    @GraphQLDescription("Element for which attributes were removed")
    public String getElement() {
        return element;
    }


    @GraphQLField
    @GraphQLName("attributes")
    @GraphQLDescription("Removed attributes")
    public Set<String> getAttributes() {
        return attributes;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public void setAttributes(Set<String> attributes) {
        this.attributes = attributes;
    }
}
