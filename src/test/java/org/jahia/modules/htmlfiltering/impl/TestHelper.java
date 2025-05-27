package org.jahia.modules.htmlfiltering.impl;

import org.jahia.modules.htmlfiltering.Policy;
import org.jahia.modules.htmlfiltering.model.ElementModel;
import org.jahia.modules.htmlfiltering.model.PolicyModel;
import org.jahia.modules.htmlfiltering.model.RuleSetModel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class TestHelper {

    public static Policy buildCompletePolicy() {
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
        Map<String, Pattern> formatPatterns = new HashMap<>();
        formatPatterns.put("HTML_ID", Pattern.compile("^[a-zA-Z0-9_]+$"));
        formatPatterns.put("NO_GIF", Pattern.compile(".*\\.gif"));
        return ConfigBuilder.buildPolicy(formatPatterns, policyModel, "myPolicy");
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
}
