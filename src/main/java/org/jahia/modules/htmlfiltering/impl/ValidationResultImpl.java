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

    private final Map<String, PropertyRejectionResult> rejectionResultsByProperty;
    private final Map<String, String> sanitizedProperties;

    ValidationResultImpl(ValidationResultBuilderImpl validationResultBuilder) {
        rejectionResultsByProperty = validationResultBuilder.rejectionResultsByProperty;
        sanitizedProperties = validationResultBuilder.sanitizedProperties;
    }

    @Override
    public boolean isValid() {
        return rejectionResultsByProperty.isEmpty();
    }

    @Override
    public Set<Map.Entry<String, PropertyRejectionResult>> rejectionResultsEntrySet() {
        return rejectionResultsByProperty.entrySet();
    }

    @Override
    public Set<String> rejectedProperties() {
        return rejectionResultsByProperty.keySet();
    }

    @Override
    public PropertyRejectionResult getRejectionResult(String property) {
        return rejectionResultsByProperty.get(property);
    }

    @Override
    public Map<String, String> getSanitizedProperties() {
        return sanitizedProperties;
    }

    static class PropertyRejectionResultImpl implements PropertyRejectionResult {
        private final Set<String> rejectedTags = new HashSet<>();
        private final Map<String, Set<String>> rejectedAttributesByTag = new HashMap<>();

        void addRejectedTag(String tag) {
            rejectedTags.add(tag);
        }

        @Override
        public Set<String> getRejectedTags() {
            return Collections.unmodifiableSet(rejectedTags);
        }

        void addRejectedAttributeByTag(String tag, Set<String> attributes) {
            // TODO should we overwrite? how does it work when using multiple times the same tag with different attributes that are rejected?
            rejectedAttributesByTag.putIfAbsent(tag, attributes);
        }

        @Override
        public Set<Map.Entry<String, Set<String>>> getRejectedAttributesByTagEntrySet() {
            return rejectedAttributesByTag.entrySet();
        }
    }

}
