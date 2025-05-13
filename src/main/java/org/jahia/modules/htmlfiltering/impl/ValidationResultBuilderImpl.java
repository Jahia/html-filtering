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
import org.jahia.modules.htmlfiltering.ValidationResultBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

class ValidationResultBuilderImpl implements ValidationResultBuilder {

    final Map<String, ValidationResult.PropertyValidationResult> propertyValidationResults = new HashMap<>();

    @Override
    public void rejectTag(String propertyName, String tag) {
        ValidationResult.PropertyValidationResult result = propertyValidationResults.computeIfAbsent(propertyName, k -> new ValidationResultImpl.PropertyValidationResultImpl());
        ((ValidationResultImpl.PropertyValidationResultImpl) result).addRejectedTag(tag);
    }

    @Override
    public void rejectAttributes(String propertyName, String tag, String[] attributeNames) {
        ValidationResult.PropertyValidationResult result = propertyValidationResults.computeIfAbsent(propertyName, k -> new ValidationResultImpl.PropertyValidationResultImpl());
        ((ValidationResultImpl.PropertyValidationResultImpl) result).addRejectedAttributeByTag(tag, new HashSet<>(Arrays.asList(attributeNames)));
    }

    @Override
    public ValidationResult build() {
        return new ValidationResultImpl(this);
    }
}
