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
 */
public interface ValidationResult {
    /**
     * Returns <code>true</code> if the validation result is valid, i.e., no rejected tags or attributes were found.
     *
     * @return <code>true</code> if the validation result is valid, <code>false</code> otherwise.
     */
    boolean isValid();

    /**
     * Retrieves a map containing information about validation errors for specific properties.
     * The map keys represent property names, and the values are instances of
     * {@link PropertyRejectionResult}, providing details about rejected tags and attributes
     * for the specific properties.
     *
     * @return a map where the keys are property names and the values are {@link PropertyRejectionResult}
     * objects containing validation rejection details for the respective properties
     */
    Map<String, PropertyRejectionResult> getRejectionResultsByProperty();

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
     *   <li>Rejected attributes ({@link #getRejectedAttributesByTag()})</li>
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
}
