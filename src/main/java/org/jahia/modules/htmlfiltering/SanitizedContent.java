package org.jahia.modules.htmlfiltering;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SanitizedContent {
    private final Set<String> removedTags = new HashSet<>();
    private final Map<String, Set<String>> removedAttributes = new HashMap<>();
    private String sanitizedContent;

    public void setSanitizedContent(String sanitizedContent) {
        this.sanitizedContent = sanitizedContent;
    }

    public Set<String> getRemovedTags() {
        return removedTags;
    }

    public Map<String, Set<String>> getRemovedAttributes() {
        return removedAttributes;
    }

    public String getSanitizedContent() {
        return sanitizedContent;
    }
}
