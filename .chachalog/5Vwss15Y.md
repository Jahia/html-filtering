---
# Allowed version bumps: patch, minor, major
html-filtering: minor
---

Added a `DISABLED` strategy to install the module and run its report before any content is rejected or sanitized (#190). The log warns about each configuration that uses it. Custom code that handles each strategy value must also handle `DISABLED`.
