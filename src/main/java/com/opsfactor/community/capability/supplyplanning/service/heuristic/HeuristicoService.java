package com.opsfactor.community.capability.supplyplanning.service.heuristic;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjectionFactory;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.production.productionresource.projection.BIProjectionCapacidadeProdutiva;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanBiProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningBiProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.capability.lowlevelcode.engine.LowLevelCode;
import com.opsfactor.community.capability.supplyplanning.engine.SupplyPlanning;
import com.opsfactor.community.capability.supplyplanning.service.SupplyPlanService;
import com.opsfactor.community.capability.supplyplanning.service.spi.SupplyPlanExecutionProfileLocationPolicySpi;
import com.opsfactor.community.capability.supplyplanning.service.spi.SupplyPlanExecutionProfileLocationPolicySpi.SupplyPlanningProjectionLocationPolicy;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Motor heuristico de Supply Planning Community.
 *
 * <p>Este service gera requisicoes, sugestoes de producao, estoque projetado e
 * plano restrito quantitativo. Ele nao conhece otimizador, process chain,
 * custos, P&L, line scheduling, fleets, AI optimizer ou variaveis/constraints
 * de solver; essas capacidades sao tratadas pelo overlay Enterprise.</p>
 */
@Slf4j
@Service
public class HeuristicoService {

    /*
     * Back-reference para metodos de persistencia do orquestrador. A injecao
     * lazy evita ciclo de criacao Spring: SupplyPlanService coordena o fluxo e
     * chama HeuristicoService, enquanto a heuristica reutiliza apenas saves
     * padronizados do service principal.
     */
    @Autowired
    @Lazy
    private SupplyPlanService supplyPlanService;

    /**
     * Service que aplica o plano restrito heuristico quando o perfil Community
     * encadeia a restricao apos o plano irrestrito.
     */
    @Autowired
    private ConstrainedPlanService constrainedPlanService;

    /** Redistribui a produção irrestrita quando o perfil habilita nivelamento. */
    @Autowired
    private NivelamentoCapacidadePlanoIrrestritoHeuristicoService nivelamentoCapacidadePlanoIrrestritoHeuristicoService;

    /**
     * Constrói uma fotografia única por rodada para que LLC/location usem
     * views locais, em vez de repetir consultas de linhas persistidas.
     */
    @Autowired
    private SupplyPlanBiProjectionFactory supplyPlanBiProjectionFactory;

    /**
     * Optional Enterprise resolver for the three generation flags that vary by
     * location. Community keeps the defaults initialized by the projection
     * when this private capability is absent.
     */
    @Autowired(required = false)
    private SupplyPlanExecutionProfileLocationPolicySpi supplyPlanExecutionProfileLocationPolicySpi;

