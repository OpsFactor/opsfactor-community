package com.opsfactor.community.capability.supplyplanning.service.heuristic;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.supplyplanning.distributionplan.repository.DistributionPlanItemRepository;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.repository.InventoryPlanLinhaRepository;
import com.opsfactor.community.capability.supplyplanning.productionplan.repository.ProductionPlanLinhaRepository;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.DemandaDiretaConsideradaLinhaRepository;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.SupplyPlanRepository;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan.ModoExecucao;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjectionFactory;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjection;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjectionFactory;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjectionFactory;
import com.opsfactor.community.capability.masterdata.production.productionresource.projection.BIProjectionCapacidadeProdutiva;
import com.opsfactor.community.capability.masterdata.production.productionresource.projection.BIProjectionCapacidadeProdutivaFactory;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.capability.lowlevelcode.engine.LowLevelCode;
import com.opsfactor.community.capability.supplyplanning.engine.SupplyPlanning;
import com.opsfactor.community.capability.supplyplanning.engine.constrained.ConstrainedPlanningHeuristicoRotinas;
import com.opsfactor.community.platform.exception.SupplyPlanException;
import com.opsfactor.community.capability.supplyplanning.service.SupplyPlanService;
import com.opsfactor.community.capability.supplyplanning.service.spi.SupplyPlanExecutionProfileMaterialScopeSpi;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calcula o plano restrito Community a partir do resultado heuristico.
 *
 * <p>Este service trabalha apenas com quantidades, disponibilidade, capacidade
 * produtiva simples e fair share direto. Analises economicas, precos, COGS,
 * causa raiz detalhada de restricoes e explicabilidade de margem pertencem ao
 * OpsFactor Enterprise.</p>
 */
@Slf4j
@Service
public class ConstrainedPlanService {

    /**
     * Repositorio do Supply Plan a restringir.
     */
    @Autowired
    private SupplyPlanRepository supplyPlanRepository;

    /**
     * Repository com updates massivos de producao restrita/trabalho. Usar
     * operacoes em lote aqui evita salvar linha a linha durante a restricao.
     */
    @Autowired
    private ProductionPlanLinhaRepository productionPlanLinhaRepository;

    /**
     * Repository com updates massivos de distribuicao restrita/trabalho.
     */
    @Autowired
    private DistributionPlanItemRepository distributionPlanItemRepository;

    /*
     * Back-reference para os metodos padronizados de persistencia do
     * SupplyPlanService. A criacao lazy quebra o ciclo de bootstrap enquanto
     * mantemos o calculo restrito no service heuristico Community.
     */
    @Autowired
    @Lazy
    private SupplyPlanService supplyPlanService;

    /**
     * Factory que materializa projections de Supply Plan por location/material
     * e carrega os planos de estoque, producao, distribuicao e demanda direta
     * considerada usados na restricao.
     */
    @Autowired
    private SupplyPlanProjectionFactory supplyPlanProjectionFactory;

    /**
     * Factory de parametros globais, materiais e locations ativos usados para
     * montar projections sem N+1 durante o loop de low level code.
     */
    @Autowired
    private ClusterEParametrosProjectionFactory parametrosProjectionFactory;

    /**
     * Factory da malha em memoria, usada para linhas inbound, BOM, roteiros e
     * recursos produtivos do fluxo heuristico.
     */
    @Autowired
    private SupplyNetworkProjectionFactory supplyNetworkProjectionFactory;

    /**
     * Repository de inventory plan usado para reset e persistencia em lote do
     * plano restrito/trabalho quando o perfil salva estoque projetado.
     */
    @Autowired
    private InventoryPlanLinhaRepository inventoryPlanLinhaRepository;

    /**
     * Repository de demanda direta considerada, incluindo o plano restrito que
     * sera propagado apos cortes por insumos/capacidade/fair share.
     */
    @Autowired
    private DemandaDiretaConsideradaLinhaRepository demandaDiretaConsideradaLinhaRepository;

    /**
     * Factory de politicas de estoque operacionais. Safety stock existe no
     * Community; apenas otimizacao automatica da politica e Enterprise.
     */
    @Autowired
    private PoliticaEstoquesProjectionFactory politicaEstoquesProjectionFactory;

