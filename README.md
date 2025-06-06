# HTML Filtering V2.x Module for Jahia

## Overview

The HTML Filtering module provides XSS (Cross-Site Scripting) filtering protection in Jahia when contributing content that have properties containing HTML markup. It helps ensure that user-generated HTML content is safe and free from potentially malicious code.

**Note**:The module doesn't have to be enabled on a site to work and start filtering HTML. As soon as the module is installed and started, it **will be applied** and fully working based on the global default configuration that is embedded.

## Configuration

### Configuration Files and Priority

The module can be configured at three different levels, with a clear priority order:

1. **Site-specific configuration** (highest priority)
    - Filename: `org.jahia.modules.htmlfiltering.site-<SITE_KEY>.yml` (example: `org.jahia.modules.htmlfiltering.site-digitall.yml`)
    - Purpose: Create separate configuration for a specific site

2. **Global custom configuration** (medium priority)
    - Filename: `org.jahia.modules.htmlfiltering.global.custom.yml`
    - Purpose: Override default settings for all Jahia sites
    - Managed by administrators to customize HTML filtering rules

3. **Global default configuration** (lowest priority)
    - Filename: `org.jahia.modules.htmlfiltering.global.default.yml`
    - Purpose: Provide minimal recommended configuration
    - Installed automatically by the module
    - Not recommended to modify as it will be reinstalled with each module update

If a configuration file is invalid or contains incorrect values, it will not be loaded, and the system will fall back to the next configuration in the priority chain. Error logs will indicate which configuration was not loaded and why.

### Configuration Structure

All configuration files follow the same structure, with separate sections for different workspaces. Here's an example of a configuration file:

```yaml
# Configuration file structure
htmlFiltering:
  formatDefinitions:
    HTML_ID: '[a-zA-Z0-9\:\-_\.]+'
    NUMBER_OR_PERCENT: '\d+%?'
    LINKS_URL: '(?:(?:[\p{L}\p{N}\\\.#@$%\+&;\-_~,\?=/!{}:]+|#(\w)+)|(\s*(?:(?:ht|f)tps?://|mailto:)[\p{L}\p{N}][\p{L}\p{N}\p{Zs}\.#@$%\+&:\-_~,\?=/!\(\)]*+\s*))'
  editWorkspace:
    strategy: REJECT
    skipOnPermissions: []
    process: ['nt:base.*']
    skip: []
    allowedRuleSet:
      elements:
        # Rules for allowed elements and attributes
      protocols: [http, https, mailto]
  liveWorkspace:
    strategy: SANITIZE
    skipOnPermissions: []
    process: ['nt:base.*']
    skip: []
    allowedRuleSet:
      elements:
        # Rules for allowed elements and attributes
      protocols: [http, https, mailto]
```

### Configuration Components

#### Format Definitions

The `formatDefinitions` section contains regular expression patterns that can be reused in rules:

```yaml
formatDefinitions:
  HTML_ID: '[a-zA-Z0-9\:\-_\.]+'
  NUMBER_OR_PERCENT: '\d+%?'
  LINKS_URL: '(?:(?:[\p{L}\p{N}\\\.#@$%\+&;\-_~,\?=/!{}:]+|#(\w)+)|(\s*(?:(?:ht|f)tps?://|mailto:)[\p{L}\p{N}][\p{L}\p{N}\p{Zs}\.#@$%\+&:\-_~,\?=/!\(\)]*+\s*))'
```

These patterns can be referenced in the rules to enforce specific formats for attribute values.

#### Workspace Configuration

The module provides separate sections for each Jahia workspace within the same configuration file:

- **editWorkspace**: Configuration section for the default workspace (used for content editing)
- **liveWorkspace**: Configuration section for the live workspace (used for direct content creation on the live workspace)

Having separate sections allows for different levels of protection based on the context. The live workspace often requires stricter rules since it may include content created by users with fewer privileges (e.g., blog posts, forum comments).

#### Strategy

The `strategy` setting defines how the module handles HTML content with potentially unsafe elements:

- **SANITIZE**: Automatically removes invalid markup when content is saved
    - Advantage: Doesn't block saving, cleans content automatically
    - Jahia recommended strategy for live workspace where direct feedback may not be available

