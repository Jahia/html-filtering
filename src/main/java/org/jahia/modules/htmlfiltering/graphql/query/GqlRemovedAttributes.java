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

import java.util.Set;

/**
 * Graphql representation of a removed attribute.
 */
public class GqlRemovedAttributes {

    private String tag;
    private Set<String> attributes;

    @GraphQLField
    @GraphQLDescription("Tag on witch the attribute has been removed")
    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    @GraphQLField
    @GraphQLDescription("Name of the removed attribute")
    public Set<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Set<String> attributes) {
        this.attributes = attributes;
    }
}
