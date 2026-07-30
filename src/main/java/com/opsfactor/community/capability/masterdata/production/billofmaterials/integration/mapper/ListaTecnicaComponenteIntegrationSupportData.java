package com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.mapper;

import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import java.util.Map;

/**
 * Dados de apoio para resolver BOM, material componente e UOM durante data
 * upload Community.
 */
public class ListaTecnicaComponenteIntegrationSupportData {
    
    public Map<String,UnidadeMedida> mapaUnidadeMedidaPorId;
    public Map<String,ListaTecnica> mapaListaTecnicaPorId;
    public Map<String,Produto> mapaMaterialPorId;
    
}
