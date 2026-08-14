package com.opsfactor.community.capability.masterdata.network.location.domain;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.configuration.domain.ParametrosProdutoLocation;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaLocation;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaLocationInterface;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.ValorCaracteristicaLocation;
import com.opsfactor.community.capability.masterdata.organization.economicgroup.domain.EconomicGroup;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte.LinhaTransporteCompositeKey;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporteProduto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import com.pivovarit.function.ThrowingFunction;
import lombok.*;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Location operacional usada por Demand Planning, Supply Planning e master data
 * produtivo.
 *
 * <p>No Community, a location cobre o minimo necessario para planejamento
 * material/location: status operacional, flags de planejamento, relacoes de
 * malha de abastecimento, recursos produtivos e parametros por material.
 * Coordenadas, visualizacao em mapa, last mile e estruturas dinamicas de
 * agregacao pertencem ao Enterprise. Os defaults estáticos de capacidade
 * logística pertencem à própria location; os overrides por data, warehouses e
 * snapshots de plano continuam privados no Enterprise.</p>
 */
@Entity
@Data
@ToString(of="id")
@RequiredArgsConstructor
@EqualsAndHashCode(of = "id", callSuper = false)
@NoArgsConstructor
public class Location extends LocationAbstract implements Serializable, Comparable<Location> {

    @Id
    @NonNull
    @Column(length = 100)
    private String id;

    public Boolean ativo;

    private LocalDate dataIntroducao;
    private LocalDate dataDescontinuacao;

    private Boolean planejaProducao; // caso false, não se permite abertura do planning book Production Planning
    private Boolean planejaSupply; // caso false, não se permite abertura do planning book Supply Planning

    /**
     * Indica se a location pode participar do planejamento de deployment.
     *
     * <p>A coluna permanece no agregado Community porque e um escalar da
     * propria location e nao exige tabela, join ou colecao adicional. A
     * capacidade de configurar ou publicar deployment continua Enterprise;
     * por compatibilidade com cadastros anteriores, ausencia de configuracao
     * significa que a location permanece habilitada.</p>
     */
    @Getter(AccessLevel.NONE)
    private Boolean deploymentPlanningEnabled;
    
    private Boolean consideraRestricaoLinhaInbound; // se false, gera requisições mesmo desrespeitando lead time
    private Boolean consideraRestricaoProducao; // disponibilidade capac. produtiva + disponib. insumos
    
    // usado para inputs de dados em nível agregado, planning book SNP
    @Getter(AccessLevel.NONE)
    @ManyToOne
    private UnidadeMedida unidadeMedidaSnp;

    /**
     * Unidade cadastrada para expedicao da location.
     *
     * <p>O relacionamento permanece no aggregate compartilhado porque e um
     * atributo escalar da mesma location, sem tabela lateral nem colecao
     * inversa. Community nao o publica nem o consome; o mapper Enterprise o
     * reabre somente como preparacao de dado para capabilities privadas ainda
     * fora deste recorte.</p>
     */
    @Getter(AccessLevel.NONE)
    @ManyToOne
    private UnidadeMedida expeditionUom;

    /** Defaults estáticos de capacidade logística da própria location. */
    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    private UnidadeMedida unidadeMedidaCapacidadeArmazenagem;
    private Double capacidadeArmazenagemPadrao;
    private Boolean capacidadeArmazenagemFinita;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    private UnidadeMedida unidadeMedidaCapacidadeInbound;
    @Enumerated(EnumType.STRING)
    private Constantes.TamanhoBucket periodoIncidenciaCapacidadeInboundPadrao;
    private Double capacidadeInboundPadrao;
    private Boolean capacidadeInboundFinita;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    private UnidadeMedida unidadeMedidaCapacidadeOutbound;
    @Enumerated(EnumType.STRING)
    private Constantes.TamanhoBucket periodoIncidenciaCapacidadeOutboundPadrao;
    private Double capacidadeOutboundPadrao;
    private Boolean capacidadeOutboundFinita;
    
    // PARAMETROS DE SAFETY STOCK
    @Getter(AccessLevel.NONE)
    private Boolean incluiDemandaIndiretaNoSafetyStock;
    
