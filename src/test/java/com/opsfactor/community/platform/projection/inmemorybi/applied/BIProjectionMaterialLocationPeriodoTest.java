package com.opsfactor.community.platform.projection.inmemorybi.applied;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.javatuples.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.List;

/**
 * Valida o BI em memoria usado pelo Planning Book Community.
 *
 * <p>O contrato publico desta projection e restrito a material/location,
 * periodo e atributos adicionais simples. Agrupamentos por caracteristicas
 * pertencem ao Enterprise e nao devem ser necessarios para consultar linhas
 * Community.</p>
 */
public class BIProjectionMaterialLocationPeriodoTest {

    @Test
    public void getValoresShouldFilterByMaterialLocationAndPeriod() {

        Produto materialSelecionado = new Produto();
        materialSelecionado.setId("MAT-1");

        Produto outroMaterial = new Produto();
        outroMaterial.setId("MAT-2");

        Location locationSelecionada = new Location();
        locationSelecionada.setId("LOC-1");

        Location outraLocation = new Location();
        outraLocation.setId("LOC-2");

        BIProjectionMaterialLocationPeriodo<RegistroTeste> biProjectionMaterialLocationPeriodo =
                new BIProjectionMaterialLocationPeriodo<>(
                        Calendario.criaCalendarioDeOffsetsPeriodos(
                                Constantes.TamanhoBucket.MENSAL,
                                LocalDateTime.of(2026, 1, 1, 0, 0),
                                0,
                                0,
                                31,
                                0),
                        RegistroTeste::material,
                        RegistroTeste::location,
                        RegistroTeste::dataReferencia,
                        RegistroTeste.class,
                        true,
                        true);

        RegistroTeste registroEsperado = new RegistroTeste(
                materialSelecionado,
                locationSelecionada,
                LocalDateTime.of(2026, 1, 1, 0, 0));
        biProjectionMaterialLocationPeriodo.addDadoAoBI(registroEsperado);
        biProjectionMaterialLocationPeriodo.addDadoAoBI(new RegistroTeste(
                outroMaterial,
                locationSelecionada,
                LocalDateTime.of(2026, 1, 1, 0, 0)));
        biProjectionMaterialLocationPeriodo.addDadoAoBI(new RegistroTeste(
                materialSelecionado,
                outraLocation,
                LocalDateTime.of(2026, 1, 1, 0, 0)));

        List<RegistroTeste> registrosFiltrados = biProjectionMaterialLocationPeriodo.getValores(
                materialSelecionado,
                locationSelecionada,
                Pair.with("periodo", 0))
                .toList();

        Assertions.assertEquals(List.of(registroEsperado), registrosFiltrados);

    }

    private record RegistroTeste(
            Produto material,
            Location location,
            TemporalAccessor dataReferencia) {
    }

}
