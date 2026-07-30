package com.opsfactor.community.capability.supplyplanning.supplyplan.service;

import com.opsfactor.community.capability.supplyplanning.distributionplan.repository.DistributionPlanLegacyBaselineRequirement;
import com.opsfactor.community.capability.supplyplanning.distributionplan.repository.DistributionPlanItemRepository;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.repository.InventoryPlanLegacyBaselineRequirement;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.repository.InventoryPlanLinhaRepository;
import com.opsfactor.community.capability.supplyplanning.productionplan.repository.ProductionPlanLegacyBaselineRequirement;
import com.opsfactor.community.capability.supplyplanning.productionplan.repository.ProductionPlanLinhaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Gate somente-leitura das colunas baseline depreciadas de Supply Planning.
 *
 * <p>O runtime Community nao deve compor series novas a partir de valores
 * persistidos nas colunas antigas. Antes de materializar uma projection ou
 * exportar o plano, este gate consulta apenas uma amostra escalar e paginada
 * de cada serie. Nao ha DML, schema change, fetch join ou navegacao lazy.</p>
 */
@Service
public class SupplyPlanPersistedBaselinePreflight {

    /** Limita a mensagem sem ocultar que existem pendencias adicionais. */
    private static final int MAXIMUM_ISSUE_COUNT_PER_SERIES = 25;

    /** Consulta escalar dos resíduos baseline de Inventory Plan por envelope. */
    @Autowired
    private InventoryPlanLinhaRepository inventoryPlanLinhaRepository;

    /** Consulta escalar dos resíduos baseline de Distribution Plan por envelope. */
    @Autowired
    private DistributionPlanItemRepository distributionPlanItemRepository;

    /** Consulta escalar dos resíduos baseline de Production Plan por envelope. */
    @Autowired
    private ProductionPlanLinhaRepository productionPlanLinhaRepository;

    /**
     * Bloqueia a leitura de um Supply Plan quando qualquer serie ainda tem
     * valor baseline efetivo e informa chaves/colunas para a regularizacao.
     */
    @Transactional(readOnly = true)
    public void assertSupplyPlanReadyForCanonicalRuntime(Long supplyPlanId) {

        assertSupplyPlanIdsReadyForCanonicalRuntime(Set.of(requireSupplyPlanId(supplyPlanId)));

    }

    /**
     * Variante por envelope usada por exportacoes genericas. As seis consultas
     * (evidencia limitada e contagem por serie) continuam constantes por lote,
     * em vez de uma leitura por Supply Plan.
     */
    @Transactional(readOnly = true)
    public void assertSupplyPlanIdsReadyForCanonicalRuntime(
            Collection<Long> supplyPlanIds) {

        SupplyPlanPersistedBaselinePreflightReport preflightReport = inspectSupplyPlanIds(supplyPlanIds);
        if (!preflightReport.hasBlockingIssues()) {
            return;
        }

        throw new IllegalStateException(
                "Supply Plan persisted baseline cutover is required before canonical Community runtime. "
                        + "Populate the canonical Unconstrained/Constrained/Working fields, then clear or remove effective legacy baseline values. "
                        + "Affected series and keys: "
                        + preflightReport.getLimitedIssueList()
                        + ".");

    }

    /**
     * Produz uma fotografia read-only tipada para um envelope explicito de
     * Supply Plans antes de uma janela de cutover.
     *
     * <p>Cada serie faz uma leitura escalar limitada para manter o retorno
     * operacional navegavel e uma contagem independente para que o operador
     * saiba quando a evidencia foi truncada. As consultas permanecem em lote
     * por serie, sem carregar entidades, colecoes ou grafos de planejamento.</p>
     *
     * @param supplyPlanIds envelope explicito de Supply Plans a inspecionar.
     * @return contagens completas e amostras limitadas das pendencias por
     *         serie fisica.
     */
    @Transactional(readOnly = true)
    public SupplyPlanPersistedBaselinePreflightReport inspectSupplyPlanIds(
            Collection<Long> supplyPlanIds) {

        return inspectSupplyPlanIdsForSeries(
                supplyPlanIds,
                Set.of(
                        SupplyPlanPersistedBaselineSeries.INVENTORY,
                        SupplyPlanPersistedBaselineSeries.DISTRIBUTION,
                        SupplyPlanPersistedBaselineSeries.PRODUCTION));

    }

