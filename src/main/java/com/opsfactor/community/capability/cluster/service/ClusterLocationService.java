package com.opsfactor.community.capability.cluster.service;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.repository.location.ClusterLocationsRepository;
import com.opsfactor.community.platform.utility.Constantes;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service de modelo para clusters de locations usados por Demand Planning e
 * Planning Book Community.
 */
@Service
public class ClusterLocationService {

    /**
     * Repository dos clusters de location, incluindo dependencias de
     * parametros usadas para filtrar clusters planejaveis em Demand Planning.
     */
    @Autowired
    private ClusterLocationsRepository clusterLocationsRepository;

    /**
     * Busca um cluster de location por id.
     */
    public Optional<ClusterLocations> getClusterLocation(long id){
        return clusterLocationsRepository.findById(id);
    }


    /**
     * Lista clusters de location, opcionalmente removendo o cluster padrao.
     */
    public List<ClusterLocations> getClusterLocationList(boolean withoutDefault){
        if (withoutDefault){
            return clusterLocationsRepository.customFindAll().stream()
                    .filter(x -> !x.getId().equals(Constantes.CLUSTER_LOCATION_PADRAO_ID))
                    .collect(Collectors.toList());
        } else {
            return new ArrayList<>(clusterLocationsRepository.customFindAll());
        }
    }

    /**
     * Lista apenas clusters habilitados para Demand Planning.
     */
    public List<ClusterLocations> getListaClusterLocationsParaDemandPlanning() {
        return getClusterLocationList(true).stream()
                    .filter(x -> x.getParametrosClusterLocations().getPlanejaDP())
                    .collect(Collectors.toList());    
    }
        
}
