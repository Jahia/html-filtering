package org.jahia.modules.htmlfiltering.validation;

import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.validation.JCRNodeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Html filtering Bean to be validated
 */
@HtmlFilteringConstraint
public class HtmlFilteringValidator implements JCRNodeValidator {

    private static final Logger logger = LoggerFactory.getLogger(HtmlFilteringValidator.class);
    private final JCRNodeWrapper node;

    public HtmlFilteringValidator(JCRNodeWrapper node) {
        logger.debug("Creating HtmlFilteringValidator for {}", node.getPath());
        this.node = node;
    }

    public JCRNodeWrapper getNode() {
        return node;
    }
}