    /**
     * Executa o fluxo heuristico completo sobre projections ja preparadas pelo
     * orquestrador.
     *
     * <p>O metodo percorre os low level codes na ordem fornecedores -> demanda,
     * cria uma projection por location, popula dados operacionais, gera
     * requisicoes/producao e persiste o resultado por batch atraves do
     * SupplyPlanService.</p>
     */
    public void executaSupplyPlanHeuristico(
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            SupplyNetworkProjection supplyNetworkProjection,
            BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva,
            PoliticaEstoquesProjection politicaEstoquesProjection,
            MaterialProjection materialProjection,
            LocationProjection locationProjection) {

        validaEntradasHeuristicoCommunity(
                supplyPlan,
                perfilExecucaoSupplyPlan,
                supplyNetworkProjection,
                biProjectionCapacidadeProdutiva,
                politicaEstoquesProjection,
                materialProjection,
                locationProjection);

        log.info("Calculando low level codes");
        /*
         * LowLevelCode permanece como rotina compartilhada de malha/BOM. A
         * heuristica Community consome esse resultado ja ordenado para manter
         * o fluxo simples e evitar duplicar traversal de dependencias.
         */
        LowLevelCode lowLevelCode = new LowLevelCode(
                supplyNetworkProjection,
                supplyPlan.getVersaoMalha(),
                materialProjection,
                locationProjection,
                supplyPlan.getDataInicioPlano());
        lowLevelCode.atualizaMapaDFUsPorLowLevelCode();

        ClusterEParametrosProjection clusterEParametrosProjection = supplyNetworkProjection.getClusterEParametrosProjection();
        SupplyPlanningProjectionLocationPolicy supplyPlanningProjectionLocationPolicy =
                getSupplyPlanningProjectionLocationPolicy(perfilExecucaoSupplyPlan);
        SupplyPlanningBiProjection supplyPlanningBiProjection =
                supplyPlanBiProjectionFactory.getSupplyPlanningBiProjectionCompleto(
                        supplyPlan,
                        perfilExecucaoSupplyPlan,
                        supplyNetworkProjection,
                        politicaEstoquesProjection,
                        materialProjection,
                        locationProjection);

        /*
         * Executa os planos de producao e distribuicao por low level code. A
         * ordem preserva dependencias de insumos e abastecimento antes de
         * chegar nas DFUs de demanda.
         */
        int ultimoLowLevelCode = lowLevelCode.getUltimoLowLevelCode().getAsInt();
        for (int posicaoLowLevelCode = 1; posicaoLowLevelCode <= ultimoLowLevelCode; posicaoLowLevelCode++) {
            log.info("Executando Distribution / Production Planning para Low Level Code " + posicaoLowLevelCode + "/"
                    + ultimoLowLevelCode + " , com "
                    + lowLevelCode.getNumeroDFUsLowLevelCode(posicaoLowLevelCode) + " DFUs material/location");

            // um projection é inicializado por location
            for (Location location : lowLevelCode.getLocationsLowLevelCode(posicaoLowLevelCode)) {

                /*
                 * Clientes que apenas propagam demanda nao geram plano nesta
                 * etapa. A demanda direta considerada ja foi preparada antes
                 * da execucao heuristica e o loop de producao/distribuicao
                 * trabalha apenas com locations que realmente planejam fluxo.
                 */
                if (perfilExecucaoSupplyPlan.getLocationsClienteApenasPropagamDemanda() &&
                        perfilExecucaoSupplyPlan.getModoPropagacaoDemanda().verificaSeRealizaPropagacao(location)) continue;

                log.info("Gerando Supply Plan " + supplyPlan.getId() + " para Location " + location.getId());
                // gera lista de produtos a partir das DFUs da location
                Set<Produto> produtosLocation = lowLevelCode.getMateriaisLowLevelCodeEmLocation(posicaoLowLevelCode, location);
                MaterialProjection materialProjectionMateriaisLowLevelCodeLocationAtuais = MaterialProjectionFactory.getProjectionSetMateriais(
                        produtosLocation, clusterEParametrosProjection);

                SupplyPlanningProjection supplyPlanningProjection = supplyPlanningBiProjection.getSupplyPlanningProjection(
                        location,
                        materialProjectionMateriaisLowLevelCodeLocationAtuais);
                aplicaPoliticaExecucaoLocation(
                        supplyPlanningProjectionLocationPolicy,
                        supplyPlanningProjection);
                SupplyPlanning.geraRequisicoesESugestoesProducao(
                        supplyPlanningProjection,
                        materialProjection, locationProjection);

                if (!perfilExecucaoSupplyPlan.getPermiteBacklogDemanda()) {
                    SupplyPlanning.limitaEstoquesNegativosAZero(Constantes.TipoPlano.PLANO_IRRESTRITO, supplyPlanningProjection);
                }

                // Atualiza a parcela de requisições que atende indiretamente a demanda direta.
                // No Community, os componentes transacionais de carteira/pedidos ficam zerados.
                SupplyPlanning.atualizaDistributionPlanItemComParcelaAtendimentoDemandaDireta(supplyPlanningProjection);

                supplyPlanningBiProjection.sincroniza(supplyPlanningProjection);

            }
        }

        /*
         * O Constrained Plan é obrigatório. O plano irrestrito calculado acima
         * é o baseline da rodada; a restrição materializa a série viável que o
         * Working Plan usará por padrão. Projeções econômicas de preço/COGS
         * pertenciam apenas à explicabilidade legada, mantida no Enterprise.
         */
        salvaCheckpointSupplyPlanningBiProjection(supplyPlanningBiProjection, false);

        boolean nivelamentoAplicado = nivelamentoCapacidadePlanoIrrestritoHeuristicoService.aplica(
                supplyPlan,
                perfilExecucaoSupplyPlan,
                supplyPlan.getCalendarioDoSupplyPlan(clusterEParametrosProjection.getParametrosGlobais()),
                supplyNetworkProjection,
                biProjectionCapacidadeProdutiva,
                supplyPlanningBiProjection);
        if (nivelamentoAplicado) {
            /* Linhas primárias podem ir a zero depois da realocação; o segundo
             * checkpoint precisa persistir esses zeros antes do plano restrito. */
            salvaCheckpointSupplyPlanningBiProjection(supplyPlanningBiProjection, true);
        }

        constrainedPlanService.restringePlano(
                supplyPlan,
                perfilExecucaoSupplyPlan,
                supplyPlan.getCalendarioDoSupplyPlan(clusterEParametrosProjection.getParametrosGlobais()),
                supplyNetworkProjection,
                biProjectionCapacidadeProdutiva,
                politicaEstoquesProjection,
                lowLevelCode);

        // atualiza o plano de trabalho (working plan)
        supplyPlanService.atualizaPlanoTrabalhoComRestritoOuIrrestrito(supplyPlan);

    }

