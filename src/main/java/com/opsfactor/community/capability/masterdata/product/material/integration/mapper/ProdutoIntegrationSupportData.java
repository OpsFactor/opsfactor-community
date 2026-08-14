package com.opsfactor.community.capability.masterdata.product.material.integration.mapper;

import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProduto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import java.util.List;
import java.util.Map;
import lombok.Builder;

/**
 * Dados de apoio para resolver UOMs referenciadas pelo cadastro Community de
 * materiais.
 */
@Builder
public class ProdutoIntegrationSupportData {

    List<CaracteristicaProduto> caracteristicaProdutoList;
    Map<String,UnidadeMedida> unidadeMedidaMap;

    /** Retorna o catalogo ordenado que define as colunas dinamicas do arquivo. */
    public List<CaracteristicaProduto> getCaracteristicaProdutoList() {

        return caracteristicaProdutoList;

    }

    /**
     * Expõe o snapshot de UOM carregado pelo service para que overlays
     * Enterprise resolvam ids por batch, sem consultas entidade a entidade.
     */
    public Map<String, UnidadeMedida> getUnidadeMedidaMap() {

        return unidadeMedidaMap;

    }
    
}
