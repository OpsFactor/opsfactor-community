package com.opsfactor.community.capability.demandplanning.configuration.domain;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutosDemandPlanning;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.*;

import jakarta.persistence.*;
import java.io.Serializable;

/**
 * Parametros de forecast para uma combinacao de cluster de material e cluster
 * de location.
 *
 * <p>No Community, esta entidade representa a unidade configuravel de execucao
 * do Demand Planning: modelos estatisticos abertos, agregacao top-down ou
 * bottom-up por dimensao, horizonte historico e unidade de medida padrao. A
 * arvore de segmentacao, support series, regressores e configuracoes
 * Enterprise nao sao mapeadas aqui.</p>
 */
@Data
@Builder
// lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "parametrosDemandPlanNivelClusterCompositeKey")
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
public class ParametrosDemandPlanNivelCluster extends ParametrosModeloEstatisticoAbstract implements Serializable {
    
    @EmbeddedId @NonNull
    private ParametrosDemandPlanNivelClusterCompositeKey parametrosDemandPlanNivelClusterCompositeKey;

    @Data
    @NoArgsConstructor
    @RequiredArgsConstructor
    @Embeddable 
    @EqualsAndHashCode
    public static class ParametrosDemandPlanNivelClusterCompositeKey implements Serializable {

        @ManyToOne(optional = false)
        @NonNull
        private PerfilExecucaoDemandPlan perfilExecucaoDemandPlan;

        @ManyToOne(optional = false)
        @NonNull
        private ClusterProdutosDemandPlanning clusterProdutosDemandPlanning;

        @ManyToOne(optional = false)
        @NonNull
        private ClusterLocations clusterLocations;

    }

    // PARAMETROS ESPECIFICOS DO NIVEL CLUSTER (SEM EQUIVALENTE POR NODE ARVORE BINARIA)
    private Boolean executaDp;

    /**
     * Tipo de agregação usado para a dimensão material na geração do forecast.
     *
     * `BOTTOM_UP` gera a série estatística diretamente no menor nível material
     * disponível para a unidade de execução. `TOP_DOWN` gera a série em nível
     * agregado de materiais e depois desagrega para material/location via split
     * histórico. O enum é persistido por ordinal; `BOTTOM_UP` deve permanecer
     * ordinal 0 e `TOP_DOWN` ordinal 1.
     */
    @Enumerated(EnumType.ORDINAL)
    private Constantes.DPNivelAgregacao materialAggregationType;

    /**
     * Tipo de agregação usado para a dimensão location na geração do forecast.
     *
     * A regra é independente da dimensão material: podemos ter, por exemplo,
     * forecast top-down para material e bottom-up para location. A factory de
     * forecast transforma a combinação material/location em uma lista de
     * projections agregadas sem criar unidades vazias.
     */
    @Enumerated(EnumType.ORDINAL)
    private Constantes.DPNivelAgregacao locationAggregationType;

    private Integer diasHistoricosForecastEstatistico; // número de dias considerados para a geração do forecast

    private Boolean dpUsaHistoricoDemandaInativos;
    private Boolean dpGeraForecastParaDescontinuados;

    /**
     * Permite que este par de clusters use o modelo AutoFit padrão do perfil
     * quando um overlay Enterprise o disponibilizar.
     *
     * <p>O Community apenas preserva a opção na tabela única de parâmetros e
     * não ativa nem resolve AutoFit. {@code null} mantém compatibilidade com
     * linhas existentes e equivale a {@code true}; somente {@code false}
     * preserva explicitamente os parâmetros manuais deste par.</p>
     */
    private Boolean useExecutionProfileAutofitModel;

    /**
     * Ponte escalar para o Budget privado selecionado pelo modelo Enterprise
     * Budget as Forecast.
     *
     * <p>O aggregate Community mantem a coluna na tabela unica de parametros,
     * mas nao conhece a entidade privada, nao declara relacao JPA e bloqueia
     * qualquer uso funcional na sua borda. O overlay Enterprise resolve e
     * valida o id apenas quando o modelo correspondente estiver selecionado.</p>
     */
    private Long budgetId;

    @ManyToOne
    private UnidadeMedida unidadeMedidaPadraoDP;

    private Boolean arredondaParaUnidadeVenda;

    /*
     * Regressores internos compartilhados com o Enterprise. O Community os
     * persiste somente para manter a tabela de parametros unica, mas bloqueia
     * qualquer ativacao em sua borda de configuracao e nao os entrega ao
     * runtime aberto. O Enterprise materializa os vetores de trend/working
     * days em memoria antes da chamada da engine estatistica.
     */
    private Boolean includeTargetTrendGrowthRegressor;
    private Integer trendHistoricalWindowInDays;
    private Double targetTrendGrowthYearOverYear;
    private Boolean includeWorkingDaysRegressor;

    public PerfilExecucaoDemandPlan getPerfilExecucaoDemandPlan() {
        return parametrosDemandPlanNivelClusterCompositeKey.getPerfilExecucaoDemandPlan();
    }
    public ClusterProdutosDemandPlanning getClusterProdutosDemandPlanning() {
        return parametrosDemandPlanNivelClusterCompositeKey.getClusterProdutosDemandPlanning();
    }

    /**
     * Alias funcional para o cluster de materiais do Demand Planning.
     *
     * <p>O campo do embedded-id permanece `clusterProdutosDemandPlanning` para
     * preservar o mapeamento JPA transicional baseado em `ClusterProdutos`.
     * Services/projections que tratam o conceito funcional devem preferir este
     * getter.</p>
     */
    public ClusterProdutosDemandPlanning getClusterMateriaisDemandPlanning() {
        return getClusterProdutosDemandPlanning();
    }

    public ClusterLocations getClusterLocations() {
        return parametrosDemandPlanNivelClusterCompositeKey.getClusterLocations();
    }

    /**
     * Resolve a opção persistida sem alterar linhas existentes que ainda não
     * possuem valor para o novo campo.
     */
    public boolean usesExecutionProfileAutofitModel() {

        return !Boolean.FALSE.equals(useExecutionProfileAutofitModel);

    }

    public Integer getNumeroDiasProdutoNovo() {

        /*
         * Tratamento especifico de materiais novos e uma capacidade Enterprise.
         * O metodo permanece para contratos transicionais de projections e
         * DTOs compartilhados; ao retornar null, os callers Community caem no
         * default global, que tambem e 0.
         */
        return null;

    }

}
