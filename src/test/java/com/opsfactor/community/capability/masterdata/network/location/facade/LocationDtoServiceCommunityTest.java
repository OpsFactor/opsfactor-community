package com.opsfactor.community.capability.masterdata.network.location.facade;

import com.opsfactor.community.capability.masterdata.network.location.facade.dto.LocationDTO;
import com.opsfactor.community.capability.masterdata.network.location.facade.mapper.LocationMapper;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import jakarta.persistence.NoResultException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Valida o contrato Community do endpoint legado de location.
 *
 * <p>O service ainda existe para compatibilidade, mas nao pode reabrir
 * deployment, restricoes logisticas ou coordenadas geograficas.</p>
 */
public class LocationDtoServiceCommunityTest {

    private static final Set<String> COMMUNITY_ACCEPTED_FIELD_NAMES = Set.of(
            "id",
            "locationType",
            "active",
            "description",
            "country",
            "state",
            "city",
            "showInSupplyPlanningBook",
            "showInProductionPlanningBook",
            "applyInboundConstraints",
            "safetyStockConsiderIndirectDemand",
            "applyProductionConstraints");

    @Test
    public void saveLocationDTOShouldRejectMissingPayloadBeforeRepository() throws Exception {

        LocationDtoService locationDtoService = getLocationDtoService();

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> locationDtoService.saveLocationDTO(null));

        LocationDTO locationDTO = new LocationDTO();
        locationDTO.id = " ";

