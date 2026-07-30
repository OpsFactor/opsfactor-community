package com.opsfactor.community.capability.masterdata.demand.dfu.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contrato Community do filtro DFU material/location.
 *
 * <p>O filtro e um escopo tecnico em memoria usado por Planning Book,
 * Demand Planning e Supply Planning. Ele nao representa filtros/agregadores
 * funcionais Enterprise; por isso deve aceitar escopo vazio, mas rejeitar
 * snapshots estruturalmente quebrados antes de stream, agrupamento ou consulta
 * de DFU ativa.</p>
 */
public class FiltroDfuProjectionCommunityContractTest {

    @Test
    public void cartesianScopeShouldAcceptEmptySetsAndOwnMutableCopy() {

        Set<Location> locations = new HashSet<>(Set.of(new Location("LOC_01")));
        Set<Produto> materiais = new HashSet<>(Set.of(new Produto("MAT_01")));
        FiltroDFUProjection filtroDFUProjection = new FiltroDFUProjection(
                locations,
                materiais,
                null);

        /*
         * A projection copia o escopo recebido porque alguns fluxos aplicam
         * filtro incremental. A mutacao externa posterior nao deve alterar o
         * snapshot tecnico ja criado.
         */
        locations.clear();
        materiais.clear();

        Assertions.assertEquals(1, filtroDFUProjection.getLocations().size());
        Assertions.assertEquals(1, filtroDFUProjection.getMateriais().size());

    }

    @Test
    public void constructorsShouldRejectNullCollectionsAndItems() {

        IllegalArgumentException nullLocationsException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new FiltroDFUProjection(
                        null,
                        Set.of(new Produto("MAT_01")),
                        null));
        Assertions.assertEquals(
                "FiltroDFUProjection location set is required.",
                nullLocationsException.getMessage());

