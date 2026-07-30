package com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.mapper;

import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoques;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;

import java.util.Map;

/**
 * Support data da carga material/location de politica de estoque.
 *
 * <p>Os mapas sao carregados uma vez por batch para que o mapper resolva
 * politica, material e location sem buscas repetidas no banco a cada linha do
 * arquivo.</p>
 */
public class PoliticaEstoquesMaterialLocationIntegrationSupportData {

    /**
     * Politicas operacionais disponiveis por id.
     */
    public Map<String, PoliticaEstoques> mapaPoliticaEstoquesPorId;

    /**
     * Materiais Community disponiveis por id.
     */
    public Map<String, Produto> mapaMaterialPorId;

    /**
     * Locations Community disponiveis por id.
     */
    public Map<String, Location> mapaLocationPorId;

}
