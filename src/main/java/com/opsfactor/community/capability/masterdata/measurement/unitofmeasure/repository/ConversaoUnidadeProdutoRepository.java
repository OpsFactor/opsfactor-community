package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.ConversaoUnidadeProduto;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


/**
 * Repository JPA de ConversaoUnidadeProdutoRepository.
 */
@Repository
public interface ConversaoUnidadeProdutoRepository extends JpaRepository<ConversaoUnidadeProduto,ConversaoUnidadeProduto.ConversaoUnidadeProdutoCompositeKey> {

    List<ConversaoUnidadeProduto> findAll();

    @Query("SELECT cup FROM ConversaoUnidadeProduto cup "
            + "LEFT JOIN FETCH cup.conversaoUnidadeProdutoCompositeKey.produto p "
            + "LEFT JOIN FETCH cup.conversaoUnidadeProdutoCompositeKey.unidadeMedidaOrigem umo "
            + "LEFT JOIN FETCH cup.conversaoUnidadeProdutoCompositeKey.unidadeMedidaDestino umd")
    List<ConversaoUnidadeProduto> customFindAllJoinProdutoEUnidades();

    /**
     * Carrega e bloqueia somente as conversoes por material que ainda possuem
     * razao direta legada na janela administrativa de retirada desse campo.
     *
     * <p>O executor confere novamente o estado de cada linha depois do lock;
     * portanto o metodo nao deve ser reutilizado por fluxos de leitura ou
     * upload usuais.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cup FROM ConversaoUnidadeProduto cup "
            + "LEFT JOIN FETCH cup.conversaoUnidadeProdutoCompositeKey.produto p "
            + "LEFT JOIN FETCH cup.conversaoUnidadeProdutoCompositeKey.unidadeMedidaOrigem umo "
            + "LEFT JOIN FETCH cup.conversaoUnidadeProdutoCompositeKey.unidadeMedidaDestino umd "
            + "WHERE cup.quantidadeUnidadeDestinoPorUnidadeOrigem IS NOT NULL")
    List<ConversaoUnidadeProduto> findAllJoinProdutoEUnidadesForLegacyRatioCutover();


    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva conversao de unidade por produto e invalida snapshots de unidade e malha.
     */
    @Override
    @CacheEvict(value = {"unidadeMedidaProjection", "supplyNetworkProjection"}, allEntries = true)
    public <S extends ConversaoUnidadeProduto> S save(S entity);

    /**
     * Salva conversoes de unidade por produto em lote e invalida snapshots de unidade e malha.
     */
    @Override
    @CacheEvict(value = {"unidadeMedidaProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends ConversaoUnidadeProduto> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove conversao de unidade por produto e invalida snapshots de unidade e malha.
     */
    @Override
    @CacheEvict(value = {"unidadeMedidaProjection", "supplyNetworkProjection"}, allEntries = true)
    public void delete(ConversaoUnidadeProduto entity);

    /**
     * Remove conversoes de unidade por produto em lote e invalida snapshots de unidade e malha.
     */
    @Override
    @CacheEvict(value = {"unidadeMedidaProjection", "supplyNetworkProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends ConversaoUnidadeProduto> entities);

}
