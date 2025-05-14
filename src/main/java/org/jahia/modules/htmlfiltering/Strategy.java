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
package org.jahia.modules.htmlfiltering;

/**
 * Defines the strategy defined in a {@link Policy} for handling HTML content.
 */
public enum Strategy {
    /**
     * Strategy to reject HTML content that does not adhere to the allowed rules (or matches disallowed rules).
     * Any content that violates the defined rules will be deemed invalid and not accepted.
     */
    REJECT,
    /**
     * Strategy to sanitize the HTML content by removing all tags and attributes that
     * are not part of allowed rules or that are defined in disallowed rules.
     */
    SANITIZE
}
