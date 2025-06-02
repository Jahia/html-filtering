import {addNode, createSite, deleteSite} from '@jahia/cypress';
import {mutateNodeTextProperty} from '../fixtures/utils';

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
        mutateNodeTextProperty(PATH, 'prop', text).then(result => {
            const resultNoSpace = result.replace(/\s/g, '');
            const expectedNoSpace = expected.replace(/\s/g, '');
            console.log('result', resultNoSpace);
            console.log('expected', expectedNoSpace);
            expect(resultNoSpace).to.eq(expectedNoSpace);
        });
    }

    // -----------
    // protocols :
    // -----------

    const allowedProtocols = ['http', 'https', 'mailto'];
    allowedProtocols.forEach(protocol => {
        it(`"${protocol}" is an allowed protocol`, () => {
            const text = `<a href="${protocol}://sample.link">sample</a>`;
            modifyAndCheck(text);
        });
    });
    const disallowedProtocols = ['irc', 'ftp', 'ftps', 'ssh']; // Non exhaustive list
    disallowedProtocols.forEach(protocol => {
        it(`"${protocol}" is not an allowed protocol`, () => {
            const text = `<a href="${protocol}://sample.link">sample</a>`;
            modifyAndCheck(text, 'sample'); // The whole <a> tag should be removed
        });
    });

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

    const validHeightValues = ['123', '123.45', '25%', 'auto'];
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
});
