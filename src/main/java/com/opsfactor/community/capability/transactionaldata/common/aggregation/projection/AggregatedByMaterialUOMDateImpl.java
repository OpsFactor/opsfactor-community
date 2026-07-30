package com.opsfactor.community.capability.transactionaldata.common.aggregation.projection;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

/**
 * Implementacao concreta para agregados por material/UOM/data.
 */
@Builder
@Getter
public class AggregatedByMaterialUOMDateImpl implements AggregatedByMaterialUOMDate {

    private Produto material;
    private UnidadeMedida uom;
    private LocalDate referenceDate;

    private Double totalQuantity;

}
