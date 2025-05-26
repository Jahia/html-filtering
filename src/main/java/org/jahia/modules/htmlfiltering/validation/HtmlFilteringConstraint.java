package org.jahia.modules.htmlfiltering.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom constraint to validate html
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = HtmlValidator.class)
public @interface HtmlFilteringConstraint {
    String message() default "{htmlFiltering.invalid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
