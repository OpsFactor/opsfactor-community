package com.opsfactor.community.capability.configuration.web;

import com.opsfactor.community.capability.configuration.facade.ConfiguredViewFacade;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredViewKeyFigure;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.user.repository.ConfiguredViewKeyFigureRepository;
import com.opsfactor.community.capability.configuration.user.repository.ConfiguredViewRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewDTO;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewKeyFigureDTO;
import com.opsfactor.community.capability.configuration.facade.mapper.ConfiguredViewAutoMapper;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * Contratos da fotografia persistida de Key Figures no Planning Book
 * Community.
 */
class ConfiguredViewKeyFigureSelectionCommunityTest {

    @Test
    void saveShouldReplaceTheWholeKeyFigureSnapshotAndNormalizeOrderAndEdition() {

        ConfiguredView configuredView = configuredView();
        ConfiguredViewRepository configuredViewRepository = Mockito.mock(ConfiguredViewRepository.class);
        ConfiguredViewKeyFigureRepository configuredViewKeyFigureRepository =
                Mockito.mock(ConfiguredViewKeyFigureRepository.class);

        Mockito.when(configuredViewRepository
                        .findByConfiguredViewCompositeKeyUserIdAndConfiguredViewCompositeKeyNomeViewAndConfiguredViewCompositeKeyTipoView(
                                "DEBUG", "Planning Book", ConfiguredView.TipoView.DEMANDPLANNINGBOOK))
                .thenReturn(Optional.of(configuredView));
        Mockito.when(configuredViewRepository.save(Mockito.any(ConfiguredView.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(configuredViewKeyFigureRepository.findAllByConfiguredViewIn(Mockito.anyCollection()))
                .thenReturn(List.of(configuredViewKeyFigure(configuredView, "Historical Sales")));

        ConfiguredViewFacade service = configuredViewFrontService(
                configuredViewRepository,
                configuredViewKeyFigureRepository);
        ConfiguredViewDTO configuredViewDTO = configuredViewDTO();
        configuredViewDTO.keyFigureList = List.of(
                keyFigure("Demand Adjustment", false, 98),
                keyFigure("Baseline", null, -3));

        service.saveConfiguredViewDTO(configuredViewDTO, "DEBUG", false);

        InOrder inOrder = Mockito.inOrder(configuredViewKeyFigureRepository);
        inOrder.verify(configuredViewKeyFigureRepository)
                .deleteAllByConfiguredView(configuredView);
        ArgumentCaptor<Iterable<ConfiguredViewKeyFigure>> keyFiguresCaptor = iterableCaptor();
        inOrder.verify(configuredViewKeyFigureRepository).saveAll(keyFiguresCaptor.capture());

        List<ConfiguredViewKeyFigure> savedKeyFigures = StreamSupport
                .stream(keyFiguresCaptor.getValue().spliterator(), false)
                .toList();
        Assertions.assertEquals(List.of("Demand Adjustment", "Baseline"), savedKeyFigures.stream()
                .map(ConfiguredViewKeyFigure::getKeyFigureId)
                .toList());
        Assertions.assertEquals(List.of(1, 2), savedKeyFigures.stream()
                .map(ConfiguredViewKeyFigure::getPosition)
                .toList());
        Assertions.assertEquals(List.of(false, true), savedKeyFigures.stream()
                .map(ConfiguredViewKeyFigure::getAllowChanges)
                .toList());
        Mockito.verify(configuredViewKeyFigureRepository, Mockito.never())
                .findAllByConfiguredViewIn(Mockito.anyCollection());
    }

    @Test
    void removeShouldBulkDeleteKeyFigureChildrenBeforeDeletingView() {

        ConfiguredView configuredView = configuredView();
        ConfiguredViewRepository configuredViewRepository = Mockito.mock(ConfiguredViewRepository.class);
        ConfiguredViewKeyFigureRepository configuredViewKeyFigureRepository =
                Mockito.mock(ConfiguredViewKeyFigureRepository.class);
        Mockito.when(configuredViewRepository
                        .findByConfiguredViewCompositeKeyUserIdAndConfiguredViewCompositeKeyNomeViewAndConfiguredViewCompositeKeyTipoView(
                                "DEBUG", "Planning Book", ConfiguredView.TipoView.DEMANDPLANNINGBOOK))
                .thenReturn(Optional.of(configuredView));

        ConfiguredViewFacade service = configuredViewFrontService(
                configuredViewRepository,
                configuredViewKeyFigureRepository);
        service.removeConfiguredView(configuredViewDTO());

        InOrder inOrder = Mockito.inOrder(configuredViewKeyFigureRepository, configuredViewRepository);
        inOrder.verify(configuredViewKeyFigureRepository).deleteAllByConfiguredView(configuredView);
        inOrder.verify(configuredViewRepository)
                .removeByConfiguredViewCompositeKeyUserIdAndConfiguredViewCompositeKeyNomeViewAndConfiguredViewCompositeKeyTipoView(
                        "DEBUG", "Planning Book", ConfiguredView.TipoView.DEMANDPLANNINGBOOK);
    }

    @Test
    void saveShouldRejectEnterpriseKeyFigureBeforeRepositoryAccess() {

        ConfiguredViewFacade service = new ConfiguredViewFacade();
        ConfiguredViewDTO configuredViewDTO = configuredViewDTO();
        configuredViewDTO.keyFigureList = List.of(keyFigure("Uplift", false, 1));

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> service.saveConfiguredViewDTO(configuredViewDTO, "DEBUG", false));
    }

    @Test
    void saveShouldRejectMonetarySalesKeyFiguresBeforeRepositoryAccess() {

        ConfiguredViewFacade service = new ConfiguredViewFacade();

        for (String keyFigureId : List.of("Gross Sales", "Net Sales")) {
            ConfiguredViewDTO configuredViewDTO = configuredViewDTO();
            configuredViewDTO.keyFigureList = List.of(keyFigure(keyFigureId, false, 1));

            Assertions.assertThrows(
                    RequiresEnterpriseVersionException.class,
                    () -> service.saveConfiguredViewDTO(configuredViewDTO, "DEBUG", false));
        }

    }

    @Test
    void listingShouldLoadTheGlobalParametersSnapshotOnlyOnceForSeveralViews() {

        ConfiguredView firstConfiguredView = configuredView();
        ConfiguredView secondConfiguredView = new ConfiguredView(new ConfiguredView.ConfiguredViewCompositeKey(
                "DEBUG", "Planning Book 2", ConfiguredView.TipoView.DEMANDPLANNINGBOOK));
        ConfiguredViewRepository configuredViewRepository = Mockito.mock(ConfiguredViewRepository.class);
        ConfiguredViewKeyFigureRepository configuredViewKeyFigureRepository =
                Mockito.mock(ConfiguredViewKeyFigureRepository.class);
        ConfiguredViewAutoMapper configuredViewAutoMapper = Mockito.mock(ConfiguredViewAutoMapper.class);
        ParametrosGlobaisService parametrosGlobaisService = Mockito.mock(ParametrosGlobaisService.class);

        Mockito.when(configuredViewRepository
                        .findByConfiguredViewCompositeKeyUserIdAndConfiguredViewCompositeKeyTipoView(
                                "DEBUG", ConfiguredView.TipoView.DEMANDPLANNINGBOOK))
                .thenReturn(List.of(firstConfiguredView, secondConfiguredView));
        Mockito.when(configuredViewKeyFigureRepository.findAllByConfiguredViewIn(Mockito.anyCollection()))
                .thenReturn(List.of());
        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        Mockito.when(parametrosGlobaisService.getParametrosGlobais()).thenReturn(parametrosGlobais);
        Mockito.when(configuredViewAutoMapper.converteComKeyFigures(
                        Mockito.any(ConfiguredView.class),
                        Mockito.same(parametrosGlobais),
                        Mockito.anyList()))
                .thenAnswer(invocation -> configuredViewDTOForListing(invocation.getArgument(0)));

        ConfiguredViewFacade service = configuredViewFrontService(
                configuredViewRepository,
                configuredViewKeyFigureRepository);
        ReflectionTestUtils.setField(service, "configuredViewAutoMapper", configuredViewAutoMapper);
        ReflectionTestUtils.setField(service, "parametrosGlobaisService", parametrosGlobaisService);

        Assertions.assertEquals(2, service.getConfiguredViewDTOListDemandPlanningBook("DEBUG").size());
        Mockito.verify(parametrosGlobaisService, Mockito.times(1)).getParametrosGlobais();
        Mockito.verify(configuredViewKeyFigureRepository, Mockito.times(1))
                .findAllByConfiguredViewIn(Mockito.anyCollection());
    }

    @Test
    void supplyListingShouldLoadTheKeyFigureSnapshotInOneBatchForSeveralViews() {

        ConfiguredView firstConfiguredView = new ConfiguredView(new ConfiguredView.ConfiguredViewCompositeKey(
                "DEBUG", "Supply Planning Book", ConfiguredView.TipoView.SUPPLYPLANNINGBOOK));
        ConfiguredView secondConfiguredView = new ConfiguredView(new ConfiguredView.ConfiguredViewCompositeKey(
                "DEBUG", "Supply Planning Book 2", ConfiguredView.TipoView.SUPPLYPLANNINGBOOK));
        ConfiguredViewRepository configuredViewRepository = Mockito.mock(ConfiguredViewRepository.class);
        ConfiguredViewKeyFigureRepository configuredViewKeyFigureRepository =
                Mockito.mock(ConfiguredViewKeyFigureRepository.class);
        ConfiguredViewAutoMapper configuredViewAutoMapper = Mockito.mock(ConfiguredViewAutoMapper.class);
        ParametrosGlobaisService parametrosGlobaisService = Mockito.mock(ParametrosGlobaisService.class);

        Mockito.when(configuredViewRepository
                        .findByConfiguredViewCompositeKeyUserIdAndConfiguredViewCompositeKeyTipoView(
                                "DEBUG", ConfiguredView.TipoView.SUPPLYPLANNINGBOOK))
                .thenReturn(List.of(firstConfiguredView, secondConfiguredView));
        Mockito.when(configuredViewKeyFigureRepository.findAllByConfiguredViewIn(Mockito.anyCollection()))
                .thenReturn(List.of());
        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        Mockito.when(parametrosGlobaisService.getParametrosGlobais()).thenReturn(parametrosGlobais);
        Mockito.when(configuredViewAutoMapper.converteComKeyFigures(
                        Mockito.any(ConfiguredView.class),
                        Mockito.same(parametrosGlobais),
                        Mockito.anyList()))
                .thenAnswer(invocation -> configuredViewDTOForListing(invocation.getArgument(0)));

        ConfiguredViewFacade service = configuredViewFrontService(
                configuredViewRepository,
                configuredViewKeyFigureRepository);
        ReflectionTestUtils.setField(service, "configuredViewAutoMapper", configuredViewAutoMapper);
        ReflectionTestUtils.setField(service, "parametrosGlobaisService", parametrosGlobaisService);

        Assertions.assertEquals(2, service.getConfiguredViewDTOListSupplyPlanningBook("DEBUG").size());
        Mockito.verify(configuredViewKeyFigureRepository, Mockito.times(1))
                .findAllByConfiguredViewIn(Mockito.anyCollection());
        Mockito.verify(parametrosGlobaisService, Mockito.times(1)).getParametrosGlobais();

    }

    private static ConfiguredViewFacade configuredViewFrontService(
            ConfiguredViewRepository configuredViewRepository,
            ConfiguredViewKeyFigureRepository configuredViewKeyFigureRepository) {

        ConfiguredViewFacade service = new ConfiguredViewFacade();
        ReflectionTestUtils.setField(service, "configuredViewRepository", configuredViewRepository);
        ReflectionTestUtils.setField(service, "configuredViewKeyFigureRepository", configuredViewKeyFigureRepository);

        UnidadeMedidaRepository unidadeMedidaRepository = Mockito.mock(UnidadeMedidaRepository.class);
        Mockito.when(unidadeMedidaRepository.findById("UN"))
                .thenReturn(Optional.of(new UnidadeMedida("UN")));
        ReflectionTestUtils.setField(service, "unidadeMedidaRepository", unidadeMedidaRepository);
        return service;
    }

    private static ConfiguredView configuredView() {

        return new ConfiguredView(new ConfiguredView.ConfiguredViewCompositeKey(
                "DEBUG", "Planning Book", ConfiguredView.TipoView.DEMANDPLANNINGBOOK));
    }

    private static ConfiguredViewKeyFigure configuredViewKeyFigure(
            ConfiguredView configuredView,
            String keyFigureId) {

        return new ConfiguredViewKeyFigure(new ConfiguredViewKeyFigure.Key(configuredView, keyFigureId));
    }

    private static ConfiguredViewDTO configuredViewDTO() {

        ConfiguredViewDTO dto = new ConfiguredViewDTO();
        dto.userId = "DEBUG";
        dto.viewName = "Planning Book";
        dto.viewType = ConfiguredView.TipoView.DEMANDPLANNINGBOOK;
        dto.unitOfMeasure = "UN";
        dto.showMaterialLevel = true;
        dto.showLocationLevel = true;
        return dto;
    }

    private static ConfiguredViewKeyFigureDTO keyFigure(
            String keyFigureId,
            Boolean allowChanges,
            Integer position) {

        ConfiguredViewKeyFigureDTO dto = new ConfiguredViewKeyFigureDTO();
        dto.keyFigure = keyFigureId;
        dto.allowChanges = allowChanges;
        dto.position = position;
        return dto;
    }

    private static ConfiguredViewDTO configuredViewDTOForListing(ConfiguredView configuredView) {

        ConfiguredViewDTO dto = configuredViewDTO();
        dto.viewName = configuredView.getNomeView();
        dto.viewType = configuredView.getTipoView();
        dto.materialCharacteristicDetailList = List.of();
        dto.locationCharacteristicDetailList = List.of();
        dto.materialLocationCharacteristicDetailList = List.of();
        dto.keyFigureList = List.of();
        return dto;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentCaptor<Iterable<ConfiguredViewKeyFigure>> iterableCaptor() {

        return (ArgumentCaptor) ArgumentCaptor.forClass(Iterable.class);
    }

}
