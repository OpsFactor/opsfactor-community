package com.opsfactor.community.capability.configuration.projection.cluster;

import com.opsfactor.community.capability.configuration.projection.parametros.CadastroEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.CadastroEParametrosProjectionFactory;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.domain.location.RegraAlocacaoClusterLocationsPaisEstado;
import com.opsfactor.community.capability.cluster.domain.location.RegraAlocacaoClusterLocationsTipoLocation;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutos;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterMateriais;
import com.opsfactor.community.capability.cluster.domain.produto.RegraAlocacaoClusterProdutosCaracteristica;
import com.opsfactor.community.capability.cluster.domain.produto.RegraAlocacaoClusterProdutosStatus;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.cluster.repository.location.ClusterLocationsRepository;
import com.opsfactor.community.capability.cluster.repository.material.ClusterMateriaisRepository;
import com.opsfactor.community.capability.configuration.repository.cluster.location.RegraAlocacaoClusterLocationsPaisEstadoRepository;
import com.opsfactor.community.capability.configuration.repository.cluster.location.RegraAlocacaoClusterLocationsTipoLocationRepository;
import com.opsfactor.community.capability.configuration.repository.cluster.produto.RegraAlocacaoClusterProdutosStatusRepository;
import com.opsfactor.community.capability.configuration.repository.cluster.produto.RegraAlocacaoClusterProdutosCaracteristicaRepository;
import com.opsfactor.community.platform.utility.FuncoesMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Carrega regras e índices de uma classificação em consultas por lote.
 * O cache identifica o perfil; cadastros comuns possuem cache independente.
 */
@Component
public class ClusterizacaoProjectionFactory {

    @Autowired
    private CadastroEParametrosProjectionFactory cadastroEParametrosProjectionFactory;

    @Autowired
    private ClusterMateriaisRepository clusterMateriaisDemandPlanningRepository;
    @Autowired
    private ClusterLocationsRepository clusterLocationsRepository;
    @Autowired
    private RegraAlocacaoClusterProdutosStatusRepository regraAlocacaoClusterProdutosStatusRepository;
    @Autowired
    private RegraAlocacaoClusterProdutosCaracteristicaRepository regraAlocacaoClusterProdutosCaracteristicaRepository;
    @Autowired
    private RegraAlocacaoClusterLocationsPaisEstadoRepository regraAlocacaoClusterLocationsPaisEstadoRepository;
    @Autowired
    private RegraAlocacaoClusterLocationsTipoLocationRepository regraAlocacaoClusterLocationsTipoLocationRepository;

    /** IDs locais de clusters não são compartilhados por perfis distintos. */
    @Cacheable(value = "clusterizacaoProjection", key = "#p0", sync = true)
    public ClusterizacaoProjection getClusterizacaoProjectionDeCache(String perfilClusterizacaoId) {

        return getClusterizacaoProjectionBase(perfilClusterizacaoId,
                cadastroEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache());

    }

