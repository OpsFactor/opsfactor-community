package com.opsfactor.community.platform.calendar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsfactor.community.capability.masterdata.calendar.profile.domain.PerfilCalendario;
import com.opsfactor.community.capability.masterdata.calendar.profile.dto.PerfilCalendarioDTO;
import com.opsfactor.community.capability.masterdata.calendar.profile.facade.PerfilCalendarioFacade;
import com.opsfactor.community.capability.masterdata.calendar.profile.repository.PerfilCalendarioRepository;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.calendar.PerfilCalendarioSimplesSupplyPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.calendar.PerfilCalendarioSupplyPlanRepository;
import com.opsfactor.community.platform.database.CalendarProfileMigrationInitializer;
import com.opsfactor.community.platform.database.CommunityJpaConfiguration;
import com.opsfactor.community.platform.utility.Constantes.TamanhoBucket;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Persistência real exclusivamente em H2 temporário; nenhum datasource operacional é usado. */
@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers_skip_column_definitions=true",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true"})
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2, replace = AutoConfigureTestDatabase.Replace.ANY)
@ContextConfiguration(classes = CommunityJpaConfiguration.class)
@Import({PerfilCalendarioFacade.class, ObjectMapper.class})
class CalendarProfileCommunityPersistenceTest {

    @Autowired EntityManager entityManager;
    @Autowired PerfilCalendarioFacade facade;
    @Autowired ObjectMapper objectMapper;
    @Autowired PerfilCalendarioRepository perfilCalendarioRepository;
    @Autowired PerfilCalendarioSupplyPlanRepository perfilCalendarioSupplyPlanRepository;

    /**
     * Executa o backfill real na transação H2 e força reload das associações.
     * A seleção precisa verificar a inexistência da linha filha, não testar a
     * PK preenchida do pai ao navegar no @OneToOne inverso com @MapsId.
     */
    @Test
    void migracaoEncontraPlanosSemLinhaFilhaPreservaCopiasEPermaneceIdempotente() {

        SupplyPlan mensal = new SupplyPlan();
        mensal.setTamanhoBucket(TamanhoBucket.MENSAL);
        mensal.setDataInicioPlano(LocalDateTime.of(2026, 3, 1, 0, 0));
        mensal.setDataFimPlano(LocalDateTime.of(2026, 6, 30, 23, 59, 59));
        entityManager.persist(mensal);

        SupplyPlan diario = new SupplyPlan();
        diario.setTamanhoBucket(TamanhoBucket.DIARIO);
        diario.setDataInicioPlano(LocalDateTime.of(2026, 3, 15, 0, 0));
        diario.setDataFimPlano(LocalDateTime.of(2026, 3, 17, 23, 59, 59));
        entityManager.persist(diario);

        // Informação insuficiente deve permanecer pendente, nunca ganhar defaults.
        SupplyPlan incompleto = new SupplyPlan();
        entityManager.persist(incompleto);

        SupplyPlan existente = new SupplyPlan();
        existente.setTamanhoBucket(TamanhoBucket.MENSAL);
        existente.setDataInicioPlano(LocalDateTime.of(2026, 1, 1, 0, 0));
        existente.setDataFimPlano(LocalDateTime.of(2026, 12, 31, 23, 59, 59));
        var copiaExistente = new PerfilCalendarioSimplesSupplyPlan(existente, TamanhoBucket.DIARIO, 9);
        copiaExistente.setDescricao("Cópia original preservada");
        existente.setPerfilCalendarioSupplyPlan(copiaExistente);
        entityManager.persist(existente);
        entityManager.flush();
        entityManager.clear();

        // Não registra runner automático nem habilita a migração nos defaults.
        // A invocação explícita participa da transação isolada deste teste H2.
        var inicializador = new CalendarProfileMigrationInitializer(entityManager,
                perfilCalendarioRepository, perfilCalendarioSupplyPlanRepository);
        inicializador.run(new DefaultApplicationArguments(new String[0]));
        entityManager.clear();

        assertEquals(3L, perfilCalendarioSupplyPlanRepository.count());
        var copiaMensal = (PerfilCalendarioSimplesSupplyPlan) Hibernate.unproxy(perfilCalendarioSupplyPlanRepository
                .findById(mensal.getId()).orElseThrow());
        assertEquals(4, copiaMensal.getNumeroPeriodosBucketBase());
        assertEquals(TamanhoBucket.MENSAL, copiaMensal.getTamanhoBucketBase());
        assertNull(copiaMensal.getPerfilCalendarioOrigemId());
        SupplyPlan mensalRecarregado = entityManager.createQuery("select p from SupplyPlan p "
                        + "left join fetch p.perfilCalendarioSupplyPlan where p.id = :id", SupplyPlan.class)
                .setParameter("id", mensal.getId()).getSingleResult();
        assertEquals(mensal.getId(), mensalRecarregado.getPerfilCalendarioSupplyPlan().getSupplyPlanId());
        var copiaDiaria = (PerfilCalendarioSimplesSupplyPlan) Hibernate.unproxy(perfilCalendarioSupplyPlanRepository
                .findById(diario.getId()).orElseThrow());
        assertEquals(3, copiaDiaria.getNumeroPeriodosBucketBase());
        assertEquals(TamanhoBucket.DIARIO, copiaDiaria.getTamanhoBucketBase());
        assertFalse(perfilCalendarioSupplyPlanRepository.existsById(incompleto.getId()));

        inicializador.run(new DefaultApplicationArguments(new String[0]));
        entityManager.clear();
        assertEquals(3L, perfilCalendarioSupplyPlanRepository.count());
        var copiaPreservada = (PerfilCalendarioSimplesSupplyPlan) Hibernate.unproxy(perfilCalendarioSupplyPlanRepository
                .findById(existente.getId()).orElseThrow());
        assertEquals(9, copiaPreservada.getNumeroPeriodosBucketBase());
        assertEquals(TamanhoBucket.DIARIO, copiaPreservada.getTamanhoBucketBase());
        assertEquals("Cópia original preservada", copiaPreservada.getDescricao());

    }

