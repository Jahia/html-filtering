import {addNode, createSite, deleteSite} from '@jahia/cypress';
import {
    enableHtmlFiltering,
    getContent,
    installConfig,
    modifyContent
} from '../fixtures/utils';

describe('HTML rich text filtering', () => {
    const siteKey = 'filteringSite';
    const textName = 'myText';
    const path = `/sites/${siteKey}/contents/${textName}`;

    before(() => {
        createSite(siteKey);
        addNode({
            parentPathOrId: `/sites/${siteKey}/contents`,
            primaryNodeType: 'jnt:bigText',
            name: textName,
            properties: [{name: 'text', value: '<p>hello there</p>', language: 'en'}]
        });
    });

    after(() => {
        deleteSite(siteKey);
    });

    it('does not apply html filtering when disabled (default)', () => {
        modifyContent(path, '<iframe title="My iframe"></iframe>');
        getContent(path).then(result => {
            expect(result.data.jcr.nodeByPath.property.value).to.contain('iframe');
        });
    });

    it('does not apply html filtering to attribute when disabled (default)', () => {
        modifyContent(path, '<img src="stub.jpg" loading="lazy">');
        getContent(path).then(result => {
            expect(result.data.jcr.nodeByPath.property.value).to.contain('img');
            expect(result.data.jcr.nodeByPath.property.value).to.contain('src');
            expect(result.data.jcr.nodeByPath.property.value).to.contain('loading');
        });
    });

    it('applies default html filtering when enabled', () => {
        enableHtmlFiltering(siteKey);
        modifyContent(path, '<iframe title="My iframe"></iframe>');
        getContent(path).then(result => {
            expect(result.data.jcr.nodeByPath.property.value).to.not.contain('iframe');
        });
    });

    it('applies default filtering to html element when enabled', () => {
        modifyContent(path, '<iframe title="My iframe"></iframe>');
        getContent(path).then(result => {
            expect(result.data.jcr.nodeByPath.property.value).to.not.contain('iframe');
        });
    });

    it('applies default filtering to html attributes when enabled', () => {
        modifyContent(path, '<img src="stub.jpg" loading="lazy">');
        getContent(path).then(result => {
            expect(result.data.jcr.nodeByPath.property.value).to.contain('img');
            expect(result.data.jcr.nodeByPath.property.value).to.contain('src');
            expect(result.data.jcr.nodeByPath.property.value).to.not.contain('loading', 'filtered attribute');
        });
    });

    it('does not override filtering config for other site', () => {
        installConfig('configs/org.jahia.modules.richtext.config-noSite.yml');
        modifyContent(path, '<strong>This text is important!</strong>');
        getContent(path).then(result => {
            expect(result.data.jcr.nodeByPath.property.value).to.contain('strong');
        });
    });

    it('can override filtering config for specified site', () => {
        installConfig('configs/org.jahia.modules.richtext.config-filteringSite.yml');
        modifyContent(path, '<strong>This text is important!</strong>');
        getContent(path).then(result => {
            expect(result.data.jcr.nodeByPath.property.value).to.not.contain('strong');
        });
    });

    // TODO Need to clean up configuration manually (delete in file system) for re-runs
});
