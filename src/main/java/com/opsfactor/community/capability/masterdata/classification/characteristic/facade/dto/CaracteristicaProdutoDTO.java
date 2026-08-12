package com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;


import java.util.ArrayList;
import java.util.List;

/**
 * DTO público do catálogo de características de material/produto.
 *
 * <p>O Community publica id, descrição, tipo e atributos para seletores de
 * escopo e integrações de dados mestres.</p>
 */
@Data @JsonInclude(JsonInclude.Include.NON_NULL)
public class CaracteristicaProdutoDTO {
    public String caracteristicaId;
    public String descricao;
    public TipoCaracteristicaDTO tipoCaracteristica;
    /** Valores disponíveis para seleção no catálogo Community. */
    public List<String> listaAtributos = new ArrayList<>();
    public String atributo;

    public void addAtributo(String atributo){
        if (!listaAtributos.contains(atributo)){
            listaAtributos.add(atributo);
        }
    }
}
