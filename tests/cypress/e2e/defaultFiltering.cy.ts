import {addNode, deleteNode} from '@jahia/cypress';
import {
    disableHtmlFiltering,
    enableHtmlFiltering,
    getContent,
    modifyContent
} from '../fixtures/utils';

/**
 * Test scenarios for default filtering cases
 */
describe('Default HTML filtering', () => {
    const siteKey = 'digitall';
    const textName = 'myText';
    const path = `/sites/${siteKey}/contents/${textName}`;

    before(() => {
        addNode({
            parentPathOrId: `/sites/${siteKey}/contents`,
            primaryNodeType: 'jnt:bigText',
            name: textName,
            properties: [{name: 'text', value: '<p>hello there</p>', language: 'en'}]
        });
        enableHtmlFiltering(siteKey);
    });

    after(() => {
        disableHtmlFiltering(siteKey);
        deleteNode(path);
    });

    it('allows asset links', () => {
        // note that the actual href text being sent over to the sanitizer is '##doc-context##/{workspace}/##ref:link1##'
        const text = '<p><a href="/files/{workspace}/sites/digitall/files/images/pdf/Conference%20Guide.pdf" ' +
            'title="Conference Guide.pdf">/files/{workspace}/sites/digitall/files/images/pdf/Conference%20Guide.pdf</a></p>';
        modifyContent(path, text);
        getContent(path).then(result => {
            expect(result.data.jcr.nodeByPath.property.value).to.contain('href');
        });
    });
});
