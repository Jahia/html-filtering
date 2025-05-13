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
 * Represents the result of a validation process, encapsulating information about validation of properties, rejected tags, and rejected attributes.
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
     * Retrieves a set of property validation results, where each entry consists of a property name as the key and its corresponding validation result as the value.
     *
     * @return a set of map entries representing property validation results, with each entry associating a property name (key)
     * with its corresponding {@link ValidationResult.PropertyValidationResult} object (value)
     */
    Set<Map.Entry<String, ValidationResult.PropertyValidationResult>> propertyValidationResultSet();

    /**
     * Represents the result of the validation process for a single property.
     */
    interface PropertyValidationResult {
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
