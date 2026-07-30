package com.opsfactor.community.capability.masterdata.calendar.temporalsplit.projection;

import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosDemandPlanProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.factory.ParametrosDemandPlanningProjectionFactory;
import com.opsfactor.community.platform.calendar.Calendario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Permite conversoes temporais basicas entre calendarios no Community.
 *
 * <p>A edicao Community deve usar somente a curva flat implicita. Curvas
 * cadastradas por DFU, com filtros e pesos especificos, pertencem ao Enterprise.
 * Por isso a factory nao recebe entidades de curva temporal configuravel nesta
 * edicao; o Enterprise reintroduz esse caminho com sua propria implementacao.</p>
 */
@Component
public class SplitTemporalProjectionFactory {

    /**
     * Factory da projection central usada para carregar parametros globais do
     * calendario Supply.
     */
    @Autowired
    private ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory;

    /**
     * Factory de parametros Demand Planning usada para reconstruir o calendario
     * origem completo do Demand Plan.
     */
    @Autowired
    private ParametrosDemandPlanningProjectionFactory parametrosDemandPlanningProjectionFactory;


    public static SplitTemporalProjection geraSplitTemporalProjectionComCurvaFlat(Calendario calendarioOrigem, Calendario calendarioTarget) {

        SplitTemporalProjection splitTemporalProjection = new SplitTemporalProjection(
                calendarioOrigem,
                calendarioTarget);
        incorporaSplitTemporalProjectionCurvaFlat(splitTemporalProjection);
        return splitTemporalProjection;
    }

    public SplitTemporalProjectionPorDfu geraSplitTemporalProjectionPorDfu(DemandPlan demandPlan, SupplyPlan supplyPlan) {

        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();

        ParametrosDemandPlanProjection parametrosDemandPlanProjection = parametrosDemandPlanningProjectionFactory.getParametrosDemandPlanProjectionDeCache(
                        demandPlan.getPerfilExecucaoDemandPlan());
        ParametrosGlobais parametrosGlobais = clusterEParametrosProjection.getParametrosGlobais();

        return geraSplitTemporalProjectionPorDfu(
                demandPlan.getCalendarioDoDemandPlanComHistoricoMaximo(parametrosDemandPlanProjection),
                supplyPlan.getCalendarioDoSupplyPlan(parametrosGlobais));
    }

    public SplitTemporalProjectionPorDfu geraSplitTemporalProjectionPorDfu(Calendario calendarioOrigem, Calendario calendarioDestino) {


        SplitTemporalProjectionPorDfu splitTemporalProjectionPorDfu = new SplitTemporalProjectionPorDfu(
                calendarioOrigem,
                calendarioDestino);

        // Community sempre usa curva flat para distribuir o valor do periodo
        // original no calendario target, sem selecao por material/location.
        incorporaSplitTemporalProjectionCurvaFlat(splitTemporalProjectionPorDfu);

        return splitTemporalProjectionPorDfu;

    }

    /**
     * Valida o Demand Plan usado para montar split temporal DFU.
     */
    /**
     * Valida o Supply Plan usado como calendario destino do split temporal DFU.
     */
    /**
     * Valida a projection central antes de ler parametros globais.
     *
     * <p>Mesmo com curva flat, o split Demand->Supply precisa dos calendarios
     * funcionais dos planos. Snapshot de cluster/parametros quebrado deve
     * falhar antes de carregar parametros Demand ou calcular calendarios.</p>
     */
    /**
     * Valida os parametros Demand Planning que constroem o calendario origem.
     */
    /**
     * Valida calendarios explicitos usados pelo split temporal flat.
     */
    private static void incorporaSplitTemporalProjectionCurvaFlat(
            SplitTemporalProjection splitTemporalProjection) {
        splitTemporalProjection.splitTemporalProjectionCurvaBase = new SplitTemporalProjectionCurvaFlat(
                splitTemporalProjection.getCalendarioOrigem(),
                splitTemporalProjection.getCalendarioTarget());
    }



}
