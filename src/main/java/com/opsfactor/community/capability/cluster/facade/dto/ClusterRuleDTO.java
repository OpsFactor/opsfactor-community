package com.opsfactor.community.capability.cluster.facade.dto;

import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto.CaracteristicaProdutoDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Envelope historico usado pelos endpoints de exclusao de clusters.
 *
 * <p>No Community os endpoints usam apenas `id`, `name`, `criterion` e
 * `priority`. `characteristics` e `attributes` permanecem no DTO
 * para tolerar payloads antigos do front compartilhado, mas nao reabrem
 * criterios por caracteristica dinamica: criacao/edicao dessas regras falha no
 * `ClusteringFrontService`.</p>
 */
@Data @NoArgsConstructor @AllArgsConstructor
public class ClusterRuleDTO {
    private Long id;
    private String name;
    private String criterion;
    private Integer priority;
    /**
     * Campo tolerado apenas para payloads antigos do front compartilhado.
     * Qualquer uso funcional de caracteristicas dinamicas deve falhar antes de
     * persistir uma regra Community.
     */
    private List<CaracteristicaProdutoDTO> characteristics;
    /**
     * Alias historico de atributos/caracteristicas enviado por telas antigas.
     * No Community, criterios validos continuam restritos aos campos simples da
     * regra de cluster.
     */
    private List<String> attributes;
}
