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
        supplyPlanningBiProjection.preparaBaselineIrrestritoParaExecucaoHeuristica();

        /*
         * A demanda original nasce no irrestrito. Como o baseline calculado foi
         * zerado acima, a cópia inicializa demanda, ordens firmes e estoque do
         * restrito sem carregar sugestões planejadas de uma rodada anterior.
         */
        supplyPlanningBiProjection.atualizaPlanoRestritoComPlanoIrrestrito();

        executaPlanoRestritoNiveladoPorLowLevelCode(
                supplyPlan,
                perfilExecucaoSupplyPlan,
                materialProjection,
                locationProjection,
                lowLevelCode,
                clusterEParametrosProjection,
                supplyPlanningProjectionLocationPolicy,
                supplyPlanningBiProjection,
                supplyNetworkProjection,
                biProjectionCapacidadeProdutiva);

        /*
         * A última passagem LLC pode recalcular uma compra depois que o estoque
         * do respectivo insumo já foi visitado. Reprojetamos a fotografia
         * restrita completa somente após todos os níveis, sem criar ordens
         * adicionais nem refazer decisões de capacidade.
         */
        nivelamentoCapacidadePlanoIrrestritoHeuristicoService.atualizaEstoquesDoPlano(
                perfilExecucaoSupplyPlan,
                supplyPlanningBiProjection,
                Constantes.TipoPlano.PLANO_RESTRITO);
        nivelamentoCapacidadePlanoIrrestritoHeuristicoService
                .reconciliaComprasPlanejadasDeFornecedoresComPlanoRestrito(
                        perfilExecucaoSupplyPlan,
                        supplyPlanningBiProjection);

        /*
         * O irrestrito parte da solução restrita e preserva a demanda original.
         * Uma única passada LLC, sem capacidade, joga somente o residual nas
         * origens primárias e propaga seus insumos até a compra.
         */
        supplyPlanningBiProjection.atualizaPlanoIrrestritoComPlanoRestritoSemSobrescreverDemanda();
        executaPlanoPorLowLevelCode(
                supplyPlan,
                perfilExecucaoSupplyPlan,
                materialProjection,
                locationProjection,
                lowLevelCode,
                clusterEParametrosProjection,
                supplyPlanningProjectionLocationPolicy,
                supplyPlanningBiProjection,
                Constantes.TipoPlano.PLANO_IRRESTRITO,
                true);
        salvaCheckpointSupplyPlanningBiProjection(supplyPlanningBiProjection, true);

        // atualiza o plano de trabalho (working plan)
        supplyPlanService.atualizaPlanoTrabalhoComRestritoOuIrrestrito(supplyPlan);

    }

    /**
     * Gera e nivela o plano restrito na própria ordem do low level code.
     *
     * <p>Cada nível é concluído antes de gerar o seguinte. Assim, quando o
     * nivelamento desloca um acabado para outra fábrica, a necessidade de skid
     * ainda não foi materializada: ela nasce diretamente na fábrica escolhida.
     * A mesma regra se repete de skid para bobina e de bobina para matéria-prima.
     * Isso evita preservar como carga fixa uma produção dependente calculada
     * para uma decisão de fábrica que já deixou de existir.</p>
     */
    private void executaPlanoRestritoNiveladoPorLowLevelCode(
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            MaterialProjection materialProjection,
            LocationProjection locationProjection,
            LowLevelCode lowLevelCode,
            ClusterEParametrosProjection clusterEParametrosProjection,
            SupplyPlanningProjectionLocationPolicy supplyPlanningProjectionLocationPolicy,
            SupplyPlanningBiProjection supplyPlanningBiProjection,
            SupplyNetworkProjection supplyNetworkProjection,
            BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva) {

        int ultimoLowLevelCode = lowLevelCode.getUltimoLowLevelCode().getAsInt();
        for (int posicaoLowLevelCode = 1;
                posicaoLowLevelCode <= ultimoLowLevelCode;
                posicaoLowLevelCode++) {
            NivelamentoCapacidadePlanoIrrestritoHeuristicoService.FotografiaPlanoIrrestrito
                    fotografiaAntesDoNivel = nivelamentoCapacidadePlanoIrrestritoHeuristicoService
                    .capturaFotografiaPlano(
                            supplyNetworkProjection,
                            supplyPlanningBiProjection,
                            Constantes.TipoPlano.PLANO_RESTRITO);

            executaPlanoPosicaoLowLevelCode(
                    supplyPlan,
                    perfilExecucaoSupplyPlan,
                    materialProjection,
                    locationProjection,
                    lowLevelCode,
                    clusterEParametrosProjection,
                    supplyPlanningProjectionLocationPolicy,
                    supplyPlanningBiProjection,
                    Constantes.TipoPlano.PLANO_RESTRITO,
                    false,
                    posicaoLowLevelCode,
                    ultimoLowLevelCode);

            /*
             * Somente a produção criada neste nível entra na rodada de
             * capacidade. As decisões de níveis anteriores já estão fixas e
             * reduzem a capacidade residual; o residual do restrito não volta
             * à origem primária quando nenhuma alternativa consegue absorvê-lo.
             */
            nivelamentoCapacidadePlanoIrrestritoHeuristicoService.aplicaIncrementosGeradosApos(
                    fotografiaAntesDoNivel,
                    supplyPlan,
                    perfilExecucaoSupplyPlan,
                    supplyPlan.getCalendarioDoSupplyPlan(
                            clusterEParametrosProjection.getParametrosGlobais()),
                    supplyNetworkProjection,
                    biProjectionCapacidadeProdutiva,
                    supplyPlanningBiProjection,
                    Constantes.TipoPlano.PLANO_RESTRITO,
                    false,
                    lowLevelCode,
                    posicaoLowLevelCode);
        }

    }

    /**
     * Propaga o residual irrestrito por low level code depois de o plano
     * restrito estar completo.
     *
     * <p>Nesta passada final não há consulta de capacidade. O método preserva
     * a fotografia restrita e materializa nas origens primárias somente a
     * demanda ainda residual e suas dependências.</p>
     */
    private void executaPlanoPorLowLevelCode(
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            MaterialProjection materialProjection,
            LocationProjection locationProjection,
            LowLevelCode lowLevelCode,
            ClusterEParametrosProjection clusterEParametrosProjection,
            SupplyPlanningProjectionLocationPolicy supplyPlanningProjectionLocationPolicy,
            SupplyPlanningBiProjection supplyPlanningBiProjection,
            Constantes.TipoPlano tipoPlano,
            boolean recalculoPosNivelamento) {

        int ultimoLowLevelCode = lowLevelCode.getUltimoLowLevelCode().getAsInt();
        for (int posicaoLowLevelCode = 1;
                posicaoLowLevelCode <= ultimoLowLevelCode;
                posicaoLowLevelCode++) {
            executaPlanoPosicaoLowLevelCode(
                    supplyPlan,
                    perfilExecucaoSupplyPlan,
                    materialProjection,
                    locationProjection,
                    lowLevelCode,
                    clusterEParametrosProjection,
                    supplyPlanningProjectionLocationPolicy,
                    supplyPlanningBiProjection,
                    tipoPlano,
                    recalculoPosNivelamento,
                    posicaoLowLevelCode,
                    ultimoLowLevelCode);
        }

    }

    /** Executa todas as DFUs material/location pertencentes a um único nível. */
    private void executaPlanoPosicaoLowLevelCode(
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            MaterialProjection materialProjection,
            LocationProjection locationProjection,
            LowLevelCode lowLevelCode,
            ClusterEParametrosProjection clusterEParametrosProjection,
            SupplyPlanningProjectionLocationPolicy supplyPlanningProjectionLocationPolicy,
            SupplyPlanningBiProjection supplyPlanningBiProjection,
            Constantes.TipoPlano tipoPlano,
            boolean recalculoPosNivelamento,
            int posicaoLowLevelCode,
            int ultimoLowLevelCode) {

        log.info(
                "{} Distribution / Production Planning para Low Level Code {}/{} , com {} DFUs material/location",
                recalculoPosNivelamento ? "Recalculando" : "Executando",
                posicaoLowLevelCode,
                ultimoLowLevelCode,
                lowLevelCode.getNumeroDFUsLowLevelCode(posicaoLowLevelCode));

        for (Location location : lowLevelCode.getLocationsLowLevelCode(posicaoLowLevelCode)) {
            /*
             * Clientes que apenas propagam demanda não geram plano nesta
             * etapa. A demanda direta considerada já foi preparada antes da
             * execução e o loop trabalha apenas com locations de fluxo real.
             */
            if (perfilExecucaoSupplyPlan.getLocationsClienteApenasPropagamDemanda()
                    && perfilExecucaoSupplyPlan.getModoPropagacaoDemanda().verificaSeRealizaPropagacao(location)) {
                continue;
            }

            log.info(
                    "{} Supply Plan {} para Location {}",
                    recalculoPosNivelamento ? "Recalculando" : "Gerando",
                    supplyPlan.getId(),
                    location.getId());
            Set<Produto> produtosLocation = lowLevelCode.getMateriaisLowLevelCodeEmLocation(
                    posicaoLowLevelCode,
                    location);
            executaPlanoLocation(
                    perfilExecucaoSupplyPlan,
                    materialProjection,
                    locationProjection,
                    clusterEParametrosProjection,
                    supplyPlanningProjectionLocationPolicy,
                    supplyPlanningBiProjection,
                    location,
                    produtosLocation,
                    tipoPlano);
        }

    }

    /**
     * Executa a geracao irrestrita para um recorte material/location e devolve
     * imediatamente as linhas alteradas ao snapshot compartilhado da rodada.
     */
    private void executaPlanoLocation(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            MaterialProjection materialProjection,
            LocationProjection locationProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            SupplyPlanningProjectionLocationPolicy supplyPlanningProjectionLocationPolicy,
            SupplyPlanningBiProjection supplyPlanningBiProjection,
            Location location,
            Set<Produto> produtosLocation,
            Constantes.TipoPlano tipoPlano) {

        MaterialProjection materialProjectionMateriaisLowLevelCodeLocationAtuais =
                MaterialProjectionFactory.getProjectionSetMateriais(
                        produtosLocation,
                        clusterEParametrosProjection);
        SupplyPlanningProjection supplyPlanningProjection =
                supplyPlanningBiProjection.getSupplyPlanningProjection(
                        location,
                        materialProjectionMateriaisLowLevelCodeLocationAtuais);
        aplicaPoliticaExecucaoLocation(
                supplyPlanningProjectionLocationPolicy,
                supplyPlanningProjection);

        SupplyPlanning.geraRequisicoesESugestoesProducao(
                supplyPlanningProjection,
                materialProjection,
                locationProjection,
                tipoPlano);

        if (!perfilExecucaoSupplyPlan.getPermiteBacklogDemanda()) {
            SupplyPlanning.limitaEstoquesNegativosAZero(
                    tipoPlano,
                    supplyPlanningProjection);
        }

        SupplyPlanning.atualizaDistributionPlanItemComParcelaAtendimentoDemandaDireta(
                supplyPlanningProjection,
                tipoPlano);
        supplyPlanningBiProjection.sincroniza(supplyPlanningProjection);

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
