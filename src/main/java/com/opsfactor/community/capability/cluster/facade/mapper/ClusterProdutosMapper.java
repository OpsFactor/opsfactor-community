package com.opsfactor.community.capability.cluster.facade.mapper;

import com.opsfactor.community.capability.masterdata.product.material.facade.dto.ProdutoDTO;
import com.opsfactor.community.capability.masterdata.product.material.facade.mapper.MaterialMapper;
import com.opsfactor.community.capability.cluster.domain.produto.*;
import com.opsfactor.community.capability.cluster.facade.dto.ClusterProdutosDTO;
import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto.CaracteristicaProdutoDTO;
import com.opsfactor.community.capability.cluster.facade.dto.RegraAlocaoClusterProdutosDTO;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;

import java.util.List;
import java.util.stream.Collectors;

public abstract class ClusterProdutosMapper {

    public static ClusterProdutosDTO convertBase(ClusterProdutos clusterProdutos){
        ClusterProdutosDTO dto = new ClusterProdutosDTO();
        dto.setDescription(clusterProdutos.getDescricao()) ;
        dto.setId(clusterProdutos.getId());
        dto.setPriority(clusterProdutos.getPrioridade());
        if (clusterProdutos instanceof ClusterProdutosDemandPlanning){
            dto.setProcess("DP");
        }
        return dto;
    }

    public static ClusterProdutosDTO convertComRegrasAlocacaoDTO(ClusterProdutos clusterProdutos){

        ClusterProdutosDTO dto = convertBase(clusterProdutos);
        for (RegraAlocacaoClusterProdutos regrasAlocacaoClusterProduto :  clusterProdutos.getRegrasAlocacaoClusterProdutos()) {
            RegraAlocaoClusterProdutosDTO regraDTO = new RegraAlocaoClusterProdutosDTO();
            regraDTO.setId(regrasAlocacaoClusterProduto.getId());
            switch (regrasAlocacaoClusterProduto.getRegraAlocacaoTipo()) {
                case CARACTERISTICA:
                    throw new RequiresEnterpriseVersionException("Material characteristic cluster allocation");
                case STATUS_PRODUTO:
                    for (Constantes.StatusProduto statusProduto : regrasAlocacaoClusterProduto.getStatusProdutoSet()) {
                        /*
                         * NEW e capacidade Enterprise ligada ao tratamento de
                         * new materials. Se uma base legada ainda trouxer essa
                         * regra, o DTO Community simplesmente nao a devolve
                         * para a tela compartilhada.
                         */
                        if (Constantes.StatusProduto.NOVO.equals(statusProduto)) {
                            continue;
                        }
                        CaracteristicaProdutoDTO caracteristicaDTO = getCaracteristicaProdutoDTODeStatusProdutoCommunity(statusProduto);
                        regraDTO.setCaracteristicaDTO(caracteristicaDTO);
                        regraDTO.setCriterio(Constantes.RegraAlocacaoClusterProdutosTipo.STATUS_PRODUTO);
                        break; // pega sempre o 1o resultado Community para mandar para o front
                    }
                    break;
            }
            if (regraDTO.getCriterio() != null) {
                dto.getRegraAlocacaoClusterDTOList().add(regraDTO);
            }
        }
        if (clusterProdutos instanceof ClusterProdutosDemandPlanning){
            dto.setProcess("DP");
        }
        return dto;

    }

    private static CaracteristicaProdutoDTO getCaracteristicaProdutoDTODeStatusProdutoCommunity(
            Constantes.StatusProduto statusProduto) {

        CaracteristicaProdutoDTO caracteristicaDTO = new CaracteristicaProdutoDTO();
        switch (statusProduto) {
            case DESCONTINUADO:
                caracteristicaDTO.setCaracteristicaId("DISCONTINUED");
                caracteristicaDTO.setDescricao("DISCONTINUED");
                break;
            case REGULAR:
                caracteristicaDTO.setCaracteristicaId("REGULAR");
                caracteristicaDTO.setDescricao("REGULAR");
                break;
            case NAO_LANCADO:
                caracteristicaDTO.setCaracteristicaId("NOT RELEASED");
                caracteristicaDTO.setDescricao("NOT RELEASED");
                break;
            case NOVO:
                throw new RequiresEnterpriseVersionException("New material cluster allocation");
        }
        return caracteristicaDTO;

    }


    public static ClusterProdutosDTO convertComListaMateriaisERegrasAlocacaoDTO(
            ClusterProdutos clusterProdutos,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        ClusterProdutosDTO dto = convertComRegrasAlocacaoDTO(clusterProdutos);

        List<ProdutoDTO> produtoDTOList = clusterEParametrosProjection.getMateriaisDeClusterProdutos(clusterProdutos, true)
                .stream()
                .map(MaterialMapper::convertSemStatusESemCaracteristicasPorMaterial)
                .collect(Collectors.toList());

        dto.setMaterials(produtoDTOList);

        return dto;

    }

}
