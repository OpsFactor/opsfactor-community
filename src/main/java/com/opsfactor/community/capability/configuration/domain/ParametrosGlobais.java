package com.opsfactor.community.capability.configuration.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.*;
import java.io.Serializable;

/**
 * Parametros globais funcionais do runtime Community.
 *
 * <p>Esta entidade ainda preserva alguns campos transicionais de schema, mas
 * os getters Community neutralizam configuracoes Enterprise como OTB,
 * deployment, curva, custo de estoque e produtos novos. As APIs de front e os
 * mappers bloqueiam esses campos antes que eles virem configuracao funcional.</p>
 */

@Data
@Entity
public class ParametrosGlobais implements Serializable {

    @Id
    @ColumnDefault("0")
    private Long id;

    // ex : America/Sao_Paulo
    private String timeZone;

    /**
     * Logo customizado do topo da aplicacao em formato data URL base64.
     *
     * <p>A configuracao visual e global, portanto permanece no registro
     * singleton de {@code ParametrosGlobais}. O limite de coluna protege a
     * persistencia contra payloads acidentalmente muito maiores que o limite
     * funcional de upload, validado pela fachada de front.</p>
     */
    @Lob
    @Column(length = 2097152)
    private String topbarLogoDataUrl;

    /**
     * Nome original do arquivo de logo, conservado apenas para apresentacao
     * administrativa.
     */
    private String topbarLogoFileName;

    /**
     * MIME type extraido e validado a partir do data URL persistido.
     */
    private String topbarLogoContentType;
    
    public enum ModeloCadastroProdutoLocation {
        @JsonProperty("No Product/Location Record = Active") ATIVO_SE_AUSENTE,
        @JsonProperty("No Product/Location Record = Inactive") INATIVO_SE_AUSENTE
    }
    @Enumerated(EnumType.ORDINAL)
    private ModeloCadastroProdutoLocation modeloCadastroProdutoLocation;
    
    private Integer diasHistoricosMediaMovel;
    public Integer getDiasHistoricosMediaMovel(){
        if (diasHistoricosMediaMovel == null) diasHistoricosMediaMovel = 4;
        return diasHistoricosMediaMovel;
    }

    /**
     * Número de períodos passados onde a venda histórica será apresentada
     */
    private Integer periodosHistoricosTelaDP;
    public Integer getPeriodosHistoricosTelaDP(){

        if (periodosHistoricosTelaDP == null) periodosHistoricosTelaDP = 120;//4 meses
        return getInteiroOperacionalPositivoCadastrado(
                periodosHistoricosTelaDP,
                "Demand Planning historical display periods");

    }

    // horizonte forecast:
    // semanas   : M1W1(atual) --- M1W2 --- M1W3 --- M1W4 --- M2W1
    // horizonte :     1            2        3        4        5.
    // Configuracoes de acuracia/auto-fit por MAPE pertencem ao Enterprise.
    private Integer horizonteForecastDias; // número de dias futuros considerados no horizonte forecast (incluindo data atual)
    private Boolean dpUsaHistoricoDemandaInativos; // se falso, o histórico de vendas de produtos inativos (nível produto, não produto/location) será ignorado
    private Boolean dpGeraForecastParaDescontinuados; // se falso, o forecast não será gerado para itens descontinuados
    private Integer diasHistoricosDoh; // histórico de vendas passadas usadas para cálculo DOH
    /**
     * Percentil transicional usado apenas pelo modelo Enterprise de limpeza de
     * outliers. No Community, a série {@code vendaHistoricaTratamentoOutliers}
     * é cópia direta da série após tratamento de stockout.
     */
    private Double percentilOutliersVenda;

