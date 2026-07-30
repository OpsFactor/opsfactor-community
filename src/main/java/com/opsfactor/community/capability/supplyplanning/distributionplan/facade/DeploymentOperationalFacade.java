package com.opsfactor.community.capability.supplyplanning.distributionplan.facade;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjectionFactory;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjection;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjectionFactory;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjectionFactory;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardEnum;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import com.opsfactor.community.capability.masterdata.network.location.service.LocationService;
import com.opsfactor.community.capability.supplyplanning.distributionplan.facade.dto.DeploymentOperationalInboundUpdateDTO;
import com.opsfactor.community.capability.supplyplanning.distributionplan.facade.dto.DeploymentOperationalLineDTO;
import com.opsfactor.community.capability.supplyplanning.service.SupplyPlanService;
import com.opsfactor.community.capability.supplyplanning.supplyplan.service.SupplyPlanningModificacoesService;
import com.opsfactor.community.platform.utility.Constantes;
import jakarta.persistence.NoResultException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

/**
 * Community facade for one operational deployment transfer at a time.
 *
 * <p>The legacy deployment screen combines several Enterprise analytics in a
 * broad DTO. This service deliberately reconstructs only the physical working
 * transfer from the existing Supply Planning projections and delegates its
 * persistence to the central modification service.</p>
 */
@Service
public class DeploymentOperationalFacade {

    /** Resolve locations físicas da rota operacional solicitada. */
    @Autowired
    private LocationService locationService;

    /** Carrega o material persistido da transferência. */
    @Autowired
    private ProdutoRepository produtoRepository;

    /** Obtém o Supply Plan de trabalho usado pela operação. */
    @Autowired
    private SupplyPlanService supplyPlanService;

    /** Fornece parâmetros globais para montar o calendário do plano. */
    @Autowired
    private ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory;

    /** Carrega políticas de estoque que qualificam a projection operacional. */
    @Autowired
    private PoliticaEstoquesProjectionFactory politicaEstoquesProjectionFactory;

    /** Materializa a malha e seus relacionamentos físicos em memória. */
    @Autowired
    private SupplyNetworkProjectionFactory supplyNetworkProjectionFactory;

    /** Constrói o snapshot de Supply Planning da rota selecionada. */
    @Autowired
    private SupplyPlanProjectionFactory supplyPlanProjectionFactory;

    /** Persiste ajustes manuais pelo fluxo central de Supply Planning. */
    @Autowired
    private SupplyPlanningModificacoesService supplyPlanningModificacoesService;

    /**
     * Reads the current Working Plan planned transfer for one physical route.
     */
    public DeploymentOperationalLineDTO getDeploymentOperationalLine(
            Long supplyPlanId,
            String originLocationId,
            String destinationLocationId,
            String materialId) {

        DeploymentOperationalContext deploymentOperationalContext =
                createDeploymentOperationalContext(
                        supplyPlanId,
                        originLocationId,
                        destinationLocationId,
                        materialId);

        return createDeploymentOperationalLineDTO(deploymentOperationalContext);

    }

