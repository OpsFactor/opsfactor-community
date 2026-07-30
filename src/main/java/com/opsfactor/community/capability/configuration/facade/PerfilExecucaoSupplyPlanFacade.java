package com.opsfactor.community.capability.configuration.facade;

import com.opsfactor.community.capability.supplyplanning.configuration.facade.dto.PerfilExecucaoSupplyPlanDTO;
import com.opsfactor.community.capability.supplyplanning.configuration.facade.mapper.PerfilExecucaoSupplyPlanAutoMapper;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoPoliticaEstoques;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoques;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.repository.PoliticaEstoquesRepository;
import com.opsfactor.community.capability.supplyplanning.configuration.repository.PerfilExecucaoSupplyPlanRepository;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service front do perfil de execucao Supply Planning Community.
 *
 * <p>Esta classe e a borda de validacao para o DTO compartilhado com o front.
 * Campos Enterprise podem existir no payload para compatibilidade visual, mas
 * qualquer tentativa de habilitar otimizador, process chain, custos, P&L,
 * frotas, filtros, pedidos transacionais, line scheduling ou restricoes
 * logisticas deve falhar antes da persistencia.</p>
 */
@Service
public class PerfilExecucaoSupplyPlanFacade {

    private static final String SUPPLY_PLANNING_LOCATION_LEVEL_PROFILE = "Supply Planning location-level execution profile";

    /**
     * Repositorio Community do perfil de execucao. Deve permanecer explicito
     * como bean porque esta classe e uma borda de persistencia do cadastro e
     * nao apenas um validador estatico de DTO.
     */
    @Autowired
    private PerfilExecucaoSupplyPlanRepository perfilExecucaoSupplyPlanRepository;

    /**
     * Mapper compartilhado entre tela e entidade Community. A validacao de
     * campos Enterprise sempre acontece antes da chamada a este mapper para
     * evitar materializar estado fora do recorte publico.
     */
    @Autowired
    private PerfilExecucaoSupplyPlanAutoMapper perfilExecucaoSupplyPlanAutoMapper;

    /**
     * Repositorio das politicas de estoque associadas ao perfil. Estoque de
     * seguranca existe no Community; apenas a otimizacao da politica fica no
     * Enterprise.
     */
    @Autowired
    private PoliticaEstoquesRepository politicaEstoquesRepository;

    /**
     * Carrega os perfis Community ja persistidos e os converte para o DTO
     * consumido pelo front compartilhado.
     *
     * <p>Campos Enterprise podem existir no contrato JSON, mas nao sao
     * retornados como configuracoes habilitadas pelo backend Community.</p>
     */
    public Set<PerfilExecucaoSupplyPlanDTO> getPerfilExecucaoSupplyPlanDTOSet() {

        List<PerfilExecucaoSupplyPlan> perfilExecucaoSupplyPlanList =
                perfilExecucaoSupplyPlanRepository.customFindAll();
        validaPerfilExecucaoSupplyPlanListCarregadoCommunity(perfilExecucaoSupplyPlanList);

        return convertePerfilExecucaoSupplyPlanDTOSetCommunity(perfilExecucaoSupplyPlanList);

    }

    /**
     * Converte a lista de entidades para DTO e valida o snapshot efetivamente
     * devolvido ao front Community.
     *
     * <p>A validacao anterior protege o dado carregado do repository. Esta
     * etapa protege a borda de API contra mapper/stub/overlay quebrado que
     * reintroduza campos Enterprise no DTO final, em especial Optimizer ou
     * Process Chain no seletor de execution engine.</p>
     */
    private Set<PerfilExecucaoSupplyPlanDTO> convertePerfilExecucaoSupplyPlanDTOSetCommunity(
            List<PerfilExecucaoSupplyPlan> perfilExecucaoSupplyPlanList) {

        List<PerfilExecucaoSupplyPlanDTO> perfilExecucaoSupplyPlanDTOList =
                perfilExecucaoSupplyPlanAutoMapper.converteListaEntidadesParaDTOs(
                        perfilExecucaoSupplyPlanList);
        validaPerfilExecucaoSupplyPlanDTOListListagemCommunity(perfilExecucaoSupplyPlanDTOList);
        return new LinkedHashSet<>(perfilExecucaoSupplyPlanDTOList);

    }

