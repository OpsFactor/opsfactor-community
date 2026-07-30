package com.opsfactor.community.capability.configuration.user.projection;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredViewKeyFigure;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.FiltroDFUProjection;
import com.opsfactor.community.capability.planningbook.domain.AjusteCelulaPlanningBook;
import com.opsfactor.community.capability.planningbook.domain.PlanningBookDfuScope;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureInterface;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Projection da view de Planning Book ja materializada para consumo dos
 * calculos e do front.
 *
 * <p>No Community, a view trabalha sempre no menor nivel material/location.
 * Caracteristicas dinamicas de material, location e material-location/DFU sao
 * recursos Enterprise: nao podem aparecer como filtros, agrupamentos ou
 * colunas configuraveis. Qualquer sujeira legada precisa chegar aqui ja
 * bloqueada ou removida pelo ConfiguredViewFrontService e pela
 * ConfiguredViewProjectionFactory.</p>
 *
 * <p>A unidade interna de exibicao/atualizacao e {@link PlanningBookDfuScope}.
 * Ela representa apenas conjuntos de materiais e locations ja filtrados, sem
 * carregar estrutura de agrupamento por caracteristica. O primeiro escopo da
 * view costuma conter todos os DFUs filtrados; selecoes enviadas pelo front
 * geram escopos unitarios material/location.</p>
 */
@Data
public class ConfiguredViewProjection {

    protected ClusterEParametrosProjection clusterEParametrosProjection;
    protected ConfiguredView configuredView; 

    protected List<KeyFigureInterface> keyFiguresOrdenadasParaExibicao;

    /*
     * Fotografia carregada apenas na abertura do Planning Book pela
     * KeyFigureProjectionFactory. A ConfiguredViewProjectionFactory não toca
     * esta estrutura porque é usada em lote por fluxos que não precisam de
     * seleção de key figures.
     */
    protected Map<String, ConfiguredViewKeyFigure> keyFiguresConfiguradasPorId = new LinkedHashMap<>();

    /*
     * Escopos material/location efetivamente visiveis no Planning Book
     * Community. Nao existem group-bys por caracteristicas nesta edicao; a
     * factory cria um escopo amplo para renderizacao da view e o service cria
     * escopos unitarios para celulas selecionadas.
     */
    protected Set<PlanningBookDfuScope> planningBookDfuScopes;
    
    // APENAS USADOS EM INTERAÇÕES COM FRONT-END
    // delimitam o escopo das atualizações solicitadas pelo front-end
    Set<AjusteCelulaPlanningBook> detalhesSelecaoAAtualizar;
    // DetalhesSelecao que geraram algum erro na atualização, associados às respectivas mensagens de erro
    Map<AjusteCelulaPlanningBook,String> erroAtualizacaoPorDetalheSelecao;

    // APLICANDO FILTRO DFUS
    protected FiltroDFUProjection dfuProjectionFiltrado;

    // CAMPOS ESPECÍFICOS DEMAND PLANNING ---------------------
    protected KeyFigureInterface keyFigureAjusteDemandaTotal;

    public boolean getExibeMateriais() {

        // Community nao possui modo agregado por material. O Planning Book
        // sempre opera no menor nivel material/location para evitar inputs
        // agregados que nao existem no backend Community.
        return true;

    }
    
    public boolean getExibeLocations() {

        // Community nao possui modo agregado por location. O comportamento
        // deve permanecer estavel mesmo se uma view legada trouxer a flag
        // persistida como falsa.
        return true;

    }
    public UnidadeMedida getUnidadeMedidaView(ParametrosGlobais parametrosGlobais) {
        return getConfiguredView().getUnidadeMedidaView(parametrosGlobais);
    }

    /**
     * Escopos material/location visiveis no Planning Book.
     *
     * <p>A factory Community popula essa colecao incrementalmente, portanto o
     * getter preserva a colecao mutavel. Mesmo assim, snapshot nulo ou item
     * nulo indicam quebra estrutural da view e devem falhar antes dos services
     * de front montarem grupos ou DTOs.</p>
     */
    public Set<PlanningBookDfuScope> getPlanningBookDfuScopes() {

        if (planningBookDfuScopes == null) {
            throw new IllegalStateException(
                    "ConfiguredViewProjection requires Planning Book DFU scopes before rendering the Planning Book.");
        }
        validaPlanningBookDfuScopes(planningBookDfuScopes);
        return planningBookDfuScopes;

    }

