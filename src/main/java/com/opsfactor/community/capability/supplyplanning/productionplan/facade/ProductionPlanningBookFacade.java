package com.opsfactor.community.capability.supplyplanning.productionplan.facade;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjectionFactory;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjection;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjectionFactory;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjectionFactory;
import com.opsfactor.community.capability.masterdata.production.productionresource.projection.BIProjectionCapacidadeProdutiva;
import com.opsfactor.community.capability.masterdata.production.productionresource.projection.BIProjectionCapacidadeProdutivaFactory;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import com.opsfactor.community.capability.supplyplanning.productionplan.repository.ProductionPlanLinhaRepository;
import com.opsfactor.community.capability.masterdata.network.location.service.LocationService;
import com.opsfactor.community.capability.supplyplanning.productionplan.facade.dto.ProductionPlanningBookDTO;
import com.opsfactor.community.capability.supplyplanning.productionplan.facade.dto.ProductionPlanningBookUpdateDTO;
import com.opsfactor.community.capability.supplyplanning.productionplan.facade.dto.ProductionPlanningMaterialDTO;
import com.opsfactor.community.capability.supplyplanning.productionplan.facade.dto.ProductionPlanningResourceDTO;
import com.opsfactor.community.capability.supplyplanning.service.SupplyPlanService;
import com.opsfactor.community.capability.supplyplanning.supplyplan.service.SupplyPlanningModificacoesService;
import com.opsfactor.community.platform.utility.Constantes;
import jakarta.persistence.NoResultException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Community facade for the basic Production Planning Book.
 *
 * <p>The facade reads a single {@link SupplyPlanningProjection} assembled by
 * the existing factory and persists manual changes through
 * {@link SupplyPlanningModificacoesService}. It never creates a parallel
 * production snapshot or writes {@code ProductionPlanLinha} directly.</p>
 */
@Service
public class ProductionPlanningBookFacade {

    /** Lê as linhas de produção persistidas do plano selecionado. */
    @Autowired
    private ProductionPlanLinhaRepository productionPlanLinhaRepository;

    /** Carrega materiais usados na edição da produção planejada. */
    @Autowired
    private ProdutoRepository produtoRepository;

    /** Resolve a location da tela de planejamento de produção. */
    @Autowired
    private LocationService locationService;

    /** Obtém o Supply Plan e seu perfil de execução. */
    @Autowired
    private SupplyPlanService supplyPlanService;

    /** Materializa a projection de planejamento da location selecionada. */
    @Autowired
    private SupplyPlanProjectionFactory supplyPlanProjectionFactory;

    /** Aplica ajustes manuais pelo serviço central de modificações. */
    @Autowired
    private SupplyPlanningModificacoesService supplyPlanningModificacoesService;

    /** Carrega parâmetros globais necessários ao calendário e à malha. */
    @Autowired
    private ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory;

    /** Resolve a política de estoque que compõe a projection do livro. */
    @Autowired
    private PoliticaEstoquesProjectionFactory politicaEstoquesProjectionFactory;

    /** Fornece a estrutura produtiva da malha Community em cache. */
    @Autowired
    private SupplyNetworkProjectionFactory supplyNetworkProjectionFactory;

    /** Calcula capacidade e ocupação por recurso na visualização do livro. */
    @Autowired
    private BIProjectionCapacidadeProdutivaFactory biProjectionCapacidadeProdutivaFactory;

    /**
     * Loads the Community production book for one location and saved Supply Plan.
     */
    public ProductionPlanningBookDTO getProductionPlanningBook(
            String locationId,
            Long supplyPlanId) {

        Location location = getRequiredLocation(locationId);
        SupplyPlan supplyPlan = getRequiredSupplyPlan(supplyPlanId);
        SupplyPlanningBookContext productionPlanningBookContext =
                createProductionPlanningBookContext(location, supplyPlan);

        return createProductionPlanningBookDTO(productionPlanningBookContext);

    }

