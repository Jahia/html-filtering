package org.jahia.modules.richtext.graphql.models;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@GraphQLDescription("Model for HTML filter test")
public class GqlHTMLFilteringTest {

    private String html;
    private List<GqlHTMLFilteringRemovedAttributes> removeAttributes = new ArrayList<>();
    private Set<String> removeTags = new HashSet<>();


    @GraphQLField
    @GraphQLName("html")
    @GraphQLDescription("Html after filtering")
    public String getHtml() {
        return html;
    }

    @GraphQLField
    @GraphQLName("removedTags")
    @GraphQLDescription("List of removed tags. Any attributes removed with the tag are not reported")
    public Set<String> getRemovedTags() {
        return removeTags;
    }


    @GraphQLField
    @GraphQLName("removedAttributes")
    @GraphQLDescription("Removed attributes")
    public List<GqlHTMLFilteringRemovedAttributes> getRemovedAttributes() {
        return removeAttributes;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public void setRemoveAttributes(List<GqlHTMLFilteringRemovedAttributes> removeAttributes) {
        this.removeAttributes = removeAttributes;
    }

    public void setRemoveTags(Set<String> removeTags) {
        this.removeTags = removeTags;
    }
}
