package com.opsfactor.community.capability.supplyplanning.configuration.facade.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan.ModeloEstoqueTarget;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.Constantes.TipoQuantidadeValor;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * DTO do perfil de execucao de Supply Planning publicado pela edicao
 * Community.
 *
 * <p>O front-end compartilhado precisa conhecer campos Enterprise para exibir
 * bloqueios visuais e para nao quebrar payloads antigos. Por isso este DTO
 * ainda contem atributos que nao sao estado funcional Community. A fronteira de
 * seguranca fica no service de configuracao: qualquer campo Enterprise ativado
 * ou preenchido deve gerar RequiresEnterpriseVersionException antes de mapper,
 * repository ou persistencia.</p>
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PerfilExecucaoSupplyPlanDTO {

    /*
     * Este DTO é compartilhado com o front-end e, por isso, ainda carrega campos
     * Enterprise que não existem como estado persistido ou runtime na edição
     * Community. O service de configuração valida esses campos explicitamente e
     * lança RequiresEnterpriseVersionException quando algum deles chega
     * preenchido/ativado.
     */

    /** Identificador funcional do perfil de execucao de Supply Planning. */
    private String id;

    /** Descricao exibida no cadastro do perfil. */
    private String description;
    
    /**
     * Campo Enterprise: filtros persistidos de material nao existem no
     * Community.
     *
     * <p>O contrato publico usa material, alinhado ao restante da edicao
     * Community.</p>
     */
    private String materialFilterId;
    
    /** Campo compartilhado. Community aceita/exibe apenas HEURISTICO. */
    private PerfilExecucaoSupplyPlan.ModoExecucao executionModel;
    /** Campo Enterprise: AI optimizer nao participa do motor heuristico Community. */
    private PerfilExecucaoSupplyPlan.OtimizadorInteligenciaArtificial aiOptimizer;
    /** Campo Enterprise: sequenciamento tatico de linha requer o modelo CP-SAT privado. */
    private Boolean enableLineSequencing;
    /** Campo Enterprise: decisao Greenfield/Brownfield requer o modelo CP-SAT privado. */
    private Boolean enableGreenfieldBrownfield;
    /** Campo Enterprise recebido apenas para rejeicao defensiva na edicao Community. */
    private String optimizationModelType;
    
    /** Campos Enterprise: reconciliacao entre forecast e pedidos transacionais. */
    private PerfilExecucaoSupplyPlan.ModeloMajoracaoDemandaDireta customerOrdersAndForecastReconciliationModelForProjectedInventory;
    private PerfilExecucaoSupplyPlan.ModeloMajoracaoDemandaDireta customerOrdersAndForecastReconciliationModelForSafetyStock;
    private Integer customerOrderHorizonInDays;
    /** Campos Enterprise: coeficientes da funcao objetivo do modelo otimizado. */
    private Double demandPlanMetDemandImpactCoefficient;
    private Double customerOrderMetDemandImpactCoefficient;
    /** Campo Enterprise: priorização temporal na função objetivo otimizada. */
    private Boolean increaseObjectiveFunctionImpactInEarlierPeriods;
    /** Campo Enterprise: incremento aplicado ao primeiro período da função objetivo otimizada. */
    private Double maximumPercentageIncreaseObjectiveFunctionImpactAtFirstPeriod;
    /** Campo Enterprise: modelo de decaimento temporal da função objetivo otimizada. */
    private PerfilExecucaoSupplyPlan.ModeloDecaimentoImpactoTemporal objectiveFunctionTemporalImpactDecayModel;
    /** Campo Enterprise: fator multiplicativo do decaimento temporal exponencial. */
    private Double objectiveFunctionTemporalImpactExponentialDecayFactor;
    /** Campo Enterprise: piso do multiplicador temporal exponencial. */
    private Double objectiveFunctionTemporalImpactMinimumMultiplier;
    /** Campos Enterprise: modelos configuraveis de priorizacao. */
    private String customerDemandPrioritizationModelId;
    private String safetyStockPrioritizationModelId;
    
    private Boolean generatePlannedInboundOrders;
    private Boolean generatePlannedProductionOrders;
    /** usado para heurístico : 
     * se requisicoes inbound são criadas mas ordens de producao planejadas não,
     * quando true faz com que materiais que poderiam ser produzidos não sejam repostos via requisição
     */
    private Boolean generatePlannedInboundOrdersWhenProductionIsViable;
    
    /** Se true, o cálculo será via DRP independente do modelo de reposição ser kanban,
     * drp ou ponto de ressuprimento. Community usa o fluxo heurístico padrão de Supply Planning. */
    private Boolean alwaysUseDrp;
    
    private Integer planHorizonInDays;

    /**
     * Community exige que materiais/locations MTO continuem consumindo o Demand
     * Plan como fonte futura. O comportamento fully make-to-order, sem forecast
     * e apoiado em carteira/pedidos, pertence ao Enterprise.
     */
    private Boolean considerForecastForMto;
    
    private Boolean automaticallyRunConstrainedPlan;
    
    /** se true, o plano de transferências será arredondado para cima p/ atender ao lote mínimo e múltiplo */
    private Boolean roundRequisitionsByMoqAndLotSize;
    /** se true, o arredondamento de transferências/compras em múltiplos vale para todos os períodos de expedição */
    private Boolean roundRequisitionsByMoqAndLotSizeForAllExpeditionPeriods;
    /** número de períodos iniciais de expedição em que o arredondamento de transferências/compras será inteiro */
    private Integer expeditionPeriodsToRoundRequisitionsByMoqAndLotSize;
    /** Campo Enterprise: arredondamento otimizado de compras planejadas. */
    private Boolean roundPlannedPurchaseOrdersByMinimumLotSize;
    /** Campo Enterprise: alocacao de transferencias em frotas/veiculos. */
    private Boolean allocateTransfersInFleets;
    /** se true, o plano de produção será arredondado para cima p/ atender ao lote mínimo e múltiplo de produção */
    private Boolean roundProductionByMoqAndLotSize;
    /** se true, o arredondamento de produção em múltiplos vale para todos os períodos do horizonte */
    private Boolean roundProductionByMoqAndLotSizeForAllPeriods;
    /** número de períodos iniciais do horizonte em que o arredondamento de produção será inteiro */
    private Integer periodsToRoundProductionByMoqAndLotSize;
    
    /** Campos Enterprise: pedidos transacionais nao existem como fonte de demanda no Community. */
    private Boolean considerSelloutOrdersBacklog;
    @JsonProperty("considerSelloutOrdersFuture")
    private Boolean considerSelloutOrdersFuture;
    private Boolean considerSellinOrdersBacklog;
    @JsonProperty("considerSellinOrdersFuture")
    private Boolean considerSellinOrdersFuture;
    private Boolean considerTransferOrdersBacklog;
    @JsonProperty("considerTransferOrdersFuture")
    private Boolean considerTransferOrdersFuture;
    private Boolean considerPurchaseOrdersBacklog;
    @JsonProperty("considerPurchaseOrdersFuture")
    private Boolean considerPurchaseOrdersFuture;
    private Boolean considerProductionOrdersBacklog;
    @JsonProperty("considerProductionOrdersFuture")
    private Boolean considerProductionOrdersFuture;
    
    /** Campo Enterprise: carregamento de backlog entre periodos. */
    private Boolean allowBacklogCarryOver;

    /** Campo Enterprise: modelo fully make-to-order. */
    private Boolean forceMakeToOrderModel;

    /** Campo Enterprise: recuperacao de gap de demanda via vendas passadas. */
    private Boolean enableDemandCatchUpFromPastSellout;

    /** Ativa nivelamento de capacidade do plano irrestrito no modo heurístico. */
    private Boolean heuristicUnconstrainedPlanCapacityLeveling;

    private Boolean considerInitialStock;
    /** Campo Enterprise: salva variaveis/restricoes do otimizador. */
    private Boolean saveOptimizerVariablesAndConstraints;

    /** se true, o plano é armazenado e resgatado para os relatórios / planning books. 
     * se false, apenas o estoque inicial e o estoque em trânsito são salvos. o estoque projetado sempre será recalculado */
    private Boolean saveInventoryPlan;
    
    /** Campo Enterprise: diagnostico/backtracking de restricoes. */
    private Boolean saveConstraintBacktracking;
    
    /** Community sempre processa todas as locations ativas do escopo base; seleção por location é Enterprise. */
    private Boolean executeSupplyPlanForAllLocations;
    
    private ModeloEstoqueTarget targetStockModel;
    
    /*
     * Campos transicionais do plano irrestrito heuristico. Lead time/margem do
     * modelo otimizado continuam Enterprise, mas a decisao final sobre expor ou
     * esconder o plano irrestrito simples no Community segue documentada no
     * recorte funcional.
     */
    private Boolean generateUnconstrainedPlan;
    private Boolean ignoreProductionConstraintsForUnconstrainedPlan;
    private Boolean ignoreStorageConstraintsForUnconstrainedPlan;
    private Boolean ignoreOutboundConstraintsForUnconstrainedPlan;
    private Boolean ignoreInboundConstraintsForUnconstrainedPlan;
    private Boolean ignoreLeadTimeConstraintsForUnconstrainedPlan;
    private Double maximumTransferCostImpactForLeadTimeReduction;
    private Double maximumMaterialObjectiveValueImpactForLeadTimeReduction;
    private Boolean ignoreMarginConstraintsForUnconstrainedPlan;
    private Double metDemandObjectiveValueIncreasePercentage;
    private Double minimumMetDemandObjectiveValue;

    /** Campo Enterprise: geração de P&L e cost-to-serve. */
    private Boolean generatePL;
    /** Campo Enterprise: retroação econômica de faturamento/impostos/COGS via lista técnica. */
    private Boolean associateSalesToInputMaterialsInRetroaction;

    /**
     * Preferencia persistida do perfil para a futura geracao Enterprise de
     * Profit/Loss. Neste slice ela nao dispara task nem calculo.
     */
    private Boolean generateProfitLoss;

    /**
     * Define se o futuro motor Enterprise pode retroagir impacto de vendas
     * para materiais de entrada atraves da BOM. Quando omitido, o perfil usa
     * o default seguro e compativel `true`.
     */
    private Boolean allowSalesProfitLossBomRetroaction;

    /** Campo Enterprise: base quantitativa/valor para modelos otimizados e econômicos. */
    private TipoQuantidadeValor salesMeasure;
    private PerfilExecucaoSupplyPlan.ModoApuracaoImpostos taxApportionmentModel;
    
    /** Campos Enterprise: unidade/valor de referencia usados pelo modelo otimizado. */
    private String optimizationUom;
    private Double unitValueByOptimizationUom;

    /** Campos operacionais: propagacao/consolidacao de demanda para o heuristico Community. */
    private Boolean consolidateClientDemand;
    private PerfilExecucaoSupplyPlan.ModoPropagacaoDemanda demandConsolidationMode;

    /** Campos Enterprise: estoques em clientes e pontos de transbordo. */
    private Boolean allowStockAtClients;
    private Boolean allowStockAtTransshipmentPoints;
    
    // Campos Enterprise de greenfield/brownfield.
    private Boolean considerBudgetForGreenfieldLocationActivation;
    private Double greenfieldLocationActivationBudget;
        
    // Campos Enterprise de custos, preços, impostos e working capital.
    private Boolean considerLocationFixedCost;
    private Boolean considerProductionResourceFixedCost;
    private Boolean considerStorageCost;
    private Boolean considerInboundOutboundCosts;
    private Boolean considerTransferCost;
    private Boolean considerTaxesInTransportationLines;
    private Boolean considerProductionCost;
    private Boolean considerSupplierPrices;

    /** Campo Enterprise: estimativa de COGS para working capital e política de estoques otimizada. */
    private Boolean estimateUnitCogsForWorkingCapitalAndInventoryPolicy;

    /** Campo Enterprise: custo de não atendimento diferenciado por cliente. */
    private Boolean considerUnmetClientOrderImpact;
    /** Community permite somente restricoes produtivas operacionais; restricoes logisticas sao Enterprise. */
    private Boolean considerProductionConstraints;
    private Boolean considerStorageConstraints;
    private Boolean considerInboundConstraints;
    private Boolean considerOutboundConstraints;

    /** Community permite somente horas totais por dia; quantidade/turnos sao Enterprise. */
    private PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva productiveCapacityType;
    /** Campo Enterprise: nivel de capacidade logistica. */
    private PerfilExecucaoSupplyPlan.TipoCapacidadeLogistica logisticsCapacityLevel;

    /**
     * Campo Enterprise: capacidade produtiva detalhada considerando set-ups e
     * tempo de manutencao. No Community a capacidade produtiva e operacional e
     * calculada em horas totais por dia, sem line scheduling nem cadastro de
     * turnos/manutencao.
     */
    private Boolean generateProductionScheduling;
    
    /*
     * Fair share quantitativo Community. O heuristico simples sempre aplica
     * reducao proporcional da demanda direta; este campo deve ser retornado
     * true para a UI e normalizado como true ao salvar.
     */
    private Boolean directDemandFairShare;
    // Campo Enterprise: fair share otimizado de safety stock.
    private Boolean safetyStockFairShare;
    
    private Integer numberSegmentsDirectDemandGapLinearization;
    private Integer numberSegmentsSafetyStockGapLinearization;


    // impacto máximo fair share demanda direta : % impacto adicional sobre demanda potencial
    private Double fairShareMaximumPercentagePenaltyUnmetDemand; // ex. 0.1%
    // Campo Enterprise: impacto máximo de fair share sobre gap de estoque de segurança.
    private Double fairShareMaximumPercentagePenaltySafetyStockGap; // ex. 0.01%
    
    // Campo Enterprise: custo percentual do gap de estoque de segurança.
    private Double safetyStockGapPercentualCost;
    
    private Double workingCapitalPercentualCost; 
    
    private Long maximumOptimizerExecutionTime;
    
    private Double entityTabuRatio;
    private Integer acceptedCountLimit;
    
    // parâmetros Enterprise para soft targets e travas manuais de variaveis.
    private Double softTargetMaximumPercentPenalty;
    private Double softTargetDeviationAmplitudeAsTargetPercent;
    private Integer softTargetDeviationLinearizationNumberSegments;
    private Double firmOrderCogsIncentivePercentage;

    // Campos Enterprise de line scheduling detalhado.
    private Boolean generateDetailedPlan;
    private Constantes.TamanhoBucket detailedPlanBucketSize;
    private Integer detailedPlanPlanningHorizonInBuckets;
    private Boolean roundProductionAndSetupsToDetailedPlanBucket;

    /**
     * se true, a otimização será rodada para cada valor de aging possível (em períodos)
     * tanto o cálculo do writeoff quanto o tempo de processo dependem dessa opção
     */
    private Boolean segmentInventoryByBatch;
    /** se true, lotes mais antigos recebem maior impacto de working capital */
    private Boolean increaseWorkingCapitalImpactForOlderBatches;
    /** incremento percentual máximo aplicado ao lote mais antigo modelado */
    private Double maximumPercentageIncreaseWorkingCapitalImpactForOldestBatch;

    /** Indica qual dos planos (restrito ou irrestrito) será copiado como o plano de trabalho */
    private Constantes.TipoPlano planTypeForWorkVersion;


    /**
     * Politicas de estoque associadas ao perfil. A politica operacional e o
     * safety stock existem no Community; apenas a otimizacao automatica dessas
     * politicas pertence ao Enterprise.
     */
    private Set<String> inventoryPolicyIdSet = new HashSet<>();

    /**
     * Campo Enterprise: curvas de split temporal da demanda. O Community usa o
     * Demand Plan como fonte unica e nao permite selecionar infraestrutura de
     * curvas temporais no perfil.
     */
    private Set<String> temporalSplitCurveIdSet = new HashSet<>();

    // Campos Enterprise de penalizacao economica do nao-atendimento.
    private Boolean penalizeUnmetDemand;
    private Double unmetDemandPenalizationAsFractionOfGrossSales;
    private Double unmetDemandPenalizationAsUnitImpact;
    private String unmetDemandPenalizationAsUnitImpactUomId;

    // Campos Enterprise de curvas de custo logistico.
    private Long logisticsCostCurvesId;
    private Boolean applyFreightCostCurves;
    private Boolean applyLocationCostCurves;

}