    /**
     * Persists a Working Plan planned-production adjustment atomically and
     * returns the rebuilt projection-backed book.
     */
    @Transactional(rollbackFor = Exception.class)
    public ProductionPlanningBookDTO updatePlannedProduction(
            ProductionPlanningBookUpdateDTO productionPlanningBookUpdateDTO) {

        validateUpdateCommand(productionPlanningBookUpdateDTO);

        Location location = getRequiredLocation(productionPlanningBookUpdateDTO.locationId());
        SupplyPlan supplyPlan = getRequiredSupplyPlan(productionPlanningBookUpdateDTO.supplyPlanId());
        Produto material = produtoRepository.findById(productionPlanningBookUpdateDTO.materialId())
                .orElseThrow(() -> new NoResultException(
                        "Product " + productionPlanningBookUpdateDTO.materialId() + " not found"));
        ProductionPlanningBookUpdateContext productionPlanningBookUpdateContext =
                createProductionPlanningBookUpdateContext(
                        location,
                        supplyPlan,
                        material,
                        productionPlanningBookUpdateDTO.productionResourceId());
        int periodPosition = productionPlanningBookUpdateContext.supplyPlanningProjection()
                .getCalendario()
                .getPosicaoPeriodo(productionPlanningBookUpdateDTO.periodEndDate());

        validatePeriodPosition(
                productionPlanningBookUpdateContext.supplyPlanningProjection(),
                productionPlanningBookUpdateDTO.periodEndDate(),
                periodPosition);

        ClusterEParametrosProjection clusterAndParametersProjection =
                productionPlanningBookUpdateContext.clusterAndParametersProjection();
        UnidadeMedida standardUnitOfMeasure = clusterAndParametersProjection
                .getSNPUnidadeMedidaPadrao(material, location);

        supplyPlanningModificacoesService.modificaProductionPlanParaRecursoProdutivo(
                Constantes.TipoPlano.PLANO_TRABALHO,
                productionPlanningBookUpdateDTO.plannedQuantity(),
                standardUnitOfMeasure,
                productionPlanningBookUpdateContext.supplyPlanningProjection(),
                productionPlanningBookUpdateContext.inventoryPolicyProjection(),
                periodPosition,
                productionPlanningBookUpdateContext.productionResource(),
                material);

        return getProductionPlanningBook(location.getId(), supplyPlan.getId());

    }

    /**
     * Builds the shared projection/factory context once per book request.
     */
    private SupplyPlanningBookContext createProductionPlanningBookContext(
            Location location,
            SupplyPlan supplyPlan) {

        PerfilExecucaoSupplyPlan executionProfile = supplyPlan.getPerfilExecucaoSupplyPlan();
        if (executionProfile == null) {
            throw new IllegalStateException(
                    "Supply Plan " + supplyPlan.getId() + " has no execution profile for Community Production Planning Book.");
        }

        ClusterEParametrosProjection clusterAndParametersProjection =
                clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();
        ParametrosGlobais globalParameters = clusterAndParametersProjection.getParametrosGlobais();
        if (globalParameters == null) {
            throw new IllegalStateException(
                    "Global parameters are required for Community Production Planning Book.");
        }

        PoliticaEstoquesProjection inventoryPolicyProjection =
                politicaEstoquesProjectionFactory.getPoliticaEstoquesProjection(
                        supplyPlan.getCalendarioDoSupplyPlan(globalParameters),
                        clusterAndParametersProjection,
                        executionProfile);
        SupplyNetworkProjection supplyNetworkProjection =
                supplyNetworkProjectionFactory.getSupplyNetworkProjectionCompletoDeCache();

        Set<Produto> producedMaterials = getProducedMaterials(
                supplyPlan,
                location,
                supplyNetworkProjection);
        SupplyPlanningProjection supplyPlanningProjection =
                supplyPlanProjectionFactory.getSupplyPlanningProjectionVazio(
                        supplyPlan,
                        executionProfile,
                        location,
                        supplyNetworkProjection,
                        inventoryPolicyProjection,
                        MaterialProjectionFactory.getProjectionSetMateriais(
                                producedMaterials,
                                clusterAndParametersProjection),
                        LocationProjectionFactory.getLocationProjectionDePerfilExecucaoSupplyPlan(
                                executionProfile,
                                clusterAndParametersProjection));
        supplyPlanProjectionFactory.populaSupplyPlanningProjectionComProductionPlanOutput(
                supplyPlanningProjection);
        BIProjectionCapacidadeProdutiva productionCapacityProjection =
                biProjectionCapacidadeProdutivaFactory
                        .getBIProjectionCapacidadeProdutivaDeSupplyPlan(
                                supplyPlan,
                                supplyPlanningProjection.getCalendario());

        return new SupplyPlanningBookContext(
                supplyPlanningProjection,
                supplyNetworkProjection,
                inventoryPolicyProjection,
                clusterAndParametersProjection,
                productionCapacityProjection);

    }

