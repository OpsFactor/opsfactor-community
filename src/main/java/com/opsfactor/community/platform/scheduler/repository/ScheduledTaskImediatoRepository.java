package com.opsfactor.community.platform.scheduler.repository;

import com.opsfactor.community.platform.scheduler.domain.ScheduledTaskImediato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository do unico tipo concreto de task criado pelo Community.
 */
@Repository
public interface ScheduledTaskImediatoRepository extends JpaRepository<ScheduledTaskImediato,String> {
    
    @Query("SELECT DISTINCT st FROM ScheduledTaskImediato st "
            + "LEFT JOIN FETCH st.scheduledTaskExecutionSet stes "
            + "WHERE st.id = :scheduledTaskId")
    public Optional<ScheduledTaskImediato> customFindById(String scheduledTaskId);
    
}
