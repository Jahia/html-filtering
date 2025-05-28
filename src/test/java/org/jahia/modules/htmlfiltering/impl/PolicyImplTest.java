package org.jahia.modules.htmlfiltering.impl;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.jahia.modules.htmlfiltering.Policy;
import org.jahia.modules.htmlfiltering.PolicySanitizedHtmlResult;
import org.jahia.modules.htmlfiltering.impl.config.Config;
import org.jahia.modules.htmlfiltering.model.ConfigModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.cm.ConfigurationException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.jahia.modules.htmlfiltering.impl.TestHelper.buildConfigModel;
import static org.jahia.modules.htmlfiltering.impl.TestHelper.buildCompleteConfigModel;
import static org.jahia.modules.htmlfiltering.impl.TestHelper.buildElement;
import static org.jahia.modules.htmlfiltering.impl.TestHelper.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnitParamsRunner.class)
public class PolicyImplTest {

    @Parameters({
            // basic paragraph
            "p,<p>sample text</p>,<p>sample text</p>",
            // only paragraph tags are kept
            "p,<p><i>sample<i> text</p><script>alert('hello')</script>,<p>sample text</p>",
            // by default, the owasp-java-html-sanitizer does not allow text content in <script> so we need to overwrite this behavior
            "script,<script>alert('hello')</script>,<script>alert('hello')</script>",
            // other tags are removed
            "script,<p>text</p><script>alert('hello')</script>,text<script>alert('hello')</script>",
    })
    @Test
    public void GIVEN_a_configuration_that_allows_tags_WHEN_sanitizing_THEN_its_text_content_is_kept(String tag, String html, String expectedHtml) throws ConfigurationException {
        ConfigModel configModel = buildConfigModel(tag);
        Config config = ConfigBuilder.buildFromModel(configModel);
        Policy policy = config.getEditWorkspacePolicy();

        String sanitized = policy.sanitize(html).getSanitizedHtml();

        assertEquals(expectedHtml, sanitized);
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
    public void GIVEN_minimal_configuration_with_unknown_tag_WHEN_sanitizing_THEN_only_content_is_kept(String html, String expectedHtml) throws ConfigurationException {
        ConfigModel configModel = buildConfigModel("basicTag");
        Config config = ConfigBuilder.buildFromModel(configModel);
        Policy policy = config.getEditWorkspacePolicy();

        String sanitized = policy.sanitize(html).getSanitizedHtml();

        assertEquals(expectedHtml, sanitized);
    }

    @Test
    public void GIVEN_minimal_configuration_WHEN_validating_THEN_tags_and_attributes_are_rejected() throws ConfigurationException {
        ConfigModel configModel = buildConfigModel("basicTag");
        Config config = ConfigBuilder.buildFromModel(configModel);
        Policy policy = config.getEditWorkspacePolicy();
        String html = "<p>Hello World</p><script>alert('Javascript')</script>";

        PolicySanitizedHtmlResult validationResult = policy.sanitize(html);

        assertFalse(validationResult.isValid());
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
    public void GIVEN_configuration_with_allowed_tags_without_attributes_WHEN_sanitizing_THEN_string_is_sanitized(String html, String expectedHtml) throws ConfigurationException {
        ConfigModel configModel = buildConfigModel("h1", "p");
        Config config = ConfigBuilder.buildFromModel(configModel);
        Policy policy = config.getEditWorkspacePolicy();

        String sanitized = policy.sanitize(html).getSanitizedHtml();

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
    public void GIVEN_configuration_with_allowed_attributes_on_tags_WHEN_sanitizing_THEN_string_is_sanitized(String html, String expectedHtml) throws ConfigurationException {
        ConfigModel configModel = TestHelper.buildConfigModel();
        configModel.getEditWorkspace().getAllowedRuleSet().setElements(of(
                buildElement(of("h1", "p"), of("class", "id"), null),
                buildElement(of("h1", "h2", "p"), null, null) // the tags must also be allowed "globally"
        ));
        Config config = ConfigBuilder.buildFromModel(configModel);
        Policy policy = config.getEditWorkspacePolicy();

        String sanitized = policy.sanitize(html).getSanitizedHtml();

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
    public void GIVEN_configuration_with_allowed_attributes_and_allowed_tags_WHEN_sanitizing_THEN_string_is_sanitized(String html, String expectedHtml) throws ConfigurationException {
        ConfigModel configModel = TestHelper.buildConfigModel();
        configModel.getEditWorkspace().getAllowedRuleSet().setElements(of(
                buildElement(of("h1", "p"), null, null),
                buildElement(null, of("class", "id"), null)
        ));
        Config config = ConfigBuilder.buildFromModel(configModel);
        Policy policy = config.getEditWorkspacePolicy();

        String sanitized = policy.sanitize(html).getSanitizedHtml();

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
    public void GIVEN_configuration_with_allowed_protocols_on_a_tags_WHEN_sanitizing_THEN_string_is_sanitized(String html, String expectedHtml) throws ConfigurationException {
        ConfigModel configModel = TestHelper.buildConfigModel();
        configModel.getEditWorkspace().getAllowedRuleSet().setProtocols(of("http", "https"));
        configModel.getEditWorkspace().getAllowedRuleSet().setElements(of(
                buildElement(null, of("href", "src"), null),
                buildElement(of("a", "p", "img"), null, null)
        ));
        Config config = ConfigBuilder.buildFromModel(configModel);
        Policy policy = config.getEditWorkspacePolicy();

        String sanitized = policy.sanitize(html).getSanitizedHtml();

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
    public void GIVEN_configuration_with_formats_defined_WHEN_sanitizing_THEN_string_is_sanitized_with_the_format(String html, String expectedHtml) throws ConfigurationException {
        ConfigModel configModel = TestHelper.buildConfigModel();
        configModel.getEditWorkspace().getAllowedRuleSet().setElements(of(
                buildElement(of("p"), of("id"), "LOWERCASE_LETTERS"),
                buildElement(of("p"), of("class"), "DIGITS"),
                buildElement(of("pre"), of("id"), "DIGITS"),
                buildElement(of("textarea"), of("id"), null),
                buildElement(of("p", "pre", "textarea"), null, null)
        ));
        Map<String, String> formatPatterns = new HashMap<>();
        formatPatterns.put("LOWERCASE_LETTERS","^[a-z]+$");
        formatPatterns.put("DIGITS", "^[0-9]+$");
        configModel.setFormatDefinitions(formatPatterns);
        Config config = ConfigBuilder.buildFromModel(configModel);
        Policy policy = config.getEditWorkspacePolicy();

        String sanitized = policy.sanitize(html).getSanitizedHtml();

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
            // editor post processing should keep known placeholders: {mode}, {lang}, {workspace} should be conserved during sanitization
            "<a href=\"##cms-context##/{mode}/{lang}/{workspace}/##ref:link1##.html\">link</a>, <a href=\"##cms-context##/{mode}/{lang}/{workspace}/##ref:link1##.html\">link</a>",
            // editor post processing should not keep unknown placeholders: {unknown} should replaced with %7bunknown%7d in href
            "<a href=\"##cms-context##/{unknown}/##ref:link1##.html\">link</a>, <a href=\"##cms-context##/%7bunknown%7d/##ref:link1##.html\">link</a>",
            // TODO not working for now, should be fixed or the behaviour properly explained to customers
            // 'src' attribute of <img> matching the disallowed format are removed:
            // "<img src=\"ftps://example.com/foo.jpg\"/><img src=\"ftps://example.com/other.gif\"/>, <img src=\"ftps://example.com/foo.jpg\"/>",
    })
    public void GIVEN_a_complete_configuration_WHEN_sanitizing_THEN_string_matches_expected_output(String html, String expectedHtml) throws ConfigurationException {
        ConfigModel configModel = buildCompleteConfigModel();
        Config config = ConfigBuilder.buildFromModel(configModel);
        Policy policy = config.getEditWorkspacePolicy();

        String sanitized = policy.sanitize(html).getSanitizedHtml();

        assertEquals(expectedHtml, sanitized);
    }

}
