import {addNode, createSite, deleteSite} from '@jahia/cypress';
import {getContent, installConfig, modifyContent, removeGlobalCustomConfig, removeSiteConfig, readYAMLConfig, installYAMLConfig} from '../fixtures/utils';

describe('Test explicit properties declaration', () => {
    const SPEC_NAME = Cypress.spec.name.split('.')[0];
    const SITES = [
        {
            KEY: SPEC_NAME,
            CONFIG_NAME: `org.jahia.modules.htmlfiltering.site-${SPEC_NAME}.yml`,
            CONFIG_PATH: `configs/${SPEC_NAME}/org.jahia.modules.htmlfiltering.site-${SPEC_NAME}.yml`,
            NODE_PATH: `/sites/${SPEC_NAME}/home/pagecontent/testRichTextNode`
        },
        {
            KEY: `${SPEC_NAME}-other`,
            CONFIG_NAME: '', // No specific config for this site, it will use either global custom or global.default config
            CONFIG_PATH: '', // No specific config for this site, it will use either global custom or global.default config
            NODE_PATH: `/sites/${SPEC_NAME}-other/home/pagecontent/testRichTextNode`
        }
    ];

    const CONFIG_CUSTOM_NAME = 'org.jahia.modules.htmlfiltering.global.custom.yml';
    const CONFIG_CUSTOM_PATH = `configs/${SPEC_NAME}/${CONFIG_CUSTOM_NAME}`;

    const HTML_TEXT = '<h1 id="@invalid">H1 Header</h1> | <p id="abc" class="myClass">my text</p><script>alert(document.location)</script>';
    const EXPECTED_HTML_TEXT_WITH_PER_SITE = '<h1>H1 Header</h1> | <p class="myClass">my text</p>';
    const EXPECTED_HTML_TEXT_WITH_GLOBAL_CUSTOM = 'H1 Header | <p id="abc">my text</p>';
    const EXPECTED_HTML_TEXT_WITH_GLOBAL_DEFAULT = '<h1>H1 Header</h1> | <p id="abc" class="myClass">my text</p>';

    const modifyAndValidate = (path: string, htmlText: string, expectedHtmlText: string) => {
        modifyContent(path, htmlText);
        getContent(path).then(result => {
            expect(result.data.jcr.nodeByPath.property.value).to.be.equal(expectedHtmlText);
        });
    };

    /**
     * Cleans up the site configuration, removes the site and creates it from scratch.
     * @param siteKey Key of the site to initialize
     */
    const initializeSite = (siteKey: string) => {
        // Remove any existing site along with all its content
        deleteSite(siteKey);
        // Create a new site with the specified key
        createSite(siteKey, {locale: 'en', serverName: 'localhost', templateSet: 'html-filtering-test-module'});
        // Add a content node to the site where the RichText will be stored
        addNode({
            parentPathOrId: `/sites/${siteKey}/home`,
            name: 'pagecontent',
            primaryNodeType: 'jnt:contentList',
            children: [
                {
                    name: 'testRichTextNode',
                    primaryNodeType: 'jnt:bigText',
                    properties: [{name: 'text', value: '', language: 'en'}]
                }
            ]
        });
    };

    /**
     * As an alternate solution, all testcases can be described as an array of objects, and then iterated over.
     * This way, some code duplication can be avoided, however such approach makes spec less readable.
     * That said, traditional approach was used here to keep the spec readable and easy to follow.
     * Nested describe blocks are used to group test cases logically.
     */

    describe('If REQUIRED properties are either INVALID or MISSING in per-site config, then global.default config should be processed instead', () => {
        before(() => initializeSite(SITES[0].KEY));
        beforeEach(() => {
            removeSiteConfig(SITES[0].KEY);
            removeGlobalCustomConfig();
        });

        it('Config is rejected if liveWorkspace is MISSING', () => {
            cy.step(`Install INVALID "site-${SITES[0].KEY}" config`, () => {
                readYAMLConfig(SITES[0].CONFIG_PATH).then(data => {
                    delete data.htmlFiltering.liveWorkspace;
                    installYAMLConfig(SITES[0].CONFIG_NAME, data);
                });
            });

            cy.step('Modify ReachText content and make sure global.default config is applied', () => {
                modifyAndValidate(SITES[0].NODE_PATH, HTML_TEXT, EXPECTED_HTML_TEXT_WITH_GLOBAL_DEFAULT);
            });
        });

        it('Config is rejected if it contains additional third workspace (liveWorkspaceX) with valid values', () => {
            cy.step(`Install INVALID "site-${SITES[0].KEY}" config`, () => {
                readYAMLConfig(SITES[0].CONFIG_PATH).then(data => {
                    // Clone the liveWorkspace to create a new workspace with unexpected name
                    data.htmlFiltering.liveWorkspaceX = JSON.parse(JSON.stringify(data.htmlFiltering.liveWorkspace));
                    installYAMLConfig(SITES[0].CONFIG_NAME, data);
                });
            });

            cy.step('Modify ReachText content and make sure global.default config is applied', () => {
                modifyAndValidate(SITES[0].NODE_PATH, HTML_TEXT, EXPECTED_HTML_TEXT_WITH_GLOBAL_DEFAULT);
            });
        });

        it('Config is rejected if editWorkspace.strategy is MISSING', () => {
            cy.step(`Install INVALID "site-${SITES[0].KEY}" config`, () => {
                readYAMLConfig(SITES[0].CONFIG_PATH).then(data => {
                    delete data.htmlFiltering.editWorkspace.strategy;
                    installYAMLConfig(SITES[0].CONFIG_NAME, data);
                });
            });

            cy.step('Modify ReachText content and make sure global.default config is applied', () => {
                modifyAndValidate(SITES[0].NODE_PATH, HTML_TEXT, EXPECTED_HTML_TEXT_WITH_GLOBAL_DEFAULT);
            });
        });

        it('Config is rejected if editWorkspace.strategy contains INVALID value (IGNORE)', () => {
            cy.step(`Install INVALID "site-${SITES[0].KEY}" config`, () => {
                readYAMLConfig(SITES[0].CONFIG_PATH).then(data => {
                    data.htmlFiltering.editWorkspace.strategy = 'IGNORE';
                    installYAMLConfig(SITES[0].CONFIG_NAME, data);
                });
            });

            cy.step('Modify ReachText content and make sure global.default config is applied', () => {
                modifyAndValidate(SITES[0].NODE_PATH, HTML_TEXT, EXPECTED_HTML_TEXT_WITH_GLOBAL_DEFAULT);
            });
        });

        it('Config is rejected if editWorkspace.strategy is explicitly specified as null', () => {
            cy.step(`Install INVALID "site-${SITES[0].KEY}" config`, () => {
                readYAMLConfig(SITES[0].CONFIG_PATH).then(data => {
                    data.htmlFiltering.editWorkspace.strategy = null;
                    installYAMLConfig(SITES[0].CONFIG_NAME, data);
                });
            });

            cy.step('Modify ReachText content and make sure global.default config is applied', () => {
                modifyAndValidate(SITES[0].NODE_PATH, HTML_TEXT, EXPECTED_HTML_TEXT_WITH_GLOBAL_DEFAULT);
            });
        });

        it('Config is rejected if editWorkspace.allowedRuleSet.elements is EMPTY', () => {
            cy.step(`Install INVALID "site-${SITES[0].KEY}" config`, () => {
                readYAMLConfig(SITES[0].CONFIG_PATH).then(data => {
                    data.htmlFiltering.editWorkspace.allowedRuleSet.elements = [];
                    installYAMLConfig(SITES[0].CONFIG_NAME, data);
                });
            });

            cy.step('Modify ReachText content and make sure global.default config is applied', () => {
                modifyAndValidate(SITES[0].NODE_PATH, HTML_TEXT, EXPECTED_HTML_TEXT_WITH_GLOBAL_DEFAULT);
            });
        });

        it('Config is rejected if editWorkspace.allowedRuleSet.elements contains INVALID attribute (only tags or/and attributes allowed)', () => {
            cy.step(`Install INVALID "site-${SITES[0].KEY}" config`, () => {
                readYAMLConfig(SITES[0].CONFIG_PATH).then(data => {
                    data.htmlFiltering.editWorkspace.allowedRuleSet.elements = {tags: ['p'], invalidAttribute: ['p', 'h1']};
                    installYAMLConfig(SITES[0].CONFIG_NAME, data);
                });
            });

            cy.step('Modify ReachText content and make sure global.default config is applied', () => {
                modifyAndValidate(SITES[0].NODE_PATH, HTML_TEXT, EXPECTED_HTML_TEXT_WITH_GLOBAL_DEFAULT);
            });
        });
    });

    describe('If OPTIONAL properties are either INVALID or MISSING in per-site config, they are ignored and config is still being processed', () => {
        before(() => initializeSite(SITES[0].KEY));
        beforeEach(() => removeSiteConfig(SITES[0].KEY));

        it('Config is processed if optional editWorkspace.disallowedRuleSet property is MISSING', () => {
            cy.step(`Install VALID "site-${SITES[0].KEY}" config`, () => {
                readYAMLConfig(SITES[0].CONFIG_PATH).then(data => {
                    delete data.htmlFiltering.editWorkspace.disallowedRuleSet;
                    installYAMLConfig(SITES[0].CONFIG_NAME, data);
                });
            });

            cy.step('Modify ReachText content and make sure per-site config is applied', () => {
                modifyAndValidate(SITES[0].NODE_PATH, HTML_TEXT, EXPECTED_HTML_TEXT_WITH_PER_SITE);
            });
        });

        it('Config is processed if disallowedRuleSet.elements is EMPTY', () => {
            cy.step(`Install VALID "site-${SITES[0].KEY}" config`, () => {
                readYAMLConfig(SITES[0].CONFIG_PATH).then(data => {
                    data.htmlFiltering.editWorkspace.disallowedRuleSet.elements = [];
                    installYAMLConfig(SITES[0].CONFIG_NAME, data);
                });
            });

            cy.step('Modify ReachText content and make sure per-site config is applied', () => {
                modifyAndValidate(SITES[0].NODE_PATH, HTML_TEXT, EXPECTED_HTML_TEXT_WITH_PER_SITE);
            });
        });

        it('Config is processed if editWorkspace.allowedRuleSet.protocols is MISSING', () => {
            cy.step(`Install VALID "site-${SITES[0].KEY}" config`, () => {
                readYAMLConfig(SITES[0].CONFIG_PATH).then(data => {
                    delete data.htmlFiltering.editWorkspace.allowedRuleSet.protocols;
                    installYAMLConfig(SITES[0].CONFIG_NAME, data);
                });
            });

            cy.step('Modify ReachText content and make sure per-site config is applied', () => {
                modifyAndValidate(SITES[0].NODE_PATH, HTML_TEXT, EXPECTED_HTML_TEXT_WITH_PER_SITE);
            });
        });

        it('Config is processed if editWorkspace.allowedRuleSet.protocols is explicitly set as null', () => {
            cy.step(`Install VALID "site-${SITES[0].KEY}" config`, () => {
                readYAMLConfig(SITES[0].CONFIG_PATH).then(data => {
                    data.htmlFiltering.editWorkspace.allowedRuleSet.protocols = null;
                    installYAMLConfig(SITES[0].CONFIG_NAME, data);
                });
            });

            cy.step('Modify ReachText content and make sure per-site config is applied', () => {
                modifyAndValidate(SITES[0].NODE_PATH, HTML_TEXT, EXPECTED_HTML_TEXT_WITH_PER_SITE);
            });
        });

        it('Config is processed if editWorkspace.allowedRuleSet.protocols contains INVALID value', () => {
            cy.step(`Install VALID "site-${SITES[0].KEY}" config`, () => {
                readYAMLConfig(SITES[0].CONFIG_PATH).then(data => {
                    data.htmlFiltering.editWorkspace.allowedRuleSet.protocols = ['http', 'invalidProtocol'];
                    installYAMLConfig(SITES[0].CONFIG_NAME, data);
                });
            });

            cy.step('Modify ReachText content and make sure per-site config is applied', () => {
                modifyAndValidate(SITES[0].NODE_PATH, HTML_TEXT, EXPECTED_HTML_TEXT_WITH_PER_SITE);
            });
        });
    });

    describe('Validate configs resolution order for several sites', () => {
        before(() => {
            initializeSite(SITES[0].KEY);
            initializeSite(SITES[1].KEY);
        });

        beforeEach(() => {
            removeSiteConfig(SITES[0].KEY);
            removeGlobalCustomConfig();
        });

        it('If site-1 cfg is invalid, site-2 cfg is absent but default.custom is valid, then default.custom is used for both sites', () => {
            cy.step('Install VALID "global.custom" config', () => {
                installConfig(CONFIG_CUSTOM_PATH);
            });

            cy.step(`Install INVALID "site-${SITES[0].KEY}" config`, () => {
                readYAMLConfig(SITES[0].CONFIG_PATH).then(data => {
                    delete data.htmlFiltering.editWorkspace;
                    installYAMLConfig(SITES[0].CONFIG_NAME, data);
                });
            });

            cy.step('Modify ReachText content in site-1 and make sure global.custom config is applied', () => {
                modifyAndValidate(SITES[0].NODE_PATH, HTML_TEXT, EXPECTED_HTML_TEXT_WITH_GLOBAL_CUSTOM);
            });

            cy.step('Modify ReachText content in site-2 and make sure global.custom config is applied', () => {
                modifyAndValidate(SITES[1].NODE_PATH, HTML_TEXT, EXPECTED_HTML_TEXT_WITH_GLOBAL_CUSTOM);
            });
        });

        it('If site-1 cfg is invalid, site-2 cfg is absent and default.custom is invalid, then default.global is used for both sites', () => {
            cy.step(`Install INVALID "site-${SITES[0].KEY}" config`, () => {
                readYAMLConfig(SITES[0].CONFIG_PATH).then(data => {
                    delete data.htmlFiltering.editWorkspace;
                    installYAMLConfig(SITES[0].CONFIG_NAME, data);
                });
            });

            cy.step(`Install INVALID "${CONFIG_CUSTOM_NAME}" config`, () => {
                readYAMLConfig(CONFIG_CUSTOM_PATH).then(data => {
                    delete data.htmlFiltering.editWorkspace;
                    installYAMLConfig(CONFIG_CUSTOM_NAME, data);
                });
            });

            cy.step('Modify ReachText content in site-1 and make sure global.default config is applied', () => {
                modifyAndValidate(SITES[0].NODE_PATH, HTML_TEXT, EXPECTED_HTML_TEXT_WITH_GLOBAL_DEFAULT);
            });

            cy.step('Modify ReachText content in site-2 and make sure global.default config is applied', () => {
                modifyAndValidate(SITES[1].NODE_PATH, HTML_TEXT, EXPECTED_HTML_TEXT_WITH_GLOBAL_DEFAULT);
            });
        });

        it('If site-1 cfg is valid, site-2 cfg is missing but default.custom is valid, then site-1-cfg and default.custom are used accordingly', () => {
            cy.step(`Install VALID "site-${SITES[0].KEY}" config`, () => {
                installConfig(SITES[0].CONFIG_PATH);
            });

            cy.step('Install VALID "global.custom" config', () => {
                installConfig(CONFIG_CUSTOM_PATH);
            });

            cy.step('Modify ReachText content in site-1 and make sure per-site config is applied', () => {
                modifyAndValidate(SITES[0].NODE_PATH, HTML_TEXT, EXPECTED_HTML_TEXT_WITH_PER_SITE);
            }
            );

            cy.step('Modify ReachText content in site-2 and make sure global.custom config is applied', () => {
                modifyAndValidate(SITES[1].NODE_PATH, HTML_TEXT, EXPECTED_HTML_TEXT_WITH_GLOBAL_CUSTOM);
            });
        });
    });
});
