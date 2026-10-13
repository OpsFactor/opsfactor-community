package com.opsfactor.community.platform.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.opsfactor.community.platform.integration.validation.IntegrationInputAware;
import com.opsfactor.community.platform.integration.validation.IntegrationInputContext;
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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class IntegrationPrimaryKeyDTOAbstract<PRIMARYKEYDTO extends IntegrationPrimaryKeyDTOAbstract, ENTITY> implements IntegrationInputAware {

    /** Input provenance is private and excluded from JSON and business equality. */
    @JsonIgnore
    private transient IntegrationInputContext integrationInputContext;

    @Override
    @JsonIgnore
    public IntegrationInputContext getIntegrationInputContext() {

        return integrationInputContext;

    }

    @Override
    @JsonIgnore
    public void setIntegrationInputContext(IntegrationInputContext integrationInputContext) {

        this.integrationInputContext = integrationInputContext;

    }

    @Override
    @JsonIgnore
    public boolean isIntegrationPrimaryKey() {

        return true;

    }


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
