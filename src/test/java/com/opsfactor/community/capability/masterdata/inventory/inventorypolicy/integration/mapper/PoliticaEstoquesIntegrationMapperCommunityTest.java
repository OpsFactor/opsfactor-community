package com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.mapper;

import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.dto.PoliticaEstoquesIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.dto.PoliticaEstoquesMaterialLocationIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoques;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoquesMaterialLocation;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.platform.exception.MissingDependencyDataUploadException;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Valida o contrato Community dos mappers de politica de estoque.
 *
 * <p>O detalhe material/location permanece aberto para safety stock
 * operacional, mas a frequencia de reabastecimento fica bloqueada por ser
 * parametro de Inventory Policy Optimization Enterprise.</p>
 */
public class PoliticaEstoquesIntegrationMapperCommunityTest {

    @Test
    public void inventoryPolicyHeadersShouldStayStable() {

        PoliticaEstoquesIntegrationMapper politicaEstoquesIntegrationMapper =
                new PoliticaEstoquesIntegrationMapper();

        List<String> processedFileHeaders = politicaEstoquesIntegrationMapper.getProcessedFileHeaders();

        Assertions.assertEquals(
                List.of(
                        "Id",
                        "Priority",
                        "Start Date",
                        "End Date"),
                processedFileHeaders);
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> processedFileHeaders.add("Enterprise Column"));

    }

    @Test
    public void inventoryPolicyMapperShouldRoundTripCommunityFields() {

        PoliticaEstoquesIntegrationMapper politicaEstoquesIntegrationMapper =
                new PoliticaEstoquesIntegrationMapper();
        LocalDateTime dataInicio = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime dataFim = LocalDateTime.of(2026, 12, 31, 23, 59);
        PoliticaEstoques politicaEstoques = new PoliticaEstoques();
        politicaEstoques.setId("POL_01");
        politicaEstoques.setPrioridade(10);
        politicaEstoques.setDataHorarioInicio(dataInicio);
        politicaEstoques.setDataHorarioFim(dataFim);

        ProcessedFileRow processedFileRow =
                politicaEstoquesIntegrationMapper.convertEntityToProcessedFileRow(
                        politicaEstoques,
                        new PoliticaEstoquesIntegrationSupportData());
        PoliticaEstoquesIntegrationDataDto politicaEstoquesIntegrationDataDto =
                politicaEstoquesIntegrationMapper.getDtoWithoutPrimaryKeyFromEntity(politicaEstoques);

        Assertions.assertEquals("POL_01", processedFileRow.getColumnValue(0));
        Assertions.assertEquals(10, politicaEstoquesIntegrationDataDto.priority);
        Assertions.assertEquals(dataInicio, politicaEstoquesIntegrationDataDto.startDateTime);
        Assertions.assertEquals(dataFim, politicaEstoquesIntegrationDataDto.endDateTime);

    }

    @Test
    public void inventoryPolicyDetailHeadersShouldHideReorderFrequencyCommunity() {

        PoliticaEstoquesMaterialLocationIntegrationMapper mapper =
                new PoliticaEstoquesMaterialLocationIntegrationMapper();

        List<String> processedFileHeaders = mapper.getProcessedFileHeaders();

        Assertions.assertEquals(
                List.of(
                        "Inventory Policy Id",
                        "Material Id",
                        "Location Id",
                        "Operational Model : MTS / MTO (default = MTS if empty)",
                        "Reorder Model : DRP / KANBAN (default = DRP if empty)",
                        "Safety Stock Type : DAYS / QUANTITY (default = DAYS if empty)",
                        "DRP Safety Stock or Kanban Target Stock (# days or quantity)",
                        "DRP Maximum Stock (# days or quantity)"),
                processedFileHeaders);
        Assertions.assertEquals(8, mapper.getDeleteProcessedFileRowPosition(null));
        Assertions.assertFalse(processedFileHeaders.stream().anyMatch(header -> header.contains("Reorder Frequency")));
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> processedFileHeaders.add("Reorder Frequency"));

    }

    @Test
    public void inventoryPolicyDetailShouldRejectReorderFrequencyCommunity() {

        PoliticaEstoquesMaterialLocationIntegrationMapper mapper =
                new PoliticaEstoquesMaterialLocationIntegrationMapper();
        PoliticaEstoquesMaterialLocation entity = getPoliticaEstoquesMaterialLocation();
        PoliticaEstoquesMaterialLocationIntegrationDataDto dto =
                PoliticaEstoquesMaterialLocationIntegrationDataDto.builder()
                        .reorderFrequencyDays(7.0d)
                        .build();

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> mapper.updateEntityNonPrimaryFieldsFromDTO(
                        entity,
                        dto,
                        null,
                        null));

    }

    @Test
    public void inventoryPolicyDetailShouldAcceptCommunityOperationalFields() {

        PoliticaEstoquesMaterialLocationIntegrationMapper mapper =
                new PoliticaEstoquesMaterialLocationIntegrationMapper();
        PoliticaEstoquesMaterialLocation entity = getPoliticaEstoquesMaterialLocation();
        PoliticaEstoquesMaterialLocationIntegrationDataDto dto =
                PoliticaEstoquesMaterialLocationIntegrationDataDto.builder()
                        .operationalModel(Constantes.SNPModeloOperacional.MTO)
                        .reorderModel(Constantes.SNPModeloReabastecimento.KANBAN)
                        .safetyStockType(Constantes.SNPCalculoSafetyStock.QUANTITY)
                        .drpSafetyStockOrKanbanTargetStockValue(12.0d)
                        .drpMaximumStockValue(40.0d)
                        .build();

        mapper.updateEntityNonPrimaryFieldsFromDTO(
                entity,
                dto,
                new PoliticaEstoquesMaterialLocationIntegrationSupportData(),
                null);

        Assertions.assertEquals(Constantes.SNPModeloOperacional.MTO, entity.getModeloOperacionalCadastrado());
        Assertions.assertEquals(Constantes.SNPModeloReabastecimento.KANBAN, entity.getModeloReabastecimentoCadastrado());
        Assertions.assertEquals(Constantes.SNPCalculoSafetyStock.QUANTITY, entity.getCalculoSafetyStockCadastrado());
        Assertions.assertEquals(12.0d, entity.getEstoqueSegurancaDrpOuTargetKanbanCadastrado());
        Assertions.assertEquals(40.0d, entity.getEstoqueMaximoDrpCadastrado());
        Assertions.assertNull(entity.getFrequenciaReabastecimentoDiasCadastrado());

    }

    @Test
    public void inventoryPolicyDetailExportShouldHidePersistedReorderFrequencyCommunity() {

        PoliticaEstoquesMaterialLocationIntegrationMapper mapper =
                new PoliticaEstoquesMaterialLocationIntegrationMapper();
        PoliticaEstoquesMaterialLocation entity = getPoliticaEstoquesMaterialLocation();
        entity.setModeloOperacional(Constantes.SNPModeloOperacional.MTS);
        entity.setModeloReabastecimento(Constantes.SNPModeloReabastecimento.DRP);
        entity.setCalculoSafetyStock(Constantes.SNPCalculoSafetyStock.DAYS);
        entity.setEstoqueSegurancaDrpOuTargetKanban(5.0d);
        entity.setEstoqueMaximoDrp(20.0d);
        entity.setFrequenciaReabastecimentoDias(7.0d);

        PoliticaEstoquesMaterialLocationIntegrationDataDto dto =
                mapper.getDtoWithoutPrimaryKeyFromEntity(entity);
        ProcessedFileRow processedFileRow =
                mapper.convertEntityToProcessedFileRow(
                        entity,
                        new PoliticaEstoquesMaterialLocationIntegrationSupportData());

        Assertions.assertNull(dto.reorderFrequencyDays);
        Assertions.assertEquals(8, processedFileRow.getRowSize());
        Assertions.assertEquals("MTS", processedFileRow.getColumnValue(3));
        Assertions.assertEquals("DRP", processedFileRow.getColumnValue(4));
        Assertions.assertEquals("DAYS", processedFileRow.getColumnValue(5));
        Assertions.assertEquals(5.0d, processedFileRow.getColumnValue(6));
        Assertions.assertEquals(20.0d, processedFileRow.getColumnValue(7));

    }

    @Test
    public void inventoryPolicyDetailShouldResolvePrimaryKeyDependenciesFromSupportData() {

        PoliticaEstoquesMaterialLocationIntegrationMapper mapper =
                new PoliticaEstoquesMaterialLocationIntegrationMapper();
        PoliticaEstoques politicaEstoques = getPoliticaEstoques("POL_01");
        Produto material = new Produto("MAT_01");
        Location location = new Location("LOC_01");
        PoliticaEstoquesMaterialLocationIntegrationSupportData supportData =
                new PoliticaEstoquesMaterialLocationIntegrationSupportData();
        supportData.mapaPoliticaEstoquesPorId = Map.of("POL_01", politicaEstoques);
        supportData.mapaMaterialPorId = Map.of("MAT_01", material);
        supportData.mapaLocationPorId = Map.of("LOC_01", location);

        PoliticaEstoquesMaterialLocation entity =
                mapper.createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
                        new PoliticaEstoquesMaterialLocationIntegrationDataDto.PoliticaEstoquesMaterialLocationPrimaryKeyIntegrationDTO(
                                "POL_01",
                                "MAT_01",
                                "LOC_01"),
                        supportData);

        Assertions.assertSame(politicaEstoques, entity.getPoliticaEstoques());
        Assertions.assertSame(material, entity.getMaterial());
        Assertions.assertSame(location, entity.getLocation());

    }

    @Test
    public void inventoryPolicyDetailShouldFailWhenDependencyIsMissing() {

        PoliticaEstoquesMaterialLocationIntegrationMapper mapper =
                new PoliticaEstoquesMaterialLocationIntegrationMapper();
        PoliticaEstoquesMaterialLocationIntegrationSupportData supportData =
                new PoliticaEstoquesMaterialLocationIntegrationSupportData();
        supportData.mapaPoliticaEstoquesPorId = Map.of();
        supportData.mapaMaterialPorId = Map.of("MAT_01", new Produto("MAT_01"));
        supportData.mapaLocationPorId = Map.of("LOC_01", new Location("LOC_01"));

        Assertions.assertThrows(
                MissingDependencyDataUploadException.class,
                () -> mapper.createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
                        new PoliticaEstoquesMaterialLocationIntegrationDataDto.PoliticaEstoquesMaterialLocationPrimaryKeyIntegrationDTO(
                                "POL_MISSING",
                                "MAT_01",
                                "LOC_01"),
                        supportData));

    }

    @Test
    public void inventoryPolicyDetailShouldParseProcessedFileRow() {

        PoliticaEstoquesMaterialLocationIntegrationMapper mapper =
                new PoliticaEstoquesMaterialLocationIntegrationMapper();
        ProcessedFileRow processedFileRow =
                new ProcessedFileRow(List.of(
                        "POL_01",
                        "MAT_01",
                        "LOC_01",
                        "MTO",
                        "KANBAN",
                        "QUANTITY",
                        3.0d,
                        9.0d));

        PoliticaEstoquesMaterialLocationIntegrationDataDto dto =
                mapper.getDtoWithoutPrimaryKeyFromProcessedFileRow(
                        processedFileRow,
                        new PoliticaEstoquesMaterialLocationIntegrationSupportData());
        PoliticaEstoquesMaterialLocationIntegrationDataDto.PoliticaEstoquesMaterialLocationPrimaryKeyIntegrationDTO primaryKeyDto =
                mapper.getPrimaryKeyDtoFromProcessedFileRow(
                        processedFileRow,
                        new PoliticaEstoquesMaterialLocationIntegrationSupportData());

        Assertions.assertEquals(Constantes.SNPModeloOperacional.MTO, dto.operationalModel);
        Assertions.assertEquals(Constantes.SNPModeloReabastecimento.KANBAN, dto.reorderModel);
        Assertions.assertEquals(Constantes.SNPCalculoSafetyStock.QUANTITY, dto.safetyStockType);
        Assertions.assertEquals(3.0d, dto.drpSafetyStockOrKanbanTargetStockValue);
        Assertions.assertEquals(9.0d, dto.drpMaximumStockValue);
        Assertions.assertEquals("POL_01", primaryKeyDto.inventoryPolicyId);
        Assertions.assertEquals("MAT_01", primaryKeyDto.materialId);
        Assertions.assertEquals("LOC_01", primaryKeyDto.locationId);

    }

    private PoliticaEstoquesMaterialLocation getPoliticaEstoquesMaterialLocation() {

        return new PoliticaEstoquesMaterialLocation(
                new PoliticaEstoquesMaterialLocation.PoliticaEstoquesMaterialLocationCompositeKey(
                        getPoliticaEstoques("POL_01"),
                        new Produto("MAT_01"),
                        new Location("LOC_01")));

    }

    private PoliticaEstoques getPoliticaEstoques(String id) {

        PoliticaEstoques politicaEstoques = new PoliticaEstoques();
        politicaEstoques.setId(id);
        return politicaEstoques;

    }

}
