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

import javax.jcr.RepositoryException;

/**
 * Defines the HTML filtering policy for a given site and workspace.
 * The policies can be retrieved using {@link RegistryService#getPolicy(String, String)}.
 */
public interface Policy {

    /**
     * Retrieves the strategy defined in the policy for handling HTML content.
     * If not defined, the default strategy is {@link Strategy#SANITIZE}.
     *
     * @return the strategy
     */
    Strategy getStrategy();

    /**
     * Sanitizes the given HTML text as per the HTML filtering policy.
     *
     * @param htmlText the HTML text to sanitize
     * @return the sanitized HTML text
     */
    String sanitize(String htmlText);

    /**
     * Sanitizes the provided HTML text in the context of a specific property definition. It applies the sanitization rules applicable to the given property
     * (based on <code>include</code> and <code>skip</code> settings in the configuration files).
     *
     * @param definition       the extended property definition which provides context for sanitization
     * @param propertyHtmlText the HTML text to be sanitized according to the specified policy
     * @return the sanitized HTML string after applying the filtering rules
     */
    String sanitize(ExtendedPropertyDefinition definition, String propertyHtmlText);

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
