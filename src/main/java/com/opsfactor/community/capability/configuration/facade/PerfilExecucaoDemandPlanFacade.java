package com.opsfactor.community.capability.configuration.facade;

import com.opsfactor.community.capability.demandplanning.configuration.facade.dto.PerfilExecucaoDemandPlanDTO;
import com.opsfactor.community.capability.demandplanning.configuration.facade.mapper.PerfilExecucaoDemandPlanAutoMapper;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.capability.demandplanning.configuration.repository.PerfilExecucaoDemandPlanRepository;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.service.UnidadeMedidaService;
import com.opsfactor.community.capability.demandplanning.forecast.configuration.DemandPlanningModelCatalog;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Service front do perfil de execucao Demand Planning Community.
 *
 * <p>O DTO pode receber campos Enterprise em payloads legados/transicionais,
 * mas o Community aceita apenas sell-out, bucket/horizonte, restricao de edicao
 * e unidade de medida padrao. Configuracoes de sell-in, sales orders, agregacao
 * MAPE, auto-fit e regression tree sao bloqueadas explicitamente.</p>
 */
@Service
public class PerfilExecucaoDemandPlanFacade {

    /**
     * Repository do perfil de execucao de Demand Planning. Toda escrita passa
     * por validacao Community antes de usar este bean, garantindo que campos
     * Enterprise nao sejam persistidos por payload manual.
     */
    @Autowired
    private PerfilExecucaoDemandPlanRepository perfilExecucaoDemandPlanRepository;

    /**
     * Mapper de entidade para DTO usado nas listagens do front. No Community ele
     * deve devolver apenas campos operacionais permitidos e deixar campos
     * Enterprise nulos.
     */
    @Autowired
    private PerfilExecucaoDemandPlanAutoMapper perfilExecucaoDemandPlanAutoMapper;

    /**
     * Service de UOM usado apenas para resolver a unidade padrao de Demand
     * Planning quando o payload Community informa `defaultDemandPlanningUomId`.
     */
    @Autowired
    private UnidadeMedidaService unidadeMedidaService;

    /**
     * Service de parametros globais usado para interpretar defaults
     * transicionais da entidade, especialmente o documento historico salvo ou
     * herdado.
     */
    @Autowired
    private ParametrosGlobaisService parametrosGlobaisService;

    /**
     * Lista perfis de execucao de Demand Planning no contrato DTO Community.
     *
     * <p>Campos de MAPE, auto-fit e regression tree podem continuar existindo
     * no DTO por compatibilidade de desserializacao, mas o mapper Community nao
     * deve popula-los nesta resposta.</p>
     */
    public List<PerfilExecucaoDemandPlanDTO> getPerfilExecucaoDemandPlanDTOSet() {

        /*
         * A listagem e usada pelo front como catalogo operacional de perfis.
         * Retorno nulo de repository/service representa snapshot estrutural
         * quebrado, nao ausencia valida de perfis; ausencia valida e lista
         * vazia.
         */
        Collection<PerfilExecucaoDemandPlan> perfilExecucaoDemandPlanCollection =
                perfilExecucaoDemandPlanRepository.customFindAll();
        ParametrosGlobais parametrosGlobais =
                parametrosGlobaisService.getParametrosGlobais();

        return convertePerfilExecucaoDemandPlanDTOListCommunity(
                perfilExecucaoDemandPlanCollection,
                parametrosGlobais);

    }

    /**
     * Converte os perfis para DTO e valida a fotografia que sera devolvida ao
     * front Community.
     *
     * <p>A validacao de repository garante que as entidades existem. Esta etapa
     * garante que o mapper Community nao reintroduziu campos Enterprise na
     * resposta, principalmente sell-in/sales orders, agregacoes MAPE, auto-fit
     * ou regression tree.</p>
     */
    private List<PerfilExecucaoDemandPlanDTO> convertePerfilExecucaoDemandPlanDTOListCommunity(
            Collection<PerfilExecucaoDemandPlan> perfilExecucaoDemandPlanCollection,
            ParametrosGlobais parametrosGlobais) {

        List<PerfilExecucaoDemandPlanDTO> perfilExecucaoDemandPlanDTOList =
                perfilExecucaoDemandPlanAutoMapper.converteListaEntidadesParaDtoList(
                        perfilExecucaoDemandPlanCollection,
                        parametrosGlobais);
        validaPerfilExecucaoDemandPlanDTOListListagemCommunity(perfilExecucaoDemandPlanDTOList);
        return perfilExecucaoDemandPlanDTOList;

    }

