package org.jahia.modules.htmlfiltering.graphql.mutation;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;

@GraphQLTypeExtension(DXGraphQLProvider.Mutation.class)
@SuppressWarnings("java:S1118") // ignore "Utility classes should not have public constructors"
public class GqlHtmlFilteringMutationExtension {

    @GraphQLField
    @GraphQLName("htmlFiltering")
    @GraphQLDescription("Main access to html filtering mutations")
    public static GqlHtmlFilteringMutation getHtmlFiltering() {
        return new GqlHtmlFilteringMutation();
    }
}
