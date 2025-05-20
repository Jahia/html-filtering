import gql from 'graphql-tag';
import {executeGroovy} from '@jahia/cypress';

const pid = 'org.jahia.modules.htmlfiltering.config';

export const installConfig = configFilePath => {
    return cy.runProvisioningScript(
        {fileContent: `- installConfiguration: "${configFilePath}"`, type: 'application/yaml'},
        [{fileName: `${configFilePath}`, type: 'text/plain'}]
    );
};

export const removeDefaultConfig = () => {
    executeGroovy('groovy/removeConfig.groovy', {PID: 'org.jahia.modules.htmlfiltering.default', IDENTIFIER: ''});
};

export const removeSiteConfig = siteKey => {
    executeGroovy('groovy/removeConfig.groovy', {PID: 'org.jahia.modules.htmlfiltering', IDENTIFIER: siteKey});
};

export const removeConfig = siteKey => {
    executeGroovy('groovy/removeConfig.groovy', {PID: pid, IDENTIFIER: siteKey});
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