    /**
     * Replaces one current planned inbound quantity atomically, using the
     * current projection value as the old value instead of trusting the client.
     */
    @Transactional(rollbackFor = Exception.class)
    public DeploymentOperationalLineDTO updatePlannedInbound(
            DeploymentOperationalInboundUpdateDTO deploymentOperationalInboundUpdateDTO) {

        validateUpdateCommand(deploymentOperationalInboundUpdateDTO);
        DeploymentOperationalContext deploymentOperationalContext =
                createDeploymentOperationalContext(
                        deploymentOperationalInboundUpdateDTO.supplyPlanId(),
                        deploymentOperationalInboundUpdateDTO.originLocationId(),
                        deploymentOperationalInboundUpdateDTO.destinationLocationId(),
                        deploymentOperationalInboundUpdateDTO.materialId());
        SupplyPlanningProjection destinationSupplyPlanningProjection =
                createDestinationSupplyPlanningProjection(deploymentOperationalContext);
        int currentPeriodPosition = destinationSupplyPlanningProjection
                .getCalendario()
                .getPosicaoPeriodoPresente();
        double currentPlannedInboundQuantity = getPlannedInboundQuantity(
                destinationSupplyPlanningProjection,
                deploymentOperationalContext,
                currentPeriodPosition);

        supplyPlanningModificacoesService.modificaSupplyPlan(
                Constantes.TipoPlano.PLANO_TRABALHO,
                KeyFigureStandardEnum.INBOUND_PLANEJADO,
                deploymentOperationalInboundUpdateDTO.plannedInboundQuantity(),
                currentPlannedInboundQuantity,
                deploymentOperationalContext.unitOfMeasure(),
                destinationSupplyPlanningProjection,
                Constantes.ReferenciaPeriodo.CONSUMO_CAPACIDADE,
                currentPeriodPosition,
                Set.of(deploymentOperationalContext.material()),
                Set.of(deploymentOperationalContext.originLocation()));

        return getDeploymentOperationalLine(
                deploymentOperationalContext.supplyPlan().getId(),
                deploymentOperationalContext.originLocation().getId(),
                deploymentOperationalContext.destinationLocation().getId(),
                deploymentOperationalContext.material().getId());

    }

    /**
     * Resolves and validates the physical route before any projection is built.
     */
    private DeploymentOperationalContext createDeploymentOperationalContext(
            Long supplyPlanId,
            String originLocationId,
            String destinationLocationId,
            String materialId) {

        validateReadIdentifiers(supplyPlanId, originLocationId, destinationLocationId, materialId);

        SupplyPlan supplyPlan = supplyPlanService.getSupplyPlanDeId(supplyPlanId);
        PerfilExecucaoSupplyPlan executionProfile = supplyPlan.getPerfilExecucaoSupplyPlan();
        if (executionProfile == null) {
            throw new IllegalStateException(
                    "Supply Plan " + supplyPlanId + " has no execution profile for Community deployment.");
        }
        Location originLocation = locationService.getLocation(originLocationId);
        Location destinationLocation = locationService.getLocation(destinationLocationId);
        Produto material = produtoRepository.findById(materialId)
                .orElseThrow(() -> new NoResultException("Product " + materialId + " not found"));
        ClusterEParametrosProjection clusterAndParametersProjection =
                clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();
        ParametrosGlobais globalParameters = clusterAndParametersProjection.getParametrosGlobais();
        if (globalParameters == null) {
            throw new IllegalStateException("Global parameters are required for Community deployment.");
        }
        PoliticaEstoquesProjection inventoryPolicyProjection =
                politicaEstoquesProjectionFactory.getPoliticaEstoquesProjection(
                        supplyPlan.getCalendarioDoSupplyPlan(globalParameters),
                        clusterAndParametersProjection,
                        executionProfile);
        SupplyNetworkProjection supplyNetworkProjection =
                supplyNetworkProjectionFactory.getSupplyNetworkProjectionCompletoDeCache();
        LocationProjection executionProfileLocationProjection =
                LocationProjectionFactory.getLocationProjectionDePerfilExecucaoSupplyPlan(
                        executionProfile,
                        clusterAndParametersProjection);

        validateRoute(
                supplyPlan,
                executionProfileLocationProjection,
                supplyNetworkProjection,
                originLocation,
                destinationLocation,
                material);

        UnidadeMedida unitOfMeasure = clusterAndParametersProjection
                .getSNPUnidadeMedidaPadrao(material, destinationLocation);
        LocationProjection routeLocationProjection = LocationProjectionFactory
                .getProjectionSetLocations(
                        Set.of(originLocation, destinationLocation),
                        clusterAndParametersProjection);

        return new DeploymentOperationalContext(
                supplyPlan,
                executionProfile,
                originLocation,
                destinationLocation,
                material,
                unitOfMeasure,
                inventoryPolicyProjection,
                supplyNetworkProjection,
                clusterAndParametersProjection,
                routeLocationProjection);

    }

