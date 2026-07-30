package com.opsfactor.community.web.restcontroller.configuration;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.service.UnidadeMedidaService;
import com.opsfactor.community.capability.configuration.facade.ParametrosGlobaisFacade;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.MetodosUtilidade;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * API Community de Global Parameters.
 *
 * <p>O front-end compartilhado pode continuar conhecendo campos Enterprise,
 * mas esta borda publica somente os parametros operacionais aceitos no
 * Community: sell-out como documento historico, forecast estatistico basico,
 * unidades padrao e horizonte operacional. Qualquer campo ligado a sell-in,
 * pedidos, normalizacao por stockout/outlier/evento, frota/logistica, curva ou
 * material novo deve falhar explicitamente antes de alterar a entidade.</p>
 */
@RestController
public class ParametrosGlobaisController {

    /**
     * Fachada Community que falha antes do salvamento quando o payload tenta
     * ativar parametros globais Enterprise.
     */
    @Autowired
    private ParametrosGlobaisFacade parametrosGlobaisFrontService;

    /**
     * Service de master data usado apenas para resolver as unidades padrao de
     * Demand Planning e Supply Planning aceitas no Community.
     */
    @Autowired
    private UnidadeMedidaService unidadeMedidaService;

    /**
     * Policy de edicao para DTO, validacao e saneamento especifico da borda
     * HTTP. O Enterprise pode sobrepor este bean com `@Primary` sem duplicar as
     * rotas do controller.
     */
    @Autowired
    private ParametrosGlobaisControllerPolicy parametrosGlobaisControllerPolicy;

