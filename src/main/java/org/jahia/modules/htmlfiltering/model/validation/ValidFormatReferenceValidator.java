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


import org.jahia.modules.htmlfiltering.model.ConfigModel;
import org.jahia.modules.htmlfiltering.model.ElementModel;
import org.jahia.modules.htmlfiltering.model.PolicyModel;
import org.jahia.modules.htmlfiltering.model.RuleSetModel;
import org.jahia.modules.htmlfiltering.model.validation.constraints.ValidFormatReference;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class ValidFormatReferenceValidator implements ConstraintValidator<ValidFormatReference, ConfigModel> {
    @Override
    public void initialize(ValidFormatReference constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(ConfigModel configModel, ConstraintValidatorContext context) {
        if (configModel == null) {
            return true; // Let @NotNull handle null values
        }

        Map<String, String> formatDefinitions = configModel.getFormatDefinitions();
        Set<String> formatNames = formatDefinitions == null ? Collections.emptySet() : formatDefinitions.keySet();

        boolean isValid = true;

        // Check both workspaces
        isValid &= validatePolicyModel(configModel.getEditWorkspace(), formatNames, context, "editWorkspace");
        isValid &= validatePolicyModel(configModel.getLiveWorkspace(), formatNames, context, "liveWorkspace");

        return isValid;
    }

    private boolean validatePolicyModel(PolicyModel model, Set<String> formatNames,
                                        ConstraintValidatorContext context, String path) {
        if (model == null) {
            return true;
        }

        boolean isValid = true;

        if (model.getAllowedRuleSet() != null) {
            isValid &= validateRuleSet(model.getAllowedRuleSet(), formatNames, context, path + ".allowedRuleSet");
        }

        if (model.getDisallowedRuleSet() != null) {
            isValid &= validateRuleSet(model.getDisallowedRuleSet(), formatNames, context, path + ".disallowedRuleSet");
        }

        return isValid;
    }

    private boolean validateRuleSet(RuleSetModel ruleSet, Set<String> formatNames,
                                    ConstraintValidatorContext context, String path) {
        if (ruleSet == null || ruleSet.getElements() == null) {
            return true;
        }

        boolean isValid = true;

        for (int i = 0; i < ruleSet.getElements().size(); i++) {
            ElementModel element = ruleSet.getElements().get(i);
            if (element != null && element.getFormat() != null && !formatNames.contains(element.getFormat())) {
                ValidatorUtils.addMessageParameter(context, "formatKey", element.getFormat());
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                                "{org.jahia.modules.htmlfiltering.model.validation.constraints.ValidFormatReference.undefined.message}")
                        .addPropertyNode(path)
                        .addPropertyNode("elements")
                        .addPropertyNode("[" + i + "].format")
                        .addConstraintViolation();
                isValid = false;
            }
        }

        return isValid;
    }
}
