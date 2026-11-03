package org.jahia.modules.htmlfiltering.report;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLName;

/**
 * The state of the last run of the report of a site.
 */
@GraphQLName("HTMLFilteringReportStatus")
@GraphQLDescription("The state of the last run of the HTML filtering report of a site")
public enum ReportStatus {
    /**
     * The report runs, and its items are the items found so far.
     */
    RUNNING,
    /**
     * The report scanned the whole site.
     */
    DONE,
    /**
     * The report stopped once it found the maximum number of items.
     */
    LIMIT_REACHED,
    /**
     * The report stopped on an error. The log of the processing node holds it.
     */
    FAILED
}