    /**
     * Persiste um perfil Community depois de bloquear qualquer parametro que
     * dependa de otimizador, process chain, custos, pedidos transacionais,
     * filtros/agregadores de material ou restricoes logisticas.
     *
     * <p>O DTO e compartilhado com a edicao Enterprise, entao a primeira parte
     * deste metodo e deliberadamente verbosa: cada grupo funcional falha antes
     * de consultar repositorios ou chamar o mapper. Isso deixa claro para quem
     * mantiver o Community quais campos sao apenas compatibilidade de API
     * compartilhada com Enterprise e quais entram de fato no cadastro
     * Community.</p>
     */
    public void savePerfilExecucaoSupplyPlanDTO(PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO) {

        validaPayloadPerfilExecucaoSupplyPlanCommunity(perfilExecucaoSupplyPlanDTO);
        validaPoliticasEstoqueOperacionaisCommunity(perfilExecucaoSupplyPlanDTO);

        /*
         * As validacoes Community usam apenas o DTO recebido e precisam acontecer
         * antes de qualquer mapper/repository. Assim payloads Enterprise enviados
         * manualmente falham com RequiresEnterpriseVersionException sem consultar
         * politicas de estoque nem materializar entidade transicional.
         */
        validaCurvasCustoLogisticoCommunity(perfilExecucaoSupplyPlanDTO);
        validaFrotasEOtimizadorInteligenciaArtificialCommunity(perfilExecucaoSupplyPlanDTO);
        validaGreenfieldCommunity(perfilExecucaoSupplyPlanDTO);
        validaCurvasSplitTemporalCommunity(perfilExecucaoSupplyPlanDTO);
        validaFiltroMateriaisCommunity(perfilExecucaoSupplyPlanDTO);
        validaModoExecucaoCommunity(perfilExecucaoSupplyPlanDTO);
        validaPerfilLocationLevelCommunity(perfilExecucaoSupplyPlanDTO);
        validaParametrosModeloOtimizadoCommunity(perfilExecucaoSupplyPlanDTO);
        validaPedidosTransacionaisCommunity(perfilExecucaoSupplyPlanDTO);
        validaCapacidadesEConstraintsCommunity(perfilExecucaoSupplyPlanDTO);

        List<PoliticaEstoques> politicaEstoquesList = politicaEstoquesRepository.findAll();
        validaSnapshotPoliticasEstoqueCommunity(politicaEstoquesList);
        Map<String,PoliticaEstoques> mapaPoliticasEstoques = politicaEstoquesList.stream()
                .collect(Collectors.toMap(x -> x.getId(), x -> x));
        Set<PoliticaEstoques> politicaEstoquesSet = perfilExecucaoSupplyPlanDTO.getInventoryPolicyIdSet().stream()
                .map(politicaEstoquesId -> getPoliticaEstoquesCommunity(
                        politicaEstoquesId,
                        mapaPoliticasEstoques))
                .collect(Collectors.toSet());

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = perfilExecucaoSupplyPlanAutoMapper.converte(perfilExecucaoSupplyPlanDTO);
        /*
         * O modo de execucao nao e estado persistido no Community. A validacao
         * acima bloqueia payload Enterprise e o getter da entidade fixa o
         * runtime em HEURISTICO.
         */
        normalizaParametrosModeloOtimizadoCommunity(perfilExecucaoSupplyPlan);
        normalizaPedidosTransacionaisCommunity(perfilExecucaoSupplyPlan);
        normalizaCapacidadesEConstraintsCommunity(perfilExecucaoSupplyPlan);
        normalizaFairShareCommunity(perfilExecucaoSupplyPlan);
        for (PoliticaEstoques politicaEstoques : politicaEstoquesSet) {
            perfilExecucaoSupplyPlan.getSetPerfilExecucaoPoliticaEstoques().add(
                    new PerfilExecucaoPoliticaEstoques(
                            new PerfilExecucaoPoliticaEstoques.PerfilExecucaoPoliticaEstoquesCompositeKey(
                                    perfilExecucaoSupplyPlan, politicaEstoques)));
        }

        /*
         * Mesmo sem retorno para o controller, validamos o snapshot salvo para
         * diferenciar sucesso real de persistencia quebrada. A tela Community
         * assume que o cadastro passou a existir apos esta chamada; retornar
         * silenciosamente com entity nula, sem id ou com modo Enterprise
         * reintroduzido pelo mapper/repository deixaria a falha aparecer apenas
         * na proxima execucao de Supply Planning.
         */
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlanSalvo =
                perfilExecucaoSupplyPlanRepository.save(perfilExecucaoSupplyPlan);
        validaPerfilExecucaoSupplyPlanSalvoCommunity(perfilExecucaoSupplyPlanSalvo);

    }

    /**
     * Valida a chave minima do perfil antes das travas Enterprise e antes de
     * qualquer mapper/repository.
     *
     * <p>O DTO e compartilhado com o front Enterprise, mas o Community ainda
     * precisa de um id local para persistir ou atualizar o perfil. Payload nulo
     * ou sem id e erro de request, nao ausencia de capability Enterprise.</p>
     */
    private void validaPayloadPerfilExecucaoSupplyPlanCommunity(
            PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO) {

        if (perfilExecucaoSupplyPlanDTO == null) {
            throw new IllegalArgumentException("Supply Planning execution profile DTO is required.");
        }
        if (perfilExecucaoSupplyPlanDTO.getId() == null || perfilExecucaoSupplyPlanDTO.getId().isBlank()) {
            throw new IllegalArgumentException("Supply Planning execution profile id is required.");
        }
        /*
         * Estes campos sao configuracoes operacionais Community, nao parametros
         * Enterprise. `null` preserva os defaults historicos da entidade ou dos
         * parametros globais; valor preenchido precisa ser positivo para nao
         * ser mascarado pelos getters com `Math.max(1, ...)` ou por fallback de
         * horizonte global.
         */
        validaInteiroOperacionalPositivo(
                perfilExecucaoSupplyPlanDTO.getPlanHorizonInDays(),
                "Supply Planning execution profile plan horizon");
        validaInteiroOperacionalPositivo(
                perfilExecucaoSupplyPlanDTO.getExpeditionPeriodsToRoundRequisitionsByMoqAndLotSize(),
                "Supply Planning requisition rounding expedition window");
        validaInteiroOperacionalPositivo(
                perfilExecucaoSupplyPlanDTO.getPeriodsToRoundProductionByMoqAndLotSize(),
                "Supply Planning production rounding window");

    }

