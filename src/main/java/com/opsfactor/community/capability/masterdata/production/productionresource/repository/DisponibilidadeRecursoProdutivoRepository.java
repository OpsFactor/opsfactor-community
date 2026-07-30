package com.opsfactor.community.capability.masterdata.production.productionresource.repository;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.DisponibilidadeRecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.DisponibilidadeRecursoProdutivo.DisponibilidadeRecursoProdutivoCompositeKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Repository JPA de DisponibilidadeRecursoProdutivoRepository.
 */
@Repository
public interface DisponibilidadeRecursoProdutivoRepository extends JpaRepository<DisponibilidadeRecursoProdutivo,DisponibilidadeRecursoProdutivoCompositeKey> {
    List<DisponibilidadeRecursoProdutivo> findAll();
    
    List<DisponibilidadeRecursoProdutivo> findAllByDisponibilidadeRecursoProdutivoCompositeKeyRecursoProdutivoLocationAndDisponibilidadeRecursoProdutivoCompositeKeyDataReferenciaBetween(
            Location location, LocalDate dataInicial, LocalDate dataFinal);

    @Query("SELECT drp FROM DisponibilidadeRecursoProdutivo drp "
            + "LEFT JOIN FETCH drp.disponibilidadeRecursoProdutivoCompositeKey.recursoProdutivo rp "
            + "LEFT JOIN FETCH rp.location loc "
            + "LEFT JOIN FETCH rp.unidadeMedidaCapacidadeEmUom uom "
            + "WHERE loc IN :locations "
            + "AND drp.disponibilidadeRecursoProdutivoCompositeKey.dataReferencia BETWEEN :dataInicial AND :dataFinal")
    List<DisponibilidadeRecursoProdutivo> customFindAllByDisponibilidadeRecursoProdutivoCompositeKeyRecursoProdutivoLocationInAndDisponibilidadeRecursoProdutivoCompositeKeyDataReferenciaBetween(
            Collection<Location> locations, LocalDate dataInicial, LocalDate dataFinal);

    List<DisponibilidadeRecursoProdutivo> findAllByDisponibilidadeRecursoProdutivoCompositeKeyRecursoProdutivoIdInAndDisponibilidadeRecursoProdutivoCompositeKeyDataReferenciaBetween(
            Set<String> idsRecursosProdutivos, LocalDate dataInicial, LocalDate dataFinal);
    
    @Query("SELECT drp FROM DisponibilidadeRecursoProdutivo drp "
            + "LEFT JOIN FETCH drp.disponibilidadeRecursoProdutivoCompositeKey.recursoProdutivo rp "
            + "LEFT JOIN FETCH rp.unidadeMedidaCapacidadeEmUom uom "
            + "WHERE drp.disponibilidadeRecursoProdutivoCompositeKey.dataReferencia BETWEEN :dataInicial AND :dataFinal")
    List<DisponibilidadeRecursoProdutivo> customFindAllWhereDataReferenciaBetween(LocalDate dataInicial, LocalDate dataFinal);

}
