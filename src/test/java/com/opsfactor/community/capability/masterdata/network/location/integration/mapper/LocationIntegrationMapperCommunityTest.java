package com.opsfactor.community.capability.masterdata.network.location.integration.mapper;

import com.opsfactor.community.capability.masterdata.network.location.integration.dto.LocationIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.Caracteristica;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaLocation;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.platform.exception.MissingDependencyDataUploadException;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Valida o contrato Community do data upload de locations.
 *
 * <p>Locations continuam sendo master data Community, mas coordenadas
 * geograficas, deployment e unidade de expedicao pertencem ao Enterprise.</p>
 */
public class LocationIntegrationMapperCommunityTest {

    private static final Set<String> COMMUNITY_ACCEPTED_FIELD_NAMES = Set.of(
            "description",
            "active",
            "locationType",
            "country",
            "state",
            "city",
            "availableInProductionPlanningBook",
            "availableInSupplyPlanningBook",
            "finiteProductionCapacity",
            "defaultSNPUomId",
            "referenceLocationForProductLocationParameters",
            "safetyStockConsiderIndirectDemand",
            "valueByCharacteristic");

    @Test
    public void locationShouldRejectLatitudeCommunity() {

        LocationIntegrationMapper locationIntegrationMapper = new LocationIntegrationMapper();
        LocationIntegrationDataDto locationIntegrationDataDto =
                LocationIntegrationDataDto.builder()
                        .latitude(-23.5d)
                        .build();

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> locationIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                        new Location("LOC_01"),
                        locationIntegrationDataDto,
                        getLocationIntegrationSupportData(),
                        null));

    }

    @Test
    public void locationShouldRejectLongitudeCommunity() {

        LocationIntegrationMapper locationIntegrationMapper = new LocationIntegrationMapper();
        LocationIntegrationDataDto locationIntegrationDataDto =
                LocationIntegrationDataDto.builder()
                        .longitude(-46.6d)
                        .build();

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> locationIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                        new Location("LOC_01"),
                        locationIntegrationDataDto,
                        getLocationIntegrationSupportData(),
                        null));

    }

    @Test
    public void locationShouldRejectExpeditionUomCommunity() {

        LocationIntegrationMapper locationIntegrationMapper = new LocationIntegrationMapper();
        LocationIntegrationDataDto locationIntegrationDataDto =
                LocationIntegrationDataDto.builder()
                        .expeditionUomId("PALLET")
                        .build();

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> locationIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                        new Location("LOC_01"),
                        locationIntegrationDataDto,
                        getLocationIntegrationSupportData(),
                        null));

    }

    @Test
    public void locationShouldRejectOrderFulfillmentTimeCommunity() {

        LocationIntegrationMapper locationIntegrationMapper = new LocationIntegrationMapper();
        LocationIntegrationDataDto locationIntegrationDataDto =
                LocationIntegrationDataDto.builder()
                        .orderFulfillmentTimeDays(3)
                        .build();

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> locationIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                        new Location("LOC_01"),
                        locationIntegrationDataDto,
                        getLocationIntegrationSupportData(),
                        null));

    }

    @Test
    public void locationShouldSaveCharacteristicsCommunity() {

        LocationIntegrationMapper locationIntegrationMapper = new LocationIntegrationMapper();
        CaracteristicaLocation caracteristicaLocation = getLocationCharacteristic("Region", "Region");
        LocationIntegrationDataDto locationIntegrationDataDto =
                LocationIntegrationDataDto.builder()
                        .valueByCharacteristic(Map.of("Region", "South"))
                        .build();
        Location location = new Location("LOC_01");

        locationIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                location,
                locationIntegrationDataDto,
                getLocationIntegrationSupportData(caracteristicaLocation),
                null);

        Assertions.assertEquals(
                "South",
                location.getMapaLocationAtributo().get(caracteristicaLocation).getAtributo());

    }

    @Test
    public void locationShouldRejectEveryNonCommunityField() throws IllegalAccessException {

        LocationIntegrationMapper locationIntegrationMapper = new LocationIntegrationMapper();

        /*
         * Este teste funciona como allowlist viva do contrato publico Community.
         * Se um novo campo for adicionado ao DTO compartilhado, ele precisa ser
         * classificado explicitamente: ou entra na allowlist Community acima, ou
         * o mapper deve rejeita-lo com RequiresEnterpriseVersionException.
         */
        for (Field field : LocationIntegrationDataDto.class.getDeclaredFields()) {
            if (COMMUNITY_ACCEPTED_FIELD_NAMES.contains(field.getName())) {
                continue;
            }

            LocationIntegrationDataDto locationIntegrationDataDto =
                    LocationIntegrationDataDto.builder()
                            .build();
            field.setAccessible(true);
            field.set(locationIntegrationDataDto, getEnterpriseFieldValue(field));

            Assertions.assertThrows(
                    RequiresEnterpriseVersionException.class,
                    () -> locationIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                            new Location("LOC_01"),
                            locationIntegrationDataDto,
                            getLocationIntegrationSupportData(),
                            null));
        }

    }

    @Test
    public void locationShouldResolveReferenceLocationFromBatchSupportData() {

        LocationIntegrationMapper locationIntegrationMapper = new LocationIntegrationMapper();
        Location referenceLocation = new Location("REFERENCE_01");
        Location location = new Location("LOCATION_01");

        locationIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                location,
                LocationIntegrationDataDto.builder()
                        .referenceLocationForProductLocationParameters(referenceLocation.getId())
                        .build(),
                getLocationIntegrationSupportData(referenceLocation),
                null);

        Assertions.assertSame(
                referenceLocation,
                location.getReferenceLocationForProductLocationParameters());

    }

    @Test
    public void locationShouldClearReferenceLocationWhenPayloadValueIsNull() {

        LocationIntegrationMapper locationIntegrationMapper = new LocationIntegrationMapper();
        Location location = new Location("LOCATION_01");
        location.setReferenceLocationForProductLocationParameters(new Location("REFERENCE_01"));

        locationIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                location,
                LocationIntegrationDataDto.builder().build(),
                getLocationIntegrationSupportData(),
                null);

        Assertions.assertNull(location.getReferenceLocationForProductLocationParameters());

    }

    @Test
    public void locationShouldRejectUnknownReferenceLocation() {

        LocationIntegrationMapper locationIntegrationMapper = new LocationIntegrationMapper();

        Assertions.assertThrows(
                MissingDependencyDataUploadException.class,
                () -> locationIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                        new Location("LOCATION_01"),
                        LocationIntegrationDataDto.builder()
                                .referenceLocationForProductLocationParameters("MISSING_REFERENCE")
                                .build(),
                        getLocationIntegrationSupportData(),
                        null));

    }

    @Test
    public void locationExportShouldHideEnterpriseFieldsCommunity() {

        LocationIntegrationMapper locationIntegrationMapper = new LocationIntegrationMapper();
        Location location = new Location("LOC_01");
        location.setLatitude(-23.5d);
        location.setLongitude(-46.6d);
        location.setPrazoAtendimentoDias(5);

        LocationIntegrationDataDto locationIntegrationDataDto =
                locationIntegrationMapper.getDtoWithoutPrimaryKeyFromEntity(location);
        ProcessedFileRow processedFileRow =
                locationIntegrationMapper.convertEntityToProcessedFileRow(
                        location,
                        getLocationIntegrationSupportData());

        Assertions.assertNull(locationIntegrationDataDto.latitude);
        Assertions.assertNull(locationIntegrationDataDto.longitude);
        Assertions.assertNull(locationIntegrationDataDto.orderFulfillmentTimeDays);
        Assertions.assertEquals(13, processedFileRow.getRowSize());
        Assertions.assertNull(processedFileRow.getColumnValue(7));
        Assertions.assertEquals(location.getPlanejaProducaoCadastrado(), processedFileRow.getColumnValue(8));
        Assertions.assertEquals(location.getPlanejaSupplyCadastrado(), processedFileRow.getColumnValue(9));
        Assertions.assertEquals(location.getConsideraRestricaoProducaoCadastrado(), processedFileRow.getColumnValue(10));
        Assertions.assertEquals(location.getIncluiDemandaIndiretaNoSafetyStockCadastrado(), processedFileRow.getColumnValue(12));

    }

    @Test
    public void locationHeadersShouldExposeOnlyCommunityColumns() {

        LocationIntegrationMapper locationIntegrationMapper = new LocationIntegrationMapper();

        List<String> processedFileHeaders = locationIntegrationMapper.getProcessedFileHeaders();

        Assertions.assertTrue(LocationIntegrationMapper.class.isAnnotationPresent(Component.class));
        Assertions.assertEquals(List.of(
                "Location Id",
                "Description",
                "Active (True/False or 1/0) : Default = True if empty",
                "Location Type : 'Internal', 'End Client', 'Supplier', 'Commercial Region' or 'Transshipment Point'. Default = 'Internal'",
                "Country",
                "State",
                "City",
                "Reference Location for Product-Location parameter mirroring",
                "Available in Production Planning Book : true/false or 0/1",
                "Available in Supply Planning Book : true/false or 0/1",
                "Finite production capacity (for constrained plan. default = true) : true/false or 0/1",
                "Default UOM for supply planning (SNP)",
                "Safety Stocks considers indirect demand (default = true) : true/false or 0/1"
        ), processedFileHeaders);

        Assertions.assertFalse(processedFileHeaders.stream().anyMatch(processedFileHeader ->
                processedFileHeader.contains("Enterprise")
                        || processedFileHeader.contains("Latitude")
                        || processedFileHeader.contains("Longitude")
                        || processedFileHeader.contains("Expedition")
                        || processedFileHeader.contains("Fullfillment")
                        || processedFileHeader.contains("Fulfillment")));
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> processedFileHeaders.add("Enterprise Test Header"));

    }

    private LocationIntegrationSupportData getLocationIntegrationSupportData() {

        return LocationIntegrationSupportData.builder()
                .caracteristicaLocationList(List.of())
                .unidadeMedidaMap(new HashMap<>())
                .locationMap(new HashMap<>())
                .build();

    }

    /**
     * Monta a fotografia batch minima para testar a resolucao de referencia
     * sem qualquer consulta por linha.
     */
    private LocationIntegrationSupportData getLocationIntegrationSupportData(Location referenceLocation) {

        return LocationIntegrationSupportData.builder()
                .caracteristicaLocationList(List.of())
                .unidadeMedidaMap(new HashMap<>())
                .locationMap(Map.of(referenceLocation.getId(), referenceLocation))
                .build();

    }

    private LocationIntegrationSupportData getLocationIntegrationSupportData(
            CaracteristicaLocation caracteristicaLocation) {

        return LocationIntegrationSupportData.builder()
                .caracteristicaLocationList(List.of(caracteristicaLocation))
                .unidadeMedidaMap(new HashMap<>())
                .locationMap(new HashMap<>())
                .build();

    }

    private CaracteristicaLocation getLocationCharacteristic(String id, String description) {

        CaracteristicaLocation caracteristicaLocation = new CaracteristicaLocation();
        caracteristicaLocation.setId(id);
        caracteristicaLocation.setDescricao(description);
        caracteristicaLocation.setTipoCaracteristica(Caracteristica.TipoCaracteristica.CATEGORICO);
        return caracteristicaLocation;

    }

    private Object getEnterpriseFieldValue(Field field) {

        if (field.getType().equals(Double.class)) {
            return 1.0d;
        }
        if (field.getType().equals(Integer.class)) {
            return 1;
        }
        if (field.getType().equals(String.class)) {
            return "ENTERPRISE_VALUE";
        }
        if (Map.class.isAssignableFrom(field.getType())) {
            return Map.of("EnterpriseCharacteristic", "Value");
        }

        throw new IllegalArgumentException("Unsupported Enterprise field type in test: " + field);

    }

}
