package org.jahia.modules.htmlfiltering.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jahia.api.Constants;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;

import javax.jcr.RepositoryException;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * Stores the last run of the report of each site, in the default workspace under {@value #REPORTS_PATH}.
 * Every node of a cluster reads the same result, whatever node ran the report.
 */
final class ReportStore {

    static final String REPORTS_PATH = "/settings/htmlFilteringReports";

    private static final String REPORTS_TYPE = "jnt:htmlFilteringReports";
    private static final String REPORT_TYPE = "jnt:htmlFilteringReport";
    private static final String STATUS = "status";
    private static final String WORKSPACE = "workspace";
    private static final String STARTED = "started";
    private static final String FINISHED = "finished";
    private static final String MAX_ITEMS = "maxItems";
    private static final String ITEM_COUNT = "itemCount";
    private static final String ITEMS = "items";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<ReportItem>> ITEM_LIST = new TypeReference<>() {
    };

    private ReportStore() {
    }

    /**
     * @return the last run of the report of the site, or <code>null</code> if the report never ran on the site
     */
    static Report read(String siteKey) throws RepositoryException {
        JCRSessionWrapper session = session();
        String path = REPORTS_PATH + "/" + siteKey;
        if (!session.nodeExists(path)) {
            return null;
        }
        JCRNodeWrapper node = session.getNode(path);
        return new Report(
                ReportStatus.valueOf(node.getProperty(STATUS).getString()),
                node.getProperty(WORKSPACE).getString(),
                node.getProperty(STARTED).getDate(),
                node.hasProperty(FINISHED) ? node.getProperty(FINISHED).getDate() : null,
                node.getProperty(MAX_ITEMS).getLong(),
                node.getProperty(ITEM_COUNT).getLong(),
                node.hasProperty(ITEMS) ? parse(node.getProperty(ITEMS).getString()) : Collections.emptyList());
    }

    /**
     * Replaces the last run of the report of the site with a run that starts.
     */
    static void start(String siteKey, String workspace, int maxItems) throws RepositoryException {
        JCRSessionWrapper session = session();
        if (!session.nodeExists(REPORTS_PATH)) {
            session.getNode("/settings").addNode("htmlFilteringReports", REPORTS_TYPE);
        }
        JCRNodeWrapper reports = session.getNode(REPORTS_PATH);
        if (reports.hasNode(siteKey)) {
            reports.getNode(siteKey).remove();
        }
        JCRNodeWrapper node = reports.addNode(siteKey, REPORT_TYPE);
        node.setProperty(STATUS, ReportStatus.RUNNING.name());
        node.setProperty(WORKSPACE, workspace);
        node.setProperty(STARTED, Calendar.getInstance());
        node.setProperty(MAX_ITEMS, maxItems);
        node.setProperty(ITEM_COUNT, 0L);
        session.save();
    }

    static void progress(String siteKey, int itemCount) throws RepositoryException {
        JCRSessionWrapper session = session();
        session.getNode(REPORTS_PATH + "/" + siteKey).setProperty(ITEM_COUNT, itemCount);
        session.save();
    }

    static void finish(String siteKey, ReportStatus status, List<ReportItem> items) throws RepositoryException {
        JCRSessionWrapper session = session();
        JCRNodeWrapper node = session.getNode(REPORTS_PATH + "/" + siteKey);
        node.setProperty(STATUS, status.name());
        node.setProperty(FINISHED, Calendar.getInstance());
        node.setProperty(ITEM_COUNT, items.size());
        try {
            node.setProperty(ITEMS, MAPPER.writeValueAsString(items));
        } catch (JsonProcessingException e) {
            throw new RepositoryException("Unable to write the items of the HTML filtering report of " + siteKey, e);
        }
        session.save();
    }

    private static List<ReportItem> parse(String json) throws RepositoryException {
        try {
            return MAPPER.readValue(json, ITEM_LIST);
        } catch (JsonProcessingException e) {
            throw new RepositoryException("Unable to read the items of an HTML filtering report", e);
        }
    }

    private static JCRSessionWrapper session() throws RepositoryException {
        return JCRSessionFactory.getInstance().getCurrentSystemSession(Constants.EDIT_WORKSPACE, null, null);
    }
}
