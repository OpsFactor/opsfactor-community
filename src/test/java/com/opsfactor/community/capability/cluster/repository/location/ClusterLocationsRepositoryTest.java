package com.opsfactor.community.capability.cluster.repository.location;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.domain.location.RegraAlocacaoClusterLocations;
import com.opsfactor.community.capability.cluster.domain.location.RegraAlocacaoClusterLocationsPaisEstado;
import com.opsfactor.community.capability.cluster.domain.location.RegraAlocacaoClusterLocationsTipoLocation;
import com.opsfactor.community.capability.configuration.domain.cluster.location.ParametrosClusterLocations;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.cluster.facade.mapper.ClusterLocationsMapper;
import com.opsfactor.community.bootstrap.CommunityModelApplication;
import com.opsfactor.community.platform.utility.Constantes;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste JPA basico do relacionamento entre cluster de locations e seus
 * parametros Community.
 */
//@RunWith(SpringRunner.class) // deprecated, junit4
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DataJpaTest
@ContextConfiguration(classes = CommunityModelApplication.class)
public class ClusterLocationsRepositoryTest {

    /** Repository JPA sob teste. */
    @Autowired
    ClusterLocationsRepository clusterLocationsRepository;

    /** Entity manager usado para reproduzir a listagem com entidades desacopladas do cache de primeiro nivel. */
    @Autowired
    EntityManager entityManager;


    @BeforeAll
    public void setUp() throws Exception {

    }

    @Test
    public void testFindByClusterProdutosAndClusterLocations() {

        ClusterLocations clusterLocationsM = new ClusterLocations();
        ClusterLocations clusterLocationsN = new ClusterLocations();

        /*
         * ClusterLocations usa @GeneratedValue. O teste deixa o banco gerar o
         * id para validar o ciclo real de persistencia; setar id manualmente
         * faria o Spring Data chamar merge e trataria a entidade como detached.
         */
        clusterLocationsM.setDescricao("M");
        clusterLocationsN.setDescricao("N");

        clusterLocationsRepository.save(clusterLocationsM);
        clusterLocationsRepository.save(clusterLocationsN);

        ParametrosClusterLocations parametrosClusterLocationsM = new ParametrosClusterLocations(
                new ParametrosClusterLocations.ParametrosClusterLocationsCompositeKey(clusterLocationsM));
        ParametrosClusterLocations parametrosClusterLocationsN = new ParametrosClusterLocations(
                new ParametrosClusterLocations.ParametrosClusterLocationsCompositeKey(clusterLocationsN));

//        parametrosClusterLocationsM.setTamanhoBucketDp(Constantes.TamanhoBucket.SEMANAL);
//        parametrosClusterLocationsN.setTamanhoBucketDp(Constantes.TamanhoBucket.MENSAL);

        clusterLocationsM.setParametrosClusterLocations(parametrosClusterLocationsM);
        clusterLocationsN.setParametrosClusterLocations(parametrosClusterLocationsN);

        clusterLocationsRepository.save(clusterLocationsM);
        clusterLocationsRepository.save(clusterLocationsN);
        clusterLocationsRepository.flush();
        
        long numeroClustersLocations = 0;
        for (ClusterLocations clusterLocations : clusterLocationsRepository.findAll()) {
            assertNotNull(clusterLocations.getDescricao());
            assertNotNull(clusterLocations.getParametrosClusterLocations());
//            if (clusterLocations.getDescricao().equals("M")) {
//                assertEquals(clusterLocations.getParametrosClusterLocations().getTamanhoBucketDp(null), Constantes.TamanhoBucket.SEMANAL);
//            } else if (clusterLocations.getDescricao().equals("N")) {
//                assertEquals(clusterLocations.getParametrosClusterLocations().getTamanhoBucketDp(null), Constantes.TamanhoBucket.MENSAL);
//            }
            numeroClustersLocations++;
        }

        assertEquals(numeroClustersLocations, 2);

    }

