import {removeGlobalCustomConfig} from '../fixtures/utils';

describe('Graphql test', () => {
    const safeHTML = '<p>value</p>';
    const unsafeHTML = '<badTag badTagAttribute>value</badTag><p class="myClass" badPAttribute>test</p><script>alert(document.location)</script>';
    const sanitizedUnsafeHTML = 'value<p class="myClass">test</p>';

    before(() => {
        // Clean up any previous configurations
        removeGlobalCustomConfig();
    });

    it('Should return a safe content', () => {
        cy.apollo({
            queryFile: 'graphql/graphqlTest.graphql',
            variables: {
                html: safeHTML
            }
        }).its('data.htmlFiltering.validate').should(res => {
            expect(res.removedTags).to.be.empty;
            expect(res.removedAttributes).to.be.empty;
            expect(res.sanitizedHtml).to.equal(safeHTML);
            expect(res.safe).to.be.true;
        });
    });

    it('Should return sanitized unsafe content', () => {
        cy.apollo({
            queryFile: 'graphql/graphqlTest.graphql',
            variables: {
                html: unsafeHTML
            }
        }).its('data.htmlFiltering.validate').should(res => {
            expect(res.removedTags.length).to.equal(2);
            expect(res.removedTags).to.contain('badtag');
            expect(res.removedTags).to.contain('script');
            expect(res.removedAttributes.length).to.equal(1);
            expect(res.removedAttributes[0].tag).to.equal('p');
            expect(res.removedAttributes[0].attributes).to.contain('badpattribute');
            expect(res.removedAttributes[0].attributes).to.not.contain('class');
            expect(res.sanitizedHtml).to.equal(sanitizedUnsafeHTML);
            expect(res.safe).to.be.false;
        });
    });
});
