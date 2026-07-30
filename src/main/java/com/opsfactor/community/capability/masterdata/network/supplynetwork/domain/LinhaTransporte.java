

package com.opsfactor.community.capability.masterdata.network.supplynetwork.domain;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporteProduto.LinhaTransporteProdutoCompositeKey;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.Constantes.StatusProduto;
import lombok.*;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
@ToString(of="linhaTransporteCompositeKey") // importante para nao gerar dependencia recursiva entre linha transporte produto e linha transporte
@NoArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(of="linhaTransporteCompositeKey")
@Entity
public class LinhaTransporte implements Serializable {

    @EmbeddedId
    @NonNull // null check pelo lombok : também usado para definir campos obrigatórios no construtor lombok
    private LinhaTransporteCompositeKey linhaTransporteCompositeKey;
    
    @Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class LinhaTransporteCompositeKey implements Serializable {

        @ManyToOne
        @NonNull // null check pelo lombok : também usado para definir campos obrigatórios no construtor lombok
        private VersaoMalha versaoMalha;
        
        @ManyToOne
        @NonNull // null check pelo lombok : também usado para definir campos obrigatórios no construtor lombok
        private Location locationOrigem;
        
        @ManyToOne
        @NonNull // null check pelo lombok : também usado para definir campos obrigatórios no construtor lombok
        private Location locationDestino;

    }
    
    /*
     * Lead time fisico da linha em dias. Null preserva ausencia operacional
     * legada e equivale a zero; valor cadastrado precisa ser finito e nao
     * negativo para nao antecipar recebimentos silenciosamente.
     */
    private Double leadTimeDias;

    /*
     * Distancia da rota e reservada ao Enterprise, onde pode alimentar mapa,
     * baricentro, frete e analises geograficas. O Community preserva o campo
     * por compatibilidade de schema, mas suas APIs nao exportam nem persistem
     * valores preenchidos.
     */
    private Double distanciaKm;
    
    /**
     * Prioridade de uso da linha de transporte, onde 0 representa a prioridade máxima
     * Sobrescrito no nível material em LinhaTransporteProduto
     */
    private Integer prioridade;
    
    private Boolean habilitadoProdutosDescontinuados;
    private Boolean habilitadoProdutosNaoLancados;
    private Boolean habilitadoProdutosNaoCadastradosLinhaTransporte;

    /**
     * Unidade de medida para múltiplos e lote mínimo
     */
    @Getter(AccessLevel.NONE)
    @ManyToOne
    private UnidadeMedida unidadeMedidaLoteMinimoMultiploTransporte;
    /*
     * Null significa ausencia de restricao fisica. Quando o usuario cadastra
     * valor, a linha de transporte deve falhar cedo para negativo ou valor nao
     * finito, antes de a projection transformar esse cadastro em lote/multiplo
     * usado por heuristico e optimizer Enterprise.
     */
    private Double loteMinimoTransporte;

    /*
     * Diferente do lote minimo, multiplo presente precisa ser estritamente
     * positivo: zero nao representa "sem multiplo", pois a propria presenca do
     * OptionalDouble ativa variaveis/restricoes de arredondamento.
     */
    private Double multiploTransporte;
    
    @OneToMany(mappedBy="linhaTransporteProdutoCompositeKey.linhaTransporte", fetch = FetchType.LAZY)
    @MapKeyJoinColumn(name = "produto_id")
    private Map<Produto, LinhaTransporteProduto> mapaLinhaTransporteProduto = new HashMap<>();
    
    private Boolean ativo;
        
    public Location getLocationOrigem() {
        return linhaTransporteCompositeKey.locationOrigem;
    }
    
    public void setLocationOrigem(Location location) {
        linhaTransporteCompositeKey.locationOrigem = location;
    }
    
    public Location getLocationDestino() {
        return linhaTransporteCompositeKey.locationDestino;
    }
    
    public void setLocationDestino(Location location) {
        linhaTransporteCompositeKey.locationDestino = location;
    }
    
