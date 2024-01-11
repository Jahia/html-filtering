package org.jahia.modules.richtext.graphql.query.impl.htmlFiltering;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.richtext.graphql.models.GqlHTMLFiltering;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.sites.JahiaSitesService;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

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
}
