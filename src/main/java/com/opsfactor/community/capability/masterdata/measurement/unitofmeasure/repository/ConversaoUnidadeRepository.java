package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.ConversaoUnidade;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


/**
 * Repository JPA de ConversaoUnidadeRepository.
 */
@Repository
public interface ConversaoUnidadeRepository extends JpaRepository<ConversaoUnidade,ConversaoUnidade.ConversaoUnidadeCompositeKey> {

    List<ConversaoUnidade> findAll();
    @Query("SELECT cun FROM ConversaoUnidade cun "
            + "LEFT JOIN FETCH cun.conversaoUnidadeCompositeKey.unidadeMedidaOrigem umo "
            + "LEFT JOIN FETCH cun.conversaoUnidadeCompositeKey.unidadeMedidaDestino umd")
    List<ConversaoUnidade> customFindAllJoinUnidades();

    /**
     * Carrega e bloqueia somente as conversoes globais que ainda possuem o
     * fator legado de UOM durante seu cutover fisico.
     *
     * <p>Esta query nao participa das leituras funcionais normais. O lock
     * pessimista serializa exclusivamente a janela administrativa, evitando
     * que uma edicao concorrente invalide a conferencia feita pelo executor
     * antes de ele limpar a coluna depreciada.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cun FROM ConversaoUnidade cun "
            + "LEFT JOIN FETCH cun.conversaoUnidadeCompositeKey.unidadeMedidaOrigem umo "
            + "LEFT JOIN FETCH cun.conversaoUnidadeCompositeKey.unidadeMedidaDestino umd "
            + "WHERE cun.quantidadeUnidadeDestinoPorUnidadeOrigem IS NOT NULL")
    List<ConversaoUnidade> findAllJoinUnidadesForLegacyRatioCutover();


    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva conversao global de unidade e invalida snapshots de unidade e malha.
     */
    @Override
    @CacheEvict(value = {"unidadeMedidaProjection", "supplyNetworkProjection"}, allEntries = true)
    public <S extends ConversaoUnidade> S save(S entity);

    /**
     * Salva conversoes globais de unidade em lote e invalida snapshots de unidade e malha.
     */
    @Override
    @CacheEvict(value = {"unidadeMedidaProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends ConversaoUnidade> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove conversao global de unidade e invalida snapshots de unidade e malha.
     */
    @Override
    @CacheEvict(value = {"unidadeMedidaProjection", "supplyNetworkProjection"}, allEntries = true)
    public void delete(ConversaoUnidade entity);

    /**
     * Remove conversoes globais de unidade em lote e invalida snapshots de unidade e malha.
     */
    @Override
    @CacheEvict(value = {"unidadeMedidaProjection", "supplyNetworkProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends ConversaoUnidade> entities);

}
