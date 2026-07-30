package com.opsfactor.community.capability.transactionaldata.sales.sellout.facade.mapper;

import com.opsfactor.community.capability.transactionaldata.sales.sellout.facade.dto.SelloutReportDTO;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.domain.Sellout;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;

/**
 * Mapper do relatorio de venda observada Community.
 *
 * <p>O Community usa a UOM padrao DP dos parametros globais para este report.
 * Parametrizacoes mais granulares por cluster/projection ficam fora deste
 * mapper para manter o sell-out quantitativo simples.</p>
 */
public class SelloutReportMapper {
    
    public static SelloutReportDTO convertEntityToDTO(
            Sellout entity,
            ClusterEParametrosProjection clusterEParametrosProjection,
            UnidadeMedidaProjection unidadeMedidaProjection) {    
        
        ParametrosGlobais parametrosGlobais = clusterEParametrosProjection.getParametrosGlobais();
        
        Produto material = entity.getProduto();
        Location locationOrigem = entity.getLocationOrigem();

        UnidadeMedida unidadeMedidaSellout = entity.getUnidadeMedida(parametrosGlobais);
        UnidadeMedida unidadeMedidaPadraoDP = parametrosGlobais.getUnidadeMedidaPadraoDP();
        UnidadeMedida unidadeMedidaPadraoSNP = clusterEParametrosProjection.getSNPUnidadeMedidaPadrao(material, locationOrigem);
        
        double quantidadeUnidadeSellout = entity.getQuantidade();

        double conversaoParaUnidadeMedidaPadraoDP = unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                material, unidadeMedidaSellout, unidadeMedidaPadraoDP);
        double conversaoParaUnidadeMedidaPadraoSNP = unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                material, unidadeMedidaSellout, unidadeMedidaPadraoSNP);

        return SelloutReportDTO.builder()
                .documentId(entity.getId())
                .referenceDate(entity.getDataVenda())
                .originLocationId(entity.getLocationOrigem().getId())
                .materialId(material.getId())
                .uomId(unidadeMedidaSellout.getId())
                .quantity(quantidadeUnidadeSellout)
                .defaultDpUomId(unidadeMedidaPadraoDP.getId())
                .quantityInDefaultDpUom(quantidadeUnidadeSellout * conversaoParaUnidadeMedidaPadraoDP)
                .defaultSnpUomId(unidadeMedidaPadraoSNP.getId())
                .quantityInDefaultSnpUom(quantidadeUnidadeSellout * conversaoParaUnidadeMedidaPadraoSNP)
                .build();
        
    }
    
}
