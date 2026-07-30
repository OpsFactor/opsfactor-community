package com.opsfactor.community.capability.cluster.facade.mapper;

import com.opsfactor.community.capability.cluster.facade.dto.ClusterLocationsDTO;
import com.opsfactor.community.capability.cluster.facade.dto.RegraAlocaoClusterLocationsPaisEstadoDTO;
import com.opsfactor.community.capability.cluster.facade.dto.RegraAlocaoClusterLocationsTipoLocationDTO;
import com.opsfactor.community.capability.masterdata.network.location.facade.dto.LocationDTO;
import com.opsfactor.community.capability.masterdata.network.location.facade.mapper.LocationMapper;
import com.opsfactor.community.capability.cluster.domain.location.*;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ClusterLocationsMapper {

    public static ClusterLocationsDTO convert(ClusterLocations clusterLocations){
        ClusterLocationsDTO dto = new ClusterLocationsDTO();
        dto.setDescription(clusterLocations.getDescricao());
        dto.setId(clusterLocations.getId());
        dto.setPriority(clusterLocations.getPrioridade());
        return dto;
    }

    public static List<ClusterLocationsDTO> convert(Collection<ClusterLocations> clustersLocations){
        return clustersLocations.stream()
                .map(ClusterLocationsMapper::convert)
                .collect(Collectors.toList());
    }

    public static ClusterLocationsDTO convertComRegrasAlocacaoDTO(ClusterLocations clusterLocations){
        ClusterLocationsDTO dto = new ClusterLocationsDTO();
        dto.setId(clusterLocations.getId());
        dto.setDescription(clusterLocations.getDescricao());
        dto.setPriority(clusterLocations.getPrioridade());
        for (RegraAlocacaoClusterLocations regrasAlocacaoClusterLocation :  clusterLocations.getRegrasAlocacaoClusterLocations()) {
            switch (regrasAlocacaoClusterLocation.getRegraAlocacaoTipo()) {
                case CARACTERISTICA:
                    throw new RequiresEnterpriseVersionException("Location characteristic cluster allocation");
                case PAIS_ESTADO:
                    RegraAlocaoClusterLocationsPaisEstadoDTO regraDTOPaisEstado = new RegraAlocaoClusterLocationsPaisEstadoDTO();
                    regraDTOPaisEstado.setCriterio(Constantes.RegraAlocacaoClusterLocationsTipo.PAIS_ESTADO);
                    regraDTOPaisEstado.setId(regrasAlocacaoClusterLocation.getId());
                    for (RegraAlocacaoClusterLocationsPaisEstado regraAlocacaoPaisEstado: regrasAlocacaoClusterLocation.getRegrasAlocacaoClusterLocationsPaisEstadoSet()) {
                        regraDTOPaisEstado.setPais(regraAlocacaoPaisEstado.getPais());
                        regraDTOPaisEstado.setEstado(regraAlocacaoPaisEstado.getEstado());
                        dto.getRegraAlocacaoClusterDTOList().add(regraDTOPaisEstado);
                    }
                    break;
                case TIPO_LOCATION:
                    RegraAlocaoClusterLocationsTipoLocationDTO regraDTOTipoLocation = new RegraAlocaoClusterLocationsTipoLocationDTO();
                    regraDTOTipoLocation.setCriterio(Constantes.RegraAlocacaoClusterLocationsTipo.TIPO_LOCATION);
                    regraDTOTipoLocation.setId(regrasAlocacaoClusterLocation.getId());
                    for (RegraAlocacaoClusterLocationsTipoLocation regraAlocacaoTipoLocation: regrasAlocacaoClusterLocation.getRegrasAlocacaoClusterLocationsTipoLocationSet()) {
                        regraDTOTipoLocation.setLocationType(regraAlocacaoTipoLocation.getTipoLocation());
                        dto.getRegraAlocacaoClusterDTOList().add(regraDTOTipoLocation);
                    }
                    break;
            }
        }
        return dto;
    }

    public static ClusterLocationsDTO convertComListaLocationsERegrasAlocacaoDTO(
            ClusterLocations clusterLocations,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        ClusterLocationsDTO dto = convertComRegrasAlocacaoDTO(clusterLocations);

        List<LocationDTO> produtoDTOList = clusterEParametrosProjection.getLocationsDeClusterLocations(clusterLocations, true)
                .stream()
                .map(LocationMapper::convertSemCaracteristicasLocation)
                .collect(Collectors.toList());

        dto.setLocations(produtoDTOList);

        return dto;

    }

}
