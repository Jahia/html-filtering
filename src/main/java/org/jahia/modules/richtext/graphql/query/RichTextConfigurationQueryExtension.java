package org.jahia.modules.richtext.graphql.query;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.richtext.graphql.query.impl.GqlRichTextConfigurationQuery;

@GraphQLTypeExtension(DXGraphQLProvider.Query.class)
public class RichTextConfigurationQueryExtension {

    @GraphQLField
    @GraphQLName("richtextConfiguration")
    @GraphQLDescription("Entry point for richtext configuration queries")
    public static GqlRichTextConfigurationQuery getRichtextConfiguration() {
        return new GqlRichTextConfigurationQuery();
    }
}
