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

import javax.jcr.RepositoryException;

/**
 * Defines the HTML filtering policy for a given site and workspace.
 * The policies can be retrieved using {@link RegistryService#getPolicy(String, String)}.
 */
public interface Policy {
    boolean isValidationEnabled();

    /**
     * Sanitize the given HTML text as per the HTML filtering policy.
     *
     * @param htmlText the HTML text to sanitize
     * @return the sanitized HTML text
     */
    String sanitize(String htmlText);

    /**
     * Validate the properties of a given JCR node as per the HTML filtering policy.
     *
     * @param node the JCR node to validate.
     * @return a {@link ValidationResult} object containing the validation result. The result can be used to retrieve the list of rejected tags and attributes.
     * @throws RepositoryException if an error occurs while retrieving the node properties
     */
    ValidationResult validate(JCRNodeWrapper node) throws RepositoryException;
}
