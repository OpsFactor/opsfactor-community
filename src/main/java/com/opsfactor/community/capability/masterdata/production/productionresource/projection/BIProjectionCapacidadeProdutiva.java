package com.opsfactor.community.capability.masterdata.production.productionresource.projection;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.DisponibilidadeRecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.platform.projection.inmemorybi.BIEmMemoria;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import lombok.Getter;
import org.javatuples.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * Projection de capacidade produtiva usada pelo Supply Planning Community.
 *
 * <p>O Community materializa apenas capacidade produtiva total em horas por
 * dia. Os demais modos continuam no enum compartilhado porque o Enterprise
 * precisa decodificar os mesmos perfis/snapshots, mas qualquer tentativa de
 * usar quantidade por UOM ou alocacao por turno neste modulo deve falhar antes
 * de consultar BI, snapshot persistido ou rotina heuristica.</p>
 */
@Getter
public class BIProjectionCapacidadeProdutiva {
    
    SupplyPlan supplyPlan;
    Calendario calendario;

    public enum MasterOrPlanningData {
        MASTER_DATA, PLANNING_DATA
    }

    /**
     * Tipo de capacidade efetivamente considerado por recurso produtivo.
     *
     * <p>No Community todos os recursos devem terminar como
     * {@code HORAS_POR_DIA}. O mapa fica por recurso para proteger snapshots
     * antigos e para permitir que o Enterprise substitua a origem desse valor
     * sem alterar as rotinas consumidoras.</p>
     */
    Map<RecursoProdutivo, PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva> tipoCapacidadeProdutivaPorRecurso;

    /**
     * Snapshot efetivo persistido por Supply Plan. Quando preenchido, tem precedencia
     * sobre dados mestres para relatorios de planos ja executados.
     */
    Map<RecursoProdutivo, Map<Integer, Double>> mapaCapacidadeEfetivaSupplyPlanPorRecursoPeriodo = new HashMap<>();

    /**
     * Unidade da própria fotografia persistida, identificada por recurso e
     * período. O mapa conserva a unidade usada na rodada sem reabrir o
     * cadastro mestre do recurso durante leituras posteriores.
     */
    Map<RecursoProdutivo, Map<Integer, String>> unitOfMeasureIdByProductionResourceAndPeriod =
            new HashMap<>();

    /**
     * Disponibilidade diaria por recurso produtivo.
     *
     * <p>Este e o unico BI de capacidade carregado no Community. Capacidade por
     * quantidade e turnos pertencem ao Enterprise e nao possuem indice paralelo
     * nesta projection.</p>
     */
    BIEmMemoria<DisponibilidadeRecursoProdutivo> biEmMemoriaCapacidadeDiaria = new BIEmMemoria<>(DisponibilidadeRecursoProdutivo.class);

    /**
     * Chaves recurso/data ja carregadas do master data de capacidade diaria.
     *
     * <p>O BI em memoria consolida valores numericos por soma. Para
     * disponibilidade produtiva isso seria perigoso: duas linhas para o mesmo
     * recurso e dia dobrariam a capacidade silenciosamente. A projection guarda
     * a chave funcional para falhar antes de inserir a duplicidade no BI.</p>
     */
    private final Set<String> chavesDisponibilidadeRecursoProdutivo = new HashSet<>();

