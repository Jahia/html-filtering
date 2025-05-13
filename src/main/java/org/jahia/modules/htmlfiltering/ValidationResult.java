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


import java.util.*;

/**
 * Represents the result of a validation process, encapsulating information about validation of properties,
 * rejected tags, and rejected attributes.
 * <p>
 * This class is immutable and contains the outcomes of validations performed on properties and their associated
 * tags or attributes.
 */
public final class ValidationResult {

    private final Map<String, PropertyValidationResult> propertyValidationResults;

    private ValidationResult(ValidationResultBuilder validationResultBuilder) {
        propertyValidationResults = validationResultBuilder.propertyValidationResults;
    }

    /**
     * Returns <code>true</code> if the validation result is valid, i.e. no rejected tags or attributes were found.
     *
     * @return <code>true</code> if the validation result is valid, i.e. no rejected tags or attributes were found.
     */
    public boolean isValid() {
        return propertyValidationResults.isEmpty();
    }

    public Set<Map.Entry<String, PropertyValidationResult>> propertyValidationResultSet() {
        return propertyValidationResults.entrySet();
    }

    public static class PropertyValidationResult {
        private final Set<String> rejectedTags = new HashSet<>();
        private final Map<String, Set<String>> rejectedAttributesByTag = new HashMap<>();

        private void addRejectedTag(String tag) {
            rejectedTags.add(tag);
        }

        public Set<String> getRejectedTags() {
            return Collections.unmodifiableSet(rejectedTags);
        }

        private void addRejectedAttributeByTag(String tag, Set<String> attributes) {
            // TODO should we overwrite? how does it work when using multiple times the same tag with different attributes that are rejected?
            rejectedAttributesByTag.putIfAbsent(tag, attributes);
        }

        public Set<Map.Entry<String, Set<String>>> getRejectedAttributesByTagEntrySet() {
            return rejectedAttributesByTag.entrySet();
        }
    }

    public static class ValidationResultBuilder {

        private final Map<String, PropertyValidationResult> propertyValidationResults = new HashMap<>();

        public void rejectTag(String propertyName, String tag) {
            propertyValidationResults.getOrDefault(propertyName, new PropertyValidationResult()).addRejectedTag(tag);
        }

        public void rejectAttributes(String propertyName, String tag, String[] attributeNames) {
            propertyValidationResults.getOrDefault(propertyName, new PropertyValidationResult()).addRejectedAttributeByTag(tag, new HashSet<>(Arrays.asList(attributeNames)));
        }

        public ValidationResult build() {
            return new ValidationResult(this);
        }
    }
}
