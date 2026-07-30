package com.opsfactor.community.capability.supplyplanning.engine;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducaoSimples;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjectionCompleto;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

/**
 * Contratos Community das rotinas heuristicas de Supply Planning.
 *
 * <p>Estes testes cobrem bordas fail-fast que devem ocorrer antes de mutar a
 * projection em memoria. Assim mantemos claro quando o Community precisa de
 * master data operacional minima, sem cair em NPE ou mensagens genericas.</p>
 */
class SupplyPlanningCommunityContractTest {

    @Test
    void positiveProductionAdjustmentShouldRejectMissingSimpleProductionVersionBeforeWritingPlan() {

        Produto material = new Produto("MAT-01");
        Location location = new Location("LOC-01");
        SupplyPlanningProjection supplyPlanningProjection = new SupplyPlanningProjection(
                null,
                null,
                new FakeSupplyNetworkProjection(),
                null,
                null,
                location,
                new MaterialProjectionCompleto(),
                null);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> SupplyPlanning.modificaProducaoTotalMaterial(
                        10.0d,
                        supplyPlanningProjection,
                        0,
                        material,
                        Constantes.TipoPlano.PLANO_IRRESTRITO,
                        Constantes.FirmePlanejado.PLANEJADO,
                        null));

        Assertions.assertTrue(illegalStateException.getMessage().contains(
                "SupplyPlanning positive production adjustment requires a viable simple production version"));
        Assertions.assertTrue(illegalStateException.getMessage().contains("material=MAT-01"));
        Assertions.assertTrue(illegalStateException.getMessage().contains("location=LOC-01"));

    }

    @Test
    void safetyStockUpdateShouldRejectMissingCalculationModelBeforeUpdatingProjectedSafetyStock() {

        Produto material = new Produto("MAT-02");
        Location location = new Location("LOC-02");
        Calendario calendario = Calendario.criaCalendarioDeDatas(
                Constantes.TamanhoBucket.MENSAL,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0));
        SupplyPlanningProjection supplyPlanningProjection = new SupplyPlanningProjection(
                null,
                new PerfilExecucaoSupplyPlan(),
                new FakeSupplyNetworkProjection(),
                new FakePoliticaEstoquesProjection(),
                calendario,
                location,
                null,
                null);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> SupplyPlanning.atualizaEstoqueSeguranca(
                        supplyPlanningProjection,
                        material,
                        Constantes.TipoPlano.PLANO_IRRESTRITO));

        Assertions.assertTrue(illegalStateException.getMessage().contains(
                "SupplyPlanning heuristic safety stock calculation requires QUANTITY or DAYS"));
        Assertions.assertTrue(illegalStateException.getMessage().contains("calculation model=null"));
        Assertions.assertTrue(illegalStateException.getMessage().contains("material=MAT-02"));
        Assertions.assertTrue(illegalStateException.getMessage().contains("location=LOC-02"));

    }

    @Test
    void lotSegmentationShouldRejectProcessTimeGreaterThanShelfLifeAsInvalidMasterData() {

        Produto material = new Produto("MAT-03");
        Location location = new Location("LOC-03");
        Calendario calendario = Calendario.criaCalendarioDeDatas(
                Constantes.TamanhoBucket.MENSAL,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0));

        /*
         * Aging por lote e shelf-life sao capabilities Enterprise, mas a
         * rotina matematica fica no Community porque tambem e util ao overlay.
         * Quando o cadastro privado informa tempo de processo maior que o
         * prazo de validade, a falha e dado mestre invalido, nao capability
         * ausente.
         */
        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> SupplyPlanning.getNumeroSegmentosLotes(
                        location,
                        material,
                        calendario,
                        new FakeClusterEParametrosProjectionComLotesInvalidos()));

        Assertions.assertEquals(
                "Process Time cannot be larger than Shelf Life for Material MAT-03 / Location LOC-03",
                illegalArgumentException.getMessage());

    }

    @Test
    void supplyPlanningSourceShouldNotKeepLegacyHeuristicImplementationBlocksCommented() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path supplyPlanningSourcePath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/supplyplanning/engine/SupplyPlanning.java");
        String supplyPlanningSource = Files.readString(
                supplyPlanningSourcePath,
                StandardCharsets.UTF_8);

        /*
         * `SupplyPlanning` e o miolo heuristico Community. Comentarios de regra
         * sao importantes aqui, mas blocos antigos de implementacao escondem se
         * a heuristica atual usa snapshot de estoque, horizonte dinamico ou
         * calculo alternativo de safety stock.
         */
        Assertions.assertFalse(
                supplyPlanningSource.contains("//        float valorTotalOriginal")
                        || supplyPlanningSource.contains("//        int ultimoPeriodoConsiderado")
                        || supplyPlanningSource.contains("//            float frequenciaRessuprimento")
                        || supplyPlanningSource.contains("//        float estoqueAtual")
                        || supplyPlanningSource.contains("//                modificaProducaoPlanejadaTotalMaterial"),
                "SupplyPlanning nao deve manter blocos legados de implementacao comentados.");

    }

    private Path resolveCommunityWorkspaceDirectory() {

        Path currentDirectory = Paths.get("").toAbsolutePath().normalize();

        if ("community".equals(currentDirectory.getFileName().toString())
                || currentDirectory.getFileName().toString().startsWith("community-")) {
            return currentDirectory.getParent();
        }

        return currentDirectory;

    }

    private static class FakeSupplyNetworkProjection extends SupplyNetworkProjection {

        @Override
        public UnidadeMedidaProjection getConversaoUnidadeMedidaProjection() {

            return null;

        }

        @Override
        public ClusterEParametrosProjection getClusterEParametrosProjection() {

            return new FakeClusterEParametrosProjection();

        }

        @Override
        public Optional<VersaoProducaoSimples> getVersaoProducaoSimplesViavelPrioritaria(
                Location location,
                Produto material,
                Collection<Produto> possiveisMateriaisInput) {

            return Optional.empty();

        }

    }

    private static class FakeClusterEParametrosProjection extends ClusterEParametrosProjection {

        @Override
        public UnidadeMedida getSNPUnidadeMedidaPadrao(
                Produto material,
                Location location) {

            return null;

        }

    }

    private static class FakeClusterEParametrosProjectionComLotesInvalidos extends ClusterEParametrosProjection {

        @Override
        public Optional<Integer> getPrazoValidadeEmPeriodos(
                Location location,
                Produto material,
                Calendario calendario) {

            return Optional.of(2);

        }

        @Override
        public Optional<Integer> getTempoProcessoEmPeriodos(
                Location location,
                Produto material,
                Calendario calendario) {

            return Optional.of(3);

        }

    }

    private static class FakePoliticaEstoquesProjection extends PoliticaEstoquesProjection {

        @Override
        public Constantes.SNPModeloReabastecimento getSNPModeloReabastecimento(
                int posicaoPeriodo,
                Produto material,
                Location location) {

            return Constantes.SNPModeloReabastecimento.DRP;

        }

        @Override
        public double getSNPEstoqueSegurancaDrpOuTargetKanban(
                int posicaoPeriodo,
                Produto material,
                Location location) {

            return 1.0d;

        }

        @Override
        public double getSNPEstoqueMaximoDrp(
                int posicaoPeriodo,
                Produto material,
                Location location) {

            return 1.0d;

        }

        @Override
        public Constantes.SNPCalculoSafetyStock getSNPModeloCalculoSafetyStock(
                int posicaoPeriodo,
                Produto material,
                Location location) {

            return null;

        }

    }

}
