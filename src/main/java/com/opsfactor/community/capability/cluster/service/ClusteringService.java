package com.opsfactor.community.capability.cluster.service;

import com.opsfactor.community.capability.cluster.domain.produto.ClusterMateriais;
import com.opsfactor.community.capability.cluster.repository.material.ClusterMateriaisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Servico minimo de clusterizacao disponivel no OpsFactor Community.
 *
 * <p>O Community preserva os clusters padrao necessarios para Demand Planning
 * e contratos compartilhados, mas nao possui cadastro nem calculo de curvas
 * ABC, Pricing ou outros agrupamentos Enterprise. Regras Enterprise baseadas
 * nesses conceitos devem ser implementadas no modulo Enterprise, junto com as
 * entidades, repositories e configuracoes correspondentes.</p>
 */
@Service
public class ClusteringService {

    /**
     * Descricao canonica do cluster default de materiais usado pelo Demand
     * Planning Community.
     */
    private static final String CLUSTER_PADRAO_HASH_DEMAND_PLANNING = "Default Material DP Cluster";

    /**
     * Repository do cluster padrao de Demand Planning.
     *
     * <p>O bean Spring permanece com `@Autowired` explicito para deixar claro
     * que e uma dependencia gerenciada, nao estado local.</p>
     */
    @Autowired
    private ClusterMateriaisRepository clusterMateriaisDemandPlanningRepository;

    /**
     * Garante que os clusters padrao existam.
     *
     * <p>Esta rotina e usada por fluxos de inicializacao/configuracao que
     * precisam de um agrupamento material default mesmo quando o usuario ainda
     * nao cadastrou clusters especificos.</p>
     */
    public void criaClustersPadrao() {

        getClusterProdutosDemandPlanningDefault();

    }

    /**
     * Retorna o cluster padrao de Demand Planning, criando-o se necessario.
     */
    public ClusterMateriais getClusterProdutosDemandPlanningDefault() {

        ClusterMateriais clusterMateriais;
        Optional<ClusterMateriais> optionalClusterProdutos =
                clusterMateriaisDemandPlanningRepository.findByDescricao(CLUSTER_PADRAO_HASH_DEMAND_PLANNING);
        validaOptionalClusterProdutosDemandPlanningDefaultCommunity(optionalClusterProdutos);
        clusterMateriais = optionalClusterProdutos.orElseGet(
                () -> {
                    ClusterMateriais clusterMateriaisCriado =
                            new ClusterMateriais(
                                    CLUSTER_PADRAO_HASH_DEMAND_PLANNING,
                                    true,
                                    9999999);
                    ClusterMateriais clusterMateriaisSalvo =
                            clusterMateriaisDemandPlanningRepository.save(clusterMateriaisCriado);
                    validaClusterProdutosDemandPlanningDefaultCommunity(clusterMateriaisSalvo);
                    return clusterMateriaisSalvo;
                }
        );
        validaClusterProdutosDemandPlanningDefaultCommunity(clusterMateriais);
        return clusterMateriais;

    }

    private void validaOptionalClusterProdutosDemandPlanningDefaultCommunity(
            Optional<ClusterMateriais> optionalClusterProdutos) {

        if (optionalClusterProdutos == null) {
            throw new IllegalArgumentException("Default material DP cluster lookup result is required.");
        }

    }

    private void validaClusterProdutosDemandPlanningDefaultCommunity(
            ClusterMateriais clusterMateriais) {

        if (clusterMateriais == null) {
            throw new IllegalArgumentException("Default material DP cluster is required.");
        }
        if (clusterMateriais.getId() == null) {
            throw new IllegalArgumentException("Default material DP cluster must have an id.");
        }
        if (!CLUSTER_PADRAO_HASH_DEMAND_PLANNING.equals(clusterMateriais.getDescricao())) {
            throw new IllegalArgumentException(
                    "Default material DP cluster must have description "
                            + CLUSTER_PADRAO_HASH_DEMAND_PLANNING
                            + ".");
        }

    }

}
