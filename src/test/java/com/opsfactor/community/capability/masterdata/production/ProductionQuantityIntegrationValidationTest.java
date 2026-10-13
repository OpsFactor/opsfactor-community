package com.opsfactor.community.capability.masterdata.production;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.facade.mapper.ListaTecnicaAutoMapper;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.facade.mapper.ListaTecnicaComponenteAutoMapper;
import java.util.Arrays;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnicaComponente;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.dto.ListaTecnicaIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.dto.ListaTecnicaComponenteIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.mapper.*;
import com.opsfactor.community.capability.masterdata.production.operation.domain.OperacaoRoteiro;
import com.opsfactor.community.capability.masterdata.production.operation.facade.mapper.OperacaoRoteiroAutoMapper;
import com.opsfactor.community.capability.masterdata.production.operation.integration.service.OperacaoRoteiroIntegrationService;
import com.opsfactor.community.capability.masterdata.production.operation.repository.OperacaoRoteiroRepository;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.productionresource.repository.RecursoProdutivoRepository;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.production.routing.integration.dto.RoteiroIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.production.routing.integration.mapper.RoteiroIntegrationMapper;
import com.opsfactor.community.capability.masterdata.production.routing.integration.mapper.RoteiroIntegrationSupportData;
import com.opsfactor.community.capability.masterdata.production.routing.repository.RoteiroRepository;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface.MetodoAtualizacaoCampo;
import com.opsfactor.community.platform.integration.validation.IntegrationInputContext;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFile;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/** Regression contract: reject invalid writes while keeping historical values exportable. */
class ProductionQuantityIntegrationValidationTest {

    private final Location location = new Location("LOCATION");
    private final Produto material = createMaterial();