    /**
     * Factory da capacidade produtiva simples usada pelo plano restrito
     * Community. Capacidade detalhada por turno/scheduling fica no Enterprise.
     */
    @Autowired
    private BIProjectionCapacidadeProdutivaFactory biProjectionCapacidadeProdutivaFactory;

    /** Optional Enterprise narrowing of the shared active-material snapshot. */
    @Autowired(required = false)
    private SupplyPlanExecutionProfileMaterialScopeSpi supplyPlanExecutionProfileMaterialScope;

    /**
     * Recarrega o Supply Plan e executa a restricao apenas se o perfil for
     * heuristico.
     */
    public void restringePlanoComPerfilHeuristico(Long supplyPlanId) {


        Optional<SupplyPlan> supplyPlanOptional = supplyPlanRepository.findById(supplyPlanId);

        /*
         * Optional.empty() continua representando Supply Plan inexistente e usa
         * a mensagem funcional ja esperada pela camada de chamada. Retorno
         * nulo do repository, por outro lado, quebra o contrato Spring Data e
         * deve falhar antes de perfil, projections ou updates massivos.
         */
        if (supplyPlanOptional == null) {
            throw new IllegalStateException(
                    "Supply Plan repository returned null Optional for constrained heuristic execution id "
                            + supplyPlanId
                            + ".");
        }

        SupplyPlan supplyPlan = supplyPlanOptional
                .orElseThrow(() -> new SupplyPlanException("Supply plan " + supplyPlanId + " not found"));
        validaSupplyPlanSnapshotConstrainedPlanningCommunity(
                supplyPlan,
                supplyPlanId);
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = supplyPlan.getPerfilExecucaoSupplyPlan();

        validaPerfilHeuristicoConstrainedPlanningCommunity(perfilExecucaoSupplyPlan);

        restringePlano(supplyPlan, perfilExecucaoSupplyPlan);
        
    }

    /**
     * Garante que o plano restrito Community esteja sendo chamado somente para
     * rodadas heuristicas.
     *
     * <p>A entidade `SupplyPlan` preserva o default historico de retornar um
     * perfil vazio quando o relacionamento ainda nao esta carregado; esse
     * perfil vazio tambem defaulta o modo para `HEURISTICO`. A validacao aqui
     * portanto existe para cortar motores Enterprise explicitos antes de
     * qualquer factory/projection, nao para rejeitar o default heuristico.</p>
     *
     * <p>Optimizer e Process Chain podem acionar limpeza/restricao propria via
     * overlays Enterprise. O service Community nao deve tentar interpretar
     * esses motores, porque seus parametros, projections e persistencias
     * pertencem ao repositorio privado.</p>
     */
    private void validaPerfilHeuristicoConstrainedPlanningCommunity(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) {

        if (perfilExecucaoSupplyPlan == null) {
            throw new IllegalStateException("Constrained Supply Planning execution profile is null.");
        }

        if (!ModoExecucao.HEURISTICO.equals(perfilExecucaoSupplyPlan.getModoExecucao())) {
            throw new RequiresEnterpriseVersionException("Supply Planning execution engine");
        }

    }

    /**
     * Garante que a restricao heuristica receba um Supply Plan materializado.
     *
     * <p>O plano restrito Community sempre recalcula linhas ja persistidas pelo
     * plano heuristico. Sem id nao ha snapshot operacional para restringir, por
     * isso a falha precisa ocorrer antes de repository/projection.</p>
     */
    /**
     * Valida a identidade do Supply Plan recarregado pelo repository.
     *
     * <p>O plano restrito por id e acionado depois de uma rodada heuristica ja
     * persistida. Se o repository devolver uma entidade sem id ou com id
     * divergente, o service nao deve interpretar esse snapshot como plano
     * valido nem chegar a perfil/projections/updates em lote.</p>
     */
    private void validaSupplyPlanSnapshotConstrainedPlanningCommunity(
            SupplyPlan supplyPlan,
            Long expectedSupplyPlanId) {

        if (supplyPlan.getId() == null) {
            throw new IllegalStateException(
                    "Supply Plan snapshot id is required for constrained heuristic execution id "
                            + expectedSupplyPlanId
                            + ".");
        }
        if (!expectedSupplyPlanId.equals(supplyPlan.getId())) {
            throw new IllegalStateException(
                    "Supply Plan snapshot id must match constrained heuristic execution id "
                            + expectedSupplyPlanId
                            + ".");
        }

    }
    
