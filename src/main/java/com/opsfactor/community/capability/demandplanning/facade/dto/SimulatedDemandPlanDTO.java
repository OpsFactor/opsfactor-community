package com.opsfactor.community.capability.demandplanning.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.capability.cluster.facade.dto.ClusterLocationsDTO;
import com.opsfactor.community.capability.cluster.facade.dto.ClusterProdutosDTO;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO usado para enviar a simulacao de forecast para a tela de Demand Analysis.
 *
 * <p>No Community, a simulacao exposta ao front fica restrita ao nivel
 * material/location. Nivel MAPE configuravel e agregacao por caracteristicas
 * sao Enterprise e nao fazem parte deste DTO.</p>
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SimulatedDemandPlanDTO {

    /**
     * Datas finais de cada periodo do calendario usado na simulacao.
     *
     * <p>Todas as series material/location retornadas no DTO usam este mesmo
     * eixo temporal, o que permite comparar historico, tratamentos, forecast e
     * residuos sem publicar qualquer nivel agregado Enterprise.</p>
     */
    public List<LocalDate> periodos = new ArrayList<>();

    /**
     * Agrupador visual desagregado usado no eixo secundario do grafico.
     *
     * <p>Para calendario mensal representa meses, para semanal representa
     * semanas WW e para diario representa dia da semana. O campo e apenas apoio
     * de apresentacao; nao reabre configuracao de agregacao MAPE ou arvore.</p>
     */
    List<Integer> agrupadoresPeriodoDesagregado;

    /**
     * Agrupador visual agregado usado para organizar periodos no grafico.
     *
     * <p>Para calendario mensal/semanal representa anos e para diario representa
     * semanas YYYYWW. Assim como o agrupador desagregado, e metadado de eixo,
     * nao projection agregada de forecast.</p>
     */
    List<Integer> agrupadoresPeriodoAgregado;

    /**
     * Primeiro periodo em que o forecast passa a ser exibido como plano futuro.
     */
    public LocalDate periodoInicioForecast;

    /**
     * Posicao de {@link #periodoInicioForecast} dentro de {@link #periodos}.
     */
    public Integer posicaoPeriodoInicioForecast;

    /**
     * Ultimo periodo com venda historica observada na projection de sales.
     */
    public LocalDate periodoUltimaVenda;

    /**
     * Posicao de {@link #periodoUltimaVenda} dentro de {@link #periodos}.
     */
    public Integer posicaoPeriodoUltimaVenda;

    /*
     * O tipo DTO ainda nasce do dominio legado `ClusterProdutosDTO`, mas o
     * payload publico da simulacao usa materialClusterDTO para ficar alinhado
     * aos campos de configuracao materialClusterId/materialAggregationType.
     */
    public ClusterProdutosDTO materialClusterDTO;

    /**
     * Cluster de locations usado como escopo da simulacao.
     */
    public ClusterLocationsDTO clusterLocationsDTO;

    /**
     * Series finais publicadas ao front, sempre em nivel material/location.
     *
     * <p>O Community nao deve adicionar listas paralelas de agregado MAPE,
     * tree node, support series ou diagnosticos de auto-fit neste DTO. O overlay
     * Enterprise deve abrir novos campos apenas quando houver runtime real para
     * eles.</p>
     */
    public List<SimulatedDemandPlanMaterialLocationDTO> materialLocationData;

}
