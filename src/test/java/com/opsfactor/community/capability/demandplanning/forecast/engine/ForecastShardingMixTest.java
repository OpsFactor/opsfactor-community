package com.opsfactor.community.capability.demandplanning.forecast.engine;

import com.opsfactor.community.capability.demandplanning.demandplan.projection.*;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.platform.calendar.CalendarioSimples;
import com.opsfactor.community.platform.utility.Constantes;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ForecastShardingMixTest {

    @Test
    void inactiveMembersKeepHistoryButDoNotReceiveFutureAndEmptyScopeIsExplicit() {

        var calendario=calendario();
        var raiz=new DemandPlanForecastProjectionAgregado(calendario,new UnidadeMedida(),false);
        var a=folha(calendario,"A",new double[]{20,20,20,20},20);
        var b=folha(calendario,"B",new double[]{80,80,80,80},80);
        b.setElegivelMixSharding(false);
        raiz.getDemandPlanForecastProjectionDesagregados().addAll(List.of(a,b));
        raiz.forecastBaseline[4]=100;
        ForecastShardingMix.aplicar(calendario,3,raiz,true);
        assertEquals(100,a.forecastBaseline[4],1e-8);
        assertEquals(0,b.forecastBaseline[4],1e-8);
        assertArrayEquals(new double[]{80,80,80,80},b.demanda);
        a.setElegivelMixSharding(false);
        ForecastShardingMix.aplicar(calendario,3,raiz,true);
        assertEquals(0,raiz.forecastBaseline[4]);
        assertEquals("NO_ELIGIBLE_DFU",raiz.getDiagnosticoMixSharding().get("regra"));

    }

    @Test
    void mixRecenteConservaTotalEHistoricoComIdentidadeMapeReal() {

        var calendario = calendario();
        var raiz = new DemandPlanForecastProjectionAgregado(calendario, new UnidadeMedida(), false);
        var a = folha(calendario, "A", new double[]{80, 80, 20, 20}, 100);
        var b = folha(calendario, "B", new double[]{20, 20, 80, 80}, 100);
        raiz.getDemandPlanForecastProjectionDesagregados().addAll(List.of(a,b));
        raiz.forecastBaseline[4] = 200;
        raiz.forecastBaseline[0] = 777;
        a.fitModeloHistorico = new double[]{1,2,3,4};
        ForecastShardingMix.aplicar(calendario, 2, raiz, true);
        assertEquals(40, a.forecastBaseline[4], 1e-9);
        assertEquals(160, b.forecastBaseline[4], 1e-9);
        assertEquals(200, raiz.forecastBaseline[4], 1e-9);
        assertEquals(777, raiz.forecastBaseline[0], 1e-9);
        assertArrayEquals(new double[]{1,2,3,4}, a.fitModeloHistorico);
        assertArrayEquals(new double[]{80,80,20,20}, a.demanda);
        assertEquals(2, raiz.getDiagnosticoMixSharding().get("numeroPeriodosUsados"));

    }

    @Test
    void exposicoesLocaisUsamRespostaMapeAntesDaRespostaDfu() {

        var calendario = calendario();
        var raiz = new DemandPlanForecastProjectionAgregado(calendario,new UnidadeMedida(),false);
        var a = folha(calendario,"A",new double[]{10,10,10,10},20);
        var b = folha(calendario,"A",new double[]{30,30,10,10},20);
        var c = folha(calendario,"B",new double[]{10,10,10,10},10);
        raiz.getDemandPlanForecastProjectionDesagregados().addAll(List.of(a,b,c));
        raiz.forecastBaseline[4] = 50;
        ForecastShardingMix.aplicar(calendario,2,raiz,true);
        // MAPE A: volume recente 40 x resposta 40/30; B: 20 x 10/10.
        // Portanto A=400/11, B=150/11. Dentro A respostas 2:1 e mix 1:1.
        assertEquals(400D/11*2/3,a.forecastBaseline[4],1e-9);
        assertEquals(400D/11/3,b.forecastBaseline[4],1e-9);
        assertEquals(150D/11,c.forecastBaseline[4],1e-9);
        assertEquals(50,raiz.forecastBaseline[4],1e-9);

    }

    @Test
    void agregadoSemCurvasLocaisUsaMixEJanelaCurtaSemVazarFuturo() {

        var calendario=calendario();
        var raiz=new DemandPlanForecastProjectionAgregado(calendario,new UnidadeMedida(),false);
        var a=folha(calendario,"A",new double[]{0,0,10,30,999999},0);
        var b=folha(calendario,"B",new double[]{0,0,30,10,0},0);
        raiz.getDemandPlanForecastProjectionDesagregados().addAll(List.of(a,b));
        raiz.forecastBaseline[4]=80;
        ForecastShardingMix.aplicar(calendario,1,raiz,false);
        assertEquals(60,a.forecastBaseline[4],1e-9);
        assertEquals(20,b.forecastBaseline[4],1e-9);
        ForecastShardingMix.aplicar(calendario,12,raiz,false);
        assertEquals(40,a.forecastBaseline[4],1e-9);
        assertEquals(4,raiz.getDiagnosticoMixSharding().get("numeroPeriodosUsados"));

    }

    @Test
    void fallbackAuditadoNaoInventaMassaEAusenciaParametroPreservaFluxo() {

        var calendario=calendario();
        var raiz=new DemandPlanForecastProjectionAgregado(calendario,new UnidadeMedida(),false);
        var a=folha(calendario,"A",new double[]{10,10,0,0},0);
        var b=folha(calendario,"B",new double[]{30,30,0,0},0);
        raiz.getDemandPlanForecastProjectionDesagregados().addAll(List.of(a,b));
        raiz.forecastBaseline[4]=80;
        ForecastShardingMix.aplicar(calendario,null,raiz,false);
        assertEquals(0,a.forecastBaseline[4]);
        ForecastShardingMix.aplicar(calendario,2,raiz,false);
        assertEquals(20,a.forecastBaseline[4],1e-9);
        assertTrue(raiz.getDiagnosticoMixSharding().get("regras").toString().contains("FULL_TRAINING_HISTORY"));
        Arrays.fill(a.demanda,0);Arrays.fill(b.demanda,0);
        assertThrows(IllegalStateException.class,()->ForecastShardingMix.aplicar(calendario,2,raiz,false));

    }

    private static CalendarioSimples calendario() {

        return CalendarioSimples.criaCalendarioDeOffsetsPeriodos(Constantes.TamanhoBucket.MENSAL,
                LocalDateTime.of(2026,1,1,0,0),0,4,1,0);

    }

    private static DemandPlanForecastProjectionMaterialLocation folha(CalendarioSimples calendario,
            String grupo, double[] demanda, double forecast) {

        var folha=new DemandPlanForecastProjectionMaterialLocation(calendario,new UnidadeMedida(),new Location(),new Produto(),false);
        folha.setGrupoMapeForecast(grupo);
        folha.demanda=demanda;
        folha.forecastBaseline[4]=forecast;
        return folha;

    }
}
