package org.jahia.modules.htmlfiltering.validation;

import org.jahia.services.content.decorator.validation.JCRNodeValidatorDefinition;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

/**
 * Registers the html validator beans by nodeType.
 */
@Component(immediate = true, service = JCRNodeValidatorDefinition.class)
public class HtmlFilteringValidatorDefinition extends JCRNodeValidatorDefinition {

    private final static Logger logger = LoggerFactory.getLogger(HtmlFilteringValidatorDefinition.class);

    @Override
    public Map<String, Class> getValidators() {
        logger.info("Regisering HTML filtering validators");
        return Collections.singletonMap("nt:base", HtmlFilteringValidator.class);
    }
}
