/*
 * Copyright (C) 2002-2025 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.modules.htmlfiltering;

import java.util.Map;
import java.util.Set;

/**
 * Represents the result of a validation process, encapsulating information about the properties being rejected (with their rejected tags and/or rejected attributes).
 * It also contains a map of sanitized properties, exposed via the {@link #getSanitizedProperties()} method, where the keys represent property names.
 * <p>
 * Implementations of this interface should be immutable.
 */
public interface ValidationResult {
    /**
     * Returns <code>true</code> if the validation result is valid, i.e., no rejected tags or attributes were found.
     *
     * @return <code>true</code> if the validation result is valid, <code>false</code> otherwise.
     */
    boolean isValid();

    /**
     * Retrieves a set of property rejection results, where each entry consists of a property name as the key and its corresponding property rejection result as the value.
     * If the validation result is valid, the set is empty.
     *
     * @return a set of map entries representing property rejection results, with each entry associating a property name (key)
     * with its corresponding {@link PropertyRejectionResult} object (value)
     */
    Set<Map.Entry<String, PropertyRejectionResult>> rejectionResultsEntrySet();

    /**
     * Retrieves the set of property names that have been rejected during the validation process.
     *
     * @return a set of strings representing the names of the rejected properties.
     */
    Set<String> rejectedProperties();

    /**
     * Retrieves the rejection result for a specific property, if available.
     *
     * @param property the name of the property for which the rejection result is to be retrieved
     * @return the {@link PropertyRejectionResult} containing details about rejected tags and attributes for the given property,
     * or <code>null</code> if no rejection result exists for the specified property
     */
    PropertyRejectionResult getRejectionResult(String property);

    /**
     * Retrieves a map of sanitized properties where the keys represent property names
     * and the values represent the sanitized content of those properties.
     * The map is populated regardless of the validity of the validation result.
     *
     * @return a map containing sanitized properties with property names as keys and their sanitized values as values
     */
    Map<String, String> getSanitizedProperties();

    /**
     * Contains information about elements that failed validation for a specific property.
     * This interface tracks two types of rejected elements:
     * <ul>
     *   <li>Rejected HTML tags not allowed in the content ({@link #getRejectedTags()})</li>
     *   <li>Rejected attributes ({@link #getRejectedAttributesByTagEntrySet()})</li>
     * </ul>
     * The interface provides methods to access details about these rejected elements to help identify
     * validation failures.
     */
    interface PropertyRejectionResult {
        /**
         * Retrieves the set of tags that were rejected during the validation process.
         *
         * @return a set of strings representing the names of the rejected tags
         */
        Set<String> getRejectedTags();

        /**
         * Retrieves a set of entries representing rejected attributes grouped by their respective tags.
         * Each entry in the set consists of a tag name as the key and a set of attributes rejected for that tag as the value.
         *
         * @return a set of map entries where each entry associates a tag name (key) with a set of rejected attributes (value)
         */
        Set<Map.Entry<String, Set<String>>> getRejectedAttributesByTagEntrySet();
    }
}