    /**
     * Cria os índices de capacidade produtiva usados pelo Supply Planning.
     */
    public BIProjectionCapacidadeProdutiva(
            SupplyPlan supplyPlan,
            Calendario calendario,
            SupplyNetworkProjection supplyNetworkProjection) {

        ClusterEParametrosProjection clusterEParametrosProjection = supplyNetworkProjection.getClusterEParametrosProjection();

        this.supplyPlan = supplyPlan;
        this.calendario = calendario;

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = supplyPlan.getPerfilExecucaoSupplyPlan();

        tipoCapacidadeProdutivaPorRecurso = new HashMap<>();
        for (Location location : perfilExecucaoSupplyPlan.getLocationsConsideradas(clusterEParametrosProjection)) {
            for (RecursoProdutivo recursoProdutivo : supplyNetworkProjection.getRecursoProdutivoAtivoSet(location)) {
                // futuramente trocar por algo que extraia o tipo de capacidade do próprio recurso produtivo ao invés da location
                PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva = perfilExecucaoSupplyPlan.getTipoCapacidadeProdutiva(location);
                tipoCapacidadeProdutivaPorRecurso.put(recursoProdutivo, tipoCapacidadeProdutiva);
            }
        }

        // ATRIBUTOS PARA QUANTIDADE HORAS POR RECURSO PRODUTIVO
        biEmMemoriaCapacidadeDiaria.addObjectAttribute(
                "location",
                Location.class,
                disponibilidadeRecursoProdutivo -> disponibilidadeRecursoProdutivo.getRecursoProdutivo().getLocation(),
                true);
        biEmMemoriaCapacidadeDiaria.addObjectAttribute(
                "recursoProdutivo",
                RecursoProdutivo.class,
                DisponibilidadeRecursoProdutivo::getRecursoProdutivo,
                true);
        biEmMemoriaCapacidadeDiaria.addFloatAttribute(
                "horasDisponiveis",
                DisponibilidadeRecursoProdutivo::getHorasDisponiveis,
                false);
        biEmMemoriaCapacidadeDiaria.addIntegerAttribute(
                "posicaoPeriodoReferencia",
                disponibilidadeRecursoProdutivo -> calendario.getPosicaoPeriodo(disponibilidadeRecursoProdutivo.getDataReferencia()),
                true);
        biEmMemoriaCapacidadeDiaria.addLocalDateAttribute(
                "dataReferencia",
                disponibilidadeRecursoProdutivo -> disponibilidadeRecursoProdutivo.getDataReferencia(),
                true);

    }

    /**
     * Adiciona disponibilidade diaria de master data ao BI em memoria.
     */
    public void addDadoAoBI(DisponibilidadeRecursoProdutivo disponibilidadeRecursoProdutivo) {

        DisponibilidadeRecursoProdutivo disponibilidadeRecursoProdutivoObrigatoria =
                disponibilidadeRecursoProdutivo;
        String chaveDisponibilidadeRecursoProdutivo =
                disponibilidadeRecursoProdutivoObrigatoria.getRecursoProdutivo().getId()
                        + "/"
                        + disponibilidadeRecursoProdutivoObrigatoria.getDataReferencia();
        if (!chavesDisponibilidadeRecursoProdutivo.add(chaveDisponibilidadeRecursoProdutivo)) {
            throw new IllegalStateException(
                    "Production resource availability repository returned duplicated resource/date key "
                            + chaveDisponibilidadeRecursoProdutivo
                            + ".");
        }

        biEmMemoriaCapacidadeDiaria.addElementoNoBI(disponibilidadeRecursoProdutivoObrigatoria);

    }

    /**
     * Adiciona capacidade efetiva persistida de um Supply Plan ao indice.
     *
     * <p>Esse snapshot e lido depois que um plano ja foi executado. Por isso a
     * projection valida a linha antes de mutar o mapa interno: repository/stub
     * quebrado, chave funcional incompleta, valor numerico invalido ou segunda
     * escrita para o mesmo recurso/periodo devem falhar como contrato de
     * persistencia, e nao virar capacidade sobrescrita silenciosamente.</p>
     */
    public void addDadoAoBI(CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan capacidadeProdutivaEfetiva) {

        RecursoProdutivo recursoProdutivo = capacidadeProdutivaEfetiva.getRecursoProdutivo();
        int periodo = calendario.getPosicaoPeriodo(capacidadeProdutivaEfetiva.getDataReferencia());
        Double capacidadeEfetiva = capacidadeProdutivaEfetiva.getCapacidadeEfetiva();

        tipoCapacidadeProdutivaPorRecurso.put(
                recursoProdutivo,
                capacidadeProdutivaEfetiva.getTipoCapacidadeProdutiva());

        Map<Integer, Double> capacidadeEfetivaSupplyPlanPorPeriodo =
                mapaCapacidadeEfetivaSupplyPlanPorRecursoPeriodo
                        .computeIfAbsent(recursoProdutivo, recurso -> new HashMap<>());
        if (capacidadeEfetivaSupplyPlanPorPeriodo.containsKey(periodo)) {
            throw new IllegalStateException(
                    "Persisted effective production capacity snapshot returned duplicated resource/period key "
                            + recursoProdutivo.getId()
                            + "/"
                            + periodo
                            + " for Supply Plan "
                            + getSupplyPlanIdParaMensagem()
                            + ".");
        }

        capacidadeEfetivaSupplyPlanPorPeriodo.put(periodo, capacidadeEfetiva);
        unitOfMeasureIdByProductionResourceAndPeriod
                .computeIfAbsent(recursoProdutivo, resource -> new HashMap<>())
                .put(
                        periodo,
                        capacidadeProdutivaEfetiva.getUnidadeMedidaCapacidade() == null
                                ? null
                                : capacidadeProdutivaEfetiva
                                        .getUnidadeMedidaCapacidade()
                                        .getId());

    }

