package com.opsfactor.community.platform.scheduler.repository;

import com.opsfactor.community.platform.scheduler.domain.ScheduledTaskExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository dos registros de execucao de cada task imediata Community.
 */
@Repository
public interface ScheduledTaskExecutionRepository extends JpaRepository<ScheduledTaskExecution,ScheduledTaskExecution.ScheduledTaskExecutionCompositeKey> {
    
    
}
