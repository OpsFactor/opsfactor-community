package com.opsfactor.community.capability.configuration.user.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.FiltroDFUProjection;
import com.opsfactor.community.capability.planningbook.domain.AjusteCelulaPlanningBook;
import com.opsfactor.community.capability.planningbook.domain.PlanningBookDfuScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Contrato de integridade da projection Community de Planning Book.
 *
 * <p>A factory cobre a montagem normal da view. Estes testes protegem a borda
 * dos consumidores: snapshots sem filtro DFU, escopos nulos ou selecoes sem
 * material/location devem falhar antes de streams e DTOs de front.</p>
 */
public class ConfiguredViewProjectionCommunityContractTest {

    @Test
    public void filteredMaterialLocationReadShouldRequireDfuProjection() {

        ConfiguredViewProjection configuredViewProjection = new ConfiguredViewProjection();

        IllegalStateException materiaisException = Assertions.assertThrows(
                IllegalStateException.class,
                configuredViewProjection::getMateriaisFiltrados);
        Assertions.assertEquals(
                "ConfiguredViewProjection requires filtered DFU projection before reading Planning Book material/location scope.",
                materiaisException.getMessage());

        IllegalStateException locationsException = Assertions.assertThrows(
                IllegalStateException.class,
                configuredViewProjection::getLocationsFiltradas);
        Assertions.assertEquals(
                "ConfiguredViewProjection requires filtered DFU projection before reading Planning Book material/location scope.",
                locationsException.getMessage());

    }

    @Test
    public void planningBookScopesShouldRejectMissingCollectionOrNullItem() {

        ConfiguredViewProjection configuredViewProjection = new ConfiguredViewProjection();

        IllegalStateException missingCollectionException = Assertions.assertThrows(
                IllegalStateException.class,
                configuredViewProjection::getPlanningBookDfuScopes);
        Assertions.assertEquals(
                "ConfiguredViewProjection requires Planning Book DFU scopes before rendering the Planning Book.",
                missingCollectionException.getMessage());

        Set<PlanningBookDfuScope> planningBookDfuScopes = new HashSet<>();
        planningBookDfuScopes.add(null);
        configuredViewProjection.setPlanningBookDfuScopes(planningBookDfuScopes);

        IllegalStateException nullItemException = Assertions.assertThrows(
                IllegalStateException.class,
                configuredViewProjection::getPlanningBookDfuScopes);
        Assertions.assertEquals(
                "ConfiguredViewProjection Planning Book DFU scope at index 0 is required.",
                nullItemException.getMessage());

    }

    @Test
    public void selectedCellsShouldRejectNullItemOrMissingDfuScope() {

        ConfiguredViewProjection configuredViewProjection = new ConfiguredViewProjection();
        Assertions.assertNull(configuredViewProjection.getDetalhesSelecaoAAtualizar());

        Set<AjusteCelulaPlanningBook> selecaoComItemNulo = new HashSet<>();
        selecaoComItemNulo.add(null);
        configuredViewProjection.setDetalhesSelecaoAAtualizar(selecaoComItemNulo);

        IllegalStateException nullSelectionItemException = Assertions.assertThrows(
                IllegalStateException.class,
                configuredViewProjection::getDetalhesSelecaoAAtualizar);
        Assertions.assertEquals(
                "ConfiguredViewProjection selected Planning Book cell at index 0 is required.",
                nullSelectionItemException.getMessage());

        Set<AjusteCelulaPlanningBook> selecaoSemEscopo = new HashSet<>();
        selecaoSemEscopo.add(new AjusteCelulaPlanningBook(
                null,
                "BASELINE",
                "EA",
                10.0,
                0.0,
                null));
        configuredViewProjection.setDetalhesSelecaoAAtualizar(selecaoSemEscopo);

        IllegalStateException missingScopeException = Assertions.assertThrows(
                IllegalStateException.class,
                configuredViewProjection::getDetalhesSelecaoAAtualizar);
        Assertions.assertEquals(
                "ConfiguredViewProjection selected Planning Book cell at index 0 requires material/location DFU scope.",
                missingScopeException.getMessage());

    }

