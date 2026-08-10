package com.opsfactor.community.web.restcontroller.configuration;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.forecast.configuration.DemandPlanningModelCatalog;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.MetodosUtilidade;
import org.springframework.stereotype.Service;

/**
 * Politica Community da API de Global Parameters.
 *
 * <p>O controller permanece unico e reutilizavel pelo Enterprise. A diferenca
 * entre edicoes fica nesta policy: o Community publica, valida e normaliza
 * apenas sell-out e parametros operacionais basicos; o Enterprise pode registrar
 * uma policy {@code @Primary} para reabrir somente campos com runtime privado
 * real.</p>
 */
@Service
public class ParametrosGlobaisControllerPolicy {

    /**
     * Converte a entidade para o DTO publico da edicao Community.
     */
    public ParametrosGlobaisController.ParametrosGlobaisDTO criaParametrosGlobaisDTO(
            ParametrosGlobais parametrosGlobais) {

        return ParametrosGlobaisController.ParametrosGlobaisDTO.fromCommunity(parametrosGlobais);

    }

    /**
     * Valida o payload antes de o controller copiar campos para a entidade.
     */
    public void validaParametrosGlobaisDTO(
            ParametrosGlobaisController.ParametrosGlobaisDTO dto) {

        if (dto == null) {
            throw new IllegalArgumentException("Global parameters payload must not be null");
        }

        validaTipoDocumentoVenda(dto.getTipoDocumentoVenda());
        validaModeloDemandaBase(dto.getModeloDemandaBase());
        validaParametrosOperacionaisCommunity(dto);
        validaBooleanEnterpriseDesabilitado(
                dto.getDpArredondaParaUnidadeVenda(),
                "Forecast rounding to sales UOM");
        validaParametrosLimpezaHistorico(dto);
        validaInteiroDesabilitado(dto.getDiasHistoricosDoh(), "Stockout normalization DOH");
        validaInteiroDesabilitado(dto.getDiasHistoricosDohStockout(), "Stockout normalization threshold");
        validaBooleanEnterpriseDesabilitado(
                dto.getPermiteAjusteAgregadoSemBaselineProduto(),
                "Aggregated material adjustment without baseline");
        validaBooleanEnterpriseDesabilitado(
                dto.getPermiteAjusteAgregadoSemBaselineLocation(),
                "Aggregated location adjustment without baseline");
        validaBooleanEnterpriseDesabilitado(
                dto.getRemessasConsomemDisponibilidadeNoPrimeiroPeriodo(),
                "Deliveries consuming first-period availability");
        validaTextoEnterpriseDesabilitado(
                dto.getUnidadeMedidaPadraoCapacidadeLogisticaPeso(),
                "Fleet/logistics weight capacity unit");
        validaTextoEnterpriseDesabilitado(
                dto.getUnidadeMedidaPadraoCapacidadeLogisticaVolume(),
                "Fleet/logistics volume capacity unit");
        validaInteiroDesabilitado(dto.getDiasHistoricosCurva(), "Curve calculation");
        validaInteiroDesabilitado(dto.getNumeroDiasMaterialNovo(), "New material treatment");
        validaBooleanEnterpriseDesabilitado(
                dto.getQuantidadesEmPedidosRepresentamSaldoRestante(),
                "Sales order remaining-balance interpretation");

    }

    /**
     * Aplica defaults/saneamento da edicao antes do front service salvar.
     */
    public void aplicaParametrosEdicao(
            ParametrosGlobais parametrosGlobais,
            ParametrosGlobaisController.ParametrosGlobaisDTO dto) {

        parametrosGlobais.setTipoDocumentoVenda(Constantes.TipoDocumentoVenda.SELLOUT);
        parametrosGlobais.setDpArredondaParaUnidadeVenda(false);
        /*
         * Pedidos transacionais/backlog pertencem ao Enterprise. Este setter
         * limpa valores antigos eventualmente persistidos antes do recorte
         * Community, evitando que uma simples alteracao de parametro global
         * fique bloqueada por dado legado que a propria UI nao exibe.
         */
        parametrosGlobais.setConsideraPedidosBacklog(false);

    }

    /**
     * Valida campos operacionais que continuam editaveis no Community.
     *
     * <p>Esses campos nao representam uma capability Enterprise; por isso nao
     * devem retornar {@link RequiresEnterpriseVersionException}. Valores nulos
     * continuam usando defaults da entidade/model service, mas valores
     * preenchidos precisam ser semanticamente validos antes de serem copiados
     * pelo controller para {@link ParametrosGlobais}.</p>
     */
    private void validaParametrosOperacionaisCommunity(
            ParametrosGlobaisController.ParametrosGlobaisDTO dto) {

        validaInteiroPositivo(
                dto.getDemandPlanningHistoricalDisplayPeriods(),
                "Global Parameters Demand Planning historical display periods");
        validaInteiroPositivo(
                dto.getHorizonteForecastDias(),
                "Global Parameters forecast horizon");
        validaInteiroPositivo(
                dto.getDiasHistoricosForecastEstatistico(),
                "Global Parameters statistical forecast history window");
        validaInteiroNaoNegativo(
                dto.getDiasHorizonteCongelado(),
                "Global Parameters frozen horizon");

    }

