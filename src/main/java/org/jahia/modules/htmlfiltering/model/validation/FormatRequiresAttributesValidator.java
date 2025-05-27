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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jahia.modules.htmlfiltering.model.ElementModel;
import org.jahia.modules.htmlfiltering.model.validation.constraints.FormatRequiresAttributes;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class FormatRequiresAttributesValidator implements ConstraintValidator<FormatRequiresAttributes, ElementModel> {
    @Override
    public void initialize(FormatRequiresAttributes constraintAnnotation) {
        // Do nothing
    }

    @Override
    public boolean isValid(ElementModel element, ConstraintValidatorContext constraint) {
        // if a format is specified, attributes must be provided
        return element == null || StringUtils.isBlank(element.getFormat()) || CollectionUtils.isNotEmpty(element.getAttributes());
    }

}