    /**
     * Selecoes de celula enviadas pelo front para atualizacao.
     *
     * <p>`null` continua significando leitura/renderizacao sem selecao
     * especifica. Quando existe colecao, cada ajuste precisa carregar seu
     * periodo, key figure, unidade, novo valor e {@link PlanningBookDfuScope};
     * selecoes sem material/location identificaveis devem ser resolvidas para
     * DFUs reais antes da montagem da resposta.</p>
     */
    public Set<AjusteCelulaPlanningBook> getDetalhesSelecaoAAtualizar() {

        if (detalhesSelecaoAAtualizar == null) {
            return null;
        }
        validaDetalhesSelecaoAAtualizar(detalhesSelecaoAAtualizar);
        return detalhesSelecaoAAtualizar;

    }

    /**
     * Erros por celula selecionada para exibicao no Planning Book.
     *
     * <p>`null` continua significando que nao houve tentativa de atualizacao
     * ou que nenhuma celula falhou. Quando o mapa existe, cada entrada precisa
     * trazer a celula original, periodo, key figure, escopo material/location e
     * mensagem, pois o PlanningBookService usa esses dados para pintar a celula
     * e montar o log exibido ao usuario.</p>
     */
    public Map<AjusteCelulaPlanningBook,String> getErroAtualizacaoPorDetalheSelecao() {

        if (erroAtualizacaoPorDetalheSelecao == null) {
            return null;
        }
        validaErroAtualizacaoPorDetalheSelecao(erroAtualizacaoPorDetalheSelecao);
        return erroAtualizacaoPorDetalheSelecao;

    }

    /**
     * Retorna todos os materiais ativos que passam pelo filtro da view
     * @return 
     */
    public Set<Produto> getMateriaisFiltrados() {

        validaDfuProjectionFiltrado();
        return dfuProjectionFiltrado.getMateriais();
        
    }
    /**
     * Traz materiais dos escopos DFU selecionados para atualizacao, sem duplicatas.
     *
     * <p>No Community nao ha ajuste agregado, filtros por caracteristica ou
     * selecao em outro nivel de granularidade: todos os escopos de atualizacao
     * ja representam os materiais editaveis diretamente.</p>
     *
     * @return 
     */
    public Set<Produto> getMateriaisAAtualizar() {
        
        if (detalhesSelecaoAAtualizar == null) return getMateriaisFiltrados();
        
        return getDetalhesSelecaoAAtualizar().stream()
                .map(AjusteCelulaPlanningBook::getPlanningBookDfuScope)
                .flatMap(planningBookDfuScope -> planningBookDfuScope.getMateriais().stream())
                .collect(Collectors.toSet());
        
    }
    
    /**
     * Retorna todas as locations ativas que passam pelo filtro da view
     * @return 
     */
    public Set<Location> getLocationsFiltradas() {

        validaDfuProjectionFiltrado();
        return dfuProjectionFiltrado.getLocations();
        
    }
    /**
     * Traz locations dos escopos DFU selecionados para atualizacao, sem duplicatas.
     *
     * <p>No Community nao ha ajuste agregado, filtros por caracteristica ou
     * selecao em outro nivel de granularidade: todos os escopos de atualizacao
     * ja representam as locations editaveis diretamente.</p>
     *
     * @return 
     */
    public Set<Location> getLocationsAAtualizar() {
        
        if (detalhesSelecaoAAtualizar == null) return getLocationsFiltradas();
        
        return getDetalhesSelecaoAAtualizar().stream()
                .map(AjusteCelulaPlanningBook::getPlanningBookDfuScope)
                .flatMap(planningBookDfuScope -> planningBookDfuScope.getLocations().stream())
                .collect(Collectors.toSet());
        
    }

    public boolean contemKeyFigurePropagacaoComponentesListaTecnica() {

        // Propagacao por custom key figure e recurso Enterprise. No Community
        // a lista de KFs e fixa e nao contem KFs customizadas.
        return false;

    }

