package com.opsfactor.community.platform.integration.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.lang.reflect.Field;

/**
 * Base das chaves primarias dos DTOs de integracao.
 * <p>
 * As subclasses declaram campos publicos da chave e o Lombok gera
 * `equals/hashCode` sobre esses campos. O metodo
 * {@link #hasSameKeyAsEntity(Object)} existe para comparar o DTO com a entidade
 * JPA correspondente durante merges e deletes.
 */
@AllArgsConstructor
@EqualsAndHashCode
public abstract class IntegrationPrimaryKeyDTOAbstract<PRIMARYKEYDTO extends IntegrationPrimaryKeyDTOAbstract, ENTITY> {

    public abstract boolean hasSameKeyAsEntity(ENTITY entity);

    /**
     * Normaliza campos texto da chave antes de usar equals/hashCode no fluxo
     * generico de integracao.
     */
    public void normalizaCamposTextoEntradaIntegracao() {

        IntegrationTextNormalization.normalizaCamposPublicos(this);

    }
    
    public boolean allFieldsAreEmpty() {

        /*
         * As chaves de integracao expõem seus campos como publicos para manter
         * o mapper generico simples. Falha de reflexao aqui indica erro de
         * codigo e deve interromper a carga, nao marcar a linha como vazia.
         */
        try {
            Field[] fields = this.getClass().getFields();
            for (Field field : fields) {
                if (field.get(this) != null) return false;
            }
            return true;
        } catch (IllegalAccessException illegalAccessException) {
            throw new IllegalStateException(
                    "Could not inspect integration primary key DTO fields for " + this.getClass().getName(),
                    illegalAccessException);
        }
    }
        
}
