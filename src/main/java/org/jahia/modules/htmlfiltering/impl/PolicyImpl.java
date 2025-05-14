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

import org.apache.commons.collections.CollectionUtils;
import org.jahia.modules.htmlfiltering.Policy;
import org.jahia.modules.htmlfiltering.Strategy;
import org.jahia.modules.htmlfiltering.ValidationResult;
import org.jahia.modules.htmlfiltering.configuration.ElementCfg;
import org.jahia.modules.htmlfiltering.configuration.RuleSetCfg;
import org.jahia.modules.htmlfiltering.configuration.WorkspaceCfg;
import org.jahia.services.content.JCRNodeWrapper;
import org.owasp.html.HtmlChangeListener;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import javax.annotation.Nullable;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Implementation of the {@link Policy} interface for defining HTML filtering policies
 * within a specific workspace configuration. This class processes allowed and disallowed
 * rules for HTML elements, attributes, and protocols as specified in the workspace configuration.
 * It uses {@link HtmlPolicyBuilder} (OWASP Java HTML Sanitizer) to construct the filtering rules and applies them for sanitization
 * and validation operations.
 */
final class PolicyImpl implements Policy {
    private final Strategy strategy;
    private final PolicyFactory policyFactory;

    public PolicyImpl(Map<String, Pattern> formatPatterns, WorkspaceCfg workspace) {

        HtmlPolicyBuilder builder = new HtmlPolicyBuilder();
        if (workspace == null) {
            this.strategy = Strategy.SANITIZE;
        } else {
            this.strategy = WorkspaceCfg.StrategyCfg.REJECT.equals(workspace.getStrategy()) ? Strategy.REJECT : Strategy.SANITIZE;
            processRuleSet(builder, workspace.getAllowedRuleSet(), formatPatterns,
                    HtmlPolicyBuilder::allowAttributes, HtmlPolicyBuilder::allowElements, HtmlPolicyBuilder::allowUrlProtocols);

            processRuleSet(builder, workspace.getDisallowedRuleSet(), formatPatterns,
                    HtmlPolicyBuilder::disallowAttributes, HtmlPolicyBuilder::disallowElements, HtmlPolicyBuilder::disallowUrlProtocols);
        }
        this.policyFactory = builder.toFactory();
    }

    private static void processRuleSet(HtmlPolicyBuilder builder, RuleSetCfg ruleSet,
                                       Map<String, Pattern> formatPatterns,
                                       AttributeBuilderHandlerFunction attributeBuilderHandlerFunction, BuilderHandlerFunction tagHandler, BuilderHandlerFunction protocolHandler) {
        if (ruleSet != null) {
            // Apply element rules
            if (ruleSet.getElements() != null) {
                for (ElementCfg element : ruleSet.getElements()) {
                    processElement(builder, formatPatterns, attributeBuilderHandlerFunction, tagHandler, element);
                }
            }

            // Apply protocol rules
            if (ruleSet.getProtocols() != null) {
                protocolHandler.handle(builder, ruleSet.getProtocols().toArray(new String[0]));
            }
        }
    }

    private static void processElement(HtmlPolicyBuilder builder, Map<String, Pattern> formatPatterns, AttributeBuilderHandlerFunction attributeBuilderHandlerFunction, BuilderHandlerFunction tagHandler, ElementCfg element) {
        boolean noTags = CollectionUtils.isEmpty(element.getTags());
        boolean noAttributes = CollectionUtils.isEmpty(element.getAttributes());
        if (noTags && noAttributes) {
            throw new IllegalArgumentException("Each item in the 'elements' of an 'allowedRuleSet' / 'disallowedRuleSet' must contain 'tags' and/or 'attributes'. Item: " + element);
        }
        if (noAttributes) {
            // Contains tags without attributes
            if (element.getFormat() != null) {
                throw new IllegalArgumentException("'format' can only be used with 'attributes'. Item: " + element);
            }
            tagHandler.handle(builder, element.getTags().toArray(new String[0]));
        } else {
            HtmlPolicyBuilder.AttributeBuilder attributeBuilder =
                    attributeBuilderHandlerFunction.handle(builder, element.getAttributes().toArray(new String[0]));

            // Handle format pattern for allowed attributes only
            if (element.getFormat() != null) {
                Pattern formatPattern = formatPatterns.get(element.getFormat());
                if (formatPattern == null) {
                    throw new IllegalArgumentException("Format " + element.getFormat() +
                            " not defined, check your configuration");
                }
                attributeBuilder.matching(formatPattern);
            }

            if (noTags) {
                // The attributes are for all tags
                attributeBuilder.globally();
            } else {
                attributeBuilder.onElements(element.getTags().toArray(new String[0]));
            }
        }
    }

    @Override
    public Strategy getStrategy() {
        return strategy;
    }

    @Override
    public String sanitize(String htmlText) {
        return policyFactory.sanitize(htmlText);
    }

    @Override
    public ValidationResult validate(JCRNodeWrapper node) throws RepositoryException {
        ValidationResultImpl validationResult = new ValidationResultImpl();
        for (Map.Entry<String, String> propertyEntry : node.getPropertiesAsString().entrySet()) {
            validate(node, propertyEntry.getKey(), propertyEntry.getValue(), validationResult);
        }
        return validationResult;
    }

    private void validate(JCRNodeWrapper node, String name, String value, ValidationResultImpl validationResult) throws RepositoryException {
        if (node.getProperty(name).getDefinition().getRequiredType() == PropertyType.STRING) {
            validate(name, value, validationResult);
        }
    }

    void validate(String name, String value, ValidationResultImpl validationResult) {
        String sanitized = policyFactory.sanitize(value, new Listener(name), validationResult);
        validationResult.addSanitizedProperty(name, sanitized);
    }

    @FunctionalInterface
    private interface AttributeBuilderHandlerFunction {
        HtmlPolicyBuilder.AttributeBuilder handle(HtmlPolicyBuilder builder, String[] attributes);
    }

    @FunctionalInterface
    private interface BuilderHandlerFunction {
        void handle(HtmlPolicyBuilder builder, String[] items);
    }

    private static class Listener implements HtmlChangeListener<ValidationResultImpl> {

        private final String propertyName;

        public Listener(String name) {
            propertyName = name;
        }

        @Override
        public void discardedTag(@Nullable ValidationResultImpl context, String elementName) {
            if (context != null) {
                context.rejectTag(propertyName, elementName);
            }
        }

        @Override
        public void discardedAttributes(@Nullable ValidationResultImpl context, String tagName, String... attributeNames) {
            if (context != null) {
                context.rejectAttributes(propertyName, tagName, attributeNames);
            }
        }
    }
}
