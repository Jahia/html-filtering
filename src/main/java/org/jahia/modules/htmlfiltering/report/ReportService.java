package org.jahia.modules.htmlfiltering.report;

import org.jahia.modules.htmlfiltering.Policy;
import org.jahia.modules.htmlfiltering.PolicyResolver;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.scheduler.BackgroundJob;
import org.jahia.services.scheduler.SchedulerService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.Collections;

/**
 * Runs the HTML filtering report of a site as a background job, and reads the result of its last run.
 * <p>
 * The job runs on the persistent scheduler, which Jahia starts on the processing node only.
 */
@Component(service = ReportService.class, configurationPid = "org.jahia.modules.htmlfiltering.report")
@Designate(ocd = ReportService.Config.class)
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    @ObjectClassDefinition(name = "HTML filtering report", description = "Limits of the HTML filtering report")
    public @interface Config {
        @AttributeDefinition(name = "Maximum number of items", description = "The report stops once it finds this number of items")
        int maxItems() default 500;

        @AttributeDefinition(name = "Batch size", description = "The number of content nodes the report reads between two pauses")
        int batchSize() default 100;

        @AttributeDefinition(name = "Pause", description = "The pause between two batches, in milliseconds")
        long pauseMillis() default 100;
    }

    private PolicyResolver policyResolver;
    private volatile Config config;

    @Reference
    public void setPolicyResolver(PolicyResolver policyResolver) {
        this.policyResolver = policyResolver;
    }

    @Activate
    @Modified
    public void activate(Config config) {
        this.config = config;
    }

    /**
     * @return the last run of the report of the site, or <code>null</code> if the report never ran on the site
     */
    public Report getReport(String siteKey) throws RepositoryException, SchedulerException {
        Report report = ReportStore.read(siteKey);
        // A restart of the processing node ends a run without storing its end, and the scheduler then holds no job.
        if (report != null && report.getStatus() == ReportStatus.RUNNING && !isRunning()) {
            return new Report(ReportStatus.FAILED, report.getWorkspace(), report.getStarted(), null, report.getMaxItems(),
                    report.getItemCount(), report.getItems());
        }
        return report;
    }

    /**
     * @return <code>true</code> if a report runs on any site of the platform
     */
    public boolean isRunning() throws SchedulerException {
        // getAllActiveJobs(String) reads a trigger group, and the instant trigger of a job is in the group "instant".
        String group = BackgroundJob.getGroupName(ReportJob.class);
        return scheduler().getAllActiveJobs().stream().anyMatch(job -> group.equals(job.getGroup()));
    }

    /**
     * Schedules a run of the report of the site, unless a report already runs on the platform.
     *
     * @param workspace the name of the workspace to scan, <code>default</code> or <code>live</code>
     * @return <code>false</code> if a report already runs
     */
    public synchronized boolean start(String siteKey, String workspace) throws RepositoryException, SchedulerException {
        if (isRunning()) {
            return false;
        }
        ReportStore.start(siteKey, workspace, config.maxItems());
        JobDetail job = BackgroundJob.createJahiaJob("HTML filtering report of " + siteKey, ReportJob.class);
        job.getJobDataMap().put(ReportJob.SITE_KEY, siteKey);
        job.getJobDataMap().put(ReportJob.WORKSPACE, workspace);
        scheduler().scheduleJobNow(job);
        return true;
    }

    /**
     * Scans the site and stores the result. The job calls it on the processing node.
     */
    void run(String siteKey, String workspace) throws RepositoryException, InterruptedException {
        Config current = config;
        long start = System.currentTimeMillis();
        logger.info("Starting the HTML filtering report of the site {} in the workspace {}", siteKey, workspace);
        try {
            Policy policy = policyResolver.resolvePolicy(siteKey, workspace);
            ReportScanner scanner = new ReportScanner(policy, current.maxItems(), current.batchSize(), current.pauseMillis());
            if (policy != null) {
                scanner.scan(siteKey, workspace, itemCount -> ReportStore.progress(siteKey, itemCount));
            }
            ReportStore.finish(siteKey, scanner.isLimitReached() ? ReportStatus.LIMIT_REACHED : ReportStatus.DONE, scanner.getItems());
            logger.info("The HTML filtering report of the site {} found {} items in {} ms", siteKey, scanner.getItems().size(),
                    System.currentTimeMillis() - start);
        } catch (RepositoryException | RuntimeException | InterruptedException e) {
            ReportStore.finish(siteKey, ReportStatus.FAILED, Collections.emptyList());
            throw e;
        }
    }

    private static SchedulerService scheduler() {
        return ServicesRegistry.getInstance().getSchedulerService();
    }
}
