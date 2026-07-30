package com.opsfactor.community.capability.supplyplanning.service.spi;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import jakarta.annotation.Nullable;

/**
 * Ponto de extensao de alto nivel para motores de execucao de Supply Planning.
 *
 * <p>O Community possui implementacao heuristica dentro do fluxo principal de
 * SupplyPlanService. As implementacoes Enterprise de otimizador e process chain
 * devem implementar esta SPI para que o service Community consiga delegar sem
 * depender do codigo Enterprise em tempo de compilacao.</p>
 *
 * <p>Implementacoes devem ser stateless ou tratar estado apenas em variaveis
 * locais da chamada, pois execucoes de planejamento podem ocorrer em paralelo.</p>
 */
public interface SupplyPlanExecutionServiceSpi {

    /**
     * Remove ou reinicia artefatos especificos do modo de execucao antes de
     * reprocessar um Supply Plan existente.
     */
    void reiniciaSupplyPlanExistente(SupplyPlan supplyPlan);

    /**
     * Executa o motor ou orquestrador Enterprise especifico.
     *
     * <p>Otimizadores atomicos normalmente recebem esta chamada depois que o
     * {@code SupplyPlanService} preparou plano, calendario e dados base
     * compartilhados. Process chains sao diferentes: elas podem receber a
     * chamada antes das projections compartilhadas para expandir a cadeia e
     * chamar novamente o fluxo principal com perfis atomicos de heuristica ou
     * otimizacao. Mesmo nesse contrato antecipado, o header do Supply Plan ja
     * deve estar persistido e carregado com id funcional, pois todas as etapas
     * gravam artefatos sobre o mesmo plano fisico. Implementacoes devem
     * documentar explicitamente qual desses contratos seguem.</p>
     */
    void executaSupplyPlan(
            SupplyPlan supplyPlan,
            @Nullable SupplyPlan supplyPlanParaProjecaoEstoqueInicial,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            boolean novoSupplyPlan,
            boolean consideraRequisicoesEtapaAnterior,
            boolean consideraOrdensProducaoPlanejadasEtapaAnterior);

}