    /**
     * Atualiza o plano DRP com o que efetivamente pode ser disponibilizado de estoque
     * Calcula fair share da disponibilidade limitada de estoque
     * Atualiza o plano DP com a demanda efetivamente atendida
     */
    private void restringePlano(SupplyPlan supplyPlan, PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) {

        log.info("Extraindo dados para restrição do supply plan " + supplyPlan.getId());
        
        VersaoMalha versaoMalha = supplyPlan.getVersaoMalha();

        ClusterEParametrosProjection clusterEParametrosProjection =
                parametrosProjectionFactory.getParametrosProjectionCompletoDeCache();
               
        ParametrosGlobais parametrosGlobais =
                clusterEParametrosProjection.getParametrosGlobais();
        
        PoliticaEstoquesProjection politicaEstoquesProjection =
                politicaEstoquesProjectionFactory.getPoliticaEstoquesProjection(
                                supplyPlan.getCalendarioDoSupplyPlan(parametrosGlobais),
                                clusterEParametrosProjection,
                                perfilExecucaoSupplyPlan);

        MaterialProjection materialProjection =
                getMaterialProjectionComEscopoEnterprise(
                                perfilExecucaoSupplyPlan,
                                clusterEParametrosProjection,
                                MaterialProjectionFactory.getMaterialProjectionDePerfilExecucaoSupplyPlan(
                                        perfilExecucaoSupplyPlan,
                                        clusterEParametrosProjection));
        LocationProjection locationProjection =
                LocationProjectionFactory.getLocationProjectionDePerfilExecucaoSupplyPlan(
                                perfilExecucaoSupplyPlan, clusterEParametrosProjection);
        
        SupplyNetworkProjection supplyNetworkProjection =
                supplyNetworkProjectionFactory.getSupplyNetworkProjectionCompletoDeCache();

        Calendario calendario = supplyPlan.getCalendarioDoSupplyPlan(parametrosGlobais);
        
        BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva =
                biProjectionCapacidadeProdutivaFactory.getBIProjectionCapacidadeProdutiva(
                                supplyPlan,
                                calendario);

        // GERA MAPA DE DFUS ORDENADOS POR LOW LEVEL CODE ------------------------------------
        LowLevelCode lowLevelCode = new LowLevelCode(
                supplyNetworkProjection, 
                versaoMalha, 
                materialProjection, 
                locationProjection, 
                supplyPlan.getDataInicioPlano());
        lowLevelCode.atualizaMapaDFUsPorLowLevelCode();
        
        restringePlano(
                supplyPlan, 
                perfilExecucaoSupplyPlan, 
                calendario, 
                supplyNetworkProjection, 
                biProjectionCapacidadeProdutiva,
                politicaEstoquesProjection,
                lowLevelCode);
        
    }

