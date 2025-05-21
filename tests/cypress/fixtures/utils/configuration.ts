import gql from 'graphql-tag';
import {executeGroovy} from '@jahia/cypress';

const pid = 'org.jahia.modules.htmlfiltering.config';

export const installConfig = configFilePath => {
    return cy.runProvisioningScript(
        {fileContent: `- installConfiguration: "${configFilePath}"`, type: 'application/yaml'},
        [{fileName: `${configFilePath}`, type: 'text/plain'}]
    );
};

export const removeGlobalCustomConfig = () => {
    executeGroovy('groovy/removeConfig.groovy', {PID: 'org.jahia.modules.htmlfiltering.global.custom', IDENTIFIER: ''});
};

export const removeSiteConfig = siteKey => {
    executeGroovy('groovy/removeConfig.groovy', {PID: 'org.jahia.modules.htmlfiltering.site', IDENTIFIER: siteKey});
};

export const editSiteConfig = (key, value, siteKey) => {
    const editConfigGql = gql`
        mutation editConfig($pid: String!, $identifier: String!, $key: String!, $value: String!) {
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
        mutation: editConfigGql,
        variables: {pid: 'org.jahia.modules.htmlfiltering', identifier: siteKey, key, value}
    });
};

export const editConfig = (key, value, siteKey = 'default') => {
    const editConfigGql = gql`
        mutation editConfig($pid: String!, $identifier: String!, $key: String!, $value: String!) {
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
        mutation: editConfigGql,
        variables: {pid, identifier: siteKey, key, value}
    });
};
