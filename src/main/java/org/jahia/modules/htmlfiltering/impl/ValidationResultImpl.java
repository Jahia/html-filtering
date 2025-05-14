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
package org.jahia.modules.htmlfiltering.impl;


import org.jahia.modules.htmlfiltering.ValidationResult;

import java.util.*;


final class ValidationResultImpl implements ValidationResult {

    private final Map<String, PropertyRejectionResult> rejectionResultsByProperty = new HashMap<>();
    private final Map<String, String> sanitizedProperties = new HashMap<>();

    @Override
    public boolean isValid() {
        return rejectionResultsByProperty.isEmpty();
    }

    @Override
    public Map<String, PropertyRejectionResult> getRejectionResultsByProperty() {
        return rejectionResultsByProperty;
    }

    @Override
    public Map<String, String> getSanitizedProperties() {
        return sanitizedProperties;
    }

    void rejectTag(String propertyName, String tag) {
        ValidationResult.PropertyRejectionResult result = rejectionResultsByProperty.computeIfAbsent(propertyName, k -> new ValidationResultImpl.PropertyRejectionResultImpl());
        ((ValidationResultImpl.PropertyRejectionResultImpl) result).addRejectedTag(tag);
    }

    void rejectAttributes(String propertyName, String tag, String[] attributeNames) {
        ValidationResult.PropertyRejectionResult result = rejectionResultsByProperty.computeIfAbsent(propertyName, k -> new ValidationResultImpl.PropertyRejectionResultImpl());
        ((ValidationResultImpl.PropertyRejectionResultImpl) result).addRejectedAttributeByTag(tag, new HashSet<>(Arrays.asList(attributeNames)));
    }

    void addSanitizedProperty(String propertyName, String sanitizedValue) {
        sanitizedProperties.put(propertyName, sanitizedValue);
    }

    static class PropertyRejectionResultImpl implements PropertyRejectionResult {
        private final Set<String> rejectedTags = new HashSet<>();
        private final Map<String, Set<String>> rejectedAttributesByTag = new HashMap<>();

        void addRejectedTag(String tag) {
            rejectedTags.add(tag);
        }

        @Override
        public Set<String> getRejectedTags() {
            return rejectedTags;
        }

        void addRejectedAttributeByTag(String tag, Set<String> attributes) {
            // merge the attributes with the existing ones for that tag (if any)
            rejectedAttributesByTag.compute(tag, (k, existingAttributes) -> {
                if (existingAttributes == null) {
                    return attributes;
                } else {
                    existingAttributes.addAll(attributes);
                    return existingAttributes;
                }
            });
        }

        @Override
        public Map<String, Set<String>> getRejectedAttributesByTag() {
            return rejectedAttributesByTag;
        }
    }

}
