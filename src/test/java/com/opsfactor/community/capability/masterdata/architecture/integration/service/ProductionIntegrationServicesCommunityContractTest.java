package com.opsfactor.community.capability.masterdata.architecture.integration.service;

import com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.service.ListaTecnicaComponenteIntegrationService;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.service.ListaTecnicaIntegrationService;
import com.opsfactor.community.capability.masterdata.production.operation.integration.service.OperacaoRoteiroIntegrationService;
import com.opsfactor.community.capability.masterdata.production.productionresource.integration.dto.DisponibilidadeRecursoProdutivoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.dto.ListaTecnicaComponenteIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.production.productionresource.integration.dto.RecursoProdutivoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.production.productionresource.integration.service.DisponibilidadeRecursoProdutivoIntegrationService;
import com.opsfactor.community.capability.masterdata.production.productionresource.integration.service.RecursoProdutivoIntegrationService;
import com.opsfactor.community.capability.masterdata.production.productionversion.integration.service.VersaoProducaoIntegrationService;
import com.opsfactor.community.capability.masterdata.production.routing.integration.dto.RoteiroIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.production.productionversion.integration.dto.VersaoProducaoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.operation.domain.OperacaoRoteiro;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.production.routing.integration.service.RoteiroIntegrationService;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.repository.ListaTecnicaRepository;
import com.opsfactor.community.capability.masterdata.production.operation.repository.OperacaoRoteiroRepository;
import com.opsfactor.community.capability.masterdata.production.productionresource.repository.RecursoProdutivoRepository;
import com.opsfactor.community.capability.masterdata.production.routing.repository.RoteiroRepository;
import com.opsfactor.community.capability.masterdata.production.productionversion.repository.VersaoProducaoRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.capability.masterdata.network.location.service.LocationService;
import com.opsfactor.community.capability.masterdata.product.material.service.MaterialService;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFile;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contratos das integracoes Community de master data produtivo.
 *
 * <p>Esses services alimentam o Supply Planning heuristico: recurso produtivo,
 * disponibilidade, roteiro, operacao, BOM, componentes e versao simples de
 * producao. Custos, turnos, manutencao, line scheduling, parallel routing e
 * capacidade por quantidade/UOM permanecem Enterprise e nao devem entrar aqui
 * como dependencias implicitas.</p>
 */
class ProductionIntegrationServicesCommunityContractTest {

