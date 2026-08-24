package com.opsfactor.community.capability.supplyplanning.configuration.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.*;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Perfil de execução do Supply Planning Community.
 *
 * <p>A entidade permanece ampla porque a tabela e os DTOs são compartilhados
 * com a edição Enterprise. Campos e enums associados a otimizador, process
 * chain, custos, frota, capacidade logística detalhada e outras capacidades
 * privadas podem existir fisicamente aqui para compatibilidade de schema, mas
 * devem ser bloqueados nas bordas Community antes de qualquer cálculo. Métodos
 * com default Community documentam explicitamente quando um conceito foi
 * preservado apenas para overlay Enterprise ou payload compartilhado.</p>
 */
@Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
@EqualsAndHashCode(of="id")
@NoArgsConstructor 
@AllArgsConstructor
@Entity
public class PerfilExecucaoSupplyPlan implements Serializable, Cloneable {
        
    /*
     * Define como a demanda direta e escolhida para cada bucket.
     *
     * No Community, apenas SOMENTE_FORECAST e funcional. Os valores que citam
     * Client Orders permanecem no enum para compatibilidade de payload/schema e
     * sao bloqueados nas bordas de configuracao Community.
     */
    public enum ModeloMajoracaoDemandaDireta {
        @JsonProperty("Largest by Period") MAIOR_POR_PERIODO,
        @JsonProperty("Client Orders over fixed Horizon") CARTEIRA_EM_HORIZONTE_FIXO,
        @JsonProperty("Demand Plan only") SOMENTE_FORECAST,
        @JsonProperty("Client Orders only") SOMENTE_PEDIDOS_CLIENTES,
        @JsonProperty("Demand Plan + Client Orders") PLANO_DEMANDA_MAIS_CARTEIRA,
    }
    
    /*
     * PROCESS_CHAIN permanece no enum porque o front compartilhado pode enviar
     * este valor e a API Community precisa decodificar o payload para bloqueá-lo
     * com RequiresEnterpriseVersionException. A estrutura física da cadeia
     * pertence somente ao Enterprise.
     */
    public enum ModoExecucao {
        @JsonProperty("Heuristic") HEURISTICO,
        @JsonProperty("Optimizer") OTIMIZADOR,
        @JsonProperty("Process Chain") PROCESS_CHAIN,
    }

    public enum ModeloEstoqueTarget {
        @JsonProperty("Min/Max") MIN_MAX,
        @JsonProperty("Average Stock") ESTOQUE_MEDIO,
    }

    public enum TipoCapacidadeProdutiva {
        @JsonProperty("Total Hours / Day") HORAS_POR_DIA,
        @JsonProperty("Quantity in UOM") QUANTIDADE_POR_UOM,
        @JsonProperty("Shift Allocation / Day") ALOCACAO_TURNOS
    }

    public enum TipoCapacidadeLogistica {
        @JsonProperty("Location Level") NIVEL_LOCATION,
        @JsonProperty("Location / Date Level") NIVEL_LOCATION_DATA
    }

    public enum ModoApuracaoImpostos {
        @JsonProperty("Simplified Tax Calculation") CALCULO_SIMPLIFICADO,
        @JsonProperty("ICMS Apportionment") APURACAO_ICMS
    }
    
    public enum OtimizadorInteligenciaArtificial {
        @JsonProperty("Disabled - Only Mixed Integer Optimization") DESABILITADO,
        @JsonProperty("SNP Scheduling") SNP,
        @JsonProperty("Detailed Scheduling") DETALHADO,
        @JsonProperty("DRP Fleet Allocation") DRP,
        @JsonProperty("Greenfield") GREENFIELD,
        @JsonProperty("Lot Size Minimum / Multiple") LOT_SIZE_ESTOQUE_MAX, // estoque max vira no futuro
        @JsonProperty("Last Mile") LAST_MILE,
    }

    public enum ModeloDecaimentoImpactoTemporal {
        @JsonProperty("Linear") LINEAR,
        @JsonProperty("Exponential") EXPONENCIAL,
    }

    public enum ModoPropagacaoDemanda {
        @JsonProperty("Propagate Demand to Internal Locations") PROPAGACAO_DEMANDA_LOCATIONS_INTERNAS,
        @JsonProperty("Propagate Demand to Commercial Regions") PROPAGACAO_DEMANDA_REGIOES_COMERCIAIS;

        /**
         * Retorna o tipo de location para o qual a demanda direta deve ser
         * propagada antes da execução do plano.
         *
         * <p>O switch é intencionalmente exaustivo para que qualquer novo modo
         * Enterprise adicionado ao enum seja obrigado a declarar seu destino em
         * vez de cair em um comportamento Community silencioso.</p>
         */
        public Location.TipoLocation getTipoLocationDestinoPropagacao() {

            switch (this) {
                case PROPAGACAO_DEMANDA_LOCATIONS_INTERNAS: return LocationAbstract.TipoLocation.INTERNA;
                case PROPAGACAO_DEMANDA_REGIOES_COMERCIAIS: return LocationAbstract.TipoLocation.REGIAO_COMERCIAL;
                default:
                    throw new IllegalStateException(
                            "Modo de propagacao de demanda sem location destino configurada: " + this);
            }

        }

        /**
         * Lista os tipos de location que podem fornecer a demanda original para
         * cada modo de propagação.
         *
         * <p>Esse contrato é usado por projections/factories de Supply Planning
         * para decidir se uma demanda direta deve permanecer na location de
         * origem ou ser deslocada para um nível operacional agregado.</p>
         */
        public List<Location.TipoLocation> getTiposLocationOrigemPropagacao() {

            switch (this) {
                case PROPAGACAO_DEMANDA_LOCATIONS_INTERNAS: return List.of(
                        LocationAbstract.TipoLocation.CLIENTE_FINAL,
                        LocationAbstract.TipoLocation.REGIAO_COMERCIAL);
                case PROPAGACAO_DEMANDA_REGIOES_COMERCIAIS: return List.of(
                        LocationAbstract.TipoLocation.CLIENTE_FINAL);
                default:
                    throw new IllegalStateException(
                            "Modo de propagacao de demanda sem locations origem configuradas: " + this);
            }

        }

        public boolean verificaSeRealizaPropagacao(Location.TipoLocation tipoLocationDeLocationComDemanda) {

            return getTiposLocationOrigemPropagacao().contains(tipoLocationDeLocationComDemanda);

        }

        public boolean verificaSeRealizaPropagacao(Location locationComDemanda) {

            return verificaSeRealizaPropagacao(locationComDemanda.getTipoLocation());

        }
    }

    @Id
    private String id;
    
    private String descricao;
    
    private Boolean geraRequisicoesInbound;
    private Boolean geraOrdensProducaoPlanejadas;
    /** usado para heurístico : 
     * se requisicoes inbound são criadas mas ordens de producao planejadas não,
     * quando true faz com que produtos que poderiam ser produzidos não sejam repostos via requisição
     */
    private Boolean geraRequisicoesInboundParaMateriaisComProducaoViavel;
    
    /**
     * Se true, o calculo sera via DRP independente do modelo de reposicao ser
     * kanban, DRP ou ponto de ressuprimento. O seed Community usa o perfil SNP
     * heuristico como configuracao padrao.
     */
    private Boolean trataPoliticaEstoqueComoDrp;
    
    @Getter(AccessLevel.NONE)
    private Integer horizontePlanoDias;

    /**
     * Define se materiais make-to-order continuam recebendo a parcela futura do
     * Demand Plan.
     *
     * <p>O campo permanece como escalar no aggregate compartilhado para evitar
     * uma tabela lateral apenas para o overlay Enterprise. O runtime Community
     * nao oferece carteira transacional e, portanto, a borda aberta continua
     * forçando o comportamento {@code true}. No Enterprise, {@code false}
     * materializa sell-out e sell-in na carteira da demanda direta e elimina a
     * parcela de forecast somente nos DFUs MTO.</p>
     */
    private Boolean considerForecastForMto;
    
    /** se true, apenas calcula o SNP até o horizonte lead time + política estoque + frequência ressuprimento */
    private Boolean horizontePlanoDinamico;

    /**
     * Legado persistido de quando a rodada restrita podia ser opcional.
     *
     * <p>O heurístico Community sempre executa a rodada restrita após montar o
     * plano irrestrito. O valor permanece mapeado apenas para compatibilidade
     * com perfis já existentes; não pode mais desligar a etapa que materializa
     * o Constrained Plan.</p>
     */
    private Boolean encadeiaExecucaoPlanoRestrito;
    
    /** Indica qual dos planos (restrito ou irrestrito) será copiado como o plano de trabalho */
    @Enumerated(EnumType.ORDINAL)
    private Constantes.TipoPlano tipoPlanoTrabalho;

    /**
     * Motor de execução solicitado pelo perfil.
     *
     * <p>Community implementa somente {@link ModoExecucao#HEURISTICO}. Mesmo
     * assim este campo permanece na entidade porque o schema é compartilhado
     * com Enterprise e porque o service precisa enxergar valores Enterprise
     * persistidos para falhar explicitamente quando o bean privado não estiver
     * no classpath. Nulo continua significando o default Community:
     * heurístico.</p>
     */
    @Enumerated(EnumType.ORDINAL)
    private ModoExecucao modoExecucao;

    /**
     * Flag persistida do overlay Enterprise para substituir, no Supply
     * Planning, a parte passada do Demand Plan por sell-out historico.
     *
     * <p>O aggregate e compartilhado para preservar uma unica tabela e o
     * lifecycle do perfil. O Community mantem a capability bloqueada tanto na
     * API quanto no runtime; por isso consumidores abertos devem continuar
     * usando {@link #getEfetuaCatchUpSelloutNoPlanoDemanda()}, que devolve
     * sempre {@code false}.</p>
     */
    private Boolean enableDemandCatchUpFromPastSellout;

