package com.opsfactor.community.capability.masterdata.product.material.facade;

import com.opsfactor.community.capability.cluster.facade.dto.ClusterProdutosDTO;
import com.opsfactor.community.capability.masterdata.product.material.facade.dto.ProdutoDTO;
import com.opsfactor.community.capability.cluster.facade.mapper.ClusterProdutosMapper;
import com.opsfactor.community.capability.masterdata.product.material.facade.mapper.MaterialMapper;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutos;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.cluster.repository.material.ClusterProdutosRepository;
import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service DTO Community para a borda publica de materiais.
 *
 * <p>As entidades, repositories e alguns DTOs continuam com nomes fisicos
 * `Produto`/`ClusterProdutos` enquanto o schema compartilhado nao for
 * renomeado. Esta classe e a borda de traducao usada pelos controllers novos:
 * nomes de metodos e variaveis seguem material, e os nomes fisicos ficam
 * restritos aos tipos Java inevitaveis.</p>
 */
@Service
public class MaterialDtoService {

    /**
     * Repository da entidade fisica transicional `Produto`, usado aqui apenas
     * para listar materiais ativos da borda publica.
     */
    @Autowired
    private ProdutoRepository produtoRepository;

    /**
     * Repository dos clusters fisicos de material. O nome Java ainda segue
     * `ClusterProdutos`, mas o contrato publico exposto pela service e
     * material cluster.
     */
    @Autowired
    private ClusterProdutosRepository clusterProdutosRepository;

    /**
     * Mapper Community que converte Produto para DTO de material sem expor
     * caracteristicas Enterprise.
     */
    @Autowired
    private MaterialMapper materialMapper;

    /**
     * Projection em memoria usada para resolver membros de cluster de material
     * sem consultas JPA por item.
     */
    @Autowired
    private ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory;

    /**
     * Lista materiais ativos para consumo do front Community.
     *
     * <p>Caracteristicas de material sao Enterprise; por isso a conversao
     * preserva apenas campos base e nao consulta repositorios de
     * caracteristicas.</p>
     */
    public List<ProdutoDTO> getMaterialDTOList() {

        List<Produto> materiaisAtivos = produtoRepository.customFindProdutosAtivos();
        validaMateriaisAtivosSnapshot(materiaisAtivos);
        if (!materiaisAtivos.isEmpty()) {
            return materialMapper.convertComStatusSemCaracteristicasPorMaterial(materiaisAtivos);
        }

        return new ArrayList<>();

    }

    /**
     * Lista clusters de materiais no formato DTO ainda compartilhado com o
     * dominio fisico `ClusterProdutos`.
     */
    public List<ClusterProdutosDTO> getMaterialClusterDTOList() {

        List<ClusterProdutosDTO> materialClusterDTOList = new ArrayList<>();

        Iterable<ClusterProdutos> clusterMateriaisIterable = clusterProdutosRepository.findAll();
        List<ClusterProdutos> clusterMateriaisList =
                getClusterMateriaisSnapshotValidado(clusterMateriaisIterable);

        for (ClusterProdutos clusterMateriais : clusterMateriaisList) {
            materialClusterDTOList.add(ClusterProdutosMapper.convertBase(clusterMateriais));
        }

        return materialClusterDTOList;

    }

    /**
     * Lista materiais pertencentes a um cluster de material.
     *
     * <p>A entidade fisica e o repository continuam usando `Produto`, mas a
     * borda publica Community/Enterprise deve nomear o conceito como
     * material. O retorno permanece sem caracteristicas porque caracteristicas
     * dinamicas de material sao Enterprise.</p>
     */
    public List<ProdutoDTO> getMaterialDTOListFromMaterialClusterId(Long materialClusterId) {

        if (materialClusterId == null) {
            throw new IllegalArgumentException(
                    "Material cluster id is required to list Community material DTOs.");
        }

        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();
        if (clusterEParametrosProjection == null) {
            throw new IllegalStateException(
                    "Cluster and parameters projection snapshot is required to list Community material DTOs.");
        }

        Set<Produto> materiaisDoCluster =
                clusterEParametrosProjection.getMateriaisDeClusterProdutosId(materialClusterId, true);
        validaMateriaisDoClusterSnapshot(
                materialClusterId,
                materiaisDoCluster);

        return materiaisDoCluster.stream()
                .map(MaterialMapper::convertSemStatusESemCaracteristicasPorMaterial)
                .sorted(Comparator.comparing(ProdutoDTO::getId))
                .collect(Collectors.toList());

    }

