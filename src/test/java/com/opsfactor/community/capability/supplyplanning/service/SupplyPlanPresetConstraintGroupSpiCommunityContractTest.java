package com.opsfactor.community.capability.supplyplanning.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.optimizer.presetconstraint.RestricaoPredefinidaGrupo;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.facade.dto.VersaoSupplyPlanDTO;
import com.opsfactor.community.capability.supplyplanning.service.spi.SupplyPlanPresetConstraintGroupSpi;
import com.opsfactor.community.platform.task.SupplyPlanningTask;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.scheduler.domain.ScheduledTaskAbstract;
import com.opsfactor.community.platform.scheduler.services.ScheduledTaskPersistenceService;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Contrato da passagem de preset constraints pelo fluxo compartilhado de
 * Supply Planning.
 */
class SupplyPlanPresetConstraintGroupSpiCommunityContractTest {

    @Test
    void shouldRejectPresetConstraintSelectionWithoutEnterpriseResolver() {

        SupplyPlanService service = new SupplyPlanService();

        RequiresEnterpriseVersionException exception = assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> service.resolvePresetConstraintGroupParaNovoSupplyPlan("GROUP-1"));

        assertEquals(
                "REQUIRES_ENTERPRISE_VERSION: Supply Planning Preset Constraints requires OpsFactor Enterprise.",
                exception.getMessage());

    }

    @Test
    void shouldResolvePresetConstraintOnlyWhenEnterpriseSpiIsPresent() throws Exception {

        SupplyPlanService service = new SupplyPlanService();
        RestricaoPredefinidaGrupo expectedGroup = new RestricaoPredefinidaGrupo();
        expectedGroup.setId("GROUP-1");
        SupplyPlanPresetConstraintGroupSpi resolver = presetConstraintGroupId -> expectedGroup;
        setField(service, "supplyPlanPresetConstraintGroupSpi", resolver);

        assertSame(
                expectedGroup,
                service.resolvePresetConstraintGroupParaNovoSupplyPlan("GROUP-1"));
        assertNull(service.resolvePresetConstraintGroupParaNovoSupplyPlan(null));

    }

    @Test
    void shouldKeepPersistedPresetConstraintGroupInVersionDto() {

        SupplyPlan supplyPlan = new SupplyPlan();
        supplyPlan.setId(15L);
        RestricaoPredefinidaGrupo presetConstraintGroup = new RestricaoPredefinidaGrupo();
        presetConstraintGroup.setId("GROUP-1");
        supplyPlan.setPresetConstraintGroup(presetConstraintGroup);

        /*
         * Apenas os campos lidos pelo construtor são montados. O grupo deve
         * atravessar a serialização de uma reexecução sem consulta adicional.
         */
        DemandPlan demandPlan = new DemandPlan();
        demandPlan.setDescricao("Demand");
        demandPlan.setDataInicioPlano(LocalDateTime.of(2026, 7, 1, 0, 0));
        PerfilExecucaoSupplyPlan executionProfile = new PerfilExecucaoSupplyPlan();
        executionProfile.setId("PROFILE");
        VersaoMalha supplyNetworkVersion = new VersaoMalha();
        supplyNetworkVersion.setId("NETWORK");
        supplyPlan.setDemandPlan(demandPlan);
        supplyPlan.setPerfilExecucaoSupplyPlan(executionProfile);
        supplyPlan.setVersaoMalha(supplyNetworkVersion);
        supplyPlan.setTamanhoBucket(Constantes.TamanhoBucket.MENSAL);
        supplyPlan.setHorarioGeracao(LocalDateTime.now());

        VersaoSupplyPlanDTO result = new VersaoSupplyPlanDTO(supplyPlan);

        assertEquals("GROUP-1", result.getPresetConstraintGroupId());

    }

    @Test
    void shouldPassPresetConstraintGroupIdFromTaskToService() {

        SupplyPlanService service = Mockito.mock(SupplyPlanService.class);
        ScheduledTaskAbstract scheduledTask = Mockito.mock(ScheduledTaskAbstract.class);
        Mockito.when(scheduledTask.getUserId()).thenReturn("user-1");
        SupplyPlanningTask task = new SupplyPlanningTask(
                scheduledTask,
                Mockito.mock(ScheduledTaskPersistenceService.class),
                service);
        VersaoSupplyPlanDTO dto = new VersaoSupplyPlanDTO();
        dto.setDemandPlanId(2L);
        dto.setSupplyPlanId(15L);
        dto.setSupplyPlanIdForStartingStockProjection(14L);
        dto.setExecutionProfileId("PROFILE");
        dto.setSupplyNetworkVersionId("NETWORK");
        dto.setPresetConstraintGroupId("GROUP-1");
        dto.setTamanhoBucket(Constantes.TamanhoBucket.MENSAL);
        dto.setPeriodoReferencia("202607");
        dto.setDescricaoSupplyPlan("Supply");

        task.executaTask(dto, service);

        Mockito.verify(service).executeSupplyPlan(
                Mockito.eq(2L),
                Mockito.eq(15L),
                Mockito.eq(14L),
                Mockito.eq("PROFILE"),
                Mockito.eq("NETWORK"),
                Mockito.eq("GROUP-1"),
                Mockito.eq(Constantes.TamanhoBucket.MENSAL),
                Mockito.any(LocalDateTime.class),
                Mockito.eq("Supply"),
                Mockito.eq("user-1"));

    }

    private static void setField(
            Object target,
            String fieldName,
            Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

}