    // SEGMENTADO / TOTAL . Não utilizado
    @Enumerated(EnumType.ORDINAL)
    private Constantes.SNPTipoEstoque tipoEstoque; // caso backlog seja permitido o estoque projetado poderá ser negativo, efetivamente atrasando a entrega de pedidos

    /**
     * Aplicável somente à demanda e não às transferências / produção
     */
    @Getter(AccessLevel.NONE)
    private Integer prazoAtendimentoDias;

    @Getter(AccessLevel.PRIVATE) // pois depende de versão
    @OneToMany(mappedBy = "linhaTransporteCompositeKey.locationDestino", orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LinhaTransporte> linhasTransporteOndeDestino = new ArrayList<>();
    @Getter(AccessLevel.PRIVATE) // pois depende de versão
    @OneToMany(mappedBy = "linhaTransporteCompositeKey.locationOrigem", orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LinhaTransporte> linhasTransporteOndeOrigem = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "location", orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RecursoProdutivo> recursosProdutivos = new ArrayList<>();
    
    @OneToMany(mappedBy = "parametrosProdutoLocationCompositeKey.location", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @MapKeyJoinColumn(name = "produto_id")
    private Map<Produto, ParametrosProdutoLocation> mapaParametrosProdutoLocation = new HashMap<>();

    /**
     * Valores das caracteristicas dinamicas cadastradas para esta location.
     * O mapeamento e equivalente ao agregado legado para permitir round-trip
     * no mesmo arquivo mestre da location.
     */
    @OneToMany(
            mappedBy = "valorCaracteristicaLocationCompositeKey.location",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @MapKeyJoinColumn(name = "caracteristica_location_id")
    private Map<CaracteristicaLocation, ValorCaracteristicaLocation> mapaLocationAtributo = new HashMap<>();

    /**
     * Cabeçalho do grupo econômico ao qual a location pertence.
     *
     * <p>A associação é unidirecional e lazy: o Community preserva a chave
     * estrangeira sem carregar nem publicar as capacidades Enterprise de
     * consolidação fiscal que usam esse agrupamento.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    private EconomicGroup economicGroup;

    /**
     * Location de referencia para parametrizacao de material/location.
     *
     * <p>A relacao e mantida no aggregate compartilhado porque representa uma
     * chave estrangeira direta entre locations operacionais. Ela e
     * unidirecional e lazy de proposito: nao ha colecao inversa, join extra ou
     * necessidade de carregar locations dependentes para usar a referencia.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    private Location referenceLocationForProductLocationParameters;
 
    public Location(String id, String descricao) {
        this.id = id;
        this.descricao = descricao;
    }

    /**
     * Varre todas as linhas de transporte onde a location é o destino Retorna
     * para um produto a sua respectiva LinhaTransporteProduto de maior
     * prioridade
     *
     * @return
     */
    public Optional<Location> getLocationOrigemLinhaTransportePrioritaria(
            Produto material, LocalDateTime dataReferencia, ParametrosGlobais parametrosGlobais) {
        
        Optional<LinhaTransporteProduto> linhaTransporteProduto = getLinhaTransporteProdutoPrioritariaOndeDestino(
                material, dataReferencia, parametrosGlobais);
        return linhaTransporteProduto.map(LinhaTransporteProduto::getLocationOrigem);
        
    }

    public Optional<LinhaTransporte> getLinhaTransportePrioritariaOndeDestino(
            Produto material, LocalDateTime dataReferencia, ParametrosGlobais parametrosGlobais) {
        // extrai linha de transporte com maior prioridade em linhaTransporteProduto
        // prioridade 0 = máxima
        // nesta etapa só se consideram materiais que tenham prioridade na linhaTransporteProduto
        Optional<LinhaTransporte> linhaTransportePrioritariaOptional = getLinhasTransporteOndeDestino().stream()
                .filter(x -> x.verificaSeMaterialPodeSerTransferidoNaLinhaTransporte(material, dataReferencia, parametrosGlobais))
                        //&& x.getLinhaTransporteProduto(material).getPrioridade() != null)
                .sorted((lt1, lt2) -> Integer.compare(
                        lt1.getLinhaTransporteProduto(material).getPrioridade(),
                        lt2.getLinhaTransporteProduto(material).getPrioridade()))
                .findFirst();
        
        return linhaTransportePrioritariaOptional;
    }
    
    /**
     * Cria linha transporte produto se não existe
     * @return
     */
    public Optional<LinhaTransporteProduto> getLinhaTransporteProdutoPrioritariaOndeDestinoDentroLeadTime(
            VersaoMalha versaoMalha, Produto material, Calendario calendario, int posicaoPeriodoReferencia, 
            ParametrosGlobais parametrosGlobais) {

        // extrai lista linha transporte produto já ordenada por prioridade
        List<LinhaTransporteProduto> linhasTransporteProduto = getLinhasTransporteProdutoInboundParaMaterialDentroLeadTime(versaoMalha, material, calendario, posicaoPeriodoReferencia, parametrosGlobais);
        
        if (linhasTransporteProduto.isEmpty()) return Optional.empty();
        
        return Optional.of(linhasTransporteProduto.get(0));
        
    }
    
    public boolean getConsideraRestricaoLinhaInbound() {
        return (consideraRestricaoLinhaInbound == null) ? true : consideraRestricaoLinhaInbound;
    }

    public boolean getConsideraRestricaoProducao() {
        return (consideraRestricaoProducao == null) ? true : consideraRestricaoProducao;
    }
    
    public Boolean getConsideraRestricaoProducaoCadastrado() {
        return consideraRestricaoProducao;
    }
    
    public Constantes.SNPTipoEstoque getTipoEstoque(ParametrosGlobais parametrosGlobais) {
        return (tipoEstoque == null) ? parametrosGlobais.getTipoEstoque() : tipoEstoque;
    }    
    
    public boolean getPlanejaProducao(){
        return (planejaProducao == null) ? true : planejaProducao;
    }
    
    public Boolean getPlanejaProducaoCadastrado(){
        return planejaProducao;
    }
    
    public boolean getPlanejaSupply(){
        return (planejaSupply == null) ? true : planejaSupply;
    }
    
    public Boolean getPlanejaSupplyCadastrado(){
        return planejaSupply;
    }

    /**
     * Retorna a habilitacao efetiva para deployment.
     *
     * <p>O default funcional e {@code true} para que registros anteriores a
     * coluna nao sejam removidos implicitamente do planejamento.</p>
     */
    public boolean getDeploymentPlanningEnabled() {

        return deploymentPlanningEnabled == null || deploymentPlanningEnabled;

    }

    /** Resolve a UOM estática de armazenagem com fallback ao parâmetro global. */
    public UnidadeMedida getUnidadeMedidaCapacidadeArmazenagem(ParametrosGlobais parametrosGlobais) {

        return unidadeMedidaCapacidadeArmazenagem == null
                ? parametrosGlobais.getUnidadeMedidaPadraoSNP()
                : unidadeMedidaCapacidadeArmazenagem;

    }

    public UnidadeMedida getUnidadeMedidaCapacidadeArmazenagemCadastrado() {

        return unidadeMedidaCapacidadeArmazenagem;

    }

    public double getCapacidadeArmazenagemPadrao() {

        return getCapacidadeLogisticaNaoNegativaOuZero(capacidadeArmazenagemPadrao, "default storage");

    }

    public Double getCapacidadeArmazenagemPadraoCadastrado() {

        return capacidadeArmazenagemPadrao;

    }

    public boolean getCapacidadeArmazenagemFinita() {

        return capacidadeArmazenagemFinita != null && capacidadeArmazenagemFinita;

    }

    public Boolean getCapacidadeArmazenagemFinitaCadastrada() {

        return capacidadeArmazenagemFinita;

    }

    public UnidadeMedida getUnidadeMedidaCapacidadeInbound(ParametrosGlobais parametrosGlobais) {

        return unidadeMedidaCapacidadeInbound == null
                ? parametrosGlobais.getUnidadeMedidaPadraoSNP()
                : unidadeMedidaCapacidadeInbound;

    }

    public UnidadeMedida getUnidadeMedidaCapacidadeInboundCadastrado() {

        return unidadeMedidaCapacidadeInbound;

    }

    public double getCapacidadeInboundPadrao() {

        return getCapacidadeLogisticaNaoNegativaOuZero(capacidadeInboundPadrao, "default inbound");

    }

    public Double getCapacidadeInboundPadraoCadastrado() {

        return capacidadeInboundPadrao;

    }

    public boolean getCapacidadeInboundFinita() {

        return capacidadeInboundFinita != null && capacidadeInboundFinita;

    }

    public Boolean getCapacidadeInboundFinitaCadastrada() {

        return capacidadeInboundFinita;

    }

    public Constantes.TamanhoBucket getPeriodoIncidenciaCapacidadeInboundPadrao() {

        return periodoIncidenciaCapacidadeInboundPadrao == null
                ? Constantes.TamanhoBucket.MENSAL
                : periodoIncidenciaCapacidadeInboundPadrao;

    }

    public Constantes.TamanhoBucket getPeriodoIncidenciaCapacidadeInboundPadraoCadastrado() {

        return periodoIncidenciaCapacidadeInboundPadrao;

    }

    public UnidadeMedida getUnidadeMedidaCapacidadeOutbound(ParametrosGlobais parametrosGlobais) {

        return unidadeMedidaCapacidadeOutbound == null
                ? parametrosGlobais.getUnidadeMedidaPadraoSNP()
                : unidadeMedidaCapacidadeOutbound;

    }

    public UnidadeMedida getUnidadeMedidaCapacidadeOutboundCadastrado() {

        return unidadeMedidaCapacidadeOutbound;

    }

    public double getCapacidadeOutboundPadrao() {

        return getCapacidadeLogisticaNaoNegativaOuZero(capacidadeOutboundPadrao, "default outbound");

    }

    public Double getCapacidadeOutboundPadraoCadastrado() {

        return capacidadeOutboundPadrao;

    }

    public boolean getCapacidadeOutboundFinita() {

        return capacidadeOutboundFinita != null && capacidadeOutboundFinita;

    }

    public Boolean getCapacidadeOutboundFinitaCadastrada() {

        return capacidadeOutboundFinita;

    }

    public Constantes.TamanhoBucket getPeriodoIncidenciaCapacidadeOutboundPadrao() {

        return periodoIncidenciaCapacidadeOutboundPadrao == null
                ? Constantes.TamanhoBucket.MENSAL
                : periodoIncidenciaCapacidadeOutboundPadrao;

    }

    public Constantes.TamanhoBucket getPeriodoIncidenciaCapacidadeOutboundPadraoCadastrado() {

        return periodoIncidenciaCapacidadeOutboundPadrao;

    }

    public double getCapacidadeInboundNoBucketTarget(Constantes.TamanhoBucket tamanhoBucketTarget) {

        return getCapacidadeInboundPadrao()
                * Calendario.getNumeroMedioPeriodosBucketOrigemNoBucketDestino(
                        getPeriodoIncidenciaCapacidadeInboundPadrao(), tamanhoBucketTarget);

    }

    public double getCapacidadeOutboundNoBucketTarget(Constantes.TamanhoBucket tamanhoBucketTarget) {

        return getCapacidadeOutboundPadrao()
                * Calendario.getNumeroMedioPeriodosBucketOrigemNoBucketDestino(
                        getPeriodoIncidenciaCapacidadeOutboundPadrao(), tamanhoBucketTarget);

    }

    /** Valida um escalar estático antes de ele tornar-se RHS de restrição Enterprise. */
    private double getCapacidadeLogisticaNaoNegativaOuZero(Double capacidade, String nomeCampo) {

        if (capacidade == null) {
            return 0.0d;
        }
        if (!Double.isFinite(capacidade) || capacidade < 0.0d) {
            throw new IllegalStateException("Logistics capacity " + nomeCampo
                    + " capacity must be finite and non-negative for Location " + id + ": " + capacidade + ".");
        }
        return capacidade;

    }
    
    public ParametrosProdutoLocation getParametrosProdutoLocation(Produto material) {
        if (mapaParametrosProdutoLocation.containsKey(material)) {
            return mapaParametrosProdutoLocation.get(material);
            // caso não haja parâmetros configurados retorna configuração padrão
        } else {
            return new ParametrosProdutoLocation(new ParametrosProdutoLocation.ParametrosProdutoLocationCompositeKey(material, this));
        }
    }
    
    public Optional<LinhaTransporteProduto> getLinhaTransporteProdutoPrioritariaOndeDestino(
            Produto material, LocalDateTime dataReferencia, ParametrosGlobais parametrosGlobais) {
        // criar nova linha transporte onde origem = nulo e destino = esta location (this)
        // criar nova linha transporte produto usando essa LT
        if (material == null || linhasTransporteOndeDestino.isEmpty()) return Optional.empty();
        Optional<LinhaTransporte> optionalLinhaTransportePrioritaria = getLinhaTransportePrioritariaOndeDestino(material, dataReferencia, parametrosGlobais);
        if (!optionalLinhaTransportePrioritaria.isPresent()) return Optional.empty();
        return optionalLinhaTransportePrioritaria.map(linhaTransporte ->
                linhaTransporte.getLinhaTransporteProduto(material));
    }
    
    public List<Location> getLocationsDestinoOndeOrigemPrioritaria(
            VersaoMalha versaoMalha,
            Produto material, LocalDateTime dataReferencia, 
            ParametrosGlobais parametrosGlobais) {
        // extrai linha de transporte com maior prioridade em linhaTransporteProduto
        // prioridade 0 = máxima
        // nesta etapa só se consideram materiais que tenham prioridade na linhaTransporteProduto
        return getLinhasTransporteOndeOrigem().stream()
                .filter(x -> x.getVersaoMalha().equals(versaoMalha))
                .filter(x -> x.verificaSeMaterialPodeSerTransferidoNaLinhaTransporte(material, dataReferencia, parametrosGlobais))
                .map(x -> x.getLocationDestino())
                .filter(x -> this.equals(x.getLocationOrigemLinhaTransportePrioritaria(material, dataReferencia, parametrosGlobais).orElse(null)))
                .collect(Collectors.toList());
    }
    
    /**
     * Extrai todas as linhas transporte produto outbound onde o material pode trafegar
     * Importante para low level code
     * @param material
     * @param dataReferencia
     * @param parametrosGlobais
     * @return 
     */
    public List<LinhaTransporteProduto> getLinhasTransporteProdutoOutboundParaMaterial(
            VersaoMalha versaoMalha, Produto material, LocalDateTime dataReferencia, 
            ParametrosGlobais parametrosGlobais) {
        
        return getLinhasTransporteOndeOrigem().stream()
                .filter(x -> x.getVersaoMalha().equals(versaoMalha))
                .filter(x -> x.verificaSeMaterialPodeSerTransferidoNaLinhaTransporte(material, dataReferencia, parametrosGlobais))
                .map(x -> x.getLinhaTransporteProduto(material))
                .collect(Collectors.toList());
        
    }
    
    /**
     * Extrai todas as linhas transporte inbound outbound onde o material pode trafegar
     * Importante para low level code
     * @param material
     * @param dataReferencia
     * @param parametrosGlobais
     * @return 
     */
    public List<LinhaTransporteProduto> getLinhasTransporteProdutoInboundParaMaterial(
            VersaoMalha versaoMalha, Produto material, LocalDateTime dataReferencia, 
            ParametrosGlobais parametrosGlobais) {
        
        return getLinhasTransporteOndeDestino().stream()
                .filter(x -> x.getVersaoMalha().equals(versaoMalha))
                .filter(x -> x.verificaSeMaterialPodeSerTransferidoNaLinhaTransporte(material, dataReferencia, parametrosGlobais))
                .map(x -> x.getLinhaTransporteProduto(material))
                .sorted(Comparator.comparing(x -> x.getPrioridade()))
                .collect(Collectors.toList());
        
    }
    
    public List<LinhaTransporteProduto> getLinhasTransporteProdutoInboundParaMaterialDentroLeadTime(
            VersaoMalha versaoMalha,
            Produto material, Calendario calendario, 
            int posicaoPeriodoReferencia, ParametrosGlobais parametrosGlobais) {
        
        return getLinhasTransporteProdutoInboundParaMaterial(versaoMalha, material, calendario.getDataHorarioInicial(), parametrosGlobais).stream()
                .filter(x -> x.getVersaoMalha().equals(versaoMalha))
                .filter(x -> posicaoPeriodoReferencia >= calendario.getPosicaoPeriodoPresente() + x.getLeadTimePeriodos(calendario))
                .sorted(Comparator.comparing(x -> x.getPrioridade()))
                .collect(Collectors.toList());
        
    }
    
    public Optional<LinhaTransporte> getLinhaTransporteParaDestino(VersaoMalha versaoMalha, Location locationDestino) {
        return getLinhasTransporteOndeOrigem().stream()
                .filter(x -> x.getVersaoMalha().equals(versaoMalha))
                .filter(x -> x.getLocationDestino().equals(locationDestino))
                .findFirst();
    }
    
    public Optional<LinhaTransporte> getLinhaTransporteDeOrigem(VersaoMalha versaoMalha, Location locationOrigem) {
        return getLinhasTransporteOndeDestino().stream()
                .filter(x -> x.getVersaoMalha().equals(versaoMalha))
                .filter(x -> x.getLocationOrigem().equals(locationOrigem))
                .findFirst();
    }
    
    public LinhaTransporteProduto getLinhaTransporteProdutoParaDestino(VersaoMalha versaoMalha, Location locationDestino, Produto produto) {
        LinhaTransporte linhaTransporte = getLinhasTransporteOndeOrigem().stream()
                .filter(x -> x.getVersaoMalha().equals(versaoMalha))
                .filter(x -> x.getLocationDestino().equals(locationDestino))
                .findAny().orElse(new LinhaTransporte(new LinhaTransporteCompositeKey(versaoMalha, this, locationDestino)));
        
        return linhaTransporte.getLinhaTransporteProduto(produto);
    }

    public List<LinhaTransporte> getLinhasTransporteOndeDestino(VersaoMalha versaoMalha) {
        return getLinhasTransporteOndeDestino().stream()
                .filter(x -> x.getVersaoMalha().equals(versaoMalha))
                .collect(Collectors.toList());
    }
    public List<LinhaTransporte> getLinhasTransporteOndeOrigem(VersaoMalha versaoMalha) {
        return getLinhasTransporteOndeOrigem().stream()
                .filter(x -> x.getVersaoMalha().equals(versaoMalha))
                .collect(Collectors.toList());
    }
    
    public void addLinhaTransporte(LinhaTransporte linhaTransporte) {
        if (linhaTransporte.getLocationDestino().equals(this)) {
            if (getLinhasTransporteOndeDestino().contains(linhaTransporte)) {
                linhasTransporteOndeDestino.remove(linhaTransporte);
            }
            linhasTransporteOndeDestino.add(linhaTransporte);
        } else if (linhaTransporte.getLocationOrigem().equals(this)) {
            if (getLinhasTransporteOndeDestino().contains(linhaTransporte)) {
                linhasTransporteOndeOrigem.remove(linhaTransporte);
            }
            linhasTransporteOndeOrigem.add(linhaTransporte);
        }
    }

    /** Retorna o valor de uma caracteristica real ou sintetica da location. */
    public String getValorCaracteristica(CaracteristicaLocationInterface caracteristicaLocation) {

        return caracteristicaLocation.getValorCaracteristicaDeLocation(this);

    }

    /**
     * Cria ou atualiza o valor de uma caracteristica dentro do aggregate da
     * location, reproduzindo o comportamento usado pelo Data Upload legado.
     */
    public void setValorCaracteristica(
            CaracteristicaLocation caracteristicaLocation,
            String valorCaracteristica) {

        ValorCaracteristicaLocation valorCaracteristicaLocation = mapaLocationAtributo.get(caracteristicaLocation);
        if (valorCaracteristicaLocation == null) {
            valorCaracteristicaLocation = new ValorCaracteristicaLocation(
                    new ValorCaracteristicaLocation.ValorCaracteristicaLocationCompositeKey(
                            this,
                            caracteristicaLocation),
                    valorCaracteristica);
            mapaLocationAtributo.put(caracteristicaLocation, valorCaracteristicaLocation);
        } else {
            valorCaracteristicaLocation.setAtributo(valorCaracteristica);
        }

    }
    
    // Métodos ligados ao planejamento de produção ----------------------
    public Set<Produto> getMateriaisOutputEmRoteirosAtivos() {
        return getRecursosProdutivosAtivos().stream()
                .filter(x -> x.getAtivo())
                .map(ThrowingFunction.unchecked(RecursoProdutivo::getMateriaisOutputRoteirosAtivos))
                .flatMap(x -> x.stream())
                .collect(Collectors.toSet());
    }
                
    public List<RecursoProdutivo> getRecursosProdutivosAtivos() {
        return recursosProdutivos.stream()
                .filter(x -> x.getAtivo())
                .collect(Collectors.toList());
    }

    public UnidadeMedida getUnidadeMedidaSnp(ParametrosGlobais parametrosGlobais) {
        return (unidadeMedidaSnp == null) ? parametrosGlobais.getUnidadeMedidaPadraoSNP() : unidadeMedidaSnp;
    }
    public UnidadeMedida getUnidadeMedidaSnpCadastrado() {
        return unidadeMedidaSnp;
    }

    /**
     * Retorna somente o override cadastrado para expedicao, sem aplicar
     * fallback de Deployment que permanece fora do Community e deste recorte.
     */
    public UnidadeMedida getExpeditionUomRegistered() {

        return expeditionUom;

    }

    public boolean getIncluiDemandaIndiretaNoSafetyStock(ParametrosGlobais parametrosGlobais) {
        return (incluiDemandaIndiretaNoSafetyStock == null) ? parametrosGlobais.getIncluiDemandaIndiretaNoSafetyStock() : incluiDemandaIndiretaNoSafetyStock;
    }
    public Boolean getIncluiDemandaIndiretaNoSafetyStockCadastrado() {
        return incluiDemandaIndiretaNoSafetyStock;
    }

    public Constantes.StatusProduto getStatusLocation(LocalDate dataReferencia, ParametrosGlobais parametrosGlobais) {
        if (dataDescontinuacao != null) {
            if (dataReferencia.isAfter(dataDescontinuacao) || dataReferencia.equals(dataDescontinuacao)) {
                return Constantes.StatusProduto.DESCONTINUADO;
            }
        }
        
        if (dataIntroducao == null) {
            return Constantes.StatusProduto.REGULAR;
        } else {
            int numeroDiasLocationNova = parametrosGlobais.getNumeroDiasLocationNova();
            if (numeroDiasLocationNova <= 0) {
                /*
                 * Community nao possui janela funcional de location nova. A data
                 * de introducao continua definindo "not launched" antes do
                 * lancamento, mas no proprio dia de introducao a location ja e
                 * tratada como regular.
                 */
                return dataReferencia.isBefore(dataIntroducao)
                        ? Constantes.StatusProduto.NAO_LANCADO
                        : Constantes.StatusProduto.REGULAR;
            }

            LocalDate dataFinalNovo = dataIntroducao.plusDays(numeroDiasLocationNova);
            if (dataReferencia.isAfter(dataFinalNovo)) {
                return Constantes.StatusProduto.REGULAR;
            } else if (dataReferencia.isBefore(dataIntroducao)) {
                return Constantes.StatusProduto.NAO_LANCADO;
            } else {
                return Constantes.StatusProduto.NOVO;
            }
        }
    }

    public Integer getPrazoAtendimentoDiasCadastrado() {
        return prazoAtendimentoDias;
    }
    public int getPrazoAtendimentoDias() {
        return (prazoAtendimentoDias == null) ? 0 : prazoAtendimentoDias;
    }
    
    public boolean getAtivo() {
        return (ativo == null) ? true : ativo;
    }
    public Boolean getAtivoCadastrado() {
        return ativo;
    }
    
    public static Set<Location> filtraLocationsAtivasSet(Collection<Location> locationCollection) {
        return locationCollection.stream()
                    .filter(x -> x.getAtivo())
                    .collect(Collectors.toSet());
    }

    public static Set<Location> filtraLocationsAtivasSetComTipoLocation(Collection<Location> locationCollection, TipoLocation tipoLocation) {
        return locationCollection.stream()
                .filter(x -> x.getAtivo())
                .filter(x -> x.getTipoLocation().equals(tipoLocation))
                .collect(Collectors.toSet());
    }

    public static Set<Location> filtraLocationsAtivasSetComTiposLocation(Collection<Location> locationCollection, TipoLocation... tiposLocation) {
        return locationCollection.stream()
                .filter(x -> x.getAtivo())
                .filter(x -> Arrays.stream(tiposLocation).collect(Collectors.toList()).contains(x.getTipoLocation()))
                .collect(Collectors.toSet());
    }

    @Override
    public int compareTo(Location location) {
        return getId().compareTo(location.getId());
    }
        
}
