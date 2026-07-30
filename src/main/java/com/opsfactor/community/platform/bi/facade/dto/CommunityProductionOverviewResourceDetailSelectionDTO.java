package com.opsfactor.community.platform.bi.facade.dto;

import com.opsfactor.community.capability.masterdata.network.location.facade.dto.LocationDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Corpo aceito pela abertura de detalhe do Production Overview legado.
 *
 * <p>A abertura é por recurso e período, portanto somente o filtro de
 * características de material participa da seleção das linhas. Os demais
 * campos são mantidos para que o POST legado continue desserializável, mas
 * não redefinem o plano, a location, o período nem a unidade por linha.</p>
 */
public class CommunityProductionOverviewResourceDetailSelectionDTO {

    /** Mantido por compatibilidade; o plano é a variável da rota. */
    public Long supplyPlanId;

    /** Mantido por compatibilidade; a resposta publica a UOM própria de cada linha. */
    public String uomId;

    /** Mantido por compatibilidade; o recurso da rota já determina sua location. */
    public List<LocationDTO> locationDTOs = new ArrayList<>();

    /** Único filtro do corpo aplicado na abertura detalhada. */
    public Map<String, List<String>> valuesByMaterialCharacteristicId = new HashMap<>();

}
