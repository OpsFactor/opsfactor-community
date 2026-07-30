package com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;


import java.util.ArrayList;
import java.util.List;

/**
 * DTO historico de caracteristica de material/produto.
 *
 * <p>No Community, este DTO nao representa um cadastro funcional de
 * caracteristicas dinamicas. Ele permanece porque a tela compartilhada de
 * clusters reaproveita o mesmo shape para enviar o status do material
 * selecionado, e porque payloads Enterprise precisam ser rejeitados de forma
 * explicita nos services consumidores.</p>
 */
@Data @JsonInclude(JsonInclude.Include.NON_NULL)
public class CaracteristicaProdutoDTO {
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
