package com.opsfactor.community.capability.cluster.facade;

import com.opsfactor.community.capability.cluster.facade.dto.ClusterLocationsDTO;
import com.opsfactor.community.capability.cluster.facade.mapper.ClusterLocationsMapper;
import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.repository.location.ClusterLocationsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service DTO para listar clusters de locations usados pelas configuracoes Community.
 */
@Service
public class ClusterLocationDtoService {

    /**
     * Repository de clusters de locations. O @Autowired fica explicito para
     * separar beans Spring de estado local do service.
     */
    @Autowired
    private ClusterLocationsRepository clusterLocationsRepository;

    /**
     * Traz DTO de todos os clusters de locations disponíveis para seleção.
     *
     * <p>O Community não filtra aqui o cluster padrão: qualquer restrição de
     * uso deve ficar na borda funcional que consome a lista, para evitar que
     * este service DTO esconda cadastro ainda necessário em telas de configuração.</p>
     */
    public List<ClusterLocationsDTO> getListaClusterLocationDTO() {

        List<ClusterLocations> listaClusterLocations = clusterLocationsRepository.customFindAll();
        return listaClusterLocations.stream()
                .map(clusterLocations -> ClusterLocationsMapper.convertComRegrasAlocacaoDTO(clusterLocations))
                .collect(Collectors.toList());

    }
    
}
