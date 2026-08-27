package com.opsfactor.community.capability.masterdata.network.supplynetwork.facade;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.dto.*;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnicaComponente;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjectionFactory;
import org.javatuples.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.Set;

class SupplyNetworkDependencyExplorerServiceTest {

    @Test
    void shouldKeepSupplierWithoutProductionVersionAsViableSyntheticAlternative() {

        ExplorerFixture fixture = new ExplorerFixture(LocationAbstract.TipoLocation.FORNECEDOR);
        Mockito.when(fixture.projection.getTodasVersoesProducao(fixture.location, fixture.material, true, null))
                .thenReturn(Set.of());

        MaterialLocationDependencyDTO root = getSingleRoot(fixture.service.getDependencies("NETWORK", "LOCATION", "MATERIAL", 2));

        Assertions.assertTrue(root.viableProduction);
        ProductionVersionDependencyDTO supplierVersion = root.productionVersionDependencies.iterator().next();
        Assertions.assertEquals("Supplier with No Production Version - LOCATION - MATERIAL", supplierVersion.productionVersionId);
        Assertions.assertTrue(supplierVersion.viableStep);

    }

    @Test
    void shouldStopAtMaximumDepthBeforeLoadingFurtherAlternatives() {

        ExplorerFixture fixture = new ExplorerFixture(LocationAbstract.TipoLocation.INTERNA);

        MaterialLocationDependencyDTO root = getSingleRoot(fixture.service.getDependencies("NETWORK", "LOCATION", "MATERIAL", 0));

        Assertions.assertEquals(0, root.depth);
        Assertions.assertTrue(root.viableStep);
        Assertions.assertTrue(root.productionVersionDependencies.isEmpty());
        Assertions.assertTrue(root.inboundTransportationLineDependencies.isEmpty());
        Mockito.verify(fixture.projection, Mockito.never())
                .getTodasVersoesProducao(Mockito.any(), Mockito.any(), Mockito.eq(true), Mockito.isNull());

    }

    @Test
    void shouldExpandBomComponentsThroughProjectionOnly() {

        ExplorerFixture fixture = new ExplorerFixture(LocationAbstract.TipoLocation.INTERNA);
        VersaoProducao productionVersion = Mockito.mock(VersaoProducao.class);
        Roteiro routing = Mockito.mock(Roteiro.class);
        ListaTecnica bom = Mockito.mock(ListaTecnica.class);
        ListaTecnicaComponente component = Mockito.mock(ListaTecnicaComponente.class);
        Produto componentMaterial = Mockito.mock(Produto.class);
        Location componentLocation = Mockito.mock(Location.class);
        Mockito.when(productionVersion.getId()).thenReturn("PV");
        Mockito.when(productionVersion.getAtivo()).thenReturn(true);
        Mockito.when(productionVersion.getCombinacoesRoteiroListaTecnica()).thenReturn(List.of(Pair.with(routing, bom)));
        Mockito.when(routing.getId()).thenReturn("ROUTING");
        Mockito.when(routing.getAtivo()).thenReturn(true);
        Mockito.when(routing.getRecursoProdutivoSet()).thenReturn(Set.of());
        Mockito.when(bom.getId()).thenReturn("BOM");
        Mockito.when(bom.getAtivo()).thenReturn(true);
        Mockito.when(bom.getLocation()).thenReturn(componentLocation);
        Mockito.when(bom.getListaTecnicaComponenteSet()).thenReturn(Set.of(component));
        Mockito.when(component.getMaterial()).thenReturn(componentMaterial);
        Mockito.when(componentMaterial.getId()).thenReturn("COMPONENT");
        Mockito.when(componentLocation.getId()).thenReturn("COMPONENT-LOCATION");
        Mockito.when(fixture.projection.getTodasVersoesProducao(fixture.location, fixture.material, true, null))
                .thenReturn(Set.of(productionVersion));
        Mockito.when(fixture.projection.getRoteiroFromId("ROUTING")).thenReturn(Optional.of(routing));
        Mockito.when(fixture.projection.getListaTecnicaFromId("BOM")).thenReturn(Optional.of(bom));
        Mockito.when(fixture.parametersProjection.isDfuAtiva(componentMaterial, componentLocation)).thenReturn(false);

        MaterialLocationDependencyDTO root = getSingleRoot(fixture.service.getDependencies("NETWORK", "LOCATION", "MATERIAL", 1));

        BillOfMaterialsDependencyDTO bomDependency = root.productionVersionDependencies.iterator().next()
                .routingAndBomCombinationDependencies.iterator().next().bomDependency;
        MaterialLocationDependencyDTO componentDependency = bomDependency.bomComponentDependencies.iterator().next();
        Assertions.assertEquals("COMPONENT", componentDependency.materialId);
        Assertions.assertEquals("COMPONENT-LOCATION", componentDependency.locationId);
        Assertions.assertEquals(1, componentDependency.depth);
        Assertions.assertFalse(componentDependency.active);
        Mockito.verify(fixture.supplyNetworkProjectionFactory).getSupplyNetworkProjectionCompletoDeCache();
        Mockito.verifyNoMoreInteractions(fixture.supplyNetworkProjectionFactory);

    }

