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
import org.jahia.modules.htmlfiltering.*;
import org.jahia.modules.htmlfiltering.configuration.ElementCfg;
import org.jahia.modules.htmlfiltering.configuration.RuleSetCfg;
import org.jahia.modules.htmlfiltering.configuration.WorkspaceCfg;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.SelectorType;
import org.owasp.html.HtmlChangeListener;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
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
    private static final Logger logger = LoggerFactory.getLogger(PolicyImpl.class);
    private final Strategy strategy;
    /**
     * A map that associates node types with a set of property names to be processed.
     * Each entry in the map defines a node type (key) and its corresponding properties (value)
     * that must be considered for processing within that node type.
     * <p>
     * The value can be <code>null</code> in case all properties of the node type are to be processed.
     */
    final Map<String, Set<String>> propsToProcessByNodeType;
    /**
     * A map that associates node types with a set of property names to be skipped.
     * Each entry in the map defines a node type (key) and its corresponding properties (value)
     * that must be skipped for processing within that node type.
     * <p>
     * The value can be <code>null</code> in case all properties of the node type are to be skipped.
     */
    final Map<String, Set<String>> propsToSkipByNodeType;
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
        propsToProcessByNodeType = createPropsByNodeType(workspace.getProcess(), "process");
        if (CollectionUtils.isEmpty(workspace.getSkip())) {
            workspace.setSkip(Collections.emptyList());
        }
        propsToSkipByNodeType = createPropsByNodeType(workspace.getSkip(), "skip");

        if (workspace.getAllowedRuleSet() == null) {
            throw new IllegalArgumentException("'allowedRuleSet' is not set");
        }
        processRuleSet(builder, workspace.getAllowedRuleSet(), formatPatterns,
                HtmlPolicyBuilder::allowAttributes, HtmlPolicyBuilder::allowElements, HtmlPolicyBuilder::allowUrlProtocols);

        processRuleSet(builder, workspace.getDisallowedRuleSet(), formatPatterns,
                HtmlPolicyBuilder::disallowAttributes, HtmlPolicyBuilder::disallowElements, HtmlPolicyBuilder::disallowUrlProtocols);
        this.policyFactory = builder.toFactory();
    }

    private static Map<String, Set<String>> createPropsByNodeType(List<String> propsByNodeType, String configSectionName) {
        Map<String, Set<String>> result = new HashMap<>();
        for (String nodeTypeProperty : propsByNodeType) {
            if (StringUtils.isEmpty(nodeTypeProperty)) {
                throw new IllegalArgumentException(String.format("Each item in '%s' must be set and not empty", configSectionName));
            }
            String[] parts = StringUtils.split(nodeTypeProperty, '.');
            switch (parts.length) {
                case 1:
                    setWildcardEntryForNodeType(result, parts[0], configSectionName);
                    break;
                case 2:
                    String nodeType = parts[0];
                    String propertyPattern = parts[1];
                    setPropertyPatternEntryForNodeType(propertyPattern, result, nodeType, configSectionName);
                    break;
                default:
                    throw new IllegalArgumentException(String.format("Invalid format for item '%s' in '%s'. Expected format is 'nodeType.property' or 'nodeType.*'", nodeTypeProperty, configSectionName));
            }
        }
        return result;
    }

    private static void setPropertyPatternEntryForNodeType(String propertyPattern, Map<String, Set<String>> result, String nodeType, String configSectionName) {
        if (propertyPattern.equals("*")) {
            setWildcardEntryForNodeType(result, nodeType, configSectionName);
        } else {
            Set<String> properties = result.get(nodeType);
            if (properties == null) {
                if (result.containsKey(nodeType)) {
                    logger.warn("There is already a wildcard entry for the node type {} under '{}'. Ignoring the property '{}'", nodeType, configSectionName, propertyPattern);
                    return;
                }
                properties = new HashSet<>();
                result.put(nodeType, properties);
            }
            properties.add(propertyPattern);
        }
    }

    private static void setWildcardEntryForNodeType(Map<String, Set<String>> result, String nodeType, String configSectionName) {
        if (result.containsKey(nodeType)) {
            logger.warn("There is already an entry for the node type {} under '{}'. Overwriting it with the wildcard", nodeType, configSectionName);
        }
        // Wildcard pattern: all properties are to be processed for this node type
        result.put(nodeType, null);
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
    public boolean isApplicableToProperty(JCRNodeWrapper node, String propertyName, ExtendedPropertyDefinition propertyDefinition) throws RepositoryException {
        boolean result = isRichTextStringProperty(propertyDefinition)
                && isPropertyConfigured(node, propertyName, propsToProcessByNodeType)
                && !isPropertyConfigured(node, propertyName, propsToSkipByNodeType);

        if (logger.isDebugEnabled()) {
            logger.debug("The policy is{} applicable to the node: {}, property: {}, definition: {}.", result ? "" : " not",
                    node.getPath(), propertyName, propertyDefinition);
        }

        return result;
    }

    private static boolean isRichTextStringProperty(ExtendedPropertyDefinition definition) {
        return definition.getRequiredType() == PropertyType.STRING
                && definition.getSelector() == SelectorType.RICHTEXT;
    }

    private static boolean isPropertyConfigured(JCRNodeWrapper node, String propertyName, Map<String, Set<String>> propsByNodeType) throws RepositoryException {
        for (Map.Entry<String, Set<String>> entry : propsByNodeType.entrySet()) {
            String nodeType = entry.getKey();
            if (node.isNodeType(nodeType)) {
                Set<String> props = entry.getValue();
                // it is a wildcard, or it matches the property's name
                return props == null || props.contains(propertyName);
            }
        }
        return false; // no match found for any node type
    }

    @Override
    public PolicyExecutionResultImpl execute(String htmlText) {
        PolicyExecutionResultImpl result = new PolicyExecutionResultImpl();
        String sanitized = policyFactory.sanitize(htmlText, new HtmlChangeListener<PolicyExecutionResultImpl>() {
            @Override
            public void discardedTag(PolicyExecutionResultImpl context, String elementName) {
                context.addRejectedTag(elementName);
            }

            @Override
            public void discardedAttributes(PolicyExecutionResultImpl context, String tagName, String... attributeNames) {
                context.addRejectedAttributeByTag(tagName, new HashSet<>(Arrays.asList(attributeNames)));
            }
        }, result);

        result.setSanitizedHtml(sanitized);
        return result;
    }

    @FunctionalInterface
    private interface AttributeBuilderHandlerFunction {
        HtmlPolicyBuilder.AttributeBuilder handle(HtmlPolicyBuilder builder, String[] attributes);
    }

    @FunctionalInterface
    private interface BuilderHandlerFunction {
        void handle(HtmlPolicyBuilder builder, String[] items);
    }
}
