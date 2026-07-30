package com.opsfactor.community.capability.masterdata.organization.economicgroup.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * Identificador compartilhado de um grupo econômico.
 *
 * <p>O Community mantém somente o cabeçalho necessário para que outras
 * entidades de master data possam referenciá-lo. Regras fiscais, consolidação
 * e manutenção por Data Upload permanecem em capabilities Enterprise.</p>
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = "id")
@EqualsAndHashCode(of = "id")
public class EconomicGroup implements Serializable, Comparable<EconomicGroup> {

    @Id
    @Column(length = 100)
    private String id;

    private String description;

    @Override
    public int compareTo(EconomicGroup otherEconomicGroup) {

        return id.compareTo(otherEconomicGroup.id);

    }
}
