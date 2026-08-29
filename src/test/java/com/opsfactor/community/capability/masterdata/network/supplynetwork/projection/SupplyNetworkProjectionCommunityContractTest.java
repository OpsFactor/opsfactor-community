package com.opsfactor.community.capability.masterdata.network.supplynetwork.projection;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporteProduto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.LinhaTransporteRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.VersaoMalhaRepository;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.repository.ListaTecnicaRepository;
import com.opsfactor.community.capability.masterdata.production.productionresource.repository.RecursoProdutivoRepository;
import com.opsfactor.community.capability.masterdata.production.routing.repository.RoteiroRepository;
import com.opsfactor.community.capability.masterdata.production.productionversion.repository.VersaoProducaoRepository;
import com.opsfactor.community.capability.masterdata.production.productionversion.service.VersaoProducaoService;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Contrato Community da projection de malha produtiva.
 *
 * <p>O Community usa apenas versoes de producao simples. A projection precisa
 * localizar a combinacao roteiro/lista tecnica coerente sem depender de
 * parallel routing/output Enterprise e deve falhar cedo quando roteiro e BOM
 * apontam para locations ou materiais diferentes.</p>
 */
class SupplyNetworkProjectionCommunityContractTest {

    @Test
    void getPrioritaryProductionVersionShouldAcceptCompatibleRoutingAndBom() {

        Location location = new Location("PLANT");
        Produto material = new Produto("FG");
        Roteiro roteiro = getRoteiro("ROUTING", location, material);
        ListaTecnica listaTecnica = getListaTecnica("BOM", location, material);
        VersaoProducao versaoProducao =
                new VersaoProducao("PV", location, 1, roteiro, listaTecnica);
        SupplyNetworkProjection supplyNetworkProjection =
                getSupplyNetworkProjection(location, material, versaoProducao);

        Optional<VersaoProducao> optionalVersaoProducao =
                supplyNetworkProjection.getVersaoProducaoViavelPrioritaria(
                        roteiro,
                        listaTecnica,
                        false,
                        null);

        Assertions.assertTrue(optionalVersaoProducao.isPresent());
        Assertions.assertSame(versaoProducao, optionalVersaoProducao.get());

    }

    @Test
    void getProductionVersionFromIdShouldMatchByValue() {

        Location location = new Location("PLANT");
        Produto material = new Produto("FG");
        VersaoProducao versaoProducao =
                new VersaoProducao(
                        "PV-1000",
                        location,
                        1,
                        getRoteiro("ROUTING", location, material),
                        getListaTecnica("BOM", location, material));
        SupplyNetworkProjection supplyNetworkProjection =
                getSupplyNetworkProjection(location, material, versaoProducao);
        String versaoProducaoIdPayload = new String("PV-1000");
        Assertions.assertNotSame(
                versaoProducao.getId(),
                versaoProducaoIdPayload);

        Optional<VersaoProducao> optionalVersaoProducao =
                supplyNetworkProjection.getVersaoProducaoFromId(
                        versaoProducaoIdPayload,
                        false);

        Assertions.assertTrue(optionalVersaoProducao.isPresent());
        Assertions.assertSame(versaoProducao, optionalVersaoProducao.get());

    }

    @Test
    void getPrioritaryProductionVersionShouldRejectRoutingAndBomWithDifferentLocation() {

        Produto material = new Produto("FG");
        Roteiro roteiro = getRoteiro("ROUTING", new Location("PLANT_A"), material);
        ListaTecnica listaTecnica = getListaTecnica("BOM", new Location("PLANT_B"), material);
        SupplyNetworkProjection supplyNetworkProjection = new SupplyNetworkProjection();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyNetworkProjection.getVersaoProducaoViavelPrioritaria(
                        roteiro,
                        listaTecnica,
                        false,
                        null));

