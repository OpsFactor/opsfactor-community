package com.opsfactor.community.platform.projection.inmemorybi;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Classe que permite indexação de datas/horários pelo CQEngine.
 *
 * <p>A consulta de igualdade do BI depende de {@code equals/hashCode}; apenas
 * implementar {@link Comparable} permite ordenação, mas não identifica outra
 * instância que represente a mesma data.</p>
 */
public class LocalDateTimeComparable implements Comparable<LocalDateTimeComparable> {
        
    public LocalDateTime localDateTime;
    
    public LocalDateTimeComparable(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    @Override
    public int compareTo(LocalDateTimeComparable o) {
        return localDateTime.compareTo(o.localDateTime);
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }
        if (!(object instanceof LocalDateTimeComparable that)) {
            return false;
        }
        return Objects.equals(localDateTime, that.localDateTime);

    }

    @Override
    public int hashCode() {

        return Objects.hash(localDateTime);

    }

}