    @ParameterizedTest
    @ValueSource(doubles = {0d, -1d, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidBomOutputBeforeUpdatingEntity(double invalidQuantity) {

        ListaTecnica entity = bom(10d);
        ListaTecnicaIntegrationMapper mapper = new ListaTecnicaIntegrationMapper();
        ListaTecnicaIntegrationDataDto dto = bomDto(invalidQuantity);
        dto.setIntegrationInputContext(new IntegrationInputContext(true, 18, mapper.getProcessedFileHeaders()));
        DataUploadException exception = assertThrows(DataUploadException.class,
                () -> mapper.updateEntityNonPrimaryFieldsFromDTO(entity, dto, bomSupport(), null));
        assertTrue(exception.getMessage().contains("BOM"));
        assertTrue(exception.getMessage().contains("Output Quantity"));
        assertTrue(exception.getMessage().contains("18"));
        assertEquals(10d, entity.getQuantidade());

    }

    @ParameterizedTest
    @ValueSource(doubles = {0d, -1d, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidRoutingBaseBeforeUpdatingEntity(double invalidQuantity) {

        Roteiro entity = routing(10d);
        RoteiroIntegrationMapper mapper = new RoteiroIntegrationMapper();
        DataUploadException exception = assertThrows(DataUploadException.class,
                () -> mapper.updateEntityNonPrimaryFieldsFromDTO(entity, routingDto(invalidQuantity), routingSupport(), null));
        assertTrue(exception.getMessage().contains("ROUTING"));
        assertTrue(exception.getMessage().contains("baseQuantity"));
        assertEquals(10d, entity.getQuantidadeBase());

    }

    @ParameterizedTest
    @ValueSource(doubles = {0d, -1d, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidComponentBeforeUpdatingEntity(double invalidQuantity) {

        ListaTecnicaComponente entity = component(10d);
        ListaTecnicaComponenteIntegrationMapper mapper = new ListaTecnicaComponenteIntegrationMapper();
        DataUploadException exception = assertThrows(DataUploadException.class,
                () -> mapper.updateEntityNonPrimaryFieldsFromDTO(entity, componentDto(invalidQuantity), componentSupport(), null));
        assertTrue(exception.getMessage().contains("BOM/MATERIAL"));
        assertTrue(exception.getMessage().contains("componentMaterialQuantity"));
        assertEquals(10d, entity.getQuantidade());

    }

    @Test
    void acceptsTemporaryPositiveQuantityAndRetainsNullDefault() {

        ListaTecnicaIntegrationMapper bomMapper = new ListaTecnicaIntegrationMapper();
        RoteiroIntegrationMapper routingMapper = new RoteiroIntegrationMapper();
        ListaTecnicaComponenteIntegrationMapper componentMapper = new ListaTecnicaComponenteIntegrationMapper();
        ListaTecnica bom = bom(10d);
        Roteiro routing = routing(10d);
        ListaTecnicaComponente component = component(10d);
        for (Double quantity : new Double[]{0.001d, null}) {
            bomMapper.updateEntityNonPrimaryFieldsFromDTO(bom, bomDto(quantity), bomSupport(), null);
            routingMapper.updateEntityNonPrimaryFieldsFromDTO(routing, routingDto(quantity), routingSupport(), null);
            componentMapper.updateEntityNonPrimaryFieldsFromDTO(component, componentDto(quantity), componentSupport(), null);
            double expected = quantity == null ? 1d : quantity;
            assertEquals(expected, bom.getQuantidade());
            assertEquals(expected, routing.getQuantidadeBase());
            assertEquals(expected, component.getQuantidade());
        }
        assertNull(bomMapper.getDtoWithoutPrimaryKeyFromEntity(bom).outputQuantity);
        assertNull(componentMapper.getDtoWithoutPrimaryKeyFromEntity(component).componentMaterialQuantity);

    }

    @Test
    void ignoresQuantityFieldsWithoutRejectingOrReplacingExistingValues() {

        ListaTecnica bom = bom(10d);
        Roteiro routing = routing(10d);
        ListaTecnicaComponente component = component(10d);
        new ListaTecnicaIntegrationMapper().updateEntityNonPrimaryFieldsFromDTO(bom, bomDto(-1d), bomSupport(),
                Map.of("outputQuantity", MetodoAtualizacaoCampo.IGNORE));
        new RoteiroIntegrationMapper().updateEntityNonPrimaryFieldsFromDTO(routing, routingDto(-1d), routingSupport(),
                Map.of("baseQuantity", MetodoAtualizacaoCampo.IGNORE));
        new ListaTecnicaComponenteIntegrationMapper().updateEntityNonPrimaryFieldsFromDTO(component, componentDto(-1d), componentSupport(),
                Map.of("componentMaterialQuantity", MetodoAtualizacaoCampo.IGNORE));
        assertEquals(10d, bom.getQuantidade());
        assertEquals(10d, routing.getQuantidadeBase());
        assertEquals(10d, component.getQuantidade());

    }

    @ParameterizedTest
    @ValueSource(doubles = {0d, -1d, Double.NaN, Double.POSITIVE_INFINITY})
    void exportsHistoricalInvalidValuesWithoutRelaxingCalculations(double invalidQuantity) {

        ListaTecnica bom = bom(invalidQuantity);
        Roteiro routing = routing(invalidQuantity);
        ListaTecnicaComponente component = component(invalidQuantity);
        ListaTecnicaIntegrationMapper bomMapper = new ListaTecnicaIntegrationMapper();
        RoteiroIntegrationMapper routingMapper = new RoteiroIntegrationMapper();
        ListaTecnicaComponenteIntegrationMapper componentMapper = new ListaTecnicaComponenteIntegrationMapper();
        assertEquals(invalidQuantity, bomMapper.getDtoWithoutPrimaryKeyFromEntity(bom).outputQuantity);
        assertEquals(invalidQuantity, bomMapper.convertEntityToProcessedFileRow(bom, null).getColumnValueAsDouble(4));
        assertEquals(invalidQuantity, routingMapper.getDtoWithoutPrimaryKeyFromEntity(routing).baseQuantity);
        assertEquals(invalidQuantity, routingMapper.convertEntityToProcessedFileRow(routing, null).getColumnValueAsDouble(4));
        assertEquals(invalidQuantity, componentMapper.getDtoWithoutPrimaryKeyFromEntity(component).componentMaterialQuantity);
        assertEquals(invalidQuantity, componentMapper.convertEntityToProcessedFileRow(component, null).getColumnValueAsDouble(3));
        assertEquals(invalidQuantity, Mappers.getMapper(ListaTecnicaAutoMapper.class)
                .converte(bom, new ParametrosGlobais()).getOutputQuantity());
        assertEquals(invalidQuantity, Mappers.getMapper(ListaTecnicaComponenteAutoMapper.class)
                .converte(component, new ParametrosGlobais()).getQuantity());
        assertThrows(IllegalStateException.class, bom::getQuantidade);
        assertThrows(IllegalStateException.class, routing::getQuantidadeBase);
        assertThrows(IllegalStateException.class, component::getQuantidade);

    }

    @Test
    void deletionDoesNotConsumeQuantityOrNonKeyReferences() {

        ListaTecnicaIntegrationDataDto bomDto = ListaTecnicaIntegrationDataDto.builder().outputQuantity(-1d).build();
        bomDto.delete = "D";
        RoteiroIntegrationDataDto routingDto = RoteiroIntegrationDataDto.builder().baseQuantity(-1d).build();
        routingDto.delete = "D";
        ListaTecnicaComponenteIntegrationDataDto componentDto = ListaTecnicaComponenteIntegrationDataDto.builder().componentMaterialQuantity(-1d).build();
        componentDto.delete = "D";
        ListaTecnica bom = bom(-2d);
        Roteiro routing = routing(-2d);
        ListaTecnicaComponente component = component(-2d);
        new ListaTecnicaIntegrationMapper().updateEntityNonPrimaryFieldsFromDTO(bom, bomDto, null, null);
        new RoteiroIntegrationMapper().updateEntityNonPrimaryFieldsFromDTO(routing, routingDto, null, null);
        new ListaTecnicaComponenteIntegrationMapper().updateEntityNonPrimaryFieldsFromDTO(component, componentDto, null, null);
        assertEquals(-2d, bom.getQuantidadeCadastrdo());
        assertEquals(-2d, routing.getQuantidadeBaseCadastrada());
        assertEquals(-2d, component.getQuantidadeCadastrada());

    }

    @ParameterizedTest
    @ValueSource(doubles = {-1d, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void operationUploadRejectsInvalidDurationWithoutSaving(double invalidDuration) {

        OperationFixture fixture = operationFixture(10d);
        DataUploadException exception = assertThrows(DataUploadException.class,
                () -> fixture.service.saveFile(operationFile(invalidDuration, "")));
        assertTrue(exception.getMessage().contains("line 2"));
        verify(fixture.repository, never()).saveAll(anyList());

    }

    @Test
    void operationUploadAllowsZeroAndDeletesHistoricalInvalidDurationByKey() {

        OperationFixture fixture = operationFixture(-1d);
        fixture.service.saveFile(operationFile(0d, ""));
        assertEquals(0d, fixture.operation.getTempoPorQuantidadeBase());
        fixture.operation.setTempoPorQuantidadeBase(-1d);
        fixture.service.saveFile(operationFile(null, "D"));
        verify(fixture.repository).deleteAll(List.of(fixture.operation));
        assertEquals(-1d, fixture.operation.getTempoPorQuantidadeBaseCadastrado());

    }

    @Test
    void operationExportsRawInvalidDurationInBothFileAndFrontDto() {

        OperationFixture fixture = operationFixture(-1d);
        assertEquals(-1d, fixture.service.getFile().get(1).get(3));
        assertEquals(-1d, Mappers.getMapper(OperacaoRoteiroAutoMapper.class).converte(fixture.operation).getOperationDuration());
        verify(fixture.repository).customFindAllForFront();
        assertThrows(IllegalStateException.class, fixture.operation::getHorasPorQuantidadeBase);

    }

    private OperationFixture operationFixture(Double duration) {

        OperacaoRoteiroRepository repository = mock(OperacaoRoteiroRepository.class);
        RoteiroRepository routingRepository = mock(RoteiroRepository.class);
        RecursoProdutivoRepository resourceRepository = mock(RecursoProdutivoRepository.class);
        Roteiro routing = routing(10d);
        RecursoProdutivo resource = new RecursoProdutivo();
        resource.setId("RESOURCE");
        resource.setLocation(location);
        OperacaoRoteiro operation = new OperacaoRoteiro(new OperacaoRoteiro.OperacaoRoteiroCompositeKey(10, routing));
        operation.setRecursoProdutivo(resource);
        operation.setTempoPorQuantidadeBase(duration);
        when(repository.customFindAll()).thenReturn(List.of(operation));
        when(repository.customFindAllForFront()).thenReturn(List.of(operation));
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(routingRepository.findAll()).thenReturn(List.of(routing));
        when(resourceRepository.findAll()).thenReturn(List.of(resource));
        OperacaoRoteiroIntegrationService service = new OperacaoRoteiroIntegrationService();
        ReflectionTestUtils.setField(service, "operacaoRoteiroRepository", repository);
        ReflectionTestUtils.setField(service, "roteiroRepository", routingRepository);
        ReflectionTestUtils.setField(service, "recursoProdutivoRepository", resourceRepository);
        return new OperationFixture(service, repository, operation);

    }

    private ProcessedFile operationFile(Double duration, String delete) {

        ProcessedFile file = new ProcessedFile();
        file.addRow(new ProcessedFileRow(List.of("Routing", "Sequence", "Resource", "Duration", "Unit", "Delete")));
        file.addRow(new ProcessedFileRow(Arrays.asList("ROUTING", 10,
                "D".equals(delete) ? null : "RESOURCE", duration, "H", delete)));
        return file;

    }

    private ListaTecnica bom(Double quantity) {

        ListaTecnica entity = new ListaTecnica();
        entity.setId("BOM");
        entity.setLocation(location);
        entity.setMaterialOutput(material);
        entity.setQuantidade(quantity);
        return entity;

    }

    private Roteiro routing(Double quantity) {

        Roteiro entity = new Roteiro();
        entity.setId("ROUTING");
        entity.setLocation(location);
        entity.setMaterialOutput(material);
        entity.setQuantidadeBase(quantity);
        return entity;

    }

    private ListaTecnicaComponente component(Double quantity) {

        ListaTecnicaComponente entity = new ListaTecnicaComponente(new ListaTecnicaComponente.ListaTecnicaComponenteCompositeKey(bom(10d), material));
        entity.setQuantidade(quantity);
        return entity;

    }

    private ListaTecnicaIntegrationSupportData bomSupport() {

        ListaTecnicaIntegrationSupportData support = new ListaTecnicaIntegrationSupportData();
        support.mapaLocationPorId = Map.of("LOCATION", location);
        support.mapaMaterialPorId = Map.of("MATERIAL", material);
        support.mapaUnidadeMedidaPorId = Map.of();
        return support;

    }

    private RoteiroIntegrationSupportData routingSupport() {

        RoteiroIntegrationSupportData support = new RoteiroIntegrationSupportData();
        support.mapaLocationPorId = Map.of("LOCATION", location);
        support.mapaMaterialPorId = Map.of("MATERIAL", material);
        support.mapaUnidadeMedidaPorId = Map.of();
        return support;

    }

    private ListaTecnicaComponenteIntegrationSupportData componentSupport() {

        ListaTecnicaComponenteIntegrationSupportData support = new ListaTecnicaComponenteIntegrationSupportData();
        support.mapaUnidadeMedidaPorId = Map.of();
        return support;

    }

    private ListaTecnicaIntegrationDataDto bomDto(Double quantity) {

        return ListaTecnicaIntegrationDataDto.builder().locationId("LOCATION").outputMaterialId("MATERIAL").outputQuantity(quantity).build();

    }

    private RoteiroIntegrationDataDto routingDto(Double quantity) {

        return RoteiroIntegrationDataDto.builder().locationId("LOCATION").outputMaterialId("MATERIAL").baseQuantity(quantity).build();

    }

    private ListaTecnicaComponenteIntegrationDataDto componentDto(Double quantity) {

        return ListaTecnicaComponenteIntegrationDataDto.builder().componentMaterialQuantity(quantity).build();

    }

    private static Produto createMaterial() {

        Produto material = new Produto();
        material.setId("MATERIAL");
        return material;

    }

    private record OperationFixture(OperacaoRoteiroIntegrationService service, OperacaoRoteiroRepository repository, OperacaoRoteiro operation) { }
}
