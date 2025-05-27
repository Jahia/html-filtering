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
package org.jahia.modules.htmlfiltering.model;

import org.jahia.modules.htmlfiltering.interceptor.HtmlFilteringInterceptor;
import org.jahia.modules.htmlfiltering.validation.HtmlFilteringValidator;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;

import javax.jcr.Value;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;

public class PolicyModel {
    private static final String NODE_TYPES_PROPS_MSG = "{org.jahia.modules.htmlfiltering.model.PolicyModel.nodeTypesProperties.invalidFormat.message}";
    private static final String NODE_TYPES_PROPS_REGEX = "^[\\w:_-]+(\\.\\*|\\.[\\w:_-]+)?$"; // Examples: "mynt:mynode", "mynt:mynode.*", "mynt:mynode.myProp"
    @NotNull
    @Valid
    private RuleSetModel allowedRuleSet;
    private RuleSetModel disallowedRuleSet;
    @NotNull
    private PolicyStrategy strategy;
    @NotEmpty
    private List<@Pattern(regexp = NODE_TYPES_PROPS_REGEX, message = NODE_TYPES_PROPS_MSG) String> process;
    private List<@Pattern(regexp = NODE_TYPES_PROPS_REGEX, message = NODE_TYPES_PROPS_MSG) String> skip;
    private List<String> skipOnPermissions;

    public RuleSetModel getAllowedRuleSet() {
        return allowedRuleSet;
    }

    public void setAllowedRuleSet(RuleSetModel allowedRuleSet) {
        this.allowedRuleSet = allowedRuleSet;
    }

    public RuleSetModel getDisallowedRuleSet() {
        return disallowedRuleSet;
    }

    public void setDisallowedRuleSet(RuleSetModel disallowedRuleSet) {
        this.disallowedRuleSet = disallowedRuleSet;
    }

    public PolicyStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(PolicyStrategy strategy) {
        this.strategy = strategy;
    }

    public List<String> getProcess() {
        return process;
    }

    public void setProcess(List<String> process) {
        this.process = process;
    }

    public List<String> getSkip() {
        return skip;
    }

    public void setSkip(List<String> skip) {
        this.skip = skip;
    }

    public List<String> getSkipOnPermissions() {
        return skipOnPermissions;
    }

    public void setSkipOnPermissions(List<String> skipOnPermissions) {
        this.skipOnPermissions = skipOnPermissions;
    }

    /**
     * Defines the strategy for handling HTML content that does not adhere
     * to the allowed rule set.
     */
    public enum PolicyStrategy {
        /**
         * Strategy to reject HTML content that does not adhere to the allowed rule set.
         * Any content that violates the defined rules will be deemed invalid and not accepted.
         *
         * @see org.jahia.modules.htmlfiltering.validation.HtmlValidator#isValid(HtmlFilteringValidator, ConstraintValidatorContext)
         */
        REJECT,
        /**
         * Strategy to sanitize the HTML content by removing all tags and attributes that
         * are not part of the allowed rule set.
         * This cleanup is performed automatically before storing the value of a node property in the JCR.
         *
         * @see HtmlFilteringInterceptor#beforeSetValue(JCRNodeWrapper, String, ExtendedPropertyDefinition, Value)
         */
        SANITIZE
    }

    @Override
    public String toString() {
        return "PolicyModel{" +
                "allowedRuleSet=" + allowedRuleSet +
                ", disallowedRuleSet=" + disallowedRuleSet +
                ", strategy=" + strategy +
                ", process=" + process +
                ", skip=" + skip +
                ", skipOnPermissions=" + skipOnPermissions +
                '}';
    }
}
