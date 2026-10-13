package com.opsfactor.community.platform.integration.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Associates a DTO property with the existing file-header contract.
 *
 * <p>The zero-based position supplies the Excel label without duplicating its
 * text. Required is opt-in for scalar/primary-key validation; reference
 * resolution retains the mapper's operation-specific required argument.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface IntegrationFileColumn {

    int value();

    boolean required() default false;

}
