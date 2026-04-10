import {addNode, createSite, deleteSite} from '@jahia/cypress';
import {mutateNodeProperty} from '../fixtures/utils';

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
            modifyAndCheck(text, '<div>sample</div>');
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
            modifyAndCheck(text, `no attribute<${tag} id="myid">with id</${tag}>`);
        });
    });
    it('inline formatting line breaks <br> tag is allowed and formatted', () => {
        const text = '<p>text<br>with<br/>line<br />breaks</p>';
        modifyAndCheck(text, '<p>text<br />with<br />line<br />breaks</p>');
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

    const allowedHrefOnA = [{input: '/path/of/link.html', expected: '/path/of/link.html'}, {
        input: 'http://sample.link',
        expected: 'http://sample.link'
    }, {input: 'https://sample.link', expected: 'https://sample.link'}, {
        input: 'mailto:johndoe@gmail.com',
        expected: 'mailto:johndoe&#64;gmail.com'
    }, {
        input: 'https://sample.com/sub/path/asset.pdf',
        expected: 'https://sample.com/sub/path/asset.pdf'
    }, {
        input: 'http://mywebsite.io/mypage.html?p1=v1&p2=v2',
        expected: 'http://mywebsite.io/mypage.html?p1&#61;v1&amp;p2&#61;v2'
    }];
    allowedHrefOnA.forEach(link => {
        it(`"${link.input}" is allowed as a[href]`, () => {
            const text = `<a href="${link.input}">sample</a>`;
            const expected = `<a href="${link.expected}">sample</a>`;
            modifyAndCheck(text, expected);
        });
    });
    // eslint-disable-next-line no-script-url
    const disallowedHrefOnA = ['irc://sample.link', 'ftp://sample.link', 'ftps://sample.link', 'ssh://sample.link', 'javascript:alert("Hack!");'];
    disallowedHrefOnA.forEach(link => {
        it(`"${link}" is not allowed as a[href]`, () => {
            const text = `<a href="${link}">sample</a>`;
            modifyAndCheck(text, 'sample'); // The whole <a> tag should be removed
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

    it('Style attribute is protected against CSS-based attacks while preserving safe styles', () => {
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