    /**
     * Valida o snapshot de perfis carregado para a listagem Community.
     *
     * <p>Lista vazia e valida para uma instalacao nova. Cada perfil existente
     * precisa ter id e modo efetivamente heuristico; perfis Optimizer ou
     * Process Chain pertencem ao Enterprise e nao devem ser mascarados como
     * selecionaveis pelo front Community.</p>
     */
    private void validaPerfilExecucaoSupplyPlanListCarregadoCommunity(
            List<PerfilExecucaoSupplyPlan> perfilExecucaoSupplyPlanList) {

        if (perfilExecucaoSupplyPlanList == null) {
            throw new IllegalStateException("Supply Planning execution profile list snapshot is required.");
        }

        Set<String> perfilExecucaoSupplyPlanIds = new HashSet<>();
        int index = 0;
        for (PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan : perfilExecucaoSupplyPlanList) {
            if (perfilExecucaoSupplyPlan == null) {
                throw new IllegalStateException(
                        "Supply Planning execution profile at index " + index + " is required in list snapshot.");
            }
            if (perfilExecucaoSupplyPlan.getId() == null
                    || perfilExecucaoSupplyPlan.getId().isBlank()) {
                throw new IllegalStateException(
                        "Supply Planning execution profile at index " + index + " has no id in list snapshot.");
            }
            if (!perfilExecucaoSupplyPlanIds.add(perfilExecucaoSupplyPlan.getId())) {
                throw new IllegalStateException(
                        "Supply Planning execution profile list snapshot has duplicated id "
                                + perfilExecucaoSupplyPlan.getId()
                                + ".");
            }
            if (!PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO.equals(
                    perfilExecucaoSupplyPlan.getModoExecucao())) {
                throw new IllegalStateException(
                        "Supply Planning execution profile at index " + index + " must be heuristic in Community.");
            }
            index++;
        }

    }

    /**
     * Valida os DTOs ja mapeados para a listagem do front.
     *
     * <p>A listagem Community deve retornar sempre profiles identificaveis,
     * com modo de execucao explicitamente heuristico e sem campos Enterprise
     * ativados. Diferente do save, aqui `executionModel` nulo e considerado
     * snapshot quebrado: a tela compartilhada usa esse valor para renderizar a
     * selecao atual e marcar Optimizer/Process Chain como opcoes bloqueadas.</p>
     */
    private void validaPerfilExecucaoSupplyPlanDTOListListagemCommunity(
            List<PerfilExecucaoSupplyPlanDTO> perfilExecucaoSupplyPlanDTOList) {

        if (perfilExecucaoSupplyPlanDTOList == null) {
            throw new IllegalStateException("Supply Planning execution profile DTO list snapshot is required.");
        }

        Set<String> perfilExecucaoSupplyPlanDTOIds = new HashSet<>();
        int index = 0;
        for (PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO : perfilExecucaoSupplyPlanDTOList) {
            if (perfilExecucaoSupplyPlanDTO == null) {
                throw new IllegalStateException(
                        "Supply Planning execution profile DTO at index " + index + " is required in list snapshot.");
            }
            if (perfilExecucaoSupplyPlanDTO.getId() == null
                    || perfilExecucaoSupplyPlanDTO.getId().isBlank()) {
                throw new IllegalStateException(
                        "Supply Planning execution profile DTO at index " + index + " has no id in list snapshot.");
            }
            if (!perfilExecucaoSupplyPlanDTOIds.add(perfilExecucaoSupplyPlanDTO.getId())) {
                throw new IllegalStateException(
                        "Supply Planning execution profile DTO list snapshot has duplicated id "
                                + perfilExecucaoSupplyPlanDTO.getId()
                                + ".");
            }
            if (!PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO.equals(
                    perfilExecucaoSupplyPlanDTO.getExecutionModel())) {
                throw new IllegalStateException(
                        "Supply Planning execution profile DTO at index " + index + " must be heuristic in Community.");
            }
            if (perfilExecucaoSupplyPlanDTO.getInventoryPolicyIdSet() == null) {
                throw new IllegalStateException(
                        "Supply Planning execution profile DTO at index "
                                + index
                                + " must expose an inventory policy id set in Community.");
            }

            /*
             * Reutilizamos as mesmas travas do save para garantir que o DTO de
             * saida nao exponha configuracoes Enterprise como selecionadas. Elas
             * aceitam valores neutros/null/false, preservando os campos que o
             * front precisa mostrar como bloqueados.
             */
            validaCurvasCustoLogisticoCommunity(perfilExecucaoSupplyPlanDTO);
            validaFrotasEOtimizadorInteligenciaArtificialCommunity(perfilExecucaoSupplyPlanDTO);
            validaGreenfieldCommunity(perfilExecucaoSupplyPlanDTO);
            validaCurvasSplitTemporalCommunity(perfilExecucaoSupplyPlanDTO);
            validaFiltroMateriaisCommunity(perfilExecucaoSupplyPlanDTO);
            validaModoExecucaoCommunity(perfilExecucaoSupplyPlanDTO);
            validaPerfilLocationLevelCommunity(perfilExecucaoSupplyPlanDTO);
            validaParametrosModeloOtimizadoCommunity(perfilExecucaoSupplyPlanDTO);
            validaPedidosTransacionaisCommunity(perfilExecucaoSupplyPlanDTO);
            validaCapacidadesEConstraintsCommunity(perfilExecucaoSupplyPlanDTO);
            index++;
        }

    }

