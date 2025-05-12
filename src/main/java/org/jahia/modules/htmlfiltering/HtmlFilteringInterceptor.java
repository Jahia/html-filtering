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

import org.apache.commons.lang3.StringUtils;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRStoreService;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.interceptor.BaseInterceptor;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.validation.constraints.NotNull;
import java.util.Collections;

@Component(immediate = true)
public class HtmlFilteringInterceptor extends BaseInterceptor {

    private final static Logger logger = LoggerFactory.getLogger(HtmlFilteringInterceptor.class);

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
        HTMLFilteringService filteringService = BundleUtils.getOsgiService(HTMLFilteringService.class, null);

        JCRSiteNode resolveSite = node.getResolveSite();
        if (filteringService == null || filteringService.validate()) {
            return originalValue;
        }
        String content = originalValue.getString();

        SanitizedContent result = filteringService.validate(content, resolveSite.getSiteKey());

        return getModifiedValue(node, preservePlaceholders(result.getSanitizedContent()), content, originalValue);
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