    /**
     * Builds a complete, single-material destination projection. The complete
     * factory reuses its batch repository queries for inventory, inbound,
     * outbound, production and direct demand; no entity is queried per cell.
     */
    private SupplyPlanningProjection createDestinationSupplyPlanningProjection(
            DeploymentOperationalContext deploymentOperationalContext) {

        return supplyPlanProjectionFactory.getSupplyPlanningProjectionCompleto(
                deploymentOperationalContext.supplyPlan(),
                deploymentOperationalContext.executionProfile(),
                deploymentOperationalContext.destinationLocation(),
                deploymentOperationalContext.supplyNetworkProjection(),
                deploymentOperationalContext.inventoryPolicyProjection(),
                MaterialProjectionFactory.getProjectionUnicoMaterial(
                        deploymentOperationalContext.material(),
                        deploymentOperationalContext.clusterAndParametersProjection()),
                deploymentOperationalContext.routeLocationProjection());

    }

    /**
     * Maps only planned inbound transfer data for the selected origin; firm
     * order quantities are never read into this Community DTO.
     */
    private DeploymentOperationalLineDTO createDeploymentOperationalLineDTO(
            DeploymentOperationalContext deploymentOperationalContext) {

        SupplyPlanningProjection destinationSupplyPlanningProjection =
                createDestinationSupplyPlanningProjection(deploymentOperationalContext);
        int currentPeriodPosition = destinationSupplyPlanningProjection
                .getCalendario()
                .getPosicaoPeriodoPresente();
        double plannedInboundQuantity = getPlannedInboundQuantity(
                destinationSupplyPlanningProjection,
                deploymentOperationalContext,
                currentPeriodPosition);
        int leadTimeDays = deploymentOperationalContext.supplyNetworkProjection()
                .getLeadTimeDiasEntreOrigemDestinoParaMaterial(
                        deploymentOperationalContext.supplyPlan().getVersaoMalha(),
                        deploymentOperationalContext.originLocation(),
                        deploymentOperationalContext.destinationLocation(),
                        deploymentOperationalContext.material(),
                        deploymentOperationalContext.supplyPlan().getDataInicioPlano())
                .orElseThrow(() -> new IllegalStateException(
                        "Community deployment route has no lead time for origin "
                                + deploymentOperationalContext.originLocation().getId()
                                + ", destination "
                                + deploymentOperationalContext.destinationLocation().getId()
                                + " and material "
                                + deploymentOperationalContext.material().getId()
                                + "."));
        LocalDate expectedReceiptDate = destinationSupplyPlanningProjection
                .getCalendario()
                .getPrimeiraDataPeriodo(currentPeriodPosition)
                .plusDays(leadTimeDays);

        return new DeploymentOperationalLineDTO(
                deploymentOperationalContext.supplyPlan().getId(),
                deploymentOperationalContext.originLocation().getId(),
                deploymentOperationalContext.destinationLocation().getId(),
                deploymentOperationalContext.material().getId(),
                deploymentOperationalContext.material().getDescricao(),
                destinationSupplyPlanningProjection.getCalendario()
                        .getListDataHorariosFinaisPorPeriodo()
                        .get(currentPeriodPosition),
                leadTimeDays,
                expectedReceiptDate,
                deploymentOperationalContext.unitOfMeasure().getId(),
                plannedInboundQuantity);

    }

    /**
     * Reads exactly the planned inbound component for the selected origin and
     * current capacity-consumption period.
     */
    private double getPlannedInboundQuantity(
            SupplyPlanningProjection destinationSupplyPlanningProjection,
            DeploymentOperationalContext deploymentOperationalContext,
            int currentPeriodPosition) {

        return destinationSupplyPlanningProjection.getQuantidadeDistributionPlanInbound(
                Constantes.ReferenciaPeriodo.CONSUMO_CAPACIDADE,
                currentPeriodPosition,
                deploymentOperationalContext.material(),
                deploymentOperationalContext.originLocation(),
                Constantes.FirmePlanejado.PLANEJADO,
                Constantes.TipoPlano.PLANO_TRABALHO,
                deploymentOperationalContext.unitOfMeasure());

    }