    /**
     * Ativa a redistribuição heurística opcional de produção no plano
     * irrestrito antes da rodada restrita. Nulo preserva perfis existentes.
     */
    private Boolean heuristicUnconstrainedPlanCapacityLeveling;

    /** se true, o plano de transferências será arredondado para cima p/ atender ao lote mínimo e múltiplo */
    private Boolean arredondaRequisicoesLoteMinimoEMultiplo;
    /** se true, o arredondamento de transferências/compras em múltiplos vale para todo o horizonte de expedição */
    @Column(name = "arredonda_requisicoes_lote_minimoemultiplo_em_todos_periodos")
    private Boolean arredondaRequisicoesLoteMinimoEMultiploEmTodosPeriodosExpedicao;
    /** se false o campo acima, limita quantos períodos de expedição a partir do presente serão inteiros */
    @Column(name = "numero_periodos_arredonda_requisicoes_lote_minimoemultiplo")
    private Integer numeroPeriodosExpedicaoArredondaRequisicoesLoteMinimoEMultiplo;
    private Boolean arredondaProducaoLoteMinimoEMultiplo;
    /** se true, o arredondamento de produção em múltiplos vale para todo o horizonte futuro */
    private Boolean arredondaProducaoLoteMinimoEMultiploEmTodosPeriodos;
    /** se false o campo acima, limita quantos períodos a partir do presente serão inteiros */
    private Integer numeroPeriodosArredondaProducaoLoteMinimoEMultiplo;
    /**
     * Ponte Enterprise para arredondamento de compras planejadas.
     *
     * <p>Community não executa compras discretas por otimizador; o getter
     * default mantém o contrato privado desligado quando o overlay não está no
     * classpath.</p>
     */
    private Boolean arredondaComprasPlanejadasLoteMinimo;

    protected Boolean consideraEstoqueInicial;

    /**
     * Ponte de schema para pedidos abertos consumidos pelo optimizer Enterprise.
     *
     * <p>Community nao carrega sales orders, sell-in, compras ou transferencias
     * transacionais no Supply Planning. Mesmo assim estes campos permanecem na
     * entidade compartilhada porque o overlay Enterprise salva o mesmo perfil e
     * o {@code SupplyPlanOptimizationService} decide, pelos getters publicos
     * abaixo, se deve materializar a projection privada de pedidos abertos. A
     * borda Community continua neutralizando estes campos em DTO/service.</p>
     */
    private Boolean consideraOrdensSelloutBacklog;
    private Boolean consideraOrdensSelloutFuturas;
    private Boolean consideraOrdensSellinBacklog;
    private Boolean consideraOrdensSellinFuturas;
    private Boolean consideraOrdensTransferenciaBacklog;
    private Boolean consideraOrdensTransferenciaFuturas;
    private Boolean consideraOrdensCompraBacklog;
    private Boolean consideraOrdensCompraFuturas;

    /**
     * Opt-in Enterprise para carregar demanda nao atendida entre buckets do
     * perfil do otimizador.
     *
     * <p>O campo permanece no aggregate compartilhado para evitar uma tabela
     * satelite para um unico escalar. A borda Community o fixa como
     * {@code false}; o overlay Enterprise so o persiste e executa quando o
     * modo e {@code OTIMIZADOR} e o SPI privado esta presente.</p>
     */
    private Boolean allowBacklogCarryOver;

    /**
     * Opt-in Enterprise para que o optimizer limite a producao planejada pela
     * demanda atendida no mesmo material e periodo.
     *
     * <p>O escalar permanece no aggregate compartilhado para nao criar tabela
     * satelite para uma unica configuracao do perfil. A borda Community o
     * mantem desligado; o overlay Enterprise somente o reabre para perfis de
     * {@code OTIMIZADOR} que possuem o runtime privado capaz de materializar a
     * restricao.</p>
     */
    private Boolean forceMakeToOrderModel;

    /**
     * Flags Enterprise para materializar ordens firmes de producao como input
     * da rodada Supply. Os nomes novos seguem o contrato publico em ingles e
     * nao reutilizam as colunas legadas em portugues: a migracao fisica dos
     * perfis existentes sera tratada na entrada em producao.
     *
     * <p>O Community mantem seus getters historicos de ordens de producao
     * efetivamente desligados. O overlay Enterprise le estes campos por seus
     * getters em ingles e os consome somente pelo SPI privado.</p>
     */
    private Boolean considerFirmProductionOrdersBacklog;
    private Boolean considerFirmProductionOrdersFuture;

    /** se true, plano heurístico de inbound das locations cliente será igual à demanda defasada do lead time p/ a origem prioritária */
    private Boolean locationsClienteApenasPropagamDemanda;

    @Enumerated(EnumType.ORDINAL)
    private ModoPropagacaoDemanda modoPropagacaoDemanda;

    /** se true, o plano é armazenado e resgatado para os relatórios / planning books. 
     * se false, apenas o estoque inicial e o estoque em trânsito são salvos. o estoque projetado sempre será recalculado */
    private Boolean salvaInventoryPlan;

    /**
     * Solicita o backtracking causal das restrições que impediram o atendimento
     * de demanda no Supply Plan.
     *
     * <p>O campo pertence ao perfil compartilhado porque a opção é parte da
     * rodada de Supply Plan, embora a geração e a persistência do Constraint
     * Tracker sejam capability Enterprise. Valor ausente preserva o contrato
     * histórico: a análise é habilitada por padrão no overlay Enterprise.</p>
     */
    private Boolean saveConstraintBacktracking;
        
    /** MIN_MAX ou ESTOQUE_MEDIO */
    private ModeloEstoqueTarget modeloEstoqueTarget;
    
    // PARÂMETROS DO PLANO IRRESTRITO COMMUNITY ------------------------------------------------------------------------------------

    /**
     * O plano irrestrito Community é uma visão quantitativa do heurístico.
     *
     * <p>Campos de otimizador, função objetivo, margem, lead time econômico,
     * P&L e cost-to-serve podem existir aqui como ponte de schema entre
     * edições, mas não têm semântica executável no Community. A única engine
     * Community é o heurístico; esses parâmetros só passam a influenciar
     * resultado quando o overlay Enterprise executa optimizer/process chain.</p>
     */
    private Boolean geraPlanoIrrestrito;

    // Parametro especifico do plano irrestrito Community: apenas producao e uma restricao propria desta edicao.
    private Boolean ignoraRestricaoProducaoIrrestrito;

    /*
     * Ponte de schema/contrato para o optimizer Enterprise.
     *
     * O Community nao usa estes parametros em calculos: a unica engine
     * disponivel e heuristica. Ainda assim a entidade fica larga para que o
     * Enterprise possa consumir a mesma tabela, DTOs e copias de perfil sem
     * criar uma hierarquia JPA paralela e fragil. Quando uma capability
     * Enterprise precisar de dados privados, ela deve usar um overlay,
     * projection tipada ou uma ponte escalar explicitamente nomeada; o
     * aggregate Community nao expõe referencias genericas a tipos privados.
     */
    private Boolean ignoraRestricaoArmazenagemIrrestrito;
    private Boolean ignoraRestricaoOutboundIrrestrito;
    private Boolean ignoraRestricaoInboundIrrestrito;
    private Boolean ignoraRestricaoLeadTimeIrrestrito;
    @Column(name = "impacto_reducao_lt_custo_transf")
    private Double impactoMaximoReducaoLeadTimeCustoTransferencia;
    @Column(name = "impacto_reducao_lt_custo_mat")
    private Double impactoMaximoReducaoLeadTimeCustoTransferenciaCustoMaterial;
    private Boolean ignoraRestricaoMargemIrrestrito;
    @Column(name = "inc_atend_demanda_obj_irrestrito")
    private Double incrementoPercentualAtendimentoDemandaFuncaoObjetivo;
    @Column(name = "valor_min_atend_demanda_obj")
    private Double valorMinimoAtendimentoDemandaFuncaoObjetivo;

    private Boolean salvaVariaveisERestricoesOtimizador;
    private Boolean alocaTransferenciasNasFrotas;
    private Boolean consideraRestricaoArmazenagem;
    private Boolean consideraRestricaoOutbound;
    private Boolean consideraRestricaoInbound;

    private Boolean penalizaDemandaNaoAtendida;
    private Double percentualPenalizacaoDemandaNaoAtendidaFatFunObj;
    private Double penalizacaoUnitariaDemandaNaoAtendida;
    @Getter(AccessLevel.NONE)
    @ManyToOne
    private UnidadeMedida unidadeMedidaPenalizacaoUnitariaDemandaNaoAtendida;

    @Enumerated(EnumType.ORDINAL)
    private Constantes.TipoQuantidadeValor tipoValorFaturamentoFuncaoObjetivo;

    @Getter(AccessLevel.NONE)
    @ManyToOne
    private UnidadeMedida unidadeMedidaOtimizacao;
    private Double valorUnitarioVenda;

    private Boolean consideraCustoFixoLocation;
    private Boolean consideraCustoFixoRecursoProdutivo;
    private Boolean consideraCustoArmazenagem;
    private Boolean consideraCustoTransferencia;
    private Boolean consideraValorImpostosLinhaTransporte;
    private Boolean consideraCustosInboundOutbound;
    private Boolean consideraCustoProducao;
    private Boolean consideraPrecoFornecedor;

    @Enumerated(EnumType.ORDINAL)
    private ModoApuracaoImpostos modoApuracaoImpostos;

    /**
     * Habilita a geracao de Profit/Loss pelo lifecycle Enterprise futuro.
     *
     * <p>O atributo fica no perfil compartilhado para preservar um aggregate
     * unico e evitar uma tabela auxiliar de configuracao. O Community apenas
     * armazena e expoe a preferencia: este recorte nao conecta task, motor ou
     * execucao automatica de DRE.</p>
     */
    private Boolean generateProfitLoss;