    /**
     * Resolve a fotografia de cluster/parametros usada para extrair snapshots
     * do plano restrito.
     *
     * <p>Este caminho privado monta todas as projections consumidas pela
     * sobrecarga projection-aware. A ausencia do snapshot principal precisa
     * falhar aqui, antes de filtros material/location, calendario ou low level
     * code, para nao transformar erro de montagem em NPE interno.</p>
     */
    /**
     * Resolve os parametros globais da fotografia estrutural do plano restrito.
     */
    /**
     * Resolve a politica de estoques usada na restricao heuristica.
     */
    /**
     * Resolve o recorte de materiais do perfil antes do low level code.
     */
    /** Applies the optional private scope while preserving Community fallback. */
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
     * Resolve o recorte de locations do perfil antes do low level code.
     */
    /**
     * Resolve a malha operacional usada pela restricao heuristica.
     */
    /**
     * Resolve a projection de capacidade produtiva simples usada na restricao.
     */
        /**
     * Atualiza o plano DRP com o que efetivamente pode ser disponibilizado de estoque
     * Calcula fair share da disponibilidade limitada de estoque
     * Atualiza o plano DP com a demanda efetivamente atendida
     */
    public void restringePlano(
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            Calendario calendario,
            SupplyNetworkProjection supplyNetworkProjection,
            BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva,
            PoliticaEstoquesProjection politicaEstoquesProjection,
            LowLevelCode lowLevelCode) {

        validaEntradasProjectionAwareConstrainedPlanningCommunity(
                supplyPlan,
                perfilExecucaoSupplyPlan,
                calendario,
                supplyNetworkProjection,
                biProjectionCapacidadeProdutiva,
                politicaEstoquesProjection,
                lowLevelCode);

        log.info("Restringindo plano de supply " + supplyPlan.getId());
        
        ClusterEParametrosProjection clusterEParametrosProjection = supplyNetworkProjection.getClusterEParametrosProjection();
        LocationProjection locationProjection = lowLevelCode.getLocationProjection();
        
        /*
         * Os resets abaixo sao updates massivos de linhas ja calculadas pelo
         * plano heuristico. Falhas de banco sao registradas com contexto do
         * plano; a rotina segue para preservar o comportamento operacional
         * legado de tentar recalcular a restricao a partir do estado existente.
         */
        try {
            productionPlanLinhaRepository.atualizaPlanoRestritoComPlanoIrrestrito(supplyPlan.getId());
        } catch (DataAccessException dataAccessException) {
            log.error("Erro ao realizar reset do plano de producao restrito para o Supply Plan {}", supplyPlan.getId(), dataAccessException);
        }
        try {
            distributionPlanItemRepository.atualizaPlanoRestritoComPlanoIrrestrito(supplyPlan.getId());
        } catch (DataAccessException dataAccessException) {
            log.error("Erro ao realizar reset do plano de distribuicao restrito para o Supply Plan {}", supplyPlan.getId(), dataAccessException);
        }
        if (perfilExecucaoSupplyPlan.getSalvaInventoryPlan()) {
            inventoryPlanLinhaRepository.atualizaPlanoRestritoComPlanoIrrestrito(supplyPlan.getId());
        }
        try {
            demandaDiretaConsideradaLinhaRepository.atualizaPlanoRestritoComPlanoIrrestrito(supplyPlan.getId());
        } catch (DataAccessException dataAccessException) {
            log.error("Erro ao realizar reset da demanda direta considerada restrita para o Supply Plan {}", supplyPlan.getId(), dataAccessException);
        }

        
        int ultimoLowLevelCode = lowLevelCode.getUltimoLowLevelCode().getAsInt();

        // Community nao materializa capacidade logistica por location/deposito nem aplica
        // restricoes de armazenagem, inbound ou outbound. A unica rodada de restricao
        // fisica mantida aqui e a de insumos e capacidade produtiva por horas/dia.
        
        for (int i=calendario.getPosicaoPeriodoPresente(); i <= calendario.getPosicaoPeriodoFinalFuturo(); i++) {
            // guardará o consumo acumulado por recurso/período a cada etapa das iterações
            // importante pois podem haver diversas passagens pelo recurso em diferentes momentos da iteração de DFUs
            // a princípio se assume que o consumo de capacidade acumulado é de 0 horas: o valor é atualizado após cada iteração
            Map<RecursoProdutivo, Map<Integer,Double>> consumoCapacidadeProducaoAcumuladoPorLocationPeriodo = new HashMap<>();
            
            // RODADA FORNECEDORES -> PONTAS(DEMANDA) : restringe com base em insumos, cap. produtiva e cap. outbound
            // restringe outbound
            for (int posicaoLowLevelCode = ultimoLowLevelCode; posicaoLowLevelCode > 0; posicaoLowLevelCode--) {
                log.info("Gerando restrições no sentido fornecedores -> demanda : Low Level Code " + posicaoLowLevelCode + " periodo " + i);

                // um projection é inicializado por location
                for (Location location : lowLevelCode.getLocationsLowLevelCode(posicaoLowLevelCode)) {
                    
                    // cliente final + 'locations cliente propagam demanda' : utiliza método massivo mais adiante
                    // evita para cada cliente extrair DP projection + supply projection
                    if (perfilExecucaoSupplyPlan.getLocationsClienteApenasPropagamDemanda() &&
                            perfilExecucaoSupplyPlan.getModoPropagacaoDemanda().verificaSeRealizaPropagacao(location)) continue;

                    log.info("Restringindo Supply Plan no sentido fornecedores -> demanda " + supplyPlan.getId() + " para Location " + location.getId());

                    // CRIA PROJECTION
                    Set<Produto> produtosLowLevelCodeLocation = lowLevelCode.getMateriaisLowLevelCodeEmLocation(posicaoLowLevelCode, location);
                    MaterialProjection materialProjectionMateriaisLowLevelCodeLocation = MaterialProjectionFactory.getProjectionSetMateriais(
                            produtosLowLevelCodeLocation, clusterEParametrosProjection);

                    SupplyPlanningProjection supplyPlanningProjection = supplyPlanProjectionFactory.getSupplyPlanningProjectionVazio(
                            supplyPlan, 
                            perfilExecucaoSupplyPlan, 
                            location,
                            supplyNetworkProjection, 
                            politicaEstoquesProjection,
                            materialProjectionMateriaisLowLevelCodeLocation,
                            locationProjection);

                    supplyPlanProjectionFactory.populaSupplyPlanningProjectionComInventoryPlan(supplyPlanningProjection);
                    supplyPlanProjectionFactory.populaSupplyPlanningProjectionComProductionPlanOutput(supplyPlanningProjection);
                    supplyPlanProjectionFactory.populaSupplyPlanningProjectionComProductionPlanInput(supplyPlanningProjection);
                    supplyPlanProjectionFactory.populaSupplyPlanningProjectionComDistributionPlanInbound(supplyPlanningProjection);
                    supplyPlanProjectionFactory.populaSupplyPlanningProjectionComDistributionPlanOutbound(supplyPlanningProjection);
                    supplyPlanProjectionFactory.populaSupplyPlanningProjectionComDemandaDiretaConsideradaProjection(supplyPlanningProjection);
                    // popula com estoque dos insumos
                    supplyPlanProjectionFactory.populaSupplyPlanningProjectionComInventoryPlanDeInputsProducao(supplyPlanningProjection, i);
                    
                    // se não há inventory plan salvo (nesse caso só teremos a posição inicial de estoque salva)
                    // será necessário recalcular estoques
                    if (!perfilExecucaoSupplyPlan.getSalvaInventoryPlan()) {
                        SupplyPlanning.atualizaEstoqueProjetadoSemLimitarAZero(
                                supplyPlanningProjection,
                                Constantes.TipoPlano.PLANO_RESTRITO);
                    }
                    
                    // RESTRIÇÕES PRODUÇÃO : INSUMOS E CAPACIDADE
                    boolean restricaoInsumosAplicada = false;
                    boolean restricaoProducaoAplicada = false;
                                        
                    if (perfilExecucaoSupplyPlan.getConsideraRestricaoProducao(location)) {
                        
                        // 100% dos materiais, não apenas os materiais low level code atual
                        Set<Produto> todosMateriaisAtivosLocation = lowLevelCode
                                .getMaterialProjection()
                                .getMateriaisAtivosEmLocation(location);
                        
                        // cria novo supply planning projection específico para insumos
                        SupplyPlanningProjection supplyPlanningProjectionInsumos = supplyPlanProjectionFactory.getSupplyPlanningProjectionVazio(
                                supplyPlan, 
                                perfilExecucaoSupplyPlan,
                                location, 
                                supplyNetworkProjection, 
                                politicaEstoquesProjection,
                                MaterialProjectionFactory.getProjectionSetMateriais(todosMateriaisAtivosLocation, clusterEParametrosProjection), // 100% dos materiais, não apenas os materiais low level code atual
                                locationProjection);
                        
                        supplyPlanProjectionFactory.populaSupplyPlanningProjectionComInventoryPlan(supplyPlanningProjectionInsumos);
                        supplyPlanProjectionFactory.populaSupplyPlanningProjectionComProductionPlanOutput(supplyPlanningProjectionInsumos);
                        supplyPlanProjectionFactory.populaSupplyPlanningProjectionComProductionPlanInput(supplyPlanningProjectionInsumos);
                        supplyPlanProjectionFactory.populaSupplyPlanningProjectionComDistributionPlanInbound(supplyPlanningProjectionInsumos);
                        supplyPlanProjectionFactory.populaSupplyPlanningProjectionComDistributionPlanOutbound(supplyPlanningProjectionInsumos);
                        /*
                         * A projection auxiliar de insumos tambem projeta
                         * estoque. Ela precisa carregar a demanda direta
                         * considerada propria; popular apenas a projection
                         * principal deixa o calculo de saldo sem a serie de
                         * demanda para esses materiais.
                         */
                        supplyPlanProjectionFactory.populaSupplyPlanningProjectionComDemandaDiretaConsideradaProjection(supplyPlanningProjectionInsumos);
                        
                        // se não há inventory plan salvo (nesse caso só teremos a posição inicial de estoque salva)
                        // será necessário recalcular estoques
                        if (!perfilExecucaoSupplyPlan.getSalvaInventoryPlan()) {
                            SupplyPlanning.atualizaEstoqueProjetadoSemLimitarAZero(
                                    supplyPlanningProjectionInsumos,
                                    Constantes.TipoPlano.PLANO_RESTRITO);
                        }
                                        
                        // RESTRINGE ORDENS PRODUCAO COM BASE NA DISPONIBILIDADE DE INSUMOS
                        log.debug("Restringindo production plan com base em restrições de insumos na location " + location.getId());
                        restricaoInsumosAplicada = ConstrainedPlanningHeuristicoRotinas.restringeSugestoesEOrdensProducaoNoProjectionPorDisponibilidadeInsumos(
                                supplyPlanningProjection,
                                supplyPlanningProjectionInsumos,
                                i);
                        
                        // RESTRINGE ORDENS PRODUCAO COM BASE NA CAPACIDADE PRODUTIVA
                        log.debug("Restringindo production plan com base em restrições de capacidade produtiva na location " + location.getId());
                        restricaoProducaoAplicada = ConstrainedPlanningHeuristicoRotinas.restringeSugestoesEOrdensProducaoNoProjectionPorCapacidadeProdutiva(
                                supplyPlanningProjection,
                                biProjectionCapacidadeProdutiva,
                                i, 
                                consumoCapacidadeProducaoAcumuladoPorLocationPeriodo);
                    }
                    
                    // RESTRINGE REQUISICOES E PEDIDOS OUTBOUND
                    // RESTRINGE DEMAND PLAN
                    boolean restricaoDistribuicaoAplicada = ConstrainedPlanningHeuristicoRotinas.restringeDistributionPlanOutboundEDemandPlan(
                            supplyPlanningProjection,
                            i);
                    
                    // LINHA PARA GANHO DE DESEMPENHO REMOVIDA : IGNORA SITUACOES ONDE NAO HOUVE RESTRICAO POR CAPACIDADE MAS 
                    // PRODUCAO FOI AJUSTADA MANUALMENTE PARA BAIXO : NESSES CASOS PODERÁ HAVER RESTRICAO OUTBOUND
                    
                    // quando se corta produção os insumos deixam de ser consumidos : criam-se novos projections apenas para insumos
                    // de ordens firmes/planejadas que sofreram restrição e reprojetam-se os estoques
                    if (restricaoProducaoAplicada && perfilExecucaoSupplyPlan.getSalvaInventoryPlan()) {
                        // Gerar supply planning projection para insumos : corte de 
                        Set<Produto> materiaisInputEmProducaoComRestricao = supplyPlanningProjection.getProductionPlanLinhaOutput(i).stream()
                                .filter(x -> x.getQuantidadeOrdemFirmeProducaoRestrita() + x.getQuantidadeOrdemPlanejadaProducaoRestrita()
                                        < x.getQuantidadeOrdemFirmeProducaoIrrestrita() + x.getQuantidadeOrdemPlanejadaProducaoIrrestrita())
                                .map(x -> x.getMateriaisInput(supplyNetworkProjection))
                                .flatMap(x -> x.stream())
                                .collect(Collectors.toSet());
                        MaterialProjection materialProjectionMateriaisInputEmProducaoComRestricao = MaterialProjectionFactory.getProjectionSetMateriais(
                                materiaisInputEmProducaoComRestricao, clusterEParametrosProjection);
                        
                        SupplyPlanningProjection supplyPlanningProjectionMateriaisInput = supplyPlanProjectionFactory.getSupplyPlanningProjectionVazio(
                            supplyPlan, 
                            perfilExecucaoSupplyPlan, 
                            location, 
                            supplyNetworkProjection, 
                            politicaEstoquesProjection,
                            materialProjectionMateriaisInputEmProducaoComRestricao,
                            locationProjection);
                        
                        supplyPlanProjectionFactory.populaSupplyPlanningProjectionComInventoryPlan(supplyPlanningProjectionMateriaisInput);
                        supplyPlanProjectionFactory.populaSupplyPlanningProjectionComProductionPlanOutput(supplyPlanningProjectionMateriaisInput);
                        supplyPlanProjectionFactory.populaSupplyPlanningProjectionComProductionPlanInput(supplyPlanningProjectionMateriaisInput);
                        supplyPlanProjectionFactory.populaSupplyPlanningProjectionComDistributionPlanInbound(supplyPlanningProjectionMateriaisInput);
                        supplyPlanProjectionFactory.populaSupplyPlanningProjectionComDistributionPlanOutbound(supplyPlanningProjectionMateriaisInput);
                        /*
                         * Esta projection e recriada depois de cortes de
                         * producao para recalcular o estoque dos insumos.
                         * Portanto ela, e nao apenas a projection principal,
                         * precisa da demanda direta considerada carregada.
                         */
                        supplyPlanProjectionFactory.populaSupplyPlanningProjectionComDemandaDiretaConsideradaProjection(supplyPlanningProjectionMateriaisInput);
                        
                        SupplyPlanning.atualizaEstoqueProjetadoSemLimitarAZero(
                                supplyPlanningProjectionMateriaisInput,
                                Constantes.TipoPlano.PLANO_RESTRITO);
                        
                        if (perfilExecucaoSupplyPlan.getSalvaInventoryPlan()) {
                            supplyPlanService.saveInventoryPlanDePlanningProjection(supplyPlanningProjectionMateriaisInput, i, false);
                        }
                        
                    }

                    // após restrições, caso backlog não seja permitido limita o estoque a 0 (remoção estoques negativos)
                    if (!perfilExecucaoSupplyPlan.getPermiteBacklogDemanda()) {
                        SupplyPlanning.limitaEstoquesNegativosAZero(Constantes.TipoPlano.PLANO_RESTRITO, supplyPlanningProjection);
                    }
        
                    log.debug("Salvando plano restrito na location " + location.getId());
                    // Salva elementos do supply plan : distribution plan , production plan e inventory plan
                    supplyPlanService.saveProductionPlanOutputDePlanningProjection(supplyPlanningProjection, i, false);
                    supplyPlanService.saveDistributionPlanOutboundDePlanningProjection(supplyPlanningProjection, i, false);
                    if (perfilExecucaoSupplyPlan.getSalvaInventoryPlan()) {
                        supplyPlanService.saveInventoryPlanDePlanningProjection(supplyPlanningProjection, i, false);
                    }
                    // Salva demanda direta considerando restrições atualizadas
                    supplyPlanService.saveDemandaDiretaConsideradaLinhaDeProjection(supplyPlanningProjection, i, false);
                }
            }
        }
        
        // caso especificado no perfil execução, copia o supply plan restrito no supply plan irrestrito
        // como ajustes na linha de supply adjustment. O demand plan não é modificado
        if (perfilExecucaoSupplyPlan.getTipoPlanoTrabalho().equals(Constantes.TipoPlano.PLANO_RESTRITO)) {
            log.info("Copiando valores do plano restrito de supply no plano de trabalho como ajuste supply");
            distributionPlanItemRepository.atualizaOrdensPlanejadasPlanoTrabalhoComPlanoRestrito(supplyPlan.getId());
            distributionPlanItemRepository.atualizaOrdensFirmesPlanoTrabalhoComPlanoRestrito(supplyPlan.getId());
            productionPlanLinhaRepository.atualizaOrdensPlanejadasPlanoTrabalhoComPlanoRestrito(supplyPlan.getId());
            productionPlanLinhaRepository.atualizaOrdensFirmesPlanoTrabalhoComPlanoRestrito(supplyPlan.getId());
            if (perfilExecucaoSupplyPlan.getSalvaInventoryPlan()) {
                inventoryPlanLinhaRepository.atualizaInventoryPlanTrabalhoComPlanoRestrito(supplyPlan.getId());
            }
        }

        /*
         * A explicabilidade de restricoes deixou de ser gerada dentro das
         * etapas intermediarias do heuristico. A leitura causal atual roda no
         * pos-processamento do SupplyPlanService, usando os outputs finais do
         * plano e gravando impactos conclusivos de restricao -> demanda.
         */
        
        log.info("Plano restrito gerado");
    }

