package com.opsfactor.community.capability.lowlevelcode.engine;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.DFU;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.lowlevelcode.facade.dto.DFUMalhaCircularDTO;
import com.opsfactor.community.platform.exception.CircularNetworkException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * Contratos pequenos da rotina de low level code Community.
 *
 * <p>O low level code começa em 1 porque esse nivel representa demanda em
 * cliente final/regiao comercial. Qualquer chamada com nivel menor indica erro
 * de fluxo antes de consultar malha, materiais ou locations.</p>
 */
class LowLevelCodeCommunityContractTest {

    @Test
    void getDFUsNoLowLevelCodeShouldRejectNonPositiveLevelBeforeProjectionAccess() {

        LowLevelCode lowLevelCode = new LowLevelCode(
                null,
                null,
                null,
                null,
                null);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> lowLevelCode.getDFUsNoLowLevelCode(0));

        Assertions.assertTrue(illegalArgumentException.getMessage().contains(
                "LowLevelCode can return DFUs only for levels greater than or equal to 1"));
        Assertions.assertTrue(illegalArgumentException.getMessage().contains("received 0"));

    }

    @Test
    void circularDetailShouldReturnEveryTransportationEdgeForThreeNodeCycle() {

        SupplyNetworkProjection supplyNetworkProjection = Mockito.mock(SupplyNetworkProjection.class);
        VersaoMalha supplyNetworkVersion = Mockito.mock(VersaoMalha.class);
        MaterialProjection materialProjection = Mockito.mock(MaterialProjection.class);
        LocationProjection locationProjection = Mockito.mock(LocationProjection.class);
        Location locationA = Mockito.mock(Location.class);
        Location locationB = Mockito.mock(Location.class);
        Location locationC = Mockito.mock(Location.class);
        Produto material = Mockito.mock(Produto.class);
        Mockito.when(locationA.getId()).thenReturn("A");
        Mockito.when(locationB.getId()).thenReturn("B");
        Mockito.when(locationC.getId()).thenReturn("C");
        Mockito.when(material.getId()).thenReturn("M");
        Mockito.when(locationProjection.getLocationsAtivasOuNuloSeLocationProjectionCompleto()).thenReturn(null);
        Mockito.when(materialProjection.getMateriaisAtivosOuNuloSeMaterialProjectionCompleto()).thenReturn(null);

        LinhaTransporte lineAB = getTransportationLine(locationA, locationB);
        LinhaTransporte lineBC = getTransportationLine(locationB, locationC);
        LinhaTransporte lineCA = getTransportationLine(locationC, locationA);
        Mockito.when(supplyNetworkProjection.getLinhaTransportePrioritariaSetOutbound(
                Mockito.eq(supplyNetworkVersion), Mockito.eq(locationA), Mockito.eq(material), Mockito.any(), Mockito.isNull()))
                .thenReturn(Set.of(lineAB));
        Mockito.when(supplyNetworkProjection.getLinhaTransportePrioritariaSetOutbound(
                Mockito.eq(supplyNetworkVersion), Mockito.eq(locationB), Mockito.eq(material), Mockito.any(), Mockito.isNull()))
                .thenReturn(Set.of(lineBC));
        Mockito.when(supplyNetworkProjection.getLinhaTransportePrioritariaSetOutbound(
                Mockito.eq(supplyNetworkVersion), Mockito.eq(locationC), Mockito.eq(material), Mockito.any(), Mockito.isNull()))
                .thenReturn(Set.of(lineCA));
        Mockito.when(supplyNetworkProjection.getListaTecnicaViavelPrioritariaSetOndeMaterialEInput(
                Mockito.any(), Mockito.any(), Mockito.eq(false), Mockito.isNull())).thenReturn(Set.of());

        Set<DFU> remainingDfus = Set.of(
                new DFU(material, locationA),
                new DFU(material, locationB),
                new DFU(material, locationC));
        LowLevelCode lowLevelCode = new LowLevelCode(
                supplyNetworkProjection,
                supplyNetworkVersion,
                materialProjection,
                locationProjection,
                LocalDateTime.of(2026, 1, 1, 0, 0));

        lowLevelCode.atualizaDetalheErroCircularidade(new CircularNetworkException(
                "cycle", 3, Map.of(), remainingDfus));

        Set<DFUMalhaCircularDTO> details = lowLevelCode.getDetalheErroCircularidade();
        Assertions.assertEquals(3, details.size());
        Assertions.assertEquals(Set.of("A-B", "B-C", "C-A"), details.stream()
                .map(detail -> detail.masterDataId)
                .collect(java.util.stream.Collectors.toSet()));
        Assertions.assertEquals(Set.of(1), details.stream()
                .map(detail -> detail.circularNetworkId)
                .collect(java.util.stream.Collectors.toSet()));
        Assertions.assertTrue(details.stream().allMatch(detail -> "Transportation Line".equals(detail.masterData)));
        Assertions.assertTrue(details.stream().allMatch(detail -> "M".equals(detail.materialId)));

    }

    @Test
    void circularDetailShouldReturnEveryBillOfMaterialsEdgeForThreeNodeCycle() {

        SupplyNetworkProjection supplyNetworkProjection = Mockito.mock(SupplyNetworkProjection.class);
        VersaoMalha supplyNetworkVersion = Mockito.mock(VersaoMalha.class);
        MaterialProjection materialProjection = Mockito.mock(MaterialProjection.class);
        LocationProjection locationProjection = Mockito.mock(LocationProjection.class);
        Location location = Mockito.mock(Location.class);
        Produto materialA = getMaterial("A");
        Produto materialB = getMaterial("B");
        Produto materialC = getMaterial("C");
        Mockito.when(location.getId()).thenReturn("PLANT");
        Mockito.when(locationProjection.getLocationsAtivasOuNuloSeLocationProjectionCompleto()).thenReturn(null);
        Mockito.when(materialProjection.getMateriaisAtivosOuNuloSeMaterialProjectionCompleto()).thenReturn(null);
        ListaTecnica billOfMaterialsAB = getBillOfMaterials("BOM-A-B", materialB);
        ListaTecnica billOfMaterialsBC = getBillOfMaterials("BOM-B-C", materialC);
        ListaTecnica billOfMaterialsCA = getBillOfMaterials("BOM-C-A", materialA);
        Mockito.when(supplyNetworkProjection.getLinhaTransportePrioritariaSetOutbound(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.isNull())).thenReturn(Set.of());
        Mockito.when(supplyNetworkProjection.getListaTecnicaViavelPrioritariaSetOndeMaterialEInput(
                Mockito.eq(location), Mockito.eq(materialA), Mockito.eq(false), Mockito.isNull()))
                .thenReturn(Set.of(billOfMaterialsAB));
        Mockito.when(supplyNetworkProjection.getListaTecnicaViavelPrioritariaSetOndeMaterialEInput(
                Mockito.eq(location), Mockito.eq(materialB), Mockito.eq(false), Mockito.isNull()))
                .thenReturn(Set.of(billOfMaterialsBC));
        Mockito.when(supplyNetworkProjection.getListaTecnicaViavelPrioritariaSetOndeMaterialEInput(
                Mockito.eq(location), Mockito.eq(materialC), Mockito.eq(false), Mockito.isNull()))
                .thenReturn(Set.of(billOfMaterialsCA));

        LowLevelCode lowLevelCode = new LowLevelCode(
                supplyNetworkProjection,
                supplyNetworkVersion,
                materialProjection,
                locationProjection,
                LocalDateTime.of(2026, 1, 1, 0, 0));
        lowLevelCode.atualizaDetalheErroCircularidade(new CircularNetworkException(
                "cycle", 4, Map.of(), Set.of(
                new DFU(materialA, location),
                new DFU(materialB, location),
                new DFU(materialC, location))));

        Set<DFUMalhaCircularDTO> details = lowLevelCode.getDetalheErroCircularidade();
        Assertions.assertEquals(3, details.size());
        Assertions.assertEquals(Set.of("BOM-A-B", "BOM-B-C", "BOM-C-A"), details.stream()
                .map(detail -> detail.masterDataId)
                .collect(java.util.stream.Collectors.toSet()));
        Assertions.assertEquals(Set.of(1), details.stream()
                .map(detail -> detail.circularNetworkId)
                .collect(java.util.stream.Collectors.toSet()));
        Assertions.assertTrue(details.stream().allMatch(detail -> "Bill of Materials".equals(detail.masterData)));
        Assertions.assertEquals(Set.of("A-B", "B-C", "C-A"), details.stream()
                .map(detail -> detail.materialId + "-" + detail.outputMaterialId)
                .collect(java.util.stream.Collectors.toSet()));

    }

    private LinhaTransporte getTransportationLine(Location origin, Location destination) {

        LinhaTransporte transportationLine = Mockito.mock(LinhaTransporte.class);
        Mockito.when(transportationLine.getLocationOrigem()).thenReturn(origin);
        Mockito.when(transportationLine.getLocationDestino()).thenReturn(destination);
        return transportationLine;

    }

    private Produto getMaterial(String id) {

        Produto material = Mockito.mock(Produto.class);
        Mockito.when(material.getId()).thenReturn(id);
        return material;

    }

    private ListaTecnica getBillOfMaterials(String id, Produto outputMaterial) {

        ListaTecnica billOfMaterials = Mockito.mock(ListaTecnica.class);
        Mockito.when(billOfMaterials.getId()).thenReturn(id);
        Mockito.when(billOfMaterials.getMaterialOutput()).thenReturn(outputMaterial);
        return billOfMaterials;

    }

}
