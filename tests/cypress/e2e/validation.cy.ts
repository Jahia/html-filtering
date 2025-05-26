import {installConfig, removeGlobalCustomConfig} from '../fixtures/utils';

describe('Ensure node validation is returning translated messages', () => {
    before(() => {
        installConfig('configs/validation/org.jahia.modules.htmlfiltering.global.custom.yml');
    });

    after(() => {
        removeGlobalCustomConfig();
    });

    it('Should return invalid tag message', () => {
        cy.apollo({
            mutationFile: 'graphql/validationTest.graphql',
            variables: {
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
});
