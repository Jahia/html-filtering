import {addNode, createSite, deleteSite} from '@jahia/cypress';
import {getPropertyValue, installConfig, mutateNodeTextProperty, removeSiteConfig} from '../fixtures/utils';
import gql from 'graphql-tag';

describe('Test the fitering on node types and selectors', () => {
    const SITE_KEY = 'testNodeTypesAndSelectors';
    const PATH = `/sites/${SITE_KEY}/home/pagecontent`;
    const ORIGINAL_HTML_TEXT = '<h1 id="myid">my title</h1><h2>sub-title</h2><p class="myClass">my text</p>';
    const SANITIZED_HTML_TEXT = '<h1>my title</h1>sub-title<p>my text</p>'; // Only <h1> and <p> tags are allowed as per the configuration

    const STRING_RICH_TEXT_PROPS_TEST_DATA = [
        {
            name: 'Filter everything',
            process: ['nt:base.*'],
            skip: [],
            alteredProperties: ['/testA.prop1', '/testA.prop2', '/testA.prop3', '/testB.prop1', '/testB.prop2', '/testB.prop4', '/testC.prop1', '/testC.prop5', '/testD.prop6', '/testD.prop7'] // All properties are sanitized
        },
        {
            name: 'Filter everything but one property',
            process: ['nt:base.*'],
            skip: ['htmlFilteringTestModule:testNodeTypesAndSelectorsB.prop4'],
            alteredProperties: ['/testA.prop1', '/testA.prop2', '/testA.prop3', '/testB.prop1', '/testB.prop2', '/testC.prop1', '/testC.prop5', '/testD.prop6', '/testD.prop7'] // All except testB.prop4
        },
        {
            name: 'Filter out multiple properties',
            process: ['nt:base.*'],
            skip: ['htmlFilteringTestModule:testNodeTypesAndSelectorsA.prop1', 'htmlFilteringTestModule:testNodeTypesAndSelectorsA.prop3', 'htmlFilteringTestModule:testNodeTypesAndSelectorsC.prop5', 'unknownNS:unknownNode.unknownProp'],
            alteredProperties: ['/testA.prop2', '/testB.prop1', '/testB.prop2', '/testB.prop4', '/testC.prop1', '/testD.prop6', '/testD.prop7']
        },
        {
            name: 'Filter out props of a specific node type',
            process: ['nt:base.*'],
            skip: ['htmlFilteringTestModule:testNodeTypesAndSelectorsB.*'],
            alteredProperties: ['/testA.prop1', '/testA.prop2', '/testA.prop3', '/testC.prop1', '/testC.prop5', '/testD.prop6', '/testD.prop7']
        },
        {
            name: 'Filter out props of multiple node types, including a mixin with inherited properties 1',
            process: ['nt:base.*'],
            skip: ['htmlFilteringTestModule:testNodeTypesAndSelectorsA.*', 'htmlFilteringTestModuleMix:testNodeTypesAndSelectorsMixin.*'],
            alteredProperties: ['/testB.prop1', '/testB.prop2', '/testB.prop4', '/testC.prop1', '/testC.prop5']
        },
        {
            name: 'Filter out props of multiple node types, including a mixin with inherited properties 2',
            process: ['nt:base.*'],
            skip: ['htmlFilteringTestModule:testNodeTypesAndSelectorsC.*', 'htmlFilteringTestModuleMix:testNodeTypesAndSelectorsMixin.*'],
            alteredProperties: ['/testA.prop1', '/testA.prop2', '/testA.prop3', '/testB.prop1', '/testB.prop2', '/testB.prop4']
        },
        {
            name: 'Filter out props of multiple node types, including a mixin with inherited properties 3',
            process: ['nt:base.*'],
            skip: ['htmlFilteringTestModule:testNodeTypesAndSelectorsC.*', 'htmlFilteringTestModuleMix:testNodeTypesAndSelectorsMixin.prop7'],
            alteredProperties: ['/testA.prop1', '/testA.prop2', '/testA.prop3', '/testB.prop1', '/testB.prop2', '/testB.prop4', '/testD.prop6']
        },
        {
            name: 'Process all properties of a specific node type',
            process: ['htmlFilteringTestModule:testNodeTypesAndSelectorsB.*'],
            skip: [],
            alteredProperties: ['/testB.prop1', '/testB.prop2', '/testB.prop4']
        },
        {
            name: 'Process all properties of a specific node type but skip one of them',
            process: ['htmlFilteringTestModule:testNodeTypesAndSelectorsB.*'],
            skip: ['htmlFilteringTestModule:testNodeTypesAndSelectorsB.prop2'],
            alteredProperties: ['/testB.prop1', '/testB.prop4']
        },
        {
            name: 'Process all properties on a node type that is inherited',
            process: ['htmlFilteringTestModule:testNodeTypesAndSelectorsC.*'],
            skip: [],
            alteredProperties: ['/testC.prop1', '/testC.prop5', '/testD.prop6', '/testD.prop7']
        },
        {
            name: 'Skip a property that is not processed',
            process: ['htmlFilteringTestModule:testNodeTypesAndSelectorsA.*', 'htmlFilteringTestModule:testNodeTypesAndSelectorsC.prop1'],
            skip: ['htmlFilteringTestModule:testNodeTypesAndSelectorsA.prop2', 'htmlFilteringTestModule:testNodeTypesAndSelectorsB.prop2'],
            alteredProperties: ['/testA.prop1', '/testA.prop3', '/testC.prop1']
        },
        {
            name: 'Skip everything with nt:base.*',
            process: ['nt:base.*'],
            skip: ['nt:base.*'],
            alteredProperties: []
        },
        {
            name: 'Skip everything by listing all node types',
            process: ['nt:base.*'],
            skip: ['htmlFilteringTestModule:testNodeTypesAndSelectorsA.*', 'htmlFilteringTestModule:testNodeTypesAndSelectorsB.*', 'htmlFilteringTestModule:testNodeTypesAndSelectorsC.*', 'htmlFilteringTestModuleMix:testNodeTypesAndSelectorsMixin.*', 'htmlFilteringTestModule:testNodeTypesAndSelectorsD.*'],
            alteredProperties: []
        }
    ];

    before(() => {
        createSite(SITE_KEY, {locale: 'en', serverName: 'localhost', templateSet: 'html-filtering-test-module'});
        addNode({
            parentPathOrId: `/sites/${SITE_KEY}/home`,
            name: 'pagecontent',
            primaryNodeType: 'jnt:contentList',
            children: [
                {
                    name: 'testA',
                    primaryNodeType: 'htmlFilteringTestModule:testNodeTypesAndSelectorsA'
                },
                {
                    name: 'testB',
                    primaryNodeType: 'htmlFilteringTestModule:testNodeTypesAndSelectorsB'
                },
                {
                    name: 'testC',
                    primaryNodeType: 'htmlFilteringTestModule:testNodeTypesAndSelectorsC'
                },
                {
                    name: 'testD',
                    primaryNodeType: 'htmlFilteringTestModule:testNodeTypesAndSelectorsD'
                }
            ]
        });
    });
    after(() => {
        deleteSite(SITE_KEY);
    });
    beforeEach(() => {
        installConfig(`configs/nodeTypesAndSelectors/org.jahia.modules.htmlfiltering.site-${SITE_KEY}.yml`);
    });
    afterEach(() => {
        removeSiteConfig(SITE_KEY);
    });

    function mutateAndAssert(node: string, propertyName: string, alteredProperties: string[]) {
        const fullPath = node + '.' + propertyName;
        const expected = alteredProperties.includes(fullPath) ? SANITIZED_HTML_TEXT : ORIGINAL_HTML_TEXT;
        console.log('mutateAndAssert: ', node, propertyName, fullPath, expected);

        return mutateNodeTextProperty(PATH + node, propertyName, ORIGINAL_HTML_TEXT).then(updatedTextProperty => {
            expect(updatedTextProperty).to.be.equal(expected);
        });
    }

    function editIncludeExcludeProperties(processValues: string[], skipValues: string[]) {
        // Determine if we need to process each set of values
        const hasProcessValues = processValues && processValues.length > 0;
        const hasSkipValues = skipValues && skipValues.length > 0;

        // Build variable declarations
        const variableDeclarations = [
            '$pid: String!',
            '$identifier: String!'
        ];

        if (hasProcessValues) {
            processValues.forEach((_, i) => variableDeclarations.push(`$processValue${i}: String!`));
        }

        if (hasSkipValues) {
            skipValues.forEach((_, i) => variableDeclarations.push(`$skipValue${i}: String!`));
        }

        // Build configuration mutations
        const configurationMutations = [];

        if (hasProcessValues) {
            configurationMutations.push('processRemove: remove(name: "htmlFiltering.editWorkspace.process")');
            const processAddValueQueries = processValues.map((_, index) =>
                `processAddValue${index}: addValue(value: $processValue${index})`
            ).join('\n');
            configurationMutations.push(`processMutateList: mutateList(name: "htmlFiltering.editWorkspace.process") {
                ${processAddValueQueries}
            }`);
        }

        if (hasSkipValues) {
            configurationMutations.push('skipRemove: remove(name: "htmlFiltering.editWorkspace.skip")');
            const skipAddValueQueries = skipValues.map((_, index) =>
                `skipAddValue${index}: addValue(value: $skipValue${index})`
            ).join('\n');
            configurationMutations.push(`skipMutateList: mutateList(name: "htmlFiltering.editWorkspace.skip") {
                ${skipAddValueQueries}
            }`);
        }

        const editConfigGql = gql`
            mutation editConfig(${variableDeclarations.join(', ')}) {
                admin {
                    jahia {
                        configuration(pid: $pid, identifier: $identifier) {
                            ${configurationMutations.join('\n')}
                        }
                    }
                }
            }
        `;
        // Create variables object
        const variables = {
            pid: 'org.jahia.modules.htmlfiltering.site',
            identifier: SITE_KEY
        };
        // Add each array value as a separate variable
        if (hasProcessValues) {
            processValues.forEach((value, index) => {
                variables[`processValue${index}`] = value;
            });
        }

        if (hasSkipValues) {
            skipValues.forEach((value, index) => {
                variables[`skipValue${index}`] = value;
            });
        }

        return cy.apollo({
            mutation: editConfigGql,
            variables
        });
    }

    STRING_RICH_TEXT_PROPS_TEST_DATA.forEach(test => {
        it(`${test.name}: should filter the correct properties`, () => {
            // First set up the configuration
            cy.then(() => {
                return editIncludeExcludeProperties(test.process, test.skip);
            })
                // Props with the TextArea selector are ignored:
                .then(() => mutateAndAssert('/testA', 'areaSelectorTextProp', test.alteredProperties))
                // Props with no selector are ignored:
                .then(() => mutateAndAssert('/testA', 'noSelectorTextProp', test.alteredProperties))
                .then(() => mutateAndAssert('/testA', 'prop1', test.alteredProperties))
                .then(() => mutateAndAssert('/testA', 'prop2', test.alteredProperties))
                .then(() => mutateAndAssert('/testA', 'prop3', test.alteredProperties))
                .then(() => mutateAndAssert('/testB', 'prop1', test.alteredProperties))
                .then(() => mutateAndAssert('/testB', 'prop2', test.alteredProperties))
                .then(() => mutateAndAssert('/testB', 'prop4', test.alteredProperties))
                .then(() => mutateAndAssert('/testC', 'prop1', test.alteredProperties))
                .then(() => mutateAndAssert('/testC', 'prop5', test.alteredProperties))
                .then(() => mutateAndAssert('/testD', 'prop6', test.alteredProperties))
                .then(() => mutateAndAssert('/testD', 'prop7', test.alteredProperties));
        });
    });
});
