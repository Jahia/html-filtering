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
import org.jahia.modules.htmlfiltering.HTMLFilteringService;
import org.jahia.modules.htmlfiltering.SanitizedContent;
import org.jahia.modules.htmlfiltering.graphql.models.GqlHTMLFilteringRemovedAttributes;
import org.jahia.modules.htmlfiltering.graphql.models.GqlHTMLFilteringTest;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRTemplate;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

        SanitizedContent sanitizedContent = filteringService.validate(html, siteKey);
        gqlHTMLFilteringTest.setHtml(sanitizedContent.getSanitizedContent());
        gqlHTMLFilteringTest.setRemoveElements(sanitizedContent.getRemovedTags());
        List<GqlHTMLFilteringRemovedAttributes> gqlRemovedAttributes = sanitizedContent.getRemovedAttributes().entrySet().stream()
                .map(entry -> {
                    GqlHTMLFilteringRemovedAttributes gqlAttr = new GqlHTMLFilteringRemovedAttributes();
                    gqlAttr.setElement(entry.getKey());
                    gqlAttr.setAttributes(entry.getValue());
                    return gqlAttr;
                })
                .collect(Collectors.toList());
        gqlHTMLFilteringTest.setRemoveAttributes(gqlRemovedAttributes);

        return gqlHTMLFilteringTest;
    }
}