    @Test
    void mapsIdCascadePreservaReceitaELePlanoAntigoSemSnapshot() {

        PerfilCalendarioDTO dto = new PerfilCalendarioDTO();
        dto.setId("PERSISTENCIA");
        dto.setDescription("Original");
        dto.setBaseBucketSize(TamanhoBucket.MENSAL);
        dto.setNumberOfBasePeriods(4);
        facade.salvar(objectMapper.valueToTree(dto));
        PerfilCalendario receita = facade.obterPerfilCompleto(dto.getId());
        SupplyPlan plano = new SupplyPlan();
        plano.setPerfilCalendarioSupplyPlan(receita.copiarParaSupplyPlan(plano));
        entityManager.persist(plano);
        SupplyPlan planoAntigo = new SupplyPlan();
        entityManager.persist(planoAntigo);
        entityManager.flush();
        Long planoId = plano.getId();
        Long antigoId = planoAntigo.getId();
        assertEquals(planoId, plano.getPerfilCalendarioSupplyPlan().getSupplyPlanId());
        dto.setNumberOfBasePeriods(12);
        dto.setDescription("Alterado");
        facade.salvar(objectMapper.valueToTree(dto));
        entityManager.flush();
        entityManager.clear();

        SupplyPlan carregado = entityManager.createQuery("select p from SupplyPlan p "
                + "left join fetch p.perfilCalendarioSupplyPlan where p.id = :id", SupplyPlan.class)
                .setParameter("id", planoId).getSingleResult();
        var copia = (PerfilCalendarioSimplesSupplyPlan) Hibernate.unproxy(carregado.getPerfilCalendarioSupplyPlan());
        assertEquals(4, copia.getNumeroPeriodosBucketBase());
        assertEquals("Original", copia.getDescricao());
        assertEquals("PERSISTENCIA", copia.getPerfilCalendarioOrigemId());
        assertNull(entityManager.createQuery("select p from SupplyPlan p "
                        + "left join fetch p.perfilCalendarioSupplyPlan where p.id = :id", SupplyPlan.class)
                .setParameter("id", antigoId).getSingleResult().getPerfilCalendarioSupplyPlan());
        entityManager.remove(carregado);
        entityManager.flush();
        assertEquals(0L, entityManager.createQuery("select count(p) from PerfilCalendarioSupplyPlan p", Long.class)
                .getSingleResult());

    }

}