    /**
     * Persiste um perfil de execucao Demand Planning com o recorte Community.
     *
     * <p>O documento historico gravado e sempre sell-out. Payloads que tentem
     * habilitar sell-in, sales orders, MAPE aggregation, auto-fit ou regression
     * tree falham antes de repository/service auxiliar para evitar que uma tela
     * Enterprise escondida no front abra comportamento sem backend Community.</p>
     */
    public void savePerfilExecucaoDemandPlanDTO(PerfilExecucaoDemandPlanDTO perfilExecucaoDemandPlanDTO) {

        validaPayloadBasePerfilExecucaoDemandPlan(perfilExecucaoDemandPlanDTO);
        validaConfiguracoesEnterpriseCommunity(perfilExecucaoDemandPlanDTO);

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan =
                getPerfilExecucaoDemandPlanParaSave(perfilExecucaoDemandPlanDTO.id);

        perfilExecucaoDemandPlan.setTipoDocumentoVenda(Constantes.TipoDocumentoVenda.SELLOUT);
        perfilExecucaoDemandPlan.setDescricao(perfilExecucaoDemandPlanDTO.description);
        perfilExecucaoDemandPlan.setTamanhoBucket(perfilExecucaoDemandPlanDTO.bucketSize);
        perfilExecucaoDemandPlan.setNumeroPeriodosHorizontePlanejamento(perfilExecucaoDemandPlanDTO.planningHorizonInPeriods);
        perfilExecucaoDemandPlan.setRestringePeriodosEdicaoPlano(perfilExecucaoDemandPlanDTO.constrainPlanEditPeriods);
        perfilExecucaoDemandPlan.setPeriodoInicialEdicaoPlano(perfilExecucaoDemandPlanDTO.initialPlanEditPeriod);
        perfilExecucaoDemandPlan.setPeriodoFinalEdicaoPlano(perfilExecucaoDemandPlanDTO.finalPlanEditPeriod);
        perfilExecucaoDemandPlan.setUnidadeMedidaPadraoDP(
                perfilExecucaoDemandPlanDTO.defaultDemandPlanningUomId == null ?
                        null
                        : unidadeMedidaService.getUnidadeMedidaDeId(
                                perfilExecucaoDemandPlanDTO.defaultDemandPlanningUomId));
        /*
         * O metodo nao retorna DTO, mas o front trata a chamada como sucesso de
         * cadastro. Validar a entidade devolvida pelo repository evita sucesso
         * silencioso quando a persistencia ou um stub futuro devolver snapshot
         * nulo/incompleto.
         */
        PerfilExecucaoDemandPlan perfilExecucaoDemandPlanSalvo =
                perfilExecucaoDemandPlanRepository.save(perfilExecucaoDemandPlan);
        validaPerfilExecucaoDemandPlanSalvoCommunity(perfilExecucaoDemandPlanSalvo);

    }

    /**
     * Valida a fotografia de perfis retornada pelo repository antes do mapper.
     *
     * <p>Lista vazia e uma configuracao operacional valida para uma base ainda
     * sem perfis cadastrados. Lista nula, item nulo ou perfil sem id indica
     * quebra do contrato da query/fotografia e deve falhar antes de chegar ao
     * MapStruct.</p>
     */
    

