import gql from 'graphql-tag';

const pid = 'org.jahia.modules.richtext.config';

export const installConfig = configFilePath => {
    return cy.runProvisioningScript(
        {fileContent: `- installConfiguration: "${configFilePath}"`, type: 'application/yaml'},
        [{fileName: `${configFilePath}`, type: 'text/plain'}]
    );
};

export const getConfig = siteKey => {
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

export const editConfig = (key, value, siteKey = 'default') => {
    const addConfigGql = gql`
        mutation addConfig($pid: String!, $identifier: String!, $key: String!, $value: String!) {
            admin {
                jahia {
                    configuration(pid: $pid, identifier: $identifier) {
                        value(name: $key value: $value)
                    }
                }
            }
        }
    `;
    return cy.apollo({
        mutation: addConfigGql,
        variables: {pid, identifier: siteKey, key, value}
    });
};