    /**
     * Janela transicional de DOH para tratamento Enterprise de stockouts. No
     * Community, {@code vendaHistoricaTratamentoStockouts} é cópia direta da
     * venda histórica observada.
     */
    private Integer diasHistoricosDohStockout;
    private Integer diasHistoricosForecastEstatistico; // # dias passados considerados no forecast estatístico (ARIMA/TBATS). padrão 104 = 2 anos, para ter trend+sazonalidade corretos
    private Boolean calculaCustoEstoque; // se TRUE, o valor do estoque é calculado com os recebimentos x vendas, usando as qtdes na base estoque_segmentado (qtd/semana/semana origem) e o valor unitário dos recebimentos da semana origem
    private Integer diasHistoricosCurva; // dias passados usados no cálculo da curva (A/B/C)
    private Integer numeroDiasProdutoNovo; // # de dias desde o início das vendas (na location) em que o produto é considerado como novo
    private Integer numeroDiasLocationNova; // # de dias desde o início das vendas em que a location é considerado como nova
    private Boolean permiteAjusteAgregadoSemBaselineProduto;
    private Boolean permiteAjusteAgregadoSemBaselineLocation;
    private Integer diasHorizonteCongelado;

    // indica se DP deve arredondar forecast para unidade de venda. também configurável nível cluster locations / cluster produtos
    private Boolean dpArredondaParaUnidadeVenda;
    // indica se ajustes dentro do horizonte congelado DP devem ser congelados
    private Boolean bloqueiaAjustesDentroHorizonteCongeladoDp;
    // diz se o log de inputs de usuário é explodido no nível DFU (ex. input na linha do planning book = impacto em N DFUs)
    private Boolean geraLogModificacoesDemandPlanNivelDfu;
    
    @Enumerated(EnumType.ORDINAL)
    private Constantes.SNPTipoEstoque tipoEstoque; // caso backlog seja permitido o estoque projetado poderá ser negativo, efetivamente atrasando a entrega de pedidos
    @Enumerated(EnumType.ORDINAL) // ULTIMO_DIPONIVEL ou PRICING_PLAN
    private Constantes.TamanhoBucket tamanhoBucketEstoqueSegmentado; // ULTIMO_DISPONIVEL = 1 PRICING_PLAN = 2
    
    // identificação de prováveis foras de linha
    private Integer janelaDiasAnteriorForaLinha; // número de dias considerados como baseline para identificação FDLs
    private Integer janelaDiasPosteriorForaLinha; // número de dias avaliados contra baseline para identificação FDLs
    private Float percentualVendasForaLinha; // se a janela posterior tiver venda por período menor que x% da janela anterior, item é fora de linha

    // Tratamento histórico Enterprise: stockouts, outliers/eventos e pricing.
    @Enumerated(EnumType.ORDINAL)
    private Constantes.DPModeloDemandaBase modeloDemandaBase; // Community aceita apenas DESATIVADO; DOH_ESTOQUE_FIM_PERIODO fica bloqueado como Enterprise.
    private Integer diasHistoricosNormalizacao; // janela transicional usada por limpeza Enterprise de outliers/preço; Community não executa limpeza histórica real.
    @Enumerated(EnumType.ORDINAL)
    private Constantes.DPModeloNormalizacao modeloNormalizacao; // Community aceita apenas DESATIVADO; PERCENTIS/CAMPANHA ficam bloqueados como Enterprise.

    // deployment
    private Integer diasHistoricosDeployment;
    
    // plano consolidado
    private Integer numeroDiasPassadosPlanoConsolidado;
    
    // OTB
    private Constantes.TamanhoBucket tamanhoBucketOTB;
    private Integer horizonteOTBDias; // número de dias futuros considerados no horizonte OTB (incluindo data atual)
    
    // clusterização
    private Integer diasHistoricosClusterizacao;
    
    // curvas
    @Enumerated(EnumType.ORDINAL)
    private static Constantes.TipoQuantidadeValor tipoDemandaCurva;
    
    @ManyToOne
    private UnidadeMedida unidadeMedidaPadraoDP;
    @ManyToOne
    private UnidadeMedida unidadeMedidaPadraoSNP;
    @ManyToOne
    private UnidadeMedida unidadeMedidaPadraoDeployment;
    @ManyToOne
    private UnidadeMedida unidadeMedidaPadraoPricing;
        
    // regras para cálculo do estoque de segurança
    private Boolean incluiDemandaIndiretaNoSafetyStock;
    
    // PARAMETROS ASSOCIADOS A PEDIDOS E REMESSAS --------------------
    // se true, o backlog será considerado no primeiro período do SNP
    private Boolean consideraPedidosBacklog;
    // substitui a funcionalidade acima, que simplesmente abate as remessas do estoque
    // neste comportamento, as remessas em separação são consideradas no primeiro período para abater a
    // disponibilidade de estoque no primeiro momento possível, mas sem alterar o estoque incial
    private Boolean remessasConsomemDisponibilidadeNoPrimeiroPeriodo;
    private Boolean quantidadesEmPedidosRepresentamSaldoRestante;
    // --------------------

