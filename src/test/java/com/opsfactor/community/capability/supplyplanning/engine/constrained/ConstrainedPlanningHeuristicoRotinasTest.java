package com.opsfactor.community.capability.supplyplanning.engine.constrained;

import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Contratos das rotinas quantitativas do plano restrito heuristico.
 */
public class ConstrainedPlanningHeuristicoRotinasTest {

    @Test
    public void fulfilledDemandShouldNotExceedConstrainedClientInbound() {

        DistributionPlanItem distributionPlanItem = new DistributionPlanItem();
        distributionPlanItem.setQuantidade(
                95.0d,
                Constantes.FirmePlanejado.PLANEJADO,
                Constantes.TipoPlano.PLANO_RESTRITO);
        distributionPlanItem.setParcelaParaAtendimentoDemandaDireta(
                120.0d,
                Constantes.FirmePlanejado.PLANEJADO,
                Constantes.TipoPlano.PLANO_RESTRITO);
        distributionPlanItem.setQuantidade(
                30.0d,
                Constantes.FirmePlanejado.ORDEM,
                Constantes.TipoPlano.PLANO_RESTRITO);
        distributionPlanItem.setParcelaParaAtendimentoDemandaDireta(
                50.0d,
                Constantes.FirmePlanejado.ORDEM,
                Constantes.TipoPlano.PLANO_RESTRITO);

        ConstrainedPlanningHeuristicoRotinas
                .limitaParcelasAtendimentoDemandaDiretaAoVolumeRestrito(
                        List.of(distributionPlanItem));

        Assertions.assertEquals(
                95.0d,
                distributionPlanItem.getParcelaParaAtendimentoIndiretoDemandaDireta(
                        Constantes.FirmePlanejado.PLANEJADO,
                        Constantes.TipoPlano.PLANO_RESTRITO));
        Assertions.assertEquals(
                30.0d,
                distributionPlanItem.getParcelaParaAtendimentoIndiretoDemandaDireta(
                        Constantes.FirmePlanejado.ORDEM,
                        Constantes.TipoPlano.PLANO_RESTRITO));

    }

    @Test
    public void fulfilledDemandBelowConstrainedInboundShouldRemainUnchanged() {

        DistributionPlanItem distributionPlanItem = new DistributionPlanItem();
        distributionPlanItem.setQuantidade(
                100.0d,
                Constantes.FirmePlanejado.PLANEJADO,
                Constantes.TipoPlano.PLANO_RESTRITO);
        distributionPlanItem.setParcelaParaAtendimentoDemandaDireta(
                80.0d,
                Constantes.FirmePlanejado.PLANEJADO,
                Constantes.TipoPlano.PLANO_RESTRITO);

        ConstrainedPlanningHeuristicoRotinas
                .limitaParcelasAtendimentoDemandaDiretaAoVolumeRestrito(
                        List.of(distributionPlanItem));

        Assertions.assertEquals(
                80.0d,
                distributionPlanItem.getParcelaParaAtendimentoIndiretoDemandaDireta(
                        Constantes.FirmePlanejado.PLANEJADO,
                        Constantes.TipoPlano.PLANO_RESTRITO));

    }
}
