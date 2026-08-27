package com.opsfactor.community.capability.masterdata.network.supplynetwork.projection;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnicaComponente;
import com.opsfactor.community.capability.masterdata.production.operation.domain.OperacaoRoteiro;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducaoInexistente;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducaoSimples;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporteProduto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.platform.exception.UnitOfMeasureConversionException;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.FuncoesMap;
import lombok.Builder;
import lombok.Getter;

import jakarta.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Projection em memoria da malha de supply e dos dados produtivos Community.
 *
 * <p>O Community usa esta projection para o heuristico: linhas de transporte,
 * lead time, lote/multiplo, roteiros, listas tecnicas, recursos produtivos e
 * versoes simples de producao. Frotas, custos, rotas last-mile, paralelismo de
 * producao, scheduling e demais capacidades Enterprise devem entrar por
 * projections/overlays privados.</p>
 */
public class SupplyNetworkProjection {

    /**
     * Projection de UOM usada para converter lotes, multiplos e quantidades de
     * producao/transporte sem consultar banco durante calculos.
     */
    @Getter
    protected UnidadeMedidaProjection conversaoUnidadeMedidaProjection;

    /**
     * Projection central de materiais, locations, parametros e clusters.
     */
    @Getter
    protected ClusterEParametrosProjection clusterEParametrosProjection;

    /**
     * Inicializa as projections transversais usadas por toda a malha.
     *
     * <p>A factory Community e extensões Enterprise usam este único ponto de
     * montagem. Assim, uma projection filha não precisa redeclarar estado
     * comum apenas para sua factory preencher os campos herdados.</p>
     */
    public void inicializaProjectionsBase(
            UnidadeMedidaProjection conversaoUnidadeMedidaProjection,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        this.conversaoUnidadeMedidaProjection = conversaoUnidadeMedidaProjection;
        this.clusterEParametrosProjection = clusterEParametrosProjection;

    }
    
    // DADOS DA MALHA (LINHAS DE TRANSPORTE) --------------------------------------------------
    protected Map<String, VersaoMalha> mapaVersaoMalhaPorId;
    protected Map<VersaoMalha, Set<LinhaTransporte>> mapaLinhaTransporteSetPorVersaoMalha; // linhas por versao malha
    protected Map<VersaoMalha, Map<LinhaTransporte,Map<Produto,LinhaTransporteProduto>>> mapaLinhaTransporteProdutoPorLinhaTransporteEProduto;

    protected Map<VersaoMalha, Map<Location,Set<LinhaTransporte>>> mapaLinhaTransporteInboundAtivaSetPorLocation; // linhas inbound por destino
    protected Map<VersaoMalha, Map<Location,Set<LinhaTransporte>>> mapaLinhaTransporteOutboundAtivaSetPorLocation; // linhas outbound por origem

    // prioridade em mapa separado pois origem da informação pode ser tanto LinhaTransporte como LinhaTransporteProduto
    // desta forma mesmo com um mapa Map<Location,Map<Produto,List<LinhaTransporte>>> ordenado não se sabe se a origem da informação
    // é linhaTransporte ou linhaTransporteProduto
    // no futuro as prioridades poderão variar periodo a periodo
    @Builder
    @Getter
    public static class ParametrosLinhaTransporte {
        @Builder.Default // considera o valor de inicialização
        protected Integer prioridade = Integer.MAX_VALUE;
        @Builder.Default // considera o valor de inicialização
        protected Integer leadTimeDias = 0;
        protected UnidadeMedida unidadeMedidaLoteMinimoMultiploTransporte;
        protected Double loteMinimoTransporte;
        protected OptionalDouble multiploTransporte;
        public Integer getLeadTimeEmPeriodos(Calendario calendario) {
            return (int) Math.floor(calendario.converteDiasParaPeriodosCalendario(leadTimeDias));
        }

        /**
         * Tipo de operacao fiscal associado ao trecho.
         *
         * <p>Regra fiscal detalhada e ICMS pertencem ao Enterprise. O Community
         * nao importa a entidade privada de tipo de operacao fiscal; por isso o
         * metodo e generico e retorna nulo ate que uma projection Enterprise
         * substitua/complemente estes parametros.</p>
         */
        public <T> T getTipoOperacaoFiscal() {

            return null;

        }
        
    }
    
    // DADOS DE PRODUÇÃO --------------------------------------------------
    protected Map<String, RecursoProdutivo> mapaRecursosProdutivos;
    protected Map<String, Roteiro> mapaRoteiros;
    protected Map<String, ListaTecnica> mapaListasTecnicas;

    protected Map<Location,Set<RecursoProdutivo>> mapaRecursoProdutivoAtivoSetPorLocation;
    protected Map<RecursoProdutivo,Map<Produto,Set<Roteiro>>> mapaRoteiroSetPorRecursoProdutivoMaterial;
    
    // versão de produção inexistente equivale a um campo de versao de producao nulo por exemplo no production plan linha
    @Getter
    protected VersaoProducaoInexistente versaoProducaoInexistente;
    protected Map<RecursoProdutivo,Set<VersaoProducao>> mapaVersaoProducaoViavelSetPorRecursoProdutivo;
    protected Map<Location,Map<Produto,Set<VersaoProducao>>> mapaVersaoProducaoSetPorLocationMaterial;
    protected Map<Location,Map<Produto,Set<VersaoProducao>>> mapaVersaoProducaoViavelSetPorLocationMaterial;
    // versão de produção prioritária por location/produto. se não há versão de produção
    // associada a uma lista técnica/roteiro, os métodos desta classe buscam ambos
    // de forma independente, através de suas próprias prioridades
    protected Map<Location,Map<Produto,VersaoProducao>> mapaVersaoProducaoViavelPrioritariaPorLocationProduto;

    // inclui tanto roteiros ativos como inativos (inativos necessários para processar ordens antigas)
    protected Map<Location,Map<Produto,Set<Roteiro>>> mapaRoteiroSetPorLocationMaterial;
    protected Map<Location,Map<Produto,Set<Roteiro>>> mapaRoteiroViavelSetPorLocationMaterial;
    
    // inclui tanto listas técnicas ativas como inativas (inativas necessários para processar ordens antigas)
    protected Map<Location,Map<Produto,Set<ListaTecnica>>> mapaListaTecnicaSetPorLocationMaterial;
    protected Map<Location,Map<Produto,Set<ListaTecnica>>> mapaListaTecnicaViavelSetPorLocationMaterial;

    // chamadas muito frequentes e cuja execução via streams é muito ineficiente
    // inclui apenas listas técnicas prioritárias (para respectivos materiais output)
    // usado na definição de low level codes e na restrição do plano de supply
    // populado via método getListaTecnicaViavelPrioritariaSetOndeMaterialEInput
    protected Map<Location,Map<Produto,Set<ListaTecnica>>> mapaListaTecnicaViavelPrioritariaSetOndeMaterialEInput;

    /**
     * Define se uma versão de produção participa da consulta solicitada.
     *
     * <p>O Community só carrega versões simples. A extensão Enterprise pode
     * sobrescrever este ponto para aplicar filtros próprios sem introduzir
     * tipos privados no contrato aberto.</p>
     */
    protected boolean isVersaoProducaoDisponivel(
            VersaoProducao versaoProducao,
            boolean consideraVersoesProducaoParalelas) {

        return true;

    }
    
    
    /**
     * Determina se o material na location será ressuprido através de produção, requisição inbound
     * ou se não há como se ressuprir o material
     * Não considera o efeito do lead time
     * @param location
     * @param material
     * @return Constantes.TipoRessuprimento = PRODUCAO, REQUISICAO, SEM_RESSUPRIMENTO
     */
    public Constantes.SNPOrigemReabastecimento getTipoRessuprimento(
            VersaoMalha versaoMalha, 
            Location location, 
            Produto material, 
            LocalDateTime dataReferenciaParaStatusProduto, 
            boolean consideraVersoesProducaoParalelas,
            @Nullable Collection<Location> locationsOrigemPossiveis,
            @Nullable Collection<Produto> possiveisMateriaisInput) {
        
        if (verificaSeHaProducao(
                location, material, consideraVersoesProducaoParalelas, possiveisMateriaisInput)) {
            return Constantes.SNPOrigemReabastecimento.PRODUCAO;
        } else if (getLinhaTransporteViavelPrioritariaInbound(
                versaoMalha, location, material, dataReferenciaParaStatusProduto, locationsOrigemPossiveis).isPresent()) {
            return Constantes.SNPOrigemReabastecimento.REQUISICAO;
        }
        return Constantes.SNPOrigemReabastecimento.SEM_RESSUPRIMENTO;
    }
    
    public Set<Roteiro> getRoteirosDeVersaoProducaoPrioritaria(
            Location location, 
            Produto material, 
            boolean consideraVersoesProducaoParalelas,
            @Nullable Collection<Produto> possiveisMateriaisInput) {
        
        Optional<VersaoProducao> versaoProducaoPrioritaria = mapaVersaoProducaoViavelSetPorLocationMaterial
                .getOrDefault(location, new HashMap<>())
                .getOrDefault(material, new HashSet<>()).stream()
                        .filter(versaoProducao -> isVersaoProducaoDisponivel(versaoProducao, consideraVersoesProducaoParalelas))
                        .filter(versaoProducao -> possiveisMateriaisInput == null || possiveisMateriaisInput.containsAll(versaoProducao.getMateriaisInput()))
                        .sorted(Comparator.comparing(VersaoProducao::getPrioridade))
                        .findFirst();
        
        return versaoProducaoPrioritaria
                .map(VersaoProducao::getRoteiros)
                .orElseGet(HashSet::new);
        
    }
        
    public Set<Roteiro> getTodosRoteiros(Location location, Produto material) {
        
        return mapaRoteiroSetPorLocationMaterial
                .getOrDefault(location, new HashMap<>())
                .getOrDefault(material, new HashSet<>());
        
    }

