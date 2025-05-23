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

import org.apache.commons.lang3.StringUtils;
import org.jahia.modules.htmlfiltering.*;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the {@link Policy} interface for defining HTML filtering policies
 * within a specific workspace configuration. This class processes allowed and disallowed
 * rules for HTML elements, attributes, and protocols as specified in the workspace configuration.
 * It uses {@link HtmlPolicyBuilder} (OWASP Java HTML Sanitizer) to construct the filtering rules and applies them for sanitization
 * and validation operations.
 */
public final class PolicyImpl implements Policy {
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

    public PolicyImpl(Strategy strategy, Map<String, Set<String>> propsToProcessByNodeType, Map<String, Set<String>> propsToSkipByNodeType, PolicyFactory policyFactory) {
        this.strategy = strategy;
        this.propsToProcessByNodeType = propsToProcessByNodeType;
        this.propsToSkipByNodeType = propsToSkipByNodeType;
        this.policyFactory = policyFactory;
    }

    @Override
    public Strategy getStrategy() {
        return strategy;
    }

    @Override
    public boolean isApplicableToProperty(JCRNodeWrapper node, String propertyName, ExtendedPropertyDefinition propertyDefinition) {
        boolean result = isRichTextStringProperty(propertyDefinition)
                && isPropertyConfigured(node, propertyName, propsToProcessByNodeType)
                && !isPropertyConfigured(node, propertyName, propsToSkipByNodeType);

        if (logger.isDebugEnabled()) {
            logger.debug("The policy is{} applicable to the node: {}, property: {}, definition: {}.", result ? "" : " not",
                    node.getPath(), propertyName, propertyDefinition);
        }

        return result;
    }

    @Override
    public PolicySanitizedHtmlResult sanitize(String htmlText) {
        PolicySanitizedHtmlResultImpl result = new PolicySanitizedHtmlResultImpl();
        String sanitized = policyFactory.sanitize(htmlText, new HtmlChangeListener<PolicySanitizedHtmlResultImpl>() {
            @Override
            public void discardedTag(PolicySanitizedHtmlResultImpl context, String elementName) {
                context.addRejectedTag(elementName);
            }

            @Override
            public void discardedAttributes(PolicySanitizedHtmlResultImpl context, String tagName, String... attributeNames) {
                context.addRejectedAttributeByTag(tagName, new HashSet<>(Arrays.asList(attributeNames)));
            }
        }, result);

        result.setSanitizedHtml(postProcessSanitizedHtml(sanitized));
        return result;
    }

    private boolean isRichTextStringProperty(ExtendedPropertyDefinition definition) {
        return definition.getRequiredType() == PropertyType.STRING
                && definition.getSelector() == SelectorType.RICHTEXT;
    }

    private boolean isPropertyConfigured(JCRNodeWrapper node, String propertyName, Map<String, Set<String>> propsByNodeType) {
        for (Map.Entry<String, Set<String>> entry : propsByNodeType.entrySet()) {
            String nodeType = entry.getKey();
            if (safeIsNodeType(node, nodeType)) {
                Set<String> props = entry.getValue();
                // it is a wildcard, or it matches the property's name
                return props == null || props.contains(propertyName);
            }
        }
        return false; // no match found for any node type
    }

    private boolean safeIsNodeType(JCRNodeWrapper node, String nodeType) {
        try {
            return node.isNodeType(nodeType);
        } catch (RepositoryException e) {
            logger.warn("Unable to check if the node {} is of type {}, " +
                    "please review your html-filtering configured skip/process node types declarations", node, nodeType);
        }
        return false;
    }

    private String postProcessSanitizedHtml(String sanitizedHtml) {
        if (StringUtils.isAllBlank(sanitizedHtml)) {
            return sanitizedHtml;
        }
        // post process the sanitized HTML to replace the Jahia rich text editors placeholders
        // todo make this configurable/extendable ?
        String result = sanitizedHtml.replace("%7bmode%7d", "{mode}");
        result = result.replace("%7blang%7d", "{lang}");
        return result.replace("%7bworkspace%7d", "{workspace}");
    }
}
