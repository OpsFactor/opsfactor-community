package com.opsfactor.community.capability.demandplanning.export;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlanItem;
import com.opsfactor.community.capability.demandplanning.demandplan.repository.DemandPlanItemRepository;
import com.opsfactor.community.capability.demandplanning.demandplan.repository.DemandPlanRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.platform.calendar.Calendario;
import jakarta.persistence.NoResultException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Extracao Community do Demand Plan detalhado nas oito colunas standard.
 *
 * <p>As linhas ja sao carregadas pelo repository com as dimensoes necessarias,
 * evitando navegacao lazy entidade a entidade durante a geracao do arquivo.</p>
 */
@Service
public class CommunityDemandPlanDetailedExportService implements DemandPlanDetailedExportService {

    @Autowired
    private DemandPlanRepository demandPlanRepository;

    @Autowired
    private DemandPlanItemRepository demandPlanItemRepository;

    @Autowired
    private ClusterEParametrosProjectionFactory clusterAndParametersProjectionFactory;

    /** Exporta a fotografia standard completa do plano. */
    @Override
    @Transactional(readOnly = true)
    public List<List<Object>> getFile(Long demandPlanId) {

        getDemandPlan(demandPlanId);
        return buildFile(demandPlanItemRepository.customFindSnapshotForDetailedExport(demandPlanId));

    }

    /** Exporta o bucket correspondente a data informada. */
    @Override
    @Transactional(readOnly = true)
    public List<List<Object>> getFileByPeriod(Long demandPlanId, LocalDate referenceDate) {

        if (referenceDate == null) {
            throw new IllegalArgumentException("Demand Plan detailed export reference date is required.");
        }
        DemandPlan demandPlan = getDemandPlan(demandPlanId);
        LocalDateTime periodReferenceDate = referenceDate.atStartOfDay();
        LocalDateTime initialReferenceDate = Calendario.getPrimeiraDataHorarioPeriodo(
                periodReferenceDate,
                demandPlan.getTamanhoBucket());
        LocalDateTime finalReferenceDate = Calendario.getUltimaDataHorarioPeriodo(
                periodReferenceDate,
                demandPlan.getTamanhoBucket());
        return buildFile(demandPlanItemRepository.customFindSnapshotForDetailedExport(
                demandPlanId,
                initialReferenceDate,
                finalReferenceDate));

    }

    /** Materializa cabecalhos e linhas sem alterar a ordem funcional do legado. */
    private List<List<Object>> buildFile(List<DemandPlanItem> demandPlanItems) {

        if (demandPlanItems == null) {
            throw new IllegalStateException("Demand Plan detailed export query returned null.");
        }
        ParametrosGlobais globalParameters = getRequiredGlobalParameters();
        List<List<Object>> file = new ArrayList<>();
        file.add(new ArrayList<>(List.of(
                "Editable (changing will result in the creation of another record in the database)",
                "Editable (changing will result in the creation of another record in the database)",
                "Editable (changing will result in the creation of another record in the database)",
                "Ignored on upload",
                "Editable",
                "Editable",
                "Editable",
                "Editable")));
        file.add(new ArrayList<>(List.of(
                "Location Id",
                "Product Id",
                "Date (represents the end of each period : month/week/day)",
                "Unit of Measure Id",
                "Baseline Quantity",
                "New Products Quantity",
                "Uplift Quantity",
                "Adjustment Quantity")));
        demandPlanItems.stream()
                .sorted(Comparator.comparing((DemandPlanItem item) -> item.getLocation().getId())
                        .thenComparing(item -> item.getProduto().getId())
                        .thenComparing(DemandPlanItem::getDataReferencia))
                .map(item -> toFileRow(item, globalParameters))
                .forEach(file::add);
        return file;

    }

    /** Converte uma linha persistida nas oito colunas canonicas. */
    private List<Object> toFileRow(DemandPlanItem demandPlanItem, ParametrosGlobais globalParameters) {

        UnidadeMedida unitOfMeasure = demandPlanItem.getUnidadeMedida(globalParameters);
        if (unitOfMeasure == null || unitOfMeasure.getId() == null || unitOfMeasure.getId().isBlank()) {
            throw new IllegalStateException("Demand Plan detailed export requires a UOM for Demand Plan line.");
        }
        return new ArrayList<>(List.of(
                demandPlanItem.getLocation().getId(),
                demandPlanItem.getProduto().getId(),
                demandPlanItem.getDataReferencia(),
                unitOfMeasure.getId(),
                demandPlanItem.getQuantidadeBaseline(),
                demandPlanItem.getQuantidadeItensNovos(),
                demandPlanItem.getQuantidadeUplift(),
                demandPlanItem.getQuantidadeAjusteDemanda()));

    }

    /** Resolve o agregado e falha explicitamente quando o id nao existe. */
    private DemandPlan getDemandPlan(Long demandPlanId) {

        if (demandPlanId == null) {
            throw new IllegalArgumentException("Demand Plan id is required.");
        }
        return demandPlanRepository.customFindByIdComPerfilExecucao(demandPlanId)
                .orElseThrow(() -> new NoResultException("Demand Plan Id not found"));

    }

    /** Obtem os parametros globais usados para resolver a UOM da linha. */
    private ParametrosGlobais getRequiredGlobalParameters() {

        ClusterEParametrosProjection clusterAndParametersProjection = clusterAndParametersProjectionFactory
                .getParametrosProjectionCompletoDeCache();
        if (clusterAndParametersProjection == null
                || clusterAndParametersProjection.getParametrosGlobais() == null) {
            throw new IllegalStateException(
                    "Demand Plan detailed export requires global parameters projection.");
        }
        return clusterAndParametersProjection.getParametrosGlobais();

    }

}
