package com.opsfactor.community.capability.supplyplanning.service;

import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.supplyplanning.distributionplan.repository.DistributionPlanItemRepository;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.domain.InventoryPlanLinha;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.repository.InventoryPlanLinhaRepository;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import com.opsfactor.community.capability.supplyplanning.productionplan.repository.ProductionPlanLinhaDAO;
import com.opsfactor.community.capability.supplyplanning.productionplan.repository.ProductionPlanLinhaRepository;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.DemandaDiretaConsideradaLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.DemandaDiretaConsideradaProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningMultiplasLocationsProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.DemandaDiretaConsideradaLinhaDAO;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.DemandaDiretaConsideradaLinhaRepository;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.SupplyPlanRepository;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan.ModoExecucao;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.optimizer.presetconstraint.RestricaoPredefinidaGrupo;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjectionFactory;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByMaterialUOMImpl;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.projection.EstoqueProjectionLocationProduto;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.projection.EstoqueProjectionProduto;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.projection.EstoqueProjectionFactory;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjection;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjectionFactory;
import com.opsfactor.community.capability.masterdata.production.productionresource.projection.BIProjectionCapacidadeProdutiva;
import com.opsfactor.community.capability.masterdata.production.productionresource.projection.BIProjectionCapacidadeProdutivaFactory;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanningProjection;
import com.opsfactor.community.capability.masterdata.calendar.temporalsplit.projection.SplitTemporalProjectionFactory;
import com.opsfactor.community.capability.masterdata.calendar.temporalsplit.projection.SplitTemporalProjectionPorDfu;
import com.opsfactor.community.capability.supplyplanning.configuration.repository.PerfilExecucaoSupplyPlanRepository;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.repository.EstoqueRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.VersaoMalhaRepository;
import com.opsfactor.community.capability.demandplanning.demandplan.repository.DemandPlanRepository;
import com.opsfactor.community.capability.masterdata.production.productionversion.service.VersaoProducaoService;
import com.opsfactor.community.capability.supplyplanning.engine.SupplyPlanning;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.exception.SupplyPlanException;
import com.opsfactor.community.capability.demandplanning.service.DemandPlanningService;
import com.opsfactor.community.capability.supplyplanning.service.heuristic.HeuristicoService;
import com.opsfactor.community.capability.supplyplanning.productionplan.service.CapacidadeEfetivaSupplyPlanService;
import com.opsfactor.community.capability.supplyplanning.service.spi.SupplyPlanExecutionServiceSpi;
import com.opsfactor.community.capability.supplyplanning.service.spi.SupplyPlanExecutionProfileMaterialScopeSpi;
import com.opsfactor.community.capability.supplyplanning.service.spi.SupplyPlanExecutionProfileLocationScopeSpi;
import com.opsfactor.community.capability.supplyplanning.service.spi.SupplyPlanDemandCatchUpProjectionSpi;
import com.opsfactor.community.capability.supplyplanning.service.spi.SupplyPlanFirmProductionOrdersSpi;
import com.opsfactor.community.capability.supplyplanning.service.spi.SupplyPlanOpenOrdersHeuristicSpi;
import com.opsfactor.community.capability.supplyplanning.service.spi.SupplyPlanOptimizationServiceSpi;
import com.opsfactor.community.capability.supplyplanning.service.spi.SupplyPlanProcessChainServiceSpi;
import com.opsfactor.community.capability.supplyplanning.service.spi.SupplyPlanPresetConstraintGroupSpi;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.Constantes.ReferenciaPeriodo;
import com.opsfactor.community.platform.utility.Constantes.TamanhoBucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Nullable;
import jakarta.persistence.NoResultException;
import java.sql.ResultSetMetaData;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service principal de Supply Planning Community.
 *
 * <p>O Community executa somente o motor heuristico e operacoes associadas:
 * plano irrestrito/restrito simples, capacidade produtiva operacional, fair
 * share direto de demanda e Planning Book material/location. Otimizador,
 * process chain, custos, P&L, constraint tracker e line scheduling entram por
 * SPIs/overlays Enterprise reais.</p>
 */
@Slf4j
@Service
public class SupplyPlanService {

    /**
     * Service de Demand Planning usado para carregar o plano de demanda base e
     * montar a demanda direta considerada no Supply Planning Community.
     */
    @Autowired
    private DemandPlanningService demandPlanningService;

    /**
     * Service de versao de producao usado na malha/roteiro operacional do
     * heuristico.
     */
    @Autowired
    private VersaoProducaoService versaoProducaoService;

    /**
     * Implementacao Community do motor heuristico de Supply Planning.
     */
    @Autowired
    private HeuristicoService heuristicoService;

    /**
     * Service de persistencia/calculo de capacidade efetiva. Capacidade
     * produtiva e fluxo padrao do Community, nao pos-processamento Enterprise.
     */
    @Autowired
    private CapacidadeEfetivaSupplyPlanService capacidadeEfetivaSupplyPlanService;

    /**
     * Bean opcional Enterprise. Deve permanecer null no Community; se um perfil
     * tentar executar otimizador sem overlay Enterprise, o resolver lancara
     * RequiresEnterpriseVersionException.
     */
    @Autowired(required = false)
    private SupplyPlanOptimizationServiceSpi supplyPlanOptimizationService;

    /**
     * Bean opcional Enterprise. Process chain nao existe no Community, mas o
     * campo explicito documenta o ponto de extensao e evita registry/listas.
     */
    @Autowired(required = false)
    private SupplyPlanProcessChainServiceSpi supplyPlanProcessChainService;

    /**
     * Resolver Enterprise opcional do cabeçalho de preset constraints.
     *
     * <p>O Community não declara este bean. Assim, uma seleção explícita de
     * grupo em plano novo falha como capacidade Enterprise, enquanto
     * reexecuções preservam a referência já gravada sem nova resolução.</p>
     */
    @Autowired(required = false)
    private SupplyPlanPresetConstraintGroupSpi supplyPlanPresetConstraintGroupSpi;

    /**
     * Optional Enterprise resolver for location-level execution-profile
     * configuration.
     *
     * <p>Community intentionally has no persisted location-level profile
     * relation. When the private overlay is absent, the projection assembled by
     * the Community factory remains the complete functional scope.</p>
     */
    @Autowired(required = false)
    private SupplyPlanExecutionProfileLocationScopeSpi supplyPlanExecutionProfileLocationScope;

    /**
     * Optional Enterprise resolver for material-level execution-profile
     * configuration. Community keeps its complete material scope when this
     * private bean is absent.
     */
    @Autowired(required = false)
    private SupplyPlanExecutionProfileMaterialScopeSpi supplyPlanExecutionProfileMaterialScope;

    /**
     * Extensao Enterprise opcional que transforma ordens firmes de producao
     * em linhas de plano antes dos dois motores. Sem o overlay, o Community
     * mantem o snapshot inicial sem dados transacionais de producao.
     */
    @Autowired(required = false)
    private SupplyPlanFirmProductionOrdersSpi supplyPlanFirmProductionOrdersSpi;

    /**
     * Extensao Enterprise opcional que transforma ordens abertas de compra,
     * transferencia e venda em entradas reconheciveis pelo heuristico comum.
     * Sem esse bean, o Community continua sem leitura transacional privada.
     */
    @Autowired(required = false)
    private SupplyPlanOpenOrdersHeuristicSpi supplyPlanOpenOrdersHeuristicSpi;

    /**
     * Projection de Demand Planning usada pelos dois fluxos Supply que podem
     * exigir catch-up de sell-out passado. Community fornece a implementacao
     * normal e bloqueia a flag; Enterprise a substitui por um overlay que
     * reutiliza a projection oficial de vendas.
     */
    @Autowired
    private SupplyPlanDemandCatchUpProjectionSpi supplyPlanDemandCatchUpProjectionSpi;

    /**
     * Repository de Demand Plan usado para resolver a origem da demanda.
     */
    @Autowired
    private DemandPlanRepository demandPlanRepository;

    /**
     * Repository de Supply Plan usado para criar, atualizar, consultar e remover
     * versoes de plano.
     */
    @Autowired
    private SupplyPlanRepository supplyPlanRepository;

    /**
     * Repository da versao de malha operacional usada pelo heuristico.
     */
    @Autowired
    private VersaoMalhaRepository versaoMalhaRepository;

    /**
     * Repository das linhas de distribuicao/movimentacao do plano heuristico.
     */
    @Autowired
    private DistributionPlanItemRepository distributionPlanItemRepository;

    /**
     * Repository das linhas de producao planejada do plano heuristico.
     */
    @Autowired
    private ProductionPlanLinhaRepository productionPlanLinhaRepository;

    /**
     * DAO JDBC para upsert/delete em lote do snapshot de producao. Evita o
     * merge JPA de uma chave composta com grafo amplo e seus selects por linha.
     */
    @Autowired
    private ProductionPlanLinhaDAO productionPlanLinhaDAO;

    /**
     * Repository das linhas de inventario operacional quando o perfil salva
     * inventory plan.
     */
    @Autowired
    private InventoryPlanLinhaRepository inventoryPlanLinhaRepository;

    /**
     * Repository do perfil de execucao Supply. No Community, modos diferentes
     * de `HEURISTICO` exigem SPI Enterprise real.
     */
    @Autowired
    private PerfilExecucaoSupplyPlanRepository perfilExecucaoSupplyPlanRepository;

    /**
     * Repository de estoque inicial operacional.
     */
    @Autowired
    private EstoqueRepository estoqueRepository;

    /**
     * Repository usado somente para verificar se a reexecucao ja possui a
     * fotografia persistida de demanda direta antes de preserva-la.
     */
    @Autowired
    private DemandaDiretaConsideradaLinhaRepository demandaDiretaConsideradaLinhaRepository;

    /**
     * DAO JDBC usado apenas para persistencia em lote de demanda direta
     * considerada. A entidade possui chave composta e alto volume por plano,
     * portanto o DAO evita `merge` entidade a entidade no fluxo principal.
     */
    @Autowired
    private DemandaDiretaConsideradaLinhaDAO demandaDiretaConsideradaLinhaDAO;

    /**
     * Factory da projection de Supply Plan usada para leitura/escrita em memoria
     * das linhas geradas.
     */
    @Autowired
    private SupplyPlanProjectionFactory supplyPlanProjectionFactory;

    /**
     * Factory da malha operacional de supply usada pelo heuristico.
     */
    @Autowired
    private SupplyNetworkProjectionFactory supplyNetworkProjectionFactory;

    /**
     * Factory da projection global de parametros, clusters e master data.
     */
    @Autowired
    private ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory;

    /**
     * Factory de UOM/conversoes usada nas projections e persistencia de volumes.
     */
    @Autowired
    private UnidadeMedidaProjectionFactory conversaoUnidadeMedidaProjectionFactory;

    /**
     * Factory de estoque inicial operacional.
     */
    @Autowired
    private EstoqueProjectionFactory estoqueProjectionFactory;

    /**
     * Factory de split temporal. No Community nao há curva configuravel
     * Enterprise, mas a factory ainda suporta a curva flat/operacional exigida
     * pelo fluxo base.
     */
    @Autowired
    private SplitTemporalProjectionFactory splitTemporalProjectionFactory;

    /**
     * Factory de politica de estoques operacional para safety stock. Nao cobre
     * otimizacao de politica de estoques Enterprise.
     */
    @Autowired
    private PoliticaEstoquesProjectionFactory politicaEstoquesProjectionFactory;

    /**
     * Factory de capacidade produtiva operacional.
     */
    @Autowired
    private BIProjectionCapacidadeProdutivaFactory biProjectionCapacidadeProdutivaFactory;

    /**
     * Usado em updates defensivos em massa, como resets de plano.
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Lista planos de Supply Planning disponíveis para a camada de configuração/front.
     *
     * <p>O Community possui somente o fluxo heurístico atual; qualquer motor
     * otimizado ou process chain passa pelas SPIs opcionais Enterprise. A lista
     * vinda do repository precisa ser um snapshot estruturalmente válido antes
     * da ordenação, porque a tela usa esses ids para reexecução e seleção de
     * planos de estoque inicial.</p>
     */
    public List<SupplyPlan> getSupplyPlanList() {

        /*
         * A listagem administrativa acessa demand plan, malha e perfil na
         * camada front. O selector carrega essas referencias em lote e evita
         * uma consulta lazy adicional para cada Supply Plan retornado.
         */
        List<SupplyPlan> supplyPlans = supplyPlanRepository.customFindAllForSelector();
        validaSupplyPlanListCarregadaCommunity(supplyPlans);

        return supplyPlans.stream()
                .sorted(Comparator.comparing(SupplyPlan::getId))
                .collect(Collectors.toList());

    }

    /**
     * Valida a fotografia de Supply Plans carregada para listagem.
     *
     * <p>Lista vazia é válida, mas lista nula, item nulo ou plano sem id indicam
     * quebra da fronteira de persistência. Falhar aqui evita `NullPointerException`
     * opaco dentro do comparator e impede que a camada front receba um plano que
     * não pode ser selecionado/reexecutado de forma rastreável.</p>
     */
    private void validaSupplyPlanListCarregadaCommunity(
            List<SupplyPlan> supplyPlans) {

        if (supplyPlans == null) {
            throw new IllegalStateException(
                    "Supply Plan repository returned null list for Supply Plan listing.");
        }

        for (int index = 0; index < supplyPlans.size(); index++) {
            SupplyPlan supplyPlan = supplyPlans.get(index);
            if (supplyPlan == null) {
                throw new IllegalStateException(
                        "Supply Plan repository returned null item at index "
                                + index
                                + " for Supply Plan listing.");
            }
            if (supplyPlan.getId() == null) {
                throw new IllegalStateException(
                        "Supply Plan repository returned item without id at index "
                                + index
                                + " for Supply Plan listing.");
            }
        }

    }

    public void executeSupplyPlan(
            Long demandPlanId,
            Long supplyPlanId,
            Long supplyPlanIdParaProjecaoEstoqueInicial,
            String perfilExecucaoSupplyPlanId,
            String versaoMalhaId,
            String restricaoPredefinidaGrupoId,
            Constantes.TamanhoBucket tamanhoBucket,
            LocalDateTime dataInicioPlano,
            String descricao,
            String userId) {

        /*
         * A entrada REST legada usa id nulo para novo plano. Internamente
         * reaproveitamos o mesmo fluxo de busca e por isso substituimos por um
         * sentinela fora do banco, sem tratar nulo como plano existente.
         */
        if (supplyPlanId == null) {
            supplyPlanId = -1L;
        }
        SupplyPlan supplyPlan;
        /*
         * `Optional.empty()` e o caminho normal para criar um Supply Plan novo.
         * Retorno `null` no lugar do Optional indica quebra do repository e
         * deve falhar antes de escolher entre fluxo novo/reexecucao.
         */
        Optional<SupplyPlan> supplyPlanExistenteOptional =
                supplyPlanRepository.customFindById(supplyPlanId);
        if (supplyPlanExistenteOptional == null) {
            throw new IllegalStateException(
                    "Supply Plan repository returned null Optional while checking existing Supply Plan id "
                            + supplyPlanId
                            + ".");
        }
        SupplyPlan supplyPlanExistente = supplyPlanExistenteOptional.orElse(null);

        if (supplyPlanExistente != null) {
            dataInicioPlano = supplyPlanExistente.getDataInicioPlano();
        }

        SupplyPlan supplyPlanParaProjecaoEstoqueInicial;
        boolean novoSupplyPlan = true;
        if (supplyPlanExistente != null) {

            supplyPlan = supplyPlanExistente;
            validaModoExecucaoCommunity(supplyPlan.getPerfilExecucaoSupplyPlan());
            /*
             * A validacao do motor vem antes da resolucao do snapshot auxiliar:
             * em runtime Community, planos salvos como Optimizer/Process Chain
             * devem falhar por edicao antes de qualquer consulta adicional.
             * Para planos permitidos, ainda validamos o snapshot antes do reset
             * para nao limpar artefatos de um plano existente se a referencia
             * de estoque inicial estiver inconsistente.
             */
            supplyPlanParaProjecaoEstoqueInicial = (supplyPlanIdParaProjecaoEstoqueInicial != null) ?
                    getSupplyPlanParaProjecaoEstoqueInicialObrigatorio(supplyPlanIdParaProjecaoEstoqueInicial)
                    : null;

            switch (supplyPlan.getPerfilExecucaoSupplyPlan().getModoExecucao()) {
                case HEURISTICO:
                    log.info("Resetting Supply Plan {} while preserving supply-plan adjustment lines", supplyPlanId);
                    reiniciaArtefatosSupplyPlanExistente(supplyPlan);
                    break;
                case OTIMIZADOR:
                case PROCESS_CHAIN:
                    getSupplyPlanEnterpriseExecutionService(supplyPlan.getPerfilExecucaoSupplyPlan().getModoExecucao())
                            .reiniciaSupplyPlanExistente(supplyPlan);
                    break;
                default:
                    throw new RequiresEnterpriseVersionException("Supply Planning execution engine");
            }

            novoSupplyPlan = false;
        } else {
            supplyPlan = new SupplyPlan();
            supplyPlan.setSupplyPlanIdParaProjecaoEstoqueInicial(supplyPlanIdParaProjecaoEstoqueInicial);
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlanNovo =
                    getPerfilExecucaoSupplyPlanObrigatorio(perfilExecucaoSupplyPlanId);
            /*
             * O perfil novo e validado antes de demand plan, malha e projections
             * de parametros ou snapshots auxiliares. Assim uma chamada Community
             * para optimizer/process chain falha na borda de edicao, sem montar
             * dados pesados que seriam descartados logo depois.
             */
            validaModoExecucaoCommunity(perfilExecucaoSupplyPlanNovo);
            supplyPlanParaProjecaoEstoqueInicial = (supplyPlanIdParaProjecaoEstoqueInicial != null) ?
                    getSupplyPlanParaProjecaoEstoqueInicialObrigatorio(supplyPlanIdParaProjecaoEstoqueInicial)
                    : null;
            DemandPlan demandPlan = getDemandPlanObrigatorio(demandPlanId);
            supplyPlan.setVersaoMalha(getVersaoMalhaObrigatoria(versaoMalhaId));
            supplyPlan.setPerfilExecucaoSupplyPlan(perfilExecucaoSupplyPlanNovo);
            supplyPlan.setPresetConstraintGroup(
                    resolvePresetConstraintGroupParaNovoSupplyPlan(
                            restricaoPredefinidaGrupoId));
            supplyPlan.setDataInicioPlano(dataInicioPlano);
            supplyPlan.setTamanhoBucket(tamanhoBucket);

            /*
             * O calendario do Supply Plan normaliza a data inicial para o
             * primeiro instante do bucket e calcula o fim como o maior horizonte
             * entre as locations do perfil. Persistimos esse intervalo
             * normalizado no snapshot do plano novo.
             */
            ClusterEParametrosProjection clusterEParametrosProjection =
                    clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();
            ParametrosGlobais parametrosGlobais =
                    clusterEParametrosProjection.getParametrosGlobais();
            Calendario calendarioSupplyPlan = supplyPlan.getCalendarioDoSupplyPlan(parametrosGlobais);
            supplyPlan.setDataInicioPlano(calendarioSupplyPlan.getDataHorarioInicial());
            supplyPlan.setDataFimPlano(calendarioSupplyPlan.getDataHorarioFinal());
            supplyPlan.setTamanhoBucket(tamanhoBucket);
            supplyPlan.setDemandPlan(demandPlan);
        }

        /*
         * Metadados de geracao sao atualizados tanto em planos novos quanto em
         * reexecucoes; os artefatos calculados ja foram limpos acima quando
         * havia plano existente.
         */
        supplyPlan.setHorarioGeracao(LocalDateTime.now());
        supplyPlan.setUsuarioGeradorPlano((userId != null) ? userId : "System");
        if (novoSupplyPlan) {
            /*
             * A descricao default pertence ao snapshot salvo do novo plano. Ela
             * precisa ser resolvida por valor antes da execucao interna; comparar
             * String por identidade faria `new String("")` escapar da regra e
             * deixaria o plano persistido sem descricao operacional.
             */
            supplyPlan.setDescricao(getDescricaoSupplyPlanOuDefault(
                    descricao,
                    supplyPlan.getTamanhoBucket()));
        }


        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = supplyPlan.getPerfilExecucaoSupplyPlan();
        validaModoExecucaoCommunity(perfilExecucaoSupplyPlan);
        /*
         * A process chain precisa ser expandida antes da execucao interna para
         * que cada etapa monte projections, filtros e capacidade com o proprio
         * perfil de execucao. Se deixassemos o perfil base entrar no metodo
         * interno, o fluxo Community prepararia os dados usando um perfil que
         * existe apenas como orquestrador Enterprise.
         */
        if (PerfilExecucaoSupplyPlan.ModoExecucao.PROCESS_CHAIN.equals(perfilExecucaoSupplyPlan.getModoExecucao())) {
            if (novoSupplyPlan) {
                /*
                 * A process chain Enterprise reusa um unico Supply Plan fisico
                 * para todas as etapas. Por isso ela precisa receber um header
                 * ja persistido, com id rastreavel, mesmo antes de montar as
                 * projections pesadas do fluxo interno. Mantemos
                 * `novoSupplyPlan = true` na chamada para que a primeira etapa
                 * atomica ainda execute a preparacao inicial de estoques,
                 * demanda direta e demais snapshots do plano recem-criado.
                 */
                supplyPlan = validaSupplyPlanSalvoInicialCommunity(
                        supplyPlanRepository.saveAndFlush(supplyPlan));
            }
            getSupplyPlanEnterpriseExecutionService(perfilExecucaoSupplyPlan.getModoExecucao())
                    .executaSupplyPlan(
                            supplyPlan,
                            supplyPlanParaProjecaoEstoqueInicial,
                            perfilExecucaoSupplyPlan,
                            novoSupplyPlan,
                            false,
                            false);
        } else {
            executeSupplyPlan(
                    supplyPlan,
                    supplyPlanParaProjecaoEstoqueInicial,
                    perfilExecucaoSupplyPlan,
                    novoSupplyPlan,
                    false,
                    false);
        }



    }

