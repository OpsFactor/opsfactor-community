package com.opsfactor.community.platform.bi.facade;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjectionFactory;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjectionFactory;
import com.opsfactor.community.capability.masterdata.production.productionresource.projection.BIProjectionCapacidadeProdutiva;
import com.opsfactor.community.capability.masterdata.production.productionresource.projection.BIProjectionCapacidadeProdutivaFactory;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewResourceDetailDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewResourceDetailResponseDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewResourceDetailSelectionDTO;
import com.opsfactor.community.capability.supplyplanning.service.SupplyPlanService;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abre uma única célula do Production Overview sem reconstruir seu agregado.
 *
 * <p>A seleção fica limitada a plano, recurso e período da rota. A projection
 * Community é carregada uma vez para a location do recurso e recebe somente
 * as linhas de output de produção. Não há tabela auxiliar, join sob demanda,
 * agregação de materiais nem UOM escolhida globalmente pelo usuário.</p>
 */
@Service
public class CommunityProductionOverviewResourceDetailService {

    /** Carrega parâmetros globais necessários para resolver o calendário do plano. */
    @Autowired
    private ClusterEParametrosProjectionFactory clusterAndParametersProjectionFactory;

    /** Resolve recursos e estrutura produtiva da malha Community em memória. */
    @Autowired
    private SupplyNetworkProjectionFactory supplyNetworkProjectionFactory;

    /** Materializa capacidade por recurso e período do plano selecionado. */
    @Autowired
    private BIProjectionCapacidadeProdutivaFactory productionCapacityProjectionFactory;

    /** Constrói o snapshot de Supply Plan limitado à location do recurso. */
    @Autowired
    private SupplyPlanProjectionFactory supplyPlanProjectionFactory;

    /** Obtém o plano persistido e seu perfil de execução. */
    @Autowired
    private SupplyPlanService supplyPlanService;

    /** Abre o detalhe sem filtro adicional, como o GET histórico. */
    public CommunityProductionOverviewResourceDetailResponseDTO getResourceDetail(
            Long supplyPlanId,
            String productionResourceId,
            Integer periodIndex) {

        return getResourceDetail(supplyPlanId, productionResourceId, periodIndex, null);

    }

    /**
     * Abre o detalhe preservando exclusivamente o filtro de características
     * de material informado pela tela. UOM, location e plano presentes no
     * corpo não são autoridades: cada um já é definido pela rota ou pela linha.
     */
    public CommunityProductionOverviewResourceDetailResponseDTO getResourceDetail(
            Long supplyPlanId,
            String productionResourceId,
            Integer periodIndex,
            CommunityProductionOverviewResourceDetailSelectionDTO selectionDTO) {

        ClusterEParametrosProjection clusterAndParametersProjection =
                clusterAndParametersProjectionFactory.getParametrosProjectionCompletoDeCache();
        SupplyNetworkProjection supplyNetworkProjection =
                supplyNetworkProjectionFactory.getSupplyNetworkProjectionCompletoDeCache();
        SupplyPlan supplyPlan = supplyPlanService.getSupplyPlanDeId(supplyPlanId);
        PerfilExecucaoSupplyPlan supplyExecutionProfile = supplyPlan.getPerfilExecucaoSupplyPlan();
        PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva productionCapacityType =
                supplyExecutionProfile.getTipoCapacidadeProdutiva();
        RecursoProdutivo productionResource = supplyNetworkProjection
                .getRecursoProdutivoPersistido(productionResourceId);
        /* A rota referencia um recurso persistido: continuar sem ele produziria
         * um detalhe inconsistente, portanto a seleção inválida falha claramente. */
        if (productionResource == null) {
            throw new IllegalArgumentException("Production resource not found: " + productionResourceId);
        }
        Location location = productionResource.getLocation();
        Calendario supplyPlanCalendar = supplyPlan.getCalendarioDoSupplyPlan(
                clusterAndParametersProjection.getParametrosGlobais());
        BIProjectionCapacidadeProdutiva productionCapacityProjection =
                productionCapacityProjectionFactory.getBIProjectionCapacidadeProdutivaDeSupplyPlan(
                        supplyPlan,
                        supplyPlanCalendar);

        validatePeriod(periodIndex, supplyPlanCalendar);
        MaterialProjection materialProjection = getMaterialProjection(
                selectionDTO,
                clusterAndParametersProjection);
        SupplyPlanningProjection supplyPlanningProjection = supplyPlanProjectionFactory
                .getSupplyPlanningProjectionVazio(
                        supplyPlan,
                        supplyExecutionProfile,
                        location,
                        supplyNetworkProjection,
                        null,
                        materialProjection,
                        LocationProjectionFactory.getLocationProjectionCompleto(
                                clusterAndParametersProjection));
        supplyPlanProjectionFactory.populaSupplyPlanningProjectionComProductionPlanOutput(
                supplyPlanningProjection);

        CommunityProductionOverviewResourceDetailResponseDTO response = getResponseHeader(
                supplyPlan,
                location,
                productionResource,
                periodIndex,
                supplyPlanCalendar,
                productionCapacityType,
                productionCapacityProjection,
                clusterAndParametersProjection);
        response.rows = supplyPlanningProjection.getProductionPlanLinhaOutput(periodIndex, productionResource)
                .stream()
                .filter(line -> materialProjection.isMaterialFiltradoEAtivoNaLocation(
                        line.getMaterialOutput(),
                        line.getLocation()))
                .map(line -> getDetailLine(
                        line,
                        productionResource,
                        supplyPlan,
                        periodIndex,
                        productionCapacityType,
                        supplyNetworkProjection,
                        clusterAndParametersProjection))
                .filter(this::hasAnyPublishedMeasure)
                .sorted(Comparator
                        .comparing((CommunityProductionOverviewResourceDetailDTO dto) -> dto.outputMaterialId)
                        .thenComparing(dto -> dto.routingId)
                        .thenComparing(dto -> dto.billOfMaterialsId))
                .collect(Collectors.toList());
        return response;

    }

