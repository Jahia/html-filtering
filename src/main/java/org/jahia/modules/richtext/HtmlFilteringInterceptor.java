/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2023 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/Apache2 OR 2/JSEL
 *
 *     1/ Apache2
 *     ==================================================================================
 *
 *     Copyright (C) 2002-2023 Jahia Solutions Group SA. All rights reserved.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.richtext;

import org.apache.commons.lang.StringUtils;
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
import org.owasp.html.PolicyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import java.util.Collections;

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
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {

        String content = originalValue.getString();
        if (StringUtils.isEmpty(content) || !content.contains("<")) {
            if (logger.isDebugEnabled()) {
                logger.debug("The value does not contain any HTML tags. Skip filtering.");
            }
            return originalValue;
        }

        JCRSiteNode resolveSite = node.getResolveSite();
        if (!resolveSite.isHtmlMarkupFilteringEnabled()) {
            return originalValue;
        }

        RichTextConfigurationInterface filteringConfig = BundleUtils.getOsgiService(RichTextConfigurationInterface.class, null);

        if (filteringConfig == null) {
            return originalValue;
        }

        PolicyFactory policyFactory = filteringConfig.getMergedOwaspPolicyFactory(RichTextConfigurationInterface.DEFAULT_POLICY_KEY, resolveSite.getSiteKey());

        if (policyFactory == null) {
            return originalValue;
        }

        Value modifiedValue = originalValue;

        if (logger.isDebugEnabled()) {
            logger.debug("Performing HTML tag filtering for " + node.getPath() + "/" + name);
            if (logger.isTraceEnabled()) {
                logger.trace("Original value: " + content);
            }
        }

        String result = policyFactory.sanitize(content);

        if (result != content && !result.equals(content)) {
            modifiedValue = node.getSession().getValueFactory().createValue(result);
            if (logger.isDebugEnabled()) {
                logger.debug("Done filtering of \"unwanted\" HTML tags.");
                if (logger.isTraceEnabled()) {
                    logger.trace("Modified value: " + result);
                }
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("The value does not contain HTML tags that needs to be removed. The content remains unchanged.");
            }
        }

        return modifiedValue;
    }

    @Override
    public Value[] beforeSetValues(JCRNodeWrapper node, String name,
            ExtendedPropertyDefinition definition, Value[] originalValues)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        Value[] res = new Value[originalValues.length];

        for (int i = 0; i < originalValues.length; i++) {
            Value originalValue = originalValues[i];
            res[i] = beforeSetValue(node, name, definition, originalValue);
        }
        return res;
    }
}
