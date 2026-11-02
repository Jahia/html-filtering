package org.jahia.modules.htmlfiltering.graphql.query;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.modules.htmlfiltering.report.ReportItem;

import java.util.List;
import java.util.stream.Collectors;

@GraphQLName("HTMLFilteringReportItems")
@GraphQLDescription("A page of the items of an HTML filtering report that the filters keep")
public class GqlReportItems {

    private final List<ReportItem> page;
    private final int totalCount;

    public GqlReportItems(List<ReportItem> page, int totalCount) {
        this.page = page;
        this.totalCount = totalCount;
    }

    @GraphQLField
    @GraphQLName("totalCount")
    @GraphQLNonNull
    @GraphQLDescription("The number of items that the filters keep, on all the pages")
    public int getTotalCount() {
        return totalCount;
    }

    @GraphQLField
    @GraphQLName("nodes")
    @GraphQLNonNull
    @GraphQLDescription("The items of the page")
    public List<GqlReportItem> getNodes() {
        return page.stream().map(GqlReportItem::new).collect(Collectors.toList());
    }
}
