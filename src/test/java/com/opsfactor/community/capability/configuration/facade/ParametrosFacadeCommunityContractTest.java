package com.opsfactor.community.capability.configuration.facade;

import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.configuration.domain.ParametrosProdutoLocation;
import com.opsfactor.community.capability.configuration.domain.cluster.location.ParametrosClusterLocations;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.repository.ParametrosProdutoLocationRepository;
import com.opsfactor.community.capability.configuration.repository.cluster.location.ParametrosClusterLocationsRepository;
import com.opsfactor.community.capability.cluster.repository.location.ClusterLocationsRepository;
import com.opsfactor.community.capability.masterdata.network.location.service.LocationService;
import com.opsfactor.community.capability.masterdata.product.material.service.MaterialService;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.capability.configuration.facade.dto.ParametroClusterLocationDTO;
import com.opsfactor.community.capability.configuration.facade.dto.ParametrosMaterialDTO;
import com.opsfactor.community.capability.configuration.facade.dto.ParametrosMaterialLocationDTO;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class ParametrosFacadeCommunityContractTest {

    @Test
    public void getParametroClusterLocationDTOShouldUseFetchedClusterParametersSnapshot()
            throws Exception {

        ClusterLocations clusterLocations = new ClusterLocations();
        clusterLocations.setId(10L);
        clusterLocations.setDescricao("Cluster Location 10");
        clusterLocations.getParametrosClusterLocations().setPlanejaDP(true);
        AtomicInteger quantidadeLeiturasComFetch = new AtomicInteger();

        ParametrosFacade parametrosFrontService = new ParametrosFacade();
        setPrivateField(
                parametrosFrontService,
                "clusterLocationsRepository",
                getClusterLocationsRepositoryComSnapshotFetch(
                        quantidadeLeiturasComFetch,
                        List.of(clusterLocations)));

        List<ParametroClusterLocationDTO> parametroClusterLocationDTOList =
                parametrosFrontService.getParametroClusterLocationDTO();

        Assertions.assertEquals(1, quantidadeLeiturasComFetch.get());
        Assertions.assertEquals(1, parametroClusterLocationDTOList.size());
        Assertions.assertEquals(10L, parametroClusterLocationDTOList.getFirst().getClusterLocationsID());
        Assertions.assertTrue(parametroClusterLocationDTOList.getFirst().getPlanejaDP());

    }

    @Test
    public void saveParametroClusterLocationDTOShouldRejectMissingPayloadBeforeRepository() {

        ParametrosFacade parametrosFrontService = new ParametrosFacade();

        /*
         * DTO nulo ou sem cluster nao forma chave de parametros. A validacao
         * precisa ocorrer antes dos repositories, que nao sao injetados neste
         * teste propositalmente.
         */
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> parametrosFrontService.saveParametroClusterLocationDTO(null));
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> parametrosFrontService.saveParametroClusterLocationDTO(new ParametroClusterLocationDTO()));

    }

    @Test
    public void saveParametroClusterLocationDTOShouldRejectPricingParametersCommunity() {

        ParametrosFacade parametrosFrontService = new ParametrosFacade();

        ParametroClusterLocationDTO parametroClusterLocationDTO = new ParametroClusterLocationDTO();
        parametroClusterLocationDTO.setPlanejaPricing(true);

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> parametrosFrontService.saveParametroClusterLocationDTO(parametroClusterLocationDTO));

    }

    @Test
    public void saveParametroClusterLocationDTOShouldReturnFalseWhenSavedSnapshotIsBroken()
            throws Exception {

        ParametroClusterLocationDTO parametroClusterLocationDTO = new ParametroClusterLocationDTO();
        parametroClusterLocationDTO.setClusterLocationsID(10L);
        parametroClusterLocationDTO.setPlanejaDP(true);

        ParametrosFacade parametrosFrontServiceComSnapshotNulo =
                criaParametrosFrontServiceParaSaveParametroClusterLocation(null);

        Assertions.assertFalse(
                parametrosFrontServiceComSnapshotNulo.saveParametroClusterLocationDTO(
                        parametroClusterLocationDTO));

        ParametrosFacade parametrosFrontServiceComSnapshotSemChave =
                criaParametrosFrontServiceParaSaveParametroClusterLocation(
                        new ParametrosClusterLocations());

        Assertions.assertFalse(
                parametrosFrontServiceComSnapshotSemChave.saveParametroClusterLocationDTO(
                        parametroClusterLocationDTO));

    }

    @Test
    public void saveParametroClusterLocationDTOShouldReturnFalseWhenClusterRepositoryReturnsNullOptional()
            throws Exception {

        ParametroClusterLocationDTO parametroClusterLocationDTO = new ParametroClusterLocationDTO();
        parametroClusterLocationDTO.setClusterLocationsID(10L);
        parametroClusterLocationDTO.setPlanejaDP(true);

        ParametrosFacade parametrosFrontService = new ParametrosFacade();
        setPrivateField(
                parametrosFrontService,
                "clusterLocationsRepository",
                getClusterLocationsRepositoryComFindByIdRetornando(null));

        /*
         * Optional.empty() representa cluster inexistente; Optional nulo e
         * repository quebrado. Ambos preservam o contrato publico false, mas
         * nenhum deles deve chegar ao save dos parametros.
         */
        Assertions.assertFalse(
                parametrosFrontService.saveParametroClusterLocationDTO(
                        parametroClusterLocationDTO));

    }

    @Test
    public void saveParametrosMaterialDTOShouldRejectMissingPayloadBeforeRepository() {

        ParametrosFacade parametrosFrontService = new ParametrosFacade();

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> parametrosFrontService.saveParametrosMaterialDTO(null));

        ParametrosMaterialDTO parametrosMaterialDTO = new ParametrosMaterialDTO();
        parametrosMaterialDTO.setId(" ");
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> parametrosFrontService.saveParametrosMaterialDTO(parametrosMaterialDTO));

    }

    @Test
    public void saveParametrosMaterialDTOShouldReturnFalseWhenMaterialDoesNotExist() throws Exception {

        ParametrosFacade parametrosFrontService = new ParametrosFacade();
        setPrivateField(
                parametrosFrontService,
                "produtoRepository",
                getProdutoRepositoryVazio());

        ParametrosMaterialDTO parametrosMaterialDTO = new ParametrosMaterialDTO();
        parametrosMaterialDTO.setId("MAT-404");

        /*
         * Os saves administrativos desta service retornam booleano. Material
         * inexistente continua retornando false, mas agora a causa interna e
         * uma falha funcional explicita em vez de um Optional.get ou no-op true.
         */
        Assertions.assertFalse(parametrosFrontService.saveParametrosMaterialDTO(parametrosMaterialDTO));

    }

    @Test
    public void saveParametrosMaterialDTOShouldReturnFalseWhenMaterialRepositoryReturnsNullOptional()
            throws Exception {

        ParametrosFacade parametrosFrontService = new ParametrosFacade();
        setPrivateField(
                parametrosFrontService,
                "produtoRepository",
                getProdutoRepositoryComFindByIdRetornando(null));

        ParametrosMaterialDTO parametrosMaterialDTO = new ParametrosMaterialDTO();
        parametrosMaterialDTO.setId("MAT-01");

        Assertions.assertFalse(
                parametrosFrontService.saveParametrosMaterialDTO(parametrosMaterialDTO));

    }

    @Test
    public void saveParametrosMaterialDTOShouldReturnFalseWhenSavedSnapshotIsBroken() throws Exception {

        ParametrosMaterialDTO parametrosMaterialDTO = new ParametrosMaterialDTO();
        parametrosMaterialDTO.setId("MAT-01");
        parametrosMaterialDTO.setDescricao("Material 01");
        parametrosMaterialDTO.setAtivo(true);

        ParametrosFacade parametrosFrontServiceComSnapshotNulo = new ParametrosFacade();
        setPrivateField(
                parametrosFrontServiceComSnapshotNulo,
                "produtoRepository",
                getProdutoRepositoryComSaveRetornando(null));

        Assertions.assertFalse(
                parametrosFrontServiceComSnapshotNulo.saveParametrosMaterialDTO(parametrosMaterialDTO));

        Produto produtoSalvoSemId = new Produto();
        produtoSalvoSemId.setId(" ");
        ParametrosFacade parametrosFrontServiceComSnapshotSemId = new ParametrosFacade();
        setPrivateField(
                parametrosFrontServiceComSnapshotSemId,
                "produtoRepository",
                getProdutoRepositoryComSaveRetornando(produtoSalvoSemId));

        Assertions.assertFalse(
                parametrosFrontServiceComSnapshotSemId.saveParametrosMaterialDTO(parametrosMaterialDTO));

    }

    @Test
    public void getParametrosMaterialLocationShouldRejectBlankLocationBeforeService() {

        ParametrosFacade parametrosFrontService = new ParametrosFacade();

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> parametrosFrontService.getParametrosMaterialLocation(" "));

    }

    @Test
    public void saveParametrosMaterialLocationDTOShouldRejectMissingPayloadBeforeRepository() {

        ParametrosFacade parametrosFrontService = new ParametrosFacade();

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> parametrosFrontService.saveParametrosMaterialLocationDTO(null));

        ParametrosMaterialLocationDTO parametrosMaterialLocationDTOWithoutLocation = new ParametrosMaterialLocationDTO();
        parametrosMaterialLocationDTOWithoutLocation.setMaterialID("MAT-01");
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> parametrosFrontService.saveParametrosMaterialLocationDTO(
                        parametrosMaterialLocationDTOWithoutLocation));

        ParametrosMaterialLocationDTO parametrosMaterialLocationDTOWithoutMaterial = new ParametrosMaterialLocationDTO();
        parametrosMaterialLocationDTOWithoutMaterial.setLocationID("LOC-01");
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> parametrosFrontService.saveParametrosMaterialLocationDTO(
                        parametrosMaterialLocationDTOWithoutMaterial));

    }

    @Test
    public void saveParametrosMaterialLocationDTOShouldRejectNegativeFrozenDemandPlanningHorizonBeforeRepository() {

        ParametrosFacade parametrosFrontService = new ParametrosFacade();
        ParametrosMaterialLocationDTO parametrosMaterialLocationDTO =
                getParametrosMaterialLocationDTO("MAT-01", "LOC-01");
        parametrosMaterialLocationDTO.setFrozenHorizonDpInDays(-1);

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> parametrosFrontService.saveParametrosMaterialLocationDTO(
                        parametrosMaterialLocationDTO));

    }

    @Test
    public void saveParametrosMaterialLocationDTOShouldPersistFrozenDemandPlanningHorizonOverride()
            throws Exception {

        Produto produto = new Produto("MAT-01");
        Location location = new Location("LOC-01");
        ParametrosProdutoLocation parametrosProdutoLocation = new ParametrosProdutoLocation(
                new ParametrosProdutoLocation.ParametrosProdutoLocationCompositeKey(produto, location));
        ParametrosFacade parametrosFrontService = new ParametrosFacade();
        setPrivateField(parametrosFrontService, "locationService", new LocationServiceWithLocation(location));
        setPrivateField(parametrosFrontService, "materialService", new MaterialServiceWithProduto(produto));
        setPrivateField(
                parametrosFrontService,
                "parametrosProdutoLocationRepository",
                getParametrosProdutoLocationRepositoryComFindAndSave(parametrosProdutoLocation));

        ParametrosMaterialLocationDTO parametrosMaterialLocationDTO =
                getParametrosMaterialLocationDTO("MAT-01", "LOC-01");
        parametrosMaterialLocationDTO.setFrozenHorizonDpInDays(21);

        Assertions.assertTrue(
                parametrosFrontService.saveParametrosMaterialLocationDTO(
                        parametrosMaterialLocationDTO));
        Assertions.assertEquals(21, parametrosProdutoLocation.getNumeroDiasHorizonteCongeladoDpCadastrado());

    }

    @Test
    public void getParametrosMaterialLocationShouldExposeConfiguredFrozenDemandPlanningHorizonOrNull()
            throws Exception {

        Produto produto = new Produto("MAT-01");
        Location location = new Location("LOC-01");
        ParametrosProdutoLocation parametrosProdutoLocation = new ParametrosProdutoLocation(
                new ParametrosProdutoLocation.ParametrosProdutoLocationCompositeKey(produto, location));
        parametrosProdutoLocation.setNumeroDiasHorizonteCongeladoDp(14);
        location.setMapaParametrosProdutoLocation(Map.of(produto, parametrosProdutoLocation));

        ParametrosFacade parametrosFrontServiceComOverride = new ParametrosFacade();
        setPrivateField(parametrosFrontServiceComOverride, "locationService", new LocationServiceWithLocation(location));
        setPrivateField(
                parametrosFrontServiceComOverride,
                "produtoRepository",
                getProdutoRepositoryComMateriais(List.of(produto)));
        setPrivateField(
                parametrosFrontServiceComOverride,
                "parametrosProdutoLocationRepository",
                getParametrosProdutoLocationRepositoryComFetch(List.of(parametrosProdutoLocation)));

        Assertions.assertEquals(
                14,
                parametrosFrontServiceComOverride.getParametrosMaterialLocation("LOC-01")
                        .getFirst()
                        .getFrozenHorizonDpInDays());

        Location locationSemOverride = new Location("LOC-02");
        ParametrosFacade parametrosFrontServiceSemOverride = new ParametrosFacade();
        setPrivateField(
                parametrosFrontServiceSemOverride,
                "locationService",
                new LocationServiceWithLocation(locationSemOverride));
        setPrivateField(
                parametrosFrontServiceSemOverride,
                "produtoRepository",
                getProdutoRepositoryComMateriais(List.of(produto)));
        setPrivateField(
                parametrosFrontServiceSemOverride,
                "parametrosProdutoLocationRepository",
                getParametrosProdutoLocationRepositoryComFetch(List.of()));

        Assertions.assertNull(
                parametrosFrontServiceSemOverride.getParametrosMaterialLocation("LOC-02")
                        .getFirst()
                        .getFrozenHorizonDpInDays());

    }

    @Test
    public void materialLocationUomOverridesShouldRoundTripUsingOneBatchResolution() throws Exception {

        Produto produto = new Produto("MAT-01");
        Location location = new Location("LOC-01");
        ParametrosProdutoLocation parametrosProdutoLocation = new ParametrosProdutoLocation(
                new ParametrosProdutoLocation.ParametrosProdutoLocationCompositeKey(produto, location));
        UnidadeMedida unidadeMedidaPadrao = new UnidadeMedida("UN");
        UnidadeMedida unidadeMedidaProducao = new UnidadeMedida("CX");
        AtomicInteger quantidadeLeiturasEmLote = new AtomicInteger();

        ParametrosFacade parametrosFrontService = new ParametrosFacade();
        setPrivateField(parametrosFrontService, "locationService", new LocationServiceWithLocation(location));
        setPrivateField(parametrosFrontService, "materialService", new MaterialServiceWithProduto(produto));
        setPrivateField(
                parametrosFrontService,
                "parametrosProdutoLocationRepository",
                getParametrosProdutoLocationRepositoryComFindAndSave(parametrosProdutoLocation));
        setPrivateField(
                parametrosFrontService,
                "unidadeMedidaRepository",
                getUnidadeMedidaRepositoryComBatch(
                        quantidadeLeiturasEmLote,
                        List.of(unidadeMedidaPadrao, unidadeMedidaProducao)));

        ParametrosMaterialLocationDTO parametrosMaterialLocationDTO =
                getParametrosMaterialLocationDTO("MAT-01", "LOC-01");
        parametrosMaterialLocationDTO.setDefaultUomId("UN");
        parametrosMaterialLocationDTO.setProductionMinimumMultipleUomId("CX");

        Assertions.assertTrue(parametrosFrontService.saveParametrosMaterialLocationDTO(
                parametrosMaterialLocationDTO));
        Assertions.assertSame(
                unidadeMedidaPadrao,
                parametrosProdutoLocation.getUnidadeMedidaPadraoCadastrado());
        Assertions.assertSame(
                unidadeMedidaProducao,
                parametrosProdutoLocation.getUnidadeMedidaLoteMinimoMultiploProducaoCadastrado());
        Assertions.assertEquals(
                1,
                quantidadeLeiturasEmLote.get(),
                "Os dois ids de unidade devem ser resolvidos pela mesma leitura em lote.");

        location.setMapaParametrosProdutoLocation(Map.of(produto, parametrosProdutoLocation));
        setPrivateField(
                parametrosFrontService,
                "produtoRepository",
                getProdutoRepositoryComMateriais(List.of(produto)));

        ParametrosMaterialLocationDTO parametrosMaterialLocationDtoRetornado =
                parametrosFrontService.getParametrosMaterialLocation("LOC-01").getFirst();
        Assertions.assertEquals("UN", parametrosMaterialLocationDtoRetornado.getDefaultUomId());
        Assertions.assertEquals(
                "CX",
                parametrosMaterialLocationDtoRetornado.getProductionMinimumMultipleUomId());

    }

    @Test
    public void materialLocationUomOverridesShouldAllowExplicitCleanupWithoutUnitQuery()
            throws Exception {

        Produto produto = new Produto("MAT-01");
        Location location = new Location("LOC-01");
        ParametrosProdutoLocation parametrosProdutoLocation = new ParametrosProdutoLocation(
                new ParametrosProdutoLocation.ParametrosProdutoLocationCompositeKey(produto, location));
        parametrosProdutoLocation.setUnidadeMedidaPadrao(new UnidadeMedida("UN"));
        parametrosProdutoLocation.setUnidadeMedidaLoteMinimoMultiploProducao(new UnidadeMedida("CX"));

        ParametrosFacade parametrosFrontService = new ParametrosFacade();
        setPrivateField(parametrosFrontService, "locationService", new LocationServiceWithLocation(location));
        setPrivateField(parametrosFrontService, "materialService", new MaterialServiceWithProduto(produto));
        setPrivateField(
                parametrosFrontService,
                "parametrosProdutoLocationRepository",
                getParametrosProdutoLocationRepositoryComFindAndSave(parametrosProdutoLocation));

        Assertions.assertTrue(parametrosFrontService.saveParametrosMaterialLocationDTO(
                getParametrosMaterialLocationDTO("MAT-01", "LOC-01")));
        Assertions.assertNull(parametrosProdutoLocation.getUnidadeMedidaPadraoCadastrado());
        Assertions.assertNull(
                parametrosProdutoLocation.getUnidadeMedidaLoteMinimoMultiploProducaoCadastrado());

    }

    @Test
    public void materialLocationLifecycleShouldRoundTripAndLifecycleStageShouldTakePrecedence()
            throws Exception {

        Produto produto = new Produto("MAT-01");
        Location location = new Location("LOC-01");
        ParametrosProdutoLocation parametrosProdutoLocation = new ParametrosProdutoLocation(
                new ParametrosProdutoLocation.ParametrosProdutoLocationCompositeKey(produto, location));
        LocalDateTime dataIntroducao = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime dataDescontinuacao = LocalDateTime.of(2026, 12, 31, 0, 0);

        ParametrosFacade parametrosFrontService = new ParametrosFacade();
        setPrivateField(parametrosFrontService, "locationService", new LocationServiceWithLocation(location));
        setPrivateField(parametrosFrontService, "materialService", new MaterialServiceWithProduto(produto));
        setPrivateField(
                parametrosFrontService,
                "parametrosProdutoLocationRepository",
                getParametrosProdutoLocationRepositoryComFindAndSave(parametrosProdutoLocation));

        ParametrosMaterialLocationDTO parametrosMaterialLocationDTO =
                getParametrosMaterialLocationDTO("MAT-01", "LOC-01");
        parametrosMaterialLocationDTO.setLifecycleStage(Constantes.StatusProduto.DESCONTINUADO);
        parametrosMaterialLocationDTO.setIntroductionDate(dataIntroducao);
        parametrosMaterialLocationDTO.setDiscontinuationDate(dataDescontinuacao);

        Assertions.assertTrue(parametrosFrontService.saveParametrosMaterialLocationDTO(parametrosMaterialLocationDTO));
        Assertions.assertEquals(
                Constantes.StatusProduto.DESCONTINUADO,
                parametrosProdutoLocation.getEstagioCicloVidaCadastrado());
        Assertions.assertEquals(dataIntroducao, parametrosProdutoLocation.getDataIntroducao());
        Assertions.assertEquals(dataDescontinuacao, parametrosProdutoLocation.getDataDescontinuacao());
        Assertions.assertEquals(
                Constantes.StatusProduto.DESCONTINUADO,
                parametrosProdutoLocation.getStatusProduto(LocalDateTime.of(2026, 1, 1, 0, 0), null));

        location.setMapaParametrosProdutoLocation(Map.of(produto, parametrosProdutoLocation));
        setPrivateField(
                parametrosFrontService,
                "produtoRepository",
                getProdutoRepositoryComMateriais(List.of(produto)));

        ParametrosMaterialLocationDTO parametrosMaterialLocationDtoRetornado =
                parametrosFrontService.getParametrosMaterialLocation("LOC-01").getFirst();
        Assertions.assertEquals(
                Constantes.StatusProduto.DESCONTINUADO,
                parametrosMaterialLocationDtoRetornado.getLifecycleStage());
        Assertions.assertEquals(dataIntroducao, parametrosMaterialLocationDtoRetornado.getIntroductionDate());
        Assertions.assertEquals(dataDescontinuacao, parametrosMaterialLocationDtoRetornado.getDiscontinuationDate());

    }

    @Test
    public void productionMinimumAndMultipleShouldPersistAndReadFromFetchedRepositoryIndex()
            throws Exception {

        Produto produto = new Produto("MAT-01");
        Location location = new Location("LOC-01");
        ParametrosProdutoLocation parametrosProdutoLocation = new ParametrosProdutoLocation(
                new ParametrosProdutoLocation.ParametrosProdutoLocationCompositeKey(produto, location));
        AtomicInteger quantidadeLeiturasComFetch = new AtomicInteger();

        ParametrosFacade parametrosFrontService = new ParametrosFacade();
        setPrivateField(parametrosFrontService, "locationService", new LocationServiceWithLocation(location));
        setPrivateField(parametrosFrontService, "materialService", new MaterialServiceWithProduto(produto));
        setPrivateField(
                parametrosFrontService,
                "produtoRepository",
                getProdutoRepositoryComMateriais(List.of(produto)));
        setPrivateField(
                parametrosFrontService,
                "parametrosProdutoLocationRepository",
                getParametrosProdutoLocationRepositoryComFindSaveAndFetch(
                        parametrosProdutoLocation,
                        quantidadeLeiturasComFetch));

        ParametrosMaterialLocationDTO parametrosMaterialLocationDTO =
                getParametrosMaterialLocationDTO("MAT-01", "LOC-01");
        parametrosMaterialLocationDTO.setProductionMinimumQuantity(20.0d);
        parametrosMaterialLocationDTO.setProductionMultipleQuantity(5.0d);

        Assertions.assertTrue(parametrosFrontService.saveParametrosMaterialLocationDTO(
                parametrosMaterialLocationDTO));
        Assertions.assertEquals(20.0d, parametrosProdutoLocation.getLoteMinimoProducaoCadastrado());
        Assertions.assertEquals(5.0d, parametrosProdutoLocation.getMultiploProducaoCadastrado());

        ParametrosMaterialLocationDTO parametrosRetornados =
                parametrosFrontService.getParametrosMaterialLocation("LOC-01").getFirst();
        Assertions.assertEquals(20.0d, parametrosRetornados.getProductionMinimumQuantity());
        Assertions.assertEquals(5.0d, parametrosRetornados.getProductionMultipleQuantity());
        Assertions.assertEquals(
                1,
                quantidadeLeiturasComFetch.get(),
                "O GET administrativo deve buscar parâmetros uma vez, sem consultar o mapa LAZY por par.");

    }

    @Test
    public void saveParametrosMaterialLocationDTOShouldRejectInvalidProductionQuantitiesBeforeRepository() {

        ParametrosFacade parametrosFrontService = new ParametrosFacade();
        ParametrosMaterialLocationDTO parametrosMaterialLocationDTO =
                getParametrosMaterialLocationDTO("MAT-01", "LOC-01");

        parametrosMaterialLocationDTO.setProductionMinimumQuantity(-0.1d);
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> parametrosFrontService.saveParametrosMaterialLocationDTO(
                        parametrosMaterialLocationDTO));

        parametrosMaterialLocationDTO.setProductionMinimumQuantity(0.0d);
        parametrosMaterialLocationDTO.setProductionMultipleQuantity(0.0d);
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> parametrosFrontService.saveParametrosMaterialLocationDTO(
                        parametrosMaterialLocationDTO));

        parametrosMaterialLocationDTO.setProductionMultipleQuantity(Double.NaN);
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> parametrosFrontService.saveParametrosMaterialLocationDTO(
                        parametrosMaterialLocationDTO));

    }

    @Test
    public void materialLocationLifecycleShouldAllowExplicitCleanupOfAllOverrides() throws Exception {

        Produto produto = new Produto("MAT-01");
        Location location = new Location("LOC-01");
        ParametrosProdutoLocation parametrosProdutoLocation = new ParametrosProdutoLocation(
                new ParametrosProdutoLocation.ParametrosProdutoLocationCompositeKey(produto, location));
        parametrosProdutoLocation.setEstagioCicloVida(Constantes.StatusProduto.NOVO);
        parametrosProdutoLocation.setDataIntroducao(LocalDateTime.of(2026, 1, 10, 0, 0));
        parametrosProdutoLocation.setDataDescontinuacao(LocalDateTime.of(2026, 12, 31, 0, 0));

        ParametrosFacade parametrosFrontService = new ParametrosFacade();
        setPrivateField(parametrosFrontService, "locationService", new LocationServiceWithLocation(location));
        setPrivateField(parametrosFrontService, "materialService", new MaterialServiceWithProduto(produto));
        setPrivateField(
                parametrosFrontService,
                "parametrosProdutoLocationRepository",
                getParametrosProdutoLocationRepositoryComFindAndSave(parametrosProdutoLocation));

        ParametrosMaterialLocationDTO parametrosMaterialLocationDTO =
                getParametrosMaterialLocationDTO("MAT-01", "LOC-01");

        Assertions.assertTrue(parametrosFrontService.saveParametrosMaterialLocationDTO(parametrosMaterialLocationDTO));
        Assertions.assertNull(parametrosProdutoLocation.getEstagioCicloVidaCadastrado());
        Assertions.assertNull(parametrosProdutoLocation.getDataIntroducao());
        Assertions.assertNull(parametrosProdutoLocation.getDataDescontinuacao());

    }

    @Test
    public void saveParametrosMaterialLocationDTOShouldReturnFalseWhenSavedSnapshotIsBroken()
            throws Exception {

        ParametrosMaterialLocationDTO parametrosMaterialLocationDTO =
                new ParametrosMaterialLocationDTO();
        parametrosMaterialLocationDTO.setMaterialID("MAT-01");
        parametrosMaterialLocationDTO.setLocationID("LOC-01");

        ParametrosFacade parametrosFrontServiceComSnapshotNulo =
                criaParametrosFrontServiceParaSaveParametrosMaterialLocation(null);

        Assertions.assertFalse(
                parametrosFrontServiceComSnapshotNulo.saveParametrosMaterialLocationDTO(
                        parametrosMaterialLocationDTO));

        ParametrosFacade parametrosFrontServiceComSnapshotSemChave =
                criaParametrosFrontServiceParaSaveParametrosMaterialLocation(
                        new ParametrosProdutoLocation());

        Assertions.assertFalse(
                parametrosFrontServiceComSnapshotSemChave.saveParametrosMaterialLocationDTO(
                        parametrosMaterialLocationDTO));

    }

    @Test
    public void saveParametrosMaterialLocationDTOShouldReturnFalseWhenRepositoryReturnsNullOptional()
            throws Exception {

        ParametrosMaterialLocationDTO parametrosMaterialLocationDTO =
                new ParametrosMaterialLocationDTO();
        parametrosMaterialLocationDTO.setMaterialID("MAT-01");
        parametrosMaterialLocationDTO.setLocationID("LOC-01");

        ParametrosFacade parametrosFrontService = new ParametrosFacade();
        setPrivateField(
                parametrosFrontService,
                "locationService",
                new LocationServiceStub());
        setPrivateField(
                parametrosFrontService,
                "materialService",
                new MaterialServiceStub());
        setPrivateField(
                parametrosFrontService,
                "parametrosProdutoLocationRepository",
                getParametrosProdutoLocationRepositoryComFindByIdRetornando(null));

        Assertions.assertFalse(
                parametrosFrontService.saveParametrosMaterialLocationDTO(
                        parametrosMaterialLocationDTO));

    }

    private static ProdutoRepository getProdutoRepositoryVazio() {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("findById".equals(method.getName())) {
                return Optional.empty();
            }
            if ("toString".equals(method.getName())) {
                return "ProdutoRepository vazio para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (ProdutoRepository) Proxy.newProxyInstance(
                ProdutoRepository.class.getClassLoader(),
                new Class<?>[]{ProdutoRepository.class},
                invocationHandler);

    }

    private static ProdutoRepository getProdutoRepositoryComFindByIdRetornando(
            Optional<Produto> optionalProduto) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("findById".equals(method.getName())) {
                return optionalProduto;
            }
            if ("toString".equals(method.getName())) {
                return "ProdutoRepository findById controlado para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (ProdutoRepository) Proxy.newProxyInstance(
                ProdutoRepository.class.getClassLoader(),
                new Class<?>[]{ProdutoRepository.class},
                invocationHandler);

    }

    private static ProdutoRepository getProdutoRepositoryComMateriais(List<Produto> produtoList) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("findAll".equals(method.getName())) {
                return produtoList;
            }
            if ("toString".equals(method.getName())) {
                return "ProdutoRepository com materiais para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (ProdutoRepository) Proxy.newProxyInstance(
                ProdutoRepository.class.getClassLoader(),
                new Class<?>[]{ProdutoRepository.class},
                invocationHandler);

    }

    private static ParametrosFacade criaParametrosFrontServiceParaSaveParametroClusterLocation(
            ParametrosClusterLocations parametrosClusterLocationsSalvos) throws Exception {

        ParametrosFacade parametrosFrontService = new ParametrosFacade();
        setPrivateField(
                parametrosFrontService,
                "clusterLocationsRepository",
                getClusterLocationsRepositoryComParametro());
        setPrivateField(
                parametrosFrontService,
                "parametrosClusterLocationsRepository",
                getParametrosClusterLocationsRepositoryComSaveRetornando(parametrosClusterLocationsSalvos));
        return parametrosFrontService;

    }

    private static ClusterLocationsRepository getClusterLocationsRepositoryComParametro() {

        ClusterLocations clusterLocations = new ClusterLocations();
        clusterLocations.setId(10L);
        clusterLocations.setDescricao("Cluster Location 10");
        clusterLocations.getParametrosClusterLocations();

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("findById".equals(method.getName())) {
                return Optional.of(clusterLocations);
            }
            if ("toString".equals(method.getName())) {
                return "ClusterLocationsRepository com parametros para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (ClusterLocationsRepository) Proxy.newProxyInstance(
                ClusterLocationsRepository.class.getClassLoader(),
                new Class<?>[]{ClusterLocationsRepository.class},
                invocationHandler);

    }

    /**
     * Simula a fotografia administrativa que ja busca os parametros do
     * cluster. A falha explicita para `findAll` protege o service contra a
     * reintroducao do caminho que causaria N+1 em clusters persistidos.
     */
    private static ClusterLocationsRepository getClusterLocationsRepositoryComSnapshotFetch(
            AtomicInteger quantidadeLeiturasComFetch,
            List<ClusterLocations> clusterLocationsList) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("customFindAll".equals(method.getName())) {
                quantidadeLeiturasComFetch.incrementAndGet();
                return clusterLocationsList;
            }
            if ("findAll".equals(method.getName())) {
                throw new AssertionError(
                        "A listagem de parametros de cluster deve usar customFindAll com fetch.");
            }
            if ("toString".equals(method.getName())) {
                return "ClusterLocationsRepository com snapshot fetch para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (ClusterLocationsRepository) Proxy.newProxyInstance(
                ClusterLocationsRepository.class.getClassLoader(),
                new Class<?>[]{ClusterLocationsRepository.class},
                invocationHandler);

    }

    private static ClusterLocationsRepository getClusterLocationsRepositoryComFindByIdRetornando(
            Optional<ClusterLocations> optionalClusterLocations) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("findById".equals(method.getName())) {
                return optionalClusterLocations;
            }
            if ("toString".equals(method.getName())) {
                return "ClusterLocationsRepository findById controlado para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (ClusterLocationsRepository) Proxy.newProxyInstance(
                ClusterLocationsRepository.class.getClassLoader(),
                new Class<?>[]{ClusterLocationsRepository.class},
                invocationHandler);

    }

    private static ParametrosClusterLocationsRepository getParametrosClusterLocationsRepositoryComSaveRetornando(
            ParametrosClusterLocations parametrosClusterLocationsSalvos) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("save".equals(method.getName())) {
                return parametrosClusterLocationsSalvos;
            }
            if ("toString".equals(method.getName())) {
                return "ParametrosClusterLocationsRepository save quebrado para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (ParametrosClusterLocationsRepository) Proxy.newProxyInstance(
                ParametrosClusterLocationsRepository.class.getClassLoader(),
                new Class<?>[]{ParametrosClusterLocationsRepository.class},
                invocationHandler);

    }

    private static ProdutoRepository getProdutoRepositoryComSaveRetornando(Produto produtoSalvo) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("findById".equals(method.getName())) {
                return Optional.of(new Produto((String) args[0]));
            }
            if ("save".equals(method.getName())) {
                return produtoSalvo;
            }
            if ("toString".equals(method.getName())) {
                return "ProdutoRepository save quebrado para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (ProdutoRepository) Proxy.newProxyInstance(
                ProdutoRepository.class.getClassLoader(),
                new Class<?>[]{ProdutoRepository.class},
                invocationHandler);

    }

    private static ParametrosFacade criaParametrosFrontServiceParaSaveParametrosMaterialLocation(
            ParametrosProdutoLocation parametrosProdutoLocationSalvo) throws Exception {

        ParametrosFacade parametrosFrontService = new ParametrosFacade();
        setPrivateField(
                parametrosFrontService,
                "locationService",
                new LocationServiceStub());
        setPrivateField(
                parametrosFrontService,
                "materialService",
                new MaterialServiceStub());
        setPrivateField(
                parametrosFrontService,
                "parametrosProdutoLocationRepository",
                getParametrosProdutoLocationRepositoryComSaveRetornando(parametrosProdutoLocationSalvo));
        return parametrosFrontService;

    }

    private static ParametrosProdutoLocationRepository getParametrosProdutoLocationRepositoryComSaveRetornando(
            ParametrosProdutoLocation parametrosProdutoLocationSalvo) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("findById".equals(method.getName())) {
                return Optional.empty();
            }
            if ("save".equals(method.getName())) {
                return parametrosProdutoLocationSalvo;
            }
            if ("toString".equals(method.getName())) {
                return "ParametrosProdutoLocationRepository save quebrado para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (ParametrosProdutoLocationRepository) Proxy.newProxyInstance(
                ParametrosProdutoLocationRepository.class.getClassLoader(),
                new Class<?>[]{ParametrosProdutoLocationRepository.class},
                invocationHandler);

    }

    private static ParametrosProdutoLocationRepository getParametrosProdutoLocationRepositoryComFindByIdRetornando(
            Optional<ParametrosProdutoLocation> optionalParametrosProdutoLocation) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("findById".equals(method.getName())) {
                return optionalParametrosProdutoLocation;
            }
            if ("toString".equals(method.getName())) {
                return "ParametrosProdutoLocationRepository findById controlado para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (ParametrosProdutoLocationRepository) Proxy.newProxyInstance(
                ParametrosProdutoLocationRepository.class.getClassLoader(),
                new Class<?>[]{ParametrosProdutoLocationRepository.class},
                invocationHandler);

    }

    private static ParametrosProdutoLocationRepository getParametrosProdutoLocationRepositoryComFindAndSave(
            ParametrosProdutoLocation parametrosProdutoLocation) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("findById".equals(method.getName())) {
                return Optional.of(parametrosProdutoLocation);
            }
            if ("save".equals(method.getName())) {
                return args[0];
            }
            if ("customFindAllComFetchAtributosManyToOne".equals(method.getName())) {
                return List.of(parametrosProdutoLocation);
            }
            if ("toString".equals(method.getName())) {
                return "ParametrosProdutoLocationRepository para roundtrip Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (ParametrosProdutoLocationRepository) Proxy.newProxyInstance(
                ParametrosProdutoLocationRepository.class.getClassLoader(),
                new Class<?>[]{ParametrosProdutoLocationRepository.class},
                invocationHandler);

    }

    private static ParametrosProdutoLocationRepository getParametrosProdutoLocationRepositoryComFetch(
            List<ParametrosProdutoLocation> parametrosProdutoLocationList) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("customFindAllComFetchAtributosManyToOne".equals(method.getName())) {
                return parametrosProdutoLocationList;
            }
            if ("toString".equals(method.getName())) {
                return "ParametrosProdutoLocationRepository com fetch Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (ParametrosProdutoLocationRepository) Proxy.newProxyInstance(
                ParametrosProdutoLocationRepository.class.getClassLoader(),
                new Class<?>[]{ParametrosProdutoLocationRepository.class},
                invocationHandler);

    }

    private static ParametrosProdutoLocationRepository
    getParametrosProdutoLocationRepositoryComFindSaveAndFetch(
            ParametrosProdutoLocation parametrosProdutoLocation,
            AtomicInteger quantidadeLeiturasComFetch) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("findById".equals(method.getName())) {
                return Optional.of(parametrosProdutoLocation);
            }
            if ("save".equals(method.getName())) {
                return args[0];
            }
            if ("customFindAllComFetchAtributosManyToOne".equals(method.getName())) {
                quantidadeLeiturasComFetch.incrementAndGet();
                return List.of(parametrosProdutoLocation);
            }
            if ("toString".equals(method.getName())) {
                return "ParametrosProdutoLocationRepository com find/save/fetch Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (ParametrosProdutoLocationRepository) Proxy.newProxyInstance(
                ParametrosProdutoLocationRepository.class.getClassLoader(),
                new Class<?>[]{ParametrosProdutoLocationRepository.class},
                invocationHandler);

    }

    private static UnidadeMedidaRepository getUnidadeMedidaRepositoryComBatch(
            AtomicInteger quantidadeLeiturasEmLote,
            List<UnidadeMedida> unidadeMedidaList) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("findAllById".equals(method.getName())) {
                quantidadeLeiturasEmLote.incrementAndGet();
                Assertions.assertEquals(
                        Set.of("UN", "CX"),
                        args[0],
                        "A resolucao deve deduplicar os dois ids no mesmo batch.");
                return unidadeMedidaList;
            }
            if ("toString".equals(method.getName())) {
                return "UnidadeMedidaRepository batch para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (UnidadeMedidaRepository) Proxy.newProxyInstance(
                UnidadeMedidaRepository.class.getClassLoader(),
                new Class<?>[]{UnidadeMedidaRepository.class},
                invocationHandler);

    }

    private static class LocationServiceStub extends LocationService {

        @Override
        public Location getLocation(String locationId) {

            return new Location(locationId);

        }

    }

    private static class LocationServiceWithLocation extends LocationService {

        private final Location location;

        private LocationServiceWithLocation(Location location) {

            this.location = location;

        }

        @Override
        public Location getLocation(String locationId) {

            return location;

        }

    }

    private static class MaterialServiceStub extends MaterialService {

        @Override
        public Produto getMaterialDeId(String id) {

            return new Produto(id);

        }

    }

    private static class MaterialServiceWithProduto extends MaterialService {

        private final Produto produto;

        private MaterialServiceWithProduto(Produto produto) {

            this.produto = produto;

        }

        @Override
        public Produto getMaterialDeId(String id) {

            return produto;

        }

    }

    private static ParametrosMaterialLocationDTO getParametrosMaterialLocationDTO(
            String materialId,
            String locationId) {

        ParametrosMaterialLocationDTO parametrosMaterialLocationDTO =
                new ParametrosMaterialLocationDTO();
        parametrosMaterialLocationDTO.setMaterialID(materialId);
        parametrosMaterialLocationDTO.setLocationID(locationId);
        return parametrosMaterialLocationDTO;

    }

    private static void setPrivateField(
            Object target,
            String fieldName,
            Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

}
