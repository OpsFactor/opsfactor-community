package com.opsfactor.community.capability.masterdata.production.routing.facade.mapper;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.production.routing.facade.dto.RoteiroDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/**
 * Garante que a superficie REST de roteiro preserva a configuracao bruta da
 * flag, sem transformar a ausencia em seu fallback efetivo.
 */
class RoteiroAutoMapperCommunityTest {

    private final RoteiroAutoMapper roteiroAutoMapper = Mappers.getMapper(RoteiroAutoMapper.class);

    @Test
    void shouldExposeConfiguredValueWithoutApplyingEntityFallback() {

        Roteiro roteiroSemConfiguracao = new Roteiro();
        roteiroSemConfiguracao.setMaterialOutput(new Produto("MAT-1"));
        roteiroSemConfiguracao.setHabilitadoParaUsoSemVersaoProducao(null);

        RoteiroDTO dtoSemConfiguracao = roteiroAutoMapper.converte(roteiroSemConfiguracao);

        Assertions.assertNull(dtoSemConfiguracao.getCanBeUsedWithoutProductionVersion());
        Assertions.assertTrue(roteiroSemConfiguracao.getHabilitadoParaUsoSemVersaoProducao());

        Roteiro roteiroBloqueado = new Roteiro();
        roteiroBloqueado.setMaterialOutput(new Produto("MAT-2"));
        roteiroBloqueado.setHabilitadoParaUsoSemVersaoProducao(false);

        RoteiroDTO dtoBloqueado = roteiroAutoMapper.converte(roteiroBloqueado);

        Assertions.assertFalse(dtoBloqueado.getCanBeUsedWithoutProductionVersion());

    }

}
