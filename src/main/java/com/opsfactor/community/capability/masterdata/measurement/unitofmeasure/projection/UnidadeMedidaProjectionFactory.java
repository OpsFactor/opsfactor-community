package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.ConversaoUnidade;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.ConversaoUnidadeProduto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnitOfMeasureConversionLegacyRatioState;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.platform.exception.UnitOfMeasureConversionException;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.ConversaoUnidadeProdutoRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.ConversaoUnidadeRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory da projection de unidades de medida usada por calculos Community.
 *
 * <p>A projection concentra conversoes globais e por material em memoria para
 * evitar consultas repetidas durante Planning Book, Demand Planning e Supply
 * Planning. Parametros de forecast por cluster nao fazem parte desta factory;
 * eles sao carregados pelas factories especificas de Demand Planning.</p>
 */
@Component
public class UnidadeMedidaProjectionFactory {

    /**
     * Service de parametros globais usado para preencher unidades padrao no
     * projection de conversoes.
     */
    @Autowired
    private ParametrosGlobaisService parametrosGlobaisService;

    /**
     * Repository do catalogo de unidades de medida.
     */
    @Autowired
    private UnidadeMedidaRepository unidadeMedidaRepository;

    /**
     * Repository de conversoes globais entre unidades.
     */
    @Autowired
    private ConversaoUnidadeRepository conversaoUnidadeRepository;

    /**
     * Repository de conversoes especificas por material.
     */
    @Autowired
    private ConversaoUnidadeProdutoRepository conversaoUnidadeProdutoRepository;