    // DEFINIÇÂO DA BASE DE VENDAS : SELLOUT OU PEDIDOS
    @Enumerated(EnumType.ORDINAL)
    private Constantes.TipoDocumentoVenda tipoDocumentoVenda;
    
    // Data Upload
    private Boolean ignoraCamposVaziosDataUpload; // quando true, se campo vazio no arquivo excel/csv não substitui valor tabela por null
    private Boolean ignoraCamposVaziosDataUploadPedidos; // quando true, se campo vazio no arquivo excel/csv não substitui valor tabela por null
    private Boolean ignoraCamposVaziosDataUploadRemessas; // quando true, se campo vazio no arquivo excel/csv não substitui valor tabela por null
    private Boolean ignoraCamposVaziosDataUploadEstoque; // quando true, se campo vazio no arquivo excel/csv não substitui valor tabela por null
    private Boolean ignoraCamposVaziosDataUploadSellout; // quando true, se campo vazio no arquivo excel/csv não substitui valor tabela por null
       
    private Boolean logDataUploadPedidos;
    private Boolean logDataUploadRemessas;
    
    // TELA DE LOW LEVEL CODES
    private Boolean exibeLocationsClienteFinalLowLevelCode;
    private Boolean logConteudoInputsIntegracaoDados;

    // Ex. Cálculo de ocupação será em KG e M³
    @Getter(AccessLevel.NONE)
    @ManyToOne
    UnidadeMedida unidadeMedidaPadraoCapacidadeLogisticaPeso;
    @Getter(AccessLevel.NONE)
    @ManyToOne
    UnidadeMedida unidadeMedidaPadraoCapacidadeLogisticaVolume;

    
    public Constantes.TamanhoBucket getTamanhoBucketEstoqueSegmentado() {
        if(this.tamanhoBucketEstoqueSegmentado == null ) return Constantes.ESTOQUE_SEGMENTADO_PADRAO_TAMANHO_BUCKET;
        return tamanhoBucketEstoqueSegmentado;
    }
    
    public void setTipoDemandaCurva(Constantes.TipoQuantidadeValor tipoDemandaCurva) {
        this.tipoDemandaCurva = tipoDemandaCurva;
    }
    
    public Integer getHorizonteForecastDias() {

        return (horizonteForecastDias == null)
                ? Constantes.DP_PADRAO_DIAS_HORIZONTE_FORECAST
                : getInteiroOperacionalPositivoCadastrado(
                        horizonteForecastDias,
                        "Global forecast horizon in days");

    }
    
    public Integer getHorizonteOTBDias() {
        return (horizonteOTBDias == null) ? 0 : Math.max(1, horizonteOTBDias);
    }

    /**
     * Valida inteiros globais que possuem default para ausencia, mas nao podem
     * aceitar valor explicitamente cadastrado como zero ou negativo.
     *
     * <p>As bordas REST e front bloqueiam payloads novos invalidos. Esta
     * validacao local protege snapshots antigos, cargas diretas e consumidores
     * internos que recebem a entidade ja materializada pelo repository.</p>
     */
    private int getInteiroOperacionalPositivoCadastrado(
            Integer valorOperacionalCadastrado,
            String descricaoCampo) {

        if (valorOperacionalCadastrado <= 0) {
            throw new IllegalStateException(
                    descricaoCampo
                            + " must be positive when explicitly configured: "
                            + valorOperacionalCadastrado
                            + ".");
        }
        return valorOperacionalCadastrado;

    }

    public Integer getDiasHistoricosDoh() {
        return (diasHistoricosDoh == null) ? Constantes.GLOBAL_PADRAO_DIAS_HISTORICOS_DOH : diasHistoricosDoh;
    }

    public Double getPercentilOutliersVenda() {
        return (percentilOutliersVenda == null) ? Constantes.DP_PADRAO_PERCENTIL_OUTLIERS_VENDA : percentilOutliersVenda;
    }
    