- **REJECT**: Validates content when saved and rejects the save operation if invalid markup is found
    - Returns constraint violations listing all invalid tags
    - Requires the user to manually fix the content before saving
    - Jahia recommended strategy for default workspace where content creators have access to advanced editing UI

#### Process and Skip Settings

These settings define which node types and properties should be processed or skipped by the HTML filtering:

- **process**: Defines node types and properties to be processed
    - Example: `process: ['nt:base.*']` applies filtering to all properties of all node types

- **skip**: Defines node types and properties to be excluded from filtering
    - Example: `skip: ['nt:myNodeType.*']` skips filtering for all properties of a specific node type
    - Example: `skip: ['nt:myNodeType.myProperty']` skips filtering for a specific property

The `skip` setting takes precedence over the `process` setting.

**Note**: The node type and property notation doesn't necessarily follow a strict nodetype-property relation. For example, if a node is of type `jnt:bigText` and has an additional mixin `jmix:htmlReadme` (which contains property `j:htmlContent`), then a configuration like `skip: ['jnt:bigText.j:htmlContent']` is valid. It will skip processing of `j:htmlContent` when set on a `jnt:bigText` node, even if there's no direct relation between them.

#### Skip on Permissions

The `skipOnPermissions` setting allows you to specify permissions that, when granted to a user, will cause HTML filtering to be bypassed:

```yaml
skipOnPermissions: ['view-full-wysiwyg-editor', 'site-admin']
```

This is useful when certain privileged users (link to user roles and permissions doc ?) need to include HTML markup that would normally be filtered out. For example:

- `view-full-wysiwyg-editor`: Users with access to the full WYSIWYG rich text editor experience could be granted the power to bypass HTML filtering
- `site-admin`: Site administrators might need to include specific HTML elements for site functionality

It's important to note that Jahia does not recommend any default permissions for this setting to keep content safe regardless of privilege level. It's up to each implementation to determine whether to use this feature and which permissions to include.

**Note**: Using `skipOnPermissions` can create inconsistent content editor experiences. If a privileged user creates HTML content with markup that would normally be filtered, less privileged users attempting to edit that same content later may not be able to save their changes. This happens because the HTML filtering will be applied to their edits, causing the save operation to fail if the existing content contains elements not allowed for their permission level.

#### Rule Sets

The `allowedRuleSet` and `disallowedRuleSet` sections define which HTML elements and attributes are permitted or prohibited:

```yaml
allowedRuleSet:
  elements:
    - attributes: [class, dir, hidden, lang, role, style, title]
    - attributes:
        - id
      format: HTML_ID
    - attributes: [align]
      tags: [caption, col, colgroup, hr, img, table, tbody, td, tfoot, th, thead, tr]
    # More element rules...
  protocols: [http, https, mailto]
```

Each rule can specify:
- **tags**: Which HTML tags the rule applies to
- **attributes**: Which attributes are allowed for those tags
- **format**: A reference to a format definition that the attribute value must match

The `protocols` section defines which URL protocols are allowed in attributes like `href` and `src`.

**Note**: The `allowedRuleSet` is mandatory and should contain at least one rule, while `disallowedRuleSet` is optional.

## GraphQL API

The module exposes a GraphQL API for validating and sanitizing HTML content:

```graphql
query HtmlFiltering($html: String!, $workspace: Workspace = EDIT, $siteKey: String) {
    htmlFiltering {
        validate(html: $html, workspace: $workspace, siteKey: $siteKey) {
            removedTags
            removedAttributes {
                attributes
                tag
            }
            sanitizedHtml
            safe
        }
    }
}
```

This API can be used to validate or sanitize HTML strings, with the appropriate configuration selected based on the specified workspace and site key.

The API returns the following fields:

- **removedTags**: A list of tags that were removed during the sanitization process
- **removedAttributes**: A list of removed attributes along with the tags they were removed from
- **sanitizedHtml**: A sanitized version of the input HTML markup based on the underlying configuration
- **safe**: A boolean value that returns `true` if nothing was removed from the input HTML markup and it's valid according to the configuration that was used