    @Test
    void shouldExpandApplicableInboundLineAndItsOriginMaterial() {

        ExplorerFixture fixture = new ExplorerFixture(LocationAbstract.TipoLocation.FORNECEDOR);
        LinhaTransporte transportationLine = Mockito.mock(LinhaTransporte.class);
        Location origin = Mockito.mock(Location.class);
        Mockito.when(transportationLine.getHabilitadoProdutosNaoCadastradosLinhaTransporte()).thenReturn(true);
        Mockito.when(transportationLine.getLocationOrigem()).thenReturn(origin);
        Mockito.when(origin.getId()).thenReturn("ORIGIN");
        Mockito.when(origin.getTipoLocation()).thenReturn(LocationAbstract.TipoLocation.FORNECEDOR);
        Mockito.when(fixture.projection.getTodasVersoesProducao(fixture.location, fixture.material, true, null))
                .thenReturn(Set.of());
        Mockito.when(fixture.projection.getLinhasTransporte(fixture.network, null, Set.of(fixture.location)))
                .thenReturn(Set.of(transportationLine));
        Mockito.when(fixture.projection.getLinhaTransporteInboundViavelSetParaLocationMaterial(
                Mockito.eq(fixture.network), Mockito.eq(fixture.location), Mockito.eq(fixture.material), Mockito.any(), Mockito.eq(Set.of(origin))))
                .thenReturn(Set.of(transportationLine));
        Mockito.when(fixture.parametersProjection.isDfuAtiva(fixture.material, origin)).thenReturn(true);

        MaterialLocationDependencyDTO root = getSingleRoot(fixture.service.getDependencies("NETWORK", "LOCATION", "MATERIAL", 1));

        TransportationLineDependencyDTO lineDependency = root.inboundTransportationLineDependencies.iterator().next();
        Assertions.assertTrue(lineDependency.active);
        Assertions.assertEquals("ORIGIN", lineDependency.originLocationId);
        Assertions.assertEquals(1, lineDependency.materialAtOriginLocationDependency.depth);

    }

