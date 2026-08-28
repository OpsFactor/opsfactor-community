package com.opsfactor.community.capability.masterdata.production.productionversion.service;

import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.production.productionversion.repository.VersaoProducaoRepository;
import org.springframework.stereotype.Service;

/** Mantém a sentinela dentro da própria entidade única de versão. */
@Service
public class VersaoProducaoService {

    private final VersaoProducaoRepository versaoProducaoRepository;

    public VersaoProducaoService(VersaoProducaoRepository versaoProducaoRepository) {

        this.versaoProducaoRepository = versaoProducaoRepository;

    }

    public VersaoProducao getOuPersisteVersaoProducaoInexistente() {

        return versaoProducaoRepository.findById(VersaoProducao.ID_VERSAO_PRODUCAO_VAZIA)
                .orElseGet(() -> {
                    return versaoProducaoRepository.save(
                            VersaoProducao.criaVersaoProducaoInexistente());
                });

    }
}
