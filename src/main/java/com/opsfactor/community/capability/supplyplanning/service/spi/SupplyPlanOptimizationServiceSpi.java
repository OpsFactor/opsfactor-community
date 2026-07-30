package com.opsfactor.community.capability.supplyplanning.service.spi;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.production.productionresource.projection.BIProjectionCapacidadeProdutiva;
import jakarta.annotation.Nullable;

/**
 * SPI implementada pelo Enterprise para execucao otimizada do Supply Planning.
 *
 * <p>Nao existe implementacao Community. Se um perfil solicitar otimizador sem
 * que o bean Enterprise esteja presente, o SupplyPlanService deve falhar com
 * RequiresEnterpriseVersionException.</p>
 *
 * <p>A assinatura especifica do otimizador recebe as projections ja preparadas
 * pelo SupplyPlanService. O otimizador precisa desses mesmos recortes de malha,
 * material, location, politica de estoque e capacidade produtiva para montar o
 * modelo matematico; recriar tudo dentro do Enterprise tornaria mais dificil
 * garantir que a rodada otimizada usou a mesma fotografia de dados do fluxo
 * base.</p>
 */
public interface SupplyPlanOptimizationServiceSpi extends SupplyPlanExecutionServiceSpi {

    /**
     * Executa o otimizador Enterprise com as projections compartilhadas ja
     * calculadas pelo SupplyPlanService Community.
     *
     * <p>Implementacoes continuam stateless: todos os dados de rodada entram
     * por parametro e qualquer estado intermediario do modelo deve viver apenas
     * em variaveis locais.</p>
     */
    void executaSupplyPlan(
            SupplyPlan supplyPlan,
            @Nullable SupplyPlan supplyPlanParaProjecaoEstoqueInicial,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            boolean novoSupplyPlan,
            boolean consideraRequisicoesEtapaAnterior,
            boolean consideraOrdensProducaoPlanejadasEtapaAnterior,
            SupplyNetworkProjection supplyNetworkProjection,
            BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva,
            PoliticaEstoquesProjection politicaEstoquesProjection,
            MaterialProjection materialProjection,
            LocationProjection locationProjection);

}
