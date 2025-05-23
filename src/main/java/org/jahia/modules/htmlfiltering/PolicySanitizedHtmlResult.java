package org.jahia.modules.htmlfiltering;

import java.util.Map;
import java.util.Set;

/**
 * This represents the result of a sanitized HTML.
 * It contains the sanitized HTML and information about any rejected elements.
 */
public interface PolicySanitizedHtmlResult {

    /**
     * @return <code>true</code> if no tags, attributes were rejected during sanitization process. <code>false</code> otherwise.
     */
    boolean isValid();

    /**
     * @return - The sanitized HTML
     */
    String getSanitizedHtml();

    /**
     * Retrieves the set of tags that were rejected during the sanitization process.
     *
     * @return a set of strings representing the names of the rejected tags
     */
    Set<String> getRejectedTags();

    /**
     * Retrieves a mapping of HTML tags to their corresponding sets of rejected attributes.
     * Each entry in the map represents an HTML tag as the key and a set of attribute names
     * that were rejected during validation for that tag as the value.
     * <p><strong>Note:</strong> if the same tag name has been rejected multiple times, the attributes will be merged</p>
     *
     * @return a map where the keys are the names of HTML tags (as strings) and the values
     * are sets of strings representing the rejected attribute names for each tag.
     */
    Map<String, Set<String>> getRejectedAttributesByTag();
}