    /**
     * Valida o contrato operacional das politicas de estoque associadas ao
     * perfil.
     *
     * <p>Safety stock operacional existe no Community, entao uma lista vazia de
     * politicas e um snapshot valido. Valor nulo, por outro lado, nao tem
     * semantica funcional: ele normalmente indica payload manual/incompleto e
     * deve falhar antes de consultar {@link #politicaEstoquesRepository} ou
     * materializar a entidade pelo mapper.</p>
     */
    private void validaPoliticasEstoqueOperacionaisCommunity(
            PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO) {

        if (perfilExecucaoSupplyPlanDTO.getInventoryPolicyIdSet() == null) {
            throw new IllegalArgumentException("Supply Planning inventory policy id set must be provided");
        }

    }

    /**
     * Valida o snapshot de politicas antes de criar o mapa por id.
     *
     * <p>O conjunto de politicas pode ser vazio, mas a lista carregada precisa
     * ser estruturalmente valida. Sem esta guarda, item nulo, id ausente ou id
     * duplicado virariam erro opaco dentro de `Collectors.toMap` ou chave
     * composta quebrada no vinculo do perfil com safety stock.</p>
     */
    private void validaSnapshotPoliticasEstoqueCommunity(
            List<PoliticaEstoques> politicaEstoquesList) {

        if (politicaEstoquesList == null) {
            throw new IllegalStateException(
                    "Supply Planning inventory policy repository returned null list.");
        }

        Set<String> politicaEstoquesIds = new HashSet<>();
        for (int index = 0; index < politicaEstoquesList.size(); index++) {
            PoliticaEstoques politicaEstoques = politicaEstoquesList.get(index);
            if (politicaEstoques == null) {
                throw new IllegalStateException(
                        "Supply Planning inventory policy repository returned null item at index "
                                + index
                                + ".");
            }
            if (politicaEstoques.getId() == null || politicaEstoques.getId().isBlank()) {
                throw new IllegalStateException(
                        "Supply Planning inventory policy repository returned item without id at index "
                                + index
                                + ".");
            }
            if (!politicaEstoquesIds.add(politicaEstoques.getId())) {
                throw new IllegalStateException(
                        "Supply Planning inventory policy repository returned duplicate id "
                                + politicaEstoques.getId()
                                + ".");
            }
        }

    }

    /**
     * Resolve a politica de estoque Community referenciada pelo perfil.
     *
     * <p>Safety stock operacional existe na edicao Community, portanto ids de
     * politica de estoque sao dados funcionais validos. Ao mesmo tempo, o
     * vinculo do perfil usa uma chave composta com a entidade gerenciada:
     * aceitar id nulo, vazio ou inexistente aqui empurraria o erro para uma
     * excecao JPA tardia e pouco diagnostica. Falhamos antes de criar a chave
     * composta para deixar claro que o payload aponta para cadastro ausente.</p>
     */
    private PoliticaEstoques getPoliticaEstoquesCommunity(
            String politicaEstoquesId,
            Map<String, PoliticaEstoques> mapaPoliticasEstoques) {

        if (politicaEstoquesId == null || politicaEstoquesId.isBlank()) {
            throw new IllegalArgumentException("Supply Planning inventory policy id is required.");
        }

        PoliticaEstoques politicaEstoques = mapaPoliticasEstoques.get(politicaEstoquesId);
        if (politicaEstoques == null) {
            throw new IllegalArgumentException(
                    "Supply Planning inventory policy not found: " + politicaEstoquesId);
        }

        return politicaEstoques;

    }

    /**
     * Valida o snapshot salvo pelo repository antes de considerar a operacao
     * concluida para o front.
     *
     * <p>O Community pode manter campos Enterprise na entidade por
     * compatibilidade de schema, mas um perfil salvo pela borda Community deve
     * continuar identificavel e efetivamente heuristico. Se um mapper, overlay
     * acidental ou stub quebrado devolver outra coisa, falhamos aqui em vez de
     * deixar o erro surgir tardiamente no runtime de Supply Planning.</p>
     */
    private void validaPerfilExecucaoSupplyPlanSalvoCommunity(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlanSalvo) {

        if (perfilExecucaoSupplyPlanSalvo == null) {
            throw new IllegalStateException(
                    "Saved Supply Planning execution profile snapshot is required after Community profile persistence.");
        }
        if (perfilExecucaoSupplyPlanSalvo.getId() == null
                || perfilExecucaoSupplyPlanSalvo.getId().isBlank()) {
            throw new IllegalStateException(
                    "Saved Supply Planning execution profile id is required after Community profile persistence.");
        }
        if (!PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO.equals(
                perfilExecucaoSupplyPlanSalvo.getModoExecucao())) {
            throw new IllegalStateException(
                    "Saved Supply Planning execution profile must remain heuristic in Community.");
        }

    }

