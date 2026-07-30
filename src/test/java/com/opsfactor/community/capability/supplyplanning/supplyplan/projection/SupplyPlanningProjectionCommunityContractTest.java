package com.opsfactor.community.capability.supplyplanning.supplyplan.projection;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducaoInexistente;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducaoSimples;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.domain.InventoryPlanLinha;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjectionCompleto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

/**
 * Contratos Community da projection em memoria do Supply Planning.
 *
 * <p>A projection indexa linhas por versoes reais de producao. A sentinela
 * `VersaoProducaoInexistente` pode existir em cadastros operacionais, mas nao
 * deve ser usada como chave dos mapas de production plan.</p>
 */
class SupplyPlanningProjectionCommunityContractTest {

    @Test
    void locationExecutionPolicyShouldDefaultToParentProfileAndAcceptResolvedTypedValues() {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setGeraRequisicoesInbound(false);
        perfilExecucaoSupplyPlan.setGeraOrdensProducaoPlanejadas(true);
        perfilExecucaoSupplyPlan.setTrataPoliticaEstoqueComoDrp(false);

        SupplyPlanningProjection supplyPlanningProjection = new SupplyPlanningProjection(
                null,
                perfilExecucaoSupplyPlan,
                new FakeSupplyNetworkProjection(),
                null,
                null,
                new Location("LOC-POLICY"),
                null,
                null);

        Assertions.assertFalse(supplyPlanningProjection.isGenerateInbound());
        Assertions.assertTrue(supplyPlanningProjection.isGeneratePlannedProductionOrder());
        Assertions.assertFalse(supplyPlanningProjection.isTreatPolicyAsDrp());

        supplyPlanningProjection.configuraPoliticaExecucaoLocation(true, false, true);

        Assertions.assertTrue(supplyPlanningProjection.isGenerateInbound());
        Assertions.assertFalse(supplyPlanningProjection.isGeneratePlannedProductionOrder());
        Assertions.assertTrue(supplyPlanningProjection.isTreatPolicyAsDrp());

    }

    @Test
    void productionPlanOutputIndexShouldRejectMissingProductionVersionBeforeMapAccess() {

        SupplyPlanningProjection supplyPlanningProjection = getSupplyPlanningProjection();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanningProjection.getProductionPlanLinhaOutput(0, (VersaoProducao) null));

