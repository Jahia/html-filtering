package org.jahia.modules.htmlfiltering.report;

import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.scheduler.BackgroundJob;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

/**
 * The background job of one run of the HTML filtering report.
 */
public class ReportJob extends BackgroundJob {

    static final String SITE_KEY = "htmlFilteringSiteKey";
    static final String WORKSPACE = "htmlFilteringWorkspace";

    @Override
    public void executeJahiaJob(JobExecutionContext context) throws Exception {
        ReportService service = BundleUtils.getOsgiService(ReportService.class, null);
        if (service == null) {
            throw new IllegalStateException("The HTML filtering report service is not available");
        }
        JobDataMap data = context.getJobDetail().getJobDataMap();
        try {
            service.run(data.getString(SITE_KEY), data.getString(WORKSPACE));
        } finally {
            JCRSessionFactory.getInstance().closeAllSessions();
        }
    }
}