    /**
     * Valida o tipo de documento historico permitido na edicao.
     */
    protected void validaTipoDocumentoVenda(String tipoDocumentoVenda) {

        Constantes.TipoDocumentoVenda tipoDocumentoVendaSelecionado = parseEnum(
                Constantes.TipoDocumentoVenda.class,
                tipoDocumentoVenda,
                Constantes.TipoDocumentoVenda.SELLOUT);

        if (!DemandPlanningModelCatalog.isTipoDocumentoHistoricoCommunity(tipoDocumentoVendaSelecionado)) {
            throw new RequiresEnterpriseVersionException("Sell-in or sales-order demand source");
        }

    }

    /**
     * Valida que o tratamento de stockout historico continue inativo.
     */
    protected void validaModeloDemandaBase(String modeloDemandaBase) {

        Constantes.DPModeloDemandaBase modeloDemandaBaseSelecionado = parseEnum(
                Constantes.DPModeloDemandaBase.class,
                modeloDemandaBase,
                Constantes.DPModeloDemandaBase.DESATIVADO);

        if (!Constantes.DPModeloDemandaBase.DESATIVADO.equals(modeloDemandaBaseSelecionado)) {
            throw new RequiresEnterpriseVersionException("Stockout historical demand normalization");
        }

    }

    /**
     * Valida que limpeza por outlier/campanha continue inativa.
     */
    protected void validaModeloNormalizacao(String modeloNormalizacao) {

        Constantes.DPModeloNormalizacao modeloNormalizacaoSelecionado = parseEnum(
                Constantes.DPModeloNormalizacao.class,
                modeloNormalizacao,
                Constantes.DPModeloNormalizacao.DESATIVADO);

        if (!Constantes.DPModeloNormalizacao.DESATIVADO.equals(modeloNormalizacaoSelecionado)) {
            throw new RequiresEnterpriseVersionException("Outlier, percentile or campaign demand normalization");
        }

    }

    /**
     * Valida a familia de limpeza historica/outlier smoothing.
     *
     * <p>No Community, a familia inteira deve permanecer neutra. O Enterprise
     * sobrepoe este metodo para liberar os modelos privados ja migrados, como
     * {@code PERCENTIS} e {@code CAMPANHA}, sem expor essa regra no controller
     * Community.</p>
     */
    protected void validaParametrosLimpezaHistorico(
            ParametrosGlobaisController.ParametrosGlobaisDTO dto) {

        validaModeloNormalizacao(dto.getModeloNormalizacao());
        validaInteiroIgual(
                dto.getDiasHistoricosNormalizacao(),
                Constantes.DP_PADRAO_DIAS_NORMALIZACAO,
                "Outlier normalization window");
        validaDoubleIgual(
                dto.getPercentilOutliersVenda(),
                Constantes.DP_PADRAO_PERCENTIL_OUTLIERS_VENDA,
                "Outlier percentile");

    }

    protected void validaBooleanEnterpriseDesabilitado(Boolean valor, String featureName) {

        if (Boolean.TRUE.equals(valor)) {
            throw new RequiresEnterpriseVersionException(featureName);
        }

    }

    protected void validaTextoEnterpriseDesabilitado(String valor, String featureName) {

        if (valor != null && !valor.trim().isEmpty()) {
            throw new RequiresEnterpriseVersionException(featureName);
        }

    }

    protected void validaInteiroDesabilitado(Integer valor, String featureName) {

        /*
         * Campos Enterprise desabilitados aceitam somente ausencia ou zero
         * explicito. Valor negativo nao tem semantica Community e tambem nao
         * deve ser ignorado silenciosamente pelo controller, pois esses campos
         * nao sao copiados para a entidade nesta edicao.
         */
        if (valor != null && valor != 0) {
            throw new RequiresEnterpriseVersionException(featureName);
        }

    }

    protected void validaInteiroIgual(Integer valor, int valorCommunity, String featureName) {

        if (valor != null && valor != valorCommunity) {
            throw new RequiresEnterpriseVersionException(featureName);
        }

    }

    protected void validaDoubleIgual(Double valor, double valorCommunity, String featureName) {

        if (valor != null && Double.compare(valor, valorCommunity) != 0) {
            throw new RequiresEnterpriseVersionException(featureName);
        }

    }

    protected void validaInteiroPositivo(Integer valor, String fieldName) {

        if (valor != null && valor <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive.");
        }

    }

    protected void validaInteiroNaoNegativo(Integer valor, String fieldName) {

        if (valor != null && valor < 0) {
            throw new IllegalArgumentException(fieldName + " must be zero or positive.");
        }

    }

    protected static <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value, T fallback) {

        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        /*
         * Global Parameters ainda usa campos String por compatibilidade com o
         * front legado/compartilhado. Aceitamos tanto o name() tecnico
         * (`SELLOUT`) quanto o label JSON publicado para a SPA (`Sell-out`),
         * mas mantemos erro explicito para qualquer valor desconhecido.
         */
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException ignored) {
            /*
             * Se nao for o name() tecnico, tentamos o label publico
             * @JsonProperty. O erro final continua explicito para payloads
             * realmente invalidos.
             */
        }

        return MetodosUtilidade.getOptionalValorEnumDeJsonProperty(enumClass, value)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid enum value " + value + " for " + enumClass.getSimpleName()));

    }

}
