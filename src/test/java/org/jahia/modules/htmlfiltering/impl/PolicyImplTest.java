package org.jahia.modules.htmlfiltering.impl;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.jahia.modules.htmlfiltering.ValidationResult;
import org.jahia.modules.htmlfiltering.configuration.ElementCfg;
import org.jahia.modules.htmlfiltering.configuration.RuleSetCfg;
import org.jahia.modules.htmlfiltering.configuration.WorkspaceCfg;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.*;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

@RunWith(JUnitParamsRunner.class)
public class PolicyImplTest {

    @Test
    public void GIVEN_element_without_attributes_and_tags_WHEN_creating_policy_THEN_exception_is_thrown() {
        WorkspaceCfg workspace = new WorkspaceCfg();
        RuleSetCfg allowedRuleSet = new RuleSetCfg();
        ElementCfg element = new ElementCfg();
        element.setTags(Collections.emptyList()); // no tags
        element.setAttributes(Collections.emptyList()); // no attributes
        allowedRuleSet.setElements(of(element));
        workspace.setAllowedRuleSet(allowedRuleSet);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new PolicyImpl(Collections.emptyMap(), workspace));

        assertEquals("Each item in the 'elements' of an 'allowedRuleSet' / 'disallowedRuleSet' must contain 'tags' and/or 'attributes'. Item: " + element, exception.getMessage());
    }

    @Test
    public void GIVEN_empty_configuration_WHEN_sanitizing_THEN_only_content_is_kept() {
        PolicyImpl policy = new PolicyImpl(Collections.emptyMap(), new WorkspaceCfg());
        String html = "<p>Hello World</p><script>alert('Javascript')</script>";

        String sanitized = policy.sanitize(html);

        assertEquals("Hello World", sanitized);
    }

    @Test
    public void GIVEN_empty_configuration_WHEN_validating_THEN_tags_and_attributes_are_rejected() {
        PolicyImpl policy = new PolicyImpl(Collections.emptyMap(), new WorkspaceCfg());
        String html = "<p>Hello World</p><script>alert('Javascript')</script>";
        ValidationResultBuilderImpl validationResultBuilder = new ValidationResultBuilderImpl();

        policy.validate("myProp", html, validationResultBuilder);
        ValidationResult validationResult = validationResultBuilder.build();

        assertFalse(validationResult.isValid());
        assertEquals(1, validationResult.rejectedProperties().size());
        assertTrue(validationResult.rejectedProperties().contains("myProp"));
        ValidationResult.PropertyRejectionResult rejectionResult = validationResult.getRejectionResult("myProp");
        HashSet<String> expectedRejectedTags = new HashSet<>();
        expectedRejectedTags.add("script");
        expectedRejectedTags.add("p");
        assertEquals(expectedRejectedTags, rejectionResult.getRejectedTags());
        assertTrue(rejectionResult.getRejectedAttributesByTagEntrySet().isEmpty());

//        validationResult.propertyValidationResultSet()
//        assertEquals(validationResult.pr);
        // TODO to complete
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

    private static ElementCfg buildElement(List<String> tags, List<String> attributes, String format) {
        ElementCfg elementCfg = new ElementCfg();
        elementCfg.setTags(tags);
        elementCfg.setAttributes(attributes);
        elementCfg.setFormat(format);
        return elementCfg;
    }

    private static <T> List<T> of(T... items) {
        return Arrays.asList(items);
    }
}