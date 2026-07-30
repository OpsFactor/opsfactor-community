package com.opsfactor.community.capability.demandplanning.configuration.projection.forecast;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.configuration.domain.ParametrosModeloEstatisticoAbstract;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.Getter;

/**
 * Parametros da limpeza historica usada antes da execucao estatistica.
 *
 * <p>O Community carrega este value object para manter um contrato unico de
 * configuracao e projections com o Enterprise, mas so aceita a configuracao
 * neutra na borda de DTO. A execucao Community continua copiando a venda
 * historica tratada por stockout para a serie final de forecast. O Enterprise
 * pode usar os mesmos campos em processors {@code @Primary}, sem ampliar o
 * codigo aberto com entidades de campanha, pricing ou stockout real.</p>
 *
 * <p>A nomenclatura acompanha as series do workflow: a entrada desta etapa e
 * {@code vendaHistoricaTratamentoStockouts}; a saida e
 * {@code vendaHistoricaTratamentoOutliers}. Nao usamos mais o termo generico
 * "demanda normalizada" para evitar esconder qual tratamento ocorreu.</p>
 */
@Getter
public class ParametrosLimpezaHistoricoForecast {

    /**
     * Indica se a etapa anterior deve considerar dados de estoque para tratar
     * stockouts. No Community o mapper bloqueia {@code true}; o campo existe
     * para persistencia transicional e para overlays Enterprise.
     */
    private final boolean consideraDadosEstoque;

    /**
     * Modelo de limpeza historica. Community aceita somente DESATIVADO;
     * Enterprise pode liberar PERCENTIS e CAMPANHA por overlay. CAMPANHA exige
     * entidades privadas de evento/campanha e por isso permanece apenas como
     * contrato compartilhado no Community.
     */
    private final Constantes.DPModeloNormalizacao modeloNormalizacao;

    /**
     * Janela, em dias, usada para montar a vizinhanca historica dos percentis.
     */
    private final int diasHistoricosNormalizacao;

    /**
     * Quando verdadeiro, valores acima do percentil superior sao limitados ao
     * proprio percentil.
     */
    private final boolean habilitaLimpezaHistoricoPercentilSuperior;

    /**
     * Percentil superior expresso entre 0 e 1.
     *
     * <p>Mesmo quando a aplicacao do percentil estiver desligada, o valor
     * materializado continua pertencendo ao contrato numerico do projection.
     * Por isso {@code NaN}, infinito e valores fora da escala interna devem
     * falhar ja na construcao deste value object.</p>
     */
    private final double percentilSuperiorLimpezaHistorico;

    /**
     * Quando verdadeiro, valores abaixo do percentil inferior sao elevados ao
     * proprio percentil.
     */
    private final boolean habilitaLimpezaHistoricoPercentilInferior;

    /**
     * Percentil inferior expresso entre 0 e 1.
     *
     * <p>Quando superior e inferior estiverem habilitados ao mesmo tempo, o
     * inferior nao pode ficar acima do superior. Essa validacao permanece aqui
     * para proteger factories/projections novos antes mesmo de chegar ao
     * processor Enterprise que executa a limpeza real.</p>
     */
    private final double percentilInferiorLimpezaHistorico;

    public ParametrosLimpezaHistoricoForecast(
            Boolean consideraDadosEstoque,
            Constantes.DPModeloNormalizacao modeloNormalizacao,
            Integer diasHistoricosNormalizacao,
            Boolean habilitaLimpezaHistoricoPercentilSuperior,
            Double percentilSuperiorLimpezaHistorico,
            Boolean habilitaLimpezaHistoricoPercentilInferior,
            Double percentilInferiorLimpezaHistorico,
            ParametrosGlobais parametrosGlobais) {

        this.consideraDadosEstoque =
                consideraDadosEstoque != null && consideraDadosEstoque;
        this.modeloNormalizacao =
                modeloNormalizacao == null
                        ? Constantes.DPModeloNormalizacao.DESATIVADO
                        : modeloNormalizacao;
        this.diasHistoricosNormalizacao =
                getDiasHistoricosNormalizacao(
                        diasHistoricosNormalizacao,
                        parametrosGlobais);
        this.habilitaLimpezaHistoricoPercentilSuperior =
                habilitaLimpezaHistoricoPercentilSuperior == null
                        || habilitaLimpezaHistoricoPercentilSuperior;
        this.percentilSuperiorLimpezaHistorico =
                getPercentilSuperiorLimpezaHistorico(
                        percentilSuperiorLimpezaHistorico,
                        parametrosGlobais);
        this.habilitaLimpezaHistoricoPercentilInferior =
                habilitaLimpezaHistoricoPercentilInferior == null
                        || habilitaLimpezaHistoricoPercentilInferior;
        this.percentilInferiorLimpezaHistorico =
                getPercentilInferiorLimpezaHistorico(
                        percentilInferiorLimpezaHistorico,
                        parametrosGlobais);

    }

