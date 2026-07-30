package com.opsfactor.community.capability.transactionaldata.sales.saleshistory.service;

import com.opsfactor.community.capability.transactionaldata.sales.sellout.repository.SelloutRepository;
import com.opsfactor.community.capability.demandplanning.forecast.configuration.DemandPlanningModelCatalog;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service de leitura de metadados do historico de vendas Community.
 *
 * <p>O Community possui somente sell-out como fonte historica executavel. O
 * enum compartilhado ainda aceita sell-in e pedidos para compatibilidade de
 * payloads e para que o backend consiga falhar com erro funcional explicito,
 * mas este service nao deve consultar repositories Enterprise nem tratar esses
 * documentos como ausencia comum de dados.</p>
 */
@Service
public class SalesService {

    /**
     * Repository Community de sell-out, unica fonte historica aberta.
     */
    @Autowired
    private SelloutRepository selloutRepository;

    /**
     * Resolve a primeira data disponível da fonte histórica configurada
     * explicitamente para o fluxo chamador.
     */
    public LocalDateTime getPrimeiraDataComHistoricoVendasRegistrado(Constantes.TipoDocumentoVenda tipoDocumentoVenda) {

        validaTipoDocumentoVendaCommunity(tipoDocumentoVenda);

        LocalDateTime primeiraDataHistoricoVendas = selloutRepository.customFindPrimeiroSellout();

        return primeiraDataHistoricoVendas;

    }

    /**
     * Resolve a última data disponível da fonte histórica configurada
     * explicitamente para o fluxo chamador.
     */
    public LocalDateTime getUltimaDataComHistoricoVendasRegistrado(Constantes.TipoDocumentoVenda tipoDocumentoVenda) {

        validaTipoDocumentoVendaCommunity(tipoDocumentoVenda);

        LocalDateTime ultimaDataHistoricoVendas = selloutRepository.customFindUltimoSellout();

        return ultimaDataHistoricoVendas;

    }

    /**
     * O Community trabalha somente com sell-out como documento historico de venda.
     * Sell-in e pedidos permanecem no Enterprise para evitar expor entidades e
     * repositorios que nao fazem parte do contrato aberto.
     */
    private void validaTipoDocumentoVendaCommunity(Constantes.TipoDocumentoVenda tipoDocumentoVenda) {

        if (tipoDocumentoVenda == null) {
            throw new IllegalArgumentException("Historical sales document type is required.");
        }
        if (!DemandPlanningModelCatalog.isTipoDocumentoHistoricoCommunity(tipoDocumentoVenda)) {
            throw new RequiresEnterpriseVersionException("Sell-in and sales orders as historical sales source");
        }

    }

    /**
     * Falha cedo quando o tipo configurado no perfil não possui histórico
     * persistido, evitando NPEs e mensagens genéricas em camadas acima.
     */
    

}
