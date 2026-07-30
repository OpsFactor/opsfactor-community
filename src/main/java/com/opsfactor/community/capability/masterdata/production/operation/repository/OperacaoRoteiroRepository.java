package com.opsfactor.community.capability.masterdata.production.operation.repository;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.operation.domain.OperacaoRoteiro;
import java.util.Collection;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Repository JPA de OperacaoRoteiroRepository.
 */
@Repository
public interface OperacaoRoteiroRepository extends JpaRepository<OperacaoRoteiro,OperacaoRoteiro.OperacaoRoteiroCompositeKey> {

    @Query("SELECT opr FROM OperacaoRoteiro opr "
            + "LEFT JOIN FETCH opr.operacaoRoteiroCompositeKey.roteiro r")
    List<OperacaoRoteiro> customFindAll();

    /**
     * Carrega a fotografia completa das operacoes usada exclusivamente pela
     * listagem administrativa do Community.
     *
     * <p>O mapper do front acessa o roteiro embutido na chave composta, o
     * recurso produtivo e a unidade de medida cadastrada. Todos precisam vir
     * na mesma consulta para que a lista nao execute uma consulta lazy por
     * operacao.</p>
     *
     * @return operacoes com os atributos {@code many-to-one} do contrato de
     * front ja inicializados.
     */
    @Query("SELECT opr FROM OperacaoRoteiro opr "
            + "JOIN FETCH opr.operacaoRoteiroCompositeKey.roteiro r "
            + "JOIN FETCH opr.recursoProdutivo rp "
            + "LEFT JOIN FETCH opr.unidadeMedida um")
    List<OperacaoRoteiro> customFindAllForFront();

    List<OperacaoRoteiro> findAllByOperacaoRoteiroCompositeKeyRoteiroLocationInAndOperacaoRoteiroCompositeKeyRoteiroMaterialOutputIn(
            Collection<Location> locations, Collection<Produto> produtos);

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva operacao de roteiro e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    public <S extends OperacaoRoteiro> S save(S entity);

    /**
     * Salva operacoes de roteiro em lote e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    <S extends OperacaoRoteiro> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove operacao de roteiro e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    public void delete(OperacaoRoteiro entity);

    /**
     * Remove operacoes de roteiro em lote e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    void deleteAll(Iterable<? extends OperacaoRoteiro> entities);

}
