package com.opsfactor.community.capability.masterdata.product.material.repository;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository Community de materiais/produtos mestres.
 *
 * <p>As operacoes de escrita invalidam projections de parametros e malha que
 * dependem do cadastro de material.</p>
 */
@Repository
public interface ProdutoRepository extends JpaRepository<Produto,String> {

    @Query("SELECT DISTINCT p FROM Produto p "
            + "WHERE p.ativo is null or p.ativo = true")
    List<Produto> customFindProdutosAtivos();

    /**
     * Carrega o snapshot completo de material para exportacao/JSON sem fazer
     * uma consulta lazy por unidade de medida em cada material.
     *
     * <p>As quatro relacoes sao {@code ManyToOne}; portanto, os fetch joins
     * preservam uma linha por material e nao exigem nova projection ou tabela.
     * O mapper Community usa as tres UOMs operacionais e o overlay Enterprise
     * usa tambem a UOM de COGS.</p>
     */
    @Query("SELECT DISTINCT p FROM Produto p "
            + "LEFT JOIN FETCH p.mapaProdutoAtributo "
            + "LEFT JOIN FETCH p.unidadeMedidaPadrao "
            + "LEFT JOIN FETCH p.unidadeMedidaVendas "
            + "LEFT JOIN FETCH p.unidadeMedidaTransferencia "
            + "LEFT JOIN FETCH p.unitCogsUnitOfMeasure")
    List<Produto> findAllWithUnitOfMeasures();

    /**
     * Carrega materiais e valores de caracteristicas para o merge de um lote,
     * evitando inicializacao lazy material a material durante o upload.
     */
    @Query("SELECT DISTINCT p FROM Produto p "
            + "LEFT JOIN FETCH p.mapaProdutoAtributo "
            + "WHERE p.id IN :materialIds")
    List<Produto> findAllByIdWithCharacteristics(Collection<String> materialIds);

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva material mestre e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public <S extends Produto> S save(S entity);

    /**
     * Salva materiais mestres em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends Produto> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove material mestre e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public void delete(Produto entity);

    /**
     * Remove materiais mestres em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends Produto> entities);


}
