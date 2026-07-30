package com.opsfactor.community.capability.demandplanning.demandplan.projection;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlanItem;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.HistoricoDemandPlanItem;
import com.opsfactor.community.platform.exception.UnitOfMeasureConversionException;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.FiltroDFUProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandard;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardEnum;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

class DemandPlanningProjectionCommunityContractTest {

    @Test
    void directLineSetterShouldRejectNonZeroEnterpriseKeyFigures() {

        Fixture fixture = getFixture();

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> fixture.demandPlanningProjection.setValorDemandPlanItemEmKeyFigureStandardDp(
                        fixture.demandPlanItem,
                        10.0d,
                        Constantes.TipoDemanda.ITENS_NOVOS,
                        Constantes.TipoPlano.PLANO_IRRESTRITO,
                        fixture.unidadeMedida));

    }

    @Test
    void periodSetterShouldRejectNonZeroEnterpriseKeyFigures() {

        Fixture fixture = getFixture();

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> fixture.demandPlanningProjection.setValorDemandPlanItemEmKeyFigureStandardDp(
                        0,
                        fixture.location,
                        fixture.material,
                        10.0d,
                        Constantes.TipoDemanda.UPLIFT,
                        Constantes.TipoPlano.PLANO_IRRESTRITO,
                        fixture.unidadeMedida));

    }

    @Test
    void directLineSetterShouldPreserveUnitConversionCause() {

        Fixture fixture = getFixture();
        UnidadeMedida unidadeMedidaEntrada = new UnidadeMedida("KG");

        UnitOfMeasureConversionException unitOfMeasureConversionException = Assertions.assertThrows(
                UnitOfMeasureConversionException.class,
                () -> fixture.demandPlanningProjection.setValorDemandPlanItemEmKeyFigureStandardDp(
                        fixture.demandPlanItem,
                        10.0d,
                        Constantes.TipoDemanda.BASELINE,
                        Constantes.TipoPlano.PLANO_IRRESTRITO,
                        unidadeMedidaEntrada));

        assertUnitConversionCausePreserved(unitOfMeasureConversionException);

    }

    @Test
    void periodSetterShouldPreserveUnitConversionCause() {

        Fixture fixture = getFixture();
        UnidadeMedida unidadeMedidaEntrada = new UnidadeMedida("KG");

        UnitOfMeasureConversionException unitOfMeasureConversionException = Assertions.assertThrows(
                UnitOfMeasureConversionException.class,
                () -> fixture.demandPlanningProjection.setValorDemandPlanItemEmKeyFigureStandardDp(
                        0,
                        fixture.location,
                        fixture.material,
                        10.0d,
                        Constantes.TipoDemanda.BASELINE,
                        Constantes.TipoPlano.PLANO_IRRESTRITO,
                        unidadeMedidaEntrada));

        assertUnitConversionCausePreserved(unitOfMeasureConversionException);

    }

    @Test
    void enterpriseKeyFigureZeroWriteShouldRemainAllowedForDefensiveNeutralization() {

        Fixture fixture = getFixture();
        fixture.demandPlanItem.setQuantidadeUplift(123.0d);

        fixture.demandPlanningProjection.setValorDemandPlanItemEmKeyFigureStandardDp(
                fixture.demandPlanItem,
                0.0d,
                Constantes.TipoDemanda.UPLIFT,
                Constantes.TipoPlano.PLANO_IRRESTRITO,
                fixture.unidadeMedida);

        Assertions.assertEquals(0.0d, fixture.demandPlanItem.getQuantidadeUplift(), 0.0001d);

    }

    @Test
    void indexingShouldRejectDuplicatedDemandPlanItemKeyBeforeOverwrite() {

        Fixture fixture = getFixture();
        DemandPlanItem demandPlanItemDuplicada = new DemandPlanItem(
                new DemandPlanItem.DemandPlanItemKey(
                        fixture.demandPlanItem.getDemandPlan(),
                        fixture.location,
                        fixture.material,
                        fixture.demandPlanItem.getDataReferencia()));

        /*
         * A projection deve ter uma unica linha material/location por periodo.
         * Outra instancia com a mesma chave faria Planning Book e Demand -> Supply
         * dependerem da ordem de carga do snapshot.
         */
        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> fixture.demandPlanningProjection.addDemandPlanItem(demandPlanItemDuplicada));

        Assertions.assertEquals(
                "Demand Planning projection already has a Demand Plan line for period 0, location LOCATION and material MATERIAL.",
                illegalStateException.getMessage());

    }

    @Test
    void indexingShouldRejectDuplicatedHistoricalDemandPlanItemKeyBeforeOverwrite() {

        Fixture fixture = getFixture();
        HistoricoDemandPlanItem historicoDemandPlanItemDuplicada = new HistoricoDemandPlanItem(
                new HistoricoDemandPlanItem.HistoricoDemandPlanItemKey(
                        fixture.demandPlanItem.getDemandPlan(),
                        fixture.location,
                        fixture.material,
                        fixture.demandPlanItem.getDataReferencia()));

        /*
         * Historico de forecast salvo tambem e chave unica por periodo/location/
         * material; duplicidade silenciosa trocaria trend/seasonal/forecast sem
         * deixar rastro para a simulacao ou o Planning Book.
         */
        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> fixture.demandPlanningProjection.addHistoricoDemandPlanItem(
                        historicoDemandPlanItemDuplicada));

        Assertions.assertEquals(
                "Demand Planning projection already has a historical Demand Plan line for period 0, location LOCATION and material MATERIAL.",
                illegalStateException.getMessage());

    }

    @Test
    void directLineSetterShouldRejectNullDemandOrPlanEnumsWithContractMessage() {

        Fixture fixture = getFixture();

        IllegalArgumentException tipoDemandaAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> fixture.demandPlanningProjection.setValorDemandPlanItemEmKeyFigureStandardDp(
                        fixture.demandPlanItem,
                        10.0d,
                        null,
                        Constantes.TipoPlano.PLANO_IRRESTRITO,
                        fixture.unidadeMedida));
        IllegalArgumentException tipoPlanoAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> fixture.demandPlanningProjection.setValorDemandPlanItemEmKeyFigureStandardDp(
                        fixture.demandPlanItem,
                        10.0d,
                        Constantes.TipoDemanda.BASELINE,
                        null,
                        fixture.unidadeMedida));

        /*
         * Enums nulos indicam erro de contrato da chamada. A mensagem precisa ser
         * funcional, nao um NullPointerException no `.name()` do enum.
         */
        Assertions.assertTrue(tipoDemandaAusenteException.getMessage().contains(
                "DemandPlanningProjection can write only Demand Plan physical components"));
        Assertions.assertTrue(tipoDemandaAusenteException.getMessage().contains("received null"));
        Assertions.assertTrue(tipoPlanoAusenteException.getMessage().contains(
                "DemandPlanningProjection can write only Demand Plan line variants"));
        Assertions.assertTrue(tipoPlanoAusenteException.getMessage().contains("received null"));

    }

    @Test
    void directLineSetterShouldRejectDerivedDemandComponentBeforeMutation() {

        Fixture fixture = getFixture();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> fixture.demandPlanningProjection.setValorDemandPlanItemEmKeyFigureStandardDp(
                        fixture.demandPlanItem,
                        10.0d,
                        Constantes.TipoDemanda.TOTAL,
                        Constantes.TipoPlano.PLANO_IRRESTRITO,
                        fixture.unidadeMedida));

        Assertions.assertTrue(
                illegalArgumentException.getMessage().contains(
                        "DemandPlanningProjection can write only Demand Plan physical components"));
        Assertions.assertTrue(
                illegalArgumentException.getMessage().contains("received TOTAL"));

    }

    @Test
    void periodSetterShouldRejectUnsupportedPlanVariantBeforeMutation() {

        Fixture fixture = getFixture();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> fixture.demandPlanningProjection.setValorDemandPlanItemEmKeyFigureStandardDp(
                        0,
                        fixture.location,
                        fixture.material,
                        10.0d,
                        Constantes.TipoDemanda.BASELINE,
                        Constantes.TipoPlano.HISTORICO,
                        fixture.unidadeMedida));

        Assertions.assertTrue(
                illegalArgumentException.getMessage().contains(
                        "DemandPlanningProjection can write only Demand Plan line variants"));
        Assertions.assertTrue(
                illegalArgumentException.getMessage().contains("received HISTORICO"));

    }

    @Test
    void trendSeasonalReadShouldReturnZeroWhenThereIsNoLine() throws Exception {

        Fixture fixture = getFixture();

        Assertions.assertEquals(
                0.0,
                fixture.demandPlanningProjection.getValorDemandPlanItem(
                        1,
                        fixture.location,
                        fixture.material,
                        null,
                        fixture.unidadeMedida));
        Assertions.assertEquals(
                0.0,
                fixture.demandPlanningProjection.getValorHistoricoDemandPlanItem(
                        1,
                        fixture.location,
                        fixture.material,
                        null,
                        fixture.unidadeMedida));

    }

    @Test
    void totalReadShouldIgnoreEnterpriseKeyFiguresInCommunityProjection() {

        Fixture fixture = getFixture();
        fixture.demandPlanItem.setQuantidadeBaseline(10.0d);
        fixture.demandPlanItem.setQuantidadeAjusteDemanda(5.0d);
        fixture.demandPlanItem.setQuantidadeUplift(100.0d);
        fixture.demandPlanItem.setQuantidadeItensNovos(50.0d);
        fixture.demandPlanItem.setQuantidadeBaselineAtendida(7.0d);
        fixture.demandPlanItem.setQuantidadeAjusteDemandaAtendida(3.0d);
        fixture.demandPlanItem.setQuantidadeUpliftAtendida(80.0d);
        fixture.demandPlanItem.setQuantidadeItensNovosAtendida(40.0d);

        /*
         * A entidade continua representando a linha fisica completa. A
         * projection, entretanto, deve expor o total funcional Community, sem
         * Uplift e New Materials.
         */
        Assertions.assertEquals(165.0d, fixture.demandPlanItem.getQuantidadeTotal(), 0.0001d);
        Assertions.assertEquals(15.0d, fixture.demandPlanningProjection.getValorDemandPlanItem(
                fixture.demandPlanItem,
                Constantes.TipoDemanda.TOTAL,
                Constantes.TipoPlano.PLANO_IRRESTRITO,
                fixture.unidadeMedida), 0.0001d);
        Assertions.assertEquals(10.0d, fixture.demandPlanningProjection.getValorDemandPlanItem(
                fixture.demandPlanItem,
                Constantes.TipoDemanda.TOTAL,
                Constantes.TipoPlano.PLANO_RESTRITO,
                fixture.unidadeMedida), 0.0001d);
        Assertions.assertEquals(15.0d, fixture.demandPlanningProjection.getValorDemandPlanItem(
                0,
                List.of(fixture.location),
                List.of(fixture.material),
                Constantes.TipoDemanda.TOTAL,
                Constantes.TipoPlano.PLANO_IRRESTRITO,
                fixture.unidadeMedida), 0.0001d);

    }

    @Test
    void totalReadShouldRejectNonPhysicalPlanWithContractMessage() {

        Fixture fixture = getFixture();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> fixture.demandPlanningProjection.getValorDemandPlanItem(
                        fixture.demandPlanItem,
                        Constantes.TipoDemanda.TOTAL,
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        fixture.unidadeMedida));

        Assertions.assertTrue(illegalArgumentException.getMessage().contains(
                "DemandPlanningProjection can read the Community functional total only from Demand Plan physical variants"));
        Assertions.assertTrue(illegalArgumentException.getMessage().contains("received PLANO_TRABALHO"));

    }

    @Test
    void directLineReadShouldRejectNullDemandOrPlanEnumsWithContractMessage() {

        Fixture fixture = getFixture();

        IllegalArgumentException tipoDemandaAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> fixture.demandPlanningProjection.getValorDemandPlanItem(
                        fixture.demandPlanItem,
                        null,
                        Constantes.TipoPlano.PLANO_IRRESTRITO,
                        fixture.unidadeMedida));
        IllegalArgumentException tipoPlanoAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> fixture.demandPlanningProjection.getValorDemandPlanItem(
                        fixture.demandPlanItem,
                        Constantes.TipoDemanda.BASELINE,
                        null,
                        fixture.unidadeMedida));

        Assertions.assertEquals(
                "DemandPlanningProjection requires demand component to read Demand Plan line; received null.",
                tipoDemandaAusenteException.getMessage());
        Assertions.assertEquals(
                "DemandPlanningProjection requires plan variant to read Demand Plan line; received null.",
                tipoPlanoAusenteException.getMessage());

    }

    @Test
    void totalizationReferenceShouldRejectEnterpriseDemandKeyFiguresExceptNewProductsReadPath() {

        Fixture fixture = getFixture();
        fixture.demandPlanItem.setQuantidadeItensNovos(4.0d);
        FiltroDFUProjection filtroDFUProjection = new FiltroDFUProjection(
                Set.of(fixture.location),
                Set.of(fixture.material),
                new TestClusterEParametrosProjection());

        List<KeyFigureStandardEnum> keyFigureStandardEnumEnterpriseList = List.of(
                KeyFigureStandardEnum.UPLIFT,
                KeyFigureStandardEnum.CARTEIRA,
                KeyFigureStandardEnum.DEMANDA_DIRETA_TOTAL_COMPARACAO,
                KeyFigureStandardEnum.DEMANDA_DIRETA_TOTAL_DP_POR_DIA_UTIL);

        /*
         * New Products ainda nao e editavel no Community, mas a projection
         * precisa conseguir ler e totalizar a coluna fisica para o overlay
         * Enterprise que ja reabriu a linha no Planning Book.
         */
        Assertions.assertEquals(
                4.0d,
                fixture.demandPlanningProjection.getValorDemandPlanItem(
                        0,
                        filtroDFUProjection,
                        List.of(new KeyFigureStandard(KeyFigureStandardEnum.ITENS_NOVOS)),
                        fixture.unidadeMedida),
                0.0001d);
        Assertions.assertEquals(
                4.0d,
                fixture.demandPlanningProjection.getValorTotalKeyFigures(
                        List.of(new KeyFigureStandard(KeyFigureStandardEnum.ITENS_NOVOS)),
                        0,
                        filtroDFUProjection,
                        fixture.unidadeMedida),
                0.0001d);
        Assertions.assertEquals(
                4.0d,
                fixture.demandPlanningProjection.getValorTotalKeyFigure(
                        new KeyFigureStandard(KeyFigureStandardEnum.ITENS_NOVOS),
                        0,
                        fixture.location,
                        fixture.material,
                        fixture.unidadeMedida),
                0.0001d);

        for (KeyFigureStandardEnum keyFigureStandardEnumEnterprise : keyFigureStandardEnumEnterpriseList) {
            Assertions.assertThrows(
                    RequiresEnterpriseVersionException.class,
                    () -> fixture.demandPlanningProjection.getValorDemandPlanItem(
                            0,
                            filtroDFUProjection,
                            List.of(new KeyFigureStandard(keyFigureStandardEnumEnterprise)),
                            fixture.unidadeMedida));
            Assertions.assertThrows(
                    RequiresEnterpriseVersionException.class,
                    () -> fixture.demandPlanningProjection.getValorTotalKeyFigures(
                            List.of(new KeyFigureStandard(keyFigureStandardEnumEnterprise)),
                            0,
                            filtroDFUProjection,
                            fixture.unidadeMedida));
            Assertions.assertThrows(
                    RequiresEnterpriseVersionException.class,
                    () -> fixture.demandPlanningProjection.getValorTotalKeyFigure(
                            new KeyFigureStandard(keyFigureStandardEnumEnterprise),
                            0,
                            fixture.location,
                            fixture.material,
                            fixture.unidadeMedida));
        }

    }

    @Test
    void totalizationReferenceShouldRejectCommunityKeyFiguresThatAreNotSplitBase() {

        Fixture fixture = getFixture();
        FiltroDFUProjection filtroDFUProjection = new FiltroDFUProjection(
                Set.of(fixture.location),
                Set.of(fixture.material),
                new TestClusterEParametrosProjection());

        /*
         * Direct Demand e Historical Sales sao KFs Community de exibicao, mas
         * nao sao bases editaveis do rateio. O split precisa usar somente as
         * KFs materiais do total: Baseline e Demand Adjustment.
         */
        for (KeyFigureStandardEnum keyFigureStandardEnumNaoBase : List.of(
                KeyFigureStandardEnum.DEMANDA_DIRETA_TOTAL_DP,
                KeyFigureStandardEnum.HISTORICO_VENDAS)) {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> fixture.demandPlanningProjection.getValorDemandPlanItem(
                            0,
                            filtroDFUProjection,
                            List.of(new KeyFigureStandard(keyFigureStandardEnumNaoBase)),
                            fixture.unidadeMedida));
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> fixture.demandPlanningProjection.getValorTotalKeyFigures(
                            List.of(new KeyFigureStandard(keyFigureStandardEnumNaoBase)),
                            0,
                            filtroDFUProjection,
                            fixture.unidadeMedida));
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> fixture.demandPlanningProjection.getValorTotalKeyFigure(
                            new KeyFigureStandard(keyFigureStandardEnumNaoBase),
                            0,
                            fixture.location,
                            fixture.material,
                            fixture.unidadeMedida));
        }

    }

    @Test
    void physicalComponentHelperShouldRejectDisplayKeyFigureBeforeReadingLine() {

        Fixture fixture = getFixture();
        KeyFigureStandard historicalSalesKeyFigure = new KeyFigureStandard(KeyFigureStandardEnum.HISTORICO_VENDAS);

        IllegalArgumentException lineOverloadException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> fixture.demandPlanningProjection.getValorKeyFigureStandardDpNaoTotalizadora(
                        fixture.demandPlanItem,
                        historicalSalesKeyFigure,
                        fixture.unidadeMedida));
        IllegalArgumentException periodOverloadException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> fixture.demandPlanningProjection.getValorKeyFigureStandardDpNaoTotalizadora(
                        historicalSalesKeyFigure,
                        0,
                        fixture.location,
                        fixture.material,
                        fixture.unidadeMedida));

        Assertions.assertTrue(lineOverloadException.getMessage().contains(
                "DemandPlanningProjection can read this helper only for physical Demand Planning components"));
        Assertions.assertTrue(lineOverloadException.getMessage().contains("received key figure HISTORICO_VENDAS"));
        Assertions.assertTrue(periodOverloadException.getMessage().contains(
                "Display, historical, total and Enterprise key figures must be handled by their Planning Book boundary"));

    }

    @Test
    void totalizationReferenceShouldStillReadCommunityBaseKeyFigures() {

        Fixture fixture = getFixture();
        fixture.demandPlanItem.setQuantidadeBaseline(14.0d);
        fixture.demandPlanItem.setQuantidadeAjusteDemanda(3.0d);
        FiltroDFUProjection filtroDFUProjection = new FiltroDFUProjection(
                Set.of(fixture.location),
                Set.of(fixture.material),
                new TestClusterEParametrosProjection());

        Assertions.assertEquals(
                17.0d,
                fixture.demandPlanningProjection.getValorDemandPlanItem(
                        0,
                        filtroDFUProjection,
                        List.of(
                                new KeyFigureStandard(KeyFigureStandardEnum.BASELINE),
                                new KeyFigureStandard(KeyFigureStandardEnum.AJUSTE_DEMANDA)),
                        fixture.unidadeMedida),
                0.0001d);
        Assertions.assertEquals(
                17.0d,
                fixture.demandPlanningProjection.getValorTotalKeyFigures(
                        List.of(
                                new KeyFigureStandard(KeyFigureStandardEnum.BASELINE),
                                new KeyFigureStandard(KeyFigureStandardEnum.AJUSTE_DEMANDA)),
                        0,
                        filtroDFUProjection,
                        fixture.unidadeMedida),
                0.0001d);
        Assertions.assertEquals(
                14.0d,
                fixture.demandPlanningProjection.getValorTotalKeyFigure(
                        new KeyFigureStandard(KeyFigureStandardEnum.BASELINE),
                        0,
                        fixture.location,
                        fixture.material,
                        fixture.unidadeMedida),
                0.0001d);

    }

    @Test
    void aggregatedStandardAdjustmentShouldPreserveLegacyDirectDemandDistribution() {

        Fixture fixture = getFixture();
        Location secondLocation = new Location("LOCATION-2");
        DemandPlanItem secondDemandPlanItem = new DemandPlanItem(
                new DemandPlanItem.DemandPlanItemKey(
                        fixture.demandPlanItem.getDemandPlan(),
                        secondLocation,
                        fixture.material,
                        fixture.demandPlanItem.getDataReferencia()));
        secondDemandPlanItem.setUnidadeMedida(fixture.unidadeMedida);
        fixture.demandPlanningProjection.addDemandPlanItem(secondDemandPlanItem);

        fixture.demandPlanItem.setQuantidadeBaseline(80.0d);
        secondDemandPlanItem.setQuantidadeBaseline(20.0d);
        FiltroDFUProjection aggregatedDfuFilter = new FiltroDFUProjection(
                Set.of(fixture.location, secondLocation),
                Set.of(fixture.material),
                new TestClusterEParametrosProjection());

        var adjustedLines = fixture.demandPlanningProjection.setValorDemandPlanItemEmKeyFigureStandardDp(
                0,
                aggregatedDfuFilter,
                Constantes.TipoDemanda.AJUSTE_DEMANDA,
                Constantes.TipoPlano.PLANO_IRRESTRITO,
                List.of(new KeyFigureStandard(KeyFigureStandardEnum.BASELINE)),
                50.0d,
                fixture.unidadeMedida);

        Assertions.assertEquals(2, adjustedLines.size());
        Assertions.assertEquals(40.0d, fixture.demandPlanItem.getQuantidadeAjusteDemanda(), 0.0001d);
        Assertions.assertEquals(10.0d, secondDemandPlanItem.getQuantidadeAjusteDemanda(), 0.0001d);

    }

    private static Fixture getFixture() {

        Calendario calendario = Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                0,
                1,
                0);
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        Location location = new Location("LOCATION");
        Produto material = new Produto("MATERIAL");

        DemandPlan demandPlan = new DemandPlan();
        demandPlan.setTamanhoBucket(Constantes.TamanhoBucket.DIARIO);
        demandPlan.setDataInicioPlano(calendario.getPrimeiraDataHorarioPeriodo(0));

        DemandPlanningProjection demandPlanningProjection = new DemandPlanningProjection(
                demandPlan,
                new UnidadeMedidaProjection(),
                new TestClusterEParametrosProjection(),
                null,
                calendario,
                null,
                false,
                null,
                null);

        DemandPlanItem demandPlanItem = new DemandPlanItem(
                new DemandPlanItem.DemandPlanItemKey(
                        demandPlan,
                        location,
                        material,
                        calendario.getUltimoSegundoPeriodo(0)));
        demandPlanItem.setUnidadeMedida(unidadeMedida);
        demandPlanningProjection.addDemandPlanItem(demandPlanItem);

        HistoricoDemandPlanItem historicoDemandPlanItem = new HistoricoDemandPlanItem(
                new HistoricoDemandPlanItem.HistoricoDemandPlanItemKey(
                        demandPlan,
                        location,
                        material,
                        calendario.getUltimoSegundoPeriodo(0)));
        historicoDemandPlanItem.setUnidadeMedida(unidadeMedida);
        demandPlanningProjection.addHistoricoDemandPlanItem(historicoDemandPlanItem);

        return new Fixture(
                demandPlanningProjection,
                demandPlanItem,
                location,
                material,
                unidadeMedida);

    }

    private static void assertUnitConversionCausePreserved(
            UnitOfMeasureConversionException unitOfMeasureConversionException) {

        /*
         * A projection acrescenta contexto funcional da DFU, mas a causa
         * original da UnidadeMedidaProjection precisa sobreviver para mostrar
         * qual par de UOM nao tinha conversao cadastrada.
         */
        Assertions.assertTrue(unitOfMeasureConversionException.getMessage().contains(
                "No conversion found from input UOM KG to configured DP UOM UN for material MATERIAL and location LOCATION"));
        Assertions.assertTrue(unitOfMeasureConversionException.getCause() instanceof UnitOfMeasureConversionException);
        Assertions.assertTrue(unitOfMeasureConversionException.getCause().getMessage().contains(
                "No conversion available from KG to UN for material MATERIAL"));

    }

    /**
     * Stub minimo para testar a projection sem Spring nem repositories.
     *
     * <p>A regra em teste precisa apenas dos parametros globais para decidir
     * arredondamento de DP; demais mapas de cluster nao sao acessados porque o
     * teste usa uma linha existente com unidade de medida ja definida.</p>
     */
    private static class TestClusterEParametrosProjection extends ClusterEParametrosProjection {

        private TestClusterEParametrosProjection() {

            this.parametrosGlobais = new ParametrosGlobais();

        }

    }

    private record Fixture(
            DemandPlanningProjection demandPlanningProjection,
            DemandPlanItem demandPlanItem,
            Location location,
            Produto material,
            UnidadeMedida unidadeMedida) {
    }

}
