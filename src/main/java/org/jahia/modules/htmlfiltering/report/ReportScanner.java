package org.jahia.modules.htmlfiltering.report;

import org.jahia.modules.htmlfiltering.Policy;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.services.content.nodetypes.SelectorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Scans the content of a site for the rich text values that do not pass the HTML filtering policy of the site.
 * <p>
 * The scan queries only the node types that declare a rich text property, a batch at a time, and it releases the
 * session cache and pauses between two batches.
 */
final class ReportScanner {

    private static final Logger logger = LoggerFactory.getLogger(ReportScanner.class);

    /**
     * Receives the number of items found after each batch.
     */
    interface ProgressListener {
        void onProgress(int itemCount) throws RepositoryException;
    }

    private final Policy policy;
    private final int batchSize;
    private final long pauseMillis;
    private final ItemCollector collector;

    ReportScanner(Policy policy, int maxItems, int batchSize, long pauseMillis) {
        this.policy = policy;
        this.batchSize = batchSize;
        this.pauseMillis = pauseMillis;
        this.collector = new ItemCollector(maxItems);
    }

    /**
     * @return the items, up to the maximum number of items
     */
    List<ReportItem> getItems() {
        return collector.getItems();
    }

    /**
     * @return <code>true</code> if the scan stopped at the maximum number of items
     */
    boolean isLimitReached() {
        return collector.isFull();
    }

    void scan(String siteKey, String workspace, ProgressListener listener) throws RepositoryException, InterruptedException {
        JCRSessionFactory sessionFactory = JCRSessionFactory.getInstance();
        JCRSessionWrapper session = sessionFactory.getCurrentSystemSession(workspace, null, null);
        String sitePath = "/sites/" + siteKey;
        if (!session.nodeExists(sitePath)) {
            logger.info("The site {} does not exist in the workspace {}, the HTML filtering report finds no item", siteKey, workspace);
            return;
        }
        List<Locale> locales = ((JCRSiteNode) session.getNode(sitePath)).getLanguagesAsLocales();
        // A session without a fallback language reads only the values stored in its own language.
        Map<Locale, JCRSessionWrapper> localizedSessions = new LinkedHashMap<>();
        for (Locale locale : locales) {
            localizedSessions.put(locale, sessionFactory.getCurrentSystemSession(workspace, locale, null));
        }

        for (Map.Entry<String, List<String>> entry : richTextPropertiesByDeclaringType().entrySet()) {
            String nodeType = entry.getKey();
            String sql = "SELECT * FROM [" + nodeType + "] AS n WHERE ISDESCENDANTNODE(n, '" + sitePath + "')";
            // One query per type: the result loads the nodes lazily, while a query per batch with an offset would read
            // again every node before the offset.
            Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            JCRNodeIteratorWrapper nodes = (JCRNodeIteratorWrapper) query.execute().getNodes();
            int batchCount = 0;
            while (nodes.hasNext() && !collector.isFull()) {
                JCRNodeWrapper node = (JCRNodeWrapper) nodes.nextNode();
                for (String propertyName : entry.getValue()) {
                    checkProperty(node, nodeType, propertyName, localizedSessions);
                }
                if (++batchCount == batchSize) {
                    batchCount = 0;
                    endBatch(session, localizedSessions, listener);
                    Thread.sleep(pauseMillis);
                }
            }
            endBatch(session, localizedSessions, listener);
            if (collector.isFull()) {
                return;
            }
        }
    }

    /**
     * Reports the progress, and releases the session caches, so that the heap does not grow with the size of the site.
     */
    private void endBatch(JCRSessionWrapper session, Map<Locale, JCRSessionWrapper> localizedSessions, ProgressListener listener)
            throws RepositoryException {
        listener.onProgress(collector.getItems().size());
        session.refresh(false);
        for (JCRSessionWrapper localizedSession : localizedSessions.values()) {
            localizedSession.refresh(false);
        }
    }

    private void checkProperty(JCRNodeWrapper node, String nodeType, String propertyName,
                               Map<Locale, JCRSessionWrapper> localizedSessions) throws RepositoryException {
        ExtendedPropertyDefinition definition = node.getApplicablePropertyDefinition(propertyName);
        // Another type of the node can redefine the property, so only the type that holds the definition in use checks it.
        if (definition == null || !nodeType.equals(definition.getDeclaringNodeType().getName())
                || !policy.isApplicableToPropertyIgnoringPermissions(node, propertyName, definition)) {
            return;
        }
        if (!definition.isInternationalized()) {
            checkValues(node, propertyName, null);
            return;
        }
        for (Map.Entry<Locale, JCRSessionWrapper> entry : localizedSessions.entrySet()) {
            if (collector.isFull()) {
                return;
            }
            if (node.hasI18N(entry.getKey(), false)) {
                checkValues(entry.getValue().getNodeByIdentifier(node.getIdentifier()), propertyName, entry.getKey().toString());
            }
        }
    }

    private void checkValues(JCRNodeWrapper node, String propertyName, String language) throws RepositoryException {
        if (!node.hasProperty(propertyName)) {
            return;
        }
        JCRPropertyWrapper property = node.getProperty(propertyName);
        Value[] values = property.isMultiple() ? property.getValues() : new Value[]{property.getValue()};
        // The values of a multiple property give one item, with the markups of every value.
        Set<String> markups = new LinkedHashSet<>();
        for (Value value : values) {
            markups.addAll(ItemCollector.markupsOf(policy.sanitize(value.getString())));
        }
        collector.add(node.getPath(), propertyName, language, new ArrayList<>(markups));
    }

    /**
     * @return the names of the rich text properties of each node type that declares one, by node type name
     */
    static Map<String, List<String>> richTextPropertiesByDeclaringType() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (ExtendedNodeType nodeType : NodeTypeRegistry.getInstance().getAllNodeTypes()) {
            List<String> names = new ArrayList<>();
            for (ExtendedPropertyDefinition definition : nodeType.getDeclaredPropertyDefinitions()) {
                if (definition.getRequiredType() == PropertyType.STRING && definition.getSelector() == SelectorType.RICHTEXT
                        && !"*".equals(definition.getName())) {
                    names.add(definition.getName());
                }
            }
            if (!names.isEmpty()) {
                result.put(nodeType.getName(), names);
            }
        }
        return result;
    }
}
