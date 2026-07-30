package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.facade;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosDemandPlanProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.factory.ParametrosDemandPlanningProjectionFactory;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.configuration.repository.PerfilExecucaoSupplyPlanRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.VersaoMalhaRepository;
import com.opsfactor.community.capability.demandplanning.demandplan.repository.DemandPlanRepository;
import com.opsfactor.community.capability.supplyplanning.distributionplan.repository.DistributionPlanItemRepository;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.SupplyPlanRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.facade.dto.UnidadeConversaoFaltanteDTO;
import com.opsfactor.community.platform.utility.Constantes;
import jakarta.persistence.NoResultException;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Contratos Community do diagnostico de conversoes faltantes.
 *
 * <p>Os testes mantem todos os demais collaborators Spring ausentes de
 * proposito. Cada id obrigatorio deve falhar antes de montar projections de
 * UOM, estoque, malha ou plano de demanda.</p>
 */
public class UnidadeMedidaFacadeCommunityContractTest {

    @Test
    public void getUnidadeConversaoFaltanteSNPListDTOShouldFailWhenSupplyNetworkVersionDoesNotExist() throws Exception {

        UnidadeMedidaFacade unidadeMedidaFrontService = new UnidadeMedidaFacade();
        setField(
                unidadeMedidaFrontService,
                "versaoMalhaRepository",
                getVersaoMalhaRepositoryStub(Optional.empty()));

        NoResultException noResultException = Assertions.assertThrows(
                NoResultException.class,
                () -> unidadeMedidaFrontService.getUnidadeConversaoFaltanteSNPListDTO(
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        Constantes.TamanhoBucket.DIARIO,
                        "MISSING_NETWORK",
                        "PROFILE",
                        1L));

        Assertions.assertEquals(
                "Supply Network Version MISSING_NETWORK not found for missing UOM diagnostic.",
                noResultException.getMessage());

    }

    @Test
    public void getUnidadeConversaoFaltanteSNPListDTOShouldFailWhenDemandPlanDoesNotExistBeforeProjectionAccess() throws Exception {

        UnidadeMedidaFacade unidadeMedidaFrontService = new UnidadeMedidaFacade();
        setField(
                unidadeMedidaFrontService,
                "versaoMalhaRepository",
                getVersaoMalhaRepositoryStub(Optional.of(new VersaoMalha("NETWORK"))));
        setField(
                unidadeMedidaFrontService,
                "demandPlanRepository",
                getDemandPlanRepositoryStub(Optional.empty()));

        NoResultException noResultException = Assertions.assertThrows(
                NoResultException.class,
                () -> unidadeMedidaFrontService.getUnidadeConversaoFaltanteSNPListDTO(
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        Constantes.TamanhoBucket.DIARIO,
                        "NETWORK",
                        "PROFILE",
                        77L));

        Assertions.assertEquals(
                "Demand Plan 77 not found for missing UOM diagnostic.",
                noResultException.getMessage());

    }

    @Test
    public void getUnidadeConversaoFaltanteSNPListDTOShouldFailWhenSupplyPlanningProfileDoesNotExistBeforeProjectionAccess() throws Exception {

        UnidadeMedidaFacade unidadeMedidaFrontService = new UnidadeMedidaFacade();
        setField(
                unidadeMedidaFrontService,
                "versaoMalhaRepository",
                getVersaoMalhaRepositoryStub(Optional.of(new VersaoMalha("NETWORK"))));
        setField(
                unidadeMedidaFrontService,
                "demandPlanRepository",
                getDemandPlanRepositoryStub(Optional.of(getDemandPlanComId(77L))));
        setField(
                unidadeMedidaFrontService,
                "perfilExecucaoSupplyPlanRepository",
                getPerfilExecucaoSupplyPlanRepositoryStub(Optional.empty()));

        NoResultException noResultException = Assertions.assertThrows(
                NoResultException.class,
                () -> unidadeMedidaFrontService.getUnidadeConversaoFaltanteSNPListDTO(
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        Constantes.TamanhoBucket.DIARIO,
                        "NETWORK",
                        "MISSING_PROFILE",
                        77L));

        Assertions.assertEquals(
                "Supply Planning Profile MISSING_PROFILE not found for missing UOM diagnostic.",
                noResultException.getMessage());

    }

