import {addNode, createSite, deleteSite} from '@jahia/cypress';
import {getContent, installConfig, modifyContent, removeDefaultConfig, removeSiteConfig} from '../fixtures/utils';
// NB: this is not intended to be a comprehensive tests suite for the sanitization, but rather a quick sanity check
//     to ensure that the right policy is used for a given site, depending on the configuration in place.
//     For comprehensive tests, see PolicyImplTest.
describe('Test the configuration strategy used by the HTML filtering module', () => {
    const SITE_KEY = 'testHtmlFilteringConfigurationStrategy';
    const OTHER_SITE = 'otherSite'; // Site that does not have a configuration for the module
    const RICH_TEXT_NODE = 'testRichTextNode';
    const PATH = `/sites/${SITE_KEY}/home/pagecontent/${RICH_TEXT_NODE}`;
    const HTML_TEXT = '<h1 id="@invalid">my title</h1><p id="abc" class="myClass">my text</p>';
    const EXPECTED_HTML_TEXT_WITH_FALLBACK = '<h1>my title</h1><p id="abc" class="myClass">my text</p>';
    const EXPECTED_HTML_TEXT_WITH_DEFAULT = 'my title<p id="abc">my text</p>';
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

    it('when no configuration is provided, the HTML text is sanitized using the fallback strategy', () => {
        modifyContent(PATH, HTML_TEXT);
        getContent(PATH).then(result => {
            const value = result.data.jcr.nodeByPath.property.value;
            expect(value).to.be.equal(EXPECTED_HTML_TEXT_WITH_FALLBACK);
        });
    });

    it('when only a default configuration is provided, the HTML text is sanitized using the default strategy', () => {
        installConfig('configs/configurationStrategy/org.jahia.modules.htmlfiltering.default.yml');

        modifyContent(PATH, HTML_TEXT);
        getContent(PATH).then(result => {
            const value = result.data.jcr.nodeByPath.property.value;
            expect(value).to.be.equal(EXPECTED_HTML_TEXT_WITH_DEFAULT);
        });
        removeDefaultConfig();
    });

    it('when only a per-site configuration is provided, the HTML text is sanitized using the per-site strategy', () => {
        installConfig(`configs/configurationStrategy/org.jahia.modules.htmlfiltering-${SITE_KEY}.yml`);

        modifyContent(PATH, HTML_TEXT);
        getContent(PATH).then(result => {
            const value = result.data.jcr.nodeByPath.property.value;
            expect(value).to.be.equal(EXPECTED_HTML_TEXT_WITH_PER_SITE);
        });

        removeSiteConfig(SITE_KEY);
    });

    it('when a default and a per-site configuration is provided, the HTML text is sanitized using the per-site strategy', () => {
        installConfig('configs/configurationStrategy/org.jahia.modules.htmlfiltering.default.yml');
        installConfig(`configs/configurationStrategy/org.jahia.modules.htmlfiltering-${SITE_KEY}.yml`);

        modifyContent(PATH, HTML_TEXT);
        getContent(PATH).then(result => {
            const value = result.data.jcr.nodeByPath.property.value;
            expect(value).to.be.equal(EXPECTED_HTML_TEXT_WITH_PER_SITE);
        });

        removeSiteConfig(SITE_KEY);
        removeDefaultConfig();
    });

    it('when only a per-site configuration for another site is provided, the HTML text is sanitized using the fallback strategy', () => {
        installConfig(`configs/configurationStrategy/org.jahia.modules.htmlfiltering-${OTHER_SITE}.yml`);

        modifyContent(PATH, HTML_TEXT);
        getContent(PATH).then(result => {
            const value = result.data.jcr.nodeByPath.property.value;
            expect(value).to.be.equal(EXPECTED_HTML_TEXT_WITH_FALLBACK);
        });

        removeSiteConfig(OTHER_SITE);
    });

    it('when a default and a per-site configuration for another site is provided, the HTML text is sanitized using the default strategy', () => {
        installConfig('configs/configurationStrategy/org.jahia.modules.htmlfiltering.default.yml');
        installConfig(`configs/configurationStrategy/org.jahia.modules.htmlfiltering-${OTHER_SITE}.yml`);

        modifyContent(PATH, HTML_TEXT);
        getContent(PATH).then(result => {
            const value = result.data.jcr.nodeByPath.property.value;
            expect(value).to.be.equal(EXPECTED_HTML_TEXT_WITH_DEFAULT);
        });

        removeSiteConfig(OTHER_SITE);
        removeDefaultConfig();
    });
});
