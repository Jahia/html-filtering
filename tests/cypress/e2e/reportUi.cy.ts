import {createUser, deleteSite, deleteUser, grantRoles} from '@jahia/cypress';
import {
    createReportSite,
    installSlowReport,
    removeGlobalCustomConfig,
    removeReportConfig,
    removeSiteConfig,
    runReport,
    waitForReport
} from '../fixtures/utils';

describe('Test the page of the HTML filtering report', () => {
    const SITE_KEY = 'testHtmlFilteringReport';
    const SITE_PATH = `/sites/${SITE_KEY}`;
    const LIST_PATH = `${SITE_PATH}/home/pagecontent`;
    const REPORT_URL = `/jahia/administration/${SITE_KEY}/htmlFilteringReport`;
    const SITE_ADMIN = {username: 'reportSiteAdmin', password: 'password'};
    const EDITOR = {username: 'reportUiEditor', password: 'password'};

    const visitReport = (user = undefined) => {
        if (user) {
            cy.login(user.username, user.password);
        } else {
            cy.login();
        }

        cy.visit(REPORT_URL);
        cy.get('[data-sel-role="html-filtering-report"]', {timeout: 30000}).should('be.visible');
    };

    const runFromPage = (workspace: 'edit' | 'live') => {
        cy.get('[data-sel-role="report-run"]').click();
        cy.get('[data-sel-role="report-run-modal"]').should('be.visible');
        cy.get(`[data-sel-role="report-workspace-${workspace}"]`).click({force: true});
        cy.get('[data-sel-role="report-run-start"]').click();
        cy.get('[data-sel-role="report-state"]', {timeout: 60000}).should('have.attr', 'data-sel-state', 'DONE');
    };

    const rowTexts = () => cy.get('[data-sel-role="report-table"] tbody tr').then(rows => [...rows].map(row => row.innerText));

    const setFilter = (id: 'path' | 'markup', value: string) => {
        cy.get(`[data-sel-role="filter-chip"][data-sel-filter="${id}"] [data-sel-role="filter-chip-edit"]`).click();
        cy.get(`[data-sel-role="filter-${id}"]`).clear();
        cy.get(`[data-sel-role="filter-${id}"]`).type(`${value}{enter}`);
        cy.get('[data-sel-role="filter-editor-close"]').click();
    };

    before(() => {
        removeGlobalCustomConfig();
        removeSiteConfig(SITE_KEY);
        removeReportConfig();
        deleteUser(SITE_ADMIN.username);
        deleteUser(EDITOR.username);
        deleteSite(SITE_KEY);

        createReportSite(SITE_KEY);
        createUser(SITE_ADMIN.username, SITE_ADMIN.password);
        grantRoles(SITE_PATH, ['site-administrator'], SITE_ADMIN.username, 'USER');
        createUser(EDITOR.username, EDITOR.password);
        grantRoles(SITE_PATH, ['editor'], EDITOR.username, 'USER');
    });

    after(() => {
        cy.logout();
        removeSiteConfig(SITE_KEY);
        removeReportConfig();
        deleteUser(SITE_ADMIN.username);
        deleteUser(EDITOR.username);
        deleteSite(SITE_KEY);
    });

    it('shows the entry to a site administrator and not to an editor', () => {
        cy.login(SITE_ADMIN.username, SITE_ADMIN.password);
        cy.visit(`/jahia/administration/${SITE_KEY}`);
        cy.get('[role="treeitem"]', {timeout: 30000}).contains('HTML filtering report').should('be.visible');

        cy.login(EDITOR.username, EDITOR.password);
        cy.visit(`/jahia/administration/${SITE_KEY}`, {failOnStatusCode: false});
        cy.contains('HTML filtering report').should('not.exist');
    });

    it('runs the report on the workspace Edit, and shows the rows', () => {
        visitReport(SITE_ADMIN);
        cy.get('[data-sel-role="report-state"]').should('have.attr', 'data-sel-state', 'NEVER');
        runFromPage('edit');
        cy.get('[data-sel-role="report-state"]').should('have.attr', 'data-sel-workspace', 'EDIT');
        rowTexts().then(rows => {
            expect(rows).to.have.length(3);
            expect(rows.some(row => row.includes(`${LIST_PATH}/B`) && row.includes('<script>'))).to.be.true;
            expect(rows.some(row => row.includes(`${LIST_PATH}/C`) && row.includes('<img onerror>'))).to.be.true;
        });
    });

    it('filters the rows by path and by markup, and exports them as CSV', () => {
        visitReport();
        setFilter('path', `${LIST_PATH}/B`);
        rowTexts().should('have.length', 1);
        cy.get('[data-sel-role="filter-chip"][data-sel-filter="path"] [data-sel-role="filter-chip-clear"]').click();

        setFilter('markup', 'onerror');
        rowTexts().then(rows => {
            expect(rows).to.have.length(1);
            expect(rows[0]).to.contain(`${LIST_PATH}/C`);
        });
        cy.get('[data-sel-role="report-export"]').click();
        cy.readFile(`cypress/downloads/html-filtering-report-${SITE_KEY}.csv`, {timeout: 15000})
            .should('eq', `Path,Property,Language,Markups\r\n"${LIST_PATH}/C","textA",,"<img onerror>"\r\n`);
    });

    it('shows the workspace Live after a run on it', () => {
        visitReport();
        runFromPage('live');
        cy.get('[data-sel-role="report-state"]').should('have.attr', 'data-sel-workspace', 'LIVE');
        rowTexts().then(rows => {
            expect(rows.some(row => row.includes(`${LIST_PATH}/B`))).to.be.true;
            expect(rows.some(row => row.includes(`${LIST_PATH}/C`))).to.be.false;
        });
    });

    it('does not start a second run while a report is running', () => {
        visitReport();
        installSlowReport();
        // The page shows the last run, which ended, and the second run starts from another client.
        runReport(SITE_KEY, 'EDIT').its('data.htmlFiltering.runReport').should('eq', true);
        cy.get('[data-sel-role="report-run"]').click();
        cy.get('[data-sel-role="report-run-start"]').click();
        cy.get('[data-sel-role="report-already-running"]').should('be.visible');
        cy.get('[data-sel-role="report-state"]').should('have.attr', 'data-sel-state', 'RUNNING');
        waitForReport(SITE_KEY);
        removeReportConfig();
    });
});
