package com.opsfactor.community.capability.planningbook.keyfigure.projection;

import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredViewKeyFigure;
import com.opsfactor.community.capability.configuration.user.projection.ConfiguredViewProjection;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureInterface;
import com.opsfactor.community.capability.configuration.user.repository.ConfiguredViewKeyFigureRepository;
import com.opsfactor.community.capability.demandplanning.planningbook.domain.DemandPlanningPlanningBookCatalog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

/**
 * Garante que ausência de seleção persistida não produza uma grade vazia.
 */
class ConfiguredViewKeyFigureSelectionProjectionCommunityTest {

    @Test
    void emptyPersistedSelectionShouldUseTheCommunityDemandPlanningBookCatalog() {

        ConfiguredView configuredView = new ConfiguredView(new ConfiguredView.ConfiguredViewCompositeKey(
                "DEBUG",
                "Planning Book",
                ConfiguredView.TipoView.DEMANDPLANNINGBOOK));
        ConfiguredViewProjection configuredViewProjection = new ConfiguredViewProjection();
        configuredViewProjection.setConfiguredView(configuredView);

        KeyFigureProjection keyFigureProjection = new KeyFigureProjection();
        keyFigureProjection.configuredViewProjection = configuredViewProjection;

        ConfiguredViewKeyFigureRepository configuredViewKeyFigureRepository =
                Mockito.mock(ConfiguredViewKeyFigureRepository.class);
        Mockito.when(configuredViewKeyFigureRepository.findAllByConfiguredViewIn(Mockito.anyCollection()))
                .thenReturn(List.of());

        KeyFigureProjectionFactoryExposta factory = new KeyFigureProjectionFactoryExposta();
        ReflectionTestUtils.setField(
                factory,
                "configuredViewKeyFigureRepository",
                configuredViewKeyFigureRepository);

        factory.loadPersistedSelection(keyFigureProjection, configuredViewProjection);
        factory.buildPresentedDemandKeyFigures(keyFigureProjection, configuredViewProjection);

        Assertions.assertEquals(
                DemandPlanningPlanningBookCatalog.getKeyFiguresVisiveisDemandPlanningBookCommunity(),
                keyFigureProjection.getKeyFiguresApresentadosEOrdenados().stream()
                        .map(KeyFigureInterface::getId)
                        .toList());
        Assertions.assertTrue(configuredViewProjection.getKeyFiguresConfiguradasPorId().isEmpty());
        Mockito.verify(configuredViewKeyFigureRepository, Mockito.times(1))
                .findAllByConfiguredViewIn(Mockito.anyCollection());
    }

    @Test
    void persistedSupplySelectionShouldPreserveConfiguredOrderAndAllowChanges() {

        ConfiguredView configuredView = new ConfiguredView(new ConfiguredView.ConfiguredViewCompositeKey(
                "DEBUG",
                "Supply Planning Book",
                ConfiguredView.TipoView.SUPPLYPLANNINGBOOK));
        ConfiguredViewProjection configuredViewProjection = new ConfiguredViewProjection();
        configuredViewProjection.setConfiguredView(configuredView);
        KeyFigureProjection keyFigureProjection = new KeyFigureProjection();
        keyFigureProjection.configuredViewProjection = configuredViewProjection;

        ConfiguredViewKeyFigure stock = configuredViewKeyFigure(
                configuredView,
                "Stock-Working Plan",
                2,
                false);
        ConfiguredViewKeyFigure plannedInbound = configuredViewKeyFigure(
                configuredView,
                "Planned Inbound-Working Plan",
                1,
                true);
        ConfiguredViewKeyFigureRepository configuredViewKeyFigureRepository =
                Mockito.mock(ConfiguredViewKeyFigureRepository.class);
        Mockito.when(configuredViewKeyFigureRepository.findAllByConfiguredViewIn(Mockito.anyCollection()))
                .thenReturn(List.of(stock, plannedInbound));

        KeyFigureProjectionFactoryExposta factory = new KeyFigureProjectionFactoryExposta();
        ReflectionTestUtils.setField(
                factory,
                "configuredViewKeyFigureRepository",
                configuredViewKeyFigureRepository);

        factory.loadPersistedSelection(keyFigureProjection, configuredViewProjection);

        Assertions.assertEquals(
                List.of("Planned Inbound-Working Plan", "Stock-Working Plan"),
                configuredViewProjection.getKeyFiguresOrdenadasParaExibicao().stream()
                        .map(KeyFigureInterface::getId)
                        .toList());
        Assertions.assertFalse(configuredViewProjection.getKeyFiguresConfiguradasPorId()
                .get("Stock-Working Plan")
                .getAllowChanges());
        Assertions.assertTrue(configuredViewProjection.getKeyFiguresConfiguradasPorId()
                .get("Planned Inbound-Working Plan")
                .getAllowChanges());
        Mockito.verify(configuredViewKeyFigureRepository, Mockito.times(1))
                .findAllByConfiguredViewIn(Mockito.anyCollection());

    }

    private static ConfiguredViewKeyFigure configuredViewKeyFigure(
            ConfiguredView configuredView,
            String keyFigureId,
            int position,
            boolean allowChanges) {

        ConfiguredViewKeyFigure configuredViewKeyFigure = new ConfiguredViewKeyFigure(
                new ConfiguredViewKeyFigure.Key(configuredView, keyFigureId));
        configuredViewKeyFigure.setPosition(position);
        configuredViewKeyFigure.setAllowChanges(allowChanges);
        return configuredViewKeyFigure;

    }

    private static class KeyFigureProjectionFactoryExposta extends KeyFigureProjectionFactory {

        private void loadPersistedSelection(
                KeyFigureProjection keyFigureProjection,
                ConfiguredViewProjection configuredViewProjection) {

            super.carregaSelecaoPersistidaDeKeyFigures(
                    keyFigureProjection,
                    configuredViewProjection);
        }

        private void buildPresentedDemandKeyFigures(
                KeyFigureProjection keyFigureProjection,
                ConfiguredViewProjection configuredViewProjection) {

            super.atualizaProjectionComKeyFiguresDemandPlanningApresentados(
                    keyFigureProjection,
                    configuredViewProjection);
        }

    }

}
