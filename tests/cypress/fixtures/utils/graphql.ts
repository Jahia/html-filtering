/**
 * Corresponds to the GraphQL endpoint "validate" under "HTMLFilteringQuery". See GqlHtmlFilteringQuery#validate()
 * @param html the HTML text to validate
 * @param siteKey the site key
 * @param workspace the workspace
 */
export const graphqlValidate = (html: string | null, siteKey: string = null, workspace: string = null) => {
    return cy.apollo({
        queryFile: 'graphql/endpoints/validate.graphql',
        variables: {
            html: html,
            workspace: workspace,
            siteKey: siteKey
        }
    });
};