    /**
     * Valida a entrada projection-aware do plano restrito Community.
     *
     * <p>Esta sobrecarga e chamada tanto pelo heuristico Community quanto por
     * fluxos Enterprise que reaproveitam o calculo restrito quantitativo. Por
     * isso a ordem das validacoes e parte do contrato: primeiro bloqueamos
     * perfil/motor Enterprise, depois exigimos os snapshots ja materializados
     * que esta rotina efetivamente consome. Assim payloads Enterprise falham
     * por edicao antes de qualquer projection, enquanto snapshots quebrados de
     * uma rodada heuristica falham com mensagem funcional antes dos updates em
     * lote ou loops por low level code.</p>
     */
    private void validaEntradasProjectionAwareConstrainedPlanningCommunity(
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            Calendario calendario,
            SupplyNetworkProjection supplyNetworkProjection,
            BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva,
            PoliticaEstoquesProjection politicaEstoquesProjection,
            LowLevelCode lowLevelCode) {

        if (supplyPlan == null) {
            throw new IllegalArgumentException(
                    "Supply Plan is required for constrained heuristic execution.");
        }
        validaPerfilHeuristicoConstrainedPlanningCommunity(perfilExecucaoSupplyPlan);
        validaRestricoesLogisticasCommunity(perfilExecucaoSupplyPlan);

        if (calendario == null) {
            throw new IllegalArgumentException(
                    "Calendar is required for constrained heuristic execution.");
        }
        if (supplyNetworkProjection == null) {
            throw new IllegalArgumentException(
                    "Supply Network projection is required for constrained heuristic execution.");
        }
        if (supplyNetworkProjection.getClusterEParametrosProjection() == null) {
            throw new IllegalArgumentException(
                    "Cluster and parameters projection is required for constrained heuristic execution.");
        }
        if (lowLevelCode == null) {
            throw new IllegalArgumentException(
                    "Low level code projection is required for constrained heuristic execution.");
        }
        if (lowLevelCode.getLocationProjection() == null) {
            throw new IllegalArgumentException(
                    "Location projection from low level code is required for constrained heuristic execution.");
        }
        if (biProjectionCapacidadeProdutiva == null) {
            throw new IllegalArgumentException(
                    "Production capacity projection is required for constrained heuristic execution.");
        }
        if (politicaEstoquesProjection == null) {
            throw new IllegalArgumentException(
                    "Inventory policy projection is required for constrained heuristic execution.");
        }

    }