    /**
     * Resolve o único vínculo Enterprise que o cabeçalho Community pode
     * armazenar ao criar um Supply Plan novo.
     *
     * <p>Não é chamado em reexecução: nesse caminho o plano carregado já é a
     * fonte de verdade do grupo, evitando que payloads atrasados removam ou
     * troquem a configuração originalmente usada pela rodada.</p>
     */
    protected RestricaoPredefinidaGrupo resolvePresetConstraintGroupParaNovoSupplyPlan(
            String presetConstraintGroupId) {

        if (presetConstraintGroupId == null) {
            return null;
        }

        if (supplyPlanPresetConstraintGroupSpi == null) {
            throw new RequiresEnterpriseVersionException(
                    "Supply Planning Preset Constraints");
        }

        RestricaoPredefinidaGrupo presetConstraintGroup =
                supplyPlanPresetConstraintGroupSpi.resolvePresetConstraintGroup(
                        presetConstraintGroupId);
        if (presetConstraintGroup == null) {
            throw new IllegalStateException(
                    "Enterprise preset constraint group resolver returned null for id "
                            + presetConstraintGroupId
                            + ".");
        }

        return presetConstraintGroup;

    }

    /**
     * Carrega o Demand Plan obrigatorio para criar um Supply Plan novo.
     *
     * <p>Ausencia de Demand Plan nao e uma condicao de no-op: sem esta origem
     * nao existe demanda direta considerada para alimentar o Supply Planning
     * Community. Por isso o fluxo falha com erro funcional claro, em vez de
     * simplesmente retornar sem criar nada.</p>
     */
    private DemandPlan getDemandPlanObrigatorio(Long demandPlanId) {


        Optional<DemandPlan> demandPlanOptional =
                demandPlanRepository.customFindByIdComPerfilExecucao(demandPlanId);
        if (demandPlanOptional == null) {
            throw new IllegalStateException(
                    "Demand Plan repository returned null Optional for Supply Planning execution id "
                            + demandPlanId
                            + ".");
        }

        return demandPlanOptional.orElseThrow(() -> new SupplyPlanException(
                "Demand Plan " + demandPlanId + " not found for Supply Planning execution."));

    }

    /**
     * Carrega a versao de malha obrigatoria para a execucao.
     *
     * <p>A malha define os caminhos de producao/distribuicao usados pelo
     * heuristico. Se o id informado nao existir, seguir silenciosamente deixaria
     * a task parecer concluida sem nenhum plano operacional gerado.</p>
     */
    private VersaoMalha getVersaoMalhaObrigatoria(String versaoMalhaId) {


        Optional<VersaoMalha> versaoMalhaOptional =
                versaoMalhaRepository.findById(versaoMalhaId);
        if (versaoMalhaOptional == null) {
            throw new IllegalStateException(
                    "Supply Network Version repository returned null Optional for Supply Planning execution id "
                            + versaoMalhaId
                            + ".");
        }

        return versaoMalhaOptional.orElseThrow(() -> new SupplyPlanException(
                "Supply Network Version " + versaoMalhaId + " not found for Supply Planning execution."));

    }

    /**
     * Carrega o perfil de execucao obrigatorio para a execucao.
     *
     * <p>O perfil determina motor, horizonte, outputs e restricoes Community.
     * A falta desse cadastro deve falhar antes de montar projections para que a
     * origem do problema fique visivel na task/API.</p>
     */
    private PerfilExecucaoSupplyPlan getPerfilExecucaoSupplyPlanObrigatorio(
            String perfilExecucaoSupplyPlanId) {


        Optional<PerfilExecucaoSupplyPlan> perfilExecucaoSupplyPlanOptional =
                perfilExecucaoSupplyPlanRepository.customFindById(perfilExecucaoSupplyPlanId);
        if (perfilExecucaoSupplyPlanOptional == null) {
            throw new IllegalStateException(
                    "Supply Planning Execution Profile repository returned null Optional for Supply Planning execution id "
                            + perfilExecucaoSupplyPlanId
                            + ".");
        }

        return perfilExecucaoSupplyPlanOptional.orElseThrow(() -> new SupplyPlanException(
                "Supply Planning Execution Profile " + perfilExecucaoSupplyPlanId
                        + " not found for Supply Planning execution."));

    }

    /**
     * Carrega o Supply Plan usado como origem da projecao de estoque inicial.
     *
     * <p>O parametro e opcional, mas quando informado precisa apontar para um
     * plano existente. Sem esse snapshot nao ha base confiavel para construir
     * o estoque inicial do novo plano, entao a execucao falha antes de montar
     * projections e antes de gravar linhas de Supply Planning.</p>
     */
    private SupplyPlan getSupplyPlanParaProjecaoEstoqueInicialObrigatorio(
            Long supplyPlanIdParaProjecaoEstoqueInicial) {


        Optional<SupplyPlan> supplyPlanParaProjecaoEstoqueInicialOptional =
                supplyPlanRepository.customFindById(supplyPlanIdParaProjecaoEstoqueInicial);
        if (supplyPlanParaProjecaoEstoqueInicialOptional == null) {
            throw new IllegalStateException(
                    "Supply Plan repository returned null Optional for initial stock projection id "
                            + supplyPlanIdParaProjecaoEstoqueInicial
                            + ".");
        }

        return supplyPlanParaProjecaoEstoqueInicialOptional.orElseThrow(() -> new SupplyPlanException(
                "Supply Plan " + supplyPlanIdParaProjecaoEstoqueInicial
                        + " not found for initial stock projection."));

    }

    /**
     * Resolve a descricao funcional usada quando a tela/API nao informa um
     * texto para o novo Supply Plan.
     *
     * <p>String vazia ou composta apenas por espacos e tratada como ausencia de
     * descricao. A comparacao deve ser por conteudo, nunca por identidade do
     * objeto {@link String}, porque payloads REST podem criar instancias novas
     * com o mesmo valor vazio.</p>
     */
    private String getDescricaoSupplyPlanOuDefault(
            String descricao,
            TamanhoBucket tamanhoBucket) {

        if (descricao != null && !descricao.isBlank()) {
            return descricao;
        }

        return "Supply Plan " + String.valueOf(Calendario.getDescricaoIntegerPeriodo(
                LocalDateTime.now(),
                tamanhoBucket));

    }

    /**
     * Executa o supply plan populando linhas de producao, distribuicao e
     * estoques sobre projections ja preparadas.
     *
     * <p>O fluxo Community nao expoe checked exceptions nesta borda. Falhas de
     * projection, motor heuristico, SPI Enterprise opcional ou persistencia
     * devem aparecer como excecoes funcionais/runtime, permitindo rollback do
     * plano novo sem manter contrato generico legado.</p>
     */
    public void executeSupplyPlan(
            SupplyPlan supplyPlan,
            @Nullable SupplyPlan supplyPlanParaProjecaoEstoqueInicial,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            boolean novoSupplyPlan,
            boolean consideraRequisicoesEtapaAnterior,
            boolean consideraOrdensProducaoPlanejadasEtapaAnterior) {

        /*
         * Este metodo interno tambem e chamado pela process chain Enterprise.
         * Validar o motor antes de montar projections pesadas garante que uma
         * chamada Community com perfil Enterprise falhe com mensagem funcional,
         * em vez de cair em NullPointerException de alguma factory ainda nao
         * inicializada.
         */
        validaModoExecucaoCommunity(perfilExecucaoSupplyPlan);

        if (PerfilExecucaoSupplyPlan.ModoExecucao.PROCESS_CHAIN.equals(perfilExecucaoSupplyPlan.getModoExecucao())) {
            /*
             * Process chain e um orquestrador Enterprise, nao um motor atomico
             * que consome a fotografia de projections deste metodo. Se uma
             * chamada direta chegar aqui com PROCESS_CHAIN, delegamos antes de
             * montar malha, filtros, estoque e demanda direta. Cada etapa da
             * cadeia chamara este mesmo metodo novamente com um perfil atomico
             * HEURISTICO ou OTIMIZADOR, preservando a preparacao correta por
             * etapa.
             */
            getSupplyPlanProcessChainServiceEnterprise()
                    .executaSupplyPlan(
                            supplyPlan,
                            supplyPlanParaProjecaoEstoqueInicial,
                            perfilExecucaoSupplyPlan,
                            novoSupplyPlan,
                            consideraRequisicoesEtapaAnterior,
                            consideraOrdensProducaoPlanejadasEtapaAnterior);
            return;
        }

        /*
         * Depois que a process chain foi desviada, o restante do metodo trata
         * apenas motores atomicos. Flags Enterprise que nao pertencem ao
         * heuristico Community devem falhar antes da montagem de UOM, cluster,
         * malha e demais projections compartilhadas.
         */
        validaOtimizadorInteligenciaArtificialCommunity(perfilExecucaoSupplyPlan);
        validaPedidosTransacionaisCommunity(perfilExecucaoSupplyPlan);

        UnidadeMedidaProjection unidadeMedidaProjection = conversaoUnidadeMedidaProjectionFactory.getUnidadeMedidaProjectionComConversoes();

        ClusterEParametrosProjection clusterEParametrosProjection =
                clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();

        ParametrosGlobais parametrosGlobais =
                clusterEParametrosProjection.getParametrosGlobais();
        VersaoMalha versaoMalha = supplyPlan.getVersaoMalha();
        /*
         * Parametros de optimizer, P&L, cost-to-serve, line scheduling, soft
         * targets e penalidades economicas sao bloqueados na borda de
         * configuracao (`PerfilExecucaoSupplyPlanFrontService`) antes de virar
         * entidade Community. Neste ponto de execucao, a protecao relevante e o
         * motor: apenas HEURISTICO roda sem SPI Enterprise.
         */

        PoliticaEstoquesProjection politicaEstoquesProjection =
                politicaEstoquesProjectionFactory.getPoliticaEstoquesProjection(
                                supplyPlan.getCalendarioDoSupplyPlan(parametrosGlobais),
                                clusterEParametrosProjection,
                                perfilExecucaoSupplyPlan);

        // só mantém locations que são executadas de acordo com perfil execução supply plan
        // ignora o valor do campo ExecutaSupplyPlanParaTodasLocations
        LocationProjection locationProjection =
                LocationProjectionFactory.getLocationProjectionDePerfilExecucaoSupplyPlan(
                                perfilExecucaoSupplyPlan, clusterEParametrosProjection);
        locationProjection = getLocationProjectionComEscopoEnterprise(
                        perfilExecucaoSupplyPlan,
                        clusterEParametrosProjection,
                        locationProjection);
        //só mantém materiais que são executadas de acordo com perfil execução supply plan
        MaterialProjection materialProjection =
                getMaterialProjectionComEscopoEnterprise(
                                perfilExecucaoSupplyPlan,
                                clusterEParametrosProjection,
                                MaterialProjectionFactory.getMaterialProjectionDePerfilExecucaoSupplyPlan(
                                        perfilExecucaoSupplyPlan,
                                        clusterEParametrosProjection));

        SupplyNetworkProjection supplyNetworkProjection =
                supplyNetworkProjectionFactory.getSupplyNetworkProjectionCompletoDeCache();
        Calendario calendarioSupply = supplyPlan.getCalendarioDoSupplyPlan(parametrosGlobais);

        if (novoSupplyPlan) {
            supplyPlan = validaSupplyPlanSalvoInicialCommunity(
                    supplyPlanRepository.saveAndFlush(supplyPlan));
        } else {
            // Reexecucao de plano existente: atualiza somente os metadados para
            // evitar merge do grafo completo de malha/perfil ja carregado.
            supplyPlanRepository.atualizaMetadadosGeracao(
                    supplyPlan.getId(),
                    supplyPlan.getHorarioGeracao(),
                    supplyPlan.getUsuarioGeradorPlano());
        }

        // envolvendo em try/catch para remover o plano caso ele seja novo e haja um erro de execução
        try {
            // Atualiza o estado inicial do plano antes da heuristica. No
            // Community puro, somente estoque inicial, plano de demanda e
            // capacidade produtiva seguem para o calculo; o overlay Enterprise
            // pode acrescentar ordens abertas pela extensao tipada abaixo.
            if(novoSupplyPlan) { // executa somente se novo supply plan
                log.info("Carregando estoques iniciais do supply plan Community");
                atualizaSupplyPlanComEstoquesIniciaisOrdensProducaoTransferenciasComprasEstoqueEmTransito(
                        supplyPlan,
                        supplyPlanParaProjecaoEstoqueInicial,
                        perfilExecucaoSupplyPlan,
                        supplyNetworkProjection,
                        politicaEstoquesProjection,
                        materialProjection,
                        locationProjection.getLocationsAtivasSetComTiposLocation(
                                LocationAbstract.TipoLocation.INTERNA, LocationAbstract.TipoLocation.PONTO_TRANSBORDO, LocationAbstract.TipoLocation.FORNECEDOR)); // apenas locations internas, transbordos ou fornecedores
                // Repete a preparação para locations cliente quando elas são
                // planejadas diretamente pelo perfil de execução.
                if (!perfilExecucaoSupplyPlan.getLocationsClienteApenasPropagamDemanda()) {
                    atualizaSupplyPlanComEstoquesIniciaisOrdensProducaoTransferenciasComprasEstoqueEmTransito(
                            supplyPlan,
                            supplyPlanParaProjecaoEstoqueInicial,
                            perfilExecucaoSupplyPlan,
                            supplyNetworkProjection,
                            politicaEstoquesProjection,
                            materialProjection,
                            locationProjection.getLocationsAtivasSetComTiposLocation(
                                    LocationAbstract.TipoLocation.CLIENTE_FINAL, LocationAbstract.TipoLocation.REGIAO_COMERCIAL)); // apenas locations de clientes
                }

            }

            /*
             * A demanda direta e a fotografia de entrada dos motores de Supply
             * em toda reexecucao. Normalmente ela e criada somente no primeiro
             * processamento para preservar ajustes persistidos. Se nenhuma linha
             * sobreviveu, porem, seguir com a fotografia vazia gera um plano sem
             * sinal e apaga as saidas sem tornar a inconsistencia observavel.
             */
            boolean demandaDiretaConsideradaGerada = deveGerarDemandaDiretaConsiderada(
                    novoSupplyPlan,
                    supplyPlan.getId());
            if (demandaDiretaConsideradaGerada) {
                if (!novoSupplyPlan) {
                    log.warn("Supply Plan {} has no persisted direct-demand snapshot on rerun; rebuilding it from demand plan",
                            supplyPlan.getId());
                }

                // Carrega a DemandaDiretaConsiderada somente a partir do Demand Plan.
                // Carteira, sell-in e orders transacionais sao Enterprise e nao
                // entram neste fluxo Community.
                log.info("Gerando a demanda direta considerada Community a partir do plano de demanda");
                SplitTemporalProjectionPorDfu splitTemporalProjectionPorDfu = splitTemporalProjectionFactory.geraSplitTemporalProjectionPorDfu(
                        supplyPlan.getDemandPlan(), supplyPlan);

                DemandPlanningProjection demandPlanningProjection = supplyPlanDemandCatchUpProjectionSpi
                        .getDemandPlanningProjectionParaDemandaDireta(
                        perfilExecucaoSupplyPlan,
                        supplyPlan.getDemandPlan(),
                        perfilExecucaoSupplyPlan.getLocationsClienteApenasPropagamDemanda() ?
                                clusterEParametrosProjection.getLocationsAtivas() // se há apenas propagação, trazer a demanda de todas as locations, inclusive aquelas excluídas do perfil execução
                                : locationProjection.getLocationSet(), // se locations cliente são efetivamente planejadas, aplicar o filtro de locations do perfil de execução
                        materialProjection.getMateriaisAtivos());

                atualizaSupplyPlanComDemandaDiretaConsiderada(
                        supplyPlan,
                        perfilExecucaoSupplyPlan,
                        demandPlanningProjection,
                        splitTemporalProjectionPorDfu,
                        politicaEstoquesProjection,
                        supplyNetworkProjection,
                        unidadeMedidaProjection);

            }

            /*
             * O overlay Enterprise entra depois da demanda base estar
             * persistida e antes da escolha do motor. Assim vendas podem
             * atualizar a mesma demanda direta do heuristico, enquanto compras
             * e transferencias se tornam movimentos firmes sem duplicar o
             * calculo Community. Em reexecucao, somente recompomos a carteira
             * quando a propria fotografia de demanda precisou ser reconstruida.
             */
            if (novoSupplyPlan && supplyPlanOpenOrdersHeuristicSpi != null) {
                supplyPlanOpenOrdersHeuristicSpi
                        .materializaEntradasECarteiraParaNovoPlanoHeuristico(
                                supplyPlan,
                                perfilExecucaoSupplyPlan,
                                supplyNetworkProjection,
                                politicaEstoquesProjection,
                                materialProjection,
                                locationProjection);
            } else if (demandaDiretaConsideradaGerada && supplyPlanOpenOrdersHeuristicSpi != null) {
                supplyPlanOpenOrdersHeuristicSpi
                        .materializaCarteiraParaDemandaDiretaHeuristica(
                                supplyPlan,
                                perfilExecucaoSupplyPlan,
                                supplyNetworkProjection,
                                politicaEstoquesProjection,
                                materialProjection,
                                locationProjection);
            }

            /*
             * A projection de capacidade produtiva e parte do input comum dos
             * motores de Supply Planning. O Community usa esse snapshot no
             * heuristico; o Enterprise recebe exatamente a mesma fotografia pela
             * SPI projection-aware do optimizer.
             */
            BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva = biProjectionCapacidadeProdutivaFactory
                    .getBIProjectionCapacidadeProdutiva(
                            supplyPlan, calendarioSupply);

            switch (perfilExecucaoSupplyPlan.getModoExecucao()) {
                case HEURISTICO:
                    log.info("Gerando Supply Plan Heurístico");
                    heuristicoService.executaSupplyPlanHeuristico(
                            supplyPlan, perfilExecucaoSupplyPlan, supplyNetworkProjection, biProjectionCapacidadeProdutiva,
                            politicaEstoquesProjection,
                            materialProjection, locationProjection);
                    break;
                case OTIMIZADOR:
                    /*
                     * O otimizador Enterprise consome exatamente a mesma
                     * fotografia de dados montada para o fluxo base:
                     * malha, capacidade, politica de estoque e filtros de
                     * material/location. Passar essas projections pela SPI evita
                     * que o overlay privado reconstrua dados pesados com algum
                     * recorte diferente do usado pelo service Community.
                     */
                    getSupplyPlanOptimizationServiceEnterprise().executaSupplyPlan(
                            supplyPlan,
                            supplyPlanParaProjecaoEstoqueInicial,
                            perfilExecucaoSupplyPlan,
                            novoSupplyPlan,
                            consideraRequisicoesEtapaAnterior,
                            consideraOrdensProducaoPlanejadasEtapaAnterior,
                            supplyNetworkProjection,
                            biProjectionCapacidadeProdutiva,
                            politicaEstoquesProjection,
                            materialProjection,
                            locationProjection);
                    break;
                default:
                    throw new RequiresEnterpriseVersionException("Supply Planning execution engine");
            }

            capacidadeEfetivaSupplyPlanService.salvaCapacidadesEfetivasSupplyPlan(
                    supplyPlan,
                    calendarioSupply,
                    perfilExecucaoSupplyPlan,
                    supplyNetworkProjection,
                    biProjectionCapacidadeProdutiva);

            processaArtefatosComplementaresAposExecucaoSupplyPlan(
                    supplyPlan,
                    calendarioSupply,
                    perfilExecucaoSupplyPlan,
                    supplyNetworkProjection);

        /*
         * Se a execucao falhar, removemos apenas planos novos para nao deixar
         * um Supply Plan parcial na tela. O fluxo Community e suas SPIs
         * Enterprise opcionais nao declaram checked exceptions nesta borda;
         * falhas de engine, projection e repository chegam como runtime. O
         * cleanup interno permanece separado para preservar a excecao original
         * se a remocao do plano parcial tambem falhar.
         */
        } catch (RuntimeException e) {
            log.error("Erro ao executar Supply Plan Community {}", supplyPlan.getId(), e);
            try {
                if (novoSupplyPlan) {
                    supplyPlanRepository.flush();
                    deleteSupplyPlan(supplyPlan.getId());
                    supplyPlanRepository.flush();
                }
            } catch (RuntimeException e2) {
                // Cleanup pos-falha usa apenas repositories/deletes runtime.
                // Preservamos sempre a excecao original da execucao do plano.
                log.error("Supply Plan Community {} nao pode ser removido apos erro de execucao", supplyPlan.getId(), e2);
                throw e;
            }
            throw e;
        }

    }

