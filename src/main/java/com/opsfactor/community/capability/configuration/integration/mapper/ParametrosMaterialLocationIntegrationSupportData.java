package com.opsfactor.community.capability.configuration.integration.mapper;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;

import java.util.Map;

/**
 * Dados de apoio para mapear a carga material-location Community.
 *
 * <p>A entidade fisica de material ainda e {@link Produto}, mas o mapa exposto
 * para o mapper usa a nomenclatura funcional nova: material.</p>
 */
public class ParametrosMaterialLocationIntegrationSupportData {
    
    public Map<String,Produto> mapaMaterialPorId;
    public Map<String,Location> mapaLocationPorId;
    public Map<String,UnidadeMedida> mapaUnidadeMedidaPorId;
    
}