    /** Um recorte de execução não pode substituir a fotografia completa em cache. */
    public ClusterizacaoProjection getClusterizacaoProjectionBase(String perfilClusterizacaoId,
                                                                 CadastroEParametrosProjection cadastroEParametrosProjection) {

        ClusterizacaoProjection clusterizacaoProjection = new ClusterizacaoProjection(
                perfilClusterizacaoId, cadastroEParametrosProjection);

        /*
         * Carrega os clusters de materiais de Demand Planning. Os campos
         * internos da projection ainda preservam `ClusterProdutosDemandPlanning`
         * porque a entidade JPA transicional herda de ClusterProdutos, mas a
         * factory trabalha conceitualmente com materiais.
         */
        List<ClusterMateriais> clusterMateriaisList =
                clusterMateriaisDemandPlanningRepository.findAllByPerfilClusterizacaoId(perfilClusterizacaoId)
                        .stream()
                        .sorted(Comparator.comparing(ClusterProdutos::getPrioridade))
                        .collect(Collectors.toList());
        validaClustersMateriaisDemandPlanning(clusterMateriaisList);
        clusterizacaoProjection.clusterProdutosPadraoDP = exigePadraoUnico(clusterMateriaisList, ClusterProdutos::getPadrao, perfilClusterizacaoId, "materiais");
        clusterizacaoProjection.clusterMateriaisList =
                Collections.unmodifiableList(clusterMateriaisList);
        clusterizacaoProjection.clusterProdutosDemandPlanningMap = clusterizacaoProjection.clusterMateriaisList.stream()
                .collect(Collectors.toMap(x -> x.getId(), x -> x));

        clusterizacaoProjection.valoresCaracteristicaRegraAlocacaoClusterProdutos =
                regraAlocacaoClusterProdutosCaracteristicaRepository.findAllForClusteringProfile(perfilClusterizacaoId)
                        .stream()
                        .collect(Collectors.groupingBy(
                                RegraAlocacaoClusterProdutosCaracteristica::getRegraAlocacaoClusterProdutos,
                                Collectors.toSet()));
        FuncoesMap.convertToNestedUnmodifiableMap(
                clusterizacaoProjection.valoresCaracteristicaRegraAlocacaoClusterProdutos);

        clusterizacaoProjection.valoresStatusRegraAlocacaoClusterProdutos = regraAlocacaoClusterProdutosStatusRepository.findAllForClusteringProfile(perfilClusterizacaoId)
                .stream()
                .collect(Collectors.groupingBy(RegraAlocacaoClusterProdutosStatus::getRegraAlocacaoClusterProdutos, Collectors.toSet()));
        FuncoesMap.convertToNestedUnmodifiableMap(clusterizacaoProjection.valoresStatusRegraAlocacaoClusterProdutos);

        // CARREGA LISTA DE CLUSTERS DE LOCATIONS
        List<ClusterLocations> clusterLocationsList =
                clusterLocationsRepository.findAllByPerfilClusterizacaoId(perfilClusterizacaoId)
                        .stream()
                        .sorted(Comparator.comparingInt(ClusterLocations::getPrioridade))
                        .collect(Collectors.toList());
        validaClustersLocations(clusterLocationsList);
        clusterizacaoProjection.clusterLocationsList =
                Collections.unmodifiableList(clusterLocationsList);
        clusterizacaoProjection.clusterLocationsPadrao = exigePadraoUnico(clusterizacaoProjection.clusterLocationsList, ClusterLocations::getPadrao, perfilClusterizacaoId, "locations");
        clusterizacaoProjection.clusterLocationsMap = clusterizacaoProjection.clusterLocationsList.stream()
                .collect(Collectors.toMap(
                        ClusterLocations::getId,
                        Function.identity()));

        clusterizacaoProjection.paisEstadoRegraAlocacaoClusterLocations = regraAlocacaoClusterLocationsPaisEstadoRepository.findAllForClusteringProfile(perfilClusterizacaoId)
                .stream()
                .collect(Collectors.groupingBy(RegraAlocacaoClusterLocationsPaisEstado::getRegraAlocacaoClusterLocations, Collectors.toSet()));
        FuncoesMap.convertToNestedUnmodifiableMap(clusterizacaoProjection.paisEstadoRegraAlocacaoClusterLocations);
        clusterizacaoProjection.tipoLocationRegraAlocacaoClusterLocations = regraAlocacaoClusterLocationsTipoLocationRepository.findAllForClusteringProfile(perfilClusterizacaoId)
                .stream()
                .collect(Collectors.groupingBy(RegraAlocacaoClusterLocationsTipoLocation::getRegraAlocacaoClusterLocations, Collectors.toSet()));
        FuncoesMap.convertToNestedUnmodifiableMap(clusterizacaoProjection.tipoLocationRegraAlocacaoClusterLocations);

        // Pré-calcula a alocação material -> cluster DP para eliminar recálculo em loops críticos.
        // A regra de status é avaliada no nível global do material (sem material/location).
        clusterizacaoProjection.clusterProdutosDemandPlanningPorMaterial = Collections.unmodifiableMap(
                cadastroEParametrosProjection.getMaterialSet().stream()
                        .collect(Collectors.toMap(
                                Function.identity(),
                                clusterizacaoProjection::getClusterProdutosDemandPlanningSemCache)));

        // Pré-calcula a alocação location -> cluster para eliminar recálculo em loops críticos.
        clusterizacaoProjection.clusterLocationsPorLocation = Collections.unmodifiableMap(
                cadastroEParametrosProjection.getLocationSet().stream()
                        .collect(Collectors.toMap(
                                Function.identity(),
                                clusterizacaoProjection::getClusterLocationsDeLocationSemCache)));

        // Publicação somente depois da materialização: os leitores não podem
        // alterar índices compartilhados por execuções do mesmo perfil.
        clusterizacaoProjection.clusterProdutosDemandPlanningMap =
                Collections.unmodifiableMap(new HashMap<>(clusterizacaoProjection.clusterProdutosDemandPlanningMap));
        clusterizacaoProjection.clusterLocationsMap =
                Collections.unmodifiableMap(new HashMap<>(clusterizacaoProjection.clusterLocationsMap));

        return clusterizacaoProjection;

    }

    private void validaClustersMateriaisDemandPlanning(
            Collection<ClusterMateriais> clusterMateriaisCollection) {

        Set<Long> clusterProdutosDemandPlanningIds = new HashSet<>();
        int indice = 0;
        for (ClusterMateriais clusterMateriais : clusterMateriaisCollection) {
            if (clusterMateriais.getId() == null) {
                throw new IllegalStateException(
                        "Demand Planning material cluster repository item at index "
                                + indice
                                + " requires id for Cluster and parameters projection.");
            }
            if (!clusterProdutosDemandPlanningIds.add(clusterMateriais.getId())) {
                throw new IllegalStateException(
                        "Demand Planning material cluster repository has duplicated id "
                                + clusterMateriais.getId()
                                + " for Cluster and parameters projection.");
            }
            indice++;
        }

    }

    private void validaClustersLocations(
            Collection<ClusterLocations> clusterLocationsCollection) {

        Set<Long> clusterLocationsIds = new HashSet<>();
        int indice = 0;
        for (ClusterLocations clusterLocations : clusterLocationsCollection) {
            if (clusterLocations.getId() == null) {
                throw new IllegalStateException(
                        "Location cluster repository item at index "
                                + indice
                                + " requires id for Cluster and parameters projection.");
            }
            if (!clusterLocationsIds.add(clusterLocations.getId())) {
                throw new IllegalStateException(
                        "Location cluster repository has duplicated id "
                                + clusterLocations.getId()
                                + " for Cluster and parameters projection.");
            }
            indice++;
        }

    }

    private static <T> T exigePadraoUnico(List<T> clusters, Function<T, Boolean> padrao,
                                        String perfilId, String dimensao) {

        List<T> padroes = clusters.stream().filter(cluster -> Boolean.TRUE.equals(padrao.apply(cluster))).toList();
        if (padroes.size() != 1) {
            throw new IllegalStateException("Perfil de clusterização " + perfilId + ": esperado exatamente um cluster padrão de " + dimensao + ", encontrados " + padroes.size() + ".");
        }
        return padroes.getFirst();

    }

}
