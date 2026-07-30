package com.opsfactor.community.capability.supplyplanning.service.spi;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanningProjection;

import java.util.Set;

/**
 * Materializa a projection de Demand Planning que o Supply Planning consome
 * quando uma rodada pede catch-up de sell-out passado.
 *
 * <p>O contrato permanece deliberadamente pequeno: ele recebe apenas o
 * recorte ja calculado pelo {@code SupplyPlanService} e devolve uma projection
 * pronta para consumo. Assim a extensao Enterprise pode reutilizar a
 * {@code SalesProjectionFactory} existente, sem repository adicional, consulta
 * diaria ad-hoc ou acesso entidade a entidade no calculo.</p>
 */
public interface SupplyPlanDemandCatchUpProjectionSpi {

    /**
     * Constroi a projection usada para materializar demanda direta do plano
     * novo. A projection retornada ja contem as linhas de Demand Plan exigidas
     * por esse fluxo.
     */
    DemandPlanningProjection getDemandPlanningProjectionParaDemandaDireta(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            DemandPlan demandPlan,
            Set<Location> locations,
            Set<Produto> materiais);

    /**
     * Constroi a projection completa usada para projetar estoque inicial a
     * partir de um Supply Plan anterior.
     */
    DemandPlanningProjection getDemandPlanningProjectionCompletaParaEstoqueInicial(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            DemandPlan demandPlan,
            Location location,
            Set<Produto> materiais);

}
