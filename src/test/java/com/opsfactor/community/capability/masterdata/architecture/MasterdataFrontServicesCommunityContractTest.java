package com.opsfactor.community.capability.masterdata.architecture;

import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.LinhaTransporteIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.LinhaTransporteProdutoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.mapper.LinhaTransporteIntegrationMapper;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.mapper.LinhaTransporteIntegrationSupportData;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.mapper.LinhaTransporteProdutoIntegrationMapper;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.mapper.LinhaTransporteProdutoIntegrationSupportData;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporteProduto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnicaComponente;
import com.opsfactor.community.capability.masterdata.production.operation.domain.OperacaoRoteiro;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.LinhaTransporteProdutoRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.LinhaTransporteRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.VersaoMalhaRepository;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.repository.ListaTecnicaComponenteRepository;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.repository.ListaTecnicaRepository;
import com.opsfactor.community.capability.masterdata.production.operation.repository.OperacaoRoteiroRepository;
import com.opsfactor.community.capability.masterdata.production.productionresource.repository.RecursoProdutivoRepository;
import com.opsfactor.community.capability.masterdata.production.routing.repository.RoteiroRepository;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.dto.VersaoMalhaDTO;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.service.LinhaTransporteIntegrationService;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.service.LinhaTransporteProdutoIntegrationService;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.LinhaTransporteFacade;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.facade.ListaTecnicaFacade;
import com.opsfactor.community.capability.masterdata.production.productionresource.facade.dto.RecursoProdutivoDTO;
import com.opsfactor.community.capability.masterdata.production.productionresource.facade.RecursoProdutivoFacade;
import com.opsfactor.community.capability.masterdata.production.routing.facade.RoteiroFacade;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.mapper.VersaoMalhaAutoMapper;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.facade.mapper.ListaTecnicaAutoMapper;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.facade.mapper.ListaTecnicaComponenteAutoMapper;
import com.opsfactor.community.capability.masterdata.production.operation.facade.mapper.OperacaoRoteiroAutoMapper;
import com.opsfactor.community.capability.masterdata.production.productionresource.facade.mapper.RecursoProdutivoAutoMapper;
import com.opsfactor.community.capability.masterdata.production.routing.facade.mapper.RoteiroAutoMapper;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.facade.dto.ListaTecnicaComponenteDTO;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.facade.dto.ListaTecnicaDTO;
import com.opsfactor.community.capability.masterdata.production.operation.facade.dto.OperacaoRoteiroDTO;
import com.opsfactor.community.capability.masterdata.production.routing.facade.dto.RoteiroDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Contratos dos services front de master data operacional Community.
 *
 * <p>Esses services alimentam a SPA com malha simples e master data produtivo
 * usado pelo heuristico. Eles nao devem abrir conceitos Enterprise como mapa,
 * baricentro, frota, last mile, custos, turnos, manutencao, line scheduling ou
 * Supply Network Flows.</p>
 */
class MasterdataFrontServicesCommunityContractTest {

    @Test
    void masterdataFrontServicesShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertRequiredAutowiredFields(
                LinhaTransporteFacade.class,
                List.of(
                        "linhaTransporteRepository",
                        "linhaTransporteProdutoRepository",
                        "versaoMalhaRepository",
                        "locationRepository",
                        "linhaTransporteIntegrationMapper",
                        "linhaTransporteIntegrationService",
                        "linhaTransporteProdutoIntegrationMapper",
                        "linhaTransporteProdutoIntegrationService",
                        "versaoMalhaAutoMapper"));

        assertRequiredAutowiredFields(
                RecursoProdutivoFacade.class,
                List.of(
                        "recursoProdutivoRepository",
                        "recursoProdutivoAutoMapper",
                        "locationRepository"));

        assertRequiredAutowiredFields(
                RoteiroFacade.class,
                List.of(
                        "roteiroRepository",
                        "operacaoRoteiroRepository",
                        "roteiroAutoMapper",
                        "operacaoRoteiroAutoMapper"));

