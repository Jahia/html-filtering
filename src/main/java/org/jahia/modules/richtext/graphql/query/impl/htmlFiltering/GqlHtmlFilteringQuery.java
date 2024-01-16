package org.jahia.modules.richtext.graphql.query.impl.htmlFiltering;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.richtext.RichTextConfigurationInterface;
import org.jahia.modules.richtext.graphql.models.*;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.sites.JahiaSitesService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.owasp.html.HtmlChangeListener;
import org.owasp.html.PolicyFactory;

import javax.annotation.Nullable;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@GraphQLName("HTMLFilteringQuery")
@GraphQLDescription("Query for html filtering settings")
public class GqlHtmlFilteringQuery {

    @GraphQLField
    @GraphQLName("filteringSettings")
    @GraphQLDescription("HTML filtering settings for a site")
    public GqlHTMLFiltering getFilteringSettings(@GraphQLNonNull @GraphQLName("siteKey") @GraphQLDescription("Site key for the affected site") String siteKey) {
        GqlHTMLFiltering filtering = null;

        try {
            filtering = JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<GqlHTMLFiltering>() {

                @Override
                public GqlHTMLFiltering doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    JCRNodeWrapper siteNode = session.getNode("/sites/" + siteKey);
                    boolean enabled = siteNode.hasProperty("j:doTagFiltering") && siteNode.getProperty("j:doTagFiltering").getBoolean();

                    return new GqlHTMLFiltering(siteKey, enabled);
                }
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
                if (siteNode.hasProperty("j:doTagFiltering") && siteNode.getProperty("j:doTagFiltering").getBoolean()) {
                    enabled.add(siteNode.getSiteKey());
                }
            }
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }

        return enabled;
    }

    @GraphQLField
    @GraphQLName("richtextConfiguration")
    @GraphQLDescription("RichText filtering configuration for a given site")
    public GqlRichTextConfig getRichTextConfiguration(@GraphQLNonNull @GraphQLName("siteKey") @GraphQLDescription("Site key for the affected site") String siteKey) {
        RichTextConfigurationInterface filteringConfig = BundleUtils.getOsgiService(RichTextConfigurationInterface.class, null);

        if (filteringConfig == null || !filteringConfig.configExists(siteKey)) {
            return null;
        }

        JSONObject config = filteringConfig.getMergedJSONPolicy(RichTextConfigurationInterface.DEFAULT_POLICY_KEY, siteKey);
        config = config.getJSONObject("htmlFiltering");

        GqlRichTextConfig gqlRichTextConfig = new GqlRichTextConfig();
        getProtocols(config, gqlRichTextConfig);
        getElements(config, gqlRichTextConfig);
        getAttributes(config, gqlRichTextConfig);

        if (config.has("disallow")) {
            RichTextConfigInterface disallow = gqlRichTextConfig.getDisallow();
            config = config.getJSONObject("disallow");
            getProtocols(config, disallow);
            getElements(config, disallow);
            getAttributes(config, disallow);
        }

        return gqlRichTextConfig;
    }

    private void getProtocols(JSONObject config, RichTextConfigInterface gqlRichTextConfig) {
        if (config.has("protocols")) {
            JSONArray protocols = config.getJSONArray("protocols");
            protocols.forEach(p -> gqlRichTextConfig.getProtocols().add((String) p));
        }
    }

    private void getElements(JSONObject config, RichTextConfigInterface gqlRichTextConfig) {
        if (config.has("elements")) {
            JSONArray elements = config.getJSONArray("elements");
            elements.forEach(e -> {
                if (((JSONObject)e).get("name") instanceof JSONArray) {
                    ((JSONObject)e).getJSONArray("name").forEach(el -> gqlRichTextConfig.getElements().add((String) el));
                } else {
                    gqlRichTextConfig.getElements().add(((JSONObject)e).getString("name"));
                }
            });
        }
    }

    private void getAttributes(JSONObject config, RichTextConfigInterface gqlRichTextConfig) {
        if (config.has("attributes")) {
            JSONArray attributes = config.getJSONArray("attributes");
            List<GqlRichTextConfigAttribute> a = gqlRichTextConfig.getAttributes();

            attributes.forEach(attr -> {
                List<String> toHandle = new ArrayList<>();

                if (((JSONObject) attr).get("name") instanceof JSONArray) {
                    ((JSONObject) attr).getJSONArray("name").forEach(t -> toHandle.add((String) t));
                } else {
                    toHandle.add(((JSONObject) attr).getString("name"));
                }

                for (String s : toHandle) {
                    GqlRichTextConfigAttribute at = findOrCreateAttributesByAttribute(a, s);

                    if (((JSONObject) attr).has("pattern")) {
                        at.setPattern(((JSONObject) attr).getString("pattern"));
                    }

                    if (((JSONObject) attr).has("elements")) {
                        if (((JSONObject) attr).get("elements") instanceof JSONArray) {
                            ((JSONObject) attr).getJSONArray("elements").forEach(e -> at.getElements().add((String) e));
                        } else {
                            at.getElements().add(((JSONObject) attr).getString("elements"));
                        }
                    }
                }
            });
        }
    }

    private GqlRichTextConfigAttribute findOrCreateAttributesByAttribute(List<GqlRichTextConfigAttribute> attr, String attribute) {
        Optional<GqlRichTextConfigAttribute> opt = attr.stream().filter(a -> attribute.equals(a.getAttribute())).findFirst();

        if (opt.isPresent()) {
            return opt.get();
        }

        GqlRichTextConfigAttribute gqlRichTextConfigAttribute = new GqlRichTextConfigAttribute();
        gqlRichTextConfigAttribute.setAttribute(attribute);
        attr.add(gqlRichTextConfigAttribute);
        return gqlRichTextConfigAttribute;
    }
}