    /**
     * Enforces the physical and profile scopes of the selected transfer route.
     */
    private void validateRoute(
            SupplyPlan supplyPlan,
            LocationProjection executionProfileLocationProjection,
            SupplyNetworkProjection supplyNetworkProjection,
            Location originLocation,
            Location destinationLocation,
            Produto material) {

        if (!executionProfileLocationProjection.getLocationsAtivas().contains(originLocation)
                || !executionProfileLocationProjection.getLocationsAtivas().contains(destinationLocation)) {
            throw new IllegalArgumentException(
                    "Community deployment origin and destination must both belong to the Supply Plan execution profile.");
        }
        if (!supplyNetworkProjection.getLocationDestinoViavelSet(
                supplyPlan.getVersaoMalha(),
                originLocation,
                material,
                supplyPlan.getDataInicioPlano(),
                executionProfileLocationProjection.getLocationsAtivas()).contains(destinationLocation)) {
            throw new IllegalArgumentException(
                    "Community deployment route is not viable for origin " + originLocation.getId()
                            + ", destination " + destinationLocation.getId()
                            + " and material " + material.getId() + ".");
        }

    }

    /**
     * Rejects incomplete identifiers before any repository or factory access.
     */
    private void validateReadIdentifiers(
            Long supplyPlanId,
            String originLocationId,
            String destinationLocationId,
            String materialId) {

        if (supplyPlanId == null) {
            throw new IllegalArgumentException("Community deployment Supply Plan id is required.");
        }
        validateRequiredIdentifier(originLocationId, "origin location");
        validateRequiredIdentifier(destinationLocationId, "destination location");
        validateRequiredIdentifier(materialId, "material");

    }

    /**
     * Validates the command-specific quantity before any state is loaded.
     */
    private void validateUpdateCommand(
            DeploymentOperationalInboundUpdateDTO deploymentOperationalInboundUpdateDTO) {

        if (deploymentOperationalInboundUpdateDTO == null) {
            throw new IllegalArgumentException("Community deployment update payload is required.");
        }
        validateReadIdentifiers(
                deploymentOperationalInboundUpdateDTO.supplyPlanId(),
                deploymentOperationalInboundUpdateDTO.originLocationId(),
                deploymentOperationalInboundUpdateDTO.destinationLocationId(),
                deploymentOperationalInboundUpdateDTO.materialId());
        if (deploymentOperationalInboundUpdateDTO.plannedInboundQuantity() == null
                || !Double.isFinite(deploymentOperationalInboundUpdateDTO.plannedInboundQuantity())) {
            throw new IllegalArgumentException(
                    "Community deployment planned inbound quantity must be a finite number.");
        }
        if (deploymentOperationalInboundUpdateDTO.plannedInboundQuantity() < 0.0d) {
            throw new IllegalArgumentException(
                    "Community deployment planned inbound quantity must be greater than or equal to zero.");
        }

    }

    /**
     * Validates one typed business identifier.
     */
    private void validateRequiredIdentifier(String identifier, String identifierName) {

        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException(
                    "Community deployment " + identifierName + " id is required.");
        }

    }

    /**
     * Request-local dependencies required to assemble one operational route.
     */
    private record DeploymentOperationalContext(
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan executionProfile,
            Location originLocation,
            Location destinationLocation,
            Produto material,
            UnidadeMedida unitOfMeasure,
            PoliticaEstoquesProjection inventoryPolicyProjection,
            SupplyNetworkProjection supplyNetworkProjection,
            ClusterEParametrosProjection clusterAndParametersProjection,
            LocationProjection routeLocationProjection) {

    }
}
