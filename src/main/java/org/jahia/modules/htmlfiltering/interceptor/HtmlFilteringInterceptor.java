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
package org.jahia.modules.htmlfiltering.interceptor;

import org.jahia.modules.htmlfiltering.Policy;
import org.jahia.modules.htmlfiltering.PolicyResolver;
import org.jahia.modules.htmlfiltering.Strategy;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRStoreService;
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
import javax.jcr.ValueFactory;

@Component(immediate = true)
@SuppressWarnings("java:S2160") // ignore warning asking to override equals methods
public class HtmlFilteringInterceptor extends BaseInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(HtmlFilteringInterceptor.class);

    private JCRStoreService jcrStoreService;
    private PolicyResolver policyResolver;

    @Activate
    public void start() {
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

    @Reference
    public void setPolicyResolver(PolicyResolver policyResolver) {
        this.policyResolver = policyResolver;
    }

    @Override
    public Value beforeSetValue(JCRNodeWrapper node, String name, ExtendedPropertyDefinition definition, Value originalValue)
            throws RepositoryException {
        Policy policy = getPolicyForInterceptor(node, name, definition, originalValue);
        if (policy != null) {
            return processValue(policy, originalValue, node.getSession().getValueFactory());
        }

        return originalValue;
    }

    @Override
    public Value[] beforeSetValues(JCRNodeWrapper node, String name, ExtendedPropertyDefinition definition, Value[] originalValues)
            throws RepositoryException {
        Policy policy = getPolicyForInterceptor(node, name, definition, originalValues);
        if (policy != null) {
            Value[] sanitizedValues = new Value[originalValues.length];
            for (int i = 0; i < originalValues.length; i++) {
                sanitizedValues[i] = processValue(policy, originalValues[i], node.getSession().getValueFactory());
            }
            return sanitizedValues;
        }
        return originalValues;
    }

    private Policy getPolicyForInterceptor(JCRNodeWrapper node, String propertyName, ExtendedPropertyDefinition definition, Object originalValue) throws RepositoryException {
        if (originalValue != null) {
            // Resolve policy with strategy: SANITIZE
            Policy policy = policyResolver.resolvePolicy(node.getResolveSite().getSiteKey(),
                    node.getSession().getWorkspace().getName(), Strategy.SANITIZE);
            if (policy != null && policy.isApplicableToProperty(node, propertyName, definition)) {
                return policy;
            }
        }

        return null;
    }

    private static Value processValue(Policy policy, Value originalValue, ValueFactory valueFactory) throws RepositoryException {
        String originalText = originalValue.getString();
        String sanitizedText = policy.sanitize(originalValue.getString()).getSanitizedHtml();
        if (!originalText.equals(sanitizedText)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Sanitize value from {} to {}", originalText, sanitizedText);
            }
            return valueFactory.createValue(sanitizedText);
        }
        return originalValue;
    }
}
