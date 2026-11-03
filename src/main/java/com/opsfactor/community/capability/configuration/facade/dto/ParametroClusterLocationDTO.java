package com.opsfactor.community.capability.configuration.facade.dto;

import com.opsfactor.community.capability.configuration.domain.cluster.PerfilClusterizacao;
import lombok.Data;

@Data
public class ParametroClusterLocationDTO {

    /** Contexto explícito da identidade do cluster; entrada antiga permanece em LEGADO. */
    private String perfilClusterizacaoId = PerfilClusterizacao.ID_LEGADO;
    private long id;
    private String clusterLocations;
    private Long clusterLocationsID;
    private Boolean planejaDP;
    /**
     * Campo mantido apenas como envelope do front compartilhado.
     * No Community ele sempre e devolvido como false, e true e rejeitado na borda.
     */
    private Boolean planejaPricing;

}
