package com.opsfactor.community.platform.dto.service;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO generico de arvore hierarquica usado por seletores simples.
 *
 * <p>Ele nao representa agregacao dinamica do Planning Book Community; esse
 * tipo de agrupamento configuravel permanece no Enterprise.</p>
 */
public class EstruturaHierarquicaDTO {
    //<editor-fold defaultstate="collapsed" desc="Codigo em json">
    // indica qual a estrutura hierárquica em que cada produto se insere
    // exemplo:
    //{
    //	"codigo" : 1,
    //	"descricao" : "categoria1",
    //	"elementos" : [
    //		{
    //			"codigo" : 1,
    //			"descricao" : "sub1",
    //			"elementos" : [
    //				{
    //					"codigo" : 34,
    //					"descricao" : "produto34"
    //					
    //				},
    //				{
    //					"codigo" : 55,
    //					"descricao" : "produto55"
    //				}
    //			]
    //		}
    //	]
    //}
    //</editor-fold>
    public String codigo;
    public String descricao;
    public Integer tipoLocation;
    public List<EstruturaHierarquicaDTO> elementos = new ArrayList<>();

    public EstruturaHierarquicaDTO() {
    }
    public EstruturaHierarquicaDTO(String codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }
    public EstruturaHierarquicaDTO(String codigo, String descricao, List<EstruturaHierarquicaDTO> elementos) {
        this.codigo = codigo;
        this.descricao = descricao;
        this.elementos = elementos;
    }
    public EstruturaHierarquicaDTO(String codigo, String descricao,Integer tipoLocation, List<EstruturaHierarquicaDTO> elementos) {
        this.codigo = codigo;
        this.descricao = descricao;
        this.tipoLocation = tipoLocation;
        this.elementos = elementos;
    }
    public void addElemento(EstruturaHierarquicaDTO estruturaHierarquicaDTO){
        elementos.add(estruturaHierarquicaDTO);
    }
    
    
}
