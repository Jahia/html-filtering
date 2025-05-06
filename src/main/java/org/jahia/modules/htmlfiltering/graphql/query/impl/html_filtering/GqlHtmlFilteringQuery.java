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
package org.jahia.modules.htmlfiltering.graphql.query.impl.html_filtering;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.htmlfiltering.HTMLFilteringService;
import org.jahia.modules.htmlfiltering.graphql.models.GqlHTMLFiltering;
import org.jahia.modules.htmlfiltering.graphql.models.GqlHTMLFilteringConfig;
import org.jahia.modules.htmlfiltering.graphql.models.GqlHTMLFilteringConfigAttribute;
import org.jahia.modules.htmlfiltering.graphql.models.HTMLFilteringConfigInterface;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.sites.JahiaSitesService;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@GraphQLName("HTMLFilteringQuery")
@GraphQLDescription("Query for html filtering settings")
public class GqlHtmlFilteringQuery {

    public static final String TAG_FILTERING = "j:doTagFiltering";
    public static final String ELEMENTS = "elements";

    @GraphQLField
    @GraphQLName("filteringSettings")
    @GraphQLDescription("HTML filtering settings for a site")
    public GqlHTMLFiltering getFilteringSettings(@GraphQLNonNull @GraphQLName("siteKey") @GraphQLDescription("Site key for the affected site") String siteKey) {
        GqlHTMLFiltering filtering;

        try {
            filtering = JCRTemplate.getInstance().doExecuteWithSystemSession(session -> {
                JCRNodeWrapper siteNode = session.getNode("/sites/" + siteKey);
                boolean enabled = siteNode.hasProperty(TAG_FILTERING) && siteNode.getProperty(TAG_FILTERING).getBoolean();

                return new GqlHTMLFiltering(siteKey, enabled);
            });
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }

        return filtering;
    }

    @GraphQLField
    @GraphQLName("sitesWithActiveFiltering")
    @GraphQLDescription("Shows sites where html filtering is enabled")
    public List<String> getSitesWithActiveFiltering() {
        List<String> enabled = new ArrayList<>();

        try {
            List<JCRSiteNode> siteNodes = JahiaSitesService.getInstance().getSitesNodeList();

            for (JCRSiteNode siteNode : siteNodes) {
                if (siteNode.hasProperty(TAG_FILTERING) && siteNode.getProperty(TAG_FILTERING).getBoolean()) {
                    enabled.add(siteNode.getSiteKey());
                }
            }
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }

        return enabled;
    }

    @GraphQLField
    @GraphQLName("configuration")
    @GraphQLDescription("HTML filtering configuration for a given site")
    public GqlHTMLFilteringConfig getConfiguration(@GraphQLNonNull @GraphQLName("siteKey") @GraphQLDescription("Site key for the affected site") String siteKey) {
        HTMLFilteringService htmlFilteringService = BundleUtils.getOsgiService(HTMLFilteringService.class, null);

        if (htmlFilteringService == null || !htmlFilteringService.configExists(siteKey)) {
            return null;
        }

        JSONObject config = htmlFilteringService.getMergedJSONPolicy(HTMLFilteringService.DEFAULT_POLICY_KEY, siteKey);
        config = config.getJSONObject("htmlFiltering");

        GqlHTMLFilteringConfig gqlHTMLFilteringConfig = new GqlHTMLFilteringConfig();
        getProtocols(config, gqlHTMLFilteringConfig);
        getElements(config, gqlHTMLFilteringConfig);
        getAttributes(config, gqlHTMLFilteringConfig);

        if (config.has("disallow")) {
            HTMLFilteringConfigInterface disallow = gqlHTMLFilteringConfig.getDisallow();
            config = config.getJSONObject("disallow");
            getProtocols(config, disallow);
            getElements(config, disallow);
            getAttributes(config, disallow);
        }

        return gqlHTMLFilteringConfig;
    }

    private void getProtocols(JSONObject config, HTMLFilteringConfigInterface gqlHTMLFilteringConfig) {
        if (config.has("protocols")) {
            JSONArray protocols = config.getJSONArray("protocols");
            protocols.forEach(p -> gqlHTMLFilteringConfig.getProtocols().add((String) p));
        }
    }

    private void getElements(JSONObject config, HTMLFilteringConfigInterface gqlHTMLFilteringConfig) {
        if (config.has(ELEMENTS)) {
            JSONArray elements = config.getJSONArray(ELEMENTS);
            elements.forEach(e -> {
                if (((JSONObject)e).get("name") instanceof JSONArray) {
                    ((JSONObject)e).getJSONArray("name").forEach(el -> gqlHTMLFilteringConfig.getElements().add((String) el));
                } else {
                    gqlHTMLFilteringConfig.getElements().add(((JSONObject)e).getString("name"));
                }
            });
        }
    }

    private void getAttributes(JSONObject config, HTMLFilteringConfigInterface gqlHTMLFilteringConfig) {
        if (config.has("attributes")) {
            JSONArray attributes = config.getJSONArray("attributes");
            List<GqlHTMLFilteringConfigAttribute> a = gqlHTMLFilteringConfig.getAttributes();

            attributes.forEach(attr -> {
                List<String> toHandle = new ArrayList<>();

                if (((JSONObject) attr).get("name") instanceof JSONArray) {
                    ((JSONObject) attr).getJSONArray("name").forEach(t -> toHandle.add((String) t));
                } else {
                    toHandle.add(((JSONObject) attr).getString("name"));
                }

                handleAttributes((JSONObject) attr, toHandle, a);
            });
        }
    }

    private void handleAttributes(JSONObject attr, List<String> toHandle, List<GqlHTMLFilteringConfigAttribute> a) {
        for (String s : toHandle) {
            GqlHTMLFilteringConfigAttribute at = findOrCreateAttributesByAttribute(a, s);

            if (attr.has("pattern")) {
                at.setPattern(attr.getString("pattern"));
            }

            if (attr.has(ELEMENTS)) {
                if (attr.get(ELEMENTS) instanceof JSONArray) {
                    attr.getJSONArray(ELEMENTS).forEach(e -> at.getElements().add((String) e));
                } else {
                    at.getElements().add(attr.getString(ELEMENTS));
                }
            }
        }
    }

    private GqlHTMLFilteringConfigAttribute findOrCreateAttributesByAttribute(List<GqlHTMLFilteringConfigAttribute> attr, String attribute) {
        Optional<GqlHTMLFilteringConfigAttribute> opt = attr.stream().filter(a -> attribute.equals(a.getAttribute())).findFirst();

        if (opt.isPresent()) {
            return opt.get();
        }

        GqlHTMLFilteringConfigAttribute gqlHTMLFilteringConfigAttribute = new GqlHTMLFilteringConfigAttribute();
        gqlHTMLFilteringConfigAttribute.setAttribute(attribute);
        attr.add(gqlHTMLFilteringConfigAttribute);
        return gqlHTMLFilteringConfigAttribute;
    }
}
