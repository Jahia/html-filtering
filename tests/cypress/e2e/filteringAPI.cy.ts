import {createSite, deleteSite} from '@jahia/cypress';
import {DocumentNode} from 'graphql';
import {enableHtmlFiltering, installConfig} from '../fixtures/utils';

describe('HTML rich text filtering API', () => {
    const siteKey = 'filteringSite';
    const text = '<div id="myId" role="myRole" removed-attribute="removed">Testing <h1>Testing</h1><p><strong>Testing</strong></p></div>';
    let previewMutation: DocumentNode;
    let configQuery: DocumentNode;

    before(() => {
        createSite(siteKey);
        installConfig('configs/org.jahia.modules.htmlfiltering.config-filteringSite.yml');
        enableHtmlFiltering(siteKey);
        previewMutation = require('graphql-tag/loader!../fixtures/filteringAPI/preview.graphql');
        configQuery = require('graphql-tag/loader!../fixtures/filteringAPI/config.graphql');
    });

    after(() => {
        deleteSite(siteKey);
    });

    it('filters via API and reports on removed elements/attributes', () => {
        cy.apollo({
            mutation: previewMutation,
            variables: {
                text: text,
                siteKey: siteKey
            }
        }).then(response => {
            expect(response.data.htmlFilteringConfiguration.htmlFiltering.testFiltering.removedAttributes).length(1);
            expect(response.data.htmlFilteringConfiguration.htmlFiltering.testFiltering.removedAttributes[0].attributes[0]).to.equal('removed-attribute');
            expect(response.data.htmlFilteringConfiguration.htmlFiltering.testFiltering.removedAttributes[0].element).to.equal('div');
            expect(response.data.htmlFilteringConfiguration.htmlFiltering.testFiltering.removedElements).length(1);
            expect(response.data.htmlFilteringConfiguration.htmlFiltering.testFiltering.removedElements).contain('strong');
            expect(response.data.htmlFilteringConfiguration.htmlFiltering.testFiltering.html).contain('role="myRole"');
            expect(response.data.htmlFilteringConfiguration.htmlFiltering.testFiltering.html).contain('id="myId"');
            expect(response.data.htmlFilteringConfiguration.htmlFiltering.testFiltering.html).contain('<p>Testing</p>');
        });
    });

    it('returns a list of configured elements and attributes', () => {
        cy.apollo({
            query: configQuery,
            variables: {
                siteKey: siteKey
            }
        }).then(response => {
            expect(response.data.htmlFilteringConfiguration.htmlFiltering.configuration.attributes).length(40);
            expect(response.data.htmlFilteringConfiguration.htmlFiltering.configuration.attributes.find(a => a.attribute === 'class')).to.deep.equal({
                attribute: 'class',
                elements: [],
                isGlobal: true,
                pattern: null,
                __typename: 'GqlHTMLFilteringConfigAttribute'
            });
            expect(response.data.htmlFilteringConfiguration.htmlFiltering.configuration.attributes.find(a => a.attribute === 'autoplay')).to.deep.equal({
                attribute: 'autoplay',
                elements: [
                    'audio',
                    'video'
                ],
                isGlobal: false,
                pattern: null,
                __typename: 'GqlHTMLFilteringConfigAttribute'
            });
            expect(response.data.htmlFilteringConfiguration.htmlFiltering.configuration.elements).length(71);
            expect(response.data.htmlFilteringConfiguration.htmlFiltering.configuration.protocols).length(3);
            expect(response.data.htmlFilteringConfiguration.htmlFiltering.configuration.disallow.elements).length(1);
            expect(response.data.htmlFilteringConfiguration.htmlFiltering.configuration.disallow.elements).contain('strong');
        });
    });
});
