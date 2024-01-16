package org.jahia.modules.richtext.graphql.mutation.impl.htmlFiltering;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.richtext.RichTextConfigurationInterface;
import org.jahia.modules.richtext.graphql.models.GqlHTMLFilteringRemovedAttributes;
import org.jahia.modules.richtext.graphql.models.GqlHTMLFilteringTest;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
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
            return JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Boolean>() {
                @Override
                public Boolean doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    JCRNodeWrapper siteNode = session.getNode("/sites/" + siteKey);
                    siteNode.setProperty("j:doTagFiltering", true);
                    session.save();

                    return true;
                }
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
            return JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Boolean>() {
                @Override
                public Boolean doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    JCRNodeWrapper siteNode = session.getNode("/sites/" + siteKey);
                    siteNode.setProperty("j:doTagFiltering", false);
                    session.save();

                    return true;
                }
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
        RichTextConfigurationInterface filteringConfig = BundleUtils.getOsgiService(RichTextConfigurationInterface.class, null);
        GqlHTMLFilteringTest gqlHTMLFilteringTest = new GqlHTMLFilteringTest();

        if (filteringConfig == null) {
            gqlHTMLFilteringTest.setHtml(html);
            return gqlHTMLFilteringTest;
        }

        PolicyFactory policyFactory = filteringConfig.getMergedOwaspPolicyFactory(RichTextConfigurationInterface.DEFAULT_POLICY_KEY, siteKey);

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
