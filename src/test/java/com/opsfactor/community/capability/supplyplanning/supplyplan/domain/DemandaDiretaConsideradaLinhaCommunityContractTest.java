package com.opsfactor.community.capability.supplyplanning.supplyplan.domain;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

/**
 * Contrato Community das linhas de demanda direta consideradas pelo Supply Plan.
 *
 * <p>O runtime aberto usa apenas o Demand Plan como fonte de demanda direta;
 * carteira/customer orders e valores economicos ficam preservados no schema
 * para compatibilidade com o overlay Enterprise. As quantidades fisicas,
 * entretanto, alimentam estoque projetado, safety stock e fair share tanto no
 * heuristico Community quanto no otimizador Enterprise, portanto `null`
 * continua significando ausencia operacional zero e valores presentes negativos
 * ou nao finitos devem falhar antes de qualquer soma ou conversao de unidade.</p>
 */
class DemandaDiretaConsideradaLinhaCommunityContractTest {

    @Test
    void directDemandQuantitiesShouldTreatNullAsOperationalZero() {

        DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinha =
                criaDemandaDiretaConsideradaLinha();

        Assertions.assertEquals(
                0.0d,
                demandaDiretaConsideradaLinha.getQuantidadePlanoDemandaOriginal());
        Assertions.assertEquals(
                0.0d,
                demandaDiretaConsideradaLinha.getQuantidadeDemandaDiretaPlanoDemandaIrrestrita());
        Assertions.assertEquals(
                0.0d,
                demandaDiretaConsideradaLinha.getQuantidadeDemandaDiretaEstoqueSeguranca());

    }

    @Test
    void originalDirectDemandQuantitiesShouldRejectNegativeRegisteredValues() {

        DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinha =
                criaDemandaDiretaConsideradaLinha();
        demandaDiretaConsideradaLinha.setQuantidadePlanoDemandaOriginal(-1.0d);

        IllegalStateException illegalStateException =
                Assertions.assertThrows(
                        IllegalStateException.class,
                        demandaDiretaConsideradaLinha::getQuantidadePlanoDemandaOriginal);

        Assertions.assertEquals(
                "Direct demand quantity original demand plan must be finite and non-negative for "
                        + "material MAT / location PLANT / reference date 2026-01-01T00:00: -1.0.",
                illegalStateException.getMessage());

    }

    @Test
    void effectiveDirectDemandQuantitiesShouldRejectNonFiniteRegisteredValues() {

        DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinha =
                criaDemandaDiretaConsideradaLinha();
        demandaDiretaConsideradaLinha.setQuantidadeDemandaDiretaPlanoDemandaRestrita(Double.POSITIVE_INFINITY);

        IllegalStateException illegalStateException =
                Assertions.assertThrows(
                        IllegalStateException.class,
                        demandaDiretaConsideradaLinha::getQuantidadeDemandaDiretaPlanoDemandaRestrita);

        Assertions.assertEquals(
                "Direct demand quantity restricted demand plan direct demand must be finite and non-negative for "
                        + "material MAT / location PLANT / reference date 2026-01-01T00:00: Infinity.",
                illegalStateException.getMessage());

    }

    private static DemandaDiretaConsideradaLinha criaDemandaDiretaConsideradaLinha() {

        DemandaDiretaConsideradaLinha.DemandaDiretaConsideradaLinhaCompositeKey demandaDiretaConsideradaLinhaCompositeKey =
                new DemandaDiretaConsideradaLinha.DemandaDiretaConsideradaLinhaCompositeKey(
                        new SupplyPlan(),
                        new Location("PLANT"),
                        new Produto("MAT"),
                        LocalDateTime.of(2026, 1, 1, 0, 0));

        return new DemandaDiretaConsideradaLinha(demandaDiretaConsideradaLinhaCompositeKey);

    }

}
