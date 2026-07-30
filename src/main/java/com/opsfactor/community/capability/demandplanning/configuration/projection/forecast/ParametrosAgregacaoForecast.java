package com.opsfactor.community.capability.demandplanning.configuration.projection.forecast;

import com.opsfactor.community.capability.demandplanning.configuration.domain.ParametrosDemandPlanNivelCluster;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.Getter;


/**
 * Descreve a combinacao material/location usada para construir a unidade de
 * execucao de forecast.
 *
 * <p>As duas dimensoes usam o mesmo enum porque o comportamento é o mesmo:
 * `BOTTOM_UP` gera séries no menor nível da dimensão e `TOP_DOWN` gera uma
 * série agregada que depois será aberta até material/location. A classe mantém
 * dois atributos para deixar explicita a combinacao efetiva, por exemplo
 * material top-down com location bottom-up. Apesar de nascer a partir dos
 * parametros de nivel cluster, este value object pertence ao workflow de
 * forecast: Enterprise podera criar unidades equivalentes a partir de nodes de
 * arvore ou outros escopos sem mudar a semantica desses dois campos.</p>
 */
@Getter
public class ParametrosAgregacaoForecast {

    /**
     * Nivel de agregacao da dimensao location.
     *
     * <p>Valor nulo e normalizado para `TOP_DOWN`, que e o comportamento mais
     * conservador: exige projection agregada e split explicito, em vez de
     * tratar uma configuracao incompleta como bottom-up por acidente.</p>
     */
    private Constantes.DPNivelAgregacao locationAggregationType;

    /**
     * Nivel de agregacao da dimensao material.
     *
     * <p>Assim como location, nulo vira `TOP_DOWN`. Isso preserva o contrato
     * antigo de configuracoes transicionais sem deixar um setter externo
     * recolocar `null` depois da construcao.</p>
     */
    private Constantes.DPNivelAgregacao materialAggregationType;

    public ParametrosAgregacaoForecast(
            Constantes.DPNivelAgregacao locationAggregationType,
            Constantes.DPNivelAgregacao materialAggregationType) {

        setLocationAggregationType(locationAggregationType);
        setMaterialAggregationType(materialAggregationType);

    }

    public ParametrosAgregacaoForecast(
            ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelCluster) {

        if (parametrosDemandPlanNivelCluster == null) {
            throw new IllegalArgumentException(
                    "ParametrosDemandPlanNivelCluster e obrigatorio para derivar ParametrosAgregacaoForecast");
        }
        setLocationAggregationType(parametrosDemandPlanNivelCluster.getLocationAggregationType());
        setMaterialAggregationType(parametrosDemandPlanNivelCluster.getMaterialAggregationType());

    }

    /**
     * Atualiza o nivel de location preservando o fallback conservador.
     */
    public void setLocationAggregationType(
            Constantes.DPNivelAgregacao locationAggregationType) {

        this.locationAggregationType = normalizaNivelAgregacao(locationAggregationType);

    }

    /**
     * Atualiza o nivel de material preservando o fallback conservador.
     */
    public void setMaterialAggregationType(
            Constantes.DPNivelAgregacao materialAggregationType) {

        this.materialAggregationType = normalizaNivelAgregacao(materialAggregationType);

    }

    /**
     * Retorna verdadeiro quando qualquer dimensao exige execucao agregada e
     * posterior desagregacao para material/location.
     */
    public boolean isQualquerDimensaoTopDown() {

        return Constantes.DPNivelAgregacao.TOP_DOWN.equals(materialAggregationType)
                || Constantes.DPNivelAgregacao.TOP_DOWN.equals(locationAggregationType);

    }

    /**
     * Normaliza configuracoes antigas/incompletas para o modo mais explicito e
     * menos permissivo do workflow.
     */
    private Constantes.DPNivelAgregacao normalizaNivelAgregacao(
            Constantes.DPNivelAgregacao dpNivelAgregacao) {

        return (dpNivelAgregacao == null) ? Constantes.DPNivelAgregacao.TOP_DOWN : dpNivelAgregacao;

    }

}