    /** Valida a coluna antes de abrir a projection para não publicar período fora do plano. */
    private void validatePeriod(Integer periodIndex, Calendario supplyPlanCalendar) {

        if (periodIndex == null
                || periodIndex < supplyPlanCalendar.getPosicaoPeriodoPresente()
                || periodIndex > supplyPlanCalendar.getPosicaoPeriodoFinalFuturo()) {
            throw new IllegalArgumentException("Invalid Production Overview period index: " + periodIndex);
        }

    }

    /** O corpo do POST aplica apenas o filtro de características de material do legado. */
    private MaterialProjection getMaterialProjection(
            CommunityProductionOverviewResourceDetailSelectionDTO selectionDTO,
            ClusterEParametrosProjection clusterAndParametersProjection) {

        if (selectionDTO == null) {
            return MaterialProjectionFactory.getProjectionByMaterialCharacteristicValues(
                    null,
                    clusterAndParametersProjection,
                    false);
        }
        return MaterialProjectionFactory.getProjectionByMaterialCharacteristicValues(
                selectionDTO.valuesByMaterialCharacteristicId,
                clusterAndParametersProjection,
                false);

    }

    /** Cria o cabeçalho da célula usando a mesma projection de capacidade Community. */
    private CommunityProductionOverviewResourceDetailResponseDTO getResponseHeader(
            SupplyPlan supplyPlan,
            Location location,
            RecursoProdutivo productionResource,
            Integer periodIndex,
            Calendario supplyPlanCalendar,
            PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva productionCapacityType,
            BIProjectionCapacidadeProdutiva productionCapacityProjection,
            ClusterEParametrosProjection clusterAndParametersProjection) {

        CommunityProductionOverviewResourceDetailResponseDTO response =
                new CommunityProductionOverviewResourceDetailResponseDTO();
        response.supplyPlanId = supplyPlan.getId();
        response.locationId = location.getId();
        response.locationDescription = location.getDescricao();
        response.productionResourceId = productionResource.getId();
        response.productionResourceDescription = productionResource.getDescricao();
        response.periodIndex = periodIndex;
        response.plannedDate = supplyPlanCalendar.getListaDatasHorarios().get(periodIndex);
        response.resourceCapacityUnitOfMeasureId = getResourceCapacityUnitOfMeasureId(
                productionResource,
                productionCapacityType,
                clusterAndParametersProjection);
        response.availableCapacityInHoursOrQuantity = productionCapacityProjection
                .getCapacidadeEmQuantidadeOuHorasEmPosicaoPeriodo(
                        periodIndex,
                        productionResource,
                        BIProjectionCapacidadeProdutiva.MasterOrPlanningData.MASTER_DATA);
        return response;

    }

