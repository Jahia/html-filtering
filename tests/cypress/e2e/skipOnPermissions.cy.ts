import {addNode, createSite, createUser, deleteSite, deleteUser, grantRoles} from '@jahia/cypress';
import {installConfig, mutateNodeTextProperty, removeSiteConfig} from '../fixtures/utils';

describe('Test the skipOnPermissions configuration', () => {
    const SITE_KEY = 'testSkipOnPermissions';
    const ORIGINAL_HTML_TEXT = '<h1 id="myid">my title</h1><h2>sub-title</h2><p class="myClass">my text</p>';
    const SANITIZED_HTML_TEXT = '<h1>my title</h1>sub-title<p>my text</p>'; // Only <h1> and <p> tags are allowed as per the configuration
    const USER_EDITOR = 'bob';
    const USER_EDITOR_IN_CHIEF = 'billy';
    const PASSWORD = 'password';

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
        createUser(USER_EDITOR, PASSWORD);
        createUser(USER_EDITOR_IN_CHIEF, PASSWORD);
        grantRoles(`/sites/${SITE_KEY}`, ['editor'], USER_EDITOR, 'USER');
        grantRoles(`/sites/${SITE_KEY}`, ['editor-in-chief'], USER_EDITOR_IN_CHIEF, 'USER');
    });

    after(() => {
        removeSiteConfig(SITE_KEY);
        deleteUser(USER_EDITOR);
        deleteUser(USER_EDITOR_IN_CHIEF);
        deleteSite(SITE_KEY);
    });

    it('When user does not have the permission - HTML-filtering is applied', () => {
        // The configuration used for this test is using:
        // - skipOnPermissions: ['view-full-wysiwyg-editor']
        // it's a permission that is granted to the editor-in-chief role by default in Jahia.
        // So bob (editor) should not be able to bypass the HTML filtering.
        mutateNodeTextProperty(`/sites/${SITE_KEY}/home/pagecontent/content`, 'textA', ORIGINAL_HTML_TEXT, 'en', cy.apolloClient({username: USER_EDITOR, password: PASSWORD}))
            .then(updatedTextProperty => {
                expect(updatedTextProperty).to.be.equal(SANITIZED_HTML_TEXT);
            });
    });

    it('When user has the permission - HTML-filtering bypassed', () => {
        // The configuration used for this test is using:
        // - skipOnPermissions: ['view-full-wysiwyg-editor']
        // it's a permission that is granted to the editor-in-chief role by default in Jahia.
        // But billy (editor-in-chief) should be able to bypass the HTML filtering.
        mutateNodeTextProperty(`/sites/${SITE_KEY}/home/pagecontent/content`, 'textA', ORIGINAL_HTML_TEXT, 'en', cy.apolloClient({username: USER_EDITOR_IN_CHIEF, password: 'password'}))
            .then(updatedTextProperty => {
                expect(updatedTextProperty).to.be.equal(ORIGINAL_HTML_TEXT);
            });
    });
});