        Assertions.assertTrue(illegalArgumentException.getMessage().contains(
                "SupplyNetworkProjection requires routing and BOM to share the same location"));
        Assertions.assertTrue(illegalArgumentException.getMessage().contains("routing ROUTING uses location PLANT_A"));
        Assertions.assertTrue(illegalArgumentException.getMessage().contains("BOM BOM uses location PLANT_B"));

    }

    @Test
    void getPrioritaryProductionVersionShouldRejectRoutingAndBomWithDifferentOutputMaterial() {

        Location location = new Location("PLANT");
        Roteiro roteiro = getRoteiro("ROUTING", location, new Produto("FG_A"));
        ListaTecnica listaTecnica = getListaTecnica("BOM", location, new Produto("FG_B"));
        SupplyNetworkProjection supplyNetworkProjection = new SupplyNetworkProjection();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyNetworkProjection.getVersaoProducaoViavelPrioritaria(
                        roteiro,
                        listaTecnica,
                        false,
                        null));

        Assertions.assertTrue(illegalArgumentException.getMessage().contains(
                "SupplyNetworkProjection requires routing and BOM to share the same output material"));
        Assertions.assertTrue(illegalArgumentException.getMessage().contains("routing ROUTING outputs material FG_A"));
        Assertions.assertTrue(illegalArgumentException.getMessage().contains("BOM BOM outputs material FG_B"));

    }

    @Test
    void getPrioritarySimpleViableProductionVersionShouldRejectRoutingAndBomWithDifferentLocation() {

        Produto material = new Produto("FG");
        Roteiro roteiro = getRoteiro("ROUTING", new Location("PLANT_A"), material);
        ListaTecnica listaTecnica = getListaTecnica("BOM", new Location("PLANT_B"), material);
        SupplyNetworkProjection supplyNetworkProjection = new SupplyNetworkProjection();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyNetworkProjection.getVersaoProducaoViavelPrioritaria(
                        roteiro,
                        listaTecnica));

        Assertions.assertTrue(illegalArgumentException.getMessage().contains(
                "SupplyNetworkProjection requires routing and BOM to share the same location"));
        Assertions.assertTrue(illegalArgumentException.getMessage().contains("routing ROUTING uses location PLANT_A"));
        Assertions.assertTrue(illegalArgumentException.getMessage().contains("BOM BOM uses location PLANT_B"));

    }

    @Test
    void getPrioritarySimpleProductionVersionShouldRejectRoutingAndBomWithDifferentOutputMaterial() {

        Location location = new Location("PLANT");
        Roteiro roteiro = getRoteiro("ROUTING", location, new Produto("FG_A"));
        ListaTecnica listaTecnica = getListaTecnica("BOM", location, new Produto("FG_B"));
        SupplyNetworkProjection supplyNetworkProjection = new SupplyNetworkProjection();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyNetworkProjection.getVersaoProducaoPrioritaria(
                        roteiro,
                        listaTecnica));

        Assertions.assertTrue(illegalArgumentException.getMessage().contains(
                "SupplyNetworkProjection requires routing and BOM to share the same output material"));
        Assertions.assertTrue(illegalArgumentException.getMessage().contains("routing ROUTING outputs material FG_A"));
        Assertions.assertTrue(illegalArgumentException.getMessage().contains("BOM BOM outputs material FG_B"));

    }

    @Test
    void getTransportationParametersShouldRejectNegativeMaterialLeadTimeOverride() {

        VersaoMalha versaoMalha =
                new VersaoMalha("NETWORK");
        Location locationOrigem =
                new Location("ORIGIN");
        Location locationDestino =
                new Location("DEST");
        Produto material =
                new Produto("MAT");
        LinhaTransporte linhaTransporte =
                new LinhaTransporte(
                        new LinhaTransporte.LinhaTransporteCompositeKey(
                                versaoMalha,
                                locationOrigem,
                                locationDestino));
        LinhaTransporteProduto linhaTransporteProduto =
                new LinhaTransporteProduto(
                        new LinhaTransporteProduto.LinhaTransporteProdutoCompositeKey(
                                linhaTransporte,
                                material));
        linhaTransporteProduto.setLeadTimeDias(-1);
        linhaTransporteProduto.setUnidadeMedidaLoteMinimoMultiploTransporte(new UnidadeMedida("PL"));

        SupplyNetworkProjection supplyNetworkProjection =
                new SupplyNetworkProjection();
        supplyNetworkProjection.mapaLinhaTransporteProdutoPorLinhaTransporteEProduto = new HashMap<>();
        supplyNetworkProjection.mapaLinhaTransporteProdutoPorLinhaTransporteEProduto
                .computeIfAbsent(versaoMalha, ignored -> new HashMap<>())
                .computeIfAbsent(linhaTransporte, ignored -> new HashMap<>())
                .put(material, linhaTransporteProduto);

        /*
         * A projection deve chamar o getter efetivo do override por material, e
         * nao o valor cadastrado bruto, para nao reintroduzir o Math.max(0, ...)
         * que a entidade deixou de aceitar.
         */
        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> supplyNetworkProjection.getParametrosLinhaTransporte(
                        linhaTransporte,
                        material,
                        null));

        Assertions.assertEquals(
                "Transportation line material lead time days must be non-negative for ORIGIN -> DEST / material MAT: -1.",
                illegalStateException.getMessage());

    }

    @Test
    void defaultRawMaterialOriginLeadTimeShouldRejectNonFiniteValue() {

        SupplyNetworkProjectionFactory supplyNetworkProjectionFactory =
                new SupplyNetworkProjectionFactory();
        VersaoMalha versaoMalha =
                new VersaoMalha("NETWORK");
        versaoMalha.setLeadTimeDiasLocationOrigemPadraoMateriasPrimas(Double.NaN);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        supplyNetworkProjectionFactory,
                        "getLeadTimeDiasInteiroLocationOrigemPadraoMateriasPrimas",
                        versaoMalha));

        Assertions.assertEquals(
                "Default raw material source lead time must be finite and non-negative for network version NETWORK: NaN.",
                illegalStateException.getMessage());

    }

    @Test
    void defaultRawMaterialOriginLeadTimeShouldDeclareNullableAbsenceContract() throws Exception {

        SupplyNetworkProjectionFactory supplyNetworkProjectionFactory =
                new SupplyNetworkProjectionFactory();
        VersaoMalha versaoMalha =
                new VersaoMalha("NETWORK");
        Method method = SupplyNetworkProjectionFactory.class.getDeclaredMethod(
                "getLeadTimeDiasInteiroLocationOrigemPadraoMateriasPrimas",
                VersaoMalha.class);

        Assertions.assertTrue(
                method.isAnnotationPresent(Nullable.class),
                "Lead time padrao de materias-primas deve declarar @Nullable quando nao cadastrado.");
        Assertions.assertNull(
                ReflectionTestUtils.invokeMethod(
                        supplyNetworkProjectionFactory,
                        "getLeadTimeDiasInteiroLocationOrigemPadraoMateriasPrimas",
                        versaoMalha));

    }

    @Test
    void populateTransportationLinesShouldRejectDuplicatedTransportationLaneBeforeIndexing() throws Exception {

        VersaoMalha versaoMalha =
                new VersaoMalha("NETWORK");
        Location locationOrigem =
                new Location("ORIGIN");
        Location locationDestino =
                new Location("DEST");
        List<LinhaTransporte> linhaTransporteList = List.of(
                new LinhaTransporte(
                        new LinhaTransporte.LinhaTransporteCompositeKey(
                                versaoMalha,
                                locationOrigem,
                                locationDestino)),
                new LinhaTransporte(
                        new LinhaTransporte.LinhaTransporteCompositeKey(
                                versaoMalha,
                                locationOrigem,
                                locationDestino)));

        SupplyNetworkProjectionFactory supplyNetworkProjectionFactory =
                new SupplyNetworkProjectionFactory();
        LinhaTransporteRepository linhaTransporteRepository =
                Mockito.mock(LinhaTransporteRepository.class);
        Mockito.when(
                        linhaTransporteRepository.findByLinhaTransporteCompositeKeyVersaoAndLinhaTransporteCompositeKeyLocationOrigemInAndLinhaTransporteCompositeKeyLocationDestinoIn(
                                Mockito.any(VersaoMalha.class),
                                Mockito.anyCollection(),
                                Mockito.anyCollection()))
                .thenReturn(linhaTransporteList);
        setPrivateField(
                supplyNetworkProjectionFactory,
                "linhaTransporteRepository",
                linhaTransporteRepository);

        SupplyNetworkProjection supplyNetworkProjection =
                getSupplyNetworkProjectionComCluster(locationOrigem, new Produto("MAT"));

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> supplyNetworkProjectionFactory.populaSupplyNetworkProjectionComLinhasTransporte(
                        supplyNetworkProjection,
                        versaoMalha));

        Assertions.assertEquals(
                "Transportation line repository returned duplicate transportation line for network version NETWORK, origin ORIGIN and destination DEST for Supply Network Projection.",
                illegalStateException.getMessage());

    }

    @Test
    void populateProductionMasterDataShouldRejectResourceWithoutIdBeforeIndexing() throws Exception {

        RecursoProdutivoRepository recursoProdutivoRepository =
                Mockito.mock(RecursoProdutivoRepository.class);
        Mockito.when(recursoProdutivoRepository.customFindByLocationIn(Mockito.anyCollection()))
                .thenReturn(List.of(new RecursoProdutivo()));
        SupplyNetworkProjectionFactory supplyNetworkProjectionFactory =
                new SupplyNetworkProjectionFactory();
        setPrivateField(
                supplyNetworkProjectionFactory,
                "recursoProdutivoRepository",
                recursoProdutivoRepository);

        SupplyNetworkProjection supplyNetworkProjection =
                getSupplyNetworkProjectionComCluster(new Location("PLANT"), new Produto("FG"));

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> supplyNetworkProjectionFactory.populaSupplyNetworkProjectionComDadosMestresProducao(
                        supplyNetworkProjection));

        Assertions.assertEquals(
                "Production resource repository returned production resource without id at index 0 for Supply Network Projection.",
                illegalStateException.getMessage());

    }

    @Test
    void populateProductionMasterDataShouldRejectDuplicatedResourceIdBeforeIndexing() throws Exception {

        List<RecursoProdutivo> recursoProdutivoList = List.of(
                RecursoProdutivo.builder().id("RES").build(),
                RecursoProdutivo.builder().id("RES").build());

        RecursoProdutivoRepository recursoProdutivoRepository =
                Mockito.mock(RecursoProdutivoRepository.class);
        Mockito.when(recursoProdutivoRepository.customFindByLocationIn(Mockito.anyCollection()))
                .thenReturn(recursoProdutivoList);
        SupplyNetworkProjectionFactory supplyNetworkProjectionFactory =
                new SupplyNetworkProjectionFactory();
        setPrivateField(
                supplyNetworkProjectionFactory,
                "recursoProdutivoRepository",
                recursoProdutivoRepository);

        SupplyNetworkProjection supplyNetworkProjection =
                getSupplyNetworkProjectionComCluster(new Location("PLANT"), new Produto("FG"));

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> supplyNetworkProjectionFactory.populaSupplyNetworkProjectionComDadosMestresProducao(
                        supplyNetworkProjection));

        Assertions.assertEquals(
                "Production resource repository returned duplicate production resource id RES for Supply Network Projection.",
                illegalStateException.getMessage());

    }

    @Test
    void populateProductionMasterDataShouldRejectBomWithoutIdBeforeIndexing() throws Exception {

        SupplyNetworkProjectionFactory supplyNetworkProjectionFactory =
                getSupplyNetworkProjectionFactoryComDadosMestresProducao(
                        List.of(),
                        List.of(new ListaTecnica()),
                        List.of());
        SupplyNetworkProjection supplyNetworkProjection =
                getSupplyNetworkProjectionComCluster(new Location("PLANT"), new Produto("FG"));

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> supplyNetworkProjectionFactory.populaSupplyNetworkProjectionComDadosMestresProducao(
                        supplyNetworkProjection));

        Assertions.assertEquals(
                "BOM repository returned bill of materials without id at index 0 for Supply Network Projection.",
                illegalStateException.getMessage());

    }

    @Test
    void populateProductionMasterDataShouldRejectDuplicatedBomIdBeforeIndexing() throws Exception {

        List<ListaTecnica> listaTecnicaList = List.of(
                getListaTecnica("BOM", new Location("PLANT"), new Produto("FG")),
                getListaTecnica("BOM", new Location("PLANT"), new Produto("FG")));
        SupplyNetworkProjectionFactory supplyNetworkProjectionFactory =
                getSupplyNetworkProjectionFactoryComDadosMestresProducao(
                        List.of(),
                        listaTecnicaList,
                        List.of());
        SupplyNetworkProjection supplyNetworkProjection =
                getSupplyNetworkProjectionComCluster(new Location("PLANT"), new Produto("FG"));

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> supplyNetworkProjectionFactory.populaSupplyNetworkProjectionComDadosMestresProducao(
                        supplyNetworkProjection));

        Assertions.assertEquals(
                "BOM repository returned duplicate bill of materials id BOM for Supply Network Projection.",
                illegalStateException.getMessage());

    }

    @Test
    void populateProductionMasterDataShouldRejectRoutingWithoutIdBeforeIndexing() throws Exception {

        SupplyNetworkProjectionFactory supplyNetworkProjectionFactory =
                getSupplyNetworkProjectionFactoryComDadosMestresProducao(
                        List.of(),
                        List.of(),
                        List.of(new Roteiro()));
        SupplyNetworkProjection supplyNetworkProjection =
                getSupplyNetworkProjectionComCluster(new Location("PLANT"), new Produto("FG"));

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> supplyNetworkProjectionFactory.populaSupplyNetworkProjectionComDadosMestresProducao(
                        supplyNetworkProjection));

        Assertions.assertEquals(
                "Routing repository returned routing without id at index 0 for Supply Network Projection.",
                illegalStateException.getMessage());

    }

    @Test
    void populateProductionMasterDataShouldRejectDuplicatedRoutingIdBeforeIndexing() throws Exception {

        List<Roteiro> roteiroList = List.of(
                getRoteiro("ROUTING", new Location("PLANT"), new Produto("FG")),
                getRoteiro("ROUTING", new Location("PLANT"), new Produto("FG")));
        SupplyNetworkProjectionFactory supplyNetworkProjectionFactory =
                getSupplyNetworkProjectionFactoryComDadosMestresProducao(
                        List.of(),
                        List.of(),
                        roteiroList);
        SupplyNetworkProjection supplyNetworkProjection =
                getSupplyNetworkProjectionComCluster(new Location("PLANT"), new Produto("FG"));

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> supplyNetworkProjectionFactory.populaSupplyNetworkProjectionComDadosMestresProducao(
                        supplyNetworkProjection));

        Assertions.assertEquals(
                "Routing repository returned duplicate routing id ROUTING for Supply Network Projection.",
                illegalStateException.getMessage());

    }

    @Test
    void populateProductionMasterDataShouldRejectSimpleProductionVersionWithoutIdBeforeIndexing() throws Exception {

        SupplyNetworkProjectionFactory supplyNetworkProjectionFactory =
                getSupplyNetworkProjectionFactoryComVersoesProducao(
                        List.of(new VersaoProducao()));
        SupplyNetworkProjection supplyNetworkProjection =
                getSupplyNetworkProjectionComCluster(new Location("PLANT"), new Produto("FG"));

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> supplyNetworkProjectionFactory.populaSupplyNetworkProjectionComDadosMestresProducao(
                        supplyNetworkProjection));

        Assertions.assertEquals(
                "Production version repository returned production version without id at index 0 for Supply Network Projection.",
                illegalStateException.getMessage());

    }

    @Test
    void populateProductionMasterDataShouldRejectDuplicatedSimpleProductionVersionIdBeforeIndexing()
            throws Exception {

        Location location = new Location("PLANT");
        Produto material = new Produto("FG");
        List<VersaoProducao> versaoProducaoList = List.of(
                new VersaoProducao(
                        "PV",
                        location,
                        1,
                        getRoteiro("ROUTING-1", location, material),
                        getListaTecnica("BOM-1", location, material)),
                new VersaoProducao(
                        "PV",
                        location,
                        2,
                        getRoteiro("ROUTING-2", location, material),
                        getListaTecnica("BOM-2", location, material)));
        SupplyNetworkProjectionFactory supplyNetworkProjectionFactory =
                getSupplyNetworkProjectionFactoryComVersoesProducao(
                        versaoProducaoList);
        SupplyNetworkProjection supplyNetworkProjection =
                getSupplyNetworkProjectionComCluster(location, material);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> supplyNetworkProjectionFactory.populaSupplyNetworkProjectionComDadosMestresProducao(
                        supplyNetworkProjection));

        Assertions.assertEquals(
                "Production version repository returned duplicate production version id PV for Supply Network Projection.",
                illegalStateException.getMessage());

    }

    @Test
    void getCompleteCachedProjectionShouldRejectNetworkVersionWithoutIdBeforeIndexing() throws Exception {

        SupplyNetworkProjectionFactory supplyNetworkProjectionFactory =
                getSupplyNetworkProjectionFactoryComVersoesMalha(List.of(new VersaoMalha()));

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                supplyNetworkProjectionFactory::getSupplyNetworkProjectionCompletoDeCache);

        Assertions.assertEquals(
                "Network version repository returned network version without id at index 0 for Supply Network Projection.",
                illegalStateException.getMessage());

    }

    @Test
    void getCompleteCachedProjectionShouldRejectDuplicatedNetworkVersionIdBeforeIndexing() throws Exception {

        SupplyNetworkProjectionFactory supplyNetworkProjectionFactory =
                getSupplyNetworkProjectionFactoryComVersoesMalha(
                        List.of(new VersaoMalha("NETWORK"), new VersaoMalha("NETWORK")));

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                supplyNetworkProjectionFactory::getSupplyNetworkProjectionCompletoDeCache);

        Assertions.assertEquals(
                "Network version repository returned duplicate network version id NETWORK for Supply Network Projection.",
                illegalStateException.getMessage());

    }

    private static SupplyNetworkProjection getSupplyNetworkProjection(
            Location location,
            Produto material,
            VersaoProducao versaoProducao) {

        SupplyNetworkProjection supplyNetworkProjection = new SupplyNetworkProjection();
        supplyNetworkProjection.mapaRoteiros = Map.of(
                versaoProducao.getRoteiro().getId(),
                versaoProducao.getRoteiro());
        supplyNetworkProjection.mapaListasTecnicas = Map.of(
                versaoProducao.getListaTecnica().getId(),
                versaoProducao.getListaTecnica());
        supplyNetworkProjection.mapaVersaoProducaoPorId = Map.of(
                versaoProducao.getId(),
                versaoProducao);
        supplyNetworkProjection.mapaVersaoProducaoViavelSetPorLocationMaterial = new HashMap<>();
        supplyNetworkProjection.mapaVersaoProducaoViavelSetPorLocationMaterial
                .computeIfAbsent(location, ignored -> new HashMap<>())
                .put(material, Set.of(versaoProducao));

        return supplyNetworkProjection;

    }

    private static SupplyNetworkProjectionFactory getSupplyNetworkProjectionFactoryComDadosMestresProducao(
            List<RecursoProdutivo> recursoProdutivoList,
            List<ListaTecnica> listaTecnicaList,
            List<Roteiro> roteiroList) throws Exception {

        SupplyNetworkProjectionFactory supplyNetworkProjectionFactory =
                new SupplyNetworkProjectionFactory();

        RecursoProdutivoRepository recursoProdutivoRepository =
                Mockito.mock(RecursoProdutivoRepository.class);
        Mockito.when(recursoProdutivoRepository.customFindByLocationIn(Mockito.anyCollection()))
                .thenReturn(recursoProdutivoList);
        ListaTecnicaRepository listaTecnicaRepository =
                Mockito.mock(ListaTecnicaRepository.class);
        Mockito.when(listaTecnicaRepository.customFindAllByLocationInAndMaterialOutputInFetchListaTecnicaComponente(
                        Mockito.anyCollection(),
                        Mockito.anyCollection()))
                .thenReturn(listaTecnicaList);
        RoteiroRepository roteiroRepository =
                Mockito.mock(RoteiroRepository.class);
        Mockito.when(roteiroRepository.customFindAllByLocationInAndMaterialOutputInFetchOperacaoRoteiroSet(
                        Mockito.anyCollection(),
                        Mockito.anyCollection()))
                .thenReturn(roteiroList);

        setPrivateField(
                supplyNetworkProjectionFactory,
                "recursoProdutivoRepository",
                recursoProdutivoRepository);
        setPrivateField(
                supplyNetworkProjectionFactory,
                "listaTecnicaRepository",
                listaTecnicaRepository);
        setPrivateField(
                supplyNetworkProjectionFactory,
                "roteiroRepository",
                roteiroRepository);

        return supplyNetworkProjectionFactory;

    }

    private static SupplyNetworkProjectionFactory getSupplyNetworkProjectionFactoryComVersoesProducao(
            List<VersaoProducao> versaoProducaoList) throws Exception {

        SupplyNetworkProjectionFactory supplyNetworkProjectionFactory =
                getSupplyNetworkProjectionFactoryComDadosMestresProducao(
                        List.of(),
                        List.of(),
                        List.of());

        VersaoProducaoService versaoProducaoService =
                Mockito.mock(VersaoProducaoService.class);
        Mockito.when(versaoProducaoService.getOuPersisteVersaoProducaoInexistente())
                .thenReturn(criaVersaoProducaoSentinela());
        VersaoProducaoRepository versaoProducaoRepository =
                Mockito.mock(VersaoProducaoRepository.class);
        Mockito.when(versaoProducaoRepository
                        .customFindAllByLocationInAndMaterialOutputIn(
                                Mockito.anyCollection(),
                                Mockito.anyCollection()))
                .thenReturn(versaoProducaoList);

        setPrivateField(
                supplyNetworkProjectionFactory,
                "versaoProducaoService",
                versaoProducaoService);
        setPrivateField(
                supplyNetworkProjectionFactory,
                "versaoProducaoRepository",
                versaoProducaoRepository);

        return supplyNetworkProjectionFactory;

    }

    private static VersaoProducao criaVersaoProducaoSentinela() {

        VersaoProducao versaoProducao = new VersaoProducao();
        versaoProducao.setId(VersaoProducao.ID_VERSAO_PRODUCAO_VAZIA);
        return versaoProducao;

    }

    private static SupplyNetworkProjectionFactory getSupplyNetworkProjectionFactoryComVersoesMalha(
            List<VersaoMalha> versaoMalhaList) throws Exception {

        SupplyNetworkProjectionFactory supplyNetworkProjectionFactory =
                new SupplyNetworkProjectionFactory();
        VersaoMalhaRepository versaoMalhaRepository =
                Mockito.mock(VersaoMalhaRepository.class);
        Mockito.when(versaoMalhaRepository.customFindAll())
                .thenReturn(versaoMalhaList);

        setPrivateField(
                supplyNetworkProjectionFactory,
                "unidadeMedidaProjectionFactory",
                new TestUnidadeMedidaProjectionFactory(getUnidadeMedidaProjectionComParametrosGlobais()));
        setPrivateField(
                supplyNetworkProjectionFactory,
                "clusterEParametrosProjectionFactory",
                new TestClusterEParametrosProjectionFactory(
                        new TestClusterEParametrosProjection(Set.of(), Set.of())));
        setPrivateField(
                supplyNetworkProjectionFactory,
                "versaoMalhaRepository",
                versaoMalhaRepository);

        return supplyNetworkProjectionFactory;

    }

    private static SupplyNetworkProjection getSupplyNetworkProjectionComCluster(
            Location location,
            Produto material) {

        SupplyNetworkProjection supplyNetworkProjection = new SupplyNetworkProjection();
        supplyNetworkProjection.clusterEParametrosProjection =
                new TestClusterEParametrosProjection(Set.of(location), Set.of(material));

        return supplyNetworkProjection;

    }

    private static Roteiro getRoteiro(String id, Location location, Produto material) {

        Roteiro roteiro = new Roteiro();
        roteiro.setId(id);
        roteiro.setLocation(location);
        roteiro.setMaterialOutput(material);

        return roteiro;

    }

    private static ListaTecnica getListaTecnica(String id, Location location, Produto material) {

        ListaTecnica listaTecnica = new ListaTecnica();
        listaTecnica.setId(id);
        listaTecnica.setLocation(location);
        listaTecnica.setMaterialOutput(material);

        return listaTecnica;

    }

    private static UnidadeMedidaProjection getUnidadeMedidaProjectionComParametrosGlobais() {

        return new TestUnidadeMedidaProjection(new ParametrosGlobais());

    }

    private static void setPrivateField(
            Object target,
            String fieldName,
            Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

    private static class TestUnidadeMedidaProjectionFactory extends UnidadeMedidaProjectionFactory {

        private final UnidadeMedidaProjection unidadeMedidaProjection;

        private TestUnidadeMedidaProjectionFactory(UnidadeMedidaProjection unidadeMedidaProjection) {

            this.unidadeMedidaProjection = unidadeMedidaProjection;

        }

        @Override
        public UnidadeMedidaProjection getUnidadeMedidaProjectionCompletoDeCache() {

            return unidadeMedidaProjection;

        }

    }

    private static class TestClusterEParametrosProjectionFactory extends ClusterEParametrosProjectionFactory {

        private final ClusterEParametrosProjection clusterEParametrosProjection;

        private TestClusterEParametrosProjectionFactory(ClusterEParametrosProjection clusterEParametrosProjection) {

            this.clusterEParametrosProjection = clusterEParametrosProjection;

        }

        @Override
        public ClusterEParametrosProjection getParametrosProjectionCompletoDeCache() {

            return clusterEParametrosProjection;

        }

    }

    private static class TestClusterEParametrosProjection extends ClusterEParametrosProjection {

        private final Set<Location> locationsAtivas;
        private final Set<Produto> materiaisAtivos;

        private TestClusterEParametrosProjection(
                Set<Location> locationsAtivas,
                Set<Produto> materiaisAtivos) {

            this.locationsAtivas = locationsAtivas;
            this.materiaisAtivos = materiaisAtivos;

        }

        @Override
        public Set<Location> getLocationsAtivas() {

            return locationsAtivas;

        }

        @Override
        public Set<Produto> getMateriaisAtivos() {

            return materiaisAtivos;

        }

        @Override
        public boolean isDfuAtiva(
                Produto material,
                Location location) {

            return materiaisAtivos.contains(material) && locationsAtivas.contains(location);

        }

        @Override
        public ParametrosGlobais getParametrosGlobais() {

            return new ParametrosGlobais();

        }

    }

    private static class TestUnidadeMedidaProjection extends UnidadeMedidaProjection {

        private TestUnidadeMedidaProjection(ParametrosGlobais parametrosGlobais) {

            this.parametrosGlobais = parametrosGlobais;

        }

    }

}
