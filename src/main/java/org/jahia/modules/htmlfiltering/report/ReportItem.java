package org.jahia.modules.htmlfiltering.report;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Locale;

/**
 * One property of one content node in one language, whose value does not pass the HTML filtering policy.
 */
public final class ReportItem {

    private final String path;
    private final String property;
    private final String language;
    private final List<String> markups;

    @JsonCreator
    public ReportItem(@JsonProperty("path") String path, @JsonProperty("property") String property,
                      @JsonProperty("language") String language, @JsonProperty("markups") List<String> markups) {
        this.path = path;
        this.property = property;
        this.language = language;
        this.markups = List.copyOf(markups);
    }

    public String getPath() {
        return path;
    }

    public String getProperty() {
        return property;
    }

    /**
     * @return the language of the value, or <code>null</code> for a property that is not internationalized
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @return each rejected tag, such as <code>&lt;script&gt;</code>, and each rejected attribute with its tag, such as
     * <code>&lt;img onerror&gt;</code>
     */
    public List<String> getMarkups() {
        return markups;
    }

    /**
     * @param path the path of a node, or a blank value for every node
     * @return <code>true</code> if the item is the node at that path or a node under it
     */
    public boolean isUnder(String path) {
        if (path == null || path.isBlank()) {
            return true;
        }
        String parent = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        return this.path.equals(parent) || this.path.startsWith(parent + "/");
    }

    /**
     * @param markup a tag or an attribute name, with or without its angle brackets, or a blank value for every item
     * @return <code>true</code> if one of the markups of the item holds that tag or that attribute
     */
    public boolean includesMarkup(String markup) {
        if (markup == null || markup.isBlank()) {
            return true;
        }
        String name = markup.replaceAll("[<>]", "").trim().toLowerCase(Locale.ROOT);
        return markups.stream()
                .flatMap(m -> List.of(m.replaceAll("[<>]", "").split(" ")).stream())
                .anyMatch(name::equals);
    }
}
