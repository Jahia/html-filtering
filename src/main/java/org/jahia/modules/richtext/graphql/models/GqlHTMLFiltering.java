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

@GraphQLDescription("Model for HTML filter settings of a site")
public class GqlHTMLFiltering {

    private String siteKey;
    private boolean filteringEnabled;

    public GqlHTMLFiltering(String siteKey, boolean filteringEnabled) {
        this.siteKey = siteKey;
        this.filteringEnabled = filteringEnabled;
    }

    @GraphQLField
    @GraphQLName("siteKey")
    @GraphQLDescription("Site key")
    public String getSiteKey() {
        return siteKey;
    }


    @GraphQLField
    @GraphQLName("filteringEnabled")
    @GraphQLDescription("Indicates if html filtering is enabled or not")
    public Boolean getFilteringEnabled() {
        return filteringEnabled;
    }
}
