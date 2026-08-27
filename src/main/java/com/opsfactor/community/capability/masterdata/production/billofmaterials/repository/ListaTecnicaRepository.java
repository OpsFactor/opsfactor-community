package com.opsfactor.community.capability.masterdata.production.billofmaterials.repository;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import java.util.Collection;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository JPA de ListaTecnicaRepository.
 */
@Repository
public interface ListaTecnicaRepository extends JpaRepository<ListaTecnica,String> {

    /**
     * Carrega a fotografia administrativa completa das listas técnicas.
     *
     * <p>A listagem da SPA consulta location, material de saída e unidade
     * cadastrada/elegível para cada BOM. O fetch único evita consultas lazy
     * adicionais por linha durante a validação e o mapeamento do DTO.</p>
     */
    @Query("SELECT DISTINCT lt FROM ListaTecnica lt "
            + "LEFT JOIN FETCH lt.location "
            + "LEFT JOIN FETCH lt.materialOutput "
            + "LEFT JOIN FETCH lt.unidadeMedidaMaterialOutput")
    List<ListaTecnica> customFindAllWithLocationMaterialOutputAndUnidadeMedidaMaterialOutput();

    @Query("SELECT DISTINCT lt FROM ListaTecnica lt "
            + "LEFT JOIN FETCH lt.location "
            + "LEFT JOIN FETCH lt.materialOutput "
            + "LEFT JOIN FETCH lt.unidadeMedidaMaterialOutput "
            + "LEFT JOIN FETCH lt.listaTecnicaComponenteSet ltc "
            + "LEFT JOIN FETCH ltc.materialComponente "
            + "LEFT JOIN FETCH ltc.unidadeMedidaMaterialComponente "
            + "WHERE lt.location IN :locations "
            + "AND lt.materialOutput IN :materiais")
    public List<ListaTecnica> customFindAllByLocationInAndMaterialOutputInFetchListaTecnicaComponente(
            @Param("locations") Collection<Location> locations,
            @Param("materiais") Collection<Produto> materiais);

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva lista tecnica e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    public <S extends ListaTecnica> S save(S entity);

    /**
     * Salva listas tecnicas em lote e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    <S extends ListaTecnica> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove lista tecnica e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    public void delete(ListaTecnica entity);

    /**
     * Remove listas tecnicas em lote e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    void deleteAll(Iterable<? extends ListaTecnica> entities);

}