    /**
     * Permite retroagir impacto de vendas aos materiais de entrada via BOM.
     *
     * <p>O default conserva a semantica historica da DRE. A regra somente sera
     * consumida pelo motor Finance Enterprise quando ele for migrado.</p>
     */
    private Boolean allowSalesProfitLossBomRetroaction;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Enumerated(EnumType.ORDINAL)
    private OtimizadorInteligenciaArtificial otimizadorInteligenciaArtificial;

    /**
     * Habilita a capability Enterprise de sequenciamento tatico de linha no
     * modelo CP-SAT. O atributo permanece escalar no perfil compartilhado para
     * evitar uma configuracao paralela; o Community apenas o preserva como
     * contrato e bloqueia sua ativacao na propria borda.
     */
    private Boolean enableLineSequencing;

    /**
     * Habilita a capability Enterprise de decisao Greenfield/Brownfield no
     * modelo CP-SAT. O Community preserva o escalar para manter uma unica
     * tabela do perfil, mas nunca o materializa como comportamento proprio.
     */
    private Boolean enableGreenfieldBrownfield;

    private Boolean aplicaFairShareSafetyStock;
    private Integer numeroSegmentosLinearizacaoGapDemandaDireta;
    private Integer numeroSegmentosLinearizacaoGapSafetyStock;
    private Double penalidadePercentualFairShareDemandaNaoAtendida;
    private Double penalidadePercentualFairShareEstoqueSegurancaNaoAtendido;
    private Double coeficienteImpactoDemandaAtendidaPlanoDemanda;
    private Double coeficienteImpactoDemandaAtendidaCarteira;
    private Boolean incrementaImpactoFuncaoObjetivoPrimeirosPeriodos;
    private Double incrementoPercentualImpactoFuncaoObjetivoPrimeiroPeriodo;
    @Enumerated(EnumType.STRING)
    private ModeloDecaimentoImpactoTemporal modeloDecaimentoImpactoTemporalFuncaoObjetivo;
    private Double fatorDecaimentoExponencialImpactoTemporalFuncaoObjetivo;
    private Double pisoMultiplicadorImpactoTemporalFuncaoObjetivo;
    private Boolean segmentaEstoqueEmLotes;
    private Boolean incrementaImpactoWorkingCapitalLotesMaisAntigos;
    @Column(name = "inc_pct_max_imp_work_cap_lote_antigo")
    private Double incrementoPercentualMaximoImpactoWorkingCapitalLoteMaisAntigo;
    private Double bigMOtimizador;
    private Double custoPercentualWorkingCapital;
    private Long numeroMaximoMinutosOtimizacao;

    /*
     * Parâmetros escalares dos solvers combinatórios Enterprise.
     *
     * Eles pertencem ao mesmo agregado de configuração e não referenciam
     * nenhuma entidade privada. Por isso são persistidos diretamente no
     * Community, preservando uma única tabela para o perfil compartilhado.
     */
    private Double entityTabuRatio;
    private Integer acceptedCountLimit;

    /** Limita o custo agregado de ativação de locations Greenfield. */
    private Boolean considerBudgetForGreenfieldLocationActivation;
    private Double greenfieldLocationActivationBudget;

    /**
     * Quando ativo, uma produção ou setup parcial ocupa o bucket detalhado
     * inteiro no scheduling SNP Enterprise.
     */
    private Boolean roundProductionAndSetupsToDetailedPlanBucket;

    private Double coeficienteImpactoGapSafetyStock;
    private Boolean permiteEstoqueEmClientes;
    private Boolean permiteEstoqueEmPontosTransbordo;
    private Double penalidadePercentualDiferencaParaTargetVariavel;
    private Double amplitudeDesvioComoPercentualValorTarget;
    private Integer numeroSegmentosLinearizacaoDiferencaVariavelTarget;
    private Double incentivoPercentualAtendimentoOrdemFirmeSobreCogs;

    /*
     * Identificador persistido do modelo matematico de otimizacao.
     *
     * O Community preserva somente a representacao textual publica. O overlay
     * Enterprise resolve o identificador para seu enum privado antes de montar
     * o solver, sem introduzir uma dependencia Maven reversa no Community.
     */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String tipoModeloOtimizacao;

    /*
     * Pontes fisicas para modelos de priorizacao Enterprise.
     *
     * As entidades privadas vivem no modulo Enterprise. O perfil compartilhado
     * persiste apenas seus ids para que o overlay privado consiga materializar
     * projections sem criar dependencia Maven reversa. O runtime migrado usa
     * apenas ids e projections tipadas, sem objetos Enterprise transientes.
     */
    private String modeloPriorizacaoDemandaId;
    private String modeloPriorizacaoSafetyStockId;

    /*
     * Ponte fisica para filtros de material Enterprise.
     *
     * O nome publico novo e `materialFilterId`, alinhado ao front e ao dominio
     * Community/Enterprise. A coluna fisica preserva o legado
     * `filtro_produtos_id` para que o schema compartilhado continue lendo os
     * perfis existentes. O Community nunca resolve esse id para entidade nem
     * permite salva-lo pela borda publica; o overlay Enterprise valida o
     * cadastro privado e persiste somente a chave escalar.
     */
    @Column(name = "filtro_produtos_id")
    private String materialFilterId;

    /*
     * Ponte fisica para curvas logisticas Enterprise.
     *
     * A entidade privada `CurvasCustoLogistico` nao pode ser referenciada pelo
     * Community. Por isso o perfil compartilhado persiste apenas o id e os
     * booleans de aplicacao. O overlay Enterprise resolve esse id na projection
     * privada do optimizer. O runtime migrado usa somente o id e a projection
     * tipada, sem carregar objeto Enterprise transiente na entidade Community.
     */
    private Long curvasCustoLogisticoId;
    private Boolean aplicaCurvasCustoFrete;
    private Boolean aplicaCurvasCustoLocation;

    /**
     * Ponte Enterprise para estimativa de COGS via lista técnica.
     *
     * <p>Valores econômicos não existem no Community; portanto, o default
     * operacional é sempre falso.</p>
     */
    private Boolean estimaCogsUnitarioParaWorkingCapitalEPoliticaEstoques;

    // Parâmetros usados tanto para modelo Quantidade
    /** se true, o plano será restringido de acordo com capacidades . se falso, restrições não serão aplicadas */
    private Boolean consideraRestricaoProducao;

    /**
     * Fair share quantitativo da demanda direta usado pelo heurístico Community.
     *
     * <p>O plano restrito heuristico aplica reducao proporcional sempre que ha
     * indisponibilidade quantitativa. Portanto, no Community este parametro e
     * normalizado como ligado. Penalidades, linearizacoes e fair share de
     * safety stock pertencem ao modelo otimizado Enterprise: quando presentes
     * nesta entidade, permanecem apenas como ponte de schema e nao alteram a
     * execucao heuristica Community.</p>
     */
    private Boolean aplicaFairShareDemandaDireta;

    /**
     * Nivel de capacidade logistica usado pelo optimizer Enterprise.
     *
     * <p>Community nao permite selecionar este campo na API, mas a entidade
     * compartilhada precisa preservar o estado para o overlay privado. O default
     * funcional continua alinhado ao legado: capacidade no nivel da location.</p>
     */
    @Enumerated(EnumType.ORDINAL)
    private TipoCapacidadeLogistica tipoCapacidadeLogistica;

