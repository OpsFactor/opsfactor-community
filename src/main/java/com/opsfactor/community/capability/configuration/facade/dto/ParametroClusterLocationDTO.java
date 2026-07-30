package com.opsfactor.community.capability.configuration.facade.dto;

import lombok.Data;

@Data
public class ParametroClusterLocationDTO {

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
