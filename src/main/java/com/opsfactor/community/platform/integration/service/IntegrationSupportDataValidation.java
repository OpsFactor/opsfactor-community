package com.opsfactor.community.platform.integration.service;

import com.opsfactor.community.platform.exception.DataUploadException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Validacoes comuns para snapshots de support data usados pelos data uploads.
 *
 * <p>Os mappers de integracao resolvem referencias por mapas carregados antes
 * do processamento das linhas. Um snapshot vazio pode ser uma ausencia
 * operacional valida; snapshot nulo, item nulo, id vazio ou duplicado representa
 * quebra estrutural do repository/cache e deve falhar antes de a linha do
 * arquivo ser interpretada como erro funcional isolado.</p>
 */
public final class IntegrationSupportDataValidation {

    private IntegrationSupportDataValidation() {

    }

    /**
     * Indexa uma colecao obrigatoria de support data por id.
     *
     * <p>O uso de {@link LinkedHashMap} preserva ordem de leitura para que as
     * mensagens de erro continuem deterministicas em testes e diagnosticos.
     * O metodo nao consulta banco nem aplica regra de negocio; ele apenas
     * transforma um snapshot ja carregado em mapa validado para o mapper.</p>
     */
    public static <ENTITY> Map<String, ENTITY> getMapaPorIdObrigatorio(
            Collection<? extends ENTITY> entityCollection,
            Function<ENTITY, String> idExtractor,
            String snapshotDescription) {

        if (entityCollection == null) {
            throw new DataUploadException(snapshotDescription + " returned null.");
        }

        Map<String, ENTITY> mapaPorId = new LinkedHashMap<>();
        int indice = 0;
        for (ENTITY entity : entityCollection) {
            if (entity == null) {
                throw new DataUploadException(snapshotDescription + " returned null item at index " + indice + ".");
            }

            String id = idExtractor.apply(entity);
            if (id == null || id.isBlank()) {
                throw new DataUploadException(snapshotDescription + " returned item without id at index " + indice + ".");
            }
            if (mapaPorId.put(id, entity) != null) {
                throw new DataUploadException(snapshotDescription + " returned duplicated id " + id + ".");
            }

            indice++;
        }

        return mapaPorId;

    }

}