    @Test
    public void selectedCellsShouldRejectMissingEditableCellCoordinatesOrValue() {

        PlanningBookDfuScope planningBookDfuScope = PlanningBookDfuScope.deMaterialLocation(
                new Produto("MAT_01", "Material 01"),
                new Location("LOC_01", "Location 01"));
        ConfiguredViewProjection configuredViewProjection = new ConfiguredViewProjection();

        configuredViewProjection.setDetalhesSelecaoAAtualizar(Set.of(new AjusteCelulaPlanningBook(
                null,
                "BASELINE",
                "EA",
                10.0,
                0.0,
                planningBookDfuScope)));
        IllegalStateException missingPeriodException = Assertions.assertThrows(
                IllegalStateException.class,
                configuredViewProjection::getDetalhesSelecaoAAtualizar);
        Assertions.assertEquals(
                "ConfiguredViewProjection selected Planning Book cell at index 0 requires period reference.",
                missingPeriodException.getMessage());

        configuredViewProjection.setDetalhesSelecaoAAtualizar(Set.of(new AjusteCelulaPlanningBook(
                LocalDateTime.of(2026, 1, 1, 0, 0),
                " ",
                "EA",
                10.0,
                0.0,
                planningBookDfuScope)));
        IllegalStateException missingKeyFigureException = Assertions.assertThrows(
                IllegalStateException.class,
                configuredViewProjection::getDetalhesSelecaoAAtualizar);
        Assertions.assertEquals(
                "ConfiguredViewProjection selected Planning Book cell at index 0 requires key figure id.",
                missingKeyFigureException.getMessage());

        configuredViewProjection.setDetalhesSelecaoAAtualizar(Set.of(new AjusteCelulaPlanningBook(
                LocalDateTime.of(2026, 1, 1, 0, 0),
                "BASELINE",
                null,
                10.0,
                0.0,
                planningBookDfuScope)));
        IllegalStateException missingUomException = Assertions.assertThrows(
                IllegalStateException.class,
                configuredViewProjection::getDetalhesSelecaoAAtualizar);
        Assertions.assertEquals(
                "ConfiguredViewProjection selected Planning Book cell at index 0 requires unit of measure id.",
                missingUomException.getMessage());

        configuredViewProjection.setDetalhesSelecaoAAtualizar(Set.of(new AjusteCelulaPlanningBook(
                LocalDateTime.of(2026, 1, 1, 0, 0),
                "BASELINE",
                "EA",
                Double.NaN,
                0.0,
                planningBookDfuScope)));
        IllegalStateException invalidNewValueException = Assertions.assertThrows(
                IllegalStateException.class,
                configuredViewProjection::getDetalhesSelecaoAAtualizar);
        Assertions.assertEquals(
                "ConfiguredViewProjection selected Planning Book cell at index 0 requires finite new value.",
                invalidNewValueException.getMessage());

    }

    @Test
    public void selectedMaterialLocationScopeShouldDriveUpdateScopeWhenPresent() {

        Produto materialFiltrado = new Produto("MAT_FILTER", "Material Filter");
        Produto materialSelecionado = new Produto("MAT_SELECTED", "Material Selected");
        Location locationFiltrada = new Location("LOC_FILTER", "Location Filter");
        Location locationSelecionada = new Location("LOC_SELECTED", "Location Selected");

        ConfiguredViewProjection configuredViewProjection = new ConfiguredViewProjection();
        configuredViewProjection.setDfuProjectionFiltrado(new FiltroDFUProjection(
                Set.of(locationFiltrada),
                Set.of(materialFiltrado),
                null));

        Assertions.assertEquals(Set.of(materialFiltrado), configuredViewProjection.getMateriaisAAtualizar());
        Assertions.assertEquals(Set.of(locationFiltrada), configuredViewProjection.getLocationsAAtualizar());

        Set<AjusteCelulaPlanningBook> detalhesSelecaoAAtualizar = Set.of(new AjusteCelulaPlanningBook(
                LocalDateTime.of(2026, 1, 1, 0, 0),
                "BASELINE",
                "EA",
                10.0,
                0.0,
                PlanningBookDfuScope.deMaterialLocation(materialSelecionado, locationSelecionada)));
        configuredViewProjection.setDetalhesSelecaoAAtualizar(detalhesSelecaoAAtualizar);

        Assertions.assertEquals(Set.of(materialSelecionado), configuredViewProjection.getMateriaisAAtualizar());
        Assertions.assertEquals(Set.of(locationSelecionada), configuredViewProjection.getLocationsAAtualizar());

    }

