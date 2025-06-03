package org.jahia.modules.htmlfiltering.impl;

import org.jahia.modules.htmlfiltering.model.ConfigModel;
import org.jahia.modules.htmlfiltering.model.ElementModel;
import org.jahia.modules.htmlfiltering.model.PolicyModel;
import org.jahia.modules.htmlfiltering.model.RuleSetModel;

import javax.validation.ConstraintViolation;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestHelper {

    public static ConfigModel buildCompleteConfigModel() {
        PolicyModel policyModel = new PolicyModel();
        policyModel.setStrategy(PolicyModel.PolicyStrategy.REJECT);
        policyModel.setProcess(of("nt:base.*"));
        RuleSetModel allowedRuleSet = new RuleSetModel();
        allowedRuleSet.setElements(of(
                // accept attributes globally
                buildElement(null, of("class", "title"), null),
                // define a format for the 'id' attribute, global to all tags
                buildElement(null, of("id"), "HTML_ID"),
                // allow the 'src' attribute only on <img> tags
                buildElement(of("img"), of("src"), null),
                // allow the 'href' attribute only on <a> tags
                buildElement(of("a"), of("href"), null),
                // allow a few tags globally
                buildElement(of("h1", "h2", "h3", "h4", "h5", "h6", "p", "a", "img", "textarea"), null, null)
        ));
        allowedRuleSet.setProtocols(of("ftps", "https"));
        policyModel.setAllowedRuleSet(allowedRuleSet);
        RuleSetModel disallowedRuleSet = new RuleSetModel();
        disallowedRuleSet.setElements(of(
                // disallow the 'title' attribute on <p> tags:
                buildElement(of("p"), of("title"), null),
                // disallow regex for img src attributes:
                buildElement(of("img"), of("src"), "NO_GIF"), // TODO
                // disallow a few tags globally
                buildElement(of("h5", "h6"), null, null)
        ));
        disallowedRuleSet.setProtocols(of("https"));
        policyModel.setDisallowedRuleSet(disallowedRuleSet);
        Map<String, String> formatPatterns = new HashMap<>();
        formatPatterns.put("HTML_ID", "^[a-zA-Z0-9_]+$");
        formatPatterns.put("NO_GIF", ".*\\.gif");

        // config model with the same policy for edit and live workspaces
        ConfigModel configModel = new ConfigModel();
        configModel.setFormatDefinitions(formatPatterns);
        configModel.setEditWorkspace(policyModel);
        configModel.setLiveWorkspace(policyModel);
        return configModel;
    }

    /**
     * Build a configuration model that is valid but with minimal content.
     *
     * @return a valid configuration model with minimal content
     */
    public static ConfigModel buildConfigModel() {
        return buildConfigModel("p");
    }

    public static ConfigModel buildConfigModel(String... allowedTags) {
        ConfigModel configModel = new ConfigModel();
        configModel.setEditWorkspace(buildPolicyModel(allowedTags));
        configModel.setLiveWorkspace(buildPolicyModel(allowedTags));
        return configModel;
    }

    private static PolicyModel buildPolicyModel(String... allowedTags) {
        PolicyModel policyModel = new PolicyModel();
        RuleSetModel allowedRuleSet = new RuleSetModel();
        allowedRuleSet.setElements(of(buildElement(Arrays.asList(allowedTags), null, null)));
        policyModel.setAllowedRuleSet(allowedRuleSet);
        policyModel.setStrategy(PolicyModel.PolicyStrategy.REJECT);
        policyModel.setProcess(of("nt:base.*"));
        return policyModel;
    }

    public static ElementModel buildElement(List<String> tags, List<String> attributes, String format) {
        ElementModel elementModel = new ElementModel();
        elementModel.setTags(tags);
        elementModel.setAttributes(attributes);
        elementModel.setFormat(format);
        return elementModel;
    }

    // equivalent of List.of(...) only available in Java 9+
    public static <T> List<T> of(T... items) {
        return Arrays.asList(items);
    }

    // equivalent of Set.of(...) only available in Java 9+
    public static <T> Set<T> setOf(T... items) {
        return new HashSet<>(Arrays.asList(items));
    }

    /**
     * Verifies that the provided {@code ValidationConfigurationException} contains exactly one validation error
     * for the specified field and constraint type. This is particularly useful
     * when testing validation failures for built-in constraints with localized error messages.
     *
     * @param configurationException the exception containing the validation errors to be checked
     * @param fieldName              the name of the field expected to have a validation error
     * @param constraintType         the constraint type of the validation error expected for the specified field
     */
    public static void assertContainsExactValidationError(ValidationConfigurationException configurationException, String fieldName, Class<? extends Annotation> constraintType) {
        assertEquals("Only one violation is expected ", 1, configurationException.getViolations().size());
        assertContainsValidationError(configurationException, fieldName, constraintType);
    }

    /**
     * Verifies that the provided {@code ValidationConfigurationException} contains exactly one validation error
     * for the specified field and error message. Ensures that there is only one violation in total
     * and that it matches the given field name and error message. This method is intended for custom constraints
     * that do not have localized error messages.
     *
     * @param configurationException the exception containing the validation errors to be checked
     * @param fieldName              the name of the field expected to have a validation error
     * @param message                the error message expected for the specified field
     */
    public static void assertContainsExactValidationError(ValidationConfigurationException configurationException, String fieldName, String message) {
        assertEquals("Only one violation is expected ", 1, configurationException.getViolations().size());
        assertContainsValidationError(configurationException, fieldName, message);
    }

    public static void assertContainsValidationError(ValidationConfigurationException exception, String fieldName, String message) {
        assertContainsValidationError(
                exception,
                fieldName,
                violation -> violation.getMessage().equals(message),
                String.format("with message '%s'", message)
        );
    }

    public static void assertContainsValidationError(ValidationConfigurationException exception, String fieldName,
                                                     Class<? extends Annotation> constraintType) {
        assertContainsValidationError(
                exception,
                fieldName,
                violation -> violation.getConstraintDescriptor().getAnnotation().annotationType().equals(constraintType),
                String.format("of type '%s'", constraintType)
        );
    }

    private static void assertContainsValidationError(ValidationConfigurationException exception, String fieldName,
                                                      Predicate<ConstraintViolation<?>> violationPredicate, String violationDescription) {
        long matchingViolations = exception.getViolations().stream()
                .filter(violation -> violation.getPropertyPath().toString().equals(fieldName))
                .filter(violationPredicate)
                .count();

        assertTrue(String.format("No violation found for field '%s' %s", fieldName, violationDescription),
                matchingViolations > 0);
        assertEquals(String.format("Expected exactly one violation for field '%s' %s", fieldName, violationDescription),
                1, matchingViolations);
    }
}
