package com.opsfactor.community.capability.cluster.service;

import com.opsfactor.community.capability.cluster.domain.produto.ClusterMateriais;
import com.opsfactor.community.capability.cluster.repository.material.ClusterMateriaisRepository;
import com.opsfactor.community.capability.configuration.domain.cluster.PerfilClusterizacao;
import com.opsfactor.community.platform.database.CommunityJpaConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Bootstrap usa entidades reais, com transacao e perfil compartilhado. */
@DataJpaTest
@ContextConfiguration(classes = CommunityJpaConfiguration.class)
class ClusteringServiceInitializationJpaTest {

    @Autowired private EntityManager entityManager;
    @Autowired private ClusterMateriaisRepository clusters;
    @Autowired private PlatformTransactionManager transactionManager;
    private ClusteringService service;

    @BeforeEach
    void prepare() {

        entityManager.persist(new PerfilClusterizacao(PerfilClusterizacao.ID_LEGADO, "Default"));
        entityManager.flush();
        service = new ClusteringService();
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        ReflectionTestUtils.setField(service, "transactionManager", transactionManager);
        ReflectionTestUtils.setField(service, "clusterMateriaisDemandPlanningRepository", clusters);

    }

    @Test
    void createsDefaultOnEmptyInstallationOnlyOnce() {

        service.inicializaClusterMaterialPadrao();
        service.inicializaClusterMaterialPadrao();
        assertEquals(1, clusters.findAllByPerfilClusterizacaoIdAndPadraoTrue(PerfilClusterizacao.ID_LEGADO).size());

    }

    @Test
    void repairsMissingDefaultWithoutOverwritingSpecificCluster() {

        clusters.saveAndFlush(new ClusterMateriais("Specific", false, 10));
        service.inicializaClusterMaterialPadrao();
        service.inicializaClusterMaterialPadrao();
        var defaults = clusters.findAllByPerfilClusterizacaoIdAndPadraoTrue(PerfilClusterizacao.ID_LEGADO);
        assertEquals(1, defaults.size());
        assertEquals("Default Material DP Cluster", defaults.getFirst().getDescricao());
        assertEquals(2, clusters.findProfileRoots(PerfilClusterizacao.ID_LEGADO).size());

    }

    @Test
    void preservesExistingDefaultAndRejectsMultipleDefaults() {

        clusters.saveAndFlush(new ClusterMateriais("Existing default", true, 10));
        service.inicializaClusterMaterialPadrao();
        assertEquals("Existing default", clusters.findAllByPerfilClusterizacaoIdAndPadraoTrue(
                PerfilClusterizacao.ID_LEGADO).getFirst().getDescricao());
        clusters.saveAndFlush(new ClusterMateriais("Other default", true, 20));
        assertThrows(IllegalStateException.class, service::inicializaClusterMaterialPadrao);

    }
}