    private void validaDfuProjectionFiltrado() {

        if (dfuProjectionFiltrado == null) {
            throw new IllegalStateException(
                    "ConfiguredViewProjection requires filtered DFU projection before reading Planning Book material/location scope.");
        }

    }

    private static void validaPlanningBookDfuScopes(
            Set<PlanningBookDfuScope> planningBookDfuScopes) {

        int index = 0;
        for (PlanningBookDfuScope planningBookDfuScope : planningBookDfuScopes) {
            if (planningBookDfuScope == null) {
                throw new IllegalStateException(
                        "ConfiguredViewProjection Planning Book DFU scope at index " + index + " is required.");
            }
            index++;
        }

    }

    private static void validaDetalhesSelecaoAAtualizar(
            Set<AjusteCelulaPlanningBook> detalhesSelecaoAAtualizar) {

        int index = 0;
        for (AjusteCelulaPlanningBook ajusteCelulaPlanningBook : detalhesSelecaoAAtualizar) {
            if (ajusteCelulaPlanningBook == null) {
                throw new IllegalStateException(
                        "ConfiguredViewProjection selected Planning Book cell at index " + index + " is required.");
            }
            validaAjusteCelulaPlanningBook(
                    ajusteCelulaPlanningBook,
                    "ConfiguredViewProjection selected Planning Book cell at index " + index);
            index++;
        }

    }

    private static void validaErroAtualizacaoPorDetalheSelecao(
            Map<AjusteCelulaPlanningBook,String> erroAtualizacaoPorDetalheSelecao) {

        int index = 0;
        for (Map.Entry<AjusteCelulaPlanningBook,String> entry : erroAtualizacaoPorDetalheSelecao.entrySet()) {
            AjusteCelulaPlanningBook ajusteCelulaPlanningBook = entry.getKey();
            if (ajusteCelulaPlanningBook == null) {
                throw new IllegalStateException(
                        "ConfiguredViewProjection update error entry at index " + index + " requires selected Planning Book cell.");
            }
            validaAjusteCelulaPlanningBook(
                    ajusteCelulaPlanningBook,
                    "ConfiguredViewProjection update error entry at index " + index);
            String mensagemErro = entry.getValue();
            if (mensagemErro == null || mensagemErro.isBlank()) {
                throw new IllegalStateException(
                        "ConfiguredViewProjection update error entry at index " + index + " requires error message.");
            }
            index++;
        }

    }

    /**
     * Valida a celula editavel material/location do Planning Book Community.
     *
     * <p>O service de configuracao ja valida o DTO vindo da SPA, mas esta
     * projection tambem e usada por Demand, Supply e testes/factories. Manter
     * a guarda no objeto compartilhado impede que uma instancia montada fora
     * da borda normal chegue aos consumers com periodo, key figure, unidade ou
     * valor ausentes.</p>
     */
    private static void validaAjusteCelulaPlanningBook(
            AjusteCelulaPlanningBook ajusteCelulaPlanningBook,
            String contextoAjuste) {

        if (ajusteCelulaPlanningBook.getPlanningBookDfuScope() == null) {
            throw new IllegalStateException(
                    contextoAjuste + " requires material/location DFU scope.");
        }
        if (ajusteCelulaPlanningBook.getDataHorarioReferencia() == null) {
            throw new IllegalStateException(
                    contextoAjuste + " requires period reference.");
        }
        if (ajusteCelulaPlanningBook.getKeyFigureId() == null || ajusteCelulaPlanningBook.getKeyFigureId().isBlank()) {
            throw new IllegalStateException(
                    contextoAjuste + " requires key figure id.");
        }
        if (ajusteCelulaPlanningBook.getUomId() == null || ajusteCelulaPlanningBook.getUomId().isBlank()) {
            throw new IllegalStateException(
                    contextoAjuste + " requires unit of measure id.");
        }
        if (ajusteCelulaPlanningBook.getValorNovo() == null || !Double.isFinite(ajusteCelulaPlanningBook.getValorNovo())) {
            throw new IllegalStateException(
                    contextoAjuste + " requires finite new value.");
        }

    }
        
}
