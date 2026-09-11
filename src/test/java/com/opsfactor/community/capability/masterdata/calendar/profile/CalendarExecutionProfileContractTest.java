package com.opsfactor.community.capability.masterdata.calendar.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.capability.demandplanning.configuration.facade.dto.PerfilExecucaoDemandPlanDTO;
import com.opsfactor.community.capability.masterdata.calendar.profile.domain.PerfilCalendarioSimples;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.facade.dto.VersaoSupplyPlanDTO;
import com.opsfactor.community.platform.utility.Constantes.TamanhoBucket;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Contratos entre receita, perfil de execução, payload de tela e versão histórica. */
class CalendarExecutionProfileContractTest {

    @Test
    void demandDerivaBucketEHorizonteDaReceitaSemUsarColunasAntigas() {

        var perfil = new PerfilExecucaoDemandPlan("DEMAND");
        perfil.setTamanhoBucket(TamanhoBucket.DIARIO);
        perfil.setNumeroPeriodosHorizontePlanejamento(90);
        perfil.setPerfilCalendario(new PerfilCalendarioSimples("MENSAL_4", TamanhoBucket.MENSAL, 4));

        assertEquals(TamanhoBucket.MENSAL, perfil.getTamanhoBucket());
        assertEquals(4, perfil.getNumeroPeriodosHorizontePlanejamento());
        assertEquals(120, perfil.getNumeroDiasHorizontePlanejamento(LocalDateTime.of(2026, 1, 1, 0, 0)));

    }

    @Test
    void demandNaoAceitaRaizAbstrataNoRelacionamento() throws Exception {

        assertEquals(PerfilCalendarioSimples.class,
                PerfilExecucaoDemandPlan.class.getDeclaredField("perfilCalendario").getType());

    }

    @Test
    void supplyReconstruiCopiaMesmoDepoisDeAlterarPerfilECadastro() {

        var receita = new PerfilCalendarioSimples("DIARIO_17", TamanhoBucket.DIARIO, 17);
        var perfil = new PerfilExecucaoSupplyPlan();
        perfil.setPerfilCalendario(receita);
        var plano = new SupplyPlan();
        plano.setId(1L);
        plano.setPerfilExecucaoSupplyPlan(perfil);
        plano.setDataInicioPlano(LocalDateTime.of(2026, 3, 15, 0, 0));
        plano.setPerfilCalendarioSupplyPlan(receita.copiarParaSupplyPlan(plano));
        receita.setNumeroPeriodosBucketBase(100);
        receita.setTamanhoBucketBase(TamanhoBucket.MENSAL);
        perfil.setPerfilCalendario(new PerfilCalendarioSimples("OUTRO", TamanhoBucket.ANUAL, 3));

        var calendario = plano.getCalendarioDoSupplyPlan(new ParametrosGlobais());
        assertEquals(17, calendario.getNumeroPeriodosFuturos());
        assertEquals(LocalDateTime.of(2026, 3, 31, 23, 59, 59), calendario.getDataHorarioFinal());
        assertEquals("DIARIO_17", plano.getPerfilCalendarioSupplyPlan().getPerfilCalendarioOrigemId());

    }

    @Test
    void planoHistoricoSemCopiaConservaGradePelasDatasSalvas() {

        var plano = new SupplyPlan();
        plano.setTamanhoBucket(TamanhoBucket.MENSAL);
        plano.setDataInicioPlano(LocalDateTime.of(2026, 1, 1, 0, 0));
        plano.setDataFimPlano(LocalDateTime.of(2026, 4, 30, 23, 59, 59));
        var calendario = plano.getCalendarioDoSupplyPlan(new ParametrosGlobais());
        assertEquals(4, calendario.getNumeroPeriodosFuturos());
        assertEquals(TamanhoBucket.MENSAL, calendario.getTamanhoBucket(0));

    }

    @Test
    void dtoDemandExibeBucketDerivadoMasNaoAceitaConfiguracaoTemporalDuplicada() throws Exception {

        var dto = new PerfilExecucaoDemandPlanDTO();
        dto.id = "DEMAND";
        dto.calendarProfileId = "DIARIO_17";
        dto.bucketSize = TamanhoBucket.MENSAL;
        dto.planningHorizonInPeriods = 12;
        var json = new ObjectMapper().valueToTree(dto);
        assertEquals("DIARIO_17", json.path("calendarProfileId").asText());
        assertEquals("Monthly", json.path("bucketSize").asText());
        var recebido = new ObjectMapper().treeToValue(json, PerfilExecucaoDemandPlanDTO.class);
        assertEquals("DIARIO_17", recebido.calendarProfileId);
        assertNull(recebido.bucketSize);
        assertNull(recebido.planningHorizonInPeriods);

    }

    @Test
    void execucaoPreservaDiaEscolhidoSemDerivarDoBucketRaiz() throws Exception {

        var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        var dto = mapper.readValue("{\"executionProfileId\":\"SUPPLY\",\"dataInicioPlano\":\"2026-03-15T00:00:00\"}",
                VersaoSupplyPlanDTO.class);
        assertEquals(LocalDateTime.of(2026, 3, 15, 0, 0), dto.getDataInicioPlano());

    }

}