    public Integer getDiasHistoricosDohStockout() {
        return (diasHistoricosDohStockout == null) ? Constantes.GLOBAL_PADRAO_DIAS_HISTORICOS_DOH_STOCKOUT : diasHistoricosDohStockout;
    }

    public Integer getDiasHistoricosForecastEstatistico() {
        return (diasHistoricosForecastEstatistico == null) ? Constantes.DP_PADRAO_DIAS_HISTORICOS_FORECAST_ESTATISTICO : diasHistoricosForecastEstatistico;
    }

    /**
     * Valor bruto cadastrado para calculo de custo de estoque.
     *
     * <p>O getter operacional {@link #getCalculaCustoEstoque()} retorna sempre
     * {@code false} no recorte Community, porque custo de estoque/Pricing e
     * capacidade Enterprise. Bordas de validacao precisam deste helper para
     * diferenciar payload ausente ou neutro de uma tentativa explicita de
     * ativar custo de estoque.</p>
     */
    public Boolean getCalculaCustoEstoqueCadastrado() {

        return calculaCustoEstoque;

    }

    public Boolean getCalculaCustoEstoque() {
        return false;
    }

    public Integer getDiasHistoricosCurva() {
        return (diasHistoricosCurva == null) ? 0 : diasHistoricosCurva;
    }

    public int getJanelaDiasAnteriorForaLinha() {
        return (janelaDiasAnteriorForaLinha == null) ? Constantes.GLOBAL_PADRAO_DIAS_JANELA_ANTERIOR_FORA_LINHA : janelaDiasAnteriorForaLinha;
    }

    public int getJanelaDiasPosteriorForaLinha() {
        return (janelaDiasPosteriorForaLinha == null) ? Constantes.GLOBAL_PADRAO_DIAS_JANELA_POSTERIOR_FORA_LINHA : janelaDiasPosteriorForaLinha;
    }

    public float getPercentualVendasForaLinha() {
        return (percentualVendasForaLinha == null) ? Constantes.GLOBAL_PADRAO_PERCENTUAL_VENDAS_FORA_LINHA : percentualVendasForaLinha;
    }

    /**
     * Valor padrão : 0 (produto nunca é considerado como novo)
     * @return 
     */
    public Integer getNumeroDiasProdutoNovo() {
        return (numeroDiasProdutoNovo == null) ? 0 : numeroDiasProdutoNovo;
    }
    
    /**
     * Valor padrão : 0 (location nunca é considerada como nova)
     * @return 
     */
    public Integer getNumeroDiasLocationNova() {
        return (numeroDiasLocationNova == null) ? 0 : numeroDiasLocationNova;
    }

    /**
     * Valor bruto cadastrado para a janela global de limpeza historica.
     *
     * <p>O getter operacional {@link #getDiasHistoricosNormalizacao()} preserva
     * compatibilidade historica aplicando default e minimo tecnico. Bordas
     * Enterprise de validacao/persistencia precisam do valor bruto para
     * diferenciar configuracao ausente, que herda default, de payload
     * explicitamente invalido, que deve falhar ou ser saneado antes do save.</p>
     */
    public Integer getDiasHistoricosNormalizacaoCadastrado() {

        return diasHistoricosNormalizacao;

    }

    public int getDiasHistoricosNormalizacao() {
        return (diasHistoricosNormalizacao == null) ? Constantes.DP_PADRAO_DIAS_NORMALIZACAO : Math.max(1, diasHistoricosNormalizacao);
    }

    public Constantes.DPModeloDemandaBase getModeloDemandaBase() {
        return (modeloDemandaBase == null) ? Constantes.DP_PADRAO_TIPO_MODELO_DEMANDA_BASE : modeloDemandaBase;
    }

    public Constantes.DPModeloNormalizacao getModeloNormalizacao() {
        return (modeloNormalizacao == null) ? Constantes.DP_PADRAO_TIPO_MODELO_NORMALIZACAO : modeloNormalizacao;
    }

    public Constantes.TamanhoBucket getTamanhoBucketOTB() {
        return (tamanhoBucketOTB == null) ? Constantes.TamanhoBucket.MENSAL : tamanhoBucketOTB;
    }
    
