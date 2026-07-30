package com.opsfactor.community.capability.configuration.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * DTO usado para passar a selecao de uma view para o back-end.
 *
 * <p>No Community, `referencePlanId` permanece apenas para payloads legados ou
 * transicionais. O Planning Book Community nao permite plano de
 * referencia/comparacao em Demand Planning; `DemandPlanningFrontService`
 * bloqueia esse campo com `RequiresEnterpriseVersionException` quando ele vem
 * preenchido.</p>
 *
 * O id do usuario nao e necessario pois e puxado atraves da sessao.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfiguredViewSelectionDTO {
    
    // id usuário não é necessário : é puxado através da sessão
    public String viewName;
    // ex. ID do plano de demanda
    public String planId;
    // Campo Enterprise no Community: plano mostrado a titulo de comparacao/referencia.
    public String referencePlanId;
    // Identificadores transitórios Enterprise usados somente para abrir uma
    // grade read-only agrupada. Eles não pertencem à ConfiguredView nem são
    // aceitos pela abertura Community.
    public String materialAggregationLevelId;
    public String locationAggregationLevelId;
    // ex. ID da location para supply plan. não necessário para demand plan
    public String locationId;
    
    public List<String> locationIdList;
}
