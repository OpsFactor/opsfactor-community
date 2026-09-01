package com.opsfactor.community.capability.masterdata.production.productionversion.repository;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/** Repository da entidade única de versão de produção. */
@Repository
public interface VersaoProducaoRepository extends JpaRepository<VersaoProducao, String> {

    @Query("SELECT DISTINCT vp FROM VersaoProducao vp "
            + "JOIN FETCH vp.location "
            + "JOIN FETCH vp.roteiro rt "
            + "JOIN FETCH rt.materialOutput "
            + "JOIN FETCH vp.listaTecnica lt "
            + "JOIN FETCH lt.materialOutput "
            + "WHERE vp.location IN :locations AND rt.materialOutput IN :produtos")
    List<VersaoProducao> customFindAllByLocationInAndMaterialOutputIn(
            @Param("locations") Collection<Location> locations,
            @Param("produtos") Collection<Produto> produtos);

    /**
     * Carrega versões pelo escopo de location sem supor output singular. A
     * factory reassocia os mestres às instâncias canônicas já fetchadas.
     */
    @Query("SELECT DISTINCT vp FROM VersaoProducao vp "
            + "JOIN FETCH vp.location "
            + "JOIN FETCH vp.roteiro "
            + "JOIN FETCH vp.listaTecnica "
            + "WHERE vp.location IN :locations")
    List<VersaoProducao> customFindAllByLocationIn(@Param("locations") Collection<Location> locations);

    @Query("SELECT DISTINCT vp FROM VersaoProducao vp "
            + "JOIN FETCH vp.location "
            + "JOIN FETCH vp.roteiro rt "
            + "JOIN FETCH rt.materialOutput "
            + "JOIN FETCH vp.listaTecnica lt "
            + "JOIN FETCH lt.materialOutput")
    List<VersaoProducao> customFindAllForIntegrationExport();

    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    <S extends VersaoProducao> S save(S entity);

    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    <S extends VersaoProducao> List<S> saveAll(Iterable<S> entities);

    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    void delete(VersaoProducao entity);

    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    void deleteAll(Iterable<? extends VersaoProducao> entities);
}
