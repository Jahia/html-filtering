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
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

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

    private static final Set<String> PENDING =
            Set.of(BackgroundJob.STATUS_ADDED, BackgroundJob.STATUS_SCHEDULED, BackgroundJob.STATUS_EXECUTING);

    /**
     * The time another node of the cluster takes to read what the processing node stored, with a margin.
     */
    private static final long SYNC_DELAY_MILLIS = 60_000;

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
        Optional<JobDetail> pending = findPendingJob(siteKey);
        if (pending.isPresent()) {
            return running(report, pending.get());
        }
        Optional<JobDetail> last = findLastJob(siteKey);
        if (last.isPresent() && !holdsEndOf(report, last.get())) {
            // The processing node stores the end of a run, and another node of the cluster reads it a few seconds later.
            if (System.currentTimeMillis() - longValue(last.get(), BackgroundJob.JOB_END) < SYNC_DELAY_MILLIS) {
                return running(report, last.get());
            }
            return failed(report, last.get());
        }
        // A restart of the processing node ends a run without storing its end.
        if (report != null && report.getStatus() == ReportStatus.RUNNING) {
            return new Report(ReportStatus.FAILED, report.getWorkspace(), report.getStarted(), null, report.getMaxItems(),
                    report.getItemCount(), Collections.emptyList());
        }
        return report;
    }

    /**
     * @return the stored report if it is the run of the job, or else a run of the job that has not stored its start yet
     */
    private Report running(Report report, JobDetail job) {
        if (report != null && report.getStatus() == ReportStatus.RUNNING && isRunOf(report, job)) {
            return report;
        }
        return new Report(ReportStatus.RUNNING, job.getJobDataMap().getString(ReportJob.WORKSPACE), created(job), null,
                config.maxItems(), 0, Collections.emptyList());
    }

    private Report failed(Report report, JobDetail job) {
        boolean ownRun = report != null && isRunOf(report, job);
        return new Report(ReportStatus.FAILED, job.getJobDataMap().getString(ReportJob.WORKSPACE),
                ownRun ? report.getStarted() : created(job), null, ownRun ? report.getMaxItems() : config.maxItems(),
                ownRun ? report.getItemCount() : 0, Collections.emptyList());
    }

    private static boolean holdsEndOf(Report report, JobDetail job) {
        return report != null && report.getStatus() != ReportStatus.RUNNING && isRunOf(report, job);
    }

    /**
     * The job stores the start of its run after the job is created, so an older start belongs to a previous run.
     */
    private static boolean isRunOf(Report report, JobDetail job) {
        return !report.getStarted().before(created(job));
    }

    private static Calendar created(JobDetail job) {
        Calendar created = Calendar.getInstance();
        Object value = job.getJobDataMap().get(BackgroundJob.JOB_CREATED);
        if (value instanceof Date) {
            created.setTime((Date) value);
        } else {
            created.setTimeInMillis(0);
        }
        return created;
    }

    private static long longValue(JobDetail job, String key) {
        Object value = job.getJobDataMap().get(key);
        try {
            return value == null ? 0 : Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * @return <code>true</code> if a report runs, or waits for the processing node, on any site of the platform
     */
    public boolean isRunning() throws SchedulerException {
        return findPendingJob(null).isPresent();
    }

    /**
     * Schedules a run of the report of the site, unless a report already runs on the platform. The job stores the
     * report, on the processing node.
     *
     * @param workspace the name of the workspace to scan, <code>default</code> or <code>live</code>
     * @return <code>false</code> if a report already runs
     */
    public synchronized boolean start(String siteKey, String workspace) throws SchedulerException {
        if (isRunning()) {
            return false;
        }
        JobDetail job = BackgroundJob.createJahiaJob("HTML filtering report of " + siteKey, ReportJob.class);
        job.getJobDataMap().put(ReportJob.SITE_KEY, siteKey);
        job.getJobDataMap().put(ReportJob.WORKSPACE, workspace);
        scheduler().scheduleJobNow(job);
        return true;
    }

    /**
     * Reads the job store, which every node of a cluster shares. A job counts until its trigger is gone: it waits for
     * the processing node with the status "added", then runs with the status "executing".
     *
     * @param siteKey the site of the job, or <code>null</code> for any site
     */
    private static Optional<JobDetail> findPendingJob(String siteKey) throws SchedulerException {
        String group = BackgroundJob.getGroupName(ReportJob.class);
        Scheduler scheduler = scheduler().getScheduler();
        for (String triggerGroup : scheduler.getTriggerGroupNames()) {
            for (String triggerName : scheduler.getTriggerNames(triggerGroup)) {
                Trigger trigger = scheduler.getTrigger(triggerName, triggerGroup);
                if (trigger == null || !group.equals(trigger.getJobGroup())) {
                    continue;
                }
                JobDetail job = scheduler.getJobDetail(trigger.getJobName(), trigger.getJobGroup());
                if (job != null && PENDING.contains(job.getJobDataMap().getString(BackgroundJob.JOB_STATUS))
                        && (siteKey == null || siteKey.equals(job.getJobDataMap().getString(ReportJob.SITE_KEY)))) {
                    return Optional.of(job);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * @return the job of the last run of the report of the site, which the job store keeps once the run ends
     */
    private static Optional<JobDetail> findLastJob(String siteKey) throws SchedulerException {
        return scheduler().getAllJobs(BackgroundJob.getGroupName(ReportJob.class)).stream()
                .filter(job -> job != null && siteKey.equals(job.getJobDataMap().getString(ReportJob.SITE_KEY)))
                .max(Comparator.comparing(ReportService::created));
    }

    /**
     * Scans the site and stores the result. The job calls it on the processing node.
     */
    void run(String siteKey, String workspace) throws RepositoryException, InterruptedException {
        Config current = config;
        long start = System.currentTimeMillis();
        logger.info("Starting the HTML filtering report of the site {} in the workspace {}", siteKey, workspace);
        ReportStore.start(siteKey, workspace, current.maxItems());
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
            logger.error("The HTML filtering report of the site {} failed on the workspace {}: {}", siteKey, workspace, e.toString());
            ReportStore.finish(siteKey, ReportStatus.FAILED, Collections.emptyList());
            throw e;
        }
    }

    private static SchedulerService scheduler() {
        return ServicesRegistry.getInstance().getSchedulerService();
    }
}