    public Set<Roteiro> getTodosRoteiros(
            Location location,
            @Nullable Collection<Produto> possiveisMateriaisOutput) {
        
        return mapaRoteiroSetPorLocationMaterial
            .getOrDefault(location, new HashMap<>()).entrySet().stream()
                .filter(entry -> possiveisMateriaisOutput == null || possiveisMateriaisOutput.contains(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toSet());
        
    }
    
    public Set<Roteiro> getTodosRoteiros(
            @Nullable Collection<Location> possiveisLocations,
            @Nullable Collection<Produto> possiveisMateriaisOutput) {
    
        if (possiveisLocations == null) {
            return mapaRoteiroSetPorLocationMaterial.entrySet().stream()
                    .filter(entry -> possiveisLocations == null || possiveisLocations.contains(entry.getKey()))
                    .flatMap(entry -> entry.getValue().entrySet().stream())
                    .filter(entry -> possiveisMateriaisOutput == null || possiveisMateriaisOutput.contains(entry.getKey()))
                    .flatMap(entry -> entry.getValue().stream())
                    .collect(Collectors.toSet());
        } else {
            return possiveisLocations.stream()
                    .map(location -> mapaRoteiroSetPorLocationMaterial.getOrDefault(location, new HashMap<>()))
                    .flatMap(subMap -> subMap.entrySet().stream())
                    .filter(entry -> possiveisMateriaisOutput == null || possiveisMateriaisOutput.contains(entry.getKey()))
                    .flatMap(entry -> entry.getValue().stream())
                    .collect(Collectors.toSet());
        }
    }
    
    public Set<ListaTecnica> getTodasListasTecnicas(
            @Nullable Collection<Produto> possiveisMateriaisOutput,
            @Nullable Collection<Produto> possiveisMateriaisInput) {

        if (possiveisMateriaisOutput == null && possiveisMateriaisInput == null) {
            return mapaListasTecnicas.entrySet().stream()
                    .map(entry -> entry.getValue())
                    .collect(Collectors.toSet());
        }

        return mapaListaTecnicaSetPorLocationMaterial.entrySet().stream()
                .flatMap(entry -> entry.getValue().entrySet().stream())
                .filter(entry -> possiveisMateriaisOutput == null || possiveisMateriaisOutput.contains(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .filter(listaTecnica -> possiveisMateriaisInput == null || possiveisMateriaisInput.containsAll(listaTecnica.getMateriaisInput()))
                .collect(Collectors.toSet());
    }

    public Set<RecursoProdutivo> getTodosRecursosProdutivos() {
        return mapaRecursosProdutivos.values().stream()
                .collect(Collectors.toSet());
    }

    public Set<Roteiro> getRoteirosViaveis(Location location, Produto material) {
        
        return mapaRoteiroViavelSetPorLocationMaterial
                .getOrDefault(location, new HashMap<>())
                .getOrDefault(material, new HashSet<>());
        
    }
    
    public Set<Roteiro> getRoteirosViaveis(
            Location location,
            @Nullable Collection<Produto> possiveisMateriaisOutput) {
        
        return mapaRoteiroViavelSetPorLocationMaterial
            .getOrDefault(location, new HashMap<>()).entrySet().stream()
                .filter(entry -> possiveisMateriaisOutput == null || possiveisMateriaisOutput.contains(entry.getKey()))
                .flatMap(x -> x.getValue().stream())
                .collect(Collectors.toSet());
        
    }
    
    public Set<Produto> getMateriaisProduzidosNaLocation(
            Location location,
            @Nullable Collection<Produto> possiveisMateriaisOutput) {
        
        return getRoteirosViaveis(location, possiveisMateriaisOutput).stream()
                .map(Roteiro::getMaterialOutput)
                .collect(Collectors.toSet());
        
    }

    public Set<Location> getLocationsOndeHaProducao(
            Produto material,
            boolean consideraVersoesProducaoParalelas,
            @Nullable Collection<Produto> possiveisMateriaisInput) {

        Set<Location> locationsComRoteiro = mapaRoteiroViavelSetPorLocationMaterial.keySet();

        return locationsComRoteiro
                .stream()
                .filter(location -> verificaSeHaProducao(location, material, consideraVersoesProducaoParalelas, possiveisMateriaisInput))
                .collect(Collectors.toSet());

    }
    
    public Set<ListaTecnica> getListasTecnicasDeVersaoProducaoPrioritaria(
            Location location, 
            Produto material, 
            boolean consideraVersoesProducaoParalelas,
            @Nullable Collection<Produto> possiveisMateriaisInput) {
        
        Optional<VersaoProducao> versaoProducaoPrioritaria = mapaVersaoProducaoViavelSetPorLocationMaterial
                .getOrDefault(location, new HashMap<>())
                .getOrDefault(material, new HashSet<>()).stream()
                        .filter(versaoProducao -> isVersaoProducaoDisponivel(versaoProducao, consideraVersoesProducaoParalelas))
                        .filter(versaoProducao -> possiveisMateriaisInput == null || possiveisMateriaisInput.containsAll(versaoProducao.getMateriaisInput()))
                        .sorted(Comparator.comparing(VersaoProducao::getPrioridade))
                        .findFirst();
        
        return versaoProducaoPrioritaria
                .map(VersaoProducao::getListasTecnicas)
                .orElseGet(HashSet::new);
        
    }
    
    public Set<ListaTecnica> getTodasListasTecnicas(
            Location location, 
            Produto material,
            @Nullable Collection<Produto> possiveisMateriaisInput) {
        return mapaListaTecnicaSetPorLocationMaterial
                .getOrDefault(location, new HashMap<>())
                .getOrDefault(material, new HashSet<>()).stream()
                .filter(listaTecnica -> possiveisMateriaisInput == null || possiveisMateriaisInput.containsAll(listaTecnica.getMateriaisInput()))
                .collect(Collectors.toSet());
    }

    public Set<ListaTecnica> getListasTecnicasViaveis(
            Location location, 
            Produto material,
            @Nullable Collection<Produto> possiveisMateriaisInput) {
        return mapaListaTecnicaViavelSetPorLocationMaterial
                .getOrDefault(location, new HashMap<>())
                .getOrDefault(material, new HashSet<>()).stream()
                .filter(listaTecnica -> possiveisMateriaisInput == null || possiveisMateriaisInput.containsAll(listaTecnica.getMateriaisInput()))
                .collect(Collectors.toSet());
    }
    
    public Set<ListaTecnica> getListasTecnicasDeVersaoProducaoPrioritaria(
            Roteiro roteiro, 
            boolean consideraVersoesProducaoParalelas,
            @Nullable Collection<Produto> possiveisMateriaisInput) {
        
        Produto material = roteiro.getMaterialOutput();
        Location location = roteiro.getLocation();
        
        Optional<VersaoProducao> versaoProducaoPrioritaria = mapaVersaoProducaoViavelSetPorLocationMaterial
                .getOrDefault(location, new HashMap<>())
                .getOrDefault(material, new HashSet<>()).stream()
                        .filter(versaoProducao -> isVersaoProducaoDisponivel(versaoProducao, consideraVersoesProducaoParalelas))
                        .filter(x -> x.getRoteiros().contains(roteiro))
                        .filter(x -> possiveisMateriaisInput == null || possiveisMateriaisInput.containsAll(x.getMateriaisInput()))
                        .sorted(Comparator.comparing(VersaoProducao::getPrioridade))
                        .findFirst();
        
        return versaoProducaoPrioritaria
                .map(VersaoProducao::getListasTecnicas)
                .orElseGet(HashSet::new);
        
    }
    
    public Set<Roteiro> getRoteirosDeVersaoProducaoPrioritaria(
            ListaTecnica listaTecnica, 
            boolean consideraVersoesProducaoParalelas) {
        
        Produto material = listaTecnica.getMaterialOutput();
        Location location = listaTecnica.getLocation();
        
        Optional<VersaoProducao> versaoProducaoPrioritaria = mapaVersaoProducaoViavelSetPorLocationMaterial
                .getOrDefault(location, new HashMap<>())
                .getOrDefault(material, new HashSet<>()).stream()
                        .filter(versaoProducao -> isVersaoProducaoDisponivel(versaoProducao, consideraVersoesProducaoParalelas))
                        .filter(x -> x.getListasTecnicas().contains(listaTecnica))
                        .sorted(Comparator.comparing(VersaoProducao::getPrioridade))
                        .findFirst();
        
        return versaoProducaoPrioritaria
                .map(VersaoProducao::getRoteiros)
                .orElseGet(HashSet::new);
        
    }
    
    public Optional<Location> getLocationOrigemPrioritaria(
            VersaoMalha versaoMalha, 
            Location locationDestino, 
            Produto material, 
            LocalDateTime dataReferenciaParaStatusProduto,
            @Nullable Collection<Location> possiveisLocationsOrigem) {
        
        Optional<LinhaTransporte> optionalLinhaTransporteInbound = getLinhaTransporteViavelPrioritariaInbound(
                versaoMalha, locationDestino, material, dataReferenciaParaStatusProduto, possiveisLocationsOrigem);
        
        return optionalLinhaTransporteInbound.map(LinhaTransporte::getLocationOrigem);
        
    }

    public Optional<Location> getLocationOrigemPrioritaria(
            Location.TipoLocation tipoLocationOrigem,
            VersaoMalha versaoMalha,
            Location locationDestino,
            Produto material,
            LocalDateTime dataReferenciaParaStatusProduto,
            @Nullable Collection<Location> possiveisLocationsOrigem) {

        Optional<LinhaTransporte> optionalLinhaTransporteInbound = getLinhaTransporteInboundViavelListOrdenadaPorPrioridade(
                versaoMalha, locationDestino, material, dataReferenciaParaStatusProduto, possiveisLocationsOrigem)
                .stream()
                .filter(linhaTransporte -> linhaTransporte.getLocationOrigem().getTipoLocation().equals(tipoLocationOrigem))
                .findFirst();

        return optionalLinhaTransporteInbound.map(LinhaTransporte::getLocationOrigem);

    }

    public Optional<LinhaTransporte> getLinhaTransporteEntreOrigemEDestino(VersaoMalha versaoMalha, Location locationOrigem, Location locationDestino) {
        
        return mapaLinhaTransporteInboundAtivaSetPorLocation
                .getOrDefault(versaoMalha, new HashMap<>())
                .getOrDefault(locationDestino, new HashSet<>()).stream()
                .filter(x -> x.getLocationOrigem().equals(locationOrigem))
                .findAny();
        
    }
        
    public Optional<ParametrosLinhaTransporte> getParametrosLinhaTransporte(VersaoMalha versaoMalha, Location locationOrigem, Location locationDestino, Produto material, LocalDateTime dataReferenciaParaStatusProduto) {
        return getParametrosLinhaTransporte(
                new LinhaTransporte(new LinhaTransporte.LinhaTransporteCompositeKey(versaoMalha, locationOrigem, locationDestino)),
                material,
                dataReferenciaParaStatusProduto);
    }
    
    public Optional<ParametrosLinhaTransporte> getParametrosLinhaTransporte(LinhaTransporte linhaTransporte, Produto material, @Nullable LocalDateTime dataReferenciaParaStatusProduto) {
        
        if (dataReferenciaParaStatusProduto != null && !verificaSeMaterialPodeSerTransferidoNaLinhaTransporte(material, linhaTransporte, dataReferenciaParaStatusProduto)) return Optional.empty();
        
        Optional<LinhaTransporteProduto> optionalLinhaTransporteProduto = Optional.ofNullable(
                mapaLinhaTransporteProdutoPorLinhaTransporteEProduto
                        .getOrDefault(linhaTransporte.getVersaoMalha(), new HashMap<>())
                        .getOrDefault(linhaTransporte, new HashMap<>())
                        .get(material));

        return Optional.of(optionalLinhaTransporteProduto
                .map(linhaTransporteProduto -> ParametrosLinhaTransporte.builder()
                        .prioridade((linhaTransporteProduto.getPrioridadeCadastrada() == null) ? linhaTransporte.getPrioridade() : linhaTransporteProduto.getPrioridadeCadastrada())
                        /*
                         * Usar o getter efetivo evita contornar a validacao do
                         * override por material. O valor cadastrado puro e
                         * mantido apenas para export/data upload.
                         */
                        .leadTimeDias(linhaTransporteProduto.getLeadTimeDias())
                        .unidadeMedidaLoteMinimoMultiploTransporte((linhaTransporteProduto.getUnidadeMedidaLoteMinimoMultiploTransporteCadastrada() == null) ?
                                clusterEParametrosProjection.getSNPUnidadeMedidaPadrao(material, linhaTransporte.getLocationDestino())
                                : linhaTransporteProduto.getUnidadeMedidaLoteMinimoMultiploTransporteCadastrada())
                        .loteMinimoTransporte((linhaTransporteProduto.getLoteMinimoTransporteCadastrado() == null) ? linhaTransporte.getLoteMinimoTransporte() : linhaTransporteProduto.getLoteMinimoTransporte())
                        .multiploTransporte((linhaTransporteProduto.getMultiploTransporteCadastrado() == null) ? linhaTransporte.getMultiploTransporte() : linhaTransporteProduto.getMultiploTransporte())
                        .build())
                .orElseGet(() -> ParametrosLinhaTransporte.builder()
                        .prioridade(linhaTransporte.getPrioridade())
                        .leadTimeDias(linhaTransporte.getLeadTimeDiasInteiro())
                        .unidadeMedidaLoteMinimoMultiploTransporte((linhaTransporte.getUnidadeMedidaLoteMinimoMultiploTransporteCadastrada() == null) ?
                                clusterEParametrosProjection.getSNPUnidadeMedidaPadrao(material, linhaTransporte.getLocationDestino())
                                : linhaTransporte.getUnidadeMedidaLoteMinimoMultiploTransporteCadastrada())
                        .loteMinimoTransporte(linhaTransporte.getLoteMinimoTransporte())
                        .multiploTransporte(linhaTransporte.getMultiploTransporte())
                        .build()));
    }

    /**
     * Retorna parâmetros de transporte quando o caller ja restringiu o fluxo a
     * linhas viaveis para o material.
     *
     * <p>Esse helper evita materializacao direta de Optional nos pontos onde a ausencia de
     * parametro nao representa "sem rota", mas sim quebra de uma premissa local:
     * a linha foi filtrada como viavel e, portanto, precisa ter prioridade e
     * lead time calculaveis para ordenar ou projetar o abastecimento.</p>
     */
        public List<LinhaTransporte> getLinhaTransporteInboundViavelListOrdenadaPorPrioridade(
            VersaoMalha versaoMalha, 
            Location locationDestino, 
            Produto material, 
            LocalDateTime dataReferenciaParaStatusProduto,
            @Nullable Collection<Location> possiveisLocationsOrigem) {
        
        return mapaLinhaTransporteInboundAtivaSetPorLocation
                .getOrDefault(versaoMalha, new HashMap<>())
                .getOrDefault(locationDestino, new HashSet<>()).stream()
                .filter(x -> possiveisLocationsOrigem == null || possiveisLocationsOrigem.contains(x.getLocationOrigem()))
                .filter(x -> verificaSeMaterialPodeSerTransferidoNaLinhaTransporte(material, x, dataReferenciaParaStatusProduto))
                .sorted(Comparator.comparing(linhaTransporte -> getParametrosLinhaTransporte(linhaTransporte, material, dataReferenciaParaStatusProduto)
                .orElseThrow(() -> new IllegalStateException(
                        "Parametros de transporte obrigatorios ausentes para material "
                                + material
                                + " na linha "
                                + linhaTransporte)).prioridade))
                .collect(Collectors.toList());
        
    }
    
    /**
     * Confronta habilitadoProdutosDescontinuados, habilitadoProdutosNaoLancados e habilitadoProdutosNaoCadastradosLinhaTransporte
     * com produto e verifica se o material pode ser transportado
     * @return
     */
    public boolean verificaSeMaterialPodeSerTransferidoNaLinhaTransporte(
            Produto material, LinhaTransporte linhaTransporte, LocalDateTime dataReferenciaParaStatusProduto) {
        
        if (!clusterEParametrosProjection.isDfuAtiva(material, linhaTransporte.getLocationOrigem())) return false;
        if (!clusterEParametrosProjection.isDfuAtiva(material, linhaTransporte.getLocationDestino())) return false;
        
        Optional<LinhaTransporteProduto> optionalLinhaTransporteProduto = Optional.ofNullable(mapaLinhaTransporteProdutoPorLinhaTransporteEProduto
                .getOrDefault(linhaTransporte.getVersaoMalha(), new HashMap<>())
                .getOrDefault(linhaTransporte, new HashMap<>())
                .getOrDefault(material, null));
        
        boolean materialAtivoNaLinhaTransporteProduto =
                optionalLinhaTransporteProduto.map(LinhaTransporteProduto::getAtivo).orElse(false);

        if ((linhaTransporte.getHabilitadoProdutosNaoCadastradosLinhaTransporte() && !optionalLinhaTransporteProduto.isPresent()) ||
                materialAtivoNaLinhaTransporteProduto) {
            
            if (linhaTransporte.getHabilitadoProdutosDescontinuados() && linhaTransporte.getHabilitadoProdutosNaoLancados()) {
                return true;
            } else {
                
                Constantes.StatusProduto statusProduto = clusterEParametrosProjection.getStatusProduto(
                        material, linhaTransporte.getLocationDestino(), dataReferenciaParaStatusProduto);
            
                if (linhaTransporte.getHabilitadoProdutosDescontinuados() || !statusProduto.equals(Constantes.StatusProduto.DESCONTINUADO)) {
                    if (linhaTransporte.getHabilitadoProdutosNaoLancados() || !statusProduto.equals(Constantes.StatusProduto.NAO_LANCADO)) {
                        return true;
                    }
                }
                
            }
        }
        return false;
    }

    public boolean verificaSeMaterialPodeSerTransferidoNaLinhaTransporte(
            Produto material, VersaoMalha versaoMalha, Location locationOrigem, Location locationDestino, LocalDateTime dataReferenciaParaStatusProduto) {
        LinhaTransporte linhaTransporteTemporaria = new LinhaTransporte(new LinhaTransporte.LinhaTransporteCompositeKey(versaoMalha, locationOrigem, locationDestino));
        return verificaSeMaterialPodeSerTransferidoNaLinhaTransporte(material, linhaTransporteTemporaria, dataReferenciaParaStatusProduto);
    }
    
    public Optional<LinhaTransporteProduto> getLinhaTransporteMaterial(LinhaTransporte linhaTransporte, Produto material) {
        
        return Optional.ofNullable(mapaLinhaTransporteProdutoPorLinhaTransporteEProduto
                .getOrDefault(linhaTransporte.getVersaoMalha(), new HashMap<>())
                .getOrDefault(linhaTransporte, new HashMap<>())
                .get(material));
        
    }
    
    /**
     * Retorna materiais que podem trafegar pela linha de transporte
     * Não leva em consideração se material está ativo na origem ou destino
     * @param linhaTransporte
     * @return 
     */
    public Set<Produto> getMateriaisHabilitadosEmLinhaTransporte(
            LinhaTransporte linhaTransporte, 
            LocalDateTime dataReferenciaParaStatusProduto,
            @Nullable Collection<Produto> possiveisMateriais) {
        
        Collection<LinhaTransporteProduto> linhasTransporteProduto = mapaLinhaTransporteProdutoPorLinhaTransporteEProduto
                .getOrDefault(linhaTransporte.getVersaoMalha(), new HashMap<>())
                .getOrDefault(linhaTransporte, new HashMap<>())
                .values();
        
        Set<Produto> materiaisExplicitamenteDesabilitadosLinhaTransporteProduto = linhasTransporteProduto.stream()
                .filter(linhaTransporteProduto -> {
                    
                    if (!linhaTransporteProduto.getAtivo()) return true;
                    if (linhaTransporte.getHabilitadoProdutosDescontinuados() && linhaTransporte.getHabilitadoProdutosNaoLancados()) return false;
                    
                    Produto material = linhaTransporteProduto.getProduto();
                    if (possiveisMateriais != null && !possiveisMateriais.contains(material)) return false;
                    
                    Constantes.StatusProduto statusProduto = clusterEParametrosProjection.getStatusProduto(
                        material, linhaTransporte.getLocationDestino(), dataReferenciaParaStatusProduto);
                    
                    if (statusProduto.equals(Constantes.StatusProduto.DESCONTINUADO) && !linhaTransporte.getHabilitadoProdutosDescontinuados()) return true;
                    if (statusProduto.equals(Constantes.StatusProduto.NAO_LANCADO) && !linhaTransporte.getHabilitadoProdutosNaoLancados()) return true;
                    
                    return false;
                    
                })
                .map(linhaTransporteProduto -> linhaTransporteProduto.getProduto())
                .collect(Collectors.toSet());
        
        Set<Produto> conjuntoBaseProdutos = (linhaTransporte.getHabilitadoProdutosNaoCadastradosLinhaTransporte()) ? 
                new HashSet((possiveisMateriais == null) ?
                        clusterEParametrosProjection.getMaterialSet()
                        : possiveisMateriais)
                : linhasTransporteProduto.stream().map(linhaTransporteProduto -> linhaTransporteProduto.getProduto()).collect(Collectors.toSet());

        conjuntoBaseProdutos.removeAll(materiaisExplicitamenteDesabilitadosLinhaTransporteProduto);
        
        return conjuntoBaseProdutos;

    }
    
    public Optional<Integer> getLeadTimeEmPeriodosMaximo(
            VersaoMalha versaoMalha, 
            Location locationDestino, 
            Produto material, 
            Calendario calendario, 
            LocalDateTime dataReferenciaParaStatusProduto,
            @Nullable Collection<Location> possiveisLocationsOrigem) {
        
        Set<LinhaTransporte> linhaTransporteSet = getLinhaTransporteInboundViavelSetParaLocationMaterial(
                versaoMalha, locationDestino, material, dataReferenciaParaStatusProduto, possiveisLocationsOrigem);
        
        Integer leadTimeMaximoPeriodos = null;
        for (LinhaTransporte linhaTransporte : linhaTransporteSet) {
            Optional<Integer> leadTimeEmPeriodos = getLeadTimePeriodosEntreOrigemDestinoParaMaterial(versaoMalha, linhaTransporte.getLocationOrigem(), locationDestino, material, calendario, dataReferenciaParaStatusProduto);
            if (leadTimeEmPeriodos.isPresent()) {
                int leadTimeEmPeriodosAtual = leadTimeEmPeriodos.orElseThrow(() -> new IllegalStateException(
                        "Lead time em períodos presente não pode desaparecer durante soma de lead time máximo"));
                if (leadTimeMaximoPeriodos == null) { 
                    leadTimeMaximoPeriodos = leadTimeEmPeriodosAtual;
                } else {
                    leadTimeMaximoPeriodos += leadTimeEmPeriodosAtual;
                }
            }
        }
        
        return Optional.ofNullable(leadTimeMaximoPeriodos);
        
    }
    
    public Optional<Integer> getLeadTimeEmDiasDeOrigemPrioritaria(
            VersaoMalha versaoMalha, 
            Location locationDestino, 
            Produto material, 
            LocalDateTime dataReferenciaParaStatusProduto,
            @Nullable Collection<Location> possiveisLocationsOrigem) {

        Optional<LinhaTransporte> optionalLinhaTransporteInboundPrioritaria = getLinhaTransporteViavelPrioritariaInbound(
                versaoMalha, locationDestino, material, dataReferenciaParaStatusProduto, possiveisLocationsOrigem);
        
        if (!optionalLinhaTransporteInboundPrioritaria.isPresent()) {
            return Optional.empty();
        } else {
            return optionalLinhaTransporteInboundPrioritaria
                    .map(linhaTransporte -> getParametrosLinhaTransporte(linhaTransporte, material, dataReferenciaParaStatusProduto)
                .orElseThrow(() -> new IllegalStateException(
                        "Parametros de transporte obrigatorios ausentes para material "
                                + material
                                + " na linha "
                                + linhaTransporte)).leadTimeDias);
        }
        
    }
    
    public Optional<Integer> getLeadTimeEmPeriodosDeOrigemPrioritaria(
            VersaoMalha versaoMalha, 
            Location locationDestino, 
            Produto material, 
            Calendario calendario, 
            LocalDateTime dataReferenciaParaStatusProduto,
            @Nullable Collection<Location> possiveisLocationsOrigem) {
        
        Optional<Integer> optionalLeadTimeDias = getLeadTimeEmDiasDeOrigemPrioritaria(
                versaoMalha, locationDestino, material, dataReferenciaParaStatusProduto, possiveisLocationsOrigem);
        
        if (!optionalLeadTimeDias.isPresent()) {
            return Optional.empty();
        } else {
            return optionalLeadTimeDias
                    .map(leadTimeDias -> (int) Math.floor(calendario.converteDiasParaPeriodosCalendario(
                            leadTimeDias)));
        }
                
    }
    
    public LocalDateTime getDataHorarioFinalProducao(
            LocalDateTime dataHorarioInicial, 
            Roteiro roteiro,
            double quantidade,
            UnidadeMedida unidadeMedidaQuantidade) {
        
        // extrai roteiro com operações prepopuladas (para evitar N+1)
        Roteiro roteiroConsiderado = getRoteiroFromId(roteiro.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Roteiro " + roteiro.getId() + " não encontrado na SupplyNetworkProjection"));
        
        double horasTotais = roteiroConsiderado.getOperacaoRoteiroSet().stream()
                .map(operacao -> getHorasTotaisConsumidasDeOperacao(operacao, quantidade, unidadeMedidaQuantidade))
                .reduce(0.0, (horasTotais1, horasTotais2) ->
                        Math.max(horasTotais1, horasTotais2));
        
        long numeroSegundos = (long) Math.ceil(horasTotais * 3600 / quantidade);
        
        return dataHorarioInicial.plusSeconds(numeroSegundos);
        
    }
    
    public double getHorasTotaisConsumidasDeOperacao(
            OperacaoRoteiro operacao,
            double quantidade, UnidadeMedida unidadeMedidaQuantidade) {
        
        ParametrosGlobais parametrosGlobais = getClusterEParametrosProjection().getParametrosGlobais();
        
        Roteiro roteiro = operacao.getRoteiro();
        double conversaoParaUnidadeRoteiroOperacao = conversaoUnidadeMedidaProjection.getConversaoParaUnidadeDestino(
                roteiro.getMaterialOutput(),
                unidadeMedidaQuantidade, 
                roteiro.getUnidadeMedidaQuantidadeBase(parametrosGlobais));

        double consumoHoras = quantidade
                * conversaoParaUnidadeRoteiroOperacao
                / roteiro.getQuantidadeBase()
                * operacao.getHorasPorQuantidadeBase()
                / operacao.getRecursoProdutivo().getEficiencia();

        return consumoHoras;
        
    }

    public double getConsumoCapacidadePorRecursoProdutivoEmHorasOuQuantidade(
            Roteiro roteiro,
            RecursoProdutivo recursoProdutivo,
            double quantidade,
            UnidadeMedida unidadeMedidaQuantidade,
            PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva) {

        Roteiro roteiroPopulado = getRoteiroFromId(roteiro.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Roteiro " + roteiro.getId() + " não encontrado na SupplyNetworkProjection"));
        Produto material = roteiroPopulado.getMaterialOutput();

        ParametrosGlobais parametrosGlobais = clusterEParametrosProjection.getParametrosGlobais();

        double consumoCapacidade =  0.0;
        for (OperacaoRoteiro operacao : roteiroPopulado.getOperacaoRoteiroSet()) {

            RecursoProdutivo recursoProdutivoOperacao = operacao.getRecursoProdutivo();

            if (recursoProdutivoOperacao.equals(recursoProdutivo)) {

                switch (tipoCapacidadeProdutiva) {
                    case QUANTIDADE_POR_UOM:
                        UnidadeMedida unidadeMedidaCapacidadeRecursoProdutivo = recursoProdutivo.getUnidadeMedidaCapacidadeEmUom(parametrosGlobais);

                        consumoCapacidade += conversaoUnidadeMedidaProjection.getConversaoParaUnidadeDestino(
                                material,
                                unidadeMedidaQuantidade,
                                unidadeMedidaCapacidadeRecursoProdutivo);
                        break;
                    default:
                        consumoCapacidade += getHorasTotaisConsumidasDeOperacao(operacao,
                                (float) quantidade,
                                unidadeMedidaQuantidade);
                        break;
                }
            }

        }
        return consumoCapacidade;

    }

    /**
     * Retorna o consumo total de horas ou quantidade (sugestões + ordens firmes) consumidas
     * A definição do tipo de consumo é feita no proprio recurso produtivo
     */
    public Map<RecursoProdutivo,Double> getConsumoCapacidadePorRecursoProdutivoEmHorasOuQuantidade(
            Roteiro roteiro,
            double quantidade,
            UnidadeMedida unidadeMedidaQuantidade,
            PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva) {
        
        Roteiro roteiroPopulado = getRoteiroFromId(roteiro.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Roteiro " + roteiro.getId() + " não encontrado na SupplyNetworkProjection"));
        Produto material = roteiroPopulado.getMaterialOutput();
        
        Map<RecursoProdutivo,Double> mapaConsumoCapacidadePorRecursoProdutivo = new HashMap<>();
        
        ParametrosGlobais parametrosGlobais = clusterEParametrosProjection.getParametrosGlobais();
                
        // O roteiro e recarregado a partir da projection para garantir operacoes e recursos produtivos
        // populados antes do calculo de capacidade. Manter esta leitura centralizada evita lazy loading
        // acidental durante a rotina heuristica.
        
        for (OperacaoRoteiro operacao : roteiroPopulado.getOperacaoRoteiroSet()) {
        
            RecursoProdutivo recursoProdutivo = operacao.getRecursoProdutivo();
            
            switch (tipoCapacidadeProdutiva) {
                case QUANTIDADE_POR_UOM:
                    UnidadeMedida unidadeMedidaCapacidadeRecursoProdutivo = recursoProdutivo.getUnidadeMedidaCapacidadeEmUom(parametrosGlobais);
                    
                    FuncoesMap.updateElementoNoNestedMap(
                            // valor inicialmente é nulo pois não há valores associados à chave
                            0.0,
                            valor -> valor + conversaoUnidadeMedidaProjection.getConversaoParaUnidadeDestino(
                                    material, unidadeMedidaQuantidade, unidadeMedidaCapacidadeRecursoProdutivo), 
                            Double.class,
                            mapaConsumoCapacidadePorRecursoProdutivo, 
                            recursoProdutivo);
                    break;
                // tanto capacidade em turnos (convertidos em horas) ou diretamente em horas
                default:
                    FuncoesMap.updateElementoNoNestedMap(
                            // valor inicialmente é nulo pois não há valores associados à chave
                            0.0,
                            valor -> valor + getHorasTotaisConsumidasDeOperacao(operacao, quantidade, unidadeMedidaQuantidade),
                            Double.class,
                            mapaConsumoCapacidadePorRecursoProdutivo, 
                            recursoProdutivo);
                    break;
            }
            
        }
        return mapaConsumoCapacidadePorRecursoProdutivo;
    }  
    
    public Map<RecursoProdutivo,Double> getConsumoCapacidadePorRecursoProdutivoEmHoras(
            Roteiro roteiro,
            double quantidade,
            UnidadeMedida unidadeMedidaQuantidade) throws UnitOfMeasureConversionException {
        
        Roteiro roteiroPopulado = getRoteiroFromId(roteiro.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Roteiro " + roteiro.getId() + " não encontrado na SupplyNetworkProjection"));
        
        Map<RecursoProdutivo,Double> mapaConsumoCapacidadePorRecursoProdutivo = new HashMap<>();
                        
        // O roteiro e recarregado a partir da projection para garantir operacoes e recursos produtivos
        // populados antes do calculo de capacidade. Manter esta leitura centralizada evita lazy loading
        // acidental durante a rotina heuristica.
        
        for (OperacaoRoteiro operacao : roteiroPopulado.getOperacaoRoteiroSet()) {
        
            RecursoProdutivo recursoProdutivo = operacao.getRecursoProdutivo();

            FuncoesMap.updateElementoNoNestedMap(
                    // valor inicialmente é nulo pois não há valores associados à chave
                    0.0,
                    valor -> valor + getHorasTotaisConsumidasDeOperacao(operacao, quantidade, unidadeMedidaQuantidade),
                    Double.class,
                    mapaConsumoCapacidadePorRecursoProdutivo, 
                    recursoProdutivo);
            
        }
        
        return mapaConsumoCapacidadePorRecursoProdutivo;
        
    }  
        
    public Set<LinhaTransporte> getLinhaTransportePrioritariaSetOutbound(
            VersaoMalha versaoMalha, 
            Location locationOrigem,
            Produto material,
            LocalDateTime dataReferenciaParaStatusProduto,
            @Nullable Collection<Location> possiveisLocationsDestino) {
        
        return mapaLinhaTransporteOutboundAtivaSetPorLocation
                .getOrDefault(versaoMalha, new HashMap<>())
                .getOrDefault(locationOrigem, new HashSet<>()).stream()
                .filter(x -> possiveisLocationsDestino == null || possiveisLocationsDestino.contains(x.getLocationDestino()))
                .filter(x -> verificaSeMaterialPodeSerTransferidoNaLinhaTransporte(material, x, dataReferenciaParaStatusProduto))
                .filter(x -> {
                    Optional<LinhaTransporte> optionalLinhaTransporteInboundPrioritariaParaDestino = getLinhaTransporteViavelPrioritariaInbound(
                            versaoMalha, x.getLocationDestino(), material, dataReferenciaParaStatusProduto, null);
                    return optionalLinhaTransporteInboundPrioritariaParaDestino
                            .map(linhaTransporteInboundPrioritariaParaDestino ->
                                    linhaTransporteInboundPrioritariaParaDestino.equals(x))
                            .orElse(false);
                })
                .collect(Collectors.toSet());
        
    }
    
    public Set<Location> getLocationDestinoPrioritarioSet(
            VersaoMalha versaoMalha, 
            Location locationOrigem, 
            Produto material, 
            LocalDateTime dataReferenciaParaStatusProduto,
            @Nullable Collection<Location> possiveisLocationsDestino) {
        return getLinhaTransportePrioritariaSetOutbound(versaoMalha, locationOrigem, material, dataReferenciaParaStatusProduto, possiveisLocationsDestino).stream()
                .map(LinhaTransporte::getLocationDestino)
                .collect(Collectors.toSet());
    }
    
    public Set<Location> getLocationDestinoViavelSet(
            VersaoMalha versaoMalha, 
            Location locationOrigem, 
            @Nullable Collection<Location> possiveisLocationsDestino) {
        return mapaLinhaTransporteOutboundAtivaSetPorLocation
                .getOrDefault(versaoMalha, new HashMap<>())
                .getOrDefault(locationOrigem, new HashSet<>())
                .stream()
                .filter(x -> possiveisLocationsDestino == null || possiveisLocationsDestino.contains(x.getLocationDestino()))
                .map(x -> x.getLocationDestino())
                .collect(Collectors.toSet());
    }
    
    public Set<Location> getLocationDestinoViavelSet(
            VersaoMalha versaoMalha, 
            Location locationOrigem, 
            Produto material, 
            LocalDateTime dataReferenciaParaStatusProduto,
            @Nullable Collection<Location> possiveisLocationsDestino) {
        return mapaLinhaTransporteOutboundAtivaSetPorLocation
                .getOrDefault(versaoMalha, new HashMap<>())
                .getOrDefault(locationOrigem, new HashSet<>()).stream()
                .filter(x -> possiveisLocationsDestino == null || possiveisLocationsDestino.contains(x.getLocationDestino()))
                .filter(x -> verificaSeMaterialPodeSerTransferidoNaLinhaTransporte(material, x, dataReferenciaParaStatusProduto))
                .map(LinhaTransporte::getLocationDestino)
                .collect(Collectors.toSet());
    }
    
    public Set<Location> getLocationOrigemViavelSet(
            VersaoMalha versaoMalha, 
            Location locationDestino, 
            Produto material, 
            LocalDateTime dataReferenciaParaStatusProduto,
            @Nullable Collection<Location> possiveisLocationsOrigem) {
        return mapaLinhaTransporteInboundAtivaSetPorLocation
                .getOrDefault(versaoMalha, new HashMap<>())
                .getOrDefault(locationDestino, new HashSet<>()).stream()
                .filter(x -> possiveisLocationsOrigem == null || possiveisLocationsOrigem.contains(x.getLocationOrigem()))
                .filter(x -> verificaSeMaterialPodeSerTransferidoNaLinhaTransporte(material, x, dataReferenciaParaStatusProduto))
                .map(LinhaTransporte::getLocationOrigem)
                .collect(Collectors.toSet());
    }
    
    public Set<ListaTecnica> getListaTecnicaViavelPrioritariaSet(
            Location location, 
            boolean consideraVersoesProducaoParalelas,
            @Nullable Collection<Produto> possiveisMateriaisInput) {
        
        return mapaVersaoProducaoViavelPrioritariaPorLocationProduto
                .getOrDefault(location, new HashMap<>()).values().stream()
                .filter(versaoProducao -> isVersaoProducaoDisponivel(versaoProducao, consideraVersoesProducaoParalelas))
                .flatMap(x -> x.getListasTecnicas().stream())
                .filter(listaTecnica -> possiveisMateriaisInput == null || possiveisMateriaisInput.containsAll(listaTecnica.getMateriaisInput()))
                .collect(Collectors.toSet());
        
    }
    
    public Set<ListaTecnica> getListaTecnicaViavelPrioritariaSetOndeMaterialEInput(
            Location location, 
            Produto materialInput, 
            boolean consideraVersoesProducaoParalelas,
            @Nullable Collection<Produto> possiveisMateriaisOutput) {
        
        inicializaMapaListaTecnicaViavelPrioritariaSetOndeMaterialEInput(consideraVersoesProducaoParalelas);
        
        // retorna o valor do mapa já pronto
        return mapaListaTecnicaViavelPrioritariaSetOndeMaterialEInput
                .getOrDefault(location, new HashMap<>())
                .getOrDefault(materialInput, new HashSet<>()).stream()
                .filter(listaTecnica -> possiveisMateriaisOutput == null || possiveisMateriaisOutput.contains(listaTecnica.getMaterialOutput()))
                .collect(Collectors.toSet());
        
    }

    public Set<Produto> getMateriaisProduzidosNaLocationQueNaoSaoInputListaTecnica(
            Location location,
            @Nullable Collection<Produto> possiveisMateriaisOutput) {

        return getMateriaisProduzidosNaLocation(location, possiveisMateriaisOutput)
                .stream()
                .filter(material -> getListaTecnicaViavelPrioritariaSetOndeMaterialEInput(
                        location,
                        material,
                        true,
                        null)
                        .isEmpty())
                .collect(Collectors.toSet());

    }

    public Map<Location,Set<ListaTecnica>> getListaTecnicaViavelPrioritariaSetOndeMaterialEInputPorLocation(
            Produto materialInput, 
            boolean consideraVersoesProducaoParalelas,
            @Nullable Collection<Produto> possiveisMateriaisOutput) {
        
        inicializaMapaListaTecnicaViavelPrioritariaSetOndeMaterialEInput(consideraVersoesProducaoParalelas);
        
        // retorna o valor do mapa já pronto
        return mapaListaTecnicaViavelPrioritariaSetOndeMaterialEInput
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        (Entry<Location,Map<Produto,Set<ListaTecnica>>> entry) -> entry.getKey(),
                        (Entry<Location,Map<Produto,Set<ListaTecnica>>> entry) -> entry.getValue().getOrDefault(materialInput, new HashSet<>())
                                .stream()
                                .filter(listaTecnica -> possiveisMateriaisOutput == null || possiveisMateriaisOutput.contains(listaTecnica.getMaterialOutput()))
                                .collect(Collectors.toSet())));
        
    }
    
    // Community chama esta cache apenas com versoes paralelas desabilitadas. No Enterprise, onde
    // parallel routing/output volta a existir, a implementacao deve separar caches por modo.
    private void inicializaMapaListaTecnicaViavelPrioritariaSetOndeMaterialEInput(boolean consideraVersoesProducaoParalelas) {
        // inicializa mapa, se for nulo
        if (mapaListaTecnicaViavelPrioritariaSetOndeMaterialEInput == null) {
            mapaListaTecnicaViavelPrioritariaSetOndeMaterialEInput = new HashMap<>();
            
            for (Location locationIterada : mapaVersaoProducaoViavelPrioritariaPorLocationProduto.keySet()) {
                mapaListaTecnicaViavelPrioritariaSetOndeMaterialEInput.put(locationIterada, new HashMap<>());
                // nao lança possiveisMateriaisOutput pois o mapa em memória deverá ser completo. o filtro ocorrerá na linha Return, quando ele é lido
                for (VersaoProducao versaoProducao : getVersoesProducaoViaveisPrioritarias(
                        locationIterada, consideraVersoesProducaoParalelas, null, null)) {
                    for (ListaTecnica listaTecnica : versaoProducao.getListasTecnicas()) {
                        ListaTecnica listaTecnicaPersistidaCompleta = getListaTecnicaFromId(listaTecnica.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Lista técnica " + listaTecnica.getId() + " não encontrada na SupplyNetworkProjection"));
                        for (ListaTecnicaComponente listaTecnicaComponente : listaTecnicaPersistidaCompleta.getListaTecnicaComponenteSet()) {
                            mapaListaTecnicaViavelPrioritariaSetOndeMaterialEInput.get(locationIterada).computeIfAbsent(
                                    listaTecnicaComponente.getMaterialComponente(), x -> new HashSet<>())
                                    .add(listaTecnicaPersistidaCompleta);
                        }
                    }
                }
            }
        }
    }
    
    public Optional<LinhaTransporte> getLinhaTransporteViavelPrioritariaInbound(
            VersaoMalha versaoMalha, 
            Location locationDestino, 
            Produto material, 
            LocalDateTime dataReferenciaParaStatusProduto,
            @Nullable Collection<Location> possiveisLocationsOrigem) {
        
        return getLinhaTransporteInboundViavelListOrdenadaPorPrioridade(
                versaoMalha, locationDestino, material, dataReferenciaParaStatusProduto, possiveisLocationsOrigem)
                .stream()
                .findFirst();
        
    }
    
    public List<LinhaTransporte> getLinhasTransporteAtivasInboundOrdenadasPorPrioridade(
            VersaoMalha versaoMalha, 
            Location locationDestino,
            @Nullable Collection<Location> possiveisLocationsOrigem) {
        
        return mapaLinhaTransporteInboundAtivaSetPorLocation
                .getOrDefault(versaoMalha, new HashMap<>())
                .getOrDefault(locationDestino, new HashSet<>()).stream()
                .filter(linhaTransporte -> possiveisLocationsOrigem == null || possiveisLocationsOrigem.contains(linhaTransporte.getLocationOrigem()))
                .sorted(Comparator.comparing(x -> x.getPrioridade()))
                .collect(Collectors.toList());
        
    }
    
    public Set<Produto> getMateriaisProduzidosLocation(Location location) {
        return mapaRoteiroViavelSetPorLocationMaterial.getOrDefault(location, new HashMap<>()).keySet();
    }
    
    public Set<RecursoProdutivo> getRecursoProdutivoAtivoSet(Location location) {
        return mapaRecursoProdutivoAtivoSetPorLocation.getOrDefault(location, new HashSet<>());
    }
    
    /**
     * Retorna o conjunto de materiais com roteiros ativos que façam uso do recurso produtivo indicado
     * @param recursoProdutivo
     * @return 
     */
    public Set<Produto> getMaterialSetProduzidoEmRecursoProdutivo(RecursoProdutivo recursoProdutivo) {
        Map<Produto, Set<Roteiro>> roteirosViaveisPorMaterial = mapaRoteiroViavelSetPorLocationMaterial.get(recursoProdutivo.getLocation());

        return roteirosViaveisPorMaterial.entrySet().stream()
                .filter(x -> x.getValue().stream()
                        .anyMatch(y -> y.getRecursoProdutivoSet().contains(recursoProdutivo)))
                .map(x -> x.getKey())
                .collect(Collectors.toSet());
        
    }
    
    public List<VersaoProducao> getVersoesProducaoViaveisOrdenadasPorPrioridade(
            Location location, 
            Produto material, 
            boolean consideraVersoesProducaoParalelas,
            @Nullable Collection<Produto> possiveisMateriaisInput) {
        
        Set<VersaoProducao> versaoProducaoViavelSet = mapaVersaoProducaoViavelSetPorLocationMaterial
                .getOrDefault(location, new HashMap<>())
                .getOrDefault(material, new HashSet<>());
        
        return versaoProducaoViavelSet.stream()
                .filter(versaoProducao -> isVersaoProducaoDisponivel(versaoProducao, consideraVersoesProducaoParalelas))
                .filter(versaoProducao -> possiveisMateriaisInput == null || possiveisMateriaisInput.containsAll(
                        versaoProducao.getMateriaisInput()))
                .sorted(Comparator.comparing(VersaoProducao::getPrioridade))
                .collect(Collectors.toList());
        
    }

    public boolean verificaSeHaProducao(
            Location location,
            Produto material,
            boolean consideraVersoesProducaoParalelas,
            @Nullable Collection<Produto> possiveisMateriaisInput) {

        Set<VersaoProducao> versaoProducaoViavelSet = mapaVersaoProducaoViavelSetPorLocationMaterial
                .getOrDefault(location, new HashMap<>())
                .getOrDefault(material, new HashSet<>());

        return versaoProducaoViavelSet.stream()
                .anyMatch(versaoProducao ->
                        isVersaoProducaoDisponivel(versaoProducao, consideraVersoesProducaoParalelas)
                        && (possiveisMateriaisInput == null || possiveisMateriaisInput.containsAll(versaoProducao.getMateriaisInput())));

    }
    
    public Optional<VersaoProducao> getVersaoProducaoViavelPrioritaria(
            Roteiro roteiro, 
            ListaTecnica listaTecnica, 
            boolean consideraVersoesProducaoParalelas,
            @Nullable Collection<Produto> possiveisMateriaisInput) {

        /*
         * Roteiro e lista tecnica formam uma combinacao produtiva somente
         * quando compartilham a mesma location e o mesmo material output. Esse
         * metodo e usado em ajustes manuais, heuristico e projections salvas;
         * portanto uma incompatibilidade deve falhar cedo em vez de retornar
         * Optional.empty() e mascarar cadastro inconsistente como ausencia de
         * versao de producao.
         */
        if (!Objects.equals(roteiro.getLocation(), listaTecnica.getLocation())) {
            throw getIncompatibleRoutingAndBomLocationException(roteiro, listaTecnica);
        }
        if (!Objects.equals(roteiro.getMaterialOutput(), listaTecnica.getMaterialOutput())) {
            throw getIncompatibleRoutingAndBomMaterialOutputException(roteiro, listaTecnica);
        }
        
        Location location = roteiro.getLocation();
        Produto materialOutput = roteiro.getMaterialOutput();
        
        return getVersoesProducaoViaveisOrdenadasPorPrioridade(
                location, materialOutput, consideraVersoesProducaoParalelas, possiveisMateriaisInput).stream()
                .filter(versaoProducao -> versaoProducao.getRoteiros().contains(roteiro))
                .filter(versaoProducao -> versaoProducao.getListasTecnicas().contains(listaTecnica))
                .findFirst();
        
    }    

    private IllegalArgumentException getIncompatibleRoutingAndBomLocationException(
            Roteiro roteiro,
            ListaTecnica listaTecnica) {

        return new IllegalArgumentException(
                "SupplyNetworkProjection requires routing and BOM to share the same location; routing "
                        + getId(roteiro)
                        + " uses location "
                        + getId(roteiro.getLocation())
                        + " and BOM "
                        + getId(listaTecnica)
                        + " uses location "
                        + getId(listaTecnica.getLocation())
                        + ".");

    }

    private IllegalArgumentException getIncompatibleRoutingAndBomMaterialOutputException(
            Roteiro roteiro,
            ListaTecnica listaTecnica) {

        return new IllegalArgumentException(
                "SupplyNetworkProjection requires routing and BOM to share the same output material; routing "
                        + getId(roteiro)
                        + " outputs material "
                        + getId(roteiro.getMaterialOutput())
                        + " and BOM "
                        + getId(listaTecnica)
                        + " outputs material "
                        + getId(listaTecnica.getMaterialOutput())
                        + ".");

    }

    private static String getId(Object objetoDominio) {

        if (objetoDominio == null) {
            return "null";
        }
        if (objetoDominio instanceof Location location) {
            return location.getId();
        }
        if (objetoDominio instanceof Produto material) {
            return material.getId();
        }
        if (objetoDominio instanceof Roteiro roteiro) {
            return roteiro.getId();
        }
        if (objetoDominio instanceof ListaTecnica listaTecnica) {
            return listaTecnica.getId();
        }
        return objetoDominio.toString();

    }
    
    public Optional<VersaoProducao> getVersaoProducaoViavelPrioritaria(
            Location location, 
            Produto material, 
            boolean consideraVersoesProducaoParalelas,
            @Nullable Collection<Produto> possiveisMateriaisInput) {
        
        return getVersoesProducaoViaveisOrdenadasPorPrioridade(
                location, material, consideraVersoesProducaoParalelas, possiveisMateriaisInput).stream().findFirst();
        
    }
    
    public Optional<VersaoProducaoSimples> getVersaoProducaoSimplesViavelPrioritaria(
            Location location,
            Produto material,
            @Nullable Collection<Produto> possiveisMateriaisInput) {
        
        return mapaVersaoProducaoViavelSetPorLocationMaterial
                .getOrDefault(location, new HashMap<>())
                .getOrDefault(material, new HashSet<>()).stream()
                .filter(x -> x instanceof VersaoProducaoSimples)
                .filter(x -> possiveisMateriaisInput == null || possiveisMateriaisInput.containsAll(((VersaoProducaoSimples) x).getListaTecnica().getMateriaisInput()))
                .sorted(Comparator.comparing(x -> x.getPrioridade()))
                .map(x -> (VersaoProducaoSimples) x)
                .findFirst();
        
    }
        
    public Set<VersaoProducao> getVersoesProducaoViaveisPrioritarias(
            Location location, 
            boolean consideraVersoesProducaoParalelas,
            @Nullable Collection<Produto> possiveisMateriaisOutput,
            @Nullable Collection<Produto> possiveisMateriaisInput) {
        
        return mapaVersaoProducaoViavelSetPorLocationMaterial
                .getOrDefault(location, new HashMap<>()).values().stream()
                .flatMap(x -> x.stream())
                .filter(versaoProducao -> isVersaoProducaoDisponivel(versaoProducao, consideraVersoesProducaoParalelas))
                .filter(versaoProducao -> possiveisMateriaisOutput == null || possiveisMateriaisOutput.containsAll(versaoProducao.getMateriaisOutput()))
                .filter(versaoProducao -> possiveisMateriaisInput == null || possiveisMateriaisInput.containsAll(versaoProducao.getMateriaisInput()))
                .sorted(Comparator.comparing(x -> x.getPrioridade()))
                .map(x -> x)
                .collect(Collectors.toSet());
        
    }
    
    public Optional<VersaoProducaoSimples> getVersaoProducaoSimplesViavelPrioritaria(
            Roteiro roteiro, 
            ListaTecnica listaTecnica) {
        
        if (!Objects.equals(roteiro.getLocation(), listaTecnica.getLocation())) {
            throw getIncompatibleRoutingAndBomLocationException(roteiro, listaTecnica);
        }
        if (!Objects.equals(roteiro.getMaterialOutput(), listaTecnica.getMaterialOutput())) {
            throw getIncompatibleRoutingAndBomMaterialOutputException(roteiro, listaTecnica);
        }

        Location location = roteiro.getLocation();
        Produto material = roteiro.getMaterialOutput();

        return mapaVersaoProducaoViavelSetPorLocationMaterial
                .getOrDefault(location, new HashMap<>())
                .getOrDefault(material, new HashSet<>()).stream()
                .filter(x -> x instanceof VersaoProducaoSimples)
                .filter(x -> ((VersaoProducaoSimples) x).getRoteiro().equals(roteiro) && ((VersaoProducaoSimples) x).getListaTecnica().equals(listaTecnica)) 
                .sorted(Comparator.comparing(x -> x.getPrioridade()))
                .map(x -> (VersaoProducaoSimples) x)
                .findFirst();
                        
    }    
    
    public Optional<VersaoProducaoSimples> getVersaoProducaoSimplesPrioritaria(
            Roteiro roteiro, 
            ListaTecnica listaTecnica) {
        
        if (!Objects.equals(roteiro.getLocation(), listaTecnica.getLocation())) {
            throw getIncompatibleRoutingAndBomLocationException(roteiro, listaTecnica);
        }
        if (!Objects.equals(roteiro.getMaterialOutput(), listaTecnica.getMaterialOutput())) {
            throw getIncompatibleRoutingAndBomMaterialOutputException(roteiro, listaTecnica);
        }

        Location location = roteiro.getLocation();
        Produto material = roteiro.getMaterialOutput();

        return mapaVersaoProducaoSetPorLocationMaterial
                .getOrDefault(location, new HashMap<>())
                .getOrDefault(material, new HashSet<>()).stream()
                .filter(x -> x instanceof VersaoProducaoSimples)
                .filter(x -> ((VersaoProducaoSimples) x).getRoteiro().equals(roteiro) && ((VersaoProducaoSimples) x).getListaTecnica().equals(listaTecnica)) 
                .sorted(Comparator.comparing(x -> x.getPrioridade()))
                .map(x -> (VersaoProducaoSimples) x)
                .findFirst();
                        
    }
    
    public Set<LinhaTransporte> getLinhaTransporteInboundViavelSetParaLocationMaterial(
            VersaoMalha versaoMalha, 
            Location locationDestino, 
            Produto material, 
            LocalDateTime dataReferenciaParaStatusProduto,
            @Nullable Collection<Location> possiveisLocationsOrigem) {
        
        return mapaLinhaTransporteInboundAtivaSetPorLocation
                .getOrDefault(versaoMalha, new HashMap<>())
                .getOrDefault(locationDestino, new HashSet<>()).stream()
                        .filter(linhaTransporte -> possiveisLocationsOrigem == null || possiveisLocationsOrigem.contains(linhaTransporte.getLocationOrigem()))
                        .filter(linhaTransporte -> verificaSeMaterialPodeSerTransferidoNaLinhaTransporte(material, linhaTransporte, dataReferenciaParaStatusProduto))
                        .collect(Collectors.toSet());
                
    }
    /**
     * Usado para extrair roteiro deste projection, que já vem pré-populado com operações
     * e recursos produtivos
     */
    public Set<Roteiro> getRoteiroSetByRecursoProdutivoEMaterial(RecursoProdutivo recursoProdutivo, Produto material) {
        return mapaRoteiroSetPorRecursoProdutivoMaterial
                .getOrDefault(recursoProdutivo, new HashMap<>())
                .getOrDefault(material, new HashSet<>());
    }
    
    public Set<Roteiro> getRoteiroSetByRecursoProdutivo(RecursoProdutivo recursoProdutivo) {
        return mapaRoteiroSetPorRecursoProdutivoMaterial
                .getOrDefault(recursoProdutivo, new HashMap<>()).values().stream()
                .flatMap(x -> x.stream())
                .collect(Collectors.toSet());
    }
    
    public Set<VersaoProducao> getVersoesProducaoViaveis(Location location, boolean consideraVersoesProducaoParalelas) {
        return mapaVersaoProducaoViavelSetPorLocationMaterial
                .getOrDefault(location, new HashMap<>())
                .entrySet().stream()
                .flatMap(x -> x.getValue().stream())
                .filter(versaoProducao -> isVersaoProducaoDisponivel(versaoProducao, consideraVersoesProducaoParalelas))
                .collect(Collectors.toSet());
    }
    
    public Set<VersaoProducao> getVersoesProducaoViaveis(RecursoProdutivo recursoProdutivo, boolean consideraVersoesProducaoParalelas) {
        return mapaVersaoProducaoViavelSetPorRecursoProdutivo
                .getOrDefault(recursoProdutivo, new HashSet<>())
                .stream()
                .filter(versaoProducao -> isVersaoProducaoDisponivel(versaoProducao, consideraVersoesProducaoParalelas))
                .collect(Collectors.toSet());
    }
    
    public Set<VersaoProducao> getVersoesProducaoViaveis(boolean consideraVersoesProducaoParalelas) {
        return mapaVersaoProducaoViavelSetPorLocationMaterial
                .values().stream()
                .flatMap(x -> x.values().stream())
                .flatMap(x -> x.stream())
                .filter(versaoProducao -> isVersaoProducaoDisponivel(versaoProducao, consideraVersoesProducaoParalelas))
                .collect(Collectors.toSet());
    }
    
    public Optional<VersaoProducao> getVersaoProducaoFromId(String versaoProducaoId, boolean consideraVersoesProducaoParalelas) {
        
        return getVersoesProducaoViaveis(consideraVersoesProducaoParalelas).stream()
                .filter(versaoProducao -> Objects.equals(
                        versaoProducao.getId(),
                        versaoProducaoId))
                .findAny();
        
    }
        
    public Optional<Roteiro> getRoteiroFromId(String roteiroId) {
        return Optional.ofNullable(mapaRoteiros.get(roteiroId));
    }
    
    public Optional<ListaTecnica> getListaTecnicaFromId(String listaTecnicaId) {
        return Optional.ofNullable(mapaListasTecnicas.get(listaTecnicaId));
    }

    /**
     * Recupera roteiro populado por id quando o caller ja sabe que ele deve
     * existir na projection.
     *
     * <p>Os calculos de capacidade usam esta rota para evitar lazy loading e
     * precisam falhar com mensagem clara se a projection foi montada sem o
     * roteiro solicitado.</p>
     */
    /**
     * Recupera lista técnica populada por id para caches internos de input.
     */
        public Set<LinhaTransporte> getLinhasTransporte(VersaoMalha versaoMalha) {
        return mapaLinhaTransporteSetPorVersaoMalha
                .getOrDefault(versaoMalha, new HashSet<>());
    }
    public Set<LinhaTransporte> getLinhasTransporte(
            VersaoMalha versaoMalha,
            @Nullable Collection<Location> possiveisLocationsOrigem,
            @Nullable Collection<Location> possiveisLocationsDestino) {

        return mapaLinhaTransporteSetPorVersaoMalha
                .getOrDefault(versaoMalha, new HashSet<>())
                .stream()
                .filter(linhaTransporte -> possiveisLocationsDestino == null || possiveisLocationsDestino.contains(linhaTransporte.getLocationDestino()))
                .filter(linhaTransporte -> possiveisLocationsOrigem == null || possiveisLocationsOrigem.contains(linhaTransporte.getLocationOrigem()))
                .collect(Collectors.toSet());

    }

    public Set<LinhaTransporte> getLinhasTransporteAtivas(
            VersaoMalha versaoMalha,
            @Nullable Collection<Location> possiveisLocationsOrigem,
            @Nullable Collection<Location> possiveisLocationsDestino) {

        return mapaLinhaTransporteInboundAtivaSetPorLocation
                .getOrDefault(versaoMalha, new HashMap<>())
                .entrySet().stream()
                .filter(entry -> possiveisLocationsDestino == null || possiveisLocationsDestino.contains(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .filter(linhaTransporte -> possiveisLocationsOrigem == null || possiveisLocationsOrigem.contains(linhaTransporte.getLocationOrigem()))
                .collect(Collectors.toSet());

    }
    public Optional<Integer> getLeadTimeDiasEntreOrigemDestinoParaMaterial(
            VersaoMalha versaoMalha, 
            Location locationOrigem, 
            Location locationDestino, 
            Produto material, 
            LocalDateTime dataReferenciaParaStatusProduto) {
        
        Optional<LinhaTransporte> optionalLinhaTransporte = getLinhaTransporteEntreOrigemEDestino(versaoMalha, locationOrigem, locationDestino);
        
        return optionalLinhaTransporte
                .flatMap(linhaTransporte -> getParametrosLinhaTransporte(
                        linhaTransporte, material, dataReferenciaParaStatusProduto))
                .map(parametrosLinhaTransporte -> parametrosLinhaTransporte.leadTimeDias);
        
    }
    public Optional<Integer> getLeadTimePeriodosEntreOrigemDestinoParaMaterial(
            VersaoMalha versaoMalha, 
            Location locationOrigem, 
            Location locationDestino, 
            Produto material, 
            Calendario calendario, 
            LocalDateTime dataReferenciaParaStatusProduto) {
        
        Optional<Integer> optionalLeadTimeDias = getLeadTimeDiasEntreOrigemDestinoParaMaterial(versaoMalha, locationOrigem, locationDestino, material, dataReferenciaParaStatusProduto);
        
        return optionalLeadTimeDias
                .map(leadTimeDias -> (int) Math.floor(calendario.converteDiasParaPeriodosCalendario(
                        leadTimeDias)));
        
    }

    public UnidadeMedida getUnidadeMedidaLoteMinimoMultiploTransporte(
            VersaoMalha versaoMalha, 
            Location locationOrigem, 
            Location locationDestino, 
            Produto material, 
            @Nullable LocalDateTime dataReferenciaParaStatusProduto) {

        Optional<LinhaTransporte> optionalLinhaTransporte = getLinhaTransporteEntreOrigemEDestino(versaoMalha, locationOrigem, locationDestino);
        
        return optionalLinhaTransporte
                .map(linhaTransporte -> getParametrosLinhaTransporte(
                        linhaTransporte, material, dataReferenciaParaStatusProduto)
                        .map(parametrosLinhaTransporte -> parametrosLinhaTransporte.unidadeMedidaLoteMinimoMultiploTransporte)
                        .orElseGet(() -> getClusterEParametrosProjection().getSNPUnidadeMedidaPadrao(
                                material, locationDestino)))
                .orElse(null);
        
    }
    
    public OptionalDouble getLoteMinimoTransporte(
            VersaoMalha versaoMalha, 
            Location locationOrigem, 
            Location locationDestino, 
            Produto material, 
            LocalDateTime dataReferenciaParaStatusProduto) {
        
        Optional<LinhaTransporte> optionalLinhaTransporte = getLinhaTransporteEntreOrigemEDestino(versaoMalha, locationOrigem, locationDestino);
        
        return optionalLinhaTransporte
                .flatMap(linhaTransporte -> getParametrosLinhaTransporte(
                        linhaTransporte, material, dataReferenciaParaStatusProduto))
                .stream()
                .mapToDouble(parametrosLinhaTransporte -> parametrosLinhaTransporte.loteMinimoTransporte)
                .findAny();

    }

    public OptionalDouble getMultiploTransporte(
            VersaoMalha versaoMalha, 
            Location locationOrigem, 
            Location locationDestino, 
            Produto material, 
            LocalDateTime dataReferenciaParaStatusProduto) {
        
        Optional<LinhaTransporte> optionalLinhaTransporte = getLinhaTransporteEntreOrigemEDestino(versaoMalha, locationOrigem, locationDestino);
        
        return optionalLinhaTransporte
                .flatMap(linhaTransporte -> getParametrosLinhaTransporte(
                        linhaTransporte, material, dataReferenciaParaStatusProduto))
                .map(parametrosLinhaTransporte -> parametrosLinhaTransporte.multiploTransporte)
                .orElse(OptionalDouble.empty());

    }
    
    public OptionalDouble getMultiploTransporteNaUnidadeTarget(
            VersaoMalha versaoMalha, 
            Location locationOrigem, 
            Location locationDestino, 
            Produto material, 
            UnidadeMedida unidadeMedidaTarget, 
            LocalDateTime dataReferenciaParaStatusProduto) throws UnitOfMeasureConversionException {
        
        OptionalDouble multiploNaUnidadeOriginal = getMultiploTransporte(versaoMalha, locationOrigem, locationDestino, material, dataReferenciaParaStatusProduto);
        
        if (!multiploNaUnidadeOriginal.isPresent()) return OptionalDouble.empty();
        
        UnidadeMedida unidadeMedidaOriginal = getUnidadeMedidaLoteMinimoMultiploTransporte(versaoMalha, locationOrigem, locationDestino, material, dataReferenciaParaStatusProduto);
        
        return OptionalDouble.of(multiploNaUnidadeOriginal.getAsDouble() * conversaoUnidadeMedidaProjection.getConversaoParaUnidadeDestino(material, unidadeMedidaOriginal, unidadeMedidaTarget));
        
    }
    
    public OptionalDouble getLoteMinimoTransporteNaUnidadeTarget(
            VersaoMalha versaoMalha, 
            Location locationOrigem, 
            Location locationDestino, 
            Produto material, 
            UnidadeMedida unidadeMedidaTarget, 
            LocalDateTime dataReferenciaParaStatusProduto) throws UnitOfMeasureConversionException {
        
        OptionalDouble loteMinimoNaUnidadeOriginal = getLoteMinimoTransporte(versaoMalha, locationOrigem, locationDestino, material, dataReferenciaParaStatusProduto);
        
        if (!loteMinimoNaUnidadeOriginal.isPresent()) return OptionalDouble.empty();
        
        UnidadeMedida unidadeMedidaOriginal = getUnidadeMedidaLoteMinimoMultiploTransporte(versaoMalha, locationOrigem, locationDestino, material, dataReferenciaParaStatusProduto);
        
        return OptionalDouble.of(loteMinimoNaUnidadeOriginal.getAsDouble() * conversaoUnidadeMedidaProjection.getConversaoParaUnidadeDestino(material, unidadeMedidaOriginal, unidadeMedidaTarget));
        
    }
    
    public Optional<Integer> getPrioridadeLinhaTransporteEntreOrigemDestinoParaMaterial(
            VersaoMalha versaoMalha, 
            Location locationOrigem, 
            Location locationDestino, 
            Produto material, 
            LocalDateTime dataReferenciaParaStatusProduto) {
        
        Optional<LinhaTransporte> optionalLinhaTransporte = getLinhaTransporteEntreOrigemEDestino(versaoMalha, locationOrigem, locationDestino);
        
        return optionalLinhaTransporte
                .flatMap(linhaTransporte -> getParametrosLinhaTransporte(
                        linhaTransporte, material, dataReferenciaParaStatusProduto))
                .map(parametrosLinhaTransporte -> parametrosLinhaTransporte.prioridade);
        
    }
    
    /**
     * varre todas as operações do roteiro e retorna o menor throughput
     * @param calendario
     * @param roteiro
     * @return quantidade produzida / periodo
     */
    public double getQuantidadeMinimaRoteiroPorPeriodo(Calendario calendario, int posicaoPeriodo, Roteiro roteiro) {
        
        ParametrosGlobais parametrosGlobais = getClusterEParametrosProjection().getParametrosGlobais();
        UnidadeMedida unidadeMedidaPadrao = getClusterEParametrosProjection().getSNPUnidadeMedidaPadrao(
                roteiro.getMaterialOutput(), roteiro.getLocation());
        
        double quantidadePorHoraMinimo = roteiro.getOperacaoRoteiroSet().stream()
                .mapToDouble(operacao -> roteiro.getQuantidadeBase()
                        * getConversaoUnidadeMedidaProjection().getConversaoParaUnidadeDestino(
                                roteiro.getMaterialOutput(), 
                                roteiro.getUnidadeMedidaQuantidadeBase(parametrosGlobais),
                                unidadeMedidaPadrao)
                        / operacao.getHorasPorQuantidadeBase())
                .min()
                .orElse(0D);
        
        double numeroHorasPorPeriodo = calendario.getNumeroPeriodosNoBucketReferencia(posicaoPeriodo, Constantes.TamanhoBucket.HORARIO);
        
        return quantidadePorHoraMinimo * numeroHorasPorPeriodo;
        
    }
    public List<VersaoProducaoSimples> getVersoesProducaoSimplesViaveis(
            Location location, 
            Produto material,
            @Nullable Collection<Produto> possiveisMateriaisInput) {
        return mapaVersaoProducaoViavelSetPorLocationMaterial
                .getOrDefault(location, new HashMap<>())
                .getOrDefault(material, new HashSet<>())
                .stream()
                .filter(x -> x instanceof VersaoProducaoSimples)
                .map(x -> (VersaoProducaoSimples) x)
                .filter(x -> possiveisMateriaisInput == null || possiveisMateriaisInput.containsAll(x.getMateriaisInput()))
                .collect(Collectors.toList());
                
    }
    
    public RecursoProdutivo getRecursoProdutivoPersistido(String recursoProdutivoId) {
        return mapaRecursosProdutivos.get(recursoProdutivoId);
    }
    
    public Set<VersaoProducao> getTodasVersoesProducao(
            Location location, Produto material, 
            boolean consideraVersoesProducaoParalelas,
            @Nullable Collection<Produto> possiveisMateriaisInput) {
        
        return mapaVersaoProducaoSetPorLocationMaterial
                .getOrDefault(location, new HashMap<>())
                .getOrDefault(material, new HashSet<>())
                .stream()
                .filter(versaoProducao -> isVersaoProducaoDisponivel(versaoProducao, consideraVersoesProducaoParalelas))
                .filter(versaoProducao -> possiveisMateriaisInput == null || possiveisMateriaisInput.containsAll(versaoProducao.getMateriaisInput()))
                .collect(Collectors.toSet());
        
    }
    
    public boolean verificaSeRoteiroEViavel(Roteiro roteiro) {
        
        Produto material = roteiro.getMaterialOutput();
        Location location = roteiro.getLocation();
        
        return mapaRoteiroViavelSetPorLocationMaterial
                .getOrDefault(location, new HashMap<>())
                .getOrDefault(material, new HashSet<>())
                .contains(roteiro);
        
    }
    
    public boolean verificaSeListaTecnicaEViavel(ListaTecnica listaTecnica) {
        Produto material = listaTecnica.getMaterialOutput();
        Location location = listaTecnica.getLocation();
        
        return mapaListaTecnicaViavelSetPorLocationMaterial
                .getOrDefault(location, new HashMap<>())
                .getOrDefault(material, new HashSet<>())
                .contains(listaTecnica);
        
    }
    
    public Optional<VersaoMalha> getVersaoMalhaDeId(String versaoMalhaId) {
        return Optional.ofNullable(mapaVersaoMalhaPorId.get(versaoMalhaId));
    }
    
    public Set<Location> getLocationsDestinoFinaisAPartirDeMaterialLocation(VersaoMalha versaoMalha, Location location, Produto material, LocalDateTime dataReferenciaParaStatusMaterial) {
        
        Set<Location> locationsDestinoImediatos = getLocationDestinoPrioritarioSet(versaoMalha, location, material, dataReferenciaParaStatusMaterial, null);
        if (locationsDestinoImediatos.isEmpty()) return Set.of(location);
        
        Set<Location> locationSet = new HashSet<>();
        for (Location locationDestino : locationsDestinoImediatos) {
            locationSet.addAll(getLocationsDestinoFinaisAPartirDeMaterialLocation(versaoMalha, locationDestino, material, dataReferenciaParaStatusMaterial));
        }
        
        return locationSet;
        
    }

    public Set<VersaoMalha> getTodasVersoesMalha() {
        return new HashSet<>(mapaVersaoMalhaPorId.values());
    }

    public Set<LinhaTransporteProduto> getLinhasTransporteProdutoAtivasInbound(VersaoMalha versaoMalha, Location locationDestino, @Nullable Collection<Location> possiveisLocationsOrigem) {
        List<LinhaTransporte> linhasTransporteInbound = getLinhasTransporteAtivasInboundOrdenadasPorPrioridade(versaoMalha, locationDestino, possiveisLocationsOrigem);
        Map<LinhaTransporte, Map<Produto, LinhaTransporteProduto>> subMapa = mapaLinhaTransporteProdutoPorLinhaTransporteEProduto.get(versaoMalha);
        return linhasTransporteInbound
                .parallelStream()
                .flatMap(linhaTransporteInbound -> subMapa
                        .getOrDefault(linhaTransporteInbound, new HashMap<>())
                        .values()
                        .stream())
                .collect(Collectors.toSet());
    }

    public Optional<LinhaTransporteProduto> getLinhaTransporteProduto(VersaoMalha versaoMalha, LinhaTransporte linhaTransporte, Produto material) {
        return FuncoesMap.getElementoDeNestedMap(
                mapaLinhaTransporteProdutoPorLinhaTransporteEProduto,
                LinhaTransporteProduto.class,
                versaoMalha, linhaTransporte, material);
    }
    
}
