package com.opsfactor.community.capability.demandplanning.facade;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.domain.location.RegraAlocacaoClusterLocations;
import com.opsfactor.community.capability.cluster.domain.location.RegraAlocacaoClusterLocationsPaisEstado;
import com.opsfactor.community.capability.cluster.domain.location.RegraAlocacaoClusterLocationsTipoLocation;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterMateriais;
import com.opsfactor.community.capability.cluster.domain.produto.RegraAlocacaoClusterProdutos;
import com.opsfactor.community.capability.cluster.domain.produto.RegraAlocacaoClusterProdutosCaracteristica;
import com.opsfactor.community.capability.cluster.facade.mapper.ClusterLocationsMapper;
import com.opsfactor.community.capability.cluster.facade.mapper.ClusterProdutosMapper;
import com.opsfactor.community.capability.cluster.repository.location.ClusterLocationsRepository;
import com.opsfactor.community.capability.cluster.repository.material.ClusterMateriaisRepository;
import com.opsfactor.community.capability.configuration.domain.cluster.PerfilClusterizacao;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.Caracteristica;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProduto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.platform.database.CommunityJpaConfiguration;
import com.opsfactor.community.platform.utility.Constantes;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reproduz o mapper depois de destacar as entidades carregadas para a previa. */
@DataJpaTest
@ContextConfiguration(classes = CommunityJpaConfiguration.class)
class DemandSimulationClusterFetchJpaTest {

    @Autowired private EntityManager entityManager;
    @Autowired private ClusterMateriaisRepository clusterMateriaisRepository;
    @Autowired private ClusterLocationsRepository clusterLocationsRepository;

    @Test
    void selectedClusterRulesAreMappedAfterDetachmentForEachProfile() {

        PerfilClusterizacao perfil = new PerfilClusterizacao("PREVIEW_TEST", "Preview test");
        entityManager.persist(perfil);
        CaracteristicaProduto caracteristica = new CaracteristicaProduto();
        caracteristica.setId("PREVIEW_FAMILY");
        caracteristica.setDescricao("Preview family");
        caracteristica.setTipoCaracteristica(Caracteristica.TipoCaracteristica.CATEGORICO);
        entityManager.persist(caracteristica);

        ClusterMateriais materiais = new ClusterMateriais("Materiais", false, 10);
        materiais.setPerfilClusterizacao(perfil);
        RegraAlocacaoClusterProdutos status = new RegraAlocacaoClusterProdutos();
        status.setClusterProdutos(materiais);
        status.setRegraAlocacaoTipo(Constantes.RegraAlocacaoClusterProdutosTipo.STATUS_PRODUTO);
        status.addStatusProduto(Constantes.StatusProduto.REGULAR);
        materiais.getRegrasAlocacaoClusterProdutos().add(status);
        materiais = clusterMateriaisRepository.saveAndFlush(materiais);
        RegraAlocacaoClusterProdutos regraCaracteristica = new RegraAlocacaoClusterProdutos();
        regraCaracteristica.setClusterProdutos(materiais);
        regraCaracteristica.setRegraAlocacaoTipo(Constantes.RegraAlocacaoClusterProdutosTipo.CARACTERISTICA);
        regraCaracteristica.addRegraAlocacaoCaracteristica(new RegraAlocacaoClusterProdutosCaracteristica(
                new RegraAlocacaoClusterProdutosCaracteristica.RegraAlocacaoClusterProdutosCaracteristicaCompositeKey(
                        regraCaracteristica, caracteristica, "Papel")));
        materiais.getRegrasAlocacaoClusterProdutos().add(regraCaracteristica);
        materiais = clusterMateriaisRepository.saveAndFlush(materiais);

        ClusterLocations locations = new ClusterLocations("Locations", false, 20);
        locations.setPerfilClusterizacao(perfil);
        RegraAlocacaoClusterLocations estado = new RegraAlocacaoClusterLocations();
        estado.setClusterLocations(locations);
        estado.setRegraAlocacaoTipo(Constantes.RegraAlocacaoClusterLocationsTipo.PAIS_ESTADO);
        estado.addRegraAlocacaoPaisEstado(new RegraAlocacaoClusterLocationsPaisEstado(
                new RegraAlocacaoClusterLocationsPaisEstado.RegraAlocacaoClusterLocationsPaisEstadoCompositeKey(
                        estado, "BR", "SP")));
        locations.getRegrasAlocacaoClusterLocations().add(estado);
        locations = clusterLocationsRepository.saveAndFlush(locations);
        RegraAlocacaoClusterLocations tipo = new RegraAlocacaoClusterLocations();
        tipo.setClusterLocations(locations);
        tipo.setRegraAlocacaoTipo(Constantes.RegraAlocacaoClusterLocationsTipo.TIPO_LOCATION);
        tipo.addRegraAlocacaoTipoLocation(new RegraAlocacaoClusterLocationsTipoLocation(
                new RegraAlocacaoClusterLocationsTipoLocation.RegraAlocacaoClusterLocationsTipoLocationCompositeKey(
                        tipo, Location.TipoLocation.INTERNA)));
        locations.getRegrasAlocacaoClusterLocations().add(tipo);
        locations = clusterLocationsRepository.saveAndFlush(locations);

        Long materialId = materiais.getId();
        Long locationId = locations.getId();
        entityManager.clear();
        ClusterMateriais materialCarregado = clusterMateriaisRepository.findForSimulation(materialId, perfil.getId()).orElseThrow();
        ClusterLocations locationCarregado = clusterLocationsRepository.findForSimulation(locationId, perfil.getId()).orElseThrow();
        entityManager.clear();

        assertEquals(2, ClusterProdutosMapper.convertComRegrasAlocacaoDTO(materialCarregado).getRegraAlocacaoClusterDTOList().size());
        assertEquals(2, ClusterLocationsMapper.convertComRegrasAlocacaoDTO(locationCarregado).getRegraAlocacaoClusterDTOList().size());
        assertTrue(clusterMateriaisRepository.findForSimulation(materialId, "OTHER_PROFILE").isEmpty());
        assertTrue(clusterLocationsRepository.findForSimulation(locationId, "OTHER_PROFILE").isEmpty());

    }
}
