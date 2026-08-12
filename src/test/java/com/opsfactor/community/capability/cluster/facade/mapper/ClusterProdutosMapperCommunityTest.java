package com.opsfactor.community.capability.cluster.facade.mapper;

import com.opsfactor.community.capability.cluster.facade.dto.ClusterProdutosDTO;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterMateriais;
import com.opsfactor.community.capability.cluster.domain.produto.RegraAlocacaoClusterProdutos;
import com.opsfactor.community.capability.cluster.domain.produto.RegraAlocacaoClusterProdutosCaracteristica;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProduto;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Valida a leitura Community de regras de cluster de material.
 *
 * <p>O service ja bloqueia criacao de regra `NEW`, mas bases legadas podem
 * conter esse status persistido. O mapper nao deve devolver essa regra ao
 * front Community, pois ela depende do tratamento Enterprise de new materials.</p>
 */
public class ClusterProdutosMapperCommunityTest {

    @Test
    public void convertComRegrasAlocacaoDTOShouldExposeCharacteristicRuleCommunity() {

        ClusterMateriais clusterMateriais = new ClusterMateriais(
                "Cluster",
                false,
                1);

        RegraAlocacaoClusterProdutos regraAlocacaoClusterProdutos = new RegraAlocacaoClusterProdutos();
        regraAlocacaoClusterProdutos.setClusterProdutos(clusterMateriais);
        regraAlocacaoClusterProdutos.setRegraAlocacaoTipo(
                Constantes.RegraAlocacaoClusterProdutosTipo.CARACTERISTICA);
        CaracteristicaProduto caracteristicaProduto = new CaracteristicaProduto();
        caracteristicaProduto.setId("BRAND");
        caracteristicaProduto.setDescricao("Brand");
        regraAlocacaoClusterProdutos.addRegraAlocacaoCaracteristica(
                new RegraAlocacaoClusterProdutosCaracteristica(
                        new RegraAlocacaoClusterProdutosCaracteristica
                                .RegraAlocacaoClusterProdutosCaracteristicaCompositeKey(
                                        regraAlocacaoClusterProdutos,
                                        caracteristicaProduto,
                                        "White Paper")));
        clusterMateriais.getRegrasAlocacaoClusterProdutos().add(regraAlocacaoClusterProdutos);

        ClusterProdutosDTO clusterProdutosDTO = ClusterProdutosMapper.convertComRegrasAlocacaoDTO(
                clusterMateriais);

        Assertions.assertEquals(1, clusterProdutosDTO.getRegraAlocacaoClusterDTOList().size());
        Assertions.assertEquals(
                Constantes.RegraAlocacaoClusterProdutosTipo.CARACTERISTICA,
                clusterProdutosDTO.getRegraAlocacaoClusterDTOList().get(0).getCriterio());
        Assertions.assertEquals(
                "BRAND",
                clusterProdutosDTO.getRegraAlocacaoClusterDTOList().get(0).getCaracteristicaDTO().getCaracteristicaId());
        Assertions.assertEquals(
                java.util.List.of("White Paper"),
                clusterProdutosDTO.getRegraAlocacaoClusterDTOList().get(0).getCaracteristicaDTO().getListaAtributos());

    }

    @Test
    public void convertComRegrasAlocacaoDTOShouldHideLegacyNewStatusRuleCommunity() {

        ClusterMateriais clusterMateriais = criaClusterComRegraStatus(
                Constantes.StatusProduto.NOVO);

        ClusterProdutosDTO clusterProdutosDTO = ClusterProdutosMapper.convertComRegrasAlocacaoDTO(
                clusterMateriais);

        Assertions.assertTrue(clusterProdutosDTO.getRegraAlocacaoClusterDTOList().isEmpty());

    }

    @Test
    public void convertComRegrasAlocacaoDTOShouldKeepRegularStatusRuleCommunity() {

        ClusterMateriais clusterMateriais = criaClusterComRegraStatus(
                Constantes.StatusProduto.REGULAR);

        ClusterProdutosDTO clusterProdutosDTO = ClusterProdutosMapper.convertComRegrasAlocacaoDTO(
                clusterMateriais);

        Assertions.assertEquals(1, clusterProdutosDTO.getRegraAlocacaoClusterDTOList().size());
        Assertions.assertEquals(
                "REGULAR",
                clusterProdutosDTO.getRegraAlocacaoClusterDTOList().get(0).getCaracteristicaDTO().getDescricao());

    }

    private ClusterMateriais criaClusterComRegraStatus(Constantes.StatusProduto statusProduto) {

        ClusterMateriais clusterMateriais = new ClusterMateriais(
                "Cluster",
                false,
                1);

        RegraAlocacaoClusterProdutos regraAlocacaoClusterProdutos = new RegraAlocacaoClusterProdutos();
        regraAlocacaoClusterProdutos.setClusterProdutos(clusterMateriais);
        regraAlocacaoClusterProdutos.setRegraAlocacaoTipo(Constantes.RegraAlocacaoClusterProdutosTipo.STATUS_PRODUTO);
        regraAlocacaoClusterProdutos.addStatusProduto(statusProduto);
        clusterMateriais.getRegrasAlocacaoClusterProdutos().add(regraAlocacaoClusterProdutos);

        return clusterMateriais;

    }

}
