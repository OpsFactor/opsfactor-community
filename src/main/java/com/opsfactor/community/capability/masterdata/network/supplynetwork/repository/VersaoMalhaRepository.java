package com.opsfactor.community.capability.masterdata.network.supplynetwork.repository;

import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository JPA de VersaoMalhaRepository.
 */
@Repository
public interface VersaoMalhaRepository extends JpaRepository<VersaoMalha,String> {

    @Query("SELECT vm FROM VersaoMalha vm "
            + "LEFT JOIN FETCH vm.locationOrigemPadraoClientes lopc "
            + "LEFT JOIN FETCH vm.locationOrigemPadraoMateriasPrimas lopmp")
    List<VersaoMalha> customFindAll();

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva versao de malha e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    public <S extends VersaoMalha> S save(S entity);

    /**
     * Salva versoes de malha em lote e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    <S extends VersaoMalha> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove versao de malha e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    public void delete(VersaoMalha entity);

    /**
     * Remove versoes de malha em lote e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    void deleteAll(Iterable<? extends VersaoMalha> entities);

}
