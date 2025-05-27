package org.jahia.modules.htmlfiltering.impl;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.jahia.modules.htmlfiltering.Policy;
import org.jahia.modules.htmlfiltering.Strategy;
import org.jahia.modules.htmlfiltering.model.ElementModel;
import org.jahia.modules.htmlfiltering.model.PolicyModel;
import org.jahia.modules.htmlfiltering.model.RuleSetModel;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.Dictionary;

import static org.jahia.modules.htmlfiltering.impl.TestHelper.buildElement;
import static org.jahia.modules.htmlfiltering.impl.TestHelper.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

/**
 * Unit test class for testing the ConfigBuilder functionality, i.e. {@link ConfigBuilder#build(Dictionary)}, to ensure expected behavior
 * during policy creation under various scenarios.
 * <p>Note: It does <b>not</b> test the actual HTML filtering functionality, that should be done in {@link PolicyImplTest}.</p>
 */
@RunWith(JUnitParamsRunner.class)
public class ConfigBuilderTest {

    @Test
    @Parameters({
            "myPolicy",
            "foo",
    })
    public void GIVEN_a_null_policy_model_WHEN_creating_policy_THEN_exception_is_thrown(String policyName) {

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ConfigBuilder.buildPolicy(Collections.emptyMap(), null, policyName));