    /**
     * Retorna um projection que contenha as conversões entre unidades de medida,
     * mas sem trazer as unidades de medida padrão para cada processo
     * @return
     */
    public UnidadeMedidaProjection getUnidadeMedidaProjectionComConversoes() {

        List<UnidadeMedida> unidadeMedidaList = unidadeMedidaRepository.findAll();
        List<ConversaoUnidade> conversoesUnidadePadrao = conversaoUnidadeRepository.customFindAllJoinUnidades();
        List<ConversaoUnidadeProduto> conversoesUnidadePorProduto = conversaoUnidadeProdutoRepository.customFindAllJoinProdutoEUnidades();
        ParametrosGlobais parametrosGlobais = parametrosGlobaisService.getParametrosGlobais();

        validaUnidadeMedidaList(unidadeMedidaList);
        validaConversoesUnidadePadrao(conversoesUnidadePadrao, unidadeMedidaList);
        validaConversoesUnidadePorProduto(conversoesUnidadePorProduto, unidadeMedidaList);
        validaParametrosGlobais(parametrosGlobais);

        UnidadeMedidaProjection unidadeMedidaProjection = new UnidadeMedidaProjection();
        unidadeMedidaProjection.parametrosGlobais = parametrosGlobais;
        unidadeMedidaProjection.unidadeMedidaSet = new HashSet(unidadeMedidaList);

        // adiciona conversões padrão ao projection (ex. kg->pc)
        for (ConversaoUnidade conversaoUnidade : conversoesUnidadePadrao) {
            UnidadeMedida unidadeMedidaOrigem = conversaoUnidade.getUnidadeMedidaOrigem();
            UnidadeMedida unidadeMedidaDestino = conversaoUnidade.getUnidadeMedidaDestino();
            double conversao = conversaoUnidade.getQuantidadeUnidadeDestinoPorUnidadeOrigem();

            if (!Double.isFinite(conversao) || conversao <= 0) {
                throw new UnitOfMeasureConversionException(
                        "Global UOM conversion must be finite and positive from "
                                + unidadeMedidaOrigem.getId()
                                + " to "
                                + unidadeMedidaDestino.getId());
            }

            if (!unidadeMedidaProjection.mapaConversoesPadrao.containsKey(unidadeMedidaOrigem)) {
                unidadeMedidaProjection.mapaConversoesPadrao.put(unidadeMedidaOrigem, new ConcurrentHashMap<>());
            }

            unidadeMedidaProjection.mapaConversoesPadrao.get(unidadeMedidaOrigem).put(
                    unidadeMedidaDestino, conversao);
        }

        // adiciona conversões padrão ao projection, no sentido inverso (ex. pc->kg)
        for (ConversaoUnidade conversaoUnidade : conversoesUnidadePadrao) {
            UnidadeMedida unidadeMedidaOrigem = conversaoUnidade.getUnidadeMedidaDestino();
            UnidadeMedida unidadeMedidaDestino = conversaoUnidade.getUnidadeMedidaOrigem();
            double conversao = 1 / conversaoUnidade.getQuantidadeUnidadeDestinoPorUnidadeOrigem(); // inverso

            if (!unidadeMedidaProjection.mapaConversoesPadrao.containsKey(unidadeMedidaOrigem)) {
                unidadeMedidaProjection.mapaConversoesPadrao.put(unidadeMedidaOrigem, new ConcurrentHashMap<>());
            }

            unidadeMedidaProjection.mapaConversoesPadrao.get(unidadeMedidaOrigem).put(
                    unidadeMedidaDestino, conversao);
        }

        // adiciona conversões por produto ao projection (ex. kg->pc)
        for (ConversaoUnidadeProduto conversaoUnidadeProduto : conversoesUnidadePorProduto) {
            Produto produto = conversaoUnidadeProduto.getProduto();

            UnidadeMedida unidadeMedidaOrigem = conversaoUnidadeProduto.getUnidadeMedidaOrigem();
            /*
             * Normaliza a instancia da unidade para o catalogo carregado no
             * projection. Isso evita que proxies JPA equivalentes entrem como
             * chaves diferentes nos mapas de conversao.
             */
            unidadeMedidaOrigem = unidadeMedidaProjection.getUnidadeMedidaFromId(unidadeMedidaOrigem.getId());

            UnidadeMedida unidadeMedidaDestino = conversaoUnidadeProduto.getUnidadeMedidaDestino();
            /*
             * Mesmo motivo da unidade origem: manter chaves canonicas no mapa
             * de conversoes por material.
             */
            unidadeMedidaDestino = unidadeMedidaProjection.getUnidadeMedidaFromId(unidadeMedidaDestino.getId());

            double conversao = conversaoUnidadeProduto.getQuantidadeUnidadeDestinoPorUnidadeOrigem();

            if (!Double.isFinite(conversao) || conversao <= 0) {
                throw new UnitOfMeasureConversionException(
                        "Material-level UOM conversion must be finite and positive from "
                                + unidadeMedidaOrigem.getId()
                                + " to "
                                + unidadeMedidaDestino.getId()
                                + " for material "
                                + produto.getId());
            }

            if (!unidadeMedidaProjection.mapaConversoesPorProduto.containsKey(produto)) {
                unidadeMedidaProjection.mapaConversoesPorProduto.put(produto, new ConcurrentHashMap<>());
            }
            if (!unidadeMedidaProjection.mapaConversoesPorProduto.get(produto).containsKey(unidadeMedidaOrigem)) {
                unidadeMedidaProjection.mapaConversoesPorProduto.get(produto).put(unidadeMedidaOrigem, new ConcurrentHashMap<>());
            }

            // Escrita em etapas para deixar explicita a chave material -> origem -> destino.
            Map<UnidadeMedida,Map<UnidadeMedida,Double>> subMapa = unidadeMedidaProjection.mapaConversoesPorProduto.get(produto);
            Map<UnidadeMedida,Double> subSubMapa = subMapa.get(unidadeMedidaOrigem);
            subSubMapa.put(unidadeMedidaDestino, conversao);

        }

        // adiciona conversões por produto ao projection, no sentido inverso (ex. pc->kg)
        for (ConversaoUnidadeProduto conversaoUnidadeProduto : conversoesUnidadePorProduto) {
            Produto produto = conversaoUnidadeProduto.getProduto();
            UnidadeMedida unidadeMedidaOrigem = conversaoUnidadeProduto.getUnidadeMedidaDestino();
            UnidadeMedida unidadeMedidaDestino = conversaoUnidadeProduto.getUnidadeMedidaOrigem();
            double conversao = 1 / conversaoUnidadeProduto.getQuantidadeUnidadeDestinoPorUnidadeOrigem();

            if (!unidadeMedidaProjection.mapaConversoesPorProduto.containsKey(produto)) {
                unidadeMedidaProjection.mapaConversoesPorProduto.put(produto, new ConcurrentHashMap<>());
            }
            if (!unidadeMedidaProjection.mapaConversoesPorProduto.get(produto).containsKey(unidadeMedidaOrigem)) {
                unidadeMedidaProjection.mapaConversoesPorProduto.get(produto).put(unidadeMedidaOrigem, new ConcurrentHashMap<>());
            }

            unidadeMedidaProjection.mapaConversoesPorProduto.get(produto).get(unidadeMedidaOrigem).put(
                    unidadeMedidaDestino, conversao);
        }


        return unidadeMedidaProjection;

    }

