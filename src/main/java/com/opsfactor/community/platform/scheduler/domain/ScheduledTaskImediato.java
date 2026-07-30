package com.opsfactor.community.platform.scheduler.domain;

import jakarta.persistence.*;

import lombok.*;

/**
 * Task imediata executada sincronamente pelo Community.
 */
@Getter 
@Setter
@DiscriminatorValue("imediato")
@NoArgsConstructor
@Entity
public class ScheduledTaskImediato extends ScheduledTaskAbstract {
    
    public ScheduledTaskImediato(String id) {
        super(id);
    }
    
}