    /**
     * Produz um preview limitado às séries explicitamente aprovadas, sem
     * consultar ou reportar as outras séries do mesmo Supply Plan.
     *
     * <p>O gate de runtime e o executor APPLY continuam usando a variante
     * completa acima para garantir que não reste baseline em série omitida.
     * Esta variante existe somente para que a evidência PREVIEW espelhe o
     * arquivo administrativo que será submetido à aprovação.</p>
     */
    @Transactional(readOnly = true)
    public SupplyPlanPersistedBaselinePreflightReport inspectSupplyPlanIdsForSeries(
            Collection<Long> supplyPlanIds,
            Collection<SupplyPlanPersistedBaselineSeries>
                    selectedSeriesCollection) {

        Set<Long> normalizedSupplyPlanIds = normalizeSupplyPlanIds(supplyPlanIds);
        Set<SupplyPlanPersistedBaselineSeries> selectedSeriesSet =
                normalizeSelectedSeries(selectedSeriesCollection);
        if (normalizedSupplyPlanIds.isEmpty()) {
            return SupplyPlanPersistedBaselinePreflightReport.empty();
        }

        PageRequest issuePageRequest = PageRequest.of(0, MAXIMUM_ISSUE_COUNT_PER_SERIES);
        SupplyPlanPersistedBaselineSeriesReport inventoryPlanReport = selectedSeriesSet.contains(
                SupplyPlanPersistedBaselineSeries.INVENTORY)
                ? getInventoryPlanReport(normalizedSupplyPlanIds, issuePageRequest)
                : SupplyPlanPersistedBaselineSeriesReport.empty("Inventory Plan");
        SupplyPlanPersistedBaselineSeriesReport distributionPlanReport = selectedSeriesSet.contains(
                SupplyPlanPersistedBaselineSeries.DISTRIBUTION)
                ? getDistributionPlanReport(normalizedSupplyPlanIds, issuePageRequest)
                : SupplyPlanPersistedBaselineSeriesReport.empty("Distribution Plan");
        SupplyPlanPersistedBaselineSeriesReport productionPlanReport = selectedSeriesSet.contains(
                SupplyPlanPersistedBaselineSeries.PRODUCTION)
                ? getProductionPlanReport(normalizedSupplyPlanIds, issuePageRequest)
                : SupplyPlanPersistedBaselineSeriesReport.empty("Production Plan");

        return new SupplyPlanPersistedBaselinePreflightReport(
                normalizedSupplyPlanIds,
                inventoryPlanReport,
                distributionPlanReport,
                productionPlanReport);

    }

    /** Executa as duas consultas escalares necessárias apenas para Inventory. */
    private SupplyPlanPersistedBaselineSeriesReport getInventoryPlanReport(
            Set<Long> supplyPlanIdSet,
            PageRequest issuePageRequest) {

        List<SupplyPlanPersistedBaselineIssue> issueList = new ArrayList<>();
        appendInventoryIssues(
                issueList,
                inventoryPlanLinhaRepository.findLegacyBaselineRequirementsBySupplyPlanIdIn(
                        supplyPlanIdSet,
                        issuePageRequest));
        return getSeriesReport(
                "Inventory Plan",
                inventoryPlanLinhaRepository.countLegacyBaselineRequirementsBySupplyPlanIdIn(supplyPlanIdSet),
                issueList);

    }

