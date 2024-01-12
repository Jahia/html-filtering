package org.jahia.modules.richtext.graphql.mutation.impl;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.modules.richtext.graphql.mutation.impl.htmlFiltering.GqlHtmlFilteringMutation;

@GraphQLName("RichTextConfigurationMutation")
@GraphQLDescription("RichText configuration mutations entry point")
public class GqlRichTextConfigurationMutation {

    @GraphQLField
    @GraphQLName("htmlFiltering")
    @GraphQLDescription("HTML filtering mutation")
    public GqlHtmlFilteringMutation getHtmlFiltering() {
        return new GqlHtmlFilteringMutation();
    }
}
