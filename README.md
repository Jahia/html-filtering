# HTML Filtering Configuration

HTML filtering in Jahia uses a multi-level configuration resolution strategy to determine which rules to apply for a site:

1. **Site-specific configuration**: `org.jahia.modules.htmlfiltering.site-<site key>.yml` found in any OSGi bundle
2. **Global custom configuration**: `org.jahia.modules.htmlfiltering.global.custom.yml` found in any OSGi bundle
3. **Global default configuration**: `org.jahia.modules.htmlfiltering.global.default.yml` provided by this *HTML Filtering* bundle

The system tries to find a policy in the order listed above. If a site-specific configuration exists, it will be used. If not, the global custom configuration applies. If neither exists, the system falls back to the global default built-in configuration.

**Note**: The global default configuration should not be modified as module updates would replace it.

Full documentation for the module can be found in [this Jahia Academy page](https://academy.jahia.com/documentation/jahia-cms/jahia-8-2/developer/security/html-filtering).