## Usage

For a property to be processed by HTML filtering, the following conditions must be met:

1. The current user doesn't have any permission defined in the `skipOnPermissions` configuration
2. The property matches at least one pattern in the `process` configuration
3. The property doesn't match any pattern in the `skip` configuration
4. The property is defined as a RichText property in the Content Definition File (.cnd)

Example of property definitions in a .cnd file:

```
[nt:myNodeType] > jnt:content, jmix:droppableContent
    - myHTMLProperty (string, richtext)
    - willNotProcessed (string)
```

In this example, `myHTMLProperty` will be processed by HTML filtering because it has the `richtext` constraint, while `willNotProcessed` will not be processed because it lacks this constraint.

### JSON Overrides and HTML Filtering

jContent supports .cnd overrides (called JSON overrides, link ?) which allow you to override the .cnd constraints. For example, JSON Overrides can override property constraints to use a different field type when editing content in Jahia jContent edition UI.

It's important to note that HTML filtering is not able to process these JSON overrides, so they will be ignored by the filtering system. For example, consider a .cnd:

```
[nt:myNodeType] > jnt:content, jmix:droppableContent
    - willNotProcessed (string)
```

coupled with a JSON Override:

```json
{
  "nodeType": "nt:myNodeType",
  "sections": [
    {
      "name": "content",
      "fieldSets": [       
            {
              "rank": 1.2,
              "name": "willNotProcessed",
              "selectorType": "RichText"
            }
          ]
        }
      ]
    }
  ]
}
```

This configuration will have no effect on HTML filtering. jContent will display a WYSIWYG field for your property, allowing you to contribute HTML markup, but HTML filtering will simply ignore this property because it's not declared as `richtext` in the .cnd definition.

## Best Practices

1. **Don't modify the global default configuration file**
    - Create a custom global configuration or site-specific configuration instead

2. **Be careful with skipOnPermissions**
    - Only grant HTML filtering bypass permissions to trusted users
    - Be aware of the potential inconsistent editing experience it may create

3. **Monitor for rejected content saves**
    - Educate content creators about allowed HTML elements and attributes
    - Consider expanding the allowed elements if legitimate use cases arise

4. **Always declare HTML properties with the richtext constraint in .cnd**
    - Remember that JSON overrides do not affect HTML filtering
    - To protect all properties that can contain HTML markup, they must be declared using the RichText selector type in .cnd

5. **Check server logs when adding or editing configurations**
    - Verify that your configuration has been properly loaded
    - Some configuration properties are mandatory and subject to validation
    - If a configuration is invalid, it will not be loaded, and HTML filtering will fall back to another configuration in the priority chain

# Migrating from HTML Filtering v1.x to v2.x

HTML Filtering v2 builds upon the foundation of v1 while introducing significant improvements to address limitations in the original version. While v1 and v2 use similar underlying technology for HTML sanitizing and validation, v2 offers enhanced flexibility and configuration options.

## Important Note
As soon as HTML Filtering v2 is installed and started, it will take over from previous v1.x, and any custom v1 configurations will no longer be taken into account. It's therefore important to familiarize yourself with the new v2 configuration format and adapt any custom configurations you may have in place from v1 before upgrading. Please follow the step-by-step migration instructions below to ensure a smooth transition.

## The most significant changes in v2 include:

- Support for two different filtering strategies: `SANITIZE` or `REJECT` (v1 only supported `SANITIZE` by default with no option to change)
- Separate configuration sections for different workspaces
- Improved configuration file organization to avoid conflicts with default settings
- Enhanced flexibility for content processing based on permissions, node types, and property names
- Configurable format definitions (patterns were hardcoded in v1)

## Configuration Changes

The configuration system has been significantly revised in v2, with changes to both file naming conventions and the structure of the configuration itself.

### File Naming and Organization

Version 2.x introduces a three-tier configuration system designed to make updates safer while allowing for customization:

**V1.x Configuration Files:**
- Default configuration: `org.jahia.modules.htmlfiltering.config-default.yml`
- Site-specific configuration: `org.jahia.modules.htmlfiltering.config-SITE_KEY.yml`

