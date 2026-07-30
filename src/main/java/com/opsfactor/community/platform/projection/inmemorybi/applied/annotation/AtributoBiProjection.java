package com.opsfactor.community.platform.projection.inmemorybi.applied.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, // objetivo é indexar campos e métodos no BIEmMemoria
        ElementType.ANNOTATION_TYPE}) // pode ser aplicado a outras interfaces para criar interfaces compostas
public @interface AtributoBiProjection {

}
