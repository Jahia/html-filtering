import {removeGlobalCustomConfig} from '../fixtures/utils';
import {graphqlValidate} from '../fixtures/utils/graphql';

describe('Graphql test', () => {
    const safeHTML = '<p>value</p>';
    const unsafeHTML = '<badTag badTagAttribute>value</badTag><p class="myClass" badPAttribute>test</p><script>alert(document.location)</script>';
    const sanitizedUnsafeHTML = 'value<p class="myClass">test</p>';

    before(() => {
        // Clean up any previous configurations
        removeGlobalCustomConfig();
    });

    it('Should throw a validation error if no html parameter is passed', () => {
        graphqlValidate(null).then(result => {
            expect(result.graphQLErrors).to.exist;
            expect(result.graphQLErrors).to.have.length(1);
            expect(result.graphQLErrors[0].extensions.classification).to.equal('ValidationError');
            expect(result.graphQLErrors[0].message).to.include('\'html\'');
        });
    });

    it('Should return a safe content', () => {
        graphqlValidate(safeHTML).its('data.htmlFiltering.validate').should(res => {
            expect(res.removedTags).to.be.empty;
            expect(res.removedAttributes).to.be.empty;
            expect(res.sanitizedHtml).to.equal(safeHTML);
            expect(res.safe).to.be.true;
        });
    });

    it('Should return sanitized unsafe content', () => {
        graphqlValidate(unsafeHTML).its('data.htmlFiltering.validate').should(res => {
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
