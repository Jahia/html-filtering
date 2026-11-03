package org.jahia.modules.htmlfiltering.impl;

import org.jahia.modules.htmlfiltering.Policy;
import org.jahia.modules.htmlfiltering.Strategy;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.SelectorType;
import org.junit.Before;
import org.junit.Test;
import org.owasp.html.HtmlPolicyBuilder;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PolicyImplApplicabilityTest {

    private JCRNodeWrapper node;
    private ExtendedPropertyDefinition richText;

    @Before
    public void setUp() throws RepositoryException {
        node = mock(JCRNodeWrapper.class);
        when(node.isNodeType("nt:base")).thenReturn(true);
        when(node.hasPermission("site-admin")).thenReturn(true);
        richText = definition(PropertyType.STRING, SelectorType.RICHTEXT);
    }

    @Test
    public void GIVEN_a_user_with_a_skip_permission_WHEN_checking_the_property_THEN_only_the_check_that_ignores_permissions_applies() {
        Policy policy = policy(List.of("site-admin"), Collections.emptyMap());

        assertFalse(policy.isApplicableToProperty(node, "text", richText));
        assertTrue(policy.isApplicableToPropertyIgnoringPermissions(node, "text", richText));
    }

    @Test
    public void GIVEN_no_skip_permission_WHEN_checking_the_property_THEN_both_checks_apply() {
        Policy policy = policy(Collections.emptyList(), Collections.emptyMap());

        assertTrue(policy.isApplicableToProperty(node, "text", richText));
        assertTrue(policy.isApplicableToPropertyIgnoringPermissions(node, "text", richText));
    }

    @Test
    public void GIVEN_a_skipped_property_WHEN_checking_it_ignoring_permissions_THEN_it_is_not_applicable() {
        Map<String, Set<String>> skip = new HashMap<>();
        skip.put("nt:base", Set.of("text"));
        Policy policy = policy(Collections.emptyList(), skip);

        assertFalse(policy.isApplicableToPropertyIgnoringPermissions(node, "text", richText));
        assertTrue(policy.isApplicableToPropertyIgnoringPermissions(node, "other", richText));
    }

    @Test
    public void GIVEN_a_property_without_the_richtext_selector_WHEN_checking_it_ignoring_permissions_THEN_it_is_not_applicable() {
        Policy policy = policy(Collections.emptyList(), Collections.emptyMap());

        assertFalse(policy.isApplicableToPropertyIgnoringPermissions(node, "text", definition(PropertyType.STRING, SelectorType.SMALLTEXT)));
    }

    private static Policy policy(List<String> skipOnPermissions, Map<String, Set<String>> skip) {
        // A null set of properties stands for every property of the node type.
        Map<String, Set<String>> process = new HashMap<>();
        process.put("nt:base", null);
        return new PolicyImpl(Strategy.REJECT, process, skip, skipOnPermissions, new HtmlPolicyBuilder().toFactory());
    }

    private static ExtendedPropertyDefinition definition(int requiredType, int selector) {
        ExtendedPropertyDefinition definition = mock(ExtendedPropertyDefinition.class);
        when(definition.getRequiredType()).thenReturn(requiredType);
        when(definition.getSelector()).thenReturn(selector);
        return definition;
    }
}
