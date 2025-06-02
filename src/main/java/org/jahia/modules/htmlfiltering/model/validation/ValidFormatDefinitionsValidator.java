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

import org.apache.commons.lang3.StringUtils;
import org.jahia.modules.htmlfiltering.model.validation.constraints.ValidFormatDefinitions;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ValidFormatDefinitionsValidator implements ConstraintValidator<ValidFormatDefinitions, Map<String, String>> {

    @Override
    public void initialize(ValidFormatDefinitions constraintAnnotation) {
        // Do nothing
    }

    @Override
    public boolean isValid(Map<String, String> map, ConstraintValidatorContext context) {
        if (map == null) {
            return true;
        }

        for (Map.Entry<String, String> entry : map.entrySet()) {
            String regex = entry.getValue();
            if (StringUtils.isBlank(regex)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(String.format("the value for the format definition of '%s' must not be blank", entry.getKey()))
                        .addConstraintViolation();
                return false;
            }

            try {
                Pattern.compile(regex);
            } catch (PatternSyntaxException e) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        String.format("the value for the format definition of '%s' must be a valid regular expression", entry.getKey())).addConstraintViolation();
                return false;
            }
        }

        return true;
    }
}
