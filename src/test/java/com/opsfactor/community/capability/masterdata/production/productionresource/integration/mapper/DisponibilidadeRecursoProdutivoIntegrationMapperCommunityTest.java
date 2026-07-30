package com.opsfactor.community.capability.masterdata.production.productionresource.integration.mapper;

import com.opsfactor.community.capability.masterdata.production.productionresource.integration.dto.DisponibilidadeRecursoProdutivoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.DisponibilidadeRecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.DisponibilidadeRecursoProdutivo.DisponibilidadeRecursoProdutivoCompositeKey;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/**
 * Contrato Community da disponibilidade produtiva diaria.
 *
 * <p>O plano restrito heuristico precisa de horas por recurso/dia, mas nao deve
 * aceitar capacidade produtiva por quantidade/UOM nem a antiga coluna de
 * quantidade do template legado Enterprise.</p>
 */
public class DisponibilidadeRecursoProdutivoIntegrationMapperCommunityTest {

    private static final Set<String> COMMUNITY_ACCEPTED_FIELD_NAMES = Set.of(
            "availableHours");

    @Test
    public void updateEntityShouldRejectCapacityInQuantityCommunity() {

        DisponibilidadeRecursoProdutivoIntegrationMapper disponibilidadeRecursoProdutivoIntegrationMapper =
                new DisponibilidadeRecursoProdutivoIntegrationMapper();
        DisponibilidadeRecursoProdutivo disponibilidadeRecursoProdutivo =
                getDisponibilidadeRecursoProdutivo();
        DisponibilidadeRecursoProdutivoIntegrationDataDto disponibilidadeRecursoProdutivoIntegrationDataDto =
                DisponibilidadeRecursoProdutivoIntegrationDataDto.builder()
                        .availableHours(8f)
                        .capacityInQuantity(100f)
                        .build();

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> disponibilidadeRecursoProdutivoIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                        disponibilidadeRecursoProdutivo,
                        disponibilidadeRecursoProdutivoIntegrationDataDto,
                        getSupportData(),
                        null));

    }

    @Test
    public void updateEntityShouldRejectEveryNonCommunityField() throws IllegalAccessException {

        DisponibilidadeRecursoProdutivoIntegrationMapper disponibilidadeRecursoProdutivoIntegrationMapper =
                new DisponibilidadeRecursoProdutivoIntegrationMapper();

        /*
         * Disponibilidade produtiva Community e somente horas disponiveis por
         * recurso/dia. Qualquer campo direto adicional do DTO precisa ser
         * classificado explicitamente ou rejeitado como Enterprise.
         */
        for (Field field : DisponibilidadeRecursoProdutivoIntegrationDataDto.class.getDeclaredFields()) {
            if (COMMUNITY_ACCEPTED_FIELD_NAMES.contains(field.getName())) {
                continue;
            }

            DisponibilidadeRecursoProdutivoIntegrationDataDto disponibilidadeRecursoProdutivoIntegrationDataDto =
                    DisponibilidadeRecursoProdutivoIntegrationDataDto.builder()
                            .availableHours(8f)
                            .build();
            field.setAccessible(true);
            field.set(disponibilidadeRecursoProdutivoIntegrationDataDto, getEnterpriseFieldValue(field));

            Assertions.assertThrows(
                    RequiresEnterpriseVersionException.class,
                    () -> disponibilidadeRecursoProdutivoIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                            getDisponibilidadeRecursoProdutivo(),
                            disponibilidadeRecursoProdutivoIntegrationDataDto,
                            getSupportData(),
                            null));
        }

    }

    @Test
    public void updateEntityShouldRejectCapacityInQuantityUomCommunity() {

        DisponibilidadeRecursoProdutivoIntegrationMapper disponibilidadeRecursoProdutivoIntegrationMapper =
                new DisponibilidadeRecursoProdutivoIntegrationMapper();
        DisponibilidadeRecursoProdutivo disponibilidadeRecursoProdutivo =
                getDisponibilidadeRecursoProdutivo();
        DisponibilidadeRecursoProdutivoIntegrationDataDto disponibilidadeRecursoProdutivoIntegrationDataDto =
                DisponibilidadeRecursoProdutivoIntegrationDataDto.builder()
                        .availableHours(8f)
                        .capacityInQuantityUomId("KG")
                        .build();

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> disponibilidadeRecursoProdutivoIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                        disponibilidadeRecursoProdutivo,
                        disponibilidadeRecursoProdutivoIntegrationDataDto,
                        getSupportData(),
                        null));

    }

    @Test
    public void updateEntityShouldRejectLegacyQuantityColumnReadAsDeleteColumn() {

        DisponibilidadeRecursoProdutivoIntegrationMapper disponibilidadeRecursoProdutivoIntegrationMapper =
                new DisponibilidadeRecursoProdutivoIntegrationMapper();
        DisponibilidadeRecursoProdutivo disponibilidadeRecursoProdutivo =
                getDisponibilidadeRecursoProdutivo();
        DisponibilidadeRecursoProdutivoIntegrationDataDto disponibilidadeRecursoProdutivoIntegrationDataDto =
                DisponibilidadeRecursoProdutivoIntegrationDataDto.builder()
                        .availableHours(8f)
                        .build();
        disponibilidadeRecursoProdutivoIntegrationDataDto.delete = "100";

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> disponibilidadeRecursoProdutivoIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                        disponibilidadeRecursoProdutivo,
                        disponibilidadeRecursoProdutivoIntegrationDataDto,
                        getSupportData(),
                        null));

    }

    @Test
    public void exportShouldHideCapacityInQuantityCommunity() {

        DisponibilidadeRecursoProdutivoIntegrationMapper disponibilidadeRecursoProdutivoIntegrationMapper =
                new DisponibilidadeRecursoProdutivoIntegrationMapper();
        DisponibilidadeRecursoProdutivo disponibilidadeRecursoProdutivo =
                getDisponibilidadeRecursoProdutivo();
        disponibilidadeRecursoProdutivo.setHorasDisponiveis(8f);
        disponibilidadeRecursoProdutivo.setCapacidadeEmQuantidade(100f);

        DisponibilidadeRecursoProdutivoIntegrationDataDto disponibilidadeRecursoProdutivoIntegrationDataDto =
                disponibilidadeRecursoProdutivoIntegrationMapper.getDtoWithoutPrimaryKeyFromEntity(disponibilidadeRecursoProdutivo);
        ProcessedFileRow processedFileRow =
                disponibilidadeRecursoProdutivoIntegrationMapper.convertEntityToProcessedFileRow(
                        disponibilidadeRecursoProdutivo,
                        getSupportData());

        Assertions.assertEquals(8f, disponibilidadeRecursoProdutivoIntegrationDataDto.availableHours);
        Assertions.assertNull(disponibilidadeRecursoProdutivoIntegrationDataDto.capacityInQuantity);
        Assertions.assertNull(disponibilidadeRecursoProdutivoIntegrationDataDto.capacityInQuantityUomId);
        Assertions.assertEquals(3, processedFileRow.getRowSize());
        Assertions.assertEquals(8f, processedFileRow.getColumnValue(2));

    }

    private DisponibilidadeRecursoProdutivo getDisponibilidadeRecursoProdutivo() {

        RecursoProdutivo recursoProdutivo = new RecursoProdutivo();
        recursoProdutivo.setId("RES_01");
        recursoProdutivo.setLocation(new Location("LOC_01"));

        return new DisponibilidadeRecursoProdutivo(
                new DisponibilidadeRecursoProdutivoCompositeKey(
                        recursoProdutivo,
                        LocalDate.of(2026, 1, 1)));

    }

    private DisponibilidadeRecursoProdutivoIntegrationSupportData getSupportData() {

        RecursoProdutivo recursoProdutivo = new RecursoProdutivo();
        recursoProdutivo.setId("RES_01");
        recursoProdutivo.setLocation(new Location("LOC_01"));

        DisponibilidadeRecursoProdutivoIntegrationSupportData disponibilidadeRecursoProdutivoIntegrationSupportData =
                new DisponibilidadeRecursoProdutivoIntegrationSupportData();
        disponibilidadeRecursoProdutivoIntegrationSupportData.mapaRecursoProdutivoPorId =
                Map.of("RES_01", recursoProdutivo);

        return disponibilidadeRecursoProdutivoIntegrationSupportData;

    }

    private Object getEnterpriseFieldValue(Field field) {

        if (field.getType().equals(Float.class)) {
            return 100f;
        }
        if (field.getType().equals(String.class)) {
            return "KG";
        }

        throw new IllegalArgumentException("Unsupported Enterprise field type in test: " + field);

    }

}