    /** Executa as duas consultas escalares necessárias apenas para Distribution. */
    private SupplyPlanPersistedBaselineSeriesReport getDistributionPlanReport(
            Set<Long> supplyPlanIdSet,
            PageRequest issuePageRequest) {

        List<SupplyPlanPersistedBaselineIssue> issueList = new ArrayList<>();
        appendDistributionIssues(
                issueList,
                distributionPlanItemRepository.findLegacyBaselineRequirementsBySupplyPlanIdIn(
                        supplyPlanIdSet,
                        issuePageRequest));
        return getSeriesReport(
                "Distribution Plan",
                distributionPlanItemRepository.countLegacyBaselineRequirementsBySupplyPlanIdIn(supplyPlanIdSet),
                issueList);

    }

    /** Executa as duas consultas escalares necessárias apenas para Production. */
    private SupplyPlanPersistedBaselineSeriesReport getProductionPlanReport(
            Set<Long> supplyPlanIdSet,
            PageRequest issuePageRequest) {

        List<SupplyPlanPersistedBaselineIssue> issueList = new ArrayList<>();
        appendProductionIssues(
                issueList,
                productionPlanLinhaRepository.findLegacyBaselineRequirementsBySupplyPlanIdIn(
                        supplyPlanIdSet,
                        issuePageRequest));
        return getSeriesReport(
                "Production Plan",
                productionPlanLinhaRepository.countLegacyBaselineRequirementsBySupplyPlanIdIn(supplyPlanIdSet),
                issueList);

    }

    /** Impede preview sem série ou com enum nulo, que esconderia parte da seleção aprovada. */
    private Set<SupplyPlanPersistedBaselineSeries>
            normalizeSelectedSeries(
                    Collection<SupplyPlanPersistedBaselineSeries>
                            selectedSeriesCollection) {

        if (selectedSeriesCollection == null || selectedSeriesCollection.isEmpty()) {
            throw new IllegalArgumentException(
                    "Supply Plan persisted baseline preview requires at least one selected series.");
        }

        Set<SupplyPlanPersistedBaselineSeries> selectedSeriesSet =
                new LinkedHashSet<>();
        for (SupplyPlanPersistedBaselineSeries selectedSeries
                : selectedSeriesCollection) {
            if (selectedSeries == null) {
                throw new IllegalArgumentException(
                        "Supply Plan persisted baseline preview selected series cannot contain null.");
            }
            selectedSeriesSet.add(selectedSeries);
        }
        return selectedSeriesSet;

    }

    /** Monta a evidencia por serie sem transformar contagem maior em lista extensa. */
    private SupplyPlanPersistedBaselineSeriesReport getSeriesReport(
            String seriesName,
            long totalIssueCount,
            List<SupplyPlanPersistedBaselineIssue> limitedIssueList) {

        if (totalIssueCount < 0) {
            throw new IllegalStateException(
                    "Supply Plan persisted baseline preflight "
                            + seriesName
                            + " repository returned a negative issue count.");
        }

        return new SupplyPlanPersistedBaselineSeriesReport(
                seriesName,
                totalIssueCount,
                limitedIssueList,
                totalIssueCount > limitedIssueList.size());

    }

    private Set<Long> normalizeSupplyPlanIds(
            Collection<Long> supplyPlanIds) {

        if (supplyPlanIds == null) {
            throw new IllegalArgumentException(
                    "Supply Plan persisted baseline preflight requires a non-null Supply Plan id collection.");
        }

        Set<Long> normalizedSupplyPlanIds = new LinkedHashSet<>();
        for (Long supplyPlanId : supplyPlanIds) {
            normalizedSupplyPlanIds.add(requireSupplyPlanId(supplyPlanId));
        }
        return normalizedSupplyPlanIds;

    }

    private Long requireSupplyPlanId(
            Long supplyPlanId) {

        if (supplyPlanId == null || supplyPlanId <= 0) {
            throw new IllegalArgumentException(
                    "Supply Plan persisted baseline preflight requires a positive Supply Plan id.");
        }
        return supplyPlanId;

    }

