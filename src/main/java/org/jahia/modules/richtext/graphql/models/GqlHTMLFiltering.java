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
