/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2025 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.htmlfiltering;


import java.util.*;

public class ValidationResult {

    private final Map<String, PropertyValidationResult> propertyValidationResults;

    public ValidationResult(ValidationResultBuilder validationResultBuilder) {
        propertyValidationResults = validationResultBuilder.propertyValidationResults;
    }

    public boolean isValid() {
        return propertyValidationResults.isEmpty();
    }

    public Set<Map.Entry<String, PropertyValidationResult>> propertyValidationResultSet() {
        return propertyValidationResults.entrySet();
    }

    public static class PropertyValidationResult {
        private final Set<String> rejectedTags = new HashSet<>();
        private final Map<String, Set<String>> rejectedAttributesByTag = new HashMap<>();

        public void addRejectedTag(String tag) {
            rejectedTags.add(tag);
        }

        public Set<String> getRejectedTags() {
            return Collections.unmodifiableSet(rejectedTags);
        }

        public void addRejectedAttributeByTag(String tag, Set<String> attributes) {
            // TODO should we overwrite? how does it work when using multiple times the same tag with different attributes that are rejected?
            rejectedAttributesByTag.putIfAbsent(tag, attributes);
        }

        public Set<Map.Entry<String, Set<String>>> getRejectedAttributesByTagEntrySet() {
            return rejectedAttributesByTag.entrySet();
        }

        public boolean isValid() {
            return rejectedTags.isEmpty() && rejectedAttributesByTag.isEmpty();
        }
    }

    public static class ValidationResultBuilder {

        private final Map<String, PropertyValidationResult> propertyValidationResults = new HashMap<>();

        public void addPropertyValidationResult(String propertyName, PropertyValidationResult propertyValidationResult) {
            if (!propertyValidationResult.isValid()) {
                propertyValidationResults.put(propertyName, propertyValidationResult);

            }
        }

        public ValidationResult build() {
            return new ValidationResult(this);
        }
    }
}
