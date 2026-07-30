package com.opsfactor.community.capability.masterdata.classification.characteristic.integration.mapper;

import com.opsfactor.community.capability.masterdata.network.location.integration.dto.LocationIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.product.material.integration.dto.ProdutoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.network.location.integration.mapper.LocationIntegrationMapper;
import com.opsfactor.community.capability.masterdata.network.location.integration.mapper.LocationIntegrationSupportData;
import com.opsfactor.community.capability.masterdata.product.material.integration.mapper.ProdutoIntegrationMapper;
import com.opsfactor.community.capability.masterdata.product.material.integration.mapper.ProdutoIntegrationSupportData;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Valida o contrato Community para caracteristicas de material/location nos
 * uploads de master data. Caracteristicas sao Enterprise: o template e a
 * leitura de arquivo nao carregam colunas dinamicas, e payload JSON preenchido
 * deve falhar explicitamente.
 */
public class MasterdataIntegrationCharacteristicOverrideTest {

    @Test
    public void produtoIntegrationShouldRejectCharacteristicPayloadCommunity() {

        ProdutoIntegrationMapper produtoIntegrationMapper = new ProdutoIntegrationMapper();
        Produto produto = new Produto("PRODUTO_TESTE");
        ProdutoIntegrationDataDto produtoIntegrationDataDto = ProdutoIntegrationDataDto.builder().build();
        produtoIntegrationDataDto.valueByCharacteristic = new HashMap<>();
        produtoIntegrationDataDto.valueByCharacteristic.put("COR", "VERDE");

        ProdutoIntegrationSupportData supportData = ProdutoIntegrationSupportData.builder()
                .unidadeMedidaMap(new HashMap<>())
                .build();

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> produtoIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                        produto,
                        produtoIntegrationDataDto,
                        supportData,
                        null));

    }

    @Test
    public void locationIntegrationShouldRejectCharacteristicPayloadCommunity() {

        LocationIntegrationMapper locationIntegrationMapper = new LocationIntegrationMapper();
        Location location = new Location("LOC_TESTE");
        LocationIntegrationDataDto locationIntegrationDataDto = LocationIntegrationDataDto.builder().build();
        locationIntegrationDataDto.valueByCharacteristic = new HashMap<>();
        locationIntegrationDataDto.valueByCharacteristic.put("CANAL", "DIRETO");

        LocationIntegrationSupportData supportData = LocationIntegrationSupportData.builder()
                .unidadeMedidaMap(new HashMap<>())
                .build();

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> locationIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                        location,
                        locationIntegrationDataDto,
                        supportData,
                        null));

    }

    @Test
    public void produtoProcessedFileShouldIgnoreLegacyCharacteristicColumnsCommunity() {

        ProdutoIntegrationMapper produtoIntegrationMapper = new ProdutoIntegrationMapper();
        ProdutoIntegrationSupportData supportData = ProdutoIntegrationSupportData.builder()
                .unidadeMedidaMap(new HashMap<>())
                .build();
        ProcessedFileRow processedFileRow = new ProcessedFileRow();

        /*
         * As 11 primeiras colunas sao o layout Community atual. Colunas extras
         * simulam arquivo legado/Enterprise com caracteristicas dinamicas; o
         * mapper Community deve ignora-las e nao reabrir a feature.
         */
        processedFileRow.addContent("PRODUTO_TESTE");
        processedFileRow.addContent("Produto Teste");
        processedFileRow.addContent(true);
        processedFileRow.addContent(null);
        processedFileRow.addContent(null);
        processedFileRow.addContent(null);
        processedFileRow.addContent(null);
        processedFileRow.addContent(null);
        processedFileRow.addContent(null);
        processedFileRow.addContent(null);
        processedFileRow.addContent(null);
        processedFileRow.addContent("VERDE");

        ProdutoIntegrationDataDto produtoIntegrationDataDto = produtoIntegrationMapper.getDtoWithoutPrimaryKeyFromProcessedFileRow(
                processedFileRow,
                supportData);

        Assertions.assertTrue(isEmptyOrNull(produtoIntegrationDataDto.valueByCharacteristic));

    }

    @Test
    public void locationProcessedFileShouldIgnoreLegacyCharacteristicColumnsCommunity() {

        LocationIntegrationMapper locationIntegrationMapper = new LocationIntegrationMapper();
        LocationIntegrationSupportData supportData = LocationIntegrationSupportData.builder()
                .unidadeMedidaMap(new HashMap<>())
                .build();
        ProcessedFileRow processedFileRow = new ProcessedFileRow();

        /*
         * As 16 primeiras colunas sao o layout Community atual. Colunas extras
         * simulam arquivo legado/Enterprise com caracteristicas dinamicas; o
         * mapper Community deve ignora-las e manter o DTO sem caracteristicas.
         */
        processedFileRow.addContent("LOC_TESTE");
        processedFileRow.addContent("Location Teste");
        processedFileRow.addContent(true);
        processedFileRow.addContent(null);
        processedFileRow.addContent(null);
        processedFileRow.addContent(null);
        processedFileRow.addContent(null);
        processedFileRow.addContent(null);
        processedFileRow.addContent(null);
        processedFileRow.addContent(null);
        processedFileRow.addContent(null);
        processedFileRow.addContent(null);
        processedFileRow.addContent(null);
        processedFileRow.addContent(null);
        processedFileRow.addContent(null);
        processedFileRow.addContent(null);
        processedFileRow.addContent("DIRETO");

        LocationIntegrationDataDto locationIntegrationDataDto = locationIntegrationMapper.getDtoWithoutPrimaryKeyFromProcessedFileRow(
                processedFileRow,
                supportData);

        Assertions.assertTrue(isEmptyOrNull(locationIntegrationDataDto.valueByCharacteristic));

    }

    private boolean isEmptyOrNull(Map<String, String> mapaValorPorCaracteristica) {

        return mapaValorPorCaracteristica == null || mapaValorPorCaracteristica.isEmpty();

    }

}
