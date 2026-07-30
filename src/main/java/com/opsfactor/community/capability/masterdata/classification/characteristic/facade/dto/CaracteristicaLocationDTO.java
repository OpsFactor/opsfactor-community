package com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;


import java.util.ArrayList;
import java.util.List;

/**
 * DTO historico de caracteristica de location.
 *
 * <p>Caracteristicas dinamicas de location pertencem ao Enterprise. O DTO fica
 * no Community apenas para compatibilidade com payloads do front compartilhado
 * e para permitir que services bloqueiem a tentativa de uso com erro de versao
 * em vez de ignorar silenciosamente a selecao.</p>
 */
@Data @JsonInclude(JsonInclude.Include.NON_NULL)
public class CaracteristicaLocationDTO {
    public String caracteristicaId;
    public String descricao;
    public TipoCaracteristicaDTO tipoCaracteristica;
    // Campo Enterprise/compatibilidade: no Community nao ha catalogo dinamico de atributos.
    public List<String> listaAtributos = new ArrayList<>();
    public String atributo;

    public void addAtributo(String atributo){
        if (!listaAtributos.contains(atributo)){
            listaAtributos.add(atributo);
        }
    }
}
