package com.opsfactor.community.capability.supplyplanning.productionplan.domain;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

/**
 * Contrato Community das linhas de plano de producao.
 *
 * <p>O Community mantem producao como parte do Supply Planning heuristico.
 * Quando uma linha de plano fica inconsistente com roteiro ou lista tecnica,
 * o erro precisa mostrar o identificador da dimensao divergente. Isso e
 * especialmente importante durante a migracao Community/Enterprise, pois o
 * diagnostico precisa separar claramente divergencias de location, material,
 * roteiro e BOM.</p>
 */
class ProductionPlanLinhaCommunityContractTest {

    @Test
    void shouldRejectProductionVersionWhenRoutingAndBillOfMaterialsOutputsDiffer() {

        Location location = new Location("PLANT");
        Produto materialLinha = new Produto("FG_LINE");
        Produto materialRoteiro = new Produto("FG_ROUTING");
        ListaTecnica listaTecnica = criaListaTecnica("BOM", location, materialLinha);
        Roteiro roteiro = criaRoteiro("ROUTING", location, materialRoteiro);
        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> criaProductionPlanLinha(
                        location,
                        materialLinha,
                        roteiro,
                        listaTecnica));

        Assertions.assertEquals(
                "Bill of Materials output material FG_LINE different than version material FG_ROUTING",
                illegalStateException.getMessage());

    }

    @Test
    void shouldRejectProductionVersionWhenBillOfMaterialsAndRoutingOutputsDiffer() {

        Location location = new Location("PLANT");
        Produto materialLinha = new Produto("FG_LINE");
        Produto materialListaTecnica = new Produto("FG_BOM");
        Roteiro roteiro = criaRoteiro("ROUTING", location, materialLinha);
        ListaTecnica listaTecnica = criaListaTecnica("BOM", location, materialListaTecnica);
        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> criaProductionPlanLinha(
                        location,
                        materialLinha,
                        roteiro,
                        listaTecnica));

        Assertions.assertEquals(
                "Bill of Materials output material FG_BOM different than version material FG_LINE",
                illegalStateException.getMessage());

    }

    @Test
    void productionQuantitiesShouldRejectNegativeRegisteredValues() {

        Location location = new Location("PLANT");
        Produto materialLinha = new Produto("FG_LINE");
        ListaTecnica listaTecnica = criaListaTecnica("BOM", location, materialLinha);
        Roteiro roteiro = criaRoteiro("ROUTING", location, materialLinha);
        ProductionPlanLinha productionPlanLinha = criaProductionPlanLinha(
                location,
                materialLinha,
                roteiro,
                listaTecnica);
        productionPlanLinha.setQuantidadeOrdemPlanejadaProducaoIrrestrita(-1.0d);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                productionPlanLinha::getQuantidadeOrdemPlanejadaProducaoIrrestrita);

        Assertions.assertEquals(
                "Production plan quantity unrestricted planned production must be finite and non-negative for "
                        + "material FG_LINE / location PLANT / reference date 2026-01-01T00:00: -1.0.",
                illegalStateException.getMessage());

    }

    @Test
    void productionQuantitiesShouldRejectNonFiniteRegisteredValues() {

        Location location = new Location("PLANT");
        Produto materialLinha = new Produto("FG_LINE");
        ListaTecnica listaTecnica = criaListaTecnica("BOM", location, materialLinha);
        Roteiro roteiro = criaRoteiro("ROUTING", location, materialLinha);
        ProductionPlanLinha productionPlanLinha = criaProductionPlanLinha(
                location,
                materialLinha,
                roteiro,
                listaTecnica);
        productionPlanLinha.setQuantidadeOrdemFirmeProducaoTrabalho(Double.NaN);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                productionPlanLinha::getQuantidadeOrdemFirmeProducaoTrabalho);

        Assertions.assertEquals(
                "Production plan quantity work firm production must be finite and non-negative for "
                        + "material FG_LINE / location PLANT / reference date 2026-01-01T00:00: NaN.",
                illegalStateException.getMessage());

    }

    private static ProductionPlanLinha criaProductionPlanLinha(
            Location location,
            Produto materialLinha,
            Roteiro roteiro,
            ListaTecnica listaTecnica) {

        SupplyPlan supplyPlan = new SupplyPlan();
        VersaoProducao versaoProducao =
                new VersaoProducao("PV", location, 1, roteiro, listaTecnica);
        ProductionPlanLinha.ProductionPlanLinhaCompositeKey productionPlanLinhaCompositeKey =
                new ProductionPlanLinha.ProductionPlanLinhaCompositeKey(
                        supplyPlan,
                        location,
                        versaoProducao,
                        roteiro,
                        listaTecnica,
                        LocalDateTime.of(2026, 1, 1, 0, 0));

        return new ProductionPlanLinha(productionPlanLinhaCompositeKey, materialLinha);

    }

    private static Roteiro criaRoteiro(String id, Location location, Produto material) {

        Roteiro roteiro = new Roteiro();
        roteiro.setId(id);
        roteiro.setLocation(location);
        roteiro.setMaterialOutput(material);

        return roteiro;

    }

    private static ListaTecnica criaListaTecnica(String id, Location location, Produto material) {

        ListaTecnica listaTecnica = new ListaTecnica();
        listaTecnica.setId(id);
        listaTecnica.setLocation(location);
        listaTecnica.setMaterialOutput(material);

        return listaTecnica;

    }

}
