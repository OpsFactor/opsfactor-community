package com.opsfactor.community.capability.masterdata.network.location.service;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * Contratos Community do service basico de locations.
 *
 * <p>Location fica no backend aberto como cadastro operacional simples. GIS,
 * mapa, warehouses, last mile e restricoes logisticas avancadas seguem
 * Enterprise; por isso este teste protege as bordas basicas de persistencia
 * usadas por callers internos do modelo Community.</p>
 */
public class LocationServiceCommunityContractTest {

    @Test
    public void saveShouldRejectNullLocationBeforeRepository() {

        LocationService locationService = new LocationService();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> locationService.save(null));

        Assertions.assertEquals(
                "Location to save is required.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void saveShouldRejectLocationWithoutIdBeforeRepository() {

        LocationService locationService = new LocationService();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> locationService.save(new Location()));

        Assertions.assertEquals(
                "Location to save must have an id.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void saveShouldReturnSavedLocationSnapshot() throws Exception {

        LocationService locationService = new LocationService();
        Location locationSalva = new Location("LOC_01");
        setField(
                locationService,
                "locationRepository",
                getLocationRepositoryReturningLocationOnSave(locationSalva));

        Location locationRetornada = locationService.save(new Location("LOC_01"));

        Assertions.assertSame(locationSalva, locationRetornada);

    }

    @Test
    public void saveShouldRejectNullSavedLocationSnapshot() throws Exception {

        LocationService locationService = new LocationService();
        setField(
                locationService,
                "locationRepository",
                getLocationRepositoryReturningLocationOnSave(null));

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> locationService.save(new Location("LOC_01")));

        Assertions.assertEquals(
                "Saved location is required.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void saveShouldRejectSavedLocationSnapshotWithDifferentId() throws Exception {

        LocationService locationService = new LocationService();
        setField(
                locationService,
                "locationRepository",
                getLocationRepositoryReturningLocationOnSave(new Location("LOC_02")));

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> locationService.save(new Location("LOC_01")));

        Assertions.assertEquals(
                "Saved location id LOC_02 does not match requested location id LOC_01.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void saveAllShouldRejectNullLocationCollectionBeforeRepository() {

        LocationService locationService = new LocationService();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> locationService.saveAll(null));

        Assertions.assertEquals(
                "Location collection to save is required.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void saveAllShouldRejectNullSavedLocationSnapshot() throws Exception {

        LocationService locationService = new LocationService();
        setField(
                locationService,
                "locationRepository",
                getLocationRepositoryReturningNullOnSaveAll());

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> locationService.saveAll(List.of(new Location("LOC_01"))));

        Assertions.assertEquals(
                "Saved location collection is required.",
                illegalArgumentException.getMessage());

    }

    private static LocationRepository getLocationRepositoryReturningLocationOnSave(Location locationSalva) {

        return (LocationRepository) Proxy.newProxyInstance(
                LocationRepository.class.getClassLoader(),
                new Class<?>[]{LocationRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        return locationSalva;
                    }
                    if ("toString".equals(method.getName())) {
                        return "LocationRepository save test double";
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    throw new AssertionError(
                            "Repository method should not be called by LocationService save test: "
                                    + method.getName());
                });

    }

    private static LocationRepository getLocationRepositoryReturningNullOnSaveAll() {

        return (LocationRepository) Proxy.newProxyInstance(
                LocationRepository.class.getClassLoader(),
                new Class<?>[]{LocationRepository.class},
                (proxy, method, args) -> {
                    if ("saveAll".equals(method.getName())) {
                        return null;
                    }
                    if ("toString".equals(method.getName())) {
                        return "LocationRepository saveAll test double";
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    throw new AssertionError(
                            "Repository method should not be called by LocationService saveAll test: "
                                    + method.getName());
                });

    }

    private static void setField(
            Object target,
            String fieldName,
            Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

}