    /**
     * Resolves the optional Enterprise policy once before the heuristic enters
     * the low-level-code/location loop.
     */
    private SupplyPlanningProjectionLocationPolicy getSupplyPlanningProjectionLocationPolicy(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) {

        if (supplyPlanExecutionProfileLocationPolicySpi == null) {
            return null;
        }

        SupplyPlanningProjectionLocationPolicy supplyPlanningProjectionLocationPolicy =
                supplyPlanExecutionProfileLocationPolicySpi
                        .getSupplyPlanningProjectionLocationPolicy(perfilExecucaoSupplyPlan);
        if (supplyPlanningProjectionLocationPolicy == null) {
            throw new IllegalStateException(
                    "Supply Planning execution-profile location policy resolver returned null.");
        }

        return supplyPlanningProjectionLocationPolicy;

    }

    /**
     * Persiste cada coleção canônica no máximo uma vez ao terminar o baseline,
     * evitando write batches por location/LLC e mantendo o snapshot íntegro
     * para a etapa opcional de nivelamento.
     */
    private void salvaCheckpointSupplyPlanningBiProjection(
            SupplyPlanningBiProjection supplyPlanningBiProjection,
            boolean salvaZerosProductionPlan) {

        supplyPlanService.saveProductionPlanLinhaCollection(
                supplyPlanningBiProjection.getTodosProductionPlanLinhas(),
                salvaZerosProductionPlan);
        supplyPlanService.saveDistributionPlanItemCollection(
                supplyPlanningBiProjection.getTodosDistributionPlanItems(),
                false);
        if (supplyPlanningBiProjection.getPerfilExecucaoSupplyPlanConsiderado().getSalvaInventoryPlan()) {
            supplyPlanService.saveInventoryPlanLinhaCollection(
                    supplyPlanningBiProjection.getTodosInventoryPlanLinhas(),
                    false);
        } else {
            /* A política sem série completa preserva o filtro histórico de
             * estoque inicial e trânsito por location. */
            for (Location location : supplyPlanningBiProjection.getLocationProjection().getLocationsAtivas()) {
                supplyPlanService.saveEstoqueInicialETransitoDePlanningProjection(
                        supplyPlanningBiProjection.getSupplyPlanningProjection(
                                location,
                                supplyPlanningBiProjection.getMaterialProjection()),
                        false);
            }
        }
        supplyPlanService.saveDemandaDiretaConsideradaLinhaCollection(
                supplyPlanningBiProjection.getTodasDemandasDiretasConsideradas(),
                false);

    }

