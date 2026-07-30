package com.opsfactor.community.capability.demandplanning.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Serie simulada em nivel material/location, unico nivel exibido e ajustavel
 * no Community.
 *
 * <p>A entidade persistida ainda chama o material de `Produto`, mas o payload
 * publico da simulacao deve usar `materialId` para ficar alinhado ao novo front
 * e ao restante dos DTOs Community.</p>
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SimulatedDemandPlanMaterialLocationDTO {

    /**
     * Location da DFU simulada.
     */
    public String locationId;

    /**
     * Material da DFU simulada. O nome publico evita `productId` mesmo enquanto
     * a entidade fisica ainda se chama `Produto`.
     */
    public String materialId;

    /**
     * Venda historica observada antes de qualquer tratamento.
     */
    public double[] historicalSales;

    /**
     * Diferenca periodo a periodo entre forecast baseline e venda historica.
     */
    public double[] residual;

    /**
     * Valor absoluto de {@link #residual}, usado para comparacoes visuais de
     * erro no grafico de simulacao.
     */
    public double[] absoluteResidual;

    /**
     * Forecast baseline gerado pelo modelo estatistico Community selecionado.
     */
    public double[] baselineForecast;

    /**
     * Venda historica apos tratamento de stockouts. No Community e copia da
     * venda historica observada; Enterprise podera materializar tratamento real.
     */
    public double[] historicalSalesAfterStockoutTreatment;

    /**
     * Venda historica apos limpeza de outliers/eventos. No Community e copia da
     * serie apos stockout; Enterprise podera aplicar quantis, eventos ou outros
     * modelos Enterprise antes da execucao estatistica.
     */
    public double[] historicalSalesAfterOutlierTreatment;

    /**
     * Componente de tendencia quando o modelo estatistico escolhido materializa
     * essa decomposicao. Permanece nulo para modelos que nao retornam tendencia.
     */
    public double[] trend;

    /**
     * Componente sazonal quando o modelo estatistico escolhido materializa essa
     * decomposicao. Permanece nulo para modelos sem sazonalidade explicita.
     */
    public double[] seasonal;

    /**
     * Limite inferior do intervalo de previsao, quando retornado pela engine.
     */
    public double[] lowerBound;

    /**
     * Limite superior do intervalo de previsao, quando retornado pela engine.
     */
    public double[] upperBound;

}