    @Test
    void productionIntegrationServicesShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertRequiredAutowiredFields(
                RecursoProdutivoIntegrationService.class,
                List.of(
                        "locationService",
                        "recursoProdutivoRepository",
                        "unidadeMedidaRepository",
                        "recursoProdutivoIntegrationMapper"));
        assertRequiredAutowiredFields(
                DisponibilidadeRecursoProdutivoIntegrationService.class,
                List.of(
                        "recursoProdutivoRepository",
                        "disponibilidadeRecursoProdutivoRepository",
                        "disponibilidadeRecursoProdutivoIntegrationMapper"));
        assertRequiredAutowiredFields(
                RoteiroIntegrationService.class,
                List.of(
                        "locationService",
                        "materialService",
                        "roteiroRepository",
                        "roteiroIntegrationMapper"));
        assertRequiredAutowiredFields(
                OperacaoRoteiroIntegrationService.class,
                List.of(
                        "recursoProdutivoRepository",
                        "roteiroRepository",
                        "operacaoRoteiroRepository"));
        assertRequiredAutowiredFields(
                ListaTecnicaIntegrationService.class,
                List.of(
                        "locationService",
                        "materialService",
                        "unidadeMedidaRepository",
                        "listaTecnicaRepository",
                        "listaTecnicaIntegrationMapper"));
        assertRequiredAutowiredFields(
                ListaTecnicaComponenteIntegrationService.class,
                List.of(
                        "materialService",
                        "unidadeMedidaRepository",
                        "listaTecnicaRepository",
                        "listaTecnicaComponenteRepository",
                        "listaTecnicaComponenteIntegrationMapper"));
        assertRequiredAutowiredFields(
                VersaoProducaoIntegrationService.class,
                List.of(
                        "locationService",
                        "roteiroRepository",
                        "listaTecnicaRepository",
                        "versaoProducaoRepository",
                        "versaoProducaoIntegrationMapper"));

    }

    @Test
    void productionIntegrationServicesShouldKeepStableBatchSizeAndMessages() {

        Assertions.assertEquals(1000, new RecursoProdutivoIntegrationService().getBatchSize());
        Assertions.assertEquals(
                "Production Resource data Saved",
                new RecursoProdutivoIntegrationService().getSaveSuccessMessage());

        Assertions.assertEquals(1000, new DisponibilidadeRecursoProdutivoIntegrationService().getBatchSize());
        Assertions.assertEquals(
                "Production Resources Availability data uploaded",
                new DisponibilidadeRecursoProdutivoIntegrationService().getSaveSuccessMessage());

        Assertions.assertEquals(1000, new RoteiroIntegrationService().getBatchSize());
        Assertions.assertEquals(
                "Routing Data Saved",
                new RoteiroIntegrationService().getSaveSuccessMessage());

        Assertions.assertEquals(1000, new ListaTecnicaIntegrationService().getBatchSize());
        Assertions.assertEquals(
                "Bill of Materials Data Saved",
                new ListaTecnicaIntegrationService().getSaveSuccessMessage());

        Assertions.assertEquals(1000, new ListaTecnicaComponenteIntegrationService().getBatchSize());
        Assertions.assertEquals(
                "Bill of Materials Components Data Saved",
                new ListaTecnicaComponenteIntegrationService().getSaveSuccessMessage());

        Assertions.assertEquals(1000, new VersaoProducaoIntegrationService().getBatchSize());
        Assertions.assertEquals(
                "Production Version data uploaded",
                new VersaoProducaoIntegrationService().getSaveSuccessMessage());

    }

    @Test
    void productionIntegrationServicesShouldRemainSpringComponents() {

        Assertions.assertTrue(RecursoProdutivoIntegrationService.class.isAnnotationPresent(Component.class));
        Assertions.assertTrue(DisponibilidadeRecursoProdutivoIntegrationService.class.isAnnotationPresent(Component.class));
        Assertions.assertTrue(RoteiroIntegrationService.class.isAnnotationPresent(Component.class));
        Assertions.assertTrue(OperacaoRoteiroIntegrationService.class.isAnnotationPresent(Component.class));
        Assertions.assertTrue(ListaTecnicaIntegrationService.class.isAnnotationPresent(Component.class));
        Assertions.assertTrue(ListaTecnicaComponenteIntegrationService.class.isAnnotationPresent(Component.class));

        /*
         * Este service nasceu como @Service por historico local. Ele continua
         * sendo bean Spring, mas o teste explicita a diferenca para que uma
         * futura mudanca seja consciente.
         */
        Assertions.assertTrue(VersaoProducaoIntegrationService.class.isAnnotationPresent(Service.class));

    }

    @Test
    void productionResourceAvailabilityEnvelopeShouldRejectIncompletePrimaryKeyBeforeRepository() {

        DisponibilidadeRecursoProdutivoIntegrationService disponibilidadeRecursoProdutivoIntegrationService =
                new DisponibilidadeRecursoProdutivoIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> disponibilidadeRecursoProdutivoIntegrationService
                        .getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of(
                                new DisponibilidadeRecursoProdutivoIntegrationDataDto
                                        .DisponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO(
                                        null,
                                        null))));

        Assertions.assertEquals(
                "Production resource availability upload primary key must include production resource and reference date",
                dataUploadException.getMessage());

    }

    @Test
    void productionResourcePrimaryKeyShouldRejectDuplicateBeforeRepositoryLookup() {

        RecursoProdutivoIntegrationService recursoProdutivoIntegrationService =
                new RecursoProdutivoIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> recursoProdutivoIntegrationService.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
                        List.of(
                                new RecursoProdutivoIntegrationDataDto.RecursoProdutivoPrimaryKeyIntegrationDTO("PRD-01"),
                                new RecursoProdutivoIntegrationDataDto.RecursoProdutivoPrimaryKeyIntegrationDTO("PRD-01"))));

        Assertions.assertEquals(
                "Production resource primary key collection item at index 1 has duplicated id PRD-01.",
                dataUploadException.getMessage());

    }

    @Test
    void routingPrimaryKeyShouldRejectBlankIdBeforeRepositoryLookup() {

        RoteiroIntegrationService roteiroIntegrationService = new RoteiroIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> roteiroIntegrationService.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
                        List.of(new RoteiroIntegrationDataDto.RoteiroPrimaryKeyIntegrationDTO("  "))));

        Assertions.assertEquals(
                "Routing upload primary key must include routing id",
                dataUploadException.getMessage());

    }

    @Test
    void billOfMaterialsPrimaryKeyShouldRejectNullItemBeforeRepositoryLookup() {

        ListaTecnicaIntegrationService listaTecnicaIntegrationService =
                new ListaTecnicaIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> listaTecnicaIntegrationService.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
                        Collections.singletonList(null)));

        Assertions.assertEquals(
                "Bill of Materials primary key collection item at index 0 is required.",
                dataUploadException.getMessage());

    }

    @Test
    void billOfMaterialsComponentPrimaryKeyShouldRejectDuplicateCompositeKeyBeforeRepositoryLookup() {

        ListaTecnicaComponenteIntegrationService listaTecnicaComponenteIntegrationService =
                new ListaTecnicaComponenteIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> listaTecnicaComponenteIntegrationService.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
                        List.of(
                                new ListaTecnicaComponenteIntegrationDataDto.ListaTecnicaComponentePrimaryKeyIntegrationDTO(
                                        "BOM-01",
                                        "MAT-01"),
                                new ListaTecnicaComponenteIntegrationDataDto.ListaTecnicaComponentePrimaryKeyIntegrationDTO(
                                        "BOM-01",
                                        "MAT-01"))));

        Assertions.assertEquals(
                "Bill of Materials component primary key collection item at index 1 has duplicated key bomId BOM-01 / componentMaterialId MAT-01.",
                dataUploadException.getMessage());

    }

    @Test
    void productionResourceAvailabilityPrimaryKeyShouldRejectDuplicateCompositeKeyBeforeRepositoryLookup() {

        DisponibilidadeRecursoProdutivoIntegrationService disponibilidadeRecursoProdutivoIntegrationService =
                new DisponibilidadeRecursoProdutivoIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> disponibilidadeRecursoProdutivoIntegrationService.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
                        List.of(
                                new DisponibilidadeRecursoProdutivoIntegrationDataDto
                                        .DisponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO(
                                        "PRD-01",
                                        LocalDate.of(2026, 7, 5)),
                                new DisponibilidadeRecursoProdutivoIntegrationDataDto
                                        .DisponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO(
                                        "PRD-01",
                                        LocalDate.of(2026, 7, 5)))));

        Assertions.assertEquals(
                "Production resource availability primary key collection item at index 1 has duplicated key productionResourceId PRD-01 / referenceDate 2026-07-05.",
                dataUploadException.getMessage());

    }

    @Test
    void productionVersionPrimaryKeyShouldRejectMissingIdBeforeRepositoryLookup() {

        VersaoProducaoIntegrationService versaoProducaoIntegrationService =
                new VersaoProducaoIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> versaoProducaoIntegrationService.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
                        List.of(new VersaoProducaoIntegrationDataDto
                                .VersaoProducaoPrimaryKeyIntegrationDTO(null))));

        Assertions.assertEquals(
                "Production version upload primary key must include production version id",
                dataUploadException.getMessage());

    }

    @Test
    void productionPrimaryKeyLookupsShouldAcceptEmptyCollectionWithoutRepositoryLookup() {

        Assertions.assertTrue(new RecursoProdutivoIntegrationService()
                .getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of())
                .isEmpty());
        Assertions.assertTrue(new RoteiroIntegrationService()
                .getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of())
                .isEmpty());
        Assertions.assertTrue(new ListaTecnicaIntegrationService()
                .getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of())
                .isEmpty());
        Assertions.assertTrue(new ListaTecnicaComponenteIntegrationService()
                .getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of())
                .isEmpty());
        Assertions.assertTrue(new DisponibilidadeRecursoProdutivoIntegrationService()
                .getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of())
                .isEmpty());
        Assertions.assertTrue(new VersaoProducaoIntegrationService()
                .getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of())
                .isEmpty());

    }

    @Test
    void routingOperationUploadShouldRejectNullSupportDataSnapshotBeforeIndexing() {

        RecursoProdutivoRepository recursoProdutivoRepository =
                criaRepositoryProxy(
                        RecursoProdutivoRepository.class,
                        "findAll",
                        null);
        RoteiroRepository roteiroRepository =
                criaRepositoryProxy(
                        RoteiroRepository.class,
                        "findAll",
                        List.of());
        OperacaoRoteiroRepository operacaoRoteiroRepository =
                criaRepositoryProxy(
                        OperacaoRoteiroRepository.class,
                        "customFindAll",
                        List.of());
        OperacaoRoteiroIntegrationService operacaoRoteiroIntegrationService =
                criaOperacaoRoteiroIntegrationService(
                        recursoProdutivoRepository,
                        roteiroRepository,
                        operacaoRoteiroRepository);
        ProcessedFile processedFile = new ProcessedFile();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> operacaoRoteiroIntegrationService.saveFile(processedFile));

        Assertions.assertEquals(
                "Production resource snapshot returned null.",
                dataUploadException.getMessage());

    }

    @Test
    void routingOperationUploadShouldRejectBrokenExistingOperationSnapshotBeforeProcessingRows() {

        RecursoProdutivoRepository recursoProdutivoRepository =
                criaRepositoryProxy(
                        RecursoProdutivoRepository.class,
                        "findAll",
                        List.of());
        RoteiroRepository roteiroRepository =
                criaRepositoryProxy(
                        RoteiroRepository.class,
                        "findAll",
                        List.of());
        OperacaoRoteiroRepository operacaoRoteiroRepository =
                criaRepositoryProxy(
                        OperacaoRoteiroRepository.class,
                        "customFindAll",
                        List.of(new OperacaoRoteiro()));
        OperacaoRoteiroIntegrationService operacaoRoteiroIntegrationService =
                criaOperacaoRoteiroIntegrationService(
                        recursoProdutivoRepository,
                        roteiroRepository,
                        operacaoRoteiroRepository);
        ProcessedFile processedFile = new ProcessedFile();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> operacaoRoteiroIntegrationService.saveFile(processedFile));

        Assertions.assertEquals(
                "Routing Operation snapshot returned item without primary key at index 0.",
                dataUploadException.getMessage());

    }

    @Test
    void routingOperationUploadShouldPreserveEntityValidationCauseWithLineContext() {

        Location routingLocation = new Location("ROUTING-LOCATION");
        Location productionResourceLocation = new Location("RESOURCE-LOCATION");
        Roteiro roteiro = new Roteiro();
        roteiro.setId("ROUTING-1");
        roteiro.setLocation(routingLocation);
        roteiro.setMaterialOutput(new Produto("FG-1"));
        RecursoProdutivo recursoProdutivo = new RecursoProdutivo();
        recursoProdutivo.setId("RESOURCE-1");
        recursoProdutivo.setLocation(productionResourceLocation);
        RecursoProdutivoRepository recursoProdutivoRepository =
                criaRepositoryProxy(
                        RecursoProdutivoRepository.class,
                        "findAll",
                        List.of(recursoProdutivo));
        RoteiroRepository roteiroRepository =
                criaRepositoryProxy(
                        RoteiroRepository.class,
                        "findAll",
                        List.of(roteiro));
        OperacaoRoteiroRepository operacaoRoteiroRepository =
                criaRepositoryProxy(
                        OperacaoRoteiroRepository.class,
                        "customFindAll",
                        List.of());
        OperacaoRoteiroIntegrationService operacaoRoteiroIntegrationService =
                criaOperacaoRoteiroIntegrationService(
                        recursoProdutivoRepository,
                        roteiroRepository,
                        operacaoRoteiroRepository);
        ProcessedFile processedFile = new ProcessedFile();
        processedFile.addRow(new ProcessedFileRow(List.of(
                (Object) "Routing Id",
                "Sequence",
                "Production Resource Id",
                "Base Quantity",
                "UOM",
                "Hours By Base Quantity",
                "Delete")));
        processedFile.addRow(new ProcessedFileRow(List.of(
                (Object) "ROUTING-1",
                "10",
                "RESOURCE-1")));

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> operacaoRoteiroIntegrationService.saveFile(processedFile));

        String entityValidationMessage =
                "Routing ROUTING-1 location ROUTING-LOCATION does not match production resource location "
                        + "RESOURCE-LOCATION at operation 10";
        Assertions.assertEquals(
                entityValidationMessage + " at line 2",
                dataUploadException.getMessage());
        Assertions.assertInstanceOf(
                IllegalStateException.class,
                dataUploadException.getCause());
        Assertions.assertEquals(
                entityValidationMessage,
                dataUploadException.getCause().getMessage());

    }

    @Test
    void routingOperationUploadShouldPreserveSaveDataAccessCause() {

        DataAccessResourceFailureException dataAccessException =
                new DataAccessResourceFailureException("Simulated routing operation save failure");
        Map<String, Object> methodResults = new HashMap<>();
        methodResults.put(
                "customFindAll",
                List.of());
        methodResults.put(
                "saveAll",
                dataAccessException);
        OperacaoRoteiroRepository operacaoRoteiroRepository =
                criaRepositoryProxy(OperacaoRoteiroRepository.class, methodResults);
        OperacaoRoteiroIntegrationService operacaoRoteiroIntegrationService =
                criaOperacaoRoteiroIntegrationServiceComDadosValidos(operacaoRoteiroRepository);

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> operacaoRoteiroIntegrationService.saveFile(
                        criaProcessedFileOperacaoRoteiro("")));

        Assertions.assertEquals(
                "Error saving Routing Operations " + dataAccessException,
                dataUploadException.getMessage());
        Assertions.assertSame(
                dataAccessException,
                dataUploadException.getCause());

    }

    @Test
    void routingOperationUploadShouldPreserveDeleteDataAccessCause() {

        DataAccessResourceFailureException dataAccessException =
                new DataAccessResourceFailureException("Simulated routing operation delete failure");
        Map<String, Object> methodResults = new HashMap<>();
        methodResults.put(
                "customFindAll",
                List.of());
        methodResults.put(
                "saveAll",
                List.of());
        methodResults.put(
                "deleteAll",
                dataAccessException);
        OperacaoRoteiroRepository operacaoRoteiroRepository =
                criaRepositoryProxy(OperacaoRoteiroRepository.class, methodResults);
        OperacaoRoteiroIntegrationService operacaoRoteiroIntegrationService =
                criaOperacaoRoteiroIntegrationServiceComDadosValidos(operacaoRoteiroRepository);

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> operacaoRoteiroIntegrationService.saveFile(
                        criaProcessedFileOperacaoRoteiro("D")));

        Assertions.assertEquals(
                "Error deleting Routing Operations " + dataAccessException,
                dataUploadException.getMessage());
        Assertions.assertSame(
                dataAccessException,
                dataUploadException.getCause());

    }

    @Test
    void routingOperationUploadShouldPreserveNumericParseCauseWithLineContext() {

        OperacaoRoteiroRepository operacaoRoteiroRepository =
                criaRepositoryProxy(
                        OperacaoRoteiroRepository.class,
                        "customFindAll",
                        List.of());
        OperacaoRoteiroIntegrationService operacaoRoteiroIntegrationService =
                criaOperacaoRoteiroIntegrationServiceComDadosValidos(operacaoRoteiroRepository);

        DataUploadException sequenceDataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> operacaoRoteiroIntegrationService.saveFile(
                        criaProcessedFileOperacaoRoteiro("10.5", "", "", "")));
        DataUploadException operationDurationDataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> operacaoRoteiroIntegrationService.saveFile(
                        criaProcessedFileOperacaoRoteiro("10", "2,5", "", "")));

        Assertions.assertEquals(
                "Sequence is not a valid Integer number at line 2 : no decimals should be used.",
                sequenceDataUploadException.getMessage());
        Assertions.assertEquals(
                "Invalid operation duration at line 2 : should be a decimal number with '.' as decimal and no thousands separator.",
                operationDurationDataUploadException.getMessage());
        Assertions.assertInstanceOf(
                NumberFormatException.class,
                sequenceDataUploadException.getCause());
        Assertions.assertInstanceOf(
                NumberFormatException.class,
                operationDurationDataUploadException.getCause());

    }

    @Test
    void billOfMaterialsSupportDataShouldRejectNullMaterialSnapshotBeforeIndexing() {

        LocationService locationService = new TestLocationService(List.of());
        MaterialService materialService = new TestMaterialService(null);
        UnidadeMedidaRepository unidadeMedidaRepository =
                criaRepositoryProxy(
                        UnidadeMedidaRepository.class,
                        "findAll",
                        List.of());
        ListaTecnicaRepository listaTecnicaRepository =
                criaRepositoryProxy(
                        ListaTecnicaRepository.class,
                        "findAll",
                        List.of());
        ListaTecnicaIntegrationService listaTecnicaIntegrationService =
                criaListaTecnicaIntegrationService(
                        locationService,
                        materialService,
                        unidadeMedidaRepository,
                        listaTecnicaRepository);

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                listaTecnicaIntegrationService::getSupportData);

        Assertions.assertEquals(
                "Material snapshot returned null.",
                dataUploadException.getMessage());

    }

    @Test
    void billOfMaterialsComponentSupportDataShouldRejectBrokenBillOfMaterialsSnapshotBeforeIndexing() {

        MaterialService materialService = new TestMaterialService(Set.of());
        UnidadeMedidaRepository unidadeMedidaRepository =
                criaRepositoryProxy(
                        UnidadeMedidaRepository.class,
                        "findAll",
                        List.of());
        ListaTecnicaRepository listaTecnicaRepository =
                criaRepositoryProxy(
                        ListaTecnicaRepository.class,
                        "findAll",
                        List.of(new ListaTecnica()));
        ListaTecnicaComponenteIntegrationService listaTecnicaComponenteIntegrationService =
                criaListaTecnicaComponenteIntegrationService(
                        materialService,
                        unidadeMedidaRepository,
                        listaTecnicaRepository);

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                listaTecnicaComponenteIntegrationService::getSupportData);

        Assertions.assertEquals(
                "Bill of Materials snapshot returned item without id at index 0.",
                dataUploadException.getMessage());

    }

    @Test
    void productionVersionSupportDataShouldRejectBrokenRoutingSnapshotBeforeIndexing() {

        LocationService locationService = new TestLocationService(List.of());
        RoteiroRepository roteiroRepository =
                criaRepositoryProxy(
                        RoteiroRepository.class,
                        "customFindAllForFront",
                        List.of(new Roteiro()));
        ListaTecnicaRepository listaTecnicaRepository =
                criaRepositoryProxy(
                        ListaTecnicaRepository.class,
                        "findAll",
                        List.of());
        VersaoProducaoRepository versaoProducaoRepository =
                criaRepositoryProxy(
                        VersaoProducaoRepository.class,
                        "findAll",
                        List.of());
        VersaoProducaoIntegrationService versaoProducaoIntegrationService =
                criaVersaoProducaoIntegrationService(
                        locationService,
                        roteiroRepository,
                        listaTecnicaRepository,
                        versaoProducaoRepository);

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                versaoProducaoIntegrationService::getSupportData);

        Assertions.assertEquals(
                "Routing snapshot returned item without id at index 0.",
                dataUploadException.getMessage());

    }

    @Test
    void productionVersionExportShouldUseRepositoryFetchSnapshot() {

        LocationService locationService = new TestLocationService(List.of());
        RoteiroRepository roteiroRepository = criaRepositoryProxy(
                RoteiroRepository.class,
                "findAll",
                List.of());
        ListaTecnicaRepository listaTecnicaRepository = criaRepositoryProxy(
                ListaTecnicaRepository.class,
                "findAll",
                List.of());
        VersaoProducaoRepository versaoProducaoRepository = criaRepositoryProxy(
                VersaoProducaoRepository.class,
                "customFindAllForIntegrationExport",
                List.of());
        VersaoProducaoIntegrationService versaoProducaoIntegrationService =
                criaVersaoProducaoIntegrationService(
                        locationService,
                        roteiroRepository,
                        listaTecnicaRepository,
                        versaoProducaoRepository);

        Assertions.assertEquals(
                List.of(),
                versaoProducaoIntegrationService.getAllPersistedEntities());

    }

    @Test
    void productionResourceSupportDataShouldRejectNullLocationSnapshotBeforeIndexing() {

        LocationService locationService = new TestLocationService(null);
        UnidadeMedidaRepository unidadeMedidaRepository =
                criaRepositoryProxy(
                        UnidadeMedidaRepository.class,
                        "findAll",
                        List.of());
        RecursoProdutivoIntegrationService recursoProdutivoIntegrationService =
                criaRecursoProdutivoIntegrationService(
                        locationService,
                        unidadeMedidaRepository);

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                recursoProdutivoIntegrationService::getSupportData);

        Assertions.assertEquals(
                "Location snapshot returned null.",
                dataUploadException.getMessage());

    }

    @Test
    void productionResourceAvailabilitySupportDataShouldRejectBrokenProductionResourceSnapshotBeforeIndexing() {

        RecursoProdutivoRepository recursoProdutivoRepository =
                criaRepositoryProxy(
                        RecursoProdutivoRepository.class,
                        "findAll",
                        List.of(new RecursoProdutivo()));
        DisponibilidadeRecursoProdutivoIntegrationService disponibilidadeRecursoProdutivoIntegrationService =
                criaDisponibilidadeRecursoProdutivoIntegrationService(recursoProdutivoRepository);

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                disponibilidadeRecursoProdutivoIntegrationService::getSupportData);

        Assertions.assertEquals(
                "Production resource snapshot returned item without id at index 0.",
                dataUploadException.getMessage());

    }

    @Test
    void routingSupportDataShouldRejectNullMaterialSnapshotBeforeIndexing() {

        LocationService locationService = new TestLocationService(List.of());
        MaterialService materialService = new TestMaterialService(null);
        RoteiroIntegrationService roteiroIntegrationService =
                criaRoteiroIntegrationService(
                        locationService,
                        materialService);

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                roteiroIntegrationService::getSupportData);

        Assertions.assertEquals(
                "Material snapshot returned null.",
                dataUploadException.getMessage());

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

    private static OperacaoRoteiroIntegrationService criaOperacaoRoteiroIntegrationService(
            RecursoProdutivoRepository recursoProdutivoRepository,
            RoteiroRepository roteiroRepository,
            OperacaoRoteiroRepository operacaoRoteiroRepository) {

        OperacaoRoteiroIntegrationService operacaoRoteiroIntegrationService =
                new OperacaoRoteiroIntegrationService();
        ReflectionTestUtils.setField(
                operacaoRoteiroIntegrationService,
                "recursoProdutivoRepository",
                recursoProdutivoRepository);
        ReflectionTestUtils.setField(
                operacaoRoteiroIntegrationService,
                "roteiroRepository",
                roteiroRepository);
        ReflectionTestUtils.setField(
                operacaoRoteiroIntegrationService,
                "operacaoRoteiroRepository",
                operacaoRoteiroRepository);
        return operacaoRoteiroIntegrationService;

    }

    private static OperacaoRoteiroIntegrationService criaOperacaoRoteiroIntegrationServiceComDadosValidos(
            OperacaoRoteiroRepository operacaoRoteiroRepository) {

        Location location = new Location("PLANT-1");
        Roteiro roteiro = new Roteiro();
        roteiro.setId("ROUTING-1");
        roteiro.setLocation(location);
        roteiro.setMaterialOutput(new Produto("FG-1"));
        RecursoProdutivo recursoProdutivo = new RecursoProdutivo();
        recursoProdutivo.setId("RESOURCE-1");
        recursoProdutivo.setLocation(location);
        RecursoProdutivoRepository recursoProdutivoRepository =
                criaRepositoryProxy(
                        RecursoProdutivoRepository.class,
                        "findAll",
                        List.of(recursoProdutivo));
        RoteiroRepository roteiroRepository =
                criaRepositoryProxy(
                        RoteiroRepository.class,
                        "findAll",
                        List.of(roteiro));
        return criaOperacaoRoteiroIntegrationService(
                recursoProdutivoRepository,
                roteiroRepository,
                operacaoRoteiroRepository);

    }

    private static ProcessedFile criaProcessedFileOperacaoRoteiro(String deleteFlag) {

        return criaProcessedFileOperacaoRoteiro(
                "10",
                "",
                "",
                deleteFlag);

    }

    private static ProcessedFile criaProcessedFileOperacaoRoteiro(
            String sequence,
            String operationDuration,
            String timeUnit,
            String deleteFlag) {

        ProcessedFile processedFile = new ProcessedFile();
        processedFile.addRow(new ProcessedFileRow(List.of(
                (Object) "Routing Id",
                "Operation Sequence (Integer number)",
                "Production Resource Id",
                "Operation Duration",
                "Time Unit (S, M, H or D; default H)",
                "Delete")));
        processedFile.addRow(new ProcessedFileRow(List.of(
                (Object) "ROUTING-1",
                sequence,
                "RESOURCE-1",
                operationDuration,
                timeUnit,
                deleteFlag)));
        return processedFile;

    }

    private static RecursoProdutivoIntegrationService criaRecursoProdutivoIntegrationService(
            LocationService locationService,
            UnidadeMedidaRepository unidadeMedidaRepository) {

        RecursoProdutivoIntegrationService recursoProdutivoIntegrationService =
                new RecursoProdutivoIntegrationService();
        ReflectionTestUtils.setField(
                recursoProdutivoIntegrationService,
                "locationService",
                locationService);
        ReflectionTestUtils.setField(
                recursoProdutivoIntegrationService,
                "unidadeMedidaRepository",
                unidadeMedidaRepository);
        return recursoProdutivoIntegrationService;

    }

    private static DisponibilidadeRecursoProdutivoIntegrationService criaDisponibilidadeRecursoProdutivoIntegrationService(
            RecursoProdutivoRepository recursoProdutivoRepository) {

        DisponibilidadeRecursoProdutivoIntegrationService disponibilidadeRecursoProdutivoIntegrationService =
                new DisponibilidadeRecursoProdutivoIntegrationService();
        ReflectionTestUtils.setField(
                disponibilidadeRecursoProdutivoIntegrationService,
                "recursoProdutivoRepository",
                recursoProdutivoRepository);
        return disponibilidadeRecursoProdutivoIntegrationService;

    }

    private static RoteiroIntegrationService criaRoteiroIntegrationService(
            LocationService locationService,
            MaterialService materialService) {

        RoteiroIntegrationService roteiroIntegrationService =
                new RoteiroIntegrationService();
        ReflectionTestUtils.setField(
                roteiroIntegrationService,
                "locationService",
                locationService);
        ReflectionTestUtils.setField(
                roteiroIntegrationService,
                "materialService",
                materialService);
        return roteiroIntegrationService;

    }

    private static ListaTecnicaIntegrationService criaListaTecnicaIntegrationService(
            LocationService locationService,
            MaterialService materialService,
            UnidadeMedidaRepository unidadeMedidaRepository,
            ListaTecnicaRepository listaTecnicaRepository) {

        ListaTecnicaIntegrationService listaTecnicaIntegrationService =
                new ListaTecnicaIntegrationService();
        ReflectionTestUtils.setField(
                listaTecnicaIntegrationService,
                "locationService",
                locationService);
        ReflectionTestUtils.setField(
                listaTecnicaIntegrationService,
                "materialService",
                materialService);
        ReflectionTestUtils.setField(
                listaTecnicaIntegrationService,
                "unidadeMedidaRepository",
                unidadeMedidaRepository);
        ReflectionTestUtils.setField(
                listaTecnicaIntegrationService,
                "listaTecnicaRepository",
                listaTecnicaRepository);
        return listaTecnicaIntegrationService;

    }

    private static ListaTecnicaComponenteIntegrationService criaListaTecnicaComponenteIntegrationService(
            MaterialService materialService,
            UnidadeMedidaRepository unidadeMedidaRepository,
            ListaTecnicaRepository listaTecnicaRepository) {

        ListaTecnicaComponenteIntegrationService listaTecnicaComponenteIntegrationService =
                new ListaTecnicaComponenteIntegrationService();
        ReflectionTestUtils.setField(
                listaTecnicaComponenteIntegrationService,
                "materialService",
                materialService);
        ReflectionTestUtils.setField(
                listaTecnicaComponenteIntegrationService,
                "unidadeMedidaRepository",
                unidadeMedidaRepository);
        ReflectionTestUtils.setField(
                listaTecnicaComponenteIntegrationService,
                "listaTecnicaRepository",
                listaTecnicaRepository);
        return listaTecnicaComponenteIntegrationService;

    }

    private static VersaoProducaoIntegrationService criaVersaoProducaoIntegrationService(
            LocationService locationService,
            RoteiroRepository roteiroRepository,
            ListaTecnicaRepository listaTecnicaRepository,
            VersaoProducaoRepository versaoProducaoRepository) {

        VersaoProducaoIntegrationService versaoProducaoIntegrationService =
                new VersaoProducaoIntegrationService();
        ReflectionTestUtils.setField(
                versaoProducaoIntegrationService,
                "locationService",
                locationService);
        ReflectionTestUtils.setField(
                versaoProducaoIntegrationService,
                "roteiroRepository",
                roteiroRepository);
        ReflectionTestUtils.setField(
                versaoProducaoIntegrationService,
                "listaTecnicaRepository",
                listaTecnicaRepository);
        ReflectionTestUtils.setField(
                versaoProducaoIntegrationService,
                "versaoProducaoRepository",
                versaoProducaoRepository);
        return versaoProducaoIntegrationService;

    }

    @SuppressWarnings("unchecked")
    private static <T> T criaRepositoryProxy(
            Class<T> repositoryClass,
            String methodName,
            Object methodResult) {

        Map<String, Object> methodResults = new HashMap<>();
        methodResults.put(
                methodName,
                methodResult);
        return criaRepositoryProxy(
                repositoryClass,
                methodResults);

    }

    @SuppressWarnings("unchecked")
    private static <T> T criaRepositoryProxy(
            Class<T> repositoryClass,
            Map<String, Object> methodResults) {

        return (T) Proxy.newProxyInstance(
                repositoryClass.getClassLoader(),
                new Class<?>[]{repositoryClass},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass().equals(Object.class)) {
                        return switch (method.getName()) {
                            case "toString" -> repositoryClass.getSimpleName() + " test proxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> throw new UnsupportedOperationException(
                                    "Metodo Object nao suportado no teste: " + method.getName());
                        };
                    }
                    if (methodResults.containsKey(method.getName())) {
                        Object methodResult = methodResults.get(method.getName());
                        if (methodResult instanceof RuntimeException runtimeException) {
                            throw runtimeException;
                        }
                        return methodResult;
                    }
                    if ("saveAll".equals(method.getName()) || "deleteAll".equals(method.getName())) {
                        throw new AssertionError(
                                repositoryClass.getSimpleName()
                                        + "."
                                        + method.getName()
                                        + " should not be called before snapshot validation.");
                    }
                    throw new UnsupportedOperationException(
                            repositoryClass.getSimpleName()
                                    + "."
                                    + method.getName()
                                    + " is not implemented in this test proxy.");
                });

    }

    private static class TestLocationService extends LocationService {

        private final List<Location> locations;

        private TestLocationService(List<Location> locations) {

            this.locations = locations;

        }

        @Override
        public List<Location> findAllWithoutDefault() {

            return locations;

        }

    }

    private static class TestMaterialService extends MaterialService {

        private final List<Produto> materiais;

        private TestMaterialService(Set<Produto> materiais) {

            this.materiais = materiais == null
                    ? null
                    : List.copyOf(materiais);

        }

        @Override
        public List<Produto> getMateriais(boolean somenteMateriaisAtivos) {

            return materiais;

        }

    }

}
