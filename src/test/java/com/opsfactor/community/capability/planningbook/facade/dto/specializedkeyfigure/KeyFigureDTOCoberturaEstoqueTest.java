package com.opsfactor.community.capability.planningbook.facade.dto.specializedkeyfigure;

import com.opsfactor.community.capability.planningbook.keyfigure.domain.EditMode;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.dfudata.DFUDataKeyFigureCoberturaEstoque;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyFigureDTOCoberturaEstoqueTest {

    @Test
    void calculaEsgotamentoComEstoqueEFluxosAgregadosSemMultiplicarDiasPorSku() {

        Calendario calendario = Calendario.criaCalendarioDeOffsetsPeriodos(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 3, 1, 0, 0),
                0,
                0,
                4,
                0);
        KeyFigureDTOCoberturaEstoque primeiraFolha = criaCobertura(
                calendario,
                List.of(100.0d, 0.0d, 0.0d, 0.0d),
                List.of(0.0d, -25.0d, -25.0d, -100.0d));
        KeyFigureDTOCoberturaEstoque segundaFolha = criaCobertura(
                calendario,
                List.of(300.0d, 0.0d, 0.0d, 0.0d),
                List.of(0.0d, -75.0d, -75.0d, -300.0d));

        KeyFigureDTOCoberturaEstoque coberturaAgregada =
                new KeyFigureDTOCoberturaEstoque("Stock in Days", EditMode.NOEDIT);
        coberturaAgregada.incorporaValoresDeKeyFigure(primeiraFolha);
        coberturaAgregada.incorporaValoresDeKeyFigure(segundaFolha);

        String primeiroPeriodo = calendario.getUltimaDataHorarioPeriodo(0).toString();
        assertEquals(2.5d, primeiraFolha.getValues().get(primeiroPeriodo), 0.00001d);
        assertEquals(2.5d, segundaFolha.getValues().get(primeiroPeriodo), 0.00001d);
        assertEquals(2.5d, coberturaAgregada.getValues().get(primeiroPeriodo), 0.00001d);

    }

    @Test
    void calculaEsgotamentoAgregadoMensalConvertendoCadaPeriodoUmaUnicaVezParaDias() {

        Calendario calendario = Calendario.criaCalendarioDeOffsetsPeriodos(
                Constantes.TamanhoBucket.MENSAL,
                LocalDateTime.of(2026, 3, 1, 0, 0),
                0,
                0,
                4,
                0);
        KeyFigureDTOCoberturaEstoque primeiraFolha = criaCobertura(
                calendario,
                List.of(300.0d, 0.0d, 0.0d, 0.0d),
                List.of(0.0d, -90.0d, -10.0d, -250.0d));
        KeyFigureDTOCoberturaEstoque segundaFolha = criaCobertura(
                calendario,
                List.of(100.0d, 0.0d, 0.0d, 0.0d),
                List.of(0.0d, -10.0d, -90.0d, -150.0d));

        KeyFigureDTOCoberturaEstoque coberturaAgregada =
                new KeyFigureDTOCoberturaEstoque("Stock in Days", EditMode.NOEDIT);
        coberturaAgregada.incorporaValoresDeKeyFigure(primeiraFolha);
        coberturaAgregada.incorporaValoresDeKeyFigure(segundaFolha);

        String primeiroPeriodo = calendario.getUltimaDataHorarioPeriodo(0).toString();
        double coberturaEsperadaEmDias = calendario.getNumeroDiasNoPeriodo(1)
                + calendario.getNumeroDiasNoPeriodo(2)
                + calendario.getNumeroDiasNoPeriodo(3) / 2.0d;

        assertEquals(76.0d, coberturaEsperadaEmDias, 0.00001d);
        assertEquals(coberturaEsperadaEmDias, coberturaAgregada.getValues().get(primeiroPeriodo), 0.00001d);

    }

    private KeyFigureDTOCoberturaEstoque criaCobertura(
            Calendario calendario,
            List<Double> estoquesProjetados,
            List<Double> saldosEntradasSaidas) {

        KeyFigureDTOCoberturaEstoque cobertura =
                new KeyFigureDTOCoberturaEstoque("Stock in Days", EditMode.NOEDIT);
        List<DFUDataKeyFigureCoberturaEstoque> dadosCobertura = new ArrayList<>();
        for (int periodo = 0; periodo < estoquesProjetados.size(); periodo++) {

            DFUDataKeyFigureCoberturaEstoque dadoCobertura = new DFUDataKeyFigureCoberturaEstoque();
            dadoCobertura.setData(calendario.getUltimaDataHorarioPeriodo(periodo));
            dadoCobertura.setQuantidadeEstoqueProjetado(estoquesProjetados.get(periodo));
            dadoCobertura.setSaldoEntradasSaidas(saldosEntradasSaidas.get(periodo));
            dadosCobertura.add(dadoCobertura);

        }
        cobertura.importaDadosDFUDataKeyFigure(
                calendario,
                dadosCobertura);
        return cobertura;

    }

}