**V2.x Configuration Files:**
- Global default configuration: `org.jahia.modules.htmlfiltering.global.default.yml`
    - Installed automatically with the module
    - Contains recommended security settings
    - Should not be modified as it will be overwritten with each module update
- Global custom configuration: `org.jahia.modules.htmlfiltering.global.custom.yml`
    - New in v2.x
    - Overrides the global default for all sites
    - Intended for administrator customizations
- Site-specific configuration: `org.jahia.modules.htmlfiltering.site-SITE_KEY.yml`
    - Highest priority
    - Only applies to the specific site

This new tiered approach allows the module to always ship with the latest recommended security settings in the global default configuration while giving administrators the ability to customize settings without risk of losing their changes during updates.

When migrating from v1 to v2:
- If you have customized the default configuration in v1, create a new `org.jahia.modules.htmlfiltering.global.custom.yml` file with your customizations
- If you have site-specific configurations, create new files using the `org.jahia.modules.htmlfiltering.site-SITE_KEY.yml` naming pattern
- Do not modify the `org.jahia.modules.htmlfiltering.global.default.yml` file

### Configuration Structure

The structure and syntax of the configuration file has been revised to improve clarity and organization:

#### v1.x Configuration Example

```yaml
htmlFiltering:
  protocols:
    - http
    - https
    - mailto
  attributes:
    - name: class
    - name: dir
    - name: hidden
    - name: id
      pattern: HTML_ID  # Pattern names were hardcoded and not configurable
    - name: lang
    - name: role
    - name: style
    - name: title
    - name: align
      elements: caption, col, colgroup, hr, img, table, tbody, td, tfoot, th, thead, tr
    - name: alt
      elements: img
    # More attributes...
  elements:
    - name: h1, h2, h3, h4, h5, h6, hgroup, p, a, img, figure, figcaption, canvas, picture, br, strong, b, em, i, span, div, ul, ol, li, dl, dd, dt, table, tbody, thead, tfoot, tr, td, th, col, colgroup, caption, blockquote, q, cite, code, pre, var, abbr, address, del, s, details, summary, ins, sub, sup, small, mark, hr, button, legend, audio, video, source, track, nav, article, main, aside, section, time, header, footer, wbr, u
  htmlSanitizerDryRun: false
```

#### v2.x Equivalent Configuration

```yaml
htmlFiltering:
  formatDefinitions:  # New in v2: configurable format definitions
    HTML_ID: '[a-zA-Z0-9\:\-_\.]+'
    NUMBER_OR_PERCENT: '\d+%?'
    LINKS_URL: '(?:(?:[\p{L}\p{N}\\\.#@$%\+&;\-_~,\?=/!{}:]+|#(\w)+)|(\s*(?:(?:ht|f)tps?://|mailto:)[\p{L}\p{N}][\p{L}\p{N}\p{Zs}\.#@$%\+&:\-_~,\?=/!\(\)]*+\s*))'
  editWorkspace:
    strategy: SANITIZE  # Same behavior as v1, or can be changed to REJECT
    skipOnPermissions: []
    process: ['nt:base.*']
    skip: []
    allowedRuleSet:
      elements:
        - attributes: [class, dir, hidden, lang, role, style, title]
        - attributes:
            - id
          format: HTML_ID  # References a format defined in formatDefinitions
        - attributes: [align]
          tags: [caption, col, colgroup, hr, img, table, tbody, td, tfoot, th, thead, tr]
        - attributes: [alt]
          tags: [img]
        # More element rules...
        - tags: [h1, h2, h3, h4, h5, h6, hgroup, p, a, img, figure, figcaption, canvas, picture, br, strong, b, em, i, span, div, ul, ol, li, dl, dd, dt, table, tbody, thead, tfoot, tr, td, th, col, colgroup, caption, blockquote, q, cite, code, pre, var, abbr, address, del, s, details, summary, ins, sub, sup, small, mark, hr, button, legend, audio, video, source, track, nav, article, main, aside, section, time, header, footer, wbr, u]
      protocols: [http, https, mailto]
  liveWorkspace:
    strategy: SANITIZE
    skipOnPermissions: []
    process: ['nt:base.*']
    skip: []
    allowedRuleSet:
      # Similar configuration as editWorkspace but potentially more restrictive
      elements:
        # Element rules...
      protocols: [http, https, mailto]
```

