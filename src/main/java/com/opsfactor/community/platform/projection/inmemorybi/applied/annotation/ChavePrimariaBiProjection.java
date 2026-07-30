package com.opsfactor.community.platform.projection.inmemorybi.applied.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca campos que compõem a identidade primária de um registro em um
 * {@link BIProjectionAnotacoes}.
 * <p>
 * A anotação também é um {@link AtributoBiProjection}: todo campo de chave
 * primária continua sendo indexado individualmente pelo BI. Quando o projection
 * é criado com índice composto habilitado, esses mesmos campos são usados, na
 * ordem declarada na classe, para montar uma chave única consultável por
 * {@code UniqueIndex}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@AtributoBiProjection
public @interface ChavePrimariaBiProjection {
}
