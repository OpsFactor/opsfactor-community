package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.mapper;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import lombok.Builder;

import java.util.Map;

/**
 * Dados de apoio para resolver material e UOMs usadas por conversoes de
 * unidade especificas de material no Community.
 */
@Builder
public class ConversaoUnidadeProdutoIntegrationSupportData {
    public Map<String, Produto> materialPorId;
    public Map<String, UnidadeMedida> uomPorId;
}