    /**
     * Valida o snapshot DTO depois do mapper e antes do retorno publico.
     *
     * <p>Lista vazia continua valida para uma instalacao nova. Quando ha itens,
     * cada DTO precisa ter id e documento historico explicitamente `SELLOUT`.
     * Campos Enterprise reutilizam a mesma validacao do save para manter uma
     * unica fonte de verdade sobre o recorte Community.</p>
     */
    private void validaPerfilExecucaoDemandPlanDTOListListagemCommunity(
            List<PerfilExecucaoDemandPlanDTO> perfilExecucaoDemandPlanDTOList) {

        if (perfilExecucaoDemandPlanDTOList == null) {
            throw new IllegalStateException(
                    "Demand Planning execution profile DTO listing requires mapper result.");
        }

        for (int index = 0; index < perfilExecucaoDemandPlanDTOList.size(); index++) {
            PerfilExecucaoDemandPlanDTO perfilExecucaoDemandPlanDTO =
                    perfilExecucaoDemandPlanDTOList.get(index);
            if (perfilExecucaoDemandPlanDTO == null) {
                throw new IllegalStateException(
                        "Demand Planning execution profile DTO at index " + index + " is required in list snapshot.");
            }
            if (perfilExecucaoDemandPlanDTO.id == null || perfilExecucaoDemandPlanDTO.id.isBlank()) {
                throw new IllegalStateException(
                        "Demand Planning execution profile DTO at index " + index + " has no id in list snapshot.");
            }
            if (!Constantes.TipoDocumentoVenda.SELLOUT.equals(
                    perfilExecucaoDemandPlanDTO.historicalSalesDocumentType)) {
                throw new IllegalStateException(
                        "Demand Planning execution profile DTO at index "
                                + index
                                + " must use sell-out historical sales in Community.");
            }

            /*
             * A tela compartilhada pode conhecer os campos Enterprise para
             * exibir bloqueios, mas a resposta Community nao deve devolve-los
             * preenchidos. Assim evitamos que uma opcao bloqueada apareca como
             * configuracao efetiva.
             */
            validaConfiguracoesEnterpriseCommunity(perfilExecucaoDemandPlanDTO);
        }

    }

    /**
     * Valida os parametros globais usados como contexto do mapper Community.
     *
     * <p>Mesmo que o mapper force `Sell-out` na resposta, os parametros globais
     * continuam sendo o contexto formal de fallback da entidade. Eles precisam
     * existir para diferenciar bootstrap quebrado de lista vazia valida.</p>
     */
    /**
     * Resolve a entidade existente ou cria a entidade nova com a chave funcional
     * ja validada.
     */
    private PerfilExecucaoDemandPlan getPerfilExecucaoDemandPlanParaSave(
            String perfilExecucaoDemandPlanId) {

        Optional<PerfilExecucaoDemandPlan> optionalPerfilExecucaoDemandPlan =
                perfilExecucaoDemandPlanRepository.findById(perfilExecucaoDemandPlanId);
        if (optionalPerfilExecucaoDemandPlan == null) {
            throw new IllegalStateException(
                    "Demand Planning execution profile repository returned null Optional for profile "
                            + perfilExecucaoDemandPlanId + ".");
        }

        return optionalPerfilExecucaoDemandPlan
                .orElse(new PerfilExecucaoDemandPlan(perfilExecucaoDemandPlanId));

    }

    /**
     * Resolve a UOM padrao informada no payload Community.
     *
     * <p>Quando a tela envia uma UOM, ela precisa existir. O service de dominio
     * normalmente ja falha para id inexistente; a validacao local protege tambem
     * stubs ou implementacoes futuras que retornem nulo por engano.</p>
     */
    /**
     * Valida o snapshot salvo de perfil Demand Planning Community.
     *
     * <p>O recorte Community persiste apenas configuracao operacional basica
     * do perfil; portanto o minimo inegociavel apos o save e uma entidade com
     * chave. Validacoes de sell-out, auto-fit e MAPE ja acontecem antes da
     * persistencia para separar payload Enterprise de snapshot quebrado.</p>
     */
    private void validaPerfilExecucaoDemandPlanSalvoCommunity(
            PerfilExecucaoDemandPlan perfilExecucaoDemandPlanSalvo) {

        if (perfilExecucaoDemandPlanSalvo == null) {
            throw new IllegalStateException(
                    "Saved Demand Planning execution profile snapshot is required after Community profile persistence.");
        }
        if (perfilExecucaoDemandPlanSalvo.getId() == null
                || perfilExecucaoDemandPlanSalvo.getId().isBlank()) {
            throw new IllegalStateException(
                    "Saved Demand Planning execution profile id is required after Community profile persistence.");
        }

    }

