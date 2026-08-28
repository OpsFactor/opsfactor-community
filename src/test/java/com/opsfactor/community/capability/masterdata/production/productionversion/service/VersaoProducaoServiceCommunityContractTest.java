package com.opsfactor.community.capability.masterdata.production.productionversion.service;

import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.production.productionversion.repository.VersaoProducaoRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Optional;

/** Contratos da sentinela mantida na entidade única de versão de produção. */
class VersaoProducaoServiceCommunityContractTest {

    @Test
    void shouldReturnThePersistedCanonicalSentinel() {

        VersaoProducaoRepository versaoProducaoRepository = Mockito.mock(VersaoProducaoRepository.class);
        VersaoProducao sentinelaPersistida = criaSentinela();
        Mockito.when(versaoProducaoRepository.findById(VersaoProducao.ID_VERSAO_PRODUCAO_VAZIA))
                .thenReturn(Optional.of(sentinelaPersistida));
        VersaoProducaoService versaoProducaoService = new VersaoProducaoService(versaoProducaoRepository);

        VersaoProducao resultado = versaoProducaoService.getOuPersisteVersaoProducaoInexistente();

        Assertions.assertSame(sentinelaPersistida, resultado);
        Mockito.verify(versaoProducaoRepository, Mockito.never()).save(Mockito.any());

    }

    @Test
    void shouldCreateTheCanonicalSentinelUsingTheGenericRepository() {

        VersaoProducaoRepository versaoProducaoRepository = Mockito.mock(VersaoProducaoRepository.class);
        Mockito.when(versaoProducaoRepository.findById(VersaoProducao.ID_VERSAO_PRODUCAO_VAZIA))
                .thenReturn(Optional.empty());
        Mockito.when(versaoProducaoRepository.save(Mockito.any(VersaoProducao.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        VersaoProducaoService versaoProducaoService = new VersaoProducaoService(versaoProducaoRepository);

        VersaoProducao resultado = versaoProducaoService.getOuPersisteVersaoProducaoInexistente();

        ArgumentCaptor<VersaoProducao> captor = ArgumentCaptor.forClass(VersaoProducao.class);
        Mockito.verify(versaoProducaoRepository).save(captor.capture());
        Assertions.assertSame(captor.getValue(), resultado);
        Assertions.assertTrue(resultado.isVersaoProducaoInexistente());
        Assertions.assertFalse(resultado.getAtivo());

    }

    private static VersaoProducao criaSentinela() {

        VersaoProducao versaoProducao = new VersaoProducao();
        versaoProducao.setId(VersaoProducao.ID_VERSAO_PRODUCAO_VAZIA);
        versaoProducao.setAtivo(false);
        return versaoProducao;

    }

}
