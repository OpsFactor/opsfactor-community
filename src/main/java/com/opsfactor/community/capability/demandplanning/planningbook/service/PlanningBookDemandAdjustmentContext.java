package com.opsfactor.community.capability.demandplanning.planningbook.service;

import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureInterface;
import com.opsfactor.community.capability.planningbook.domain.PlanningBookDfuScope;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.FiltroDFUProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanningProjection;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Contexto efemero de uma chamada de atualizacao do Demand Planning Book.
 *
 * <p>O Community preenche somente a resolucao de key figures standard. Um
 * overlay Enterprise pode especializar este contexto para pre-carregar
 * entidades privadas, permissoes e linhas antes do loop de celulas, sem usar
 * estado mutavel no singleton da fachada e sem resolver uma key figure por
 * celula.</p>
 */
public class PlanningBookDemandAdjustmentContext {

    private final Map<String, KeyFigureInterface> keyFiguresById = new LinkedHashMap<>();
    private String planningBookViewName;
    private final Map<PlanningBookDfuScope, FiltroDFUProjection> dfuFiltersByScope = new LinkedHashMap<>();

    /** Reutiliza a interseção de uma linha entre períodos, validação e aplicação do mesmo lote. */
    public FiltroDFUProjection getOrCreateDfuFilter(
            PlanningBookDfuScope scope, DemandPlanningProjection projection) {

        return dfuFiltersByScope.computeIfAbsent(scope, key -> key.getNovoDFUProjectionInterseccaoComProjectionFiltro(
                projection.getFiltroDfuProjection(), projection.getClusterEParametrosProjection()));

    }


    /**
     * Registra a fotografia de key figures ja resolvidas para o lote atual.
     */
    public void addKeyFigure(KeyFigureInterface keyFigure) {

        if (keyFigure == null || keyFigure.getId() == null || keyFigure.getId().isBlank()) {
            throw new IllegalArgumentException(
                    "Planning Book Demand adjustment context requires a key figure with id.");
        }
        keyFiguresById.put(keyFigure.getId(), keyFigure);

    }

    /**
     * Resolve uma key figure previamente carregada, ou {@code null} se a
     * chave pertence ao resolver padrao Community.
     */
    public KeyFigureInterface getKeyFigureOrNull(String keyFigureId) {

        return keyFiguresById.get(keyFigureId);

    }

    /** Preserva o nome da view já resolvida para capabilities privadas de auditoria. */
    public void setPlanningBookViewName(String planningBookViewName) {

        this.planningBookViewName = planningBookViewName;

    }

    /** Retorna o nome da view, ou {@code null} para chamadas internas sem view. */
    public String getPlanningBookViewName() {

        return planningBookViewName;

    }

}
