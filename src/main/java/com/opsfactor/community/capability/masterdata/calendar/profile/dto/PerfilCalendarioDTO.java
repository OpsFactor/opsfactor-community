package com.opsfactor.community.capability.masterdata.calendar.profile.dto;

import com.opsfactor.community.platform.utility.Constantes.TamanhoBucket;
import lombok.Getter;
import lombok.Setter;

/** Contrato de tela da receita uniforme; detalhes de extensões não pertencem ao Community. */
@Getter
@Setter
public class PerfilCalendarioDTO {

    private String id;
    private String description;
    private String type = "SIMPLES";
    private TamanhoBucket baseBucketSize;
    /** Calculado a partir da receita, não aceito como configuração independente. */
    private TamanhoBucket firstPeriodBucketSize;
    private Integer numberOfBasePeriods;

}
