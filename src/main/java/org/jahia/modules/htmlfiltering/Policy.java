package org.jahia.modules.htmlfiltering;/*
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
     * Validate the given JCR node as per the HTML filtering policy.
     * @param node the JCR node to validate. The node must be a node of type "jnt:htmlPage" or "jnt:htmlTemplate".
     * @return
     * @throws RepositoryException
     */
    ValidationResult validate(JCRNodeWrapper node) throws RepositoryException;
}
