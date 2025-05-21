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
import org.apache.commons.lang3.StringUtils;
import org.jahia.modules.htmlfiltering.HtmlValidationResult;
import org.jahia.modules.htmlfiltering.NodeValidationResult;
import org.jahia.modules.htmlfiltering.Policy;
import org.jahia.modules.htmlfiltering.Strategy;
import org.jahia.modules.htmlfiltering.configuration.ElementCfg;
import org.jahia.modules.htmlfiltering.configuration.RuleSetCfg;
import org.jahia.modules.htmlfiltering.configuration.WorkspaceCfg;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.nodetypes.ExtendedItemDefinition;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.SelectorType;
import org.owasp.html.HtmlChangeListener;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import javax.annotation.Nullable;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    /**
     * A map that associates node types with a set of property names to be processed.
     * Each entry in the map defines a node type (key) and its corresponding properties (value)
     * that must be considered for processing within that node type.
     * <p>
     * The value can be <code>null</code> in case all properties of the node type are to be processed.
     */
    private final Map<String, Set<String>> propsToProcessByNodeType;
    /**
     * A map that associates node types with a set of property names to be skipped.
     * Each entry in the map defines a node type (key) and its corresponding properties (value)
     * that must be skipped for processing within that node type.
     * <p>
     * The value can be <code>null</code> in case all properties of the node type are to be skipped.
     */
    private final Map<String, Set<String>> propsToSkipByNodeType;
    private final PolicyFactory policyFactory;

    public PolicyImpl(Map<String, Pattern> formatPatterns, WorkspaceCfg workspace) {

        HtmlPolicyBuilder builder = new HtmlPolicyBuilder();
        if (workspace == null) {
            throw new IllegalArgumentException("Workspace configuration is not set");
        }
        this.strategy = readStrategy(workspace);

        if (CollectionUtils.isEmpty(workspace.getProcess())) {
            throw new IllegalArgumentException("'process' is not set");
        }
        propsToProcessByNodeType = populatePropsByNodeType(workspace.getProcess());
        if (CollectionUtils.isEmpty(workspace.getSkip())) {
            workspace.setSkip(Collections.emptyList());
        }
        propsToSkipByNodeType = populatePropsByNodeType(workspace.getSkip());

        if (workspace.getAllowedRuleSet() == null) {
            throw new IllegalArgumentException("'allowedRuleSet' is not set");
        }
        processRuleSet(builder, workspace.getAllowedRuleSet(), formatPatterns,
                HtmlPolicyBuilder::allowAttributes, HtmlPolicyBuilder::allowElements, HtmlPolicyBuilder::allowUrlProtocols);

        processRuleSet(builder, workspace.getDisallowedRuleSet(), formatPatterns,
                HtmlPolicyBuilder::disallowAttributes, HtmlPolicyBuilder::disallowElements, HtmlPolicyBuilder::disallowUrlProtocols);
        this.policyFactory = builder.toFactory();
    }

    private static Map<String, Set<String>> populatePropsByNodeType(List<String> propsByNodeType) {
        Map<String, Set<String>> result = new HashMap<>();
        for (String nodeTypeProperty : propsByNodeType) {
            if (StringUtils.isEmpty(nodeTypeProperty)) {
                throw new IllegalArgumentException("Each item in 'process' / 'skip' must be set and not empty");
            }
            String[] parts = StringUtils.split(nodeTypeProperty, '.');
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid format for 'process' / 'skip' item: " + nodeTypeProperty +
                        ". Expected format is 'nodeType.property' or 'nodeType.*'");
            }
            String nodeType = parts[0];
            if (StringUtils.isEmpty(nodeType)) {
                throw new IllegalArgumentException("Node type cannot be empty in 'process' / 'skip' item: " + nodeTypeProperty);
            }
            String propertyPattern = parts[1];
            if (StringUtils.isEmpty(propertyPattern)) {
                throw new IllegalArgumentException("Property pattern cannot be empty in 'process' / 'skip' item: " + nodeTypeProperty);
            }

            if (propertyPattern.equals("*")) {
                if (result.containsKey(nodeType)) {
                    throw new IllegalArgumentException("Duplicate 'process' / 'skip' item for the node type: " + nodeType);
                }
                // Wildcard pattern: all properties are to be processed for this node type
                result.put(nodeType, null);
            } else {
                Set<String> properties = result.get(nodeType);
                if (properties == null) {
                    if (result.containsKey(nodeType)) {
                        throw new IllegalArgumentException("Duplicate 'process' / 'skip' item for the node type: " + nodeType);
                    }
                    properties = new HashSet<>();
                    result.put(nodeType, properties);
                }
                properties.add(propertyPattern);
            }
        }
        return result;
    }

    private static Strategy readStrategy(WorkspaceCfg workspace) {
        if (workspace.getStrategy() == null) {
            throw new IllegalArgumentException("'strategy' is not set");
        }
        switch (workspace.getStrategy()) {
            case REJECT:
                return Strategy.REJECT;
            case SANITIZE:
                return Strategy.SANITIZE;
            default:
                throw new IllegalArgumentException(String.format("Unknown 'strategy':%s ", workspace.getStrategy()));
        }
    }

    private static void processRuleSet(HtmlPolicyBuilder builder, RuleSetCfg ruleSet,
                                       Map<String, Pattern> formatPatterns,
                                       AttributeBuilderHandlerFunction attributeBuilderHandlerFunction, BuilderHandlerFunction tagHandler, BuilderHandlerFunction protocolHandler) {
        if (ruleSet != null) {
            if (CollectionUtils.isEmpty(ruleSet.getElements())) {
                throw new IllegalArgumentException("At least one item in 'elements' must be defined");
            }
            // Apply element rules
            for (ElementCfg element : ruleSet.getElements()) {
                processElement(builder, formatPatterns, attributeBuilderHandlerFunction, tagHandler, element);
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
            throw new IllegalArgumentException("Each item in 'elements' of 'allowedRuleSet' / 'disallowedRuleSet' must contain 'tags' and/or 'attributes'. Item: " + element);
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
                    throw new IllegalArgumentException(String.format("Format '%s' not defined, check your configuration", element.getFormat()));
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
    public String sanitize(ExtendedPropertyDefinition definition, String propertyHtmlText) {
        if (shouldBeFiltered(definition)) {
            return sanitize(propertyHtmlText);
        }
        return propertyHtmlText;
    }

  /**
   * Determines whether the given property definition should be filtered.
   * A property is filtered if all the following conditions are met:
   * <ul>
   *
   * <li>it is of type {@link PropertyType#STRING}</li>
   * <li>it has a {@link SelectorType#RICHTEXT} selector</li>
   * <li>it matches the properties to be processed for the node type (<code>process</code> parameter of the configuration)</li>
   * <li>it does not match the properties to be skipped for the node type (<code>skip</code> parameter of the configuration)</li>
   * </ul>
   *
   * @param definition the property definition to evaluate
   * @return <code>true</code> if the property should be filtered based on its type and matching conditions, <code>false</code> otherwise
   */
    private boolean shouldBeFiltered(ExtendedPropertyDefinition definition) {
        return isRichTextStringProperty(definition)
                && matches(definition, propsToProcessByNodeType)
                && !matches(definition, propsToSkipByNodeType);
    }

    private static boolean isRichTextStringProperty(ExtendedPropertyDefinition definition) {
        return
                definition.getRequiredType() == PropertyType.STRING
                        && definition.getSelector() == SelectorType.RICHTEXT;
    }

    private static boolean matches(ExtendedItemDefinition extendedItemDefinition, Map<String, Set<String>> map) {
        ExtendedNodeType nodeType = extendedItemDefinition.getDeclaringNodeType();
        if (nodeType == null) {
            return false;
        }
        for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
            if (entry.getValue() == null) {
                // for node types defined with a wildcard, also check direct or indirect supertypes
                if (nodeType.isNodeType(entry.getKey())) {
                    return true;
                }
            } else {
                // the properties are explicitly listed for this node type, use an exact match in this case
                if (entry.getKey().equals(nodeType.getName())) {
                    if (entry.getValue().contains(extendedItemDefinition.getName())) {
                        return true;
                    }
                }
            }
        }
        return false; // no match found for any node type
    }


    @Override
    public HtmlValidationResult validate(String htmlText) {
        HtmlValidationResultImpl result = new HtmlValidationResultImpl();
        String SanitizedText = policyFactory.sanitize(htmlText, new HtmlChangeListener<HtmlValidationResultImpl>() {
            @Override
            public void discardedTag(HtmlValidationResultImpl context, String elementName) {
                context.addRejectedTag(elementName);
            }

            @Override
            public void discardedAttributes(HtmlValidationResultImpl context, String tagName, String... attributeNames) {
                context.addRejectedAttributeByTag(tagName, new HashSet<>(Arrays.asList(attributeNames)));
            }
        }, result);

        result.setSanitizedHtml(SanitizedText);
        return result;
    }

    @Override
    public NodeValidationResult validate(JCRNodeWrapper node) throws RepositoryException {
        NodeValidationResultImpl validationResult = new NodeValidationResultImpl();
        for (Map.Entry<String, String> propertyEntry : node.getPropertiesAsString().entrySet()) {
            validate(node, propertyEntry.getKey(), propertyEntry.getValue(), validationResult);
        }
        return validationResult;
    }

    private void validate(JCRNodeWrapper node, String name, String value, NodeValidationResultImpl validationResult) throws RepositoryException {
        if (shouldBeFiltered(node.getApplicablePropertyDefinition(name))) {
            validate(name, value, validationResult);
        }
    }

    void validate(String name, String value, NodeValidationResultImpl validationResult) {
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

    private static class Listener implements HtmlChangeListener<NodeValidationResultImpl> {

        private final String propertyName;

        public Listener(String name) {
            propertyName = name;
        }

        @Override
        public void discardedTag(@Nullable NodeValidationResultImpl context, String elementName) {
            if (context != null) {
                context.rejectTag(propertyName, elementName);
            }
        }

        @Override
        public void discardedAttributes(@Nullable NodeValidationResultImpl context, String tagName, String... attributeNames) {
            if (context != null) {
                context.rejectAttributes(propertyName, tagName, attributeNames);
            }
        }
    }
}