    public ParametrosLimpezaHistoricoForecast(
            ParametrosModeloEstatisticoAbstract parametrosModeloEstatisticoAbstract,
            ParametrosGlobais parametrosGlobais) {

        this(
                parametrosModeloEstatisticoAbstract.getConsideraDadosEstoque(),
                parametrosModeloEstatisticoAbstract.getModeloNormalizacao(),
                parametrosModeloEstatisticoAbstract.getDiasHistoricosNormalizacao(),
                parametrosModeloEstatisticoAbstract.getHabilitaLimpezaHistoricoPercentilSuperior(),
                parametrosModeloEstatisticoAbstract.getPercentilSuperiorLimpezaHistorico(),
                parametrosModeloEstatisticoAbstract.getHabilitaLimpezaHistoricoPercentilInferior(),
                parametrosModeloEstatisticoAbstract.getPercentilInferiorLimpezaHistorico(),
                parametrosGlobais);

    }

    public ParametrosLimpezaHistoricoForecast(
            ParametrosLimpezaHistoricoForecast parametrosLimpezaHistoricoForecast,
            ParametrosGlobais parametrosGlobais) {

        this(
                parametrosLimpezaHistoricoForecast == null ? null : parametrosLimpezaHistoricoForecast.isConsideraDadosEstoque(),
                parametrosLimpezaHistoricoForecast == null ? null : parametrosLimpezaHistoricoForecast.getModeloNormalizacao(),
                parametrosLimpezaHistoricoForecast == null ? null : parametrosLimpezaHistoricoForecast.getDiasHistoricosNormalizacao(),
                parametrosLimpezaHistoricoForecast == null ? null : parametrosLimpezaHistoricoForecast.isHabilitaLimpezaHistoricoPercentilSuperior(),
                parametrosLimpezaHistoricoForecast == null ? null : parametrosLimpezaHistoricoForecast.getPercentilSuperiorLimpezaHistorico(),
                parametrosLimpezaHistoricoForecast == null ? null : parametrosLimpezaHistoricoForecast.isHabilitaLimpezaHistoricoPercentilInferior(),
                parametrosLimpezaHistoricoForecast == null ? null : parametrosLimpezaHistoricoForecast.getPercentilInferiorLimpezaHistorico(),
                parametrosGlobais);

    }

    private static int getDiasHistoricosNormalizacao(
            Integer diasHistoricosNormalizacao,
            ParametrosGlobais parametrosGlobais) {

        return diasHistoricosNormalizacao == null
                ? parametrosGlobais.getDiasHistoricosNormalizacao()
                : diasHistoricosNormalizacao;

    }

    private static double getPercentilSuperiorLimpezaHistorico(
            Double percentilSuperiorLimpezaHistorico,
            ParametrosGlobais parametrosGlobais) {

        return percentilSuperiorLimpezaHistorico == null
                ? parametrosGlobais.getPercentilOutliersVenda()
                : percentilSuperiorLimpezaHistorico;

    }

    private static double getPercentilInferiorLimpezaHistorico(
            Double percentilInferiorLimpezaHistorico,
            ParametrosGlobais parametrosGlobais) {

        return percentilInferiorLimpezaHistorico == null
                ? 1.0d - parametrosGlobais.getPercentilOutliersVenda()
                : percentilInferiorLimpezaHistorico;

    }

}
