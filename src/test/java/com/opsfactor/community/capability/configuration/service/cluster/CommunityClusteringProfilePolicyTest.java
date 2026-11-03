package com.opsfactor.community.capability.configuration.service.cluster;

import com.opsfactor.community.capability.configuration.domain.cluster.PerfilClusterizacao;
import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterMateriais;
import com.opsfactor.community.capability.cluster.facade.ClusteringFacade;
import com.opsfactor.community.capability.cluster.facade.dto.ClusterRuleDTO;
import com.opsfactor.community.capability.cluster.repository.location.ClusterLocationsRepository;
import com.opsfactor.community.capability.cluster.repository.material.ClusterProdutosRepository;
import com.opsfactor.community.capability.configuration.facade.PerfilExecucaoDemandPlanFacade;
import com.opsfactor.community.capability.configuration.repository.cluster.PerfilClusterizacaoRepository;
import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.capability.demandplanning.configuration.facade.dto.PerfilExecucaoDemandPlanDTO;
import com.opsfactor.community.capability.demandplanning.configuration.projection.factory.ParametrosDemandPlanningProjectionFactory;
import com.opsfactor.community.capability.demandplanning.configuration.repository.PerfilExecucaoDemandPlanRepository;
import com.opsfactor.community.capability.masterdata.calendar.profile.domain.PerfilCalendarioSimples;
import com.opsfactor.community.capability.masterdata.calendar.profile.repository.PerfilCalendarioSimplesRepository;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Fixed Community definition, blank wire inputs and the protected Enterprise extension boundary. */
class CommunityClusteringProfilePolicyTest {

    @Test
    void absentWireInputResolvesDefaultAndOtherProfileIsRejectedBeforeDatabaseAccess() {

        EntityManager entityManager = mock(EntityManager.class);
        var service = new PerfilClusterizacaoService(entityManager);
        assertEquals(PerfilClusterizacao.ID_LEGADO, service.resolveProfileId(null));
        assertEquals(PerfilClusterizacao.ID_LEGADO, service.resolveProfileId(""));
        assertEquals(PerfilClusterizacao.ID_LEGADO, service.resolveProfileId("   "));
        assertEquals(PerfilClusterizacao.ID_LEGADO, service.resolveProfileId(PerfilClusterizacao.ID_LEGADO));
        assertThrows(RequiresEnterpriseVersionException.class, () -> service.resolveProfileId("ANOTHER"));
        verifyNoInteractions(entityManager);

    }

    @Test
    void versionedProfileAdministrationIsNotAvailableInCommunity() {

        EntityManager entityManager = mock(EntityManager.class);
        var service = new PerfilClusterizacaoService(entityManager);
        assertThrows(RequiresEnterpriseVersionException.class, service::list);
        assertThrows(RequiresEnterpriseVersionException.class, () -> service.save(new PerfilClusterizacaoService.SaveRequest("A", "A", null)));
        assertThrows(RequiresEnterpriseVersionException.class, () -> service.createSingle(new PerfilClusterizacaoService.SaveRequest("A", "A", null)));
        assertThrows(RequiresEnterpriseVersionException.class, () -> service.cloneProfile(new PerfilClusterizacaoService.CloneRequest("LEGADO", "A", "A")));
        assertThrows(RequiresEnterpriseVersionException.class, () -> service.delete("LEGADO", 0));
        verifyNoInteractions(entityManager);

    }

    @Test
    void defaultDefinitionRemainsOperationallyEditableWithoutScanningVersionConsumers() {

        EntityManager entityManager = mock(EntityManager.class);
        PerfilClusterizacao profile = new PerfilClusterizacao(PerfilClusterizacao.ID_LEGADO, "Default");
        when(entityManager.find(PerfilClusterizacao.class, PerfilClusterizacao.ID_LEGADO, LockModeType.PESSIMISTIC_WRITE)).thenReturn(profile);
        new PerfilClusterizacaoService(entityManager).requireEditable("");
        verify(entityManager).find(PerfilClusterizacao.class, PerfilClusterizacao.ID_LEGADO, LockModeType.PESSIMISTIC_WRITE);
        verifyNoMoreInteractions(entityManager);

    }

