package com.opsfactor.community.capability.configuration.facade.mapper;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredViewKeyFigure;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/**
 * Valida o contrato Community do mapper de User Views.
 *
 * <p>O DTO transicional continua amplo, mas o mapper Community sempre devolve
 * listas vazias para configuracoes Enterprise de caracteristicas e filtros
 * DFU. A selecao de Key Figures standard e uma preferencia compartilhada e
 * chega ao mapper ja carregada em lote pelo service.</p>
 */
public class ConfiguredViewAutoMapperCommunityTest {

    @Test
    public void converteShouldExposeEmptyEnterpriseConfigurationListsCommunity() {

        ConfiguredViewAutoMapper configuredViewAutoMapper = Mappers.getMapper(ConfiguredViewAutoMapper.class);
        ConfiguredView configuredView = new ConfiguredView(new ConfiguredView.ConfiguredViewCompositeKey(
                "USER",
                "Community View",
                ConfiguredView.TipoView.DEMANDPLANNINGBOOK));
        configuredView.setUnidadeMedidaView(new UnidadeMedida("UN"));
        configuredView.setExibeMateriaisDescontinuados(true);

        ConfiguredViewDTO configuredViewDTO = configuredViewAutoMapper.converte(
                configuredView,
                new ParametrosGlobais());

        Assertions.assertNotNull(configuredViewDTO.materialCharacteristicDetailList);
        Assertions.assertNotNull(configuredViewDTO.locationCharacteristicDetailList);
        Assertions.assertNotNull(configuredViewDTO.materialLocationCharacteristicDetailList);
        Assertions.assertNotNull(configuredViewDTO.keyFigureList);
        Assertions.assertTrue(configuredViewDTO.materialCharacteristicDetailList.isEmpty());
        Assertions.assertTrue(configuredViewDTO.locationCharacteristicDetailList.isEmpty());
        Assertions.assertTrue(configuredViewDTO.materialLocationCharacteristicDetailList.isEmpty());
        Assertions.assertTrue(configuredViewDTO.keyFigureList.isEmpty());
        Assertions.assertTrue(configuredViewDTO.showDiscontinuedMaterials);

    }

    @Test
    public void converteShouldPreserveSharedWorkflowScalarIdentifiersWithoutResolvingThem() {

        ConfiguredViewAutoMapper configuredViewAutoMapper = Mappers.getMapper(ConfiguredViewAutoMapper.class);
        ConfiguredView configuredView = new ConfiguredView(new ConfiguredView.ConfiguredViewCompositeKey(
                "USER",
                "Enterprise Workflow View",
                ConfiguredView.TipoView.DEMANDPLANNINGBOOK));
        configuredView.setUnidadeMedidaView(new UnidadeMedida("UN"));
        configuredView.setDemandPlanWorkflowId("MONTHLY-COLLABORATION");
        configuredView.setDemandPlanWorkflowStageId("CONSENSUS");

        ConfiguredViewDTO configuredViewDTO = configuredViewAutoMapper.converte(
                configuredView,
                new ParametrosGlobais());

        /*
         * O mapper Community somente transporta os escalares da tabela comum:
         * ele nao cria relacao JPA nem tenta consultar entidades Enterprise.
         */
        Assertions.assertEquals("MONTHLY-COLLABORATION", configuredViewDTO.demandPlanWorkflowId);
        Assertions.assertEquals("CONSENSUS", configuredViewDTO.demandPlanWorkflowStageId);

    }

    @Test
    public void converteComKeyFiguresShouldPublishThePersistedStandardSelectionInNormalizedOrder() {

        ConfiguredViewAutoMapper configuredViewAutoMapper = Mappers.getMapper(ConfiguredViewAutoMapper.class);
        ConfiguredView configuredView = new ConfiguredView(new ConfiguredView.ConfiguredViewCompositeKey(
                "USER",
                "Community View",
                ConfiguredView.TipoView.DEMANDPLANNINGBOOK));
        configuredView.setUnidadeMedidaView(new UnidadeMedida("UN"));

        ConfiguredViewKeyFigure demandAdjustment = new ConfiguredViewKeyFigure(
                new ConfiguredViewKeyFigure.Key(configuredView, "Demand Adjustment"));
        demandAdjustment.setPosition(2);
        demandAdjustment.setAllowChanges(false);
        ConfiguredViewKeyFigure baseline = new ConfiguredViewKeyFigure(
                new ConfiguredViewKeyFigure.Key(configuredView, "Baseline"));
        baseline.setPosition(1);

        ConfiguredViewDTO configuredViewDTO = configuredViewAutoMapper.converteComKeyFigures(
                configuredView,
                new ParametrosGlobais(),
                java.util.List.of(demandAdjustment, baseline));

        Assertions.assertEquals(
                java.util.List.of("Baseline", "Demand Adjustment"),
                configuredViewDTO.keyFigureList.stream().map(keyFigure -> keyFigure.keyFigure).toList());
        Assertions.assertEquals(
                java.util.List.of(1, 2),
                configuredViewDTO.keyFigureList.stream().map(keyFigure -> keyFigure.position).toList());
        Assertions.assertEquals(
                java.util.Arrays.asList(null, false),
                configuredViewDTO.keyFigureList.stream().map(keyFigure -> keyFigure.allowChanges).toList());

    }

}
