package com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoPoliticaEstoques;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoques;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoquesMaterialLocation;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.supplyplanning.configuration.repository.PerfilExecucaoPoliticaEstoquesRepository;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.FuncoesMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Factory da projection operacional de politica de estoques.
 *
 * <p>No Community a projection alimenta apenas safety stock e parametros
 * operacionais do heuristico. Inventory Policy Optimization e resultados de
 * otimizacao de politica permanecem no overlay Enterprise.</p>
 */
@Component
public class PoliticaEstoquesProjectionFactory {
    
    /**
     * Repositório de associação entre perfil de execução Supply Plan e políticas de estoque.
     *
     * <p>Na edição Community essa factory existe para materializar os parâmetros operacionais de
     * safety stock usados pelo DRP heurístico. A otimização de política de estoques permanece fora
     * deste módulo; por isso a factory sempre parte do perfil de Supply Plan em execução e não
     * possui caminho público para carregar todas as políticas indiscriminadamente.</p>
     */
    @Autowired
    private PerfilExecucaoPoliticaEstoquesRepository perfilExecucaoPoliticaEstoquesRepository;
    
    public PoliticaEstoquesProjection getPoliticaEstoquesProjection(
            Calendario calendario,
            ClusterEParametrosProjection clusterEParametrosProjection,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) {

        List<PoliticaEstoques> politicasEstoquesOrdenadasPorPrioridade =
                perfilExecucaoPoliticaEstoquesRepository
                        .customFindByPerfilExecucaoSupplyPlan(perfilExecucaoSupplyPlan.getId())
                        .stream()
                        .map(PerfilExecucaoPoliticaEstoques::getPoliticaEstoques)
                        .sorted(Comparator.comparing(PoliticaEstoques::getPrioridade))
                        .collect(Collectors.toList());

        return getPoliticaEstoquesProjection(
                calendario,
                clusterEParametrosProjection,
                politicasEstoquesOrdenadasPorPrioridade);

    }

    private PoliticaEstoquesProjection getPoliticaEstoquesProjection(
            Calendario calendario,
            ClusterEParametrosProjection clusterEParametrosProjection,
            Collection<PoliticaEstoques> politicasEstoquesOrdenadasPorPrioridade) {

        PoliticaEstoquesProjection politicaEstoquesProjection = new PoliticaEstoquesProjection();
        politicaEstoquesProjection.calendario = calendario;
        politicaEstoquesProjection.clusterEParametrosProjection = clusterEParametrosProjection;

        for (PoliticaEstoques politicaEstoques : politicasEstoquesOrdenadasPorPrioridade) {

            int posicaoPeriodoInicio = (politicaEstoques.getDataHorarioInicio() == null) ? 0 : Math.max(0, calendario.getPosicaoPeriodo(politicaEstoques.getDataHorarioInicio()));
            int posicaoPeriodoFim = (politicaEstoques.getDataHorarioFim()== null) ? calendario.getNumeroPeriodosTotais()-1 : Math.min(calendario.getPosicaoPeriodoFinalFuturo(), calendario.getPosicaoPeriodo(politicaEstoques.getDataHorarioFim()));

            for (int i = posicaoPeriodoInicio; i <= posicaoPeriodoFim; i++) {

                for (PoliticaEstoquesMaterialLocation politicaEstoquesMaterialLocation : politicaEstoques.getPoliticaEstoquesMaterialLocationList()) {

                    Produto material = politicaEstoquesMaterialLocation.getMaterial();
                    Location location = politicaEstoquesMaterialLocation.getLocation();

                    FuncoesMap.getOrAddElementoDeNestedMap(
                            politicaEstoquesProjection.mapaPoliticaEstoquesVigenteParaPeridoLocationMaterial,
                            PoliticaEstoquesMaterialLocation.class,
                            () -> politicaEstoquesMaterialLocation,
                            i, location, material);

                }
            }
        }

        return politicaEstoquesProjection;

    }
    
}
