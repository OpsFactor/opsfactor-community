package com.opsfactor.community.capability.cluster.facade.mapper;

import com.opsfactor.community.capability.cluster.facade.dto.ClusterProdutosDTO;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutosDemandPlanning;
import com.opsfactor.community.capability.cluster.domain.produto.RegraAlocacaoClusterProdutos;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
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
    public void convertComRegrasAlocacaoDTOShouldRejectLegacyCharacteristicRuleCommunity() {

        ClusterProdutosDemandPlanning clusterProdutosDemandPlanning = new ClusterProdutosDemandPlanning(
                "Cluster",
                false,
                1);

        RegraAlocacaoClusterProdutos regraAlocacaoClusterProdutos = new RegraAlocacaoClusterProdutos();
        regraAlocacaoClusterProdutos.setClusterProdutos(clusterProdutosDemandPlanning);
        regraAlocacaoClusterProdutos.setRegraAlocacaoTipo(
                Constantes.RegraAlocacaoClusterProdutosTipo.CARACTERISTICA);
        clusterProdutosDemandPlanning.getRegrasAlocacaoClusterProdutos().add(regraAlocacaoClusterProdutos);

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> ClusterProdutosMapper.convertComRegrasAlocacaoDTO(clusterProdutosDemandPlanning));

    }

    @Test
    public void convertComRegrasAlocacaoDTOShouldHideLegacyNewStatusRuleCommunity() {

        ClusterProdutosDemandPlanning clusterProdutosDemandPlanning = criaClusterComRegraStatus(
                Constantes.StatusProduto.NOVO);

        ClusterProdutosDTO clusterProdutosDTO = ClusterProdutosMapper.convertComRegrasAlocacaoDTO(
                clusterProdutosDemandPlanning);

        Assertions.assertTrue(clusterProdutosDTO.getRegraAlocacaoClusterDTOList().isEmpty());

    }

    @Test
    public void convertComRegrasAlocacaoDTOShouldKeepRegularStatusRuleCommunity() {

        ClusterProdutosDemandPlanning clusterProdutosDemandPlanning = criaClusterComRegraStatus(
                Constantes.StatusProduto.REGULAR);

        ClusterProdutosDTO clusterProdutosDTO = ClusterProdutosMapper.convertComRegrasAlocacaoDTO(
                clusterProdutosDemandPlanning);

        Assertions.assertEquals(1, clusterProdutosDTO.getRegraAlocacaoClusterDTOList().size());
        Assertions.assertEquals(
                "REGULAR",
                clusterProdutosDTO.getRegraAlocacaoClusterDTOList().get(0).getCaracteristicaDTO().getDescricao());

    }

    private ClusterProdutosDemandPlanning criaClusterComRegraStatus(Constantes.StatusProduto statusProduto) {

        ClusterProdutosDemandPlanning clusterProdutosDemandPlanning = new ClusterProdutosDemandPlanning(
                "Cluster",
                false,
                1);

        RegraAlocacaoClusterProdutos regraAlocacaoClusterProdutos = new RegraAlocacaoClusterProdutos();
        regraAlocacaoClusterProdutos.setClusterProdutos(clusterProdutosDemandPlanning);
        regraAlocacaoClusterProdutos.setRegraAlocacaoTipo(Constantes.RegraAlocacaoClusterProdutosTipo.STATUS_PRODUTO);
        regraAlocacaoClusterProdutos.addStatusProduto(statusProduto);
        clusterProdutosDemandPlanning.getRegrasAlocacaoClusterProdutos().add(regraAlocacaoClusterProdutos);

        return clusterProdutosDemandPlanning;

    }

}