    @Test
    public void getUnidadeConversaoFaltanteSNPListDTOShouldRejectNullRepositoryOptionalsBeforeProjectionAccess()
            throws Exception {

        UnidadeMedidaFacade unidadeMedidaFrontServiceComMalhaQuebrada =
                new UnidadeMedidaFacade();
        setField(
                unidadeMedidaFrontServiceComMalhaQuebrada,
                "versaoMalhaRepository",
                getVersaoMalhaRepositoryStub(null));

        /*
         * Cada lookup obrigatorio do diagnostico SNP deve separar ausência
         * funcional (`Optional.empty()`) de quebra estrutural do repository
         * (`Optional` nulo), falhando antes de projections pesadas de UOM,
         * estoque, malha ou demanda.
         */
        IllegalStateException versaoMalhaException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> unidadeMedidaFrontServiceComMalhaQuebrada.getUnidadeConversaoFaltanteSNPListDTO(
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        Constantes.TamanhoBucket.DIARIO,
                        "NETWORK",
                        "PROFILE",
                        1L));
        Assertions.assertEquals(
                "Supply Network Version repository returned null Optional for missing UOM diagnostic id NETWORK.",
                versaoMalhaException.getMessage());

        UnidadeMedidaFacade unidadeMedidaFrontServiceComDemandPlanQuebrado =
                new UnidadeMedidaFacade();
        setField(
                unidadeMedidaFrontServiceComDemandPlanQuebrado,
                "versaoMalhaRepository",
                getVersaoMalhaRepositoryStub(Optional.of(new VersaoMalha("NETWORK"))));
        setField(
                unidadeMedidaFrontServiceComDemandPlanQuebrado,
                "demandPlanRepository",
                getDemandPlanRepositoryStub(null));

