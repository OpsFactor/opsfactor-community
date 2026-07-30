package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.service;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.ConversaoUnidade;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.ConversaoUnidadeProduto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.ConversaoUnidadeProdutoRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.ConversaoUnidadeRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.NoResultException;
import java.util.List;
import java.util.Optional;

/**
 * Service de dominio para unidades de medida e conversoes operacionais.
 *
 * <p>Unidades de medida permanecem no Community porque Demand Planning,
 * Supply Planning heuristico, integrations e Planning Book precisam converter
 * quantidades fisicas. Este service nao carrega nenhuma regra Enterprise de
 * custos, frotas, volume/peso logistico ou precificacao.</p>
 */
@Service
@Slf4j
public class UnidadeMedidaService {

    /**
     * Unidade fisica padrao criada automaticamente em bases novas.
     */
    private static final String UNIDADE_MEDIDA_PADRAO_ID = "UN";

    /**
     * Repository das unidades de medida cadastradas.
     */
    @Autowired
    private UnidadeMedidaRepository unidadeMedidaRepository;

    /**
     * Repository das conversoes gerais entre unidades de medida.
     */
    @Autowired
    private ConversaoUnidadeRepository conversaoUnidadeRepository;

    /**
     * Repository das conversoes especificas por material quando uma conversao
     * geral nao e suficiente.
     */
    @Autowired
    private ConversaoUnidadeProdutoRepository conversaoUnidadeProdutoRepository;

    /**
     * Garante a existencia da unidade padrao `UN` ao subir a aplicacao.
     */
    @PostConstruct
    private void inicializaUnidadeMedidaPadraoUN() {

        criaUnidadeMedidaUN();

    }

    /**
     * Busca uma unidade de medida obrigatoria por id.
     */
    public UnidadeMedida getUnidadeMedida(String unidadeMedidaId) {

        if (unidadeMedidaId == null) throw new NoResultException("Empty UOM Id");
        return unidadeMedidaRepository.findById(unidadeMedidaId).orElseThrow(() -> new NoResultException("UOM " + unidadeMedidaId + " not found"));

    }

    /**
     * Cria a unidade tecnica padrao `UN` caso uma base nova ainda nao a tenha.
     */
    private void criaUnidadeMedidaUN() {

        Optional<UnidadeMedida> optionalUnidadeMedidaUN =
                unidadeMedidaRepository.findById(UNIDADE_MEDIDA_PADRAO_ID);
        validaOptionalUnidadeMedidaPadraoUNCommunity(optionalUnidadeMedidaUN);

        if (optionalUnidadeMedidaUN.isPresent()) {
            validaUnidadeMedidaPadraoUNSalvaCommunity(optionalUnidadeMedidaUN.get());
        } else {
            UnidadeMedida unidadeMedidaUN = new UnidadeMedida();
            unidadeMedidaUN.setId(UNIDADE_MEDIDA_PADRAO_ID);
            unidadeMedidaUN.setDescricao("Units");
            UnidadeMedida unidadeMedidaSalva = unidadeMedidaRepository.save(unidadeMedidaUN);
            validaUnidadeMedidaPadraoUNSalvaCommunity(unidadeMedidaSalva);
        }

    }

    private void validaOptionalUnidadeMedidaPadraoUNCommunity(
            Optional<UnidadeMedida> optionalUnidadeMedidaUN) {

        if (optionalUnidadeMedidaUN == null) {
            throw new IllegalArgumentException("Default UOM lookup result is required.");
        }

    }

    private void validaUnidadeMedidaPadraoUNSalvaCommunity(
            UnidadeMedida unidadeMedida) {

        if (unidadeMedida == null) {
            throw new IllegalArgumentException("Default UOM UN is required.");
        }
        if (!UNIDADE_MEDIDA_PADRAO_ID.equals(unidadeMedida.getId())) {
            throw new IllegalArgumentException("Default UOM must have id UN.");
        }

    }

    /**
     * Retorna o fator de conversao entre duas unidades para um material.
     *
     * <p>A conversao especifica por material tem prioridade sobre a conversao
     * geral. A regra legada de fallback para {@code 1.0} permanece explicita
     * aqui para nao alterar calculos existentes durante a migracao
     * Community/Enterprise.</p>
     */
    public double getConversaoEntreUnidadesMedida(
            UnidadeMedida unidadeMedidaOrigem, 
            UnidadeMedida unidadeMedidaDestino, 
            Produto material) {

        List<ConversaoUnidade> conversoesUnidadePadrao = conversaoUnidadeRepository.findAll();
        List<ConversaoUnidadeProduto> conversoesUnidadePorProduto = conversaoUnidadeProdutoRepository.findAll();

        Optional<ConversaoUnidade> optionalConversoesUnidade = conversoesUnidadePadrao.stream()
                .filter(x -> x.getUnidadeMedidaOrigem().equals(unidadeMedidaOrigem)
                        && x.getUnidadeMedidaDestino().equals(unidadeMedidaDestino))
                .findFirst();

        Optional<ConversaoUnidadeProduto> optionalConversoesUnidadePorProduto = conversoesUnidadePorProduto.stream()
                .filter(x -> x.getProduto().equals(material) && x.getUnidadeMedidaOrigem().equals(unidadeMedidaOrigem)
                        && x.getUnidadeMedidaDestino().equals(unidadeMedidaDestino))
                .findFirst();

        if (unidadeMedidaOrigem.equals(unidadeMedidaDestino)) {
            return 1;
        }

        return optionalConversoesUnidadePorProduto
                .map(ConversaoUnidadeProduto::getQuantidadeUnidadeDestinoPorUnidadeOrigem)
                .orElseGet(() -> optionalConversoesUnidade
                        .map(ConversaoUnidade::getQuantidadeUnidadeDestinoPorUnidadeOrigem)
                        .orElse(1.0));

    }

    /**
     * Converte uma quantidade da unidade origem para a unidade destino.
     */
    public double getQuantidadeUnidadeMedidaDestino(
            double quantidadeUnidadeMedidaOrigem,
            UnidadeMedida unidadeMedidaOrigem, 
            UnidadeMedida unidadeMedidaDestino, 
            Produto material) {

        return getConversaoEntreUnidadesMedida(unidadeMedidaOrigem, unidadeMedidaDestino, material)
                * quantidadeUnidadeMedidaOrigem;

    }

    /**
     * Lista todas as unidades de medida cadastradas.
     */
    public List<UnidadeMedida> getUnidadeMedidaList() {

        return unidadeMedidaRepository.findAll();

    }

    /**
     * Alias legado de busca por id mantido para chamadas existentes.
     */
    public UnidadeMedida getUnidadeMedidaDeId(String id) {

        if (id == null) throw new NoResultException("Empty UOM Id");
        return unidadeMedidaRepository.findById(id).orElseThrow(() -> new NoResultException("UOM " + id + " not found"));

    }

}
