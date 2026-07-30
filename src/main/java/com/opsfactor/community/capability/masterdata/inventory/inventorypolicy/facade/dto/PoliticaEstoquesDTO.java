package com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.facade.dto;

import com.opsfactor.community.platform.utility.Constantes;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO da API Community de politicas operacionais de safety stock.
 *
 * <p>Este contrato permite configurar regras simples por material/location para
 * o Supply Planning heuristico. Ele nao representa o modulo Enterprise de
 * Inventory Policy Optimization; campos transicionais ligados a otimizacao
 * existem apenas para rejeicao defensiva quando payloads compartilhados chegam
 * ao backend Community.</p>
 */
@Data
public class PoliticaEstoquesDTO {

    /**
     * Identificador funcional da politica operacional.
     */
    private String id;

    /**
     * Prioridade cadastrada para desempate entre politicas aplicaveis.
     */
    private Integer prioridade;

    /**
     * Inicio de vigencia da politica.
     */
    private LocalDateTime dataHorarioInicio;

    /**
     * Fim de vigencia da politica.
     */
    private LocalDateTime dataHorarioFim;

    /**
     * Snapshot completo das regras material/location da politica.
     *
     * <p>Lista vazia e permitida para remover regras; lista nula e rejeitada
     * pelo service para evitar ambiguidade entre payload incompleto e snapshot
     * intencionalmente vazio.</p>
     */
    private List<PoliticaEstoquesMaterialLocationDTO> materialLocationList = new ArrayList<>();

    /**
     * Regra operacional de safety stock para uma combinacao material/location.
     */
    @Data
    public static class PoliticaEstoquesMaterialLocationDTO {

        /**
         * Material ao qual a regra se aplica.
         */
        private String materialId;

        /**
         * Location ao qual a regra se aplica.
         */
        private String locationId;

        /**
         * Modelo de reposicao operacional usado pelo heuristico.
         */
        private Constantes.SNPModeloReabastecimento modeloReabastecimento;

        /**
         * Modelo operacional cadastrado para a DFU quando a politica precisar
         * sobrescrever o default do material.
         */
        private Constantes.SNPModeloOperacional modeloOperacional;

        /**
         * Indica se o safety stock foi informado em dias ou quantidade.
         */
        private Constantes.SNPCalculoSafetyStock calculoSafetyStock;

        /**
         * Valor operacional de safety stock para DRP ou target de Kanban.
         */
        private Double estoqueSegurancaDrpOuTargetKanban;

        /**
         * Estoque maximo operacional de DRP.
         */
        private Double estoqueMaximoDrp;

        /*
         * Campo transicional reservado ao Enterprise. No Community, safety stock
         * operacional usa os demais parametros; frequencia de reabastecimento e
         * entrada de otimizacao de politica de estoques, fica oculta no OpenAPI
         * Community e deve ser rejeitada se chegar preenchida no payload.
         */
        private Double frequenciaReabastecimentoDias;

    }
}
