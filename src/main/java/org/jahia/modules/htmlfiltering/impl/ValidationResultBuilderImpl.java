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
