package com.opsfactor.community.platform.integration.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.opsfactor.community.platform.integration.validation.IntegrationInputAware;
import com.opsfactor.community.platform.integration.validation.IntegrationInputContext;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.lang.reflect.Field;

/**
 * Base dos DTOs de integracao usados por data upload/download.
 *
 * <p>Cada DTO carrega uma chave primaria desdobrada via `@JsonUnwrapped` e a
 * coluna tecnica `delete`. A igualdade funcional e baseada na chave primaria,
 * porque o fluxo generico compara linhas de arquivo com entidades ja
 * persistidas antes de criar, atualizar ou remover registros.</p>
 */
@SuperBuilder
@NoArgsConstructor
public abstract class IntegrationDataDtoAbstract<DTO extends IntegrationDataDtoAbstract, PRIMARYKEYDTO extends IntegrationPrimaryKeyDTOAbstract, ENTITY> implements IntegrationInputAware {

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

        return false;

    }

    
    /** Retains the pre-existing constructor without including transport metadata. */
    public IntegrationDataDtoAbstract(String delete, PRIMARYKEYDTO primaryKeyDto) {

        this.delete = delete;
        this.primaryKeyDto = primaryKeyDto;

    }

    public String delete;

    @JsonUnwrapped
    public PRIMARYKEYDTO primaryKeyDto;

    public boolean equals(DTO dto) {
        return primaryKeyDto.equals(dto.primaryKeyDto);
    }
    
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (!(o.getClass().equals(this.getClass()))) return false;
        DTO other = (DTO) o;
        return this.equals(other);
    }

    public boolean hasSameKeyAsEntity(ENTITY entity) {
        return primaryKeyDto.hasSameKeyAsEntity(entity);
    };

    /**
     * Normaliza campos texto do DTO antes de calcular chaves e entidades.
     * A regra remove espacos finais e converte texto vazio em nulo, mantendo
     * a mesma semantica ja usada por arquivos processados.
     */
    public void normalizaCamposTextoEntradaIntegracao() {

        IntegrationTextNormalization.normalizaCamposPublicos(this);

        if (primaryKeyDto != null) {
            primaryKeyDto.normalizaCamposTextoEntradaIntegracao();
        }

    }
    
    public boolean allFieldsAreEmpty() {

        /*
         * `primaryKeyDto` e `delete` sao campos estruturais do protocolo de
         * integracao. Eles so tornam a linha preenchida quando a propria chave
         * tem algum valor funcional.
         */
        if (primaryKeyDto != null && !primaryKeyDto.allFieldsAreEmpty()) return false;

        try {
            Field[] fields = this.getClass().getFields();
            for (Field field : fields) {
                if (field.getName().equals("primaryKeyDto") || field.getName().equals("delete")) continue;
                if (field.get(this) != null) return false;
            }
            return true;
        } catch (IllegalAccessException illegalAccessException) {
            throw new IllegalStateException(
                    "Could not inspect integration DTO fields for " + this.getClass().getName(),
                    illegalAccessException);
        }
    }
        
}
