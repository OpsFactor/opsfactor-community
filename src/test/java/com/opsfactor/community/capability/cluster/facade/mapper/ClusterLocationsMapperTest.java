package com.opsfactor.community.capability.cluster.facade.mapper;

import com.opsfactor.community.capability.cluster.facade.dto.ClusterLocationsDTO;
import com.opsfactor.community.capability.cluster.facade.dto.RegraAlocaoClusterLocationsPaisEstadoDTO;
import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.domain.location.RegraAlocacaoClusterLocations;
import com.opsfactor.community.capability.cluster.domain.location.RegraAlocacaoClusterLocationsPaisEstado;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Testes de contrato do mapper Community de clusters de location. Pais/estado
 * e tipo de location sao criterios Community; caracteristicas seguem bloqueadas
 * em outro ponto do mapper.
 */
public class ClusterLocationsMapperTest {

    @Test
    public void convertComRegrasAlocacaoDtoShouldRejectLegacyCharacteristicRuleCommunity() {

        ClusterLocations clusterLocations = new ClusterLocations("Sul", false, 1);

        RegraAlocacaoClusterLocations regraAlocacaoClusterLocations =
                new RegraAlocacaoClusterLocations();
        regraAlocacaoClusterLocations.setClusterLocations(clusterLocations);
        regraAlocacaoClusterLocations.setRegraAlocacaoTipo(
                Constantes.RegraAlocacaoClusterLocationsTipo.CARACTERISTICA);
        clusterLocations.getRegrasAlocacaoClusterLocations().add(regraAlocacaoClusterLocations);

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> ClusterLocationsMapper.convertComRegrasAlocacaoDTO(clusterLocations));

    }

    @Test
    public void convertComRegrasAlocacaoDtoShouldPreserveCountryAndState() {

        ClusterLocations clusterLocations = new ClusterLocations("Sul", false, 1);
        clusterLocations.setId(10L);

        RegraAlocacaoClusterLocations regraAlocacaoClusterLocations =
                new RegraAlocacaoClusterLocations();
        regraAlocacaoClusterLocations.setId(20L);
        regraAlocacaoClusterLocations.setClusterLocations(clusterLocations);
        regraAlocacaoClusterLocations.setRegraAlocacaoTipo(
                Constantes.RegraAlocacaoClusterLocationsTipo.PAIS_ESTADO);

        RegraAlocacaoClusterLocationsPaisEstado regraAlocacaoClusterLocationsPaisEstado =
                new RegraAlocacaoClusterLocationsPaisEstado(
                        new RegraAlocacaoClusterLocationsPaisEstado.RegraAlocacaoClusterLocationsPaisEstadoCompositeKey(
                                regraAlocacaoClusterLocations,
                                "BR",
                                "SP"));
        regraAlocacaoClusterLocations.addRegraAlocacaoPaisEstado(regraAlocacaoClusterLocationsPaisEstado);
        clusterLocations.getRegrasAlocacaoClusterLocations().add(regraAlocacaoClusterLocations);

        ClusterLocationsDTO clusterLocationsDTO =
                ClusterLocationsMapper.convertComRegrasAlocacaoDTO(clusterLocations);
        RegraAlocaoClusterLocationsPaisEstadoDTO regraAlocaoClusterLocationsPaisEstadoDTO =
                (RegraAlocaoClusterLocationsPaisEstadoDTO) clusterLocationsDTO
                        .getRegraAlocacaoClusterDTOList()
                        .get(0);

        Assertions.assertEquals("BR", regraAlocaoClusterLocationsPaisEstadoDTO.getPais());
        Assertions.assertEquals("SP", regraAlocaoClusterLocationsPaisEstadoDTO.getEstado());

    }

}
