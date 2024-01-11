import gql from 'graphql-tag';

export const enableHtmlFiltering = siteKey => {
    toggleHtmlFiltering(siteKey, true);
};

export const disableHtmlFiltering = siteKey => {
    toggleHtmlFiltering(siteKey, false);
};

const toggleHtmlFiltering = (siteKey, enable) => {
    const toggleFilteringGql = gql`
        mutation toggleHtmlFiltering($pathOrId: String!, $enable: String!) {
            jcr {
                mutateNode(pathOrId: $pathOrId) {
                    mutateProperty(name: "j:doTagFiltering") {
                        setValue(value: $enable, type:BOOLEAN)
                    }
                }
            }
        }
    `;

    cy.apollo({
        mutation: toggleFilteringGql,
        variables: {pathOrId: `/sites/${siteKey}`, enable}
    });
};