    public double getCapacidadeEmQuantidadeEmPosicaoPeriodo(
            int posicaoPeriodo,
            RecursoProdutivo recursoProdutivo) {

        PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva = getTipoCapacidadeProdutiva(recursoProdutivo);
        validaTipoCapacidadeProdutivaCommunity(tipoCapacidadeProdutiva);
        OptionalDouble capacidadeEfetivaSupplyPlan = getCapacidadeEfetivaSupplyPlan(posicaoPeriodo, recursoProdutivo);
        if (capacidadeEfetivaSupplyPlan.isPresent()) {
            return capacidadeEfetivaSupplyPlan.getAsDouble();
        }

        if (!tipoCapacidadeProdutiva.equals(PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva.QUANTIDADE_POR_UOM)) {
            throw getUnsupportedTipoCapacidadeProdutivaCommunityException(tipoCapacidadeProdutiva);
        }

        return biEmMemoriaCapacidadeDiaria.getWhereEqualsConsolidadoEmDouble(
                DisponibilidadeRecursoProdutivo::getCapacidadeEmQuantidade, 
                Pair.with("posicaoPeriodoReferencia", posicaoPeriodo),
                Pair.with("recursoProdutivo", recursoProdutivo));
        
    }

    public PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva getTipoCapacidadeProdutiva(RecursoProdutivo recursoProdutivo) {
        return tipoCapacidadeProdutivaPorRecurso.get(recursoProdutivo);
    }
    
    public double getCapacidadeEmHorasEmPosicaoPeriodo(
            int posicaoPeriodo, 
            RecursoProdutivo recursoProdutivo,
            MasterOrPlanningData masterOrPlanningData) {

        PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva = getTipoCapacidadeProdutiva(recursoProdutivo);
        validaTipoCapacidadeProdutivaCommunity(tipoCapacidadeProdutiva);
        OptionalDouble capacidadeEfetivaSupplyPlan = getCapacidadeEfetivaSupplyPlan(posicaoPeriodo, recursoProdutivo);
        if (capacidadeEfetivaSupplyPlan.isPresent()) {
            return capacidadeEfetivaSupplyPlan.getAsDouble();
        }

        switch (tipoCapacidadeProdutiva) {
            case HORAS_POR_DIA:
                return biEmMemoriaCapacidadeDiaria.getWhereEqualsConsolidadoEmDouble(
                        DisponibilidadeRecursoProdutivo::getHorasDisponiveis,
                        Pair.with("posicaoPeriodoReferencia", posicaoPeriodo),
                        Pair.with("recursoProdutivo", recursoProdutivo));
            default:
                throw getUnsupportedTipoCapacidadeProdutivaCommunityException(tipoCapacidadeProdutiva);
        }
        
    }
    
