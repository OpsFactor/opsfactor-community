package com.opsfactor.community.capability.supplyplanning.supplyplan.projection;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Projection agregadora de Supply Planning para execucoes com multiplas
 * locations planejadas.
 *
 * <p>O Community mantém uma `SupplyPlanningProjection` por location para que
 * as rotinas heuristicas trabalhem com o mesmo indice usado no calculo local,
 * enquanto esta classe concentra leituras que precisam enxergar todas as
 * locations do plano. Process chain, otimizacao, solver e analises de
 * variaveis/restricoes pertencem ao overlay Enterprise.</p>
 */
@Getter
public class SupplyPlanningMultiplasLocationsProjection {

    /** Supply Plan persistido ou recem-criado que recebera os resultados. */
    private final SupplyPlan supplyPlan;

    /**
     * Perfil efetivamente usado para gerar a projection multi-location.
     *
     * <p>No Community este perfil coincide com o perfil raiz do Supply Plan.
     * O Enterprise pode reintroduzir execucoes compostas escolhendo um perfil
     * especifico para cada etapa.</p>
     */
    private final PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlanConsiderado;

    /** Projection de conversoes entre unidades de medida. */
    private final UnidadeMedidaProjection conversaoUnidadeMedidaProjection;

    /** Parametros globais, materiais, locations e clusters usados no calculo. */
    private final ClusterEParametrosProjection clusterEParametrosProjection;

    /**
     * Malha operacional compartilhada pelas projections locais.
     *
     * <p>Ela define roteiros, listas tecnicas e linhas inbound prioritarias
     * usadas pelos metodos de escrita de producao, distribuicao e estoque.</p>
     */
    private final SupplyNetworkProjection supplyNetworkProjection;

    /** Politicas de estoque operacionais associadas ao perfil. */
    private final PoliticaEstoquesProjection politicaEstoquesProjection;

    /** Materiais que podem ser planejados, incluindo acabados e componentes. */
    MaterialProjection materialProjection;

    /** Locations destino planejadas nesta execucao. */
    LocationProjection locationProjection;

    /**
     * Projection local por location planejada.
     *
     * <p>A escrita deve passar por `addSupplyPlanningProjection` para garantir
     * que cada location apareça uma unica vez no snapshot multi-location.</p>
     */
    private final Map<Location, SupplyPlanningProjection> mapaSupplyPlanningProjectionPorLocation = new HashMap<>();

    /**
     * Cria o agregador multi-location ainda sem projections locais populadas.
     *
     * <p>A factory responsavel pela montagem do input data cria cada
     * `SupplyPlanningProjection` local e preenche
     * `mapaSupplyPlanningProjectionPorLocation`, preservando aqui apenas o
     * indice agregado para consultas transversais.</p>
     */
    public SupplyPlanningMultiplasLocationsProjection(SupplyPlan supplyPlan,
                                                      PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlanConsiderado,
                                                      SupplyNetworkProjection supplyNetworkProjection,
                                                      PoliticaEstoquesProjection politicaEstoquesProjection,
                                                      MaterialProjection materialProjection,
                                                      LocationProjection locationProjection) {

        if (supplyNetworkProjection == null) {
            throw new IllegalArgumentException(
                    "SupplyPlanningMultiplasLocationsProjection requires Supply Network projection.");
        }

        this.supplyPlan = supplyPlan;
        this.perfilExecucaoSupplyPlanConsiderado = perfilExecucaoSupplyPlanConsiderado;
        this.conversaoUnidadeMedidaProjection = supplyNetworkProjection.getConversaoUnidadeMedidaProjection();
        this.materialProjection = materialProjection;
        this.locationProjection = locationProjection;
        this.clusterEParametrosProjection = supplyNetworkProjection.getClusterEParametrosProjection();
        this.supplyNetworkProjection = supplyNetworkProjection;
        this.politicaEstoquesProjection = politicaEstoquesProjection;

    }

    /**
     * Adiciona uma projection local ao snapshot multi-location.
     *
     * <p>O heuristico Community e os overlays Enterprise esperam uma unica
     * `SupplyPlanningProjection` por location. Uma segunda projection para a
     * mesma location indicaria montagem ambigua do input data e nao deve
     * depender da ordem em que a factory percorreu o escopo.</p>
     */
    public void addSupplyPlanningProjection(SupplyPlanningProjection supplyPlanningProjection) {

        validaSupplyPlanningProjectionParaSnapshot(supplyPlanningProjection);

        SupplyPlanningProjection supplyPlanningProjectionAnterior =
                mapaSupplyPlanningProjectionPorLocation.putIfAbsent(
                        supplyPlanningProjection.getLocation(),
                        supplyPlanningProjection);

        if (supplyPlanningProjectionAnterior != null
                && supplyPlanningProjectionAnterior != supplyPlanningProjection) {
            throw new IllegalArgumentException(
                    "SupplyPlanningMultiplasLocationsProjection received duplicated Supply Planning projection for location "
                            + supplyPlanningProjection.getLocation().getId()
                            + ".");
        }

    }

    /**
     * Valida a projection local antes de indexa-la por location.
     */
    private void validaSupplyPlanningProjectionParaSnapshot(SupplyPlanningProjection supplyPlanningProjection) {

        if (supplyPlanningProjection == null) {
            throw new IllegalArgumentException(
                    "SupplyPlanningMultiplasLocationsProjection cannot index null Supply Planning projection.");
        }
        if (supplyPlanningProjection.getLocation() == null) {
            throw new IllegalArgumentException(
                    "SupplyPlanningMultiplasLocationsProjection requires local Supply Planning projection with location.");
        }

    }

    /**
     * Retorna a projection local da location informada.
     */
    public SupplyPlanningProjection getSupplyPlanningProjectionDeLocation(Location location) {
        return mapaSupplyPlanningProjectionPorLocation.get(location);
    }

    /**
     * Retorna todas as projections locais criadas para a execucao.
     */
    public Collection<SupplyPlanningProjection> getTodosSupplyPlanningProjections() {
        return mapaSupplyPlanningProjectionPorLocation.values();
    }

    /**
     * Retorna o inicio mais antigo entre os calendarios locais.
     */
    public LocalDateTime getDataHorarioInicialConsiderandoTodosCalendarios() {
        return mapaSupplyPlanningProjectionPorLocation.values()
                .stream()
                .map(supplyPlanningProjection -> supplyPlanningProjection.getCalendario().getDataHorarioInicial())
                .sorted()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "SupplyPlanningMultiplasLocationsProjection não possui calendars locais para calcular data inicial"));
    }

    /**
     * Retorna o fim mais recente entre os calendarios locais.
     */
    public LocalDateTime getDataHorarioFinalConsiderandoTodosCalendarios() {
        return mapaSupplyPlanningProjectionPorLocation.values()
                .stream()
                .map(supplyPlanningProjection -> supplyPlanningProjection.getCalendario().getDataHorarioFinal())
                .sorted(Comparator.reverseOrder()) // Ordenação em ordem decrescente
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "SupplyPlanningMultiplasLocationsProjection não possui calendars locais para calcular data final"));
    }

}
