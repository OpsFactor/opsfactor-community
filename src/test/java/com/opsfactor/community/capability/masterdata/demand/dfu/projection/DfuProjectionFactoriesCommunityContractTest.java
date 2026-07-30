package com.opsfactor.community.capability.masterdata.demand.dfu.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contrato das factories Community de escopo material/location.
 *
 * <p>Essas factories nao representam filtros funcionais Enterprise. Elas apenas
 * empacotam subconjuntos tecnicos de DFUs para projections em memoria. Colecao
 * vazia continua valida, mas snapshot nulo ou item nulo deve falhar antes de
 * virar NPE em calculos paralelos.</p>
 */
public class DfuProjectionFactoriesCommunityContractTest {

    @Test
    public void materialProjectionFactoryShouldAcceptEmptyMaterialCollection() {

        MaterialProjection materialProjection = MaterialProjectionFactory.getProjectionSetMateriais(
                Set.of(),
                new ClusterEParametrosProjection());

        Assertions.assertTrue(materialProjection.getMaterialSet().isEmpty());

    }

    @Test
    public void materialProjectionFactoryShouldRejectBrokenMaterialInputs() {

        IllegalArgumentException nullCollectionException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> MaterialProjectionFactory.getProjectionSetMateriais(
                        null,
                        new ClusterEParametrosProjection()));
        Assertions.assertEquals(
                "MaterialProjectionFactory received null material collection.",
                nullCollectionException.getMessage());

        IllegalArgumentException nullDfuMaterialException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> MaterialProjectionFactory.getProjectionDeDfus(
                        List.of(new DFU(null, new Location("LOC_01"))),
                        new ClusterEParametrosProjection()));
        Assertions.assertEquals(
                "MaterialProjectionFactory received DFU without material at index 0.",
                nullDfuMaterialException.getMessage());

        IllegalArgumentException nullDfuException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> MaterialProjectionFactory.getProjectionDeDfus(
                        Collections.singletonList(null),
                        new ClusterEParametrosProjection()));
        Assertions.assertEquals(
                "MaterialProjectionFactory received null DFU at index 0.",
                nullDfuException.getMessage());

    }

    @Test
    public void locationProjectionFactoryShouldAcceptEmptyLocationCollection() {

        LocationProjection locationProjection = LocationProjectionFactory.getProjectionSetLocations(
                Set.of(),
                new ClusterEParametrosProjection());

        Assertions.assertTrue(locationProjection.getLocationSet().isEmpty());

    }

    @Test
    public void locationProjectionFactoryShouldRejectBrokenLocationInputs() {

        IllegalArgumentException nullCollectionException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> LocationProjectionFactory.getProjectionSetLocations(
                        null,
                        new ClusterEParametrosProjection()));
        Assertions.assertEquals(
                "LocationProjectionFactory received null location collection.",
                nullCollectionException.getMessage());

        IllegalArgumentException nullDfuLocationException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> LocationProjectionFactory.getProjectionDeDfus(
                        List.of(new DFU(new Produto("MAT_01"), null)),
                        new ClusterEParametrosProjection()));
        Assertions.assertEquals(
                "LocationProjectionFactory received DFU without location at index 0.",
                nullDfuLocationException.getMessage());

        IllegalArgumentException blankLocationIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> LocationProjectionFactory.getProjectionSetLocationIds(
                        List.of(""),
                        new ClusterEParametrosProjection()));
        Assertions.assertEquals(
                "LocationProjectionFactory received blank location id at index 0.",
                blankLocationIdException.getMessage());

    }

    @Test
    public void projectionFactoriesShouldRejectMissingClusterProjection() {

        IllegalArgumentException materialException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> MaterialProjectionFactory.getMaterialProjectionCompleto(null));
        Assertions.assertEquals(
                "Cluster/parameter projection is required for material projection.",
                materialException.getMessage());

        IllegalArgumentException locationException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> LocationProjectionFactory.getLocationProjectionCompleto(null));
        Assertions.assertEquals(
                "Cluster/parameter projection is required for location projection.",
                locationException.getMessage());

    }

    @Test
    public void explicitScopeFactoriesShouldAllowMissingClusterProjectionWhenScopeDoesNotNeedStatusLookup() {

        MaterialProjection materialProjection = MaterialProjectionFactory.getProjectionSetMateriais(
                Set.of(new Produto("MAT_01")),
                null);
        LocationProjection locationProjection = LocationProjectionFactory.getProjectionSetLocations(
                Set.of(new Location("LOC_01")),
                null);

        Assertions.assertEquals(1, materialProjection.getMaterialSet().size());
        Assertions.assertEquals(1, locationProjection.getLocationSet().size());

    }

    @Test
    public void supplyProfileMaterialProjectionShouldRemainCompleteWhenPrivateFilterBridgeIsPresent() {

        Produto firstMaterial = new Produto("MAT_01");
        Produto secondMaterial = new Produto("MAT_02");
        ClusterEParametrosProjection clusterAndParametersProjection = Mockito.mock(
                ClusterEParametrosProjection.class);
        Mockito.when(clusterAndParametersProjection.getMateriaisAtivos())
                .thenReturn(Set.of(firstMaterial, secondMaterial));
        PerfilExecucaoSupplyPlan profile = new PerfilExecucaoSupplyPlan();
        profile.setMaterialFilterId("PRIVATE-FILTER");

        MaterialProjection materialProjection =
                MaterialProjectionFactory.getMaterialProjectionDePerfilExecucaoSupplyPlan(
                        profile,
                        clusterAndParametersProjection);

        Assertions.assertEquals(
                Set.of(firstMaterial, secondMaterial),
                materialProjection.getMaterialSet());
        Mockito.verify(clusterAndParametersProjection).getMateriaisAtivos();
        Mockito.verifyNoMoreInteractions(clusterAndParametersProjection);

    }

    @Test
    public void materialCharacteristicProjectionShouldUseOnlyCommunityMaterialIdWithoutDataAccess() {

        Produto firstMaterial = new Produto("MAT_01");
        Produto secondMaterial = new Produto("MAT_02");
        ClusterEParametrosProjection clusterAndParametersProjection = Mockito.mock(
                ClusterEParametrosProjection.class);
        Mockito.when(clusterAndParametersProjection.getMateriais(false))
                .thenReturn(Set.of(firstMaterial, secondMaterial));

        MaterialProjection materialProjection =
                MaterialProjectionFactory.getProjectionByMaterialCharacteristicValues(
                        Map.of("materialId", List.of("mat_02")),
                        clusterAndParametersProjection,
                        false);

        Assertions.assertEquals(Set.of(secondMaterial), materialProjection.getMaterialSet());
        Mockito.verify(clusterAndParametersProjection).getMateriais(false);
        Mockito.verifyNoMoreInteractions(clusterAndParametersProjection);

    }

    @Test
    public void materialCharacteristicProjectionShouldRejectEnterpriseDynamicCharacteristics() {

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> MaterialProjectionFactory.getProjectionByMaterialCharacteristicValues(
                        Map.of("FAMILY", List.of("A")),
                        new ClusterEParametrosProjection(),
                        false));

        Assertions.assertEquals(
                "Community supports only the materialId material characteristic filter.",
                exception.getMessage());

    }

}
