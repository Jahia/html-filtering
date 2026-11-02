package org.jahia.modules.htmlfiltering.report;

import java.util.Calendar;
import java.util.List;

/**
 * The stored result of the last run of the report of a site.
 */
public final class Report {

    private final ReportStatus status;
    private final String workspace;
    private final Calendar started;
    private final Calendar finished;
    private final long maxItems;
    private final long itemCount;
    private final List<ReportItem> items;

    public Report(ReportStatus status, String workspace, Calendar started, Calendar finished, long maxItems, long itemCount,
                  List<ReportItem> items) {
        this.status = status;
        this.workspace = workspace;
        this.started = started;
        this.finished = finished;
        this.maxItems = maxItems;
        this.itemCount = itemCount;
        this.items = List.copyOf(items);
    }

    public ReportStatus getStatus() {
        return status;
    }

    /**
     * @return the name of the scanned workspace, <code>default</code> or <code>live</code>
     */
    public String getWorkspace() {
        return workspace;
    }

    public Calendar getStarted() {
        return started;
    }

    /**
     * @return the end of the run, or <code>null</code> while the report runs
     */
    public Calendar getFinished() {
        return finished;
    }

    public long getMaxItems() {
        return maxItems;
    }

    /**
     * @return the number of items found, which grows while the report runs
     */
    public long getItemCount() {
        return itemCount;
    }

    /**
     * @return the items, which the report stores at the end of the run only
     */
    public List<ReportItem> getItems() {
        return items;
    }
}
