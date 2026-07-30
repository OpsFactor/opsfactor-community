package com.opsfactor.community.capability.planningbook;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.FiltroDFUProjection;
import com.opsfactor.community.capability.planningbook.domain.PlanningBookDfuScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Contrato Community do escopo material/location usado pelo Planning Book.
 *
 * <p>O escopo substitui os antigos envelopes agregados no Community. Ele deve
 * ser um snapshot tecnico imutavel: vazio e valido, mas nulo estrutural,
 * item nulo ou material/location sem id devem falhar antes de montar DTOs,
 * filters ou projections derivadas.</p>
 */
public class PlanningBookDfuScopeCommunityContractTest {

    @Test
    public void scopeShouldCopyAndExposeImmutableMaterialLocationSets() {

        Produto material = new Produto("MAT_01", "Material 01");
        Location location = new Location("LOC_01", "Location 01");
        Set<Produto> materiais = new HashSet<>(Set.of(material));
        Set<Location> locations = new HashSet<>(Set.of(location));

        PlanningBookDfuScope planningBookDfuScope = PlanningBookDfuScope.deMateriaisELocations(
                materiais,
                locations);

        materiais.clear();
        locations.clear();

        Assertions.assertEquals(Set.of(material), planningBookDfuScope.getMateriais());
        Assertions.assertEquals(Set.of(location), planningBookDfuScope.getLocations());
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> planningBookDfuScope.getMateriais().add(new Produto("MAT_02", "Material 02")));

    }

    @Test
    public void scopeShouldRejectBrokenMaterialLocationSets() {

        IllegalArgumentException nullMaterialSetException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> PlanningBookDfuScope.deMateriaisELocations(
                        null,
                        Set.of(new Location("LOC_01", "Location 01"))));
        Assertions.assertEquals(
                "PlanningBookDfuScope material set is required.",
                nullMaterialSetException.getMessage());

        Set<Produto> materiaisComItemNulo = new HashSet<>();
        materiaisComItemNulo.add(null);
        IllegalArgumentException nullMaterialException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> PlanningBookDfuScope.deMateriaisELocations(
                        materiaisComItemNulo,
                        Set.of(new Location("LOC_01", "Location 01"))));
        Assertions.assertEquals(
                "PlanningBookDfuScope material set contains null material at index 0.",
                nullMaterialException.getMessage());

        IllegalArgumentException blankLocationIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> PlanningBookDfuScope.deMaterialLocation(
                        new Produto("MAT_01", "Material 01"),
                        new Location("", "Location 01")));
        Assertions.assertEquals(
                "PlanningBookDfuScope location id is required.",
                blankLocationIdException.getMessage());

    }

    @Test
    public void intersectionShouldRejectMissingProjectionInputsBeforeLoop() {

        PlanningBookDfuScope planningBookDfuScope = PlanningBookDfuScope.deMaterialLocation(
                new Produto("MAT_01", "Material 01"),
                new Location("LOC_01", "Location 01"));

        IllegalArgumentException missingFilterException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> planningBookDfuScope.getNovoDFUProjectionInterseccaoComProjectionFiltro(
                        null,
                        new ClusterEParametrosProjection()));
        Assertions.assertEquals(
                "PlanningBookDfuScope requires DFU filter projection to create an intersection projection.",
                missingFilterException.getMessage());

        FiltroDFUProjection filtroDFUProjection = new FiltroDFUProjection(
                Set.of(new Location("LOC_01", "Location 01")),
                Set.of(new Produto("MAT_01", "Material 01")),
                new ClusterEParametrosProjection());
        IllegalArgumentException missingClusterProjectionException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> planningBookDfuScope.getNovoDFUProjectionInterseccaoComProjectionFiltro(
                        filtroDFUProjection,
                        null));
        Assertions.assertEquals(
                "PlanningBookDfuScope requires cluster/parameter projection to create an intersection projection.",
                missingClusterProjectionException.getMessage());

    }

    @Test
    public void planningBookColumnsShouldUseDomainDescriptionDefaultsWhenDescriptionIsMissing() {

        PlanningBookDfuScope planningBookDfuScope = PlanningBookDfuScope.deMaterialLocation(
                new Produto("MAT_01"),
                new Location("LOC_01", "Location 01"));

        Assertions.assertEquals(
                "",
                planningBookDfuScope.getColunasMaterialPlanningBook().get("materialDescription"));

    }

    @Test
    public void planningBookColumnsShouldUseMaterialLocationDescriptionsForDfuRows() {

        PlanningBookDfuScope planningBookDfuScope = PlanningBookDfuScope.deMaterialLocation(
                new Produto("MAT_01", "Material 01"),
                new Location("LOC_01", "Location 01"));

        Map<String, String> materialColumns = planningBookDfuScope.getColunasMaterialPlanningBook();
        Map<String, String> locationColumns = planningBookDfuScope.getColunasLocationPlanningBook();

        Assertions.assertEquals("MAT_01", materialColumns.get("materialId"));
        Assertions.assertEquals("Material 01", materialColumns.get("materialDescription"));
        Assertions.assertEquals("LOC_01", locationColumns.get("locationId"));
        Assertions.assertEquals("Location 01", locationColumns.get("locationDescription"));

    }

}