    /**
     * Valida o catalogo de unidades antes de usa-lo como base canonica da
     * projection.
     *
     * <p>Catalogo vazio e valido para uma base ainda sem UOM cadastrada.
     * Colecao nula, item nulo, id vazio ou id duplicado indicam snapshot
     * quebrado do repository e devem falhar antes de qualquer conversao tentar
     * normalizar proxies ou montar mapas.</p>
     */
    private void validaUnidadeMedidaList(List<UnidadeMedida> unidadeMedidaList) {

        if (unidadeMedidaList == null) {
            throw new IllegalStateException("Unit of Measure repository returned null collection.");
        }

        Set<String> idsCarregados = new HashSet<>();
        for (int indice = 0; indice < unidadeMedidaList.size(); indice++) {
            UnidadeMedida unidadeMedida = unidadeMedidaList.get(indice);
            if (unidadeMedida == null) {
                throw new IllegalStateException("Unit of Measure repository returned null item at index " + indice + ".");
            }
            if (unidadeMedida.getId() == null || unidadeMedida.getId().isBlank()) {
                throw new IllegalStateException("Unit of Measure repository returned item without id at index " + indice + ".");
            }
            if (!idsCarregados.add(unidadeMedida.getId())) {
                throw new IllegalStateException("Unit of Measure repository returned duplicated id " + unidadeMedida.getId() + ".");
            }
        }

    }

    /**
     * Valida conversoes globais antes de popular os mapas direto e inverso.
     */
    private void validaConversoesUnidadePadrao(
            List<ConversaoUnidade> conversoesUnidadePadrao,
            List<UnidadeMedida> unidadeMedidaList) {

        if (conversoesUnidadePadrao == null) {
            throw new IllegalStateException("Global UOM conversion repository returned null collection.");
        }

        Set<String> unidadeMedidaIdSet = getUnidadeMedidaIdSet(unidadeMedidaList);
        Set<String> chavesConversao = new HashSet<>();
        for (int indice = 0; indice < conversoesUnidadePadrao.size(); indice++) {
            ConversaoUnidade conversaoUnidade = conversoesUnidadePadrao.get(indice);
            if (conversaoUnidade == null) {
                throw new IllegalStateException("Global UOM conversion repository returned null item at index " + indice + ".");
            }
            if (conversaoUnidade.getConversaoUnidadeCompositeKey() == null) {
                throw new IllegalStateException("Global UOM conversion repository returned item without composite key at index "
                        + indice + ".");
            }

            UnidadeMedida unidadeMedidaOrigem =
                    conversaoUnidade.getConversaoUnidadeCompositeKey().getUnidadeMedidaOrigem();
            UnidadeMedida unidadeMedidaDestino =
                    conversaoUnidade.getConversaoUnidadeCompositeKey().getUnidadeMedidaDestino();
            String unidadeMedidaOrigemId = unidadeMedidaOrigem.getId();
            String unidadeMedidaDestinoId = unidadeMedidaDestino.getId();
            validaUnidadeMedidaExisteNoCatalogo(unidadeMedidaIdSet, unidadeMedidaOrigemId, "Global UOM conversion origin");
            validaUnidadeMedidaExisteNoCatalogo(unidadeMedidaIdSet, unidadeMedidaDestinoId, "Global UOM conversion target");
            if (conversaoUnidade.getLegacyRatioState()
                    == UnitOfMeasureConversionLegacyRatioState.CONFLICTING_LEGACY_AND_CANONICAL_RATIOS) {
                throw new UnitOfMeasureConversionException(
                        "Global UOM conversion has conflicting deprecated and canonical quantity ratios from "
                                + unidadeMedidaOrigemId
                                + " to "
                                + unidadeMedidaDestinoId);
            }

            String chaveConversao = unidadeMedidaOrigemId + "|" + unidadeMedidaDestinoId;
            if (!chavesConversao.add(chaveConversao)) {
                throw new IllegalStateException("Global UOM conversion repository returned duplicated conversion "
                        + chaveConversao + ".");
            }
        }

    }

