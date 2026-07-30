package com.opsfactor.community.capability.masterdata.production.productionresource.projection;

import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.DisponibilidadeRecursoProdutivo;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.productionplan.repository.CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository;
import com.opsfactor.community.capability.masterdata.production.productionresource.repository.DisponibilidadeRecursoProdutivoRepository;
import com.opsfactor.community.platform.calendar.Calendario;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Factory do BI em memoria de capacidade produtiva efetiva.
 *
 * <p>No Community a capacidade produtiva considera apenas disponibilidade em
 * horas/dia por recurso. Turnos, custos de turno, line scheduling e capacidade
 * por calendario operacional ficam no Enterprise.</p>
 */
@Service
public class BIProjectionCapacidadeProdutivaFactory {

    /**
     * Repository da disponibilidade produtiva diaria cadastrada no master data.
     */
    @Autowired
    private DisponibilidadeRecursoProdutivoRepository disponibilidadeRecursoProdutivoRepository;

    /**
     * Repository do snapshot de capacidade efetiva persistido no Supply Plan.
     */
    @Autowired
    private CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository capacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository;

    /**
     * Factory da malha produtiva usada para resolver recursos, roteiros e BOMs.
     */
    @Autowired
    private SupplyNetworkProjectionFactory supplyNetworkProjectionFactory;
    
    public BIProjectionCapacidadeProdutiva getBIProjectionCapacidadeProdutiva(
            SupplyPlan supplyPlan, Calendario calendario) {

        SupplyNetworkProjection supplyNetworkProjection = supplyNetworkProjectionFactory.getSupplyNetworkProjectionCompletoDeCache();

        BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva =
                new BIProjectionCapacidadeProdutiva(
                        supplyPlan,
                        calendario,
                        supplyNetworkProjection);

        List<DisponibilidadeRecursoProdutivo> disponibilidadesRecursoProdutivo =
                disponibilidadeRecursoProdutivoRepository.customFindAllWhereDataReferenciaBetween(
                        calendario.getDataHorarioInicialPresente().toLocalDate(),
                        calendario.getDataHorarioFinalFutura().toLocalDate());
        if (disponibilidadesRecursoProdutivo == null) {
            throw new IllegalStateException(
                    "Production resource availability repository returned null list for master data capacity projection.");
        }
        disponibilidadesRecursoProdutivo.forEach(biProjectionCapacidadeProdutiva::addDadoAoBI);

        /*
         * Community suporta apenas capacidade produtiva total em horas/dia.
         * Definicao de turnos, turnos permitidos e turnos programados pertencem
         * ao Enterprise/line scheduling e nao sao carregados nesta projection.
         */

        return biProjectionCapacidadeProdutiva;
        
    }

    public BIProjectionCapacidadeProdutiva getBIProjectionCapacidadeProdutivaDeSupplyPlan(
            SupplyPlan supplyPlan, Calendario calendario) {

        SupplyNetworkProjection supplyNetworkProjection = supplyNetworkProjectionFactory.getSupplyNetworkProjectionCompletoDeCache();

        BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva =
                new BIProjectionCapacidadeProdutiva(
                        supplyPlan,
                        calendario,
                        supplyNetworkProjection);

        List<CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan> capacidadesProdutivasEfetivas =
                capacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository.customFindBySupplyPlan(supplyPlan);
        if (capacidadesProdutivasEfetivas == null) {
            throw new IllegalStateException("Persisted effective production capacity repository returned null list for Supply Plan "
                    + supplyPlan.getId()
                    + ".");
        }
        if (capacidadesProdutivasEfetivas.isEmpty()) {
            throw new IllegalStateException("Supply Plan " + supplyPlan.getId() +
                    " nao possui snapshot de capacidade produtiva efetiva.");
        }

        capacidadesProdutivasEfetivas.forEach(biProjectionCapacidadeProdutiva::addDadoAoBI);

        return biProjectionCapacidadeProdutiva;

    }
    /**
     * Valida o Supply Plan recebido pela factory.
     *
     * <p>A capacidade produtiva e parte do fluxo Community de plano restrito.
     * Se o plano nao foi resolvido pelo service chamador, a factory deve falhar
     * antes de carregar malha ou repositories de capacidade.</p>
     */
    /**
     * Valida o calendario funcional usado para filtrar capacidade diaria.
     */
    /**
     * Valida a malha produtiva usada para resolver locations e recursos.
     *
     * <p>A projection de capacidade navega por recursos ativos a partir da
     * Supply Network Projection. Snapshot nulo ou sem parametros deve falhar
     * aqui para impedir BI parcial ou NPE dentro do construtor.</p>
     */

}
