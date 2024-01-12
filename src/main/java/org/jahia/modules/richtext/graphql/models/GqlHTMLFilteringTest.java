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
    private Set<String> removeElements = new HashSet<>();


    @GraphQLField
    @GraphQLName("html")
    @GraphQLDescription("Html after filtering")
    public String getHtml() {
        return html;
    }

    @GraphQLField
    @GraphQLName("removedElements")
    @GraphQLDescription("List of removed elements. Any attributes removed with the tag are not reported")
    public Set<String> getRemovedElements() {
        return removeElements;
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

    public void setRemoveElements(Set<String> removeElements) {
        this.removeElements = removeElements;
    }
}
