package com.opsfactor.community.capability.demandplanning.demandplan.facade.mapper;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlanItem;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.dto.DemandPlanDTO;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.dto.DemandPlanItemDTO;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

/**
 * Contrato Community do mapper de Demand Plan completo.
 *
 * <p>O mapeamento detalhado precisa propagar `ParametrosGlobais` ate o mapper
 * de linha para resolver UOM fallback. Sem esse contexto, o MapStruct gera
 * linhas vazias porque nao consegue chamar `DemandPlanItemAutoMapper`.</p>
 */
public class DemandPlanAutoMapperCommunityContractTest {

    @Test
    public void converteShouldMapDemandPlanDetailWithCommunityLineShape() throws Exception {

        DemandPlan demandPlan = getDemandPlanComLinha();

        DemandPlanDTO demandPlanDTO =
                getDemandPlanAutoMapperComDependencias()
                        .converte(
                                demandPlan,
                                new ParametrosGlobais());

        Assertions.assertEquals(10L, demandPlanDTO.demandPlanId);
        Assertions.assertEquals("Plano Teste", demandPlanDTO.description);
        Assertions.assertEquals("DP_PROFILE", demandPlanDTO.executionProfileId);
        Assertions.assertEquals(Constantes.TamanhoBucket.MENSAL, demandPlanDTO.bucketSize);
        Assertions.assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), demandPlanDTO.beginsOn);

        Assertions.assertNotNull(demandPlanDTO.demandPlanDetail);
        Assertions.assertEquals(1, demandPlanDTO.demandPlanDetail.size());

        DemandPlanItemDTO demandPlanItemDTO = demandPlanDTO.demandPlanDetail.get(0);
        Assertions.assertEquals("LOC", demandPlanItemDTO.locationId);
        Assertions.assertEquals("MAT", demandPlanItemDTO.materialId);
        Assertions.assertEquals("UN", demandPlanItemDTO.uomId);
        Assertions.assertEquals(12.0d, demandPlanItemDTO.totalQtyUnconstrained, 0.0001d);

    }

    @Test
    public void converteSemLinhasShouldKeepDemandPlanDetailNull() throws Exception {

        DemandPlan demandPlan = getDemandPlanComLinha();

        DemandPlanDTO demandPlanDTO =
                getDemandPlanAutoMapperComDependencias()
                        .converteSemLinhas(demandPlan);

        Assertions.assertEquals(10L, demandPlanDTO.demandPlanId);
        Assertions.assertNull(
                demandPlanDTO.demandPlanDetail,
                "Listagens e seletores nao devem carregar linhas de Demand Plan.");

    }

    private DemandPlan getDemandPlanComLinha() {

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan = new PerfilExecucaoDemandPlan();
        perfilExecucaoDemandPlan.setId("DP_PROFILE");

        DemandPlan demandPlan = new DemandPlan();
        demandPlan.setId(10L);
        demandPlan.setDescricao("Plano Teste");
        demandPlan.setPerfilExecucaoDemandPlan(perfilExecucaoDemandPlan);
        demandPlan.setTamanhoBucket(Constantes.TamanhoBucket.MENSAL);
        demandPlan.setDataInicioPlano(LocalDateTime.of(2026, 1, 1, 0, 0));

        DemandPlanItem demandPlanItem = new DemandPlanItem(
                new DemandPlanItem.DemandPlanItemKey(
                        demandPlan,
                        new Location("LOC"),
                        new Produto("MAT"),
                        LocalDateTime.of(2026, 1, 31, 0, 0)));
        demandPlanItem.setQuantidadeBaseline(10.0d);
        demandPlanItem.setQuantidadeAjusteDemanda(2.0d);
        demandPlanItem.setQuantidadeUplift(100.0d);
        demandPlanItem.setQuantidadeItensNovos(100.0d);

        demandPlan.getLinhasDemandPlan().add(demandPlanItem);
        return demandPlan;

    }

    private DemandPlanAutoMapper getDemandPlanAutoMapperComDependencias() throws Exception {

        DemandPlanAutoMapper demandPlanAutoMapper =
                Mappers.getMapper(DemandPlanAutoMapper.class);
        Field demandPlanItemAutoMapperField =
                demandPlanAutoMapper.getClass().getDeclaredField("demandPlanItemAutoMapper");
        demandPlanItemAutoMapperField.setAccessible(true);
        demandPlanItemAutoMapperField.set(
                demandPlanAutoMapper,
                Mappers.getMapper(DemandPlanItemAutoMapper.class));
        return demandPlanAutoMapper;

    }

}