    /**
     * Bloqueia qualquer tentativa de executar restricoes logisticas fisicas no
     * Community. A API de configuracao ja normaliza esses campos para false,
     * mas esta barreira fica no runtime para proteger perfis antigos,
     * payloads migrados manualmente ou execucoes disparadas fora do fluxo
     * padrao de tela.
     *
     * <p>O heuristico Community continua calculando disponibilidade,
     * propagacao, insumos, fair share e capacidade produtiva. Armazenagem,
     * inbound/outbound logisticos, nivel de capacidade logistica, deposito e
     * capacidade por data pertencem ao Enterprise porque dependem de tabelas e
     * projections removidas deste modulo.</p>
     */
    private void validaRestricoesLogisticasCommunity(PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) {

        boolean possuiRestricaoLogistica = perfilExecucaoSupplyPlan.getConsideraRestricaoArmazenagem()
                || perfilExecucaoSupplyPlan.getConsideraRestricaoInbound()
                || perfilExecucaoSupplyPlan.getConsideraRestricaoOutbound()
                || perfilExecucaoSupplyPlan.getTipoCapacidadeLogisticaCadastrado() != null;

        if (possuiRestricaoLogistica) {
            throw new RequiresEnterpriseVersionException("Supply Planning logistics/storage constraints");
        }

    }
    
}
