import {installConfig, removeSiteConfig} from '../fixtures/utils';
import {createSite, deleteSite, getNodeByPath} from '@jahia/cypress';

const SITE_KEY = 'testValidation';
const CONFIG_SITE_NAME = `org.jahia.modules.htmlfiltering.site-${SITE_KEY}.yml`;
const CONFIG_SITE_PATH_REJECT = `configs/validation/reject/${CONFIG_SITE_NAME}`;
const CONFIG_SITE_PATH_SANITIZE = `configs/validation/sanitize/${CONFIG_SITE_NAME}`;
const VALIDATION_ERROR_MESSAGE = '"Html validation error."';
const VALID_TAG = '<p>valid tag content</p>';
const INVALID_TAG = '<invalidTag>invalid tag content</invalidTag>';
const INVALID_TAG_SANITIZED = 'invalid tag content';
const INVALID_TAG_MESSAGE = 'Unauthorized tag: <invalidtag>.';
const INVALID_ATTRIBUTE = '<p invalidAttribute>invalid attribute content</p>';
const INVALID_ATTRIBUTE_SANITIZED = '<p>invalid attribute content</p>';
const INVALID_ATTRIBUTE_MESSAGE = 'Unauthorized attribute "invalidattribute" for tag <p>.';

/**
 * Collects constraint violation messages from the errors array-of-maps.
 * @note added to avoid linter's warnings about nested callbacks
 * @param errors Array of validation errors
 * @returns string[] Array of constraint violation messages
 */
const collectConstraintViolations = errors => {
    return errors.map(v => v.constraintMessage);
};

describe('Ensure node validation is returning translated messages', () => {
    before(() => {
        deleteSite(SITE_KEY);
        createSite(SITE_KEY, {locale: 'en', serverName: 'localhost', templateSet: 'html-filtering-test-module'});
    });

    // Make sure the REJECT configuration is installed before each test to avoid flaky tests.
    // Installing it before each test because one of the tests is changing it to SANITIZE
    // and we want to ensure the next test is using the correct one
    beforeEach(() => {
        installConfig(CONFIG_SITE_PATH_REJECT);
    });

    after(() => {
        removeSiteConfig(SITE_KEY);
    });

    it('Should return <invalid attribute> error', () => {
        cy.apollo({
            mutationFile: 'graphql/validationTest.graphql',
            variables: {
                nodeName: 'invalidTag',
                text: INVALID_TAG
            }
        }).then(errors => {
            expect(errors.graphQLErrors.length).to.equal(1);
            expect(errors.graphQLErrors[0].extensions.constraintViolations.length).to.equal(2);
            const actualMessages = collectConstraintViolations(errors.graphQLErrors[0].extensions.constraintViolations);
            expect(actualMessages).to.include(VALIDATION_ERROR_MESSAGE);
            expect(actualMessages).to.include(INVALID_TAG_MESSAGE);
        });
    });

    it('Should return <invalid attribute> error', () => {
        cy.apollo({
            mutationFile: 'graphql/validationTest.graphql',
            variables: {
                nodeName: 'invalidAttribute',
                text: INVALID_ATTRIBUTE
            }
        }).then(errors => {
            expect(errors.graphQLErrors.length).to.equal(1);
            expect(errors.graphQLErrors[0].extensions.constraintViolations.length).to.equal(2);
            const actualMessages = collectConstraintViolations(errors.graphQLErrors[0].extensions.constraintViolations);
            expect(actualMessages).to.include(VALIDATION_ERROR_MESSAGE);
            expect(actualMessages).to.include(INVALID_ATTRIBUTE_MESSAGE);
        });
    });

    it('Should return <invalid tag and attribute> errors', () => {
        cy.apollo({
            mutationFile: 'graphql/validationTest.graphql',
            variables: {
                nodeName: 'invalidAttributeAndTag',
                text: `${INVALID_ATTRIBUTE} ${INVALID_TAG}`
            }
        }).then(errors => {
            expect(errors.graphQLErrors.length).to.equal(1);
            expect(errors.graphQLErrors[0].extensions.constraintViolations.length).to.equal(3);
            const actualMessages = collectConstraintViolations(errors.graphQLErrors[0].extensions.constraintViolations);
            expect(actualMessages).to.include(VALIDATION_ERROR_MESSAGE);
            expect(actualMessages).to.include(INVALID_ATTRIBUTE_MESSAGE);
            expect(actualMessages).to.include(INVALID_TAG_MESSAGE);
        });
    });

    it('Should match and return an error for multiple properties in the same content', () => {
        cy.apollo({
            mutationFile: 'graphql/validationTestMultipleProperties.graphql',
            variables: {
                nodeName: 'invalidMultipleProperties',
                textA: INVALID_TAG,
                textB: VALID_TAG,
                textC: INVALID_ATTRIBUTE
            }
        }).then(errors => {
            expect(errors.graphQLErrors.length).to.equal(1);
            expect(errors.graphQLErrors[0].extensions.constraintViolations.length).to.equal(3);
            expect(errors.graphQLErrors[0].extensions.constraintViolations.filter(error => error.propertyName === 'textC')[0].constraintMessage).to.equal(INVALID_ATTRIBUTE_MESSAGE);
            expect(errors.graphQLErrors[0].extensions.constraintViolations.filter(error => error.propertyName === 'textB')).to.be.empty;
            expect(errors.graphQLErrors[0].extensions.constraintViolations.filter(error => error.propertyName === 'textA')[0].constraintMessage).to.equal(INVALID_TAG_MESSAGE);
        });
    });

    it('Should REJECT a property with multi values with error', () => {
        const nodeName = 'invalidPropertyMultipleReject';
        cy.apollo({
            mutationFile: 'graphql/validationTestPropertyMultiple.graphql',
            variables: {
                nodeName: nodeName,
                texts: [
                    INVALID_TAG,
                    VALID_TAG,
                    INVALID_ATTRIBUTE
                ]
            }
        }).then(errors => {
            expect(errors.graphQLErrors.length).to.equal(1);
            expect(errors.graphQLErrors[0].extensions.constraintViolations.length).to.equal(3);
            const actualMessages = collectConstraintViolations(errors.graphQLErrors[0].extensions.constraintViolations);
            expect(actualMessages).to.include(VALIDATION_ERROR_MESSAGE);
            expect(actualMessages).to.include(INVALID_TAG_MESSAGE);
            expect(actualMessages).to.include(INVALID_ATTRIBUTE_MESSAGE);
        });
    });

    it('Should SANITIZE a property with multi values', () => {
        const nodeName = 'invalidPropertyMultipleSanitize';

        cy.step('Install config with SANITIZE strategy', () => {
            installConfig(CONFIG_SITE_PATH_SANITIZE);
        });

        cy.step(`Create testing node "${nodeName}" with content`, () => {
            cy.apollo({
                mutationFile: 'graphql/validationTestPropertyMultiple.graphql',
                variables: {
                    nodeName: nodeName,
                    texts: [
                        INVALID_TAG,
                        VALID_TAG,
                        INVALID_ATTRIBUTE
                    ]
                }
            });
        });

        cy.step('Validate SANITIZATION', () => {
            getNodeByPath(`/sites/${SITE_KEY}/contents/${nodeName}`, ['textA']).then(node => {
                const values = node?.data?.jcr?.nodeByPath?.properties[0].values;
                expect(values).to.include(INVALID_TAG_SANITIZED);
                expect(values).to.include(VALID_TAG);
                expect(values).to.include(INVALID_ATTRIBUTE_SANITIZED);
            });
        });
    });
});
