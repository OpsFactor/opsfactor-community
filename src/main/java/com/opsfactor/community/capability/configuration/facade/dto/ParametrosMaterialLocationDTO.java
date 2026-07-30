package com.opsfactor.community.capability.configuration.facade.dto;

import com.opsfactor.community.platform.utility.Constantes;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO publico dos parametros operacionais material/location.
 *
 * <p>O contrato Community usa `materialID` e `material`, alinhando a borda
 * publica ao vocabulario da edicao aberta.</p>
 */
@Data
public class ParametrosMaterialLocationDTO {
    private String locationID;
    private String location;
    private String materialID;
    private String material;
    /**
     * Quantidade mínima de produção explicitamente configurada para a
     * combinação material/location.
     *
     * <p>{@code null} remove a restrição local e mantém a ausência
     * operacional. Zero é um mínimo válido.</p>
     */
    private Double productionMinimumQuantity;

    /**
     * Múltiplo de produção explicitamente configurado para a combinação
     * material/location.
     *
     * <p>{@code null} remove a restrição local. Quando preenchido, precisa
     * ser estritamente positivo porque os consumidores de Supply Planning o
     * usam como divisor de arredondamento.</p>
     */
    private Double productionMultipleQuantity;
    private Boolean foraLinha; // se verdadeiro, item não deve mais ser comprado na location. fora_linha do material sempre vale mais que material-location
    private Boolean novo;
    private Boolean inativo;

    /**
     * Estagio explicito do ciclo de vida da combinacao material/location.
     *
     * <p>Usa o mesmo enum publicado pelo Data Upload de materiais. Quando
     * preenchido, ele tem precedencia sobre as datas na resolucao de status da
     * entidade, mas as datas continuam parte do payload e nao sao apagadas.
     * Isso permite remover o override posteriormente e voltar ao ciclo
     * derivado das datas ja cadastradas.</p>
     */
    private Constantes.StatusProduto lifecycleStage;

    /**
     * Inicio opcional do ciclo de vida local da combinacao material/location.
     * {@code null} limpa a data persistida e devolve a decisao para o material
     * ou para os demais dados locais existentes.
     */
    private LocalDateTime introductionDate;

    /**
     * Fim opcional do ciclo de vida local da combinacao material/location.
     * {@code null} limpa a data persistida, sem criar valor de fallback no
     * contrato administrativo.
     */
    private LocalDateTime discontinuationDate;

    /**
     * Override em dias do horizonte congelado de Demand Planning desta
     * combinacao material/location.
     *
     * <p>O valor {@code null} deve ser preservado no contrato publico: ele
     * representa ausencia de override local e permite que a camada de
     * execucao herde o parametro global. Zero e um override explicito, que
     * desabilita o congelamento apenas para esta combinacao.</p>
     */
    private Integer frozenHorizonDpInDays;

    /**
     * Identificador da unidade de medida explicitamente cadastrada para esta
     * combinacao material/location.
     *
     * <p>O valor {@code null} nao representa uma unidade efetiva: ele limpa o
     * override local e permite que o calculo siga a hierarquia ja definida na
     * projection. A API administrativa expoe somente o identificador
     * configurado para que o cliente consiga preservar essa distincao.</p>
     */
    private String defaultUomId;

    /**
     * Identificador da unidade explicitamente cadastrada para lote minimo e
     * multiplo de producao desta combinacao material/location.
     *
     * <p>{@code null} remove o override local. O valor efetivo continua sendo
     * resolvido exclusivamente pela projection de parametros durante o
     * calculo, preservando a cascata de unidades ja existente.</p>
     */
    private String productionMinimumMultipleUomId;

}
