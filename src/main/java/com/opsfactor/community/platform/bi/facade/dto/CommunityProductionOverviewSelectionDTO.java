package com.opsfactor.community.platform.bi.facade.dto;

import com.opsfactor.community.capability.masterdata.network.location.facade.dto.LocationDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seleção compatível com o POST histórico de Production Overview.
 *
 * <p>A UOM quantitativa é obrigatória porque todas as séries de volume são
 * agregadas depois da conversão. Locations vazias mantêm o significado
 * histórico de considerar todas as locations elegíveis.</p>
 */
public class CommunityProductionOverviewSelectionDTO {

    /** Supply Plan cuja fotografia persistida será lida. */
    public Long supplyPlanId;

    /** Unidade na qual volumes, estoque, inbound e demandas são publicados. */
    public String uomId;

    /** Filtro opcional de locations; vazio significa todas as elegíveis. */
    public List<LocationDTO> locationDTOs = new ArrayList<>();

    /** Filtro opcional por características de material. */
    public Map<String, List<String>> valuesByMaterialCharacteristicId = new HashMap<>();

}