    @Test
    void shouldOmitNonFocusedParallelRoutingsAndExposeTheirCount() {

        ExplorerFixture fixture = new ExplorerFixture(LocationAbstract.TipoLocation.INTERNA);
        VersaoProducao parallelVersion = Mockito.mock(VersaoProducao.class);
        Roteiro focusedRouting = Mockito.mock(Roteiro.class);
        ListaTecnica focusedBom = Mockito.mock(ListaTecnica.class);
        Roteiro otherRouting = Mockito.mock(Roteiro.class);
        ListaTecnica otherBom = Mockito.mock(ListaTecnica.class);
        Produto otherMaterial = Mockito.mock(Produto.class);
        Mockito.when(parallelVersion.getId()).thenReturn("PARALLEL");
        Mockito.when(parallelVersion.getAtivo()).thenReturn(true);
        Mockito.when(parallelVersion.getCombinacoesRoteiroListaTecnica()).thenReturn(List.of(
                Pair.with(focusedRouting, focusedBom), Pair.with(otherRouting, otherBom)));
        Mockito.when(focusedRouting.getMaterialOutput()).thenReturn(fixture.material);
        Mockito.when(focusedBom.getMaterialOutput()).thenReturn(fixture.material);
        Mockito.when(focusedRouting.getId()).thenReturn("FOCUSED-ROUTING");
        Mockito.when(focusedRouting.getAtivo()).thenReturn(true);
        Mockito.when(focusedBom.getId()).thenReturn("FOCUSED-BOM");
        Mockito.when(focusedBom.getAtivo()).thenReturn(true);
        Mockito.when(focusedBom.getListaTecnicaComponenteSet()).thenReturn(Set.of());
        Mockito.when(otherRouting.getMaterialOutput()).thenReturn(otherMaterial);
        Mockito.when(otherBom.getMaterialOutput()).thenReturn(otherMaterial);
        Mockito.when(fixture.projection.getTodasVersoesProducao(fixture.location, fixture.material, true, null))
                .thenReturn(Set.of(parallelVersion));
        Mockito.when(fixture.projection.getListaTecnicaFromId("FOCUSED-BOM")).thenReturn(Optional.of(focusedBom));

        MaterialLocationDependencyDTO root = getSingleRoot(fixture.service.getDependencies("NETWORK", "LOCATION", "MATERIAL", 1));

        ProductionVersionDependencyDTO versionDependency = root.productionVersionDependencies.iterator().next();
        Assertions.assertTrue(versionDependency.parallelRoutingsOmitted);
        Assertions.assertEquals(1, versionDependency.omittedParallelRoutingCount);
        Assertions.assertEquals(1, versionDependency.routingAndBomCombinationDependencies.size());

    }

    private static MaterialLocationDependencyDTO getSingleRoot(List<SupplyNetworkDependencyDTO> dependencies) {

        Assertions.assertEquals(1, dependencies.size());
        return (MaterialLocationDependencyDTO) dependencies.getFirst();

    }

    private static final class ExplorerFixture {

        private final SupplyNetworkProjectionFactory supplyNetworkProjectionFactory = Mockito.mock(SupplyNetworkProjectionFactory.class);
        private final SupplyNetworkProjection projection = Mockito.mock(SupplyNetworkProjection.class);
        private final ClusterEParametrosProjection parametersProjection = Mockito.mock(ClusterEParametrosProjection.class);
        private final VersaoMalha network = Mockito.mock(VersaoMalha.class);
        private final Location location = Mockito.mock(Location.class);
        private final Produto material = Mockito.mock(Produto.class);
        private final SupplyNetworkDependencyExplorerService service =
                new SupplyNetworkDependencyExplorerService(supplyNetworkProjectionFactory);

        private ExplorerFixture(LocationAbstract.TipoLocation locationType) {

            Mockito.when(supplyNetworkProjectionFactory.getSupplyNetworkProjectionCompletoDeCache()).thenReturn(projection);
            Mockito.when(projection.getVersaoMalhaDeId("NETWORK")).thenReturn(Optional.of(network));
            Mockito.when(projection.getClusterEParametrosProjection()).thenReturn(parametersProjection);
            Mockito.when(parametersProjection.getLocationPersistida("LOCATION")).thenReturn(location);
            Mockito.when(parametersProjection.getMaterialPersistido("MATERIAL")).thenReturn(material);
            Mockito.when(location.getId()).thenReturn("LOCATION");
            Mockito.when(location.getTipoLocation()).thenReturn(locationType);
            Mockito.when(material.getId()).thenReturn("MATERIAL");
            Mockito.when(projection.getLinhasTransporte(network, null, Set.of(location))).thenReturn(Set.of());

        }
    }
}
