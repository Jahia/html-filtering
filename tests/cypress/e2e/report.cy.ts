import {createUser, deleteSite, deleteUser, grantRoles} from '@jahia/cypress';
import {
    createReportSite,
    getReport,
    installConfig,
    installReportLimit,
    installSlowReport,
    reportConfig,
    removeGlobalCustomConfig,
    removeReportConfig,
    removeSiteConfig,
    runReport,
    runReportAndWait,
    waitForReport
} from '../fixtures/utils';

describe('Test the HTML filtering report', () => {
    const SITE_KEY = 'testHtmlFilteringReport';
    const SITE_PATH = `/sites/${SITE_KEY}`;
    const LIST_PATH = `${SITE_PATH}/home/pagecontent`;
    const STRICT_CONFIG = reportConfig(SITE_KEY, 'strict');
    const PERMISSIVE_CONFIG = reportConfig(SITE_KEY, 'permissive');
    const USER_EDITOR = 'reportEditor';
    const PASSWORD = 'password';

    const itemsOf = report => report.items.nodes.map(item => `${item.path.replace(`${LIST_PATH}/`, '')}|${item.property}|${item.language ?? ''}|${item.markups.join(' ')}`);

    before(() => {
        removeGlobalCustomConfig();
        removeSiteConfig(SITE_KEY);
        removeReportConfig();
        deleteUser(USER_EDITOR);
        deleteSite(SITE_KEY);

        createReportSite(SITE_KEY);
        createUser(USER_EDITOR, PASSWORD);
        grantRoles(SITE_PATH, ['editor'], USER_EDITOR, 'USER');
    });

    after(() => {
        removeSiteConfig(SITE_KEY);
        removeReportConfig();
        deleteUser(USER_EDITOR);
        deleteSite(SITE_KEY);
    });

    it('lists the properties that are not valid, one row per language', () => {
        runReportAndWait(SITE_KEY, 'EDIT').then(report => {
            expect(report.status).to.equal('DONE');
            expect(report.workspace).to.equal('EDIT');
            expect(report.finished).to.not.be.null;
            expect(itemsOf(report)).to.have.members([
                'B|textA||<script>',
                'C|textA||<img onerror>',
                'E|textI18n|fr|<script>'
            ]);
        });
    });

    it('scans the live workspace with the liveWorkspace policy', () => {
        runReportAndWait(SITE_KEY, 'LIVE').then(report => {
            expect(report.workspace).to.equal('LIVE');
            const items = itemsOf(report);
            expect(items).to.include('B|textA||<script>');
            expect(items.some(item => item.startsWith('C|'))).to.be.false;
        });
    });

    it('filters the rows by path and by markup, and exports them as CSV', () => {
        runReportAndWait(SITE_KEY, 'EDIT');
        getReport(SITE_KEY, {path: `${LIST_PATH}/B`}).then(response => {
            expect(itemsOf(response.data.htmlFiltering.report)).to.deep.equal(['B|textA||<script>']);
        });
        getReport(SITE_KEY, {markup: 'onerror'}).then(response => {
            const report = response.data.htmlFiltering.report;
            expect(itemsOf(report)).to.deep.equal(['C|textA||<img onerror>']);
            expect(report.csv).to.equal(`Path,Property,Language,Markups\r\n"${LIST_PATH}/C","textA",,"<img onerror>"\r\n`);
        });
    });

    it('does not let skipOnPermissions hide content', () => {
        installConfig(PERMISSIVE_CONFIG);
        runReportAndWait(SITE_KEY, 'EDIT').then(report => {
            const items = itemsOf(report);
            expect(items).to.include('B|textA||<script>');
            expect(items).to.include('C|textA||<img onerror>');
        });
        installConfig(STRICT_CONFIG);
    });

    it('stops at the maximum number of items', () => {
        installReportLimit();
        runReportAndWait(SITE_KEY, 'EDIT').then(report => {
            expect(report.status).to.equal('LIMIT_REACHED');
            expect(report.maxItems).to.equal(1);
            expect(report.items.totalCount).to.equal(1);
        });
        removeReportConfig();
    });

    it('runs one report at a time', () => {
        installSlowReport();
        runReport(SITE_KEY, 'EDIT').then(response => {
            expect(response.data.htmlFiltering.runReport).to.be.true;
        });
        runReport(SITE_KEY, 'LIVE').then(response => {
            expect(response.data.htmlFiltering.runReport).to.be.false;
        });
        getReport(SITE_KEY).then(response => {
            const report = response.data.htmlFiltering.report;
            expect(report.workspace).to.equal('EDIT');
            expect(report.platformBusy).to.be.true;
        });
        waitForReport(SITE_KEY);
        removeReportConfig();
    });

    it('refuses the request of a user without the permission', () => {
        const editor = {username: USER_EDITOR, password: PASSWORD};
        runReport(SITE_KEY, 'EDIT', editor).then(response => {
            expect(response.errors?.[0]?.message).to.match(/permission/i);
            expect(response.data?.htmlFiltering?.runReport).to.not.be.true;
        });
        getReport(SITE_KEY, {}, editor).then(response => {
            expect(response.errors?.[0]?.message).to.match(/permission/i);
            expect(response.data?.htmlFiltering?.report).to.not.be.ok;
        });
    });
});
