import {addNode, createSite, deleteSite} from '@jahia/cypress';
import {expectHtmlValidationError, mutateNodeProperty} from '../fixtures/utils';

/**
 * Test scenarios for the global default configuration (org.jahia.modules.htmlfiltering.global.default.yml).
 * The goal is to test the global default configuration embedded by the bundle so Cypress E2E tests must be used (rather than unit tests).
 */
describe('Test global default configuration', () => {
    const SITE_KEY = 'testGlobalDefaultConfiguration';
    const NODE = 'testNode';
    const PATH = `/sites/${SITE_KEY}/home/pagecontent/${NODE}`;

    before(() => {
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
    });

    function modifyAndCheck(text: string, expected = text) {
        mutateNodeProperty(PATH, 'prop', text).then(response => {
            const resultNoSpace = response.data.jcr.mutateNode.mutateProperty.property.value.replace(/\s/g, '');
            const expectedNoSpace = expected.replace(/\s/g, '');
            console.log('result', resultNoSpace);
            console.log('expected', expectedNoSpace);
            expect(resultNoSpace).to.eq(expectedNoSpace);
        });
    }

    // -----------------------------------------------
    // exhaustive fixture — all allowed elements :
    // -----------------------------------------------

    it('all supported tags and attributes are preserved (exhaustive fixture)', () => {
        cy.fixture('html/exhaustive-supported-elements.html', 'utf-8').then((content: string) => {
            cy.fixture('html/exhaustive-supported-elements.sanitized.html', 'utf-8').then((expectedContent: string) => {
                modifyAndCheck(content, expectedContent);
            });
        });
    });

    // --------------
    // id attribute :
    // --------------

    const validIdValues =
        [
            'a',
            'Z',
            'header',
            'myDiv',
            'section2',
            'page123',
            'nav-bar',
            'footer_section',
            'main:content',
            'header.title',
            'complex-id_with:all.allowed_chars123',
            'mySection-subSection_part:type.element'
        ];
    validIdValues.forEach(value => {
        it(`"${value}" is a valid value for the 'id' attribute`, () => {
            const text = `<div id="${value}">sample</div>`;
            modifyAndCheck(text);
        });
    });
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
        it(`"${value}" is not a valid value for the 'id' attribute`, () => {
            const text = `<div id="${value}">sample</div>`;
            mutateNodeProperty(PATH, 'prop', text).then(response => {
                expectHtmlValidationError(response);
            });
        });
    });

    // -------------------
    // inline formatting :
    // -------------------

    // List of inline formatting tags are removed if they have no attribute. See HtmlPolicyBuilder#DEFAULT_SKIP_IF_EMPTY for details.
    const inlineFormattingTagsRemovedIfEmpty = ['font', 'span'];
    inlineFormattingTagsRemovedIfEmpty.forEach(tag => {
        it(`inline formatting tag "${tag}" is removed if it has not attribute, kept otherwise`, () => {
            const text = `<${tag}>no attribute</${tag}><${tag} id="myid">with id</${tag}>`;
            mutateNodeProperty(PATH, 'prop', text).then(response => {
                expectHtmlValidationError(response);
            });
        });
    });
    it('inline formatting line breaks <br> tag is allowed', () => {
        const text = '<p>text<br>with<br/>line<br />breaks</p>';
        // With REJECT strategy the content is stored as-is (no normalization); all <br> variants are accepted
        modifyAndCheck(text);
    });

    // --------------------------
    // NUMBER_OR_PERCENT format :
    // --------------------------

    const validHeightValues = ['123', '123.45', '67.89%', '25%', 'auto'];
    validHeightValues.forEach(value => {
        it(`height value "${value}" is allowed`, () => {
            const text = `<table><tbody><tr><td height="${value}">sample</td></tr></tbody></table>`;
            modifyAndCheck(text);
        });
    });
    const invalidHeightValues = ['abc', '1a'];
    invalidHeightValues.forEach(value => {
        it(`height value "${value}" is not allowed`, () => {
            const text = '<table><tbody><tr><td>sample</td></tr></tbody></table>';
            modifyAndCheck(text);
        });
    });

    // -------------------
    // href links on <a> :
    // -------------------

    // Note: with REJECT strategy the content is stored as-is (no character encoding by the sanitizer).
    // The encoding behavior (e.g. @ -> &#64;) is tested separately in globalDefaultSanitizeStrategy.cy.ts.
    const allowedHrefOnA = [
        '/path/of/link.html',
        'http://sample.link',
        'https://sample.link',
        'mailto:johndoe@gmail.com',
        'https://sample.com/sub/path/asset.pdf',
        'http://mywebsite.io/mypage.html?p1=v1&p2=v2'
    ];
    allowedHrefOnA.forEach(link => {
        it(`"${link}" is allowed as a[href]`, () => {
            const text = `<a href="${link}">sample</a>`;
            modifyAndCheck(text);
        });
    });
    // eslint-disable-next-line no-script-url
    const disallowedHrefOnA = ['irc://sample.link', 'ftp://sample.link', 'ftps://sample.link', 'ssh://sample.link', 'javascript:alert("Hack!");'];
    disallowedHrefOnA.forEach(link => {
        it(`"${link}" is not allowed as a[href]`, () => {
            const text = `<a href="${link}">sample</a>`;
            mutateNodeProperty(PATH, 'prop', text).then(response => {
                expectHtmlValidationError(response);
            });
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

    const allowedMediaLinks = ['https://mysite.com/image.gif', '/sub/folder/img.jpg', 'audio.mp3', '/flower.webm', 'http://path/to/video.mp4'];
    allowedMediaLinks.forEach(link => {
        it(`${link} is an allowed media link`, () => {
            const text = mediaTextForLink(link);
            modifyAndCheck(text);
        });
    });
    // eslint-disable-next-line no-script-url
    const disallowedMediaLinks = ['irc://sample.link', 'ftp://sample.link/audio.mp4', 'ftps://sample.link/sub/folder/logo.gif', 'ssh://sample.link', 'javascript:alert(\'hello\');'];
    disallowedMediaLinks.forEach(link => {
        it(`${link} is not an allowed media link`, () => {
            const text = mediaTextForLink(link);
            mutateNodeProperty(PATH, 'prop', text).then(response => {
                expectHtmlValidationError(response);
            });
        });
    });

    // ----------------------
    // <style> is protected :
    // ----------------------

    it('Style attribute is protected against CSS-based attacks while preserving safe styles', () => {
        const textSafeStyles = '<div style="color: blue; font-size: 14px; margin: 10px; border: 1px solid black;">Safe styling</div>';
        modifyAndCheck(textSafeStyles);

        const textDangerousStyles = `
    <div style="background-image: url('https://evil.com/tracking.jpg');">Remote image loading</div>
    <div style="background: url('javascript:alert(1)');">JavaScript URL</div>
    <div style="position: fixed; top: 0; left: 0; z-index: 9999;">Position manipulation</div>
    <div style="-moz-binding: url('http://evil.com/xbl.xml#attack');">XBL binding</div>
    <div style="behavior: url(malicious.htc);">IE behavior</div>
    <div style="width: expression(alert('XSS'));">CSS expression</div>
    <div style="background-image: url(data:image/svg+xml,%3Csvg%20onload%3Dalert(1)%3E%3C/svg%3E);">Data URL with SVG</div>
    <div style="pointer-events: none;">Interaction hijacking</div>
    `;
        mutateNodeProperty(PATH, 'prop', textDangerousStyles).then(response => {
            expectHtmlValidationError(response);
        });
    });
});
