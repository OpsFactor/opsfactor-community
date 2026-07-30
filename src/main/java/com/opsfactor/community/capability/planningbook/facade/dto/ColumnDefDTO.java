package com.opsfactor.community.capability.planningbook.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * Definicao de coluna enviada ao front para renderizacao do Planning Book.
 * <p>
 * No Community as colunas representam dimensoes material/location, periodos e
 * key figures padrao. Configuracoes avancadas de apresentacao por
 * caracteristica ou posicao pertencem ao Enterprise.
 */

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ColumnDefDTO {

    // Campo tecnico usado pelo front para identificar a coluna.
    public String field;
    // Nome de exibicao quando diferente do identificador tecnico.
    public String name;
    public Boolean dataColumn;
    // Classe visual opcional calculada pelo backend/front para celulas especiais.
    public String cellClass;
    // material ou location, para o back-end diferenciar inputs de dados.
    public String dimension;
    public String width;
    public Boolean enableCellEdit;
    public Boolean enableFiltering;
    public Boolean enableSorting;
    public Boolean enableHiding;
    public Boolean enablePinning;
    public Boolean pinnedLeft;

}