    /**
     * Mantem a fotografia de demanda direta de reexecucoes normais e a
     * recompõe exclusivamente quando ela esta ausente.
     *
     * <p>O booleano explicita o predicado da reexecucao para que sua regra seja
     * validada sem reconstruir as projections pesadas de Supply Planning.</p>
     */
    boolean deveGerarDemandaDiretaConsiderada(
            boolean novoSupplyPlan,
            Long supplyPlanId) {

        return novoSupplyPlan
                || !demandaDiretaConsideradaLinhaRepository
                        .existsByDemandaDiretaConsideradaLinhaCompositeKeySupplyPlanId(supplyPlanId);

    }

    @Transactional
    public void deleteSupplyPlan(Long supplyPlanId) {


        /*
         * Outros planos podem usar este Supply Plan como referência de
         * cobertura target. Eles permanecem válidos; anulamos somente o
         * vínculo opcional em um único round-trip antes de apagar filhos e
         * header do plano referenciado.
         */
        supplyPlanRepository.clearSupplyPlanReferenceByReferencedSupplyPlanId(supplyPlanId);
        distributionPlanItemRepository.removeByKeySupplyPlanId(supplyPlanId);
        productionPlanLinhaRepository.removeByProductionPlanLinhaCompositeKeySupplyPlanId(supplyPlanId);
        inventoryPlanLinhaRepository.removeByInventoryPlanLinhaCompositeKeySupplyPlanId(supplyPlanId);
        capacidadeEfetivaSupplyPlanService.removeBySupplyPlanId(supplyPlanId);
        demandaDiretaConsideradaLinhaDAO.deleteBySupplyPlanId(supplyPlanId);

        supplyPlanRepository.deleteById(supplyPlanId);

    }

    /**
     * Permite que capabilities opcionais processem artefatos derivados depois
     * de todos os outputs e snapshots de capacidade terem sido persistidos.
     *
     * <p>O Community não possui artefato complementar neste ponto. A assinatura
     * preserva o contexto já preparado pela execução e evita que extensões
     * precisem repetir ou interceptar o ciclo completo de Supply Planning.</p>
     */
    protected void processaArtefatosComplementaresAposExecucaoSupplyPlan(
            SupplyPlan supplyPlan,
            Calendario calendarioSupply,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            SupplyNetworkProjection supplyNetworkProjection) {

        // Hook deliberadamente vazio no runtime Community.

    }

    /**
     * Valida ids numericos obrigatorios antes de qualquer chamada a repository.
     *
     * <p>As tasks Community podem ser disparadas por controller, scheduler ou
     * chamada direta de teste. Falhar aqui evita que ids ausentes virem erros
     * tecnicos de Spring Data, DAO ou repository mockado, preservando a origem
     * funcional da inconsistencia.</p>
     */
    /**
     * Valida ids textuais obrigatorios usados para malha/perfil de Supply.
     *
     * <p>Blank e tratado como ausencia real de parametro. Essa validacao fica
     * no service porque os mesmos metodos privados podem ser chamados por
     * entry points diferentes, todos antes das projections de Supply Planning.</p>
     */
    /**
     * Valida o cabecalho salvo de um Supply Plan novo antes de executar o motor.
     *
     * <p>O `saveAndFlush` inicial materializa o id usado por linhas de estoque,
     * demanda direta, producao, distribuicao e capacidade efetiva. Se o
     * repository devolver snapshot nulo ou sem as referencias funcionais
     * minimas, a rodada deve falhar imediatamente, antes de chamar heuristico ou
     * SPI Enterprise com um plano que nao pode ser persistido de forma
     * rastreavel.</p>
     */
    private SupplyPlan validaSupplyPlanSalvoInicialCommunity(
            SupplyPlan supplyPlanSalvo) {

        if (supplyPlanSalvo == null) {
            throw new IllegalStateException(
                    "Saved Supply Plan snapshot is required before Community Supply Planning execution.");
        }

        if (supplyPlanSalvo.getId() == null) {
            throw new IllegalStateException(
                    "Saved Supply Plan snapshot has no id before Community Supply Planning execution.");
        }

        if (supplyPlanSalvo.getDemandPlan() == null) {
            throw new IllegalStateException(
                    "Saved Supply Plan snapshot has no Demand Plan before Community Supply Planning execution.");
        }

        if (supplyPlanSalvo.getVersaoMalha() == null) {
            throw new IllegalStateException(
                    "Saved Supply Plan snapshot has no Supply Network version before Community Supply Planning execution.");
        }

        if (supplyPlanSalvo.getPerfilExecucaoSupplyPlanCadastrado() == null) {
            throw new IllegalStateException(
                    "Saved Supply Plan snapshot has no execution profile before Community Supply Planning execution.");
        }

        return supplyPlanSalvo;

    }

    /**
     * Resolve a projection de parametros e clusters usada por Supply Planning.
     *
     * <p>Esse snapshot vem do cache/factory comum e alimenta calendario,
     * filtros de material/location, unidade SNP e varias projections de
     * calculo. Validar a projection em si antes de ler seus campos evita que
     * uma falha de montagem do snapshot seja mascarada como NPE em rotinas
     * internas ou factories downstream.</p>
     */
    /**
     * Resolve os parametros globais carregados na projection estrutural.
     *
     * <p>O estoque inicial Community depende da unidade padrao SNP e do
     * calendario funcional. Esses parametros devem vir da mesma fotografia de
     * cluster/material/location usada pelo restante do Supply Plan; se a
     * projection estiver incompleta, a falha precisa acontecer aqui, antes de
     * chegar em factories de estoque ou rotinas heuristicas.</p>
     */
    /**
     * Valida a projection de conversao antes de montar estoque inicial.
     *
     * <p>Mesmo no Community, o estoque pode ser carregado em unidade diferente
     * da unidade padrao SNP. A projection de UOM e parte do snapshot fisico
     * minimo do heuristico e nao deve virar `NullPointerException` dentro da
     * factory.</p>
     */
    /**
     * Resolve a projection de malha usada como snapshot estrutural do Supply.
     *
     * <p>Mesmo quando o calculo Community usa apenas estoque inicial e
     * heuristico, a malha carrega tambem parametros, clusters e conversao de
     * unidade. Validar a projection na entrada do preparo evita que chamadas
     * diretas deste metodo publico quebrem por getter nulo antes de uma
     * mensagem funcional de snapshot incompleto.</p>
     */
    /**
     * Resolve a policy projection usada pelo fluxo principal de Supply.
     *
     * <p>Politica de estoques e parte do calculo aberto: alimenta safety stock,
     * inventory plan e majoracoes quantitativas do heuristico. Projection nula
     * indica falha de montagem do snapshot, nao ausencia operacional de
     * politica.</p>
     */
    /**
     * Resolve a projection de locations do perfil de execucao.
     *
     * <p>Essa projection e consumida antes do motor heuristico quando o plano
     * novo prepara estoque inicial e demanda direta. Por isso o service
     * principal precisa validar a fotografia logo depois da factory.</p>
     */
    /**
     * Applies the optional private location scope only after the Community has
     * established its own active-location and demand-propagation rules.
     *
     * <p>Keeping this call here gives every atomic execution path the exact
     * same scope: heuristic execution, Enterprise optimizer SPI and atomic
     * process-chain steps all consume the projection returned by this method.
     * The absence of an Enterprise bean is deliberately an identity operation.
     * It is therefore impossible for Community runtime to perform a private
     * repository lookup merely because this extension point exists.</p>
     */
    private LocationProjection getLocationProjectionComEscopoEnterprise(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            ClusterEParametrosProjection clusterEParametrosProjection,
            LocationProjection locationProjectionCommunity) {

        if (supplyPlanExecutionProfileLocationScope == null) {
            return locationProjectionCommunity;
        }

        return supplyPlanExecutionProfileLocationScope
                .getLocationProjectionParaExecucaoSupplyPlan(
                        perfilExecucaoSupplyPlan,
                        clusterEParametrosProjection,
                        locationProjectionCommunity);

    }

    /**
     * Applies the optional private material scope only after Community has
     * built the active-material snapshot consumed by every Supply motor.
     *
     * <p>Without the Enterprise bean this is an identity operation, so the
     * open edition never resolves a private filter or performs an extra query.
     * Keeping the hook before inventory, direct demand and motor dispatch makes
     * the same material projection reach heuristic, optimizer and process
     * chain execution.</p>
     */
    private MaterialProjection getMaterialProjectionComEscopoEnterprise(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            ClusterEParametrosProjection clusterEParametrosProjection,
            MaterialProjection materialProjectionCommunity) {

        if (supplyPlanExecutionProfileMaterialScope == null) {
            return materialProjectionCommunity;
        }

        return supplyPlanExecutionProfileMaterialScope
                .getMaterialProjectionParaExecucaoSupplyPlan(
                        perfilExecucaoSupplyPlan,
                        clusterEParametrosProjection,
                        materialProjectionCommunity);

    }

    /**
     * Resolve a projection de materiais do perfil de execucao.
     *
     * <p>Materiais filtrados sao parte da fotografia comum que passa por
     * estoque inicial, demanda direta, heuristico Community e SPI Enterprise.
     * Snapshot nulo deve falhar antes desses consumidores.</p>
     */
    /**
     * Inicializa o Inventory Plan no periodo -1 a partir do estoque inicial
     * Community.
     *
     * <p>Pedidos, transferencias, compras, ordens firmes e estoque em transito
     * pertencem ao Enterprise. Este metodo preserva o nome historico por ora,
     * mas a implementacao Community persiste apenas o estoque de abertura
     * calculado ou extraido.</p>
     *
     * @param supplyPlan
     */
    public void atualizaSupplyPlanComEstoquesIniciaisOrdensProducaoTransferenciasComprasEstoqueEmTransito(
            SupplyPlan supplyPlan,
            @Nullable SupplyPlan supplyPlanParaProjecaoEstoqueInicial,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            SupplyNetworkProjection supplyNetworkProjection,
            PoliticaEstoquesProjection politicaEstoquesProjection,
            MaterialProjection materialProjection,
            Collection<Location> locations) {


        UnidadeMedidaProjection unidadeMedidaProjection =
                supplyNetworkProjection.getConversaoUnidadeMedidaProjection();
        ClusterEParametrosProjection clusterEParametrosProjection =
                supplyNetworkProjection.getClusterEParametrosProjection();
        ParametrosGlobais parametrosGlobais =
                clusterEParametrosProjection.getParametrosGlobais();

        Calendario calendarioSupplyPlan = supplyPlan.getCalendarioDoSupplyPlan(parametrosGlobais);
        VersaoMalha versaoMalha = supplyPlan.getVersaoMalha();

        LocationProjection locationProjection = LocationProjectionFactory.getProjectionSetLocations(locations, clusterEParametrosProjection);

        SupplyPlanningMultiplasLocationsProjection supplyPlanningMultiplasLocationsProjection = supplyPlanProjectionFactory.getSupplyPlanningMultiplasLocationsProjectionVazio(
                supplyPlan,
                perfilExecucaoSupplyPlan,
                supplyNetworkProjection,
                politicaEstoquesProjection,
                materialProjection,
                locationProjection);

        // POPULA PROJECTION COM ESTOQUES INICIAIS ----------------
        if (perfilExecucaoSupplyPlan.getConsideraEstoqueInicial()) {
            if (supplyPlanParaProjecaoEstoqueInicial == null) {
                EstoqueProjectionLocationProduto estoqueProjectionLocationProduto = getEstoqueProjectionProdutoSemProjecao(
                        supplyPlan.getDataInicioPlano(),
                        locationProjection,
                        materialProjection,
                        clusterEParametrosProjection,
                        unidadeMedidaProjection);
                supplyPlanProjectionFactory.populaInventoryPlanComEstoqueInicial(
                        supplyPlanningMultiplasLocationsProjection,
                        estoqueProjectionLocationProduto);
            } else {
                /*
                 * A projecao a partir de um Supply Plan passado ainda e feita
                 * por location porque a projection de estoque inicial depende
                 * do recorte material/location de cada location. Uma otimizacao
                 * futura deve preservar esse contrato antes de tentar reduzir
                 * o numero de projections intermediarias.
                 */
                for (Location location : locationProjection.getLocationsAtivas()) {
                    SupplyPlanningProjection supplyPlanningProjection = supplyPlanningMultiplasLocationsProjection.getSupplyPlanningProjectionDeLocation(location);
                    EstoqueProjectionProduto estoqueProjectionProduto = getEstoqueProjectionProdutoProjetadoAPartirUltimoEstoqueCadastradoESupplyPlanPassado(
                            supplyPlanningProjection.getCalendario().getPrimeiraDataHorarioPeriodo(
                                    supplyPlanningProjection.getCalendario().getPosicaoPeriodoPresente()),
                            perfilExecucaoSupplyPlan, location,
                            materialProjection,
                            supplyPlanParaProjecaoEstoqueInicial,
                            supplyNetworkProjection, unidadeMedidaProjection, politicaEstoquesProjection);

                    // Atualiza estoque inicial Community. Remessas nao
                    // faturadas/nao separadas pertencem ao Enterprise.
                    supplyPlanProjectionFactory.populaInventoryPlanComEstoqueInicial(
                            supplyPlanningProjection, estoqueProjectionProduto);
                }
            }
        }

        /*
         * O ponto de extensao fica apos o estoque inicial e antes da
         * persistencia das linhas do snapshot. Assim uma implementacao
         * Enterprise compartilha as mesmas ProductionPlanLinhas firmes com
         * heuristico e optimizer, enquanto a ausencia do bean preserva o
         * recorte Community sem ordens transacionais.
         */
        if (supplyPlanFirmProductionOrdersSpi != null) {
            supplyPlanFirmProductionOrdersSpi.populaOrdensFirmesProducao(
                    supplyPlanningMultiplasLocationsProjection);
        }

        // ATUALIZA DADOS TRANSACIONAIS PLANO COM VALORES INICIAIS DE ESTOQUE -------------
        // Community nao processa sales orders, sell-in, transfer orders,
        // purchase orders, production orders ou estoque em transito como dado
        // transacional de entrada. O loop abaixo persiste apenas o estoque
        // inicial ja populado na projection de cada location.
        for (Location location : locationProjection.getLocationsAtivas()) {

            // se estamos trabalhando com propagação simples de demanda não entraremos nos detalhes das locations clientes
            if (perfilExecucaoSupplyPlan.getLocationsClienteApenasPropagamDemanda()
                    && perfilExecucaoSupplyPlan.getModoPropagacaoDemanda().verificaSeRealizaPropagacao(location)) continue;

            SupplyPlanningProjection supplyPlanningProjection = supplyPlanningMultiplasLocationsProjection.getSupplyPlanningProjectionDeLocation(location);

            // estoque inicial - remessas não faturadas
            saveInventoryPlanDePlanningProjection(supplyPlanningProjection, -1, false);

            // O SPI Enterprise, quando presente e habilitado, ja adicionou as
            // linhas firmes nesta mesma projection. Persisti-las aqui garante
            // que ambos os motores reabram a mesma fotografia em lote.
            if (!supplyPlanningProjection.getTodosProductionPlanLinhasOutput().isEmpty()) {
                saveProductionPlanOutputDePlanningProjection(supplyPlanningProjection, false);
            }
        }
    }

    /**
     * Community nao aplica curvas de custo logistico no Supply Plan. Custos,
     * P&L e cost-to-serve sao capacidades Enterprise; portanto qualquer perfil
     * que tente ativar sua aplicacao deve falhar explicitamente antes da
     * montagem de projections economicas.
     *
     * <p>A selecao de um modelo fisico de curvas e validada na borda DTO; aqui
     * restam apenas flags legadas que podem vir de perfil antigo ou payload
     * compartilhado ja convertido para entidade.</p>
     */
    private void validaCurvasCustoLogisticoCommunity(PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) {

        if (perfilExecucaoSupplyPlan.getAplicaCurvasCustoFrete()
                || perfilExecucaoSupplyPlan.getAplicaCurvasCustoLocation()) {
            throw new RequiresEnterpriseVersionException("Logistics cost curves");
        }

    }

    /**
     * Community nao executa otimizadores de inteligencia artificial.
     *
     * <p>A unica excecao e o motor atomico `OTIMIZADOR` quando o bean
     * Enterprise real esta presente. Nesse caso o service compartilhado apenas
     * prepara a fotografia base e delega para o overlay privado, onde os
     * loaders do optimizer conhecem os modos de AI optimizer. Etapas
     * heuristicas continuam bloqueadas mesmo no runtime Enterprise, porque o
     * heuristico Community nao possui scheduling/AI solver.</p>
     */
    private void validaOtimizadorInteligenciaArtificialCommunity(PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) {

        if (!PerfilExecucaoSupplyPlan.OtimizadorInteligenciaArtificial.DESABILITADO
                .equals(perfilExecucaoSupplyPlan.getOtimizadorInteligenciaArtificial())
                && !devePermitirOtimizadorInteligenciaArtificialNoOptimizerEnterprise(perfilExecucaoSupplyPlan)) {
            throw new RequiresEnterpriseVersionException("AI optimizer");
        }

    }

    /**
     * Decide se o AI optimizer pode atravessar a borda Community por estar
     * sendo consumido pelo optimizer Enterprise.
     *
     * <p>A regra segue a mesma filosofia dos pedidos transacionais: nao usamos
     * variavel externa de edicao. O que habilita a passagem e a presenca real
     * do bean Enterprise opcional e o modo atomico `OTIMIZADOR`. Process chain
     * chega aqui por etapa concreta; quando a etapa for otimizador, seu perfil
     * tambem tera modo `OTIMIZADOR` e sera permitido somente se a SPI existir.</p>
     */
    private boolean devePermitirOtimizadorInteligenciaArtificialNoOptimizerEnterprise(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) {

        return PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR.equals(perfilExecucaoSupplyPlan.getModoExecucao())
                && supplyPlanOptimizationService != null;

    }

