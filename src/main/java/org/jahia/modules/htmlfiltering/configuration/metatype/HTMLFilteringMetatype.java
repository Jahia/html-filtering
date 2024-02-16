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
 *     Copyright (C) 2002-2024 Jahia Solutions Group. All rights reserved.
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
package org.jahia.modules.htmlfiltering.configuration.metatype;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration for the HTML Filtering service
 *
 * htmlFiltering: {
 *   htmlSanitizerDryRun: boolean,
 *   protocols: [string]
 *   elements: [{name: string}]
 *   attributes: [{name: string, pattern?: string, elements?: string }],
 *   disallow: {
 *     elements: [{name: string}]
 *     attributes: [{name: string, pattern?: string, elements?: string }],
 *   }
 * }
 */
@ObjectClassDefinition(name = "HTML Filtering Configuration", description = "Configuration for the HTML Filtering service")
public @interface HTMLFilteringMetatype {
    HTMLFiltering htmlFiltering();
}
