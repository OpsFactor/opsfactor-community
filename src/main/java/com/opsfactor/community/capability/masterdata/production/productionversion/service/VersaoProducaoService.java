package com.opsfactor.community.capability.masterdata.production.productionversion.service;

import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.production.productionversion.repository.VersaoProducaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Mantém a sentinela dentro da própria entidade única de versão. */
@Service
public class VersaoProducaoService {

    /** Repository da versao unica e da sentinela de producao. */
    @Autowired
    private VersaoProducaoRepository versaoProducaoRepository;

    public VersaoProducao getOuPersisteVersaoProducaoInexistente() {

        return versaoProducaoRepository.findById(VersaoProducao.ID_VERSAO_PRODUCAO_VAZIA)
                .orElseGet(() -> {
                    return versaoProducaoRepository.save(
                            VersaoProducao.criaVersaoProducaoInexistente());
                });

    }
}