    /**
     * Valida conversoes especificas por material antes de popular os mapas
     * direto e inverso.
     */
    private void validaConversoesUnidadePorProduto(
            List<ConversaoUnidadeProduto> conversoesUnidadePorProduto,
            List<UnidadeMedida> unidadeMedidaList) {

        if (conversoesUnidadePorProduto == null) {
            throw new IllegalStateException("Material-level UOM conversion repository returned null collection.");
        }

        Set<String> unidadeMedidaIdSet = getUnidadeMedidaIdSet(unidadeMedidaList);
        Set<String> chavesConversao = new HashSet<>();
        for (int indice = 0; indice < conversoesUnidadePorProduto.size(); indice++) {
            ConversaoUnidadeProduto conversaoUnidadeProduto = conversoesUnidadePorProduto.get(indice);
            if (conversaoUnidadeProduto == null) {
                throw new IllegalStateException("Material-level UOM conversion repository returned null item at index " + indice + ".");
            }
            if (conversaoUnidadeProduto.getConversaoUnidadeProdutoCompositeKey() == null) {
                throw new IllegalStateException("Material-level UOM conversion repository returned item without composite key at index "
                        + indice + ".");
            }
            Produto produto = conversaoUnidadeProduto.getConversaoUnidadeProdutoCompositeKey().getProduto();
            if (produto == null
                    || produto.getId() == null
                    || produto.getId().isBlank()) {
                throw new IllegalStateException("Material-level UOM conversion repository returned item without material id at index "
                        + indice + ".");
            }

            String unidadeMedidaOrigemId =
                    conversaoUnidadeProduto.getConversaoUnidadeProdutoCompositeKey()
                            .getUnidadeMedidaOrigem()
                            .getId();
            String unidadeMedidaDestinoId =
                    conversaoUnidadeProduto.getConversaoUnidadeProdutoCompositeKey()
                            .getUnidadeMedidaDestino()
                            .getId();
            validaUnidadeMedidaExisteNoCatalogo(unidadeMedidaIdSet, unidadeMedidaOrigemId, "Material-level UOM conversion origin");
            validaUnidadeMedidaExisteNoCatalogo(unidadeMedidaIdSet, unidadeMedidaDestinoId, "Material-level UOM conversion target");
            if (conversaoUnidadeProduto.getLegacyRatioState()
                    == UnitOfMeasureConversionLegacyRatioState.CONFLICTING_LEGACY_AND_CANONICAL_RATIOS) {
                throw new UnitOfMeasureConversionException(
                        "Material-level UOM conversion has conflicting deprecated and canonical quantity ratios from "
                                + unidadeMedidaOrigemId
                                + " to "
                                + unidadeMedidaDestinoId
                                + " for material "
                                + produto.getId());
            }

            String chaveConversao = produto.getId()
                    + "|"
                    + unidadeMedidaOrigemId
                    + "|"
                    + unidadeMedidaDestinoId;
            if (!chavesConversao.add(chaveConversao)) {
                throw new IllegalStateException("Material-level UOM conversion repository returned duplicated conversion "
                        + chaveConversao + ".");
            }
        }

    }

    /**
     * Valida parametros globais carregados para a projection.
     */
    private void validaParametrosGlobais(ParametrosGlobais parametrosGlobais) {

        if (parametrosGlobais == null) {
            throw new IllegalStateException("Global parameters service returned null for Unit of Measure projection.");
        }

    }

    private Set<String> getUnidadeMedidaIdSet(List<UnidadeMedida> unidadeMedidaList) {

        Set<String> unidadeMedidaIdSet = new HashSet<>();
        for (UnidadeMedida unidadeMedida : unidadeMedidaList) {
            unidadeMedidaIdSet.add(unidadeMedida.getId());
        }
        return unidadeMedidaIdSet;

    }

    private void validaUnidadeMedidaExisteNoCatalogo(
            Set<String> unidadeMedidaIdSet,
            String unidadeMedidaId,
            String contextoConversao) {

        if (!unidadeMedidaIdSet.contains(unidadeMedidaId)) {
            throw new IllegalStateException(
                    contextoConversao + " UOM " + unidadeMedidaId + " is not present in Unit of Measure catalog.");
        }

    }

    /**
     * Retorna o snapshot completo de unidades de medida e conversoes em cache.
     *
     * <p>Essa projection centraliza o catalogo de UOM e as conversoes
     * funcionais usadas por outras projections. A entrada de cache precisa
     * refletir apenas cadastros validados, porque os consumidores assumem que
     * incompatibilidades de unidade falham antes do calculo.</p>
     */
    @Cacheable(value = "unidadeMedidaProjection", sync = true)
    public UnidadeMedidaProjection getUnidadeMedidaProjectionCompletoDeCache() {

        UnidadeMedidaProjection unidadeMedidaProjection = getUnidadeMedidaProjectionComConversoes();

        return unidadeMedidaProjection;

    }

}
