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
package org.jahia.modules.htmlfiltering.graphql.mutation.impl.html_filtering;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.htmlfiltering.configuration.HTMLFilteringService;
import org.jahia.modules.htmlfiltering.graphql.models.GqlHTMLFilteringRemovedAttributes;
import org.jahia.modules.htmlfiltering.graphql.models.GqlHTMLFilteringTest;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRTemplate;
import org.owasp.html.HtmlChangeListener;
import org.owasp.html.PolicyFactory;

import javax.annotation.Nullable;
import javax.jcr.RepositoryException;
import java.util.Arrays;
import java.util.Optional;

@GraphQLName("HtmlFilteringMutation")
@GraphQLDescription("Mutation to manipulate html filtering settings on a site")
public class GqlHtmlFilteringMutation {

    @GraphQLField
    @GraphQLName("enableFiltering")
    @GraphQLDescription("Enables html filtering on site")
    public Boolean getEnableFiltering(@GraphQLNonNull @GraphQLName("siteKey") @GraphQLDescription("Site key for the affected site") String siteKey) {

        try {
            return JCRTemplate.getInstance().doExecuteWithSystemSession(session -> {
                JCRNodeWrapper siteNode = session.getNode("/sites/" + siteKey);
                siteNode.setProperty("j:doTagFiltering", true);
                session.save();

                return true;
            });
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    @GraphQLField
    @GraphQLName("disableFiltering")
    @GraphQLDescription("Disables html filtering on site")
    public Boolean getDisableFiltering(@GraphQLNonNull @GraphQLName("siteKey") @GraphQLDescription("Site key for the affected site") String siteKey) {

        try {
            return JCRTemplate.getInstance().doExecuteWithSystemSession(session -> {
                JCRNodeWrapper siteNode = session.getNode("/sites/" + siteKey);
                siteNode.setProperty("j:doTagFiltering", false);
                session.save();

                return true;
            });
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    @GraphQLField
    @GraphQLName("testFiltering")
    @GraphQLDescription("Allows to test filtering on a given site")
    public GqlHTMLFilteringTest getTestFiltering(@GraphQLNonNull @GraphQLName("siteKey") @GraphQLDescription("Site key for the affected site") String siteKey,
                                       @GraphQLNonNull @GraphQLName("html") @GraphQLDescription("HTML to be sanitized/filtered") String html ) {
        HTMLFilteringService filteringService = BundleUtils.getOsgiService(HTMLFilteringService.class, null);
        GqlHTMLFilteringTest gqlHTMLFilteringTest = new GqlHTMLFilteringTest();

        if (filteringService == null) {
            gqlHTMLFilteringTest.setHtml(html);
            return gqlHTMLFilteringTest;
        }

        PolicyFactory policyFactory = filteringService.getMergedOwaspPolicyFactory(HTMLFilteringService.DEFAULT_POLICY_KEY, siteKey);

        String h =  policyFactory.sanitize(html, new HtmlChangeListener<Object>() {
            @Override
            public void discardedTag(@Nullable Object o, String tag) {
                gqlHTMLFilteringTest.getRemovedElements().add(tag);
            }

            @Override
            public void discardedAttributes(@Nullable Object o, String tag, String... attrs) {
                findOrCreateRemovedAttributesByTag(gqlHTMLFilteringTest, tag).getAttributes().addAll(Arrays.asList(attrs));
            }
        }, null);

        gqlHTMLFilteringTest.setHtml(h);
        return gqlHTMLFilteringTest;
    }

    private GqlHTMLFilteringRemovedAttributes findOrCreateRemovedAttributesByTag(GqlHTMLFilteringTest gqlHTMLFilteringTest, String tag) {
        Optional<GqlHTMLFilteringRemovedAttributes> opt = gqlHTMLFilteringTest.getRemovedAttributes().stream().filter(t -> tag.equals(t.getElement())).findFirst();

        if (opt.isPresent()) {
            return opt.get();
        }

        GqlHTMLFilteringRemovedAttributes gqlHTMLFilteringRemovedAttributes = new GqlHTMLFilteringRemovedAttributes();
        gqlHTMLFilteringRemovedAttributes.setElement(tag);
        gqlHTMLFilteringTest.getRemovedAttributes().add(gqlHTMLFilteringRemovedAttributes);
        return gqlHTMLFilteringRemovedAttributes;
    }
}
