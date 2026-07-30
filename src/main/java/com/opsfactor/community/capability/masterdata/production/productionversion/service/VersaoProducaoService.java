package com.opsfactor.community.capability.masterdata.production.productionversion.service;

import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducaoInexistente;
import com.opsfactor.community.capability.masterdata.production.productionversion.repository.VersaoProducaoInexistenteRepository;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service de modelo para a sentinela de versao de producao inexistente usada
 * por projections e heuristicas Community.
 */
@Service
public class VersaoProducaoService {

    /**
     * Repository da sentinela persistida de versao de producao inexistente.
     */
    @Autowired
    private VersaoProducaoInexistenteRepository versaoProducaoInexistenteRepository;

    /**
     * Retorna a versao de producao inexistente persistida em banco.
     *
     * <p>Qualquer registro com id diferente da sentinela padrao e removido
     * para evitar que projections interpretem multiplas versoes inexistentes
     * como alternativas produtivas reais.</p>
     */
    public VersaoProducaoInexistente getOuPersisteVersaoProducaoInexistente() {

        List<VersaoProducaoInexistente> versoesProducaoInexistentes =
                versaoProducaoInexistenteRepository.findAll();
        validaSnapshotVersoesProducaoInexistentesCommunity(versoesProducaoInexistentes);
        List<VersaoProducaoInexistente> resultadosIncompativeis = new ArrayList<>();
        VersaoProducaoInexistente versaoProducaoInexistente = null;
        
        for (VersaoProducaoInexistente versaoProducaoInexistenteIterada : versoesProducaoInexistentes) {
            if (versaoProducaoInexistenteIterada.getId().equals(VersaoProducaoInexistente.ID_VERSAO_PRODUCAO_VAZIA)) {
                versaoProducaoInexistente = versaoProducaoInexistenteIterada;
            } else {
                /*
                 * Remove registros VersaoProducaoInexistente com chave
                 * diferente da chave padrao. A sentinela e conceito unico; uma
                 * segunda linha seria interpretada por projections como
                 * alternativa produtiva inexistente adicional.
                 */
                resultadosIncompativeis.add(versaoProducaoInexistenteIterada);
            }
        }
        
        if (!resultadosIncompativeis.isEmpty()) versaoProducaoInexistenteRepository.deleteAll(resultadosIncompativeis);
        if (versaoProducaoInexistente == null) {
            versaoProducaoInexistente = versaoProducaoInexistenteRepository.save(new VersaoProducaoInexistente());
            validaVersaoProducaoInexistenteSalvaCommunity(versaoProducaoInexistente);
        }
        return versaoProducaoInexistente;
        
    }

    private void validaSnapshotVersoesProducaoInexistentesCommunity(
            List<VersaoProducaoInexistente> versoesProducaoInexistentes) {

        if (versoesProducaoInexistentes == null) {
            throw new IllegalArgumentException("Production version sentinel snapshot is required.");
        }
        int indiceVersaoProducaoInexistente = 0;
        for (VersaoProducaoInexistente versaoProducaoInexistente : versoesProducaoInexistentes) {
            if (versaoProducaoInexistente == null) {
                throw new IllegalArgumentException(
                        "Production version sentinel snapshot item at index "
                                + indiceVersaoProducaoInexistente
                                + " is required.");
            }
            if (versaoProducaoInexistente.getId() == null
                    || versaoProducaoInexistente.getId().isBlank()) {
                throw new IllegalArgumentException(
                        "Production version sentinel snapshot item at index "
                                + indiceVersaoProducaoInexistente
                                + " must have an id.");
            }
            indiceVersaoProducaoInexistente++;
        }

    }

    private void validaVersaoProducaoInexistenteSalvaCommunity(
            VersaoProducaoInexistente versaoProducaoInexistente) {

        if (versaoProducaoInexistente == null) {
            throw new IllegalArgumentException("Saved production version sentinel is required.");
        }
        if (!VersaoProducaoInexistente.ID_VERSAO_PRODUCAO_VAZIA.equals(versaoProducaoInexistente.getId())) {
            throw new IllegalArgumentException(
                    "Saved production version sentinel must have id "
                            + VersaoProducaoInexistente.ID_VERSAO_PRODUCAO_VAZIA
                            + ".");
        }

    }
        
}