    /**
     * Community possui somente o motor heuristico de Supply Planning. Quando
     * houver overlay Enterprise no classpath, os beans opcionais de otimizador
     * ou process chain podem satisfazer o modo solicitado; sem esses beans, o
     * mesmo perfil falha com RequiresEnterpriseVersionException.
     */
    private void validaModoExecucaoCommunity(PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) {

        if (perfilExecucaoSupplyPlan == null) {
            /*
             * Perfil nulo representa estado estrutural invalido da chamada ou
             * do Supply Plan persistido. Falhar aqui evita que a resolucao de
             * SPI Enterprise esconda o problema como NullPointerException.
             */
            throw new IllegalStateException("Supply Planning execution profile is null.");
        }

        if (SupplyPlanningExecutionModelCatalog.isModoExecucaoSupplyPlanCommunity(
                perfilExecucaoSupplyPlan.getModoExecucao())) {
            return;
        }

        getSupplyPlanEnterpriseExecutionService(perfilExecucaoSupplyPlan.getModoExecucao());

    }

    /**
     * Resolve os motores Enterprise opcionais sem registry, ObjectProvider ou
     * lista de estrategias. O campo explicito torna visivel no codigo quais
     * features dependem de implementacao Enterprise.
     */
    private SupplyPlanExecutionServiceSpi getSupplyPlanEnterpriseExecutionService(ModoExecucao modoExecucao) {

        if (modoExecucao == null) {
            throw new IllegalStateException("Supply Planning execution mode is required.");
        }

        return switch (modoExecucao) {
            case OTIMIZADOR -> getSupplyPlanOptimizationServiceEnterprise();
            case PROCESS_CHAIN -> getSupplyPlanProcessChainServiceEnterprise();
            default -> throw new RequiresEnterpriseVersionException("Supply Planning execution engine");
        };

    }

    /**
     * Resolve a implementacao Enterprise tipada do otimizador.
     *
     * <p>A chamada direta evita usar o campo opcional sem null-check nos pontos
     * onde a assinatura especifica do otimizador recebe projections
     * compartilhadas ja montadas pelo fluxo base.</p>
     */
    private SupplyPlanOptimizationServiceSpi getSupplyPlanOptimizationServiceEnterprise() {

        if (supplyPlanOptimizationService == null) {
            throw new RequiresEnterpriseVersionException("Supply Planning Optimizer");
        }

        return supplyPlanOptimizationService;

    }

    /**
     * Resolve a implementacao Enterprise tipada de process chain.
     */
    private SupplyPlanProcessChainServiceSpi getSupplyPlanProcessChainServiceEnterprise() {

        if (supplyPlanProcessChainService == null) {
            throw new RequiresEnterpriseVersionException("Supply Planning Process Chain");
        }

        return supplyPlanProcessChainService;

    }

    /**
     * Community deve receber demanda futura somente do Demand Plan e estoque
     * inicial somente do dado de inventario. Perfis legados que ainda apontem
     * para carteira, sell-in, pedidos, ordens firmes, backlog carry-over ou MTO
     * puro devem falhar antes de montar projections.
     *
     * <p>Pedidos sell-out, sell-in, transferência e compra podem atravessar a
     * borda somente quando uma capacidade Enterprise real os materializa: o
     * optimizer usa sua projection privada e o heurístico recebe movimentos
     * firmes e demanda direta pela extensão de ordens abertas. A exceção não
     * reabre ordens genéricas de produção, backlog carry-over, MTO sem forecast
     * ou process chain diretamente no Community.</p>
     */
    private void validaPedidosTransacionaisCommunity(PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) {

        if (perfilExecucaoSupplyPlan.getConsideraOrdensProducao()) {
            throw new RequiresEnterpriseVersionException("Supply Planning transactional orders");
        }

        if ((perfilExecucaoSupplyPlan.getConsideraOrdensSellout()
                || perfilExecucaoSupplyPlan.getConsideraOrdensSellin()
                || perfilExecucaoSupplyPlan.getConsideraOrdensTransferencia()
                || perfilExecucaoSupplyPlan.getConsideraOrdensCompra())
                && !devePermitirPedidosTransacionaisEnterprise(perfilExecucaoSupplyPlan)) {
            throw new RequiresEnterpriseVersionException("Supply Planning transactional orders");
        }

        if (perfilExecucaoSupplyPlan.getPermiteBacklogDemanda()
                && !devePermitirBacklogCarryOverNoOptimizerEnterprise(perfilExecucaoSupplyPlan)) {
            throw new RequiresEnterpriseVersionException("Supply Planning backlog carry-over");
        }

        if (perfilExecucaoSupplyPlan.getForcaModeloMakeToOrder()
                && !devePermitirForceMakeToOrderNoOptimizerEnterprise(perfilExecucaoSupplyPlan)) {
            throw new RequiresEnterpriseVersionException("Supply Planning fully make-to-order");
        }

        /*
         * MTO sem forecast continua indisponivel no runtime Community. No
         * overlay Enterprise, porem, o mesmo SPI de optimizer que executa o
         * modelo MTO materializa a carteira e seus valores no snapshot de
         * demanda direta; bloquear a flag aqui impediria uma capability que ja
         * possui consumidor concreto.
         */
        if (!perfilExecucaoSupplyPlan.getConsideraForecastParaMto()
                && !devePermitirForceMakeToOrderNoOptimizerEnterprise(
                perfilExecucaoSupplyPlan)) {
            throw new RequiresEnterpriseVersionException("Supply Planning fully make-to-order");
        }

        if (!perfilExecucaoSupplyPlan.getModeloMajoracaoDemandaDiretaProjecaoEstoque()
                .equals(PerfilExecucaoSupplyPlan.ModeloMajoracaoDemandaDireta.SOMENTE_FORECAST)) {
            throw new RequiresEnterpriseVersionException("Supply Planning customer orders");
        }

        if (!perfilExecucaoSupplyPlan.getModeloMajoracaoDemandaDiretaEstoqueSeguranca()
                .equals(PerfilExecucaoSupplyPlan.ModeloMajoracaoDemandaDireta.SOMENTE_FORECAST)) {
            throw new RequiresEnterpriseVersionException("Supply Planning customer orders");
        }

    }

    /**
     * Decide se os flags de pedidos transacionais podem atravessar a borda
     * Community por estarem sendo consumidos por uma capacidade Enterprise.
     *
     * <p>A regra olha para a presenca real do bean opcional, nao para variavel
     * externa de edicao. Assim, no runtime Community puro o mesmo payload segue
     * bloqueado, enquanto no runtime Enterprise o service compartilhado permite
     * que o optimizer privado ou a extensão do heurístico recebam apenas os
     * parâmetros que sabem executar.</p>
     */
    private boolean devePermitirPedidosTransacionaisEnterprise(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) {

        if (PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR
                .equals(perfilExecucaoSupplyPlan.getModoExecucao())) {
            return supplyPlanOptimizationService != null;
        }

        return PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO
                .equals(perfilExecucaoSupplyPlan.getModoExecucao())
                && supplyPlanOpenOrdersHeuristicSpi != null;

    }

    /**
     * Autoriza backlog carry-over somente no runtime que materializa as
     * variaveis privadas do modelo otimizado.
     *
     * <p>A entidade compartilhada pode reter a escolha para evitar uma tabela
     * auxiliar. Isso nao e autorizacao suficiente: no Community puro, em
     * perfis heuristico ou em process chain, a validacao anterior continua
     * interrompendo a rodada antes de projections e calculos.</p>
     */
    private boolean devePermitirBacklogCarryOverNoOptimizerEnterprise(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) {

        return PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR.equals(
                perfilExecucaoSupplyPlan.getModoExecucao())
                && supplyPlanOptimizationService != null;

    }

    /**
     * Autoriza o modelo make-to-order forçado apenas no optimizer Enterprise
     * que declara a restricao correspondente.
     *
     * <p>O modo continua proibido no Community puro, no heuristico e em process
     * chain. A escolha persistida na entidade compartilhada nao basta: a
     * presenca do SPI privado e obrigatoria para que a restricao de producao
     * versus demanda seja efetivamente materializada.</p>
     */
    private boolean devePermitirForceMakeToOrderNoOptimizerEnterprise(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) {

        return PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR.equals(
                perfilExecucaoSupplyPlan.getModoExecucao())
                && supplyPlanOptimizationService != null;

    }

    /**
     * Limpa os artefatos recalculaveis antes de reutilizar um Supply Plan.
     *
     * <p>Este metodo e propositalmente publico porque os runtimes Enterprise
     * opcionais, como process chain e otimizador, precisam reaproveitar
     * exatamente a mesma limpeza da rodada heuristica Community. A limpeza
     * remove somente valores recalculados do plano irrestrito e preserva linhas
     * de ajuste manual do Planning Book. Capacidade efetiva fica fora deste
     * reset porque {@link CapacidadeEfetivaSupplyPlanService#salvaCapacidadesEfetivasSupplyPlan}
     * ja remove e recria o snapshot ao fim de cada execucao normal.</p>
     *
     * <p>O inventario irrestrito so e reiniciado quando o perfil salva
     * Inventory Plan. Essa condicao replica o comportamento historico da
     * reexecucao heuristica e evita apagar linhas que nao fazem parte da
     * superficie materializada do perfil. Process chains Enterprise devem usar
     * a sobrecarga com booleano explicito, pois o perfil salvo no Supply Plan
     * fisico e o perfil base da cadeia, nao necessariamente o perfil atomico
     * que materializou inventario.</p>
     *
     * @param supplyPlan plano existente que sera recalculado.
     */
    public void reiniciaArtefatosSupplyPlanExistente(SupplyPlan supplyPlan) {

        if (supplyPlan == null) {
            throw new IllegalArgumentException("Supply Plan is required.");
        }

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = supplyPlan.getPerfilExecucaoSupplyPlan();
        boolean reiniciaPlanoInventarioIrrestrito =
                perfilExecucaoSupplyPlan != null && perfilExecucaoSupplyPlan.getSalvaInventoryPlan();

        reiniciaArtefatosSupplyPlanExistente(
                supplyPlan,
                reiniciaPlanoInventarioIrrestrito);

    }

    /**
     * Limpa os artefatos recalculaveis com controle explicito sobre Inventory
     * Plan.
     *
     * <p>Esta sobrecarga existe para process chains Enterprise. Uma cadeia pode
     * ter perfil base apenas orquestrador e etapas heuristicas com configuracoes
     * diferentes de persistencia de inventario. O chamador Enterprise valida a
     * lista de etapas antes e passa aqui se alguma etapa materializa Inventory
     * Plan; o Community continua sem conhecer a estrutura privada da cadeia.</p>
     *
     * @param supplyPlan plano existente que sera recalculado.
     * @param reiniciaPlanoInventarioIrrestrito indica se linhas de Inventory
     * Plan irrestrito tambem devem ser zeradas.
     */
    public void reiniciaArtefatosSupplyPlanExistente(
            SupplyPlan supplyPlan,
            boolean reiniciaPlanoInventarioIrrestrito) {

        if (supplyPlan == null) {
            throw new IllegalArgumentException("Supply Plan is required.");
        }

        Long supplyPlanId = supplyPlan.getId();

        resetPlanoDistribuicaoIrrestritoBySupplyPlanId(supplyPlanId);
        resetPlanoProducaoIrrestritoBySupplyPlanId(supplyPlanId);

        if (reiniciaPlanoInventarioIrrestrito) {
            resetPlanoInventarioIrrestritoBySupplyPlanId(supplyPlanId);
        }

    }

    /**
     * Zera as quantidades recalculadas do plano de distribuicao irrestrito ao
     * reutilizar um Supply Plan existente.
     *
     * <p>Ajuste manual de Supply Planning e preservado. Colunas historicas de
     * New Materials, Uplift e carteira tambem sao zeradas quando existem na
     * tabela, evitando que dados Enterprise ou legados contaminem a rodada
     * Community.</p>
     *
     * <p>O id e obrigatorio porque este metodo executa update JDBC direto. Id
     * ausente indica snapshot quebrado do plano existente, nao ausencia de
     * linhas a recalcular.</p>
     *
     * @param supplyPlanId identificador do plano reaproveitado.
     */
    @Transactional
    public void resetPlanoDistribuicaoIrrestritoBySupplyPlanId(Long supplyPlanId) {

        resetQuantidadesRecalculadasBySupplyPlanId(
                "distribution_plan_item",
                supplyPlanId,
                List.of(
                        "quantidade_requisicao_baseline",
                        "quantidade_requisicao_atendimento_carteira",
                        "quantidade_pedido_atendimento_carteira"),
                List.of(
                        "quantidade_requisicao_itens_novos",
                        "quantidade_requisicao_uplift",
                        "quantidade_requisicao_ajuste_demanda"));

    }

    /**
     * Zera as quantidades recalculadas de estoque do plano irrestrito ao
     * reutilizar um Supply Plan existente.
     *
     * <p>Ajuste manual de Supply Planning e preservado. Colunas historicas de
     * New Materials e Uplift tambem sao zeradas quando existem na tabela.</p>
     *
     * <p>O id e obrigatorio porque este metodo executa update JDBC direto. Id
     * ausente indica snapshot quebrado do plano existente, nao ausencia de
     * linhas a recalcular.</p>
     *
     * @param supplyPlanId identificador do plano reaproveitado.
     */
    @Transactional
    public void resetPlanoInventarioIrrestritoBySupplyPlanId(Long supplyPlanId) {

        resetQuantidadesRecalculadasBySupplyPlanId(
                "inventory_plan_linha",
                supplyPlanId,
                List.of(
                        "quantidade_estoque_seguranca_baseline",
                        "quantidade_estoque_baseline"),
                List.of(
                        "quantidade_estoque_seguranca_itens_novos",
                        "quantidade_estoque_itens_novos",
                        "quantidade_estoque_uplift",
                        "quantidade_estoque_ajuste_demanda"));

    }

    /**
     * Zera as quantidades recalculadas de producao do plano irrestrito ao
     * reutilizar um Supply Plan existente.
     *
     * <p>Ajuste manual de Supply Planning e preservado. Colunas historicas de
     * New Materials e Uplift tambem sao zeradas quando existem na tabela.</p>
     *
     * <p>O id e obrigatorio porque este metodo executa update JDBC direto. Id
     * ausente indica snapshot quebrado do plano existente, nao ausencia de
     * linhas a recalcular.</p>
     *
     * @param supplyPlanId identificador do plano reaproveitado.
     */
    @Transactional
    public void resetPlanoProducaoIrrestritoBySupplyPlanId(Long supplyPlanId) {

        resetQuantidadesRecalculadasBySupplyPlanId(
                "production_plan_linha",
                supplyPlanId,
                List.of("quantidade_sugestao_producao_baseline"),
                List.of(
                        "quantidade_sugestao_producao_itens_novos",
                        "quantidade_sugestao_producao_uplift",
                        "quantidade_sugestao_producao_ajuste_demanda"));

    }

    /**
     * Zera colunas recalculadas respeitando o schema fisico da edicao ativa.
     *
     * <p>O schema Community materializa apenas os componentes de demanda que o
     * motor aberto calcula. O Enterprise acrescenta New Materials, Uplift e
     * Demand Adjustment nas mesmas tabelas. A reexecucao compartilhada precisa
     * limpar todos os componentes presentes, mas nao pode referenciar colunas
     * privadas ausentes no SQLite Community.</p>
     *
     * <p>As colunas obrigatorias representam o contrato Community e sua
     * ausencia falha explicitamente. As colunas opcionais sao adicionadas ao
     * mesmo UPDATE somente quando o metadata da tabela confirma sua presenca,
     * preservando a limpeza completa em schemas Enterprise.</p>
     *
     * @param tableName tabela conhecida de output do Supply Plan.
     * @param supplyPlanId plano existente que sera reexecutado.
     * @param requiredColumnNames colunas obrigatorias no schema Community.
     * @param optionalColumnNames extensoes presentes apenas em schemas maiores.
     */
    private void resetQuantidadesRecalculadasBySupplyPlanId(
            String tableName,
            Long supplyPlanId,
            List<String> requiredColumnNames,
            List<String> optionalColumnNames) {

        if (supplyPlanId == null) {
            throw new IllegalArgumentException(
                    "Supply Plan id is required to reset recalculated quantities from "
                            + tableName
                            + ".");
        }

        Set<String> existingColumnNames = getExistingColumnNames(tableName);
        List<String> missingRequiredColumnNames = requiredColumnNames.stream()
                .filter(columnName -> !existingColumnNames.contains(columnName))
                .toList();

        if (!missingRequiredColumnNames.isEmpty()) {
            throw new IllegalStateException(
                    "Table "
                            + tableName
                            + " is missing required Community columns: "
                            + String.join(", ", missingRequiredColumnNames)
                            + ".");
        }

        List<String> columnNamesToReset = new ArrayList<>(requiredColumnNames);
        optionalColumnNames.stream()
                .filter(existingColumnNames::contains)
                .forEach(columnNamesToReset::add);

        String setClause = columnNamesToReset.stream()
                .map(columnName -> columnName + " = 0")
                .collect(Collectors.joining(", "));

        jdbcTemplate.update(
                "update " + tableName + " SET " + setClause + " where supply_plan_id = ?",
                supplyPlanId);

    }

    /**
     * Le os nomes de coluna diretamente do ResultSet metadata sem consultar
     * linhas funcionais da tabela.
     *
     * <p>A consulta vazia e portavel entre SQLite e MySQL, ao contrario de
     * PRAGMA ou INFORMATION_SCHEMA. Os nomes sao normalizados para lower case
     * porque drivers podem devolver identificadores com casing diferente.</p>
     *
     * @param tableName tabela conhecida de output do Supply Plan.
     * @return nomes fisicos de coluna normalizados.
     */
    private Set<String> getExistingColumnNames(String tableName) {

        return jdbcTemplate.query(
                "select * from " + tableName + " where 1 = 0",
                resultSet -> {
                    ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                    Set<String> existingColumnNames = new LinkedHashSet<>();

                    for (int columnIndex = 1;
                            columnIndex <= resultSetMetaData.getColumnCount();
                            columnIndex++) {
                        existingColumnNames.add(
                                resultSetMetaData.getColumnName(columnIndex)
                                        .toLowerCase(Locale.ROOT));
                    }

                    return existingColumnNames;
                });

    }

    /**
     * Salva production plan linhas output (retrito e irrestrito) para o período indicado
     * Somente salva se algum dos campos tiver valor não-irrelevante (valor absoluto > 0.000001)
     * Trata as versões de produção temporárias
     * @param productionPlanLinhaCollection
     * @param salvaZeros
     */
    public void saveProductionPlanLinhaCollection(
            Collection<ProductionPlanLinha> productionPlanLinhaCollection,
            boolean salvaZeros) {

        validaProductionPlanLinhaCollectionCommunity(productionPlanLinhaCollection);

        /*
         * Sem o SPI privado, o Community nao tem uma fonte transacional de
         * ordens firmes e neutraliza eventuais residuos da base antes do
         * filtro/save. Quando o SPI Enterprise esta presente, ele ja
         * materializou as linhas firmes nesta mesma projection; apaga-las
         * aqui invalidaria capacidade, estoque e checkpoints subsequentes.
         */
        if (deveNeutralizarOrdensFirmesProducaoCommunity()) {
            productionPlanLinhaCollection.forEach(this::neutralizaOrdensFirmesCommunity);
        }

        List<ProductionPlanLinha> productionPlanLinhas = productionPlanLinhaCollection.stream()
                .filter(x -> salvaZeros ||
                        Math.abs(x.getQuantidadeOrdemFirmeProducaoIrrestrita()) > 0.000001 ||
                        Math.abs(x.getQuantidadeOrdemPlanejadaProducaoIrrestrita()) > 0.000001 ||
                        Math.abs(x.getQuantidadeOrdemPlanejadaProducaoRestrita()) > 0.000001 ||
                        Math.abs(x.getQuantidadeOrdemPlanejadaProducaoTrabalho()) > 0.000001)
                .toList();
        if (productionPlanLinhas.isEmpty()) return;

        VersaoProducao versaoProducaoInexistente = versaoProducaoService.getOuPersisteVersaoProducaoInexistente();

        // versões de produção criadas artificialmente para acomodar roteiros/LTs sem versão devem ser apagadas antes de salvar,
        // pois não possuem correspondência na tabela de versões de produção (id nulo)
        for (ProductionPlanLinha productionPlanLinha : productionPlanLinhaCollection) {
            if (productionPlanLinha.getVersaoProducaoCadastrada().isVersaoProducaoTemporaria()) {
                productionPlanLinha.getProductionPlanLinhaCompositeKey().setVersaoProducao(versaoProducaoInexistente);
            }
        }

        validaProductionPlanLinhasParaPersistenciaCommunity(productionPlanLinhas);
        productionPlanLinhaDAO.saveInBatch(productionPlanLinhas);
        validaProductionPlanLinhasSalvasCommunity(
                productionPlanLinhas,
                productionPlanLinhas.size());

    }