    public Constantes.SNPTipoEstoque getTipoEstoque() {
        return (tipoEstoque == null) ? Constantes.DRP_PADRAO_TIPO_ESTOQUE : tipoEstoque;
    }
        
    public Integer getDiasHistoricosClusterizacao() {
        return (diasHistoricosClusterizacao == null) ? Constantes.CLUSTERING_PADRAO_NUMERO_DIAS_HISTORICOS : diasHistoricosClusterizacao;
    }
    
    public Constantes.TipoQuantidadeValor getTipoDemandaCurva() {
        return (tipoDemandaCurva == null) ? Constantes.TipoQuantidadeValor.QUANTIDADE : tipoDemandaCurva;
    }
    
    public Integer getDiasHistoricosDeployment() {
        return (diasHistoricosDeployment == null) ? 0 : Math.max(1,diasHistoricosDeployment);
    }
    
    public Integer getNumeroDiasPassadosPlanoConsolidado() {
        return (numeroDiasPassadosPlanoConsolidado == null) ? 125 : numeroDiasPassadosPlanoConsolidado;
    }

    public boolean getDpUsaHistoricoDemandaInativos() {
        return (dpUsaHistoricoDemandaInativos == null) ? true : dpUsaHistoricoDemandaInativos;
    }
    
    public UnidadeMedida getUnidadeMedidaPadraoDP() {
        return (unidadeMedidaPadraoDP == null) ? new UnidadeMedida("UN") : unidadeMedidaPadraoDP;
    }
    
    public UnidadeMedida getUnidadeMedidaPadraoSNP() {
        return (unidadeMedidaPadraoSNP == null) ? new UnidadeMedida("UN") : unidadeMedidaPadraoSNP;
    }
    
    public UnidadeMedida getUnidadeMedidaPadraoDeployment() {
        return (unidadeMedidaPadraoDeployment == null) ? getUnidadeMedidaPadraoSNP() : unidadeMedidaPadraoDeployment;
    }
    
    public UnidadeMedida getUnidadeMedidaPadraoPricing() {
        return (unidadeMedidaPadraoPricing == null) ? getUnidadeMedidaPadraoDP() : unidadeMedidaPadraoPricing;
    }

    public boolean getIgnoraCamposVaziosDataUpload() {
        return (ignoraCamposVaziosDataUpload == null) ? true : ignoraCamposVaziosDataUpload;
    }
    
    public boolean getIgnoraCamposVaziosDataUploadPedidos() {
        return (ignoraCamposVaziosDataUploadPedidos == null) ? true : ignoraCamposVaziosDataUploadPedidos;
    }
    public boolean getIgnoraCamposVaziosDataUploadRemessas() {
        return (ignoraCamposVaziosDataUploadRemessas == null) ? true : ignoraCamposVaziosDataUploadRemessas;
    }
    public boolean getIgnoraCamposVaziosDataUploadEstoque() {
        return (ignoraCamposVaziosDataUploadEstoque == null) ? true : ignoraCamposVaziosDataUploadEstoque;
    }
    public boolean getIgnoraCamposVaziosDataUploadSellout() {
        return (ignoraCamposVaziosDataUploadSellout == null) ? true : ignoraCamposVaziosDataUploadSellout;
    }

    public boolean getIncluiDemandaIndiretaNoSafetyStock() {
        return (incluiDemandaIndiretaNoSafetyStock == null) ? true : incluiDemandaIndiretaNoSafetyStock;
    }
    
    public boolean getConsideraPedidosBacklog() {
        return (consideraPedidosBacklog == null) ? true : consideraPedidosBacklog;
    }

    /**
     * Indica se o parametro transacional de backlog de pedidos foi
     * explicitamente cadastrado como ativo.
     *
     * <p>`getConsideraPedidosBacklog()` preserva o default historico do modelo
     * fisico e retorna {@code true} quando o campo esta nulo. Esse default nao
     * pode ser usado para validar capability Community, pois pedidos pertencem
     * ao Enterprise. Services Community e overlays Enterprise usam este helper
     * para diferenciar "campo ausente" de "usuario/configuracao tentou ativar
     * backlog de pedidos".</p>
     */
    public boolean isConsideraPedidosBacklogCadastradoAtivo() {
        return Boolean.TRUE.equals(consideraPedidosBacklog);
    }

