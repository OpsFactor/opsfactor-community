package com.opsfactor.community.capability.planningbook.keyfigure.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.platform.projection.inmemorybi.applied.BIProjectionMaterialLocationPeriodo;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureInterface;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandard;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardEnum;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.dfudata.DFUDataKeyFigureAbstract;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Contrato Community do indice de Key Figures material/location/periodo.
 *
 * <p>A factory monta a projection no fluxo normal. Estes testes protegem os
 * metodos publicos usados por Demand, Supply e overlays Enterprise para que
 * nenhum dado de Planning Book seja indexado com chave funcional quebrada,
 * periodo ausente, key figure nula ou valor nao finito.</p>
 */
class KeyFigureProjectionCommunityContractTest {

    @Test
    void addKeyFigureDataShouldRejectMissingCalendarOrBiBeforeIndexing() {

        KeyFigureProjection projectionSemCalendario = new KeyFigureProjection();
        IllegalStateException missingCalendarException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> projectionSemCalendario.addDadoDFUKeyFigurePadrao(
                        getLocationValida(),
                        getMaterialValido(),
                        0,
                        getKeyFigureValida(),
                        10.0));
        Assertions.assertEquals(
                "KeyFigureProjection requires calendar before adding Key Figure data by period.",
                missingCalendarException.getMessage());

        KeyFigureProjection projectionSemBi = new KeyFigureProjection();
        projectionSemBi.calendario = getCalendario();
        IllegalStateException missingBiException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> projectionSemBi.addDadoDFUKeyFigurePadrao(
                        getLocationValida(),
                        getMaterialValido(),
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        getKeyFigureValida(),
                        10.0));
        Assertions.assertEquals(
                "KeyFigureProjection requires BI before adding Key Figure data.",
                missingBiException.getMessage());

    }

    @Test
    void addKeyFigureDataShouldStoreValidFiniteData() {

        KeyFigureProjection keyFigureProjection = getKeyFigureProjectionComBi();
        KeyFigureInterface keyFigure = getKeyFigureValida();

        keyFigureProjection.addDadoDFUKeyFigurePadrao(
                getLocationValida(),
                getMaterialValido(),
                LocalDateTime.of(2026, 1, 1, 0, 0),
                keyFigure,
                10.0);

        Assertions.assertEquals(
                1,
                keyFigureProjection.getDadosKeyFigure(
                        keyFigure,
                        getMaterialValido(),
                        getLocationValida()).size());

    }

    @Test
    void unavailableReasonsShouldRemainSidecarWithoutAddingArtificialNumericData() {

        KeyFigureProjection keyFigureProjection = getKeyFigureProjectionComBi();
        LocalDateTime data = LocalDateTime.of(2026, 1, 1, 0, 0);

        keyFigureProjection.defineUnavailableReason(
                getLocationValida(),
                getMaterialValido(),
                data,
                getKeyFigureValida(),
                "MISSING_SOURCE_VALUE");

        Assertions.assertEquals(
                Map.of(data, "MISSING_SOURCE_VALUE"),
                keyFigureProjection.getUnavailableReasons(
                        getKeyFigureValida(),
                        getMaterialValido(),
                        getLocationValida()));
        Assertions.assertTrue(keyFigureProjection.getDadosKeyFigure(
                getKeyFigureValida(),
                getMaterialValido(),
                getLocationValida()).isEmpty());

    }

    private static KeyFigureProjection getKeyFigureProjectionComBi() {

        Calendario calendario = getCalendario();
        KeyFigureProjection keyFigureProjection = new KeyFigureProjection();
        keyFigureProjection.calendario = calendario;
        keyFigureProjection.biEmMemoriaDFUDataKeyFigure =
                new BIProjectionMaterialLocationPeriodo<>(
                        calendario,
                        DFUDataKeyFigureAbstract::getProduto,
                        DFUDataKeyFigureAbstract::getLocation,
                        DFUDataKeyFigureAbstract::getData,
                        DFUDataKeyFigureAbstract.class,
                        true,
                        true);
        keyFigureProjection.biEmMemoriaDFUDataKeyFigure.getBiEmMemoria()
                .addObjectAttribute(
                        "KeyFigure",
                        KeyFigureInterface.class,
                        DFUDataKeyFigureAbstract::getKeyFigure,
                        true);
        return keyFigureProjection;

    }

    private static Calendario getCalendario() {

        return Calendario.criaCalendarioDeOffsetsPeriodos(
                Constantes.TamanhoBucket.MENSAL,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                0,
                0,
                2,
                0);

    }

    private static Location getLocationValida() {

        return new Location("LOC_01", "Location 01");

    }

    private static Produto getMaterialValido() {

        return new Produto("MAT_01", "Material 01");

    }

    private static KeyFigureInterface getKeyFigureValida() {

        return new KeyFigureStandard(KeyFigureStandardEnum.BASELINE);

    }

}
