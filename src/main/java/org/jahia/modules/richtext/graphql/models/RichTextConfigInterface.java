package org.jahia.modules.richtext.graphql.models;

import java.util.List;
import java.util.Set;

public interface RichTextConfigInterface {

    public Set<String> getProtocols();
    public Set<String> getElements();
    public List<GqlRichTextConfigAttribute> getAttributes();
}
