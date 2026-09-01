package com.opsfactor.community.capability.masterdata.production.routing.repository;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.routing.domain.RoteiroMultiplo;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Fetch em lote dos outputs dos roteiros múltiplos, sem N+1 na projection. */
@Repository
public interface RoteiroMultiploRepository extends JpaRepository<RoteiroMultiplo, String> {

    @Query("SELECT DISTINCT r FROM RoteiroMultiplo r "
            + "LEFT JOIN FETCH r.roteiroMultiploMaterialSet rm "
            + "LEFT JOIN FETCH rm.roteiroMultiploMaterialCompositeKey.material "
            + "WHERE r.location IN :locations")
    List<RoteiroMultiplo> customFindAllByLocationInFetchMateriaisOutput(
            @Param("locations") Collection<Location> locations);
}
