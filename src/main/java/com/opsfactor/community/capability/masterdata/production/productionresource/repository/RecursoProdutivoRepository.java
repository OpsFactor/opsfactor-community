package com.opsfactor.community.capability.masterdata.production.productionresource.repository;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import java.util.Collection;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository JPA de RecursoProdutivoRepository.
 */
@Repository
public interface RecursoProdutivoRepository extends JpaRepository<RecursoProdutivo,String> {
    List<RecursoProdutivo> findAll();

    /**
     * Carrega a fotografia administrativa dos recursos produtivos com a
     * location usada pela listagem Community.
     *
     * <p>O mapper e a validacao da borda consultam a chave da location de cada
     * recurso. O fetch unico evita uma consulta lazy adicional por recurso na
     * listagem administrativa.</p>
     */
    @Query("SELECT DISTINCT rp FROM RecursoProdutivo rp "
            + "LEFT JOIN FETCH rp.location")
    List<RecursoProdutivo> customFindAllWithLocation();

    /**
     * Remove recursos produtivos associados as locations informadas.
     */
    void removeByLocationIn(Collection<Location> locations);

    @Query("SELECT DISTINCT rp FROM RecursoProdutivo rp "
            + "LEFT JOIN FETCH rp.disponibilidadesRecursoProdutivo cl "
            + "WHERE rp.location IN :locations")
    public List<RecursoProdutivo> customFindByLocationIn(@Param("locations") Collection<Location> locations);

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva recurso produtivo e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    public <S extends RecursoProdutivo> S save(S entity);

    /**
     * Salva recursos produtivos em lote e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    <S extends RecursoProdutivo> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove recurso produtivo e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    public void delete(RecursoProdutivo entity);

    /**
     * Remove recursos produtivos em lote e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    void deleteAll(Iterable<? extends RecursoProdutivo> entities);

}
