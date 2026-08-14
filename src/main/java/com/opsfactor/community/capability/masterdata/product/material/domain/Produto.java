package com.opsfactor.community.capability.masterdata.product.material.domain;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.configuration.domain.ParametrosProdutoLocation;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProduto;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProdutoInterface;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.ValorCaracteristicaProduto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporteProduto;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnicaComponente;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.ConversaoUnidadeProduto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlanItem;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.domain.InventoryPlanLinha;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.domain.Estoque;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.*;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Material planejavel da plataforma.
 *
 * <p>O Community mantem os atributos fisicos e operacionais necessarios para
 * vendas historicas, Demand Planning estatistico, safety stock e Supply
 * Planning heuristico: ciclo de vida, modelo operacional, unidades de medida,
 * parametros por location, caracteristicas cadastrais e relacoes basicas de
 * malha/producao. Custos, precos e regras economicas pertencem ao Enterprise.</p>
 */
@ToString(of="id")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@Entity
public class Produto implements Serializable, Comparable<Produto> {

    @Id
    @Column(length = 50)
    private String id;

    /*
     * Campo fisico transicional usado por fluxos fiscais Enterprise. Produto e
     * uma entidade central do Community, portanto o campo pode permanecer no
     * schema compartilhado, mas nenhuma API ou rotina Community deve ler,
     * expor ou popular esse valor.
     */
    private String codigoFiscal;
    
    private String descricao;

    private Boolean ativo;

    @Getter(AccessLevel.NONE) // único meio de obter é através de getStatusMaterialCadastrado para evitar implementações incorretas
    @Enumerated(EnumType.ORDINAL)
    private Constantes.StatusProduto estagioCicloVida;
    // usados se statusMaterial não tiver sido preenchido
    private LocalDateTime dataIntroducao;
    private LocalDateTime dataDescontinuacao;
        
    @Enumerated(EnumType.ORDINAL) // MTS, MTO
    private Constantes.SNPModeloOperacional modeloOperacional; // MTS ou MTO

    /**
     * Default shelf life configured for the material, in calendar days.
     *
     * <p>The value belongs to the shared material aggregate because it is a
     * scalar master-data attribute and the Enterprise inventory policy and
     * supply optimizers use the same fallback. A material/location setting,
     * when present, takes precedence in the parameter projection. Community
     * flows do not interpret this value.</p>
     */
    private Double shelfLifeDays;
    
    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    private UnidadeMedida unidadeMedidaPadrao;
    
    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    private UnidadeMedida unidadeMedidaVendas;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    private UnidadeMedida unidadeMedidaTransferencia;

    /**
     * COGS unitario cadastrado para o material.
     *
     * <p>O campo permanece na tabela do material porque representa um atributo
     * escalar do proprio aggregate. A interpretacao economica, sua superficie
     * de escrita e os consumidores continuam pertencendo ao Enterprise.</p>
     */
    @Getter(AccessLevel.NONE)
    private Float unitCogs;

    /**
     * Unidade de medida que referencia o {@link #unitCogs}.
     *
     * <p>A ausencia de unidade preserva o fallback historico para a unidade
     * SNP global. A relacao e lazy para que leituras Community de material nao
     * carreguem uma unidade usada somente pelo calculo economico Enterprise.</p>
     */
    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    private UnidadeMedida unitCogsUnitOfMeasure;

    /**
     * Preco gross unitario cadastrado para o material.
     *
     * <p>Preco padrao por material possui granularidade 1:1 com este
     * aggregate. Manter o valor escalar nesta mesma tabela evita uma entidade
     * complementar usada somente para extender o produto. A interpretacao
     * economica, a superficie de escrita e os consumidores continuam
     * pertencendo ao Enterprise.</p>
     */
    @Getter(AccessLevel.NONE)
    private Double unitGrossPrice;

    /**
     * Preco net unitario cadastrado para o material.
     *
     * <p>Gross e net compartilham a mesma unidade de medida cadastrada, como
     * no modelo anterior de preco padrao por material.</p>
     */
    @Getter(AccessLevel.NONE)
    private Double unitNetPrice;

    /**
     * Unidade de medida compartilhada pelos precos gross e net unitarios.
     *
     * <p>A relacao permanece lazy e sem nome fisico explicito. A ausencia de
     * unidade preserva o fallback historico para a unidade de vendas de Demand
     * Planning, sem fazer uma leitura Community carregar esse dado
     * economico.</p>
     */
    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    private UnidadeMedida unitPriceUnitOfMeasure;

