import {addNode, createSite, deleteSite} from '@jahia/cypress';
import {getContent, installConfig, modifyContent, removeGlobalCustomConfig, removeSiteConfig, readYAMLConfig, installYAMLConfig} from '../fixtures/utils';
import gql from 'graphql-tag';

describe('Test explicit properties declaration', () => {
    const SITE_KEY = 'explicitPropertiesDeclarations';
    const CONFIG_DEFAULT_NAME = 'org.jahia.modules.htmlfiltering.global.default.yml';
    const CONFIG_CUSTOM_NAME = 'org.jahia.modules.htmlfiltering.global.custom.yml';
    const CONFIG_SITE_NAME = `org.jahia.modules.htmlfiltering.site-${SITE_KEY}.yml`;
    const NODE_NAME = 'explicitPropertiesDeclarationsNode';
    const NODE_PATH = `/sites/${SITE_KEY}/home/${NODE_NAME}`;

    const OTHER_SITE = 'otherSite'; // Site that does not have a configuration for the module
    const RICH_TEXT_NODE = 'testRichTextNode';
    const PATH = `/sites/${SITE_KEY}/home/pagecontent/${RICH_TEXT_NODE}`;
    const HTML_TEXT = '<h1 id="@invalid">my title</h1><p id="abc" class="myClass">my text</p> | <script>alert(document.location)</script> | <strong>STRONG</strong> | <i>ITALIC</i> | <h1>H1 Header</h1>';
    const EXPECTED_HTML_TEXT_WITH_GLOBAL_DEFAULT = '<h1>my title</h1><p id="abc" class="myClass">my text</p>';
    const EXPECTED_HTML_TEXT_WITH_GLOBAL_CUSTOM = 'my title<p id="abc">my text</p>';
    const EXPECTED_HTML_TEXT_WITH_PER_SITE = '<h1>my title</h1><p>my text</p>';

    before(() => {
        deleteSite(SITE_KEY);
        createSite(SITE_KEY, {locale: 'en', serverName: 'localhost', templateSet: 'html-filtering-test-module'});
        // TODO: install custom config!!!
        installConfig(CONFIG_DEFAULT_NAME);

        addNode({
            parentPathOrId: `/sites/${SITE_KEY}/contents`,
            name: NODE_NAME,
            primaryNodeType: 'jnt:bigText',
            properties: [{name: 'text', value: '', language: 'en'}]
        });
    });

    after(() => {
        removeGlobalCustomConfig();
        removeSiteConfig(SITE_KEY);
    });

    it('global.custom cfg: valid editWorkspace and missing liveWorkspace', () => {});
    it('global.custom cfg: valid editWorkspace, valid liveWorkspace and third valid workspace added with unexpected name liveWorkspaceX', () => {});
    it('global.custom cfg: strategy contains invalid value (IGNORE) in editWorkspace workspace, and valid (SANITIZE) one in liveWorkspace', () => {});
    it('global.custom.cfg: strategy is explicitly specified as null in editWorkspace workspace, and contains valid value in liveWorkspace', () => {});
    it('global.custom cfg: strategy is missing in both workspaces', () => {});
    it('global.custom cfg: disallowedRuleSet is absent', () => {});
    it('global.custom cfg: disallowedRuleSet.elements contains empty list', () => {});
    it('global.custom cfg: allowedRuleSet.protocols is absent', () => {});
    it('global.custom cfg: allowedRuleSet.protocols explicitly null', () => {});
    it('global.custom cfg: allowedRuleSet.elements is empty in editWorkspace workspace, and contains valid value in liveWorkspace', () => {});
    it('global.custom cfg:  each element of allowedRuleSet.elements must contain tags and/or attributes', () => {});

    // add invalid per-site configuration site-luxe, keep global.custom in valid state
    // ✅️ global.custom is used for both sites;

    // add invalid per-site configuration site-luxe, and invalid cfg global.custom
    // ✅️ global.custom is used for both sites;

    // add valid per-site configuration site-luxe, and keep global.custom in valid state
    // ✅️ site-luxe is used for Luxe and global.custom - for Digitall;

    // global.default cfg is absent, no other configs added
    // ✅️ html filtering is not performed, js can be added to RichText

    // invalid global.default cfg, no other configs added
    // ✅️ html filtering is not performed, js can be added to RichText

    it('when no configuration is provided, the HTML text is sanitized using the global default strategy', () => {
        // modifyContent(PATH, HTML_TEXT);
        // getContent(PATH).then(result => {
        //     const value = result.data.jcr.nodeByPath.property.value;
        //     expect(value).to.be.equal(EXPECTED_HTML_TEXT_WITH_GLOBAL_DEFAULT);
        // });
    });

    it.skip('read YAML', () => {
        readYAMLConfig('configs/configurationStrategy/org.jahia.modules.htmlfiltering.global.custom.yml').then(data => {
            data.htmlFiltering.editWorkspace.strategy = 'REJECT';
            cy.log('YAML data:' + data);
            installYAMLConfig('org.jahia.modules.htmlfiltering.global.custom.yml', data);
        });

        // modifyContent(PATH, HTML_TEXT);
        // getContent(PATH).then(result => {
        //     const value = result.data.jcr.nodeByPath.property.value;
        //     expect(value).to.be.equal(EXPECTED_HTML_TEXT_WITH_GLOBAL_CUSTOM);
        // });
        // removeGlobalCustomConfig();
    });

    // it('when only a per-site configuration is provided, the HTML text is sanitized using the per-site strategy', () => {
          //installConfig(`configs/configurationStrategy/org.jahia.modules.htmlfiltering.site-${TEST_SITE}.yml`);

    //     modifyContent(PATH, HTML_TEXT);
    //     getContent(PATH).then(result => {
    //         const value = result.data.jcr.nodeByPath.property.value;
    //         expect(value).to.be.equal(EXPECTED_HTML_TEXT_WITH_PER_SITE);
    //     });

    //     removeSiteConfig(TEST_SITE);
    // });

    // it('when a global custom and a per-site configuration is provided, the HTML text is sanitized using the per-site strategy', () => {
    //     installConfig('configs/configurationStrategy/org.jahia.modules.htmlfiltering.global.custom.yml');
    //     installConfig(`configs/configurationStrategy/org.jahia.modules.htmlfiltering.site-${TEST_SITE}.yml`);

    //     modifyContent(PATH, HTML_TEXT);
    //     getContent(PATH).then(result => {
    //         const value = result.data.jcr.nodeByPath.property.value;
    //         expect(value).to.be.equal(EXPECTED_HTML_TEXT_WITH_PER_SITE);
    //     });

    //     removeSiteConfig(TEST_SITE);
    //     removeGlobalCustomConfig();
    // });

    // it('when only a per-site configuration for another site is provided, the HTML text is sanitized using the global default strategy', () => {
    //     installConfig(`configs/configurationStrategy/org.jahia.modules.htmlfiltering.site-${OTHER_SITE}.yml`);

    //     modifyContent(PATH, HTML_TEXT);
    //     getContent(PATH).then(result => {
    //         const value = result.data.jcr.nodeByPath.property.value;
    //         expect(value).to.be.equal(EXPECTED_HTML_TEXT_WITH_GLOBAL_DEFAULT);
    //     });

    //     removeSiteConfig(OTHER_SITE);
    // });

    // it('when a global custom and a per-site configuration for another site is provided, the HTML text is sanitized using the global custom strategy', () => {
    //     installConfig('configs/configurationStrategy/org.jahia.modules.htmlfiltering.global.custom.yml');
    //     installConfig(`configs/configurationStrategy/org.jahia.modules.htmlfiltering.site-${OTHER_SITE}.yml`);

    //     modifyContent(PATH, HTML_TEXT);
    //     getContent(PATH).then(result => {
    //         const value = result.data.jcr.nodeByPath.property.value;
    //         expect(value).to.be.equal(EXPECTED_HTML_TEXT_WITH_GLOBAL_CUSTOM);
    //     });

    //     removeSiteConfig(OTHER_SITE);
    //     removeGlobalCustomConfig();
    // });
});
