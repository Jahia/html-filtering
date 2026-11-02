package org.jahia.modules.htmlfiltering.graphql.query;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.modules.htmlfiltering.report.ReportItem;

import java.util.List;

@GraphQLName("HTMLFilteringReportItem")
@GraphQLDescription("A rich text property of a content node, in one language, whose value does not pass the HTML filtering policy")
public class GqlReportItem {

    private final ReportItem item;

    public GqlReportItem(ReportItem item) {
        this.item = item;
    }

    @GraphQLField
    @GraphQLName("path")
    @GraphQLNonNull
    @GraphQLDescription("The path of the content node")
    public String getPath() {
        return item.getPath();
    }

    @GraphQLField
    @GraphQLName("property")
    @GraphQLNonNull
    @GraphQLDescription("The name of the property")
    public String getProperty() {
        return item.getProperty();
    }

    @GraphQLField
    @GraphQLName("language")
    @GraphQLDescription("The language of the value, or null for a property that is not internationalized")
    public String getLanguage() {
        return item.getLanguage();
    }

    @GraphQLField
    @GraphQLName("markups")
    @GraphQLNonNull
    @GraphQLDescription("Each markup that the policy rejected: a tag, such as <script>, or an attribute with its tag, such as <img onerror>")
    public List<String> getMarkups() {
        return item.getMarkups();
    }
}
