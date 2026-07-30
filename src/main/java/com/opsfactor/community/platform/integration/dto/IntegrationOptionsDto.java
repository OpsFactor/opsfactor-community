package com.opsfactor.community.platform.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Options comuns a integracoes JSON baseadas em {@link IntegrationDto}.
 * Novos comportamentos transversais devem preferencialmente ser adicionados
 * aqui para evitar duplicacao entre DTOs especificos de options.
 */
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class IntegrationOptionsDto {

    /**
     * Quando true, registros com dependencias obrigatorias nao resolvidas
     * podem ser ignorados pela infraestrutura comum de importacao.
     */
    public Boolean skipRecordsWithUnresolvedDependencies;

}
