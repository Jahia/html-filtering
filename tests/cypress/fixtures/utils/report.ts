import {addNode, createSite, executeGroovy, getNodeByPath} from '@jahia/cypress';
import {installConfig} from './configuration';

export const REPORT_CONFIG_PID = 'org.jahia.modules.htmlfiltering.report';

/**
 * Corresponds to the GraphQL endpoint "runReport" under "HTMLFilteringMutation". See GqlHtmlFilteringMutation#runReport()
 */
export const runReport = (siteKey: string, workspace: string = null, user: {username: string, password: string} = undefined) => {
    return cy.apolloClient(user).apollo({
        mutationFile: 'graphql/report/runReport.graphql',
        variables: {siteKey, workspace},
        errorPolicy: 'all'
    });
};

/**
 * Corresponds to the GraphQL endpoint "report" under "HTMLFilteringQuery". See GqlHtmlFilteringQuery#report()
 */
export const getReport = (siteKey: string, filters: {path?: string, markup?: string} = {}, user: {username: string, password: string} = undefined) => {
    return cy.apolloClient(user).apollo({
        queryFile: 'graphql/report/report.graphql',
        variables: {siteKey, ...filters},
        fetchPolicy: 'no-cache',
        errorPolicy: 'all'
    });
};

/**
 * Waits until the last run of the report of the site ends, and yields the report.
 */
export const waitForReport = (siteKey: string, filters: {path?: string, markup?: string} = {}) => {
    const check = (attempt: number) => {
        return getReport(siteKey, filters).then(response => {
            const report = response?.data?.htmlFiltering?.report;
            if (report && report.status !== 'RUNNING') {
                return report;
            }

            expect(attempt, 'attempts to wait for the end of the report').to.be.lessThan(60);
            // eslint-disable-next-line cypress/no-unnecessary-waiting
            cy.wait(1000);
            return check(attempt + 1);
        });
    };

    return check(0);
};

export const runReportAndWait = (siteKey: string, workspace: string = null) => {
    return runReport(siteKey, workspace).then(response => {
        expect(response.data.htmlFiltering.runReport).to.be.true;
        return waitForReport(siteKey);
    });
};

export const installReportLimit = () => installConfig(`configs/report/limit/${REPORT_CONFIG_PID}.cfg`);

/** Reads one node per batch, with a pause of 3 seconds, so that a run lasts long enough to start a second one. */
export const installSlowReport = () => installConfig(`configs/report/slow/${REPORT_CONFIG_PID}.cfg`);

export const removeReportConfig = () => {
    executeGroovy('groovy/removeConfig.groovy', {PID: REPORT_CONFIG_PID, IDENTIFIER: ''});
    // eslint-disable-next-line cypress/no-unnecessary-waiting
    cy.wait(3000);
};

/**
 * The site configuration of a report test: "strict" rejects <script> and the onerror attribute, and "permissive" has
 * the same rules with skipOnPermissions: ['site-admin'], so that root saves the content that the rules reject.
 * The fixtures name the site testHtmlFilteringReport.
 */
export const reportConfig = (siteKey: string, kind: 'strict' | 'permissive') =>
    `configs/report/${kind}/org.jahia.modules.htmlfiltering.site-${siteKey}.yml`;

const waitForLiveNode = (path: string, attempt = 0) => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    getNodeByPath(path, [], 'en', [], 'LIVE' as any).then(result => {
        if (!result?.data?.jcr?.nodeByPath) {
            expect(attempt, 'attempts to wait for the publication').to.be.lessThan(30);
            // eslint-disable-next-line cypress/no-unnecessary-waiting
            cy.wait(1000);
            waitForLiveNode(path, attempt + 1);
        }
    });
};

/**
 * Creates the site of the report scenarios, in English and in French, under the permissive configuration, then loads
 * the strict configuration. The contents of the list /home/pagecontent are:
 * - A: a valid rich text
 * - B: a rich text with <script>, published
 * - C: a rich text with <img onerror>, never published
 * - D: a string without the richtext selector, with <script>
 * - E: an internationalized rich text, valid in English, with <script> in French
 */
export const createReportSite = (siteKey: string) => {
    const sitePath = `/sites/${siteKey}`;
    const listPath = `${sitePath}/home/pagecontent`;
    const richTextType = 'htmlFilteringTestModule:testValidation';
    installConfig(reportConfig(siteKey, 'permissive'));
    createSite(siteKey, {locale: 'en', languages: 'en,fr', serverName: 'localhost', templateSet: 'html-filtering-test-module'});
    addNode({
        parentPathOrId: `${sitePath}/home`,
        name: 'pagecontent',
        primaryNodeType: 'jnt:contentList',
        children: [
            {name: 'A', primaryNodeType: richTextType, properties: [{name: 'textA', value: '<p>valid</p>'}]},
            {name: 'B', primaryNodeType: richTextType, properties: [{name: 'textA', value: '<p>text</p><script>alert(1)</script>'}]},
            {name: 'D', primaryNodeType: 'htmlFilteringTestModule:testNodeTypesAndSelectorsA', properties: [{name: 'noSelectorTextProp', value: '<script>alert(1)</script>'}]},
            {
                name: 'E',
                primaryNodeType: richTextType,
                properties: [
                    {name: 'textI18n', value: '<p>valid</p>', language: 'en'},
                    {name: 'textI18n', value: '<p>texte</p><script>alert(1)</script>', language: 'fr'}
                ]
            }
        ]
    });
    cy.apollo({
        mutationFile: 'graphql/jcr/mutation/publishNode.graphql',
        variables: {pathOrId: `${sitePath}/home`, languages: ['en', 'fr'], publishSubNodes: true, includeSubTree: true}
    });
    waitForLiveNode(`${listPath}/B`);
    addNode({parentPathOrId: listPath, name: 'C', primaryNodeType: richTextType, properties: [{name: 'textA', value: '<img src="a.png" onerror="x">'}]});
    installConfig(reportConfig(siteKey, 'strict'));
};
