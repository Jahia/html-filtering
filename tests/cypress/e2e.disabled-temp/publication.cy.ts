import {installConfig, removeSiteConfig, removeGlobalCustomConfig, modifyContent} from '../fixtures/utils';
import {createSite, deleteSite, addNode, getNodeByPath} from '@jahia/cypress';

const SITE_KEY = 'testPublication';
const RICH_TEXT_NODE = 'testRichTextNode';
const NODE_PATH = `/sites/${SITE_KEY}/home/pagecontent/${RICH_TEXT_NODE}`;
const CONFIG_NAME = `org.jahia.modules.htmlfiltering.site-${SITE_KEY}.yml`;
const CONFIG_PATH_SANITIZE = `configs/publication/sanitize/${CONFIG_NAME}`;
const CONFIG_PATH_REJECT = `configs/publication/reject/${CONFIG_NAME}`;

const HTML_TEXT_ORIGINAL = '<h1>h1-text</h1><p>paragraph-text</p>';
const HTML_TEXT_EXPECTED = '<h1>h1-text</h1>paragraph-text';

// Publishes the content with the given HTML text in the rich text node
const publishContent = () => {
    return cy.apollo({
        variables: {
            pathOrId: NODE_PATH,
            languages: 'en',
            publishSubNodes: true,
            includeSubTree: true
        },
        mutationFile: 'graphql/jcr/mutation/publishNode.graphql'
    });
};

// Adds a rich text node with the specified text to the content list
const addContent = (text: string) => {
    addNode({
        parentPathOrId: `/sites/${SITE_KEY}/home`,
        name: 'pagecontent',
        primaryNodeType: 'jnt:contentList',
        children: [
            {
                name: RICH_TEXT_NODE,
                primaryNodeType: 'jnt:bigText',
                properties: [{name: 'text', value: text, language: 'en'}]
            }
        ]
    });
};

// Validates the content of the rich text node in the specified workspace
const validateContent = (expectedText: string, workspace: string) => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    getNodeByPath(NODE_PATH, ['text'], 'en', [], workspace as any).then(result => {
        const value = result?.data?.jcr?.nodeByPath?.properties[0]?.value;
        expect(value).to.be.equal(expectedText);
    });
};

describe('Test publications with HTML filtering module', () => {
    before(() => {
        // Clean up any previous configurations
        removeGlobalCustomConfig();
        removeSiteConfig(SITE_KEY);
        deleteSite(SITE_KEY);

        // Create a site with rich text component on the home page to store the HTML text to be filtered
        createSite(SITE_KEY, {locale: 'en', serverName: 'localhost', templateSet: 'html-filtering-test-module'});
        addContent('');
    });

    // EDIT workspace: SANITIZE everything, except [h1], LIVE: SANITIZE or REJECT everything, except [p]
    // Add content with tags that are allowed in EDIT workspace, but not in LIVE workspace (e.g. <h1>, <p>)
    // (SANITIZED) content can be PUBLISHED in LIVE workspace, NO tags to be SANITIZED or REJECTED by LIVE config
    ['SANITIZE', 'REJECT'].forEach(strategy => {
        it(`should publish content as is even if tags are restricted (${strategy}) in live config`, () => {
            cy.step('Install config', () => {
                installConfig(strategy === 'SANITIZE' ? CONFIG_PATH_SANITIZE : CONFIG_PATH_REJECT);
            });

            cy.step('Modify content in edit mode', () => {
                modifyContent(NODE_PATH, HTML_TEXT_ORIGINAL);
            });

            cy.step('Publish content', () => {
                publishContent();
            });

            cy.step('Validate published content in EDIT workspace', () => {
                validateContent(HTML_TEXT_EXPECTED, 'EDIT');
            });

            cy.step('Validate published content in LIVE worksapace', () => {
                validateContent(HTML_TEXT_EXPECTED, 'LIVE');
            });
        });
    });
});