    public void removeProductionPlanLinhaZeradosCollection(
            Collection<ProductionPlanLinha> productionPlanLinhaCollection) {

        validaProductionPlanLinhaCollectionCommunity(productionPlanLinhaCollection);

        /*
         * A mesma fronteira do save vale para a limpeza: uma linha que contem
         * somente ordem firme Enterprise nao e uma linha zerada e nao pode ser
         * apagada entre checkpoints da rodada.
         */
        if (deveNeutralizarOrdensFirmesProducaoCommunity()) {
            productionPlanLinhaCollection.forEach(this::neutralizaOrdensFirmesCommunity);
        }

        List<ProductionPlanLinha> productionPlanLinhasZerados = productionPlanLinhaCollection.stream()
                .filter(x ->
                        Math.abs(x.getQuantidadeOrdemFirmeProducaoIrrestrita()) <= 0.000001 &&
                        Math.abs(x.getQuantidadeOrdemPlanejadaProducaoIrrestrita()) <= 0.000001 &&
                        Math.abs(x.getQuantidadeOrdemPlanejadaProducaoRestrita()) <= 0.000001 &&
                        Math.abs(x.getQuantidadeOrdemPlanejadaProducaoTrabalho()) <= 0.000001)
                .toList();
        if (productionPlanLinhasZerados.isEmpty()) return;

        validaProductionPlanLinhasParaDeleteCommunity(productionPlanLinhasZerados);
        productionPlanLinhaDAO.deleteInBatch(productionPlanLinhasZerados);
    }

    /**
     * Salva production plan linhas output (retrito e irrestrito) para o período indicado
     * Somente salva se algum dos campos tiver valor não-irrelevante (valor absoluto > 0.000001)
     * @param planningProjection
     * @param posicaoPeriodo
     */
    public void saveProductionPlanOutputDePlanningProjection(SupplyPlanningProjection planningProjection,
            int posicaoPeriodo, boolean salvaZeros) {
        saveProductionPlanLinhaCollection(
                planningProjection.getProductionPlanLinhaOutput(posicaoPeriodo),
                salvaZeros);
    }

    /**
     * Salva production plan linhas output (retrito e irrestrito) para todos os períodos
     * Somente salva se algum dos campos tiver valor não-irrelevante (valor absoluto > 0.000001)
     * @param planningProjection
     */
    public void saveProductionPlanOutputDePlanningProjection(SupplyPlanningProjection planningProjection, boolean salvaZeros) {
        saveProductionPlanLinhaCollection(
                planningProjection.getTodosProductionPlanLinhasOutput(),
                salvaZeros);
    }

    public void saveDistributionPlanItemCollection(
            Collection<DistributionPlanItem> distributionPlanItemCollection,
            boolean salvaZeros) {

        validaDistributionPlanItemCollectionCommunity(distributionPlanItemCollection);

        /*
         * Transferencias firmes e parcelas firmes de atendimento pertencem ao
         * Enterprise. O heuristico Community trabalha somente com transferencias
         * planejadas e demanda direta vinda do Demand Plan.
         */
        if (deveNeutralizarOrdensFirmesDistribuicaoCommunity()) {
            distributionPlanItemCollection.forEach(this::neutralizaOrdensFirmesCommunity);
        }

        List<DistributionPlanItem> distributionPlanItems = distributionPlanItemCollection.stream()
                .filter(x -> salvaZeros ||
                        Math.abs(x.getQuantidadeOrdemFirmeIrrestrita()) > 0.00001 ||
                        Math.abs(x.getQuantidadeOrdemFirmeRestrita()) > 0.00001 ||
                        Math.abs(x.getQuantidadeOrdemPlanejadaTrabalho()) > 0.00001 ||
                        Math.abs(x.getQuantidadeOrdemPlanejadaIrrestrita()) > 0.00001 ||
                        Math.abs(x.getQuantidadeOrdemPlanejadaRestrita()) > 0.00001)
                .toList();
        if (distributionPlanItems.isEmpty()) return;
        validaDistributionPlanItemsParaPersistenciaCommunity(distributionPlanItems);
        List<DistributionPlanItem> distributionPlanItemsSalvas =
                distributionPlanItemRepository.saveAll(distributionPlanItems);
        validaDistributionPlanItemsSalvasCommunity(
                distributionPlanItemsSalvas,
                distributionPlanItems.size());

    }

    public void removeDistributionPlanItemZeradosCollection(
            Collection<DistributionPlanItem> distributionPlanItemCollection) {

        validaDistributionPlanItemCollectionCommunity(distributionPlanItemCollection);

        if (deveNeutralizarOrdensFirmesDistribuicaoCommunity()) {
            distributionPlanItemCollection.forEach(this::neutralizaOrdensFirmesCommunity);
        }

        List<DistributionPlanItem> distributionPlanItemsZerados = distributionPlanItemCollection.stream()
                .filter(x ->
                        Math.abs(x.getQuantidadeOrdemFirmeIrrestrita()) <= 0.00001 &&
                        Math.abs(x.getQuantidadeOrdemFirmeRestrita()) <= 0.00001 &&
                        Math.abs(x.getQuantidadeOrdemPlanejadaTrabalho()) <= 0.00001 &&
                        Math.abs(x.getQuantidadeOrdemPlanejadaIrrestrita()) <= 0.00001 &&
                        Math.abs(x.getQuantidadeOrdemPlanejadaRestrita()) <= 0.00001)
                .toList();
        if (distributionPlanItemsZerados.isEmpty()) return;

        validaDistributionPlanItemsParaDeleteCommunity(distributionPlanItemsZerados);
        distributionPlanItemRepository.deleteAll(distributionPlanItemsZerados);

    }

    /**
     * Valida o lote de linhas de producao antes de qualquer neutralizacao ou
     * stream.
     *
     * <p>Esses metodos sao chamados tanto pelo fluxo heuristico quanto por
     * overlays Enterprise que podem reaproveitar persistencia Community para
     * artefatos compartilhados. Colecao nula ou item nulo indicam snapshot
     * quebrado de calculo, nao ausencia operacional.</p>
     */
    private void validaProductionPlanLinhaCollectionCommunity(
            Collection<ProductionPlanLinha> productionPlanLinhaCollection) {

        if (productionPlanLinhaCollection == null) {
            throw new IllegalArgumentException(
                    "Production Plan line collection is required for Community production planning persistence.");
        }

        int indiceProductionPlanLinha = 0;
        for (ProductionPlanLinha productionPlanLinha : productionPlanLinhaCollection) {
            if (productionPlanLinha == null) {
                throw new IllegalArgumentException(
                        "Production Plan line at index "
                                + indiceProductionPlanLinha
                                + " is required for Community production planning persistence.");
            }
            indiceProductionPlanLinha++;
        }

    }

    /**
     * Valida o lote de linhas de distribuicao antes de neutralizar ordens firmes
     * Enterprise ou aplicar o filtro de linhas nao-zero.
     */
    private void validaDistributionPlanItemCollectionCommunity(
            Collection<DistributionPlanItem> distributionPlanItemCollection) {

        if (distributionPlanItemCollection == null) {
            throw new IllegalArgumentException(
                    "Distribution Plan line collection is required for Community distribution planning persistence.");
        }

        int indiceDistributionPlanItem = 0;
        for (DistributionPlanItem distributionPlanItem : distributionPlanItemCollection) {
            if (distributionPlanItem == null) {
                throw new IllegalArgumentException(
                        "Distribution Plan line at index "
                                + indiceDistributionPlanItem
                                + " is required for Community distribution planning persistence.");
            }
            indiceDistributionPlanItem++;
        }

    }

    /**
     * Valida o snapshot devolvido pelo repository apos persistir linhas de
     * Production Plan.
     *
     * <p>O `saveAll` do Spring Data deve devolver as entidades salvas. Se ele
     * voltar nulo, parcial, com item nulo ou com chave incompleta, tratamos
     * como erro de persistencia/mapper e falhamos antes de o fluxo heuristico
     * assumir que o plano foi materializado corretamente.</p>
     */
    private void validaProductionPlanLinhasSalvasCommunity(
            Collection<ProductionPlanLinha> productionPlanLinhasSalvas,
            int quantidadeProductionPlanLinhasEsperada) {

        if (productionPlanLinhasSalvas == null) {
            throw new IllegalStateException(
                    "Saved Production Plan line snapshot is required after Community production planning persistence.");
        }
        if (productionPlanLinhasSalvas.size() != quantidadeProductionPlanLinhasEsperada) {
            throw new IllegalStateException(
                    "Saved Production Plan line snapshot size "
                            + productionPlanLinhasSalvas.size()
                            + " differs from expected Community production planning persistence size "
                            + quantidadeProductionPlanLinhasEsperada
                            + ".");
        }

        int indiceProductionPlanLinha = 0;
        for (ProductionPlanLinha productionPlanLinhaSalva : productionPlanLinhasSalvas) {
            if (productionPlanLinhaSalva == null) {
                throw new IllegalStateException(
                        "Saved Production Plan line at index "
                                + indiceProductionPlanLinha
                                + " is required after Community production planning persistence.");
            }
            if (productionPlanLinhaSalva.getProductionPlanLinhaCompositeKey() == null ||
                    productionPlanLinhaSalva.getSupplyPlan() == null ||
                    productionPlanLinhaSalva.getLocation() == null ||
                    productionPlanLinhaSalva.getMaterialOutput() == null ||
                    productionPlanLinhaSalva.getVersaoProducao() == null ||
                    productionPlanLinhaSalva.getRoteiro() == null ||
                    productionPlanLinhaSalva.getListaTecnica() == null ||
                    productionPlanLinhaSalva.getDataReferencia() == null) {
                throw new IllegalStateException(
                        "Saved Production Plan line at index "
                                + indiceProductionPlanLinha
                                + " has an incomplete Community production planning key.");
            }
            indiceProductionPlanLinha++;
        }

    }

    /**
     * Valida o snapshot devolvido pelo repository apos persistir linhas de
     * Distribution Plan.
     */
    private void validaDistributionPlanItemsSalvasCommunity(
            Collection<DistributionPlanItem> distributionPlanItemsSalvas,
            int quantidadeDistributionPlanItemsEsperada) {

        if (distributionPlanItemsSalvas == null) {
            throw new IllegalStateException(
                    "Saved Distribution Plan line snapshot is required after Community distribution planning persistence.");
        }
        if (distributionPlanItemsSalvas.size() != quantidadeDistributionPlanItemsEsperada) {
            throw new IllegalStateException(
                    "Saved Distribution Plan line snapshot size "
                            + distributionPlanItemsSalvas.size()
                            + " differs from expected Community distribution planning persistence size "
                            + quantidadeDistributionPlanItemsEsperada
                            + ".");
        }

        int indiceDistributionPlanItem = 0;
        for (DistributionPlanItem distributionPlanItemSalva : distributionPlanItemsSalvas) {
            if (distributionPlanItemSalva == null) {
                throw new IllegalStateException(
                        "Saved Distribution Plan line at index "
                                + indiceDistributionPlanItem
                                + " is required after Community distribution planning persistence.");
            }
            if (distributionPlanItemSalva.getKey() == null ||
                    distributionPlanItemSalva.getSupplyPlan() == null ||
                    distributionPlanItemSalva.getLocationOrigem() == null ||
                    distributionPlanItemSalva.getLocationDestino() == null ||
                    distributionPlanItemSalva.getProduto() == null ||
                    distributionPlanItemSalva.getDataExpedicao() == null ||
                    distributionPlanItemSalva.getDataRecebimento() == null) {
                throw new IllegalStateException(
                        "Saved Distribution Plan line at index "
                                + indiceDistributionPlanItem
                                + " has an incomplete Community distribution planning key.");
            }
            indiceDistributionPlanItem++;
        }

    }

    /**
     * Valida as linhas de Production Plan que sobreviveram ao filtro de delete.
     *
     * <p>Linhas nao zeradas podem passar por este metodo publico sem tocar o
     * repository. Ja linhas efetivamente deletadas precisam de chave completa,
     * pois o `deleteAll` depende da chave composta materializada para remover a
     * fotografia correta.</p>
     */
    private void validaProductionPlanLinhasParaDeleteCommunity(
            Collection<ProductionPlanLinha> productionPlanLinhasParaDelete) {

        Set<String> chavesProductionPlanLinha = new HashSet<>();
        int indiceProductionPlanLinha = 0;
        for (ProductionPlanLinha productionPlanLinha : productionPlanLinhasParaDelete) {
            if (hasProductionPlanLinhaKeyIncompletaCommunity(productionPlanLinha)) {
                throw new IllegalArgumentException(
                        "Production Plan line at index "
                                + indiceProductionPlanLinha
                                + " has an incomplete Community production planning key for delete.");
            }
            String chaveProductionPlanLinha =
                    getMensagemChaveProductionPlanLinhaCommunity(productionPlanLinha);
            if (!chavesProductionPlanLinha.add(chaveProductionPlanLinha)) {
                throw new IllegalArgumentException(
                        "Production Plan line at index "
                                + indiceProductionPlanLinha
                                + " has duplicated Community production planning key for delete: "
                                + chaveProductionPlanLinha
                                + ".");
            }
            indiceProductionPlanLinha++;
        }

    }

    /**
     * Valida as linhas de Production Plan filtradas antes de chamar o
     * repository.
     *
     * <p>A normalizacao de versoes temporarias para a versao inexistente
     * canonica acontece antes desta validacao. Assim, duas linhas geradas com
     * versoes temporarias diferentes, mas que colapsam para a mesma chave
     * persistida Community, falham antes do `saveAll`.</p>
     */
    private void validaProductionPlanLinhasParaPersistenciaCommunity(
            Collection<ProductionPlanLinha> productionPlanLinhasParaPersistencia) {

        Set<String> chavesProductionPlanLinha = new HashSet<>();
        int indiceProductionPlanLinha = 0;
        for (ProductionPlanLinha productionPlanLinha : productionPlanLinhasParaPersistencia) {
            if (hasProductionPlanLinhaKeyIncompletaCommunity(productionPlanLinha)) {
                throw new IllegalArgumentException(
                        "Production Plan line at index "
                                + indiceProductionPlanLinha
                                + " has an incomplete Community production planning key for persistence.");
            }
            String chaveProductionPlanLinha =
                    getMensagemChaveProductionPlanLinhaCommunity(productionPlanLinha);
            if (!chavesProductionPlanLinha.add(chaveProductionPlanLinha)) {
                throw new IllegalArgumentException(
                        "Production Plan line at index "
                                + indiceProductionPlanLinha
                                + " has duplicated Community production planning key for persistence: "
                                + chaveProductionPlanLinha
                                + ".");
            }
            indiceProductionPlanLinha++;
        }

    }

    private boolean hasProductionPlanLinhaKeyIncompletaCommunity(
            ProductionPlanLinha productionPlanLinha) {

        return productionPlanLinha.getProductionPlanLinhaCompositeKey() == null ||
                productionPlanLinha.getSupplyPlan() == null ||
                productionPlanLinha.getLocation() == null ||
                productionPlanLinha.getMaterialOutput() == null ||
                productionPlanLinha.getVersaoProducao() == null ||
                productionPlanLinha.getRoteiro() == null ||
                productionPlanLinha.getListaTecnica() == null ||
                productionPlanLinha.getDataReferencia() == null;

    }

    private String getMensagemChaveProductionPlanLinhaCommunity(
            ProductionPlanLinha productionPlanLinha) {

        return "supplyPlanId="
                + productionPlanLinha.getSupplyPlan().getId()
                + ", location="
                + productionPlanLinha.getLocation().getId()
                + ", outputMaterial="
                + productionPlanLinha.getMaterialOutput().getId()
                + ", productionVersion="
                + productionPlanLinha.getVersaoProducao().getId()
                + ", routing="
                + productionPlanLinha.getRoteiro().getId()
                + ", billOfMaterials="
                + productionPlanLinha.getListaTecnica().getId()
                + ", referenceDate="
                + productionPlanLinha.getDataReferencia();

    }

    /**
     * Valida as linhas de Distribution Plan que serao removidas do snapshot.
     */
    private void validaDistributionPlanItemsParaDeleteCommunity(
            Collection<DistributionPlanItem> distributionPlanItemsParaDelete) {

        Set<String> chavesDistributionPlanItem = new HashSet<>();
        int indiceDistributionPlanItem = 0;
        for (DistributionPlanItem distributionPlanItem : distributionPlanItemsParaDelete) {
            if (hasDistributionPlanItemKeyIncompletaCommunity(distributionPlanItem)) {
                throw new IllegalArgumentException(
                        "Distribution Plan line at index "
                                + indiceDistributionPlanItem
                                + " has an incomplete Community distribution planning key for delete.");
            }
            String chaveDistributionPlanItem =
                    getMensagemChaveDistributionPlanItemCommunity(distributionPlanItem);
            if (!chavesDistributionPlanItem.add(chaveDistributionPlanItem)) {
                throw new IllegalArgumentException(
                        "Distribution Plan line at index "
                                + indiceDistributionPlanItem
                                + " has duplicated Community distribution planning key for delete: "
                                + chaveDistributionPlanItem
                                + ".");
            }
            indiceDistributionPlanItem++;
        }

    }

    /**
     * Valida as linhas de Distribution Plan filtradas antes de chamar o
     * repository.
     */
    private void validaDistributionPlanItemsParaPersistenciaCommunity(
            Collection<DistributionPlanItem> distributionPlanItemsParaPersistencia) {

        Set<String> chavesDistributionPlanItem = new HashSet<>();
        int indiceDistributionPlanItem = 0;
        for (DistributionPlanItem distributionPlanItem : distributionPlanItemsParaPersistencia) {
            if (hasDistributionPlanItemKeyIncompletaCommunity(distributionPlanItem)) {
                throw new IllegalArgumentException(
                        "Distribution Plan line at index "
                                + indiceDistributionPlanItem
                                + " has an incomplete Community distribution planning key for persistence.");
            }
            String chaveDistributionPlanItem =
                    getMensagemChaveDistributionPlanItemCommunity(distributionPlanItem);
            if (!chavesDistributionPlanItem.add(chaveDistributionPlanItem)) {
                throw new IllegalArgumentException(
                        "Distribution Plan line at index "
                                + indiceDistributionPlanItem
                                + " has duplicated Community distribution planning key for persistence: "
                                + chaveDistributionPlanItem
                                + ".");
            }
            indiceDistributionPlanItem++;
        }

    }

    private boolean hasDistributionPlanItemKeyIncompletaCommunity(
            DistributionPlanItem distributionPlanItem) {

        return distributionPlanItem.getKey() == null ||
                distributionPlanItem.getSupplyPlan() == null ||
                distributionPlanItem.getLocationOrigem() == null ||
                distributionPlanItem.getLocationDestino() == null ||
                distributionPlanItem.getProduto() == null ||
                distributionPlanItem.getDataExpedicao() == null ||
                distributionPlanItem.getDataRecebimento() == null;

    }