        Set<Location> locationsComItemNulo = new HashSet<>();
        locationsComItemNulo.add(null);
        IllegalArgumentException nullLocationItemException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new FiltroDFUProjection(
                        locationsComItemNulo,
                        Set.of(new Produto("MAT_01")),
                        null));
        Assertions.assertEquals(
                "FiltroDFUProjection location set contains null location at index 0.",
                nullLocationItemException.getMessage());

        IllegalArgumentException locationWithoutIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new FiltroDFUProjection(
                        Set.of(new Location(" ")),
                        Set.of(new Produto("MAT_01")),
                        null));
        Assertions.assertEquals(
                "FiltroDFUProjection location set contains location without id at index 0.",
                locationWithoutIdException.getMessage());

        Set<Produto> materiaisComItemNulo = new HashSet<>();
        materiaisComItemNulo.add(null);
        IllegalArgumentException nullMaterialItemException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new FiltroDFUProjection(
                        Set.of(new Location("LOC_01")),
                        materiaisComItemNulo,
                        null));
        Assertions.assertEquals(
                "FiltroDFUProjection material set contains null material at index 0.",
                nullMaterialItemException.getMessage());

        IllegalArgumentException materialWithoutIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new FiltroDFUProjection(
                        Set.of(new Location("LOC_01")),
                        Set.of(new Produto(" ")),
                        null));
        Assertions.assertEquals(
                "FiltroDFUProjection material set contains material without id at index 0.",
                materialWithoutIdException.getMessage());

    }

    @Test
    public void dfuCollectionConstructorShouldRejectBrokenDfusBeforeGrouping() {

        IllegalArgumentException nullCollectionException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new FiltroDFUProjection(
                        (java.util.Collection<DFU>) null,
                        new TestClusterEParametrosProjection()));
        Assertions.assertEquals(
                "FiltroDFUProjection DFU collection is required.",
                nullCollectionException.getMessage());

        IllegalArgumentException nullDfuException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new FiltroDFUProjection(
                        Arrays.asList(new DFU(new Produto("MAT_01"), new Location("LOC_01")), null),
                        new TestClusterEParametrosProjection()));
        Assertions.assertEquals(
                "FiltroDFUProjection DFU collection contains null DFU at index 1.",
                nullDfuException.getMessage());

        IllegalArgumentException missingLocationException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new FiltroDFUProjection(
                        List.of(new DFU(new Produto("MAT_01"), null)),
                        new TestClusterEParametrosProjection()));
        Assertions.assertEquals(
                "FiltroDFUProjection DFU collection contains DFU without location at index 0.",
                missingLocationException.getMessage());

        IllegalArgumentException locationWithoutIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new FiltroDFUProjection(
                        List.of(new DFU(new Produto("MAT_01"), new Location(" "))),
                        new TestClusterEParametrosProjection()));
        Assertions.assertEquals(
                "FiltroDFUProjection DFU collection contains DFU with location without id at index 0.",
                locationWithoutIdException.getMessage());

        IllegalArgumentException missingMaterialException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new FiltroDFUProjection(
                        List.of(new DFU(null, new Location("LOC_01"))),
                        new TestClusterEParametrosProjection()));
        Assertions.assertEquals(
                "FiltroDFUProjection DFU collection contains DFU without material at index 0.",
                missingMaterialException.getMessage());

        IllegalArgumentException materialWithoutIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new FiltroDFUProjection(
                        List.of(new DFU(new Produto(" "), new Location("LOC_01"))),
                        new TestClusterEParametrosProjection()));
        Assertions.assertEquals(
                "FiltroDFUProjection DFU collection contains DFU with material without id at index 0.",
                materialWithoutIdException.getMessage());

    }

    @Test
    public void activeDfuMethodsShouldRequireClusterProjectionWhenStatusLookupIsNeeded() {

        FiltroDFUProjection filtroDFUProjection = new FiltroDFUProjection(
                Set.of(new Location("LOC_01")),
                Set.of(new Produto("MAT_01")),
                null);

        Assertions.assertEquals(1, filtroDFUProjection.getNumeroDFUs());
        Assertions.assertTrue(filtroDFUProjection.contemCombinacaoLocationMaterial(
                new Location("LOC_01"),
                new Produto("MAT_01")));

        IllegalArgumentException getDfusException = Assertions.assertThrows(
                IllegalArgumentException.class,
                filtroDFUProjection::getDFUs);
        Assertions.assertEquals(
                "FiltroDFUProjection requires cluster/parameter projection to list active DFUs.",
                getDfusException.getMessage());

        IllegalArgumentException getDfusViaveisException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> filtroDFUProjection.getDFUsViaveis(null, null));
        Assertions.assertEquals(
                "FiltroDFUProjection requires cluster/parameter projection to list viable active DFUs.",
                getDfusViaveisException.getMessage());

    }

    @Test
    public void operationalMethodsShouldRejectNullDfuArguments() {

        FiltroDFUProjection filtroDFUProjection = new FiltroDFUProjection(
                false,
                new TestClusterEParametrosProjection());

        IllegalArgumentException addLocationException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> filtroDFUProjection.addDFU(null, new Produto("MAT_01")));
        Assertions.assertEquals(
                "FiltroDFUProjection cannot add DFU without location.",
                addLocationException.getMessage());

        IllegalArgumentException addMaterialException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> filtroDFUProjection.addDFU(new Location("LOC_01"), null));
        Assertions.assertEquals(
                "FiltroDFUProjection cannot add DFU without material.",
                addMaterialException.getMessage());

        IllegalArgumentException addLocationWithoutIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> filtroDFUProjection.addDFU(new Location(" "), new Produto("MAT_01")));
        Assertions.assertEquals(
                "FiltroDFUProjection cannot add DFU without location. Location id is required.",
                addLocationWithoutIdException.getMessage());

        IllegalArgumentException addMaterialWithoutIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> filtroDFUProjection.addDFU(new Location("LOC_01"), new Produto(" ")));
        Assertions.assertEquals(
                "FiltroDFUProjection cannot add DFU without material. Material id is required.",
                addMaterialWithoutIdException.getMessage());

        IllegalArgumentException filterException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> filtroDFUProjection.aplicaFiltroDFUs(null));
        Assertions.assertEquals(
                "FiltroDFUProjection DFU filter collection is required.",
                filterException.getMessage());

    }

    @Test
    public void viableDfusShouldRejectNullItemsInsideOptionalFilters() {

        TestClusterEParametrosProjection clusterEParametrosProjection = new TestClusterEParametrosProjection();
        FiltroDFUProjection filtroDFUProjection = new FiltroDFUProjection(
                Set.of(new Location("LOC_01")),
                Set.of(new Produto("MAT_01")),
                clusterEParametrosProjection);

        Set<Produto> materiaisComItemNulo = new HashSet<>();
        materiaisComItemNulo.add(null);
        IllegalArgumentException materialFilterException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> filtroDFUProjection.getDFUsViaveis(materiaisComItemNulo, null));
        Assertions.assertEquals(
                "FiltroDFUProjection material filter contains null material at index 0.",
                materialFilterException.getMessage());

        Set<Produto> materiaisSemId = new HashSet<>();
        materiaisSemId.add(new Produto(" "));
        IllegalArgumentException materialWithoutIdFilterException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> filtroDFUProjection.getDFUsViaveis(materiaisSemId, null));
        Assertions.assertEquals(
                "FiltroDFUProjection material filter contains material without id at index 0.",
                materialWithoutIdFilterException.getMessage());

        Set<Location> locationsComItemNulo = new HashSet<>();
        locationsComItemNulo.add(null);
        IllegalArgumentException locationFilterException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> filtroDFUProjection.getDFUsViaveis(null, locationsComItemNulo));
        Assertions.assertEquals(
                "FiltroDFUProjection location filter contains null location at index 0.",
                locationFilterException.getMessage());

        Set<Location> locationsSemId = new HashSet<>();
        locationsSemId.add(new Location(" "));
        IllegalArgumentException locationWithoutIdFilterException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> filtroDFUProjection.getDFUsViaveis(null, locationsSemId));
        Assertions.assertEquals(
                "FiltroDFUProjection location filter contains location without id at index 0.",
                locationWithoutIdFilterException.getMessage());

    }

    /**
     * Projection minima para testes de contrato do filtro.
     *
     * <p>Ela evita popular todo o snapshot de clusters/parametros, mas preserva
     * a semantica necessaria: toda DFU material/location informada no teste e
     * considerada ativa.</p>
     */
    private static class TestClusterEParametrosProjection extends ClusterEParametrosProjection {

        @Override
        public boolean isDfuAtiva(Produto material, Location location) {

            return material != null && location != null;

        }

        @Override
        public Set<Produto> getMateriaisAtivosEmLocation(Location location) {

            return Set.of(new Produto("MAT_01"));

        }

        @Override
        public Set<Location> getLocationsAtivas() {

            return Set.of(new Location("LOC_01"));

        }

    }

}
