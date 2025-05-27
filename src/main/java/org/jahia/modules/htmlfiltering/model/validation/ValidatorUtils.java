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
package org.jahia.modules.htmlfiltering.model.validation;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintValidatorContext;
import javax.validation.ValidationException;

final class ValidatorUtils {
    private static final Logger logger = LoggerFactory.getLogger(ValidatorUtils.class);

    /**
     * Adds a message parameter to the provided {@link ConstraintValidatorContext} using Hibernate-specific APIs (as the JSR 303 has no support for message parameterization).
     * If Hibernate Validator is not used, the method logs an error and no parameter is added.
     *
     * @param context the {@link ConstraintValidatorContext} instance to which the message parameter will be added
     * @param name    the name of the message parameter
     * @param value   the value of the message parameter
     */
    static void addMessageParameter(ConstraintValidatorContext context, String name, String value) {
        HibernateConstraintValidatorContext hibernateContext;
        try {
            hibernateContext = context.unwrap(HibernateConstraintValidatorContext.class);
        } catch (ValidationException e) {
            logger.debug("Unable to unwrap context to HibernateConstraintValidatorContext, no parameter will be added", e);
            return;
        }
        hibernateContext.addMessageParameter(name, value);
    }
}
