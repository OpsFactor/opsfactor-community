package com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VersaoMalhaDTO {
    
    private String id;
    
    private String description;

    /**
     * Location de origem usada pelo heuristico quando uma materia-prima nao
     * possui abastecimento inbound mais especifico na versao de malha.
     */
    private String defaultRawMaterialOriginLocationId;

    /**
     * Lead time, em dias, da linha temporaria criada para a origem padrao de
     * materia-prima. Nulo remove o override da versao de malha.
     */
    private Double defaultRawMaterialOriginLeadTimeDays;
    
}
