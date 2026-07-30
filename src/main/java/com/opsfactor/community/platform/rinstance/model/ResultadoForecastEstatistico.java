package com.opsfactor.community.platform.rinstance.model;

import com.opsfactor.community.platform.calendar.Calendario;

/**
 * Resultado bruto retornado pelos modelos estatisticos executados em R.
 *
 * <p>Os arrays seguem as posicoes do {@link Calendario}. Em geral, `forecast`,
 * `trend`, `seasonal`, `lowerBound` e `upperBound` usam o calendario completo,
 * enquanto `fitHistorico` representa o ajuste historico quando o modelo R
 * disponibiliza essa serie. As engines Community copiam estes arrays para a
 * `DemandPlanForecastProjection` de trabalho e depois o workflow decide se
 * precisa desagregar para material/location.</p>
 */
public class ResultadoForecastEstatistico {
    
    Calendario calendario;
    
    /**
     * Forecast baseline indexado pelo calendario completo.
     */
    public double[] forecast;

    /**
     * Componente de tendencia quando o modelo gera decomposicao.
     */
    public double[] trend;

    /**
     * Componente sazonal quando o modelo gera decomposicao.
     */
    public double[] seasonal;

    /**
     * Limite inferior estatistico, quando retornado pelo pacote R.
     */
    public double[] lowerBound;

    /**
     * Limite superior estatistico, quando retornado pelo pacote R.
     */
    public double[] upperBound;

    /**
     * Serie fitted historica usada em simulacoes/inspecoes do modelo.
     */
    public double[] fitHistorico;
    
    public ResultadoForecastEstatistico() {
        
    }
    
    public ResultadoForecastEstatistico(Calendario calendario) {
        
        this.calendario = calendario;
        
        forecast = new double[calendario.getNumeroPeriodosTotais()];
        trend = new double[calendario.getNumeroPeriodosTotais()];
        seasonal = new double[calendario.getNumeroPeriodosTotais()];
        lowerBound = new double[calendario.getNumeroPeriodosTotais()];
        upperBound = new double[calendario.getNumeroPeriodosTotais()];
        fitHistorico = new double[calendario.getNumeroPeriodosTotais()];
        
    } 
    
}
