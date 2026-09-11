package com.opsfactor.community.capability.masterdata.network.supplynetwork.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Regressão do Production Overview ao recarregar produção sem versão cadastrada. */
class ProductionPlanUnversionedOutputReferenceTest {

    @Test
    void persistedSentinelAndNullVersionUseTheSavedMastersWithoutInventingAProductionVersion() {

        var fixture = new Fixture();
        VersaoProducao sentinela = VersaoProducao.criaVersaoProducaoInexistente();
        assertTrue(fixture.linha(sentinela).representaConsumoCompartilhadoDoPacote(fixture.projection));
        assertTrue(fixture.linha(null).representaConsumoCompartilhadoDoPacote(fixture.projection));
        assertNull(sentinela.getListaTecnica(), "A leitura não deve transformar a sentinela em versão produtiva");

        // Os índices de versões não foram sequer inicializados: a leitura usa
        // exclusivamente os IDs de roteiro e BOM efetivamente salvos na linha.
        assertNull(fixture.projection.mapaVersaoProducaoPorId);

    }

    @Test
    void realAndInMemoryTemporaryVersionsPreserveTheirExistingOutputResolution() {

        var fixture = new Fixture();
        var cadastrada = new VersaoProducao("PV", fixture.location, 1, fixture.roteiro, fixture.listaTecnica);
        var temporaria = new VersaoProducao(null, fixture.location, 1, fixture.roteiro, fixture.listaTecnica);
        assertTrue(fixture.linha(cadastrada).representaConsumoCompartilhadoDoPacote(fixture.projection));
        assertTrue(fixture.linha(temporaria).representaConsumoCompartilhadoDoPacote(fixture.projection));

    }

    @Test
    void missingOrInconsistentSavedMastersRemainExplicitErrors() {

        var fixture = new Fixture();
        fixture.projection.mapaListasTecnicas = Map.of();
        IllegalStateException ausente = assertThrows(IllegalStateException.class,
                () -> fixture.linha(null).representaConsumoCompartilhadoDoPacote(fixture.projection));
        assertEquals("BOM not projected: BOM", ausente.getMessage());

        fixture.projection.mapaListasTecnicas = Map.of("BOM", fixture.listaTecnica);
        fixture.listaTecnica.setMaterialOutput(new Produto("OTHER"));
        assertThrows(IllegalArgumentException.class,
                () -> fixture.linha(null).representaConsumoCompartilhadoDoPacote(fixture.projection));
        fixture.listaTecnica.setMaterialOutput(fixture.material);
        fixture.listaTecnica.setLocation(new Location("OTHER_PLANT"));
        assertThrows(IllegalArgumentException.class,
                () -> fixture.linha(null).representaConsumoCompartilhadoDoPacote(fixture.projection));

    }

    private static class Fixture {

        private final Location location = new Location("PLANT");
        private final Produto material = new Produto("FG");
        private final Roteiro roteiro = new Roteiro();
        private final ListaTecnica listaTecnica = new ListaTecnica();
        private final SupplyNetworkProjection projection = new SupplyNetworkProjection();

        private Fixture() {

            roteiro.setId("ROUTING");
            roteiro.setLocation(location);
            roteiro.setMaterialOutput(material);
            listaTecnica.setId("BOM");
            listaTecnica.setLocation(location);
            listaTecnica.setMaterialOutput(material);
            projection.mapaRoteiros = Map.of(roteiro.getId(), roteiro);
            projection.mapaListasTecnicas = Map.of(listaTecnica.getId(), listaTecnica);

        }

        private ProductionPlanLinha linha(VersaoProducao versaoProducao) {

            return new ProductionPlanLinha(new ProductionPlanLinha.ProductionPlanLinhaCompositeKey(
                    new SupplyPlan(), location, versaoProducao, roteiro, listaTecnica, material,
                    LocalDateTime.of(2026, 3, 31, 23, 59, 59)), material);

        }

    }

}