    /**
     * Builds the narrow but complete calculation context required before a
     * planned-production write. A production adjustment changes component
     * consumption and available stock, so populating only production output
     * would let the persistence service save an incoherent inventory or inbound
     * snapshot. This sequence mirrors the established adjustment workflow and
     * remains material/resource scoped to avoid loading an unrelated book.
     */
    private ProductionPlanningBookUpdateContext createProductionPlanningBookUpdateContext(
            Location location,
            SupplyPlan supplyPlan,
            Produto material,
            String productionResourceId) {

        PerfilExecucaoSupplyPlan executionProfile = supplyPlan.getPerfilExecucaoSupplyPlan();
        if (executionProfile == null) {
            throw new IllegalStateException(
                    "Supply Plan " + supplyPlan.getId() + " has no execution profile for Community Production Planning Book update.");
        }

        ClusterEParametrosProjection clusterAndParametersProjection =
                clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();
        ParametrosGlobais globalParameters = clusterAndParametersProjection.getParametrosGlobais();
        if (globalParameters == null) {
            throw new IllegalStateException(
                    "Global parameters are required for Community Production Planning Book update.");
        }

        PoliticaEstoquesProjection inventoryPolicyProjection =
                politicaEstoquesProjectionFactory.getPoliticaEstoquesProjection(
                        supplyPlan.getCalendarioDoSupplyPlan(globalParameters),
                        clusterAndParametersProjection,
                        executionProfile);
        SupplyNetworkProjection supplyNetworkProjection =
                supplyNetworkProjectionFactory.getSupplyNetworkProjectionCompletoDeCache();
        RecursoProdutivo productionResource = getRequiredProductionResource(
                supplyNetworkProjection,
                location,
                productionResourceId);
        validateMaterialProducedByResource(
                supplyNetworkProjection,
                material,
                productionResource,
                location);
        SupplyPlanningProjection supplyPlanningProjection =
                supplyPlanProjectionFactory.getSupplyPlanningProjectionVazio(
                        supplyPlan,
                        executionProfile,
                        location,
                        supplyNetworkProjection,
                        inventoryPolicyProjection,
                        MaterialProjectionFactory.getProjectionUnicoMaterial(
                                material,
                                clusterAndParametersProjection),
                        LocationProjectionFactory.getLocationProjectionDePerfilExecucaoSupplyPlan(
                                executionProfile,
                                clusterAndParametersProjection));

        /*
         * The modification service recomputes inventory and persists production
         * inputs/inbounds. Populate every dependent operational series before
         * invoking it; none of these calls introduces a new projection.
         */
        supplyPlanProjectionFactory.populaSupplyPlanningProjectionComInventoryPlan(
                supplyPlanningProjection);
        supplyPlanProjectionFactory.populaSupplyPlanningProjectionComDistributionPlanInbound(
                supplyPlanningProjection);
        supplyPlanProjectionFactory.populaSupplyPlanningProjectionComDistributionPlanOutbound(
                supplyPlanningProjection);
        supplyPlanProjectionFactory.populaSupplyPlanningProjectionComProductionPlanInput(
                supplyPlanningProjection);
        supplyPlanProjectionFactory.populaSupplyPlanningProjectionComProductionPlanOutput(
                supplyPlanningProjection,
                productionResource,
                material);

        return new ProductionPlanningBookUpdateContext(
                supplyPlanningProjection,
                inventoryPolicyProjection,
                clusterAndParametersProjection,
                productionResource);

    }

    /**
     * Collects the material envelope in one repository call and augments it
     * with currently producible materials from the already loaded network
     * projection. No material is loaded cell by cell.
     */
    private Set<Produto> getProducedMaterials(
            SupplyPlan supplyPlan,
            Location location,
            SupplyNetworkProjection supplyNetworkProjection) {

        Set<Produto> producedMaterials = productionPlanLinhaRepository
                .findProductionPlanMaterialsForPlanningBook(supplyPlan, location)
                .stream()
                .map(ProductionPlanLinha::getMaterialOutput)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        producedMaterials.addAll(supplyNetworkProjection.getMateriaisProduzidosLocation(location));

        return producedMaterials;

    }

    /**
     * Converts the projection-backed book to the compact public Community DTO.
     */
    private ProductionPlanningBookDTO createProductionPlanningBookDTO(
            SupplyPlanningBookContext productionPlanningBookContext) {

        SupplyPlanningProjection supplyPlanningProjection =
                productionPlanningBookContext.supplyPlanningProjection();
        List<Integer> periodPositions = getVisiblePeriodPositions(supplyPlanningProjection);
        List<LocalDateTime> periodEndDates = periodPositions.stream()
                .map(position -> supplyPlanningProjection.getCalendario()
                        .getListDataHorariosFinaisPorPeriodo()
                        .get(position))
                .toList();
        List<ProductionPlanningResourceDTO> resourceDTOs = productionPlanningBookContext
                .supplyNetworkProjection()
                .getRecursoProdutivoAtivoSet(supplyPlanningProjection.getLocation())
                .stream()
                .sorted(Comparator.comparing(RecursoProdutivo::getId))
                .map(productionResource -> createProductionPlanningResourceDTO(
                        productionPlanningBookContext,
                        productionResource,
                        periodPositions))
                .toList();

        return new ProductionPlanningBookDTO(
                supplyPlanningProjection.getSupplyPlan().getId(),
                supplyPlanningProjection.getLocation().getId(),
                periodEndDates,
                resourceDTOs);

    }

