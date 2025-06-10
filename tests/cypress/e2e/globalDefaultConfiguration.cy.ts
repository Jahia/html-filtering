import {addNode, createSite, deleteSite} from '@jahia/cypress';
import {mutateAndGetNodeProperty} from '../fixtures/utils';

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
        mutateAndGetNodeProperty(PATH, 'prop', text).then(result => {
            const resultNoSpace = result.replace(/\s/g, '');
            const expectedNoSpace = expected.replace(/\s/g, '');
            console.log('result', resultNoSpace);
            console.log('expected', expectedNoSpace);
            expect(resultNoSpace).to.eq(expectedNoSpace);
        });
    }

    // -------------------
    // global attributes :
    // -------------------

    it('Global attributes are allowed', () => {
        const text = `<div id="sample-element" accesskey="s" autocapitalize="sentences" autocorrect="on" autofocus="autofocus" class="demo-element highlight" dir="ltr" draggable="true" enterkeyhint="send" exportparts="header footer" hidden="hidden" inert="inert" inputmode="text" lang="en-US" nonce="abc123random456" part="custom-section" popover="auto" slot="content-area" spellcheck="true" style="color:navy;padding:10px" tabindex="0" title="Hover for more information" translate="yes" writingsuggestions="on">
      Sample with all the common global attributes
    </div>`;
        modifyAndCheck(text);
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

    const inlineFormattingTags = ['b', 'big', 'code', 'del', 'em', 'i', 'ins', 'o', 's', 'small', 'strike', 'strong', 'sub', 'sup', 'tt', 'u'];
    inlineFormattingTags.forEach(tag => {
        it(`inline formatting tag "${tag}" is allowed`, () => {
            const text = `<p>text with <${tag}>special formatting</${tag}></p>`;
            modifyAndCheck(text);
        });
    });
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

    // --------
    // blocks :
    // --------

    it('block tags are allowed', () => {
        const text = '<div>this is a div</div><blockquote cite="https://example.com/more.html"><p>my text</p></blockquote>';
        modifyAndCheck(text);
    });
    const blockHeaderTags = ['h1', 'h2', 'h3', 'h4', 'h5', 'h6'];
    blockHeaderTags.forEach(tag => {
        it(`header tag "${tag}" is allowed`, () => {
            const text = `<${tag}>title</${tag}>`;
            modifyAndCheck(text);
        });
    });
    it('list tags are allowed', () => {
        const text = `
    <ol><li>Coffee</li><li>Tea</li><li>Milk</li></ol>
    <ul><li>Coffee</li><li>Tea</li><li>Milk</li></ul>`;
        modifyAndCheck(text);
    });

    // -------
    // forms :
    // -------

    it('common form tags are allowed', () => {
        const text = `<form>
  <fieldset>
    <legend>Choose your favorite monster</legend>
    <input type="radio" id="kraken" name="monster" value="K" />
    <label for="kraken">Kraken</label><br />
    <input type="radio" id="sasquatch" name="monster" value="S" />
    <label for="sasquatch">Sasquatch</label><br />

    <label for="pet-select">Choose a pet:</label>
    <select id="pet-select"><option value="dog">Dog</option><option value="cat">Cat</option></select>

    <label for="story">Tell us your story:</label>
    <textarea id="story" name="story" rows="5" cols="33">
    It was a dark and stormy night...
    </textarea>

    <input type="range" id="b" name="b" value="50" /> plus
    <input type="number" id="a" name="a" value="10" /> minus
    <output name="result" for="a b">60</output>

    <label for="file">File progress:</label>
    <progress id="file" max="100" value="70">70%</progress>

    <label for="fuel">Fuel level:</label>
    <meter id="fuel" min="0" max="100" low="33" high="66" optimum="80" value="50">
      at 50/100
    </meter>
  </fieldset>
</form>`;
        // The following HTML should be allowed in the <form> but is currently not:
        // <select id="dino-select">
        //   <optgroup label="Theropods">
        //     <option>Tyrannosaurus</option>
        //     <option>Velociraptor</option>
        //     <option>Deinonychus</option>
        //     </optgroup>
        //     <optgroup label="Sauropods">
        //     <option>Diplodocus</option>
        //     <option>Saltasaurus</option>
        //     <option>Apatosaurus</option>
        //   </optgroup>
        // </select>
        // <label for="ice-cream-choice">Choose a flavor:</label>
        // <input list="ice-cream-flavors" id="ice-cream-choice" name="ice-cream-choice" />
        // <datalist id="ice-cream-flavors">
        //   <option value="Chocolate"></option>
        //   <option value="Coconut"></option>
        //   <option value="Mint"></option>
        //   <option value="Strawberry"></option>
        //   <option value="Vanilla"></option>
        // </datalist>
        // TODO add it once the library is fixed. See https://github.com/OWASP/java-html-sanitizer/issues/358
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
