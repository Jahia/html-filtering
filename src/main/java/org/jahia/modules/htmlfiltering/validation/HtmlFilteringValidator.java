package org.jahia.modules.htmlfiltering.validation;

import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.validation.JCRNodeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Html filtering Bean to be validated
 */
public class HtmlFilteringValidator implements JCRNodeValidator {

    private final static Logger logger = LoggerFactory.getLogger(HtmlFilteringValidator.class);
    private final JCRNodeWrapper node;

    public HtmlFilteringValidator(JCRNodeWrapper node) {
        logger.info("Creating HtmlFilteringValidator for {}", node.getPath());
        this.node = node;
    }

    /**
     * This method is called at the validation time. it fills the validation context with the constraint validation results
     */
    @HtmlFilteringConstraint
    public JCRNodeWrapper getNode() {
        return node;
    }

}
