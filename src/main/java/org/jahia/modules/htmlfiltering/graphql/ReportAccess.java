package org.jahia.modules.htmlfiltering.graphql;

import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.sites.JahiaSitesService;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;

/**
 * Checks that the current user may view, run and export the HTML filtering report of a site.
 */
public final class ReportAccess {

    public static final String PERMISSION = "viewHtmlFilteringReport";

    private ReportAccess() {
    }

    /**
     * @return the key of the site, once the current user is known to hold the permission on the site
     * @throws DataFetchingException if the site does not exist, or if the current user does not hold the permission on it
     */
    public static String checkSite(String siteKey) {
        try {
            JCRSiteNode site = JahiaSitesService.getInstance().getSiteByKey(siteKey,
                    JCRSessionFactory.getInstance().getCurrentUserSession());
            if (site == null || !site.hasPermission(PERMISSION)) {
                throw new AccessDeniedException("User does not have permission '" + PERMISSION + "' on the site " + siteKey);
            }
            return site.getSiteKey();
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }
}