        assertRequiredAutowiredFields(
                ListaTecnicaFacade.class,
                List.of(
                        "listaTecnicaRepository",
                        "listaTecnicaComponenteRepository",
                        "listaTecnicaAutoMapper",
                        "listaTecnicaComponenteAutoMapper",
                        "parametrosGlobaisService"));

    }

    @Test
    void masterdataFrontServicesShouldRemainSpringServices() {

        Assertions.assertTrue(LinhaTransporteFacade.class.isAnnotationPresent(Service.class));
        Assertions.assertTrue(RecursoProdutivoFacade.class.isAnnotationPresent(Service.class));
        Assertions.assertTrue(RoteiroFacade.class.isAnnotationPresent(Service.class));
        Assertions.assertTrue(ListaTecnicaFacade.class.isAnnotationPresent(Service.class));

    }

    @Test
    void supplyNetworkDefaultVersionShouldNotBeRemoved() {

        LinhaTransporteFacade linhaTransporteFrontService = new LinhaTransporteFacade();
        VersaoMalhaDTO versaoMalhaDTO = new VersaoMalhaDTO();
        versaoMalhaDTO.setId("Default");

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> linhaTransporteFrontService.removeVersaoMalhaDTO(versaoMalhaDTO));

        Assertions.assertEquals(
                "Default Supply Network Version cannot be removed",
                illegalArgumentException.getMessage());

    }

    @Test
    void supplyNetworkVersionShouldRejectMissingPayloadBeforeRepository() {

        LinhaTransporteFacade linhaTransporteFrontService = new LinhaTransporteFacade();

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> linhaTransporteFrontService.saveVersaoMalhaDTO(null));

        VersaoMalhaDTO versaoMalhaDTO = new VersaoMalhaDTO();
        versaoMalhaDTO.setId(" ");

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> linhaTransporteFrontService.saveVersaoMalhaDTO(versaoMalhaDTO));
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> linhaTransporteFrontService.removeVersaoMalhaDTO(null));
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> linhaTransporteFrontService.getLinhaTransporteIntegrationDataDtoList(""));
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> linhaTransporteFrontService.getLinhaTransporteProdutoIntegrationDataDtoList(""));

    }

    @Test
    void supplyNetworkVersionListShouldRejectBrokenSnapshotsBeforeMapper() throws Exception {

        LinhaTransporteFacade linhaTransporteFrontServiceComListaNula =
                criaLinhaTransporteFrontServiceParaListagemVersaoMalha(null);

        IllegalStateException nullListException = Assertions.assertThrows(
                IllegalStateException.class,
                linhaTransporteFrontServiceComListaNula::getVersaoMalhaDTOList);
        Assertions.assertEquals(
                "Supply Network Version list snapshot is required.",
                nullListException.getMessage());

        LinhaTransporteFacade linhaTransporteFrontServiceComItemNulo =
                criaLinhaTransporteFrontServiceParaListagemVersaoMalha(
                        java.util.Collections.singletonList(null));

        IllegalStateException nullItemException = Assertions.assertThrows(
                IllegalStateException.class,
                linhaTransporteFrontServiceComItemNulo::getVersaoMalhaDTOList);
        Assertions.assertEquals(
                "Supply Network Version at index 0 is required in list snapshot.",
                nullItemException.getMessage());

        LinhaTransporteFacade linhaTransporteFrontServiceComVersaoSemId =
                criaLinhaTransporteFrontServiceParaListagemVersaoMalha(
                        List.of(new VersaoMalha()));

        IllegalStateException missingIdException = Assertions.assertThrows(
                IllegalStateException.class,
                linhaTransporteFrontServiceComVersaoSemId::getVersaoMalhaDTOList);
        Assertions.assertEquals(
                "Supply Network Version at index 0 has no id in list snapshot.",
                missingIdException.getMessage());

    }

    @Test
    void supplyNetworkVersionListShouldRejectBrokenMapperSnapshotBeforeReturning() throws Exception {

        LinhaTransporteFacade linhaTransporteFrontServiceComListaDTONula =
                criaLinhaTransporteFrontServiceParaListagemVersaoMalha(
                        List.of(criaVersaoMalhaParaTeste()));
        setPrivateField(
                linhaTransporteFrontServiceComListaDTONula,
                "versaoMalhaAutoMapper",
                getVersaoMalhaAutoMapperParaListagemRetornando(null));

        IllegalStateException nullDtoListException = Assertions.assertThrows(
                IllegalStateException.class,
                linhaTransporteFrontServiceComListaDTONula::getVersaoMalhaDTOList);
        Assertions.assertEquals(
                "Supply Network Version DTO list snapshot is required.",
                nullDtoListException.getMessage());

        LinhaTransporteFacade linhaTransporteFrontServiceComItemDTONulo =
                criaLinhaTransporteFrontServiceParaListagemVersaoMalha(
                        List.of(criaVersaoMalhaParaTeste()));
        setPrivateField(
                linhaTransporteFrontServiceComItemDTONulo,
                "versaoMalhaAutoMapper",
                getVersaoMalhaAutoMapperParaListagemRetornando(java.util.Collections.singletonList(null)));

        IllegalStateException nullDtoItemException = Assertions.assertThrows(
                IllegalStateException.class,
                linhaTransporteFrontServiceComItemDTONulo::getVersaoMalhaDTOList);
        Assertions.assertEquals(
                "Supply Network Version DTO at index 0 is required in list snapshot.",
                nullDtoItemException.getMessage());

        VersaoMalhaDTO versaoMalhaDTOSemId = new VersaoMalhaDTO();
        LinhaTransporteFacade linhaTransporteFrontServiceComDTOSemId =
                criaLinhaTransporteFrontServiceParaListagemVersaoMalha(
                        List.of(criaVersaoMalhaParaTeste()));
        setPrivateField(
                linhaTransporteFrontServiceComDTOSemId,
                "versaoMalhaAutoMapper",
                getVersaoMalhaAutoMapperParaListagemRetornando(List.of(versaoMalhaDTOSemId)));

        IllegalStateException missingDtoIdException = Assertions.assertThrows(
                IllegalStateException.class,
                linhaTransporteFrontServiceComDTOSemId::getVersaoMalhaDTOList);
        Assertions.assertEquals(
                "Supply Network Version DTO at index 0 has no id in list snapshot.",
                missingDtoIdException.getMessage());

    }

    @Test
    void transportationLaneListsShouldRejectNullSupplyNetworkVersionOptionalBeforeLaneRepositories()
            throws Exception {

        LinhaTransporteFacade linhaTransporteFrontService = new LinhaTransporteFacade();
        setPrivateField(
                linhaTransporteFrontService,
                "versaoMalhaRepository",
                getVersaoMalhaRepositoryParaFindByIdRetornando(null));

        /*
         * As listagens front de lanes carregam a versao de malha antes de
         * consultar lanes origem/destino ou lanes por material. Optional nulo
         * sinaliza repository quebrado e deve falhar antes desses repositories.
         */
        IllegalStateException linhaTransporteException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> linhaTransporteFrontService.getLinhaTransporteIntegrationDataDtoList("NETWORK"));
        Assertions.assertEquals(
                "Supply Network Version repository returned null Optional for transportation lane front id NETWORK.",
                linhaTransporteException.getMessage());

        IllegalStateException linhaTransporteProdutoException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> linhaTransporteFrontService.getLinhaTransporteProdutoIntegrationDataDtoList("NETWORK"));
        Assertions.assertEquals(
                "Supply Network Version repository returned null Optional for transportation lane front id NETWORK.",
                linhaTransporteProdutoException.getMessage());

    }

    @Test
    void transportationLaneListsShouldRejectBrokenSupplyNetworkVersionIdentityBeforeLaneRepositories()
            throws Exception {

        LinhaTransporteFacade linhaTransporteFrontServiceComVersaoSemId =
                new LinhaTransporteFacade();
        setPrivateField(
                linhaTransporteFrontServiceComVersaoSemId,
                "versaoMalhaRepository",
                getVersaoMalhaRepositoryParaFindByIdRetornando(Optional.of(new VersaoMalha())));

        /*
         * A versao de malha encontrada e o cabecalho funcional das lanes. Se
         * vier sem id ou com id diferente, a listagem deve falhar antes de
         * consultar lane origem/destino ou lane/material.
         */
        IllegalStateException versaoSemIdException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> linhaTransporteFrontServiceComVersaoSemId.getLinhaTransporteIntegrationDataDtoList("NETWORK"));
        Assertions.assertEquals(
                "Supply Network Version snapshot id is required for transportation lane front id NETWORK.",
                versaoSemIdException.getMessage());

        LinhaTransporteFacade linhaTransporteFrontServiceComVersaoDivergente =
                new LinhaTransporteFacade();
        setPrivateField(
                linhaTransporteFrontServiceComVersaoDivergente,
                "versaoMalhaRepository",
                getVersaoMalhaRepositoryParaFindByIdRetornando(Optional.of(new VersaoMalha("OTHER_NETWORK"))));

        IllegalStateException versaoDivergenteException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> linhaTransporteFrontServiceComVersaoDivergente.getLinhaTransporteProdutoIntegrationDataDtoList("NETWORK"));
        Assertions.assertEquals(
                "Supply Network Version snapshot id must match transportation lane front id NETWORK.",
                versaoDivergenteException.getMessage());

    }

    @Test
    void transportationLaneListShouldRejectBrokenRepositorySnapshotBeforeMapper() throws Exception {

        LinhaTransporteFacade linhaTransporteFrontServiceComListaNula =
                criaLinhaTransporteFrontServiceParaListagemLinhaTransporte(null);

        /*
         * A listagem de lanes deve validar a fotografia carregada do
         * repository. Isso mantem o erro estrutural perto da borda Community e
         * impede que o mapper receba entidades incompletas ou de outra versao.
         */
        IllegalStateException nullListException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> linhaTransporteFrontServiceComListaNula.getLinhaTransporteIntegrationDataDtoList("Default"));
        Assertions.assertEquals(
                "Transportation Line list snapshot is required for Supply Network Version Default.",
                nullListException.getMessage());

        LinhaTransporteFacade linhaTransporteFrontServiceComItemNulo =
                criaLinhaTransporteFrontServiceParaListagemLinhaTransporte(
                        java.util.Collections.singletonList(null));

        IllegalStateException nullItemException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> linhaTransporteFrontServiceComItemNulo.getLinhaTransporteIntegrationDataDtoList("Default"));
        Assertions.assertEquals(
                "Transportation Line at index 0 is required in list snapshot for Supply Network Version Default.",
                nullItemException.getMessage());

        LinhaTransporteFacade linhaTransporteFrontServiceComChaveIncompleta =
                criaLinhaTransporteFrontServiceParaListagemLinhaTransporte(
                        List.of(new LinhaTransporte()));

        IllegalStateException incompleteKeyException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> linhaTransporteFrontServiceComChaveIncompleta.getLinhaTransporteIntegrationDataDtoList("Default"));
        Assertions.assertEquals(
                "Transportation Line at index 0 has incomplete key in list snapshot.",
                incompleteKeyException.getMessage());

        LinhaTransporteFacade linhaTransporteFrontServiceComVersaoDivergente =
                criaLinhaTransporteFrontServiceParaListagemLinhaTransporte(
                        List.of(criaLinhaTransporteValidaParaTeste("OTHER_NETWORK")));

        IllegalStateException divergentVersionException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> linhaTransporteFrontServiceComVersaoDivergente.getLinhaTransporteIntegrationDataDtoList("Default"));
        Assertions.assertEquals(
                "Transportation Line at index 0 must match requested Supply Network Version Default.",
                divergentVersionException.getMessage());

    }

    @Test
    void transportationLaneListShouldRejectBrokenMapperSnapshotBeforeReturning() throws Exception {

        LinhaTransporteFacade linhaTransporteFrontServiceComListaDTONula =
                criaLinhaTransporteFrontServiceParaListagemLinhaTransporte(
                        List.of(criaLinhaTransporteValidaParaTeste()));
        setPrivateField(
                linhaTransporteFrontServiceComListaDTONula,
                "linhaTransporteIntegrationMapper",
                new LinhaTransporteIntegrationMapperStub(null, null));

        IllegalStateException nullDtoListException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> linhaTransporteFrontServiceComListaDTONula.getLinhaTransporteIntegrationDataDtoList("Default"));
        Assertions.assertEquals(
                "Transportation Line DTO list snapshot is required for Supply Network Version Default.",
                nullDtoListException.getMessage());

        LinhaTransporteFacade linhaTransporteFrontServiceComItemDTONulo =
                criaLinhaTransporteFrontServiceParaListagemLinhaTransporte(
                        List.of(criaLinhaTransporteValidaParaTeste()));
        setPrivateField(
                linhaTransporteFrontServiceComItemDTONulo,
                "linhaTransporteIntegrationMapper",
                new LinhaTransporteIntegrationMapperStub(
                        null,
                        java.util.Collections.singletonList(null)));

        IllegalStateException nullDtoItemException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> linhaTransporteFrontServiceComItemDTONulo.getLinhaTransporteIntegrationDataDtoList("Default"));
        Assertions.assertEquals(
                "Transportation Line DTO at index 0 is required in list snapshot for Supply Network Version Default.",
                nullDtoItemException.getMessage());

        LinhaTransporteIntegrationDataDto linhaTransporteIntegrationDataDtoComDistancia =
                criaLinhaTransporteIntegrationDataDtoParaTeste();
        linhaTransporteIntegrationDataDtoComDistancia.distanceKm = 10.0;
        LinhaTransporteFacade linhaTransporteFrontServiceComDistancia =
                criaLinhaTransporteFrontServiceParaListagemLinhaTransporte(
                        List.of(criaLinhaTransporteValidaParaTeste()));
        setPrivateField(
                linhaTransporteFrontServiceComDistancia,
                "linhaTransporteIntegrationMapper",
                new LinhaTransporteIntegrationMapperStub(
                        null,
                        List.of(linhaTransporteIntegrationDataDtoComDistancia)));

        IllegalStateException distanceException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> linhaTransporteFrontServiceComDistancia.getLinhaTransporteIntegrationDataDtoList("Default"));
        Assertions.assertEquals(
                "Transportation Line DTO at index 0 must not expose distance in Community list snapshot.",
                distanceException.getMessage());

    }

    @Test
    void transportationLaneMaterialListShouldRejectBrokenRepositorySnapshotBeforeMapper() throws Exception {

        LinhaTransporteFacade linhaTransporteFrontServiceComListaNula =
                criaLinhaTransporteFrontServiceParaListagemLinhaTransporteProduto(null);

        IllegalStateException nullListException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> linhaTransporteFrontServiceComListaNula.getLinhaTransporteProdutoIntegrationDataDtoList("Default"));
        Assertions.assertEquals(
                "Transportation Line - Material list snapshot is required for Supply Network Version Default.",
                nullListException.getMessage());

        LinhaTransporteFacade linhaTransporteFrontServiceComItemNulo =
                criaLinhaTransporteFrontServiceParaListagemLinhaTransporteProduto(
                        java.util.Collections.singletonList(null));

        IllegalStateException nullItemException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> linhaTransporteFrontServiceComItemNulo.getLinhaTransporteProdutoIntegrationDataDtoList("Default"));
        Assertions.assertEquals(
                "Transportation Line - Material at index 0 is required in list snapshot for Supply Network Version Default.",
                nullItemException.getMessage());

        LinhaTransporteProduto linhaTransporteProdutoComProdutoSemId =
                new LinhaTransporteProduto(
                        new LinhaTransporteProduto.LinhaTransporteProdutoCompositeKey(
                                criaLinhaTransporteValidaParaTeste(),
                                new Produto()));
        LinhaTransporteFacade linhaTransporteFrontServiceComChaveIncompleta =
                criaLinhaTransporteFrontServiceParaListagemLinhaTransporteProduto(
                        List.of(linhaTransporteProdutoComProdutoSemId));

        IllegalStateException incompleteKeyException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> linhaTransporteFrontServiceComChaveIncompleta.getLinhaTransporteProdutoIntegrationDataDtoList("Default"));
        Assertions.assertEquals(
                "Transportation Line - Material at index 0 has incomplete key in list snapshot.",
                incompleteKeyException.getMessage());

        LinhaTransporteProduto linhaTransporteProdutoComVersaoDivergente =
                new LinhaTransporteProduto(
                        new LinhaTransporteProduto.LinhaTransporteProdutoCompositeKey(
                                criaLinhaTransporteValidaParaTeste("OTHER_NETWORK"),
                                criaMaterialParaTeste()));
        LinhaTransporteFacade linhaTransporteFrontServiceComVersaoDivergente =
                criaLinhaTransporteFrontServiceParaListagemLinhaTransporteProduto(
                        List.of(linhaTransporteProdutoComVersaoDivergente));

        IllegalStateException divergentVersionException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> linhaTransporteFrontServiceComVersaoDivergente.getLinhaTransporteProdutoIntegrationDataDtoList("Default"));
        Assertions.assertEquals(
                "Transportation Line - Material at index 0 must match requested Supply Network Version Default.",
                divergentVersionException.getMessage());

    }

    @Test
    void transportationLaneMaterialListShouldRejectBrokenMapperSnapshotBeforeReturning() throws Exception {

        LinhaTransporteFacade linhaTransporteFrontServiceComListaDTONula =
                criaLinhaTransporteFrontServiceParaListagemLinhaTransporteProduto(
                        List.of(criaLinhaTransporteProdutoValidaParaTeste()));
        setPrivateField(
                linhaTransporteFrontServiceComListaDTONula,
                "linhaTransporteProdutoIntegrationMapper",
                new LinhaTransporteProdutoIntegrationMapperStub(null, null));

        IllegalStateException nullDtoListException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> linhaTransporteFrontServiceComListaDTONula.getLinhaTransporteProdutoIntegrationDataDtoList("Default"));
        Assertions.assertEquals(
                "Transportation Line - Material DTO list snapshot is required for Supply Network Version Default.",
                nullDtoListException.getMessage());

        LinhaTransporteFacade linhaTransporteFrontServiceComItemDTONulo =
                criaLinhaTransporteFrontServiceParaListagemLinhaTransporteProduto(
                        List.of(criaLinhaTransporteProdutoValidaParaTeste()));
        setPrivateField(
                linhaTransporteFrontServiceComItemDTONulo,
                "linhaTransporteProdutoIntegrationMapper",
                new LinhaTransporteProdutoIntegrationMapperStub(
                        null,
                        java.util.Collections.singletonList(null)));

        IllegalStateException nullDtoItemException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> linhaTransporteFrontServiceComItemDTONulo.getLinhaTransporteProdutoIntegrationDataDtoList("Default"));
        Assertions.assertEquals(
                "Transportation Line - Material DTO at index 0 is required in list snapshot for Supply Network Version Default.",
                nullDtoItemException.getMessage());

        LinhaTransporteProdutoIntegrationDataDto linhaTransporteProdutoIntegrationDataDtoSemMaterial =
                criaLinhaTransporteProdutoIntegrationDataDtoParaTeste();
        linhaTransporteProdutoIntegrationDataDtoSemMaterial.primaryKeyDto.materialId = null;
        LinhaTransporteFacade linhaTransporteFrontServiceComMaterialAusente =
                criaLinhaTransporteFrontServiceParaListagemLinhaTransporteProduto(
                        List.of(criaLinhaTransporteProdutoValidaParaTeste()));
        setPrivateField(
                linhaTransporteFrontServiceComMaterialAusente,
                "linhaTransporteProdutoIntegrationMapper",
                new LinhaTransporteProdutoIntegrationMapperStub(
                        null,
                        List.of(linhaTransporteProdutoIntegrationDataDtoSemMaterial)));

        IllegalStateException missingMaterialException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> linhaTransporteFrontServiceComMaterialAusente.getLinhaTransporteProdutoIntegrationDataDtoList("Default"));
        Assertions.assertEquals(
                "Transportation Line - Material DTO at index 0 has incomplete key in list snapshot.",
                missingMaterialException.getMessage());

    }

    @Test
    void supplyNetworkVersionShouldRejectBrokenSavedSnapshotBeforeReturning() throws Exception {

        VersaoMalhaDTO versaoMalhaDTO = new VersaoMalhaDTO();
        versaoMalhaDTO.setId("Default");

        LinhaTransporteFacade linhaTransporteFrontServiceComSnapshotNulo =
                criaLinhaTransporteFrontServiceParaSaveVersaoMalha(null);

        IllegalStateException nullSnapshotException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> linhaTransporteFrontServiceComSnapshotNulo.saveVersaoMalhaDTO(versaoMalhaDTO));

        Assertions.assertEquals(
                "Saved Supply Network Version snapshot is required.",
                nullSnapshotException.getMessage());

        VersaoMalha versaoMalhaSalvaSemId = new VersaoMalha();
        versaoMalhaSalvaSemId.setId(" ");
        LinhaTransporteFacade linhaTransporteFrontServiceComSnapshotSemId =
                criaLinhaTransporteFrontServiceParaSaveVersaoMalha(versaoMalhaSalvaSemId);

        IllegalStateException missingIdException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> linhaTransporteFrontServiceComSnapshotSemId.saveVersaoMalhaDTO(versaoMalhaDTO));

        Assertions.assertEquals(
                "Saved Supply Network Version id is required.",
                missingIdException.getMessage());

        VersaoMalha versaoMalhaSalvaDivergente = new VersaoMalha();
        versaoMalhaSalvaDivergente.setId("Other");
        LinhaTransporteFacade linhaTransporteFrontServiceComSnapshotDivergente =
                criaLinhaTransporteFrontServiceParaSaveVersaoMalha(versaoMalhaSalvaDivergente);

        IllegalStateException divergentIdException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> linhaTransporteFrontServiceComSnapshotDivergente.saveVersaoMalhaDTO(versaoMalhaDTO));

        Assertions.assertEquals(
                "Saved Supply Network Version id must match requested id.",
                divergentIdException.getMessage());

    }

    @Test
    void transportationLineShouldRejectMissingPrimaryKeyBeforeRepository() {

        LinhaTransporteFacade linhaTransporteFrontService = new LinhaTransporteFacade();

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> linhaTransporteFrontService.saveLinhaTransporteIntegrationDataDto(null));

        LinhaTransporteIntegrationDataDto linhaTransporteIntegrationDataDto = new LinhaTransporteIntegrationDataDto();
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> linhaTransporteFrontService.saveLinhaTransporteIntegrationDataDto(
                        linhaTransporteIntegrationDataDto));

        linhaTransporteIntegrationDataDto.primaryKeyDto =
                new LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO(
                        "Default",
                        " ",
                        "DEST");
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> linhaTransporteFrontService.saveLinhaTransporteIntegrationDataDto(
                        linhaTransporteIntegrationDataDto));

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> linhaTransporteFrontService.removeLinhaTransporteIntegrationDataDtoList(List.of()));
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> linhaTransporteFrontService.removeLinhaTransporteIntegrationDataDtoList(
                        java.util.Arrays.asList((LinhaTransporteIntegrationDataDto) null)));

    }

    @Test
    void transportationLineShouldRejectBrokenSavedSnapshotBeforeReturning() throws Exception {

        LinhaTransporteIntegrationDataDto linhaTransporteIntegrationDataDto =
                criaLinhaTransporteIntegrationDataDtoParaTeste();
        LinhaTransporte linhaTransporteMapeada = criaLinhaTransporteValidaParaTeste();
        LinhaTransporteFacade linhaTransporteFrontServiceComSnapshotNulo =
                criaLinhaTransporteFrontServiceParaSaveLinhaTransporte(
                        linhaTransporteMapeada,
                        null);

        IllegalStateException nullSnapshotException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> linhaTransporteFrontServiceComSnapshotNulo.saveLinhaTransporteIntegrationDataDto(
                        linhaTransporteIntegrationDataDto));

        Assertions.assertEquals(
                "Saved Transportation Line snapshot is required.",
                nullSnapshotException.getMessage());

        LinhaTransporteFacade linhaTransporteFrontServiceComSnapshotSemChave =
                criaLinhaTransporteFrontServiceParaSaveLinhaTransporte(
                        linhaTransporteMapeada,
                        new LinhaTransporte());

        IllegalStateException missingKeyException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> linhaTransporteFrontServiceComSnapshotSemChave.saveLinhaTransporteIntegrationDataDto(
                        linhaTransporteIntegrationDataDto));

        Assertions.assertEquals(
                "Saved Transportation Line key is required.",
                missingKeyException.getMessage());

    }

    @Test
    void transportationLineMaterialShouldRejectMissingPrimaryKeyBeforeRepository() {

        LinhaTransporteFacade linhaTransporteFrontService = new LinhaTransporteFacade();

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> linhaTransporteFrontService.saveLinhaTransporteProdutoIntegrationDataDto(null));

        LinhaTransporteProdutoIntegrationDataDto linhaTransporteProdutoIntegrationDataDto =
                new LinhaTransporteProdutoIntegrationDataDto();
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> linhaTransporteFrontService.saveLinhaTransporteProdutoIntegrationDataDto(
                        linhaTransporteProdutoIntegrationDataDto));

        linhaTransporteProdutoIntegrationDataDto.primaryKeyDto =
                new LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO(
                        "Default",
                        "ORIG",
                        "DEST",
                        null);
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> linhaTransporteFrontService.saveLinhaTransporteProdutoIntegrationDataDto(
                        linhaTransporteProdutoIntegrationDataDto));

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> linhaTransporteFrontService.removeLinhaTransporteProdutoIntegrationDataDtoList(null));
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> linhaTransporteFrontService.removeLinhaTransporteProdutoIntegrationDataDtoList(
                        java.util.Arrays.asList((LinhaTransporteProdutoIntegrationDataDto) null)));

    }

    @Test
    void transportationLineMaterialShouldRejectBrokenSavedSnapshotBeforeReturning() throws Exception {

        LinhaTransporteProdutoIntegrationDataDto linhaTransporteProdutoIntegrationDataDto =
                criaLinhaTransporteProdutoIntegrationDataDtoParaTeste();
        LinhaTransporteProduto linhaTransporteProdutoMapeada =
                criaLinhaTransporteProdutoValidaParaTeste();
        LinhaTransporteFacade linhaTransporteFrontServiceComSnapshotNulo =
                criaLinhaTransporteFrontServiceParaSaveLinhaTransporteProduto(
                        linhaTransporteProdutoMapeada,
                        null);

        IllegalStateException nullSnapshotException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> linhaTransporteFrontServiceComSnapshotNulo.saveLinhaTransporteProdutoIntegrationDataDto(
                        linhaTransporteProdutoIntegrationDataDto));

        Assertions.assertEquals(
                "Saved Transportation Line - Material snapshot is required.",
                nullSnapshotException.getMessage());

        LinhaTransporteFacade linhaTransporteFrontServiceComSnapshotSemChave =
                criaLinhaTransporteFrontServiceParaSaveLinhaTransporteProduto(
                        linhaTransporteProdutoMapeada,
                        new LinhaTransporteProduto());

        IllegalStateException missingKeyException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> linhaTransporteFrontServiceComSnapshotSemChave.saveLinhaTransporteProdutoIntegrationDataDto(
                        linhaTransporteProdutoIntegrationDataDto));

        Assertions.assertEquals(
                "Saved Transportation Line - Material key is required.",
                missingKeyException.getMessage());

    }

    @Test
    void transportationLineDeleteShouldRejectBrokenMappedEntitiesBeforeRepository() throws Exception {

        LinhaTransporteFacade linhaTransporteFrontService = new LinhaTransporteFacade();

        InvocationTargetException nullListException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaLinhaTransporteEntityListParaDeleteCommunity(
                        linhaTransporteFrontService,
                        null));
        InvocationTargetException nullItemException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaLinhaTransporteEntityListParaDeleteCommunity(
                        linhaTransporteFrontService,
                        java.util.Collections.singletonList(null)));
        InvocationTargetException incompleteKeyException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaLinhaTransporteEntityListParaDeleteCommunity(
                        linhaTransporteFrontService,
                        List.of(new LinhaTransporte())));

        Assertions.assertEquals(
                "Mapped Transportation Line entity list is required for delete.",
                nullListException.getCause().getMessage());
        Assertions.assertEquals(
                "Mapped Transportation Line entity at index 0 is required for delete.",
                nullItemException.getCause().getMessage());
        Assertions.assertEquals(
                "Mapped Transportation Line entity at index 0 has incomplete key for delete.",
                incompleteKeyException.getCause().getMessage());
        Assertions.assertDoesNotThrow(
                () -> invokeValidaLinhaTransporteEntityListParaDeleteCommunity(
                        linhaTransporteFrontService,
                        List.of(criaLinhaTransporteValidaParaTeste())));

    }

    @Test
    void transportationLineMaterialDeleteShouldRejectBrokenMappedEntitiesBeforeRepository() throws Exception {

        LinhaTransporteFacade linhaTransporteFrontService = new LinhaTransporteFacade();

        InvocationTargetException nullListException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaLinhaTransporteProdutoEntityListParaDeleteCommunity(
                        linhaTransporteFrontService,
                        null));
        InvocationTargetException nullItemException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaLinhaTransporteProdutoEntityListParaDeleteCommunity(
                        linhaTransporteFrontService,
                        java.util.Collections.singletonList(null)));
        InvocationTargetException incompleteKeyException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaLinhaTransporteProdutoEntityListParaDeleteCommunity(
                        linhaTransporteFrontService,
                        List.of(new LinhaTransporteProduto())));

        Assertions.assertEquals(
                "Mapped Transportation Line - Material entity list is required for delete.",
                nullListException.getCause().getMessage());
        Assertions.assertEquals(
                "Mapped Transportation Line - Material entity at index 0 is required for delete.",
                nullItemException.getCause().getMessage());
        Assertions.assertEquals(
                "Mapped Transportation Line - Material entity at index 0 has incomplete key for delete.",
                incompleteKeyException.getCause().getMessage());
        Assertions.assertDoesNotThrow(
                () -> invokeValidaLinhaTransporteProdutoEntityListParaDeleteCommunity(
                        linhaTransporteFrontService,
                        List.of(criaLinhaTransporteProdutoValidaParaTeste())));

    }

    @Test
    void productionResourceShouldRejectMissingPayloadBeforeRepository() {

        RecursoProdutivoFacade recursoProdutivoFrontService = new RecursoProdutivoFacade();

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> recursoProdutivoFrontService.saveRecursoProdutivoDTO(null));

        RecursoProdutivoDTO recursoProdutivoDTOSemId = RecursoProdutivoDTO.builder()
                .productionResourceId(" ")
                .locationId("LOC-01")
                .build();
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> recursoProdutivoFrontService.saveRecursoProdutivoDTO(recursoProdutivoDTOSemId));

        RecursoProdutivoDTO recursoProdutivoDTOSemLocation = RecursoProdutivoDTO.builder()
                .productionResourceId("RES-01")
                .locationId(null)
                .build();
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> recursoProdutivoFrontService.saveRecursoProdutivoDTO(recursoProdutivoDTOSemLocation));

    }

    @Test
    void productionResourceListShouldRejectBrokenSnapshotsBeforeMapper() throws Exception {

        RecursoProdutivoFacade recursoProdutivoFrontServiceComListaNula =
                criaRecursoProdutivoFrontServiceParaListagem(null);

        IllegalStateException nullListException = Assertions.assertThrows(
                IllegalStateException.class,
                recursoProdutivoFrontServiceComListaNula::getRecursoProdutivoDTOList);
        Assertions.assertEquals(
                "Production Resource list snapshot is required.",
                nullListException.getMessage());

        RecursoProdutivoFacade recursoProdutivoFrontServiceComItemNulo =
                criaRecursoProdutivoFrontServiceParaListagem(
                        java.util.Collections.singletonList(null));

        IllegalStateException nullItemException = Assertions.assertThrows(
                IllegalStateException.class,
                recursoProdutivoFrontServiceComItemNulo::getRecursoProdutivoDTOList);
        Assertions.assertEquals(
                "Production Resource at index 0 is required in list snapshot.",
                nullItemException.getMessage());

        RecursoProdutivoFacade recursoProdutivoFrontServiceComRecursoSemId =
                criaRecursoProdutivoFrontServiceParaListagem(
                        List.of(RecursoProdutivo.builder()
                                .location(criaLocationParaTeste("LOC-01"))
                                .build()));

        IllegalStateException missingIdException = Assertions.assertThrows(
                IllegalStateException.class,
                recursoProdutivoFrontServiceComRecursoSemId::getRecursoProdutivoDTOList);
        Assertions.assertEquals(
                "Production Resource at index 0 has no id in list snapshot.",
                missingIdException.getMessage());

        RecursoProdutivoFacade recursoProdutivoFrontServiceComRecursoSemLocation =
                criaRecursoProdutivoFrontServiceParaListagem(
                        List.of(RecursoProdutivo.builder()
                                .id("RES-01")
                                .build()));

        IllegalStateException missingLocationException = Assertions.assertThrows(
                IllegalStateException.class,
                recursoProdutivoFrontServiceComRecursoSemLocation::getRecursoProdutivoDTOList);
        Assertions.assertEquals(
                "Production Resource at index 0 has no location in list snapshot.",
                missingLocationException.getMessage());

    }

    @Test
    void productionResourceListShouldRejectBrokenMapperSnapshotBeforeReturning() throws Exception {

        RecursoProdutivoFacade recursoProdutivoFrontServiceComListaDTONula =
                criaRecursoProdutivoFrontServiceParaListagem(
                        List.of(criaRecursoProdutivoValidoParaTeste()));
        setPrivateField(
                recursoProdutivoFrontServiceComListaDTONula,
                "recursoProdutivoAutoMapper",
                getRecursoProdutivoAutoMapperRetornando(null));

        IllegalStateException nullDtoListException = Assertions.assertThrows(
                IllegalStateException.class,
                recursoProdutivoFrontServiceComListaDTONula::getRecursoProdutivoDTOList);
        Assertions.assertEquals(
                "Production Resource DTO list snapshot is required.",
                nullDtoListException.getMessage());

        RecursoProdutivoFacade recursoProdutivoFrontServiceComItemDTONulo =
                criaRecursoProdutivoFrontServiceParaListagem(
                        List.of(criaRecursoProdutivoValidoParaTeste()));
        setPrivateField(
                recursoProdutivoFrontServiceComItemDTONulo,
                "recursoProdutivoAutoMapper",
                getRecursoProdutivoAutoMapperRetornando(java.util.Collections.singletonList(null)));

        IllegalStateException nullDtoItemException = Assertions.assertThrows(
                IllegalStateException.class,
                recursoProdutivoFrontServiceComItemDTONulo::getRecursoProdutivoDTOList);
        Assertions.assertEquals(
                "Production Resource DTO at index 0 is required in list snapshot.",
                nullDtoItemException.getMessage());

        RecursoProdutivoDTO recursoProdutivoDTOSemLocation = RecursoProdutivoDTO.builder()
                .productionResourceId("RES-01")
                .build();
        RecursoProdutivoFacade recursoProdutivoFrontServiceComDTOSemLocation =
                criaRecursoProdutivoFrontServiceParaListagem(
                        List.of(criaRecursoProdutivoValidoParaTeste()));
        setPrivateField(
                recursoProdutivoFrontServiceComDTOSemLocation,
                "recursoProdutivoAutoMapper",
                getRecursoProdutivoAutoMapperRetornando(List.of(recursoProdutivoDTOSemLocation)));

        IllegalStateException missingLocationException = Assertions.assertThrows(
                IllegalStateException.class,
                recursoProdutivoFrontServiceComDTOSemLocation::getRecursoProdutivoDTOList);
        Assertions.assertEquals(
                "Production Resource DTO at index 0 has no location in list snapshot.",
                missingLocationException.getMessage());

    }

    @Test
    void productionRoutingListShouldRejectBrokenSnapshotsBeforeMapper() throws Exception {

        RoteiroFacade roteiroFrontServiceComListaNula =
                criaRoteiroFrontServiceParaListagemRoteiro(null);

        IllegalStateException nullListException = Assertions.assertThrows(
                IllegalStateException.class,
                roteiroFrontServiceComListaNula::getRoteiroDTOList);
        Assertions.assertEquals(
                "Production Routing list snapshot is required.",
                nullListException.getMessage());

        RoteiroFacade roteiroFrontServiceComItemNulo =
                criaRoteiroFrontServiceParaListagemRoteiro(
                        java.util.Collections.singletonList(null));

        IllegalStateException nullItemException = Assertions.assertThrows(
                IllegalStateException.class,
                roteiroFrontServiceComItemNulo::getRoteiroDTOList);
        Assertions.assertEquals(
                "Production Routing at index 0 is required in list snapshot.",
                nullItemException.getMessage());

        RoteiroFacade roteiroFrontServiceComRoteiroSemId =
                criaRoteiroFrontServiceParaListagemRoteiro(
                        List.of(Roteiro.builder()
                                .location(criaLocationParaTeste("LOC-01"))
                                .materialOutput(criaMaterialParaTeste())
                                .build()));

        IllegalStateException missingIdException = Assertions.assertThrows(
                IllegalStateException.class,
                roteiroFrontServiceComRoteiroSemId::getRoteiroDTOList);
        Assertions.assertEquals(
                "Production Routing at index 0 has no id in list snapshot.",
                missingIdException.getMessage());

        Roteiro roteiroSemLocation = new Roteiro();
        roteiroSemLocation.setId("ROUT-01");
        roteiroSemLocation.setMaterialOutput(criaMaterialParaTeste());

        RoteiroFacade roteiroFrontServiceComRoteiroSemLocation =
                criaRoteiroFrontServiceParaListagemRoteiro(
                        List.of(roteiroSemLocation));

        IllegalStateException missingLocationException = Assertions.assertThrows(
                IllegalStateException.class,
                roteiroFrontServiceComRoteiroSemLocation::getRoteiroDTOList);
        Assertions.assertEquals(
                "Production Routing at index 0 has no location in list snapshot.",
                missingLocationException.getMessage());

        Roteiro roteiroSemMaterial = new Roteiro();
        roteiroSemMaterial.setId("ROUT-01");
        roteiroSemMaterial.setLocation(criaLocationParaTeste("LOC-01"));

        RoteiroFacade roteiroFrontServiceComRoteiroSemMaterial =
                criaRoteiroFrontServiceParaListagemRoteiro(
                        List.of(roteiroSemMaterial));

        IllegalStateException missingOutputMaterialException = Assertions.assertThrows(
                IllegalStateException.class,
                roteiroFrontServiceComRoteiroSemMaterial::getRoteiroDTOList);
        Assertions.assertEquals(
                "Production Routing at index 0 has no output material in list snapshot.",
                missingOutputMaterialException.getMessage());

    }

    @Test
    void productionRoutingListShouldRejectBrokenMapperSnapshotBeforeReturning() throws Exception {

        RoteiroFacade roteiroFrontServiceComListaDTONula =
                criaRoteiroFrontServiceParaListagemRoteiro(
                        List.of(criaRoteiroValidoParaTeste()));
        setPrivateField(
                roteiroFrontServiceComListaDTONula,
                "roteiroAutoMapper",
                getRoteiroAutoMapperRetornando(null));

        IllegalStateException nullDtoListException = Assertions.assertThrows(
                IllegalStateException.class,
                roteiroFrontServiceComListaDTONula::getRoteiroDTOList);
        Assertions.assertEquals(
                "Production Routing DTO list snapshot is required.",
                nullDtoListException.getMessage());

        RoteiroFacade roteiroFrontServiceComItemDTONulo =
                criaRoteiroFrontServiceParaListagemRoteiro(
                        List.of(criaRoteiroValidoParaTeste()));
        setPrivateField(
                roteiroFrontServiceComItemDTONulo,
                "roteiroAutoMapper",
                getRoteiroAutoMapperRetornando(java.util.Collections.singletonList(null)));

        IllegalStateException nullDtoItemException = Assertions.assertThrows(
                IllegalStateException.class,
                roteiroFrontServiceComItemDTONulo::getRoteiroDTOList);
        Assertions.assertEquals(
                "Production Routing DTO at index 0 is required in list snapshot.",
                nullDtoItemException.getMessage());

        RoteiroDTO roteiroDTOSemLocation =
                criaRoteiroDTOValidoParaTeste();
        roteiroDTOSemLocation.setLocationId(null);
        RoteiroFacade roteiroFrontServiceComDTOSemLocation =
                criaRoteiroFrontServiceParaListagemRoteiro(
                        List.of(criaRoteiroValidoParaTeste()));
        setPrivateField(
                roteiroFrontServiceComDTOSemLocation,
                "roteiroAutoMapper",
                getRoteiroAutoMapperRetornando(List.of(roteiroDTOSemLocation)));

        IllegalStateException missingLocationException = Assertions.assertThrows(
                IllegalStateException.class,
                roteiroFrontServiceComDTOSemLocation::getRoteiroDTOList);
        Assertions.assertEquals(
                "Production Routing DTO at index 0 has no location in list snapshot.",
                missingLocationException.getMessage());

    }

    @Test
    void productionRoutingOperationListShouldRejectBrokenSnapshotsBeforeMapper() throws Exception {

        RoteiroFacade roteiroFrontServiceComListaNula =
                criaRoteiroFrontServiceParaListagemOperacao(null);

        IllegalStateException nullListException = Assertions.assertThrows(
                IllegalStateException.class,
                roteiroFrontServiceComListaNula::getOperacaoRoteiroDTOList);
        Assertions.assertEquals(
                "Production Routing Operation list snapshot is required.",
                nullListException.getMessage());

        RoteiroFacade roteiroFrontServiceComItemNulo =
                criaRoteiroFrontServiceParaListagemOperacao(
                        java.util.Collections.singletonList(null));

        IllegalStateException nullItemException = Assertions.assertThrows(
                IllegalStateException.class,
                roteiroFrontServiceComItemNulo::getOperacaoRoteiroDTOList);
        Assertions.assertEquals(
                "Production Routing Operation at index 0 is required in list snapshot.",
                nullItemException.getMessage());

        RoteiroFacade roteiroFrontServiceComOperacaoSemPosicao =
                criaRoteiroFrontServiceParaListagemOperacao(
                        List.of(new OperacaoRoteiro()));

        IllegalStateException missingPositionException = Assertions.assertThrows(
                IllegalStateException.class,
                roteiroFrontServiceComOperacaoSemPosicao::getOperacaoRoteiroDTOList);
        Assertions.assertEquals(
                "Production Routing Operation at index 0 has no position in list snapshot.",
                missingPositionException.getMessage());

        OperacaoRoteiro operacaoRoteiroSemRoteiro =
                new OperacaoRoteiro(new OperacaoRoteiro.OperacaoRoteiroCompositeKey());
        operacaoRoteiroSemRoteiro.getOperacaoRoteiroCompositeKey().setPosicao(10);

        RoteiroFacade roteiroFrontServiceComOperacaoSemRoteiro =
                criaRoteiroFrontServiceParaListagemOperacao(
                        List.of(operacaoRoteiroSemRoteiro));

        IllegalStateException missingRoutingException = Assertions.assertThrows(
                IllegalStateException.class,
                roteiroFrontServiceComOperacaoSemRoteiro::getOperacaoRoteiroDTOList);
        Assertions.assertEquals(
                "Production Routing Operation at index 0 has no routing in list snapshot.",
                missingRoutingException.getMessage());

        OperacaoRoteiro operacaoRoteiroSemRecurso =
                new OperacaoRoteiro(new OperacaoRoteiro.OperacaoRoteiroCompositeKey(
                        10,
                        criaRoteiroValidoParaTeste()));

        RoteiroFacade roteiroFrontServiceComOperacaoSemRecurso =
                criaRoteiroFrontServiceParaListagemOperacao(
                        List.of(operacaoRoteiroSemRecurso));

        IllegalStateException missingResourceException = Assertions.assertThrows(
                IllegalStateException.class,
                roteiroFrontServiceComOperacaoSemRecurso::getOperacaoRoteiroDTOList);
        Assertions.assertEquals(
                "Production Routing Operation at index 0 has no production resource in list snapshot.",
                missingResourceException.getMessage());

    }

    @Test
    void productionRoutingOperationListShouldRejectBrokenMapperSnapshotBeforeReturning() throws Exception {

        RoteiroFacade roteiroFrontServiceComListaDTONula =
                criaRoteiroFrontServiceParaListagemOperacao(
                        List.of(criaOperacaoRoteiroValidaParaTeste()));
        setPrivateField(
                roteiroFrontServiceComListaDTONula,
                "operacaoRoteiroAutoMapper",
                getOperacaoRoteiroAutoMapperRetornando(null));

        IllegalStateException nullDtoListException = Assertions.assertThrows(
                IllegalStateException.class,
                roteiroFrontServiceComListaDTONula::getOperacaoRoteiroDTOList);
        Assertions.assertEquals(
                "Production Routing Operation DTO list snapshot is required.",
                nullDtoListException.getMessage());

        RoteiroFacade roteiroFrontServiceComItemDTONulo =
                criaRoteiroFrontServiceParaListagemOperacao(
                        List.of(criaOperacaoRoteiroValidaParaTeste()));
        setPrivateField(
                roteiroFrontServiceComItemDTONulo,
                "operacaoRoteiroAutoMapper",
                getOperacaoRoteiroAutoMapperRetornando(java.util.Collections.singletonList(null)));

        IllegalStateException nullDtoItemException = Assertions.assertThrows(
                IllegalStateException.class,
                roteiroFrontServiceComItemDTONulo::getOperacaoRoteiroDTOList);
        Assertions.assertEquals(
                "Production Routing Operation DTO at index 0 is required in list snapshot.",
                nullDtoItemException.getMessage());

        OperacaoRoteiroDTO operacaoRoteiroDTOSemRecurso =
                criaOperacaoRoteiroDTOValidaParaTeste();
        operacaoRoteiroDTOSemRecurso.setProductionResourceId(null);
        RoteiroFacade roteiroFrontServiceComDTOSemRecurso =
                criaRoteiroFrontServiceParaListagemOperacao(
                        List.of(criaOperacaoRoteiroValidaParaTeste()));
        setPrivateField(
                roteiroFrontServiceComDTOSemRecurso,
                "operacaoRoteiroAutoMapper",
                getOperacaoRoteiroAutoMapperRetornando(List.of(operacaoRoteiroDTOSemRecurso)));

        IllegalStateException missingResourceException = Assertions.assertThrows(
                IllegalStateException.class,
                roteiroFrontServiceComDTOSemRecurso::getOperacaoRoteiroDTOList);
        Assertions.assertEquals(
                "Production Routing Operation DTO at index 0 has no production resource in list snapshot.",
                missingResourceException.getMessage());

    }

    @Test
    void productionRoutingConsistencyDiagnosticShouldRejectBrokenEmbeddedOperationsBeforeSorting() throws Exception {

        Roteiro roteiroComOperacoesNulas = criaRoteiroValidoParaTeste();
        roteiroComOperacoesNulas.setOperacaoRoteiroSet(null);
        RoteiroFacade roteiroFrontServiceComOperacoesNulas =
                criaRoteiroFrontServiceParaListagemRoteiro(
                        List.of(roteiroComOperacoesNulas));

        /*
         * O diagnostico deprecated usa as operacoes navegando pelo proprio
         * roteiro, e nao pelo repository de operacoes. Snapshot interno nulo
         * ou com item quebrado deve falhar antes do sort/ultima operacao.
         */
        IllegalStateException nullOperationSetException = Assertions.assertThrows(
                IllegalStateException.class,
                roteiroFrontServiceComOperacoesNulas::getInconsistenciaReceitaProducaoDTOList);
        Assertions.assertEquals(
                "Production Routing ROUT-01 operation set is required for consistency diagnostic.",
                nullOperationSetException.getMessage());

        Roteiro roteiroComOperacaoNula = criaRoteiroValidoParaTeste();
        java.util.HashSet<OperacaoRoteiro> operacaoRoteiroSetComNulo = new java.util.HashSet<>();
        operacaoRoteiroSetComNulo.add(null);
        roteiroComOperacaoNula.setOperacaoRoteiroSet(operacaoRoteiroSetComNulo);
        RoteiroFacade roteiroFrontServiceComOperacaoNula =
                criaRoteiroFrontServiceParaListagemRoteiro(
                        List.of(roteiroComOperacaoNula));

        IllegalStateException nullOperationException = Assertions.assertThrows(
                IllegalStateException.class,
                roteiroFrontServiceComOperacaoNula::getInconsistenciaReceitaProducaoDTOList);
        Assertions.assertEquals(
                "Production Routing ROUT-01 operation 0 is required for consistency diagnostic.",
                nullOperationException.getMessage());

        Roteiro roteiroComOperacaoSemPosicao = criaRoteiroValidoParaTeste();
        roteiroComOperacaoSemPosicao.setOperacaoRoteiroSet(
                java.util.Set.of(new OperacaoRoteiro()));
        RoteiroFacade roteiroFrontServiceComOperacaoSemPosicao =
                criaRoteiroFrontServiceParaListagemRoteiro(
                        List.of(roteiroComOperacaoSemPosicao));

        IllegalStateException missingPositionException = Assertions.assertThrows(
                IllegalStateException.class,
                roteiroFrontServiceComOperacaoSemPosicao::getInconsistenciaReceitaProducaoDTOList);
        Assertions.assertEquals(
                "Production Routing ROUT-01 operation 0 position is required for consistency diagnostic.",
                missingPositionException.getMessage());

    }

    @Test
    void billOfMaterialsListShouldRejectBrokenSnapshotsBeforeMapper() throws Exception {

        ListaTecnicaFacade listaTecnicaFrontServiceComParametrosNulos =
                criaListaTecnicaFrontServiceParaListagemListaTecnica(
                        List.of(criaListaTecnicaValidaParaTeste()),
                        null);

        IllegalStateException nullGlobalParametersException = Assertions.assertThrows(
                IllegalStateException.class,
                listaTecnicaFrontServiceComParametrosNulos::getListaTecnicaDTOList);
        Assertions.assertEquals(
                "Global Parameters snapshot is required for Bill of Materials list.",
                nullGlobalParametersException.getMessage());

        ListaTecnicaFacade listaTecnicaFrontServiceComListaNula =
                criaListaTecnicaFrontServiceParaListagemListaTecnica(
                        null,
                        criaParametrosGlobaisParaTeste("UN"));

        IllegalStateException nullListException = Assertions.assertThrows(
                IllegalStateException.class,
                listaTecnicaFrontServiceComListaNula::getListaTecnicaDTOList);
        Assertions.assertEquals(
                "Bill of Materials list snapshot is required.",
                nullListException.getMessage());

        ListaTecnicaFacade listaTecnicaFrontServiceComItemNulo =
                criaListaTecnicaFrontServiceParaListagemListaTecnica(
                        java.util.Collections.singletonList(null),
                        criaParametrosGlobaisParaTeste("UN"));

        IllegalStateException nullItemException = Assertions.assertThrows(
                IllegalStateException.class,
                listaTecnicaFrontServiceComItemNulo::getListaTecnicaDTOList);
        Assertions.assertEquals(
                "Bill of Materials at index 0 is required in list snapshot.",
                nullItemException.getMessage());

        ListaTecnicaFacade listaTecnicaFrontServiceComListaSemId =
                criaListaTecnicaFrontServiceParaListagemListaTecnica(
                        List.of(ListaTecnica.builder()
                                .location(criaLocationParaTeste("LOC-01"))
                                .materialOutput(criaMaterialParaTeste())
                                .build()),
                        criaParametrosGlobaisParaTeste("UN"));

        IllegalStateException missingIdException = Assertions.assertThrows(
                IllegalStateException.class,
                listaTecnicaFrontServiceComListaSemId::getListaTecnicaDTOList);
        Assertions.assertEquals(
                "Bill of Materials at index 0 has no id in list snapshot.",
                missingIdException.getMessage());

        ListaTecnica listaTecnicaSemLocation = new ListaTecnica();
        listaTecnicaSemLocation.setId("BOM-01");
        listaTecnicaSemLocation.setMaterialOutput(criaMaterialParaTeste());

        ListaTecnicaFacade listaTecnicaFrontServiceComListaSemLocation =
                criaListaTecnicaFrontServiceParaListagemListaTecnica(
                        List.of(listaTecnicaSemLocation),
                        criaParametrosGlobaisParaTeste("UN"));

        IllegalStateException missingLocationException = Assertions.assertThrows(
                IllegalStateException.class,
                listaTecnicaFrontServiceComListaSemLocation::getListaTecnicaDTOList);
        Assertions.assertEquals(
                "Bill of Materials at index 0 has no location in list snapshot.",
                missingLocationException.getMessage());

        ListaTecnica listaTecnicaSemMaterial = new ListaTecnica();
        listaTecnicaSemMaterial.setId("BOM-01");
        listaTecnicaSemMaterial.setLocation(criaLocationParaTeste("LOC-01"));

        ListaTecnicaFacade listaTecnicaFrontServiceComListaSemMaterial =
                criaListaTecnicaFrontServiceParaListagemListaTecnica(
                        List.of(listaTecnicaSemMaterial),
                        criaParametrosGlobaisParaTeste("UN"));

        IllegalStateException missingOutputMaterialException = Assertions.assertThrows(
                IllegalStateException.class,
                listaTecnicaFrontServiceComListaSemMaterial::getListaTecnicaDTOList);
        Assertions.assertEquals(
                "Bill of Materials at index 0 has no output material in list snapshot.",
                missingOutputMaterialException.getMessage());

        ListaTecnicaFacade listaTecnicaFrontServiceComUnidadeSemId =
                criaListaTecnicaFrontServiceParaListagemListaTecnica(
                        List.of(criaListaTecnicaValidaParaTeste()),
                        criaParametrosGlobaisParaTeste(null));

        IllegalStateException missingOutputUnitException = Assertions.assertThrows(
                IllegalStateException.class,
                listaTecnicaFrontServiceComUnidadeSemId::getListaTecnicaDTOList);
        Assertions.assertEquals(
                "Bill of Materials at index 0 has no output unit of measure in list snapshot.",
                missingOutputUnitException.getMessage());

    }

    @Test
    void billOfMaterialsListShouldRejectBrokenMapperSnapshotBeforeReturning() throws Exception {

        ListaTecnicaFacade listaTecnicaFrontServiceComListaDTONula =
                criaListaTecnicaFrontServiceParaListagemListaTecnica(
                        List.of(criaListaTecnicaValidaParaTeste()),
                        criaParametrosGlobaisParaTeste("UN"));
        setPrivateField(
                listaTecnicaFrontServiceComListaDTONula,
                "listaTecnicaAutoMapper",
                getListaTecnicaAutoMapperRetornando(null));

        /*
         * A lista de entidades ja esta valida; a falha agora precisa acontecer
         * na fotografia DTO devolvida pelo mapper, antes de entregar um
         * cadastro parcial para a SPA.
         */
        IllegalStateException nullDtoListException = Assertions.assertThrows(
                IllegalStateException.class,
                listaTecnicaFrontServiceComListaDTONula::getListaTecnicaDTOList);
        Assertions.assertEquals(
                "Bill of Materials DTO list snapshot is required.",
                nullDtoListException.getMessage());

        ListaTecnicaFacade listaTecnicaFrontServiceComItemDTONulo =
                criaListaTecnicaFrontServiceParaListagemListaTecnica(
                        List.of(criaListaTecnicaValidaParaTeste()),
                        criaParametrosGlobaisParaTeste("UN"));
        setPrivateField(
                listaTecnicaFrontServiceComItemDTONulo,
                "listaTecnicaAutoMapper",
                getListaTecnicaAutoMapperRetornando(java.util.Collections.singletonList(null)));

        IllegalStateException nullDtoItemException = Assertions.assertThrows(
                IllegalStateException.class,
                listaTecnicaFrontServiceComItemDTONulo::getListaTecnicaDTOList);
        Assertions.assertEquals(
                "Bill of Materials DTO at index 0 is required in list snapshot.",
                nullDtoItemException.getMessage());

        ListaTecnicaDTO listaTecnicaDTOComQuantidadeInvalida =
                criaListaTecnicaDTOValidaParaTeste();
        listaTecnicaDTOComQuantidadeInvalida.setOutputQuantity(Float.NaN);
        ListaTecnicaFacade listaTecnicaFrontServiceComQuantidadeInvalida =
                criaListaTecnicaFrontServiceParaListagemListaTecnica(
                        List.of(criaListaTecnicaValidaParaTeste()),
                        criaParametrosGlobaisParaTeste("UN"));
        setPrivateField(
                listaTecnicaFrontServiceComQuantidadeInvalida,
                "listaTecnicaAutoMapper",
                getListaTecnicaAutoMapperRetornando(List.of(listaTecnicaDTOComQuantidadeInvalida)));

        IllegalStateException invalidQuantityException = Assertions.assertThrows(
                IllegalStateException.class,
                listaTecnicaFrontServiceComQuantidadeInvalida::getListaTecnicaDTOList);
        Assertions.assertEquals(
                "Bill of Materials DTO at index 0 has no finite output quantity in list snapshot.",
                invalidQuantityException.getMessage());

    }

    @Test
    void billOfMaterialsComponentListShouldRejectBrokenSnapshotsBeforeMapper() throws Exception {

        ListaTecnicaFacade listaTecnicaFrontServiceComListaNula =
                criaListaTecnicaFrontServiceParaListagemComponente(
                        null,
                        criaParametrosGlobaisParaTeste("UN"));

        IllegalStateException nullListException = Assertions.assertThrows(
                IllegalStateException.class,
                listaTecnicaFrontServiceComListaNula::getListaTecnicaComponenteDTOList);
        Assertions.assertEquals(
                "Bill of Materials Component list snapshot is required.",
                nullListException.getMessage());

        ListaTecnicaFacade listaTecnicaFrontServiceComItemNulo =
                criaListaTecnicaFrontServiceParaListagemComponente(
                        java.util.Collections.singletonList(null),
                        criaParametrosGlobaisParaTeste("UN"));

        IllegalStateException nullItemException = Assertions.assertThrows(
                IllegalStateException.class,
                listaTecnicaFrontServiceComItemNulo::getListaTecnicaComponenteDTOList);
        Assertions.assertEquals(
                "Bill of Materials Component at index 0 is required in list snapshot.",
                nullItemException.getMessage());

        ListaTecnicaFacade listaTecnicaFrontServiceComComponenteSemBom =
                criaListaTecnicaFrontServiceParaListagemComponente(
                        List.of(new ListaTecnicaComponente()),
                        criaParametrosGlobaisParaTeste("UN"));

        IllegalStateException missingBomException = Assertions.assertThrows(
                IllegalStateException.class,
                listaTecnicaFrontServiceComComponenteSemBom::getListaTecnicaComponenteDTOList);
        Assertions.assertEquals(
                "Bill of Materials Component at index 0 has no bill of materials in list snapshot.",
                missingBomException.getMessage());

        ListaTecnicaComponente listaTecnicaComponenteSemMaterial =
                new ListaTecnicaComponente(new ListaTecnicaComponente.ListaTecnicaComponenteCompositeKey());
        listaTecnicaComponenteSemMaterial.getListaTecnicaComponenteCompositeKey()
                .setListaTecnica(criaListaTecnicaValidaParaTeste());

        ListaTecnicaFacade listaTecnicaFrontServiceComComponenteSemMaterial =
                criaListaTecnicaFrontServiceParaListagemComponente(
                        List.of(listaTecnicaComponenteSemMaterial),
                        criaParametrosGlobaisParaTeste("UN"));

        IllegalStateException missingComponentMaterialException = Assertions.assertThrows(
                IllegalStateException.class,
                listaTecnicaFrontServiceComComponenteSemMaterial::getListaTecnicaComponenteDTOList);
        Assertions.assertEquals(
                "Bill of Materials Component at index 0 has no component material in list snapshot.",
                missingComponentMaterialException.getMessage());

        ListaTecnicaFacade listaTecnicaFrontServiceComUnidadeSemId =
                criaListaTecnicaFrontServiceParaListagemComponente(
                        List.of(criaListaTecnicaComponenteValidaParaTeste()),
                        criaParametrosGlobaisParaTeste(null));

        IllegalStateException missingComponentUnitException = Assertions.assertThrows(
                IllegalStateException.class,
                listaTecnicaFrontServiceComUnidadeSemId::getListaTecnicaComponenteDTOList);
        Assertions.assertEquals(
                "Bill of Materials Component at index 0 has no component unit of measure in list snapshot.",
                missingComponentUnitException.getMessage());

    }

    @Test
    void billOfMaterialsComponentListShouldRejectBrokenMapperSnapshotBeforeReturning() throws Exception {

        ListaTecnicaFacade listaTecnicaFrontServiceComListaDTONula =
                criaListaTecnicaFrontServiceParaListagemComponente(
                        List.of(criaListaTecnicaComponenteValidaParaTeste()),
                        criaParametrosGlobaisParaTeste("UN"));
        setPrivateField(
                listaTecnicaFrontServiceComListaDTONula,
                "listaTecnicaComponenteAutoMapper",
                getListaTecnicaComponenteAutoMapperRetornando(null));

        IllegalStateException nullDtoListException = Assertions.assertThrows(
                IllegalStateException.class,
                listaTecnicaFrontServiceComListaDTONula::getListaTecnicaComponenteDTOList);
        Assertions.assertEquals(
                "Bill of Materials Component DTO list snapshot is required.",
                nullDtoListException.getMessage());

        ListaTecnicaFacade listaTecnicaFrontServiceComItemDTONulo =
                criaListaTecnicaFrontServiceParaListagemComponente(
                        List.of(criaListaTecnicaComponenteValidaParaTeste()),
                        criaParametrosGlobaisParaTeste("UN"));
        setPrivateField(
                listaTecnicaFrontServiceComItemDTONulo,
                "listaTecnicaComponenteAutoMapper",
                getListaTecnicaComponenteAutoMapperRetornando(java.util.Collections.singletonList(null)));

        IllegalStateException nullDtoItemException = Assertions.assertThrows(
                IllegalStateException.class,
                listaTecnicaFrontServiceComItemDTONulo::getListaTecnicaComponenteDTOList);
        Assertions.assertEquals(
                "Bill of Materials Component DTO at index 0 is required in list snapshot.",
                nullDtoItemException.getMessage());

        ListaTecnicaComponenteDTO listaTecnicaComponenteDTOComMaterialAusente =
                criaListaTecnicaComponenteDTOValidaParaTeste();
        listaTecnicaComponenteDTOComMaterialAusente.setComponentMaterialId(null);
        ListaTecnicaFacade listaTecnicaFrontServiceComMaterialAusente =
                criaListaTecnicaFrontServiceParaListagemComponente(
                        List.of(criaListaTecnicaComponenteValidaParaTeste()),
                        criaParametrosGlobaisParaTeste("UN"));
        setPrivateField(
                listaTecnicaFrontServiceComMaterialAusente,
                "listaTecnicaComponenteAutoMapper",
                getListaTecnicaComponenteAutoMapperRetornando(List.of(listaTecnicaComponenteDTOComMaterialAusente)));

        IllegalStateException missingComponentMaterialException = Assertions.assertThrows(
                IllegalStateException.class,
                listaTecnicaFrontServiceComMaterialAusente::getListaTecnicaComponenteDTOList);
        Assertions.assertEquals(
                "Bill of Materials Component DTO at index 0 has no component material in list snapshot.",
                missingComponentMaterialException.getMessage());

    }

    @Test
    void productionResourceShouldRejectNullRepositoryOptionalsBeforeSave() throws Exception {

        RecursoProdutivoDTO recursoProdutivoDTO = RecursoProdutivoDTO.builder()
                .productionResourceId("RES-01")
                .locationId("LOC-01")
                .build();

        RecursoProdutivoFacade recursoProdutivoFrontServiceComRecursoQuebrado =
                new RecursoProdutivoFacade();
        setPrivateField(
                recursoProdutivoFrontServiceComRecursoQuebrado,
                "recursoProdutivoRepository",
                getRecursoProdutivoRepositoryParaFindByIdRetornando(null));

        /*
         * Recurso ausente cria novo cadastro; Optional nulo e erro estrutural e
         * deve falhar antes de consultar Location ou salvar snapshot parcial.
         */
        IllegalStateException recursoException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> recursoProdutivoFrontServiceComRecursoQuebrado.saveRecursoProdutivoDTO(recursoProdutivoDTO));
        Assertions.assertEquals(
                "Production Resource repository returned null Optional for front save id RES-01.",
                recursoException.getMessage());

        RecursoProdutivoFacade recursoProdutivoFrontServiceComLocationQuebrada =
                new RecursoProdutivoFacade();
        setPrivateField(
                recursoProdutivoFrontServiceComLocationQuebrada,
                "recursoProdutivoRepository",
                getRecursoProdutivoRepositoryParaFindByIdRetornando(Optional.empty()));
        setPrivateField(
                recursoProdutivoFrontServiceComLocationQuebrada,
                "locationRepository",
                getLocationRepositoryRetornando(null));

        IllegalStateException locationException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> recursoProdutivoFrontServiceComLocationQuebrada.saveRecursoProdutivoDTO(recursoProdutivoDTO));
        Assertions.assertEquals(
                "Location repository returned null Optional for Production Resource front save id LOC-01.",
                locationException.getMessage());

    }

    @Test
    void productionResourceShouldRejectBrokenLoadedIdentitiesBeforeSave() throws Exception {

        RecursoProdutivoDTO recursoProdutivoDTO = RecursoProdutivoDTO.builder()
                .productionResourceId("RES-01")
                .locationId("LOC-01")
                .build();

        RecursoProdutivoFacade recursoProdutivoFrontServiceComRecursoSemId =
                new RecursoProdutivoFacade();
        setPrivateField(
                recursoProdutivoFrontServiceComRecursoSemId,
                "recursoProdutivoRepository",
                getRecursoProdutivoRepositoryParaFindByIdRetornando(
                        Optional.of(RecursoProdutivo.builder().build())));

        /*
         * Quando findById encontra uma entidade existente, a identidade precisa
         * bater com o payload antes de sobrescrever campos ou consultar a
         * location. Isso protege stubs/repositories que devolvam snapshot
         * incoerente.
         */
        IllegalStateException recursoSemIdException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> recursoProdutivoFrontServiceComRecursoSemId.saveRecursoProdutivoDTO(recursoProdutivoDTO));
        Assertions.assertEquals(
                "Loaded Production Resource id is required for front save id RES-01.",
                recursoSemIdException.getMessage());

        RecursoProdutivoFacade recursoProdutivoFrontServiceComRecursoDivergente =
                new RecursoProdutivoFacade();
        setPrivateField(
                recursoProdutivoFrontServiceComRecursoDivergente,
                "recursoProdutivoRepository",
                getRecursoProdutivoRepositoryParaFindByIdRetornando(
                        Optional.of(RecursoProdutivo.builder()
                                .id("RES-02")
                                .build())));

        IllegalStateException recursoDivergenteException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> recursoProdutivoFrontServiceComRecursoDivergente.saveRecursoProdutivoDTO(recursoProdutivoDTO));
        Assertions.assertEquals(
                "Loaded Production Resource id must match front save id RES-01.",
                recursoDivergenteException.getMessage());

        RecursoProdutivoFacade recursoProdutivoFrontServiceComLocationSemId =
                new RecursoProdutivoFacade();
        setPrivateField(
                recursoProdutivoFrontServiceComLocationSemId,
                "recursoProdutivoRepository",
                getRecursoProdutivoRepositoryParaFindByIdRetornando(Optional.empty()));
        setPrivateField(
                recursoProdutivoFrontServiceComLocationSemId,
                "locationRepository",
                getLocationRepositoryRetornando(Optional.of(new Location())));

        IllegalStateException locationSemIdException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> recursoProdutivoFrontServiceComLocationSemId.saveRecursoProdutivoDTO(recursoProdutivoDTO));
        Assertions.assertEquals(
                "Loaded Location id is required for Production Resource front save id LOC-01.",
                locationSemIdException.getMessage());

        RecursoProdutivoFacade recursoProdutivoFrontServiceComLocationDivergente =
                new RecursoProdutivoFacade();
        setPrivateField(
                recursoProdutivoFrontServiceComLocationDivergente,
                "recursoProdutivoRepository",
                getRecursoProdutivoRepositoryParaFindByIdRetornando(Optional.empty()));
        setPrivateField(
                recursoProdutivoFrontServiceComLocationDivergente,
                "locationRepository",
                getLocationRepositoryRetornando(Optional.of(criaLocationParaTeste("LOC-02"))));

        IllegalStateException locationDivergenteException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> recursoProdutivoFrontServiceComLocationDivergente.saveRecursoProdutivoDTO(recursoProdutivoDTO));
        Assertions.assertEquals(
                "Loaded Location id must match Production Resource front save id LOC-01.",
                locationDivergenteException.getMessage());

    }

    @Test
    void productionResourceShouldRejectBrokenSavedSnapshotBeforeReturning() throws Exception {

        RecursoProdutivoDTO recursoProdutivoDTO = RecursoProdutivoDTO.builder()
                .productionResourceId("RES-01")
                .locationId("LOC-01")
                .build();
        RecursoProdutivoFacade recursoProdutivoFrontServiceComSnapshotNulo =
                criaRecursoProdutivoFrontServiceParaTeste(null);

        IllegalStateException nullSnapshotException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> recursoProdutivoFrontServiceComSnapshotNulo.saveRecursoProdutivoDTO(recursoProdutivoDTO));

        Assertions.assertEquals(
                "Saved Production Resource snapshot is required.",
                nullSnapshotException.getMessage());

        RecursoProdutivoFacade recursoProdutivoFrontServiceComSnapshotSemLocation =
                criaRecursoProdutivoFrontServiceParaTeste(
                        RecursoProdutivo.builder()
                        .id("RES-01")
                        .build());

        IllegalStateException missingLocationException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> recursoProdutivoFrontServiceComSnapshotSemLocation.saveRecursoProdutivoDTO(recursoProdutivoDTO));

        Assertions.assertEquals(
                "Saved Production Resource location is required.",
                missingLocationException.getMessage());

        RecursoProdutivoFacade recursoProdutivoFrontServiceComSnapshotIdDivergente =
                criaRecursoProdutivoFrontServiceParaTeste(
                        RecursoProdutivo.builder()
                                .id("RES-02")
                                .location(criaLocationParaTeste("LOC-01"))
                                .build());

        IllegalStateException divergentIdException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> recursoProdutivoFrontServiceComSnapshotIdDivergente.saveRecursoProdutivoDTO(recursoProdutivoDTO));

        Assertions.assertEquals(
                "Saved Production Resource id must match requested id.",
                divergentIdException.getMessage());

        RecursoProdutivoFacade recursoProdutivoFrontServiceComSnapshotLocationDivergente =
                criaRecursoProdutivoFrontServiceParaTeste(
                        RecursoProdutivo.builder()
                                .id("RES-01")
                                .location(criaLocationParaTeste("LOC-02"))
                                .build());

        IllegalStateException divergentLocationException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> recursoProdutivoFrontServiceComSnapshotLocationDivergente.saveRecursoProdutivoDTO(recursoProdutivoDTO));

        Assertions.assertEquals(
                "Saved Production Resource location must match requested location.",
                divergentLocationException.getMessage());

    }

    private static RecursoProdutivoFacade criaRecursoProdutivoFrontServiceParaListagem(
            List<RecursoProdutivo> recursoProdutivoList) throws Exception {

        RecursoProdutivoFacade recursoProdutivoFrontService =
                new RecursoProdutivoFacade();
        setPrivateField(
                recursoProdutivoFrontService,
                "recursoProdutivoRepository",
                getRecursoProdutivoRepositoryParaCustomFindAllWithLocationRetornando(
                        recursoProdutivoList));
        return recursoProdutivoFrontService;

    }

    private static LinhaTransporteFacade criaLinhaTransporteFrontServiceParaListagemVersaoMalha(
            List<VersaoMalha> versaoMalhaList) throws Exception {

                LinhaTransporteFacade linhaTransporteFrontService =
                new LinhaTransporteFacade();
        setPrivateField(
                linhaTransporteFrontService,
                "versaoMalhaRepository",
                getVersaoMalhaRepositoryParaFindAllRetornando(versaoMalhaList));
        return linhaTransporteFrontService;

    }

    private static LinhaTransporteFacade criaLinhaTransporteFrontServiceParaListagemLinhaTransporte(
            List<LinhaTransporte> linhaTransporteList) throws Exception {

        LinhaTransporteFacade linhaTransporteFrontService =
                new LinhaTransporteFacade();
        setPrivateField(
                linhaTransporteFrontService,
                "versaoMalhaRepository",
                getVersaoMalhaRepositoryParaFindByIdRetornando(Optional.of(criaVersaoMalhaParaTeste())));
        setPrivateField(
                linhaTransporteFrontService,
                "linhaTransporteRepository",
                getLinhaTransporteRepositoryParaFindForFrontByVersaoMalhaRetornando(linhaTransporteList));
        return linhaTransporteFrontService;

    }

    private static LinhaTransporteFacade criaLinhaTransporteFrontServiceParaListagemLinhaTransporteProduto(
            List<LinhaTransporteProduto> linhaTransporteProdutoList) throws Exception {

        LinhaTransporteFacade linhaTransporteFrontService =
                new LinhaTransporteFacade();
        setPrivateField(
                linhaTransporteFrontService,
                "versaoMalhaRepository",
                getVersaoMalhaRepositoryParaFindByIdRetornando(Optional.of(criaVersaoMalhaParaTeste())));
        setPrivateField(
                linhaTransporteFrontService,
                "linhaTransporteProdutoRepository",
                getLinhaTransporteProdutoRepositoryParaFindForFrontByVersaoMalhaRetornando(linhaTransporteProdutoList));
        return linhaTransporteFrontService;

    }

    private static RoteiroFacade criaRoteiroFrontServiceParaListagemRoteiro(
            List<Roteiro> roteiroList) throws Exception {

        RoteiroFacade roteiroFrontService = new RoteiroFacade();
        setPrivateField(
                roteiroFrontService,
                "roteiroRepository",
                getRoteiroRepositoryParaListagemFrontRetornando(roteiroList));
        return roteiroFrontService;

    }

    private static RoteiroFacade criaRoteiroFrontServiceParaListagemOperacao(
            List<OperacaoRoteiro> operacaoRoteiroList) throws Exception {

        RoteiroFacade roteiroFrontService = new RoteiroFacade();
        setPrivateField(
                roteiroFrontService,
                "operacaoRoteiroRepository",
                getOperacaoRoteiroRepositoryParaListagemFrontRetornando(operacaoRoteiroList));
        return roteiroFrontService;

    }

    private static ListaTecnicaFacade criaListaTecnicaFrontServiceParaListagemListaTecnica(
            List<ListaTecnica> listaTecnicaList,
            ParametrosGlobais parametrosGlobais) throws Exception {

        ListaTecnicaFacade listaTecnicaFrontService =
                new ListaTecnicaFacade();
        setPrivateField(
                listaTecnicaFrontService,
                "parametrosGlobaisService",
                getParametrosGlobaisServiceRetornando(parametrosGlobais));
        setPrivateField(
                listaTecnicaFrontService,
                "listaTecnicaRepository",
                getListaTecnicaRepositoryParaFindAllRetornando(listaTecnicaList));
        return listaTecnicaFrontService;

    }

    private static ListaTecnicaFacade criaListaTecnicaFrontServiceParaListagemComponente(
            List<ListaTecnicaComponente> listaTecnicaComponenteList,
            ParametrosGlobais parametrosGlobais) throws Exception {

        ListaTecnicaFacade listaTecnicaFrontService =
                new ListaTecnicaFacade();
        setPrivateField(
                listaTecnicaFrontService,
                "parametrosGlobaisService",
                getParametrosGlobaisServiceRetornando(parametrosGlobais));
        setPrivateField(
                listaTecnicaFrontService,
                "listaTecnicaComponenteRepository",
                getListaTecnicaComponenteRepositoryParaFindAllRetornando(listaTecnicaComponenteList));
        return listaTecnicaFrontService;

    }

    private static RecursoProdutivoFacade criaRecursoProdutivoFrontServiceParaTeste(
            RecursoProdutivo recursoProdutivoSalvo) throws Exception {

        RecursoProdutivoFacade recursoProdutivoFrontService =
                new RecursoProdutivoFacade();
        setPrivateField(
                recursoProdutivoFrontService,
                "recursoProdutivoRepository",
                getRecursoProdutivoRepositoryParaSaveRetornando(recursoProdutivoSalvo));
        setPrivateField(
                recursoProdutivoFrontService,
                "locationRepository",
                getLocationRepositoryComLocation(criaLocationParaTeste("LOC-01")));
        return recursoProdutivoFrontService;

    }

    private static LinhaTransporteFacade criaLinhaTransporteFrontServiceParaSaveVersaoMalha(
            VersaoMalha versaoMalhaSalva) throws Exception {

        LinhaTransporteFacade linhaTransporteFrontService =
                new LinhaTransporteFacade();
        setPrivateField(
                linhaTransporteFrontService,
                "versaoMalhaRepository",
                getVersaoMalhaRepositoryParaSaveRetornando(versaoMalhaSalva));
        setPrivateField(
                linhaTransporteFrontService,
                "versaoMalhaAutoMapper",
                getVersaoMalhaAutoMapperParaTeste());
        return linhaTransporteFrontService;

    }

    private static LinhaTransporteFacade criaLinhaTransporteFrontServiceParaSaveLinhaTransporte(
            LinhaTransporte linhaTransporteMapeada,
            LinhaTransporte linhaTransporteSalva) throws Exception {

        LinhaTransporteFacade linhaTransporteFrontService =
                new LinhaTransporteFacade();
        setPrivateField(
                linhaTransporteFrontService,
                "linhaTransporteRepository",
                getLinhaTransporteRepositoryParaSaveRetornando(linhaTransporteSalva));
        setPrivateField(
                linhaTransporteFrontService,
                "linhaTransporteIntegrationMapper",
                new LinhaTransporteIntegrationMapperStub(linhaTransporteMapeada));
        setPrivateField(
                linhaTransporteFrontService,
                "linhaTransporteIntegrationService",
                new LinhaTransporteIntegrationServiceStub());
        return linhaTransporteFrontService;

    }

    private static LinhaTransporteFacade criaLinhaTransporteFrontServiceParaSaveLinhaTransporteProduto(
            LinhaTransporteProduto linhaTransporteProdutoMapeada,
            LinhaTransporteProduto linhaTransporteProdutoSalva) throws Exception {

        LinhaTransporteFacade linhaTransporteFrontService =
                new LinhaTransporteFacade();
        setPrivateField(
                linhaTransporteFrontService,
                "linhaTransporteProdutoRepository",
                getLinhaTransporteProdutoRepositoryParaSaveRetornando(linhaTransporteProdutoSalva));
        setPrivateField(
                linhaTransporteFrontService,
                "linhaTransporteProdutoIntegrationMapper",
                new LinhaTransporteProdutoIntegrationMapperStub(linhaTransporteProdutoMapeada));
        setPrivateField(
                linhaTransporteFrontService,
                "linhaTransporteProdutoIntegrationService",
                new LinhaTransporteProdutoIntegrationServiceStub());
        return linhaTransporteFrontService;

    }

    private static void invokeValidaLinhaTransporteEntityListParaDeleteCommunity(
            LinhaTransporteFacade linhaTransporteFrontService,
            List<LinhaTransporte> linhaTransporteListParaDelete) throws Exception {

        Method validaLinhaTransporteEntityListParaDeleteCommunityMethod =
                LinhaTransporteFacade.class.getDeclaredMethod(
                        "validaLinhaTransporteEntityListParaDeleteCommunity",
                        List.class);
        validaLinhaTransporteEntityListParaDeleteCommunityMethod.setAccessible(true);
        validaLinhaTransporteEntityListParaDeleteCommunityMethod.invoke(
                linhaTransporteFrontService,
                linhaTransporteListParaDelete);

    }

    private static void invokeValidaLinhaTransporteProdutoEntityListParaDeleteCommunity(
            LinhaTransporteFacade linhaTransporteFrontService,
            List<LinhaTransporteProduto> linhaTransporteProdutoListParaDelete) throws Exception {

        Method validaLinhaTransporteProdutoEntityListParaDeleteCommunityMethod =
                LinhaTransporteFacade.class.getDeclaredMethod(
                        "validaLinhaTransporteProdutoEntityListParaDeleteCommunity",
                        List.class);
        validaLinhaTransporteProdutoEntityListParaDeleteCommunityMethod.setAccessible(true);
        validaLinhaTransporteProdutoEntityListParaDeleteCommunityMethod.invoke(
                linhaTransporteFrontService,
                linhaTransporteProdutoListParaDelete);

    }

    private static LinhaTransporte criaLinhaTransporteValidaParaTeste() {

        return criaLinhaTransporteValidaParaTeste("Default");

    }

    private static LinhaTransporte criaLinhaTransporteValidaParaTeste(String versaoMalhaId) {

        return new LinhaTransporte(
                new LinhaTransporte.LinhaTransporteCompositeKey(
                        criaVersaoMalhaParaTeste(versaoMalhaId),
                        criaLocationParaTeste("LOC-ORIGEM"),
                        criaLocationParaTeste("LOC-DESTINO")));

    }

    private static LinhaTransporteProduto criaLinhaTransporteProdutoValidaParaTeste() {

        return new LinhaTransporteProduto(
                new LinhaTransporteProduto.LinhaTransporteProdutoCompositeKey(
                        criaLinhaTransporteValidaParaTeste(),
                        criaMaterialParaTeste()));

    }

    private static LinhaTransporteIntegrationDataDto criaLinhaTransporteIntegrationDataDtoParaTeste() {

        LinhaTransporteIntegrationDataDto linhaTransporteIntegrationDataDto =
                new LinhaTransporteIntegrationDataDto();
        linhaTransporteIntegrationDataDto.primaryKeyDto =
                new LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO(
                        "Default",
                        "LOC-ORIGEM",
                        "LOC-DESTINO");
        return linhaTransporteIntegrationDataDto;

    }

    private static LinhaTransporteProdutoIntegrationDataDto criaLinhaTransporteProdutoIntegrationDataDtoParaTeste() {

        LinhaTransporteProdutoIntegrationDataDto linhaTransporteProdutoIntegrationDataDto =
                new LinhaTransporteProdutoIntegrationDataDto();
        linhaTransporteProdutoIntegrationDataDto.primaryKeyDto =
                new LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO(
                        "Default",
                        "LOC-ORIGEM",
                        "LOC-DESTINO",
                        "MAT-01");
        return linhaTransporteProdutoIntegrationDataDto;

    }

    private static VersaoMalha criaVersaoMalhaParaTeste() {

        return criaVersaoMalhaParaTeste("Default");

    }

    private static VersaoMalha criaVersaoMalhaParaTeste(String versaoMalhaId) {

        VersaoMalha versaoMalha = new VersaoMalha();
        versaoMalha.setId(versaoMalhaId);
        return versaoMalha;

    }

    private static Location criaLocationParaTeste(String id) {

        Location location = new Location();
        location.setId(id);
        return location;

    }

    private static Produto criaMaterialParaTeste() {

        Produto material = new Produto();
        material.setId("MAT-01");
        return material;

    }

    private static RecursoProdutivo criaRecursoProdutivoValidoParaTeste() {

        return RecursoProdutivo.builder()
                .id("RES-01")
                .location(criaLocationParaTeste("LOC-01"))
                .build();

    }

    private static ParametrosGlobais criaParametrosGlobaisParaTeste(String unidadeMedidaPadraoSnpId) {

        UnidadeMedida unidadeMedidaPadraoSnp = new UnidadeMedida();
        unidadeMedidaPadraoSnp.setId(unidadeMedidaPadraoSnpId);

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        parametrosGlobais.setUnidadeMedidaPadraoSNP(unidadeMedidaPadraoSnp);
        return parametrosGlobais;

    }

    private static ListaTecnica criaListaTecnicaValidaParaTeste() {

        return ListaTecnica.builder()
                .id("BOM-01")
                .location(criaLocationParaTeste("LOC-01"))
                .materialOutput(criaMaterialParaTeste())
                .build();

    }

    private static ListaTecnicaDTO criaListaTecnicaDTOValidaParaTeste() {

        ListaTecnicaDTO listaTecnicaDTO = new ListaTecnicaDTO();
        listaTecnicaDTO.setId("BOM-01");
        listaTecnicaDTO.setOutputMaterialId("MAT-01");
        listaTecnicaDTO.setOutputUnitOfMeasureId("UN");
        listaTecnicaDTO.setOutputQuantity(1.0F);

        return listaTecnicaDTO;

    }

    private static ListaTecnicaComponente criaListaTecnicaComponenteValidaParaTeste() {

        Produto materialComponente = new Produto();
        materialComponente.setId("MAT-COMP-01");

        return new ListaTecnicaComponente(
                new ListaTecnicaComponente.ListaTecnicaComponenteCompositeKey(
                        criaListaTecnicaValidaParaTeste(),
                        materialComponente));

    }

    private static ListaTecnicaComponenteDTO criaListaTecnicaComponenteDTOValidaParaTeste() {

        ListaTecnicaComponenteDTO listaTecnicaComponenteDTO =
                new ListaTecnicaComponenteDTO();
        listaTecnicaComponenteDTO.setBillOfMaterialsId("BOM-01");
        listaTecnicaComponenteDTO.setComponentMaterialId("MAT-COMP-01");
        listaTecnicaComponenteDTO.setComponentMaterialUnitOfMeasureId("UN");
        listaTecnicaComponenteDTO.setQuantity(1.0F);

        return listaTecnicaComponenteDTO;

    }

    private static Roteiro criaRoteiroValidoParaTeste() {

        return Roteiro.builder()
                .id("ROUT-01")
                .location(criaLocationParaTeste("LOC-01"))
                .materialOutput(criaMaterialParaTeste())
                .build();

    }

    private static RoteiroDTO criaRoteiroDTOValidoParaTeste() {

        RoteiroDTO roteiroDTO = new RoteiroDTO();
        roteiroDTO.setId("ROUT-01");
        roteiroDTO.setLocationId("LOC-01");
        roteiroDTO.setOutputMaterialId("MAT-01");

        return roteiroDTO;

    }

    private static OperacaoRoteiro criaOperacaoRoteiroValidaParaTeste() {

        OperacaoRoteiro operacaoRoteiro = new OperacaoRoteiro(
                new OperacaoRoteiro.OperacaoRoteiroCompositeKey(
                        10,
                        criaRoteiroValidoParaTeste()));
        operacaoRoteiro.setRecursoProdutivo(RecursoProdutivo.builder()
                .id("RES-01")
                .location(criaLocationParaTeste("LOC-01"))
                .build());

        return operacaoRoteiro;

    }

    private static OperacaoRoteiroDTO criaOperacaoRoteiroDTOValidaParaTeste() {

        OperacaoRoteiroDTO operacaoRoteiroDTO = new OperacaoRoteiroDTO();
        operacaoRoteiroDTO.setRoutingId("ROUT-01");
        operacaoRoteiroDTO.setOperationPosition(10);
        operacaoRoteiroDTO.setProductionResourceId("RES-01");

        return operacaoRoteiroDTO;

    }

    private static ParametrosGlobaisService getParametrosGlobaisServiceRetornando(
            ParametrosGlobais parametrosGlobais) {

        return new ParametrosGlobaisServiceStub(parametrosGlobais);

    }

    private static RecursoProdutivoRepository
            getRecursoProdutivoRepositoryParaCustomFindAllWithLocationRetornando(
            List<RecursoProdutivo> recursoProdutivoList) {

        return (RecursoProdutivoRepository) Proxy.newProxyInstance(
                RecursoProdutivoRepository.class.getClassLoader(),
                new Class<?>[]{RecursoProdutivoRepository.class},
                (proxy, method, args) -> {
                    if ("customFindAllWithLocation".equals(method.getName())) {
                        return recursoProdutivoList;
                    }
                    if ("toString".equals(method.getName())) {
                        return "RecursoProdutivoRepository snapshot administrativo para teste Community";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

    }

    private static ListaTecnicaRepository getListaTecnicaRepositoryParaFindAllRetornando(
            List<ListaTecnica> listaTecnicaList) {

        return (ListaTecnicaRepository) Proxy.newProxyInstance(
                ListaTecnicaRepository.class.getClassLoader(),
                new Class<?>[]{ListaTecnicaRepository.class},
                (proxy, method, args) -> {
                    if ("customFindAllWithLocationMaterialOutputAndUnidadeMedidaMaterialOutput"
                            .equals(method.getName())) {
                        return listaTecnicaList;
                    }
                    if ("toString".equals(method.getName())) {
                        return "ListaTecnicaRepository snapshot administrativo para teste Community";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

    }

    private static ListaTecnicaComponenteRepository getListaTecnicaComponenteRepositoryParaFindAllRetornando(
            List<ListaTecnicaComponente> listaTecnicaComponenteList) {

        return (ListaTecnicaComponenteRepository) Proxy.newProxyInstance(
                ListaTecnicaComponenteRepository.class.getClassLoader(),
                new Class<?>[]{ListaTecnicaComponenteRepository.class},
                (proxy, method, args) -> {
                    if ("customFindAll".equals(method.getName())
                            || "findAll".equals(method.getName())) {
                        return listaTecnicaComponenteList;
                    }
                    if ("toString".equals(method.getName())) {
                        return "ListaTecnicaComponenteRepository findAll para teste Community";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

    }

    private static ListaTecnicaAutoMapper getListaTecnicaAutoMapperRetornando(
            List<ListaTecnicaDTO> listaTecnicaDTOList) {

        return (ListaTecnicaAutoMapper) Proxy.newProxyInstance(
                ListaTecnicaAutoMapper.class.getClassLoader(),
                new Class<?>[]{ListaTecnicaAutoMapper.class},
                (proxy, method, args) -> {
                    if ("converteListaEntidadeParaListaDTO".equals(method.getName())) {
                        return listaTecnicaDTOList;
                    }
                    if ("toString".equals(method.getName())) {
                        return "ListaTecnicaAutoMapper para teste Community";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

    }

    private static ListaTecnicaComponenteAutoMapper getListaTecnicaComponenteAutoMapperRetornando(
            List<ListaTecnicaComponenteDTO> listaTecnicaComponenteDTOList) {

        return (ListaTecnicaComponenteAutoMapper) Proxy.newProxyInstance(
                ListaTecnicaComponenteAutoMapper.class.getClassLoader(),
                new Class<?>[]{ListaTecnicaComponenteAutoMapper.class},
                (proxy, method, args) -> {
                    if ("converteListaEntidadeParaListaDTO".equals(method.getName())) {
                        return listaTecnicaComponenteDTOList;
                    }
                    if ("toString".equals(method.getName())) {
                        return "ListaTecnicaComponenteAutoMapper para teste Community";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

    }

    private static RecursoProdutivoAutoMapper getRecursoProdutivoAutoMapperRetornando(
            List<RecursoProdutivoDTO> recursoProdutivoDTOList) {

        return (RecursoProdutivoAutoMapper) Proxy.newProxyInstance(
                RecursoProdutivoAutoMapper.class.getClassLoader(),
                new Class<?>[]{RecursoProdutivoAutoMapper.class},
                (proxy, method, args) -> {
                    if ("converteListaEntidadeParaListaDTO".equals(method.getName())) {
                        return recursoProdutivoDTOList;
                    }
                    if ("toString".equals(method.getName())) {
                        return "RecursoProdutivoAutoMapper para teste Community";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

    }

    private static RoteiroAutoMapper getRoteiroAutoMapperRetornando(
            List<RoteiroDTO> roteiroDTOList) {

        return (RoteiroAutoMapper) Proxy.newProxyInstance(
                RoteiroAutoMapper.class.getClassLoader(),
                new Class<?>[]{RoteiroAutoMapper.class},
                (proxy, method, args) -> {
                    if ("converteListaEntidadeParaListaDTO".equals(method.getName())) {
                        return roteiroDTOList;
                    }
                    if ("toString".equals(method.getName())) {
                        return "RoteiroAutoMapper para teste Community";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

    }

    private static OperacaoRoteiroAutoMapper getOperacaoRoteiroAutoMapperRetornando(
            List<OperacaoRoteiroDTO> operacaoRoteiroDTOList) {

        return (OperacaoRoteiroAutoMapper) Proxy.newProxyInstance(
                OperacaoRoteiroAutoMapper.class.getClassLoader(),
                new Class<?>[]{OperacaoRoteiroAutoMapper.class},
                (proxy, method, args) -> {
                    if ("converteListaEntidadeParaListaDTO".equals(method.getName())) {
                        return operacaoRoteiroDTOList;
                    }
                    if ("toString".equals(method.getName())) {
                        return "OperacaoRoteiroAutoMapper para teste Community";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

    }

    private static RoteiroRepository getRoteiroRepositoryParaListagemFrontRetornando(
            List<Roteiro> roteiroList) {

        return (RoteiroRepository) Proxy.newProxyInstance(
                RoteiroRepository.class.getClassLoader(),
                new Class<?>[]{RoteiroRepository.class},
                (proxy, method, args) -> {
                    if ("customFindAllForFront".equals(method.getName())
                            || "customFindAllForConsistencyDiagnostic".equals(method.getName())) {
                        return roteiroList;
                    }
                    if ("toString".equals(method.getName())) {
                        return "RoteiroRepository listagem front para teste Community";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

    }

    private static OperacaoRoteiroRepository getOperacaoRoteiroRepositoryParaListagemFrontRetornando(
            List<OperacaoRoteiro> operacaoRoteiroList) {

        return (OperacaoRoteiroRepository) Proxy.newProxyInstance(
                OperacaoRoteiroRepository.class.getClassLoader(),
                new Class<?>[]{OperacaoRoteiroRepository.class},
                (proxy, method, args) -> {
                    if ("customFindAllForFront".equals(method.getName())) {
                        return operacaoRoteiroList;
                    }
                    if ("toString".equals(method.getName())) {
                        return "OperacaoRoteiroRepository listagem front para teste Community";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

    }

    private static RecursoProdutivoRepository getRecursoProdutivoRepositoryParaSaveRetornando(
            RecursoProdutivo recursoProdutivoSalvo) {

        return (RecursoProdutivoRepository) Proxy.newProxyInstance(
                RecursoProdutivoRepository.class.getClassLoader(),
                new Class<?>[]{RecursoProdutivoRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return Optional.empty();
                    }
                    if ("save".equals(method.getName())) {
                        return recursoProdutivoSalvo;
                    }
                    if ("toString".equals(method.getName())) {
                        return "RecursoProdutivoRepository para teste Community";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

    }

    private static RecursoProdutivoRepository getRecursoProdutivoRepositoryParaFindByIdRetornando(
            Optional<RecursoProdutivo> recursoProdutivoOptional) {

        return (RecursoProdutivoRepository) Proxy.newProxyInstance(
                RecursoProdutivoRepository.class.getClassLoader(),
                new Class<?>[]{RecursoProdutivoRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return recursoProdutivoOptional;
                    }
                    if ("toString".equals(method.getName())) {
                        return "RecursoProdutivoRepository findById para teste Community";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

    }

    private static VersaoMalhaRepository getVersaoMalhaRepositoryParaFindAllRetornando(
            List<VersaoMalha> versaoMalhaList) {

        return (VersaoMalhaRepository) Proxy.newProxyInstance(
                VersaoMalhaRepository.class.getClassLoader(),
                new Class<?>[]{VersaoMalhaRepository.class},
                (proxy, method, args) -> {
                    if ("findAll".equals(method.getName())
                            || "customFindAll".equals(method.getName())) {
                        return versaoMalhaList;
                    }
                    if ("toString".equals(method.getName())) {
                        return "VersaoMalhaRepository findAll para teste Community";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

    }

    private static VersaoMalhaRepository getVersaoMalhaRepositoryParaSaveRetornando(
            VersaoMalha versaoMalhaSalva) {

        return (VersaoMalhaRepository) Proxy.newProxyInstance(
                VersaoMalhaRepository.class.getClassLoader(),
                new Class<?>[]{VersaoMalhaRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return Optional.empty();
                    }
                    if ("save".equals(method.getName())) {
                        return versaoMalhaSalva;
                    }
                    if ("toString".equals(method.getName())) {
                        return "VersaoMalhaRepository para teste Community";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

    }

    private static VersaoMalhaRepository getVersaoMalhaRepositoryParaFindByIdRetornando(
            Optional<VersaoMalha> versaoMalhaOptional) {

        return (VersaoMalhaRepository) Proxy.newProxyInstance(
                VersaoMalhaRepository.class.getClassLoader(),
                new Class<?>[]{VersaoMalhaRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return versaoMalhaOptional;
                    }
                    if ("toString".equals(method.getName())) {
                        return "VersaoMalhaRepository findById para teste Community";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

    }

    private static LinhaTransporteRepository getLinhaTransporteRepositoryParaSaveRetornando(
            LinhaTransporte linhaTransporteSalva) {

        return (LinhaTransporteRepository) Proxy.newProxyInstance(
                LinhaTransporteRepository.class.getClassLoader(),
                new Class<?>[]{LinhaTransporteRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        return linhaTransporteSalva;
                    }
                    if ("toString".equals(method.getName())) {
                        return "LinhaTransporteRepository para teste Community";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

    }

    private static LinhaTransporteRepository getLinhaTransporteRepositoryParaFindForFrontByVersaoMalhaRetornando(
            List<LinhaTransporte> linhaTransporteList) {

        return (LinhaTransporteRepository) Proxy.newProxyInstance(
                LinhaTransporteRepository.class.getClassLoader(),
                new Class<?>[]{LinhaTransporteRepository.class},
                (proxy, method, args) -> {
                    if ("customFindForFrontByVersaoMalha".equals(method.getName())) {
                        return linhaTransporteList;
                    }
                    if ("toString".equals(method.getName())) {
                        return "LinhaTransporteRepository findByVersaoMalha para teste Community";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

    }

    private static LinhaTransporteProdutoRepository getLinhaTransporteProdutoRepositoryParaSaveRetornando(
            LinhaTransporteProduto linhaTransporteProdutoSalva) {

        return (LinhaTransporteProdutoRepository) Proxy.newProxyInstance(
                LinhaTransporteProdutoRepository.class.getClassLoader(),
                new Class<?>[]{LinhaTransporteProdutoRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        return linhaTransporteProdutoSalva;
                    }
                    if ("toString".equals(method.getName())) {
                        return "LinhaTransporteProdutoRepository para teste Community";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

    }

    private static LinhaTransporteProdutoRepository getLinhaTransporteProdutoRepositoryParaFindForFrontByVersaoMalhaRetornando(
            List<LinhaTransporteProduto> linhaTransporteProdutoList) {

        return (LinhaTransporteProdutoRepository) Proxy.newProxyInstance(
                LinhaTransporteProdutoRepository.class.getClassLoader(),
                new Class<?>[]{LinhaTransporteProdutoRepository.class},
                (proxy, method, args) -> {
                    if ("customFindForFrontByVersaoMalha".equals(method.getName())) {
                        return linhaTransporteProdutoList;
                    }
                    if ("toString".equals(method.getName())) {
                        return "LinhaTransporteProdutoRepository findByVersaoMalha para teste Community";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

    }

    private static VersaoMalhaAutoMapper getVersaoMalhaAutoMapperParaTeste() {

        return (VersaoMalhaAutoMapper) Proxy.newProxyInstance(
                VersaoMalhaAutoMapper.class.getClassLoader(),
                new Class<?>[]{VersaoMalhaAutoMapper.class},
                (proxy, method, args) -> {
                    if ("converte".equals(method.getName())
                            && args != null
                            && args.length == 1
                            && args[0] instanceof VersaoMalhaDTO versaoMalhaDTO) {
                        VersaoMalha versaoMalha = new VersaoMalha();
                        versaoMalha.setId(versaoMalhaDTO.getId());
                        return versaoMalha;
                    }
                    if ("toString".equals(method.getName())) {
                        return "VersaoMalhaAutoMapper para teste Community";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

    }

    private static VersaoMalhaAutoMapper getVersaoMalhaAutoMapperParaListagemRetornando(
            List<VersaoMalhaDTO> versaoMalhaDTOList) {

        return (VersaoMalhaAutoMapper) Proxy.newProxyInstance(
                VersaoMalhaAutoMapper.class.getClassLoader(),
                new Class<?>[]{VersaoMalhaAutoMapper.class},
                (proxy, method, args) -> {
                    if ("converteListaEntidadesParaDTOs".equals(method.getName())) {
                        return versaoMalhaDTOList;
                    }
                    if ("toString".equals(method.getName())) {
                        return "VersaoMalhaAutoMapper listagem para teste Community";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

    }

    private static class LinhaTransporteIntegrationMapperStub extends LinhaTransporteIntegrationMapper {

        private final LinhaTransporte linhaTransporteMapeada;
        private final List<LinhaTransporteIntegrationDataDto> linhaTransporteIntegrationDataDtoListMapeada;

        private LinhaTransporteIntegrationMapperStub(LinhaTransporte linhaTransporteMapeada) {

            this(linhaTransporteMapeada, null);

        }

        private LinhaTransporteIntegrationMapperStub(
                LinhaTransporte linhaTransporteMapeada,
                List<LinhaTransporteIntegrationDataDto> linhaTransporteIntegrationDataDtoListMapeada) {

            this.linhaTransporteMapeada = linhaTransporteMapeada;
            this.linhaTransporteIntegrationDataDtoListMapeada =
                    linhaTransporteIntegrationDataDtoListMapeada;
        }

        @Override
        public LinhaTransporte convertDTOToEntity(
                LinhaTransporteIntegrationDataDto dto,
                Map<LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO, LinhaTransporte> currentlyPersistedEntitiesByPrimaryKey,
                LinhaTransporteIntegrationSupportData supportData,
                Map<String, MetodoAtualizacaoCampo> camposASobrecrever) {

            return linhaTransporteMapeada;

        }

        @Override
        public List<LinhaTransporteIntegrationDataDto> convertEntityCollectionToDTOList(
                Collection<LinhaTransporte> entityList) {

            return linhaTransporteIntegrationDataDtoListMapeada;

        }

    }

    private static class LinhaTransporteProdutoIntegrationMapperStub extends LinhaTransporteProdutoIntegrationMapper {

        private final LinhaTransporteProduto linhaTransporteProdutoMapeada;
        private final List<LinhaTransporteProdutoIntegrationDataDto> linhaTransporteProdutoIntegrationDataDtoListMapeada;

        private LinhaTransporteProdutoIntegrationMapperStub(
                LinhaTransporteProduto linhaTransporteProdutoMapeada) {

            this(linhaTransporteProdutoMapeada, null);

        }

        private LinhaTransporteProdutoIntegrationMapperStub(
                LinhaTransporteProduto linhaTransporteProdutoMapeada,
                List<LinhaTransporteProdutoIntegrationDataDto> linhaTransporteProdutoIntegrationDataDtoListMapeada) {

            this.linhaTransporteProdutoMapeada = linhaTransporteProdutoMapeada;
            this.linhaTransporteProdutoIntegrationDataDtoListMapeada =
                    linhaTransporteProdutoIntegrationDataDtoListMapeada;
        }

        @Override
        public LinhaTransporteProduto convertDTOToEntity(
                LinhaTransporteProdutoIntegrationDataDto dto,
                Map<LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO, LinhaTransporteProduto> currentlyPersistedEntitiesByPrimaryKey,
                LinhaTransporteProdutoIntegrationSupportData supportData,
                Map<String, MetodoAtualizacaoCampo> camposASobrecrever) {

            return linhaTransporteProdutoMapeada;

        }

        @Override
        public List<LinhaTransporteProdutoIntegrationDataDto> convertEntityCollectionToDTOList(
                Collection<LinhaTransporteProduto> entityList) {

            return linhaTransporteProdutoIntegrationDataDtoListMapeada;

        }

    }

    private static class LinhaTransporteIntegrationServiceStub extends LinhaTransporteIntegrationService {

        @Override
        public Map<LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO, LinhaTransporte>
        getPersistedEntityMapFromPrimaryKeyDtoCollection(
                Collection<LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO> dtoBatchList) {

            return Map.of();

        }

        @Override
        public LinhaTransporteIntegrationSupportData getSupportData() {

            return new LinhaTransporteIntegrationSupportData();

        }

    }

    private static class LinhaTransporteProdutoIntegrationServiceStub extends LinhaTransporteProdutoIntegrationService {

        @Override
        public Map<LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO, LinhaTransporteProduto>
        getPersistedEntityMapFromPrimaryKeyDtoCollection(
                Collection<LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO> dtoBatchList) {

            return Map.of();

        }

        @Override
        public LinhaTransporteProdutoIntegrationSupportData getSupportData() {

            return new LinhaTransporteProdutoIntegrationSupportData();

        }

    }

    private static class ParametrosGlobaisServiceStub extends ParametrosGlobaisService {

        private final ParametrosGlobais parametrosGlobais;

        private ParametrosGlobaisServiceStub(ParametrosGlobais parametrosGlobais) {

            this.parametrosGlobais = parametrosGlobais;

        }

        @Override
        public ParametrosGlobais getParametrosGlobais() {

            return parametrosGlobais;

        }

    }

    private static LocationRepository getLocationRepositoryComLocation(
            Location location) {

        return getLocationRepositoryRetornando(Optional.of(location));

    }

    private static LocationRepository getLocationRepositoryRetornando(
            Optional<Location> locationOptional) {

        return (LocationRepository) Proxy.newProxyInstance(
                LocationRepository.class.getClassLoader(),
                new Class<?>[]{LocationRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return locationOptional;
                    }
                    if ("toString".equals(method.getName())) {
                        return "LocationRepository para teste Community";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

    }

    private static void setPrivateField(
            Object target,
            String fieldName,
            Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

    private static void assertRequiredAutowiredFields(
            Class<?> serviceClass,
            List<String> fieldNames) throws Exception {

        for (String fieldName : fieldNames) {
            Field field = serviceClass.getDeclaredField(fieldName);
            Autowired autowired = field.getAnnotation(Autowired.class);

            Assertions.assertNotNull(
                    autowired,
                    serviceClass.getSimpleName() + "." + fieldName + " must declare @Autowired explicitly");
            Assertions.assertTrue(
                    autowired.required(),
                    serviceClass.getSimpleName() + "." + fieldName + " must be a required Spring bean");
        }

    }

}
