package com.opsfactor.community.capability.supplyplanning.supplyplan.service;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.DFU;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardEnum;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Contratos Community do service que persiste ajustes do Supply Planning Book.
 *
 * <p>A validacao principal fica na borda de front, mas este service tambem
 * precisa se proteger contra chamadas internas ou payloads transicionais que
 * tragam key figures calculadas ou Enterprise. O erro deve acontecer antes de
 * montar projection e antes de qualquer tentativa de persistencia.</p>
 */
class SupplyPlanningModificacoesServiceCommunityContractTest {

    @Test
    void modificaSupplyPlanShouldRejectCalculatedKeyFigureBeforeProjectionAccess() {

        SupplyPlanningModificacoesService supplyPlanningModificacoesService =
                new SupplyPlanningModificacoesService();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanningModificacoesService.modificaSupplyPlan(
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        KeyFigureStandardEnum.DEMANDA_TOTAL,
                        10.0,
                        8.0,
                        null,
                        null,
                        0,
                        Set.of(new Produto("MAT"))));

        Assertions.assertTrue(
                illegalArgumentException.getMessage().contains(
                        "SupplyPlanningModificacoesService can modify only Community operational Supply Planning Book key figures"));
        Assertions.assertTrue(
                illegalArgumentException.getMessage().contains("received DEMANDA_TOTAL"));
        Assertions.assertTrue(
                illegalArgumentException.getMessage().contains("must be blocked before persistence"));

    }

    @Test
    void modificaSupplyPlanShouldRejectAggregatedStockDaysBeforeProjectionAccess() {

        SupplyPlanningModificacoesService supplyPlanningModificacoesService =
                new SupplyPlanningModificacoesService();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanningModificacoesService.modificaSupplyPlan(
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        KeyFigureStandardEnum.ESTOQUE_DIAS,
                        10.0,
                        8.0,
                        null,
                        null,
                        0,
                        Set.of(
                                new Produto("MAT-1"),
                                new Produto("MAT-2"))));

        Assertions.assertTrue(
                illegalArgumentException.getMessage().contains(
                        "SupplyPlanningModificacoesService can modify Stock Days for exactly one material/location at a time"));
        Assertions.assertTrue(
                illegalArgumentException.getMessage().contains("received 2 materials"));
        Assertions.assertTrue(
                illegalArgumentException.getMessage().contains("before projection loading"));

    }

    @Test
    void modificaSupplyPlanShouldRejectMissingRequiredInputsBeforeProjectionAccess() {

        SupplyPlanningModificacoesService supplyPlanningModificacoesService =
                new SupplyPlanningModificacoesService();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");

        IllegalArgumentException tipoPlanoAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanningModificacoesService.modificaSupplyPlan(
                        null,
                        KeyFigureStandardEnum.ESTOQUE,
                        10.0,
                        8.0,
                        unidadeMedida,
                        null,
                        0,
                        Set.of(new Produto("MAT"))));
        IllegalArgumentException unidadeMedidaAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanningModificacoesService.modificaSupplyPlan(
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        KeyFigureStandardEnum.ESTOQUE,
                        10.0,
                        8.0,
                        null,
                        null,
                        0,
                        Set.of(new Produto("MAT"))));
        IllegalArgumentException projectionAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanningModificacoesService.modificaSupplyPlan(
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        KeyFigureStandardEnum.ESTOQUE,
                        10.0,
                        8.0,
                        unidadeMedida,
                        null,
                        0,
                        Set.of(new Produto("MAT"))));

        Assertions.assertEquals(
                "Supply Planning target plan type is required for Community Planning Book modification.",
                tipoPlanoAusenteException.getMessage());
        Assertions.assertEquals(
                "Unit of measure is required for Community Supply Planning Book modification of ESTOQUE.",
                unidadeMedidaAusenteException.getMessage());
        Assertions.assertEquals(
                "Supply Planning projection is required for Community Planning Book modification.",
                projectionAusenteException.getMessage());

    }

    @Test
    void modificaSupplyPlanShouldRejectBrokenMaterialSetBeforeProjectionAccess() {

        SupplyPlanningModificacoesService supplyPlanningModificacoesService =
                new SupplyPlanningModificacoesService();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        Set<Produto> materiaisComItemNulo = new LinkedHashSet<>();
        Set<Produto> materiaisComIdAusente = new LinkedHashSet<>();
        materiaisComItemNulo.add(new Produto("MAT"));
        materiaisComItemNulo.add(null);
        materiaisComIdAusente.add(new Produto());

        IllegalArgumentException materiaisAusentesException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanningModificacoesService.modificaSupplyPlan(
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        KeyFigureStandardEnum.ESTOQUE,
                        10.0,
                        8.0,
                        unidadeMedida,
                        null,
                        0,
                        null));
        IllegalArgumentException materiaisVaziosException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanningModificacoesService.modificaSupplyPlan(
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        KeyFigureStandardEnum.ESTOQUE,
                        10.0,
                        8.0,
                        unidadeMedida,
                        null,
                        0,
                        Set.of()));
        IllegalArgumentException materialNuloException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanningModificacoesService.modificaSupplyPlan(
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        KeyFigureStandardEnum.ESTOQUE,
                        10.0,
                        8.0,
                        unidadeMedida,
                        null,
                        0,
                        materiaisComItemNulo));
        IllegalArgumentException materialSemIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanningModificacoesService.modificaSupplyPlan(
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        KeyFigureStandardEnum.ESTOQUE,
                        10.0,
                        8.0,
                        unidadeMedida,
                        null,
                        0,
                        materiaisComIdAusente));

        Assertions.assertEquals(
                "Modified material set is required for Community Supply Planning Book modification.",
                materiaisAusentesException.getMessage());
        Assertions.assertEquals(
                "At least one modified material is required for Community Supply Planning Book modification.",
                materiaisVaziosException.getMessage());
        Assertions.assertEquals(
                "Modified material at index 1 is required for Community Supply Planning Book modification.",
                materialNuloException.getMessage());
        Assertions.assertEquals(
                "Modified material at index 0 must have an id for Community Supply Planning Book modification.",
                materialSemIdException.getMessage());

    }

    @Test
    void atualizaDependentesShouldRejectBrokenOriginSetsBeforeProjectionAccess() {

        SupplyPlanningModificacoesService supplyPlanningModificacoesService =
                new SupplyPlanningModificacoesService();
        Set<Location> locationsComItemNulo = new LinkedHashSet<>();
        Set<Location> locationsComIdAusente = new LinkedHashSet<>();
        locationsComItemNulo.add(new Location("LOC"));
        locationsComItemNulo.add(null);
        locationsComIdAusente.add(new Location());
        Set<Produto> materiaisComItemNulo = new LinkedHashSet<>();
        Set<Produto> materiaisComIdAusente = new LinkedHashSet<>();
        materiaisComItemNulo.add(new Produto("MAT"));
        materiaisComItemNulo.add(null);
        materiaisComIdAusente.add(new Produto());

        IllegalArgumentException locationsAusentesException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanningModificacoesService.atualizaESalvaInventoryPlanLinhasDeDFUsDependentes(
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        (Set<Location>) null,
                        Set.of(new Produto("MAT")),
                        null,
                        null,
                        null,
                        null,
                        false,
                        true,
                        true));
        IllegalArgumentException locationNulaException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanningModificacoesService.atualizaESalvaInventoryPlanLinhasDeDFUsDependentes(
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        locationsComItemNulo,
                        Set.of(new Produto("MAT")),
                        null,
                        null,
                        null,
                        null,
                        false,
                        true,
                        true));
        IllegalArgumentException materialNuloException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanningModificacoesService.atualizaESalvaInventoryPlanLinhasDeDFUsDependentes(
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        Set.of(new Location("LOC")),
                        materiaisComItemNulo,
                        null,
                        null,
                        null,
                        null,
                        false,
                        true,
                        true));
        IllegalArgumentException locationSemIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanningModificacoesService.atualizaESalvaInventoryPlanLinhasDeDFUsDependentes(
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        locationsComIdAusente,
                        Set.of(new Produto("MAT")),
                        null,
                        null,
                        null,
                        null,
                        false,
                        true,
                        true));
        IllegalArgumentException materialSemIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanningModificacoesService.atualizaESalvaInventoryPlanLinhasDeDFUsDependentes(
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        Set.of(new Location("LOC")),
                        materiaisComIdAusente,
                        null,
                        null,
                        null,
                        null,
                        false,
                        true,
                        true));

        Assertions.assertDoesNotThrow(
                () -> supplyPlanningModificacoesService.atualizaESalvaInventoryPlanLinhasDeDFUsDependentes(
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        Set.of(),
                        Set.of(new Produto("MAT")),
                        null,
                        null,
                        null,
                        null,
                        false,
                        true,
                        true));
        Assertions.assertEquals(
                "Dependent Inventory Plan origin location set is required for Community Planning Book recalculation.",
                locationsAusentesException.getMessage());
        Assertions.assertEquals(
                "Dependent Inventory Plan origin location at index 1 is required for Community Planning Book recalculation.",
                locationNulaException.getMessage());
        Assertions.assertEquals(
                "Dependent Inventory Plan origin material at index 1 is required for Community Planning Book recalculation.",
                materialNuloException.getMessage());
        Assertions.assertEquals(
                "Dependent Inventory Plan origin location at index 0 must have an id for Community Planning Book recalculation.",
                locationSemIdException.getMessage());
        Assertions.assertEquals(
                "Dependent Inventory Plan origin material at index 0 must have an id for Community Planning Book recalculation.",
                materialSemIdException.getMessage());

    }

    @Test
    void atualizaInventoryPlanLinhasDeDFUsShouldRejectBrokenDFUSetBeforeProjectionAccess() {

        SupplyPlanningModificacoesService supplyPlanningModificacoesService =
                new SupplyPlanningModificacoesService();
        Set<DFU> dfusComItemNulo = new LinkedHashSet<>();
        Set<DFU> dfusComMaterialSemId = new LinkedHashSet<>();
        dfusComItemNulo.add(new DFU(new Produto("MAT"), new Location("LOC")));
        dfusComItemNulo.add(null);
        dfusComMaterialSemId.add(new DFU(new Produto(), new Location("LOC")));

        IllegalArgumentException dfusAusentesException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanningModificacoesService.atualizaESalvaInventoryPlanLinhasDeDFUs(
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false));
        IllegalArgumentException dfuNulaException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanningModificacoesService.atualizaESalvaInventoryPlanLinhasDeDFUs(
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        dfusComItemNulo,
                        null,
                        null,
                        null,
                        null,
                        false));
        IllegalArgumentException dfuSemIdsException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanningModificacoesService.atualizaESalvaInventoryPlanLinhasDeDFUs(
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        dfusComMaterialSemId,
                        null,
                        null,
                        null,
                        null,
                        false));

        Assertions.assertDoesNotThrow(
                () -> supplyPlanningModificacoesService.atualizaESalvaInventoryPlanLinhasDeDFUs(
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        Set.of(),
                        null,
                        null,
                        null,
                        null,
                        false));
        Assertions.assertEquals(
                "Dependent DFU set is required for Community Inventory Plan recalculation.",
                dfusAusentesException.getMessage());
        Assertions.assertEquals(
                "Dependent DFU at index 1 is required for Community Inventory Plan recalculation.",
                dfuNulaException.getMessage());
        Assertions.assertEquals(
                "Dependent DFU at index 0 must have material and location ids for Community Inventory Plan recalculation.",
                dfuSemIdsException.getMessage());

    }

    @Test
    void productionResourceAdjustmentShouldRejectMissingViableProductionVersionBeforePersistence() {

        Produto material = new Produto("MAT-RESOURCE");
        Location location = new Location("LOC-RESOURCE");
        RecursoProdutivo recursoProdutivo = new RecursoProdutivo();
        recursoProdutivo.setId("RESOURCE-01");
        SupplyPlanningProjection supplyPlanningProjection = getSupplyPlanningProjectionForProductionResourceAdjustment(
                location);
        SupplyPlanningModificacoesService supplyPlanningModificacoesService =
                new SupplyPlanningModificacoesService();

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> supplyPlanningModificacoesService.modificaProductionPlanParaRecursoProdutivo(
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        10.0d,
                        new UnidadeMedida("UN"),
                        supplyPlanningProjection,
                        new PoliticaEstoquesProjection(),
                        0,
                        recursoProdutivo,
                        material));

        Assertions.assertTrue(illegalStateException.getMessage().contains(
                "SupplyPlanningModificacoesService requires a viable simple production version"));
        Assertions.assertTrue(illegalStateException.getMessage().contains("material=MAT-RESOURCE"));
        Assertions.assertTrue(illegalStateException.getMessage().contains("location=LOC-RESOURCE"));
        Assertions.assertTrue(illegalStateException.getMessage().contains("productive resource=RESOURCE-01"));

    }

    @Test
    void productionResourceAdjustmentShouldRejectBrokenInputsBeforeProjectionUse() {

        Location location = new Location("LOC-RESOURCE");
        Produto material = new Produto("MAT-RESOURCE");
        RecursoProdutivo recursoProdutivo = new RecursoProdutivo();
        recursoProdutivo.setId("RESOURCE-01");
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        SupplyPlanningProjection supplyPlanningProjection = getSupplyPlanningProjectionForProductionResourceAdjustment(
                location);
        SupplyPlanningProjection supplyPlanningProjectionSemSupplyPlan = new SupplyPlanningProjection(
                null,
                new PerfilExecucaoSupplyPlan(),
                new FakeSupplyNetworkProjection(),
                null,
                null,
                location,
                null,
                null);
        SupplyPlanningModificacoesService supplyPlanningModificacoesService =
                new SupplyPlanningModificacoesService();

        assertIllegalArgumentMessage(
                () -> supplyPlanningModificacoesService.modificaProductionPlanParaRecursoProdutivo(
                        null,
                        10.0d,
                        unidadeMedida,
                        supplyPlanningProjection,
                        new PoliticaEstoquesProjection(),
                        0,
                        recursoProdutivo,
                        material),
                "Supply Planning target plan type is required for Community production resource adjustment.");
        assertIllegalArgumentMessage(
                () -> supplyPlanningModificacoesService.modificaProductionPlanParaRecursoProdutivo(
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        10.0d,
                        null,
                        supplyPlanningProjection,
                        new PoliticaEstoquesProjection(),
                        0,
                        recursoProdutivo,
                        material),
                "Unit of measure is required for Community production resource adjustment.");
        assertIllegalArgumentMessage(
                () -> supplyPlanningModificacoesService.modificaProductionPlanParaRecursoProdutivo(
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        10.0d,
                        unidadeMedida,
                        null,
                        new PoliticaEstoquesProjection(),
                        0,
                        recursoProdutivo,
                        material),
                "Supply Planning projection is required for Community production resource adjustment.");
        assertIllegalArgumentMessage(
                () -> supplyPlanningModificacoesService.modificaProductionPlanParaRecursoProdutivo(
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        10.0d,
                        unidadeMedida,
                        supplyPlanningProjection,
                        null,
                        0,
                        recursoProdutivo,
                        material),
                "Inventory policy projection is required for Community production resource adjustment.");
        assertIllegalArgumentMessage(
                () -> supplyPlanningModificacoesService.modificaProductionPlanParaRecursoProdutivo(
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        10.0d,
                        unidadeMedida,
                        supplyPlanningProjection,
                        new PoliticaEstoquesProjection(),
                        -1,
                        recursoProdutivo,
                        material),
                "Supply Planning modification period position must be non-negative for Community production resource adjustment.");
        assertIllegalArgumentMessage(
                () -> supplyPlanningModificacoesService.modificaProductionPlanParaRecursoProdutivo(
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        10.0d,
                        unidadeMedida,
                        supplyPlanningProjection,
                        new PoliticaEstoquesProjection(),
                        0,
                        new RecursoProdutivo(),
                        material),
                "Productive resource with id is required for Community production resource adjustment.");
        assertIllegalArgumentMessage(
                () -> supplyPlanningModificacoesService.modificaProductionPlanParaRecursoProdutivo(
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        10.0d,
                        unidadeMedida,
                        supplyPlanningProjection,
                        new PoliticaEstoquesProjection(),
                        0,
                        recursoProdutivo,
                        new Produto()),
                "Material with id is required for Community production resource adjustment.");
        assertIllegalArgumentMessage(
                () -> supplyPlanningModificacoesService.modificaProductionPlanParaRecursoProdutivo(
                        Constantes.TipoPlano.PLANO_TRABALHO,
                        10.0d,
                        unidadeMedida,
                        supplyPlanningProjectionSemSupplyPlan,
                        new PoliticaEstoquesProjection(),
                        0,
                        recursoProdutivo,
                        material),
                "Persisted Supply Plan with id is required for Community production resource adjustment.");

    }

    private static SupplyPlanningProjection getSupplyPlanningProjectionForProductionResourceAdjustment(
            Location location) {

        SupplyPlan supplyPlan = new SupplyPlan();
        supplyPlan.setId(42L);

        return new SupplyPlanningProjection(
                supplyPlan,
                new PerfilExecucaoSupplyPlan(),
                new FakeSupplyNetworkProjection(),
                null,
                null,
                location,
                null,
                null);

    }

    private static void assertIllegalArgumentMessage(
            org.junit.jupiter.api.function.Executable executable,
            String expectedMessage) {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                executable);

        Assertions.assertEquals(expectedMessage, illegalArgumentException.getMessage());

    }

    private static class FakeSupplyNetworkProjection extends SupplyNetworkProjection {

        @Override
        public UnidadeMedidaProjection getConversaoUnidadeMedidaProjection() {

            return new UnidadeMedidaProjection();

        }

        @Override
        public ClusterEParametrosProjection getClusterEParametrosProjection() {

            return new FakeClusterEParametrosProjection();

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

    private static class FakeClusterEParametrosProjection extends ClusterEParametrosProjection {

        private FakeClusterEParametrosProjection() {

            parametrosGlobais = new ParametrosGlobais();

        }

    }

}
