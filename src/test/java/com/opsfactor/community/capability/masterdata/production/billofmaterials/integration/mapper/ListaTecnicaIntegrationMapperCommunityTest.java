package com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.mapper;

import com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.dto.ListaTecnicaIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Contrato da flag de uso de BOM fora de uma versao de producao no Community.
 */
class ListaTecnicaIntegrationMapperCommunityTest {

    @Test
    void exportShouldKeepConfiguredProductionVersionUsageFlagAfterActiveColumn() {

        ListaTecnicaIntegrationMapper mapper = new ListaTecnicaIntegrationMapper();
        ListaTecnica listaTecnica = createListaTecnica();
        listaTecnica.setHabilitadoParaUsoSemVersaoProducao(false);

        ProcessedFileRow processedFileRow = mapper.convertEntityToProcessedFileRow(listaTecnica, null);

        Assertions.assertEquals(9, processedFileRow.getRowSize());
        Assertions.assertEquals(true, processedFileRow.getColumnValueAsBoolean(7));
        Assertions.assertEquals(false, processedFileRow.getColumnValueAsBoolean(8));

    }

    @Test
    void importShouldPreserveNullForLegacyRowAndReadAppendedFlagForNewRow() {

        ListaTecnicaIntegrationMapper mapper = new ListaTecnicaIntegrationMapper();

        ListaTecnicaIntegrationDataDto legacyDto = mapper.getDtoWithoutPrimaryKeyFromProcessedFileRow(
                new ProcessedFileRow(getLegacyRow()), null);
        ListaTecnicaIntegrationDataDto newDto = mapper.getDtoWithoutPrimaryKeyFromProcessedFileRow(
                new ProcessedFileRow(getNewRow()), null);

        Assertions.assertEquals(true, legacyDto.active);
        Assertions.assertNull(legacyDto.canBeUsedWithoutProductionVersion);
        Assertions.assertEquals(true, newDto.active);
        Assertions.assertEquals(false, newDto.canBeUsedWithoutProductionVersion);

    }

    @Test
    void dtoAndEntityMappingShouldNotPersistEffectiveFallbackAsConfiguredValue() {

        ListaTecnicaIntegrationMapper mapper = new ListaTecnicaIntegrationMapper();
        ListaTecnica listaTecnica = createListaTecnica();

        ListaTecnicaIntegrationDataDto exportedDto = mapper.getDtoWithoutPrimaryKeyFromEntity(listaTecnica);
        Assertions.assertTrue(listaTecnica.getHabilitadoParaUsoSemVersaoProducao());
        Assertions.assertNull(exportedDto.canBeUsedWithoutProductionVersion);

        mapper.updateEntityNonPrimaryFieldsFromDTO(
                listaTecnica,
                ListaTecnicaIntegrationDataDto.builder()
                        .description("Bill of materials")
                        .locationId("LOC")
                        .outputMaterialId("MAT")
                        .outputQuantity(10.0)
                        .priority(1)
                        .active(true)
                        .canBeUsedWithoutProductionVersion(false)
                        .build(),
                getSupportData(listaTecnica),
                null);

        Assertions.assertFalse(listaTecnica.getHabilitadoParaUsoSemVersaoProducao());
        Assertions.assertEquals(false, listaTecnica.getHabilitadoParaUsoSemVersaoProducaoCadastrado());

    }

    private static ListaTecnica createListaTecnica() {

        Location location = new Location();
        location.setId("LOC");
        Produto material = new Produto();
        material.setId("MAT");

        ListaTecnica listaTecnica = new ListaTecnica();
        listaTecnica.setId("BOM");
        listaTecnica.setDescricao("Bill of materials");
        listaTecnica.setLocation(location);
        listaTecnica.setMaterialOutput(material);
        listaTecnica.setQuantidade(10.0);
        listaTecnica.setPrioridade(1);
        listaTecnica.setAtivo(true);
        return listaTecnica;

    }

    private static List<Object> getLegacyRow() {

        return List.of("BOM", "Bill of materials", "LOC", "MAT", 10.0, "PC", 1, true);

    }

    private static List<Object> getNewRow() {

        return List.of("BOM", "Bill of materials", "LOC", "MAT", 10.0, "PC", 1, true, false);

    }

    private static ListaTecnicaIntegrationSupportData getSupportData(ListaTecnica listaTecnica) {

        ListaTecnicaIntegrationSupportData supportData = new ListaTecnicaIntegrationSupportData();
        supportData.mapaLocationPorId = Map.of("LOC", listaTecnica.getLocation());
        supportData.mapaMaterialPorId = Map.of("MAT", listaTecnica.getMaterialOutput());
        supportData.mapaUnidadeMedidaPorId = Map.of();
        return supportData;

    }

}
