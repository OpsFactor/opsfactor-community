package com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto;

import com.opsfactor.community.web.dto.template.DTO;

import java.util.List;
import java.util.Map;

/**
 * Filtro compartilhado por telas historicas que ainda podem receber payloads
 * do front completo.
 *
 * <p>No Community, apenas os campos de ids explicitos sao aceitos. Os mapas de
 * caracteristicas permanecem no DTO para que o service consiga detectar uma
 * tentativa de uso de filtro Enterprise e falhar de forma clara, em vez de
 * ignorar silenciosamente uma selecao enviada pelo front.</p>
 */
public class FiltroMaterialLocationDeCombinacaoCaracteristicasDTO extends DTO {

    /**
     * Campo Enterprise. No Community qualquer valor preenchido deve resultar em
     * RequiresEnterpriseVersionException no service consumidor.
     */
    public Map<String,List<String>> valuesByMaterialCharacteristicId;

    /**
     * Campo Community: filtro simples por ids explicitos de materiais.
     */
    public List<String> materialIds;

    /**
     * Campo Enterprise. No Community qualquer valor preenchido deve resultar em
     * RequiresEnterpriseVersionException no service consumidor.
     */
    public Map<String,List<String>> valuesByLocationCharacteristicId;

    /**
     * Campo Community: filtro simples por ids explicitos de locations.
     */
    public List<String> locationIds;

    public boolean isSelecaoMateriaisVazia() {
        return
                (materialIds == null || materialIds.isEmpty())
                && (
                        valuesByMaterialCharacteristicId == null
                        || valuesByMaterialCharacteristicId.isEmpty()
                        // ex. lista com keys (caracteristicas) mas sem nenhum valor para nenhuma caracteristica (listas vazias)
                        || !valuesByMaterialCharacteristicId
                                .values()
                                .stream()
                                .anyMatch(lista -> !lista.isEmpty())
                );
    }

    public boolean isSelecaoLocationsVazia() {
        return
                (locationIds == null || locationIds.isEmpty())
                && (
                        valuesByLocationCharacteristicId == null
                        || valuesByLocationCharacteristicId.isEmpty()
                        // ex. lista com keys (caracteristicas) mas sem nenhum valor para nenhuma caracteristica (listas vazias)
                        || !valuesByLocationCharacteristicId
                                .values()
                                .stream()
                                .anyMatch(lista -> !lista.isEmpty())
                );
    }


}
