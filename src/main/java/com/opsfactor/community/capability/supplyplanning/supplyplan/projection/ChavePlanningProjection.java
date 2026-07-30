package com.opsfactor.community.capability.supplyplanning.supplyplan.projection;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Chave simples material/periodo usada em projections de planejamento.
 *
 * <p>O periodo e mantido no formato inteiro legado do calendario
 * correspondente, como YYYYMM, YYYYWW ou YYYYMMDD.</p>
 */
@Data
@AllArgsConstructor
@Builder
public class ChavePlanningProjection {

    /**
     * Periodo no formato inteiro do calendario origem.
     */
    Integer periodo;

    /**
     * Material associado ao periodo.
     */
    Produto material;

}
