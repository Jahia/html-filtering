import {addNode, createSite, deleteSite} from '@jahia/cypress';
import {
    editConfig,
    disableHtmlFiltering,
    enableHtmlFiltering,
    getContent,
    installConfig,
    modifyContent
} from '../fixtures/utils';

describe('HTML rich text filtering', () => {
    const siteKey = 'filteringSite';
    const textName = 'myText';
    const path = `/sites/${siteKey}/contents/${textName}`;

    // TODO Need to clean up configuration manually (delete in file system) for local re-runs

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

    it('does not apply html filtering to tag when disabled (default)', () => {
        modifyContent(path, '<iframe title="My iframe"></iframe><script> var today= new Date(); </script>');
        getContent(path).then(result => {
            expect(result.data.jcr.nodeByPath.property.value).to.contain('iframe');
            expect(result.data.jcr.nodeByPath.property.value).to.contain('<script>');
        });
    });

    it('does not apply html filtering to attribute when disabled (default)', () => {
        modifyContent(path, '<img src="stub.jpg" loading="lazy">');
        getContent(path).then(result => {
            expect(result.data.jcr.nodeByPath.property.value).to.contain('<img');
            expect(result.data.jcr.nodeByPath.property.value).to.contain('src');
            expect(result.data.jcr.nodeByPath.property.value).to.contain('loading');
        });
    });

    it('applies default html filtering when enabled', () => {
        enableHtmlFiltering(siteKey);
        modifyContent(path, '<iframe title="My iframe"></iframe><a title="My link"></a><script> var today= new Date(); </script>');
        getContent(path).then(result => {
            expect(result.data.jcr.nodeByPath.property.value).to.not.contain('iframe');
            expect(result.data.jcr.nodeByPath.property.value).to.not.contain('script');
            expect(result.data.jcr.nodeByPath.property.value).to.contain('<a');
        });
    });

    it('applies default html filtering when enabled', () => {
        enableHtmlFiltering(siteKey);
        modifyContent(path, '<a title="My iframe"></a>');
        getContent(path).then(result => {
            expect(result.data.jcr.nodeByPath.property.value).to.contain('a');
        });
    });

    it('applies default filtering to html element when enabled', () => {
        modifyContent(path, '<iframe title="My iframe"></iframe><p>This is a <u>nested</u>tag</p>');
        getContent(path).then(result => {
            expect(result.data.jcr.nodeByPath.property.value).to.not.contain('<iframe>');
            expect(result.data.jcr.nodeByPath.property.value).to.contain('<p>');
            expect(result.data.jcr.nodeByPath.property.value).to.contain('<u>');
        });
    });

    it('applies default filtering to html attributes when enabled', () => {
        modifyContent(path, '<img src="stub.jpg" loading="lazy"><a href="https://localhost:8080">');
        getContent(path).then(result => {
            expect(result.data.jcr.nodeByPath.property.value).to.contain('a');
            expect(result.data.jcr.nodeByPath.property.value).to.contain('href');
            expect(result.data.jcr.nodeByPath.property.value).to.contain('img');
            expect(result.data.jcr.nodeByPath.property.value).to.contain('src');
            expect(result.data.jcr.nodeByPath.property.value).to.not.contain('loading', 'filtered attribute');
        });
    });

    it('does not override filtering config for other site', () => {
        installConfig('configs/org.jahia.modules.htmlfiltering.config-noSite.yml');
        modifyContent(path, '<strong>This text is important!</strong>');
        getContent(path).then(result => {
            expect(result.data.jcr.nodeByPath.property.value).to.contain('strong');
        });
    });

    it('can override filtering config for specified site', () => {
        installConfig('configs/org.jahia.modules.htmlfiltering.config-filteringSite.yml');
        modifyContent(path, '<p>This text<strong>is important!</strong> but not this one</p>');
        getContent(path).then(result => {
            expect(result.data.jcr.nodeByPath.property.value).to.contain('<p>');
            expect(result.data.jcr.nodeByPath.property.value).to.not.contain('strong');
            expect(result.data.jcr.nodeByPath.property.value).to.contain('important', 'Tag removed but data remains');
        });
    });

    it('can update config and filter using updated rules', () => {
        editConfig('htmlFiltering.disallow.elements[1].name', 'i', 'filteringSite');
        modifyContent(path, '<p>This text<i>is important!</i> but not this one</p>');
        getContent(path).then(result => {
            expect(result.data.jcr.nodeByPath.property.value).to.contain('<p>');
            expect(result.data.jcr.nodeByPath.property.value).to.not.contain('<i>');
            expect(result.data.jcr.nodeByPath.property.value).to.contain('important', 'Tag removed but data remains');
        });
    });

    it('does not override filtering config for specified site when filtering is disabled', () => {
        disableHtmlFiltering(siteKey);
        installConfig('configs/org.jahia.modules.htmlfiltering.config-filteringSite.yml');
        modifyContent(path, '<p>This text<strong>is important!</strong> but not this one</p>');
        getContent(path).then(result => {
            expect(result.data.jcr.nodeByPath.property.value).to.contain('strong');
        });
    });
});
