package org.jahia.modules.htmlfiltering.validation;

import org.jahia.modules.htmlfiltering.HTMLFilteringService;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.owasp.html.HtmlChangeListener;
import org.owasp.html.PolicyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.jcr.RepositoryException;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Custom constraint validator
 */
public class HtmlValidator implements ConstraintValidator<HtmlFilteringConstraint, JCRNodeWrapper> {

    private final static Logger logger = LoggerFactory.getLogger(HtmlValidator.class);

    @Override
    public void initialize(HtmlFilteringConstraint constraintAnnotation) {
        // Do nothing
    }

    @Override
    public boolean isValid(JCRNodeWrapper node, ConstraintValidatorContext context) {
        logger.info("Validating node {}", node.getPath());
        JCRSiteNode resolveSite = null;
        try {
            resolveSite = node.getResolveSite();
        } catch (RepositoryException e) {
            logger.warn("Unable to resolve site", e);
        }
        HTMLFilteringService filteringService = BundleUtils.getOsgiService(HTMLFilteringService.class, null);
        String toValidate = node.getPropertyAsString("text");
        if (resolveSite == null || !resolveSite.isHtmlMarkupFilteringEnabled() || filteringService == null || toValidate == null) {
            return true;
        }

        PolicyFactory policyFactory = filteringService.getMergedOwaspPolicyFactory(HTMLFilteringService.DEFAULT_POLICY_KEY, resolveSite.getSiteKey());

        if (policyFactory == null) {
            return true;
        }
        Set<String> removedTags = new HashSet<>();
        Set<String> removedElements = new HashSet<>();

        String h = policyFactory.sanitize(toValidate, new HtmlChangeListener<Object>() {
            @Override
            public void discardedTag(@Nullable Object o, String tag) {
                removedTags.add(tag);
            }

            @Override
            public void discardedAttributes(@Nullable Object o, String tag, String... attrs) {
                removedElements.addAll(Arrays.stream(attrs).map(attr -> tag + "." + attr).collect(Collectors.toList()));
            }
        }, null);
        String errors = removedTags.isEmpty() ? "" : "Not allowed tags: " + String.join(", ", removedTags);
        errors += removedElements.isEmpty() ? "" : " Not allowed attributes:"+ String.join(", ", removedElements);
        if (removedTags.size() + removedElements.size() > 0) {
            context.buildConstraintViolationWithTemplate(errors).addConstraintViolation();
            return false;
        }
        return  true;
    }
}
