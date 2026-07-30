package com.opsfactor.community.capability.cluster.facade;

import com.opsfactor.community.capability.cluster.facade.dto.ClusterLocationsDTO;
import com.opsfactor.community.capability.cluster.facade.dto.ClusterProdutosDTO;
import com.opsfactor.community.capability.cluster.facade.dto.RegraAlocaoClusterLocationsCaracteristicaDTO;
import com.opsfactor.community.capability.cluster.facade.dto.RegraAlocaoClusterLocationsTipoLocationDTO;
import com.opsfactor.community.capability.cluster.facade.dto.RegraAlocaoClusterProdutosDTO;
import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto.CaracteristicaProdutoDTO;
import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.domain.location.RegraAlocacaoClusterLocations;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutosDemandPlanning;
import com.opsfactor.community.capability.cluster.domain.produto.RegraAlocacaoClusterProdutos;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.cluster.repository.location.ClusterLocationsRepository;
import com.opsfactor.community.capability.cluster.repository.material.ClusterProdutosDemandPlanningRepository;
import com.opsfactor.community.capability.configuration.repository.cluster.location.RegraAlocacaoClusterLocationsRepository;
import com.opsfactor.community.capability.configuration.repository.cluster.produto.RegraAlocacaoClusterProdutosRepository;
import com.opsfactor.community.capability.cluster.facade.dto.ClusterRuleDTO;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Contrato Community da configuracao de clusters.
 *
 * <p>Clusters continuam disponiveis no Community, mas regras por
 * caracteristica e regras de material por status NEW dependem de capacidades
 * Enterprise e devem falhar antes de qualquer escrita parcial.</p>
 */
public class ClusteringFacadeCommunityContractTest {

