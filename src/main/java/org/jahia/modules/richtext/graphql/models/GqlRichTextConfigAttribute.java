package org.jahia.modules.richtext.graphql.models;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.HashSet;
import java.util.Set;

@GraphQLDescription("Model for richtext configuration attribute")
public class GqlRichTextConfigAttribute {

    private String attribute;
    private Set<String> elements = new HashSet<>();
    private String pattern;

    @GraphQLField
    @GraphQLName("attribute")
    @GraphQLDescription("Html attribute")
    public String getAttribute() {
        return attribute;
    }

    @GraphQLField
    @GraphQLName("elements")
    @GraphQLDescription("Elements for which attribute is applied")
    public Set<String> getElements() {
        return elements;
    }

    @GraphQLField
    @GraphQLName("pattern")
    @GraphQLDescription("Pattern used to validate attribute value")
    public String getPattern() {
        return pattern;
    }

    @GraphQLField
    @GraphQLName("isGlobal")
    @GraphQLDescription("Indicates if attribute is configured globally or for specific elements")
    public Boolean isGlobal() {
        return elements.isEmpty();
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
}
