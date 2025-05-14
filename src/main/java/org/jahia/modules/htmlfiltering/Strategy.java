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
