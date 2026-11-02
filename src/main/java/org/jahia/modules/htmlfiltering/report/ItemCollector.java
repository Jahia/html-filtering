package org.jahia.modules.htmlfiltering.report;

import org.jahia.modules.htmlfiltering.PolicySanitizedHtmlResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Collects the items of a run, up to the maximum number of items.
 */
final class ItemCollector {

    private final int maxItems;
    private final List<ReportItem> items = new ArrayList<>();

    ItemCollector(int maxItems) {
        this.maxItems = maxItems;
    }

    /**
     * Adds an item when the value holds markups that the policy rejects, until the collector is full.
     *
     * @param markups the rejected markups of the value, empty for a valid value
     */
    void add(String path, String property, String language, List<String> markups) {
        if (!isFull() && !markups.isEmpty()) {
            items.add(new ReportItem(path, property, language, markups));
        }
    }

    boolean isFull() {
        return items.size() >= maxItems;
    }

    List<ReportItem> getItems() {
        return items;
    }

    /**
     * @return each rejected tag, such as <code>&lt;script&gt;</code>, then each rejected attribute with its tag, such as
     * <code>&lt;img onerror&gt;</code>, each list sorted, or an empty list for a valid value
     */
    static List<String> markupsOf(PolicySanitizedHtmlResult result) {
        List<String> markups = new ArrayList<>();
        new TreeSet<>(result.getRejectedTags()).forEach(tag -> markups.add("<" + tag + ">"));
        Set<String> attributes = new TreeSet<>();
        for (Map.Entry<String, Set<String>> entry : result.getRejectedAttributesByTag().entrySet()) {
            entry.getValue().forEach(attribute -> attributes.add("<" + entry.getKey() + " " + attribute + ">"));
        }
        markups.addAll(attributes);
        return markups;
    }
}