    @Test
    public void updateErrorMapShouldRejectBrokenEntriesBeforePlanningBookRendering() {

        ConfiguredViewProjection configuredViewProjection = new ConfiguredViewProjection();
        Assertions.assertNull(configuredViewProjection.getErroAtualizacaoPorDetalheSelecao());

        Map<AjusteCelulaPlanningBook, String> mapaComCelulaNula = new HashMap<>();
        mapaComCelulaNula.put(null, "erro");
        configuredViewProjection.setErroAtualizacaoPorDetalheSelecao(mapaComCelulaNula);
        IllegalStateException nullCellException = Assertions.assertThrows(
                IllegalStateException.class,
                configuredViewProjection::getErroAtualizacaoPorDetalheSelecao);
        Assertions.assertEquals(
                "ConfiguredViewProjection update error entry at index 0 requires selected Planning Book cell.",
                nullCellException.getMessage());

        Map<AjusteCelulaPlanningBook, String> mapaSemMensagem = Map.of(
                getAjusteCelulaPlanningBookValido(),
                " ");
        configuredViewProjection.setErroAtualizacaoPorDetalheSelecao(mapaSemMensagem);
        IllegalStateException missingMessageException = Assertions.assertThrows(
                IllegalStateException.class,
                configuredViewProjection::getErroAtualizacaoPorDetalheSelecao);
        Assertions.assertEquals(
                "ConfiguredViewProjection update error entry at index 0 requires error message.",
                missingMessageException.getMessage());

        Map<AjusteCelulaPlanningBook, String> mapaSemEscopo = Map.of(
                new AjusteCelulaPlanningBook(
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        "BASELINE",
                        "EA",
                        10.0,
                        0.0,
                        null),
                "erro");
        configuredViewProjection.setErroAtualizacaoPorDetalheSelecao(mapaSemEscopo);
        IllegalStateException missingScopeException = Assertions.assertThrows(
                IllegalStateException.class,
                configuredViewProjection::getErroAtualizacaoPorDetalheSelecao);
        Assertions.assertEquals(
                "ConfiguredViewProjection update error entry at index 0 requires material/location DFU scope.",
                missingScopeException.getMessage());

    }

    @Test
    public void updateErrorMapShouldAcceptCompleteErrorEntries() {

        ConfiguredViewProjection configuredViewProjection = new ConfiguredViewProjection();
        Map<AjusteCelulaPlanningBook, String> erroAtualizacaoPorDetalheSelecao = Map.of(
                getAjusteCelulaPlanningBookValido(),
                "valor invalido");
        configuredViewProjection.setErroAtualizacaoPorDetalheSelecao(erroAtualizacaoPorDetalheSelecao);

        Assertions.assertEquals(
                erroAtualizacaoPorDetalheSelecao,
                configuredViewProjection.getErroAtualizacaoPorDetalheSelecao());

    }

    private static AjusteCelulaPlanningBook getAjusteCelulaPlanningBookValido() {

        return new AjusteCelulaPlanningBook(
                LocalDateTime.of(2026, 1, 1, 0, 0),
                "BASELINE",
                "EA",
                10.0,
                0.0,
                PlanningBookDfuScope.deMaterialLocation(
                        new Produto("MAT_01", "Material 01"),
                        new Location("LOC_01", "Location 01")));

    }

}
