package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DTO de data upload Community para unidade de medida.
 *
 * <p>O contrato e propositalmente pequeno: identificador, descricao e flag de
 * exclusao. Conversoes ficam em cargas separadas para manter a leitura dos
 * arquivos simples e previsivel.</p>
 */
@Builder
@Data
@ToString 
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UnidadeMedidaDataUploadDTO {
        
    public String id;
    
    public String description;
    
    public String delete;
    
    /**
     * Numero de colunas publicas esperado na carga Community de unidade de
     * medida: id, descricao e flag de exclusao.
     */
    public static int getNumeroColunas() {
        return 3; 
    }
    
    /**
     * A primeira linha do arquivo representa o cabecalho funcional da carga.
     */
    public static int getNumeroLinhasCabecalho() {
        return 1;
    }
    
}
