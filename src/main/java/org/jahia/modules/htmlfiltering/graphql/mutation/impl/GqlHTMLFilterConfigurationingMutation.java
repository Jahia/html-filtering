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
package org.jahia.modules.htmlfiltering.graphql.mutation.impl;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.modules.htmlfiltering.graphql.mutation.impl.html_filtering.GqlHtmlFilteringMutation;

@GraphQLName("HTMLFilteringMutation")
@GraphQLDescription("HTML Filtering mutations entry point")
public class GqlHTMLFilterConfigurationingMutation {

    @GraphQLField
    @GraphQLName("htmlFiltering")
    @GraphQLDescription("HTML filtering mutation")
    public GqlHtmlFilteringMutation getHtmlFiltering() {
        return new GqlHtmlFilteringMutation();
    }
}
