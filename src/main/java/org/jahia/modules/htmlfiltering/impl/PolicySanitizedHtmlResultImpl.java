package org.jahia.modules.htmlfiltering.impl;

import org.jahia.modules.htmlfiltering.PolicySanitizedHtmlResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class PolicySanitizedHtmlResultImpl implements PolicySanitizedHtmlResult {
    private final Set<String> rejectedTags = new HashSet<>();
    private final Map<String, Set<String>> rejectedAttributesByTag = new HashMap<>();
    private String sanitizedHtml;

    public void addRejectedTag(String tag) {
        rejectedTags.add(tag);
    }

    @Override
    public boolean isValid() {
        return rejectedTags.isEmpty() && rejectedAttributesByTag.isEmpty();
    }

    @Override
    public String getSanitizedHtml() {
        return sanitizedHtml;
    }

    @Override
    public Set<String> getRejectedTags() {
        return rejectedTags;
    }

    void addRejectedAttributeByTag(String tag, Set<String> attributes) {
        // merge the attributes with the existing ones for that tag (if any)
        rejectedAttributesByTag.compute(tag, (k, existingAttributes) -> {
            if (existingAttributes == null) {
                return attributes;
            } else {
                existingAttributes.addAll(attributes);
                return existingAttributes;
            }
        });
    }

    @Override
    public Map<String, Set<String>> getRejectedAttributesByTag() {
        return rejectedAttributesByTag;
    }

    public void setSanitizedHtml(String sanitizedHtml) {
        this.sanitizedHtml = sanitizedHtml;
    }
}
