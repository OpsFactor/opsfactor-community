package com.opsfactor.community.capability.masterdata.calendar.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsfactor.community.capability.masterdata.calendar.profile.domain.PerfilCalendarioSimples;
import com.opsfactor.community.capability.masterdata.calendar.profile.facade.PerfilCalendarioFacade;
import com.opsfactor.community.capability.masterdata.calendar.profile.repository.PerfilCalendarioRepository;
import com.opsfactor.community.capability.masterdata.calendar.profile.repository.PerfilCalendarioSimplesRepository;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes.TamanhoBucket;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Regressões do contrato uniforme, snapshot por valor e fronteira pública. */
class PerfilCalendarioCommunityTest {

    @Test
    void copiaReceitaSemDependerDoCadastroAtual() {

        var perfil = new PerfilCalendarioSimples("MENSAL_4", TamanhoBucket.MENSAL, 4);
        perfil.setDescricao("Original");
        var supplyPlan = new SupplyPlan();
        supplyPlan.setId(9L);
        var copia = perfil.copiarParaSupplyPlan(supplyPlan);
        perfil.setNumeroPeriodosBucketBase(12);
        perfil.setDescricao("Alterado");
        perfil.setTamanhoBucketBase(TamanhoBucket.SEMANAL);

        assertEquals(4, copia.getNumeroPeriodosBucketBase());
        assertEquals("Original", copia.getDescricao());
        assertEquals("MENSAL_4", copia.getPerfilCalendarioOrigemId());
        assertNull(copia.getSupplyPlanId(), "@MapsId atribui a PK somente ao persistir a nova cópia");
        assertEquals(9L, copia.getSupplyPlan().getId());
        assertEquals(TamanhoBucket.MENSAL, copia.getTamanhoBucketPrimeiroPeriodo());
        assertEquals(4, copia.criarCalendario(LocalDateTime.of(2026, 3, 1, 0, 0)).getNumeroPeriodosFuturos());

    }

    @Test
    void rejeitaReceitaSemHorizonteOuIdentificador() {

        assertThrows(IllegalStateException.class,
                () -> new PerfilCalendarioSimples("", TamanhoBucket.MENSAL, 4).validarEstrutura());
        assertThrows(IllegalStateException.class,
                () -> new PerfilCalendarioSimples("INVALIDO", TamanhoBucket.MENSAL, 0).validarEstrutura());

    }

    @Test
    void rejeitaTipoPrivadoAntesDeAcessarBanco() throws Exception {

        var repository = mock(PerfilCalendarioRepository.class);
        var simplesRepository = mock(PerfilCalendarioSimplesRepository.class);
        var mapper = new ObjectMapper();
        var facade = new PerfilCalendarioFacade(repository, simplesRepository, mapper);
        assertThrows(RequiresEnterpriseVersionException.class,
                () -> facade.salvar(mapper.readTree("{\"id\":\"PRIVADO\",\"type\":\"COMPOSTO\",\"items\":[]}")));
        verifyNoInteractions(repository, simplesRepository);

    }

    @Test
    void catalogoDemandUsaConsultaTipadaECalculaBucketInicial() {

        var repository = mock(PerfilCalendarioRepository.class);
        var simplesRepository = mock(PerfilCalendarioSimplesRepository.class);
        when(simplesRepository.findAll()).thenReturn(List.of(
                new PerfilCalendarioSimples("SEMANAL", TamanhoBucket.SEMANAL, 12)));
        var facade = new PerfilCalendarioFacade(repository, simplesRepository, new ObjectMapper());

        var dto = facade.listar(true).getFirst();
        assertEquals("SIMPLES", dto.getType());
        assertEquals(TamanhoBucket.SEMANAL, dto.getFirstPeriodBucketSize());
        assertEquals(12, dto.getNumberOfBasePeriods());
        verifyNoInteractions(repository);

    }

}