    /**
     * Community nao permite selecionar curvas de custo logistico nem ativar sua
     * aplicacao no perfil de execucao. Esses campos ficam reservados ao
     * Enterprise, junto com custos, P&L e cost-to-serve.
     */
    private void validaCurvasCustoLogisticoCommunity(PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO) {

        if (perfilExecucaoSupplyPlanDTO.getLogisticsCostCurvesId() != null
                || Boolean.TRUE.equals(perfilExecucaoSupplyPlanDTO.getApplyFreightCostCurves())
                || Boolean.TRUE.equals(perfilExecucaoSupplyPlanDTO.getApplyLocationCostCurves())) {
            throw new RequiresEnterpriseVersionException("Logistics cost curves");
        }

    }

    /**
     * Community nao permite AI optimizer nem alocacao de transferencias em
     * frotas. A validacao usa o DTO para capturar tentativas vindas do front
     * antes de normalizar os campos persistidos para os defaults Community.
     */
    private void validaFrotasEOtimizadorInteligenciaArtificialCommunity(PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO) {

        if (perfilExecucaoSupplyPlanDTO.getAiOptimizer() != null
                && !PerfilExecucaoSupplyPlan.OtimizadorInteligenciaArtificial.DESABILITADO
                .equals(perfilExecucaoSupplyPlanDTO.getAiOptimizer())) {
            throw new RequiresEnterpriseVersionException("AI optimizer");
        }
        if (Boolean.TRUE.equals(perfilExecucaoSupplyPlanDTO.getAllocateTransfersInFleets())) {
            throw new RequiresEnterpriseVersionException("Fleet allocation");
        }

    }

    /**
     * Community nao permite budget de ativacao greenfield. A entidade mantem
     * campos compartilhados temporariamente, mas a borda Community deve falhar
     * se receber configuracao Enterprise e deve persistir os defaults neutros.
     */
    private void validaGreenfieldCommunity(PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO) {

        if (Boolean.TRUE.equals(perfilExecucaoSupplyPlanDTO.getConsiderBudgetForGreenfieldLocationActivation())
                || perfilExecucaoSupplyPlanDTO.getGreenfieldLocationActivationBudget() != null) {
            throw new RequiresEnterpriseVersionException("Greenfield location activation");
        }

    }

    /**
     * Curvas de split temporal sao usadas no Enterprise para redistribuir
     * demanda ao longo do horizonte. O Community deve trabalhar apenas com a
     * distribuicao padrao do fluxo base, sem selecao de curvas no perfil.
     */
    private void validaCurvasSplitTemporalCommunity(PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO) {

        if (perfilExecucaoSupplyPlanDTO.getTemporalSplitCurveIdSet() != null
                && !perfilExecucaoSupplyPlanDTO.getTemporalSplitCurveIdSet().isEmpty()) {
            throw new RequiresEnterpriseVersionException("Temporal split curves");
        }

    }

    /**
     * Community nao permite restringir o Supply Plan por filtros de materiais
     * cadastrados. O recorte publico trabalha sempre com os materiais ativos
     * do cluster/malha considerado.
     */
    private void validaFiltroMateriaisCommunity(PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO) {

        if (perfilExecucaoSupplyPlanDTO.getMaterialFilterId() != null) {
            throw new RequiresEnterpriseVersionException("Supply Planning material filters");
        }

    }

    /**
     * Community executa Supply Planning apenas pelo motor heuristico. Process
     * chains e otimizador sao reintroduzidos no Enterprise por services
     * especificos.
     */
    private void validaModoExecucaoCommunity(PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO) {

        if (perfilExecucaoSupplyPlanDTO.getExecutionModel() != null
                && !PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO
                .equals(perfilExecucaoSupplyPlanDTO.getExecutionModel())) {
            throw new RequiresEnterpriseVersionException("Supply Planning execution engine");
        }

    }

