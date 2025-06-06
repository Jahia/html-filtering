import gql from 'graphql-tag';
import {executeGroovy} from '@jahia/cypress';
import {load, dump} from 'js-yaml';
import {join} from 'path';
import {v4 as uuidv4} from 'uuid';

const pid = 'org.jahia.modules.htmlfiltering.config';
const configProvisioningDelayMs = 3000;

// Wait for the configuration to be applied
const waitForConfigProvisioning = () => {
    cy.log('Waiting for configuration provisioning...');
    // eslint-disable-next-line cypress/no-unnecessary-waiting
    cy.wait(configProvisioningDelayMs);
};

export const installConfig = configFilePath => {
    return cy.runProvisioningScript(
        {
            script: {fileContent: `- installConfiguration: "${configFilePath}"`, fileName: `${configFilePath}`, type: 'application/yaml'},
            files: [{fileName: `${configFilePath}`, type: 'text/plain'}]
        }).then(() => waitForConfigProvisioning());
};

export const removeGlobalCustomConfig = () => {
    executeGroovy('groovy/removeConfig.groovy', {PID: 'org.jahia.modules.htmlfiltering.global.custom', IDENTIFIER: ''});
    waitForConfigProvisioning();
};

export const removeSiteConfig = siteKey => {
    executeGroovy('groovy/removeConfig.groovy', {PID: 'org.jahia.modules.htmlfiltering.site', IDENTIFIER: siteKey});
    waitForConfigProvisioning();
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

/**
 * Reads a configuration file from the fixtures folder and parses it as YAML.
 * @param pathName - path to the configuration file to read (relative to the fixtures folder)
 * @returns The parsed configuration data
 * @example
 *      readYAMLConfig('configs/configurationStrategy/org.jahia.modules.htmlfiltering.global.custom.yml').then(data => {
 *           data.htmlFiltering.editWorkspace.strategy = 'REJECT';
 *           installYAMLConfig('org.jahia.modules.htmlfiltering.global.custom.yml', data);
 *       });
 */
export const readYAMLConfig = (pathName: string) => {
    return cy.fixture(pathName).then(str => {
        cy.wrap(load(str)).then(data => {
            return data;
        });
    });
};

/**
 * Saves the provided configuration data to a file in the `cypress/download` folder
 * and installs it using the `installConfig` function.
 * @param name - file name to save the configuration as
 * @param data - configuration data to save
 * @returns Cypress chainable that installs the configuration
 * @example
 *      readYAMLConfig('configs/configurationStrategy/org.jahia.modules.htmlfiltering.global.custom.yml').then(data => {
 *           data.htmlFiltering.editWorkspace.strategy = 'REJECT';
 *           installYAMLConfig('org.jahia.modules.htmlfiltering.global.custom.yml', data);
 *       });
 * @note uses 'downloads' folder since it is being cleaned up automatically (when trashAssetsBeforeRuns: true)
 */
export const installYAMLConfig = (name: string, data: object) => {
    // Dump YAML object to string
    const yamlContent = dump(data);
    // Use random uuid to avoid files caching issues
    const myuuid = uuidv4();
    // Path relative to the /tests folder
    const filePath = join('cypress/downloads', myuuid, name);
    cy.writeFile(filePath, yamlContent);
    // Path relative to the fixtures folder
    return installConfig(join('../downloads', myuuid, name));
};
