describe('Graphql test', () => {
    it('Should return a valid content', () => {
        const html = '<p>value</p>';
        cy.apollo({
            queryFile: 'graphql/graphqlTest.graphql',
            variables: {
                html
            }
        }).its('data.htmlFiltering.validate').should(res => {
            expect(res.removedTags).to.be.empty;
            expect(res.removedAttributes).to.be.empty;
            expect(res.sanitizedHtml).to.equal(html);
            expect(res.safe).to.be.true;
        });
    });

    it('Should return an invalid content', () => {
        const html = '<badTag badTagAttribute>value</badTag><p badPAttribute>test</p>';
        cy.apollo({
            queryFile: 'graphql/graphqlTest.graphql',
            variables: {
                html
            }
        }).its('data.htmlFiltering.validate').should(res => {
            expect(res.removedTags.length).to.equal(1);
            expect(res.removedTags).to.contain('badtag');
            expect(res.removedAttributes.length).to.equal(1);
            expect(res.removedAttributes[0].attributes).to.contain('badpattribute');
            expect(res.removedAttributes[0].tag).to.equal('p');
            expect(res.sanitizedHtml).to.equal('value<p>test</p>');
            expect(res.safe).to.be.false;
        });
    });
});