    /**
     * Retorna os parametros globais Community publicados para a SPA.
     */
    @GetMapping(value = "api/secured/configs/parameters", produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured("ROLE_ADMIN")
    public ParametrosGlobaisDTO getParametrosGlobaisApi() {

        /*
         * A policy converte a entidade ampla para o DTO Community, ocultando ou
         * neutralizando campos Enterprise que podem existir no schema legado.
         */
        return parametrosGlobaisControllerPolicy.criaParametrosGlobaisDTO(
                parametrosGlobaisFrontService.getParametrosGlobais());

    }

    /**
     * Atualiza os parametros globais Community aceitos pelo policy publico.
     */
    @PostMapping(value = "api/secured/configs/parameters", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured("ROLE_ADMIN")
    public ParametrosGlobaisDTO updateParametrosGlobaisApi(@RequestBody ParametrosGlobaisDTO dto) {

        /*
         * Validamos o DTO antes de copiar qualquer campo para a entidade para
         * evitar persistencia parcial de opcoes Enterprise bloqueadas.
         */
        parametrosGlobaisControllerPolicy.validaParametrosGlobaisDTO(dto);

        ParametrosGlobais parametrosGlobais = parametrosGlobaisFrontService.getParametrosGlobais();

        parametrosGlobais.setTimeZone(dto.getTimeZone());
        parametrosGlobais.setModeloCadastroProdutoLocation(parseEnum(
                ParametrosGlobais.ModeloCadastroProdutoLocation.class,
                dto.getModeloCadastroProdutoLocation(),
                parametrosGlobais.getModeloCadastroProdutoLocation()));
        parametrosGlobais.setPeriodosHistoricosTelaDP(
                dto.getDemandPlanningHistoricalDisplayPeriods());
        parametrosGlobais.setHorizonteForecastDias(dto.getHorizonteForecastDias());
        parametrosGlobais.setDiasHistoricosForecastEstatistico(dto.getDiasHistoricosForecastEstatistico());
        parametrosGlobais.setDpArredondaParaUnidadeVenda(dto.getDpArredondaParaUnidadeVenda());
        /*
         * Este default global e consumido pela projection de parametros de
         * Demand Planning quando o cluster nao registra override. A rota
         * preserva o Boolean nullable para que ausencia continue usando o
         * getter efetivo do aggregate em registros transicionais.
         */
        parametrosGlobais.setDpGeraForecastParaDescontinuados(
                dto.getDemandPlanningGenerateForecastForDiscontinuedMaterials());
        /*
         * A policy Community rejeita a ativacao destas flags antes deste
         * ponto e o service Community as neutraliza na leitura/salvamento.
         * A copia permanece no controller compartilhado para que o overlay
         * Enterprise possa reabrir somente este comportamento de ajuste
         * agregado, sem duplicar a rota HTTP.
         */
        parametrosGlobais.setPermiteAjusteAgregadoSemBaselineProduto(
                dto.getPermiteAjusteAgregadoSemBaselineProduto());
        parametrosGlobais.setPermiteAjusteAgregadoSemBaselineLocation(
                dto.getPermiteAjusteAgregadoSemBaselineLocation());
        parametrosGlobais.setDiasHorizonteCongelado(dto.getDiasHorizonteCongelado());
        parametrosGlobais.setUnidadeMedidaPadraoDP(resolveUnidadeMedida(dto.getUnidadeMedidaPadraoDP()));
        parametrosGlobais.setUnidadeMedidaPadraoSNP(resolveUnidadeMedida(dto.getUnidadeMedidaPadraoSNP()));
        /*
         * Safety stock Community pode escolher se a demanda indireta compoe a
         * demanda usada no calculo. Locations continuam podendo sobrescrever
         * esse default: null herda o global e false permanece uma escolha
         * local explicita, portanto este setter nao toca a entidade Location.
         */
        parametrosGlobais.setIncluiDemandaIndiretaNoSafetyStock(
                dto.getSafetyStockConsiderIndirectDemand());
        /*
         * A policy Community rejeita estes ids antes deste ponto. O controller
         * os copia aqui para que o overlay Enterprise possa reabrir apenas as
         * UOMs globais que o loader de capacidade logistica ja consome, usando
         * a mesma resolucao validada de UnidadeMedida dos demais campos.
         */
        parametrosGlobais.setUnidadeMedidaPadraoCapacidadeLogisticaPeso(
                resolveUnidadeMedida(dto.getUnidadeMedidaPadraoCapacidadeLogisticaPeso()));
        parametrosGlobais.setUnidadeMedidaPadraoCapacidadeLogisticaVolume(
                resolveUnidadeMedida(dto.getUnidadeMedidaPadraoCapacidadeLogisticaVolume()));
        /*
         * Esta preferencia controla somente a apresentacao do grafo tecnico de
         * Low Level Code. Diferentemente dos campos de capacidade de frota,
         * ela ja e consumida pelo servico Community para decidir se nodes de
         * cliente final permanecem visiveis na resposta; por isso a borda
         * publica a copia direta sem criar outro recorte ou projection.
         */
        parametrosGlobais.setExibeLocationsClienteFinalLowLevelCode(
                dto.getExibeLocationsClienteFinalLowLevelCode());
        /*
         * O Community rejeita estas duas interpretacoes de pedido/remessa na
         * policy antes desta copia e continua saneando-as no service. A rota
         * compartilhada as aplica para que o overlay Enterprise possa expor o
         * unico consumidor ja migrado: PedidosAbertosProjection do optimizer.
         * Isso nao amplia o caminho heuristico.
         */
        parametrosGlobais.setRemessasConsomemDisponibilidadeNoPrimeiroPeriodo(
                dto.getRemessasConsomemDisponibilidadeNoPrimeiroPeriodo());
        parametrosGlobais.setQuantidadesEmPedidosRepresentamSaldoRestante(
                dto.getQuantidadesEmPedidosRepresentamSaldoRestante());
        /*
         * O Community rejeita a janela acima de zero na policy antes desta
         * copia. O campo continua no aggregate compartilhado para que o
         * overlay Enterprise possa reabrir a regra de New Materials ja
         * consumida no pos-processamento de Demand Planning, sem uma tabela
         * ou rota paralela.
         */
        parametrosGlobais.setNumeroDiasProdutoNovo(dto.getNumeroDiasMaterialNovo());
        parametrosGlobaisControllerPolicy.aplicaParametrosEdicao(parametrosGlobais, dto);

        return parametrosGlobaisControllerPolicy.criaParametrosGlobaisDTO(
                parametrosGlobaisFrontService.saveParametrosGlobais(parametrosGlobais));

    }

    /**
     * Resolve a unidade de medida opcional enviada pelo front compartilhado.
     * Ausencia ou string em branco significam "sem unidade padrao definida" e
     * nao devem acionar lookup de master data.
     */
    @Nullable
    private UnidadeMedida resolveUnidadeMedida(@Nullable String unidadeMedidaId) {

        if (unidadeMedidaId == null || unidadeMedidaId.trim().isEmpty()) {
            return null;
        }

        return unidadeMedidaService.getUnidadeMedida(unidadeMedidaId);

    }

    private static <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value, T fallback) {

        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        /*
         * Mantem compatibilidade com payloads antigos que enviam enum.name()
         * e tambem aceita os labels JSON usados pelo front compartilhado.
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

    /**
     * DTO explicito da API Community.
     *
     * <p>Alguns campos Enterprise permanecem aqui porque o front compartilhado
     * pode envia-los ou espera-los no contrato HTTP. O metodo
     * {@link #fromCommunity(ParametrosGlobais)} sempre retorna esses campos
     * desligados, e {@link ParametrosGlobaisControllerPolicy#validaParametrosGlobaisDTO(ParametrosGlobaisDTO)}
     * rejeita qualquer tentativa de ativa-los na edicao Community.</p>
     */
    public static class ParametrosGlobaisDTO {
        private Long id;
        private String timeZone;
        private String modeloCadastroProdutoLocation;
        private String tipoDocumentoVenda;
        /**
         * Número de períodos históricos mostrados pela tela de Demand
         * Planning. Este parâmetro afeta somente a janela de apresentação,
         * nunca a série ou o cálculo de forecast.
         */
        private Integer demandPlanningHistoricalDisplayPeriods;
        private Integer horizonteForecastDias;
        private String modeloDemandaBase;
        private Integer diasHistoricosDoh;
        private Integer diasHistoricosDohStockout;
        private String modeloNormalizacao;
        private Integer diasHistoricosNormalizacao;
        private Double percentilOutliersVenda;
        private Integer diasHistoricosForecastEstatistico;
        private Boolean dpArredondaParaUnidadeVenda;
        /**
         * Default global Community para gerar forecast em materiais
         * descontinuados. Um valor nulo no parametro por cluster herda esta
         * escolha; ele nao se relaciona ao uso de historico de DFUs inativas.
         */
        private Boolean demandPlanningGenerateForecastForDiscontinuedMaterials;
        private Boolean permiteAjusteAgregadoSemBaselineProduto;
        private Boolean permiteAjusteAgregadoSemBaselineLocation;
        private Integer diasHorizonteCongelado;
        private String unidadeMedidaPadraoDP;
        /**
         * Default global Community para compor demanda indireta no safety
         * stock. O override nullable por Location nao e serializado por esta
         * rota e conserva sua precedencia no dominio.
         */
        private Boolean safetyStockConsiderIndirectDemand;
        private Boolean remessasConsomemDisponibilidadeNoPrimeiroPeriodo;
        private String unidadeMedidaPadraoSNP;
        private String unidadeMedidaPadraoCapacidadeLogisticaPeso;
        private String unidadeMedidaPadraoCapacidadeLogisticaVolume;
        /**
         * Controla a visibilidade de locations de cliente final no grafo de
         * Low Level Code. O calculo da malha e das DFUs permanece inalterado.
         */
        private Boolean exibeLocationsClienteFinalLowLevelCode;
        private Integer diasHistoricosCurva;
        /** Campo Enterprise: janela de tratamento de material novo. */
        private Integer numeroDiasMaterialNovo;
        private Boolean quantidadesEmPedidosRepresentamSaldoRestante;

        public static ParametrosGlobaisDTO fromCommunity(ParametrosGlobais parametrosGlobais) {

            ParametrosGlobaisDTO dto = new ParametrosGlobaisDTO();
            dto.setId(parametrosGlobais.getId());
            dto.setTimeZone(parametrosGlobais.getTimeZone());
            dto.setModeloCadastroProdutoLocation(parametrosGlobais.getModeloCadastroProdutoLocation().name());
            dto.setTipoDocumentoVenda(Constantes.TipoDocumentoVenda.SELLOUT.name());
            dto.setDemandPlanningHistoricalDisplayPeriods(
                    parametrosGlobais.getPeriodosHistoricosTelaDP());
            dto.setHorizonteForecastDias(parametrosGlobais.getHorizonteForecastDias());
            dto.setModeloDemandaBase(Constantes.DPModeloDemandaBase.DESATIVADO.name());
            dto.setDiasHistoricosDoh(0);
            dto.setDiasHistoricosDohStockout(0);
            dto.setModeloNormalizacao(Constantes.DPModeloNormalizacao.DESATIVADO.name());
            dto.setDiasHistoricosNormalizacao(Constantes.DP_PADRAO_DIAS_NORMALIZACAO);
            dto.setPercentilOutliersVenda(Constantes.DP_PADRAO_PERCENTIL_OUTLIERS_VENDA);
            dto.setDiasHistoricosForecastEstatistico(parametrosGlobais.getDiasHistoricosForecastEstatistico());
            dto.setDpArredondaParaUnidadeVenda(parametrosGlobais.getDpArredondaParaUnidadeVenda());
            dto.setDemandPlanningGenerateForecastForDiscontinuedMaterials(
                    parametrosGlobais.getDpGeraForecastParaDescontinuados());
            dto.setPermiteAjusteAgregadoSemBaselineProduto(false);
            dto.setPermiteAjusteAgregadoSemBaselineLocation(false);
            dto.setDiasHorizonteCongelado(parametrosGlobais.getDiasHorizonteCongelado());
            dto.setUnidadeMedidaPadraoDP(resolveUnidadeMedidaId(parametrosGlobais.getUnidadeMedidaPadraoDP()));
            dto.setSafetyStockConsiderIndirectDemand(
                    parametrosGlobais.getIncluiDemandaIndiretaNoSafetyStock());
            dto.setRemessasConsomemDisponibilidadeNoPrimeiroPeriodo(false);
            dto.setUnidadeMedidaPadraoSNP(resolveUnidadeMedidaId(parametrosGlobais.getUnidadeMedidaPadraoSNP()));
            dto.setUnidadeMedidaPadraoCapacidadeLogisticaPeso(null);
            dto.setUnidadeMedidaPadraoCapacidadeLogisticaVolume(null);
            dto.setExibeLocationsClienteFinalLowLevelCode(
                    parametrosGlobais.getExibeLocationsClienteFinalLowLevelCode());
            dto.setDiasHistoricosCurva(0);
            dto.setNumeroDiasMaterialNovo(0);
            dto.setQuantidadesEmPedidosRepresentamSaldoRestante(false);
            return dto;

        }

        /**
         * Converte a entidade opcional para o id esperado pelo contrato HTTP.
         * Null aqui representa parametro Community ainda nao configurado.
         */
        @Nullable
        private static String resolveUnidadeMedidaId(@Nullable UnidadeMedida unidadeMedida) {

            return unidadeMedida == null ? null : unidadeMedida.getId();

        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTimeZone() { return timeZone; }
        public void setTimeZone(String timeZone) { this.timeZone = timeZone; }
        public String getModeloCadastroProdutoLocation() { return modeloCadastroProdutoLocation; }
        public void setModeloCadastroProdutoLocation(String modeloCadastroProdutoLocation) { this.modeloCadastroProdutoLocation = modeloCadastroProdutoLocation; }
        public String getTipoDocumentoVenda() { return tipoDocumentoVenda; }
        public void setTipoDocumentoVenda(String tipoDocumentoVenda) { this.tipoDocumentoVenda = tipoDocumentoVenda; }
        public Integer getDemandPlanningHistoricalDisplayPeriods() { return demandPlanningHistoricalDisplayPeriods; }
        public void setDemandPlanningHistoricalDisplayPeriods(Integer demandPlanningHistoricalDisplayPeriods) { this.demandPlanningHistoricalDisplayPeriods = demandPlanningHistoricalDisplayPeriods; }
        public Integer getHorizonteForecastDias() { return horizonteForecastDias; }
        public void setHorizonteForecastDias(Integer horizonteForecastDias) { this.horizonteForecastDias = horizonteForecastDias; }
        public String getModeloDemandaBase() { return modeloDemandaBase; }
        public void setModeloDemandaBase(String modeloDemandaBase) { this.modeloDemandaBase = modeloDemandaBase; }
        public Integer getDiasHistoricosDoh() { return diasHistoricosDoh; }
        public void setDiasHistoricosDoh(Integer diasHistoricosDoh) { this.diasHistoricosDoh = diasHistoricosDoh; }
        public Integer getDiasHistoricosDohStockout() { return diasHistoricosDohStockout; }
        public void setDiasHistoricosDohStockout(Integer diasHistoricosDohStockout) { this.diasHistoricosDohStockout = diasHistoricosDohStockout; }
        public String getModeloNormalizacao() { return modeloNormalizacao; }
        public void setModeloNormalizacao(String modeloNormalizacao) { this.modeloNormalizacao = modeloNormalizacao; }
        public Integer getDiasHistoricosNormalizacao() { return diasHistoricosNormalizacao; }
        public void setDiasHistoricosNormalizacao(Integer diasHistoricosNormalizacao) { this.diasHistoricosNormalizacao = diasHistoricosNormalizacao; }
        public Double getPercentilOutliersVenda() { return percentilOutliersVenda; }
        public void setPercentilOutliersVenda(Double percentilOutliersVenda) { this.percentilOutliersVenda = percentilOutliersVenda; }
        public Integer getDiasHistoricosForecastEstatistico() { return diasHistoricosForecastEstatistico; }
        public void setDiasHistoricosForecastEstatistico(Integer diasHistoricosForecastEstatistico) { this.diasHistoricosForecastEstatistico = diasHistoricosForecastEstatistico; }
        public Boolean getDpArredondaParaUnidadeVenda() { return dpArredondaParaUnidadeVenda; }
        public void setDpArredondaParaUnidadeVenda(Boolean dpArredondaParaUnidadeVenda) { this.dpArredondaParaUnidadeVenda = dpArredondaParaUnidadeVenda; }
        public Boolean getDemandPlanningGenerateForecastForDiscontinuedMaterials() { return demandPlanningGenerateForecastForDiscontinuedMaterials; }
        public void setDemandPlanningGenerateForecastForDiscontinuedMaterials(Boolean demandPlanningGenerateForecastForDiscontinuedMaterials) { this.demandPlanningGenerateForecastForDiscontinuedMaterials = demandPlanningGenerateForecastForDiscontinuedMaterials; }
        public Boolean getPermiteAjusteAgregadoSemBaselineProduto() { return permiteAjusteAgregadoSemBaselineProduto; }
        public void setPermiteAjusteAgregadoSemBaselineProduto(Boolean permiteAjusteAgregadoSemBaselineProduto) { this.permiteAjusteAgregadoSemBaselineProduto = permiteAjusteAgregadoSemBaselineProduto; }
        public Boolean getPermiteAjusteAgregadoSemBaselineLocation() { return permiteAjusteAgregadoSemBaselineLocation; }
        public void setPermiteAjusteAgregadoSemBaselineLocation(Boolean permiteAjusteAgregadoSemBaselineLocation) { this.permiteAjusteAgregadoSemBaselineLocation = permiteAjusteAgregadoSemBaselineLocation; }
        public Integer getDiasHorizonteCongelado() { return diasHorizonteCongelado; }
        public void setDiasHorizonteCongelado(Integer diasHorizonteCongelado) { this.diasHorizonteCongelado = diasHorizonteCongelado; }
        public String getUnidadeMedidaPadraoDP() { return unidadeMedidaPadraoDP; }
        public void setUnidadeMedidaPadraoDP(String unidadeMedidaPadraoDP) { this.unidadeMedidaPadraoDP = unidadeMedidaPadraoDP; }
        public Boolean getSafetyStockConsiderIndirectDemand() { return safetyStockConsiderIndirectDemand; }
        public void setSafetyStockConsiderIndirectDemand(Boolean safetyStockConsiderIndirectDemand) { this.safetyStockConsiderIndirectDemand = safetyStockConsiderIndirectDemand; }
        public Boolean getRemessasConsomemDisponibilidadeNoPrimeiroPeriodo() { return remessasConsomemDisponibilidadeNoPrimeiroPeriodo; }
        public void setRemessasConsomemDisponibilidadeNoPrimeiroPeriodo(Boolean remessasConsomemDisponibilidadeNoPrimeiroPeriodo) { this.remessasConsomemDisponibilidadeNoPrimeiroPeriodo = remessasConsomemDisponibilidadeNoPrimeiroPeriodo; }
        public String getUnidadeMedidaPadraoSNP() { return unidadeMedidaPadraoSNP; }
        public void setUnidadeMedidaPadraoSNP(String unidadeMedidaPadraoSNP) { this.unidadeMedidaPadraoSNP = unidadeMedidaPadraoSNP; }
        public String getUnidadeMedidaPadraoCapacidadeLogisticaPeso() { return unidadeMedidaPadraoCapacidadeLogisticaPeso; }
        public void setUnidadeMedidaPadraoCapacidadeLogisticaPeso(String unidadeMedidaPadraoCapacidadeLogisticaPeso) { this.unidadeMedidaPadraoCapacidadeLogisticaPeso = unidadeMedidaPadraoCapacidadeLogisticaPeso; }
        public String getUnidadeMedidaPadraoCapacidadeLogisticaVolume() { return unidadeMedidaPadraoCapacidadeLogisticaVolume; }
        public void setUnidadeMedidaPadraoCapacidadeLogisticaVolume(String unidadeMedidaPadraoCapacidadeLogisticaVolume) { this.unidadeMedidaPadraoCapacidadeLogisticaVolume = unidadeMedidaPadraoCapacidadeLogisticaVolume; }
        public Boolean getExibeLocationsClienteFinalLowLevelCode() { return exibeLocationsClienteFinalLowLevelCode; }
        public void setExibeLocationsClienteFinalLowLevelCode(Boolean exibeLocationsClienteFinalLowLevelCode) { this.exibeLocationsClienteFinalLowLevelCode = exibeLocationsClienteFinalLowLevelCode; }
        public Integer getDiasHistoricosCurva() { return diasHistoricosCurva; }
        public void setDiasHistoricosCurva(Integer diasHistoricosCurva) { this.diasHistoricosCurva = diasHistoricosCurva; }
        public Integer getNumeroDiasMaterialNovo() { return numeroDiasMaterialNovo; }
        public void setNumeroDiasMaterialNovo(Integer numeroDiasMaterialNovo) { this.numeroDiasMaterialNovo = numeroDiasMaterialNovo; }
        public Boolean getQuantidadesEmPedidosRepresentamSaldoRestante() { return quantidadesEmPedidosRepresentamSaldoRestante; }
        public void setQuantidadesEmPedidosRepresentamSaldoRestante(Boolean quantidadesEmPedidosRepresentamSaldoRestante) { this.quantidadesEmPedidosRepresentamSaldoRestante = quantidadesEmPedidosRepresentamSaldoRestante; }
    }

}
