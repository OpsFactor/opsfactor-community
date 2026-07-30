package com.opsfactor.community.capability.cluster.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto.CaracteristicaProdutoDTO;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data @JsonInclude(JsonInclude.Include.NON_NULL)
public class RegraAlocaoClusterProdutosDTO {
    /**
     * Id da regra
     */
    private Long id;
    
    /**
     * Nivel 1
     * Tipo do Criterio.
     *
     * <p>No Community apenas `STATUS_PRODUTO` e aceito. `CARACTERISTICA`
     * permanece no enum/DTO porque o front compartilhado pode enviar esse
     * payload, mas o service deve falhar com RequiresEnterpriseVersionException.</p>
     */
    private Constantes.RegraAlocacaoClusterProdutosTipo criterio;
    
    /**
     * Nivel 2
     * Valor do Critério : 'Categoria', 'Marca', 'Status Regular'
     */
    private CaracteristicaProdutoDTO caracteristicaDTO;

    /**
     * Nivel 3
     * Campo Enterprise/compatibilidade para criterio por caracteristica.
     */
    private List<String> atributosCaracteristica = new ArrayList<>();
}
