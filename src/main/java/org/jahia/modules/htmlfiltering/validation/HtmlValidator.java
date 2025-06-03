package org.jahia.modules.htmlfiltering.validation;

import org.jahia.modules.htmlfiltering.*;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.utils.i18n.JahiaLocaleContextHolder;
import org.jahia.utils.i18n.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Custom constraint validator
 */
public class HtmlValidator implements ConstraintValidator<HtmlFilteringConstraint, HtmlFilteringValidator> {

    private static final Logger logger = LoggerFactory.getLogger(HtmlValidator.class);

    @Override
    public void initialize(HtmlFilteringConstraint constraintAnnotation) {
        // Do nothing
    }

    @Override
    public boolean isValid(HtmlFilteringValidator nodeValidator, ConstraintValidatorContext context) {
        PolicyResolver policyResolver = BundleUtils.getOsgiService(PolicyResolver.class, null);
        JCRNodeWrapper node = nodeValidator.getNode();
        boolean isValid;
        try {
            // Resolve policy with strategy: REJECT
            Policy policy = policyResolver.resolvePolicy(node.getResolveSite().getSiteKey(),
                    node.getSession().getWorkspace().getName(), Strategy.REJECT);
            if (policy == null) {
                return true;
            }

            // Validate properties
            isValid = validateNodeProperties(node, policy, context);
        } catch (RepositoryException e) {
            logger.warn("Error while validating node {}, node will be considered invalid", node.getPath(), e);
            isValid = false;
        }

        return isValid;
    }

    private boolean validateNodeProperties(JCRNodeWrapper node, Policy policy, ConstraintValidatorContext context) throws RepositoryException {
        boolean isValid = true;
        PropertyIterator properties = node.getProperties();
        while (properties.hasNext()) {
            final Property property = properties.nextProperty();
            String propertyName = property.getName();

            // Only validate property if the policy is applicable to it
            if (policy.isApplicableToProperty(node, propertyName, (ExtendedPropertyDefinition) property.getDefinition())) {
                if (property.isMultiple()) {
                    Value[] values = property.getValues();
                    for (Value value : values) {
                        // Do the evaluation for each value, return the whole result
                        isValid = validatePropertyValue(propertyName, value, policy, context) && isValid;
                    }
                } else {
                    // Do the evaluation for each property, return the whole result
                    isValid =validatePropertyValue(propertyName, property.getValue(), policy, context) && isValid;
                }
            }
        }
        return isValid;
    }

    private boolean validatePropertyValue(String propertyName, Value value, Policy policy, ConstraintValidatorContext context) throws RepositoryException {
        PolicySanitizedHtmlResult policyExecutionResult = policy.sanitize(value.getString());
        if (!policyExecutionResult.isValid()) {
            Locale locale = JahiaLocaleContextHolder.getLocale();
            for (String tag : policyExecutionResult.getRejectedTags()) {
                String errorMessage = Messages.getWithArgs("resources.html-filtering", "htmlFiltering.invalid.tags", locale,  tag);
                context.buildConstraintViolationWithTemplate(errorMessage).addPropertyNode(propertyName).addConstraintViolation();
            }
            for (Map.Entry<String, Set<String>> entry : policyExecutionResult.getRejectedAttributesByTag().entrySet()) {
                for (String tag : entry.getValue()) {
                    String errorMessage = Messages.getWithArgs("resources.html-filtering", "htmlFiltering.invalid.attributes", locale, tag, entry.getKey());
                    context.buildConstraintViolationWithTemplate(errorMessage).addPropertyNode(propertyName).addConstraintViolation();
                }
            }
            // If there are any errors, add them to the context
            return false;
        }
        return true;
    }
}
