package org.jahia.modules.richtext.graphql.models;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@GraphQLDescription("Model for disallowed richtext configuration")
public class GqlRichTextDisallowedConfig implements RichTextConfigInterface {

    private Set<String> protocols = new HashSet<>();
    private Set<String> elements = new HashSet<>();
    private List<GqlRichTextConfigAttribute> attributes = new ArrayList<>();

    @GraphQLField
    @GraphQLName("protocols")
    @GraphQLDescription("Protocols")
    public Set<String> getProtocols() {
        return protocols;
    }

    @GraphQLField
    @GraphQLName("elements")
    @GraphQLDescription("HTML elements")
    public Set<String> getElements() {
        return elements;
    }

    @GraphQLField
    @GraphQLName("attributes")
    @GraphQLDescription("HTML attributes")
    public List<GqlRichTextConfigAttribute> getAttributes() {
        return attributes;
    }
}
