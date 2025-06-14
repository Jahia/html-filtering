// ***********************************************************
// This example support/index.js is processed and
// loaded automatically before your test files.
//
// This is a great place to put global configuration and
// behavior that modifies Cypress.
//
// You can change the location of this file or turn off
// automatically serving support files with the
// 'supportFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/configuration
// ***********************************************************

// Import commands.js using ES2015 syntax:

import './commands';
import addContext from 'mochawesome/addContext';
import {removeGlobalCustomConfig} from '../fixtures/utils';

// eslint-disable-next-line @typescript-eslint/no-var-requires
require('cypress-terminal-report/src/installLogsCollector')({
    xhr: {
        printHeaderData: true,
        printRequestData: true
    },
    enableExtendedCollector: true,
    collectTypes: ['cons:log', 'cons:info', 'cons:warn', 'cons:error', 'cy:log', 'cy:xhr', 'cy:request', 'cy:intercept', 'cy:command']
});
// eslint-disable-next-line @typescript-eslint/no-var-requires
require('@jahia/cypress/dist/support/registerSupport').registerSupport();

Cypress.on('uncaught:exception', () => {
    // Returning false here prevents Cypress from
    // failing the test
    return false;
});
if (Cypress.browser.family === 'chromium') {
    Cypress.automation('remote:debugger:protocol', {
        command: 'Network.enable',
        params: {}
    });
    Cypress.automation('remote:debugger:protocol', {
        command: 'Network.setCacheDisabled',
        params: {cacheDisabled: true}
    });
}

Cypress.on('test:after:run', (test, runnable) => {
    // Add screenshots, video only for failed tests
    if (test.state === 'failed') {
        const videoFileName = Cypress.spec.relative
            .replace('/.cy.*', '')
            .replace('cypress/e2e/', '');
        addContext({test}, `videos/${videoFileName}.mp4`);

        const screenshotFolderName = Cypress.spec.relative.replace('cypress/e2e/', '');
        const screenshotFileName = `${runnable.parent.title} -- ${test.title} (failed).png`;
        addContext({test}, `screenshots/${screenshotFolderName}/${screenshotFileName}`);
    }
});

// Cleanup default.custom config on CI to avoid affecting other tests
// To be moved to jahia-cypress later on
after(() => {
    if (Cypress.env('JAHIA_URL') === 'http://jahia:8080') {
        removeGlobalCustomConfig();
    }
});
