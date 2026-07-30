package com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.mapper;

import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.VersaoMalhaIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.exception.MissingDependencyDataUploadException;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

/**
 * Contrato do mapper Community de versao de malha.
 *
 * <p>O teste protege os headers legados do subpath `supplynetworkversion` e as
 * regras de conversao das origins padrao sem reabrir atributos privados de
 * mapa, frota ou custos logisticos.</p>
 */
class VersaoMalhaIntegrationMapperCommunityTest {

    @Test
    void supplyNetworkVersionMapperShouldBeSpringComponentAndPublishLegacyHeaders() {

        VersaoMalhaIntegrationMapper versaoMalhaIntegrationMapper =
                new VersaoMalhaIntegrationMapper();

        Assertions.assertTrue(VersaoMalhaIntegrationMapper.class.isAnnotationPresent(Component.class));
        Assertions.assertEquals(
                List.of(
                        "Supply Network Version Id",
                        "Supply Network Version Description",
                        "Default Origin Location for Clients",
                        "Default Origin Location for Raw Materials",
                        "Default Origin Location for Raw Materials Lead Time Days"),
                versaoMalhaIntegrationMapper.getProcessedFileHeaders());
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> versaoMalhaIntegrationMapper.getProcessedFileHeaders().add("Distance Km"));

    }

    @Test
    void supplyNetworkVersionMapperShouldExportEntityOrigins() {

        VersaoMalhaIntegrationMapper versaoMalhaIntegrationMapper =
                new VersaoMalhaIntegrationMapper();
        VersaoMalha versaoMalha = new VersaoMalha("SNV-1");
        versaoMalha.setDescricao("Operational network");
        versaoMalha.setLocationOrigemPadraoClientes(new Location("CLIENT", "Client Origin"));
        versaoMalha.setLocationOrigemPadraoMateriasPrimas(new Location("RAW", "Raw Material Origin"));
        versaoMalha.setLeadTimeDiasLocationOrigemPadraoMateriasPrimas(3.5d);

        VersaoMalhaIntegrationDataDto dto =
                versaoMalhaIntegrationMapper.getDtoWithoutPrimaryKeyFromEntity(versaoMalha);
        ProcessedFileRow processedFileRow =
                versaoMalhaIntegrationMapper.convertEntityToProcessedFileRow(
                        versaoMalha,
                        null);

        Assertions.assertEquals("Operational network", dto.description);
        Assertions.assertEquals("CLIENT", dto.defaultClientOriginLocationId);
        Assertions.assertEquals("RAW", dto.defaultRawMaterialOriginLocationId);
        Assertions.assertEquals(3.5d, dto.defaultRawMaterialOriginLeadTimeDays);
        Assertions.assertEquals("SNV-1", processedFileRow.getColumnValue(0));
        Assertions.assertEquals("Operational network", processedFileRow.getColumnValue(1));
        Assertions.assertEquals("CLIENT", processedFileRow.getColumnValue(2));
        Assertions.assertEquals("RAW", processedFileRow.getColumnValue(3));
        Assertions.assertEquals(3.5d, processedFileRow.getColumnValue(4));

    }

    @Test
    void supplyNetworkVersionMapperShouldReadCanonicalFileRowAndDeleteMarkerOnlyAfterFiveFunctionalColumns() {

        VersaoMalhaIntegrationMapper versaoMalhaIntegrationMapper =
                new VersaoMalhaIntegrationMapper();

        VersaoMalhaIntegrationDataDto dto =
                versaoMalhaIntegrationMapper.convertProcessedFileRowToDTO(
                        new ProcessedFileRow(List.of("SNV-1", "Network", "CLIENT", "RAW", 2d)),
                        null);
        VersaoMalhaIntegrationDataDto canonicalDeleteDto =
                versaoMalhaIntegrationMapper.convertProcessedFileRowToDTO(
                        new ProcessedFileRow(List.of("SNV-2", "Network", "CLIENT", "RAW", 2d, "D")),
                        null);

        Assertions.assertEquals("SNV-1", dto.primaryKeyDto.supplyNetworkVersionId);
        Assertions.assertEquals("Network", dto.description);
        Assertions.assertEquals("CLIENT", dto.defaultClientOriginLocationId);
        Assertions.assertEquals("RAW", dto.defaultRawMaterialOriginLocationId);
        Assertions.assertEquals(2d, dto.defaultRawMaterialOriginLeadTimeDays);
        Assertions.assertEquals("SNV-2", canonicalDeleteDto.primaryKeyDto.supplyNetworkVersionId);
        Assertions.assertEquals("RAW", canonicalDeleteDto.defaultRawMaterialOriginLocationId);
        Assertions.assertEquals(2d, canonicalDeleteDto.defaultRawMaterialOriginLeadTimeDays);
        Assertions.assertEquals("D", canonicalDeleteDto.delete);

    }

    @Test
    void supplyNetworkVersionMapperShouldRejectLegacyFourColumnDeleteMarker() {

        VersaoMalhaIntegrationMapper versaoMalhaIntegrationMapper =
                new VersaoMalhaIntegrationMapper();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> versaoMalhaIntegrationMapper.convertProcessedFileRowToDTO(
                        new ProcessedFileRow(List.of("SNV-2", "Network", "CLIENT", "D")),
                        null));

        Assertions.assertEquals(
                "Supply Network Version file row must provide 5 functional columns before the optional Delete marker.",
                dataUploadException.getMessage());

    }

    @Test
    void supplyNetworkVersionMapperShouldResolveOptionalOrigins() {

        VersaoMalhaIntegrationMapper versaoMalhaIntegrationMapper =
                new VersaoMalhaIntegrationMapper();
        VersaoMalhaIntegrationSupportData supportData = getSupportData();
        VersaoMalha versaoMalha = new VersaoMalha("SNV-1");
        VersaoMalhaIntegrationDataDto dto =
                VersaoMalhaIntegrationDataDto.builder()
                        .description("Updated")
                        .defaultClientOriginLocationId("CLIENT")
                        .defaultRawMaterialOriginLocationId("RAW")
                        .defaultRawMaterialOriginLeadTimeDays(4d)
                        .build();

        versaoMalhaIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                versaoMalha,
                dto,
                supportData,
                null);

        Assertions.assertEquals("Updated", versaoMalha.getDescricao());
        Assertions.assertEquals("CLIENT", versaoMalha.getLocationOrigemPadraoClientes().getId());
        Assertions.assertEquals("RAW", versaoMalha.getLocationOrigemPadraoMateriasPrimas().getId());
        Assertions.assertEquals(4d, versaoMalha.getLeadTimeDiasLocationOrigemPadraoMateriasPrimas());

    }

    @Test
    void supplyNetworkVersionMapperShouldRejectMissingOriginBeforePersisting() {

        VersaoMalhaIntegrationMapper versaoMalhaIntegrationMapper =
                new VersaoMalhaIntegrationMapper();
        VersaoMalhaIntegrationDataDto dto =
                VersaoMalhaIntegrationDataDto.builder()
                        .defaultClientOriginLocationId("UNKNOWN")
                        .build();

        MissingDependencyDataUploadException missingDependencyDataUploadException =
                Assertions.assertThrows(
                        MissingDependencyDataUploadException.class,
                        () -> versaoMalhaIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                                new VersaoMalha("SNV-1"),
                                dto,
                                getSupportData(),
                                null));

        Assertions.assertTrue(
                missingDependencyDataUploadException.getMessage()
                        .startsWith("Default Client Origin Location Id UNKNOWN not found"),
                missingDependencyDataUploadException.getMessage());

    }

    @Test
    void supplyNetworkVersionMapperShouldRejectInvalidRawMaterialLeadTime() {

        VersaoMalhaIntegrationMapper versaoMalhaIntegrationMapper =
                new VersaoMalhaIntegrationMapper();
        VersaoMalhaIntegrationDataDto dto =
                VersaoMalhaIntegrationDataDto.builder()
                        .defaultRawMaterialOriginLeadTimeDays(-1d)
                        .build();

        IllegalArgumentException illegalArgumentException =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> versaoMalhaIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                                new VersaoMalha("SNV-1"),
                                dto,
                                getSupportData(),
                                null));

        Assertions.assertEquals(
                "Default Raw Material Origin Lead Time Days must be finite and non-negative.",
                illegalArgumentException.getMessage());

    }

    private static VersaoMalhaIntegrationSupportData getSupportData() {

        VersaoMalhaIntegrationSupportData supportData =
                new VersaoMalhaIntegrationSupportData();
        supportData.mapaLocationPorId = new HashMap<>();
        supportData.mapaLocationPorId.put("CLIENT", new Location("CLIENT", "Client Origin"));
        supportData.mapaLocationPorId.put("RAW", new Location("RAW", "Raw Material Origin"));
        return supportData;

    }

}
