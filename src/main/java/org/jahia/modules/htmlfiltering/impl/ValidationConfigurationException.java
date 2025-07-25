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

import org.jahia.modules.htmlfiltering.model.ConfigModel;
import org.osgi.service.cm.ConfigurationException;

import javax.validation.ConstraintViolation;
import java.util.Set;

public class ValidationConfigurationException extends ConfigurationException {
    private final Set<ConstraintViolation<ConfigModel>> violations;

    public ValidationConfigurationException(Set<ConstraintViolation<ConfigModel>> violations) {
        super(null, "Invalid configuration: " + buildErrorMessage(violations));
        this.violations = violations;
    }

    private static String buildErrorMessage(Set<ConstraintViolation<ConfigModel>> violations) {
        StringBuilder errorMessages = new StringBuilder("\n");
        for (ConstraintViolation<ConfigModel> violation : violations) {
            errorMessages.append(" - ").append(violation.getPropertyPath())
                    .append(": ").append(violation.getMessage()).append("\n");
        }
        return errorMessages.toString();
    }

    public Set<ConstraintViolation<ConfigModel>> getViolations() {
        return violations;
    }
}
