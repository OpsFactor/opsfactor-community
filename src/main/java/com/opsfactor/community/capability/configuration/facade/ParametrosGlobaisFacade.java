package com.opsfactor.community.capability.configuration.facade;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import com.opsfactor.community.capability.demandplanning.forecast.configuration.DemandPlanningModelCatalog;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service de fronteira para a tela de Global Parameters Community.
 *
 * <p>A responsabilidade aqui e falhar cedo quando uma tela compartilhada,
 * integracao manual ou payload legado tenta ativar capacidades Enterprise. O
 * service de modelo ainda saneia valores antigos persistidos, mas esta camada
 * preserva uma mensagem explicita de edicao Enterprise antes do salvamento.</p>
 */
@Service
public class ParametrosGlobaisFacade {

    /**
     * Service de modelo que centraliza leitura, saneamento defensivo e
     * persistencia da entidade unica de parametros globais.
     *
     * <p>A injecao permanece explicita porque esta fachada e a borda que falha
     * antes do salvamento quando o payload tenta ativar capacidade Enterprise.</p>
     */
    @Autowired
    private ParametrosGlobaisService parametrosGlobaisService;

    /**
     * Retorna os parametros globais ja saneados pelo service de modelo.
     */
    public ParametrosGlobais getParametrosGlobais() {

        return parametrosGlobaisService.getParametrosGlobais();

    }

    /**
     * Valida o recorte Community antes de persistir alteracoes vindas da tela
     * de Global Parameters.
     */
    public ParametrosGlobais saveParametrosGlobais(ParametrosGlobais parametrosGlobais) {

        validaParametrosGlobaisCommunity(parametrosGlobais);
        parametrosGlobais = parametrosGlobaisService.saveParametrosGlobais(parametrosGlobais);
        return parametrosGlobais;

    }

    /**
     * A tela/API Community deve aceitar somente parametros globais neutros para
     * capacidades Enterprise. O `ParametrosGlobaisService` tambem saneia a
     * entidade na leitura e no salvamento para proteger bancos legados, mas a
     * fronteira de front service precisa falhar explicitamente quando uma tela
     * antiga ou um payload manual tenta ativar uma opcao bloqueada.
     */
    private void validaParametrosGlobaisCommunity(ParametrosGlobais parametrosGlobais) {

        if (parametrosGlobais == null) {
            /*
             * Payload nulo e erro estrutural da chamada de Global Parameters,
             * nao ausencia de uma capability Enterprise. Falhar aqui evita NPE
             * nos getters da entidade e impede que repository/model service
             * seja acionado com uma configuracao inexistente.
             */
            throw new IllegalArgumentException("Global Parameters payload is required.");
        }

        if (!DemandPlanningModelCatalog.isTipoDocumentoHistoricoCommunity(
                parametrosGlobais.getTipoDocumentoVenda())) {
            throw new RequiresEnterpriseVersionException("Global Parameters sales document type");
        }

        if (!parametrosGlobais.getModeloDemandaBase().equals(Constantes.DPModeloDemandaBase.DESATIVADO)) {
            throw new RequiresEnterpriseVersionException("Global Parameters stockout normalization model");
        }

        if (!parametrosGlobais.getModeloNormalizacao().equals(Constantes.DPModeloNormalizacao.DESATIVADO)) {
            throw new RequiresEnterpriseVersionException("Global Parameters outlier/campaign normalization model");
        }

        validaIntegerIgual(
                parametrosGlobais.getDiasHistoricosDoh(),
                Constantes.GLOBAL_PADRAO_DIAS_HISTORICOS_DOH,
                "Global Parameters stockout normalization DOH");
        validaIntegerIgual(
                parametrosGlobais.getDiasHistoricosDohStockout(),
                Constantes.GLOBAL_PADRAO_DIAS_HISTORICOS_DOH_STOCKOUT,
                "Global Parameters stockout normalization threshold");
        validaIntegerIgual(
                parametrosGlobais.getDiasHistoricosNormalizacao(),
                Constantes.DP_PADRAO_DIAS_NORMALIZACAO,
                "Global Parameters outlier normalization window");
        validaDoubleIgual(
                parametrosGlobais.getPercentilOutliersVenda(),
                Constantes.DP_PADRAO_PERCENTIL_OUTLIERS_VENDA,
                "Global Parameters outlier percentile");

        validaFalse(
                parametrosGlobais.getPermiteAjusteAgregadoSemBaselineProduto(),
                "Global Parameters aggregated material adjustment");
        validaFalse(
                parametrosGlobais.getPermiteAjusteAgregadoSemBaselineLocation(),
                "Global Parameters aggregated location adjustment");
        validaFalse(
                parametrosGlobais.getRemessasConsomemDisponibilidadeNoPrimeiroPeriodo(),
                "Global Parameters deliveries consume availability");
        validaFalse(
                parametrosGlobais.getQuantidadesEmPedidosRepresentamSaldoRestante(),
                "Global Parameters transactional order remaining quantity");
        validaFalse(
                parametrosGlobais.isConsideraPedidosBacklogCadastradoAtivo(),
                "Global Parameters transactional order backlog");
        /*
         * O getter operacional de custo de estoque retorna sempre false no
         * Community. A borda de front precisa ler o valor bruto para nao
         * aceitar payload ativando Pricing/custos e depender apenas do
         * saneamento posterior do service de modelo.
         */
        validaFalse(
                Boolean.TRUE.equals(parametrosGlobais.getCalculaCustoEstoqueCadastrado()),
                "Global Parameters inventory cost calculation");

        validaIntegerIgual(
                parametrosGlobais.getNumeroDiasProdutoNovo(),
                0,
                "Global Parameters new material logic");
        validaIntegerIgual(
                parametrosGlobais.getNumeroDiasLocationNova(),
                0,
                "Global Parameters new location logic");

        if (parametrosGlobais.getUnidadeMedidaPadraoCapacidadeLogisticaPesoCadastrado() != null) {
            throw new RequiresEnterpriseVersionException("Global Parameters fleet capacity weight unit");
        }

        if (parametrosGlobais.getUnidadeMedidaPadraoCapacidadeLogisticaVolumeCadastrado() != null) {
            throw new RequiresEnterpriseVersionException("Global Parameters fleet capacity volume unit");
        }

    }

    private void validaFalse(boolean valor, String featureName) {

        if (valor) {
            throw new RequiresEnterpriseVersionException(featureName);
        }

    }

    private void validaIntegerIgual(Integer valor, int valorCommunity, String featureName) {

        if (valor != null && valor != valorCommunity) {
            throw new RequiresEnterpriseVersionException(featureName);
        }

    }

    private void validaDoubleIgual(Double valor, double valorCommunity, String featureName) {

        if (valor == null) {
            return;
        }

        /*
         * Campos numericos bloqueados no Community aceitam apenas ausencia ou
         * o default tecnico documentado para manter payloads saneados
         * idempotentes. `NaN` e infinito nao representam valor vazio nem
         * default: sao conteudo invalido que deve falhar na borda de edicao,
         * antes que o service de modelo tente saneamento defensivo.
         */
        if (!Double.isFinite(valor) || Double.compare(valor, valorCommunity) != 0) {
            throw new RequiresEnterpriseVersionException(featureName);
        }

    }

}
