package org.jahia.modules.htmlfiltering;

import java.util.Map;
import java.util.Set;

/**
 * Contains information about elements that failed validation.
 * This interface tracks two types of rejected elements:
 * <ul>
 *   <li>Rejected HTML tags not allowed in the content ({@link #getRejectedTags()})</li>
 *   <li>Rejected attributes ({@link #getRejectedAttributesByTag()})</li>
 * </ul>
 * The interface provides methods to access details about these rejected elements to help identify
 * validation failures.
 */
public interface PolicyExecutionResult {

    /**
     * Returns <code>true</code> if the validation result is valid, i.e., no rejected tags or attributes were found.
     *
     * @return <code>true</code> if the validation result is valid, <code>false</code> otherwise.
     */
    boolean isValid();

    /**
     * Returns the sanitized HTML.
     *
     * @return - The sanitized HTML
     */
    String getSanitizedHtml();

    /**
     * Retrieves the set of tags that were rejected during the validation process.
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