    @Test
    void blankDeletePayloadUsesDefaultIdentityForBothAxesAndProtectsDefaultClusters() {

        EntityManager entityManager = mock(EntityManager.class);
        var profile = new PerfilClusterizacao(PerfilClusterizacao.ID_LEGADO, "Default");
        when(entityManager.find(PerfilClusterizacao.class, PerfilClusterizacao.ID_LEGADO, LockModeType.PESSIMISTIC_WRITE)).thenReturn(profile);
        var service = new ClusteringFacade();
        ReflectionTestUtils.setField(service, "clusteringProfileService", new PerfilClusterizacaoService(entityManager));
        var materialRepository = mock(ClusterProdutosRepository.class);
        var locationRepository = mock(ClusterLocationsRepository.class);
        ReflectionTestUtils.setField(service, "clusterProdutosRepository", materialRepository);
        ReflectionTestUtils.setField(service, "clusterLocationsRepository", locationRepository);
        var material = new ClusterMateriais("M", false, 1); material.setId(7L); material.setPerfilClusterizacao(profile);
        var location = new ClusterLocations("L", false, 1); location.setId(7L); location.setPerfilClusterizacao(profile);
        when(materialRepository.findByPerfilClusterizacaoIdAndId(PerfilClusterizacao.ID_LEGADO, 7L)).thenReturn(Optional.of(material));
        when(locationRepository.findByPerfilClusterizacaoIdAndId(PerfilClusterizacao.ID_LEGADO, 7L)).thenReturn(Optional.of(location));
        var materialRequest = new ClusterRuleDTO(); materialRequest.setId(7L); materialRequest.setClusteringProfileId("");
        var locationRequest = new ClusterRuleDTO(); locationRequest.setId(7L); locationRequest.setClusteringProfileId("");
        service.deleteClusterProdutos(materialRequest);
        service.deleteClusterLocations(locationRequest);
        verify(materialRepository).delete(material);
        verify(locationRepository).delete(location);
        material.setPadrao(true);
        assertThrows(IllegalArgumentException.class, () -> service.deleteClusterProdutos(materialRequest));
        verify(materialRepository, times(1)).delete(material);

    }

    @Test
    void executionSaveBindsManagedDefaultAndExposesNoClusteringSelectorField() {

        var service = new PerfilExecucaoDemandPlanFacade();
        var executionRepository = mock(PerfilExecucaoDemandPlanRepository.class);
        var profileRepository = mock(PerfilClusterizacaoRepository.class);
        var calendarRepository = mock(PerfilCalendarioSimplesRepository.class);
        var managedDefault = new PerfilClusterizacao(PerfilClusterizacao.ID_LEGADO, "Default");
        var existing = new PerfilExecucaoDemandPlan("EXECUTION");
        existing.setPerfilClusterizacao(new PerfilClusterizacao("STALE", "Imported private setup"));
        when(executionRepository.findById("EXECUTION")).thenReturn(Optional.of(existing));
        when(executionRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(profileRepository.getReferenceById(PerfilClusterizacao.ID_LEGADO)).thenReturn(managedDefault);
        when(calendarRepository.findById("MONTHLY")).thenReturn(Optional.of(new PerfilCalendarioSimples("MONTHLY", Constantes.TamanhoBucket.MENSAL, 12)));
        ReflectionTestUtils.setField(service, "perfilExecucaoDemandPlanRepository", executionRepository);
        ReflectionTestUtils.setField(service, "perfilClusterizacaoRepository", profileRepository);
        ReflectionTestUtils.setField(service, "perfilCalendarioSimplesRepository", calendarRepository);
        var dto = new PerfilExecucaoDemandPlanDTO(); dto.id = "EXECUTION"; dto.calendarProfileId = "MONTHLY";
        service.savePerfilExecucaoDemandPlanDTO(dto);
        assertSame(managedDefault, existing.getPerfilClusterizacao());
        verify(profileRepository).getReferenceById(PerfilClusterizacao.ID_LEGADO);
        assertThrows(NoSuchFieldException.class, () -> PerfilExecucaoDemandPlanDTO.class.getField("clusteringProfileId"));

    }

    @Test
    void operationalProjectionAlwaysUsesTheCommunityDefaultDefinition() {

        class VisibleFactory extends ParametrosDemandPlanningProjectionFactory {
            String effective(PerfilExecucaoDemandPlan profile) { return getPerfilClusterizacaoIdEfetivo(profile); }
        }
        var execution = new PerfilExecucaoDemandPlan("EXECUTION");
        execution.setPerfilClusterizacao(new PerfilClusterizacao("STALE", "Imported private setup"));
        assertEquals(PerfilClusterizacao.ID_LEGADO, new VisibleFactory().effective(execution));

    }
}
