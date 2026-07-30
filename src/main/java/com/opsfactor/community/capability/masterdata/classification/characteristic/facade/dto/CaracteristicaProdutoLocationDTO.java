package com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Contrato compartilhado para uma caracteristica dinamica material-location.
 *
 * <p>O catalogo e a leitura desta caracteristica pertencem ao Enterprise. O
 * DTO permanece no Community somente para que o front compartilhado conheca o
 * formato do payload sem expor entidade, repository ou endpoint privados.</p>
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CaracteristicaProdutoLocationDTO {

    public String caracteristicaId;
    public String descricao;
    public TipoCaracteristicaDTO tipoCaracteristica;
    public List<String> listaAtributos = new ArrayList<>();
    public String atributo;

    /**
     * Inclui atributo sem repetir uma opcao ja informada pelo catalogo.
     */
    public void addAtributo(String atributo) {

        if (!listaAtributos.contains(atributo)) {
            listaAtributos.add(atributo);
        }

    }

}
