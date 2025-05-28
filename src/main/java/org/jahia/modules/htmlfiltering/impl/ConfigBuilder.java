package org.jahia.modules.htmlfiltering.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jahia.modules.htmlfiltering.Policy;
import org.jahia.modules.htmlfiltering.Strategy;
import org.jahia.modules.htmlfiltering.impl.config.Config;
import org.jahia.modules.htmlfiltering.model.ConfigModel;
import org.jahia.modules.htmlfiltering.model.ElementModel;
import org.jahia.modules.htmlfiltering.model.PolicyModel;
import org.jahia.modules.htmlfiltering.model.RuleSetModel;
import org.osgi.service.cm.ConfigurationException;
import org.owasp.html.HtmlPolicyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Builder class for creating an {@link Config} object from a set of properties.
 * It filters the properties to only include those starting with "htmlFiltering." and converts them
 * into a configuration object.
 */
public class ConfigBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ConfigBuilder.class);

    private ConfigBuilder() {
    }

    private static final JavaPropsMapper javaPropsMapper = JavaPropsMapper.builder()
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
            .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
            .build();

    /**
     * Builds a {@link Config} object from the given properties.
     *
     * @param properties the properties to build the configuration from
     * @return the built configuration
     * @throws ConfigurationException if an error occurs while building the configuration
     */
    public static Config build(Dictionary<String, ?> properties) throws ConfigurationException {
        // convert the dictionary to a map and keep only the props starting with "htmlFiltering."
        try {
            List<String> keys = Collections.list(properties.keys());
            Map<String, Object> propertiesMap = keys.stream()
                    .filter(key -> key.startsWith("htmlFiltering."))
                    .collect(Collectors.toMap(k -> StringUtils.substringAfter(k, "htmlFiltering."), properties::get));

            // convert the map to a configuration object
            String propertiesMapAsString = javaPropsMapper.writeValueAsString(propertiesMap);
            ConfigModel configModel = javaPropsMapper.readValue(propertiesMapAsString, ConfigModel.class);

            logger.debug("html-filtering configuration model loaded: {}", configModel);
            return buildFromModel(configModel);
        } catch (Exception e) {
            // Globally catch any exceptions happening during the building process
            logger.debug("Unable to build html-filtering configuration due to an exception. Properties: {}", properties);
            throw new ConfigurationException(null, "Unable to build html-filtering configuration", e);
        }
    }

    /**
     * Builds a valid {@link Config} object from the given {@link ConfigModel}. This method validates the
     * provided {@link ConfigModel}, compiles format definitions into regex patterns, and generates
     * policies for the edit and live workspaces.
     *
     * @param configModel the {@link ConfigModel} containing the configuration details used to build the {@link Config}
     * @return a {@link Config} instance populated with the policies derived from the provided {@link ConfigModel}
     * @throws ConfigurationException if validation of the {@link ConfigModel} fails or an error occurs during configuration building
     */
    static Config buildFromModel(ConfigModel configModel) throws ConfigurationException {
        validate(configModel);
        // compile the regex patterns for the allowed and disallowed rule sets (shared for both workspaces)
        Map<String, Pattern> formatPatterns = new HashMap<>();
        if (configModel.getFormatDefinitions() != null) {
            configModel.getFormatDefinitions().forEach((formatName, formatRegex) -> {
                formatPatterns.put(formatName, Pattern.compile(formatRegex));
                logger.debug("Compiled format {} regex: {}", formatName, formatRegex);
            });
        }

        return new Config(buildPolicy(formatPatterns, configModel.getEditWorkspace()),
                buildPolicy(formatPatterns, configModel.getLiveWorkspace()));
    }

    private static void validate(ConfigModel configModel) throws ConfigurationException {
        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = validatorFactory.getValidator();
            Set<ConstraintViolation<ConfigModel>> violations = validator.validate(configModel);
            if (!violations.isEmpty()) {
                throw new ValidationConfigurationException(violations);
            }
        }
    }

    static Policy buildPolicy(Map<String, Pattern> formatPatterns, PolicyModel policyModel) {

        // Configure OWASP
        HtmlPolicyBuilder builder = new HtmlPolicyBuilder();
        processRuleSet(builder, policyModel.getAllowedRuleSet(), formatPatterns,
                HtmlPolicyBuilder::allowAttributes, HtmlPolicyBuilder::allowElements, HtmlPolicyBuilder::allowTextIn, HtmlPolicyBuilder::allowUrlProtocols);
        processRuleSet(builder, policyModel.getDisallowedRuleSet(), formatPatterns,
                HtmlPolicyBuilder::disallowAttributes, HtmlPolicyBuilder::disallowElements, HtmlPolicyBuilder::disallowTextIn, HtmlPolicyBuilder::disallowUrlProtocols);

        return new PolicyImpl(readStrategy(policyModel),
                createPropsByNodeType(policyModel.getProcess(), "process"),
                createPropsByNodeType(policyModel.getSkip(), "skip"),
                policyModel.getSkipOnPermissions() != null ? policyModel.getSkipOnPermissions() : Collections.emptyList(),
                builder.toFactory());
    }

    private static Map<String, Set<String>> createPropsByNodeType(List<String> propsByNodeType, String configSectionName) {
        Map<String, Set<String>> result = new HashMap<>();
        if (propsByNodeType != null) {
            for (String nodeTypeProperty : propsByNodeType) {
                String[] parts = StringUtils.split(nodeTypeProperty, '.');
                switch (parts.length) {
                    case 1:
                        setWildcardEntryForNodeType(result, parts[0], configSectionName);
                        break;
                    case 2:
                        String nodeType = parts[0];
                        String propertyPattern = parts[1];
                        setPropertyPatternEntryForNodeType(propertyPattern, result, nodeType, configSectionName);
                        break;
                    default:
                        // should not happen as the configuration is validated beforehand
                        throw new IllegalStateException(String.format("Invalid format for item '%s' in '%s'. Expected format is 'nodeType.property' or 'nodeType.*'", nodeTypeProperty, configSectionName));
                }
            }
        }

        return result;
    }

    // Suppress Sonar warning regarding the Map.containsKey() usage, as it is used to check for wildcard entries
    @SuppressWarnings("java:S3824")
    private static void setPropertyPatternEntryForNodeType(String propertyPattern, Map<String, Set<String>> result, String nodeType, String configSectionName) {
        if (propertyPattern.equals("*")) {
            setWildcardEntryForNodeType(result, nodeType, configSectionName);
        } else {
            Set<String> properties = result.get(nodeType);
            if (properties == null) {
                if (result.containsKey(nodeType)) {
                    logger.warn("There is already a wildcard entry for the node type {} under '{}'. Ignoring the property '{}'", nodeType, configSectionName, propertyPattern);
                    return;
                }
                properties = new HashSet<>();
                result.put(nodeType, properties);
            }
            properties.add(propertyPattern);
        }
    }

    private static void setWildcardEntryForNodeType(Map<String, Set<String>> result, String nodeType, String configSectionName) {
        if (result.containsKey(nodeType)) {
            logger.warn("There is already an entry for the node type {} under '{}'. Overwriting it with the wildcard", nodeType, configSectionName);
        }
        // Wildcard pattern: all properties are to be processed for this node type
        result.put(nodeType, null);
    }

    private static Strategy readStrategy(PolicyModel policyModel) {
        switch (policyModel.getStrategy()) {
            case REJECT:
                return Strategy.REJECT;
            case SANITIZE:
                return Strategy.SANITIZE;
            default:
                // should not happen as the configuration is validated beforehand
                throw new IllegalStateException(String.format("Unknown 'strategy':%s ", policyModel.getStrategy()));
        }
    }

    private static void processRuleSet(HtmlPolicyBuilder builder, RuleSetModel ruleSet,
                                       Map<String, Pattern> formatPatterns,
                                       AttributeBuilderHandlerFunction attributeBuilderHandlerFunction,
                                       BuilderHandlerFunction tagHandler, BuilderHandlerFunction textContentHandler, BuilderHandlerFunction protocolHandler) {
        if (ruleSet != null) {
            // Apply element rules
            for (ElementModel element : ruleSet.getElements()) {
                processElement(builder, formatPatterns, attributeBuilderHandlerFunction, tagHandler, textContentHandler, element);
            }

            // Apply protocol rules
            if (ruleSet.getProtocols() != null) {
                protocolHandler.handle(builder, ruleSet.getProtocols().toArray(new String[0]));
            }
        }
    }

    private static void processElement(HtmlPolicyBuilder builder, Map<String, Pattern> formatPatterns,
                                       AttributeBuilderHandlerFunction attributeBuilderHandlerFunction,
                                       BuilderHandlerFunction tagHandler, BuilderHandlerFunction textContentHandler, ElementModel element) {
        boolean noTags = CollectionUtils.isEmpty(element.getTags());
        boolean noAttributes = CollectionUtils.isEmpty(element.getAttributes());
        if (noAttributes) {
            tagHandler.handle(builder, element.getTags().toArray(new String[0]));
            textContentHandler.handle(builder, element.getTags().toArray(new String[0]));
        } else {
            HtmlPolicyBuilder.AttributeBuilder attributeBuilder =
                    attributeBuilderHandlerFunction.handle(builder, element.getAttributes().toArray(new String[0]));

            // Handle format pattern for allowed attributes only
            if (element.getFormat() != null) {
                Pattern formatPattern = formatPatterns.get(element.getFormat());
                attributeBuilder.matching(formatPattern);
            }

            if (noTags) {
                // The attributes are for all tags
                attributeBuilder.globally();
            } else {
                attributeBuilder.onElements(element.getTags().toArray(new String[0]));
            }
        }
    }

    @FunctionalInterface
    private interface AttributeBuilderHandlerFunction {
        HtmlPolicyBuilder.AttributeBuilder handle(HtmlPolicyBuilder builder, String[] attributes);
    }

    @FunctionalInterface
    private interface BuilderHandlerFunction {
        void handle(HtmlPolicyBuilder builder, String[] items);
    }
}
