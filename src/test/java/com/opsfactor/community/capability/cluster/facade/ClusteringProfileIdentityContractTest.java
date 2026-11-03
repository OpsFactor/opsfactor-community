package com.opsfactor.community.capability.cluster.facade;

import com.opsfactor.community.capability.cluster.facade.dto.*;
import com.opsfactor.community.capability.cluster.repository.material.ClusterMateriaisRepository;
import com.opsfactor.community.capability.cluster.repository.location.ClusterLocationsRepository;
import com.opsfactor.community.capability.configuration.service.cluster.PerfilClusterizacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClusteringProfileIdentityContractTest {

    @Test
    void rejectsMissingPositiveIdsBeforeCreatingEitherAxis() {

        var service = new ClusteringFacade();
        var profileService = mock(PerfilClusterizacaoService.class);
        when(profileService.resolveProfileId(any())).thenReturn("LEGADO");
        ReflectionTestUtils.setField(service, "clusteringProfileService", profileService);
        var materials = mock(ClusterMateriaisRepository.class);
        var locations = mock(ClusterLocationsRepository.class);
        ReflectionTestUtils.setField(service, "clusterMateriaisDemandPlanningRepository", materials);
        ReflectionTestUtils.setField(service, "clusterLocationsRepository", locations);
        when(materials.findByPerfilClusterizacaoIdAndId("LEGADO", 999L)).thenReturn(Optional.empty());
        when(locations.findByPerfilClusterizacaoIdAndId("LEGADO", 999L)).thenReturn(Optional.empty());
        var material = new ClusterProdutosDTO(); material.setClusteringProfileId(""); material.setId(999L);
        assertThatThrownBy(() -> service.saveClusterProdutosDTO(material)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("perfil informado");
        var location = new ClusterLocationsDTO(); location.setClusteringProfileId(""); location.setId(999L);
        assertThatThrownBy(() -> service.saveClusterLocationsDTO(location)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("perfil informado");
        verify(materials, never()).save(any()); verify(locations, never()).save(any());

    }
}
