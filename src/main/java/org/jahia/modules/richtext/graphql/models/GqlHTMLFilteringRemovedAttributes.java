package org.jahia.modules.richtext.graphql.models;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.HashSet;
import java.util.Set;

@GraphQLDescription("Model for HTML filtering remove attributes")
public class GqlHTMLFilteringRemovedAttributes {

    private String tag;
    private Set<String> attributes = new HashSet<>();

    @GraphQLField
    @GraphQLName("tag")
    @GraphQLDescription("Tag for which attributes were removed")
    public String getTag() {
        return tag;
    }


    @GraphQLField
    @GraphQLName("attributes")
    @GraphQLDescription("Removed attributes")
    public Set<String> getAttributes() {
        return attributes;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setAttributes(Set<String> attributes) {
        this.attributes = attributes;
    }
}