        Assertions.assertTrue(illegalArgumentException.getMessage().contains(
                "SupplyPlanningProjection indexes production plan lines only by real production versions"));
        Assertions.assertTrue(illegalArgumentException.getMessage().contains("received null"));

    }

    @Test
    void productionPlanInputIndexShouldRejectSentinelProductionVersionBeforeMapAccess() {

        SupplyPlanningProjection supplyPlanningProjection = getSupplyPlanningProjection();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanningProjection.getProductionPlanLinhaInput(
                        0,
                        new VersaoProducaoInexistente()));

        Assertions.assertTrue(illegalArgumentException.getMessage().contains(
                "SupplyPlanningProjection indexes production plan lines only by real production versions"));
        Assertions.assertTrue(illegalArgumentException.getMessage().contains(
                "VersaoProducaoInexistente(DEFAULT_PRODUCTION_VERSION)"));

    }

    @Test
    void productionPlanWriteByRoutingAndBomShouldRejectMissingViableProductionVersion() {

        Produto material = new Produto("MAT-01");
        Location location = new Location("LOC-01");
        Roteiro roteiro = getRoteiro("ROUTING-01", location, material);
        ListaTecnica listaTecnica = getListaTecnica("BOM-01", location, material);
        SupplyPlanningProjection supplyPlanningProjection = getSupplyPlanningProjectionWithCompleteMaterialScope();

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> supplyPlanningProjection.setQuantidadeProductionPlan(
                        0,
                        roteiro,
                        listaTecnica,
                        10.0d,
                        Constantes.FirmePlanejado.PLANEJADO,
                        Constantes.TipoPlano.PLANO_IRRESTRITO,
                        null));

        Assertions.assertTrue(illegalStateException.getMessage().contains(
                "SupplyPlanningProjection requires a viable production version before writing production plan"));
        Assertions.assertTrue(illegalStateException.getMessage().contains("routing=ROUTING-01"));
        Assertions.assertTrue(illegalStateException.getMessage().contains("bom=BOM-01"));
        Assertions.assertTrue(illegalStateException.getMessage().contains("location=LOC-01"));
        Assertions.assertTrue(illegalStateException.getMessage().contains("output material=MAT-01"));

    }

    @Test
    void directSupplyLineIndexingShouldRejectNullLinesBeforeMapAccess() {

        SupplyPlanningProjection supplyPlanningProjection = getSupplyPlanningProjection();

        assertIllegalArgumentMessage(
                () -> supplyPlanningProjection.addDistributionPlanItemInbound(null),
                "SupplyPlanningProjection cannot index null Distribution Plan line during inbound Distribution Plan indexing.");
        assertIllegalArgumentMessage(
                () -> supplyPlanningProjection.addDistributionPlanItemOutbound(null),
                "SupplyPlanningProjection cannot index null Distribution Plan line during outbound Distribution Plan indexing.");
        assertIllegalArgumentMessage(
                () -> supplyPlanningProjection.addProductionPlanLinhaOutput(null),
                "SupplyPlanningProjection cannot index null Production Plan line during Production Plan output indexing.");
        assertIllegalArgumentMessage(
                () -> supplyPlanningProjection.addProductionPlanLinhaInput(null),
                "SupplyPlanningProjection cannot index null Production Plan line during Production Plan input indexing.");
        assertIllegalArgumentMessage(
                () -> supplyPlanningProjection.addInventoryPlanLinha(null),
                "SupplyPlanningProjection cannot index null Inventory Plan line during Inventory Plan indexing.");

    }

    @Test
    void directSupplyLineIndexingShouldRejectBrokenKeysBeforeCalendarAccess() {

        SupplyPlanningProjection supplyPlanningProjection = getSupplyPlanningProjection();

        assertIllegalArgumentMessage(
                () -> supplyPlanningProjection.addDistributionPlanItemInbound(new DistributionPlanItem()),
                "SupplyPlanningProjection requires Distribution Plan line with supply plan, origin, destination, material, shipping date and receiving date before inbound Distribution Plan indexing.");
        assertIllegalArgumentMessage(
                () -> supplyPlanningProjection.addDistributionPlanItemOutbound(new DistributionPlanItem()),
                "SupplyPlanningProjection requires Distribution Plan line with supply plan, origin, destination, material, shipping date and receiving date before outbound Distribution Plan indexing.");
        assertIllegalArgumentMessage(
                () -> supplyPlanningProjection.addProductionPlanLinhaOutput(new ProductionPlanLinha()),
                "SupplyPlanningProjection requires Production Plan line with supply plan, location, output material, production version, routing, bill of materials and reference date before Production Plan output indexing.");
        assertIllegalArgumentMessage(
                () -> supplyPlanningProjection.addProductionPlanLinhaInput(new ProductionPlanLinha()),
                "SupplyPlanningProjection requires Production Plan line with supply plan, location, output material, production version, routing, bill of materials and reference date before Production Plan input indexing.");
        assertIllegalArgumentMessage(
                () -> supplyPlanningProjection.addInventoryPlanLinha(new InventoryPlanLinha()),
                "SupplyPlanningProjection requires Inventory Plan line with supply plan, location, material and reference date before Inventory Plan indexing.");

    }

    @Test
    void directSupplyLineIndexingShouldRejectMissingProjectionCalendarAfterLineValidation() {

        SupplyPlanningProjection supplyPlanningProjection = getSupplyPlanningProjection();
        SupplyPlan supplyPlan = new SupplyPlan();
        Produto material = new Produto("MAT-01");
        Location locationDestino = new Location("LOC-DEST");
        Location locationOrigem = new Location("LOC-ORIG");
        LocalDateTime dataReferencia = LocalDateTime.of(2026, 7, 3, 0, 0);
        Roteiro roteiro = getRoteiro("ROUTING-01", locationDestino, material);
        ListaTecnica listaTecnica = getListaTecnica("BOM-01", locationDestino, material);

        assertIllegalStateMessage(
                () -> supplyPlanningProjection.addDistributionPlanItemInbound(new DistributionPlanItem(
                        new DistributionPlanItem.DistributionPlanItemKey(
                                supplyPlan,
                                locationDestino,
                                locationOrigem,
                                material,
                                dataReferencia,
                                dataReferencia))),
                "SupplyPlanningProjection requires calendar before inbound Distribution Plan indexing.");
        assertIllegalStateMessage(
                () -> supplyPlanningProjection.addProductionPlanLinhaOutput(new ProductionPlanLinha(
                        new ProductionPlanLinha.ProductionPlanLinhaCompositeKey(
                                supplyPlan,
                                locationDestino,
                                new VersaoProducaoInexistente(),
                                roteiro,
                                listaTecnica,
                                dataReferencia),
                        material)),
                "SupplyPlanningProjection requires calendar before Production Plan output indexing.");
        assertIllegalStateMessage(
                () -> supplyPlanningProjection.addInventoryPlanLinha(new InventoryPlanLinha(
                        new InventoryPlanLinha.InventoryPlanLinhaCompositeKey(
                                supplyPlan,
                                locationDestino,
                                material,
                                dataReferencia))),
                "SupplyPlanningProjection requires calendar before Inventory Plan indexing.");

    }

    @Test
    void multiLocationProjectionShouldRejectMissingSupplyNetworkProjectionBeforeConstructorAccess() {

        assertIllegalArgumentMessage(
                () -> new SupplyPlanningMultiplasLocationsProjection(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null),
                "SupplyPlanningMultiplasLocationsProjection requires Supply Network projection.");

    }

    @Test
    void multiLocationProjectionShouldRejectNullLocalProjectionBeforeMapAccess() {

        SupplyPlanningMultiplasLocationsProjection supplyPlanningMultiplasLocationsProjection =
                getSupplyPlanningMultiplasLocationsProjection();

        assertIllegalArgumentMessage(
                () -> supplyPlanningMultiplasLocationsProjection.addSupplyPlanningProjection(null),
                "SupplyPlanningMultiplasLocationsProjection cannot index null Supply Planning projection.");

    }

    @Test
    void multiLocationProjectionShouldRejectLocalProjectionWithoutLocationBeforeIndexing() {

        SupplyPlanningMultiplasLocationsProjection supplyPlanningMultiplasLocationsProjection =
                getSupplyPlanningMultiplasLocationsProjection();

        assertIllegalArgumentMessage(
                () -> supplyPlanningMultiplasLocationsProjection.addSupplyPlanningProjection(
                        getSupplyPlanningProjection()),
                "SupplyPlanningMultiplasLocationsProjection requires local Supply Planning projection with location.");

    }

    @Test
    void multiLocationProjectionShouldRejectDuplicatedLocationBeforeOverwrite() {

        Location location = new Location("LOC-DUP");
        SupplyPlanningMultiplasLocationsProjection supplyPlanningMultiplasLocationsProjection =
                getSupplyPlanningMultiplasLocationsProjection();

        supplyPlanningMultiplasLocationsProjection.addSupplyPlanningProjection(
                getSupplyPlanningProjection(location));

        assertIllegalArgumentMessage(
                () -> supplyPlanningMultiplasLocationsProjection.addSupplyPlanningProjection(
                        getSupplyPlanningProjection(location)),
                "SupplyPlanningMultiplasLocationsProjection received duplicated Supply Planning projection for location LOC-DUP.");

    }

    private static SupplyPlanningProjection getSupplyPlanningProjection() {

        return new SupplyPlanningProjection(
                null,
                null,
                new FakeSupplyNetworkProjection(),
                null,
                null,
                null,
                null,
                null);

    }

    private static SupplyPlanningProjection getSupplyPlanningProjection(Location location) {

        return new SupplyPlanningProjection(
                null,
                null,
                new FakeSupplyNetworkProjection(),
                null,
                null,
                location,
                null,
                null);

    }

    private static SupplyPlanningMultiplasLocationsProjection getSupplyPlanningMultiplasLocationsProjection() {

        return new SupplyPlanningMultiplasLocationsProjection(
                null,
                null,
                new FakeSupplyNetworkProjection(),
                null,
                null,
                null);

    }

    private static SupplyPlanningProjection getSupplyPlanningProjectionWithCompleteMaterialScope() {

        return new SupplyPlanningProjection(
                null,
                null,
                new FakeSupplyNetworkProjection(),
                null,
                null,
                null,
                new MaterialProjectionCompleto(),
                null);

    }

    private static Roteiro getRoteiro(
            String id,
            Location location,
            Produto material) {

        Roteiro roteiro = new Roteiro();
        roteiro.setId(id);
        roteiro.setLocation(location);
        roteiro.setMaterialOutput(material);
        return roteiro;

    }

    private static ListaTecnica getListaTecnica(
            String id,
            Location location,
            Produto material) {

        ListaTecnica listaTecnica = new ListaTecnica();
        listaTecnica.setId(id);
        listaTecnica.setLocation(location);
        listaTecnica.setMaterialOutput(material);
        return listaTecnica;

    }

    private static void assertIllegalArgumentMessage(
            Executable executable,
            String expectedMessage) {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                executable);

        Assertions.assertEquals(expectedMessage, illegalArgumentException.getMessage());

    }

    private static void assertIllegalStateMessage(
            Executable executable,
            String expectedMessage) {

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                executable);

        Assertions.assertEquals(expectedMessage, illegalStateException.getMessage());

    }

    private static class FakeSupplyNetworkProjection extends SupplyNetworkProjection {

        @Override
        public UnidadeMedidaProjection getConversaoUnidadeMedidaProjection() {

            return null;

        }

        @Override
        public Optional<VersaoProducaoSimples> getVersaoProducaoSimplesViavelPrioritaria(
                Roteiro roteiro,
                ListaTecnica listaTecnica) {

            return Optional.empty();

        }

        @Override
        public Optional<VersaoProducao> getVersaoProducaoViavelPrioritaria(
                Location location,
                Produto material,
                boolean consideraVersoesProducaoParalelas,
                Collection<Produto> possiveisMateriaisInput) {

            return Optional.empty();

        }

    }

}