Key structural differences:
- **Format definitions** are now configurable at the top of the configuration (in v1, patterns like HTML_ID were hardcoded and not customizable)
- Configuration is divided into workspace-specific sections (`editWorkspace` and `liveWorkspace`)
- v1's flat structure for attributes and elements has been reorganized into rule sets
- `htmlSanitizerDryRun` option has been removed
- v1 only supported the `SANITIZE` behavior, while v2 allows choosing between `SANITIZE` or `REJECT` strategies for each workspace

## GraphQL API Changes

The GraphQL API has been completely redesigned in v2:

### v1.x GraphQL API

```graphql
mutation PreviewFiltering($text: String!, $siteKey: String!) {
  htmlFilteringConfiguration {
    htmlFiltering {
      testFiltering(
        siteKey: $siteKey
        html: $text
      ) {
        html
        removedElements
        removedAttributes {
          element
          attributes
        }
      }
    }
  }
}
```

### v2.x GraphQL API

```graphql
query HtmlFiltering($html: String!, $workspace: Workspace = EDIT, $siteKey: String) {
  htmlFiltering {
    validate(html: $html, workspace: $workspace, siteKey: $siteKey) {
      removedTags
      removedAttributes {
        attributes
        tag
      }
      sanitizedHtml
      safe
    }
  }
}
```

Key differences in the GraphQL API:
- Changed from a mutation to a query operation
- Added `workspace` parameter to specify which workspace configuration to use
- Renamed `html` to `sanitizedHtml` in the response
- Renamed `removedElements` to `removedTags` in the response
- Added `safe` boolean field to quickly check if the HTML is valid
- Simplified the namespace from `htmlFilteringConfiguration.htmlFiltering` to just `htmlFiltering`

Additionally, v2 removes several configuration-related GraphQL APIs that were present in v1. This was done because providing such specific APIs for HTML filtering configuration doesn't make sense in the broader context. A more generic approach to configuration management via GraphQL would be better but is outside the scope of the HTML filtering module.

## Migration Steps

1. **Review your existing v1 configuration files**
    - Identify all custom configurations you have in place
    - Note that v1 always used the SANITIZE strategy by default

2. **Create new v2 configuration files**
    - For site-specific configurations: create new files using the `org.jahia.modules.htmlfiltering.site-SITE_KEY.yml` pattern
    - For global customizations: create a new `org.jahia.modules.htmlfiltering.global.custom.yml` file
    - Do not modify the default `org.jahia.modules.htmlfiltering.global.default.yml` file
    - Define your format definitions if you need custom patterns
    - Restructure your configuration according to the v2 format with workspace-specific sections
    - Choose appropriate strategies for each workspace (`SANITIZE` to maintain v1 behavior or `REJECT` for stricter validation)
    - Note that the v2 global default configuration includes new options (`process`, `skip`, `skipOnPermissions`) with values that make it behave just like v1 did. These options don't need to be changed for basic migration but are available for additional customization if needed.

3. **Update GraphQL API calls**
    - Refactor any code that uses the v1 GraphQL API to use the new v2 API
    - Update field references to match the new response structure
    - If you were using any configuration-related GraphQL APIs, these are no longer available

4. **Test thoroughly**
    - Test your migrated configuration with various HTML content to ensure it behaves as expected
    - Pay special attention to any content that might be affected by the new filtering behavior
    - If you've chosen the `REJECT` strategy, ensure your content editors are prepared for potential validation errors

5. **Remove old configuration files**
    - Once you have confirmed that your v2 configuration is working correctly, you can safely remove the old v1 configuration files
    - This includes both the default configuration (`org.jahia.modules.htmlfiltering.config-default.yml`) and any site-specific configurations (`org.jahia.modules.htmlfiltering.config-SITE_KEY.yml`)

By following these steps, you can successfully migrate from HTML Filtering v1.x to v2.x and take advantage of the enhanced features and flexibility offered by the new version.