package com.opsfactor.community.capability.demandplanning.configuration.facade.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.platform.utility.Constantes;

import jakarta.annotation.Nullable;

/**
 * Contrato REST do perfil de execucao de Demand Planning.
 *
 * <p>No Community o perfil permite apenas historico sell-out, bucket/horizonte,
 * janela de edicao e unidade padrao. Campos de MAPE, auto-fit e regression
 * tree ficam no DTO somente para rejeicao defensiva de payloads
 * Enterprise/legados; eles nao sao populados pelo mapper Community, nao
 * aparecem no OpenAPI Community e devem chegar nulos ao salvar.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PerfilExecucaoDemandPlanDTO {

    /**
     * Identificador funcional do perfil de execucao.
     */
    public String id;

    /**
     * Descricao exibida ao usuario.
     */
    public String description;

    /**
     * Tipo de documento historico aceito pelo perfil.
     *
     * <p>No Community o unico valor funcionalmente aceito e `SELLOUT`; outros
     * valores permanecem no enum compartilhado apenas para bloqueio visual ou
     * rejeicao defensiva no service.</p>
     */
    public Constantes.TipoDocumentoVenda historicalSalesDocumentType;

    /**
     * Granularidade temporal do calendario de Demand Planning.
     */
    public Constantes.TamanhoBucket bucketSize;

    /**
     * Numero de periodos futuros planejados no perfil.
     */
    public Integer planningHorizonInPeriods;

    /**
     * Indica se a edicao manual do plano deve ficar restrita a uma janela de
     * periodos.
     */
    public Boolean constrainPlanEditPeriods;

    /**
     * Primeiro periodo relativo editavel quando a janela de edicao esta ativa.
     */
    public @Nullable Integer initialPlanEditPeriod;

    /**
     * Ultimo periodo relativo editavel quando a janela de edicao esta ativa.
     */
    public @Nullable Integer finalPlanEditPeriod;

    /**
     * Unidade padrao usada para converter/exibir historico e forecast de
     * demanda.
     */
    public String defaultDemandPlanningUomId;

    /*
     * Campos Enterprise mantidos temporariamente no DTO Community apenas para
     * que a borda REST consiga rejeitar payloads legados/Enterprise com erro claro.
     * O mapper Community nao popula esses campos nas respostas e o service
     * exige que todos sejam nulos ao salvar um perfil.
     */
    public String mapeMaterialAggregationLevelId;
    public String mapeLocationAggregationLevelId;

    /**
     * Configuracao default de auto-fit Enterprise. Deve permanecer nula no
     * Community.
     */
    public Long defaultAutoTunedDemandPlanConfigurationId;

    /**
     * Tipo de execucao de auto-fit Enterprise. Deve permanecer nulo no
     * Community.
     */
    public String autofitModelType;

    /**
     * Funcao objetivo de auto-fit Enterprise, sempre rejeitada pelo Community.
     */
    public String modelAutofitObjectiveFunction;

    /**
     * Numero de periodos de avaliacao do auto-fit Enterprise.
     */
    public Integer modelAutofitNumberOfPeriodsForAccuracyEvaluation;

    /**
     * Lag de avaliacao do auto-fit Enterprise.
     */
    public Integer modelAutofitEvaluationLagInPeriods;

    /**
     * Funcao objetivo da arvore de regressao Enterprise.
     */
    public String regressionTreeObjectiveFunction;

    /**
     * Numero de dimensoes candidatas avaliadas na arvore de regressao
     * Enterprise.
     */
    public Integer numberOfDimensionsUsedForCandidateSplits;

    /**
     * Numero de splits candidatos por dimensao na arvore de regressao
     * Enterprise.
     */
    public Integer numberOfCandidateSplitsByDimension;

    /**
     * Profundidade adicional maxima apos o ultimo split confirmado na arvore de
     * regressao Enterprise.
     */
    public Integer maxDepthAfterLastConfirmedSplit;

    /**
     * Criterio de parada da expansao da arvore de regressao Enterprise.
     */
    public Double minimumPercentErrorReductionForNewSplits;

    /** Numero de periodos considerados para cross-validation na poda da arvore de regressao Enterprise. */
    public Integer numberOfPeriodsForRegressionTreePruning;

}
