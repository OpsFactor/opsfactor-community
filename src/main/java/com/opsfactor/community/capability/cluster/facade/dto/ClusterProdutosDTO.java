package com.opsfactor.community.capability.cluster.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.capability.masterdata.product.material.facade.dto.ProdutoDTO;
import lombok.Data;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO de cluster de materiais.
 *
 * <p>O nome da classe ainda acompanha o modelo fisico legado
 * `ClusterProdutos`, mas o contrato publico novo deve falar em material. Por
 * isso a lista exposta ao front usa {@link #materials}; novos campos nao devem
 * reintroduzir `product`/`produto` como vocabulario de API.</p>
 */
@Data @JsonInclude(JsonInclude.Include.NON_NULL)
public class ClusterProdutosDTO {
    private Long id;
    private String description;
    private Integer priority;
    private String process;

    private List<RegraAlocaoClusterProdutosDTO> regraAlocacaoClusterDTOList = new ArrayList<>();

    @Nullable
    public List<ProdutoDTO> materials;

    // campos do DTO antigo
    @Deprecated
    public String descricao;
    @Deprecated
    public Integer prioridade;
    @Deprecated
    public String codigo;


}
