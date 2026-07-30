package com.opsfactor.community.capability.masterdata.production.productionresource.integration.mapper;

import com.opsfactor.community.capability.masterdata.production.productionresource.integration.dto.RecursoProdutivoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Valida o contrato Community para recursos produtivos. A edicao publica usa
 * capacidade produtiva em horas por dia; capacidade em quantidade por UOM fica
 * reservada ao Enterprise e nao pode ser exposta nem persistida por data upload.
 */
public class RecursoProdutivoIntegrationMapperCommunityTest {

    private static final Set<String> COMMUNITY_ACCEPTED_FIELD_NAMES = Set.of(
            "locationId",
            "description",
            "active",
            "efficiency");

    @Test
    public void updateEntityShouldRejectCapacityInQuantityUomCommunity() {

        RecursoProdutivoIntegrationMapper recursoProdutivoIntegrationMapper =
                new RecursoProdutivoIntegrationMapper();
        RecursoProdutivo recursoProdutivo = new RecursoProdutivo();
        RecursoProdutivoIntegrationDataDto recursoProdutivoIntegrationDataDto =
                RecursoProdutivoIntegrationDataDto.builder()
                        .description("Resource")
                        .active(true)
                        .efficiency(1f)
                        .locationId("LOC_01")
                        .capacityInQuantityUomId("KG")
                        .build();

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> recursoProdutivoIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                        recursoProdutivo,
                        recursoProdutivoIntegrationDataDto,
                        getSupportData(),
                        null));

    }

    @Test
    public void updateEntityShouldRejectEveryNonCommunityField() throws IllegalAccessException {

        RecursoProdutivoIntegrationMapper recursoProdutivoIntegrationMapper =
                new RecursoProdutivoIntegrationMapper();

        /*
         * Recursos produtivos Community aceitam apenas capacidade basica em
         * horas por dia e identificacao operacional. Campos que ativam
         * capacidade por quantidade/UOM precisam falhar explicitamente.
         */
        for (Field field : RecursoProdutivoIntegrationDataDto.class.getDeclaredFields()) {
            if (COMMUNITY_ACCEPTED_FIELD_NAMES.contains(field.getName())) {
                continue;
            }

            RecursoProdutivoIntegrationDataDto recursoProdutivoIntegrationDataDto =
                    RecursoProdutivoIntegrationDataDto.builder()
                            .locationId("LOC_01")
                            .description("Resource")
                            .active(true)
                            .efficiency(1f)
                            .build();
            field.setAccessible(true);
            field.set(recursoProdutivoIntegrationDataDto, getEnterpriseFieldValue(field));

            Assertions.assertThrows(
                    RequiresEnterpriseVersionException.class,
                    () -> recursoProdutivoIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                            new RecursoProdutivo(),
                            recursoProdutivoIntegrationDataDto,
                            getSupportData(),
                            null));
        }

    }

    @Test
    public void exportShouldHideCapacityInQuantityUomCommunity() {

        RecursoProdutivoIntegrationMapper recursoProdutivoIntegrationMapper =
                new RecursoProdutivoIntegrationMapper();
        RecursoProdutivo recursoProdutivo = new RecursoProdutivo();
        recursoProdutivo.setId("RES_01");
        recursoProdutivo.setDescricao("Resource");
        recursoProdutivo.setLocation(new Location("LOC_01"));
        recursoProdutivo.setEficiencia(1f);
        recursoProdutivo.setUnidadeMedidaCapacidadeEmUom(new UnidadeMedida("KG"));
        recursoProdutivo.setAtivo(true);

        RecursoProdutivoIntegrationDataDto recursoProdutivoIntegrationDataDto =
                recursoProdutivoIntegrationMapper.getDtoWithoutPrimaryKeyFromEntity(recursoProdutivo);
        ProcessedFileRow processedFileRow =
                recursoProdutivoIntegrationMapper.convertEntityToProcessedFileRow(
                        recursoProdutivo,
                        getSupportData());

        Assertions.assertNull(recursoProdutivoIntegrationDataDto.capacityInQuantityUomId);
        Assertions.assertEquals(5, processedFileRow.getRowSize());
        Assertions.assertEquals(recursoProdutivo.getAtivoCadastrado(), processedFileRow.getColumnValue(4));

    }

    @Test
    public void productionResourceHeadersShouldExposeOnlyCommunityColumns() {

        RecursoProdutivoIntegrationMapper recursoProdutivoIntegrationMapper =
                new RecursoProdutivoIntegrationMapper();

        List<String> processedFileHeaders = recursoProdutivoIntegrationMapper.getProcessedFileHeaders();

        Assertions.assertTrue(RecursoProdutivoIntegrationMapper.class.isAnnotationPresent(Component.class));
        Assertions.assertEquals(List.of(
                "Production Resource Id",
                "Description",
                "Location Id",
                "Efficiency (1.0 if empty)",
                "Active (true/false or 1/0)"
        ), processedFileHeaders);

        Assertions.assertFalse(processedFileHeaders.stream().anyMatch(processedFileHeader ->
                processedFileHeader.contains("Enterprise")
                        || processedFileHeader.contains("Capacity Quantity")));
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> processedFileHeaders.add("Enterprise Test Header"));

    }

    private RecursoProdutivoIntegrationSupportData getSupportData() {

        RecursoProdutivoIntegrationSupportData recursoProdutivoIntegrationSupportData =
                new RecursoProdutivoIntegrationSupportData();
        recursoProdutivoIntegrationSupportData.mapaLocationPorId = Map.of(
                "LOC_01",
                new Location("LOC_01"));
        recursoProdutivoIntegrationSupportData.mapaUnidadeMedidaPorId = Map.of(
                "KG",
                new UnidadeMedida("KG"));

        return recursoProdutivoIntegrationSupportData;

    }

    private Object getEnterpriseFieldValue(Field field) {

        if (field.getType().equals(String.class)) {
            return "KG";
        }

        throw new IllegalArgumentException("Unsupported Enterprise field type in test: " + field);

    }

}
