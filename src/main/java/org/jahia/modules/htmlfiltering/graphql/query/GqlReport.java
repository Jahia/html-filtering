package org.jahia.modules.htmlfiltering.graphql.query;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.node.NodeQueryExtensions;
import org.jahia.modules.htmlfiltering.report.Report;
import org.jahia.modules.htmlfiltering.report.ReportCsv;
import org.jahia.modules.htmlfiltering.report.ReportItem;
import org.jahia.modules.htmlfiltering.report.ReportStatus;

import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

@GraphQLName("HTMLFilteringReport")
@GraphQLDescription("The last run of the HTML filtering report of a site")
public class GqlReport {

    private final Report report;
    private final boolean running;

    public GqlReport(Report report, boolean running) {
        this.report = report;
        this.running = running;
    }

    @GraphQLField
    @GraphQLName("status")
    @GraphQLNonNull
    @GraphQLDescription("The state of the run")
    public ReportStatus getStatus() {
        return report.getStatus();
    }

    @GraphQLField
    @GraphQLName("workspace")
    @GraphQLNonNull
    @GraphQLDescription("The scanned workspace")
    public NodeQueryExtensions.Workspace getWorkspace() {
        return Constants.LIVE_WORKSPACE.equals(report.getWorkspace())
                ? NodeQueryExtensions.Workspace.LIVE : NodeQueryExtensions.Workspace.EDIT;
    }

    @GraphQLField
    @GraphQLName("started")
    @GraphQLNonNull
    @GraphQLDescription("The start of the run, in ISO 8601")
    public String getStarted() {
        return iso(report.getStarted());
    }

    @GraphQLField
    @GraphQLName("finished")
    @GraphQLDescription("The end of the run, in ISO 8601, or null while the report runs")
    public String getFinished() {
        return iso(report.getFinished());
    }

    @GraphQLField
    @GraphQLName("maxItems")
    @GraphQLNonNull
    @GraphQLDescription("The maximum number of items of the run")
    public int getMaxItems() {
        return (int) report.getMaxItems();
    }

    @GraphQLField
    @GraphQLName("itemCount")
    @GraphQLNonNull
    @GraphQLDescription("The number of items found, which grows while the report runs")
    public int getItemCount() {
        return (int) report.getItemCount();
    }

    @GraphQLField
    @GraphQLName("platformBusy")
    @GraphQLNonNull
    @GraphQLDescription("Whether a report runs on the platform, on this site or on another one")
    public boolean isPlatformBusy() {
        return running;
    }

    @GraphQLField
    @GraphQLName("items")
    @GraphQLNonNull
    @GraphQLDescription("A page of the items that the filters keep")
    public GqlReportItems items(@GraphQLName("path") @GraphQLDescription("Keep the items of the node at this path and of the nodes under it") String path,
                                @GraphQLName("markup") @GraphQLDescription("Keep the items whose markups include this tag or this attribute") String markup,
                                @GraphQLName("offset") @GraphQLDescription("The index of the first item of the page") Integer offset,
                                @GraphQLName("limit") @GraphQLDescription("The number of items of the page, every item if null") Integer limit) {
        List<ReportItem> kept = filter(path, markup);
        int from = Math.min(offset == null ? 0 : Math.max(offset, 0), kept.size());
        int to = limit == null ? kept.size() : Math.min(from + Math.max(limit, 0), kept.size());
        return new GqlReportItems(kept.subList(from, to), kept.size());
    }

    @GraphQLField
    @GraphQLName("csv")
    @GraphQLNonNull
    @GraphQLDescription("Every item that the filters keep, as CSV with the columns Path, Property, Language and Markups")
    public String csv(@GraphQLName("path") @GraphQLDescription("Keep the items of the node at this path and of the nodes under it") String path,
                      @GraphQLName("markup") @GraphQLDescription("Keep the items whose markups include this tag or this attribute") String markup) {
        return ReportCsv.toCsv(filter(path, markup));
    }

    private List<ReportItem> filter(String path, String markup) {
        return report.getItems().stream()
                .filter(item -> item.isUnder(path) && item.includesMarkup(markup))
                .collect(Collectors.toList());
    }

    private static String iso(Calendar calendar) {
        return calendar == null ? null : calendar.toInstant().toString();
    }
}
