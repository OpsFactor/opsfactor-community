package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository Community de unidades de medida e conversoes globais.
 *
 * <p>Escritas neste cadastro invalidam projections de unidade e malha que
 * dependem de escala/conversao para calculos de Supply Planning.</p>
 */
@Repository
public interface UnidadeMedidaRepository extends JpaRepository<UnidadeMedida,String> {

    List<UnidadeMedida> findAll();

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva unidade de medida e invalida snapshots de unidade e malha.
     */
    @Override
    @CacheEvict(value = {"unidadeMedidaProjection", "supplyNetworkProjection"}, allEntries = true)
    public <S extends UnidadeMedida> S save(S entity);

    /**
     * Salva unidades de medida em lote e invalida snapshots de unidade e malha.
     */
    @Override
    @CacheEvict(value = {"unidadeMedidaProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends UnidadeMedida> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove unidade de medida e invalida snapshots de unidade e malha.
     */
    @Override
    @CacheEvict(value = {"unidadeMedidaProjection", "supplyNetworkProjection"}, allEntries = true)
    public void delete(UnidadeMedida entity);

    /**
     * Remove unidades de medida em lote e invalida snapshots de unidade e malha.
     */
    @Override
    @CacheEvict(value = {"unidadeMedidaProjection", "supplyNetworkProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends UnidadeMedida> entities);

}