    /**
     * Materializes one resource branch without firm orders or scheduling state.
     */
    private ProductionPlanningResourceDTO createProductionPlanningResourceDTO(
            SupplyPlanningBookContext productionPlanningBookContext,
            RecursoProdutivo productionResource,
            List<Integer> periodPositions) {

        SupplyPlanningProjection supplyPlanningProjection =
                productionPlanningBookContext.supplyPlanningProjection();
        List<Double> capacityHoursByPeriod = periodPositions.stream()
                .map(periodPosition -> productionPlanningBookContext
                        .productionCapacityProjection()
                        .getCapacidadeEmHorasEmPosicaoPeriodo(
                                periodPosition,
                                productionResource,
                                BIProjectionCapacidadeProdutiva.MasterOrPlanningData.MASTER_DATA))
                .toList();
        List<ProductionPlanningMaterialDTO> materialDTOs = productionPlanningBookContext
                .supplyNetworkProjection()
                .getMaterialSetProduzidoEmRecursoProdutivo(productionResource)
                .stream()
                .sorted(Comparator.comparing(Produto::getId))
                .map(material -> createProductionPlanningMaterialDTO(
                        supplyPlanningProjection,
                        productionResource,
                        material,
                        periodPositions))
                .toList();

        return new ProductionPlanningResourceDTO(
                productionResource.getId(),
                productionResource.getDescricao(),
                capacityHoursByPeriod,
                materialDTOs);

    }

    /**
     * Reads only planned production from the Working Plan; firm production is
     * deliberately not added to the series.
     */
    private ProductionPlanningMaterialDTO createProductionPlanningMaterialDTO(
            SupplyPlanningProjection supplyPlanningProjection,
            RecursoProdutivo productionResource,
            Produto material,
            List<Integer> periodPositions) {

        UnidadeMedida standardUnitOfMeasure = supplyPlanningProjection
                .getClusterEParametrosProjection()
                .getSNPUnidadeMedidaPadrao(material, supplyPlanningProjection.getLocation());
        List<Double> plannedQuantityByPeriod = periodPositions.stream()
                .map(periodPosition -> supplyPlanningProjection.getQuantidadeProductionPlan(
                        periodPosition,
                        material,
                        productionResource,
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        Constantes.FirmePlanejado.PLANEJADO,
                        standardUnitOfMeasure))
                .toList();

        return new ProductionPlanningMaterialDTO(
                material.getId(),
                material.getDescricao(),
                standardUnitOfMeasure.getId(),
                plannedQuantityByPeriod);

    }

    /**
     * Uses the current-and-future Planning Book envelope consistently for all
     * resource and material rows.
     */
    private List<Integer> getVisiblePeriodPositions(
            SupplyPlanningProjection supplyPlanningProjection) {

        List<Integer> periodPositions = new ArrayList<>();
        for (int periodPosition = supplyPlanningProjection.getCalendario().getPosicaoPeriodoPresente();
             periodPosition < supplyPlanningProjection.getCalendario().getNumeroPeriodosTotais();
             periodPosition++) {
            periodPositions.add(periodPosition);
        }

        return periodPositions;

    }

    /**
     * Validates the public update command before repositories, projections or
     * persistence are reached.
     */
    private void validateUpdateCommand(
            ProductionPlanningBookUpdateDTO productionPlanningBookUpdateDTO) {

        if (productionPlanningBookUpdateDTO == null) {
            throw new IllegalArgumentException("Production Planning Book update payload is required.");
        }
        if (productionPlanningBookUpdateDTO.supplyPlanId() == null) {
            throw new IllegalArgumentException("Production Planning Book update Supply Plan id is required.");
        }
        validateRequiredIdentifier(productionPlanningBookUpdateDTO.locationId(), "location");
        validateRequiredIdentifier(productionPlanningBookUpdateDTO.materialId(), "material");
        validateRequiredIdentifier(productionPlanningBookUpdateDTO.productionResourceId(), "production resource");
        if (productionPlanningBookUpdateDTO.periodEndDate() == null) {
            throw new IllegalArgumentException("Production Planning Book update period end date is required.");
        }
        if (productionPlanningBookUpdateDTO.plannedQuantity() == null
                || !Double.isFinite(productionPlanningBookUpdateDTO.plannedQuantity())) {
            throw new IllegalArgumentException(
                    "Production Planning Book planned quantity must be a finite number.");
        }
        if (productionPlanningBookUpdateDTO.plannedQuantity() < 0.0d) {
            throw new IllegalArgumentException(
                    "Production Planning Book planned quantity must be greater than or equal to zero.");
        }

    }

