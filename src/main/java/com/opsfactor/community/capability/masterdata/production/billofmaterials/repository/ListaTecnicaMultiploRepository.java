package com.opsfactor.community.capability.masterdata.production.billofmaterials.repository;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnicaMultiplo;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Fetch em lote dos outputs e unidades das listas técnicas múltiplas. */
@Repository
public interface ListaTecnicaMultiploRepository extends JpaRepository<ListaTecnicaMultiplo, String> {

    @Query("SELECT DISTINCT lt FROM ListaTecnicaMultiplo lt "
            + "LEFT JOIN FETCH lt.location "
            + "LEFT JOIN FETCH lt.listaTecnicaMultiploOutputSet output "
            + "LEFT JOIN FETCH output.listaTecnicaMultiploOutputCompositeKey.materialOutput "
            + "LEFT JOIN FETCH output.unidadeMedida "
            + "WHERE lt.location IN :locations")
    List<ListaTecnicaMultiplo> customFindAllByLocationInFetchOutputs(
            @Param("locations") Collection<Location> locations);
}
