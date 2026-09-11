package com.opsfactor.community.capability.masterdata.calendar.profile.repository;

import com.opsfactor.community.capability.masterdata.calendar.profile.domain.PerfilCalendario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

/** Catálogo das raízes; subtipos privados entram somente no runtime Enterprise. */
public interface PerfilCalendarioRepository extends JpaRepository<PerfilCalendario, String> {

    /** Serializa edição/reorder da mesma raiz; a PK protege criações concorrentes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PerfilCalendario p where p.id = :id")
    Optional<PerfilCalendario> customFindByIdForUpdate(@Param("id") String id);

}
