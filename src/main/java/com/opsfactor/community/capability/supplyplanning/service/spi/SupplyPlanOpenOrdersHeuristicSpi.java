package com.opsfactor.community.capability.supplyplanning.service.spi;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;

/**
 * Extensão opcional para acrescentar ao heurístico as ordens abertas mantidas
 * exclusivamente pelo Enterprise.
 *
 * <p>O Community não conhece entidades nem repositories de pedidos. Quando a
 * implementação não está presente, o plano heurístico mantém somente as
 * entradas abertas da edição Community. O Enterprise usa o mesmo calendário,
 * filtros e projections comuns para representar transferências e compras como
 * movimentos firmes, e vendas como demanda direta considerada.</p>
 */
public interface SupplyPlanOpenOrdersHeuristicSpi {

    /**
     * Materializa as entradas firmes e a carteira de vendas de um novo plano
     * antes de o motor heurístico iniciar a alocação.
     */
    void materializaEntradasECarteiraParaNovoPlanoHeuristico(
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            SupplyNetworkProjection supplyNetworkProjection,
            PoliticaEstoquesProjection politicaEstoquesProjection,
            MaterialProjection materialProjection,
            LocationProjection locationProjection);

    /**
     * Recompõe somente a carteira de vendas quando uma reexecução não possui
     * mais sua fotografia persistida de demanda direta.
     */
    void materializaCarteiraParaDemandaDiretaHeuristica(
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            SupplyNetworkProjection supplyNetworkProjection,
            PoliticaEstoquesProjection politicaEstoquesProjection,
            MaterialProjection materialProjection,
            LocationProjection locationProjection);

}
