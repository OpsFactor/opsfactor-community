package com.opsfactor.community.capability.masterdata.product.material.facade.mapper;

import com.opsfactor.community.capability.masterdata.product.material.facade.dto.ProdutoDTO;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper Community entre a entidade fisica de material e o DTO publico.
 *
 * <p>O nome da entidade JPA permanece {@link Produto} por compatibilidade com
 * o schema transicional, mas esta classe define a linguagem nova da borda DTO:
 * material. Caracteristicas dinamicas, valores por caracteristica e demais
 * metadados Enterprise nao sao populados aqui.</p>
 */
@Mapper(componentModel = "spring")
public abstract class MaterialMapper {

    /**
     * Service de parametros globais usado para calcular o status operacional
     * do material com as regras atuais de produto novo/inativo.
     *
     * <p>No Community os parametros Enterprise de tratamento especial de novo
     * material ficam saneados; ainda assim o status precisa da entidade global
     * para preservar a regra fisica existente em {@link Produto}.</p>
     */
    @Autowired
    private ParametrosGlobaisService parametrosGlobaisService;

    /**
     * Converte a entidade fisica `Produto` para o DTO publico de material.
     *
     * <p>O mapper ainda recebe {@link Produto} porque a entidade JPA
     * compartilhada nao foi renomeada. A semantica da borda, porem, e material:
     * nao expomos caracteristicas Enterprise e calculamos apenas o status
     * operacional permitido no Community.</p>
     */
    private ProdutoDTO convertComStatusSemCaracteristicasPorMaterial(Produto material, ParametrosGlobais parametrosGlobais) {

        ProdutoDTO produtoDTO = new ProdutoDTO();
        produtoDTO.setId(material.getId());
        produtoDTO.setDescription(material.getDescricao());
        produtoDTO.setActive(material.getAtivo());
        produtoDTO.setMaterialStatus(material.getStatusProduto(LocalDateTime.now(), parametrosGlobais).toString());
        return produtoDTO;

    }

    /**
     * Conversao simples usada em listas de membros de cluster, quando a tela
     * precisa apenas de id/descricao e nao deve recalcular status.
     */
    @Named("convertSemStatusESemCaracteristicasPorMaterial")
    public static ProdutoDTO convertSemStatusESemCaracteristicasPorMaterial(Produto material) {

        ProdutoDTO produtoDTO = new ProdutoDTO();
        produtoDTO.setId(material.getId());
        produtoDTO.setDescription(material.getDescricao());
        return produtoDTO;

    }

    /**
     * Converte o DTO publico de material para a entidade fisica transicional.
     *
     * <p>Usado apenas em fluxos simples de master data; regras Enterprise de
     * caracteristicas, sucessao ou filtros nao entram nesta conversao.</p>
     */
    public static Produto convertDTOToEntity(ProdutoDTO produtoDTO) {

        Produto material = new Produto();
        material.setId(produtoDTO.getId());
        material.setDescricao(produtoDTO.getDescription());
        material.setAtivo(produtoDTO.getActive());
        return material;

    }
    
    /**
     * Conversao padrao Community para listas de material.
     *
     * <p>Caracteristicas de material sao Enterprise; por isso o DTO Community
     * preserva apenas campos base, atividade e status calculado.</p>
     */
    public List<ProdutoDTO> convertComStatusSemCaracteristicasPorMaterial(Collection<Produto> materiais) {

        ParametrosGlobais parametrosGlobais = parametrosGlobaisService.getParametrosGlobais();
        return materiais.stream()
                .map(material -> convertComStatusSemCaracteristicasPorMaterial(material, parametrosGlobais))
                .collect(Collectors.toList());

    }
    
}