    @Test
    public void validaRegrasAlocacaoClusterProdutosCommunityShouldRejectCharacteristicRule() {

        ClusteringFacade clusteringFrontService = new ClusteringFacade();
        ClusterProdutosDTO clusterProdutosDTO = new ClusterProdutosDTO();

        RegraAlocaoClusterProdutosDTO regraAlocaoClusterProdutosDTO = new RegraAlocaoClusterProdutosDTO();
        regraAlocaoClusterProdutosDTO.setCriterio(Constantes.RegraAlocacaoClusterProdutosTipo.CARACTERISTICA);
        clusterProdutosDTO.getRegraAlocacaoClusterDTOList().add(regraAlocaoClusterProdutosDTO);

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> clusteringFrontService.validaRegrasAlocacaoClusterProdutosCommunity(clusterProdutosDTO));

    }

    @Test
    public void saveClusterProdutosDTOShouldRejectNullPayloadBeforeRepositories() {

        ClusteringFacade clusteringFrontService = new ClusteringFacade();

        /*
         * Nenhum repository e injetado. Payload nulo deve falhar como contrato
         * da request, nao como erro de ponteiro ao acessar `process` ou como
         * falha de JPA.
         */
        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> clusteringFrontService.saveClusterProdutosDTO(null));
        Assertions.assertEquals(
                "Material cluster payload is required",
                illegalArgumentException.getMessage());

    }

    @Test
    public void saveClusterProdutosDTOShouldRejectMissingProcessBeforeRepositories() {

        ClusteringFacade clusteringFrontService = new ClusteringFacade();
        ClusterProdutosDTO clusterProdutosDTO = new ClusterProdutosDTO();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> clusteringFrontService.saveClusterProdutosDTO(clusterProdutosDTO));
        Assertions.assertEquals(
                "Community material cluster process is required.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void saveClusterProdutosDTOShouldRejectIncompleteStatusRuleBeforeRepositories() {

        ClusteringFacade clusteringFrontService = new ClusteringFacade();
        ClusterProdutosDTO clusterProdutosDTO = new ClusterProdutosDTO();
        clusterProdutosDTO.setProcess("DP");

        RegraAlocaoClusterProdutosDTO regraAlocaoClusterProdutosDTO =
                new RegraAlocaoClusterProdutosDTO();
        regraAlocaoClusterProdutosDTO.setCriterio(Constantes.RegraAlocacaoClusterProdutosTipo.STATUS_PRODUTO);
        clusterProdutosDTO.getRegraAlocacaoClusterDTOList().add(regraAlocaoClusterProdutosDTO);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> clusteringFrontService.saveClusterProdutosDTO(clusterProdutosDTO));
        Assertions.assertEquals(
                "Material cluster status allocation value is required",
                illegalArgumentException.getMessage());

    }

    @Test
    public void clusterLookupsShouldRejectNullRepositoryOptionalsBeforeMapper()
            throws Exception {

        ClusteringFacade clusteringFrontServiceComMaterialClusterQuebrado =
                new ClusteringFacade();
        setPrivateField(
                clusteringFrontServiceComMaterialClusterQuebrado,
                "clusterMateriaisDemandPlanningRepository",
                getClusterProdutosDemandPlanningRepositoryComFindByIdRetornando(null));

        /*
         * Cluster inexistente na leitura continua retorno null para a tela.
         * Optional nulo do repository deve falhar antes do mapper.
         */
        IllegalStateException materialClusterException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> clusteringFrontServiceComMaterialClusterQuebrado.getClusterProdutosDTO("10", "DP"));
        Assertions.assertEquals(
                "Material cluster repository returned null Optional for Community lookup id 10.",
                materialClusterException.getMessage());

        ClusteringFacade clusteringFrontServiceComLocationClusterQuebrado =
                new ClusteringFacade();
        setPrivateField(
                clusteringFrontServiceComLocationClusterQuebrado,
                "clusterLocationsRepository",
                getClusterLocationsRepositoryComFindByIdRetornando(null));

        IllegalStateException locationClusterException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> clusteringFrontServiceComLocationClusterQuebrado.getClusterLocationsDTO("11"));
        Assertions.assertEquals(
                "Location cluster repository returned null Optional for Community lookup id 11.",
                locationClusterException.getMessage());

    }

    @Test
    public void materialClusterListsShouldRejectBrokenSnapshotsBeforeMapper()
            throws Exception {

        ClusteringFacade serviceComListaNula =
                criaClusteringFrontServiceComListaClusterMateriais(null);

        IllegalStateException nullDemandPlanningListException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComListaNula::getListaClusterProdutosDemandPlanningDTO);
        Assertions.assertEquals(
                "Material cluster snapshot is required for Demand Planning material cluster listing.",
                nullDemandPlanningListException.getMessage());

        IllegalStateException nullNonDefaultListException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComListaNula::getListaTodosClusterProdutosDTOExcetoPadrao);
        Assertions.assertEquals(
                "Material cluster snapshot is required for non-default material cluster listing.",
                nullNonDefaultListException.getMessage());

        ClusteringFacade serviceComItemNulo =
                criaClusteringFrontServiceComListaClusterMateriais(
                        java.util.Collections.singletonList(null));

        IllegalStateException nullItemException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComItemNulo::getListaClusterProdutosDemandPlanningDTO);
        Assertions.assertEquals(
                "Material cluster at index 0 is required for Demand Planning material cluster listing.",
                nullItemException.getMessage());

        ClusteringFacade serviceComClusterSemId =
                criaClusteringFrontServiceComListaClusterMateriais(
                        List.of(new ClusterProdutosDemandPlanning()));

        IllegalStateException missingIdException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComClusterSemId::getListaClusterProdutosDemandPlanningDTO);
        Assertions.assertEquals(
                "Material cluster at index 0 has no id for Demand Planning material cluster listing.",
                missingIdException.getMessage());

        ClusterProdutosDemandPlanning clusterMateriaisSemPadrao =
                criaClusterMateriaisDemandPlanningComId(1L);
        clusterMateriaisSemPadrao.setPadrao(null);

        ClusteringFacade serviceComClusterSemPadrao =
                criaClusteringFrontServiceComListaClusterMateriais(
                        List.of(clusterMateriaisSemPadrao));

        IllegalStateException missingDefaultFlagException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComClusterSemPadrao::getListaClusterProdutosDemandPlanningDTO);
        Assertions.assertEquals(
                "Material cluster at index 0 has no default flag for Demand Planning material cluster listing.",
                missingDefaultFlagException.getMessage());

        ClusterProdutosDemandPlanning clusterMateriaisSemRegras =
                criaClusterMateriaisDemandPlanningComId(1L);
        clusterMateriaisSemRegras.setRegrasAlocacaoClusterProdutos(null);

        ClusteringFacade serviceComClusterSemRegras =
                criaClusteringFrontServiceComListaClusterMateriais(
                        List.of(clusterMateriaisSemRegras));

        IllegalStateException missingRulesException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComClusterSemRegras::getListaClusterProdutosDemandPlanningDTO);
        Assertions.assertEquals(
                "Material cluster at index 0 has no allocation rules snapshot for Demand Planning material cluster listing.",
                missingRulesException.getMessage());

    }

    @Test
    public void materialClusterAdministrativeListQueriesShouldFetchRulesAndStatuses() throws Exception {

        Query allClustersQuery = ClusterProdutosDemandPlanningRepository.class
                .getMethod("customFindAllComRegrasAlocacaoEStatusProduto")
                .getAnnotation(Query.class);
        Query nonDefaultClustersQuery = ClusterProdutosDemandPlanningRepository.class
                .getMethod("customFindAllByPadraoIsFalseComRegrasAlocacaoEStatusProduto")
                .getAnnotation(Query.class);

        Assertions.assertNotNull(allClustersQuery);
        Assertions.assertTrue(allClustersQuery.value().contains("SELECT DISTINCT"));
        Assertions.assertTrue(allClustersQuery.value().contains("regrasAlocacaoClusterProdutos"));
        Assertions.assertTrue(allClustersQuery.value().contains("regraAlocacaoClusterProdutosStatusSet"));

        Assertions.assertNotNull(nonDefaultClustersQuery);
        Assertions.assertTrue(nonDefaultClustersQuery.value().contains("SELECT DISTINCT"));
        Assertions.assertTrue(nonDefaultClustersQuery.value().contains("regrasAlocacaoClusterProdutos"));
        Assertions.assertTrue(nonDefaultClustersQuery.value().contains("regraAlocacaoClusterProdutosStatusSet"));
        Assertions.assertTrue(nonDefaultClustersQuery.value().contains("WHERE cpd.padrao = false"));

    }

    @Test
    public void locationClusterListShouldRejectBrokenSnapshotsBeforeMapper()
            throws Exception {

        ClusteringFacade serviceComListaNula =
                criaClusteringFrontServiceComListaClusterLocations(null);

        IllegalStateException nullListException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComListaNula::getListaTodosClusterLocationsDTOExcetoPadrao);
        Assertions.assertEquals(
                "Location cluster snapshot is required for non-default location cluster listing.",
                nullListException.getMessage());

        List<ClusterLocations> clusterLocationsListComItemNulo = new ArrayList<>();
        clusterLocationsListComItemNulo.add(null);

        ClusteringFacade serviceComItemNulo =
                criaClusteringFrontServiceComListaClusterLocations(
                        clusterLocationsListComItemNulo);

        IllegalStateException nullItemException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComItemNulo::getListaTodosClusterLocationsDTOExcetoPadrao);
        Assertions.assertEquals(
                "Location cluster at index 0 is required for non-default location cluster listing.",
                nullItemException.getMessage());

        ClusteringFacade serviceComClusterSemId =
                criaClusteringFrontServiceComListaClusterLocations(
                        List.of(new ClusterLocations()));

        IllegalStateException missingIdException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComClusterSemId::getListaTodosClusterLocationsDTOExcetoPadrao);
        Assertions.assertEquals(
                "Location cluster at index 0 has no id for non-default location cluster listing.",
                missingIdException.getMessage());

        ClusterLocations clusterLocationsSemPadrao = criaClusterLocationsComId(1L);
        clusterLocationsSemPadrao.setPadrao(null);

        ClusteringFacade serviceComClusterSemPadrao =
                criaClusteringFrontServiceComListaClusterLocations(
                        List.of(clusterLocationsSemPadrao));

        IllegalStateException missingDefaultFlagException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComClusterSemPadrao::getListaTodosClusterLocationsDTOExcetoPadrao);
        Assertions.assertEquals(
                "Location cluster at index 0 has no default flag for non-default location cluster listing.",
                missingDefaultFlagException.getMessage());

        ClusterLocations clusterLocationsSemRegras = criaClusterLocationsComId(1L);
        clusterLocationsSemRegras.setRegrasAlocacaoClusterLocations(null);

        ClusteringFacade serviceComClusterSemRegras =
                criaClusteringFrontServiceComListaClusterLocations(
                        List.of(clusterLocationsSemRegras));

        IllegalStateException missingRulesException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComClusterSemRegras::getListaTodosClusterLocationsDTOExcetoPadrao);
        Assertions.assertEquals(
                "Location cluster at index 0 has no allocation rules snapshot for non-default location cluster listing.",
                missingRulesException.getMessage());

    }

    @Test
    public void clusterSavesShouldRejectNullRepositoryOptionalsBeforeCreationFallback()
            throws Exception {

        ClusterProdutosDTO clusterProdutosDTO = new ClusterProdutosDTO();
        clusterProdutosDTO.setProcess("DP");
        clusterProdutosDTO.setId(20L);

        ClusteringFacade clusteringFrontServiceComMaterialClusterQuebrado =
                new ClusteringFacade();
        setPrivateField(
                clusteringFrontServiceComMaterialClusterQuebrado,
                "clusterMateriaisDemandPlanningRepository",
                getClusterProdutosDemandPlanningRepositoryComFindByIdRetornando(null));

        /*
         * Optional.empty() representa criacao de novo cluster; Optional nulo nao
         * pode cair nesse fallback, porque indica repository quebrado.
         */
        IllegalStateException materialClusterException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> clusteringFrontServiceComMaterialClusterQuebrado.saveClusterProdutosDTO(clusterProdutosDTO));
        Assertions.assertEquals(
                "Material cluster repository returned null Optional for Community save id 20.",
                materialClusterException.getMessage());

        ClusterLocationsDTO clusterLocationsDTO = new ClusterLocationsDTO();
        clusterLocationsDTO.setId(21L);

        ClusteringFacade clusteringFrontServiceComLocationClusterQuebrado =
                new ClusteringFacade();
        setPrivateField(
                clusteringFrontServiceComLocationClusterQuebrado,
                "clusterLocationsRepository",
                getClusterLocationsRepositoryComFindByIdRetornando(null));

        IllegalStateException locationClusterException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> clusteringFrontServiceComLocationClusterQuebrado.saveClusterLocationsDTO(clusterLocationsDTO));
        Assertions.assertEquals(
                "Location cluster repository returned null Optional for Community save id 21.",
                locationClusterException.getMessage());

    }

    @Test
    public void saveClusterProdutosDTOShouldRejectBrokenSavedClusterSnapshot()
            throws Exception {

        ClusterProdutosDTO clusterProdutosDTO = new ClusterProdutosDTO();
        clusterProdutosDTO.setProcess("DP");

        ClusteringFacade clusteringFrontServiceComSnapshotNulo =
                criaClusteringFrontServiceComClusterMateriaisSalvo(null);

        IllegalStateException snapshotNuloException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> clusteringFrontServiceComSnapshotNulo.saveClusterProdutosDTO(clusterProdutosDTO));
        Assertions.assertEquals(
                "Saved material cluster snapshot is required.",
                snapshotNuloException.getMessage());

        ClusteringFacade clusteringFrontServiceComSnapshotSemId =
                criaClusteringFrontServiceComClusterMateriaisSalvo(
                        new ClusterProdutosDemandPlanning(null, false, null));

        IllegalStateException snapshotSemIdException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> clusteringFrontServiceComSnapshotSemId.saveClusterProdutosDTO(clusterProdutosDTO));
        Assertions.assertEquals(
                "Saved material cluster id is required.",
                snapshotSemIdException.getMessage());

    }

    @Test
    public void alocaRegrasAoClusterProdutosShouldRejectBrokenSavedRuleSnapshot()
            throws Exception {

        ClusterProdutosDemandPlanning clusterMateriaisDemandPlanning =
                new ClusterProdutosDemandPlanning(null, false, null);
        clusterMateriaisDemandPlanning.setId(20L);

        ClusterProdutosDTO clusterProdutosDTO = new ClusterProdutosDTO();
        clusterProdutosDTO.setPriority(10);
        RegraAlocaoClusterProdutosDTO regraAlocaoClusterProdutosDTO =
                new RegraAlocaoClusterProdutosDTO();
        regraAlocaoClusterProdutosDTO.setCriterio(Constantes.RegraAlocacaoClusterProdutosTipo.STATUS_PRODUTO);
        regraAlocaoClusterProdutosDTO.setCaracteristicaDTO(criaStatusProdutoDTO("REGULAR"));
        clusterProdutosDTO.getRegraAlocacaoClusterDTOList().add(regraAlocaoClusterProdutosDTO);

        ClusteringFacade clusteringFrontServiceComSnapshotNulo =
                criaClusteringFrontServiceComRegraMaterialSalva(null);

        IllegalStateException snapshotNuloException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> clusteringFrontServiceComSnapshotNulo.alocaRegrasAoClusterProdutos(
                        clusterProdutosDTO,
                        clusterMateriaisDemandPlanning));
        Assertions.assertEquals(
                "Saved material cluster allocation rule snapshot is required.",
                snapshotNuloException.getMessage());

        RegraAlocacaoClusterProdutos regraAlocacaoClusterProdutosSemId =
                new RegraAlocacaoClusterProdutos();
        regraAlocacaoClusterProdutosSemId.setClusterProdutos(clusterMateriaisDemandPlanning);
        ClusteringFacade clusteringFrontServiceComSnapshotSemId =
                criaClusteringFrontServiceComRegraMaterialSalva(regraAlocacaoClusterProdutosSemId);

        IllegalStateException snapshotSemIdException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> clusteringFrontServiceComSnapshotSemId.alocaRegrasAoClusterProdutos(
                        clusterProdutosDTO,
                        clusterMateriaisDemandPlanning));
        Assertions.assertEquals(
                "Saved material cluster allocation rule id is required.",
                snapshotSemIdException.getMessage());

    }

    @Test
    public void validaRegrasAlocacaoClusterProdutosCommunityShouldRejectNullRule() {

        ClusteringFacade clusteringFrontService = new ClusteringFacade();
        ClusterProdutosDTO clusterProdutosDTO = new ClusterProdutosDTO();
        clusterProdutosDTO.getRegraAlocacaoClusterDTOList().add(null);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> clusteringFrontService.validaRegrasAlocacaoClusterProdutosCommunity(
                        clusterProdutosDTO));
        Assertions.assertEquals(
                "Material cluster allocation rule cannot be null",
                illegalArgumentException.getMessage());

    }

    @Test
    public void validaRegrasAlocacaoClusterProdutosCommunityShouldAcceptStatusRule() {

        ClusteringFacade clusteringFrontService = new ClusteringFacade();
        ClusterProdutosDTO clusterProdutosDTO = new ClusterProdutosDTO();

        RegraAlocaoClusterProdutosDTO regraAlocaoClusterProdutosDTO = new RegraAlocaoClusterProdutosDTO();
        regraAlocaoClusterProdutosDTO.setCriterio(Constantes.RegraAlocacaoClusterProdutosTipo.STATUS_PRODUTO);
        regraAlocaoClusterProdutosDTO.setCaracteristicaDTO(criaStatusProdutoDTO("REGULAR"));
        clusterProdutosDTO.getRegraAlocacaoClusterDTOList().add(regraAlocaoClusterProdutosDTO);

        Assertions.assertDoesNotThrow(
                () -> clusteringFrontService.validaRegrasAlocacaoClusterProdutosCommunity(clusterProdutosDTO));

    }

    @Test
    public void validaRegrasAlocacaoClusterProdutosCommunityShouldRejectNewStatusRule() {

        ClusteringFacade clusteringFrontService = new ClusteringFacade();
        ClusterProdutosDTO clusterProdutosDTO = new ClusterProdutosDTO();

        RegraAlocaoClusterProdutosDTO regraAlocaoClusterProdutosDTO = new RegraAlocaoClusterProdutosDTO();
        regraAlocaoClusterProdutosDTO.setCriterio(Constantes.RegraAlocacaoClusterProdutosTipo.STATUS_PRODUTO);
        regraAlocaoClusterProdutosDTO.setCaracteristicaDTO(criaStatusProdutoDTO("NEW"));
        clusterProdutosDTO.getRegraAlocacaoClusterDTOList().add(regraAlocaoClusterProdutosDTO);

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> clusteringFrontService.validaRegrasAlocacaoClusterProdutosCommunity(clusterProdutosDTO));

    }

    @Test
    public void validaRegrasAlocacaoClusterLocationsCommunityShouldRejectCharacteristicRule() {

        ClusteringFacade clusteringFrontService = new ClusteringFacade();
        ClusterLocationsDTO clusterLocationsDTO = new ClusterLocationsDTO();

        RegraAlocaoClusterLocationsCaracteristicaDTO regraAlocaoClusterLocationsDTO =
                new RegraAlocaoClusterLocationsCaracteristicaDTO();
        regraAlocaoClusterLocationsDTO.setCriterio(Constantes.RegraAlocacaoClusterLocationsTipo.CARACTERISTICA);
        clusterLocationsDTO.getRegraAlocacaoClusterDTOList().add(regraAlocaoClusterLocationsDTO);

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> clusteringFrontService.validaRegrasAlocacaoClusterLocationsCommunity(clusterLocationsDTO));

    }

    @Test
    public void saveClusterLocationsDTOShouldRejectNullPayloadBeforeRepositories() {

        ClusteringFacade clusteringFrontService = new ClusteringFacade();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> clusteringFrontService.saveClusterLocationsDTO(null));
        Assertions.assertEquals(
                "Location cluster payload is required",
                illegalArgumentException.getMessage());

    }

    @Test
    public void saveClusterLocationsDTOShouldRejectIncompleteLocationTypeRuleBeforeRepositories() {

        ClusteringFacade clusteringFrontService = new ClusteringFacade();
        ClusterLocationsDTO clusterLocationsDTO = new ClusterLocationsDTO();

        RegraAlocaoClusterLocationsTipoLocationDTO regraAlocaoClusterLocationsDTO =
                new RegraAlocaoClusterLocationsTipoLocationDTO();
        regraAlocaoClusterLocationsDTO.setCriterio(Constantes.RegraAlocacaoClusterLocationsTipo.TIPO_LOCATION);
        clusterLocationsDTO.getRegraAlocacaoClusterDTOList().add(regraAlocaoClusterLocationsDTO);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> clusteringFrontService.saveClusterLocationsDTO(clusterLocationsDTO));
        Assertions.assertEquals(
                "Location cluster type allocation value is required",
                illegalArgumentException.getMessage());

    }

    @Test
    public void saveClusterLocationsDTOShouldRejectBrokenSavedClusterSnapshot()
            throws Exception {

        ClusterLocationsDTO clusterLocationsDTO = new ClusterLocationsDTO();

        ClusteringFacade clusteringFrontServiceComSnapshotNulo =
                criaClusteringFrontServiceComClusterLocationsSalvo(null);

        IllegalStateException snapshotNuloException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> clusteringFrontServiceComSnapshotNulo.saveClusterLocationsDTO(clusterLocationsDTO));
        Assertions.assertEquals(
                "Saved location cluster snapshot is required.",
                snapshotNuloException.getMessage());

        ClusteringFacade clusteringFrontServiceComSnapshotSemId =
                criaClusteringFrontServiceComClusterLocationsSalvo(new ClusterLocations());

        IllegalStateException snapshotSemIdException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> clusteringFrontServiceComSnapshotSemId.saveClusterLocationsDTO(clusterLocationsDTO));
        Assertions.assertEquals(
                "Saved location cluster id is required.",
                snapshotSemIdException.getMessage());

    }

    @Test
    public void alocaRegrasAoClusterLocationsShouldRejectBrokenSavedRuleSnapshot()
            throws Exception {

        ClusterLocations clusterLocations = new ClusterLocations();
        clusterLocations.setId(30L);

        ClusterLocationsDTO clusterLocationsDTO = new ClusterLocationsDTO();
        clusterLocationsDTO.setPriority(10);
        RegraAlocaoClusterLocationsTipoLocationDTO regraAlocaoClusterLocationsDTO =
                new RegraAlocaoClusterLocationsTipoLocationDTO();
        regraAlocaoClusterLocationsDTO.setCriterio(Constantes.RegraAlocacaoClusterLocationsTipo.TIPO_LOCATION);
        regraAlocaoClusterLocationsDTO.setLocationType(Location.TipoLocation.INTERNA);
        clusterLocationsDTO.getRegraAlocacaoClusterDTOList().add(regraAlocaoClusterLocationsDTO);

        ClusteringFacade clusteringFrontServiceComSnapshotNulo =
                criaClusteringFrontServiceComRegraLocationSalva(null);

        IllegalStateException snapshotNuloException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> clusteringFrontServiceComSnapshotNulo.alocaRegrasAoClusterLocations(
                        clusterLocationsDTO,
                        clusterLocations));
        Assertions.assertEquals(
                "Saved location cluster allocation rule snapshot is required.",
                snapshotNuloException.getMessage());

        RegraAlocacaoClusterLocations regraAlocacaoClusterLocationsSemId =
                new RegraAlocacaoClusterLocations();
        regraAlocacaoClusterLocationsSemId.setClusterLocations(clusterLocations);
        ClusteringFacade clusteringFrontServiceComSnapshotSemId =
                criaClusteringFrontServiceComRegraLocationSalva(regraAlocacaoClusterLocationsSemId);

        IllegalStateException snapshotSemIdException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> clusteringFrontServiceComSnapshotSemId.alocaRegrasAoClusterLocations(
                        clusterLocationsDTO,
                        clusterLocations));
        Assertions.assertEquals(
                "Saved location cluster allocation rule id is required.",
                snapshotSemIdException.getMessage());

    }

    @Test
    public void alocaRegrasAoClusterProdutosShouldMatchExistingRuleIdsByValue() {

        ClusteringFacade clusteringFrontService = new ClusteringFacade();
        ClusterProdutosDemandPlanning clusterMateriaisDemandPlanning =
                new ClusterProdutosDemandPlanning(null, false, null);
        RegraAlocacaoClusterProdutos regraAlocacaoClusterProdutosExistente =
                new RegraAlocacaoClusterProdutos();
        Long regraAlocacaoClusterProdutosIdPersistido = Long.valueOf(1000L);
        Long regraAlocacaoClusterProdutosIdPayload = Long.parseLong("1000");
        Assertions.assertNotSame(
                regraAlocacaoClusterProdutosIdPersistido,
                regraAlocacaoClusterProdutosIdPayload);

        regraAlocacaoClusterProdutosExistente.setId(regraAlocacaoClusterProdutosIdPersistido);
        regraAlocacaoClusterProdutosExistente.setClusterProdutos(clusterMateriaisDemandPlanning);
        clusterMateriaisDemandPlanning.getRegrasAlocacaoClusterProdutos()
                .add(regraAlocacaoClusterProdutosExistente);

        ClusterProdutosDTO clusterProdutosDTO = new ClusterProdutosDTO();
        clusterProdutosDTO.setPriority(10);
        RegraAlocaoClusterProdutosDTO regraAlocaoClusterProdutosDTO =
                new RegraAlocaoClusterProdutosDTO();
        regraAlocaoClusterProdutosDTO.setId(regraAlocacaoClusterProdutosIdPayload);
        regraAlocaoClusterProdutosDTO.setCriterio(Constantes.RegraAlocacaoClusterProdutosTipo.STATUS_PRODUTO);
        regraAlocaoClusterProdutosDTO.setCaracteristicaDTO(criaStatusProdutoDTO("REGULAR"));
        clusterProdutosDTO.getRegraAlocacaoClusterDTOList().add(regraAlocaoClusterProdutosDTO);

        /*
         * A regra ja existente deve ser reconhecida por igualdade de valor do
         * Long, nao por identidade de objeto. Se a comparacao voltar a usar
         * `==`, o service tentara criar e salvar uma nova regra, quebrando aqui
         * porque o repository nao foi injetado neste teste unitario.
         */
        Assertions.assertDoesNotThrow(
                () -> clusteringFrontService.alocaRegrasAoClusterProdutos(
                        clusterProdutosDTO,
                        clusterMateriaisDemandPlanning));
        Assertions.assertEquals(
                1,
                clusterMateriaisDemandPlanning.getRegrasAlocacaoClusterProdutos().size());

    }

    @Test
    public void alocaRegrasAoClusterLocationsShouldMatchExistingRuleIdsByValue() {

        ClusteringFacade clusteringFrontService = new ClusteringFacade();
        ClusterLocations clusterLocations = new ClusterLocations();
        RegraAlocacaoClusterLocations regraAlocacaoClusterLocationsExistente =
                new RegraAlocacaoClusterLocations();
        Long regraAlocacaoClusterLocationsIdPersistido = Long.valueOf(1000L);
        Long regraAlocacaoClusterLocationsIdPayload = Long.parseLong("1000");
        Assertions.assertNotSame(
                regraAlocacaoClusterLocationsIdPersistido,
                regraAlocacaoClusterLocationsIdPayload);

        regraAlocacaoClusterLocationsExistente.setId(regraAlocacaoClusterLocationsIdPersistido);
        regraAlocacaoClusterLocationsExistente.setClusterLocations(clusterLocations);
        clusterLocations.getRegrasAlocacaoClusterLocations()
                .add(regraAlocacaoClusterLocationsExistente);

        ClusterLocationsDTO clusterLocationsDTO = new ClusterLocationsDTO();
        clusterLocationsDTO.setPriority(10);
        RegraAlocaoClusterLocationsTipoLocationDTO regraAlocaoClusterLocationsDTO =
                new RegraAlocaoClusterLocationsTipoLocationDTO();
        regraAlocaoClusterLocationsDTO.setId(regraAlocacaoClusterLocationsIdPayload);
        regraAlocaoClusterLocationsDTO.setCriterio(Constantes.RegraAlocacaoClusterLocationsTipo.TIPO_LOCATION);
        regraAlocaoClusterLocationsDTO.setLocationType(Location.TipoLocation.INTERNA);
        clusterLocationsDTO.getRegraAlocacaoClusterDTOList().add(regraAlocaoClusterLocationsDTO);

        /*
         * Mesmo contrato do cluster de materiais: regra persistida e payload
         * podem carregar instancias Long diferentes com o mesmo valor.
         */
        Assertions.assertDoesNotThrow(
                () -> clusteringFrontService.alocaRegrasAoClusterLocations(
                        clusterLocationsDTO,
                        clusterLocations));
        Assertions.assertEquals(
                1,
                clusterLocations.getRegrasAlocacaoClusterLocations().size());

    }

    @Test
    public void validaProcessoClusterMateriaisDemandPlanningCommunityShouldRejectPricingProcess() {

        ClusteringFacade clusteringFrontService = new ClusteringFacade();

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> clusteringFrontService.validaProcessoClusterMateriaisDemandPlanningCommunity("PRICING"));

    }

    @Test
    public void validaProcessoClusterMateriaisDemandPlanningCommunityShouldRejectUnknownProcess() {

        ClusteringFacade clusteringFrontService = new ClusteringFacade();

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> clusteringFrontService.validaProcessoClusterMateriaisDemandPlanningCommunity("SNP"));

    }

    @Test
    public void validaProcessoClusterMateriaisDemandPlanningCommunityShouldAcceptDemandPlanningProcess() {

        ClusteringFacade clusteringFrontService = new ClusteringFacade();

        Assertions.assertDoesNotThrow(
                () -> clusteringFrontService.validaProcessoClusterMateriaisDemandPlanningCommunity("DP"));

    }

    @Test
    public void getClusterProdutosDTOShouldRejectInvalidIdBeforeRepository() {

        ClusteringFacade clusteringFrontService = new ClusteringFacade();

        IllegalArgumentException missingIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> clusteringFrontService.getClusterProdutosDTO(" ", "DP"));
        Assertions.assertEquals(
                "Material cluster id is required",
                missingIdException.getMessage());

        IllegalArgumentException invalidIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> clusteringFrontService.getClusterProdutosDTO("ABC", "DP"));
        Assertions.assertEquals(
                "Material cluster id must be numeric: ABC",
                invalidIdException.getMessage());

    }

    @Test
    public void getClusterLocationsDTOShouldRejectInvalidIdBeforeRepository() {

        ClusteringFacade clusteringFrontService = new ClusteringFacade();

        IllegalArgumentException missingIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> clusteringFrontService.getClusterLocationsDTO(null));
        Assertions.assertEquals(
                "Location cluster id is required",
                missingIdException.getMessage());

        IllegalArgumentException invalidIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> clusteringFrontService.getClusterLocationsDTO("ABC"));
        Assertions.assertEquals(
                "Location cluster id must be numeric: ABC",
                invalidIdException.getMessage());

    }

    @Test
    public void deleteClusterProdutosShouldRejectMissingPayloadOrEnterpriseProcessBeforeRepositories() {

        ClusteringFacade clusteringFrontService = new ClusteringFacade();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> clusteringFrontService.deleteClusterProdutos(null));
        Assertions.assertEquals(
                "Cluster delete payload is required",
                illegalArgumentException.getMessage());

        ClusterRuleDTO clusterRuleDTOSemId = new ClusterRuleDTO();
        IllegalArgumentException idIllegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> clusteringFrontService.deleteClusterProdutos(clusterRuleDTOSemId));
        Assertions.assertEquals(
                "Material cluster delete payload id is required",
                idIllegalArgumentException.getMessage());

        ClusterRuleDTO clusterRuleDTOPricing = new ClusterRuleDTO();
        clusterRuleDTOPricing.setId(100L);
        clusterRuleDTOPricing.setProcess("PRICING");
        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> clusteringFrontService.deleteClusterProdutos(clusterRuleDTOPricing));

    }

    private CaracteristicaProdutoDTO criaStatusProdutoDTO(String descricao) {

        CaracteristicaProdutoDTO caracteristicaProdutoDTO = new CaracteristicaProdutoDTO();
        caracteristicaProdutoDTO.setDescricao(descricao);
        return caracteristicaProdutoDTO;

    }

    private static ClusteringFacade criaClusteringFrontServiceComClusterMateriaisSalvo(
            ClusterProdutosDemandPlanning clusterMateriaisDemandPlanningSalvo) throws Exception {

        ClusteringFacade clusteringFrontService = new ClusteringFacade();
        setPrivateField(
                clusteringFrontService,
                "clusterMateriaisDemandPlanningRepository",
                getClusterProdutosDemandPlanningRepositoryComSaveRetornando(
                        clusterMateriaisDemandPlanningSalvo));
        return clusteringFrontService;

    }

    private static ClusteringFacade criaClusteringFrontServiceComListaClusterMateriais(
            List<ClusterProdutosDemandPlanning> clusterMateriaisDemandPlanningList) throws Exception {

        ClusteringFacade clusteringFrontService = new ClusteringFacade();
        setPrivateField(
                clusteringFrontService,
                "clusterMateriaisDemandPlanningRepository",
                getClusterProdutosDemandPlanningRepositoryComListagemRetornando(
                        clusterMateriaisDemandPlanningList));
        return clusteringFrontService;

    }

    private static ClusterProdutosDemandPlanning criaClusterMateriaisDemandPlanningComId(Long id) {

        ClusterProdutosDemandPlanning clusterMateriaisDemandPlanning =
                new ClusterProdutosDemandPlanning();
        clusterMateriaisDemandPlanning.setId(id);
        return clusterMateriaisDemandPlanning;

    }

    private static ClusterProdutosDemandPlanningRepository
    getClusterProdutosDemandPlanningRepositoryComListagemRetornando(
            List<ClusterProdutosDemandPlanning> clusterMateriaisDemandPlanningList) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("customFindAllComRegrasAlocacaoEStatusProduto".equals(method.getName())
                    || "customFindAllByPadraoIsFalseComRegrasAlocacaoEStatusProduto".equals(method.getName())) {
                return clusterMateriaisDemandPlanningList;
            }
            if ("toString".equals(method.getName())) {
                return "ClusterProdutosDemandPlanningRepository listagem para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (ClusterProdutosDemandPlanningRepository) Proxy.newProxyInstance(
                ClusterProdutosDemandPlanningRepository.class.getClassLoader(),
                new Class<?>[]{ClusterProdutosDemandPlanningRepository.class},
                invocationHandler);

    }

    private static ClusterProdutosDemandPlanningRepository
    getClusterProdutosDemandPlanningRepositoryComFindByIdRetornando(
            Optional<ClusterProdutosDemandPlanning> optionalClusterProdutosDemandPlanning) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("findById".equals(method.getName())) {
                return optionalClusterProdutosDemandPlanning;
            }
            if ("toString".equals(method.getName())) {
                return "ClusterProdutosDemandPlanningRepository findById para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (ClusterProdutosDemandPlanningRepository) Proxy.newProxyInstance(
                ClusterProdutosDemandPlanningRepository.class.getClassLoader(),
                new Class<?>[]{ClusterProdutosDemandPlanningRepository.class},
                invocationHandler);

    }

    private static ClusterProdutosDemandPlanningRepository
    getClusterProdutosDemandPlanningRepositoryComSaveRetornando(
            ClusterProdutosDemandPlanning clusterMateriaisDemandPlanningSalvo) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("findById".equals(method.getName())) {
                return Optional.empty();
            }
            if ("save".equals(method.getName())) {
                return clusterMateriaisDemandPlanningSalvo;
            }
            if ("toString".equals(method.getName())) {
                return "ClusterProdutosDemandPlanningRepository save quebrado para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (ClusterProdutosDemandPlanningRepository) Proxy.newProxyInstance(
                ClusterProdutosDemandPlanningRepository.class.getClassLoader(),
                new Class<?>[]{ClusterProdutosDemandPlanningRepository.class},
                invocationHandler);

    }

    private static ClusteringFacade criaClusteringFrontServiceComClusterLocationsSalvo(
            ClusterLocations clusterLocationsSalvo) throws Exception {

        ClusteringFacade clusteringFrontService = new ClusteringFacade();
        setPrivateField(
                clusteringFrontService,
                "clusterLocationsRepository",
                getClusterLocationsRepositoryComSaveRetornando(clusterLocationsSalvo));
        return clusteringFrontService;

    }

    private static ClusteringFacade criaClusteringFrontServiceComListaClusterLocations(
            List<ClusterLocations> clusterLocationsList) throws Exception {

        ClusteringFacade clusteringFrontService = new ClusteringFacade();
        setPrivateField(
                clusteringFrontService,
                "clusterLocationsRepository",
                getClusterLocationsRepositoryComListagemRetornando(clusterLocationsList));
        return clusteringFrontService;

    }

    private static ClusterLocations criaClusterLocationsComId(Long id) {

        ClusterLocations clusterLocations = new ClusterLocations();
        clusterLocations.setId(id);
        return clusterLocations;

    }

    private static ClusterLocationsRepository getClusterLocationsRepositoryComListagemRetornando(
            List<ClusterLocations> clusterLocationsList) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("customFindAll".equals(method.getName())) {
                return clusterLocationsList;
            }
            if ("toString".equals(method.getName())) {
                return "ClusterLocationsRepository listagem para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (ClusterLocationsRepository) Proxy.newProxyInstance(
                ClusterLocationsRepository.class.getClassLoader(),
                new Class<?>[]{ClusterLocationsRepository.class},
                invocationHandler);

    }

    private static ClusterLocationsRepository getClusterLocationsRepositoryComSaveRetornando(
            ClusterLocations clusterLocationsSalvo) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("findById".equals(method.getName())) {
                return Optional.empty();
            }
            if ("save".equals(method.getName())) {
                return clusterLocationsSalvo;
            }
            if ("toString".equals(method.getName())) {
                return "ClusterLocationsRepository save quebrado para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (ClusterLocationsRepository) Proxy.newProxyInstance(
                ClusterLocationsRepository.class.getClassLoader(),
                new Class<?>[]{ClusterLocationsRepository.class},
                invocationHandler);

    }

    private static ClusterLocationsRepository getClusterLocationsRepositoryComFindByIdRetornando(
            Optional<ClusterLocations> optionalClusterLocations) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("findById".equals(method.getName())) {
                return optionalClusterLocations;
            }
            if ("toString".equals(method.getName())) {
                return "ClusterLocationsRepository findById para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (ClusterLocationsRepository) Proxy.newProxyInstance(
                ClusterLocationsRepository.class.getClassLoader(),
                new Class<?>[]{ClusterLocationsRepository.class},
                invocationHandler);

    }

    private static ClusteringFacade criaClusteringFrontServiceComRegraMaterialSalva(
            RegraAlocacaoClusterProdutos regraAlocacaoClusterProdutosSalva) throws Exception {

        ClusteringFacade clusteringFrontService = new ClusteringFacade();
        setPrivateField(
                clusteringFrontService,
                "regraAlocacaoClusterProdutosRepository",
                getRegraAlocacaoClusterProdutosRepositoryComSaveRetornando(
                        regraAlocacaoClusterProdutosSalva));
        return clusteringFrontService;

    }

    private static RegraAlocacaoClusterProdutosRepository
    getRegraAlocacaoClusterProdutosRepositoryComSaveRetornando(
            RegraAlocacaoClusterProdutos regraAlocacaoClusterProdutosSalva) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("save".equals(method.getName())) {
                return regraAlocacaoClusterProdutosSalva;
            }
            if ("toString".equals(method.getName())) {
                return "RegraAlocacaoClusterProdutosRepository save quebrado para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (RegraAlocacaoClusterProdutosRepository) Proxy.newProxyInstance(
                RegraAlocacaoClusterProdutosRepository.class.getClassLoader(),
                new Class<?>[]{RegraAlocacaoClusterProdutosRepository.class},
                invocationHandler);

    }

    private static ClusteringFacade criaClusteringFrontServiceComRegraLocationSalva(
            RegraAlocacaoClusterLocations regraAlocacaoClusterLocationsSalva) throws Exception {

        ClusteringFacade clusteringFrontService = new ClusteringFacade();
        setPrivateField(
                clusteringFrontService,
                "regraAlocacaoClusterLocationsRepository",
                getRegraAlocacaoClusterLocationsRepositoryComSaveRetornando(
                        regraAlocacaoClusterLocationsSalva));
        return clusteringFrontService;

    }

    private static RegraAlocacaoClusterLocationsRepository
    getRegraAlocacaoClusterLocationsRepositoryComSaveRetornando(
            RegraAlocacaoClusterLocations regraAlocacaoClusterLocationsSalva) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("save".equals(method.getName())) {
                return regraAlocacaoClusterLocationsSalva;
            }
            if ("toString".equals(method.getName())) {
                return "RegraAlocacaoClusterLocationsRepository save quebrado para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (RegraAlocacaoClusterLocationsRepository) Proxy.newProxyInstance(
                RegraAlocacaoClusterLocationsRepository.class.getClassLoader(),
                new Class<?>[]{RegraAlocacaoClusterLocationsRepository.class},
                invocationHandler);

    }

    private static void setPrivateField(
            Object target,
            String fieldName,
            Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

}