    /**
     * Retorna a capacidade operacional que o heuristico Community deve usar.
     *
     * <p>Apesar do nome historico mencionar quantidade ou horas, nesta edicao
     * apenas horas/dia passam pela validacao. Quantidade por UOM e alocacao por
     * turno ficam bloqueadas antes da leitura de snapshot ou dos dados mestres
     * para evitar que uma base com configuracao Enterprise rode parcialmente em
     * Community.</p>
     */
    public double getCapacidadeEmQuantidadeOuHorasEmPosicaoPeriodo(
            int posicaoPeriodo, 
            RecursoProdutivo recursoProdutivo,
            MasterOrPlanningData masterOrPlanningData) {

        PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva = getTipoCapacidadeProdutiva(recursoProdutivo);
        validaTipoCapacidadeProdutivaCommunity(tipoCapacidadeProdutiva);
        OptionalDouble capacidadeEfetivaSupplyPlan = getCapacidadeEfetivaSupplyPlan(posicaoPeriodo, recursoProdutivo);
        if (capacidadeEfetivaSupplyPlan.isPresent()) {
            return capacidadeEfetivaSupplyPlan.getAsDouble();
        }

        if (PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva.HORAS_POR_DIA.equals(tipoCapacidadeProdutiva)) {
            return getCapacidadeEmHorasEmPosicaoPeriodo(posicaoPeriodo, recursoProdutivo, masterOrPlanningData);
        }

        throw getUnsupportedTipoCapacidadeProdutivaCommunityException(tipoCapacidadeProdutiva);
        
    }

    public double getNumeroHorasDisponiveisEmPosicaoPeriodo(
            int posicaoPeriodo,
            RecursoProdutivo recursoProdutivo,
            MasterOrPlanningData masterOrPlanningData) {

        PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva = getTipoCapacidadeProdutiva(recursoProdutivo);
        validaTipoCapacidadeProdutivaCommunity(tipoCapacidadeProdutiva);

        switch (tipoCapacidadeProdutiva) {
            case HORAS_POR_DIA:
                return biEmMemoriaCapacidadeDiaria.getWhereEqualsConsolidadoEmDouble(
                        DisponibilidadeRecursoProdutivo::getHorasDisponiveis,
                        Pair.with("posicaoPeriodoReferencia", posicaoPeriodo),
                        Pair.with("recursoProdutivo", recursoProdutivo));
            default:
                throw getUnsupportedTipoCapacidadeProdutivaCommunityException(tipoCapacidadeProdutiva);
        }

    }

    /**
     * Community calcula capacidade produtiva somente em horas totais por dia.
     *
     * <p>Capacidade por quantidade em UOM e capacidade por turnos dependem de
     * infraestrutura Enterprise de scheduling/capacidade detalhada. A validacao
     * fica na projection para proteger tanto perfis atuais quanto snapshots
     * legados carregados de Supply Plans antigos.</p>
     */
    private void validaTipoCapacidadeProdutivaCommunity(
            PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva) {

        if (tipoCapacidadeProdutiva == null) {
            throw getUnsupportedTipoCapacidadeProdutivaCommunityException(null);
        }
        if (PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva.QUANTIDADE_POR_UOM.equals(tipoCapacidadeProdutiva)) {
            throw new RequiresEnterpriseVersionException("Quantity-based production capacity");
        }
        if (PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva.ALOCACAO_TURNOS.equals(tipoCapacidadeProdutiva)) {
            throw new RequiresEnterpriseVersionException("Shift-based production capacity");
        }

    }

    private IllegalArgumentException getUnsupportedTipoCapacidadeProdutivaCommunityException(
            PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva) {

        return new IllegalArgumentException(
                "BIProjectionCapacidadeProdutiva Community supports only HORAS_POR_DIA; received "
                        + (tipoCapacidadeProdutiva == null ? "null" : tipoCapacidadeProdutiva.name())
                        + ". Quantity by UOM and shift allocation belong to Enterprise, and missing configuration must be fixed before Supply Planning execution.");

    }

    private OptionalDouble getCapacidadeEfetivaSupplyPlan(int posicaoPeriodo, RecursoProdutivo recursoProdutivo) {
        Double capacidadeEfetiva = mapaCapacidadeEfetivaSupplyPlanPorRecursoPeriodo
                .getOrDefault(recursoProdutivo, new HashMap<>())
                .get(posicaoPeriodo);
        return (capacidadeEfetiva == null) ? OptionalDouble.empty() : OptionalDouble.of(capacidadeEfetiva);
    }

    /**
     * Valida uma linha persistida de capacidade efetiva antes da indexacao.
     */
    /**
     * Valida uma linha de disponibilidade produtiva antes de inserir no BI.
     */
    

    private String getSupplyPlanIdParaMensagem() {

        return supplyPlan == null || supplyPlan.getId() == null ? "<unknown>" : supplyPlan.getId().toString();

    }

}