    /**
     * A listagem administrativa mapeia as regras de alocacao e seus valores
     * filhos. A consulta deve trazer toda essa arvore de uma vez para nao
     * executar uma consulta adicional por regra durante o mapper.
     */
    @Test
    public void customFindAllShouldFetchCountryStateAndLocationTypeRulesBeforeMapping() {

        ClusterLocations clusterLocations = new ClusterLocations("Regional", false, 1);
        clusterLocations = clusterLocationsRepository.saveAndFlush(clusterLocations);

        RegraAlocacaoClusterLocations regraPaisEstado = new RegraAlocacaoClusterLocations();
        regraPaisEstado.setClusterLocations(clusterLocations);
        regraPaisEstado.setRegraAlocacaoTipo(Constantes.RegraAlocacaoClusterLocationsTipo.PAIS_ESTADO);
        regraPaisEstado.addRegraAlocacaoPaisEstado(new RegraAlocacaoClusterLocationsPaisEstado(
                new RegraAlocacaoClusterLocationsPaisEstado.RegraAlocacaoClusterLocationsPaisEstadoCompositeKey(
                        regraPaisEstado,
                        "BR",
                        "SP")));

        clusterLocations.getRegrasAlocacaoClusterLocations().add(regraPaisEstado);
        clusterLocationsRepository.saveAndFlush(clusterLocations);

        RegraAlocacaoClusterLocations regraTipoLocation = new RegraAlocacaoClusterLocations();
        regraTipoLocation.setClusterLocations(clusterLocations);
        regraTipoLocation.setRegraAlocacaoTipo(Constantes.RegraAlocacaoClusterLocationsTipo.TIPO_LOCATION);
        regraTipoLocation.addRegraAlocacaoTipoLocation(new RegraAlocacaoClusterLocationsTipoLocation(
                new RegraAlocacaoClusterLocationsTipoLocation.RegraAlocacaoClusterLocationsTipoLocationCompositeKey(
                        regraTipoLocation,
                        Location.TipoLocation.INTERNA)));

        clusterLocations.getRegrasAlocacaoClusterLocations().add(regraTipoLocation);
        clusterLocationsRepository.saveAndFlush(clusterLocations);
        entityManager.clear();

        ClusterLocations clusterCarregado = clusterLocationsRepository.customFindAll().stream()
                .filter(cluster -> cluster.getDescricao().equals("Regional"))
                .findFirst()
                .orElseThrow();

        assertTrue(Hibernate.isInitialized(clusterCarregado.getRegrasAlocacaoClusterLocations()));
        assertEquals(2, clusterCarregado.getRegrasAlocacaoClusterLocations().size());

        RegraAlocacaoClusterLocations regraCarregadaPaisEstado = clusterCarregado
                .getRegrasAlocacaoClusterLocations()
                .stream()
                .filter(regra -> regra.getRegraAlocacaoTipo()
                        == Constantes.RegraAlocacaoClusterLocationsTipo.PAIS_ESTADO)
                .findFirst()
                .orElseThrow();
        RegraAlocacaoClusterLocations regraCarregadaTipoLocation = clusterCarregado
                .getRegrasAlocacaoClusterLocations()
                .stream()
                .filter(regra -> regra.getRegraAlocacaoTipo()
                        == Constantes.RegraAlocacaoClusterLocationsTipo.TIPO_LOCATION)
                .findFirst()
                .orElseThrow();

        assertTrue(Hibernate.isInitialized(regraCarregadaPaisEstado
                .getRegrasAlocacaoClusterLocationsPaisEstadoSet()));
        assertTrue(Hibernate.isInitialized(regraCarregadaTipoLocation
                .getRegrasAlocacaoClusterLocationsTipoLocationSet()));
        assertFalse(regraCarregadaPaisEstado.getRegrasAlocacaoClusterLocationsPaisEstadoSet().isEmpty());
        assertFalse(regraCarregadaTipoLocation.getRegrasAlocacaoClusterLocationsTipoLocationSet().isEmpty());
        assertEquals(2, ClusterLocationsMapper.convertComRegrasAlocacaoDTO(clusterCarregado)
                .getRegraAlocacaoClusterDTOList()
                .size());

    }




}
