import {installConfig, removeGlobalCustomConfig} from '../fixtures/utils';
import {createSite, deleteSite} from '@jahia/cypress';

const SITE_KEY = 'testValidation';

describe('Ensure node validation is returning translated messages', () => {
    before(() => {
        deleteSite(SITE_KEY);
        installConfig('configs/validation/org.jahia.modules.htmlfiltering.global.custom.yml');
        createSite(SITE_KEY, {locale: 'en', serverName: 'localhost', templateSet: 'html-filtering-test-module'});
    });

    after(() => {
        removeGlobalCustomConfig();
    });

    it('Should return invalid tag message', () => {
        cy.apollo({
            mutationFile: 'graphql/validationTest.graphql',
            variables: {
                nodeName: 'invalidTag',
                text: '<invalidTag>content</invalidTag>'
            }
        }).then(errors => {
            expect(errors.graphQLErrors.length).to.equal(1);
            expect(errors.graphQLErrors[0].extensions.constraintViolations.length).to.equal(2);
            const actualMessages = errors.graphQLErrors[0].extensions.constraintViolations.map(v => v.constraintMessage);
            expect(actualMessages).to.include('"Html validation error."');
            expect(actualMessages).to.include('Unauthorized tag: <invalidtag>.');
        });
    });

    it('Should return invalid attribute message', () => {
        cy.apollo({
            mutationFile: 'graphql/validationTest.graphql',
            variables: {
                nodeName: 'invalidAttribute',
                text: '<p invalidAttribute>content</p>'
            }
        }).then(errors => {
            expect(errors.graphQLErrors.length).to.equal(1);
            expect(errors.graphQLErrors[0].extensions.constraintViolations.length).to.equal(2);
            const actualMessages = errors.graphQLErrors[0].extensions.constraintViolations.map(v => v.constraintMessage);
            expect(actualMessages).to.include('"Html validation error."');
            expect(actualMessages).to.include('Unauthorized attribute "invalidattribute" for tag <p>.');
        });
    });

    it('Should return invalid tag and attribute messages', () => {
        cy.apollo({
            mutationFile: 'graphql/validationTest.graphql',
            variables: {
                nodeName: 'invalidAttributeAndTag',
                text: '<p invalidAttribute>content</p> <invalidTag>content</invalidTag>'
            }
        }).then(errors => {
            expect(errors.graphQLErrors.length).to.equal(1);
            expect(errors.graphQLErrors[0].extensions.constraintViolations.length).to.equal(3);
            const actualMessages = errors.graphQLErrors[0].extensions.constraintViolations.map(v => v.constraintMessage);
            expect(actualMessages).to.include('"Html validation error."');
            expect(actualMessages).to.include('Unauthorized attribute "invalidattribute" for tag <p>.');
            expect(actualMessages).to.include('Unauthorized tag: <invalidtag>.');
        });
    });
    it('Should match the multiple properties in the same content with errors', () => {
        cy.apollo({
            mutationFile: 'graphql/validationTestMultipleProperties.graphql',
            variables: {
                nodeName: 'invalidMultipleProperties',
                textA: '<invalidTagA>content</invalidTagA>',
                textB: '<p>valid text</p>',
                textC: '<invalidTagC>content</invalidTagC>'
            }
        }).then(errors => {
            expect(errors.graphQLErrors.length).to.equal(1);
            expect(errors.graphQLErrors[0].extensions.constraintViolations.length).to.equal(3);
            expect(errors.graphQLErrors[0].extensions.constraintViolations.filter(error => error.propertyName === 'textC')[0].constraintMessage).to.equal('Unauthorized tag: <invalidtagc>.');
            expect(errors.graphQLErrors[0].extensions.constraintViolations.filter(error => error.propertyName === 'textB')).to.be.empty;
            expect(errors.graphQLErrors[0].extensions.constraintViolations.filter(error => error.propertyName === 'textA')[0].constraintMessage).to.equal('Unauthorized tag: <invalidtaga>.');
        });
    });

    it('Should match a property with multi values with errors', () => {
        cy.apollo({
            mutationFile: 'graphql/validationTestPropertyMultiple.graphql',
            variables: {
                nodeName: 'invalidPropertyMultiple',
                texts: ['<invalidTagA>content</invalidTagA>', '<p>valid Content B<p>', '<invalidTagC>content</invalidTagC>']
            }
        }).then(errors => {
            expect(errors.graphQLErrors.length).to.equal(1);
            expect(errors.graphQLErrors[0].extensions.constraintViolations.length).to.equal(3);
            const actualMessages = errors.graphQLErrors[0].extensions.constraintViolations.map(v => v.constraintMessage);
            expect(actualMessages).to.include('"Html validation error."');
            expect(actualMessages).to.include('Unauthorized tag: <invalidtaga>.');
            expect(actualMessages).to.include('Unauthorized tag: <invalidtagc>.');
        });
    });
});
