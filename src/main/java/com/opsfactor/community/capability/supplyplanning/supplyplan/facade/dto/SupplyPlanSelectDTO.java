package com.opsfactor.community.capability.supplyplanning.supplyplan.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import lombok.Data;

/**
 * DTO de selecao legado para Supply Planning.
 *
 * <p>O seletor Community lista locations disponiveis e versoes de Supply Plan.
 * Ele nao representa location-level execution profile Enterprise; a execucao
 * Community segue o perfil heuristico e processa todas as locations ativas do
 * escopo aprovado.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupplyPlanSelectDTO {

    /**
     * Item simples de location para seletores legados.
     */
    @Data
    private class LocationDTO {
        /** Codigo da location. */
        String id;
        /** Descricao exibida da location. */
        String descricao;
    }

    /** Locations disponiveis para navegacao do seletor legado. */
    public List<LocationDTO> locationDTOList = new ArrayList<>();

    /** Versoes de Supply Plan ordenadas da mais recente para a mais antiga. */
    public List<VersaoSupplyPlanDTO> supplyPlanDTOList = new ArrayList<>();

    /**
     * Adiciona locations ao seletor, ordenando por id para resposta
     * deterministica.
     */
    public void addLocationList(List<Location> locationsList) {

        for (Location location : locationsList) {
            LocationDTO locationDTO = new LocationDTO();
            locationDTO.id = location.getId();
            locationDTO.descricao = location.getDescricao();
            locationDTOList.add(locationDTO);
        }
        locationDTOList.sort(Comparator.comparing(LocationDTO::getId));

    }

    /**
     * Adiciona versoes de Supply Plan, mantendo a mais recente no topo.
     */
    public void addSupplyPlanList(List<SupplyPlan> supplyPlanList) {

        for (SupplyPlan supplyPlan : supplyPlanList) {
            VersaoSupplyPlanDTO supplyPlanDTO = new VersaoSupplyPlanDTO(supplyPlan);
            supplyPlanDTOList.add(supplyPlanDTO);
        }
        supplyPlanDTOList.sort(Comparator.comparing(VersaoSupplyPlanDTO::getHorarioGeracao).reversed());

    }
}