    /** Constrói uma linha de produção individual, sem soma entre materiais. */
    private CommunityProductionOverviewResourceDetailDTO getDetailLine(
            ProductionPlanLinha productionPlanLine,
            RecursoProdutivo productionResource,
            SupplyPlan supplyPlan,
            Integer periodIndex,
            PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva productionCapacityType,
            SupplyNetworkProjection supplyNetworkProjection,
            ClusterEParametrosProjection clusterAndParametersProjection) {

        Produto outputMaterial = productionPlanLine.getMaterialOutput();
        Roteiro routing = productionPlanLine.getRoteiro();
        ListaTecnica billOfMaterials = productionPlanLine.getListaTecnica();
        VersaoProducao productionVersion = productionPlanLine.getVersaoProducaoCadastrada();
        UnidadeMedida outputUnitOfMeasure = getOutputUnitOfMeasure(
                productionPlanLine,
                productionResource,
                supplyNetworkProjection);

        CommunityProductionOverviewResourceDetailDTO detail =
                new CommunityProductionOverviewResourceDetailDTO();
        detail.supplyPlanId = supplyPlan.getId();
        detail.locationId = productionPlanLine.getLocation().getId();
        detail.locationDescription = productionPlanLine.getLocation().getDescricao();
        detail.productionResourceId = productionResource.getId();
        detail.productionResourceDescription = productionResource.getDescricao();
        detail.periodIndex = periodIndex;
        detail.plannedDate = productionPlanLine.getDataReferencia();
        detail.outputMaterialId = outputMaterial.getId();
        detail.outputMaterialDescription = outputMaterial.getDescricao();
        detail.productionVersionId = productionVersion.isVersaoProducaoInexistente()
                ? null
                : productionVersion.getId();
        detail.routingId = routing.getId();
        detail.routingDescription = routing.getDescricao();
        detail.billOfMaterialsId = billOfMaterials.getId();
        detail.billOfMaterialsDescription = billOfMaterials.getDescricao();
        detail.resourceCapacityUnitOfMeasureId = getResourceCapacityUnitOfMeasureId(
                productionResource,
                productionCapacityType,
                clusterAndParametersProjection);
        detail.unitOfMeasureId = outputUnitOfMeasure.getId();
        detail.unconstrainedHours = getResourceConsumption(
                productionPlanLine,
                productionResource,
                Constantes.TipoPlano.PLANO_IRRESTRITO,
                productionCapacityType,
                supplyNetworkProjection);
        detail.constrainedHours = getResourceConsumption(
                productionPlanLine,
                productionResource,
                Constantes.TipoPlano.PLANO_RESTRITO,
                productionCapacityType,
                supplyNetworkProjection);
        detail.workPlanHours = getResourceConsumption(
                productionPlanLine,
                productionResource,
                Constantes.TipoPlano.PLANO_TRABALHO,
                productionCapacityType,
                supplyNetworkProjection);
        detail.unconstrainedQuantity = getProductionQuantity(
                productionPlanLine,
                Constantes.TipoPlano.PLANO_IRRESTRITO,
                outputUnitOfMeasure,
                supplyNetworkProjection);
        detail.constrainedQuantity = getProductionQuantity(
                productionPlanLine,
                Constantes.TipoPlano.PLANO_RESTRITO,
                outputUnitOfMeasure,
                supplyNetworkProjection);
        detail.workPlanQuantity = getProductionQuantity(
                productionPlanLine,
                Constantes.TipoPlano.PLANO_TRABALHO,
                outputUnitOfMeasure,
                supplyNetworkProjection);
        detail.throughputQuantityPerHour = getThroughputQuantityPerHour(
                productionPlanLine,
                productionResource,
                outputUnitOfMeasure,
                supplyNetworkProjection);
        return detail;

    }

