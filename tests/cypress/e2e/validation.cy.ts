import {installConfig, mutateNodeProperty, removeGlobalCustomConfig, removeSiteConfig} from '../fixtures/utils';
import {addNode, createSite, deleteSite, getJahiaVersion, getNodeByPath} from '@jahia/cypress';
import {compare} from 'compare-versions';

const SITE_KEY = 'testValidation';
const CONFIG_SITE_NAME = `org.jahia.modules.htmlfiltering.site-${SITE_KEY}.yml`;
const CONFIG_SITE_PATH_REJECT = `configs/validation/reject/${CONFIG_SITE_NAME}`;
const CONFIG_SITE_PATH_SANITIZE = `configs/validation/sanitize/${CONFIG_SITE_NAME}`;
const TEST_NODE = 'testNode';
const VALIDATION_ERROR_MESSAGE = '"Html validation error."';
const VALID = '<p>valid content</p>';
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

const createTestNode = (properties: Array<{
    name: string,
    value?: string,
    values?: string[],
    language?: string
}>) => {
    return addNode(
        {
            parentPathOrId: '/sites/testValidation/contents',
            primaryNodeType: 'htmlFilteringTestModule:testValidation',
            name: TEST_NODE,
            properties: properties
        }
    );
};

const containsValidationErrorMessages = (result, expectedErrorMessages:string[]) => {
    expect(result.graphQLErrors.length).to.equal(1);
    expect(result.graphQLErrors[0].extensions.constraintViolations.length).to.equal(1 + expectedErrorMessages.length); // Generic VALIDATION_ERROR_MESSAGE + the expected errors
    const actualMessages = collectConstraintViolations(result.graphQLErrors[0].extensions.constraintViolations);
    expect(actualMessages).to.include(VALIDATION_ERROR_MESSAGE); // Generic error message always present
    expectedErrorMessages.forEach(errorMsg => {
        expect(actualMessages).to.include(errorMsg);
    });
};

function skipIfValidationOnI18nNotSupported(context: Mocha.Context, testFunction: () => void) {
    getJahiaVersion().then(jahiaVersion => {
        if (compare(jahiaVersion.release.replace('-SNAPSHOT', ''), '8.1.8.0', '<')) {
            // The validation was not triggered when creating nodes.
            // Fixed in https://github.com/Jahia/jahia-private/pull/1326 and backported in 8.1.8 in https://github.com/Jahia/jahia-private/pull/2170
            context.skip();
        }

        testFunction();
    });
}