    /**
     * Valida a fotografia de materiais ativos retornada pelo repository.
     *
     * <p>Lista vazia e um snapshot operacional valido. Snapshot nulo, item
     * nulo ou material sem id indicam quebra de repository/projection e devem
     * falhar antes de o mapper calcular status ou devolver uma lista
     * aparentemente vazia para a SPA.</p>
     */
    private void validaMateriaisAtivosSnapshot(Collection<Produto> materiaisAtivos) {

        validaMateriaisSnapshot(
                materiaisAtivos,
                "Active material repository snapshot");

    }

    /**
     * Valida a fotografia de clusters de material retornada pelo repository.
     *
     * <p>Clusters sem id nao podem alimentar seletores de configuracao porque
     * a tela e as factories usam esse id para resolver o escopo material.</p>
     */
    private List<ClusterProdutos> getClusterMateriaisSnapshotValidado(
            Iterable<ClusterProdutos> clusterMateriaisIterable) {

        if (clusterMateriaisIterable == null) {
            throw new IllegalStateException(
                    "Material cluster repository snapshot is required.");
        }

        List<ClusterProdutos> clusterMateriaisList = new ArrayList<>();
        clusterMateriaisIterable.forEach(clusterMateriaisList::add);

        for (int indiceCluster = 0;
             indiceCluster < clusterMateriaisList.size();
             indiceCluster++) {
            ClusterProdutos clusterMateriais = clusterMateriaisList.get(indiceCluster);
            if (clusterMateriais == null) {
                throw new IllegalStateException(
                        "Material cluster repository snapshot item at index "
                                + indiceCluster
                                + " is required.");
            }
            if (clusterMateriais.getId() == null) {
                throw new IllegalStateException(
                        "Material cluster repository snapshot item at index "
                                + indiceCluster
                                + " requires id.");
            }
        }

        return clusterMateriaisList;

    }

    /**
     * Valida a fotografia de materiais resolvida a partir da projection de
     * parametros.
     *
     * <p>A projection e compartilhada por Demand Planning, Planning Book e
     * filtros de tela. Falhar aqui evita transformar cluster inexistente ou
     * projection corrompida em lista vazia silenciosa.</p>
     */
    private void validaMateriaisDoClusterSnapshot(
            Long materialClusterId,
            Set<Produto> materiaisDoCluster) {

        validaMateriaisSnapshot(
                materiaisDoCluster,
                "Material cluster "
                        + materialClusterId
                        + " projection snapshot");

    }

    /**
     * Valida uma colecao de materiais de uma borda DTO Community.
     */
    private void validaMateriaisSnapshot(
            Collection<Produto> materiais,
            String descricaoSnapshot) {

        if (materiais == null) {
            throw new IllegalStateException(descricaoSnapshot + " is required.");
        }

        Set<String> materialIds = new HashSet<>();
        int indiceMaterial = 0;
        for (Produto material : materiais) {
            if (material == null) {
                throw new IllegalStateException(
                        descricaoSnapshot
                                + " item at index "
                                + indiceMaterial
                                + " is required.");
            }
            if (material.getId() == null || material.getId().isBlank()) {
                throw new IllegalStateException(
                        descricaoSnapshot
                                + " item at index "
                                + indiceMaterial
                                + " requires material id.");
            }
            if (!materialIds.add(material.getId())) {
                throw new IllegalStateException(
                        descricaoSnapshot
                                + " has duplicated material id "
                                + material.getId()
                                + ".");
            }
            indiceMaterial++;
        }

    }

}