    /**
     * Rejects blank business identifiers consistently at the typed boundary.
     */
    private void validateRequiredIdentifier(String identifier, String fieldName) {

        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException(
                    "Production Planning Book update " + fieldName + " id is required.");
        }

    }

    /**
     * Resolves a Community-visible location through its service boundary.
     */
    private Location getRequiredLocation(String locationId) {

        if (locationId == null || locationId.isBlank()) {
            throw new IllegalArgumentException("Production Planning Book location id is required.");
        }

        return locationService.getLocation(locationId);

    }

    /**
     * Resolves only persisted Supply Plans; a null id cannot address a book.
     */
    private SupplyPlan getRequiredSupplyPlan(Long supplyPlanId) {

        if (supplyPlanId == null) {
            throw new IllegalArgumentException("Production Planning Book Supply Plan id is required.");
        }

        return supplyPlanService.getSupplyPlanDeId(supplyPlanId);

    }

    /**
     * Resolves an active resource from the network projection instead of from
     * a lazily traversed location collection.
     */
    private RecursoProdutivo getRequiredProductionResource(
            SupplyNetworkProjection supplyNetworkProjection,
            Location location,
            String productionResourceId) {

        return supplyNetworkProjection.getRecursoProdutivoAtivoSet(location)
                .stream()
                .filter(productionResource -> productionResource.getId().equals(productionResourceId))
                .findFirst()
                .orElseThrow(() -> new NoResultException(
                        "Active production resource " + productionResourceId
                                + " was not found at location " + location.getId()));

    }

    /**
     * Prevents a forged book command from assigning a material to a resource
     * that cannot produce it. The persistence service selects the priority
     * production version by material/location, so this gate must remain before
     * it receives the resource-specific adjustment.
     */
    private void validateMaterialProducedByResource(
            SupplyNetworkProjection supplyNetworkProjection,
            Produto material,
            RecursoProdutivo productionResource,
            Location location) {

        if (!supplyNetworkProjection
                .getMaterialSetProduzidoEmRecursoProdutivo(productionResource)
                .contains(material)) {
            throw new IllegalArgumentException(
                    "Product " + material.getId()
                            + " is not produced by active production resource "
                            + productionResource.getId()
                            + " at location "
                            + location.getId()
                            + ".");
        }

    }

    /**
     * Guarantees the command targets a visible current/future calendar bucket.
     */
    private void validatePeriodPosition(
            SupplyPlanningProjection supplyPlanningProjection,
            LocalDateTime periodEndDate,
            int periodPosition) {

        if (periodPosition < supplyPlanningProjection.getCalendario().getPosicaoPeriodoPresente()
                || periodPosition >= supplyPlanningProjection.getCalendario().getNumeroPeriodosTotais()
                || !supplyPlanningProjection.getCalendario()
                .getListDataHorariosFinaisPorPeriodo()
                .get(periodPosition)
                .equals(periodEndDate)) {
            throw new IllegalArgumentException(
                    "Production Planning Book period end date " + periodEndDate
                            + " must be an exact current/future Supply Plan period end date.");
        }

    }

    /**
     * Internal single-request context; it is not persisted or exposed.
     */
    private record SupplyPlanningBookContext(
            SupplyPlanningProjection supplyPlanningProjection,
            SupplyNetworkProjection supplyNetworkProjection,
            PoliticaEstoquesProjection inventoryPolicyProjection,
            ClusterEParametrosProjection clusterAndParametersProjection,
            BIProjectionCapacidadeProdutiva productionCapacityProjection) {

    }

    /**
     * Internal material/resource-scoped write context. It is intentionally
     * separate from the full read book to make the prerequisite series for a
     * transactional adjustment explicit.
     */
    private record ProductionPlanningBookUpdateContext(
            SupplyPlanningProjection supplyPlanningProjection,
            PoliticaEstoquesProjection inventoryPolicyProjection,
            ClusterEParametrosProjection clusterAndParametersProjection,
            RecursoProdutivo productionResource) {

    }
}
