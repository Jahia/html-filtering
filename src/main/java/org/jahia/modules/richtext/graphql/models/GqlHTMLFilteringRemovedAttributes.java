package org.jahia.modules.richtext.graphql.models;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.HashSet;
import java.util.Set;

@GraphQLDescription("Model for HTML filtering remove attributes")
public class GqlHTMLFilteringRemovedAttributes {

    private String element;
    private Set<String> attributes = new HashSet<>();

    @GraphQLField
    @GraphQLName("element")
    @GraphQLDescription("Element for which attributes were removed")
    public String getElement() {
        return element;
    }


    @GraphQLField
    @GraphQLName("attributes")
    @GraphQLDescription("Removed attributes")
    public Set<String> getAttributes() {
        return attributes;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public void setAttributes(Set<String> attributes) {
        this.attributes = attributes;
    }
}
