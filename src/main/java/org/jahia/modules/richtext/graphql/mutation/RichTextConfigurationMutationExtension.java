package org.jahia.modules.richtext.graphql.mutation;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.richtext.graphql.mutation.impl.GqlRichTextConfigurationMutation;

@GraphQLTypeExtension(DXGraphQLProvider.Mutation.class)
public class RichTextConfigurationMutationExtension {

    @GraphQLField
    @GraphQLName("richtextConfiguration")
    @GraphQLDescription("Entry point for richtext configuration mutations")
    public static GqlRichTextConfigurationMutation getRichtextConfiguration() {
        return new GqlRichTextConfigurationMutation();
    }
}
