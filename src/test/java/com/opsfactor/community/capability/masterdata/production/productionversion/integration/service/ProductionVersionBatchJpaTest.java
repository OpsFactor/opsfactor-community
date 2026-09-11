package com.opsfactor.community.capability.masterdata.production.productionversion.integration.service;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.service.LocationService;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnicaMultiplo;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnicaMultiploOutput;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.repository.ListaTecnicaRepository;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.repository.ListaTecnicaMultiploRepository;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.production.routing.domain.RoteiroMultiplo;
import com.opsfactor.community.capability.masterdata.production.routing.domain.RoteiroMultiploMaterial;
import com.opsfactor.community.capability.masterdata.production.routing.repository.RoteiroRepository;
import com.opsfactor.community.capability.masterdata.production.routing.repository.RoteiroMultiploRepository;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.production.productionversion.repository.VersaoProducaoRepository;
import com.opsfactor.community.capability.masterdata.production.productionversion.integration.dto.VersaoProducaoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.production.productionversion.integration.mapper.VersaoProducaoIntegrationMapper;
import com.opsfactor.community.capability.masterdata.production.productionversion.integration.mapper.VersaoProducaoIntegrationSupportData;
import com.opsfactor.community.platform.database.CommunityJpaConfiguration;
import com.opsfactor.community.platform.exception.DataUploadException;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real JPA regression for the 34-row tutorial import and shared-session proxy safety. */
@DataJpaTest(showSql = false, properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true"})
@ContextConfiguration(classes = CommunityJpaConfiguration.class)
class ProductionVersionBatchJpaTest {

    @Autowired EntityManager entityManager;
    @Autowired RoteiroRepository routingRepository;
    @Autowired ListaTecnicaRepository bomRepository;
    @Autowired RoteiroMultiploRepository multipleRoutingRepository;
    @Autowired ListaTecnicaMultiploRepository multipleBomRepository;
    @Autowired VersaoProducaoRepository versionRepository;

    @Test
    void importsThirtyFourVersionsInOneBatchWithoutConversionQueriesOrWorkerThreads() {

        createSimpleMasters(34);
        // Reproduce the original input: findAll leaves output material proxies lazy.
        assertFalse(Hibernate.isInitialized(routingRepository.findAll().getFirst().getMaterialOutput()));
        entityManager.clear();
        VersaoProducaoIntegrationService service = createService();
        Statistics statistics = statistics();
        statistics.clear();
        VersaoProducaoIntegrationSupportData support = service.getSupportData();
        assertEquals(5, statistics.getPrepareStatementCount(), "fixed snapshots: versions, roots x2, subtypes x2");
        List<VersaoProducaoIntegrationDataDto> rows = rows(34);
        Map<VersaoProducaoIntegrationDataDto.VersaoProducaoPrimaryKeyIntegrationDTO, VersaoProducao> existing = new HashMap<>();
        Thread owner = Thread.currentThread();
        long[] queriesBeforeSave = {-1};
        doAnswer(invocation -> {
            assertSame(owner, Thread.currentThread());
            queriesBeforeSave[0] = statistics.getPrepareStatementCount();
            return invocation.callRealMethod();
        }).when(service).saveEntityList(anyCollection());
        statistics.clear();
        service.persistDTOBatchList(rows, existing, support, 0, null, null);
        assertEquals(0, queriesBeforeSave[0], "no lazy query during conversion/validation");
        verify(service, times(1)).saveEntityList(argThat(batch -> batch.size() == 34));
        assertEquals(34, versionRepository.count());
        assertFalse(service.getDtoConversionStream(rows).isParallel());

    }

    @Test
    void unexpectedLazySnapshotsRemainOnOwningThreadAndInvalidBatchIsNotSaved() {

        createSimpleMasters(34);
        VersaoProducaoIntegrationService service = createService();
        VersaoProducaoIntegrationSupportData support = new VersaoProducaoIntegrationSupportData();
        support.mapaLocationPorId = Map.of("L", new Location("L", "Location"));
        support.mapaRoteiroPorId = new HashMap<>();
        routingRepository.findAll().forEach(routing -> support.mapaRoteiroPorId.put(routing.getId(), routing));
        support.mapaListaTecnicaPorId = new HashMap<>();
        bomRepository.findAll().forEach(bom -> support.mapaListaTecnicaPorId.put(bom.getId(), bom));
        List<VersaoProducaoIntegrationDataDto> rows = rows(34);
        rows.getLast().outputMaterialId = "WRONG";
        assertThrows(DataUploadException.class,
                () -> service.persistDTOBatchList(rows, new HashMap<>(), support, 0, null, null));
        verify(service, never()).saveEntityList(anyCollection());
        assertEquals(0, versionRepository.count(), "failure occurred before any batch write, not global transaction assurance");

    }

    @Test
    void multipleOutputSnapshotsGivenToMapperAreCompleteAfterDetach() {

        createSimpleMasters(2);
        Location location = entityManager.find(Location.class, "L");
        RoteiroMultiplo routing = new RoteiroMultiplo();
        routing.setId("RM"); routing.setLocation(location);
        ListaTecnicaMultiplo bom = new ListaTecnicaMultiplo();
        bom.setId("BM"); bom.setLocation(location);
        for (int index = 0; index < 2; index++) {
            Produto material = entityManager.find(Produto.class, "M" + index);
            routing.getRoteiroMultiploMaterialSet().add(new RoteiroMultiploMaterial(routing, material));
            ListaTecnicaMultiploOutput output = new ListaTecnicaMultiploOutput(bom, material);
            output.setQuantidadeBase(1d);
            bom.getListaTecnicaMultiploOutputSet().add(output);
        }
        entityManager.persist(routing); entityManager.persist(bom);
        entityManager.flush(); entityManager.clear();
        VersaoProducaoIntegrationService service = createService();
        statistics().clear();
        VersaoProducaoIntegrationSupportData support = service.getSupportData();
        assertEquals(5, statistics().getPrepareStatementCount());
        assertTrue(Hibernate.isInitialized(((RoteiroMultiplo) support.mapaRoteiroPorId.get("RM")).getRoteiroMultiploMaterialSet()));
        assertTrue(Hibernate.isInitialized(((ListaTecnicaMultiplo) support.mapaListaTecnicaPorId.get("BM")).getListaTecnicaMultiploOutputSet()));
        entityManager.clear();
        VersaoProducao version = new VersaoProducao("VM", support.mapaLocationPorId.get("L"), 1,
                support.mapaRoteiroPorId.get("RM"), support.mapaListaTecnicaPorId.get("BM"));
        assertEquals(2, version.getMateriaisOutput().size());
        assertEquals(5, statistics().getPrepareStatementCount(), "detached validation must perform no SQL");
        // The existing seven-column DTO remains singular; do not silently invent multiple-output file semantics.
        assertThrows(IllegalStateException.class, version::getMaterialOutput);

    }

    private VersaoProducaoIntegrationService createService() {

        VersaoProducaoIntegrationService service = spy(new VersaoProducaoIntegrationService());
        LocationService locationService = mock(LocationService.class);
        when(locationService.findAllWithoutDefault()).thenReturn(List.of(new Location("L", "Location")));
        VersaoProducaoIntegrationMapper mapper = spy(new VersaoProducaoIntegrationMapper());
        Thread owner = Thread.currentThread();
        doAnswer(invocation -> {
            assertSame(owner, Thread.currentThread(), "JPA-aware mapper must not enter ForkJoinPool");
            return invocation.callRealMethod();
        }).when(mapper).updateEntityNonPrimaryFieldsFromDTO(any(), any(), any(), any());
        ReflectionTestUtils.setField(service, "locationService", locationService);
        ReflectionTestUtils.setField(service, "roteiroRepository", routingRepository);
        ReflectionTestUtils.setField(service, "listaTecnicaRepository", bomRepository);
        ReflectionTestUtils.setField(service, "roteiroMultiploRepository", multipleRoutingRepository);
        ReflectionTestUtils.setField(service, "listaTecnicaMultiploRepository", multipleBomRepository);
        ReflectionTestUtils.setField(service, "versaoProducaoRepository", versionRepository);
        ReflectionTestUtils.setField(service, "versaoProducaoIntegrationMapper", mapper);
        return service;

    }

    private void createSimpleMasters(int count) {

        Location location = new Location("L", "Location");
        entityManager.persist(location);
        for (int index = 0; index < count; index++) {
            Produto material = new Produto("M" + index);
            entityManager.persist(material);
            Roteiro routing = new Roteiro();
            routing.setId("R" + index); routing.setLocation(location); routing.setMaterialOutput(material);
            ListaTecnica bom = new ListaTecnica();
            bom.setId("B" + index); bom.setLocation(location); bom.setMaterialOutput(material);
            entityManager.persist(routing); entityManager.persist(bom);
        }
        entityManager.flush(); entityManager.clear();

    }

    private List<VersaoProducaoIntegrationDataDto> rows(int count) {

        List<VersaoProducaoIntegrationDataDto> rows = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            VersaoProducaoIntegrationDataDto row = new VersaoProducaoIntegrationDataDto();
            row.primaryKeyDto = new VersaoProducaoIntegrationDataDto.VersaoProducaoPrimaryKeyIntegrationDTO("V" + index);
            row.locationId = "L"; row.routingId = "R" + index;
            row.billOfMaterialsId = "B" + index; row.outputMaterialId = "M" + index;
            row.priority = 1; row.active = true;
            rows.add(row);
        }
        return rows;

    }

    private Statistics statistics() {

        return entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();

    }
}