    private String getMensagemChaveDistributionPlanItemCommunity(
            DistributionPlanItem distributionPlanItem) {

        return "supplyPlanId="
                + distributionPlanItem.getSupplyPlan().getId()
                + ", origin="
                + distributionPlanItem.getLocationOrigem().getId()
                + ", destination="
                + distributionPlanItem.getLocationDestino().getId()
                + ", material="
                + distributionPlanItem.getProduto().getId()
                + ", expeditionDate="
                + distributionPlanItem.getDataExpedicao()
                + ", receiptDate="
                + distributionPlanItem.getDataRecebimento();

    }

    /**
     * O bean opcional e a fronteira de runtime da capacidade Enterprise: ele
     * insere ordens firmes na projection antes da execucao e precisa que os
     * mesmos campos sobrevivam aos saves e checkpoints compartilhados.
     */
    private boolean deveNeutralizarOrdensFirmesProducaoCommunity() {

        return supplyPlanFirmProductionOrdersSpi == null;

    }

    /**
     * A presenca da extensao Enterprise indica que a projection recebeu
     * transferencias/compras firmes antes do heuristico. Esses campos precisam
     * sobreviver aos mesmos saves e checkpoints que preservam producao firme.
     */
    private boolean deveNeutralizarOrdensFirmesDistribuicaoCommunity() {

        return supplyPlanOpenOrdersHeuristicSpi == null;

    }

    private void neutralizaOrdensFirmesCommunity(ProductionPlanLinha productionPlanLinha) {

        productionPlanLinha.setQuantidadeOrdemFirmeProducaoIrrestrita(0.0);
        productionPlanLinha.setQuantidadeOrdemFirmeProducaoRestrita(0.0);
        productionPlanLinha.setQuantidadeOrdemFirmeProducaoTrabalho(0.0);

    }

    private void neutralizaOrdensFirmesCommunity(DistributionPlanItem distributionPlanItem) {

        distributionPlanItem.setQuantidadeOrdemFirmeIrrestrita(0.0);
        distributionPlanItem.setQuantidadeOrdemFirmeRestrita(0.0);
        distributionPlanItem.setQuantidadeOrdemFirmeTrabalho(0.0);
        distributionPlanItem.setParcelaOrdemFirmeIrrestritaAtendimentoDemandaDireta(0.0);
        distributionPlanItem.setParcelaOrdemFirmeRestritaAtendimentoDemandaDireta(0.0);

    }

    /**
     * Salva distribution plan linhas inbound (retrito e irrestrito) para o período indicado
     * Somente salva se algum dos campos tiver valor não-irrelevante (valor absoluto > 0.000001)
     * @param planningProjection
     * @param posicaoPeriodo
     */
    public void saveDistributionPlanInboundDePlanningProjection(
            SupplyPlanningProjection planningProjection,
            ReferenciaPeriodo referenciaPeriodo,
            int posicaoPeriodo, boolean salvaZeros) {
        saveDistributionPlanItemCollection(
                planningProjection.getDistributionPlanInboundQueue(referenciaPeriodo, posicaoPeriodo),
                salvaZeros);
    }

    /**
     * Salva distribution plan linhas inbound (retrito e irrestrito) para todos os períodos
     * Somente salva se algum dos campos tiver valor não-irrelevante (valor absoluto > 0.000001)
     * @param planningProjection
     */
    public void saveDistributionPlanInboundDePlanningProjection(SupplyPlanningProjection planningProjection, boolean salvaZeros) {
        saveDistributionPlanItemCollection(
                planningProjection.getTodosDistributionPlanItemsInboundSet(),
                salvaZeros);
    }

    /**
     * Salva distribution plan linhas outbound (retrito e irrestrito) para todos os períodos
     * Somente salva se algum dos campos tiver valor não-irrelevante (valor absoluto > 0.000001)
     * @param planningProjection
     */
    public void saveDistributionPlanOutboundDePlanningProjection(SupplyPlanningProjection planningProjection, boolean salvaZeros) {
        saveDistributionPlanItemCollection(
                planningProjection.getTodosDistributionPlanItemsOutboundSet(),
                salvaZeros);
    }

    /**
     * Salva distribution plan linhas outbound (retrito e irrestrito) para o período indicado
     * Somente salva se algum dos campos tiver valor não-irrelevante (valor absoluto > 0.000001)
     * @param planningProjection
     * @param posicaoPeriodo
     */
    public void saveDistributionPlanOutboundDePlanningProjection(SupplyPlanningProjection planningProjection,
            int posicaoPeriodo, boolean salvaZeros) {
        saveDistributionPlanItemCollection(
                planningProjection.getDistributionPlanItemOutboundQueue(ReferenciaPeriodo.CONSUMO_CAPACIDADE, posicaoPeriodo),
                salvaZeros);
    }

    public void saveDemandaDiretaConsideradaLinhaDeProjection(
            SupplyPlanningProjection supplyPlanningProjection,
            boolean salvaZeros) {
        saveDemandaDiretaConsideradaLinhaCollection(
                supplyPlanningProjection
                        .getDemandaDiretaConsideradaProjection()
                        .getAllDemandaDiretaConsideradaLinha(),
                salvaZeros);
    }

    public void saveDemandaDiretaConsideradaLinhaDeProjection(
            DemandaDiretaConsideradaProjection demandaDiretaConsideradaProjection,
            boolean salvaZeros) {
        saveDemandaDiretaConsideradaLinhaCollection(
                demandaDiretaConsideradaProjection.getAllDemandaDiretaConsideradaLinha(),
                salvaZeros);
    }

    public void saveDemandaDiretaConsideradaLinhaDeProjection(
            SupplyPlanningProjection supplyPlanningProjection,
            int periodo,
            boolean salvaZeros) {
        saveDemandaDiretaConsideradaLinhaCollection(
                supplyPlanningProjection
                        .getDemandaDiretaConsideradaProjection()
                        .getDemandaDiretaConsideradaLinha(periodo),
                salvaZeros);
    }

    public void saveDemandaDiretaConsideradaLinhaCollection(
            Collection<DemandaDiretaConsideradaLinha> demandaDiretaConsideradaLinhaCollection,
            boolean salvaZeros) {

        validaDemandaDiretaConsideradaLinhaCollectionCommunity(demandaDiretaConsideradaLinhaCollection);
        /*
         * Carteira, gross, impostos e frete pertencem ao fluxo economico e
         * transacional Enterprise. O Community persiste somente quantidades de
         * Demand Plan e safety stock; portanto a neutralizacao ocorre antes do
         * filtro para que linhas que existiam apenas por carteira/valor nao
         * sejam gravadas no snapshot aberto.
         */
        demandaDiretaConsideradaLinhaCollection.forEach(
                this::neutralizaCarteiraEValoresDemandaDiretaCommunity);

        List<DemandaDiretaConsideradaLinha> demandaDiretaConsideradaLinhasASalvar = demandaDiretaConsideradaLinhaCollection.stream()
                .filter(x -> salvaZeros ||
                        Math.abs(x.getQuantidadePlanoDemandaOriginal()) > 0.00001 ||
                        Math.abs(x.getQuantidadePlanoDemandaOriginalPropagadaLocationInterna()) > 0.00001 ||
                        Math.abs(x.getQuantidadeDemandaDiretaIrrestrita()) > 0.00001 ||
                        Math.abs(x.getQuantidadeDemandaDiretaRestrita()) > 0.00001 ||
                        Math.abs(x.getQuantidadeDemandaDiretaPlanoDemandaIrrestrita()) > 0.00001 ||
                        Math.abs(x.getQuantidadeDemandaDiretaPlanoDemandaRestrita()) > 0.00001 ||
                        Math.abs(x.getQuantidadeDemandaDiretaEstoqueSeguranca()) > 0.00001)
                .toList();
        if (demandaDiretaConsideradaLinhasASalvar.isEmpty()) {
            return;
        }
        validaDemandaDiretaConsideradaLinhasParaBatchCommunity(demandaDiretaConsideradaLinhasASalvar);
        demandaDiretaConsideradaLinhaDAO.saveInBatch(demandaDiretaConsideradaLinhasASalvar);

    }

    /**
     * Valida a colecao de demanda direta antes de filtrar/persistir.
     *
     * <p>Colecao vazia e snapshot valido e deve apenas encerrar sem round-trip
     * ao banco. Colecao nula ou item nulo indica quebra da projection anterior
     * e deve falhar antes de stream ou DAO gerarem NPE sem contexto.</p>
     */
    private void validaDemandaDiretaConsideradaLinhaCollectionCommunity(
            Collection<DemandaDiretaConsideradaLinha> demandaDiretaConsideradaLinhaCollection) {

        if (demandaDiretaConsideradaLinhaCollection == null) {
            throw new IllegalArgumentException(
                    "Direct demand considered line collection is required for Community Supply Planning persistence.");
        }

        int indiceDemandaDiretaConsideradaLinha = 0;
        for (DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinha : demandaDiretaConsideradaLinhaCollection) {
            if (demandaDiretaConsideradaLinha == null) {
                throw new IllegalArgumentException(
                        "Direct demand considered line at index "
                                + indiceDemandaDiretaConsideradaLinha
                                + " is required for Community Supply Planning persistence.");
            }
            indiceDemandaDiretaConsideradaLinha++;
        }

    }

    /**
     * Valida as linhas que realmente serao enviadas ao DAO JDBC.
     *
     * <p>A validacao de chave ocorre depois da neutralizacao/filtro porque uma
     * linha recebida apenas com componentes Enterprise pode ser descartada pelo
     * Community sem precisar ser materializada. Se a linha sobreviveu ao filtro,
     * porem, supply plan, location, material e periodo passam a ser obrigatorios
     * para o upsert batch.</p>
     */
    private void validaDemandaDiretaConsideradaLinhasParaBatchCommunity(
            Collection<DemandaDiretaConsideradaLinha> demandaDiretaConsideradaLinhasParaBatch) {

        Set<String> chavesDemandaDiretaConsideradaLinha = new HashSet<>();
        int indiceDemandaDiretaConsideradaLinha = 0;
        for (DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinha : demandaDiretaConsideradaLinhasParaBatch) {
            if (hasDemandaDiretaConsideradaLinhaKeyIncompletaCommunity(demandaDiretaConsideradaLinha)) {
                throw new IllegalArgumentException(
                        "Direct demand considered line at index "
                                + indiceDemandaDiretaConsideradaLinha
                                + " has an incomplete Community Supply Planning key.");
            }
            String chaveDemandaDiretaConsideradaLinha =
                    getMensagemChaveDemandaDiretaConsideradaLinhaCommunity(demandaDiretaConsideradaLinha);
            if (!chavesDemandaDiretaConsideradaLinha.add(chaveDemandaDiretaConsideradaLinha)) {
                throw new IllegalArgumentException(
                        "Direct demand considered line at index "
                                + indiceDemandaDiretaConsideradaLinha
                                + " has duplicated Community Supply Planning key: "
                                + chaveDemandaDiretaConsideradaLinha
                                + ".");
            }
            indiceDemandaDiretaConsideradaLinha++;
        }

    }

    private boolean hasDemandaDiretaConsideradaLinhaKeyIncompletaCommunity(
            DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinha) {

        return demandaDiretaConsideradaLinha.getDemandaDiretaConsideradaLinhaCompositeKey() == null ||
                demandaDiretaConsideradaLinha.getSupplyPlan() == null ||
                demandaDiretaConsideradaLinha.getLocation() == null ||
                demandaDiretaConsideradaLinha.getMaterial() == null ||
                demandaDiretaConsideradaLinha.getDataReferencia() == null;

    }

    private String getMensagemChaveDemandaDiretaConsideradaLinhaCommunity(
            DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinha) {

        return "supplyPlanId="
                + demandaDiretaConsideradaLinha.getSupplyPlan().getId()
                + ", location="
                + demandaDiretaConsideradaLinha.getLocation().getId()
                + ", material="
                + demandaDiretaConsideradaLinha.getMaterial().getId()
                + ", referenceDate="
                + demandaDiretaConsideradaLinha.getDataReferencia();

    }

    /**
     * Remove componentes de carteira e valores antes do save Community.
     *
     * <p>As colunas permanecem na entidade compartilhada por compatibilidade de
     * schema e pelo overlay Enterprise, mas a edicao aberta nao pode persistir
     * carteira, gross, impostos ou frete. Mantemos apenas quantidades vindas do
     * Demand Plan e a quantidade de safety stock operacional.</p>
     */
    private void neutralizaCarteiraEValoresDemandaDiretaCommunity(
            DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinha) {

        demandaDiretaConsideradaLinha.setQuantidadeCarteiraOriginal(0.0);
        demandaDiretaConsideradaLinha.setQuantidadeCarteiraOriginalPropagadaLocationInterna(0.0);

        demandaDiretaConsideradaLinha.setGrossSalesPlanoDemandaOriginal(0.0);
        demandaDiretaConsideradaLinha.setCustoFretePlanoDemandaOriginal(0.0);
        demandaDiretaConsideradaLinha.setCustoImpostosPlanoDemandaOriginal(0.0);
        demandaDiretaConsideradaLinha.setGrossSalesCarteiraOriginal(0.0);
        demandaDiretaConsideradaLinha.setCustoFreteCarteiraOriginal(0.0);
        demandaDiretaConsideradaLinha.setCustoImpostosCarteiraOriginal(0.0);

        demandaDiretaConsideradaLinha.setGrossSalesPlanoDemandaOriginalPropagadaLocationInterna(0.0);
        demandaDiretaConsideradaLinha.setCustoFretePlanoDemandaOriginalPropagadaLocationInterna(0.0);
        demandaDiretaConsideradaLinha.setCustoImpostosPlanoDemandaOriginalPropagadaLocationInterna(0.0);
        demandaDiretaConsideradaLinha.setGrossSalesCarteiraOriginalPropagadaLocationInterna(0.0);
        demandaDiretaConsideradaLinha.setCustoFreteCarteiraOriginalPropagadaLocationInterna(0.0);
        demandaDiretaConsideradaLinha.setCustoImpostosCarteiraOriginalPropagadaLocationInterna(0.0);

        demandaDiretaConsideradaLinha.setGrossSalesDemandaDiretaPlanoDemandaIrrestrita(0.0);
        demandaDiretaConsideradaLinha.setCustoFreteDemandaDiretaPlanoDemandaIrrestrita(0.0);
        demandaDiretaConsideradaLinha.setCustoImpostosDemandaDiretaPlanoDemandaIrrestrita(0.0);
        demandaDiretaConsideradaLinha.setQuantidadeDemandaDiretaCarteiraIrrestrita(0.0);
        demandaDiretaConsideradaLinha.setGrossSalesDemandaDiretaCarteiraIrrestrita(0.0);
        demandaDiretaConsideradaLinha.setCustoFreteDemandaDiretaCarteiraIrrestrita(0.0);
        demandaDiretaConsideradaLinha.setCustoImpostosDemandaDiretaCarteiraIrrestrita(0.0);

        demandaDiretaConsideradaLinha.setGrossSalesDemandaDiretaPlanoDemandaRestrita(0.0);
        demandaDiretaConsideradaLinha.setCustoFreteDemandaDiretaPlanoDemandaRestrita(0.0);
        demandaDiretaConsideradaLinha.setCustoImpostosDemandaDiretaPlanoDemandaRestrita(0.0);
        demandaDiretaConsideradaLinha.setQuantidadeDemandaDiretaCarteiraRestrita(0.0);
        demandaDiretaConsideradaLinha.setGrossSalesDemandaDiretaCarteiraRestrita(0.0);
        demandaDiretaConsideradaLinha.setCustoFreteDemandaDiretaCarteiraRestrita(0.0);
        demandaDiretaConsideradaLinha.setCustoImpostosDemandaDiretaCarteiraRestrita(0.0);

    }

    public void saveInventoryPlanLinhaCollection(
            Collection<InventoryPlanLinha> inventoryPlanLinhaCollection,
            boolean salvaZeros) {

        validaInventoryPlanLinhaCollectionCommunity(inventoryPlanLinhaCollection);

        List<InventoryPlanLinha> inventoryPlanLinhas = inventoryPlanLinhaCollection.stream()
                .filter(x -> salvaZeros ||
                        Math.abs(x.getQuantidadeEstoqueProjetadoTrabalho()) > 0.00001 ||
                        Math.abs(x.getQuantidadeEstoqueProjetadoIrrestrito()) > 0.00001 ||
                        Math.abs(x.getQuantidadeEstoqueProjetadoRestrito()) > 0.00001 ||
                        Math.abs(x.getQuantidadeEstoqueTransitoInbound()) > 0.00001 ||
                        Math.abs(x.getQuantidadeEstoqueSegurancaIrrestrito()) > 0.00001 ||
                        Math.abs(x.getQuantidadeEstoqueSegurancaRestrito()) > 0.00001)
                .toList();
        if (inventoryPlanLinhas.isEmpty()) return;

        validaInventoryPlanLinhasParaPersistenciaCommunity(inventoryPlanLinhas);
        List<InventoryPlanLinha> inventoryPlanLinhasSalvas =
                inventoryPlanLinhaRepository.saveAll(inventoryPlanLinhas);
        validaInventoryPlanLinhasSalvasCommunity(
                inventoryPlanLinhasSalvas,
                inventoryPlanLinhas.size());
        inventoryPlanLinhaRepository.flush();

    }

    public void removeInventoryPlanLinhaZeradosCollection(
            Collection<InventoryPlanLinha> inventoryPlanLinhaCollection) {

        validaInventoryPlanLinhaCollectionCommunity(inventoryPlanLinhaCollection);

        List<InventoryPlanLinha> inventoryPlanLinhasZerados = inventoryPlanLinhaCollection.stream()
                .filter(x ->
                        Math.abs(x.getQuantidadeEstoqueProjetadoTrabalho()) <= 0.00001 &&
                        Math.abs(x.getQuantidadeEstoqueProjetadoIrrestrito()) <= 0.00001 &&
                        Math.abs(x.getQuantidadeEstoqueProjetadoRestrito()) <= 0.00001 &&
                        Math.abs(x.getQuantidadeEstoqueTransitoInbound()) <= 0.00001 &&
                        Math.abs(x.getQuantidadeEstoqueSegurancaIrrestrito()) <= 0.00001 &&
                        Math.abs(x.getQuantidadeEstoqueSegurancaRestrito()) <= 0.00001)
                .toList();

        if (inventoryPlanLinhasZerados.isEmpty()) return;

        validaInventoryPlanLinhasParaDeleteCommunity(inventoryPlanLinhasZerados);
        inventoryPlanLinhaRepository.deleteAll(inventoryPlanLinhasZerados);
        inventoryPlanLinhaRepository.flush();

    }

    public void saveEstoqueEmTransitoDeInventoryPlanLinhaCollection(
            Collection<InventoryPlanLinha> inventoryPlanLinhaCollection) {

        validaInventoryPlanLinhaCollectionCommunity(inventoryPlanLinhaCollection);

        List<InventoryPlanLinha> inventoryPlanLinhas = inventoryPlanLinhaCollection.stream()
                .filter(x -> Math.abs(x.getQuantidadeEstoqueTransitoInbound()) > 0.00001)
                .toList();
        if (inventoryPlanLinhas.isEmpty()) return;
        validaInventoryPlanLinhasParaPersistenciaCommunity(inventoryPlanLinhas);
        List<InventoryPlanLinha> inventoryPlanLinhasSalvas =
                inventoryPlanLinhaRepository.saveAll(inventoryPlanLinhas);
        validaInventoryPlanLinhasSalvasCommunity(
                inventoryPlanLinhasSalvas,
                inventoryPlanLinhas.size());

    }

