import {addNode, createSite, deleteSite} from '@jahia/cypress';
import {getContent, modifyContent} from '../fixtures/utils';

/**
 * Test scenarios for default filtering cases
 */
describe('Default HTML filtering', () => {
    const SITE_KEY = 'testHtmlFiltering';
    const RICH_TEXT_NODE = 'testRichTextNode';
    const path = `/sites/${SITE_KEY}/home/pagecontent/${RICH_TEXT_NODE}`;

    before(() => {
        // Create a site with an empty rich text component on the home page
        createSite(SITE_KEY, {locale: 'en', serverName: 'localhost', templateSet: 'html-filtering-test-module'});
        addNode({
            parentPathOrId: `/sites/${SITE_KEY}/home`,
            name: 'pagecontent',
            primaryNodeType: 'jnt:contentList',
            children: [
                {
                    name: RICH_TEXT_NODE,
                    primaryNodeType: 'jnt:bigText',
                    properties: [{name: 'text', value: 'test', language: 'en'}]
                }
            ]
        });
    });

    after(() => {
        deleteSite(SITE_KEY);
    });

    it('allows internal links - files', () => {
        // Note that the actual href text being sent over to the sanitizer is '##doc-context##/{workspace}/##ref:link1##'
        const text = `<p><a href="/files/{workspace}/sites/${SITE_KEY}/files/images/pdf/Conference%20Guide.pdf" ' +
            'title="Conference Guide.pdf">/files/{workspace}/sites/digitall/files/images/pdf/Conference%20Guide.pdf</a></p>`;
        modifyContent(path, text);
        getContent(path).then(result => {
            const value = result.data.jcr.nodeByPath.property.value;
            expect(value).to.contain('<p>');
            expect(value).to.contain('<a');
            expect(value).to.contain('href');
            expect(value).to.contain('title');
            expect(value).to.contain('{workspace}');
        });
    });

    it('allows internal links - content', () => {
        // Note that the actual href text being sent over to the sanitizer is '##cms-context##/{mode}/{lang}/##ref:link1##'
        const text = `<p><a href="/cms/{mode}/{lang}/sites/${SITE_KEY}/home.html" title="go to home page">home page</a></p>`;
        modifyContent(path, text);
        getContent(path).then(result => {
            const value = result.data.jcr.nodeByPath.property.value;
            expect(value).to.contain('<p>');
            expect(value).to.contain('<a');
            expect(value).to.contain('href');
            expect(value).to.contain('{mode}');
            expect(value).to.contain('{lang}');
        });
    });

    it('allows external links', () => {
        const text = '<p>This is a <a href="http://google.com" title="My google link">google link</a></p>';
        modifyContent(path, text);
        getContent(path).then(result => {
            const value = result.data.jcr.nodeByPath.property.value;
            expect(value).to.contain('<p>');
            expect(value).to.contain('<a');
            expect(value).to.contain('href');
            expect(value).to.contain('title');
        });
    });

    it('rejects invalid protocol links', () => {
        const text = '<p>This is an <a href="javascript://%0aalert(document.location)">xss test</a></p>';
        modifyContent(path, text);
        getContent(path).then(result => {
            const value = result.data.jcr.nodeByPath.property.value;
            expect(value).to.contain('<p>');
            expect(value).to.not.contain('<a');
            expect(value).to.not.contain('href');
        });
    });

    it('rejects invalid href links', () => {
        const text = '<p>This is an <a href="#javascript:alert(\'hello\')" target="_blank">xss test</a></p>';
        modifyContent(path, text);
        getContent(path).then(result => {
            const value = result.data.jcr.nodeByPath.property.value;
            expect(value).to.contain('<p>');
            expect(value).to.contain('<a');
            expect(value).to.not.contain('href');
            expect(value).to.contain('target');
        });
    });
});
