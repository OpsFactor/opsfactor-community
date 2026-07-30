package com.opsfactor.community.capability.planningbook.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Celula material/location selecionada no Planning Book para atualizacao.
 *
 * <p>Este objeto e um envelope tecnico criado a partir do DTO da SPA depois
 * que a view Community ja foi resolvida. Ele nao representa ajuste agregado,
 * filtro por caracteristica ou upload de arquivo; esses fluxos pertencem ao
 * Enterprise e devem falhar antes de criar esta instancia.</p>
 *
 * <p>A validacao estrutural fica em {@code ConfiguredViewProjection}, porque
 * o mesmo envelope e consumido por Demand Planning, Supply Planning e pelo
 * Planning Book ao renderizar erros de celula.</p>
 */
@Getter
@Setter
@AllArgsConstructor
public class AjusteCelulaPlanningBook {

    /*
     * Periodo/bucket da celula alterada, ja convertido pelo calendario do plano
     * para a data-hora de referencia usada pelas key figures.
     */
    private LocalDateTime dataHorarioReferencia;

    /*
     * Id tecnico da key figure editada. A validacao de qual KF e editavel no
     * Community pertence ao service de Demand/Supply que aplica o ajuste.
     */
    private String keyFigureId;

    /*
     * Unidade informada pela tela para converter o valor de entrada para a
     * unidade funcional do plano/projection.
     */
    private String uomId;

    /*
     * Novo valor digitado na celula. Precisa ser finito antes de qualquer
     * tentativa de propagacao, split ou persistencia.
     */
    private Double valorNovo;

    /*
     * Valor antigo exibido pela tela. Ele e mantido para auditoria/retorno de
     * front, mas nao deve ser usado como fallback silencioso se o novo valor
     * vier quebrado.
     */
    private Double valorAntigo;

    /*
     * Escopo material/location da celula alterada. No Community, a validacao
     * anterior garante que o escopo represente exatamente um DFU; ajustes
     * agregados pertencem ao Enterprise e falham antes de chegar aqui.
     */
    private PlanningBookDfuScope planningBookDfuScope;

}
