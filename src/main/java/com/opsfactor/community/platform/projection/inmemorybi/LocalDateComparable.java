package com.opsfactor.community.platform.projection.inmemorybi;

import java.time.LocalDate;

/**
 * Classe que permite indexação de datas pelo CQEngine
 */
public class LocalDateComparable implements Comparable<LocalDateComparable> {
        
    public LocalDate localDate;
    
    public LocalDateComparable(LocalDate localDate) {
        this.localDate = localDate;
    }

    @Override
    public int compareTo(LocalDateComparable o) {
        return localDate.compareTo(o.localDate);
    }
    
}
