package org.jahia.modules.htmlfiltering.impl;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.jahia.modules.htmlfiltering.HtmlValidationResult;
import org.jahia.modules.htmlfiltering.Strategy;
import org.jahia.modules.htmlfiltering.configuration.ElementCfg;
import org.jahia.modules.htmlfiltering.configuration.RuleSetCfg;
import org.jahia.modules.htmlfiltering.configuration.WorkspaceCfg;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@RunWith(JUnitParamsRunner.class)
public class PolicyImplTest {

    @Test
    public void GIVEN_a_null_workspace_WHEN_creating_policy_THEN_exception_is_thrown() {

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new PolicyImpl(Collections.emptyMap(), null));

        assertEquals("Workspace configuration is not set", exception.getMessage());
    }

    @Test
    public void GIVEN_a_null_strategy_WHEN_creating_policy_THEN_exception_is_thrown() {

        WorkspaceCfg workspace = new WorkspaceCfg();
        workspace.setStrategy(null); // null 'strategy'
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new PolicyImpl(Collections.emptyMap(), workspace));

        assertEquals("'strategy' is not set", exception.getMessage());
    }

    @Test
    public void GIVEN_a_null_process_WHEN_creating_policy_THEN_exception_is_thrown() {

        WorkspaceCfg workspace = new WorkspaceCfg();
        workspace.setStrategy(WorkspaceCfg.StrategyCfg.REJECT);
        workspace.setProcess(null); // null 'process'
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new PolicyImpl(Collections.emptyMap(), workspace));

        assertEquals("'process' is not set", exception.getMessage());
    }

    @Test
    public void GIVEN_an_empty_process_WHEN_creating_policy_THEN_exception_is_thrown() {

        WorkspaceCfg workspace = new WorkspaceCfg();
        workspace.setStrategy(WorkspaceCfg.StrategyCfg.REJECT);
        workspace.setProcess(of()); // empty 'process'
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new PolicyImpl(Collections.emptyMap(), workspace));

        assertEquals("'process' is not set", exception.getMessage());
    }

    @Test
    public void GIVEN_a_null_allowedRuleSet_WHEN_creating_policy_THEN_exception_is_thrown() {

        WorkspaceCfg workspace = new WorkspaceCfg();
        workspace.setStrategy(WorkspaceCfg.StrategyCfg.REJECT);
        workspace.setProcess(of("nt:base.*"));
        workspace.setAllowedRuleSet(null); // null 'allowedRuleSet'
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new PolicyImpl(Collections.emptyMap(), workspace));

        assertEquals("'allowedRuleSet' is not set", exception.getMessage());
    }

    @Test
    public void GIVEN_null_elements_in_allowedRuleSet_WHEN_creating_policy_THEN_exception_is_thrown() {

        WorkspaceCfg workspace = new WorkspaceCfg();
        workspace.setStrategy(WorkspaceCfg.StrategyCfg.REJECT);
        workspace.setProcess(of("nt:base.*"));
        RuleSetCfg allowedRuleSet = new RuleSetCfg();
        allowedRuleSet.setElements(null);// null 'elements'
        workspace.setAllowedRuleSet(allowedRuleSet);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new PolicyImpl(Collections.emptyMap(), workspace));

        assertEquals("At least one item in 'elements' must be defined", exception.getMessage());
    }

    @Test
    public void GIVEN_empty_elements_in_allowedRuleSet_WHEN_creating_policy_THEN_exception_is_thrown() {

        WorkspaceCfg workspace = new WorkspaceCfg();
        workspace.setStrategy(WorkspaceCfg.StrategyCfg.REJECT);
        workspace.setProcess(of("nt:base.*"));
        RuleSetCfg allowedRuleSet = new RuleSetCfg();
        allowedRuleSet.setElements(of()); // empty 'elements'
        workspace.setAllowedRuleSet(allowedRuleSet);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new PolicyImpl(Collections.emptyMap(), workspace));

        assertEquals("At least one item in 'elements' must be defined", exception.getMessage());
    }

    @Test
    public void GIVEN_element_without_attributes_and_tags_WHEN_creating_policy_THEN_exception_is_thrown() {
        WorkspaceCfg workspace = new WorkspaceCfg();
        workspace.setStrategy(WorkspaceCfg.StrategyCfg.REJECT);
        workspace.setProcess(of("nt:base.*"));
        RuleSetCfg allowedRuleSet = new RuleSetCfg();
        ElementCfg element = new ElementCfg();
        element.setTags(Collections.emptyList()); // no tags
        element.setAttributes(Collections.emptyList()); // no attributes
        allowedRuleSet.setElements(of(element));
        workspace.setAllowedRuleSet(allowedRuleSet);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new PolicyImpl(Collections.emptyMap(), workspace));

        assertEquals("Each item in 'elements' of 'allowedRuleSet' / 'disallowedRuleSet' must contain 'tags' and/or 'attributes'. Item: " + element, exception.getMessage());
    }

    @Test
    public void GIVEN_element_with_tags_and_format_WHEN_creating_policy_THEN_exception_is_thrown() {
        WorkspaceCfg workspace = new WorkspaceCfg();
        workspace.setStrategy(WorkspaceCfg.StrategyCfg.SANITIZE);
        workspace.setProcess(of("nt:base.*"));
        RuleSetCfg allowedRuleSet = new RuleSetCfg();
        workspace.setAllowedRuleSet(allowedRuleSet);
        ElementCfg element = new ElementCfg();
        element.setTags(of("h1", "p"));
        element.setAttributes(Collections.emptyList()); // no attributes
        element.setFormat("MY_FORMAT");
        allowedRuleSet.setElements(of(element));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new PolicyImpl(Collections.emptyMap(), workspace));

        assertEquals("'format' can only be used with 'attributes'. Item: " + element, exception.getMessage());
    }

    @Test
    public void GIVEN_the_use_of_format_not_defined_WHEN_creating_policy_THEN_exception_is_thrown() {
        WorkspaceCfg workspace = new WorkspaceCfg();
        workspace.setStrategy(WorkspaceCfg.StrategyCfg.SANITIZE);
        workspace.setProcess(of("nt:base.*"));
        RuleSetCfg allowedRuleSet = new RuleSetCfg();
        workspace.setAllowedRuleSet(allowedRuleSet);
        allowedRuleSet.setElements(of(
                buildElement(null, of("id"), "UNDEFINED_FORMAT")
        ));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new PolicyImpl(Collections.emptyMap(), workspace));

        assertEquals("Format 'UNDEFINED_FORMAT' not defined, check your configuration", exception.getMessage());
    }

    @Test
    @Parameters({
            "REJECT, REJECT",
            "SANITIZE, SANITIZE",
    })
    public void GIVEN_a_workspace_with_a_specific_strategy_WHEN_creating_policy_THEN_the_strategy_matches(WorkspaceCfg.StrategyCfg strategyCfg, Strategy expectedStrategy) {
        WorkspaceCfg workspace = new WorkspaceCfg();
        workspace.setStrategy(strategyCfg);
        workspace.setProcess(of("nt:base.*"));
        workspace.setAllowedRuleSet(new RuleSetCfg());
        workspace.getAllowedRuleSet().setElements(of(
                buildElement(of("p"), null, null)
        ));

        PolicyImpl policy = new PolicyImpl(Collections.emptyMap(), workspace);

        assertEquals(expectedStrategy, policy.getStrategy());
    }


    @Test
    public void GIVEN_an_empty_process_entry_WHEN_creating_policy_THEN_exception_is_thrown() {
        WorkspaceCfg workspace = new WorkspaceCfg();
        workspace.setStrategy(WorkspaceCfg.StrategyCfg.SANITIZE);
        workspace.setProcess(of(""));
        RuleSetCfg allowedRuleSet = new RuleSetCfg();
        workspace.setAllowedRuleSet(allowedRuleSet);
        allowedRuleSet.setElements(of(
                buildElement(null, of("id"), null)
        ));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new PolicyImpl(Collections.emptyMap(), workspace));

        assertEquals("Each item in 'process' must be set and not empty", exception.getMessage());
    }

    @Test
    public void GIVEN_an_empty_skip_entry_WHEN_creating_policy_THEN_exception_is_thrown() {
        WorkspaceCfg workspace = new WorkspaceCfg();
        workspace.setStrategy(WorkspaceCfg.StrategyCfg.SANITIZE);
        workspace.setProcess(of("nt:base"));
        workspace.setSkip(of(""));
        RuleSetCfg allowedRuleSet = new RuleSetCfg();
        workspace.setAllowedRuleSet(allowedRuleSet);
        allowedRuleSet.setElements(of(
                buildElement(null, of("id"), null)
        ));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new PolicyImpl(Collections.emptyMap(), workspace));

        assertEquals("Each item in 'skip' must be set and not empty", exception.getMessage());
    }

    @Test
    @Parameters({
            "foo.bar.more",
            "  foo  .  bar .   more",
            "foo.bar.even.more",
    })
    public void GIVEN_an_invalid_process_entry_WHEN_creating_policy_THEN_exception_is_thrown(String invalidProcessEntry) {
        WorkspaceCfg workspace = new WorkspaceCfg();
        workspace.setStrategy(WorkspaceCfg.StrategyCfg.SANITIZE);
        workspace.setProcess(of(invalidProcessEntry));
        RuleSetCfg allowedRuleSet = new RuleSetCfg();
        workspace.setAllowedRuleSet(allowedRuleSet);
        allowedRuleSet.setElements(of(
                buildElement(null, of("id"), null)
        ));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new PolicyImpl(Collections.emptyMap(), workspace));

        assertEquals("Invalid format for item '" + invalidProcessEntry + "' in 'process'. Expected format is 'nodeType.property' or 'nodeType.*'", exception.getMessage());
    }

    @Test
    @Parameters({
            "foo.bar.more",
            "  foo  .  bar .   more",
            "foo.bar.even.more",
    })
    public void GIVEN_an_invalid_skip_entry_WHEN_creating_policy_THEN_exception_is_thrown(String invalidProcessEntry) {
        WorkspaceCfg workspace = new WorkspaceCfg();
        workspace.setStrategy(WorkspaceCfg.StrategyCfg.SANITIZE);
        workspace.setProcess(of("nt:base"));
        workspace.setSkip(of(invalidProcessEntry));
        RuleSetCfg allowedRuleSet = new RuleSetCfg();
        workspace.setAllowedRuleSet(allowedRuleSet);
        allowedRuleSet.setElements(of(
                buildElement(null, of("id"), null)
        ));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new PolicyImpl(Collections.emptyMap(), workspace));

        assertEquals("Invalid format for item '" + invalidProcessEntry + "' in 'skip'. Expected format is 'nodeType.property' or 'nodeType.*'", exception.getMessage());
    }

    @Test
    public void GIVEN_a_process_entry_that_overrides_a_wildcard_WHEN_creating_policy_THEN_only_wildcard_is_kept() {
        WorkspaceCfg workspace = new WorkspaceCfg();
        workspace.setStrategy(WorkspaceCfg.StrategyCfg.SANITIZE);
        workspace.setProcess(of("foo.*", "foo.bar"));
        RuleSetCfg allowedRuleSet = new RuleSetCfg();
        workspace.setAllowedRuleSet(allowedRuleSet);
        allowedRuleSet.setElements(of(
                buildElement(null, of("id"), null)
        ));
        PolicyImpl policy = new PolicyImpl(Collections.emptyMap(), workspace);

        assertEquals(1, policy.propsToProcessByNodeType.size());
        assertNull(policy.propsToProcessByNodeType.get("foo"));
    }


    @Test
    public void GIVEN_a_skip_entry_with_wildcard_that_overrides_one_with_a_prop_WHEN_creating_policy_THEN_only_wildcard_is_kept() {
        WorkspaceCfg workspace = new WorkspaceCfg();
        workspace.setStrategy(WorkspaceCfg.StrategyCfg.SANITIZE);
        workspace.setProcess(of("bar.*"));
        workspace.setSkip(of("foo.myProp", "foo.*"));
        RuleSetCfg allowedRuleSet = new RuleSetCfg();
        workspace.setAllowedRuleSet(allowedRuleSet);
        allowedRuleSet.setElements(of(
                buildElement(null, of("id"), null)
        ));

        PolicyImpl policy = new PolicyImpl(Collections.emptyMap(), workspace);

        assertEquals(1, policy.propsToSkipByNodeType.size());
        assertNull(policy.propsToProcessByNodeType.get("foo"));
    }

    @Test
    public void GIVEN_a_mix_of_process_and_skip_entries_WHEN_creating_policy_THEN_propsByNodeType_maps_are_populated_correctly() {
        WorkspaceCfg workspace = new WorkspaceCfg();
        workspace.setStrategy(WorkspaceCfg.StrategyCfg.SANITIZE);
        workspace.setProcess(of("nt:base.jcr:description", "myNt:myOtherNode.myFirstProp", "myNt:myOtherNode.mySecondProp", "myOtherNt:myMixin.*", "foo"));
        workspace.setSkip(of("myNt:myNode.propToSkip", "bar", "myNt:foo.*"));
        RuleSetCfg allowedRuleSet = new RuleSetCfg();
        workspace.setAllowedRuleSet(allowedRuleSet);
        allowedRuleSet.setElements(of(
                buildElement(null, of("id"), null)
        ));

        PolicyImpl policy = new PolicyImpl(Collections.emptyMap(), workspace);

        assertEquals(4, policy.propsToProcessByNodeType.size());
        assertEquals(setOf("jcr:description"), policy.propsToProcessByNodeType.get("nt:base"));
        assertEquals(setOf("myFirstProp", "mySecondProp"), policy.propsToProcessByNodeType.get("myNt:myOtherNode"));
        assertNull(policy.propsToProcessByNodeType.get("myOtherNt:myMixin"));
        assertNull(policy.propsToProcessByNodeType.get("foo"));
        assertEquals(3, policy.propsToSkipByNodeType.size());
        assertEquals(setOf("propToSkip"), policy.propsToSkipByNodeType.get("myNt:myNode"));
        assertNull(policy.propsToSkipByNodeType.get("bar"));
        assertNull(policy.propsToSkipByNodeType.get("myNt:foo"));
    }

    @Test
    @Parameters({
            // all tags get removed, but their content (when applicable) is kept:
            "<p>Hello World</p><script>alert('Javascript')</script>, Hello World",
            // simple HTML content is kept:
            "my text, my text",
            // invalid HTML content is removed but content is kept:
            "<p>my text<h1>title</h6>, my texttitle"
    })
    public void GIVEN_minimal_configuration_WHEN_sanitizing_THEN_only_content_is_kept(String html, String expectedHtml) {
        WorkspaceCfg workspace = new WorkspaceCfg();
        workspace.setStrategy(WorkspaceCfg.StrategyCfg.SANITIZE);
        workspace.setProcess(of("nt:base.*"));
        workspace.setAllowedRuleSet(new RuleSetCfg());
        workspace.getAllowedRuleSet().setElements(of(
                buildElement(of("basicTag"), null, null)
        ));
        PolicyImpl policy = new PolicyImpl(Collections.emptyMap(), workspace);

        String sanitized = policy.sanitize(html);

        assertEquals(expectedHtml, sanitized);
    }

    @Test
    public void GIVEN_minimal_configuration_WHEN_validating_THEN_tags_and_attributes_are_rejected() {
        WorkspaceCfg workspace = new WorkspaceCfg();
        workspace.setStrategy(WorkspaceCfg.StrategyCfg.SANITIZE);
        workspace.setProcess(of("nt:base.*"));
        workspace.setAllowedRuleSet(new RuleSetCfg());
        workspace.getAllowedRuleSet().setElements(of(
                buildElement(of("basicTag"), null, null)
        ));

        PolicyImpl policy = new PolicyImpl(Collections.emptyMap(), workspace);
        String html = "<p>Hello World</p><script>alert('Javascript')</script>";

        HtmlValidationResult validationResult = policy.validate(html);
        assertFalse(validationResult.isSafe());
        HashSet<String> expectedRejectedTags = new HashSet<>();
        expectedRejectedTags.add("script");
        expectedRejectedTags.add("p");
        assertEquals(expectedRejectedTags, validationResult.getRejectedTags());
        assertTrue(validationResult.getRejectedAttributesByTag().isEmpty());
        assertEquals("Hello World", validationResult.getSanitizedHtml());
    }

    @Test
    @Parameters({
            // allowed <h1> and <p> are kept:
            "<h1>title</h1><p>my text</p>, <h1>title</h1><p>my text</p>",
            // only <h1> is kept, <h2> is removed, but its content is kept:
            "<h1>title</h1><h2>sub-title</h2>, <h1>title</h1>sub-title",
            // disallowed <script> is removed with its content:
            "<p>Hello World</p><script>alert('Javascript')</script>, <p>Hello World</p>",
            // all tags are removed:
            "<h2>title</h2> <a href=\"link\">button</a>, title button",
            // all attributes of <h1> are removed:
            "<h1 class=\"test\" invalid=\"unknown\">title</h1>, <h1>title</h1>"
    })
    public void GIVEN_configuration_with_allowed_tags_without_attributes_WHEN_sanitizing_THEN_string_is_sanitized(String html, String expectedHtml) {
        WorkspaceCfg workspace = new WorkspaceCfg();
        workspace.setStrategy(WorkspaceCfg.StrategyCfg.SANITIZE);
        workspace.setProcess(of("nt:base.*"));
        RuleSetCfg allowedRuleSet = new RuleSetCfg();
        workspace.setAllowedRuleSet(allowedRuleSet);
        allowedRuleSet.setElements(of(
                buildElement(of("h1", "p"), null, null)
        ));
        PolicyImpl policy = new PolicyImpl(Collections.emptyMap(), workspace);

        String sanitized = policy.sanitize(html);

        assertEquals(expectedHtml, sanitized);
    }

    @Test
    @Parameters({
            // allowed 'class' and 'id' attributes are kept:
            "<h1 id=\"myid\">title</h1><p class=\"myclass\">my text</p>, <h1 id=\"myid\">title</h1><p class=\"myclass\">my text</p>",
            // other non-allowed attributes are removed:
            "<h1 id=\"myid\" dir=\"rtl\">title</h1>, <h1 id=\"myid\">title</h1>",
            // allowed attributes on <h1> are kept, but not on <h2>:
            "<h1 id=\"myid\">title</h1><h2 id=\"myotherid\">sub-title</h2>, <h1 id=\"myid\">title</h1><h2>sub-title</h2>",
    })
    public void GIVEN_configuration_with_allowed_attributes_on_tags_WHEN_sanitizing_THEN_string_is_sanitized(String html, String expectedHtml) {
        WorkspaceCfg workspace = new WorkspaceCfg();
        workspace.setStrategy(WorkspaceCfg.StrategyCfg.SANITIZE);
        workspace.setProcess(of("nt:base.*"));
        RuleSetCfg allowedRuleSet = new RuleSetCfg();
        workspace.setAllowedRuleSet(allowedRuleSet);
        allowedRuleSet.setElements(of(
                buildElement(of("h1", "p"), of("class", "id"), null),
                buildElement(of("h1", "h2", "p"), null, null) // the tags must also be allowed "globally"
        ));
        PolicyImpl policy = new PolicyImpl(Collections.emptyMap(), workspace);

        String sanitized = policy.sanitize(html);

        assertEquals(expectedHtml, sanitized);
    }

    @Test
    @Parameters({
            // allowed <h1> and <p> with their allowed attributes are kept:
            "<h1 id=\"myid\">title</h1><p class=\"myclass\">my text</p>, <h1 id=\"myid\">title</h1><p class=\"myclass\">my text</p>",
            // not allowed attributes are removed:
            "<h1 id=\"myid\" dir=\"rtl\">title</h1>, <h1 id=\"myid\">title</h1>",
            // not allowed tags are removed:
            "<h1 id=\"myid\">title</h1><h2>sub-title</h2>, <h1 id=\"myid\">title</h1>sub-title",
    })
    public void GIVEN_configuration_with_allowed_attributes_and_allowed_tags_WHEN_sanitizing_THEN_string_is_sanitized(String html, String expectedHtml) {
        WorkspaceCfg workspace = new WorkspaceCfg();
        workspace.setStrategy(WorkspaceCfg.StrategyCfg.SANITIZE);
        workspace.setProcess(of("nt:base.*"));
        RuleSetCfg allowedRuleSet = new RuleSetCfg();
        workspace.setAllowedRuleSet(allowedRuleSet);
        allowedRuleSet.setElements(of(
                buildElement(of("h1", "p"), null, null),
                buildElement(null, of("class", "id"), null)
        ));
        PolicyImpl policy = new PolicyImpl(Collections.emptyMap(), workspace);

        String sanitized = policy.sanitize(html);

        assertEquals(expectedHtml, sanitized);
    }

    @Test
    @Parameters({
            // valid protocols are kept:
            "<a href=\"http://example.com\">my link</a>, <a href=\"http://example.com\">my link</a>",
            "<img src=\"https://example.com/image.gif\" />, <img src=\"https://example.com/image.gif\" />",
            // invalid protocols are removed and the links are transformed to a regular text:
            "<p>text:<a href=\"ftp://example.com\">my link</a></p>, <p>text:my link</p>",
            // invalid protocols for <img> are removed and the whole tag is removed:
            "<p>text:<img alt=\"myimage\" src=\"ftp://example.com/image.gif\" /></p>, <p>text:</p>",
    })
    public void GIVEN_configuration_with_allowed_protocols_on_a_tags_WHEN_sanitizing_THEN_string_is_sanitized(String html, String expectedHtml) {
        WorkspaceCfg workspace = new WorkspaceCfg();
        workspace.setStrategy(WorkspaceCfg.StrategyCfg.SANITIZE);
        workspace.setProcess(of("nt:base.*"));
        RuleSetCfg allowedRuleSet = new RuleSetCfg();
        allowedRuleSet.setProtocols(of("http", "https"));
        workspace.setAllowedRuleSet(allowedRuleSet);
        allowedRuleSet.setElements(of(
                buildElement(null, of("href", "src"), null),
                buildElement(of("a", "p", "img"), null, null)
        ));
        PolicyImpl policy = new PolicyImpl(Collections.emptyMap(), workspace);

        String sanitized = policy.sanitize(html);

        assertEquals(expectedHtml, sanitized);
    }


    @Test
    @Parameters({
            // valid format for <p>:
            "<p id=\"abc\">text</p>, <p id=\"abc\">text</p>",
            // invalid format, the attribute is removed:
            "<p id=\"DEF\">text</p>, <p>text</p>",
            // valid format for <p> used on <pre> (invalid format), the attribute is removed:
            "<pre id=\"abc\">text</pre>, <pre>text</pre>",
            // valid format for <p> used on <textarea> (no format defined), the attribute is kept:
            "<textarea id=\"abc\">text</textarea>, <textarea id=\"abc\">text</textarea>",
            // using multiple formats on the same <p> tag, the attributes are kept:
            "<p id=\"abc\" class=\"123\">text</p>, <p id=\"abc\" class=\"123\">text</p>",

    })
    public void GIVEN_configuration_with_formats_defined_WHEN_sanitizing_THEN_string_is_sanitized_with_the_format(String html, String expectedHtml) {
        WorkspaceCfg workspace = new WorkspaceCfg();
        workspace.setStrategy(WorkspaceCfg.StrategyCfg.SANITIZE);
        workspace.setProcess(of("nt:base.*"));
        RuleSetCfg allowedRuleSet = new RuleSetCfg();
        allowedRuleSet.setElements(of(
                buildElement(of("p"), of("id"), "LOWERCASE_LETTERS"),
                buildElement(of("p"), of("class"), "DIGITS"),
                buildElement(of("pre"), of("id"), "DIGITS"),
                buildElement(of("textarea"), of("id"), null),
                buildElement(of("p", "pre", "textarea"), null, null)
        ));
        workspace.setAllowedRuleSet(allowedRuleSet);
        Map<String, Pattern> formatPatterns = new HashMap<>();
        formatPatterns.put("LOWERCASE_LETTERS", Pattern.compile("^[a-z]+$"));
        formatPatterns.put("DIGITS", Pattern.compile("^[0-9]+$"));
        PolicyImpl policy = new PolicyImpl(formatPatterns, workspace);

        String sanitized = policy.sanitize(html);

        assertEquals(expectedHtml, sanitized);

    }

    @Test
    @Parameters({
            // test with allowed and disallowed attributes on several tags:
            "<h1 id=\"myid1\" class=\"myclass\">title</h1><h3 id=\"myid3\" dir=\"rtl\">sub-title</h3>, <h1 id=\"myid1\" class=\"myclass\">title</h1><h3 id=\"myid3\">sub-title</h3>",
            // invalid closing tag gets fixed:
            "<h1>wrong closing tag</h2>, <h1>wrong closing tag</h1>",
            // tags that are allowed and then disallowed are removed (but their content is kept):
            "<h5>to remove</h5><h6>to remove</h6><p>text</p>, to removeto remove<p>text</p>",
            // only valid ids matching the format are kept:
            "<h1 id=\"&^z\">title1</h1><h2 id=\"A1a_\">title2</h2><h3 id=\"A1a+\">title3</h3>, <h1>title1</h1><h2 id=\"A1a_\">title2</h2><h3>title3</h3>",
            // protocols that are allowed and then disallowed are removed:
            "<a href=\"ftps://example.com\">valid link</a><a href=\"https://example.com\">not allowed link</a><a href=\"http://example.com\">not explicitly allowed means forbidden</a>, <a href=\"ftps://example.com\">valid link</a>not allowed linknot explicitly allowed means forbidden",
            // TODO not working for now, should be fixed or the behaviour properly explained to customers
            // 'src' attribute if <img> matching the disallowed format are removed:
            // "<img src=\"ftps://example.com/foo.jpg\"/><img src=\"ftps://example.com/other.gif\"/>, <img src=\"ftps://example.com/foo.jpg\"/>",

    })
    public void GIVEN_a_complete_configuration_WHEN_sanitizing_THEN_string_matches_expected_output(String html, String expectedHtml) {
        PolicyImpl policy = buildCompletePolicy();

        String sanitized = policy.sanitize(html);

        assertEquals(expectedHtml, sanitized);
    }

    private static PolicyImpl buildCompletePolicy() {
        WorkspaceCfg workspace = new WorkspaceCfg();
        workspace.setStrategy(WorkspaceCfg.StrategyCfg.REJECT);
        workspace.setProcess(of("nt:base.*"));
        RuleSetCfg allowedRuleSet = new RuleSetCfg();
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
        workspace.setAllowedRuleSet(allowedRuleSet);
        RuleSetCfg disallowedRuleSet = new RuleSetCfg();
        disallowedRuleSet.setElements(of(
                // disallow the 'title' attribute on <p> tags:
                buildElement(of("p"), of("title"), null),
                // disallow regex for img src attributes:
                buildElement(of("img"), of("src"), "NO_GIF"), // TODO
                // disallow a few tags globally
                buildElement(of("h5", "h6"), null, null)
        ));
        disallowedRuleSet.setProtocols(of("https"));
        workspace.setDisallowedRuleSet(disallowedRuleSet);
        Map<String, Pattern> formatPatterns = new HashMap<>();
        formatPatterns.put("HTML_ID", Pattern.compile("^[a-zA-Z0-9_]+$"));
        formatPatterns.put("NO_GIF", Pattern.compile(".*\\.gif"));
        return new PolicyImpl(formatPatterns, workspace);
    }

    private static ElementCfg buildElement(List<String> tags, List<String> attributes, String format) {
        ElementCfg elementCfg = new ElementCfg();
        elementCfg.setTags(tags);
        elementCfg.setAttributes(attributes);
        elementCfg.setFormat(format);
        return elementCfg;
    }

    // equivalent of List.of(...) only available in Java 9+
    private static <T> List<T> of(T... items) {
        return Arrays.asList(items);
    }

    // equivalent of Set.of(...) only available in Java 9+
    private static <T> Set<T> setOf(T... items) {
        return new HashSet<>(Arrays.asList(items));
    }
}
