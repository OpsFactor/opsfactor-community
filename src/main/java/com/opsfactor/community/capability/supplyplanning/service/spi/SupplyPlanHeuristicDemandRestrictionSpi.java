package com.opsfactor.community.capability.supplyplanning.service.spi;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.supplyplanning.engine.constrained.CorteProporcional;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.DemandaDiretaConsideradaLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.platform.utility.Constantes.TipoPlano;

import java.util.List;

/** Integra fontes opcionais de consumo ao saldo e à restrição do heurístico. */
public interface SupplyPlanHeuristicDemandRestrictionSpi {

    /** Consumo no período que ainda não está representado no saldo comum. */
    double getConsumoAdicional(
            SupplyPlanningProjection projection, int periodo, Produto material,
            TipoPlano tipoPlano, UnidadeMedida unidade);

    /** Parcelas restritas elegíveis após o corte do plano de demanda. */
    List<CorteProporcional.Parcela> getParcelasDemandaAdicional(
            SupplyPlanningProjection projection, int periodo, Produto material,
            UnidadeMedida unidade);

    /** Permite que a extensão acompanhe o corte físico do plano de demanda. */
    default void aplicaCorteValoresPlanoDemanda(DemandaDiretaConsideradaLinha linha, double fracaoCortada) {
    }

}
