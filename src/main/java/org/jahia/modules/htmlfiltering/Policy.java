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

package org.jahia.modules.htmlfiltering;

import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.SelectorType;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * Defines the HTML filtering policy for a given site and workspace.
 * The policies can be retrieved using {@link RegistryService#getPolicy(String, String)}.
 */
public interface Policy {

    /**
     * Retrieves the strategy defined in the policy for handling HTML content.
     *
     * @return the strategy
     */
    Strategy getStrategy();


    /**
     * Determines whether the policy is applicable to a given property.
     * <p>
     * A property is filtered if all the following conditions are met:
     * <ul>
     *
     * <li>it is of type {@link PropertyType#STRING}</li>
     * <li>it has a {@link SelectorType#RICHTEXT} selector</li>
     * <li>it matches the properties to be processed for the node type (<code>process</code> parameter of the configuration)</li>
     * <li>it does not match the properties to be skipped for the node type (<code>skip</code> parameter of the configuration)</li>
     * </ul>
     *
     * @param node               the JCR node
     * @param propertyName       the property name to evaluate
     * @param propertyDefinition the property definition
     * @return <code>true</code> if the policy is applicable to the given property, <code>false</code> otherwise
     */
    boolean isApplicableToProperty(JCRNodeWrapper node, String propertyName,
                                   ExtendedPropertyDefinition propertyDefinition);

    /**
     * Sanitizes the given HTML text as per the HTML filtering policy.
     *
     * @param htmlText the HTML text to sanitize
     * @return the sanitized HTML text
     */
    String sanitize(String htmlText);

    /**
     * Sanitizes the given HTML text
     *
     * @param htmlText the HTML text to sanitize
     * @return a {@link HtmlValidationResult} containing the sanitized HTML and additional information such as removed tags or attributes.
     */
    HtmlValidationResult validate(String htmlText);

    /**
     * Validates the properties of a given JCR node as per the HTML filtering policy.
     *
     * @param node the JCR node to validate.
     * @return a {@link NodeValidationResult} object containing the validation result. The result can be used to retrieve the list of rejected tags and attributes.
     * @throws RepositoryException if an error occurs while retrieving the node properties
     */
    NodeValidationResult validate(JCRNodeWrapper node) throws RepositoryException;
}
