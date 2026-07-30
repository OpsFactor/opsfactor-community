package com.opsfactor.community.capability.demandplanning.configuration.projection.aggregation;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutosDemandPlanning;
import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosForecastProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosGeraisDemandPlanningProjection;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuracao efetiva de Demand Planning para uma combinacao cluster material
 * / cluster location em um perfil de execucao.
 *
 * <p>No Community esta projection sempre representa a configuracao manual do
 * nivel cluster. Ela carrega os parametros gerais do cluster e define um hook
 * para resolver os parametros estatisticos de uma DFU material/location. A
 * implementacao Community simples retorna o mesmo bloco estatistico para todas
 * as DFUs do cluster; configuracoes por node de arvore, auto-fit ou qualquer
 * escolha dinamica por material/location pertencem ao Enterprise.</p>
 */
@Getter
@Setter
public abstract class ParametrosDemandPlanNivelClusterProjection {

    private PerfilExecucaoDemandPlan perfilExecucaoDemandPlan;
    private ClusterLocations clusterLocations;
    private ClusterProdutosDemandPlanning clusterProdutosDemandPlanning;

    /**
     * Alias funcional para a entidade transicional
     * {@link ClusterProdutosDemandPlanning}.
     *
     * <p>O getter gerado por Lombok (`getClusterProdutosDemandPlanning`) segue
     * existindo para os pontos ainda acoplados ao nome fisico legado. Codigo
     * novo no Community deve preferir este getter quando estiver tratando o
     * conceito de cluster de materiais.</p>
     */
    public ClusterProdutosDemandPlanning getClusterMateriaisDemandPlanning() {
        return clusterProdutosDemandPlanning;
    }

    /**
     * Parametros gerais que valem para toda a unidade cluster material/location:
     * execucao, unidade, agregacao top-down/bottom-up, janela historica e
     * regras Community de DFU ativa/descontinuada.
     */
    private ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection;

    /**
     * Indica se o overlay Enterprise pode substituir o forecast manual deste
     * par pelo vencedor AutoFit do perfil. A projection Community apenas
     * materializa o opt-out persistido; não consulta nem ativa AutoFit.
     */
    private boolean useExecutionProfileAutofitModel;

    /**
     * Cria a projection manual base sem acoplar sua construção a capacidades
     * Enterprise. O opt-out começa efetivamente habilitado para linhas novas.
     */
    protected ParametrosDemandPlanNivelClusterProjection(
            PerfilExecucaoDemandPlan perfilExecucaoDemandPlan,
            ClusterLocations clusterLocations,
            ClusterProdutosDemandPlanning clusterProdutosDemandPlanning,
            ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection) {

        this.perfilExecucaoDemandPlan = perfilExecucaoDemandPlan;
        this.clusterLocations = clusterLocations;
        this.clusterProdutosDemandPlanning = clusterProdutosDemandPlanning;
        this.parametrosGeraisDemandPlanningProjection = parametrosGeraisDemandPlanningProjection;
        this.useExecutionProfileAutofitModel = true;

    }

    /**
     * Resolve os parametros estatisticos aplicaveis a uma DFU material/location
     * dentro desta unidade de execucao.
     *
     * <p>No Community, a resposta independe da DFU e vem diretamente da
     * configuracao manual do cluster. O metodo continua recebendo location e
     * material para que o Enterprise possa especializar a projection por node,
     * auto-fit ou outra regra privada sem mudar o service principal.</p>
     *
     * @param location location da DFU avaliada
     * @param material material da DFU avaliada
     * @return parametros de forecast efetivos para a DFU
     */
    public abstract ParametrosForecastProjection getParametrosForecastProjection(Location location, Produto material);

}