    /**
     * Community usa o Demand Plan como unica demanda direta futura do Supply
     * Planning. Carteira de clientes, sell-in, sales orders, transferencias,
     * compras e ordens firmes/transacionais abertas pertencem ao Enterprise.
     */
    private void validaPedidosTransacionaisCommunity(PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO) {

        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getConsiderSelloutOrdersBacklog(), "Supply Planning customer orders");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getConsiderSelloutOrdersFuture(), "Supply Planning customer orders");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getConsiderSellinOrdersBacklog(), "Supply Planning sell-in orders");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getConsiderSellinOrdersFuture(), "Supply Planning sell-in orders");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getConsiderTransferOrdersBacklog(), "Supply Planning transfer orders");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getConsiderTransferOrdersFuture(), "Supply Planning transfer orders");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getConsiderPurchaseOrdersBacklog(), "Supply Planning purchase orders");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getConsiderPurchaseOrdersFuture(), "Supply Planning purchase orders");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getConsiderProductionOrdersBacklog(), "Supply Planning firm production orders");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getConsiderProductionOrdersFuture(), "Supply Planning firm production orders");
        validaModeloDemandaDiretaCommunity(
                perfilExecucaoSupplyPlanDTO.getCustomerOrdersAndForecastReconciliationModelForProjectedInventory(),
                "Supply Planning demand source");
        validaModeloDemandaDiretaCommunity(
                perfilExecucaoSupplyPlanDTO.getCustomerOrdersAndForecastReconciliationModelForSafetyStock(),
                "Supply Planning safety-stock demand source");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getCustomerOrderHorizonInDays(), "Supply Planning customer orders");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getCustomerOrderMetDemandImpactCoefficient(), "Supply Planning customer orders");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getAllowBacklogCarryOver(), "Supply Planning backlog carry-over");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getForceMakeToOrderModel(), "Supply Planning fully make-to-order");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getEnableDemandCatchUpFromPastSellout(), "Supply Planning demand catch-up");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getConsiderUnmetClientOrderImpact(), "Supply Planning customer order impact");

        if (Boolean.FALSE.equals(perfilExecucaoSupplyPlanDTO.getConsiderForecastForMto())) {
            throw new RequiresEnterpriseVersionException("Supply Planning fully make-to-order");
        }

    }

    /**
     * Community suporta apenas restricao produtiva no modelo simples de horas
     * totais por dia. Restricoes logisticas, armazenagem, inbound/outbound e
     * estoque em clientes/transshipment dependem de estruturas e outputs
     * Enterprise que nao existem no recorte publico.
     */
    private void validaCapacidadesEConstraintsCommunity(PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO) {

        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getConsiderStorageConstraints(), "Supply Planning storage constraints");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getConsiderInboundConstraints(), "Supply Planning inbound constraints");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getConsiderOutboundConstraints(), "Supply Planning outbound constraints");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getIgnoreStorageConstraintsForUnconstrainedPlan(), "Supply Planning storage constraints");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getIgnoreInboundConstraintsForUnconstrainedPlan(), "Supply Planning inbound constraints");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getIgnoreOutboundConstraintsForUnconstrainedPlan(), "Supply Planning outbound constraints");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getGenerateProductionScheduling(), "Supply Planning production scheduling");
        validaCampoEnterprisePreenchido(perfilExecucaoSupplyPlanDTO.getLogisticsCapacityLevel(), "Supply Planning logistics capacity");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getAllowStockAtClients(), "Supply Planning stock at clients");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getAllowStockAtTransshipmentPoints(), "Supply Planning stock at transshipment points");

        if (perfilExecucaoSupplyPlanDTO.getProductiveCapacityType() != null
                && !PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva.HORAS_POR_DIA
                .equals(perfilExecucaoSupplyPlanDTO.getProductiveCapacityType())) {
            throw new RequiresEnterpriseVersionException("Supply Planning production capacity type");
        }

    }

    /**
     * Community sempre executa Supply Planning sobre todas as locations ativas
     * do escopo base. Qualquer inclusao/exclusao por location ou override de
     * parametro por location pertence ao Enterprise.
     */
    private void validaPerfilLocationLevelCommunity(PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO) {

        if (Boolean.FALSE.equals(perfilExecucaoSupplyPlanDTO.getExecuteSupplyPlanForAllLocations())) {
            throw new RequiresEnterpriseVersionException(SUPPLY_PLANNING_LOCATION_LEVEL_PROFILE);
        }

    }

    /**
     * Community nao possui motor otimizado nem process chains; portanto estes
     * campos nao sao uma configuracao alternativa do heuristico. Eles pertencem
     * aos modelos otimizados Enterprise, incluindo funcao objetivo, penalidades,
     * priorizacoes, parametrizacao de solver, line scheduling e custos. As
     * preferencias persistidas de Profit/Loss pertencem ao aggregate Community
     * e sao consumidas somente pelo futuro motor Enterprise.
     */
    private void validaParametrosModeloOtimizadoCommunity(PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO) {

        validaCampoEnterprisePreenchido(perfilExecucaoSupplyPlanDTO.getOptimizationModelType(), "Supply Planning optimizer");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getEnableLineSequencing(), "Supply Planning line sequencing");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getEnableGreenfieldBrownfield(), "Supply Planning Greenfield/Brownfield");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getDemandPlanMetDemandImpactCoefficient(), "Supply Planning objective function");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getRoundPlannedPurchaseOrdersByMinimumLotSize(), "Supply Planning planned purchase orders");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getIncreaseObjectiveFunctionImpactInEarlierPeriods(), "Supply Planning objective function");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getMaximumPercentageIncreaseObjectiveFunctionImpactAtFirstPeriod(), "Supply Planning objective function");
        validaCampoEnterprisePreenchido(perfilExecucaoSupplyPlanDTO.getObjectiveFunctionTemporalImpactDecayModel(), "Supply Planning objective function");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getObjectiveFunctionTemporalImpactExponentialDecayFactor(), "Supply Planning objective function");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getObjectiveFunctionTemporalImpactMinimumMultiplier(), "Supply Planning objective function");
        validaTextoEnterprisePreenchido(perfilExecucaoSupplyPlanDTO.getCustomerDemandPrioritizationModelId(), "Supply Planning demand prioritization");
        validaTextoEnterprisePreenchido(perfilExecucaoSupplyPlanDTO.getSafetyStockPrioritizationModelId(), "Supply Planning safety-stock prioritization");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getSaveOptimizerVariablesAndConstraints(), "Supply Planning optimizer diagnostics");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getSaveConstraintBacktracking(), "Supply Planning Constraint Root Cause Analysis");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getIgnoreLeadTimeConstraintsForUnconstrainedPlan(), "Supply Planning lead-time optimization");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getMaximumTransferCostImpactForLeadTimeReduction(), "Supply Planning lead-time optimization");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getMaximumMaterialObjectiveValueImpactForLeadTimeReduction(), "Supply Planning lead-time optimization");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getIgnoreMarginConstraintsForUnconstrainedPlan(), "Supply Planning margin optimization");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getMetDemandObjectiveValueIncreasePercentage(), "Supply Planning objective function");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getMinimumMetDemandObjectiveValue(), "Supply Planning objective function");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getGeneratePL(), "Supply Planning P&L and Cost-to-Serve");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getAssociateSalesToInputMaterialsInRetroaction(), "Supply Planning P&L and Cost-to-Serve");
        validaTextoEnterprisePreenchido(perfilExecucaoSupplyPlanDTO.getOptimizationUom(), "Supply Planning optimizer unit of measure");
        validaCampoEnterprisePreenchido(perfilExecucaoSupplyPlanDTO.getSalesMeasure(), "Supply Planning sales measure optimization");
        validaCampoEnterprisePreenchido(perfilExecucaoSupplyPlanDTO.getTaxApportionmentModel(), "Supply Planning tax apportionment");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getUnitValueByOptimizationUom(), "Supply Planning optimizer unit value");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getConsiderLocationFixedCost(), "Supply Planning cost model");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getConsiderProductionResourceFixedCost(), "Supply Planning cost model");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getConsiderStorageCost(), "Supply Planning cost model");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getConsiderTransferCost(), "Supply Planning cost model");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getConsiderTaxesInTransportationLines(), "Supply Planning tax and cost model");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getConsiderInboundOutboundCosts(), "Supply Planning cost model");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getConsiderProductionCost(), "Supply Planning cost model");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getConsiderSupplierPrices(), "Supply Planning supplier prices");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getEstimateUnitCogsForWorkingCapitalAndInventoryPolicy(), "Supply Planning estimated COGS");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getNumberSegmentsDirectDemandGapLinearization(), "Supply Planning optimized fair share");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getFairShareMaximumPercentagePenaltyUnmetDemand(), "Supply Planning optimized fair share");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getFairShareMaximumPercentagePenaltySafetyStockGap(), "Supply Planning optimized fair share");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getSafetyStockFairShare(), "Supply Planning safety-stock fair share");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getNumberSegmentsSafetyStockGapLinearization(), "Supply Planning safety-stock fair share");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getWorkingCapitalPercentualCost(), "Supply Planning working capital cost");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getMaximumOptimizerExecutionTime(), "Supply Planning optimizer");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getEntityTabuRatio(), "Supply Planning optimizer");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getAcceptedCountLimit(), "Supply Planning optimizer");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getSoftTargetMaximumPercentPenalty(), "Supply Planning process-chain soft targets");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getSoftTargetDeviationAmplitudeAsTargetPercent(), "Supply Planning process-chain soft targets");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getSoftTargetDeviationLinearizationNumberSegments(), "Supply Planning process-chain soft targets");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getFirmOrderCogsIncentivePercentage(), "Supply Planning firm orders optimization");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getSafetyStockGapPercentualCost(), "Supply Planning safety-stock gap cost");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getSegmentInventoryByBatch(), "Supply Planning inventory batch optimization");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getIncreaseWorkingCapitalImpactForOlderBatches(), "Supply Planning inventory aging optimization");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getMaximumPercentageIncreaseWorkingCapitalImpactForOldestBatch(), "Supply Planning inventory aging optimization");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getGenerateDetailedPlan(), "Supply Planning line scheduling");
        /*
         * Embora seja um escalar na tabela compartilhada para o overlay
         * Enterprise, este arredondamento somente tem efeito no scheduling SNP
         * privado. Bloqueamos o payload antes do mapper/repository para que o
         * Community nao passe a persistir uma configuracao sem runtime proprio.
         */
        validaBooleanEnterprise(
                perfilExecucaoSupplyPlanDTO.getRoundProductionAndSetupsToDetailedPlanBucket(),
                "Supply Planning line scheduling");
        validaCampoEnterprisePreenchido(perfilExecucaoSupplyPlanDTO.getDetailedPlanBucketSize(), "Supply Planning line scheduling");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getDetailedPlanPlanningHorizonInBuckets(), "Supply Planning line scheduling");
        validaBooleanEnterprise(perfilExecucaoSupplyPlanDTO.getPenalizeUnmetDemand(), "Supply Planning unmet-demand penalty");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getUnmetDemandPenalizationAsFractionOfGrossSales(), "Supply Planning unmet-demand penalty");
        validaNumeroEnterprise(perfilExecucaoSupplyPlanDTO.getUnmetDemandPenalizationAsUnitImpact(), "Supply Planning unmet-demand penalty");
        validaTextoEnterprisePreenchido(perfilExecucaoSupplyPlanDTO.getUnmetDemandPenalizationAsUnitImpactUomId(), "Supply Planning unmet-demand penalty");

    }

    /**
     * Os campos de otimizador, funcao objetivo, line scheduling, soft targets
     * e penalidades economicas nao sao mapeados na
     * entidade Community como comportamento executavel.
     *
     * <p>Este metodo permanece como ponto explicito para futuros campos
     * transicionais que eventualmente precisem ser apagados apos a conversao do
     * DTO. No recorte atual, a validacao acima bloqueia payload Enterprise e o
     * mapper Community ja grava defaults neutros para os campos compartilhados
     * que precisam continuar existindo para o overlay Enterprise.</p>
     */
    private void normalizaParametrosModeloOtimizadoCommunity(PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) {

    }

    /**
     * Normaliza campos de pedidos para que perfis salvos pelo Community nao
     * reativem carteira/sell-in/sales orders por defaults historicos da entidade.
     *
     * <p>A maior parte desses atributos ja nao existe fisicamente na entidade
     * Community. O metodo fica como ponto explicito para futuros campos
     * transicionais de pedidos que ainda precisem ser neutralizados apos a
     * conversao do DTO.</p>
     */
    private void normalizaPedidosTransacionaisCommunity(PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) {

    }

    /**
     * Mantem a entidade persistida no subconjunto operacional do Community:
     * capacidade produtiva por horas/dia e sem restricoes logisticas. Estoque
     * em clientes e pontos de transbordo nao e mapeado na entidade Community.
     *
     * <p>O tipo de capacidade produtiva tambem nao e mais persistido: o getter
     * da entidade Community retorna sempre {@code HORAS_POR_DIA}.</p>
     */
    private void normalizaCapacidadesEConstraintsCommunity(PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) {

    }

    /**
     * O heuristico Community aplica fair share proporcional da demanda direta
     * sempre que ha restricao quantitativa. Nao existe modo Community para
     * desligar esse comportamento; os parametros otimizados de penalizacao e
     * safety-stock fair share ficam bloqueados em
     * {@link #validaParametrosModeloOtimizadoCommunity(PerfilExecucaoSupplyPlanDTO)}.
     */
    private void normalizaFairShareCommunity(PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) {

        perfilExecucaoSupplyPlan.setAplicaFairShareDemandaDireta(true);

    }

    private void validaBooleanEnterprise(Boolean valor, String featureName) {

        if (Boolean.TRUE.equals(valor)) {
            throw new RequiresEnterpriseVersionException(featureName);
        }

    }

    private void validaModeloDemandaDiretaCommunity(
            PerfilExecucaoSupplyPlan.ModeloMajoracaoDemandaDireta modeloMajoracaoDemandaDireta,
            String featureName) {

        /*
         * A opcao funcional do Community e Demand Plan only. O front
         * compartilhado pode omitir o campo ou enviar explicitamente o default;
         * qualquer combinacao com carteira/client orders pertence ao Enterprise.
         */
        if (modeloMajoracaoDemandaDireta != null
                && !modeloMajoracaoDemandaDireta.equals(PerfilExecucaoSupplyPlan.ModeloMajoracaoDemandaDireta.SOMENTE_FORECAST)) {
            throw new RequiresEnterpriseVersionException(featureName);
        }

    }

    private void validaNumeroEnterprise(Number valor, String featureName) {

        if (valor == null) {
            return;
        }

        /*
         * Campos numericos Enterprise aceitam apenas ausencia ou zero tecnico
         * neutro no payload Community. `NaN` nao pode ser tratado como neutro:
         * ele normalmente indica payload manual quebrado ou mapper incorreto e,
         * como `Math.abs(Double.NaN)` tambem e `NaN`, escaparia da comparacao
         * por epsilon se nao fosse validado explicitamente.
         */
        double valorDouble = valor.doubleValue();
        if (!Double.isFinite(valorDouble) || Math.abs(valorDouble) > 0.0000001d) {
            throw new RequiresEnterpriseVersionException(featureName);
        }

    }

    private void validaInteiroOperacionalPositivo(Integer valor, String fieldName) {

        if (valor != null && valor <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive.");
        }

    }

    private void validaTextoEnterprisePreenchido(String valor, String featureName) {

        if (valor != null && !valor.isBlank()) {
            throw new RequiresEnterpriseVersionException(featureName);
        }

    }

    private void validaCampoEnterprisePreenchido(Object valor, String featureName) {

        if (valor != null) {
            throw new RequiresEnterpriseVersionException(featureName);
        }

    }

}
