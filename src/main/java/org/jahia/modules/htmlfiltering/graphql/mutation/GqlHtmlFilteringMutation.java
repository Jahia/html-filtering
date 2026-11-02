package org.jahia.modules.htmlfiltering.graphql.mutation;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.NodeQueryExtensions;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;
import org.jahia.modules.htmlfiltering.graphql.ReportAccess;
import org.jahia.modules.htmlfiltering.report.ReportService;
import org.quartz.SchedulerException;

import javax.inject.Inject;
import javax.jcr.RepositoryException;

@GraphQLName("HTMLFilteringMutation")
@GraphQLDescription("HTML filtering mutation")
public class GqlHtmlFilteringMutation {

    private ReportService reportService;

    @Inject
    @GraphQLOsgiService
    public void setReportService(ReportService reportService) {
        this.reportService = reportService;
    }

    @GraphQLField
    @GraphQLName("runReport")
    @GraphQLDescription("Starts the HTML filtering report of a site in the background. Returns false if a report already runs on the platform, and starts nothing then. Requires the 'viewHtmlFilteringReport' permission on the site.")
    public boolean runReport(@GraphQLName("siteKey") @GraphQLNonNull String siteKey,
                             @GraphQLName("workspace") @GraphQLDescription("The workspace to scan, EDIT by default") NodeQueryExtensions.Workspace workspace) {
        String checkedSiteKey = ReportAccess.checkSite(siteKey);
        try {
            return reportService.start(checkedSiteKey, (workspace == null ? NodeQueryExtensions.Workspace.EDIT : workspace).getValue());
        } catch (RepositoryException | SchedulerException e) {
            throw new DataFetchingException(e);
        }
    }
}
