package com.opsfactor.community.capability.configuration.service;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.configuration.repository.ParametrosGlobaisRepository;
import com.opsfactor.community.platform.utility.Constantes;
import org.springframework.stereotype.Service;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Serviço de modelo para o registro unico de parametros globais Community.
 *
 * <p>Esta camada protege a entidade contra bancos legados que ainda possam
 * conter valores Enterprise. A API REST e o front service ja bloqueiam payloads
 * indevidos, mas a leitura/salvamento tambem aplicam defaults neutros para que
 * calculos internos nunca observem sell-in, pedidos ou backlog transacional
 * como capacidades ativas no Community.</p>
 */
@Service
public class ParametrosGlobaisService {
    
    /**
     * Repository do registro unico de parametros globais. O service aplica o
     * recorte Community antes de devolver ou persistir a entidade.
     */
    @Autowired
    private ParametrosGlobaisRepository parametrosGlobaisRepository;

    /**
     * Retorna o registro unico de parametros globais ja neutralizado para o
     * recorte Community.
     */
    public ParametrosGlobais getParametrosGlobais() {
        Optional<ParametrosGlobais> optionalParametrosGlobais =
                validaOptionalParametrosGlobaisRepository(
                        parametrosGlobaisRepository.customFindComDependencias());

        return optionalParametrosGlobais
                .map(this::aplicaRecorteCommunity)
                .orElseGet(() -> {

                    /*
                     * Bases recem-inicializadas podem nao ter o registro unico.
                     * O service cria o registro default ja passando pelo mesmo
                     * recorte Community aplicado aos parametros existentes.
                     */
                    ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
                    parametrosGlobais.setId(0L);
                    return saveParametrosGlobais(parametrosGlobais);

                });
    }
    
    /**
     * Persiste parametros globais mantendo id fixo e defaults Community.
     */
    public ParametrosGlobais saveParametrosGlobais(ParametrosGlobais parametrosGlobais) {

        validaParametrosGlobaisObrigatorios(
                parametrosGlobais,
                "Global parameters are required.");
        parametrosGlobais.setId(0L);
        aplicaRecorteCommunity(parametrosGlobais);
        return validaParametrosGlobaisSalvos(parametrosGlobaisRepository.save(parametrosGlobais));

    }

    /**
     * Valida o retorno do repository do registro unico.
     *
     * <p>{@link Optional#empty()} representa base recem-inicializada e segue
     * fluxo valido. Um {@code Optional} nulo, por outro lado, indica contrato
     * quebrado do repository/stub e precisa falhar antes de parecer ausencia
     * operacional do registro global.</p>
     */
    protected Optional<ParametrosGlobais> validaOptionalParametrosGlobaisRepository(
            Optional<ParametrosGlobais> optionalParametrosGlobais) {

        if (optionalParametrosGlobais == null) {
            throw new IllegalStateException(
                    "Global parameters repository returned null Optional.");
        }

        return optionalParametrosGlobais;

    }

    /**
     * Valida a entidade recebida por bordas publicas de salvamento.
     */
    protected void validaParametrosGlobaisObrigatorios(
            ParametrosGlobais parametrosGlobais,
            String mensagemErro) {

        if (parametrosGlobais == null) {
            throw new IllegalArgumentException(mensagemErro);
        }

    }

    /**
     * Valida o retorno de persistencia do registro unico.
     */
    protected ParametrosGlobais validaParametrosGlobaisSalvos(
            ParametrosGlobais parametrosGlobaisSalvos) {

        if (parametrosGlobaisSalvos == null) {
            throw new IllegalStateException(
                    "Global parameters repository returned null after save.");
        }

        return parametrosGlobaisSalvos;

    }

    /**
     * Aplica o recorte funcional do OpsFactor Community sobre os parâmetros
     * globais. A edição Community não pode reativar sell-in/pedidos, ajustes
     * agregados nem parâmetros transacionais/logísticos. Parâmetros de
     * stockout e limpeza histórica ainda podem existir como schema
     * transicional, mas não viram configuração funcional Community; getters e
     * mappers retornam defaults neutros para contratos compartilhados.
     *
     * O método é chamado tanto na leitura quanto no salvamento para proteger
     * bancos legados que já tragam valores Enterprise persistidos.
     */
    private ParametrosGlobais aplicaRecorteCommunity(ParametrosGlobais parametrosGlobais) {

        parametrosGlobais.setTipoDocumentoVenda(Constantes.TipoDocumentoVenda.SELLOUT);
        parametrosGlobais.setModeloDemandaBase(Constantes.DPModeloDemandaBase.DESATIVADO);
        parametrosGlobais.setDiasHistoricosDoh(Constantes.GLOBAL_PADRAO_DIAS_HISTORICOS_DOH);
        parametrosGlobais.setDiasHistoricosDohStockout(Constantes.GLOBAL_PADRAO_DIAS_HISTORICOS_DOH_STOCKOUT);
        parametrosGlobais.setModeloNormalizacao(Constantes.DPModeloNormalizacao.DESATIVADO);
        parametrosGlobais.setDiasHistoricosNormalizacao(Constantes.DP_PADRAO_DIAS_NORMALIZACAO);
        parametrosGlobais.setPercentilOutliersVenda(Constantes.DP_PADRAO_PERCENTIL_OUTLIERS_VENDA);
        /*
         * Planning Books Community trabalham no nivel material/location. Mesmo
         * que bancos legados tenham defaults antigos permitindo ajuste
         * agregado, calculos e services Community devem observar a trava como
         * false.
         */
        parametrosGlobais.setPermiteAjusteAgregadoSemBaselineProduto(false);
        parametrosGlobais.setPermiteAjusteAgregadoSemBaselineLocation(false);
        parametrosGlobais.setRemessasConsomemDisponibilidadeNoPrimeiroPeriodo(false);
        parametrosGlobais.setQuantidadesEmPedidosRepresentamSaldoRestante(false);
        parametrosGlobais.setConsideraPedidosBacklog(false);
        parametrosGlobais.setNumeroDiasProdutoNovo(0);
        parametrosGlobais.setNumeroDiasLocationNova(0);
        parametrosGlobais.setDiasHistoricosCurva(0);
        parametrosGlobais.setCalculaCustoEstoque(false);
        /*
         * OTB, Deployment e Pricing permanecem como campos transicionais de
         * schema nesta entidade compartilhada, mas nao possuem runtime
         * Community. A leitura/salvamento do registro unico deve limpar esses
         * snapshots para que nenhum calculo Community observe configuracao
         * financeira, comercial ou de distribuicao por acidente.
         */
        parametrosGlobais.setDiasHistoricosDeployment(null);
        parametrosGlobais.setUnidadeMedidaPadraoDeployment(null);
        parametrosGlobais.setUnidadeMedidaPadraoPricing(null);
        parametrosGlobais.setTamanhoBucketOTB(null);
        parametrosGlobais.setHorizonteOTBDias(null);
        parametrosGlobais.setUnidadeMedidaPadraoCapacidadeLogisticaPeso(null);
        parametrosGlobais.setUnidadeMedidaPadraoCapacidadeLogisticaVolume(null);
        parametrosGlobais.setLogDataUploadPedidos(false);
        parametrosGlobais.setLogDataUploadRemessas(false);

        return parametrosGlobais;

    }

}
