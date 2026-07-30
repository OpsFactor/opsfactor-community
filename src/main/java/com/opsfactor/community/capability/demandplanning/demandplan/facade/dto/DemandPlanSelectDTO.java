package com.opsfactor.community.capability.demandplanning.demandplan.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import lombok.Data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * DTO usado para preencher os seletores de abertura de Demand Plan.
 *
 * <p>A selecao Community ocorre por cluster de locations, location especifica
 * ou opcao `All`, e versao do Demand Plan. Clusters de materiais nao entram
 * neste DTO porque a abertura do plano e a colaboracao continuam material/location
 * dentro da view selecionada.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DemandPlanSelectDTO {

    /**
     * Cluster de locations planejado por Demand Planning.
     */
    @Data
    private class ClusterLocationsDTO {
        Long id;
        String descricao;
        List<LocationDTO> locations = new ArrayList<>();
    }

    /**
     * Location selecionavel dentro do cluster. A opcao especial `All` representa
     * todas as locations do cluster.
     */
    @Data
    private class LocationDTO {
        String id;
        String descricao;
    }

    /**
     * Clusters e locations disponiveis para filtro.
     */
    public List<ClusterLocationsDTO> clusterLocationDTOList = new ArrayList<>();

    /**
     * Versoes de Demand Plan disponiveis, ordenadas da mais recente para a mais
     * antiga.
     */
    public List<VersaoDemandPlanDTO> demandPlanDTOList = new ArrayList<>();

    /**
     * Adiciona clusters de locations e respectivas locations ativas ao DTO.
     */
    public void addClusterLocationsList(List<ClusterLocations> clusterLocationsList, ClusterEParametrosProjection clusterEParametrosProjection) {

        for (ClusterLocations clusterLocations : clusterLocationsList) {
            ClusterLocationsDTO clusterLocationsDTO = new ClusterLocationsDTO();
            clusterLocationsDTO.id = clusterLocations.getId();
            clusterLocationsDTO.descricao = clusterLocations.getDescricao();
            for (Location location : clusterEParametrosProjection.getLocationsAtivasDeClusterLocations(clusterLocations)) {
                LocationDTO locationDTO = new LocationDTO();
                locationDTO.id = location.getId();
                locationDTO.descricao = location.getDescricao();
                // adiciona location à lista de locations dentro do cluster
                clusterLocationsDTO.locations.add(locationDTO);
            }
            // ordena locations dentro do cluster por ID
            clusterLocationsDTO.locations.sort(Comparator.comparing(LocationDTO::getId));
            // adiciona location 'All' na 1a posição da lista. Representa a opção de todas as locations
            LocationDTO locationAll = new LocationDTO();
            locationAll.id = "All";
            locationAll.descricao = "";
            clusterLocationsDTO.locations.add(0, locationAll);
            // adiciona cluster locations à lista
            clusterLocationDTOList.add(clusterLocationsDTO);
        }
        // ordena clusters por ordem alfabética da descrição
        clusterLocationDTOList.sort(Comparator.comparing(ClusterLocationsDTO::getDescricao));

    }

    /**
     * Adiciona versoes de Demand Plan ao DTO em ordem decrescente de geracao.
     */
    public void addDemandPlanList(List<DemandPlan> demandPlanList) {

        for (DemandPlan demandPlan : demandPlanList) {
            VersaoDemandPlanDTO demandPlanDTO = new VersaoDemandPlanDTO(demandPlan);
            demandPlanDTOList.add(demandPlanDTO);
        }
        // ordena lista de acordo com horário de geração, mas invertido (começando do último gerado)
        demandPlanDTOList.sort(Comparator.comparing(VersaoDemandPlanDTO::getHorarioGeracao).reversed());

    }

}