        Assertions.assertThrows(
                NoResultException.class,
                () -> locationDtoService.saveLocationDTO(locationDTO));

    }

    @Test
    public void saveLocationDTOShouldRejectDeploymentVisibilityCommunity() throws Exception {

        LocationDtoService locationDtoService = getLocationDtoService();
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.id = "LOC_01";
        locationDTO.showInDeployment = true;

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> locationDtoService.saveLocationDTO(locationDTO));

    }

    @Test
    public void saveLocationDTOShouldRejectDeploymentVisibilityDisabledCommunity() throws Exception {

        LocationDtoService locationDtoService = getLocationDtoService();
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.id = "LOC_01";
        locationDTO.showInDeployment = false;

        /*
         * A coluna existe no aggregate compartilhado, mas o Community nao
         * aceita configurar deployment pelo contrato administrativo.
         */
        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> locationDtoService.saveLocationDTO(locationDTO));

    }

    @Test
    public void locationShouldKeepDeploymentPlanningEnabledByDefault() {

        Location locationWithoutDeploymentConfiguration = new Location("LOC_DEFAULT");
        Location locationWithDeploymentDisabled = new Location("LOC_DISABLED");
        locationWithDeploymentDisabled.setDeploymentPlanningEnabled(false);

        Assertions.assertTrue(
                locationWithoutDeploymentConfiguration.getDeploymentPlanningEnabled());
        Assertions.assertFalse(
                locationWithDeploymentDisabled.getDeploymentPlanningEnabled());

    }

    @Test
    public void saveLocationDTOShouldPersistInboundConstraintsCommunity() {

        LocationRepositoryStub locationRepositoryStub = new LocationRepositoryStub();
        LocationDtoService locationDtoService = getLocationDtoService(locationRepositoryStub);
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.id = "LOC_01";
        locationDTO.applyInboundConstraints = false;

        locationDtoService.saveLocationDTO(locationDTO);

        Assertions.assertFalse(
                locationRepositoryStub.savedLocation.getConsideraRestricaoLinhaInbound());

    }

    @Test
    public void saveLocationDTOShouldPersistAndClearSafetyStockIndirectDemandOverrideCommunity() {

        LocationRepositoryStub locationRepositoryStub = new LocationRepositoryStub();
        LocationDtoService locationDtoService = getLocationDtoService(locationRepositoryStub);
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.id = "LOC_SAFETY_STOCK";
        locationDTO.safetyStockConsiderIndirectDemand = false;

        locationDtoService.saveLocationDTO(locationDTO);

        Assertions.assertEquals(
                Boolean.FALSE,
                locationRepositoryStub.savedLocation
                        .getIncluiDemandaIndiretaNoSafetyStockCadastrado());

        LocationDTO cleanupLocationDTO = new LocationDTO();
        cleanupLocationDTO.id = "LOC_SAFETY_STOCK";
        cleanupLocationDTO.safetyStockConsiderIndirectDemand = null;

        locationDtoService.saveLocationDTO(cleanupLocationDTO);

        Assertions.assertNull(
                locationRepositoryStub.savedLocation
                        .getIncluiDemandaIndiretaNoSafetyStockCadastrado());

    }

    @Test
    public void saveLocationDTOShouldPersistActiveStatusCommunity() {

        LocationRepositoryStub locationRepositoryStub = new LocationRepositoryStub();
        LocationDtoService locationDtoService = getLocationDtoService(locationRepositoryStub);
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.id = "LOC_INACTIVE";
        locationDTO.active = false;

        locationDtoService.saveLocationDTO(locationDTO);

        Assertions.assertFalse(locationRepositoryStub.savedLocation.getAtivo());

    }

    @Test
    public void locationMapperShouldExposeEffectiveActiveStatusCommunity() {

        Location locationWithoutConfiguredStatus = new Location("LOC_ACTIVE_DEFAULT");
        Location locationConfiguredInactive = new Location("LOC_INACTIVE");
        locationConfiguredInactive.setAtivo(false);

        LocationDTO defaultLocationDTO = LocationMapper.convertSemCaracteristicasLocation(
                locationWithoutConfiguredStatus);
        LocationDTO inactiveLocationDTO = LocationMapper.convertSemCaracteristicasLocation(
                locationConfiguredInactive);

        Assertions.assertTrue(defaultLocationDTO.active);
        Assertions.assertFalse(inactiveLocationDTO.active);

    }

    @Test
    public void saveLocationDTOShouldKeepActiveDomainDefaultWhenFieldIsOmitted() {

        LocationRepositoryStub locationRepositoryStub = new LocationRepositoryStub();
        LocationDtoService locationDtoService = getLocationDtoService(locationRepositoryStub);
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.id = "LOC_ACTIVE_DEFAULT";

        locationDtoService.saveLocationDTO(locationDTO);

        /*
         * A coluna continua nullable para preservar cadastros existentes e o
         * getter efetivo da entidade trata ausencia como location ativa.
         */
        Assertions.assertTrue(locationRepositoryStub.savedLocation.getAtivo());

    }

    @Test
    public void saveLocationDTOShouldKeepInboundConstraintDomainDefaultWhenFieldIsOmitted() {

        LocationRepositoryStub locationRepositoryStub = new LocationRepositoryStub();
        LocationDtoService locationDtoService = getLocationDtoService(locationRepositoryStub);
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.id = "LOC_DEFAULT";

        locationDtoService.saveLocationDTO(locationDTO);

        /*
         * A coluna e nullable para compatibilidade com cadastros existentes.
         * O getter efetivo deve continuar alinhado ao heuristico Community,
         * que considera a restricao inbound quando nao ha configuracao local.
         */
        Assertions.assertTrue(
                locationRepositoryStub.savedLocation.getConsideraRestricaoLinhaInbound());

    }

    @Test
    public void saveLocationDTOShouldRejectLogisticsConstraintsCommunity() throws Exception {

        LocationDtoService locationDtoService = getLocationDtoService();
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.id = "LOC_01";
        locationDTO.applyLogisticsConstraints = true;

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> locationDtoService.saveLocationDTO(locationDTO));

    }

    @Test
    public void saveLocationDTOShouldRejectGeographicCoordinatesCommunity() throws Exception {

        LocationDtoService locationDtoService = getLocationDtoService();
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.id = "LOC_01";
        locationDTO.latitude = -23.5d;

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> locationDtoService.saveLocationDTO(locationDTO));

    }

    @Test
    public void saveLocationDTOShouldRejectLongitudeCommunity() throws Exception {

        LocationDtoService locationDtoService = getLocationDtoService();
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.id = "LOC_01";
        locationDTO.longitude = -46.6d;

        /*
         * Latitude e longitude fazem parte da mesma capacidade Enterprise de
         * mapa/GIS. O teste separado evita que uma futura alteracao bloqueie
         * apenas uma das coordenadas.
         */
        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> locationDtoService.saveLocationDTO(locationDTO));

    }

    @Test
    public void saveLocationDTOShouldRejectCharacteristicsCommunity() throws Exception {

        LocationDtoService locationDtoService = getLocationDtoService();
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.id = "LOC_01";
        locationDTO.characteristicValues.put("Region", "South");

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> locationDtoService.saveLocationDTO(locationDTO));

    }

    @Test
    public void saveLocationDTOShouldPersistCommunityFields() {

        LocationRepositoryStub locationRepositoryStub = new LocationRepositoryStub();
        LocationDtoService locationDtoService = getLocationDtoService(locationRepositoryStub);
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.id = "LOC_01";
        locationDTO.description = "Location 01";
        locationDTO.active = false;
        locationDTO.applyInboundConstraints = false;
        locationDTO.safetyStockConsiderIndirectDemand = false;
        locationDTO.applyProductionConstraints = true;
        locationDTO.showInSupplyPlanningBook = true;
        locationDTO.showInProductionPlanningBook = false;

        locationDtoService.saveLocationDTO(locationDTO);

        Assertions.assertEquals("LOC_01", locationRepositoryStub.savedLocation.getId());
        Assertions.assertEquals("Location 01", locationRepositoryStub.savedLocation.getDescricao());
        Assertions.assertFalse(locationRepositoryStub.savedLocation.getAtivo());
        Assertions.assertFalse(locationRepositoryStub.savedLocation.getConsideraRestricaoLinhaInbound());
        Assertions.assertEquals(
                Boolean.FALSE,
                locationRepositoryStub.savedLocation
                        .getIncluiDemandaIndiretaNoSafetyStockCadastrado());
        Assertions.assertEquals(Boolean.TRUE, locationRepositoryStub.savedLocation.getConsideraRestricaoProducao());
        Assertions.assertEquals(Boolean.TRUE, locationRepositoryStub.savedLocation.getPlanejaSupply());
        Assertions.assertEquals(Boolean.FALSE, locationRepositoryStub.savedLocation.getPlanejaProducao());

    }

    @Test
    public void saveLocationDTOShouldRejectBrokenSavedSnapshot() {

        LocationRepositoryStub locationRepositoryStub = new LocationRepositoryStub();
        locationRepositoryStub.saveReturnOverrideConfigured = true;
        locationRepositoryStub.saveReturnOverride = null;
        LocationDtoService locationDtoService = getLocationDtoService(locationRepositoryStub);
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.id = "LOC_01";

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> locationDtoService.saveLocationDTO(locationDTO));

        Assertions.assertEquals(
                "Community location save returned invalid snapshot.",
                illegalStateException.getMessage());
        Assertions.assertNotNull(locationRepositoryStub.savedLocation);

    }

    @Test
    public void saveLocationDTOShouldRejectEveryNonCommunityFieldBeforeRepository() throws Exception {

        LocationDtoService locationDtoService = getLocationDtoService();

        /*
         * Allowlist viva do endpoint legado de Location. A chamada roda sem
         * repository injetado para provar que qualquer campo fora do contrato
         * Community falha por versao antes de lookup/persistencia.
         */
        for (Field field : LocationDTO.class.getDeclaredFields()) {
            if (COMMUNITY_ACCEPTED_FIELD_NAMES.contains(field.getName())) {
                continue;
            }

            LocationDTO locationDTO = new LocationDTO();
            locationDTO.id = "LOC_01";
            field.setAccessible(true);
            field.set(locationDTO, getEnterpriseFieldValue(field));

            Assertions.assertThrows(
                    RequiresEnterpriseVersionException.class,
                    () -> locationDtoService.saveLocationDTO(locationDTO));
        }

    }

    private LocationDtoService getLocationDtoService() {

        /*
         * Nenhum repository e injetado de proposito: todos os payloads
         * Enterprise destes testes devem falhar antes de qualquer lookup.
         */
        LocationDtoService locationDtoService = new LocationDtoService();
        return locationDtoService;

    }

    private LocationDtoService getLocationDtoService(LocationRepositoryStub locationRepositoryStub) {

        LocationDtoService locationDtoService = new LocationDtoService();
        setPrivateField(
                locationDtoService,
                "locationRepository",
                locationRepositoryStub.getRepository());
        return locationDtoService;

    }

    private void setPrivateField(
            LocationDtoService locationDtoService,
            String fieldName,
            Object value) {

        try {
            Field field = LocationDtoService.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(locationDtoService, value);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalStateException(
                    "Unable to configure LocationDtoService test field " + fieldName,
                    reflectiveOperationException);
        }

    }

    private Object getEnterpriseFieldValue(Field field) {

        if (field.getType().equals(Double.class)) {
            return 1.0d;
        }
        if (field.getType().equals(Boolean.class)) {
            return true;
        }
        if (Map.class.isAssignableFrom(field.getType())) {
            return Map.of("EnterpriseCharacteristic", "Value");
        }
        if (field.getType().equals(String.class)) {
            return "ENTERPRISE_VALUE";
        }

        throw new IllegalArgumentException("Unsupported Enterprise field type in test: " + field);

    }

    /**
     * Repository proxy minimo para testar o endpoint legado sem Spring Data.
     */
    private static class LocationRepositoryStub {

        private Location existingLocation;
        private Location savedLocation;
        private boolean saveReturnOverrideConfigured;
        private Location saveReturnOverride;

        private LocationRepository getRepository() {

            return (LocationRepository) Proxy.newProxyInstance(
                    LocationRepository.class.getClassLoader(),
                    new Class[]{LocationRepository.class},
                    this::invoke);

        }

        private Object invoke(Object proxy, Method method, Object[] args) {

            return switch (method.getName()) {
                case "findById" -> Optional.ofNullable(existingLocation);
                case "save" -> {
                    savedLocation = (Location) args[0];
                    existingLocation = savedLocation;
                    yield saveReturnOverrideConfigured ? saveReturnOverride : savedLocation;
                }
                case "toString" -> "LocationRepositoryStub";
                case "hashCode" -> System.identityHashCode(this);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(
                        "Metodo nao suportado no stub: " + method.getName());
            };

        }

    }

}
