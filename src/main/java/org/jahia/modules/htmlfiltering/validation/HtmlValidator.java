package org.jahia.modules.htmlfiltering.validation;

import org.apache.commons.lang3.StringUtils;
import org.jahia.modules.htmlfiltering.HTMLFilteringService;
import org.jahia.modules.htmlfiltering.SanitizedContent;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        HTMLFilteringService filteringService = BundleUtils.getOsgiService(HTMLFilteringService.class, null);
        if (!filteringService.validate()) {
            // No validation to do
            return true;
        }
        Map<String, SanitizedContent> results = filteringService.sanitizeNode(node);
        if (results == null || results.isEmpty()) {
            return true;
        }
        // build error message
        final StringBuilder errors = new StringBuilder();
        results.forEach((key, content) -> {
            // Todo provide nicer error messages
            logger.debug("Sanitized property {}", key);
            errors.append(content.getRemovedTags().isEmpty() ? "" : " [" + key + "] Not allowed tags: " + String.join(", ", content.getRemovedTags()));
            String removedAttributesString = content.getRemovedAttributes().entrySet().stream()
                    .map(entry -> entry.getKey() + ": " + String.join(", ", entry.getValue()))
                    .collect(Collectors.joining("; "));
            errors.append(content.getRemovedAttributes().isEmpty() ? "" : " [" + key + "] Not allowed attributes:" + String.join(", ", removedAttributesString));
        });
        if (StringUtils.isNotEmpty(errors)) {
            context.buildConstraintViolationWithTemplate(errors.toString()).addConstraintViolation();
            return false;
        }
        return true;
    }
}
