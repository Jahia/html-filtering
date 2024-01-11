import gql from 'graphql-tag';

export const installConfig = configFilePath => {
    return cy.runProvisioningScript(
        {fileContent: `- installConfiguration: "${configFilePath}"`, type: 'application/yaml'},
        [{fileName: `${configFilePath}`, type: 'text/plain'}]
    );
};

export const getConfig = siteKey => {
    const pid = 'org.jahia.modules.richtext.config';
    const getConfigGql = gql`
        query getConfig($pid: String!, $siteKey: String!) {
            admin {
                jahia {
                    configuration(pid: $pid, identifier: $siteKey) {
                        flatKeys
                    }
                }
            }
        }
    `;
    return cy.apollo({
        query: getConfigGql,
        variables: {pid, siteKey}
    });
};