    /**
     * Confronta habilitadoProdutosDescontinuados, habilitadoProdutosNaoLancados e habilitadoProdutosNaoCadastradosLinhaTransporte
     * com produto e verifica se o material pode ser transportado
     * @param material
     * @param dataReferencia
     * @param parametrosGlobais
     * @return 
     */
    public boolean verificaSeMaterialPodeSerTransferidoNaLinhaTransporte(
            Produto material, LocalDateTime dataReferencia, ParametrosGlobais parametrosGlobais) {
        if (getHabilitadoProdutosNaoCadastradosLinhaTransporte() || mapaLinhaTransporteProduto.containsKey(material)) {
            
            Constantes.StatusProduto statusProduto = material.getStatusProduto(
                    dataReferencia, getLocationDestino(), parametrosGlobais);
            
            if (getHabilitadoProdutosDescontinuados() || !statusProduto.equals(StatusProduto.DESCONTINUADO)) {
                if (getHabilitadoProdutosNaoLancados() || !statusProduto.equals(StatusProduto.NAO_LANCADO)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Método obtém uma linhaTransporteProduto mesmo que não exista no banco de dados
     * Nova linhaTransporteProduto é objeto não persistido e com valores padrão para 
     * todos os parâmetros
     * A nova linhaTransporteProduto não é salva no mapa Map<Produto,LinhaTransporteProduto>
     * 
     * @param produto
     * @return 
     */
    public LinhaTransporteProduto getLinhaTransporteProduto(Produto produto) {
        LinhaTransporteProduto linhaTransporteProduto;
        if (!mapaLinhaTransporteProduto.containsKey(produto)) {
            linhaTransporteProduto = new LinhaTransporteProduto();
            linhaTransporteProduto.setLinhaTransporteProdutoCompositeKey(new LinhaTransporteProdutoCompositeKey());
            linhaTransporteProduto.setLinhaTransporte(this);
            linhaTransporteProduto.setProduto(produto);
        } else {
            linhaTransporteProduto = mapaLinhaTransporteProduto.get(produto);
        }
        return linhaTransporteProduto;
    }
    
    public Integer getLeadTimeDiasInteiro() {

        return (int) Math.ceil(getValorLogisticoNaoNegativoOuZero(
                leadTimeDias,
                "lead time days"));

    }

    public Double getLeadTimeDiasCadastrado() {
        return leadTimeDias;
    }
    
    public Boolean getHabilitadoProdutosDescontinuados() {
        return (habilitadoProdutosDescontinuados == null) ? true : habilitadoProdutosDescontinuados;
    }
    
    /**
     * Usado para data upload (retorna o valor real do campo, mesmo que seja nulo)
     * @return 
     */
    public Boolean getHabilitadoProdutosDescontinuadosCadastrado() {
        return habilitadoProdutosDescontinuados;
    }
    
    public Boolean getHabilitadoProdutosNaoLancados() {
        return (habilitadoProdutosNaoLancados == null) ? true : habilitadoProdutosNaoLancados;
    }
    
    /**
     * Usado para data upload (retorna o valor real do campo, mesmo que seja nulo)
     * @return 
     */
    public Boolean getHabilitadoProdutosNaoLancadosCadastrado() {
        return habilitadoProdutosNaoLancados;
    }
    
    public boolean getHabilitadoProdutosNaoCadastradosLinhaTransporte() {
        return (habilitadoProdutosNaoCadastradosLinhaTransporte == null) ? true : habilitadoProdutosNaoCadastradosLinhaTransporte;
    }
    
    public VersaoMalha getVersaoMalha() {
        return getLinhaTransporteCompositeKey().getVersaoMalha();
    }
    
    /**
     * Usado para data upload (retorna o valor real do campo, mesmo que seja nulo)
     * @return 
     */
    public Boolean getHabilitadoProdutosNaoCadastradosLinhaTransporteCadastrado() {
        return habilitadoProdutosNaoCadastradosLinhaTransporte;
    }
    
    public Integer getLeadTimePeriodos(Calendario calendario) {
        return (int) Math.floor(calendario.converteDiasParaPeriodosCalendario(getLeadTimeDiasInteiro()));
    }
    
    public UnidadeMedida getUnidadeMedidaLoteMinimoMultiploTransporte(Produto material, ParametrosGlobais parametrosGlobais) {
        return (unidadeMedidaLoteMinimoMultiploTransporte == null) ? 
                material.getParametrosProdutoLocation(getLocationDestino()).getUnidadeMedidaPadrao(parametrosGlobais) 
                : unidadeMedidaLoteMinimoMultiploTransporte;
    }
    
    public UnidadeMedida getUnidadeMedidaLoteMinimoMultiploTransporteCadastrada() {
        return unidadeMedidaLoteMinimoMultiploTransporte;
    }
    
    public Integer getPrioridade() {
        return (prioridade == null) ? Integer.MAX_VALUE : prioridade;
    }
    
    /**
     * Usado para data upload
     * @return 
     */
    public Integer getPrioridadeCadastrada() {
        return prioridade;
    }
    
    public boolean getAtivo() {
        return (ativo == null) ? true : ativo;
    }
    
    public Boolean getAtivoCadastrado() {
        return ativo;
    }
    
    public static Map<Location,List<LinhaTransporte>> getMapaLinhasTransportePorOrigem(Collection<LinhaTransporte> linhaTransporteCollection) {
        return linhaTransporteCollection.stream()
                .collect(groupingBy(x -> x.getLocationOrigem()));
    }
    
    public static Map<Location,List<LinhaTransporte>> getMapaLinhasTransportePorDestino(Collection<LinhaTransporte> linhaTransporteCollection) {
        return linhaTransporteCollection.stream()
                .collect(groupingBy(x -> x.getLocationDestino()));
    }
    
    public static Map<Location,Map<Produto,LinhaTransporte>> getMapaLinhaTransportePrioritariaInboundPorLocationDestinoProduto(
            Collection<Produto> materialCollection, Collection<LinhaTransporte> linhaTransporteCollection,
            LocalDateTime dataReferenciaStatusMaterial, ParametrosGlobais parametrosGlobais) {
        
        Map<Location,Map<Produto,LinhaTransporte>> mapaLinhaTransportePrioritariaInboundPorLocationDestinoProduto = new HashMap<>();
        
        Map<Location,List<LinhaTransporte>> mapaLinhasTransportePorDestino = getMapaLinhasTransportePorDestino(linhaTransporteCollection);
        Set<Location> locationsDestino = new HashSet(mapaLinhasTransportePorDestino.keySet());
        
        for (Location locationDestino : locationsDestino) {
            for (Produto material : materialCollection) {
                
                Optional<LinhaTransporte> optionalLinhaTransporte = mapaLinhasTransportePorDestino.get(locationDestino).stream()
                        .filter(x -> x.verificaSeMaterialPodeSerTransferidoNaLinhaTransporte(material, dataReferenciaStatusMaterial, parametrosGlobais))
                        .sorted(Comparator.comparingInt(x -> x.getLinhaTransporteProduto(material).getPrioridade()))
                        .findFirst();
                
                optionalLinhaTransporte.ifPresent(linhaTransporte ->
                    mapaLinhaTransportePrioritariaInboundPorLocationDestinoProduto
                            .computeIfAbsent(locationDestino, x -> new HashMap<>())
                            .put(material, linhaTransporte));
            }
            // libera memória e agiliza execução
            mapaLinhasTransportePorDestino.remove(locationDestino);
        }
        
        return mapaLinhaTransportePrioritariaInboundPorLocationDestinoProduto;
        
    }
    
    public static Map<Location,Map<Produto,Set<LinhaTransporte>>> getMapaLinhasTransportePrioritariasOutboundPorLocationOrigemProduto(
            Collection<Produto> materialCollection, Collection<LinhaTransporte> linhaTransporteCollection,
            LocalDateTime dataReferenciaStatusMaterial, ParametrosGlobais parametrosGlobais) {
        
        Map<Location,Map<Produto,Set<LinhaTransporte>>> mapaLinhasTransportePrioritariasOutboundPorLocationOrigemProduto = new HashMap<>();
        
        Map<Location,Map<Produto,LinhaTransporte>> mapaLinhaTransportePrioritariaInboundPorLocationDestinoProduto = getMapaLinhaTransportePrioritariaInboundPorLocationDestinoProduto(
                materialCollection, linhaTransporteCollection, dataReferenciaStatusMaterial, parametrosGlobais);
        
        Map<Location,List<LinhaTransporte>> getMapaLinhasTransportePorOrigem = getMapaLinhasTransportePorOrigem(linhaTransporteCollection);
        
        for (Location locationOrigem : getMapaLinhasTransportePorOrigem.keySet()) {
            for (Produto material : materialCollection) {
                                                
                Set<LinhaTransporte> linhasTransportePrioritariasComOrigemLocation = getMapaLinhasTransportePorOrigem.get(locationOrigem).stream()
                        .filter(x -> 
                                mapaLinhaTransportePrioritariaInboundPorLocationDestinoProduto.containsKey(x.getLocationDestino()) &&
                                mapaLinhaTransportePrioritariaInboundPorLocationDestinoProduto.get(x.getLocationDestino()).containsKey(material) && 
                                mapaLinhaTransportePrioritariaInboundPorLocationDestinoProduto.get(x.getLocationDestino()).get(material).getLocationOrigem().equals(locationOrigem))
                        .collect(Collectors.toSet());
                
                if (!linhasTransportePrioritariasComOrigemLocation.isEmpty()) {
                    mapaLinhasTransportePrioritariasOutboundPorLocationOrigemProduto
                            .computeIfAbsent(locationOrigem, x -> new HashMap<>())
                            .put(material, linhasTransportePrioritariasComOrigemLocation);
                }
                
            }
        }
        
        return mapaLinhasTransportePrioritariasOutboundPorLocationOrigemProduto;
        
    } 
    
    public Double getLoteMinimoTransporte() {

        return getValorLogisticoNaoNegativoOuZero(
                loteMinimoTransporte,
                "minimum lot");

    }
    public Double getLoteMinimoTransporteCadastrado() {
        return loteMinimoTransporte;
    }

    public OptionalDouble getMultiploTransporte() {

        return (multiploTransporte == null)
                ? OptionalDouble.empty()
                : OptionalDouble.of(multiploTransporte);

    }
    public Double getMultiploTransporteCadastrado() {
        return multiploTransporte;
    }

    /**
     * Valida grandezas logisticas que aceitam ausencia operacional.
     */
    private double getValorLogisticoNaoNegativoOuZero(
            Double valorLogistico,
            String contextoValorLogistico) {

        if (valorLogistico == null) {
            return 0.0d;
        }
        if (!Double.isFinite(valorLogistico) || valorLogistico < 0.0d) {
            throw new IllegalStateException(
                    "Transportation line "
                            + contextoValorLogistico
                            + " must be finite and non-negative for "
                            + getLocationOrigem().getId()
                            + " -> "
                            + getLocationDestino().getId()
                            + ": "
                            + valorLogistico
                            + ".");
        }
        return valorLogistico;

    }
    /**
     * Valida grandezas logisticas opcionais que, quando presentes, ativam uma
     * restricao fisica real no planejamento.
     */
    
}
