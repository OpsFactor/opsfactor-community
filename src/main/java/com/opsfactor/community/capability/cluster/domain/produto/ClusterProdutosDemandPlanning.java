package com.opsfactor.community.capability.cluster.domain.produto;

import com.opsfactor.community.capability.demandplanning.configuration.domain.ParametrosDemandPlanNivelCluster;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

/**
 * Cluster de materiais usado pela configuracao de Demand Planning.
 *
 * <p>O nome fisico ainda usa `Produtos`, mas o contrato funcional Community
 * trata este agrupamento como cluster de materiais. Ele agrupa materiais com
 * comportamento de demanda semelhante para definir parametros de forecast por
 * combinacao cluster material/location.</p>
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@DiscriminatorValue("demandplanning")
public class ClusterProdutosDemandPlanning extends ClusterProdutos {

    public ClusterProdutosDemandPlanning(String descricao, Boolean padrao, Integer prioridade) {
        super(descricao, padrao, prioridade);
    }

    /**
     * Parametros de forecast associados a este cluster de materiais em cada
     * cluster de location.
     */
    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "parametrosDemandPlanNivelClusterCompositeKey.clusterProdutosDemandPlanning", orphanRemoval = true)
    private List<ParametrosDemandPlanNivelCluster> parametrosDemandPlanNivelCluster = new ArrayList<>();

}
