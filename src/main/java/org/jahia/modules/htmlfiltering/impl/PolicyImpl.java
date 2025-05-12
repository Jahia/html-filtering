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

import org.apache.commons.collections.CollectionUtils;
import org.jahia.modules.htmlfiltering.Policy;
import org.jahia.modules.htmlfiltering.ValidationResult;
import org.jahia.modules.htmlfiltering.ValidationResult.PropertyValidationResult;
import org.jahia.modules.htmlfiltering.ValidationResult.ValidationResultBuilder;
import org.jahia.modules.htmlfiltering.configuration.ElementCfg;
import org.jahia.modules.htmlfiltering.configuration.WorkspaceCfg;
import org.jahia.services.content.JCRNodeWrapper;
import org.owasp.html.HtmlChangeListener;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import javax.annotation.Nullable;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Pattern;

final class PolicyImpl implements Policy {
    private final WorkspaceCfg workspace; // keep a reference to the workspace configuration
    private final PolicyFactory policyFactory;

    public PolicyImpl(Map<String, Pattern> formatPatterns, WorkspaceCfg workspace) {

        this.workspace = workspace;
        HtmlPolicyBuilder builder = new HtmlPolicyBuilder();
        // TODO refactor
        if (workspace != null) {
            if (workspace.getAllowedRuleSet() != null) {
                for (ElementCfg element : workspace.getAllowedRuleSet().getElements()) {
                    HtmlPolicyBuilder.AttributeBuilder attributeBuilder;

                    // TODO review conditions
                    if (CollectionUtils.isNotEmpty(element.getTags()) && CollectionUtils.isEmpty(element.getAttributes())) {
                        // contains only tags
                        builder.allowElements(element.getTags().toArray(new String[0]));
                    } else {
                        attributeBuilder = builder.allowAttributes(element.getAttributes().toArray(new String[0]));
                        if (element.getFormat() != null) {
                            Pattern formatPattern = formatPatterns.get(element.getFormat());
                            if (formatPattern == null) {
                                throw new IllegalArgumentException("Format " + element.getFormat() + " not defined, check your configuration");
                            }
                            attributeBuilder.matching(formatPattern);
                        }
                        if (CollectionUtils.isEmpty(element.getTags())) {
                            // the attributes are for all tags
                            attributeBuilder.globally();
                        } else {
                            attributeBuilder.onElements(element.getTags().toArray(new String[0]));
                        }
                    }
                }
                if (workspace.getAllowedRuleSet().getProtocols() != null) {
                    builder.allowUrlProtocols(workspace.getAllowedRuleSet().getProtocols().toArray(new String[0]));
                }
            }

            if (workspace.getDisallowedRuleSet() != null) {

                for (ElementCfg element : workspace.getDisallowedRuleSet().getElements()) {
                    HtmlPolicyBuilder.AttributeBuilder attributeBuilder;

                    // TODO review conditions
                    if (CollectionUtils.isNotEmpty(element.getTags()) && CollectionUtils.isEmpty(element.getAttributes())) {
                        // contains only tags
                        builder.disallowElements(element.getTags().toArray(new String[0]));
                    } else {
                        attributeBuilder = builder.disallowAttributes(element.getAttributes().toArray(new String[0]));
                        // TODO handle format
                        if (CollectionUtils.isEmpty(element.getTags())) {
                            // the attributes are for all tags
                            attributeBuilder.globally();
                        } else {
                            attributeBuilder.onElements(element.getTags().toArray(new String[0]));
                        }
                    }
                }
                if (workspace.getDisallowedRuleSet().getProtocols() != null) {
                    builder.disallowUrlProtocols(workspace.getAllowedRuleSet().getProtocols().toArray(new String[0]));
                }
            }
        }
        this.policyFactory = builder.toFactory();
    }

    @Override
    public boolean isValidationEnabled() {
        return WorkspaceCfg.StrategyCfg.REJECT.equals(workspace.getStrategy());
    }

    @Override
    public String sanitize(String htmlText) {
        return policyFactory.sanitize(htmlText);
    }

    @Override
    public ValidationResult validate(JCRNodeWrapper node) throws RepositoryException {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        if (workspace != null && WorkspaceCfg.StrategyCfg.REJECT.equals(workspace.getStrategy())) {
            for (Map.Entry<String, String> propertyEntry : node.getPropertiesAsString().entrySet()) {
                validateProperty(node, propertyEntry.getKey(), propertyEntry.getValue(), validationResultBuilder);
            }
        }
        return validationResultBuilder.build();
    }

    private void validateProperty(JCRNodeWrapper node, String name, String value, ValidationResultBuilder validationResultBuilder) throws RepositoryException {
        if (node.getProperty(name).getDefinition().getRequiredType() == PropertyType.STRING) {
            PropertyValidationResult propertyValidationResult = new PropertyValidationResult();
            policyFactory.sanitize(value, new Listener(), propertyValidationResult);
            validationResultBuilder.addPropertyValidationResult(name, propertyValidationResult);
        }
    }

    private static class Listener implements HtmlChangeListener<PropertyValidationResult> {

        @Override
        public void discardedTag(@Nullable PropertyValidationResult context, String elementName) {
            context.addRejectedTag(elementName);
        }

        @Override
        public void discardedAttributes(@Nullable PropertyValidationResult context, String tagName, String... attributeNames) {
            context.addRejectedAttributeByTag(tagName, new HashSet<>(Arrays.asList(attributeNames)));
        }
    }
}
