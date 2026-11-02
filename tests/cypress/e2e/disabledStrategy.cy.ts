import {addNode, createSite, deleteSite, getNodeByPath} from '@jahia/cypress';
import gql from 'graphql-tag';
import {
    expectHtmlValidationError,
    getContent,
    graphqlValidate,
    installConfig,
    modifyContent,
    removeGlobalCustomConfig,
    removeSiteConfig
} from '../fixtures/utils';

describe('Test the DISABLED strategy of the HTML filtering module', () => {
    const SITE_KEY = 'testDisabledStrategy';
    const CONFIG_CUSTOM_NAME = 'org.jahia.modules.htmlfiltering.global.custom.yml';
    const CONFIG_CUSTOM_PATH = `configs/disabledStrategy/${CONFIG_CUSTOM_NAME}`;
    const CONFIG_SITE_PATH = `configs/disabledStrategy/org.jahia.modules.htmlfiltering.site-${SITE_KEY}.yml`;
    const RICH_TEXT_NODE = 'testRichTextNode';
    const PATH = `/sites/${SITE_KEY}/home/pagecontent/${RICH_TEXT_NODE}`;
    const HTML_TEXT_UNSAFE = '<p>text</p><script>alert(1)</script>';
    const EXPECTED_WARNING = 'The html-filtering configuration org.jahia.modules.htmlfiltering.global.custom uses the DISABLED strategy in [editWorkspace, liveWorkspace]';

    // Retries, since the configuration may be loaded after the provisioning delay
    const expectLogContains = (text: string, attempts = 5) => {
        cy.executeGroovy('groovy/assertLogContains.groovy', {EXPECTED_TEXT: text}).then(result => {
            if (result !== '.installed' && attempts > 1) {
                // eslint-disable-next-line cypress/no-unnecessary-waiting
                cy.wait(2000);
                expectLogContains(text, attempts - 1);
            } else {
                expect(result, `jahia.log contains: ${text}`).to.equal('.installed');
            }
        });
    };

    const modifyLiveContent = (text: string) => {
        return cy.apollo({
            mutation: gql`
                mutation modifyLiveContent($pathOrId: String!, $text: String!) {
                    jcr(workspace: LIVE) {
                        mutateNode(pathOrId: $pathOrId) {
                            mutateProperty(name: "text") {
                                setValue(value: $text, language: "en")
                            }
                        }
                    }
                }
            `,
            variables: {pathOrId: PATH, text}
        });
    };

    const publishContent = () => {
        return cy.apollo({
            variables: {
                pathOrId: PATH,
                languages: 'en',
                publishSubNodes: true,
                includeSubTree: true
            },
            mutationFile: 'graphql/jcr/mutation/publishNode.graphql'
        });
    };

    // Retries, since a publication runs as a background job
    const expectContent = (expectedText: string, workspace: 'EDIT' | 'LIVE', attempts = 10) => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        getNodeByPath(PATH, ['text'], 'en', [], workspace as any).then(result => {
            const value = result?.data?.jcr?.nodeByPath?.properties?.[0]?.value;
            if (value !== expectedText && attempts > 1) {
                // eslint-disable-next-line cypress/no-unnecessary-waiting
                cy.wait(1000);
                expectContent(expectedText, workspace, attempts - 1);
            } else {
                expect(value).to.be.equal(expectedText);
            }
        });
    };

    before(() => {
        removeGlobalCustomConfig();
        removeSiteConfig(SITE_KEY);
        deleteSite(SITE_KEY);

        // Create a site with an empty rich text component on the home page to store the HTML text
        createSite(SITE_KEY, {locale: 'en', serverName: 'localhost', templateSet: 'html-filtering-test-module'});
        addNode({
            parentPathOrId: `/sites/${SITE_KEY}/home`,
            name: 'pagecontent',
            primaryNodeType: 'jnt:contentList',
            children: [
                {
                    name: RICH_TEXT_NODE,
                    primaryNodeType: 'jnt:bigText',
                    properties: [{name: 'text', value: '', language: 'en'}]
                }
            ]
        });
        // Publish the node upfront so it exists in LIVE before the tests write there
        publishContent();
        expectContent('', 'LIVE');
    });

    beforeEach(() => {
        removeGlobalCustomConfig();
        removeSiteConfig(SITE_KEY);
        installConfig(CONFIG_CUSTOM_PATH);
        // Reset the content, so that each test proves its own save
        modifyContent(PATH, '');
        modifyLiveContent('');
        expectContent('', 'EDIT');
        expectContent('', 'LIVE');
    });

    after(() => {
        removeGlobalCustomConfig();
        removeSiteConfig(SITE_KEY);
    });

    it('logs a warning that names the configuration and its workspaces with the DISABLED strategy', () => {
        expectLogContains(EXPECTED_WARNING);
    });

    it('keeps the HTML unchanged on a save in the edit workspace', () => {
        modifyContent(PATH, HTML_TEXT_UNSAFE);
        getContent(PATH).then(result => {
            expect(result.data.jcr.nodeByPath.property.value).to.be.equal(HTML_TEXT_UNSAFE);
        });
    });

    it('keeps the HTML unchanged on a save in the live workspace', () => {
        modifyLiveContent(HTML_TEXT_UNSAFE);
        expectContent(HTML_TEXT_UNSAFE, 'LIVE');
    });

    it('keeps the HTML unchanged on a publication', () => {
        cy.step('Modify content in the edit workspace', () => {
            modifyContent(PATH, HTML_TEXT_UNSAFE);
        });

        cy.step('Publish content', () => {
            publishContent();
        });

        cy.step('Validate the content in the live workspace', () => {
            expectContent(HTML_TEXT_UNSAFE, 'LIVE');
        });
    });

    it('still applies the rules in the GraphQL validate query', () => {
        ['EDIT', 'LIVE'].forEach(workspace => {
            graphqlValidate(HTML_TEXT_UNSAFE, SITE_KEY, workspace).its('data.htmlFiltering.validate').should(res => {
                expect(res.removedTags).to.contain('script');
                expect(res.sanitizedHtml).to.equal('<p>text</p>');
                expect(res.safe).to.be.false;
            });
        });
    });

    it('is overridden by a site configuration with the REJECT strategy', () => {
        cy.step('Install the site configuration', () => {
            installConfig(CONFIG_SITE_PATH);
        });

        cy.step('Modify content and expect a validation error', () => {
            modifyContent(PATH, '<script>alert(1)</script>').then(result => {
                expectHtmlValidationError(result);
            });
        });
    });
});