    /** A UOM pertence ao cabeçalho do roteiro e vale para todas as suas operações. */
    private UnidadeMedida getOutputUnitOfMeasure(
            ProductionPlanLinha productionPlanLine,
            RecursoProdutivo productionResource,
            SupplyNetworkProjection supplyNetworkProjection) {

        Roteiro routing = supplyNetworkProjection.getRoteiroFromId(
                productionPlanLine.getRoteiro().getId()).get();
        boolean resourceBelongsToRouting = supplyNetworkProjection
                .getOperacoesRoteiro(routing).stream()
                .filter(operation -> operation.getRecursoProdutivo().equals(productionResource))
                .findAny()
                .isPresent();
        if (!resourceBelongsToRouting) {
            throw new IllegalStateException("Production resource " + productionResource.getId()
                    + " not found in routing " + routing.getId());
        }
        return routing.getUnidadeMedidaQuantidadeBase(
                supplyNetworkProjection.getClusterEParametrosProjection().getParametrosGlobais());

    }

    /** Lê o valor da própria linha na UOM operacional publicada por ela. */
    private Double getProductionQuantity(
            ProductionPlanLinha productionPlanLine,
            Constantes.TipoPlano planType,
            UnidadeMedida outputUnitOfMeasure,
            SupplyNetworkProjection supplyNetworkProjection) {

        return productionPlanLine.getQuantidade(
                planType,
                Constantes.FirmePlanejado.TOTAL,
                outputUnitOfMeasure,
                supplyNetworkProjection.getConversaoUnidadeMedidaProjection());

    }

    /** Lê consumo de capacidade existente da linha, sem recalcular capacidade. */
    private Double getResourceConsumption(
            ProductionPlanLinha productionPlanLine,
            RecursoProdutivo productionResource,
            Constantes.TipoPlano planType,
            PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva productionCapacityType,
            SupplyNetworkProjection supplyNetworkProjection) {

        Map<RecursoProdutivo, Double> consumptionByProductionResource = productionPlanLine
                .getCapacidadeConsumidaPorRecursoProdutivoEmHorasOuQuantidade(
                        planType,
                        Constantes.FirmePlanejado.TOTAL,
                        productionCapacityType,
                        supplyNetworkProjection);
        return consumptionByProductionResource.getOrDefault(productionResource, 0.0d);

    }

    /**
     * Mantém o critério legado de produtividade: irrestrito, depois work plan
     * e então restrito. O cálculo ocorre somente sobre a própria linha.
     */
    private Double getThroughputQuantityPerHour(
            ProductionPlanLinha productionPlanLine,
            RecursoProdutivo productionResource,
            UnidadeMedida outputUnitOfMeasure,
            SupplyNetworkProjection supplyNetworkProjection) {

        for (Constantes.TipoPlano planType : List.of(
                Constantes.TipoPlano.PLANO_IRRESTRITO,
                Constantes.TipoPlano.PLANO_TRABALHO,
                Constantes.TipoPlano.PLANO_RESTRITO)) {
            double quantity = getProductionQuantity(
                    productionPlanLine,
                    planType,
                    outputUnitOfMeasure,
                    supplyNetworkProjection);
            double hours = productionPlanLine
                    .getCapacidadeConsumidaPorRecursoProdutivoEmHoras(
                            planType,
                            Constantes.FirmePlanejado.TOTAL,
                            supplyNetworkProjection)
                    .getOrDefault(productionResource, 0.0d);
            if (quantity > 0.0d && hours > 0.0d) {
                return quantity / hours;
            }
        }
        return 0.0d;

    }

    /** Mantém somente linhas que carregam algum valor publicável na célula. */
    private boolean hasAnyPublishedMeasure(CommunityProductionOverviewResourceDetailDTO detail) {

        return detail.unconstrainedHours != 0.0d
                || detail.constrainedHours != 0.0d
                || detail.workPlanHours != 0.0d
                || detail.unconstrainedQuantity != 0.0d
                || detail.constrainedQuantity != 0.0d
                || detail.workPlanQuantity != 0.0d;

    }

    /** Expõe a unidade real da capacidade, usando Hours para capacidade temporal. */
    private String getResourceCapacityUnitOfMeasureId(
            RecursoProdutivo productionResource,
            PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva productionCapacityType,
            ClusterEParametrosProjection clusterAndParametersProjection) {

        return switch (productionCapacityType) {
            case QUANTIDADE_POR_UOM -> productionResource
                    .getUnidadeMedidaCapacidadeEmUom(
                            clusterAndParametersProjection.getParametrosGlobais())
                    .getId();
            default -> "Hours";
        };

    }
}