    /**
     * Keeps the Community defaults when no Enterprise capability is loaded.
     */
    private void aplicaPoliticaExecucaoLocation(
            SupplyPlanningProjectionLocationPolicy supplyPlanningProjectionLocationPolicy,
            SupplyPlanningProjection supplyPlanningProjection) {

        if (supplyPlanningProjectionLocationPolicy == null) {
            return;
        }

        supplyPlanningProjectionLocationPolicy.applyTo(supplyPlanningProjection);

    }

    /**
     * Valida a fotografia minima consumida pelo motor heuristico Community.
     *
     * <p>O orquestrador monta as projections antes de chamar este service, mas
     * a entrada continua publica e tambem pode ser reutilizada por testes ou
     * overlays Enterprise. Falhar aqui deixa claro se o problema e tentativa
     * de executar motor privado no heuristico ou snapshot Community incompleto,
     * em vez de estourar dentro de `LowLevelCode` ou da primeira factory de
     * SupplyPlanningProjection.</p>
     */
    private void validaEntradasHeuristicoCommunity(
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            SupplyNetworkProjection supplyNetworkProjection,
            BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva,
            PoliticaEstoquesProjection politicaEstoquesProjection,
            MaterialProjection materialProjection,
            LocationProjection locationProjection) {

        if (supplyPlan == null) {
            throw new IllegalArgumentException(
                    "Supply Plan is required for heuristic Supply Planning execution.");
        }
        if (supplyPlan.getId() == null) {
            throw new IllegalArgumentException(
                    "Supply Plan id is required for heuristic Supply Planning execution.");
        }
        if (perfilExecucaoSupplyPlan == null) {
            throw new IllegalStateException(
                    "Heuristic Supply Planning execution profile is null.");
        }
        if (!PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO.equals(
                perfilExecucaoSupplyPlan.getModoExecucao())) {
            throw new RequiresEnterpriseVersionException("Supply Planning execution engine");
        }
        if (supplyNetworkProjection == null) {
            throw new IllegalArgumentException(
                    "Supply Network projection is required for heuristic Supply Planning execution.");
        }
        if (supplyNetworkProjection.getClusterEParametrosProjection() == null) {
            throw new IllegalArgumentException(
                    "Cluster and parameters projection is required for heuristic Supply Planning execution.");
        }
        if (supplyNetworkProjection.getClusterEParametrosProjection().getParametrosGlobais() == null) {
            /*
             * Parametros globais definem calendario e granularidade temporal do
             * plano. Mesmo quando o orquestrador ja validou o snapshot, esta
             * entrada publica deve falhar aqui para chamadas diretas ou overlays
             * que reutilizem o motor heuristico.
             */
            throw new IllegalArgumentException(
                    "Global parameters are required for heuristic Supply Planning execution.");
        }
        if (politicaEstoquesProjection == null) {
            throw new IllegalArgumentException(
                    "Inventory policy projection is required for heuristic Supply Planning execution.");
        }
        if (materialProjection == null) {
            throw new IllegalArgumentException(
                    "Material projection is required for heuristic Supply Planning execution.");
        }
        if (locationProjection == null) {
            throw new IllegalArgumentException(
                    "Location projection is required for heuristic Supply Planning execution.");
        }
        if (biProjectionCapacidadeProdutiva == null) {
            /*
             * Capacidade produtiva é consumida pela rodada restrita obrigatória
             * ao fim do heurístico. Sem a projection não existe Constrained
             * Plan íntegro para atualizar o Working Plan.
             */
            throw new IllegalArgumentException(
                    "Production capacity projection is required for constrained heuristic Supply Planning execution.");
        }

    }


}