    private void appendInventoryIssues(
            List<SupplyPlanPersistedBaselineIssue> issueList,
            List<InventoryPlanLegacyBaselineRequirement> requirementList) {

        requireRequirementList(requirementList, "Inventory Plan");
        for (InventoryPlanLegacyBaselineRequirement requirement : requirementList) {
            if (requirement == null) {
                throw new IllegalStateException(
                        "Supply Plan persisted baseline preflight Inventory Plan repository returned a null requirement.");
            }

            List<String> legacyFieldNameList = getEffectiveInventoryLegacyFieldNameList(requirement);
            if (!legacyFieldNameList.isEmpty()) {
                issueList.add(new SupplyPlanPersistedBaselineIssue(
                        "Inventory Plan",
                        "supplyPlan=" + getIdentifier(requirement.getSupplyPlanId())
                                + "/location=" + getIdentifier(requirement.getLocationId())
                                + "/material=" + getIdentifier(requirement.getMaterialId())
                                + "/referenceDate=" + getIdentifier(requirement.getReferenceDate()),
                        legacyFieldNameList));
            }
        }

    }

    private void appendDistributionIssues(
            List<SupplyPlanPersistedBaselineIssue> issueList,
            List<DistributionPlanLegacyBaselineRequirement> requirementList) {

        requireRequirementList(requirementList, "Distribution Plan");
        for (DistributionPlanLegacyBaselineRequirement requirement : requirementList) {
            if (requirement == null) {
                throw new IllegalStateException(
                        "Supply Plan persisted baseline preflight Distribution Plan repository returned a null requirement.");
            }

            List<String> legacyFieldNameList = getEffectiveDistributionLegacyFieldNameList(requirement);
            if (!legacyFieldNameList.isEmpty()) {
                issueList.add(new SupplyPlanPersistedBaselineIssue(
                        "Distribution Plan",
                        "supplyPlan=" + getIdentifier(requirement.getSupplyPlanId())
                                + "/origin=" + getIdentifier(requirement.getOriginLocationId())
                                + "/destination=" + getIdentifier(requirement.getDestinationLocationId())
                                + "/material=" + getIdentifier(requirement.getMaterialId())
                                + "/shippingDate=" + getIdentifier(requirement.getShippingDate())
                                + "/receivingDate=" + getIdentifier(requirement.getReceivingDate()),
                        legacyFieldNameList));
            }
        }

    }

    private void appendProductionIssues(
            List<SupplyPlanPersistedBaselineIssue> issueList,
            List<ProductionPlanLegacyBaselineRequirement> requirementList) {

        requireRequirementList(requirementList, "Production Plan");
        for (ProductionPlanLegacyBaselineRequirement requirement : requirementList) {
            if (requirement == null) {
                throw new IllegalStateException(
                        "Supply Plan persisted baseline preflight Production Plan repository returned a null requirement.");
            }

            List<String> legacyFieldNameList = getEffectiveProductionLegacyFieldNameList(requirement);
            if (!legacyFieldNameList.isEmpty()) {
                issueList.add(new SupplyPlanPersistedBaselineIssue(
                        "Production Plan",
                        "supplyPlan=" + getIdentifier(requirement.getSupplyPlanId())
                                + "/location=" + getIdentifier(requirement.getLocationId())
                                + "/outputMaterial=" + getIdentifier(requirement.getOutputMaterialId())
                                + "/productionVersion=" + getIdentifier(requirement.getProductionVersionId())
                                + "/routing=" + getIdentifier(requirement.getRoutingId())
                                + "/billOfMaterials=" + getIdentifier(requirement.getBillOfMaterialsId())
                                + "/referenceDate=" + getIdentifier(requirement.getReferenceDate()),
                        legacyFieldNameList));
            }
        }

    }

