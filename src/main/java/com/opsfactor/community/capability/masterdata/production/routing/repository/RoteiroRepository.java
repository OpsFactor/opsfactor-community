package com.opsfactor.community.capability.masterdata.production.routing.repository;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import java.util.Collection;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository JPA de RoteiroRepository.
 */
@Repository
public interface RoteiroRepository extends JpaRepository<Roteiro,String> {

    /**
     * Carrega a fotografia administrativa dos roteiros com suas dimensoes
     * estruturais necessarias para a listagem Community.
     *
     * <p>O mapper e a validacao da borda acessam location e material de saida
     * de cada roteiro. Os fetch joins unicos impedem uma consulta lazy adicional
     * por roteiro sem alterar o snapshot usado pelas projections de supply.</p>
     */
    @Query("SELECT r FROM Roteiro r "
            + "LEFT JOIN FETCH r.location "
            + "LEFT JOIN FETCH r.materialOutput")
    List<Roteiro> customFindAllForFront();

    /**
     * Carrega exclusivamente a fotografia usada pelo diagnóstico legado de
     * inconsistências de roteiros.
     *
     * <p>O diagnóstico navega o material de saída e ordena as operações de
     * cada roteiro. Os dois fetch joins evitam o padrão 1 + 2N sem incluir
     * associações administrativas que não participam da checagem.</p>
     */
    @Query("SELECT DISTINCT r FROM Roteiro r "
            + "LEFT JOIN FETCH r.materialOutput "
            + "LEFT JOIN FETCH r.operacaoRoteiroSet")
    List<Roteiro> customFindAllForConsistencyDiagnostic();

    @Query("SELECT DISTINCT r FROM Roteiro r "
            + "LEFT JOIN FETCH r.location "
            + "LEFT JOIN FETCH r.materialOutput "
            + "LEFT JOIN FETCH r.unidadeMedidaQuantidadeBase "
            + "LEFT JOIN FETCH r.operacaoRoteiroSet opr "
            + "LEFT JOIN FETCH opr.recursoProdutivo "
            + "WHERE r.location IN :locations "
            + "AND r.materialOutput IN :materiais")
    public List<Roteiro> customFindAllByLocationInAndMaterialOutputInFetchOperacaoRoteiroSet(
            @Param("locations") Collection<Location> locations,
            @Param("materiais") Collection<Produto> materiais);

    /**
     * Localiza, sem materializar os roteiros, os IDs de routing cluster que
     * ainda sao referenciados por roteiros Community.
     *
     * <p>O campo {@code routingClusterId} e escalar para que a tabela de
     * {@link Roteiro} nao dependa da entidade privada Enterprise. O overlay
     * usa esta leitura em lote antes de remover clusters, preservando a
     * integridade funcional sem criar associacao JPA cross-boundary.</p>
     */
    @Query("SELECT DISTINCT r.routingClusterId FROM Roteiro r "
            + "WHERE r.routingClusterId IN :routingClusterIds")
    List<String> customFindReferencedRoutingClusterIds(
            @Param("routingClusterIds") Collection<String> routingClusterIds);

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva roteiro e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    public <S extends Roteiro> S save(S entity);

    /**
     * Salva roteiros em lote e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    <S extends Roteiro> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove roteiro e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    public void delete(Roteiro entity);

    /**
     * Remove roteiros em lote e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    void deleteAll(Iterable<? extends Roteiro> entities);

}