    /**
     * Valida linhas de Inventory Plan antes de save/delete/stream.
     *
     * <p>Inventario e safety stock sao parte do Community, mas a entidade
     * tambem carrega campos transicionais usados por fluxos Enterprise de
     * estoque em transito e aging. Colecao vazia e no-op valido; colecao nula
     * ou item nulo sinalizam snapshot quebrado e devem falhar antes do
     * repository.</p>
     */
    private void validaInventoryPlanLinhaCollectionCommunity(
            Collection<InventoryPlanLinha> inventoryPlanLinhaCollection) {

        if (inventoryPlanLinhaCollection == null) {
            throw new IllegalArgumentException(
                    "Inventory Plan line collection is required for Community inventory planning persistence.");
        }

        int indiceInventoryPlanLinha = 0;
        for (InventoryPlanLinha inventoryPlanLinha : inventoryPlanLinhaCollection) {
            if (inventoryPlanLinha == null) {
                throw new IllegalArgumentException(
                        "Inventory Plan line at index "
                                + indiceInventoryPlanLinha
                                + " is required for Community inventory planning persistence.");
            }
            indiceInventoryPlanLinha++;
        }

    }

    /**
     * Valida as linhas de Inventory Plan que serao removidas do snapshot.
     */
    private void validaInventoryPlanLinhasParaDeleteCommunity(
            Collection<InventoryPlanLinha> inventoryPlanLinhasParaDelete) {

        Set<String> chavesInventoryPlanLinha = new HashSet<>();
        int indiceInventoryPlanLinha = 0;
        for (InventoryPlanLinha inventoryPlanLinha : inventoryPlanLinhasParaDelete) {
            if (hasInventoryPlanLinhaKeyIncompletaCommunity(inventoryPlanLinha)) {
                throw new IllegalArgumentException(
                        "Inventory Plan line at index "
                                + indiceInventoryPlanLinha
                                + " has an incomplete Community inventory planning key for delete.");
            }
            String chaveInventoryPlanLinha =
                    getMensagemChaveInventoryPlanLinhaCommunity(inventoryPlanLinha);
            if (!chavesInventoryPlanLinha.add(chaveInventoryPlanLinha)) {
                throw new IllegalArgumentException(
                        "Inventory Plan line at index "
                                + indiceInventoryPlanLinha
                                + " has duplicated Community inventory planning key for delete: "
                                + chaveInventoryPlanLinha
                                + ".");
            }
            indiceInventoryPlanLinha++;
        }

    }

    /**
     * Valida as linhas de Inventory Plan filtradas antes de chamar o repository.
     *
     * <p>A lista filtrada deve preservar a cardinalidade produzida pelo
     * planning book/heuristico ate a borda de persistencia. Duplicidade de
     * chave neste ponto indica snapshot inconsistente e nao pode ser escondida
     * por `Set` antes do `saveAll`.</p>
     */
    private void validaInventoryPlanLinhasParaPersistenciaCommunity(
            Collection<InventoryPlanLinha> inventoryPlanLinhasParaPersistencia) {

        Set<String> chavesInventoryPlanLinha = new HashSet<>();
        int indiceInventoryPlanLinha = 0;
        for (InventoryPlanLinha inventoryPlanLinha : inventoryPlanLinhasParaPersistencia) {
            if (hasInventoryPlanLinhaKeyIncompletaCommunity(inventoryPlanLinha)) {
                throw new IllegalArgumentException(
                        "Inventory Plan line at index "
                                + indiceInventoryPlanLinha
                                + " has an incomplete Community inventory planning key for persistence.");
            }
            String chaveInventoryPlanLinha =
                    getMensagemChaveInventoryPlanLinhaCommunity(inventoryPlanLinha);
            if (!chavesInventoryPlanLinha.add(chaveInventoryPlanLinha)) {
                throw new IllegalArgumentException(
                        "Inventory Plan line at index "
                                + indiceInventoryPlanLinha
                                + " has duplicated Community inventory planning key for persistence: "
                                + chaveInventoryPlanLinha
                                + ".");
            }
            indiceInventoryPlanLinha++;
        }

    }

    private boolean hasInventoryPlanLinhaKeyIncompletaCommunity(
            InventoryPlanLinha inventoryPlanLinha) {

        return inventoryPlanLinha.getInventoryPlanLinhaCompositeKey() == null ||
                inventoryPlanLinha.getSupplyPlan() == null ||
                inventoryPlanLinha.getLocation() == null ||
                inventoryPlanLinha.getProduto() == null ||
                inventoryPlanLinha.getDataReferencia() == null;

    }

    private String getMensagemChaveInventoryPlanLinhaCommunity(
            InventoryPlanLinha inventoryPlanLinha) {

        return "supplyPlanId="
                + inventoryPlanLinha.getSupplyPlan().getId()
                + ", location="
                + inventoryPlanLinha.getLocation().getId()
                + ", material="
                + inventoryPlanLinha.getProduto().getId()
                + ", referenceDate="
                + inventoryPlanLinha.getDataReferencia();

    }

    /**
     * Valida o snapshot devolvido pelo repository apos persistir linhas de
     * Inventory Plan.
     *
     * <p>O Inventory Plan Community e usado diretamente pelo Planning Book e
     * pelo plano restrito. Uma chave salva incompleta indicaria falha de
     * montagem do snapshot material/location/periodo, portanto o erro deve ser
     * explicito e imediato.</p>
     */
    private void validaInventoryPlanLinhasSalvasCommunity(
            Collection<InventoryPlanLinha> inventoryPlanLinhasSalvas,
            int quantidadeInventoryPlanLinhasEsperada) {

        if (inventoryPlanLinhasSalvas == null) {
            throw new IllegalStateException(
                    "Saved Inventory Plan line snapshot is required after Community inventory planning persistence.");
        }
        if (inventoryPlanLinhasSalvas.size() != quantidadeInventoryPlanLinhasEsperada) {
            throw new IllegalStateException(
                    "Saved Inventory Plan line snapshot size "
                            + inventoryPlanLinhasSalvas.size()
                            + " differs from expected Community inventory planning persistence size "
                            + quantidadeInventoryPlanLinhasEsperada
                            + ".");
        }

        int indiceInventoryPlanLinha = 0;
        for (InventoryPlanLinha inventoryPlanLinhaSalva : inventoryPlanLinhasSalvas) {
            if (inventoryPlanLinhaSalva == null) {
                throw new IllegalStateException(
                        "Saved Inventory Plan line at index "
                                + indiceInventoryPlanLinha
                                + " is required after Community inventory planning persistence.");
            }
            if (inventoryPlanLinhaSalva.getInventoryPlanLinhaCompositeKey() == null ||
                    inventoryPlanLinhaSalva.getSupplyPlan() == null ||
                    inventoryPlanLinhaSalva.getLocation() == null ||
                    inventoryPlanLinhaSalva.getProduto() == null ||
                    inventoryPlanLinhaSalva.getDataReferencia() == null) {
                throw new IllegalStateException(
                        "Saved Inventory Plan line at index "
                                + indiceInventoryPlanLinha
                                + " has an incomplete Community inventory planning key.");
            }
            indiceInventoryPlanLinha++;
        }

    }

    public void saveEstoqueEmTransitoDePlanningProjection(SupplyPlanningProjection supplyPlanningProjection) {
        saveEstoqueEmTransitoDeInventoryPlanLinhaCollection(
                supplyPlanningProjection.getTodosInventoryPlanLinhas());
    }

    /**
     * Salva inventory plan linhas (retrito e irrestrito) para o período indicado
     * Somente salva se algum dos campos tiver valor não-irrelevante (valor absoluto > 0.000001)
     * @param supplyPlanningProjection
     */
    public void saveInventoryPlanDePlanningProjection(SupplyPlanningProjection supplyPlanningProjection, boolean salvaZeros) {
        saveInventoryPlanLinhaCollection(
                supplyPlanningProjection.getTodosInventoryPlanLinhas(),
                salvaZeros);
    }

    public void saveEstoqueInicialETransitoDePlanningProjection(SupplyPlanningProjection supplyPlanningProjection, boolean salvaZeros) {
        List<InventoryPlanLinha> inventoryPlanLinhasASalvar = new ArrayList<>();

        inventoryPlanLinhasASalvar.addAll(supplyPlanningProjection.getInventoryPlanLinhaPlanLinha(-1));

        List<InventoryPlanLinha> inventoryPlanLinhasComEstoqueTransito = supplyPlanningProjection.getTodosInventoryPlanLinhas().stream()
                .filter(x -> Math.abs(x.getQuantidadeEstoqueTransitoInbound()) > 0.00001)
                .toList();

        inventoryPlanLinhasASalvar.addAll(inventoryPlanLinhasComEstoqueTransito);

        saveInventoryPlanLinhaCollection(inventoryPlanLinhasASalvar, salvaZeros);
    }

    /**
     * Salva inventory plan linhas (retrito e irrestrito) para o período indicado
     * Somente salva se algum dos campos tiver valor não-irrelevante (valor absoluto > 0.000001)
     * @param supplyPlanningProjection
     */
    public void saveInventoryPlanDePlanningProjection(SupplyPlanningProjection supplyPlanningProjection,
            int posicaoPeriodo, boolean salvaZeros) {
        saveInventoryPlanLinhaCollection(
                supplyPlanningProjection.getInventoryPlanLinhaPlanLinha(posicaoPeriodo),
                salvaZeros);
    }

    public SupplyPlan getSupplyPlanDeId(Long id) {

        if (id == null) {
            throw new NoResultException("Empty Supply Plan Id");
        }

        Optional<SupplyPlan> supplyPlanOptional = supplyPlanRepository.customFindById(id);
        if (supplyPlanOptional == null) {
            throw new IllegalStateException(
                    "Supply Plan repository returned null Optional while loading Supply Plan id "
                            + id
                            + ".");
        }

        return supplyPlanOptional.orElseThrow(() -> new NoResultException("Supply Plan " + id + " not found"));

    }


    public boolean verificaSeHaDistributionPlanItemInbound(SupplyPlan supplyPlan, Location locationDestino) {
        return distributionPlanItemRepository.existsByKeySupplyPlanAndKeyLocationDestino(supplyPlan, locationDestino);
    }
    public boolean verificaSeHaDistributionPlanItemOutbound(SupplyPlan supplyPlan, Location locationOrigem) {
        return distributionPlanItemRepository.existsByKeySupplyPlanAndKeyLocationOrigem(supplyPlan, locationOrigem);
    }
    public boolean verificaSeHaProductionPlanLinha(SupplyPlan supplyPlan, Location location) {
        return productionPlanLinhaRepository.existsByProductionPlanLinhaCompositeKeySupplyPlanAndProductionPlanLinhaCompositeKeyLocation(supplyPlan, location);
    }

    public EstoqueProjectionLocationProduto getEstoqueProjectionProdutoSemProjecao(
            LocalDateTime  dataHorarioReferenciaEstoque,
            LocationProjection locationProjection,
            MaterialProjection materialProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        ParametrosGlobais parametrosGlobais =
                clusterEParametrosProjection.getParametrosGlobais();
        UnidadeMedida unidadeMedidaPadraoSnp =
                parametrosGlobais.getUnidadeMedidaPadraoSNP();

        return estoqueProjectionFactory.getEstoqueProjectionLocationProduto(
                dataHorarioReferenciaEstoque,
                locationProjection.getLocationsAtivas(),
                materialProjection.getMateriaisAtivos(),
                unidadeMedidaProjection,
                clusterEParametrosProjection,
                unidadeMedidaPadraoSnp);

    }

    public EstoqueProjectionProduto getEstoqueProjectionProdutoProjetadoAPartirUltimoEstoqueCadastradoESupplyPlanPassado(
            LocalDateTime  dataHorarioEstoqueAProjetar,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            Location location,
            MaterialProjection materialProjection,
            @Nullable SupplyPlan supplyPlanParaProjecaoEstoqueInicial,
            SupplyNetworkProjection supplyNetworkProjection,
            UnidadeMedidaProjection unidadeMedidaProjection,
            PoliticaEstoquesProjection politicaEstoquesProjection) {

        if (supplyNetworkProjection == null) {
            throw new IllegalArgumentException(
                    "Supply Network projection is required for projected initial stock projection.");
        }
        ClusterEParametrosProjection clusterEParametrosProjection = supplyNetworkProjection.getClusterEParametrosProjection();
        ParametrosGlobais parametrosGlobais =
                clusterEParametrosProjection.getParametrosGlobais();
        UnidadeMedida unidadeMedidaSnp = parametrosGlobais.getUnidadeMedidaPadraoSNP();

        LocationProjection locationProjectionCompleto = LocationProjectionFactory.getLocationProjectionCompleto(clusterEParametrosProjection);
        Set<Produto> materiaisAtivosNaLocation = materialProjection.getMateriaisAtivosEmLocation(location);
        MaterialProjection materialProjectionNaLocationAtual = MaterialProjectionFactory.getProjectionSetMateriais(materiaisAtivosNaLocation, clusterEParametrosProjection);

        LocalDateTime ultimaDataHorarioComEstoqueAntesReferencia = estoqueRepository
                .getUltimaDataHorarioIgualOuMenorDataHorarioReferencia(dataHorarioEstoqueAProjetar)
                .orElse(dataHorarioEstoqueAProjetar);

        // Caso o último estoque carregado seja anterior ao da data onde seria necessário, usar projeção do estoque ao invés do estoque zerado
        if (ultimaDataHorarioComEstoqueAntesReferencia.isBefore(dataHorarioEstoqueAProjetar) &&
                supplyPlanParaProjecaoEstoqueInicial != null) {

            log.info("Calculating starting inventory for location " + location.getId() + " at period " + dataHorarioEstoqueAProjetar.toString() + " estimated from "
                    + "inventory at period " + ultimaDataHorarioComEstoqueAntesReferencia.toString() + " and supply plan " + supplyPlanParaProjecaoEstoqueInicial.getId());

            Calendario calendarioSupplyPlanParaProjecaoEstoqueInicial = Calendario.criaCalendarioDeDatas(
                            supplyPlanParaProjecaoEstoqueInicial.getTamanhoBucket(),
                            ultimaDataHorarioComEstoqueAntesReferencia, dataHorarioEstoqueAProjetar, dataHorarioEstoqueAProjetar);

            /*
             * Community ja usa Demand Plan como unica demanda direta futura no
             * proprio contrato do perfil. No Enterprise, este ponto podera usar
             * uma copia ajustada do perfil para isolar a projecao de estoque
             * inicial de carteira/orders.
             */
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlanProjectionParaProjecaoEstoqueInicial = perfilExecucaoSupplyPlan;

            SupplyPlanningProjection supplyPlanningProjectionParaProjecaoEstoqueInicial = supplyPlanProjectionFactory.getSupplyPlanningProjectionVazio(
                    supplyPlanParaProjecaoEstoqueInicial,
                    // o calendário abaixo filtra apenas o horizonte de tempo necessário para a projeção do estoque inicial, evitando extração de dados que não serão usados
                    calendarioSupplyPlanParaProjecaoEstoqueInicial,
                    perfilExecucaoSupplyPlanProjectionParaProjecaoEstoqueInicial,
                    location,
                    supplyNetworkProjection,
                    politicaEstoquesProjection,
                    materialProjectionNaLocationAtual,
                    locationProjectionCompleto);
            supplyPlanProjectionFactory.populaSupplyPlanningProjectionComDistributionPlanInbound(supplyPlanningProjectionParaProjecaoEstoqueInicial);
            supplyPlanProjectionFactory.populaSupplyPlanningProjectionComDistributionPlanOutbound(supplyPlanningProjectionParaProjecaoEstoqueInicial);
            supplyPlanProjectionFactory.populaSupplyPlanningProjectionComProductionPlanOutput(supplyPlanningProjectionParaProjecaoEstoqueInicial);
            supplyPlanProjectionFactory.populaSupplyPlanningProjectionComProductionPlanInput(supplyPlanningProjectionParaProjecaoEstoqueInicial);
            supplyPlanProjectionFactory.populaSupplyPlanningProjectionComDemandaDiretaConsideradaProjection(supplyPlanningProjectionParaProjecaoEstoqueInicial);

            /*
             * A projecao inicial de estoque reaproveita DemandPlanningProjection
             * para reconstruir a demanda futura do plano passado. No Community,
             * essa projection ja esta limitada a Demand Plan e nao aplica
             * catch-up por historico de vendas passado.
             */
            DemandPlanningProjection demandPlanningProjectionParaProjecaoEstoqueInicial = supplyPlanDemandCatchUpProjectionSpi
                    .getDemandPlanningProjectionCompletaParaEstoqueInicial(
                    perfilExecucaoSupplyPlanProjectionParaProjecaoEstoqueInicial,
                    supplyPlanParaProjecaoEstoqueInicial.getDemandPlan(),
                    location,
                    materiaisAtivosNaLocation);

            // Bucket / calendario calculo Pre-Estoque. Possuem granularidade equivalente ou maior ao bucket / calendario de supplyPlanParaProjecaoEstoqueInicial
            TamanhoBucket tamanhoBucketConsideradoProjecaoPreEstoque = getTamanhoBucketConsideradoParaProjecaoEstoqueInicialAPartirPreEstoque(
                    calendarioSupplyPlanParaProjecaoEstoqueInicial.getTamanhoBucket());
            Calendario calendarioParaProjecaoPreEstoque = Calendario.criaCalendarioDeDatas(
                    tamanhoBucketConsideradoProjecaoPreEstoque,
                    ultimaDataHorarioComEstoqueAntesReferencia, dataHorarioEstoqueAProjetar, dataHorarioEstoqueAProjetar);

            // Split temporal Calendario Supply usado para projecao inicial estoque -> Calendario Pre-Estoque
            SplitTemporalProjectionPorDfu splitTemporalProjectionPorDfuCalendarioOrigemParaCalendarioTarget = splitTemporalProjectionFactory.geraSplitTemporalProjectionPorDfu(
                    calendarioSupplyPlanParaProjecaoEstoqueInicial,
                    calendarioParaProjecaoPreEstoque);

            // Estoque em D-N (ultimo estoque disponivel na base)
            EstoqueProjectionProduto estoqueProjectionProdutoUltimoEstoqueDisponivel = estoqueProjectionFactory.getEstoqueProjectionProduto(
                    ultimaDataHorarioComEstoqueAntesReferencia,
                    location, materiaisAtivosNaLocation,
                    unidadeMedidaProjection, clusterEParametrosProjection,
                    unidadeMedidaSnp);

            // Estoque projetado para D0
            EstoqueProjectionProduto estoqueProjectionProdutoEstoqueInicialProjetado = EstoqueProjectionProduto.builder()
                    .calendario(calendarioParaProjecaoPreEstoque)
                    .clusterEParametrosProjection(clusterEParametrosProjection)
                    .conversaoUnidadeMedidaProjection(unidadeMedidaProjection)
                    .build();

            for (Produto material : materialProjection.getMateriaisAtivos()) {

                UnidadeMedida unidadeMedidaPadraoMaterial = clusterEParametrosProjection.getSNPUnidadeMedidaPadrao(material, location);

                double posicaoEstoqueInicial = SupplyPlanning.getEstoqueProjetadoDeSupplyPlanningProjectionCalendarioOrigemParaPeriodoCalendarioTarget(
                        // calendario com bucket size = supply plan antigo usado para projeção estoque inicial
                        supplyPlanningProjectionParaProjecaoEstoqueInicial, // ex : em dias
                        // estoque no período D-N, que será usado para projetar o D0
                        estoqueProjectionProdutoUltimoEstoqueDisponivel,
                        splitTemporalProjectionPorDfuCalendarioOrigemParaCalendarioTarget, // ex : split dia -> hora (calendario mais granular que o do supply plan a ser executado)
                        0,
                        calendarioParaProjecaoPreEstoque.getPosicaoPeriodoFinalPassado(), // ex : hora 0 -> hora 48
                        material,
                        Constantes.TipoDemanda.TOTAL,
                        Constantes.TipoPlano.PLANO_IRRESTRITO,
                        unidadeMedidaPadraoMaterial,
                        true, true, false, true);

                estoqueProjectionProdutoEstoqueInicialProjetado.addEstoque(
                        AggregatedByMaterialUOMImpl.builder()
                                .material(material)
                                .uom(unidadeMedidaPadraoMaterial)
                                .totalQuantity((double) posicaoEstoqueInicial)
                                .build());

            }

            return estoqueProjectionProdutoEstoqueInicialProjetado;

        // Usa a própria data inicial do calendario supply plan para extração dos estoques
        } else {
            return estoqueProjectionFactory.getEstoqueProjectionProduto(
                    dataHorarioEstoqueAProjetar, location, materiaisAtivosNaLocation,
                    unidadeMedidaProjection, clusterEParametrosProjection,
                    unidadeMedidaSnp);
        }

    }