        assertEquals("'" + policyName + "' is not set", exception.getMessage());
    }

    @Test
    public void GIVEN_a_null_strategy_WHEN_creating_policy_THEN_exception_is_thrown() {

        PolicyModel policyModel = new PolicyModel();
        policyModel.setStrategy(null); // null 'strategy'
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ConfigBuilder.buildPolicy(Collections.emptyMap(), policyModel, "myPolicy"));

        assertEquals("'strategy' is not set", exception.getMessage());
    }

    @Test
    public void GIVEN_a_null_process_WHEN_creating_policy_THEN_exception_is_thrown() {

        PolicyModel policyModel = new PolicyModel();
        policyModel.setStrategy(PolicyModel.PolicyStrategy.REJECT);
        policyModel.setProcess(null); // null 'process'
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ConfigBuilder.buildPolicy(Collections.emptyMap(), policyModel, "myPolicy"));

        assertEquals("'process' is not set", exception.getMessage());
    }

    @Test
    public void GIVEN_an_empty_process_WHEN_creating_policy_THEN_exception_is_thrown() {

        PolicyModel policyModel = new PolicyModel();
        policyModel.setStrategy(PolicyModel.PolicyStrategy.REJECT);
        policyModel.setProcess(of()); // empty 'process'
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ConfigBuilder.buildPolicy(Collections.emptyMap(), policyModel, "myPolicy"));

        assertEquals("'process' is not set", exception.getMessage());
    }

    @Test
    public void GIVEN_a_null_allowedRuleSet_WHEN_creating_policy_THEN_exception_is_thrown() {

        PolicyModel policyModel = new PolicyModel();
        policyModel.setStrategy(PolicyModel.PolicyStrategy.REJECT);
        policyModel.setProcess(of("nt:base.*"));
        policyModel.setAllowedRuleSet(null); // null 'allowedRuleSet'
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ConfigBuilder.buildPolicy(Collections.emptyMap(), policyModel, "myPolicy"));

        assertEquals("'allowedRuleSet' is not set", exception.getMessage());
    }

    @Test
    public void GIVEN_null_elements_in_allowedRuleSet_WHEN_creating_policy_THEN_exception_is_thrown() {

        PolicyModel policyModel = new PolicyModel();
        policyModel.setStrategy(PolicyModel.PolicyStrategy.REJECT);
        policyModel.setProcess(of("nt:base.*"));
        RuleSetModel allowedRuleSet = new RuleSetModel();
        allowedRuleSet.setElements(null);// null 'elements'
        policyModel.setAllowedRuleSet(allowedRuleSet);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ConfigBuilder.buildPolicy(Collections.emptyMap(), policyModel, "myPolicy"));

        assertEquals("At least one item in 'elements' must be defined", exception.getMessage());
    }

    @Test
    public void GIVEN_empty_elements_in_allowedRuleSet_WHEN_creating_policy_THEN_exception_is_thrown() {

        PolicyModel policyModel = new PolicyModel();
        policyModel.setStrategy(PolicyModel.PolicyStrategy.REJECT);
        policyModel.setProcess(of("nt:base.*"));
        RuleSetModel allowedRuleSet = new RuleSetModel();
        allowedRuleSet.setElements(of()); // empty 'elements'
        policyModel.setAllowedRuleSet(allowedRuleSet);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ConfigBuilder.buildPolicy(Collections.emptyMap(), policyModel, "myPolicy"));

        assertEquals("At least one item in 'elements' must be defined", exception.getMessage());
    }

    @Test
    public void GIVEN_element_without_attributes_and_tags_WHEN_creating_policy_THEN_exception_is_thrown() {
        PolicyModel policyModel = new PolicyModel();
        policyModel.setStrategy(PolicyModel.PolicyStrategy.REJECT);
        policyModel.setProcess(of("nt:base.*"));
        RuleSetModel allowedRuleSet = new RuleSetModel();
        ElementModel element = new ElementModel();
        element.setTags(Collections.emptyList()); // no tags
        element.setAttributes(Collections.emptyList()); // no attributes
        allowedRuleSet.setElements(of(element));
        policyModel.setAllowedRuleSet(allowedRuleSet);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ConfigBuilder.buildPolicy(Collections.emptyMap(), policyModel, "myPolicy"));

        assertEquals("Each item in 'elements' of 'allowedRuleSet' / 'disallowedRuleSet' must contain 'tags' and/or 'attributes'. Item: " + element, exception.getMessage());
    }

    @Test
    public void GIVEN_element_with_tags_and_format_WHEN_creating_policy_THEN_exception_is_thrown() {
        PolicyModel policyModel = new PolicyModel();
        policyModel.setStrategy(PolicyModel.PolicyStrategy.SANITIZE);
        policyModel.setProcess(of("nt:base.*"));
        RuleSetModel allowedRuleSet = new RuleSetModel();
        policyModel.setAllowedRuleSet(allowedRuleSet);
        ElementModel element = new ElementModel();
        element.setTags(of("h1", "p"));
        element.setAttributes(Collections.emptyList()); // no attributes
        element.setFormat("MY_FORMAT");
        allowedRuleSet.setElements(of(element));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ConfigBuilder.buildPolicy(Collections.emptyMap(), policyModel, "myPolicy"));

        assertEquals("'format' can only be used with 'attributes'. Item: " + element, exception.getMessage());
    }

    @Test
    public void GIVEN_the_use_of_format_not_defined_WHEN_creating_policy_THEN_exception_is_thrown() {
        PolicyModel policyModel = new PolicyModel();
        policyModel.setStrategy(PolicyModel.PolicyStrategy.SANITIZE);
        policyModel.setProcess(of("nt:base.*"));
        RuleSetModel allowedRuleSet = new RuleSetModel();
        policyModel.setAllowedRuleSet(allowedRuleSet);
        allowedRuleSet.setElements(of(
                buildElement(null, of("id"), "UNDEFINED_FORMAT")
        ));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ConfigBuilder.buildPolicy(Collections.emptyMap(), policyModel, "myPolicy"));

        assertEquals("Format 'UNDEFINED_FORMAT' not defined, check your configuration", exception.getMessage());
    }


    @Test
    @Parameters({
            "REJECT, REJECT",
            "SANITIZE, SANITIZE",
    })
    public void GIVEN_a_workspace_with_a_specific_strategy_WHEN_creating_policy_THEN_the_strategy_matches(PolicyModel.PolicyStrategy strategyModel, Strategy expectedStrategy) {
        PolicyModel policyModel = new PolicyModel();
        policyModel.setStrategy(strategyModel);
        policyModel.setProcess(TestHelper.of("nt:base.*"));
        policyModel.setAllowedRuleSet(new RuleSetModel());
        policyModel.getAllowedRuleSet().setElements(TestHelper.of(
                TestHelper.buildElement(TestHelper.of("p"), null, null)
        ));

        Policy policy = ConfigBuilder.buildPolicy(Collections.emptyMap(), policyModel, "myPolicy");

        assertEquals(expectedStrategy, policy.getStrategy());
    }


    @Test
    public void GIVEN_an_empty_process_entry_WHEN_creating_policy_THEN_exception_is_thrown() {
        PolicyModel policyModel = new PolicyModel();
        policyModel.setStrategy(PolicyModel.PolicyStrategy.SANITIZE);
        policyModel.setProcess(TestHelper.of(""));
        RuleSetModel allowedRuleSet = new RuleSetModel();
        policyModel.setAllowedRuleSet(allowedRuleSet);
        allowedRuleSet.setElements(TestHelper.of(
                TestHelper.buildElement(null, TestHelper.of("id"), null)
        ));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ConfigBuilder.buildPolicy(Collections.emptyMap(), policyModel, "myPolicy"));

        assertEquals("Each item in 'process' must be set and not empty", exception.getMessage());
    }

    @Test
    public void GIVEN_an_empty_skip_entry_WHEN_creating_policy_THEN_exception_is_thrown() {
        PolicyModel policyModel = new PolicyModel();
        policyModel.setStrategy(PolicyModel.PolicyStrategy.SANITIZE);
        policyModel.setProcess(TestHelper.of("nt:base"));
        policyModel.setSkip(TestHelper.of(""));
        RuleSetModel allowedRuleSet = new RuleSetModel();
        policyModel.setAllowedRuleSet(allowedRuleSet);
        allowedRuleSet.setElements(TestHelper.of(
                TestHelper.buildElement(null, TestHelper.of("id"), null)
        ));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ConfigBuilder.buildPolicy(Collections.emptyMap(), policyModel, "myPolicy"));

        assertEquals("Each item in 'skip' must be set and not empty", exception.getMessage());
    }

    @Test
    @Parameters({
            "foo.bar.more",
            "  foo  .  bar .   more",
            "foo.bar.even.more",
    })
    public void GIVEN_an_invalid_process_entry_WHEN_creating_policy_THEN_exception_is_thrown(String invalidProcessEntry) {
        PolicyModel policyModel = new PolicyModel();
        policyModel.setStrategy(PolicyModel.PolicyStrategy.SANITIZE);
        policyModel.setProcess(TestHelper.of(invalidProcessEntry));
        RuleSetModel allowedRuleSet = new RuleSetModel();
        policyModel.setAllowedRuleSet(allowedRuleSet);
        allowedRuleSet.setElements(TestHelper.of(
                TestHelper.buildElement(null, TestHelper.of("id"), null)
        ));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ConfigBuilder.buildPolicy(Collections.emptyMap(), policyModel, "myPolicy"));

        assertEquals("Invalid format for item '" + invalidProcessEntry + "' in 'process'. Expected format is 'nodeType.property' or 'nodeType.*'", exception.getMessage());
    }

    @Test
    @Parameters({
            "foo.bar.more",
            "  foo  .  bar .   more",
            "foo.bar.even.more",
    })
    public void GIVEN_an_invalid_skip_entry_WHEN_creating_policy_THEN_exception_is_thrown(String invalidProcessEntry) {
        PolicyModel policyModel = new PolicyModel();
        policyModel.setStrategy(PolicyModel.PolicyStrategy.SANITIZE);
        policyModel.setProcess(TestHelper.of("nt:base"));
        policyModel.setSkip(TestHelper.of(invalidProcessEntry));
        RuleSetModel allowedRuleSet = new RuleSetModel();
        policyModel.setAllowedRuleSet(allowedRuleSet);
        allowedRuleSet.setElements(TestHelper.of(
                TestHelper.buildElement(null, TestHelper.of("id"), null)
        ));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ConfigBuilder.buildPolicy(Collections.emptyMap(), policyModel, "myPolicy"));

        assertEquals("Invalid format for item '" + invalidProcessEntry + "' in 'skip'. Expected format is 'nodeType.property' or 'nodeType.*'", exception.getMessage());
    }


    @Test
    public void GIVEN_a_process_entry_that_overrides_a_wildcard_WHEN_creating_policy_THEN_only_wildcard_is_kept() {
        PolicyModel policyModel = new PolicyModel();
        policyModel.setStrategy(PolicyModel.PolicyStrategy.SANITIZE);
        policyModel.setProcess(TestHelper.of("foo.*", "foo.bar"));
        RuleSetModel allowedRuleSet = new RuleSetModel();
        policyModel.setAllowedRuleSet(allowedRuleSet);
        allowedRuleSet.setElements(TestHelper.of(
                TestHelper.buildElement(null, TestHelper.of("id"), null)
        ));
        PolicyImpl policy = (PolicyImpl) ConfigBuilder.buildPolicy(Collections.emptyMap(), policyModel, "myPolicy");

        assertEquals(1, policy.propsToProcessByNodeType.size());
        assertNull(policy.propsToProcessByNodeType.get("foo"));
    }


    @Test
    public void GIVEN_a_skip_entry_with_wildcard_that_overrides_one_with_a_prop_WHEN_creating_policy_THEN_only_wildcard_is_kept() {
        PolicyModel policyModel = new PolicyModel();
        policyModel.setStrategy(PolicyModel.PolicyStrategy.SANITIZE);
        policyModel.setProcess(TestHelper.of("bar.*"));
        policyModel.setSkip(TestHelper.of("foo.myProp", "foo.*"));
        RuleSetModel allowedRuleSet = new RuleSetModel();
        policyModel.setAllowedRuleSet(allowedRuleSet);
        allowedRuleSet.setElements(TestHelper.of(
                TestHelper.buildElement(null, TestHelper.of("id"), null)
        ));

        PolicyImpl policy = (PolicyImpl) ConfigBuilder.buildPolicy(Collections.emptyMap(), policyModel, "myPolicy");

        assertEquals(1, policy.propsToSkipByNodeType.size());
        assertNull(policy.propsToProcessByNodeType.get("foo"));
    }

    @Test
    public void GIVEN_a_mix_of_process_and_skip_entries_WHEN_creating_policy_THEN_propsByNodeType_maps_are_populated_correctly() {
        PolicyModel policyModel = new PolicyModel();
        policyModel.setStrategy(PolicyModel.PolicyStrategy.SANITIZE);
        policyModel.setProcess(TestHelper.of("nt:base.jcr:description", "myNt:myOtherNode.myFirstProp", "myNt:myOtherNode.mySecondProp", "myOtherNt:myMixin.*", "foo"));
        policyModel.setSkip(TestHelper.of("myNt:myNode.propToSkip", "bar", "myNt:foo.*"));
        RuleSetModel allowedRuleSet = new RuleSetModel();
        policyModel.setAllowedRuleSet(allowedRuleSet);
        allowedRuleSet.setElements(TestHelper.of(
                TestHelper.buildElement(null, TestHelper.of("id"), null)
        ));

        PolicyImpl policy = (PolicyImpl) ConfigBuilder.buildPolicy(Collections.emptyMap(), policyModel, "myPolicy");

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