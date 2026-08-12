package com.opsfactor.community.capability.masterdata.classification.characteristic.facade.mapper;

import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto.FiltroMaterialLocationDeCombinacaoCaracteristicasDTO;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjectionFactory;

/**
 * Converte o contrato público de seleção de material/location na projection
 * canônica de materiais.
 *
 * <p>Esta é a porta Community do mapper já consolidado no legado. Os callers
 * não devem reproduzir a semântica AND/OR nem consultar características
 * diretamente.</p>
 */
public interface FiltroMaterialDeCombinacaoCaracteristicasMapper {

    /**
     * Resolve materiais explícitos e características sobre o snapshot comum.
     * Uma seleção ausente ou vazia representa todo o escopo permitido pela
     * flag de atividade.
     */
    static MaterialProjection getMaterialProjection(
            FiltroMaterialLocationDeCombinacaoCaracteristicasDTO dto,
            ClusterEParametrosProjection clusterEParametrosProjection,
            boolean activeMaterialsOnly) {

        return MaterialProjectionFactory.getMaterialProjectionFiltroCombinacoesCaracteristicasIds(
                dto == null || dto.isSelecaoMateriaisVazia()
                        ? null
                        : dto.valuesByMaterialCharacteristicId,
                dto == null ? null : dto.materialIds,
                clusterEParametrosProjection,
                activeMaterialsOnly);

    }

}
