package org.jahia.modules.htmlfiltering.impl;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.jahia.modules.htmlfiltering.Policy;
import org.jahia.modules.htmlfiltering.Strategy;
import org.jahia.modules.htmlfiltering.impl.config.Config;
import org.jahia.modules.htmlfiltering.model.ConfigModel;
import org.jahia.modules.htmlfiltering.model.PolicyModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.cm.ConfigurationException;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import static org.jahia.modules.htmlfiltering.impl.ConfigBuilder.buildFromModel;
import static org.jahia.modules.htmlfiltering.impl.TestHelper.assertContainsExactValidationError;
import static org.jahia.modules.htmlfiltering.impl.TestHelper.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit test class for testing the ConfigBuilder functionality, i.e. {@link ConfigBuilder#build(Dictionary)}, to ensure expected behavior
 * during policy creation under various scenarios.
 * <p>Note: It does <b>not</b> test the actual HTML filtering functionality, that should be done in {@link PolicyImplTest}.</p>
 */
@RunWith(JUnitParamsRunner.class)
public class ConfigBuilderTest {

    //--------------------------------
    // validate fields of ConfigModel
    //--------------------------------

    @Test
    public void GIVEN_a_minimal_config_WHEN_building_THEN_success() throws ConfigurationException {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();

        Config config = buildFromModel(configModel);

        assertNotNull(config);
    }

    @Test
    public void GIVEN_a_config_without_edit_workspace_WHEN_building_THEN_validation_error() {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.setEditWorkspace(null);

        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "editWorkspace", "{javax.validation.constraints.NotNull.message}", "must not be null");
    }

    @Test
    public void GIVEN_a_config_without_live_workspace_WHEN_building_THEN_validation_error() {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.setLiveWorkspace(null);

        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "liveWorkspace", "{javax.validation.constraints.NotNull.message}", "must not be null");
    }

    @Test
    @Parameters({
            "",
            "  ",
    })
    public void GIVEN_blank_format_definition_pattern_WHEN_building_THEN_validation_error(String regex) {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        Map<String, String> formatDefinitions = new HashMap<>();
        formatDefinitions.put("MY_FORMAT", regex);
        configModel.setFormatDefinitions(formatDefinitions);

        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "formatDefinitions", "{org.jahia.modules.htmlfiltering.model.validation.constraints.ValidFormatDefinitions.blank.message}", "the value for the format definition of 'MY_FORMAT' must not be blank");
    }

    @Test
    @Parameters({
            "*",
            "(.",
            "abc\\i123",
    })
    public void GIVEN_invalid_format_definition_pattern_WHEN_building_THEN_validation_error(String regex) {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        Map<String, String> formatDefinitions = new HashMap<>();
        formatDefinitions.put("MY_FORMAT", regex);
        configModel.setFormatDefinitions(formatDefinitions);

        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "formatDefinitions", "{org.jahia.modules.htmlfiltering.model.validation.constraints.ValidFormatDefinitions.invalid.message}", "the value for the format definition of 'MY_FORMAT' must be a valid regular expression");
    }

    @Test
    @Parameters({
            "UNDEFINED_FORMAT",
            "sample",
            "Foo",
    })
    public void GIVEN_a_format_not_defined_in_format_definition_WHEN_building_THEN_validation_error(String formatName) {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        Map<String, String> formatDefinitions = new HashMap<>();
        formatDefinitions.put("MY_FORMAT", "\\w*");
        configModel.setFormatDefinitions(formatDefinitions);
        configModel.getEditWorkspace().getAllowedRuleSet().getElements().get(0).setFormat(formatName);
        configModel.getEditWorkspace().getAllowedRuleSet().getElements().get(0).setAttributes(of("id"));

        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "editWorkspace.allowedRuleSet.elements.[0].format", "{org.jahia.modules.htmlfiltering.model.validation.constraints.ValidFormatReference.undefined.message}", "Format '" + formatName + "' not defined under 'formatDefinitions'");
    }

    //--------------------------------
    // validate fields of PolicyModel
    //--------------------------------

    @Test
    public void GIVEN_a_null_strategy_in_edit_workspace_WHEN_building_THEN_validation_error() {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getEditWorkspace().setStrategy(null);

        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "editWorkspace.strategy", "{javax.validation.constraints.NotNull.message}", "must not be null");
    }

    @Test
    public void GIVEN_a_null_strategy_in_live_workspace_WHEN_building_THEN_validation_error() {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getLiveWorkspace().setStrategy(null);

        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "liveWorkspace.strategy", "{javax.validation.constraints.NotNull.message}", "must not be null");
    }

    @Test
    public void GIVEN_a_null_process_in_edit_workspace_WHEN_building_THEN_validation_error() {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getEditWorkspace().setProcess(null);

        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "editWorkspace.process", "{javax.validation.constraints.NotEmpty.message}", "must not be empty");
    }

    @Test
    public void GIVEN_a_null_process_in_live_workspace_WHEN_building_THEN_validation_error() {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getLiveWorkspace().setProcess(null);

        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "liveWorkspace.process", "{javax.validation.constraints.NotEmpty.message}", "must not be empty");
    }

    @Test
    public void GIVEN_an_empty_process_in_edit_workspace_WHEN_building_THEN_validation_error() {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getEditWorkspace().setProcess(of());

        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "editWorkspace.process", "{javax.validation.constraints.NotEmpty.message}", "must not be empty");
    }

    @Test
    public void GIVEN_an_empty_process_in_live_workspace_WHEN_building_THEN_validation_error() {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getLiveWorkspace().setProcess(of());

        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "liveWorkspace.process", "{javax.validation.constraints.NotEmpty.message}", "must not be empty");
    }

    @Test
    public void GIVEN_a_null_allowedRuleSet_in_edit_workspace_WHEN_building_THEN_validation_error() {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getEditWorkspace().setAllowedRuleSet(null);

        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "editWorkspace.allowedRuleSet", "{javax.validation.constraints.NotNull.message}", "must not be null");
    }

    @Test
    public void GIVEN_a_null_allowedRuleSet_in_live_workspace_WHEN_building_THEN_validation_error() {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getLiveWorkspace().setAllowedRuleSet(null);

        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "liveWorkspace.allowedRuleSet", "{javax.validation.constraints.NotNull.message}", "must not be null");
    }

    //--------------------------------
    // validate fields of RuleSetModel
    //--------------------------------

    @Test
    public void GIVEN_null_elements_in_allowedRuleSet_in_edit_workspace_WHEN_building_THEN_validation_error() {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getEditWorkspace().getAllowedRuleSet().setElements(null);

        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "editWorkspace.allowedRuleSet.elements", "{javax.validation.constraints.NotEmpty.message}", "must not be empty");
    }

    @Test
    public void GIVEN_null_elements_in_allowedRuleSet_in_live_workspace_WHEN_building_THEN_validation_error() {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getLiveWorkspace().getAllowedRuleSet().setElements(null);

        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "liveWorkspace.allowedRuleSet.elements", "{javax.validation.constraints.NotEmpty.message}", "must not be empty");
    }

    @Test
    public void GIVEN_empty_elements_in_allowedRuleSet_in_edit_workspace_WHEN_building_THEN_validation_error() {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getEditWorkspace().getAllowedRuleSet().setElements(of());

        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "editWorkspace.allowedRuleSet.elements", "{javax.validation.constraints.NotEmpty.message}", "must not be empty");
    }

    @Test
    public void GIVEN_empty_elements_in_allowedRuleSet_in_live_workspace_WHEN_building_THEN_validation_error() {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getLiveWorkspace().getAllowedRuleSet().setElements(of());

        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "liveWorkspace.allowedRuleSet.elements", "{javax.validation.constraints.NotEmpty.message}", "must not be empty");
    }

    //--------------------------------
    // validate fields of ElementModel
    //--------------------------------

    @Test
    public void GIVEN_element_without_attributes_and_tags_in_edit_workspace_WHEN_building_THEN_validation_error() {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getEditWorkspace().getAllowedRuleSet().getElements().get(0).setTags(Collections.emptyList()); // no tags
        configModel.getEditWorkspace().getAllowedRuleSet().getElements().get(0).setAttributes(Collections.emptyList()); // no attributes

        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "editWorkspace.allowedRuleSet.elements[0]", "{org.jahia.modules.htmlfiltering.model.validation.constraints.RequiresTagsOrAttributes.message}", "must contain 'tags' and/or 'attributes'");
    }

    @Test
    public void GIVEN_element_without_attributes_and_tags_in_live_workspace_WHEN_building_THEN_validation_error() {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getLiveWorkspace().getAllowedRuleSet().getElements().get(0).setTags(Collections.emptyList()); // no tags
        configModel.getLiveWorkspace().getAllowedRuleSet().getElements().get(0).setAttributes(Collections.emptyList()); // no attributes

        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "liveWorkspace.allowedRuleSet.elements[0]", "{org.jahia.modules.htmlfiltering.model.validation.constraints.RequiresTagsOrAttributes.message}", "must contain 'tags' and/or 'attributes'");
    }

    @Test
    public void GIVEN_element_with_tags_and_format_but_no_attributes_in_edit_workspace_WHEN_building_THEN_validation_error() {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        Map<String, String> formatDefinitions = new HashMap<>();
        formatDefinitions.put("MY_FORMAT", "[a-z]*");
        configModel.setFormatDefinitions(formatDefinitions);
        configModel.getEditWorkspace().getAllowedRuleSet().getElements().get(0).setFormat("MY_FORMAT");
        configModel.getEditWorkspace().getAllowedRuleSet().getElements().get(0).setAttributes(Collections.emptyList());
        configModel.getEditWorkspace().getAllowedRuleSet().getElements().get(0).setTags(of("h1", "p"));

        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "editWorkspace.allowedRuleSet.elements[0]", "{org.jahia.modules.htmlfiltering.model.validation.constraints.FormatRequiresAttributes.message}", "'format' must be used with 'attributes'"); // add format value pas param
    }

    @Test
    public void GIVEN_element_with_tags_and_format_but_no_attributes_in_live_workspace_WHEN_building_THEN_validation_error() {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        Map<String, String> formatDefinitions = new HashMap<>();
        formatDefinitions.put("MY_FORMAT", "[a-z]*");
        configModel.setFormatDefinitions(formatDefinitions);
        configModel.getLiveWorkspace().getAllowedRuleSet().getElements().get(0).setFormat("MY_FORMAT");
        configModel.getLiveWorkspace().getAllowedRuleSet().getElements().get(0).setAttributes(Collections.emptyList());
        configModel.getLiveWorkspace().getAllowedRuleSet().getElements().get(0).setTags(of("h1", "p"));

        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "liveWorkspace.allowedRuleSet.elements[0]", "{org.jahia.modules.htmlfiltering.model.validation.constraints.FormatRequiresAttributes.message}", "'format' must be used with 'attributes'");
    }

    @Test
    public void GIVEN_undefined_format_in_edit_workspace_WHEN_building_THEN_validation_error() {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getEditWorkspace().getAllowedRuleSet().getElements().get(0).setAttributes(of("id"));
        configModel.getEditWorkspace().getAllowedRuleSet().getElements().get(0).setFormat("UNDEFINED_FORMAT");


        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "editWorkspace.allowedRuleSet.elements.[0].format", "{org.jahia.modules.htmlfiltering.model.validation.constraints.ValidFormatReference.undefined.message}", "Format 'UNDEFINED_FORMAT' not defined under 'formatDefinitions'");
    }


    @Test
    public void GIVEN_undefined_format_in_live_workspace_WHEN_building_THEN_validation_error() {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getLiveWorkspace().getAllowedRuleSet().getElements().get(0).setAttributes(of("id"));
        configModel.getLiveWorkspace().getAllowedRuleSet().getElements().get(0).setFormat("UNDEFINED_FORMAT");

        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "liveWorkspace.allowedRuleSet.elements.[0].format", "{org.jahia.modules.htmlfiltering.model.validation.constraints.ValidFormatReference.undefined.message}", "Format 'UNDEFINED_FORMAT' not defined under 'formatDefinitions'");
    }

    @Test
    @Parameters({
            "REJECT, REJECT",
            "SANITIZE, SANITIZE",
    })
    public void GIVEN_the_edit_workspace_with_a_specific_strategy_WHEN_building_THEN_the_strategy_matches(PolicyModel.PolicyStrategy strategyModel, Strategy expectedStrategy) throws ConfigurationException {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getEditWorkspace().setStrategy(strategyModel);


        Config config = buildFromModel(configModel);
        Policy policy = config.getEditWorkspacePolicy();

        assertEquals(expectedStrategy, policy.getStrategy());
    }

    @Test
    @Parameters({
            "REJECT, REJECT",
            "SANITIZE, SANITIZE",
    })
    public void GIVEN_the_live_workspace_with_a_specific_strategy_WHEN_building_THEN_the_strategy_matches(PolicyModel.PolicyStrategy strategyModel, Strategy expectedStrategy) throws ConfigurationException {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getLiveWorkspace().setStrategy(strategyModel);

        Config config = buildFromModel(configModel);
        Policy policy = config.getLiveWorkspacePolicy();

        assertEquals(expectedStrategy, policy.getStrategy());
    }

    @Test
    @Parameters({
            "",
            "    ",
            "foo.bar.more",
            "  foo  .  bar .   more",
            "foo.bar.even.more",
    })
    public void GIVEN_an_invalid_process_entry_in_edit_workspace_WHEN_building_THEN_validation_error(String invalidProcessEntry) {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getEditWorkspace().setProcess(of(invalidProcessEntry));

        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "editWorkspace.process[0].<list element>", "{org.jahia.modules.htmlfiltering.model.PolicyModel.nodeTypesProperties.invalidFormat.message}", "must be in format 'nodeType', 'nodeType.*', or 'nodeType.property'");
    }

    @Test
    @Parameters({
            "",
            "    ",
            "foo.bar.more",
            "  foo  .  bar .   more",
            "foo.bar.even.more",
    })
    public void GIVEN_an_invalid_process_entry_in_live_workspace_WHEN_building_THEN_validation_error(String invalidProcessEntry) {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getLiveWorkspace().setProcess(of(invalidProcessEntry));

        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "liveWorkspace.process[0].<list element>", "{org.jahia.modules.htmlfiltering.model.PolicyModel.nodeTypesProperties.invalidFormat.message}", "must be in format 'nodeType', 'nodeType.*', or 'nodeType.property'");
    }

    @Test
    @Parameters({
            "",
            "    ",
            "foo.bar.more",
            "  foo  .  bar .   more",
            "foo.bar.even.more",
    })
    public void GIVEN_an_invalid_skip_entry_in_edit_workspace_WHEN_building_THEN_validation_error(String invalidProcessEntry) {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getEditWorkspace().setSkip(of(invalidProcessEntry));

        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "editWorkspace.skip[0].<list element>", "{org.jahia.modules.htmlfiltering.model.PolicyModel.nodeTypesProperties.invalidFormat.message}", "must be in format 'nodeType', 'nodeType.*', or 'nodeType.property'");
    }

    @Test
    @Parameters({
            "",
            "    ",
            "foo.bar.more",
            "  foo  .  bar .   more",
            "foo.bar.even.more",
    })
    public void GIVEN_an_invalid_skip_entry_in_live_workspace_WHEN_building_THEN_validation_error(String invalidProcessEntry) {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getLiveWorkspace().setSkip(of(invalidProcessEntry));

        ValidationConfigurationException exception = assertThrows(ValidationConfigurationException.class, () -> buildFromModel(configModel));

        assertContainsExactValidationError(exception, "liveWorkspace.skip[0].<list element>", "{org.jahia.modules.htmlfiltering.model.PolicyModel.nodeTypesProperties.invalidFormat.message}", "must be in format 'nodeType', 'nodeType.*', or 'nodeType.property'");
    }

    @Test
    public void GIVEN_a_process_entry_that_overrides_a_wildcard_in_edit_workspace_WHEN_building_THEN_only_wildcard_is_kept() throws ConfigurationException {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getEditWorkspace().setProcess(of("foo.*", "foo.bar"));

        Config config = buildFromModel(configModel);
        PolicyImpl policy = (PolicyImpl) config.getEditWorkspacePolicy();

        assertEquals(1, policy.propsToProcessByNodeType.size());
        assertTrue(policy.propsToProcessByNodeType.containsKey("foo"));
        assertNull(policy.propsToProcessByNodeType.get("foo"));
    }

    @Test
    public void GIVEN_a_process_entry_that_overrides_a_wildcard_in_live_workspace_WHEN_building_THEN_only_wildcard_is_kept() throws ConfigurationException {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getLiveWorkspace().setProcess(of("foo.*", "foo.bar"));

        Config config = buildFromModel(configModel);
        PolicyImpl policy = (PolicyImpl) config.getLiveWorkspacePolicy();

        assertEquals(1, policy.propsToProcessByNodeType.size());
        assertTrue(policy.propsToProcessByNodeType.containsKey("foo"));
        assertNull(policy.propsToProcessByNodeType.get("foo"));
    }

    @Test
    public void GIVEN_a_wildcard_process_entry_that_overrides_an_existing_entry_in_edit_workspace_WHEN_building_THEN_only_wildcard_is_kept() throws ConfigurationException {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getEditWorkspace().setProcess(of("foo.bar", "foo.*"));

        Config config = buildFromModel(configModel);
        PolicyImpl policy = (PolicyImpl) config.getEditWorkspacePolicy();

        assertEquals(1, policy.propsToProcessByNodeType.size());
        assertTrue(policy.propsToProcessByNodeType.containsKey("foo"));
        assertNull(policy.propsToProcessByNodeType.get("foo"));
    }

    @Test
    public void GIVEN_a_skip_entry_with_wildcard_that_overrides_one_with_a_prop_in_edit_workspace_WHEN_building_THEN_only_wildcard_is_kept() throws ConfigurationException {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getEditWorkspace().setSkip(of("foo.*", "foo.bar"));

        Config config = buildFromModel(configModel);
        PolicyImpl policy = (PolicyImpl) config.getEditWorkspacePolicy();

        assertEquals(1, policy.propsToSkipByNodeType.size());
        assertTrue(policy.propsToSkipByNodeType.containsKey("foo"));
        assertNull(policy.propsToSkipByNodeType.get("foo"));
    }

    @Test
    public void GIVEN_a_skip_entry_with_wildcard_that_overrides_one_with_a_prop_in_live_workspace_WHEN_building_THEN_only_wildcard_is_kept() throws ConfigurationException {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getLiveWorkspace().setSkip(of("foo.*", "foo.bar"));

        Config config = buildFromModel(configModel);
        PolicyImpl policy = (PolicyImpl) config.getLiveWorkspacePolicy();

        assertEquals(1, policy.propsToSkipByNodeType.size());
        assertTrue(policy.propsToSkipByNodeType.containsKey("foo"));
        assertNull(policy.propsToSkipByNodeType.get("foo"));
    }

    @Test
    public void GIVEN_a_mix_of_process_and_skip_entries_in_edit_workspace_WHEN_building_THEN_propsByNodeType_maps_are_populated_correctly() throws ConfigurationException {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getEditWorkspace().setProcess(of("nt:base.jcr:description", "myNt:myOtherNode.myFirstProp", "myNt:myOtherNode.mySecondProp", "myOtherNt:myMixin.*", "foo"));
        configModel.getEditWorkspace().setSkip(of("myNt:myNode.propToSkip", "bar", "myNt:foo.*"));

        Config config = buildFromModel(configModel);
        PolicyImpl policy = (PolicyImpl) config.getEditWorkspacePolicy();

        assertEquals(4, policy.propsToProcessByNodeType.size());
        assertEquals(TestHelper.setOf("jcr:description"), policy.propsToProcessByNodeType.get("nt:base"));
        assertEquals(TestHelper.setOf("myFirstProp", "mySecondProp"), policy.propsToProcessByNodeType.get("myNt:myOtherNode"));
        assertNull(policy.propsToProcessByNodeType.get("myOtherNt:myMixin"));
        assertNull(policy.propsToProcessByNodeType.get("foo"));
        assertEquals(3, policy.propsToSkipByNodeType.size());
        assertEquals(TestHelper.setOf("propToSkip"), policy.propsToSkipByNodeType.get("myNt:myNode"));
        assertNull(policy.propsToSkipByNodeType.get("bar"));
        assertNull(policy.propsToSkipByNodeType.get("myNt:foo"));
    }


    @Test
    public void GIVEN_a_mix_of_process_and_skip_entries_in_live_workspace_WHEN_building_THEN_propsByNodeType_maps_are_populated_correctly() throws ConfigurationException {
        ConfigModel configModel = TestHelper.buildBasicConfigModel();
        configModel.getEditWorkspace().setProcess(of("nt:base.jcr:description", "myNt:myOtherNode.myFirstProp", "myNt:myOtherNode.mySecondProp", "myOtherNt:myMixin.*", "foo"));
        configModel.getEditWorkspace().setSkip(of("myNt:myNode.propToSkip", "bar", "myNt:foo.*"));

        Config config = buildFromModel(configModel);
        PolicyImpl policy = (PolicyImpl) config.getEditWorkspacePolicy();

        assertEquals(4, policy.propsToProcessByNodeType.size());
        assertEquals(TestHelper.setOf("jcr:description"), policy.propsToProcessByNodeType.get("nt:base"));
        assertEquals(TestHelper.setOf("myFirstProp", "mySecondProp"), policy.propsToProcessByNodeType.get("myNt:myOtherNode"));
        assertNull(policy.propsToProcessByNodeType.get("myOtherNt:myMixin"));
        assertNull(policy.propsToProcessByNodeType.get("foo"));
        assertEquals(3, policy.propsToSkipByNodeType.size());
        assertEquals(TestHelper.setOf("propToSkip"), policy.propsToSkipByNodeType.get("myNt:myNode"));
        assertNull(policy.propsToSkipByNodeType.get("bar"));
        assertNull(policy.propsToSkipByNodeType.get("myNt:foo"));
    }
}