    @OneToMany(cascade=CascadeType.ALL, mappedBy="perfilExecucaoPoliticaEstoquesCompositeKey.perfilExecucaoSupplyPlan", orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<PerfilExecucaoPoliticaEstoques> setPerfilExecucaoPoliticaEstoques = new HashSet<>();

    public ModeloMajoracaoDemandaDireta getModeloMajoracaoDemandaDiretaProjecaoEstoque() {
        /*
         * Community usa Demand Plan como unica fonte de demanda direta futura.
         * Carteira de clientes, sell-in e outros documentos transacionais sao
         * Enterprise e nao possuem estado persistido nesta entidade.
         */
        return ModeloMajoracaoDemandaDireta.SOMENTE_FORECAST;
    }
    public ModeloMajoracaoDemandaDireta getModeloMajoracaoDemandaDiretaProjecaoEstoque(Location location) {
        return getModeloMajoracaoDemandaDiretaProjecaoEstoque();
    }
    
    public ModeloMajoracaoDemandaDireta getModeloMajoracaoDemandaDiretaEstoqueSeguranca() {
        /*
         * Safety stock Community tambem parte apenas do Demand Plan. Modelos
         * que conciliam forecast com carteira pertencem ao Enterprise.
         */
        return ModeloMajoracaoDemandaDireta.SOMENTE_FORECAST;
    }
    public ModeloMajoracaoDemandaDireta getModeloMajoracaoDemandaDiretaEstoqueSeguranca(Location location) {
        return getModeloMajoracaoDemandaDiretaEstoqueSeguranca();
    }
    
    public int getHorizonteCarteiraComoForecastDias() {
        /*
         * Nao ha janela de carteira no Community. O valor zero deixa claro que
         * qualquer chamada remanescente existe apenas por compatibilidade com
         * rotinas compartilhadas que tambem rodam no Enterprise.
         */
        return 0;
    }
    public int getHorizonteCarteiraComoForecastDias(Location location) {
        return getHorizonteCarteiraComoForecastDias();
    }
    
    public boolean getGeraRequisicoesInbound() {
        return (geraRequisicoesInbound == null) ? true : geraRequisicoesInbound;
    }
    public boolean getGeraRequisicoesInbound(Location location) {
        return getGeraRequisicoesInbound();
    }
    
    public boolean getGeraOrdensProducaoPlanejadas() {
        return (geraOrdensProducaoPlanejadas == null) ? true : geraOrdensProducaoPlanejadas;
    }
    public boolean getGeraOrdensProducaoPlanejadas(Location location) {
        return getGeraOrdensProducaoPlanejadas();
    }

    public boolean getTrataPoliticaEstoqueComoDrp() {
        return (trataPoliticaEstoqueComoDrp == null) ? true : trataPoliticaEstoqueComoDrp;
    }
    public boolean getTrataPoliticaEstoqueComoDrp(Location location) {
        return getTrataPoliticaEstoqueComoDrp();
    }
    
    public int getHorizontePlanoDias(ParametrosGlobais parametrosGlobais) {

        if (horizontePlanoDias == null) {
            return parametrosGlobais.getHorizonteForecastDias();
        }
        return getInteiroOperacionalPositivoCadastrado(
                horizontePlanoDias,
                "Supply planning plan horizon in days");

    }
    
    /**
     * Retorna o horizonte usado para criar calendarios do Supply Planning.
     *
     * <p>Overrides por location pertencem ao Enterprise; Community considera
     * somente o horizonte global do perfil.</p>
     *
     * @param parametrosGlobais
     * @return 
     */
    public int getHorizontePlanoDiasMaximo(ParametrosGlobais parametrosGlobais) {

        return getHorizontePlanoDias(parametrosGlobais);

    }
    public int getUltimoPeriodoFuturoHorizonteAPartirPeriodoPresente(Location location, Calendario calendario, ClusterEParametrosProjection clusterEParametrosProjection) {
        int numeroDiasHorizonte = getHorizontePlanoDias(clusterEParametrosProjection, location);
        int posicaoPeriodoPresente = calendario.getPosicaoPeriodoPresente();

        /*
         * O horizonte em dias deve ser inclusivo em relação ao período presente:
         * um horizonte de 24 dias sobre um calendário diário precisa cobrir os
         * buckets 0..23, e não 0..24.
         *
         * Também é importante limitar o retorno ao último período futuro do
         * próprio calendário, porque o Calendario.getPosicaoPeriodo(...) aceita
         * datas fora do range e retornaria um bucket "virtual" adicional.
         */
        int offsetDiasAteUltimoPeriodoDentroHorizonte = Math.max(0, numeroDiasHorizonte - 1);
        int ultimoPeriodoDentroHorizonte = calendario.getPosicaoPeriodoAposOffsetDoInicioPeriodoReferencia(
                posicaoPeriodoPresente,
                offsetDiasAteUltimoPeriodoDentroHorizonte,
                Constantes.TamanhoBucket.DIARIO);

        return Math.min(calendario.getPosicaoPeriodoFinalFuturo(), ultimoPeriodoDentroHorizonte);
    }
    public int getHorizontePlanoDias(ClusterEParametrosProjection clusterEParametrosProjection, Location location) {
        
        ParametrosGlobais parametrosGlobais = clusterEParametrosProjection.getParametrosGlobais();
        return getHorizontePlanoDias(parametrosGlobais);

    }
    
    public Integer getHorizontePlanoDiasCadastrado() {
        return horizontePlanoDias;
    }
    
    public boolean getConsideraForecastParaMto() {
        return !Boolean.FALSE.equals(considerForecastForMto);
    }
    
    public boolean getEncadeiaExecucaoPlanoRestrito() {
        /*
         * A execução restrita é obrigatória: o plano irrestrito é o baseline
         * gerado antes da restrição e o plano de trabalho pode escolher uma das
         * duas séries já materializadas. Perfis antigos com false não podem
         * deixar o Constrained Plan vazio.
         */
        return true;
    }
    
    public ModoExecucao getModoExecucao() {
        /*
         * Community executa somente HEURISTICO, mas nao deve apagar uma selecao
         * Enterprise antes do SupplyPlanService. Valores OTIMIZADOR e
         * PROCESS_CHAIN precisam chegar ao service para que a ausencia da SPI
         * privada gere RequiresEnterpriseVersionException; no Enterprise, os
         * mesmos valores passam a ser delegados para os beans reais.
         */
        return (modoExecucao == null) ? ModoExecucao.HEURISTICO : modoExecucao;
    }
    
    public boolean getArredondaRequisicoesLoteMinimoEMultiplo() {
        return (arredondaRequisicoesLoteMinimoEMultiplo == null) ? false : arredondaRequisicoesLoteMinimoEMultiplo;
    }

    public boolean getArredondaRequisicoesLoteMinimoEMultiploEmTodosPeriodosExpedicao() {
        return (arredondaRequisicoesLoteMinimoEMultiploEmTodosPeriodosExpedicao == null)
                ? false
                : arredondaRequisicoesLoteMinimoEMultiploEmTodosPeriodosExpedicao;
    }

    public int getNumeroPeriodosExpedicaoArredondaRequisicoesLoteMinimoEMultiplo() {

        return getInteiroOperacionalPositivoOuDefault(
                numeroPeriodosExpedicaoArredondaRequisicoesLoteMinimoEMultiplo,
                1,
                "Supply planning requisition MOQ/lot-size rounding expedition periods");

    }

    /**
     * O horizonte de integridade para transferências é configurado em número de períodos
     * de expedição, e não em dias corridos nem em períodos de recebimento.
     * Assim, para bucket diário o valor representa dias de saída; para bucket semanal,
     * semanas de saída; etc.
     *
     * Importante: esta regra não desliga a série base de fluxo/inbound planejado.
     * Ela apenas decide em quais períodos a camada auxiliar inteira de "número de múltiplos"
     * deve ser criada, preservando os fluxos planejados contínuos fora da janela configurada.
     */
    public boolean getArredondaRequisicoesLoteMinimoEMultiploNoPeriodoExpedicao(int periodoExpedicao, Calendario calendario) {
        if (!getArredondaRequisicoesLoteMinimoEMultiplo()) {
            return false;
        }
        if (periodoExpedicao < calendario.getPosicaoPeriodoPresente() || periodoExpedicao > calendario.getPosicaoPeriodoFinalFuturo()) {
            return false;
        }
        if (getArredondaRequisicoesLoteMinimoEMultiploEmTodosPeriodosExpedicao()) {
            return true;
        }
        return periodoExpedicao <= getUltimoPeriodoComArredondamentoConfigurado(
                calendario,
                getNumeroPeriodosExpedicaoArredondaRequisicoesLoteMinimoEMultiplo());
    }
    
    public boolean getArredondaProducaoLoteMinimoEMultiplo() {
        return (arredondaProducaoLoteMinimoEMultiplo == null) ? false : arredondaProducaoLoteMinimoEMultiplo;
    }

    public boolean getArredondaProducaoLoteMinimoEMultiploEmTodosPeriodos() {
        return (arredondaProducaoLoteMinimoEMultiploEmTodosPeriodos == null)
                ? false
                : arredondaProducaoLoteMinimoEMultiploEmTodosPeriodos;
    }

    public int getNumeroPeriodosArredondaProducaoLoteMinimoEMultiplo() {

        return getInteiroOperacionalPositivoOuDefault(
                numeroPeriodosArredondaProducaoLoteMinimoEMultiplo,
                1,
                "Supply planning production MOQ/lot-size rounding periods");

    }

    /**
     * Resolve inteiros operacionais que aceitam default nulo, mas nao podem
     * mascarar valor explicitamente cadastrado como zero ou negativo.
     *
     * <p>A borda de front Community ja bloqueia payloads novos invalidos antes
     * do repository. Esta validacao local protege snapshots antigos, cargas
     * diretas e consumidores Enterprise que consultam a entidade sem passar
     * pelo service de configuracao.</p>
     */
    private int getInteiroOperacionalPositivoOuDefault(
            Integer valorOperacionalCadastrado,
            int valorDefault,
            String descricaoCampo) {

        if (valorOperacionalCadastrado == null) {
            return valorDefault;
        }
        return getInteiroOperacionalPositivoCadastrado(
                valorOperacionalCadastrado,
                descricaoCampo);

    }

    /**
     * Valida inteiros operacionais explicitamente cadastrados no perfil.
     */
    private int getInteiroOperacionalPositivoCadastrado(
            Integer valorOperacionalCadastrado,
            String descricaoCampo) {

        if (valorOperacionalCadastrado <= 0) {
            throw new IllegalStateException(
                    descricaoCampo
                            + " must be positive when explicitly configured: "
                            + valorOperacionalCadastrado
                            + ".");
        }
        return valorOperacionalCadastrado;

    }

    /**
     * Mantém a janela inteira de produção alinhada ao bucket do calendário corrente.
     *
     * A produção planejada total continua existindo em todo o horizonte.
     * O filtro abaixo atua apenas sobre a variável auxiliar inteira que representa
     * a quantidade de múltiplos/lotes, reduzindo a combinatória do modelo depois
     * da janela configurada.
     */
    public boolean getArredondaProducaoLoteMinimoEMultiploNoPeriodo(int periodo, Calendario calendario) {
        if (!getArredondaProducaoLoteMinimoEMultiplo()) {
            return false;
        }
        if (periodo < calendario.getPosicaoPeriodoPresente() || periodo > calendario.getPosicaoPeriodoFinalFuturo()) {
            return false;
        }
        if (getArredondaProducaoLoteMinimoEMultiploEmTodosPeriodos()) {
            return true;
        }
        return periodo <= getUltimoPeriodoComArredondamentoConfigurado(
                calendario,
                getNumeroPeriodosArredondaProducaoLoteMinimoEMultiplo());
    }

    public boolean getArredondaComprasPlanejadasLoteMinimo() {

        /*
         * Compras planejadas discretas sao uma capacidade do otimizador
         * Enterprise. A ausencia de valor fica neutra, mas um valor preenchido
         * precisa ser preservado para o overlay Enterprise, que consome esta
         * mesma entidade compartilhada. No Community, DTO/service bloqueiam a
         * configuracao antes da execucao e o motor heuristico nao le este campo.
         */
        return arredondaComprasPlanejadasLoteMinimo == null
                ? false
                : arredondaComprasPlanejadasLoteMinimo;

    }

    private int getUltimoPeriodoComArredondamentoConfigurado(Calendario calendario, int numeroPeriodosComArredondamento) {
        /*
         * A configuração é inclusiva a partir do período presente:
         * 1 período = apenas o bucket presente;
         * 2 períodos = presente + 1 futuro;
         * etc.
         */
        int offsetPeriodos = Math.max(0, numeroPeriodosComArredondamento - 1);
        return Math.min(
                calendario.getPosicaoPeriodoFinalFuturo(),
                calendario.getPosicaoPeriodoPresente() + offsetPeriodos);
    }

    public Constantes.TipoPlano getTipoPlanoTrabalho() {
        switch (getModoExecucao()) {
            case HEURISTICO:
                if (tipoPlanoTrabalho != null) return tipoPlanoTrabalho;
                return Constantes.TipoPlano.PLANO_RESTRITO;
            case OTIMIZADOR:
                return (tipoPlanoTrabalho == null) ? Constantes.TipoPlano.PLANO_RESTRITO : tipoPlanoTrabalho;
            default:
                return (tipoPlanoTrabalho == null) ? Constantes.TipoPlano.PLANO_IRRESTRITO : tipoPlanoTrabalho;
        }
    }

    public boolean getConsideraOrdensSelloutBacklog() {

        return consideraOrdensSelloutBacklog == null ? false : consideraOrdensSelloutBacklog;

    }

    public boolean getConsideraOrdensSellinBacklog() {

        return consideraOrdensSellinBacklog == null ? false : consideraOrdensSellinBacklog;

    }

    public boolean getConsideraOrdensTransferenciaBacklog() {

        return consideraOrdensTransferenciaBacklog == null ? false : consideraOrdensTransferenciaBacklog;

    }

    public boolean getConsideraOrdensCompraBacklog() {

        return consideraOrdensCompraBacklog == null ? false : consideraOrdensCompraBacklog;

    }

    /**
     * Ordens de producao abertas/firmes sao dado transacional Enterprise.
     *
     * <p>O Community pode gerar sugestoes e ordens planejadas no proprio plano
     * heuristico, mas nao importa nem reutiliza ordens firmes abertas como
     * entrada de demanda/capacidade. Por isso este getter fica fixo em
     * {@code false}, mesmo quando um banco legado ainda tiver valor salvo.</p>
     */
    public boolean getConsideraOrdensProducaoBacklog() {

        return false;

    }

    public boolean getPermiteBacklogDemanda() {

        return getAllowBacklogCarryOver();

    }

    /**
     * Retorna a escolha persistida de carregar backlog entre buckets.
     *
     * <p>Registros anteriores ao campo e perfis que nao passaram pelo overlay
     * Enterprise mantem comportamento seguro: {@code false}. A autorizacao de
     * runtime continua centralizada no {@code SupplyPlanService} para impedir
     * que o Community puro execute uma configuracao herdada de banco.</p>
     */
    public boolean getAllowBacklogCarryOver() {

        return Boolean.TRUE.equals(allowBacklogCarryOver);

    }

    public boolean getForcaModeloMakeToOrder() {

        return Boolean.TRUE.equals(forceMakeToOrderModel);

    }

    /**
     * Indica se a demanda das locations cliente deve ser consolidada na sua
     * origem de propagacao. Perfis sem valor persistido mantem a demanda nas
     * locations cliente, para que a consolidacao seja sempre uma escolha
     * explicita do usuario.
     */
    public boolean getLocationsClienteApenasPropagamDemanda() {

        return Boolean.TRUE.equals(locationsClienteApenasPropagamDemanda);

    }

    public ModoPropagacaoDemanda getModoPropagacaoDemanda() {
        return (modoPropagacaoDemanda == null) ? ModoPropagacaoDemanda.PROPAGACAO_DEMANDA_LOCATIONS_INTERNAS : modoPropagacaoDemanda;
    }
    
    /**
     * Retorna true se o periodo estiver dentro da janela Enterprise em que
     * client orders seriam considerados como demanda direta.
     *
     * <p>No Community, {@link #getHorizonteCarteiraComoForecastDias()} retorna
     * zero e a chamada permanece apenas por compatibilidade de fluxo.</p>
     */
    public boolean verificaSePeriodoDentroDoHorizonteCarteira(int posicaoPeriodo, Calendario calendario) {
        LocalDateTime dataInicioPeriodoPresenteCalendario = calendario.getPrimeiraDataHorarioPeriodo(calendario.getPosicaoPeriodoPresente());
        int ultimaPosicaoPeriodoComCarteira = calendario.getPosicaoPeriodo(
                dataInicioPeriodoPresenteCalendario.plusDays(
                        Math.max(0, getHorizonteCarteiraComoForecastDias() - 1)));
        return (posicaoPeriodo <= ultimaPosicaoPeriodoComCarteira);
    }
    
    public boolean getHorizontePlanoDinamico() {
        return (horizontePlanoDinamico == null) ? false : horizontePlanoDinamico;
    }
    public boolean getHorizontePlanoDinamico(Location location) {
        return getHorizontePlanoDinamico();
    }
    
    public boolean getSalvaInventoryPlan() {
        return (salvaInventoryPlan == null) ? true : salvaInventoryPlan;
    }

    /**
     * Mantém o nivelamento desligado até que o perfil o habilite
     * explicitamente, preservando a semântica histórica da rodada Community.
     */
    public boolean getHeuristicUnconstrainedPlanCapacityLeveling() {

        return heuristicUnconstrainedPlanCapacityLeveling != null
                && heuristicUnconstrainedPlanCapacityLeveling;

    }

    /**
     * Informa se o overlay Enterprise deve persistir a explicabilidade causal
     * da rodada após salvar os outputs do Supply Plan.
     *
     * <p>O default {@code true} mantém a semântica histórica quando a coluna
     * ainda não foi materializada em uma base migrada. O Community não consome
     * o valor; a capability privada é acionada somente pelo runtime Enterprise.</p>
     */
    public boolean getSaveConstraintBacktracking() {

        return saveConstraintBacktracking == null || saveConstraintBacktracking;

    }

    public boolean getConsideraRestricaoProducao() {
        return (consideraRestricaoProducao == null) ? true : consideraRestricaoProducao;
    }
    public boolean getConsideraRestricaoProducao(Location location) {
        return getConsideraRestricaoProducao();
    }

    public boolean getConsideraRestricaoArmazenagem() {
        /*
         * Restricoes de armazenagem dependem de capacidade logistica fisica por
         * location/deposito, recurso Enterprise removido do Community.
         */
        return (consideraRestricaoArmazenagem == null) ? false : consideraRestricaoArmazenagem;
    }
    public boolean getConsideraRestricaoArmazenagem(Location location) {
        return getConsideraRestricaoArmazenagem();
    }

    public boolean getConsideraRestricaoOutbound() {
        /*
         * Restricoes outbound logisticas pertencem ao Enterprise. Community
         * restringe apenas disponibilidade, insumos e capacidade produtiva.
         */
        return (consideraRestricaoOutbound == null) ? false : consideraRestricaoOutbound;
    }
    public boolean getConsideraRestricaoOutbound(Location location) {
        return getConsideraRestricaoOutbound();
    }

    public boolean getConsideraRestricaoInbound() {
        /*
         * Restricoes inbound logisticas pertencem ao Enterprise. Community
         * restringe apenas disponibilidade, insumos e capacidade produtiva.
         */
        return (consideraRestricaoInbound == null) ? false : consideraRestricaoInbound;
    }
    public boolean getConsideraRestricaoInbound(Location location) {
        return getConsideraRestricaoInbound();
    }

    /**
     * AI optimizer e suas variantes sao capacidades Enterprise. O Community
     * mantem o enum apenas como contrato compartilhado com DTO/front, mas nao
     * persiste configuracao nem permite valor diferente de DESABILITADO.
     *
     * @return
     */
    public OtimizadorInteligenciaArtificial getOtimizadorInteligenciaArtificial() {

        return otimizadorInteligenciaArtificial == null
                ? OtimizadorInteligenciaArtificial.DESABILITADO
                : otimizadorInteligenciaArtificial;

    }

    public void setOtimizadorInteligenciaArtificial(
            OtimizadorInteligenciaArtificial otimizadorInteligenciaArtificial) {

        this.otimizadorInteligenciaArtificial = otimizadorInteligenciaArtificial;

    }

    /**
     * Retorna se o perfil solicita o sequenciamento tatico de linha Enterprise.
     * A ausencia de configuracao preserva o comportamento historico sem
     * sequenciamento discreto.
     */
    public boolean getEnableLineSequencing() {

        return Boolean.TRUE.equals(enableLineSequencing);

    }

    /**
     * Retorna se o perfil solicita a capability Enterprise Greenfield/Brownfield.
     * A ausencia preserva o comportamento historico sem decisao de ativacao
     * de locations.
     */
    public boolean getEnableGreenfieldBrownfield() {

        return Boolean.TRUE.equals(enableGreenfieldBrownfield);

    }

    public boolean getAplicaFairShareDemandaDireta() {
        /*
         * O registro historico e nulavel e, no Community, a ausencia sempre
         * preserva o comportamento proporcional original. O valor explicito
         * {@code false} e reservado ao overlay Enterprise do OTIMIZADOR, que
         * usa a mesma coluna compartilhada sem introduzir uma tabela paralela.
         */
        return aplicaFairShareDemandaDireta == null || aplicaFairShareDemandaDireta;
    }

    public boolean getExecutaSupplyPlanParaTodasLocations() {
        /*
         * Location-level execution profile e Enterprise. Community sempre usa
         * todas as locations ativas do escopo base do perfil.
         */
        return true;
    }
    public boolean getExecutaSupplyPlanParaLocation(Location location) {
        return true;
    }
    
    public boolean getConsideraEstoqueInicial() {
        return (consideraEstoqueInicial == null) ? true : consideraEstoqueInicial;
    }
    public boolean getConsideraOrdensSellout() {
        return getConsideraOrdensSelloutFuturas() || getConsideraOrdensSelloutBacklog();
    }
    public boolean getConsideraOrdensSelloutFuturas() {

        return consideraOrdensSelloutFuturas == null ? false : consideraOrdensSelloutFuturas;

    }
    public boolean getConsideraOrdensSellin() {
        return getConsideraOrdensSellinFuturas() || getConsideraOrdensSellinBacklog();
    }
    public boolean getConsideraOrdensSellinFuturas() {

        return consideraOrdensSellinFuturas == null ? false : consideraOrdensSellinFuturas;

    }
    public boolean getConsideraOrdensTransferencia() {
        return getConsideraOrdensTransferenciaFuturas() || getConsideraOrdensTransferenciaBacklog();
    }
    public boolean getConsideraOrdensTransferenciaFuturas() {

        return consideraOrdensTransferenciaFuturas == null ? false : consideraOrdensTransferenciaFuturas;

    }
    public boolean getConsideraOrdensCompra() {
        return getConsideraOrdensCompraFuturas() || getConsideraOrdensCompraBacklog();
    }
    public boolean getConsideraOrdensCompraFuturas() {

        return consideraOrdensCompraFuturas == null ? false : consideraOrdensCompraFuturas;

    }

    public boolean getConsideraOrdensProducao() {
        return getConsideraOrdensProducaoFuturas() || getConsideraOrdensProducaoBacklog();
    }
    /**
     * Mantem fora do Community ordens firmes futuras recebidas de sistemas
     * transacionais.
     *
     * <p>Reabrir esse dado exige projection/factory Enterprise propria. O
     * optimizer privado ja reabre alguns pedidos abertos, mas ainda nao ordens
     * firmes de producao, entao o contrato compartilhado permanece neutro aqui.</p>
     */
    public boolean getConsideraOrdensProducaoFuturas() {

        return false;

    }

    /**
     * Retorna a configuracao Enterprise persistida para incluir backlog de
     * ordens firmes de producao.
     *
     * <p>Este getter nao reabre a capability no Community: os fluxos abertos
     * continuam consultando os getters historicos em portugues, que retornam
     * {@code false}. O nome explicito em ingles evita que um consumidor
     * Community ative a fonte transacional por acidente.</p>
     */
    public boolean getConsiderFirmProductionOrdersBacklog() {

        return Boolean.TRUE.equals(considerFirmProductionOrdersBacklog);

    }

    /**
     * Retorna a configuracao Enterprise persistida para incluir ordens firmes
     * com termino planejado no horizonte futuro.
     */
    public boolean getConsiderFirmProductionOrdersFuture() {

        return Boolean.TRUE.equals(considerFirmProductionOrdersFuture);

    }
    
    public boolean getEfetuaCatchUpSelloutNoPlanoDemanda() {
        /*
         * Demand catch-up por sell-out passado pertence ao Enterprise.
         */
        return false;
    }

    /**
     * Retorna a escolha Enterprise persistida sem reabrir a capability no
     * Community.
     *
     * <p>Registros anteriores ao campo e payloads sem valor mantem o default
     * seguro desligado.</p>
     */
    public boolean getEnableDemandCatchUpFromPastSellout() {

        return Boolean.TRUE.equals(enableDemandCatchUpFromPastSellout);

    }

    public boolean getGeraRequisicoesInboundParaMateriaisComProducaoViavel() {
        return (geraRequisicoesInboundParaMateriaisComProducaoViavel == null) ? true : geraRequisicoesInboundParaMateriaisComProducaoViavel;
    }
    
    public Set<Location> getLocationsConsideradas(ClusterEParametrosProjection clusterEParametrosProjection) {
        /*
         * Community nao possui inclusao/exclusao por location no perfil de
         * execucao. A projection/factory que consome este metodo ainda pode
         * aplicar filtros globais, como propagacao de demanda, mas nao existe
         * uma whitelist/blacklist por location nesta edicao.
         */
        return clusterEParametrosProjection.getLocationsAtivas();
    }

    public boolean getGeraPlanoIrrestrito() {
        return (geraPlanoIrrestrito == null) ? true : geraPlanoIrrestrito;
    }

    public boolean getIgnoraRestricaoProducaoIrrestrito() {
        return (ignoraRestricaoProducaoIrrestrito == null) ? true : ignoraRestricaoProducaoIrrestrito;
    }

    public boolean contemLocation(ClusterEParametrosProjection clusterEParametrosProjection, Location location) {
        return getLocationsConsideradas(clusterEParametrosProjection).contains(location);
    }
    
    public Optional<Integer> getTempoMaximoDiasParaAbastecimento(Location location) {
        /*
         * Tempo maximo de abastecimento por location e configuracao Enterprise.
         */
        return Optional.empty();
    }
    
    public ModeloEstoqueTarget getModeloEstoqueTarget() {
        return (modeloEstoqueTarget == null) ? ModeloEstoqueTarget.MIN_MAX : modeloEstoqueTarget;
    }

    public PerfilExecucaoSupplyPlan getDeepCopy() throws CloneNotSupportedException {
        
        return (PerfilExecucaoSupplyPlan) this.clone();
        
    }
    
    public TipoCapacidadeProdutiva getTipoCapacidadeProdutivaCadastrado() {
        /*
         * Community nao persiste selecao de tipo de capacidade produtiva. O
         * metodo permanece para compatibilidade com telas/mappers compartilhados
         * que distinguem valor cadastrado de default no Enterprise.
         */
        return TipoCapacidadeProdutiva.HORAS_POR_DIA;
    }
    public TipoCapacidadeProdutiva getTipoCapacidadeProdutiva() {
        /*
         * Apenas capacidade total em horas/dia e selecionavel no Community.
         * Quantidade por UOM e alocacao por turno dependem de infraestrutura
         * Enterprise de scheduling/capacidade detalhada.
         */
        return TipoCapacidadeProdutiva.HORAS_POR_DIA;
    }
    public TipoCapacidadeProdutiva getTipoCapacidadeProdutiva(Location location) {
        return getTipoCapacidadeProdutiva();
    }

    public TipoCapacidadeLogistica getTipoCapacidadeLogisticaCadastrado() {
        /*
         * Retorna apenas o valor explicitamente persistido. O getter funcional
         * abaixo aplica o default legado para o optimizer Enterprise, mas o
         * Community bloqueia qualquer selecao deste campo nas bordas de
         * configuracao.
         */
        return tipoCapacidadeLogistica;
    }
    public TipoCapacidadeLogistica getTipoCapacidadeLogistica() {
        return tipoCapacidadeLogistica == null
                ? TipoCapacidadeLogistica.NIVEL_LOCATION
                : tipoCapacidadeLogistica;
    }
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cloneNotSupportedException) {
            /*
             * A classe implementa Cloneable; cair aqui indica regressao no
             * contrato Java da entidade, nao uma regra funcional de Supply.
             */
            throw new IllegalStateException(
                    "PerfilExecucaoSupplyPlan deveria suportar clone.",
                    cloneNotSupportedException);
        }
    }

    public boolean getAplicaCurvasCustoFrete() {
        /*
         * Curvas de custo logistico pertencem ao Enterprise. O Community
         * bloqueia a selecao nas bordas de configuracao; este getter existe
         * para que o overlay privado consiga usar o mesmo perfil fisico.
         */
        return Boolean.TRUE.equals(aplicaCurvasCustoFrete);
    }
    public boolean getAplicaCurvasCustoLocation() {
        /*
         * Curvas de custo logistico por location pertencem ao Enterprise e
         * seguem o mesmo contrato de bloqueio funcional no Community.
         */
        return Boolean.TRUE.equals(aplicaCurvasCustoLocation);
    }

    public Long getCurvasCustoLogisticoId() {

        return curvasCustoLogisticoId;

    }

    public boolean getIgnoraRestricaoArmazenagemIrrestrito() {

        return ignoraRestricaoArmazenagemIrrestrito == null ? false : ignoraRestricaoArmazenagemIrrestrito;

    }

    public boolean getIgnoraRestricaoOutboundIrrestrito() {

        return ignoraRestricaoOutboundIrrestrito == null ? false : ignoraRestricaoOutboundIrrestrito;

    }

    public boolean getIgnoraRestricaoInboundIrrestrito() {

        return ignoraRestricaoInboundIrrestrito == null ? false : ignoraRestricaoInboundIrrestrito;

    }

    public boolean getIgnoraRestricaoLeadTimeIrrestrito() {

        return ignoraRestricaoLeadTimeIrrestrito == null ? false : ignoraRestricaoLeadTimeIrrestrito;

    }

    public boolean getIgnoraRestricaoMargemIrrestrito() {

        return ignoraRestricaoMargemIrrestrito == null ? false : ignoraRestricaoMargemIrrestrito;

    }

    public double getImpactoMaximoReducaoLeadTimeCustoTransferencia() {

        return impactoMaximoReducaoLeadTimeCustoTransferencia == null
                ? 0.0
                : impactoMaximoReducaoLeadTimeCustoTransferencia;

    }

    public double getImpactoMaximoReducaoLeadTimeCustoTransferenciaCustoMaterial() {

        return impactoMaximoReducaoLeadTimeCustoTransferenciaCustoMaterial == null
                ? 0.0
                : impactoMaximoReducaoLeadTimeCustoTransferenciaCustoMaterial;

    }

    public double getIncrementoPercentualAtendimentoDemandaFuncaoObjetivo() {

        return incrementoPercentualAtendimentoDemandaFuncaoObjetivo == null
                ? 0.0
                : incrementoPercentualAtendimentoDemandaFuncaoObjetivo;

    }

    public double getValorMinimoAtendimentoDemandaFuncaoObjetivo() {

        return valorMinimoAtendimentoDemandaFuncaoObjetivo == null
                ? 0.0
                : valorMinimoAtendimentoDemandaFuncaoObjetivo;

    }

    public boolean getAlocaTransferenciasNasFrotas() {

        return alocaTransferenciasNasFrotas == null ? false : alocaTransferenciasNasFrotas;

    }

    public boolean getPenalizaDemandaNaoAtendida() {

        return penalizaDemandaNaoAtendida == null ? false : penalizaDemandaNaoAtendida;

    }

    public double getPercentualPenalizacaoDemandaNaoAtendidaFatFunObj() {

        return percentualPenalizacaoDemandaNaoAtendidaFatFunObj == null
                ? 0.0
                : percentualPenalizacaoDemandaNaoAtendidaFatFunObj;

    }

    public double getPenalizacaoUnitariaDemandaNaoAtendida() {

        return penalizacaoUnitariaDemandaNaoAtendida == null
                ? 0.0
                : penalizacaoUnitariaDemandaNaoAtendida;

    }

    public UnidadeMedida getUnidadeMedidaPenalizacaoUnitariaDemandaNaoAtendidaCadastrada() {

        /*
         * Retorna apenas a UOM explicitamente persistida para a penalizacao
         * unitaria de demanda nao atendida. O getter funcional abaixo continua
         * aplicando a UOM padrao do SNP quando o perfil Enterprise nao informou
         * uma unidade especifica.
         */
        return unidadeMedidaPenalizacaoUnitariaDemandaNaoAtendida;

    }

    public UnidadeMedida getUnidadeMedidaPenalizacaoUnitariaDemandaNaoAtendida(
            ParametrosGlobais parametrosGlobais) {

        return unidadeMedidaPenalizacaoUnitariaDemandaNaoAtendida == null
                ? parametrosGlobais.getUnidadeMedidaPadraoSNP()
                : unidadeMedidaPenalizacaoUnitariaDemandaNaoAtendida;

    }

    public Constantes.TipoQuantidadeValor getTipoValorFaturamentoFuncaoObjetivo() {

        return tipoValorFaturamentoFuncaoObjetivo == null
                ? Constantes.TipoQuantidadeValor.QUANTIDADE
                : tipoValorFaturamentoFuncaoObjetivo;

    }

    public UnidadeMedida getUnidadeMedidaOtimizacaoCadastrada() {

        /*
         * Retorna apenas o valor explicitamente persistido no perfil. O getter
         * funcional com ParametrosGlobais continua aplicando default SNP quando
         * a UOM nao foi configurada. O overlay Enterprise usa este metodo ao
         * converter entidade para DTO, sem precisar chamar regra funcional nem
         * depender de parametros globais.
         */
        return unidadeMedidaOtimizacao;

    }

    public UnidadeMedida getUnidadeMedidaOtimizacao(ParametrosGlobais parametrosGlobais) {

        return unidadeMedidaOtimizacao == null
                ? parametrosGlobais.getUnidadeMedidaPadraoSNP()
                : unidadeMedidaOtimizacao;

    }

    public double getValorUnitarioVenda() {

        return valorUnitarioVenda == null
                ? 0.0
                : valorUnitarioVenda;

    }

    public boolean getConsideraCustoFixoLocation() {

        return consideraCustoFixoLocation == null ? false : consideraCustoFixoLocation;

    }

    public boolean getConsideraCustoFixoRecursoProdutivo() {

        return consideraCustoFixoRecursoProdutivo == null ? false : consideraCustoFixoRecursoProdutivo;

    }

    public boolean getConsideraCustoArmazenagem() {

        return consideraCustoArmazenagem == null ? false : consideraCustoArmazenagem;

    }

    public boolean getConsideraCustoTransferencia() {

        return consideraCustoTransferencia == null ? false : consideraCustoTransferencia;

    }

    public boolean getConsideraValorImpostosLinhaTransporte() {

        return consideraValorImpostosLinhaTransporte == null ? false : consideraValorImpostosLinhaTransporte;

    }

    public boolean getConsideraCustosInboundOutbound() {

        return consideraCustosInboundOutbound == null ? false : consideraCustosInboundOutbound;

    }

    public boolean getConsideraCustoProducao() {

        return consideraCustoProducao == null ? false : consideraCustoProducao;

    }

    public boolean getConsideraPrecoFornecedor() {

        return consideraPrecoFornecedor == null ? false : consideraPrecoFornecedor;

    }

    public ModoApuracaoImpostos getModoApuracaoImpostos() {

        return modoApuracaoImpostos == null
                ? ModoApuracaoImpostos.CALCULO_SIMPLIFICADO
                : modoApuracaoImpostos;

    }

    /**
     * Indica se o perfil solicita a geracao de Profit/Loss.
     *
     * <p>Perfis existentes e payloads que omitem o campo permanecem seguros:
     * nao solicitam geracao financeira.</p>
     */
    public boolean getGenerateProfitLoss() {

        return Boolean.TRUE.equals(generateProfitLoss);

    }

    /**
     * Indica se a futura retroacao Finance pode atravessar a lista tecnica.
     *
     * <p>A ausencia do valor preserva o default historico permissivo sem
     * alterar qualquer execucao Community.</p>
     */
    public boolean getAllowSalesProfitLossBomRetroaction() {

        return !Boolean.FALSE.equals(allowSalesProfitLossBomRetroaction);

    }

    public boolean getAplicaFairShareSafetyStock() {

        return aplicaFairShareSafetyStock == null ? false : aplicaFairShareSafetyStock;

    }

    public double getPenalidadePercentualFairShareDemandaNaoAtendida() {

        return penalidadePercentualFairShareDemandaNaoAtendida == null
                ? 0.0
                : penalidadePercentualFairShareDemandaNaoAtendida;

    }

    public double getCoeficienteImpactoDemandaAtendidaPlanoDemanda() {

        return coeficienteImpactoDemandaAtendidaPlanoDemanda == null
                ? 1.0
                : coeficienteImpactoDemandaAtendidaPlanoDemanda;

    }

    public double getCoeficienteImpactoDemandaAtendidaCarteira() {

        return coeficienteImpactoDemandaAtendidaCarteira == null
                ? 1.0
                : coeficienteImpactoDemandaAtendidaCarteira;

    }

    public boolean getSegmentaEstoqueEmLotes() {

        return segmentaEstoqueEmLotes == null ? false : segmentaEstoqueEmLotes;

    }

    public boolean getIncrementaImpactoWorkingCapitalLotesMaisAntigos() {

        return incrementaImpactoWorkingCapitalLotesMaisAntigos == null
                ? false
                : incrementaImpactoWorkingCapitalLotesMaisAntigos;

    }

    public double getIncrementoPercentualMaximoImpactoWorkingCapitalLoteMaisAntigo() {

        return incrementoPercentualMaximoImpactoWorkingCapitalLoteMaisAntigo == null
                ? 0.0
                : incrementoPercentualMaximoImpactoWorkingCapitalLoteMaisAntigo;

    }

    public double getCustoPercentualWorkingCapital() {

        return custoPercentualWorkingCapital == null
                ? 0.0
                : custoPercentualWorkingCapital;

    }

    public Long getNumeroMaximoMinutosOtimizacao() {

        return numeroMaximoMinutosOtimizacao;

    }

    public double getEntityTabuRatio() {

        return entityTabuRatio == null ? 0.1 : entityTabuRatio;

    }

    public int getAcceptedCountLimit() {

        return acceptedCountLimit == null ? 10_000 : acceptedCountLimit;

    }

    public boolean getConsiderBudgetForGreenfieldLocationActivation() {

        return considerBudgetForGreenfieldLocationActivation != null
                && considerBudgetForGreenfieldLocationActivation;

    }

    public double getGreenfieldLocationActivationBudget() {

        return greenfieldLocationActivationBudget == null
                ? 0.0
                : Math.max(0.0, greenfieldLocationActivationBudget);

    }

    public boolean getRoundProductionAndSetupsToDetailedPlanBucket() {

        return roundProductionAndSetupsToDetailedPlanBucket == null
                || roundProductionAndSetupsToDetailedPlanBucket;

    }

    public double getCoeficienteImpactoGapSafetyStock() {

        return coeficienteImpactoGapSafetyStock == null
                ? 0.0
                : coeficienteImpactoGapSafetyStock;

    }

    public boolean getPermiteEstoqueEmClientes() {

        return permiteEstoqueEmClientes == null ? false : permiteEstoqueEmClientes;

    }

    public boolean getPermiteEstoqueEmPontosTransbordo() {

        return permiteEstoqueEmPontosTransbordo == null ? false : permiteEstoqueEmPontosTransbordo;

    }

    public double getPenalidadePercentualDiferencaParaTargetVariavel() {

        return penalidadePercentualDiferencaParaTargetVariavel == null
                ? 0.0
                : penalidadePercentualDiferencaParaTargetVariavel;

    }

    /**
     * Retorna a penalidade de soft target exatamente como foi cadastrada.
     *
     * <p>O getter operacional converte ausencia em {@code 0.0} para proteger
     * os consumidores de runtime. O overlay Enterprise de configuracao, por
     * outro lado, precisa distinguir ausencia de uma escolha explicita de
     * zero ao montar o DTO compartilhado.</p>
     */
    public Double getPenalidadePercentualDiferencaParaTargetVariavelCadastrada() {

        return penalidadePercentualDiferencaParaTargetVariavel;

    }

    public double getAmplitudeDesvioComoPercentualValorTarget() {

        return amplitudeDesvioComoPercentualValorTarget == null
                ? 0.0
                : amplitudeDesvioComoPercentualValorTarget;

    }

    /**
     * Retorna a amplitude de desvio de soft target exatamente como cadastrada.
     *
     * <p>O valor nulo preserva a ausencia da configuracao para a borda
     * Enterprise; o getter operacional continua materializando seu default
     * numerico para os consumidores do optimizer.</p>
     */
    public Double getAmplitudeDesvioComoPercentualValorTargetCadastrada() {

        return amplitudeDesvioComoPercentualValorTarget;

    }

    public int getNumeroSegmentosLinearizacaoDiferencaVariavelTarget() {

        return numeroSegmentosLinearizacaoDiferencaVariavelTarget == null
                ? 0
                : numeroSegmentosLinearizacaoDiferencaVariavelTarget;

    }

    /**
     * Retorna o numero de segmentos de soft target exatamente como cadastrado.
     *
     * <p>O runtime recebe {@code 0} quando o campo nao foi configurado, mas o
     * DTO Enterprise precisa preservar {@code null} para nao transformar uma
     * ausencia em parametro explicito no proximo save.</p>
     */
    public Integer getNumeroSegmentosLinearizacaoDiferencaVariavelTargetCadastrado() {

        return numeroSegmentosLinearizacaoDiferencaVariavelTarget;

    }

    public double getIncentivoPercentualAtendimentoOrdemFirmeSobreCogs() {

        return incentivoPercentualAtendimentoOrdemFirmeSobreCogs == null
                ? 0.0
                : incentivoPercentualAtendimentoOrdemFirmeSobreCogs;

    }

    /**
     * Retorna o incentivo de atendimento de ordem firme exatamente como foi
     * cadastrado.
     *
     * <p>O runtime do optimizer deve continuar usando
     * {@link #getIncentivoPercentualAtendimentoOrdemFirmeSobreCogs()}, que
     * materializa {@code 0.0} para configuracoes antigas sem valor. A borda
     * Enterprise, por outro lado, precisa distinguir ausencia de configuracao
     * de um zero explicitamente salvo para preservar o round-trip do DTO.</p>
     */
    public Double getIncentivoPercentualAtendimentoOrdemFirmeSobreCogsCadastrado() {

        return incentivoPercentualAtendimentoOrdemFirmeSobreCogs;

    }

    public boolean getIncrementaImpactoFuncaoObjetivoPrimeirosPeriodos() {

        /*
         * Community nunca executa o modelo otimizado, mas o Enterprise consome
         * esta mesma entidade quando o overlay privado esta no classpath. Por
         * isso mantemos aqui a regra funcional do optimizer, com defaults
         * conservados do legado. No Community, os services de configuracao
         * bloqueiam o preenchimento destes campos antes da persistencia.
         */
        return incrementaImpactoFuncaoObjetivoPrimeirosPeriodos == null
                ? true
                : incrementaImpactoFuncaoObjetivoPrimeirosPeriodos;

    }

    public double getIncrementoPercentualImpactoFuncaoObjetivoPrimeiroPeriodo() {

        return incrementoPercentualImpactoFuncaoObjetivoPrimeiroPeriodo == null
                ? 0.01
                : incrementoPercentualImpactoFuncaoObjetivoPrimeiroPeriodo;

    }

    public ModeloDecaimentoImpactoTemporal getModeloDecaimentoImpactoTemporalFuncaoObjetivo() {

        return modeloDecaimentoImpactoTemporalFuncaoObjetivo == null
                ? ModeloDecaimentoImpactoTemporal.LINEAR
                : modeloDecaimentoImpactoTemporalFuncaoObjetivo;

    }

    public double getFatorDecaimentoExponencialImpactoTemporalFuncaoObjetivo() {

        double fatorDecaimentoExponencial = fatorDecaimentoExponencialImpactoTemporalFuncaoObjetivo == null
                ? 0.35
                : fatorDecaimentoExponencialImpactoTemporalFuncaoObjetivo;

        if (!Double.isFinite(fatorDecaimentoExponencial)
                || fatorDecaimentoExponencial < 0.0d
                || fatorDecaimentoExponencial > 1.0d) {
            throw new IllegalStateException(
                    "Temporal objective exponential decay factor must be finite and between 0 and 1: "
                            + fatorDecaimentoExponencial
                            + ".");
        }
        return fatorDecaimentoExponencial;

    }

    public double getPisoMultiplicadorImpactoTemporalFuncaoObjetivo() {

        return pisoMultiplicadorImpactoTemporalFuncaoObjetivo == null
                ? 0.20
                : pisoMultiplicadorImpactoTemporalFuncaoObjetivo;

    }

    /**
     * Valida escalares nao negativos da funcao objetivo Enterprise.
     *
     * <p>Community nao executa o optimizer, mas esta entidade e compartilhada
     * com o overlay Enterprise. Valor nulo segue default legado no getter
     * chamador; valor presente invalido nao pode ser mascarado para zero porque
     * muda pesos temporais, penalizacoes economicas ou multiplicadores da
     * funcao objetivo.</p>
     */
    /**
     * Valida escalares estritamente positivos da funcao objetivo Enterprise.
     *
     * <p>O default nulo continua sendo decidido no getter chamador para manter
     * compatibilidade com perfis historicos. Quando o valor esta preenchido,
     * entretanto, zero, negativo ou nao finito indicam configuracao incapaz de
     * gerar bounds/coeficientes validos para o optimizer.</p>
     */
    public double aplicaMultiplicadorTemporalFuncaoObjetivo(
            double coeficienteBase,
            Integer periodo,
            Calendario calendario) {

        return coeficienteBase * getMultiplicadorTemporalFuncaoObjetivoPrimeirosPeriodos(periodo, calendario);

    }

    public double getMultiplicadorTemporalFuncaoObjetivoPrimeirosPeriodos(
            Integer periodo,
            Calendario calendario) {

        /*
         * A curva temporal pertence ao modelo otimizado Enterprise, mas fica
         * nesta entidade compartilhada porque loaders de funcao objetivo
         * consultam diretamente o perfil. Se a configuracao estiver desligada
         * ou incompleta, o multiplicador neutro preserva o coeficiente base e
         * continua sem efeito sobre o heuristico Community.
         */
        if (calendario == null
                || periodo == null
                || !getIncrementaImpactoFuncaoObjetivoPrimeirosPeriodos()
                || calendario.getNumeroPeriodosFuturos() <= 1) {
            return 1.0;
        }

        int periodoNormalizado = Math.max(0, Math.min(
                periodo,
                calendario.getNumeroPeriodosFuturos() - 1));
        if (ModeloDecaimentoImpactoTemporal.EXPONENCIAL.equals(
                getModeloDecaimentoImpactoTemporalFuncaoObjetivo())) {
            return getMultiplicadorTemporalFuncaoObjetivoExponencial(periodoNormalizado);
        }

        return getMultiplicadorTemporalFuncaoObjetivoLinear(periodoNormalizado, calendario);

    }

    private double getMultiplicadorTemporalFuncaoObjetivoLinear(
            int periodo,
            Calendario calendario) {

        int complementoPeriodo = calendario.getNumeroPeriodosFuturos() - periodo - 1;
        double incrementoPercentual = getIncrementoPercentualImpactoFuncaoObjetivoPrimeiroPeriodo()
                / (calendario.getNumeroPeriodosFuturos() - 1)
                * complementoPeriodo;

        return 1.0 + incrementoPercentual;

    }

    private double getMultiplicadorTemporalFuncaoObjetivoExponencial(int periodo) {

        double multiplicadorPrimeiroPeriodo =
                1.0 + getIncrementoPercentualImpactoFuncaoObjetivoPrimeiroPeriodo();
        double multiplicadorExponencial = multiplicadorPrimeiroPeriodo
                * Math.pow(getFatorDecaimentoExponencialImpactoTemporalFuncaoObjetivo(), periodo);

        return Math.max(
                getPisoMultiplicadorImpactoTemporalFuncaoObjetivo(),
                multiplicadorExponencial);

    }

    public String getTipoModeloOtimizacao() {

        return tipoModeloOtimizacao;

    }

    public void setTipoModeloOtimizacao(String tipoModeloOtimizacao) {

        this.tipoModeloOtimizacao = tipoModeloOtimizacao;

    }

    public String getModeloPriorizacaoDemandaId() {

        return modeloPriorizacaoDemandaId;

    }

    public String getModeloPriorizacaoSafetyStockId() {

        return modeloPriorizacaoSafetyStockId;

    }

    public boolean getEstimaCogsUnitarioParaWorkingCapitalEPoliticaEstoques() {

        /*
         * COGS e working capital pertencem ao recorte Enterprise. O default
         * Community fica falso para evitar qualquer cálculo econômico na
         * edição aberta.
         */
        return estimaCogsUnitarioParaWorkingCapitalEPoliticaEstoques == null
                ? false
                : estimaCogsUnitarioParaWorkingCapitalEPoliticaEstoques;

    }

    public String getMaterialFilterId() {

        return materialFilterId;

    }

    public void setMaterialFilterId(String materialFilterId) {

        this.materialFilterId = materialFilterId;

    }

    public Set<Location> getLocationsGreenfield(ClusterEParametrosProjection clusterEParametrosProjection) {

        /*
         * Greenfield e capability Enterprise. O Community nao abre/fecha
         * locations e por isso retorna conjunto vazio quando algum fluxo
         * compartilhado consulta este contrato.
         */
        return Collections.emptySet();

    }

}
