package com.opsfactor.community.capability.cluster.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.Data;

@Data @JsonInclude(JsonInclude.Include.NON_NULL)
// Permite ao Jackson materializar o subtipo recebido pelo front compartilhado.
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "criterio",
        visible = true // se não explícito, não irá buscar o valor de criterio do JSON no retorno ao back-end (criterio = null)
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = RegraAlocaoClusterLocationsCaracteristicaDTO.class, name = "Characteristic"),
        @JsonSubTypes.Type(value = RegraAlocaoClusterLocationsTipoLocationDTO.class, name = "Location Type"),
        @JsonSubTypes.Type(value = RegraAlocaoClusterLocationsPaisEstadoDTO.class, name = "Country / State")
})
public abstract class RegraAlocaoClusterLocationsDTO {
    /**
     * Id da regra
     */
    private Long id;
    
    /**
     * Nivel 1
     * Tipo do criterio.
     *
     * <p>No Community apenas `TIPO_LOCATION` e `PAIS_ESTADO` sao aceitos.
     * `CARACTERISTICA` permanece no contrato JSON para que o service consiga
     * rejeitar payloads Enterprise explicitamente.</p>
     */
    private Constantes.RegraAlocacaoClusterLocationsTipo criterio;
    
}