    private void requireRequirementList(
            List<?> requirementList,
            String seriesName) {

        if (requirementList == null) {
            throw new IllegalStateException(
                    "Supply Plan persisted baseline preflight "
                            + seriesName
                            + " repository returned null requirements.");
        }

    }

    private List<String> getEffectiveInventoryLegacyFieldNameList(
            InventoryPlanLegacyBaselineRequirement requirement) {

        List<String> legacyFieldNameList = new ArrayList<>();
        addIfEffective(legacyFieldNameList, "quantidadeEstoqueSegurancaBaseline", requirement.getSafetyStockBaselineUnconstrained());
        addIfEffective(legacyFieldNameList, "quantidadeEstoqueMaximoBaseline", requirement.getMaximumStockBaselineUnconstrained());
        addIfEffective(legacyFieldNameList, "quantidadeEstoqueSegurancaRestritoBaseline", requirement.getSafetyStockBaselineConstrained());
        addIfEffective(legacyFieldNameList, "quantidadeEstoqueMaximoRestritoBaseline", requirement.getMaximumStockBaselineConstrained());
        addIfEffective(legacyFieldNameList, "quantidadeEstoqueBaseline", requirement.getProjectedStockBaselineUnconstrained());
        addIfEffective(legacyFieldNameList, "quantidadeEstoqueRestritoBaseline", requirement.getProjectedStockBaselineConstrained());
        return legacyFieldNameList;

    }

    private List<String> getEffectiveDistributionLegacyFieldNameList(
            DistributionPlanLegacyBaselineRequirement requirement) {

        List<String> legacyFieldNameList = new ArrayList<>();
        addIfEffective(legacyFieldNameList, "quantidadeRequisicaoBaseline", requirement.getPlannedOrderBaselineUnconstrained());
        addIfEffective(legacyFieldNameList, "quantidadeRequisicaoBaselineAtendida", requirement.getPlannedOrderBaselineConstrained());
        addIfEffective(legacyFieldNameList, "quantidadePedidoBaseline", requirement.getFirmOrderBaselineUnconstrained());
        addIfEffective(legacyFieldNameList, "quantidadePedidoBaselineAtendido", requirement.getFirmOrderBaselineConstrained());
        return legacyFieldNameList;

    }

    private List<String> getEffectiveProductionLegacyFieldNameList(
            ProductionPlanLegacyBaselineRequirement requirement) {

        List<String> legacyFieldNameList = new ArrayList<>();
        addIfEffective(legacyFieldNameList, "quantidadeSugestaoProducaoBaseline", requirement.getPlannedProductionBaselineUnconstrained());
        addIfEffective(legacyFieldNameList, "quantidadeSugestaoProducaoBaselineAtendida", requirement.getPlannedProductionBaselineConstrained());
        addIfEffective(legacyFieldNameList, "quantidadeOrdemProducaoBaseline", requirement.getFirmProductionBaselineUnconstrained());
        addIfEffective(legacyFieldNameList, "quantidadeOrdemProducaoBaselineAtendida", requirement.getFirmProductionBaselineConstrained());
        return legacyFieldNameList;

    }

    private void addIfEffective(
            List<String> legacyFieldNameList,
            String legacyFieldName,
            Number legacyValue) {

        if (legacyValue != null && legacyValue.doubleValue() != 0.0d) {
            legacyFieldNameList.add(legacyFieldName);
        }

    }

    private String getIdentifier(
            Object identifier) {

        return identifier == null ? "<missing>" : identifier.toString();

    }

    /** Descreve uma pendencia sem vazar entidade JPA ao chamador. */
    public record SupplyPlanPersistedBaselineIssue(
            String series,
            String key,
            List<String> legacyFieldNameList) {

        public SupplyPlanPersistedBaselineIssue {

            legacyFieldNameList = List.copyOf(legacyFieldNameList);

        }

    }

