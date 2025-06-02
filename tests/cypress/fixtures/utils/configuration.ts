import gql from 'graphql-tag';
import {executeGroovy} from '@jahia/cypress';

const pid = 'org.jahia.modules.htmlfiltering.config';

export const installConfig = configFilePath => {
    return cy.runProvisioningScript(
        {
            script: {fileContent: `- installConfiguration: "${configFilePath}"`, fileName: `${configFilePath}`, type: 'application/yaml'},
            files: [{fileName: `${configFilePath}`, type: 'text/plain'}]
        }).then(() => {
        // Wait for the configuration to be applied
        cy.log('Wait for the configuration to be applied...');
        // eslint-disable-next-line cypress/no-unnecessary-waiting
        cy.wait(3000);
    });
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
