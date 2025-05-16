package org.jahia.modules.htmlfiltering.validation;

import org.apache.commons.lang3.StringUtils;
import org.jahia.modules.htmlfiltering.*;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Map;
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
        RegistryService registryService = BundleUtils.getOsgiService(RegistryService.class, null);
        NodeValidationResult nodeValidationResult;
        try {
            String siteKey = node.getResolveSite().getSiteKey();
            String workspaceName = node.getSession().getWorkspace().getName();
            Policy policy = registryService.getPolicy(siteKey, workspaceName);
            if (policy == null) {
                logger.debug("No policy available for siteKey: {}, workspace: {}. Validation skipped.", siteKey, workspaceName);
                return true;
            }
            if (!Strategy.REJECT.equals(policy.getStrategy())) {
                logger.debug("The validation is only performed when the strategy is set to REJECT, current one is: {}. Validation skipped.", policy.getStrategy());
                return true;
            }
            nodeValidationResult = policy.validate(node);
        } catch (RepositoryException e) {
            logger.warn("Error while validating node {}", node.getPath(), e);
            return true; // TODO what should we do in this case?
        }
        // build error message
        final StringBuilder errors = new StringBuilder();
        for (Map.Entry<String, RejectionResult> entry : nodeValidationResult.getRejectionResultsByProperty().entrySet()) {
            // Todo provide nicer error messages
            String propertyName = entry.getKey();
            RejectionResult propertyRejectionResult = entry.getValue();
            logger.debug("Sanitized property {}", propertyName);
            errors.append(propertyRejectionResult.getRejectedTags().isEmpty() ? "" : " [" + propertyName + "] Not allowed tags: " + String.join(", ", propertyRejectionResult.getRejectedTags()));

            String rejectedAttributesSummary = propertyRejectionResult.getRejectedAttributesByTag().entrySet().stream()
                    .map(entryRA -> entryRA.getKey() + ": " + String.join(", ", entryRA.getValue()))
                    .collect(Collectors.joining("; "));
            errors.append(propertyRejectionResult.getRejectedAttributesByTag().isEmpty() ? "" : " [" + propertyName + "] Not allowed attributes:" + String.join(", ", rejectedAttributesSummary));
        }
        if (StringUtils.isNotEmpty(errors)) {
            context.buildConstraintViolationWithTemplate(errors.toString()).addConstraintViolation();
        }
        return nodeValidationResult.isValid();
    }
}