    /**
     * Valida a chave minima do perfil antes das travas Enterprise e antes de
     * qualquer repository.
     *
     * <p>O id identifica o perfil salvo e tambem e usado como chave para criar
     * uma entidade nova quando ela ainda nao existe. Payload sem id nao pode
     * ser interpretado como criacao anonima nem virar NPE tecnico.</p>
     */
    protected void validaPayloadBasePerfilExecucaoDemandPlan(
            PerfilExecucaoDemandPlanDTO perfilExecucaoDemandPlanDTO) {

        if (perfilExecucaoDemandPlanDTO == null) {
            throw new IllegalArgumentException("Demand Planning execution profile DTO is required.");
        }
        if (perfilExecucaoDemandPlanDTO.id == null || perfilExecucaoDemandPlanDTO.id.isBlank()) {
            throw new IllegalArgumentException("Demand Planning execution profile id is required.");
        }
        if (perfilExecucaoDemandPlanDTO.id.length() > 50) {
            throw new IllegalArgumentException(
                    "Demand Planning execution profile id must be at most 50 characters long.");
        }
        /*
         * `planningHorizonInPeriods` nulo preserva o default historico da
         * entidade. Valor preenchido, porem, precisa ser positivo; deixar o
         * getter mascarar zero/negativo com `Math.max(1, ...)` faria o front
         * acreditar que salvou uma configuracao diferente da que sera usada no
         * forecast.
         */
        if (perfilExecucaoDemandPlanDTO.planningHorizonInPeriods != null
                && perfilExecucaoDemandPlanDTO.planningHorizonInPeriods <= 0) {
            throw new IllegalArgumentException(
                    "Demand Planning execution profile planning horizon must be positive.");
        }

    }

    /**
     * Valida os campos transicionais/Enterprise do DTO antes da persistencia.
     *
     * <p>Campos Community com valor livre sao copiados no metodo principal. O
     * documento historico e tratado separadamente porque `SELLOUT` e aceito,
     * enquanto `SELLIN` e `PEDIDO` exigem Enterprise.</p>
     */
    private void validaConfiguracoesEnterpriseCommunity(PerfilExecucaoDemandPlanDTO perfilExecucaoDemandPlanDTO) {

        validaParametroEnterpriseCommunity(perfilExecucaoDemandPlanDTO.mapeMaterialAggregationLevelId, "Demand Planning material aggregation level for MAPE");
        validaParametroEnterpriseCommunity(perfilExecucaoDemandPlanDTO.mapeLocationAggregationLevelId, "Demand Planning location aggregation level for MAPE");
        if (perfilExecucaoDemandPlanDTO.historicalSalesDocumentType != null
                && !DemandPlanningModelCatalog.isTipoDocumentoHistoricoCommunity(
                        perfilExecucaoDemandPlanDTO.historicalSalesDocumentType)) {
            throw new RequiresEnterpriseVersionException("Sell-in and sales orders as historical sales source");
        }
        validaParametroEnterpriseCommunity(perfilExecucaoDemandPlanDTO.defaultAutoTunedDemandPlanConfigurationId, "Demand Planning default auto-fit configuration");
        validaParametroEnterpriseCommunity(perfilExecucaoDemandPlanDTO.autofitModelType, "Demand Planning auto-fit execution");
        validaParametroEnterpriseCommunity(perfilExecucaoDemandPlanDTO.modelAutofitObjectiveFunction, "Demand Planning auto-fit execution");
        validaParametroEnterpriseCommunity(perfilExecucaoDemandPlanDTO.modelAutofitNumberOfPeriodsForAccuracyEvaluation, "Demand Planning auto-fit execution");
        validaParametroEnterpriseCommunity(perfilExecucaoDemandPlanDTO.modelAutofitEvaluationLagInPeriods, "Demand Planning auto-fit execution");
        validaParametroEnterpriseCommunity(perfilExecucaoDemandPlanDTO.regressionTreeObjectiveFunction, "Demand Planning regression tree");
        validaParametroEnterpriseCommunity(perfilExecucaoDemandPlanDTO.numberOfDimensionsUsedForCandidateSplits, "Demand Planning regression tree");
        validaParametroEnterpriseCommunity(perfilExecucaoDemandPlanDTO.numberOfCandidateSplitsByDimension, "Demand Planning regression tree");
        validaParametroEnterpriseCommunity(perfilExecucaoDemandPlanDTO.maxDepthAfterLastConfirmedSplit, "Demand Planning regression tree");
        validaParametroEnterpriseCommunity(perfilExecucaoDemandPlanDTO.minimumPercentErrorReductionForNewSplits, "Demand Planning regression tree");
        validaParametroEnterpriseCommunity(perfilExecucaoDemandPlanDTO.numberOfPeriodsForRegressionTreePruning, "Demand Planning regression tree");

    }

    /**
     * Rejeita qualquer valor preenchido em campo que nao existe no runtime
     * Community.
     */
    private void validaParametroEnterpriseCommunity(Object valor, String featureName) {

        if (valor != null) {
            throw new RequiresEnterpriseVersionException(featureName);
        }

    }

}