    /**
     * Fotografia completa do envelope, com uma amostra limitada por serie.
     *
     * <p>O campo {@code truncated} de cada serie nunca reduz a contagem: ele
     * apenas informa que a lista de chaves exibida e uma amostra operacional,
     * e nao o universo de linhas que ainda bloqueiam o runtime.</p>
     */
    public record SupplyPlanPersistedBaselinePreflightReport(
            Set<Long> supplyPlanIdSet,
            SupplyPlanPersistedBaselineSeriesReport inventoryPlanReport,
            SupplyPlanPersistedBaselineSeriesReport distributionPlanReport,
            SupplyPlanPersistedBaselineSeriesReport productionPlanReport) {

        public SupplyPlanPersistedBaselinePreflightReport {

            supplyPlanIdSet = Set.copyOf(supplyPlanIdSet);
            if (inventoryPlanReport == null
                    || distributionPlanReport == null
                    || productionPlanReport == null) {
                throw new IllegalArgumentException(
                        "Supply Plan persisted baseline preflight requires a report for every series.");
            }

        }

        /** Retorna se qualquer serie ainda contem baseline efetivo. */
        public boolean hasBlockingIssues() {

            return inventoryPlanReport.hasIssues()
                    || distributionPlanReport.hasIssues()
                    || productionPlanReport.hasIssues();

        }

        /** Junta somente as amostras ja limitadas para preservar a mensagem historica do gate. */
        public List<SupplyPlanPersistedBaselineIssue> getLimitedIssueList() {

            List<SupplyPlanPersistedBaselineIssue> limitedIssueList = new ArrayList<>();
            limitedIssueList.addAll(inventoryPlanReport.limitedIssueList());
            limitedIssueList.addAll(distributionPlanReport.limitedIssueList());
            limitedIssueList.addAll(productionPlanReport.limitedIssueList());
            return List.copyOf(limitedIssueList);

        }

        /** Fotografia vazia sem consultas para um envelope explicitamente vazio. */
        private static SupplyPlanPersistedBaselinePreflightReport empty() {

            return new SupplyPlanPersistedBaselinePreflightReport(
                    Set.of(),
                    SupplyPlanPersistedBaselineSeriesReport.empty("Inventory Plan"),
                    SupplyPlanPersistedBaselineSeriesReport.empty("Distribution Plan"),
                    SupplyPlanPersistedBaselineSeriesReport.empty("Production Plan"));

        }

    }

    /** Contagem total e evidencia limitada de uma unica serie persistida. */
    public record SupplyPlanPersistedBaselineSeriesReport(
            String seriesName,
            long totalIssueCount,
            List<SupplyPlanPersistedBaselineIssue> limitedIssueList,
            boolean truncated) {

        public SupplyPlanPersistedBaselineSeriesReport {

            if (seriesName == null || seriesName.isBlank()) {
                throw new IllegalArgumentException(
                        "Supply Plan persisted baseline series name is required.");
            }
            if (totalIssueCount < 0) {
                throw new IllegalArgumentException(
                        "Supply Plan persisted baseline total issue count cannot be negative.");
            }
            limitedIssueList = List.copyOf(limitedIssueList);
            if (totalIssueCount < limitedIssueList.size()) {
                throw new IllegalArgumentException(
                        "Supply Plan persisted baseline total issue count cannot be smaller than its evidence list.");
            }
            if (truncated != (totalIssueCount > limitedIssueList.size())) {
                throw new IllegalArgumentException(
                        "Supply Plan persisted baseline truncation must match the total issue count.");
            }

        }

        /** A contagem escalar e a fonte de verdade para bloqueio da serie. */
        public boolean hasIssues() {

            return totalIssueCount > 0;

        }

        /** Construtor sem pendencias usado somente pelo envelope vazio read-only. */
        private static SupplyPlanPersistedBaselineSeriesReport empty(String seriesName) {

            return new SupplyPlanPersistedBaselineSeriesReport(seriesName, 0, List.of(), false);

        }

    }

}
