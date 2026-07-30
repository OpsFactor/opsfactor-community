package com.opsfactor.community.capability.masterdata.production.productionversion.repository;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducaoSimples;
import java.util.Collection;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository JPA de VersaoProducaoSimplesRepository.
 */
@Repository
public interface VersaoProducaoSimplesRepository extends JpaRepository<VersaoProducaoSimples,String> {

    @Query("SELECT DISTINCT vp FROM VersaoProducaoSimples vp "
            + "LEFT JOIN FETCH vp.roteiro rt "
            + "LEFT JOIN FETCH rt.operacaoRoteiroSet ors "
            + "LEFT JOIN FETCH vp.listaTecnica lt "
            + "LEFT JOIN FETCH lt.listaTecnicaComponenteSet ltcs "
            + "WHERE vp.location IN :locations "
            + "AND vp.materialOutput IN :produtos")
    public List<VersaoProducaoSimples> findByVersaoProducaoCompositeKeyRoteiroLocationInAndVersaoProducaoCompositeKeyRoteiroMaterialOutputIn(
            @Param("locations") Collection<Location> locations, @Param("produtos") Collection<Produto> produtos);

    /**
     * Carrega todas as versoes simples com as referencias usadas pelo mapper
     * de exportacao de integracao.
     *
     * <p>O export percorre location, material output, roteiro e BOM de cada
     * versao. Os fetch joins mantem essa leitura em uma unica consulta e
     * evitam carregamentos lazy por linha exportada.</p>
     */
    @Query("SELECT DISTINCT vp FROM VersaoProducaoSimples vp "
            + "LEFT JOIN FETCH vp.location "
            + "LEFT JOIN FETCH vp.materialOutput "
            + "LEFT JOIN FETCH vp.roteiro "
            + "LEFT JOIN FETCH vp.listaTecnica")
    List<VersaoProducaoSimples> customFindAllForIntegrationExport();

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva versao de producao simples e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    public <S extends VersaoProducaoSimples> S save(S entity);

    /**
     * Salva versoes de producao simples em lote e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    <S extends VersaoProducaoSimples> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove versao de producao simples e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    public void delete(VersaoProducaoSimples entity);

    /**
     * Remove versoes de producao simples em lote e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    void deleteAll(Iterable<? extends VersaoProducaoSimples> entities);

}
