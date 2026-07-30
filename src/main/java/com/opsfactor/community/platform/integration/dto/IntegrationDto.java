package com.opsfactor.community.platform.integration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Envelope generico usado pelos endpoints JSON de data upload.
 * <p>
 * No OpsFactor Community, operacoes mutaveis de integracao sao sempre
 * sincronas. O campo {@link #threadSync} permanece no contrato para
 * compatibilidade com o front compartilhado, mas valores diferentes de
 * {@link ThreadSync#SYNC} sao bloqueados na camada web/service.
 */
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationDto<DATADTO extends IntegrationDataDtoAbstract<DATADTO,PRIMARYKEYDTO,?>, PRIMARYKEYDTO extends IntegrationPrimaryKeyDTOAbstract<PRIMARYKEYDTO, ?>, DATAFILTER, OPTIONS> {

    public List<DATADTO> data;
    @Nullable public OPTIONS options;
    @Nullable public DATAFILTER dataFilter;
    @Nullable public Map<String, IntegrationMapperInterface.MetodoAtualizacaoCampo> dataFieldConfiguration;

    public enum ThreadSync {
        @JsonProperty("sync") SYNC,
        @JsonProperty("async") ASYNC
    }
    @Nullable public ThreadSync threadSync;

}
