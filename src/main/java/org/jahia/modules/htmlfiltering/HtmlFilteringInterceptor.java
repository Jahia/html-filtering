/*
 * Copyright (C) 2002-2024 Jahia Solutions Group SA. All rights reserved.
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

import org.apache.commons.lang.StringUtils;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRStoreService;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.interceptor.BaseInterceptor;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.owasp.html.HtmlChangeListener;
import org.owasp.html.PolicyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.Collections;
import java.util.Objects;

@Component(immediate = true)
public class HtmlFilteringInterceptor extends BaseInterceptor {

    private static Logger logger = LoggerFactory.getLogger(HtmlFilteringInterceptor.class);

    private JCRStoreService jcrStoreService;

    @Activate
    public void start() {
        setRequiredTypes(Collections.singleton("String"));
        setSelectors(Collections.singleton("RichText"));
        jcrStoreService.addInterceptor(this);
    }

    @Deactivate
    public void stop() {
        jcrStoreService.removeInterceptor(this);
    }

    @Reference
    public void setJcrStoreService(JCRStoreService jcrStoreService) {
        this.jcrStoreService = jcrStoreService;
    }

    @Override
    public Value beforeSetValue(JCRNodeWrapper node, String name,
                                ExtendedPropertyDefinition definition, Value originalValue)
            throws RepositoryException {

        if (valueIsEmpty(originalValue)) return originalValue;

        JCRSiteNode resolveSite = node.getResolveSite();
        HTMLFilteringInterface filteringConfig = BundleUtils.getOsgiService(HTMLFilteringInterface.class, null);
        if (!resolveSite.isHtmlMarkupFilteringEnabled() || filteringConfig == null) {
            return originalValue;
        }

        PolicyFactory policyFactory = filteringConfig.getMergedOwaspPolicyFactory(HTMLFilteringInterface.DEFAULT_POLICY_KEY, resolveSite.getSiteKey());

        if (policyFactory == null) {
            return originalValue;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Performing HTML tag filtering for {}/{}", node.getPath(), name);
            if (logger.isTraceEnabled()) {
                logger.trace("Original value: {}", originalValue.getString());
            }
        }
        String content = originalValue.getString();
        String propInfo = node.hasProperty(definition.getName()) ? node.getProperty(definition.getName()).getRealProperty().getPath() : node.getPath();

        if (dryRun(filteringConfig, resolveSite, propInfo, policyFactory, content)) return originalValue;

        String result = policyFactory.sanitize(content);

        logger.warn("Sanitized [{}]", propInfo);

        return getModifiedValue(node, preservePlaceholders(result), content, originalValue);
    }

    @NotNull
    private static String preservePlaceholders(String result) {
        // Preserve URL context placeholders that might've been encoded by the sanitizer
        result = result.replace("%7bmode%7d", "{mode}");
        result = result.replace("%7blang%7d", "{lang}");
        result = result.replace("%7bworkspace%7d", "{workspace}");
        return result;
    }

    private static Value getModifiedValue(JCRNodeWrapper node, String result, String content, Value originalValue) throws RepositoryException {
        if (!result.equals(content)) {
            Value modifiedValue = node.getSession().getValueFactory().createValue(result);
            if (logger.isDebugEnabled()) {
                logger.debug("Done filtering of \"unwanted\" HTML tags.");
                if (logger.isTraceEnabled()) {
                    logger.trace("Modified value: {}", result);
                }
            }
            return modifiedValue;
        } else if (logger.isDebugEnabled()) {
            logger.debug("The value does not contain HTML tags that needs to be removed. The content remains unchanged.");
        }
        return originalValue;
    }

    private static boolean valueIsEmpty(Value originalValue) throws RepositoryException {
        if (StringUtils.isEmpty(originalValue.getString()) || !originalValue.getString().contains("<")) {
            if (logger.isDebugEnabled()) {
                logger.debug("The value does not contain any HTML tags. Skip filtering.");
            }
            return true;
        }
        return false;
    }

    private static boolean dryRun(HTMLFilteringInterface filteringConfig, JCRSiteNode resolveSite, String propInfo, PolicyFactory policyFactory, String content) {
        if (filteringConfig.htmlSanitizerDryRun(resolveSite.getSiteKey())) {
            logger.info("Dry run: Skipping Sanitization of [{}]", propInfo);

            policyFactory.sanitize(content, new HtmlChangeListener<Object>() {
                @Override
                public void discardedTag(@Nullable Object o, String tag) {
                    logger.info(String.format("Removed tag: %s", tag));
                }

                @Override
                public void discardedAttributes(@Nullable Object o, String tag, String... strings) {
                    logger.info(String.format("Removed attributes %s for tag %s", String.join(", ", strings), tag));
                }
            }, null);

            return true;
        }
        return false;
    }

    @Override
    public Value[] beforeSetValues(JCRNodeWrapper node, String name,
                                   ExtendedPropertyDefinition definition, Value[] originalValues)
            throws RepositoryException {
        Value[] res = new Value[originalValues.length];

        for (int i = 0; i < originalValues.length; i++) {
            Value originalValue = originalValues[i];
            res[i] = beforeSetValue(node, name, definition, originalValue);
        }
        return res;
    }
}
