import {addNode, createSite, deleteSite} from '@jahia/cypress';
import {expectHtmlValidationError, getContent, installConfig, modifyContent, removeGlobalCustomConfig, removeSiteConfig} from '../fixtures/utils';

// NB: this is not intended to be a comprehensive tests suite for the sanitization, but rather a quick sanity check
//     to ensure that the right policy is used for a given site, depending on the configuration in place.
//     For comprehensive tests, see PolicyImplTest.
describe('Test the configuration strategy used by the HTML filtering module', () => {
    const SITE_KEY = 'testHtmlFilteringConfigurationStrategy';
    const OTHER_SITE = 'otherSite'; // Site that does not have a configuration for the module
    const CONFIG_CUSTOM_NAME = 'org.jahia.modules.htmlfiltering.global.custom.yml';
    const CONFIG_CUSTOM_PATH = `configs/configurationStrategy/${CONFIG_CUSTOM_NAME}`;
    const CONFIG_SITE_NAME = `org.jahia.modules.htmlfiltering.site-${SITE_KEY}.yml`;
    const CONFIG_SITE_PATH = `configs/configurationStrategy/${CONFIG_SITE_NAME}`;
    const CONFIG_SITE_PATH_INVALID = `configs/configurationStrategy/invalid/${CONFIG_SITE_NAME}`;
    const CONFIG_OTHER_SITE_NAME = `org.jahia.modules.htmlfiltering.site-${OTHER_SITE}.yml`;
    const CONFIG_OTHER_SITE_PATH = `configs/configurationStrategy/${CONFIG_OTHER_SITE_NAME}`;
    const RICH_TEXT_NODE = 'testRichTextNode';
    const PATH = `/sites/${SITE_KEY}/home/pagecontent/${RICH_TEXT_NODE}`;
    const HTML_TEXT_INVALID = '<h1 id="@invalid">my title</h1><p id="abc" class="myClass">my text</p>';
    const HTML_TEXT_VALID = '<h1 id="validId">my title</h1><p id="abc" class="myClass">my text</p>';
    const INVALID_ID_MESSAGE = 'Unauthorized attribute "id" for tag <h1>.';
    const EXPECTED_HTML_TEXT_WITH_GLOBAL_CUSTOM = 'my title<p id="abc">my text</p>';
    const EXPECTED_HTML_TEXT_WITH_PER_SITE = '<h1>my title</h1><p>my text</p>';

    before(() => {
        // Cleanup any previous state (configs will be cleaned up in beforeEach hook)
        deleteSite(SITE_KEY);

        // Create a site with an empty rich text component on the home page to store the HTML text to be filtered
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
    });

    beforeEach(() => {
        // Each test-case might install a configuration which might change the global state and affect the next test.
        // So these have to be cleaned up just in case to avoid flaky tests.
        removeGlobalCustomConfig();
        removeSiteConfig(SITE_KEY);
        removeSiteConfig(OTHER_SITE);
    });

    it('when no configuration is provided, the REJECT strategy should reject invalid HTML', () => {
        modifyContent(PATH, HTML_TEXT_INVALID).then(result => {
            expectHtmlValidationError(result, INVALID_ID_MESSAGE);
        });
        getContent(PATH).then(result => {
            const value = result.data.jcr.nodeByPath.property.value;
            expect(value, 'the content should not have been modified').to.be.equal('');
        });
    });

    it('when no configuration is provided, valid HTML should be accepted using the global default strategy', () => {
        modifyContent(PATH, HTML_TEXT_VALID);
        getContent(PATH).then(result => {
            expect(result.data.jcr.nodeByPath.property.value).to.be.equal(HTML_TEXT_VALID);
        });
    });

    it('when only a global custom configuration is provided, the HTML text is sanitized using the global custom strategy', () => {
        cy.step('Install config(s)', () => {
            installConfig(CONFIG_CUSTOM_PATH);
        });

        cy.step('Modify and validate content', () => {
            modifyContent(PATH, HTML_TEXT_INVALID);
            getContent(PATH).then(result => {
                const value = result.data.jcr.nodeByPath.property.value;
                expect(value).to.be.equal(EXPECTED_HTML_TEXT_WITH_GLOBAL_CUSTOM);
            });
        });
    });

    it('when only a per-site configuration is provided, the HTML text is sanitized using the per-site strategy', () => {
        cy.step('Install config(s)', () => {
            installConfig(CONFIG_SITE_PATH);
        });

        cy.step('Modify and validate content', () => {
            modifyContent(PATH, HTML_TEXT_INVALID);
            getContent(PATH).then(result => {
                const value = result.data.jcr.nodeByPath.property.value;
                expect(value).to.be.equal(EXPECTED_HTML_TEXT_WITH_PER_SITE);
            });
        });
    });

    it('when a global custom and a per-site configuration is provided, the HTML text is sanitized using the per-site strategy', () => {
        cy.step('Install config(s)', () => {
            installConfig(CONFIG_CUSTOM_PATH);
            installConfig(CONFIG_SITE_PATH);
        });

        cy.step('Modify and validate content', () => {
            modifyContent(PATH, HTML_TEXT_INVALID);
            getContent(PATH).then(result => {
                const value = result.data.jcr.nodeByPath.property.value;
                expect(value).to.be.equal(EXPECTED_HTML_TEXT_WITH_PER_SITE);
            });
        });
    });

    it('when only a per-site configuration for another site is provided, the HTML text is sanitized using the global default strategy', () => {
        cy.step('Install config(s)', () => {
            installConfig(CONFIG_OTHER_SITE_PATH);
        });

        cy.step('Modify and validate content', () => {
            modifyContent(PATH, HTML_TEXT_INVALID).then(result => {
                // With REJECT strategy, this should fail validation
                expectHtmlValidationError(result);
            });
        });
    });

    it('when a global custom and a per-site configuration for another site is provided, the HTML text is sanitized using the global custom strategy', () => {
        cy.step('Install config(s)', () => {
            installConfig(CONFIG_CUSTOM_PATH);
            installConfig(CONFIG_OTHER_SITE_PATH);
        });

        cy.step('Modify and validate content', () => {
            modifyContent(PATH, HTML_TEXT_INVALID);
            getContent(PATH).then(result => {
                const value = result.data.jcr.nodeByPath.property.value;
                expect(value).to.be.equal(EXPECTED_HTML_TEXT_WITH_GLOBAL_CUSTOM);
            });
        });
    });

    it('when only an invalid per-site configuration is provided after installing a valid one, the HTML text is sanitized using the global default strategy (the config is ignored)', () => {
        cy.step('Install config(s)', () => {
            installConfig(CONFIG_SITE_PATH); // Valid
            installConfig(CONFIG_SITE_PATH_INVALID); // Invalid
        });

        cy.step('Modify and validate content', () => {
            modifyContent(PATH, HTML_TEXT_INVALID).then(result => {
                // With REJECT strategy (global default), this should fail validation
                expectHtmlValidationError(result);
            });
        });
    });
});
