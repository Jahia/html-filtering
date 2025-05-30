import {addNode, createSite, deleteSite} from '@jahia/cypress';
import {getContent, installConfig, modifyContent, removeGlobalCustomConfig, removeSiteConfig, mutateNodeTextProperty} from '../fixtures/utils';
// NB: this is not intended to be a comprehensive tests suite for the sanitization, but rather a quick sanity check
//     to ensure that the right policy is used for a given site, depending on the configuration in place.
//     For comprehensive tests, see PolicyImplTest.
describe('Test the configuration strategy used by the HTML filtering module', () => {
    const SITE_KEY = 'testHtmlFilteringConfigurationStrategy';
    const OTHER_SITE = 'otherSite'; // Site that does not have a configuration for the module
    const RICH_TEXT_NODE = 'testRichTextNode';
    const PATH = `/sites/${SITE_KEY}/home/pagecontent/${RICH_TEXT_NODE}`;
    const HTML_TEXT = '<h1 id="@invalid">my title</h1><p id="abc" class="myClass">my text</p>';
    const EXPECTED_HTML_TEXT_WITH_GLOBAL_DEFAULT = '<h1>my title</h1><p id="abc" class="myClass">my text</p>';
    const EXPECTED_HTML_TEXT_WITH_GLOBAL_CUSTOM = 'my title<p id="abc">my text</p>';
    const EXPECTED_HTML_TEXT_WITH_PER_SITE = '<h1>my title</h1><p>my text</p>';

    before(() => {
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
    after(() => {
        deleteSite(SITE_KEY);
    });

    it('when no configuration is provided, the HTML text is sanitized using the global default strategy', () => {
        modifyContent(PATH, HTML_TEXT);
        getContent(PATH).then(result => {
            const value = result.data.jcr.nodeByPath.property.value;
            expect(value).to.be.equal(EXPECTED_HTML_TEXT_WITH_GLOBAL_DEFAULT);
        });
    });

    it('when only a global custom configuration is provided, the HTML text is sanitized using the global custom strategy', () => {
        installConfig('configs/configurationStrategy/org.jahia.modules.htmlfiltering.global.custom.yml');

        modifyContent(PATH, HTML_TEXT);
        getContent(PATH).then(result => {
            const value = result.data.jcr.nodeByPath.property.value;
            expect(value).to.be.equal(EXPECTED_HTML_TEXT_WITH_GLOBAL_CUSTOM);
        });
        removeGlobalCustomConfig();
    });

    it('when only a per-site configuration is provided, the HTML text is sanitized using the per-site strategy', () => {
        installConfig(`configs/configurationStrategy/org.jahia.modules.htmlfiltering.site-${SITE_KEY}.yml`);

        modifyContent(PATH, HTML_TEXT);
        getContent(PATH).then(result => {
            const value = result.data.jcr.nodeByPath.property.value;
            expect(value).to.be.equal(EXPECTED_HTML_TEXT_WITH_PER_SITE);
        });

        removeSiteConfig(SITE_KEY);
    });

    it('when a global custom and a per-site configuration is provided, the HTML text is sanitized using the per-site strategy', () => {
        installConfig('configs/configurationStrategy/org.jahia.modules.htmlfiltering.global.custom.yml');
        installConfig(`configs/configurationStrategy/org.jahia.modules.htmlfiltering.site-${SITE_KEY}.yml`);

        modifyContent(PATH, HTML_TEXT);
        getContent(PATH).then(result => {
            const value = result.data.jcr.nodeByPath.property.value;
            expect(value).to.be.equal(EXPECTED_HTML_TEXT_WITH_PER_SITE);
        });

        removeSiteConfig(SITE_KEY);
        removeGlobalCustomConfig();
    });

    it('when only a per-site configuration for another site is provided, the HTML text is sanitized using the global default strategy', () => {
        installConfig(`configs/configurationStrategy/org.jahia.modules.htmlfiltering.site-${OTHER_SITE}.yml`);

        modifyContent(PATH, HTML_TEXT);
        getContent(PATH).then(result => {
            const value = result.data.jcr.nodeByPath.property.value;
            expect(value).to.be.equal(EXPECTED_HTML_TEXT_WITH_GLOBAL_DEFAULT);
        });

        removeSiteConfig(OTHER_SITE);
    });

    it('when a global custom and a per-site configuration for another site is provided, the HTML text is sanitized using the global custom strategy', () => {
        installConfig('configs/configurationStrategy/org.jahia.modules.htmlfiltering.global.custom.yml');
        installConfig(`configs/configurationStrategy/org.jahia.modules.htmlfiltering.site-${OTHER_SITE}.yml`);

        modifyContent(PATH, HTML_TEXT);
        getContent(PATH).then(result => {
            const value = result.data.jcr.nodeByPath.property.value;
            expect(value).to.be.equal(EXPECTED_HTML_TEXT_WITH_GLOBAL_CUSTOM);
        });

        removeSiteConfig(OTHER_SITE);
        removeGlobalCustomConfig();
    });

    it('when only an invalid per-site configuration is provided after installing a valid one, the HTML text is sanitized using the global default strategy (the config is ignored)', () => {
        cy.step('Install configs', () => {
            installConfig(`configs/configurationStrategy/org.jahia.modules.htmlfiltering.site-${SITE_KEY}.yml`); // Valid
            installConfig(`configs/configurationStrategy/invalid/org.jahia.modules.htmlfiltering.site-${SITE_KEY}.yml`); // Invalid
        });

        cy.step('Modify and validate content', () => {
            modifyContent(PATH, HTML_TEXT);
            getContent(PATH).then(result => {
                const value = result.data.jcr.nodeByPath.property.value;
                expect(value).to.be.equal(EXPECTED_HTML_TEXT_WITH_GLOBAL_DEFAULT);
            });
        });

        cy.step('Remove configs', () => {
            removeSiteConfig(SITE_KEY);
        });
    });
});