describe('Ensure the REJECT strategy triggers a validation when creating/updating content', () => {
    before(() => {
        // Clean up any previous configurations
        removeGlobalCustomConfig();
        removeSiteConfig(SITE_KEY);

        // Configure to use the REJECT strategy
        installConfig(CONFIG_SITE_PATH_REJECT);
    });

    beforeEach(() => {
        deleteSite(SITE_KEY);

        // Create a site to be used for testing
        createSite(SITE_KEY, {locale: 'en', serverName: 'localhost', templateSet: 'html-filtering-test-module'});
    });

    const invalidContentTestData = [
        {name: 'Invalid tag error', text: INVALID_TAG, expectedErrorMessages: [INVALID_TAG_MESSAGE]},
        {name: 'Invalid attribute error', text: INVALID_ATTRIBUTE, expectedErrorMessages: [INVALID_ATTRIBUTE_MESSAGE]},
        {name: 'Multiple errors', text: `${INVALID_ATTRIBUTE} ${INVALID_TAG}`, expectedErrorMessages: [INVALID_ATTRIBUTE_MESSAGE, INVALID_TAG_MESSAGE]}
    ];

    invalidContentTestData.forEach(test => {
        it(`Should return an error when creating invalid content: ${test.name}`, () => {
            createTestNode([{name: 'textA', value: test.text}])
                .then(errors => {
                    containsValidationErrorMessages(errors, test.expectedErrorMessages);
                });
        });
    });

    invalidContentTestData.forEach(test => {
        it(`Should return an error when updating with invalid content: ${test.name}`, () => {
            // First create a valid content
            createTestNode([{name: 'textA', value: VALID}])
                .then(result => {
                    expect(result.graphQLErrors).to.be.undefined;
                    expect(result.data?.jcr?.addNode).to.exist;
                });

            // Then update it with invalid content
            mutateNodeProperty(`/sites/${SITE_KEY}/contents/${TEST_NODE}`, 'textA', test.text)
                .then(errors => {
                    containsValidationErrorMessages(errors, test.expectedErrorMessages);
                });
        });
    });

    it('Should return an error when creating invalid content in multiple properties', () => {
        createTestNode([
            {value: INVALID_TAG, name: 'textA'},
            {value: VALID, name: 'textB'},
            {value: INVALID_ATTRIBUTE, name: 'textC'}
        ])
            .then(errors => {
                expect(errors.graphQLErrors.length).to.equal(1);
                expect(errors.graphQLErrors[0].extensions.constraintViolations.length).to.equal(3);
                expect(errors.graphQLErrors[0].extensions.constraintViolations.filter(error => error.propertyName === 'textC')[0].constraintMessage).to.equal(INVALID_ATTRIBUTE_MESSAGE);
                expect(errors.graphQLErrors[0].extensions.constraintViolations.filter(error => error.propertyName === 'textB')).to.be.empty;
                expect(errors.graphQLErrors[0].extensions.constraintViolations.filter(error => error.propertyName === 'textA')[0].constraintMessage).to.equal(INVALID_TAG_MESSAGE);
            });
    });

    it('Should return an error when creating invalid content in a multi-value property', () => {
        createTestNode(
            [
                {values: [
                    INVALID_TAG,
                    VALID,
                    INVALID_ATTRIBUTE
                ], name: 'textMultiValues'}
            ]
        )
            .then(errors => {
                containsValidationErrorMessages(errors, [INVALID_TAG_MESSAGE, INVALID_ATTRIBUTE_MESSAGE]);
            });
    });

    it('Should return an error when creating invalid content in an i18n property', function () {
        skipIfValidationOnI18nNotSupported(this, () => {
            createTestNode(
                [
                    {name: 'textI18n', language: 'de', value: VALID},
                    {name: 'textI18n', language: 'es', value: VALID},
                    {name: 'textI18n', language: 'it', value: INVALID_ATTRIBUTE}
                ]
            )
                .then(errors => {
                    containsValidationErrorMessages(errors, [INVALID_ATTRIBUTE_MESSAGE]);
                });
        });
    });

    it('Should return an error when when creating invalid content in an i18n multi-value property', function () {
        skipIfValidationOnI18nNotSupported(this, () => {
            createTestNode(
                [
                    {name: 'textI18nMultiValues', language: 'de', values: [VALID]},
                    {name: 'textI18nMultiValues', language: 'es', values: [VALID, INVALID_TAG]},
                    {name: 'textI18nMultiValues', language: 'it', values: [VALID, VALID]}
                ]
            )
                .then(errors => {
                    containsValidationErrorMessages(errors, [INVALID_TAG_MESSAGE]);
                });
        });
    });
});

describe('Ensure the SANITIZE strategy automatically sanitize HTML content when being created/updated', () => {
    before(() => {
        // Clean up any previous configurations
        removeGlobalCustomConfig();
        removeSiteConfig(SITE_KEY);
        deleteSite(SITE_KEY);

        // Create a site to be used for testing
        createSite(SITE_KEY, {locale: 'en', serverName: 'localhost', templateSet: 'html-filtering-test-module'});
    });

    beforeEach(() => {
        installConfig(CONFIG_SITE_PATH_SANITIZE);
    });

    it('Should SANITIZE a property with multi values', () => {
        const nodeName = 'invalidPropertyMultipleSanitize';
        //
        cy.step(`Create testing node "${nodeName}" with content`, () => {
            addNode(
                {
                    parentPathOrId: '/sites/testValidation/contents',
                    primaryNodeType: 'htmlFilteringTestModule:testValidation',
                    name: nodeName,
                    properties: [
                        {values: [
                            INVALID_TAG,
                            VALID,
                            INVALID_ATTRIBUTE
                        ], name: 'textMultiValues'}
                    ]
                }
            );
        });

        cy.step('Validate SANITIZATION', () => {
            getNodeByPath(`/sites/${SITE_KEY}/contents/${nodeName}`, ['textMultiValues']).then(node => {
                const values = node?.data?.jcr?.nodeByPath?.properties[0].values;
                expect(values).to.include(INVALID_TAG_SANITIZED);
                expect(values).to.include(VALID);
                expect(values).to.include(INVALID_ATTRIBUTE_SANITIZED);
            });
        });
    });
});
