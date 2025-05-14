# HTML Filtering Configuration

HTML filtering in Jahia uses a multi-level configuration resolution strategy to determine which rules to apply for a site:

1. **Site-specific configuration**: `org.jahia.modules.htmlfiltering-<site key>.yml` found in any OSGi bundle
2. **Default configuration**: `org.jahia.modules.htmlfiltering.default.yml` found in any OSGi bundle
3. **Fallback configuration**: `org.jahia.modules.htmlfiltering.fallback.yml` provided by the *HTML Filtering* bundle

The system tries to find a policy in the order listed above. If a site-specific configuration exists, it will be used. If not, the default configuration applies. If neither exists, the system falls back to the built-in configuration.

**Note**: The fallback configuration should not be modified as module updates would replace it.
