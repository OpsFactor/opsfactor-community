package com.opsfactor.community.capability.masterdata.product.material.integration.mapper;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import java.util.Map;
import lombok.Builder;

/**
 * Dados de apoio para resolver UOMs referenciadas pelo cadastro Community de
 * materiais.
 */
@Builder
public class ProdutoIntegrationSupportData {

    Map<String,UnidadeMedida> unidadeMedidaMap;

    /**
     * Expõe o snapshot de UOM carregado pelo service para que overlays
     * Enterprise resolvam ids por batch, sem consultas entidade a entidade.
     */
    public Map<String, UnidadeMedida> getUnidadeMedidaMap() {

        return unidadeMedidaMap;

    }
    
}
