package com.opsfactor.community.capability.masterdata.classification.characteristic.facade.mapper;

import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto.FiltroMaterialLocationDeCombinacaoCaracteristicasDTO;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjectionFactory;

/**
 * Converte o contrato público de seleção de material/location na projection
 * canônica de locations.
 *
 * <p>Esta é a porta Community do mapper já consolidado no legado. Os callers
 * não devem reproduzir a semântica AND/OR nem consultar características
 * diretamente.</p>
 */
public interface FiltroLocationDeCombinacaoCaracteristicasMapper {

    /**
     * Resolve locations explícitas e características sobre o snapshot comum.
     * Uma seleção ausente ou vazia representa todo o escopo permitido pela
     * flag de atividade.
     */
    static LocationProjection getLocationProjection(
            FiltroMaterialLocationDeCombinacaoCaracteristicasDTO dto,
            ClusterEParametrosProjection clusterEParametrosProjection,
            boolean activeLocationsOnly) {

        return LocationProjectionFactory.getLocationProjectionFiltroCombinacoesCaracteristicasIds(
                dto == null || dto.isSelecaoLocationsVazia()
                        ? null
                        : dto.valuesByLocationCharacteristicId,
                dto == null ? null : dto.locationIds,
                clusterEParametrosProjection,
                activeLocationsOnly);

    }

}
