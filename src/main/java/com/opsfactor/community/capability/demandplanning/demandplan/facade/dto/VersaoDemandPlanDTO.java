package com.opsfactor.community.capability.demandplanning.demandplan.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO de versao/execucao de Demand Plan usado em listagens e no disparo de
 * geracao.
 *
 * <p>Campos de copia a partir de outro Demand Plan permanecem no DTO para
 * compatibilidade de payload, mas o Community bloqueia esses valores no
 * `DemandPlanningService` antes de criar uma nova versao.</p>
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VersaoDemandPlanDTO implements Serializable {

    /**
     * Identificador da versao salva.
     */
    Long id;

    /**
     * Descricao funcional da versao.
     */
    String descricao;

    /**
     * Granularidade temporal do perfil que originou a versao.
     *
     * <p>O front legado recebe este campo na listagem de versoes para
     * interpretar periodos e labels sem inferir o bucket pelo plano. A fonte
     * canonica e o perfil de execucao ja carregado em batch pela listagem.</p>
     */
    Constantes.TamanhoBucket bucketSize;

    /**
     * Perfil de execucao usado ou solicitado.
     */
    String executionProfileId;

    /**
     * Data/hora em que a versao foi gerada.
     */
    LocalDateTime horarioGeracao;

    /**
     * Periodo textual de referencia da versao.
     */
    String periodoReferencia;

    /**
     * Primeira data do horizonte planejado.
     */
    LocalDateTime planStartDate;

    /**
     * Ultima data do horizonte planejado.
     */
    LocalDateTime planEndDate;

    /**
     * Plano de referencia para copia de dados. Recurso Enterprise; no Community
     * qualquer valor nao nulo deve falhar antes da execucao.
     */
    Long demandPlanReferenciaCopiaDados;

    /**
     * Indica copia apenas no horizonte congelado. Recurso Enterprise; no
     * Community `true` deve falhar antes da execucao.
     */
    Boolean copiaApenasNoHorizonteCongelado;

    /**
     * Monta o DTO a partir de uma entidade Demand Plan salva.
     */
    public VersaoDemandPlanDTO(DemandPlan demandPlan) {

        id = demandPlan.getId();
        descricao = demandPlan.getDescricao();
        executionProfileId = demandPlan.getPerfilExecucaoDemandPlan().getId();
        bucketSize = demandPlan.getPerfilExecucaoDemandPlan().getTamanhoBucket();
        horarioGeracao = demandPlan.getHorarioGeracao();
        periodoReferencia = Calendario.getDescricaoPeriodo(demandPlan.getDataInicioPlano(), demandPlan.getTamanhoBucket());
        planStartDate = demandPlan.getDataInicioPlano();
        planEndDate = demandPlan.getDataFimPlano();

    }

    /**
     * Normaliza nulo como falso para manter compatibilidade com payloads antigos
     * que nao enviam o campo.
     */
    public boolean getCopiaApenasNoHorizonteCongelado() {

        return (copiaApenasNoHorizonteCongelado == null) ? false : copiaApenasNoHorizonteCongelado;

    }

}
