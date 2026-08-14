package com.opsfactor.community.capability.masterdata.classification.characteristic.integration.mapper;

import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.Caracteristica;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaLocation;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProduto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.integration.dto.LocationIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.location.integration.mapper.LocationIntegrationMapper;
import com.opsfactor.community.capability.masterdata.network.location.integration.mapper.LocationIntegrationSupportData;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.product.material.integration.dto.ProdutoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.product.material.integration.mapper.ProdutoIntegrationMapper;
import com.opsfactor.community.capability.masterdata.product.material.integration.mapper.ProdutoIntegrationSupportData;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

/**
 * Protege o round-trip dos valores de caracteristicas dentro dos arquivos
 * mestres Community, mantendo o cadastro das definicoes em fluxo separado.
 */
public class MasterdataIntegrationCharacteristicOverrideTest {

    @Test
    public void materialMasterFileShouldRoundTripDynamicCharacteristicValues() {

        CaracteristicaProduto materialCharacteristic = getMaterialCharacteristic();
        ProdutoIntegrationSupportData supportData = ProdutoIntegrationSupportData.builder()
                .caracteristicaProdutoList(List.of(materialCharacteristic))
                .unidadeMedidaMap(new HashMap<>())
                .build();
        ProdutoIntegrationMapper mapper = new ProdutoIntegrationMapper();
        Produto sourceMaterial = new Produto("MATERIAL_TEST");
        sourceMaterial.setValorCaracteristica(materialCharacteristic, "GREEN");

        ProcessedFileRow processedFileRow = mapper.convertEntityToProcessedFileRow(
                sourceMaterial,
                supportData);
        ProdutoIntegrationDataDto dto = mapper.getDtoWithoutPrimaryKeyFromProcessedFileRow(
                processedFileRow,
                supportData);
        Produto targetMaterial = new Produto("MATERIAL_TEST");
        mapper.updateEntityNonPrimaryFieldsFromDTO(targetMaterial, dto, supportData, null);

        Assertions.assertEquals("GREEN", processedFileRow.getColumnValueAsString(10));
        Assertions.assertEquals("GREEN", dto.valueByCharacteristic.get("COLOR"));
        Assertions.assertEquals(
                "GREEN",
                targetMaterial.getMapaProdutoAtributo().get(materialCharacteristic).getAtributo());

    }

    @Test
    public void locationMasterFileShouldRoundTripDynamicCharacteristicValues() {

        CaracteristicaLocation locationCharacteristic = getLocationCharacteristic();
        LocationIntegrationSupportData supportData = LocationIntegrationSupportData.builder()
                .caracteristicaLocationList(List.of(locationCharacteristic))
                .unidadeMedidaMap(new HashMap<>())
                .locationMap(new HashMap<>())
                .build();
        LocationIntegrationMapper mapper = new LocationIntegrationMapper();
        Location sourceLocation = new Location("LOCATION_TEST");
        sourceLocation.setValorCaracteristica(locationCharacteristic, "DIRECT");

        ProcessedFileRow processedFileRow = mapper.convertEntityToProcessedFileRow(
                sourceLocation,
                supportData);
        LocationIntegrationDataDto dto = mapper.getDtoWithoutPrimaryKeyFromProcessedFileRow(
                processedFileRow,
                supportData);
        Location targetLocation = new Location("LOCATION_TEST");
        mapper.updateEntityNonPrimaryFieldsFromDTO(targetLocation, dto, supportData, null);

        Assertions.assertEquals("DIRECT", processedFileRow.getColumnValueAsString(13));
        Assertions.assertEquals("DIRECT", dto.valueByCharacteristic.get("CHANNEL"));
        Assertions.assertEquals(
                "DIRECT",
                targetLocation.getMapaLocationAtributo().get(locationCharacteristic).getAtributo());

    }

    private CaracteristicaProduto getMaterialCharacteristic() {

        CaracteristicaProduto materialCharacteristic = new CaracteristicaProduto();
        materialCharacteristic.setId("COLOR");
        materialCharacteristic.setDescricao("Color");
        materialCharacteristic.setTipoCaracteristica(Caracteristica.TipoCaracteristica.CATEGORICO);
        return materialCharacteristic;

    }

    private CaracteristicaLocation getLocationCharacteristic() {

        CaracteristicaLocation locationCharacteristic = new CaracteristicaLocation();
        locationCharacteristic.setId("CHANNEL");
        locationCharacteristic.setDescricao("Channel");
        locationCharacteristic.setTipoCaracteristica(Caracteristica.TipoCaracteristica.CATEGORICO);
        return locationCharacteristic;

    }

}
