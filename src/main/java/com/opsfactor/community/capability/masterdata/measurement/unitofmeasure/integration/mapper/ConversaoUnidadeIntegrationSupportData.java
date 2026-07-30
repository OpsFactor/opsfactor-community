package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.mapper;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import lombok.Builder;

import java.util.Map;

/**
 * Dados de apoio para resolver UOMs usadas por conversoes globais Community.
 */
@Builder
public class ConversaoUnidadeIntegrationSupportData {
    public Map<String, UnidadeMedida> uomPorId;
}
