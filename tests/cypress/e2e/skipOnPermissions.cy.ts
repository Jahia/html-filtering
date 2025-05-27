import {addNode, createSite, createUser, deleteSite, deleteUser, grantRoles} from '@jahia/cypress';
import {installConfig, mutateNodeTextProperty, removeSiteConfig} from '../fixtures/utils';

describe('Test the skipOnPermissions configuration', () => {
    const SITE_KEY = 'testSkipOnPermissions';
    const ORIGINAL_HTML_TEXT = '<h1 id="myid">my title</h1><h2>sub-title</h2><p class="myClass">my text</p>';
    const SANITIZED_HTML_TEXT = '<h1>my title</h1>sub-title<p>my text</p>'; // Only <h1> and <p> tags are allowed as per the configuration


    before(() => {
        installConfig(`configs/skipOnPermissions/org.jahia.modules.htmlfiltering.site-${SITE_KEY}.yml`);
        createSite(SITE_KEY, {locale: 'en', serverName: 'localhost', templateSet: 'html-filtering-test-module'});
        addNode({
            parentPathOrId: `/sites/${SITE_KEY}/home`,
            name: 'pagecontent',
            primaryNodeType: 'jnt:contentList',
            children: [
                {
                    name: 'content',
                    primaryNodeType: 'htmlFilteringTestModule:testValidation'
                }
            ]
        });
        createUser('billy', 'password')
        createUser('bob', 'password')
        grantRoles(`/sites/${SITE_KEY}`, ['editor'], 'bob', 'USER')
        grantRoles(`/sites/${SITE_KEY}`, ['editor-in-chief'], 'billy', 'USER')
    });

    after(() => {
        removeSiteConfig(SITE_KEY);
        deleteUser('bob');
        deleteUser('billy');
        deleteSite(SITE_KEY);
    });

    it(`When user have the permission HTML-filtering is bypassed`, () => {
        // the configuration used for this test is using:
        // - skipOnPermissions: ['view-full-wysiwyg-editor']
        // it's a permission that is granted to the editor-in-chief role by default in Jahia.
        // So bob (editor) should not be able to bypass the HTML filtering.
        mutateNodeTextProperty(`/sites/${SITE_KEY}/home/pagecontent/content`, 'textA', ORIGINAL_HTML_TEXT,
            'en', cy.apolloClient({username: 'bob', password: 'password'})).then(updatedTextProperty => {
            expect(updatedTextProperty).to.be.equal(SANITIZED_HTML_TEXT);
        });

        // But billy (editor-in-chief) should be able to bypass the HTML filtering.
        mutateNodeTextProperty(`/sites/${SITE_KEY}/home/pagecontent/content`, 'textA', ORIGINAL_HTML_TEXT,
            'en', cy.apolloClient({username: 'billy', password: 'password'})).then(updatedTextProperty => {
            expect(updatedTextProperty).to.be.equal(ORIGINAL_HTML_TEXT);
        });
    });
});
