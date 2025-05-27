package org.jahia.modules.htmlfiltering.impl;

import org.jahia.modules.htmlfiltering.model.ConfigModel;
import org.jahia.modules.htmlfiltering.model.ElementModel;
import org.jahia.modules.htmlfiltering.model.PolicyModel;
import org.jahia.modules.htmlfiltering.model.RuleSetModel;
import org.osgi.service.cm.ConfigurationException;

import javax.validation.ConstraintViolation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * @return a configuration model with minimal content
     */
    public static ConfigModel buildBasicConfigModel() {
        return buildBasicConfigModel("p");
    }

    public static ConfigModel buildBasicConfigModel(String... tags) {
        ConfigModel configModel = new ConfigModel();
        configModel.setEditWorkspace(buildMinimalPolicyModel(tags));
        configModel.setLiveWorkspace(buildMinimalPolicyModel(tags));
        return configModel;
    }

    private static PolicyModel buildMinimalPolicyModel(String... tags) {
        PolicyModel policyModel = new PolicyModel();
        RuleSetModel allowedRuleSet = new RuleSetModel();
        allowedRuleSet.setElements(of(buildElement(Arrays.asList(tags), null, null)));
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

    // TODO review signature
    public static void assertContainsValidationError(ConfigurationException configurationException, String fieldName, String error) {
        String message = configurationException.getMessage();
        // TODO review based on final impl
        assertTrue(String.format("Expected '%s' to contain the error '%s' for the field '%s'", message, error, fieldName), message.contains(String.format("- %s: %s", fieldName, error)));
    }

    public static void assertContainsExactValidationError(ValidationConfigurationException configurationException, String fieldName, String template, String message) {
        assertEquals("Only one violation is expected ", 1, configurationException.getViolations().size());
        ConstraintViolation<ConfigModel> violation = configurationException.getViolations().iterator().next();
        assertEquals(fieldName, violation.getPropertyPath().toString());
        assertEquals(template, violation.getMessageTemplate());
        assertEquals(message, violation.getMessage());

    }
}
