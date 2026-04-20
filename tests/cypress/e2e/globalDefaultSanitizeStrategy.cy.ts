import {addNode, createSite, deleteSite} from '@jahia/cypress';
import {installConfig, mutateNodeProperty, removeGlobalCustomConfig} from '../fixtures/utils';

/**
 * Test scenarios for the SANITIZE strategy behavior on the global default configuration rule set.
 *
 * These tests complement globalDefaultConfiguration.cy.ts (which uses the default REJECT strategy)
 * by verifying how the sanitizer transforms invalid/unsafe HTML when the SANITIZE strategy is active:
 *   - Invalid attributes are stripped (not rejected)
 *   - Disallowed tags/protocols result in tag/attribute removal (not a validation error)
 *   - The sanitizer normalizes and encodes certain characters in attribute values
 *
 * The rule set used here mirrors the global default configuration exactly; only the edit workspace
 * strategy is changed to SANITIZE via a global custom configuration override.
 */
describe('Test sanitization behavior of the global default configuration rule set (with SANITIZE strategy)', () => {
    const SITE_KEY = 'testGlobalDefaultConfigSanitize';
    const NODE = 'testNode';
    const PATH = `/sites/${SITE_KEY}/home/pagecontent/${NODE}`;
    const CONFIG_PATH = 'configs/globalDefaultConfiguration/org.jahia.modules.htmlfiltering.global.custom.yml';

    before(() => {
        // Install a global custom config with the same rules as the global default, but using SANITIZE strategy
        installConfig(CONFIG_PATH);
        createSite(SITE_KEY, {locale: 'en', serverName: 'localhost', templateSet: 'html-filtering-test-module'});
        addNode({
            parentPathOrId: `/sites/${SITE_KEY}/home`,
            name: 'pagecontent',
            primaryNodeType: 'jnt:contentList',
            children: [
                {
                    name: NODE,
                    primaryNodeType: 'htmlFilteringTestModule:testGlobalDefaultConfiguration',
                    properties: [{name: 'prop'}]
                }
            ]
        });
    });

    after(() => {
        deleteSite(SITE_KEY);
        removeGlobalCustomConfig();
    });

    function modifyAndCheck(text: string, expected = text) {
        mutateNodeProperty(PATH, 'prop', text).then(response => {
            const resultNoSpace = response.data.jcr.mutateNode.mutateProperty.property.value.replace(/\s/g, '');
            const expectedNoSpace = expected.replace(/\s/g, '');
            expect(resultNoSpace).to.eq(expectedNoSpace);
        });
    }

    // --------------
    // id attribute :
    // --------------

    const invalidIdValues =
        [
            '1header',
            '9section',
            '_sidebar',
            '-menu',
            ':content',
            '.title',
            'header@section',
            'content/page',
            'div#main',
            'form?field',
            '     spaces not allowed'
        ];
    invalidIdValues.forEach(value => {
        it(`"${value}" is not a valid value for the 'id' attribute - it is removed by the sanitizer`, () => {
            const text = `<div id="${value}">sample</div>`;
            modifyAndCheck(text, '<div>sample</div>');
        });
    });

    // -------------------
    // inline formatting :
    // -------------------

    // List of inline formatting tags are removed if they have no attribute. See HtmlPolicyBuilder#DEFAULT_SKIP_IF_EMPTY for details.
    const inlineFormattingTagsRemovedIfEmpty = ['font', 'span'];
    inlineFormattingTagsRemovedIfEmpty.forEach(tag => {
        it(`inline formatting tag "${tag}" is removed by the sanitizer if it has no attribute, kept otherwise`, () => {
            const text = `<${tag}>no attribute</${tag}><${tag} id="myid">with id</${tag}>`;
            modifyAndCheck(text, `no attribute<${tag} id="myid">with id</${tag}>`);
        });
    });

    it('<br> tag variants are normalized to <br /> by the sanitizer', () => {
        const text = '<p>text<br>with<br/>line<br />breaks</p>';
        modifyAndCheck(text, '<p>text<br />with<br />line<br />breaks</p>');
    });

    // -------------------
    // href links on <a> :
    // -------------------

    // The OWASP sanitizer encodes special characters in attribute values (e.g. @ -> &#64;, = -> &#61;)
    const allowedHrefOnAWithEncoding = [
        {input: 'mailto:johndoe@gmail.com', expected: 'mailto:johndoe&#64;gmail.com'},
        {input: 'http://mywebsite.io/mypage.html?p1=v1&p2=v2', expected: 'http://mywebsite.io/mypage.html?p1&#61;v1&amp;p2&#61;v2'}
    ];
    allowedHrefOnAWithEncoding.forEach(link => {
        it(`"${link.input}" is allowed as a[href] and its special characters are encoded by the sanitizer`, () => {
            const text = `<a href="${link.input}">sample</a>`;
            const expected = `<a href="${link.expected}">sample</a>`;
            modifyAndCheck(text, expected);
        });
    });

    // eslint-disable-next-line no-script-url
    const disallowedHrefOnA = ['irc://sample.link', 'ftp://sample.link', 'ftps://sample.link', 'ssh://sample.link', 'javascript:alert("Hack!");'];
    disallowedHrefOnA.forEach(link => {
        it(`"${link}" is not allowed as a[href] - the entire <a> tag is removed by the sanitizer`, () => {
            const text = `<a href="${link}">sample</a>`;
            modifyAndCheck(text, 'sample'); // The whole <a> tag is removed
        });
    });

    // -------
    // media :
    // -------

    const mediaTextForLink = (link: string) => `
    <audio controls="controls" muted="muted">
      <source src="${link}" srcset="${link}" type="audio/ogg"/>
    Your browser does not support the audio element.
    </audio>

    <img
      src="${link}"
      srcset="${link}"
      alt="alt text" />

    <video controls="controls" width="250" height="200" muted="muted">
      <source src="${link}" type="video/webm" />
    </video>

    <video controls="controls" src="${link}">
      <track
        default="default"
        kind="captions"
        srclang="en"
        src="${link}" />
      Download the
      <a href="${link}">MP4</a>
      video, and
      <a href="${link}">subtitles</a>.
    </video>

    <picture>
      <source
        srcset="${link}"
        media="(orientation: portrait)" />
      <img src="${link}" alt="alt text" />
    </picture>
    `;

    // eslint-disable-next-line no-script-url
    const disallowedMediaLinks = ['irc://sample.link', 'ftp://sample.link/audio.mp4', 'ftps://sample.link/sub/folder/logo.gif', 'ssh://sample.link', 'javascript:alert(\'hello\');'];
    disallowedMediaLinks.forEach(link => {
        it(`${link} is not an allowed media link - disallowed src/srcset attributes are removed by the sanitizer`, () => {
            const text = mediaTextForLink(link);
            const expectedText = `
    <audio controls="controls" muted="muted">
      <source type="audio/ogg"/>
    Your browser does not support the audio element.
    </audio>

    <img
      alt="alt text" />

    <video controls="controls" width="250" height="200" muted="muted">
      <source type="video/webm" />
    </video>

    <video controls="controls">
      <track
        default="default"
        kind="captions"
        srclang="en" />
      Download the
      MP4
      video, and
      subtitles.
    </video>

    <picture>
      <source
        media="(orientation: portrait)" />
      <img alt="alt text" />
    </picture>
    `;
            modifyAndCheck(text, expectedText);
        });
    });

    // ----------------------
    // <style> is protected :
    // ----------------------

    it('Style attribute: dangerous CSS properties are removed by the sanitizer while preserving safe styles', () => {
        const text = `
    <div style="color: blue; font-size: 14px; margin: 10px; border: 1px solid black;">Safe styling</div>

    <div style="background-image: url('https://evil.com/tracking.jpg');">Remote image loading</div>
    <div style="background: url('javascript:alert(1)');">JavaScript URL</div>
    <div style="position: fixed; top: 0; left: 0; z-index: 9999;">Position manipulation</div>
    <div style="-moz-binding: url('http://evil.com/xbl.xml#attack');">XBL binding</div>
    <div style="behavior: url(malicious.htc);">IE behavior</div>
    <div style="width: expression(alert('XSS'));">CSS expression</div>
    <div style="background-image: url(data:image/svg+xml,%3Csvg%20onload%3Dalert(1)%3E%3C/svg%3E);">Data URL with SVG</div>
    <div style="pointer-events: none;">Interaction hijacking</div>
    `;

        const expected = `
    <div style="color:blue;font-size:14px;margin:10px;border:1px solid black">Safe styling</div>

    <div>Remote image loading</div>
    <div>JavaScript URL</div>
    <div>Position manipulation</div>
    <div>XBL binding</div>
    <div>IE behavior</div>
    <div>CSS expression</div>
    <div>Data URL with SVG</div>
    <div>Interaction hijacking</div>
    `;

        modifyAndCheck(text, expected);
    });
});