    /**
     * Valores das caracteristicas dinamicas cadastradas para este material.
     *
     * <p>O mapa preserva o mesmo aggregate e o mesmo contrato do legado: a
     * caracteristica e a chave da coluna dinamica, enquanto a entidade de
     * valor continua sendo a dona fisica da chave composta.</p>
     */
    @OneToMany(
            mappedBy = "valorCaracteristicaProdutoCompositeKey.produto",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @MapKeyJoinColumn(name = "caracteristica_produto_id")
    private Map<CaracteristicaProduto, ValorCaracteristicaProduto> mapaProdutoAtributo = new HashMap<>();

    public Produto(String id) {
        this.id = id;
    }

    public Produto(String id, String descricao) {
        this.id = id;
        this.descricao = descricao;
    }
    
    public String getDescricao() {
        return (descricao == null) ? "" : descricao;
    }

    public ParametrosProdutoLocation getParametrosProdutoLocation(Location location) {

        /*
         * A referencia de cadastro de parametros admite um unico salto. A
         * projection central pre-resolve essa decisao para os calculos em
         * lote; este caminho de entidade preserva a mesma semantica para os
         * usos pontuais de dominio, sem seguir cadeias de referencias.
         */
        Location locationForProductLocationParameters =
                location.getReferenceLocationForProductLocationParameters();
        Location effectiveLocation = locationForProductLocationParameters == null
                ? location
                : locationForProductLocationParameters;

        return effectiveLocation.getParametrosProdutoLocation(this);
        
    }

    /** Retorna o valor de uma caracteristica real ou sintetica do material. */
    public String getValorCaracteristica(CaracteristicaProdutoInterface caracteristicaProduto) {

        return caracteristicaProduto.getValorCaracteristicaDeProduto(this);

    }

    /**
     * Cria ou atualiza o valor de uma caracteristica dentro do aggregate do
     * material, reproduzindo o comportamento usado pelo Data Upload legado.
     */
    public void setValorCaracteristica(
            CaracteristicaProduto caracteristicaProduto,
            String valorCaracteristica) {

        ValorCaracteristicaProduto valorCaracteristicaProduto = mapaProdutoAtributo.get(caracteristicaProduto);
        if (valorCaracteristicaProduto == null) {
            valorCaracteristicaProduto = new ValorCaracteristicaProduto(
                    new ValorCaracteristicaProduto.ValorCaracteristicaProdutoCompositeKey(
                            this,
                            caracteristicaProduto),
                    valorCaracteristica);
            mapaProdutoAtributo.put(caracteristicaProduto, valorCaracteristicaProduto);
        } else {
            valorCaracteristicaProduto.setAtributo(valorCaracteristica);
        }

    }

    public boolean getInativo() {
        return !getAtivo();
    }
    
    public boolean getAtivo() {
        return (ativo == null) ? true : ativo;
    }

    /**
     * Usado somente para carga de dados 'DataUpload' . Para demais usos usar getAtivo
     * @return 
     */    
    public Boolean getAtivoCadastrado() {
        return ativo;
    }

    public Constantes.StatusProduto getEstagioCicloVidaCadastrado() {
        return estagioCicloVida;
    }

    /**
     * Retorna status produto específico de uma location
     * NAO_LANCADO, STATUS_PRODUTO, REGULAR, DESCONTINUADO
     * @param dataReferencia
     * @param location
     * @param parametrosGlobais
     * @return 
     */
    public Constantes.StatusProduto getStatusProduto(LocalDateTime dataReferencia, Location location, ParametrosGlobais parametrosGlobais) {
        return getParametrosProdutoLocation(location).getStatusProduto(dataReferencia, parametrosGlobais);
    }
    
    /**
     * Retorna status produto padrão (nível produto)
     * NAO_LANCADO, STATUS_PRODUTO, REGULAR, DESCONTINUADO
     * @param dataReferencia
     * @param parametrosGlobais
     * @return 
     */
    public Constantes.StatusProduto getStatusProduto(LocalDateTime dataReferencia, ParametrosGlobais parametrosGlobais) {
        if (estagioCicloVida != null) return estagioCicloVida;
        if (dataDescontinuacao != null) {
            if (dataReferencia.isAfter(dataDescontinuacao) || dataReferencia.equals(dataDescontinuacao)) {
                return Constantes.StatusProduto.DESCONTINUADO;
            }
        }
        
        if (dataIntroducao == null) {
            return Constantes.StatusProduto.REGULAR;
        } else {
            int numeroDiasMaterialNovo = parametrosGlobais.getNumeroDiasProdutoNovo();
            if (numeroDiasMaterialNovo <= 0) {
                /*
                 * Community nao possui janela funcional de material novo. A data
                 * de introducao continua definindo "not launched" antes do
                 * lancamento, mas no proprio dia de introducao o material ja e
                 * tratado como regular.
                 */
                return dataReferencia.isBefore(dataIntroducao)
                        ? Constantes.StatusProduto.NAO_LANCADO
                        : Constantes.StatusProduto.REGULAR;
            }

            LocalDateTime dataFinalNovo = dataIntroducao.plusDays(numeroDiasMaterialNovo);
            if (dataReferencia.isAfter(dataFinalNovo)) {
                return Constantes.StatusProduto.REGULAR;
            } else if (dataReferencia.isBefore(dataIntroducao)) {
                return Constantes.StatusProduto.NAO_LANCADO;
            } else {
                return Constantes.StatusProduto.NOVO;
            }
        }
    }
        
    public UnidadeMedida getUnidadeMedidaPadrao(ParametrosGlobais parametrosGlobais) {
        return (unidadeMedidaPadrao == null) ? parametrosGlobais.getUnidadeMedidaPadraoSNP() : unidadeMedidaPadrao;
    }
    
    public UnidadeMedida getUnidadeMedidaVendas(ParametrosGlobais parametrosGlobais) {
        return (unidadeMedidaVendas == null) ? getUnidadeMedidaPadrao(parametrosGlobais) : unidadeMedidaVendas;
    }
    
    public UnidadeMedida getUnidadeMedidaTransferencia(ParametrosGlobais parametrosGlobais) {
        return (unidadeMedidaTransferencia == null) ? getUnidadeMedidaPadrao(parametrosGlobais) : unidadeMedidaTransferencia;
    }
    
    public UnidadeMedida getUnidadeMedidaPadraoCadastrado() {
        return unidadeMedidaPadrao;
    }
    
    public UnidadeMedida getUnidadeMedidaVendasCadastrado() {
        return unidadeMedidaVendas;
    }
    
    public UnidadeMedida getUnidadeMedidaTransferenciaCadastrado() {
        return unidadeMedidaTransferencia;
    }

    /**
     * Retorna o COGS unitario exatamente como foi cadastrado.
     *
     * <p>O Community nao atribui valor economico ao campo. O overlay
     * Enterprise deve distinguir ausencia/zero de valor positivo e validar
     * valores negativos ou nao finitos antes de usá-lo em uma projection.</p>
     */
    public Float getUnitCogsCadastrado() {

        return unitCogs;

    }

    /**
     * Retorna a unidade de medida de COGS exatamente como foi cadastrada.
     */
    public UnidadeMedida getUnitCogsUnitOfMeasureCadastrada() {

        return unitCogsUnitOfMeasure;

    }

    /**
     * Resolve a unidade de medida do COGS sem alterar o valor cadastrado.
     *
     * <p>O fallback historico usa a unidade SNP global, e nao a unidade padrão
     * especifica do material. Essa diferenca deve permanecer explicita para
     * evitar conversoes economicas silenciosamente incorretas.</p>
     */
    public UnidadeMedida getUnitCogsUnitOfMeasure(ParametrosGlobais parametrosGlobais) {

        return (unitCogsUnitOfMeasure == null)
                ? parametrosGlobais.getUnidadeMedidaPadraoSNP()
                : unitCogsUnitOfMeasure;

    }

    /**
     * Retorna o preco gross unitario exatamente como foi cadastrado.
     */
    public Double getUnitGrossPriceCadastrado() {

        return unitGrossPrice;

    }

    /**
     * Retorna o preco net unitario exatamente como foi cadastrado.
     */
    public Double getUnitNetPriceCadastrado() {

        return unitNetPrice;

    }

    /**
     * Retorna a unidade de medida de preco exatamente como foi cadastrada.
     *
     * <p>Snapshots de Demand Plan devem usar este getter para preservar
     * {@code null}; a unidade efetiva e resolvida somente quando o preco entra
     * na projection economica.</p>
     */
    public UnidadeMedida getUnitPriceUnitOfMeasureCadastrada() {

        return unitPriceUnitOfMeasure;

    }

    /**
     * Resolve a unidade de medida do preco sem alterar o cadastro original.
     *
     * <p>O fallback historico do preco padrao e a unidade de vendas de Demand
     * Planning. Ele e propositalmente diferente do fallback de COGS, que usa
     * a unidade SNP global.</p>
     */
    public UnidadeMedida getUnitPriceUnitOfMeasure(ParametrosGlobais parametrosGlobais) {

        return (unitPriceUnitOfMeasure == null)
                ? getUnidadeMedidaVendas(parametrosGlobais)
                : unitPriceUnitOfMeasure;

    }
    
    public Constantes.SNPModeloOperacional getModeloOperacional() {
        return (modeloOperacional == null) ? Constantes.SNPModeloOperacional.MTS : modeloOperacional;
    }

    /**
     * Retorna somente o valor persistido do modelo operacional.
     *
     * <p>A integracao precisa distinguir uma configuracao MTS explicita da
     * ausencia de configuracao, que continua usando o fallback efetivo MTS em
     * {@link #getModeloOperacional()}.</p>
     */
    public Constantes.SNPModeloOperacional getModeloOperacionalCadastrado() {

        return modeloOperacional;

    }
    
    public static Set<Produto> filtraMaterialSetAtivos(Collection<Produto> materialCollection) {
        return materialCollection.stream()
                    .filter(x -> x.getAtivo())
                    .collect(Collectors.toSet());
    }
    
    
    // CASCADE DELETES PARA PERMITIR A REMOÇÃO SEM PROBLEMAS DE DEPENDÊNCIA -------------------------------------------
    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "estoqueCompositeKey.produto", fetch = FetchType.LAZY)
    @Getter(value = AccessLevel.NONE)
    @Setter(value = AccessLevel.NONE)
    private List<Estoque> estoqueList = new ArrayList<>();
    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "materialOutput", fetch = FetchType.LAZY)
    @Getter(value = AccessLevel.NONE)
    @Setter(value = AccessLevel.NONE)
    private List<ListaTecnica> listaTecnicaList = new ArrayList<>();
    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "listaTecnicaComponenteCompositeKey.materialComponente", fetch = FetchType.LAZY)
    @Getter(value = AccessLevel.NONE)
    @Setter(value = AccessLevel.NONE)
    private List<ListaTecnicaComponente> listaTecnicaComponenteList = new ArrayList<>();
    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "materialOutput", fetch = FetchType.LAZY)
    @Getter(value = AccessLevel.NONE)
    @Setter(value = AccessLevel.NONE)
    private List<Roteiro> roteiroList = new ArrayList<>();
    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "conversaoUnidadeProdutoCompositeKey.produto", fetch = FetchType.LAZY)
    @Getter(value = AccessLevel.NONE)
    @Setter(value = AccessLevel.NONE)
    private List<ConversaoUnidadeProduto> conversaoUnidadeProdutoList = new ArrayList<>();
    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "linhaTransporteProdutoCompositeKey.produto", fetch = FetchType.LAZY)
    @Getter(value = AccessLevel.NONE)
    @Setter(value = AccessLevel.NONE)
    private List<LinhaTransporteProduto> linhaTransporteProdutoList = new ArrayList<>();
    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "key.produto", fetch = FetchType.LAZY)
    @Getter(value = AccessLevel.NONE)
    @Setter(value = AccessLevel.NONE)
    private List<DemandPlanItem> demandPlanItemList = new ArrayList<>();
    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "key.produto", fetch = FetchType.LAZY)
    @Getter(value = AccessLevel.NONE)
    @Setter(value = AccessLevel.NONE)
    private List<DistributionPlanItem> distributionPlanItemList = new ArrayList<>();
    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "inventoryPlanLinhaCompositeKey.produto", fetch = FetchType.LAZY)
    @Getter(value = AccessLevel.NONE)
    @Setter(value = AccessLevel.NONE)
    private List<InventoryPlanLinha> inventoryPlanLinhaList = new ArrayList<>();
    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "parametrosProdutoLocationCompositeKey.produto", fetch = FetchType.LAZY)
    @Getter(value = AccessLevel.NONE)
    @Setter(value = AccessLevel.NONE)
    private List<ParametrosProdutoLocation> parametrosProdutoLocationList = new ArrayList<>();

    @Override
    public int compareTo(Produto material) {
        return getId().compareTo(material.getId());
    }


    
}
