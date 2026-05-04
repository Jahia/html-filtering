# html-filtering Changelog

## 3.0.0

### Breaking Changes

* Changed the default HTML filtering strategy for the edit workspace from `SANITIZE` to `REJECT`(#170).

### Bug Fixes

* Added missing ARIA attributes, HTML microdata, and additional semantic HTML elements to the allowlist of the global default configuration (#171)

### Other updates

* Bumped the *OWASP Java HTML Sanitizer* library from 20260102.1 to 20260313.1 (#172)
* Updated jahia parent to latest supported version 8.1.9.0 (#178)