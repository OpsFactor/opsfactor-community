package com.opsfactor.community.capability.masterdata.production.productionversion.repository;

import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducaoInexistente;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository JPA de VersaoProducaoInexistenteRepository.
 */
@Repository
public interface VersaoProducaoInexistenteRepository extends JpaRepository<VersaoProducaoInexistente,String> {

    public VersaoProducaoInexistente findFirstByOrderById();

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva versao de producao inexistente e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    public <S extends VersaoProducaoInexistente> S save(S entity);

    /**
     * Salva versoes de producao inexistentes em lote e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    <S extends VersaoProducaoInexistente> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove versao de producao inexistente e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    public void delete(VersaoProducaoInexistente entity);

    /**
     * Remove versoes de producao inexistentes em lote e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    void deleteAll(Iterable<? extends VersaoProducaoInexistente> entities);

}