        IllegalStateException demandPlanException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> unidadeMedidaFrontServiceComDemandPlanQuebrado.getUnidadeConversaoFaltanteSNPListDTO(
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        Constantes.TamanhoBucket.DIARIO,
                        "NETWORK",
                        "PROFILE",
                        77L));
        Assertions.assertEquals(
                "Demand Plan repository returned null Optional for missing UOM diagnostic id 77.",
                demandPlanException.getMessage());

        UnidadeMedidaFacade unidadeMedidaFrontServiceComPerfilQuebrado =
                new UnidadeMedidaFacade();
        setField(
                unidadeMedidaFrontServiceComPerfilQuebrado,
                "versaoMalhaRepository",
                getVersaoMalhaRepositoryStub(Optional.of(new VersaoMalha("NETWORK"))));
        setField(
                unidadeMedidaFrontServiceComPerfilQuebrado,
                "demandPlanRepository",
                getDemandPlanRepositoryStub(Optional.of(getDemandPlanComId(77L))));
        setField(
                unidadeMedidaFrontServiceComPerfilQuebrado,
                "perfilExecucaoSupplyPlanRepository",
                getPerfilExecucaoSupplyPlanRepositoryStub(null));

        IllegalStateException perfilException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> unidadeMedidaFrontServiceComPerfilQuebrado.getUnidadeConversaoFaltanteSNPListDTO(
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        Constantes.TamanhoBucket.DIARIO,
                        "NETWORK",
                        "PROFILE",
                        77L));
        Assertions.assertEquals(
                "Supply Planning Profile repository returned null Optional for missing UOM diagnostic id PROFILE.",
                perfilException.getMessage());

    }

    @Test
    public void getUnidadeConversaoFaltanteSNPListDTOShouldRejectBrokenRepositoryEntityIdentitiesBeforeProjectionAccess()
            throws Exception {

        UnidadeMedidaFacade unidadeMedidaFrontServiceComMalhaSemId =
                new UnidadeMedidaFacade();
        setField(
                unidadeMedidaFrontServiceComMalhaSemId,
                "versaoMalhaRepository",
                getVersaoMalhaRepositoryStub(Optional.of(new VersaoMalha())));

        IllegalStateException malhaSemIdException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> unidadeMedidaFrontServiceComMalhaSemId.getUnidadeConversaoFaltanteSNPListDTO(
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        Constantes.TamanhoBucket.DIARIO,
                        "NETWORK",
                        "PROFILE",
                        77L));
        Assertions.assertEquals(
                "Supply Network Version snapshot id is required for missing UOM diagnostic id NETWORK.",
                malhaSemIdException.getMessage());

        UnidadeMedidaFacade unidadeMedidaFrontServiceComDemandPlanDivergente =
                new UnidadeMedidaFacade();
        setField(
                unidadeMedidaFrontServiceComDemandPlanDivergente,
                "versaoMalhaRepository",
                getVersaoMalhaRepositoryStub(Optional.of(new VersaoMalha("NETWORK"))));
        setField(
                unidadeMedidaFrontServiceComDemandPlanDivergente,
                "demandPlanRepository",
                getDemandPlanRepositoryStub(Optional.of(getDemandPlanComId(88L))));

        IllegalStateException demandPlanDivergenteException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> unidadeMedidaFrontServiceComDemandPlanDivergente.getUnidadeConversaoFaltanteSNPListDTO(
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        Constantes.TamanhoBucket.DIARIO,
                        "NETWORK",
                        "PROFILE",
                        77L));
        Assertions.assertEquals(
                "Demand Plan snapshot id must match requested missing UOM diagnostic id 77.",
                demandPlanDivergenteException.getMessage());

        UnidadeMedidaFacade unidadeMedidaFrontServiceComPerfilSemId =
                new UnidadeMedidaFacade();
        setField(
                unidadeMedidaFrontServiceComPerfilSemId,
                "versaoMalhaRepository",
                getVersaoMalhaRepositoryStub(Optional.of(new VersaoMalha("NETWORK"))));
        setField(
                unidadeMedidaFrontServiceComPerfilSemId,
                "demandPlanRepository",
                getDemandPlanRepositoryStub(Optional.of(getDemandPlanComId(77L))));
        setField(
                unidadeMedidaFrontServiceComPerfilSemId,
                "perfilExecucaoSupplyPlanRepository",
                getPerfilExecucaoSupplyPlanRepositoryStub(Optional.of(new PerfilExecucaoSupplyPlan())));

        IllegalStateException perfilSemIdException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> unidadeMedidaFrontServiceComPerfilSemId.getUnidadeConversaoFaltanteSNPListDTO(
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        Constantes.TamanhoBucket.DIARIO,
                        "NETWORK",
                        "PROFILE",
                        77L));
        Assertions.assertEquals(
                "Supply Planning Profile snapshot id is required for missing UOM diagnostic id PROFILE.",
                perfilSemIdException.getMessage());

        UnidadeMedidaFacade unidadeMedidaFrontServiceComPerfilDivergente =
                new UnidadeMedidaFacade();
        setField(
                unidadeMedidaFrontServiceComPerfilDivergente,
                "versaoMalhaRepository",
                getVersaoMalhaRepositoryStub(Optional.of(new VersaoMalha("NETWORK"))));
        setField(
                unidadeMedidaFrontServiceComPerfilDivergente,
                "demandPlanRepository",
                getDemandPlanRepositoryStub(Optional.of(getDemandPlanComId(77L))));
        setField(
                unidadeMedidaFrontServiceComPerfilDivergente,
                "perfilExecucaoSupplyPlanRepository",
                getPerfilExecucaoSupplyPlanRepositoryStub(Optional.of(getPerfilExecucaoSupplyPlanComId("OTHER_PROFILE"))));

        IllegalStateException perfilDivergenteException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> unidadeMedidaFrontServiceComPerfilDivergente.getUnidadeConversaoFaltanteSNPListDTO(
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        Constantes.TamanhoBucket.DIARIO,
                        "NETWORK",
                        "PROFILE",
                        77L));
        Assertions.assertEquals(
                "Supply Planning Profile snapshot id must match requested missing UOM diagnostic id PROFILE.",
                perfilDivergenteException.getMessage());

    }

    @Test
    public void getUnidadeConversaoFaltanteDeploymentListDTOShouldUseFetchedDistributionLinesForExpeditionAndTransferGaps()
            throws Exception {

        UnidadeMedidaFacade unidadeMedidaFrontService = new UnidadeMedidaFacade();
        SupplyPlan supplyPlan = Mockito.mock(SupplyPlan.class);
        DistributionPlanItem distributionPlanItem = Mockito.mock(DistributionPlanItem.class);
        Produto material = Mockito.mock(Produto.class);
        Location locationOrigem = Mockito.mock(Location.class);
        Location locationDestino = Mockito.mock(Location.class);
        Location locationOrigemCanonica = Mockito.mock(Location.class);
        UnidadeMedida unidadeMedidaPlano = Mockito.mock(UnidadeMedida.class);
        UnidadeMedida unidadeMedidaExpedicao = Mockito.mock(UnidadeMedida.class);
        UnidadeMedida unidadeMedidaTransferencia = Mockito.mock(UnidadeMedida.class);
        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        UnidadeMedidaProjection unidadeMedidaProjection = Mockito.mock(UnidadeMedidaProjection.class);
        ClusterEParametrosProjection clusterEParametrosProjection = Mockito.mock(ClusterEParametrosProjection.class);
        SupplyPlanRepository supplyPlanRepository = Mockito.mock(SupplyPlanRepository.class);
        DistributionPlanItemRepository distributionPlanItemRepository =
                Mockito.mock(DistributionPlanItemRepository.class);
        UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory =
                Mockito.mock(UnidadeMedidaProjectionFactory.class);
        ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory =
                Mockito.mock(ClusterEParametrosProjectionFactory.class);

        Mockito.when(supplyPlanRepository.customFindById(73L)).thenReturn(Optional.of(supplyPlan));
        Mockito.when(distributionPlanItemRepository.customFindBySupplyPlan(supplyPlan))
                .thenReturn(List.of(distributionPlanItem));
        Mockito.when(unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache())
                .thenReturn(unidadeMedidaProjection);
        Mockito.when(clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache())
                .thenReturn(clusterEParametrosProjection);
        Mockito.when(clusterEParametrosProjection.getParametrosGlobais()).thenReturn(parametrosGlobais);
        Mockito.when(clusterEParametrosProjection.getLocationMap())
                .thenReturn(Map.of("ORIGIN", locationOrigemCanonica));
        Mockito.when(clusterEParametrosProjection.getTransferenciaUnidadeMedida(
                material,
                locationOrigem,
                locationDestino)).thenReturn(unidadeMedidaTransferencia);
        Mockito.when(distributionPlanItem.getProduto()).thenReturn(material);
        Mockito.when(distributionPlanItem.getLocationOrigem()).thenReturn(locationOrigem);
        Mockito.when(distributionPlanItem.getLocationDestino()).thenReturn(locationDestino);
        Mockito.when(distributionPlanItem.getUnidadeMedida(parametrosGlobais)).thenReturn(unidadeMedidaPlano);
        Mockito.when(locationOrigem.getId()).thenReturn("ORIGIN");
        Mockito.when(locationDestino.getId()).thenReturn("DESTINATION");
        Mockito.when(locationOrigemCanonica.getExpeditionUomRegistered()).thenReturn(unidadeMedidaExpedicao);
        Mockito.when(material.getId()).thenReturn("MATERIAL");
        Mockito.when(unidadeMedidaPlano.getId()).thenReturn("PLAN_UOM");
        Mockito.when(unidadeMedidaExpedicao.getId()).thenReturn("EXPEDITION_UOM");
        Mockito.when(unidadeMedidaTransferencia.getId()).thenReturn("TRANSFER_UOM");
        Mockito.when(unidadeMedidaProjection.contemConversaoParaUnidadeDestino(
                Mockito.eq(material),
                Mockito.eq(unidadeMedidaPlano),
                Mockito.any(UnidadeMedida.class))).thenReturn(false);

        setField(unidadeMedidaFrontService, "supplyPlanRepository", supplyPlanRepository);
        setField(unidadeMedidaFrontService, "distributionPlanItemRepository", distributionPlanItemRepository);
        setField(unidadeMedidaFrontService, "unidadeMedidaProjectionFactory", unidadeMedidaProjectionFactory);
        setField(
                unidadeMedidaFrontService,
                "clusterEParametrosProjectionFactory",
                clusterEParametrosProjectionFactory);

        var result = unidadeMedidaFrontService.getUnidadeConversaoFaltanteDeploymentListDTO(73L);

        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.stream().anyMatch(gap ->
                gap.getTargetConversionRequirementType() == UnidadeConversaoFaltanteDTO.NecessidadeConversao.EXPEDICAO
                        && "EXPEDITION_UOM".equals(gap.getTargetUnitOfMeasure())
                        && "Origin Location ORIGIN".equals(gap.getTargetConversionRequirementId())));
        Assertions.assertTrue(result.stream().anyMatch(gap ->
                gap.getTargetConversionRequirementType() == UnidadeConversaoFaltanteDTO.NecessidadeConversao.TRANSFERENCIA
                        && "TRANSFER_UOM".equals(gap.getTargetUnitOfMeasure())
                        && "Origin Location ORIGIN / Destination Location DESTINATION / Material MATERIAL"
                                .equals(gap.getTargetConversionRequirementId())));
        Mockito.verify(supplyPlanRepository).customFindById(73L);
        Mockito.verify(distributionPlanItemRepository).customFindBySupplyPlan(supplyPlan);

    }

    private static UnidadeMedidaFacade createUnidadeMedidaFrontServiceParaDiagnosticoDP(
            ParametrosDemandPlanProjection parametrosDemandPlanProjection) throws Exception {

        UnidadeMedidaFacade unidadeMedidaFrontService = new UnidadeMedidaFacade();
        setField(
                unidadeMedidaFrontService,
                "unidadeMedidaProjectionFactory",
                new FakeUnidadeMedidaProjectionFactory(
                        createUnidadeMedidaProjectionComParametrosGlobais()));
        setField(
                unidadeMedidaFrontService,
                "clusterEParametrosProjectionFactory",
                new FakeClusterEParametrosProjectionFactory(
                        createClusterEParametrosProjectionComParametrosGlobais()));
        setField(
                unidadeMedidaFrontService,
                "parametrosDemandPlanningProjectionFactory",
                new FakeParametrosDemandPlanningProjectionFactory(parametrosDemandPlanProjection));
        return unidadeMedidaFrontService;

    }

    private static UnidadeMedidaProjection createUnidadeMedidaProjectionComParametrosGlobais() throws Exception {

        UnidadeMedidaProjection unidadeMedidaProjection = new UnidadeMedidaProjection();
        setField(
                unidadeMedidaProjection,
                "parametrosGlobais",
                new ParametrosGlobais());
        return unidadeMedidaProjection;

    }

    private static ClusterEParametrosProjection createClusterEParametrosProjectionComParametrosGlobais() throws Exception {

        ClusterEParametrosProjection clusterEParametrosProjection = new ClusterEParametrosProjection();
        setField(
                clusterEParametrosProjection,
                "parametrosGlobais",
                new ParametrosGlobais());
        return clusterEParametrosProjection;

    }

    private static class FakeUnidadeMedidaProjectionFactory extends UnidadeMedidaProjectionFactory {

        private final UnidadeMedidaProjection unidadeMedidaProjection;

        private FakeUnidadeMedidaProjectionFactory(UnidadeMedidaProjection unidadeMedidaProjection) {

            this.unidadeMedidaProjection = unidadeMedidaProjection;

        }

        @Override
        public UnidadeMedidaProjection getUnidadeMedidaProjectionCompletoDeCache() {

            return unidadeMedidaProjection;

        }

    }

    private static class FakeClusterEParametrosProjectionFactory extends ClusterEParametrosProjectionFactory {

        private final ClusterEParametrosProjection clusterEParametrosProjection;

        private FakeClusterEParametrosProjectionFactory(ClusterEParametrosProjection clusterEParametrosProjection) {

            this.clusterEParametrosProjection = clusterEParametrosProjection;

        }

        @Override
        public ClusterEParametrosProjection getParametrosProjectionCompletoDeCache() {

            return clusterEParametrosProjection;

        }

    }

    private static class FakeParametrosDemandPlanningProjectionFactory extends ParametrosDemandPlanningProjectionFactory {

        private final ParametrosDemandPlanProjection parametrosDemandPlanProjection;

        private FakeParametrosDemandPlanningProjectionFactory(
                ParametrosDemandPlanProjection parametrosDemandPlanProjection) {

            this.parametrosDemandPlanProjection = parametrosDemandPlanProjection;

        }

        @Override
        public ParametrosDemandPlanProjection getParametrosDemandPlanProjectionDeCache(
                String perfilExecucaoDemandPlanId) {

            return parametrosDemandPlanProjection;

        }

    }

    private static VersaoMalhaRepository getVersaoMalhaRepositoryStub(
            Optional<VersaoMalha> versaoMalhaOptional) {

        return (VersaoMalhaRepository) Proxy.newProxyInstance(
                VersaoMalhaRepository.class.getClassLoader(),
                new Class<?>[]{VersaoMalhaRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return versaoMalhaOptional;
                    }
                    if ("toString".equals(method.getName())) {
                        return "VersaoMalhaRepositoryStub";
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao esperado no proxy de teste: " + method.getName());
                });

    }

    private static DemandPlanRepository getDemandPlanRepositoryStub(
            Optional<DemandPlan> demandPlanOptional) {

        return (DemandPlanRepository) Proxy.newProxyInstance(
                DemandPlanRepository.class.getClassLoader(),
                new Class<?>[]{DemandPlanRepository.class},
                (proxy, method, args) -> {
                    if ("customFindByIdComPerfilExecucao".equals(method.getName())) {
                        return demandPlanOptional;
                    }
                    if ("toString".equals(method.getName())) {
                        return "DemandPlanRepositoryStub";
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao esperado no proxy de teste: " + method.getName());
                });

    }

    private static PerfilExecucaoSupplyPlanRepository getPerfilExecucaoSupplyPlanRepositoryStub(
            Optional<PerfilExecucaoSupplyPlan> perfilExecucaoSupplyPlanOptional) {

        return (PerfilExecucaoSupplyPlanRepository) Proxy.newProxyInstance(
                PerfilExecucaoSupplyPlanRepository.class.getClassLoader(),
                new Class<?>[]{PerfilExecucaoSupplyPlanRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return perfilExecucaoSupplyPlanOptional;
                    }
                    if ("toString".equals(method.getName())) {
                        return "PerfilExecucaoSupplyPlanRepositoryStub";
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao esperado no proxy de teste: " + method.getName());
                });

    }

    private static DemandPlan getDemandPlanComId(Long demandPlanId) {

        DemandPlan demandPlan = new DemandPlan();
        demandPlan.setId(demandPlanId);
        return demandPlan;

    }

    private static PerfilExecucaoSupplyPlan getPerfilExecucaoSupplyPlanComId(
            String perfilExecucaoSupplyPlanId) {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setId(perfilExecucaoSupplyPlanId);
        return perfilExecucaoSupplyPlan;

    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

}