    private TamanhoBucket getTamanhoBucketConsideradoParaProjecaoEstoqueInicialAPartirPreEstoque(TamanhoBucket tamanhoBucketSupplyPlan) {

        if (tamanhoBucketSupplyPlan == null) {
            throw getUnsupportedTamanhoBucketProjecaoEstoqueInicialException(null);
        }

        switch (tamanhoBucketSupplyPlan) {
            case MENSAL : return TamanhoBucket.DIARIO;
            case SEMANAL : return TamanhoBucket.TURNO;
            case DIARIO : return TamanhoBucket.HORARIO;
            case TURNO : return TamanhoBucket.HORARIO;
            case HORARIO : return TamanhoBucket.MINUTO;
            case MEIA_HORA : return TamanhoBucket.MINUTO;
            case QUARTO_HORA : return TamanhoBucket.MINUTO;
            case SEXTO_HORA : return TamanhoBucket.MINUTO;
            case MINUTO : return TamanhoBucket.SEGUNDO;
            case SEGUNDO : return TamanhoBucket.SEGUNDO;
            default : throw getUnsupportedTamanhoBucketProjecaoEstoqueInicialException(tamanhoBucketSupplyPlan);
        }

    }

    /**
     * Mantem explicita a regra de granularidade usada quando o Community precisa
     * projetar estoque inicial a partir de pre-estoque.
     */
    private IllegalArgumentException getUnsupportedTamanhoBucketProjecaoEstoqueInicialException(
            TamanhoBucket tamanhoBucketSupplyPlan) {

        return new IllegalArgumentException(
                "SupplyPlanService cannot derive an initial-stock projection bucket for Supply Plan bucket "
                        + (tamanhoBucketSupplyPlan == null ? "null" : tamanhoBucketSupplyPlan.name())
                        + ". Configure a supported Supply Plan calendar bucket before stock projection.");

    }

    /**
     * Cria e persiste a demanda direta considerada do Community a partir do
     * Demand Plan.
     *
     * <p>Quando a location do Demand Plan deve apenas propagar demanda, o
     * volume e deslocado para a origem interna/regiao comercial viavel,
     * respeitando o lead time da malha. Quando nao ha propagacao, a propria
     * location do Demand Plan recebe a demanda considerada.</p>
     *
     * <p>Carteira, sell-in e sales orders pertencem ao Enterprise e nao entram
     * neste metodo.</p>
     */
    public void atualizaSupplyPlanComDemandaDiretaConsiderada(
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            DemandPlanningProjection demandPlanningProjection,
            SplitTemporalProjectionPorDfu splitTemporalProjectionPorDfu,
            PoliticaEstoquesProjection politicaEstoquesProjection, // usado para tratar modelo de majoração considerando MTS/MTO (cadastro da política)
            SupplyNetworkProjection supplyNetworkProjection,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        ClusterEParametrosProjection clusterEParametrosProjection =
                validaEntradasDemandaDiretaConsideradaCommunity(
                        supplyPlan,
                        perfilExecucaoSupplyPlan,
                        demandPlanningProjection,
                        splitTemporalProjectionPorDfu,
                        politicaEstoquesProjection,
                        supplyNetworkProjection,
                        unidadeMedidaProjection);
        ParametrosGlobais parametrosGlobais =
                clusterEParametrosProjection.getParametrosGlobais();
        Calendario calendarioSupplyPlan = supplyPlan.getCalendarioDoSupplyPlan(parametrosGlobais);

        // Community nao possui exclusao/inclusao por location no perfil de
        // execucao. A lista abaixo mantem todas as locations relevantes para
        // demanda direta antes da consolidacao/propagacao global do perfil.
        LocationProjection locationProjectionPerfilExecucao = LocationProjectionFactory.getLocationProjectionDePerfilExecucaoSupplyPlan(perfilExecucaoSupplyPlan, clusterEParametrosProjection);
        Set<Location> locationsClientesFinaisSemExclusaoNoPerfilExecucao = clusterEParametrosProjection.getLocationsAtivas()
                .stream()
                // locations clientes no caso de propagacao para regioes comerciais ou locations clientes/regioes no caso de propagacao para locations internas
                .filter(location ->
                        location.getTipoLocation().equals(LocationAbstract.TipoLocation.INTERNA)
                        || location.getTipoLocation().equals(LocationAbstract.TipoLocation.PONTO_TRANSBORDO)
                        || perfilExecucaoSupplyPlan.getModoPropagacaoDemanda().verificaSeRealizaPropagacao(location))
                .collect(Collectors.toSet());

        MaterialProjection materialProjection = getMaterialProjectionComEscopoEnterprise(
                perfilExecucaoSupplyPlan,
                clusterEParametrosProjection,
                MaterialProjectionFactory.getMaterialProjectionDePerfilExecucaoSupplyPlan(
                        perfilExecucaoSupplyPlan,
                        clusterEParametrosProjection));

        // possíveis targets para consolidação dependem do tipo de consolidação : no nível de locations internas ou de regiões comerciais
        Set<Location> locationsCandidatasConsolidacaoDemanda;
        if (perfilExecucaoSupplyPlan.getLocationsClienteApenasPropagamDemanda()) {
            locationsCandidatasConsolidacaoDemanda = locationProjectionPerfilExecucao.getLocationsAtivasSetComTiposLocation(
                    perfilExecucaoSupplyPlan.getModoPropagacaoDemanda().getTipoLocationDestinoPropagacao());
        } else {
            locationsCandidatasConsolidacaoDemanda = locationProjectionPerfilExecucao.getLocationsAtivasSetComTiposLocation(
                    LocationAbstract.TipoLocation.INTERNA,
                    LocationAbstract.TipoLocation.CLIENTE_FINAL,
                    LocationAbstract.TipoLocation.DISTRIBUIDOR,
                    LocationAbstract.TipoLocation.REGIAO_COMERCIAL);
        }
        // fallback se por ex. configuramos modoPropagacao para PROPAGACAO_DEMANDA_REGIOES_COMERCIAIS e não há location regiao comercial que atenda ao cliente
        // neste caso, se busca uma location interna conectada ao cliente
        Set<Location> locationsInternasCandidatasConsolidacaoDemanda = locationProjectionPerfilExecucao.getLocationsAtivasSetComTiposLocation(
                LocationAbstract.TipoLocation.INTERNA);

        // projection que será populado com primeira versão dos dados de demanda direta
        DemandaDiretaConsideradaProjection demandaDiretaConsideradaProjection = new DemandaDiretaConsideradaProjection(supplyPlan, calendarioSupplyPlan, unidadeMedidaProjection);

        // PROCESSA PLANO DE DEMANDA, 1 LOCATION POR VEZ ----------------------------------------------------------------------------------------------
        locationsClientesFinaisSemExclusaoNoPerfilExecucao.parallelStream().forEach(locationComPlanoDemanda -> {

            for (Produto material : demandPlanningProjection.getMateriaisComPlanoNaLocation(locationComPlanoDemanda)) {
                // checa se material não foi excluído no perfil de execução
                if (!materialProjection.getMaterialSet().contains(material)) continue;

                // location para onde a demanda será propagada (poderá ser a mesma location da demanda)
                Optional<Location> optionalLocationConsiderada = (perfilExecucaoSupplyPlan.getLocationsClienteApenasPropagamDemanda() && locationComPlanoDemanda.getTipoLocation().equals(LocationAbstract.TipoLocation.CLIENTE_FINAL)) ?
                        supplyNetworkProjection.getLocationOrigemPrioritaria(
                                supplyPlan.getVersaoMalha(),
                                locationComPlanoDemanda,
                                material,
                                supplyPlan.getDataInicioPlano(),
                                // considera apenas origens compatíveis com perfilExecucaoSupplyPlan.getModoPropagacaoDemanda()
                                locationsCandidatasConsolidacaoDemanda)
                        : Optional.of(locationComPlanoDemanda);

                // Quando o modo manda consolidar em região comercial, tentar primeiro a região.
                // Se nenhuma região comercial estiver conectada ao cliente, cair para uma origem interna.
                if (optionalLocationConsiderada.isEmpty()
                        && perfilExecucaoSupplyPlan.getLocationsClienteApenasPropagamDemanda()
                        && locationComPlanoDemanda.getTipoLocation().equals(LocationAbstract.TipoLocation.CLIENTE_FINAL)
                        && perfilExecucaoSupplyPlan.getModoPropagacaoDemanda().equals(PerfilExecucaoSupplyPlan.ModoPropagacaoDemanda.PROPAGACAO_DEMANDA_REGIOES_COMERCIAIS)) {
                    optionalLocationConsiderada = supplyNetworkProjection.getLocationOrigemPrioritaria(
                            supplyPlan.getVersaoMalha(),
                            locationComPlanoDemanda,
                            material,
                            supplyPlan.getDataInicioPlano(),
                            locationsInternasCandidatasConsolidacaoDemanda);
                }

                // checa se location considerada não foi excluída do perfil de execução ou se não há origem definida para determinado cliente
                if (optionalLocationConsiderada.isEmpty()) continue;

                Location locationConsiderada = optionalLocationConsiderada
                        .orElseThrow(() -> new SupplyPlanException(
                                "Internal error resolving propagated demand location for material "
                                        + material.getId()
                                        + " and demand location "
                                        + locationComPlanoDemanda.getId() + "."));
                UnidadeMedida unidadeMedidaPadrao = clusterEParametrosProjection.getSNPUnidadeMedidaPadrao(material, locationConsiderada);

                boolean houveConsolidacaoDemandaClienteEmLocationInterna = !locationConsiderada.equals(locationComPlanoDemanda);

                int offsetPeriodosLeadTime = houveConsolidacaoDemandaClienteEmLocationInterna ?
                        supplyNetworkProjection
                                .getLeadTimePeriodosEntreOrigemDestinoParaMaterial(
                                        supplyPlan.getVersaoMalha(),
                                        locationConsiderada,
                                        locationComPlanoDemanda,
                                        material,
                                        calendarioSupplyPlan,
                                        calendarioSupplyPlan.getDataHorarioInicialPresente())
                                .orElse(0)
                        : 0; // não houve propagação para location interna : não há offset de deslocamento da demanda

                for (int i=calendarioSupplyPlan.getPosicaoPeriodoPresente(); i <= perfilExecucaoSupplyPlan.getUltimoPeriodoFuturoHorizonteAPartirPeriodoPresente(locationComPlanoDemanda,  calendarioSupplyPlan, clusterEParametrosProjection) - offsetPeriodosLeadTime; i++) {
                    double quantidadeDemandPlanNoBucketSupply = getQuantidadeDemandPlanCommunityNoBucketSupply(
                            demandPlanningProjection,
                            splitTemporalProjectionPorDfu,
                            i + offsetPeriodosLeadTime,
                            // se busca demanda futura (offset lead time) para atualizar o DemandaDiretaConsiderada atual
                            locationComPlanoDemanda,
                            material,
                            unidadeMedidaPadrao);
                    if (Math.abs(quantidadeDemandPlanNoBucketSupply) < 0.0001) continue;

                    // gera novo demanda direta considerada ou obtém objeto já preexistente
                    // (por ex. se 2 locations cliente enviam demanda para a mesma location origem)
                    DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinha = demandaDiretaConsideradaProjection
                            .getOrAddDemandaDiretaConsideradaLinha(
                                    locationConsiderada,
                                    material,
                                    i);

                    demandaDiretaConsideradaLinha.setUnidadeMedida(unidadeMedidaPadrao);
                    // Seta a quantidade do Demand Plan como demanda direta considerada Community.
                    demandaDiretaConsideradaLinha.addQuantidadeOriginal(
                            quantidadeDemandPlanNoBucketSupply,
                            unidadeMedidaPadrao,
                            DemandaDiretaConsideradaLinha.TipoDemandaDireta.PLANO_DEMANDA,
                            (houveConsolidacaoDemandaClienteEmLocationInterna) ?
                                    DemandaDiretaConsideradaLinha.PropagacaoDemandaDireta.PROPAGADA
                                    : DemandaDiretaConsideradaLinha.PropagacaoDemandaDireta.NAO_PROPAGADA,
                            unidadeMedidaProjection);
                    // Community materializa apenas quantidades de demanda direta; valores gross,
                    // impostos e frete pertencem ao fluxo economico Enterprise.
                }
            }
        });
        // FIM PROCESSA PLANO DE DEMANDA ----------------------------------------------------------------------------------------------

        // ATUALIZA A DEMANDA DIRETA CONSIDERADA (APENAS DEMAND PLAN) PARA A PROJEÇÃO DE ESTOQUES E PARA O CÁLCULO DO SAFETY STOCK
        demandaDiretaConsideradaProjection.atualizaPlanoIrrestritoCommunityComDemandPlan(politicaEstoquesProjection, perfilExecucaoSupplyPlan);

        /*
         * A projection recem-montada tambem passa pela mesma borda final usada
         * pelos demais saves de demanda direta considerada. Isso evita que o
         * caminho principal Demand Plan -> Supply Plan contorne a neutralizacao
         * de carteira/valores Enterprise ou a validacao de snapshot quebrado.
         */
        saveDemandaDiretaConsideradaLinhaCollection(
                demandaDiretaConsideradaProjection.getAllDemandaDiretaConsideradaLinha(),
                false);

    }

    /**
     * Valida a entrada da geracao Community de demanda direta considerada.
     *
     * <p>Este metodo e a ponte Demand Planning -> Supply Planning aberta. Ele
     * deve aceitar somente Demand Plan quantitativo, sem carteira, curvas de
     * custo, sell-in, sales orders ou valores. Por isso flags Enterprise sao
     * bloqueadas antes de montar projections de material/location, e snapshots
     * obrigatorios falham com mensagem funcional antes do processamento
     * paralelo por location.</p>
     */
    private ClusterEParametrosProjection validaEntradasDemandaDiretaConsideradaCommunity(
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            DemandPlanningProjection demandPlanningProjection,
            SplitTemporalProjectionPorDfu splitTemporalProjectionPorDfu,
            PoliticaEstoquesProjection politicaEstoquesProjection,
            SupplyNetworkProjection supplyNetworkProjection,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        if (supplyPlan == null) {
            throw new IllegalArgumentException(
                    "Supply Plan is required for Community direct demand considered update.");
        }
        if (supplyPlan.getId() == null) {
            throw new IllegalArgumentException(
                    "Supply Plan id is required for Community direct demand considered update.");
        }
        if (perfilExecucaoSupplyPlan == null) {
            throw new IllegalStateException(
                    "Supply Planning execution profile is required for Community direct demand considered update.");
        }

        validaCurvasCustoLogisticoCommunity(perfilExecucaoSupplyPlan);

        if (demandPlanningProjection == null) {
            throw new IllegalArgumentException(
                    "Demand Planning projection is required for Community direct demand considered update.");
        }
        if (splitTemporalProjectionPorDfu == null) {
            throw new IllegalArgumentException(
                    "Temporal split projection is required for Community direct demand considered update.");
        }
        if (politicaEstoquesProjection == null) {
            throw new IllegalArgumentException(
                    "Inventory policy projection is required for Community direct demand considered update.");
        }

        ClusterEParametrosProjection clusterEParametrosProjection =
                supplyNetworkProjection.getClusterEParametrosProjection();

        return clusterEParametrosProjection;

    }

    /**
     * Resolve o offset de lead time quando a demanda de cliente final e
     * consolidada em uma location interna/regiao comercial.
     *
     * <p>Quando ha consolidacao, o deslocamento temporal afeta diretamente a
     * demanda direta considerada no Supply Planning. Usar zero como fallback
     * esconderia ausencia de linha/parametro de transporte e geraria demanda no
     * bucket errado. Por isso a ausencia do lead time falha de forma explicita.</p>
     */
    /**
     * Retorna a demanda aberta que alimenta o Supply Planning Community a
     * partir do Demand Plan.
     *
     * <p>A entidade `DemandPlanItem` ainda possui campos transicionais de
     * Uplift e New Materials, e o getter total generico preserva esses campos
     * para compatibilidade de schema. A ponte Community Demand -> Supply deve
     * somar explicitamente apenas Baseline e Demand Adjustment, que sao as duas
     * KFs abertas para geracao, Planning Book e plano restrito. O metodo e
     * protected para overlays Enterprise somarem KFs privadas ja migradas sem
     * duplicar a montagem de demanda direta considerada.</p>
     */
    protected double getQuantidadeDemandPlanCommunityNoBucketSupply(
            DemandPlanningProjection demandPlanningProjection,
            SplitTemporalProjectionPorDfu splitTemporalProjectionPorDfu,
            int posicaoPeriodoCalendarioSupply,
            Location locationComPlanoDemanda,
            Produto material,
            UnidadeMedida unidadeMedidaPadrao) {

        double quantidadeBaseline = demandPlanningProjection.getValorDemandPlanItemNoCalendarioTargetSplitTemporal(
                splitTemporalProjectionPorDfu,
                posicaoPeriodoCalendarioSupply,
                locationComPlanoDemanda,
                material,
                Constantes.TipoDemanda.BASELINE,
                Constantes.TipoPlano.PLANO_IRRESTRITO,
                unidadeMedidaPadrao);
        double quantidadeAjusteDemanda = demandPlanningProjection.getValorDemandPlanItemNoCalendarioTargetSplitTemporal(
                splitTemporalProjectionPorDfu,
                posicaoPeriodoCalendarioSupply,
                locationComPlanoDemanda,
                material,
                Constantes.TipoDemanda.AJUSTE_DEMANDA,
                Constantes.TipoPlano.PLANO_IRRESTRITO,
                unidadeMedidaPadrao);

        return quantidadeBaseline + quantidadeAjusteDemanda;

    }

    public void atualizaPlanoTrabalhoComRestritoOuIrrestrito(SupplyPlan supplyPlan) {
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = supplyPlan.getPerfilExecucaoSupplyPlan();
        Constantes.TipoPlano tipoPlanoTrabalho = perfilExecucaoSupplyPlan.getTipoPlanoTrabalho();
            log.info("Updating working version = " + tipoPlanoTrabalho.toString() + " for supply plan " + supplyPlan.getId());
            switch(tipoPlanoTrabalho) {
            case PLANO_IRRESTRITO:
                distributionPlanItemRepository.atualizaOrdensPlanejadasPlanoTrabalhoComPlanoIrrestrito(supplyPlan.getId());
                distributionPlanItemRepository.atualizaOrdensFirmesPlanoTrabalhoComPlanoIrrestrito(supplyPlan.getId());
                productionPlanLinhaRepository.atualizaOrdensPlanejadasPlanoTrabalhoComPlanoIrrestrito(supplyPlan.getId());
                productionPlanLinhaRepository.atualizaOrdensFirmesPlanoTrabalhoComPlanoIrrestrito(supplyPlan.getId());
                inventoryPlanLinhaRepository.atualizaInventoryPlanTrabalhoComPlanoIrrestrito(supplyPlan.getId());
                break;
            case PLANO_RESTRITO:
                distributionPlanItemRepository.atualizaOrdensPlanejadasPlanoTrabalhoComPlanoRestrito(supplyPlan.getId());
                distributionPlanItemRepository.atualizaOrdensFirmesPlanoTrabalhoComPlanoRestrito(supplyPlan.getId());
                productionPlanLinhaRepository.atualizaOrdensPlanejadasPlanoTrabalhoComPlanoRestrito(supplyPlan.getId());
                productionPlanLinhaRepository.atualizaOrdensFirmesPlanoTrabalhoComPlanoRestrito(supplyPlan.getId());
                inventoryPlanLinhaRepository.atualizaInventoryPlanTrabalhoComPlanoRestrito(supplyPlan.getId());
                break;
            default:
                throw new IllegalArgumentException("Does not apply to " + tipoPlanoTrabalho.toString());
        }
    }


}
