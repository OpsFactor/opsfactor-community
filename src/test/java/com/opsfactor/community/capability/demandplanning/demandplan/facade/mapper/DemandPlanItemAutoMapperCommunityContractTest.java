package com.opsfactor.community.capability.demandplanning.demandplan.facade.mapper;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlanItem;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.dto.DemandPlanItemDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

/**
 * Contrato Community do mapper de linhas do Demand Plan.
 *
 * <p>O DTO publico do Planning Book expõe apenas material/location, baseline,
 * ajuste de demanda e totais derivados dessas duas key figures. Uplift e New
 * Products continuam existindo na entidade fisica por compatibilidade de
 * schema, mas nao podem entrar no total exibido pelo Community.</p>
 */
public class DemandPlanItemAutoMapperCommunityContractTest {

    @Test
    public void converteShouldExposeOnlyCommunityKeyFiguresInTotals() {

        DemandPlanItem demandPlanItem = getDemandPlanItemComValoresEnterpriseTransicionais();

        DemandPlanItemDTO demandPlanItemDTO =
                Mappers.getMapper(DemandPlanItemAutoMapper.class)
                        .converte(
                                demandPlanItem,
                                new ParametrosGlobais());

        Assertions.assertEquals("LOC", demandPlanItemDTO.locationId);
        Assertions.assertEquals("MAT", demandPlanItemDTO.materialId);
        Assertions.assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), demandPlanItemDTO.referenceDate);
        Assertions.assertEquals("UN", demandPlanItemDTO.uomId);

        Assertions.assertEquals(10.0d, demandPlanItemDTO.baselineQtyUnconstrained, 0.0001d);
        Assertions.assertEquals(2.0d, demandPlanItemDTO.demandAdjustmentQtyUnconstrained, 0.0001d);
        Assertions.assertEquals(
                12.0d,
                demandPlanItemDTO.totalQtyUnconstrained,
                0.0001d,
                "Total Community deve somar apenas Baseline + Demand Adjustment.");

        Assertions.assertEquals(6.0d, demandPlanItemDTO.baselineQtyConstrained, 0.0001d);
        Assertions.assertEquals(1.0d, demandPlanItemDTO.demandAdjustmentQtyConstrained, 0.0001d);
        Assertions.assertEquals(
                7.0d,
                demandPlanItemDTO.totalQtyConstrained,
                0.0001d,
                "Total restrito Community deve ignorar Uplift/New Products atendidos.");

    }

    private DemandPlanItem getDemandPlanItemComValoresEnterpriseTransicionais() {

        DemandPlanItem demandPlanItem = new DemandPlanItem(
                new DemandPlanItem.DemandPlanItemKey(
                        new DemandPlan(),
                        new Location("LOC"),
                        new Produto("MAT"),
                        LocalDateTime.of(2026, 1, 1, 0, 0)));

        demandPlanItem.setQuantidadeBaseline(10.0d);
        demandPlanItem.setQuantidadeAjusteDemanda(2.0d);
        demandPlanItem.setQuantidadeUplift(100.0d);
        demandPlanItem.setQuantidadeItensNovos(100.0d);

        demandPlanItem.setQuantidadeBaselineAtendida(6.0d);
        demandPlanItem.setQuantidadeAjusteDemandaAtendida(1.0d);
        demandPlanItem.setQuantidadeUpliftAtendida(100.0d);
        demandPlanItem.setQuantidadeItensNovosAtendida(100.0d);

        return demandPlanItem;

    }

}
