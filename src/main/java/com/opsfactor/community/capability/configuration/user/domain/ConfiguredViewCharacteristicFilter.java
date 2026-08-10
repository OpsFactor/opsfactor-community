package com.opsfactor.community.capability.configuration.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Valor de filtro de uma característica pública de material ou location.
 *
 * <p>O par {@code characteristicId × filteredValue} é deliberadamente menor
 * que a configuração Enterprise de apresentação e agrupamento. A edição
 * Community persiste apenas a restrição de escopo e nunca posição de coluna ou
 * hierarquia.</p>
 */
@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguredViewCharacteristicFilter implements Serializable {

    /** Identidade estável da característica publicada pelo Community. */
    @Column(name = "characteristic_id", length = 50, nullable = false)
    private String characteristicId;

    /** Um dos valores aceitos para a característica; vários valores formam OR. */
    @Column(name = "filtered_value", length = 100, nullable = false)
    private String filteredValue;

}
