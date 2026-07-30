package com.opsfactor.community.capability.masterdata.production.billofmaterials.repository;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnicaComponente;
import java.util.Collection;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Repository JPA de ListaTecnicaComponenteRepository.
 */
@Repository
public interface ListaTecnicaComponenteRepository extends JpaRepository<ListaTecnicaComponente,ListaTecnicaComponente.ListaTecnicaComponenteCompositeKey> {
    List<ListaTecnicaComponente> findAll();

    /**
     * Carrega o snapshot administrativo dos componentes de BOM com todas as
     * associações consumidas pelo DTO Community.
     *
     * <p>A listagem acessa a BOM, o material componente e a unidade cadastrada
     * de cada linha. Os fetch joins evitam que esse mapeamento transforme uma
     * única listagem em consultas adicionais por componente.</p>
     */
    @Query("SELECT ltc FROM ListaTecnicaComponente ltc "
            + "LEFT JOIN FETCH ltc.listaTecnicaComponenteCompositeKey.listaTecnica lt "
            + "LEFT JOIN FETCH ltc.listaTecnicaComponenteCompositeKey.materialComponente mc "
            + "LEFT JOIN FETCH ltc.unidadeMedidaMaterialComponente")
    List<ListaTecnicaComponente> customFindAll();

    List<ListaTecnicaComponente> findAllByListaTecnicaComponenteCompositeKeyListaTecnicaLocationInAndListaTecnicaComponenteCompositeKeyListaTecnicaMaterialOutputIn(
            Collection<Location> locations, Collection<Produto> produtos);

    List<ListaTecnicaComponente> findAllByListaTecnicaComponenteCompositeKeyListaTecnicaIdIn(Collection<String> idsListasTecnicas);

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva componente de lista tecnica e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    public <S extends ListaTecnicaComponente> S save(S entity);

    /**
     * Salva componentes de lista tecnica em lote e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    <S extends ListaTecnicaComponente> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove componente de lista tecnica e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    public void delete(ListaTecnicaComponente entity);

    /**
     * Remove componentes de lista tecnica em lote e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    void deleteAll(Iterable<? extends ListaTecnicaComponente> entities);

}
