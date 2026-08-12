package com.opsfactor.community.capability.masterdata.classification.characteristic.facade.mapper;

import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaLocation;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProduto;
import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto.FiltroMaterialLocationDeCombinacaoCaracteristicasDTO;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

class FiltroMaterialLocationDeCombinacaoCaracteristicasMapperTest {

    @Test
    void shouldIntersectExplicitIdsAndCharacteristicsUsingLegacyAndOrSemantics() {

        ClusterEParametrosProjection clusterProjection =
                Mockito.mock(ClusterEParametrosProjection.class);
        Produto matchingMaterial = Mockito.mock(Produto.class);
        Produto excludedMaterial = Mockito.mock(Produto.class);
        Location matchingLocation = Mockito.mock(Location.class);
        Location excludedLocation = Mockito.mock(Location.class);
        CaracteristicaProduto familyCharacteristic = Mockito.mock(CaracteristicaProduto.class);
        CaracteristicaProduto colorCharacteristic = Mockito.mock(CaracteristicaProduto.class);
        CaracteristicaLocation regionCharacteristic = Mockito.mock(CaracteristicaLocation.class);

        Mockito.when(clusterProjection.getMateriais(true))
                .thenReturn(Set.of(matchingMaterial, excludedMaterial));
        Mockito.when(clusterProjection.getLocations(true))
                .thenReturn(Set.of(matchingLocation, excludedLocation));
        Mockito.when(clusterProjection.getMaterialPersistido("MAT-1")).thenReturn(matchingMaterial);
        Mockito.when(clusterProjection.getLocationPersistida("LOC-1")).thenReturn(matchingLocation);
        Mockito.when(clusterProjection.getCaracteristicaProdutoDeId("family"))
                .thenReturn(familyCharacteristic);
        Mockito.when(clusterProjection.getCaracteristicaProdutoDeId("color"))
                .thenReturn(colorCharacteristic);
        Mockito.when(clusterProjection.getCaracteristicaLocationDeId("region"))
                .thenReturn(regionCharacteristic);
        Mockito.when(familyCharacteristic.findValorCaracteristicaDeProduto(matchingMaterial))
                .thenReturn(Optional.of("BEVERAGE"));
        Mockito.when(colorCharacteristic.findValorCaracteristicaDeProduto(matchingMaterial))
                .thenReturn(Optional.of("Blue"));
        Mockito.when(regionCharacteristic.findValorCaracteristicaDeLocation(matchingLocation))
                .thenReturn(Optional.of("SOUTH"));

        FiltroMaterialLocationDeCombinacaoCaracteristicasDTO filterDTO =
                new FiltroMaterialLocationDeCombinacaoCaracteristicasDTO();
        filterDTO.materialIds = List.of("MAT-1");
        filterDTO.locationIds = List.of("LOC-1");
        filterDTO.valuesByMaterialCharacteristicId = Map.of(
                "family", List.of("food", "beverage"),
                "color", List.of("blue"));
        filterDTO.valuesByLocationCharacteristicId = Map.of(
                "region", List.of("south", "southeast"));

        MaterialProjection materialProjection =
                FiltroMaterialDeCombinacaoCaracteristicasMapper.getMaterialProjection(
                        filterDTO,
                        clusterProjection,
                        true);
        LocationProjection locationProjection =
                FiltroLocationDeCombinacaoCaracteristicasMapper.getLocationProjection(
                        filterDTO,
                        clusterProjection,
                        true);

        Assertions.assertEquals(Set.of(matchingMaterial), materialProjection.getMaterialSet());
        Assertions.assertEquals(Set.of(matchingLocation), locationProjection.getLocationSet());

    }

    @Test
    void shouldTreatEmptySelectionAsCompleteActiveScope() {

        ClusterEParametrosProjection clusterProjection =
                Mockito.mock(ClusterEParametrosProjection.class);
        Produto material = Mockito.mock(Produto.class);
        Location location = Mockito.mock(Location.class);
        Mockito.when(clusterProjection.getMateriais(true)).thenReturn(Set.of(material));
        Mockito.when(clusterProjection.getLocations(true)).thenReturn(Set.of(location));

        MaterialProjection materialProjection =
                FiltroMaterialDeCombinacaoCaracteristicasMapper.getMaterialProjection(
                        new FiltroMaterialLocationDeCombinacaoCaracteristicasDTO(),
                        clusterProjection,
                        true);
        LocationProjection locationProjection =
                FiltroLocationDeCombinacaoCaracteristicasMapper.getLocationProjection(
                        new FiltroMaterialLocationDeCombinacaoCaracteristicasDTO(),
                        clusterProjection,
                        true);

        Assertions.assertEquals(Set.of(material), materialProjection.getMaterialSet());
        Assertions.assertEquals(Set.of(location), locationProjection.getLocationSet());

    }

}
