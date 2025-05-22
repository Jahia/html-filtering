package org.jahia.modules.htmlfiltering.validation;

import org.apache.commons.lang3.StringUtils;
import org.jahia.modules.htmlfiltering.*;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
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
        boolean isValid = true;
        try {
            // Resolve policy with strategy: REJECT
            Policy policy = registryService.resolvePolicy(node.getResolveSite().getSiteKey(),
                    node.getSession().getWorkspace().getName(), Strategy.REJECT);
            if (policy == null) {
                return true;
            }

            // Validate properties
            PropertyIterator properties = node.getProperties();
            StringBuilder errors = new StringBuilder();
            while (properties.hasNext()) {
                final Property property = properties.nextProperty();
                String propertyName = property.getName();

                // Only validate property if the policy is applicable to it
                if (policy.isApplicableToProperty(node, propertyName, (ExtendedPropertyDefinition) property.getDefinition())) {
                    if (property.isMultiple()) {
                        Value[] values = property.getValues();
                        for (Value value : values) {
                            isValid = isValid && validatePropertyValue(propertyName, value, policy, errors);
                        }
                    } else {
                        isValid = isValid && validatePropertyValue(propertyName, property.getValue(), policy, errors);
                    }
                }
            }

            // If there are any errors, add them to the context
            if (StringUtils.isNotEmpty(errors)) {
                context.buildConstraintViolationWithTemplate(errors.toString()).addConstraintViolation();
            }
        } catch (RepositoryException e) {
            logger.warn("Error while validating node {}, node will be considered invalid", node.getPath(), e);
            isValid = false;
        }

        return isValid;
    }

    private boolean validatePropertyValue(String propertyName, Value value, Policy policy, StringBuilder errors) throws RepositoryException {
        PolicyExecutionResult policyExecutionResult = policy.execute(value.getString());
        if (!policyExecutionResult.isValid()) {
            collectError(propertyName, errors, policyExecutionResult);
            return false;
        }
        return true;
    }

    private void collectError(String propertyName, StringBuilder errors, PolicyExecutionResult propertyRejectionResult) {
        // TODO nicer error messages ?
        errors.append(propertyRejectionResult.getRejectedTags().isEmpty() ? "" : " [" + propertyName + "] Not allowed tags: " +
                String.join(", ", propertyRejectionResult.getRejectedTags()));
        String rejectedAttributesSummary = propertyRejectionResult.getRejectedAttributesByTag().entrySet().stream()
                .map(entryRA -> entryRA.getKey() + ": " + String.join(", ", entryRA.getValue()))
                .collect(Collectors.joining("; "));
        errors.append(propertyRejectionResult.getRejectedAttributesByTag().isEmpty() ? "" : " [" + propertyName + "] Not allowed attributes:" +
                String.join(", ", rejectedAttributesSummary));
    }
}
