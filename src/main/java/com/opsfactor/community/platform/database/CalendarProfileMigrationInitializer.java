package com.opsfactor.community.platform.database;

import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.capability.masterdata.calendar.profile.domain.PerfilCalendario;
import com.opsfactor.community.capability.masterdata.calendar.profile.domain.PerfilCalendarioSimples;
import com.opsfactor.community.capability.masterdata.calendar.profile.repository.PerfilCalendarioRepository;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.calendar.PerfilCalendarioSimplesSupplyPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.calendar.PerfilCalendarioSupplyPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.calendar.PerfilCalendarioSupplyPlanRepository;
import com.opsfactor.community.platform.utility.Constantes.TamanhoBucket;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Backfill pós-schema, desabilitado por padrão em ambas as edições.
 *
 * <p>O operador habilita explicitamente {@code opsfactor.calendar-profile-migration.enabled=true}
 * após backup e disponibilização do schema. Não altera DDL nem inventa bucket
 * Supply. Uma falha de persistência desfaz toda esta transação. Registros com
 * ambiguidade permanecem intocados e são identificados nos avisos operacionais.</p>
 */
@Component
@ConditionalOnProperty(name = "opsfactor.calendar-profile-migration.enabled", havingValue = "true")
public class CalendarProfileMigrationInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(CalendarProfileMigrationInitializer.class);

    private final EntityManager entityManager;
    private final PerfilCalendarioRepository perfilCalendarioRepository;
    private final PerfilCalendarioSupplyPlanRepository perfilCalendarioSupplyPlanRepository;

    /**
     * Injeta os acessos em lote ao catálogo e às cópias históricas da migração opt-in.
     */
    @Autowired
    public CalendarProfileMigrationInitializer(EntityManager entityManager,
                                              PerfilCalendarioRepository perfilCalendarioRepository,
                                              PerfilCalendarioSupplyPlanRepository perfilCalendarioSupplyPlanRepository) {

        this.entityManager = entityManager;
        this.perfilCalendarioRepository = perfilCalendarioRepository;
        this.perfilCalendarioSupplyPlanRepository = perfilCalendarioSupplyPlanRepository;

    }

    /** Executa depois de Hibernate disponibilizar o schema, usando consultas e gravações em lote. */
    @Override
    @Transactional
    @CacheEvict(value = "parametrosDemandPlanProjection", allEntries = true)
    public void run(ApplicationArguments arguments) {

        migrarPerfisDemandExplicitos();
        migrarCopiasSupplyHistoricas();
        // Um perfil Supply antigo guarda horizonte em dias, mas não bucket.
        // Mesmo um único plano anterior não prova qual receita o usuário deseja
        // agora: o número de meses coberto por N dias varia com a data inicial.
        List<String> perfisSupplyPendentes = entityManager.createQuery(
                "select p.id from PerfilExecucaoSupplyPlan p where p.perfilCalendario is null", String.class)
                .getResultList();
        if (!perfisSupplyPendentes.isEmpty()) {
            LOGGER.warn("Calendar profile migration: configure calendarProfileId before new Supply executions; "
                    + "legacy profiles have no unambiguous bucket: {}", perfisSupplyPendentes);
        }

    }

    private void migrarPerfisDemandExplicitos() {

        // Selecionar também as colunas brutas evita os getters históricos com
        // defaults divergentes. Nenhum relacionamento é percorrido por perfil.
        List<Object[]> pendentes = entityManager.createQuery(
                "select p, p.tamanhoBucket, p.numeroPeriodosHorizontePlanejamento "
                        + "from PerfilExecucaoDemandPlan p where p.perfilCalendario is null", Object[].class)
                .getResultList();
        Map<String, PerfilCalendario> catalogo = new HashMap<>();
        perfilCalendarioRepository.findAll().forEach(perfil -> catalogo.put(perfil.getId(),
                (PerfilCalendario) Hibernate.unproxy(perfil)));
        List<PerfilCalendario> novasReceitas = new ArrayList<>();
        Map<PerfilExecucaoDemandPlan, String> vinculos = new HashMap<>();
        for (Object[] registro : pendentes) {
            PerfilExecucaoDemandPlan perfilExecucao = (PerfilExecucaoDemandPlan) registro[0];
            var resultado = CalendarProfileMigrationPlan.planejarDemand(
                    (TamanhoBucket) registro[1], (Integer) registro[2]);
            if (!resultado.podeMigrar()) {
                LOGGER.warn("Calendar profile migration: Demand profile {} pending: {}",
                        perfilExecucao.getId(), resultado.pendencia());
                continue;
            }
            String id = "MIG_SIMPLES_" + resultado.bucketSize().name() + "_" + resultado.numberOfBasePeriods();
            PerfilCalendario perfil = catalogo.get(id);
            if (perfil == null) {
                perfil = new PerfilCalendarioSimples(id, resultado.bucketSize(), resultado.numberOfBasePeriods());
                perfil.setDescricao("Receita uniforme migrada de parâmetros explícitos");
                catalogo.put(id, perfil);
                novasReceitas.add(perfil);
            }
            if (!(perfil instanceof PerfilCalendarioSimples simples)
                    || simples.getTamanhoBucketBase() != resultado.bucketSize()
                    || simples.getNumeroPeriodosBucketBase() != resultado.numberOfBasePeriods()) {
                throw new IllegalStateException("Calendar migration identifier collision: " + id);
            }
            vinculos.put(perfilExecucao, id);
        }
        // Persistir novas raízes antes de associá-las aos perfis gerenciados
        // evita referências transitórias durante o flush. Nunca save em loop.
        perfilCalendarioRepository.saveAll(novasReceitas)
                .forEach(perfil -> catalogo.put(perfil.getId(), perfil));
        vinculos.forEach((perfilExecucao, id) ->
                perfilExecucao.setPerfilCalendario((PerfilCalendarioSimples) catalogo.get(id)));
        entityManager.flush();
        LOGGER.info("Calendar profile migration: {} Demand profiles linked; {} recipes created.",
                vinculos.size(), novasReceitas.size());

    }

    private void migrarCopiasSupplyHistoricas() {

        // A cópia usa a PK do plano via @MapsId e este é o lado inverso do
        // @OneToOne. Não usar "p.perfilCalendarioSupplyPlan is null": o Hibernate
        // pode simplificar essa navegação para a PK (sempre preenchida) do pai,
        // excluindo justamente os planos históricos cuja linha filha não existe.
        // NOT EXISTS verifica a tabela de cópias por sua chave real. O fetch
        // esquerdo resolve também a associação ausente no mesmo round-trip,
        // sem uma consulta adicional por Supply Plan durante a materialização.
        List<Object[]> pendentes = entityManager.createQuery(
                "select p, p.tamanhoBucket, p.dataInicioPlano, p.dataFimPlano from SupplyPlan p "
                        + "left join fetch p.perfilCalendarioSupplyPlan "
                        + "where not exists (select 1 from PerfilCalendarioSupplyPlan copia "
                        + "where copia.supplyPlanId = p.id)", Object[].class).getResultList();
        List<PerfilCalendarioSupplyPlan> copias = new ArrayList<>();
        for (Object[] registro : pendentes) {
            SupplyPlan supplyPlan = (SupplyPlan) registro[0];
            var resultado = CalendarProfileMigrationPlan.planejarSupplyPlan(
                    (TamanhoBucket) registro[1], (LocalDateTime) registro[2], (LocalDateTime) registro[3]);
            if (!resultado.podeMigrar()) {
                LOGGER.warn("Calendar profile migration: Supply Plan {} pending: {}",
                        supplyPlan.getId(), resultado.pendencia());
                continue;
            }
            PerfilCalendarioSimplesSupplyPlan copia = new PerfilCalendarioSimplesSupplyPlan(
                    supplyPlan, resultado.bucketSize(), resultado.numberOfBasePeriods());
            // A origem é desconhecida: não inventar FK/ID de um cadastro que não
            // existia. A receita independente basta para visualização e rerun.
            copia.setDescricao("Receita uniforme reconstruída das datas históricas do plano");
            copia.validarEstrutura();
            copias.add(copia);
        }
        perfilCalendarioSupplyPlanRepository.saveAll(copias).forEach(copia ->
                copia.getSupplyPlan().setPerfilCalendarioSupplyPlan(copia));
        entityManager.flush();
        LOGGER.info("Calendar profile migration: {} historical Supply Plan recipes copied.", copias.size());

    }

}
