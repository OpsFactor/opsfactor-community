package com.opsfactor.community.capability.masterdata.network.supplynetwork.repository;

import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporteProduto;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import java.util.Collection;

import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository JPA de LinhaTransporteProdutoRepository.
 */
@Repository
public interface LinhaTransporteProdutoRepository extends JpaRepository<LinhaTransporteProduto,LinhaTransporteProduto.LinhaTransporteProdutoCompositeKey> {

    @Query("SELECT ltp FROM LinhaTransporteProduto ltp "
            + "LEFT JOIN FETCH ltp.linhaTransporteProdutoCompositeKey.produto "
            + "LEFT JOIN FETCH ltp.linhaTransporteProdutoCompositeKey.linhaTransporte lt "
            + "LEFT JOIN FETCH lt.linhaTransporteCompositeKey.locationOrigem lo "
            + "LEFT JOIN FETCH lt.linhaTransporteCompositeKey.locationDestino ld")
    public List<LinhaTransporteProduto> customFindAll();

    @Query("SELECT ltp FROM LinhaTransporteProduto ltp "
            + "LEFT JOIN FETCH ltp.linhaTransporteProdutoCompositeKey.produto "
            + "LEFT JOIN FETCH ltp.linhaTransporteProdutoCompositeKey.linhaTransporte lt "
            + "LEFT JOIN FETCH lt.linhaTransporteCompositeKey.locationOrigem lo "
            + "LEFT JOIN FETCH lt.linhaTransporteCompositeKey.locationDestino ld "
            + "WHERE lt.linhaTransporteCompositeKey.versaoMalha.id IN :versaoMalhaIds")
    public List<LinhaTransporteProduto> customFindByVersaoMalhaIdIn(@Param("versaoMalhaIds") Collection<String> versaoMalhaIds);

    public List<LinhaTransporteProduto> findByLinhaTransporteProdutoCompositeKeyLinhaTransporteLinhaTransporteCompositeKeyVersaoMalha(VersaoMalha versaoMalha);

    /**
     * Carrega a fotografia administrativa de overrides material/lane. A
     * listagem Community acessa produto, lane, versao, origem, destino e as
     * UOMs configuradas tanto na lane quanto no override; o fetch unico evita
     * que cada item da tela inicialize proxies adicionais.
     */
    @Query("SELECT DISTINCT ltp FROM LinhaTransporteProduto ltp "
            + "LEFT JOIN FETCH ltp.linhaTransporteProdutoCompositeKey.produto p "
            + "LEFT JOIN FETCH ltp.linhaTransporteProdutoCompositeKey.linhaTransporte lt "
            + "LEFT JOIN FETCH lt.linhaTransporteCompositeKey.versaoMalha vm "
            + "LEFT JOIN FETCH lt.linhaTransporteCompositeKey.locationOrigem lo "
            + "LEFT JOIN FETCH lt.linhaTransporteCompositeKey.locationDestino ld "
            + "LEFT JOIN FETCH lt.unidadeMedidaLoteMinimoMultiploTransporte laneUom "
            + "LEFT JOIN FETCH ltp.unidadeMedidaLoteMinimoMultiploTransporte overrideUom "
            + "WHERE vm = :versaoMalha")
    List<LinhaTransporteProduto> customFindForFrontByVersaoMalha(
            @Param("versaoMalha") VersaoMalha versaoMalha);

    /**
     * Retorna conjunto de linhas de transporte produto que estejam associadas a um
     * conjunto de linhas de transporte e a um conjunto de materiais
     * @param linhasTransporte
     * @param materiais
     * @return
     */
    @Query("SELECT ltp FROM LinhaTransporteProduto ltp "
            + "LEFT JOIN FETCH ltp.linhaTransporteProdutoCompositeKey.linhaTransporte "
            + "LEFT JOIN FETCH ltp.linhaTransporteProdutoCompositeKey.produto "
            + "WHERE ltp.linhaTransporteProdutoCompositeKey.linhaTransporte IN :linhasTransporte "
            + "AND ltp.linhaTransporteProdutoCompositeKey.produto IN :produtos")
    public List<LinhaTransporteProduto> findByLinhaTransporteProdutoCompositeKeyLinhaTransporteCompositeKeyLocationOrigemInOrLinhaTransporteProdutoCompositeKeyLinhaTransporteCompositeKeyLocationDestinoInWhereProdutoIn(
            @Param("linhasTransporte") Collection<LinhaTransporte> linhasTransporte,
            @Param("produtos") Collection<Produto> materiais);

    /**
     * Retorna conjunto de linhas de transporte que tenham origem ou destino
     * em um conjunto de locations e versão malha em um conjunto de versões
     * @param locations
     * @return
     */
    @Query("SELECT ltp FROM LinhaTransporteProduto ltp "
            + "LEFT JOIN FETCH ltp.linhaTransporteProdutoCompositeKey.linhaTransporte "
            + "LEFT JOIN FETCH ltp.linhaTransporteProdutoCompositeKey.produto "
            + "WHERE ltp.linhaTransporteProdutoCompositeKey.linhaTransporte.linhaTransporteCompositeKey.versaoMalha.id IN :versaoMalhaIds "
            + "AND ltp.linhaTransporteProdutoCompositeKey.produto.id IN :produtoIds "
            + "AND (ltp.linhaTransporteProdutoCompositeKey.linhaTransporte.linhaTransporteCompositeKey.locationOrigem.id IN :locationOrigemIds "
            + "AND ltp.linhaTransporteProdutoCompositeKey.linhaTransporte.linhaTransporteCompositeKey.locationDestino.id IN :locationDestinoIds)")
    public List<LinhaTransporteProduto> findByLinhaTransporteProdutoCompositeKeyVersaoIdInAndLinhaTransporteProdutoCompositeKeyProdutoIdInAndLinhaTransporteProdutoCompositeKeyLocationOrigemIdInAndLinhaTransporteProdutoCompositeKeyLocationDestinoIdIn(
            @Param("versaoMalhaIds") Collection<String> versaoMalhaIds, @Param("produtoIds") Collection<String> produtoIds, @Param("locationOrigemIds") Collection<String> locationOrigemIds, @Param("locationDestinoIds") Collection<String> locationDestinoIds);

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva relacao material-linha de transporte e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    public <S extends LinhaTransporteProduto> S save(S entity);

    /**
     * Salva relacoes material-linha de transporte em lote e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    <S extends LinhaTransporteProduto> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove relacao material-linha de transporte e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    public void delete(LinhaTransporteProduto entity);

    /**
     * Remove relacoes material-linha de transporte em lote e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    void deleteAll(Iterable<? extends LinhaTransporteProduto> entities);

}
