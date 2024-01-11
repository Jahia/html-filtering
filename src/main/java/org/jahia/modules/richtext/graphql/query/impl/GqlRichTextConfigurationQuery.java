package org.jahia.modules.richtext.graphql.query.impl;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.modules.richtext.graphql.query.impl.htmlFiltering.GqlHtmlFilteringQuery;

@GraphQLName("RichTextConfigurationQuery")
@GraphQLDescription("Entry point for site richtext configuration queries")
public class GqlRichTextConfigurationQuery {

    @GraphQLField
    @GraphQLName("htmlFiltering")
    @GraphQLDescription("HTML filtering settings queries")
    public GqlHtmlFilteringQuery getHtmlFiltering() {
        return new GqlHtmlFilteringQuery();
    }
}