    public boolean getRemessasConsomemDisponibilidadeNoPrimeiroPeriodo() {
        return (remessasConsomemDisponibilidadeNoPrimeiroPeriodo == null) ? true : remessasConsomemDisponibilidadeNoPrimeiroPeriodo;
    }

    public boolean getQuantidadesEmPedidosRepresentamSaldoRestante() {
        return (quantidadesEmPedidosRepresentamSaldoRestante == null) ? false : quantidadesEmPedidosRepresentamSaldoRestante;
    }

    public boolean getExibeLocationsClienteFinalLowLevelCode() {
        return (exibeLocationsClienteFinalLowLevelCode == null) ? false : exibeLocationsClienteFinalLowLevelCode;
    }
    
    public Constantes.TipoDocumentoVenda getTipoDocumentoVenda() {
        return (tipoDocumentoVenda == null) ? Constantes.TipoDocumentoVenda.SELLOUT : tipoDocumentoVenda;
    }
    
    public boolean getDpGeraForecastParaDescontinuados() {
        return (dpGeraForecastParaDescontinuados == null) ? true : dpGeraForecastParaDescontinuados;
    }
    
    public boolean getDpArredondaParaUnidadeVenda() {
        return (dpArredondaParaUnidadeVenda == null) ? false : dpArredondaParaUnidadeVenda;
    }
    
    
    public String getTimeZone() {
        return (timeZone == null) ? "America/Sao_Paulo" : timeZone;
    }
    
    public boolean getPermiteAjusteAgregadoSemBaselineProduto() {
        return (permiteAjusteAgregadoSemBaselineProduto == null) ? true : permiteAjusteAgregadoSemBaselineProduto;
    }
    public boolean getPermiteAjusteAgregadoSemBaselineLocation() {
        return (permiteAjusteAgregadoSemBaselineLocation == null) ? true : permiteAjusteAgregadoSemBaselineLocation;
    }
    
    public boolean getBloqueiaAjustesDentroHorizonteCongeladoDp() {
        return (bloqueiaAjustesDentroHorizonteCongeladoDp == null) ? true : bloqueiaAjustesDentroHorizonteCongeladoDp;
    }
    
    public ModeloCadastroProdutoLocation getModeloCadastroProdutoLocation() {
        return (modeloCadastroProdutoLocation == null) ? ModeloCadastroProdutoLocation.ATIVO_SE_AUSENTE : modeloCadastroProdutoLocation;
    }
    
    public boolean getGeraLogModificacoesDemandPlanNivelDfu() {
        return (geraLogModificacoesDemandPlanNivelDfu == null) ? false : geraLogModificacoesDemandPlanNivelDfu;
    }
    
    public boolean getLogConteudoInputsIntegracaoDados() {
        return (logConteudoInputsIntegracaoDados == null) ? false : logConteudoInputsIntegracaoDados;
    }
    
    public boolean getLogDataUploadPedidos() {
        return (logDataUploadPedidos == null) ? false : logDataUploadPedidos;
    }
    
    public boolean getLogDataUploadRemessas() {
        return (logDataUploadRemessas == null) ? false : logDataUploadRemessas;
    }

    public UnidadeMedida getUnidadeMedidaPadraoCapacidadeLogisticaPesoCadastrado() {
        return unidadeMedidaPadraoCapacidadeLogisticaPeso;
    }
    public UnidadeMedida getUnidadeMedidaPadraoCapacidadeLogisticaVolumeCadastrado() {
        return unidadeMedidaPadraoCapacidadeLogisticaVolume;
    }

    public UnidadeMedida getUnidadeMedidaPadraoCapacidadeLogisticaPeso() {
        if (unidadeMedidaPadraoCapacidadeLogisticaPeso != null) return unidadeMedidaPadraoCapacidadeLogisticaPeso;
        return getUnidadeMedidaPadraoSNP();
    }
    public UnidadeMedida getUnidadeMedidaPadraoCapacidadeLogisticaVolume() {
        if (unidadeMedidaPadraoCapacidadeLogisticaVolume != null) return unidadeMedidaPadraoCapacidadeLogisticaVolume;
        return getUnidadeMedidaPadraoSNP();
    }

}
