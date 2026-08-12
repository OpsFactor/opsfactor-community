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
 * Definicao concreta de um cluster de materiais.
 *
 * <p>O nome fisico ainda usa `Produtos`, mas o contrato funcional Community
 * trata este agregado como a definicao unica de cluster de materiais. Os
 * consumidores podem combinar materiais e locations sem criar uma
 * classificacao paralela por processo.</p>
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@DiscriminatorValue("demandplanning")
public class ClusterMateriais extends ClusterProdutos {

    public ClusterMateriais(String descricao, Boolean padrao, Integer prioridade) {
        super(descricao, padrao, prioridade);
    }

    /**
     * Parametros de forecast associados a este cluster de materiais em cada
     * cluster de location.
     *
     * <p>O discriminador legado permanece apenas para leitura das linhas ja
     * persistidas; ele nao participa do contrato nem da classificacao
     * funcional.</p>
     */
    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "parametrosDemandPlanNivelClusterCompositeKey.clusterMateriais", orphanRemoval = true)
    private List<ParametrosDemandPlanNivelCluster> parametrosDemandPlanNivelCluster = new ArrayList<>();

}
