package com.opsfactor.community.capability.demandplanning.demandplan.projection;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlanItem;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.HistoricoDemandPlanItem;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosDemandPlanProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.factory.ParametrosDemandPlanningProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.FiltroDFUProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.demandplanning.demandplan.repository.DemandPlanItemRepository;
import com.opsfactor.community.capability.demandplanning.demandplan.repository.HistoricoDemandPlanItemRepository;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.FuncoesMap;
import org.javatuples.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.persistence.NoResultException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Factory de projections de Demand Planning usadas por services, planning book e supply planning.
 *
 * <p>A factory recebe conjuntos de materiais e locations, monta um
 * {@link FiltroDFUProjection} e so entao popula as linhas persistidas do plano
 * e do historico. Os repositories ainda expõem nomes fisicos com Produto porque
 * a entidade JPA compartilhada nao foi renomeada nesta fase.</p>
 */
@Component
public class DemandPlanProjectionFactory {

    /**
     * Repository das linhas fisicas do Demand Plan. As consultas carregam em
     * lote por plano, location e material para evitar N+1 durante a montagem
     * da projection.
     */
    @Autowired
    private DemandPlanItemRepository demandPlanItemRepository;

    /**
     * Repository do historico persistido do Demand Plan usado em Planning Book,
     * simulacao e Supply Planning. A projection filtra depois por DFU para
     * preservar o escopo exato solicitado pelo chamador.
     */
    @Autowired
    private HistoricoDemandPlanItemRepository historicoDemandPlanItemRepository;

    /**
     * Factory da projection de unidades de medida usada para converter valores
     * do plano sem consultar repositories dentro dos loops de calculo.
     */
    @Autowired
    private UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory;

    /**
     * Factory da projection central de parametros, materiais e locations.
     */
    @Autowired
    private ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory;

    /**
     * Factory dos parametros de Demand Planning associados ao perfil do plano.
     */
    @Autowired
    private ParametrosDemandPlanningProjectionFactory parametrosDemandPlanningProjectionFactory;

    /**
     * Demand catch-up por venda passada e uma capacidade Enterprise.
     *
     * <p>Algumas assinaturas ainda carregam o booleano por compatibilidade com
     * os chamadores legados durante a migracao. No Community, qualquer tentativa
     * de ativar esse caminho deve falhar imediatamente na factory, antes de
     * montar projections auxiliares ou consultar vendas diarizadas.</p>
     */
    private void validaCatchUpVendasCommunity(boolean efetuaCatchUpVendas) {

        if (efetuaCatchUpVendas) {
            throw new RequiresEnterpriseVersionException("Supply Planning demand catch-up");
        }

    }


    public DemandPlanningProjection getDemandPlanningProjectionVazio(
            DemandPlan demandPlan, Location location, Set<Produto> materiais,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection parametrosProjection,
            ParametrosDemandPlanProjection parametrosDemandPlanProjection,
            boolean efetuaCatchUpVendas) {

        validaCatchUpVendasCommunity(efetuaCatchUpVendas);
        return getDemandPlanningProjectionVazio(demandPlan, location, materiais, unidadeMedidaProjection, parametrosProjection,
                parametrosDemandPlanProjection);

    }

    private DemandPlanningProjection getDemandPlanningProjectionVazio(
            DemandPlan demandPlan, Location location, Set<Produto> materiais,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection parametrosProjection,
            ParametrosDemandPlanProjection parametrosDemandPlanProjection) {


        Set<Location> locations  = new HashSet<>();
        locations.add(location);

        return getDemandPlanningProjectionVazio(
                demandPlan, locations, materiais,
                unidadeMedidaProjection,
                parametrosProjection,
                parametrosDemandPlanProjection);

    }

    public DemandPlanningProjection getDemandPlanningProjectionVazio(
            DemandPlan demandPlan, Set<Location> locations, Set<Produto> materiais,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            ParametrosDemandPlanProjection parametrosDemandPlanProjection,
            boolean efetuaCatchUpVendas) {

        validaCatchUpVendasCommunity(efetuaCatchUpVendas);
        return getDemandPlanningProjectionVazio(demandPlan, locations, materiais, unidadeMedidaProjection, clusterEParametrosProjection,
                parametrosDemandPlanProjection);

    }

    public DemandPlanningProjection getDemandPlanningProjectionVazio(
            DemandPlan demandPlan, Set<Location> locations, Set<Produto> materiais,
            boolean efetuaCatchUpVendas) {

        validaCatchUpVendasCommunity(efetuaCatchUpVendas);
        return getDemandPlanningProjectionVazio(demandPlan, locations, materiais);

    }

    private DemandPlanningProjection getDemandPlanningProjectionVazio(
            DemandPlan demandPlan, Set<Location> locations, Set<Produto> materiais,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            ParametrosDemandPlanProjection parametrosDemandPlanProjection) {

        /*
         * O filtro DFU e a fonte de verdade do escopo material/location da
         * projection. Mesmo quando a consulta JPA recebe conjuntos separados de
         * locations e materiais, filtramos depois pela combinacao para remover
         * pares location/material que nao fazem parte do recorte original.
         */
        FiltroDFUProjection dfuProjection = new FiltroDFUProjection(
                locations,
                materiais,
                clusterEParametrosProjection);

        return getDemandPlanningProjectionVazio(demandPlan, dfuProjection, clusterEParametrosProjection, unidadeMedidaProjection,
                parametrosDemandPlanProjection);

    }

    private DemandPlanningProjection getDemandPlanningProjectionVazio(
            DemandPlan demandPlan,
            Set<Location> locations,
            Set<Produto> materiais) {


        return getDemandPlanningProjectionVazio(
                demandPlan,
                locations,
                materiais,
                unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache(),
                clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache(),
                parametrosDemandPlanningProjectionFactory.getParametrosDemandPlanProjectionDeCache(
                                demandPlan.getPerfilExecucaoDemandPlan()));

    }

    public DemandPlanningProjection getDemandPlanningProjectionVazio(
            DemandPlan demandPlan,
            FiltroDFUProjection dfuProjection,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            ParametrosDemandPlanProjection parametrosDemandPlanProjection,
            boolean efetuaCatchUpVendas) {

        validaCatchUpVendasCommunity(efetuaCatchUpVendas);
        return getDemandPlanningProjectionVazio(demandPlan, dfuProjection, clusterEParametrosProjection, unidadeMedidaProjection,
                parametrosDemandPlanProjection);

    }

    private DemandPlanningProjection getDemandPlanningProjectionVazio(
            DemandPlan demandPlan,
            FiltroDFUProjection dfuProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ParametrosDemandPlanProjection parametrosDemandPlanProjection) {


        Calendario calendario = demandPlan.getCalendarioDoDemandPlanComHistoricoMaximo(
                parametrosDemandPlanProjection);

        return getDemandPlanningProjectionVazio(
                demandPlan,
                calendario,
                dfuProjection,
                unidadeMedidaProjection,
                clusterEParametrosProjection,
                parametrosDemandPlanProjection);

    }

    public DemandPlanningProjection getDemandPlanningProjectionVazio(
            DemandPlan demandPlan,
            Calendario calendario,
            FiltroDFUProjection dfuProjection) {


        return getDemandPlanningProjectionVazio(
                demandPlan,
                calendario,
                dfuProjection,
                unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache(),
                clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache(),
                parametrosDemandPlanningProjectionFactory.getParametrosDemandPlanProjectionDeCache(
                                demandPlan.getPerfilExecucaoDemandPlan()));

    }

    public DemandPlanningProjection getDemandPlanningProjectionVazio(
            DemandPlan demandPlan,
            Calendario calendario,
            FiltroDFUProjection dfuProjection,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            ParametrosDemandPlanProjection parametrosDemandPlanProjection) {


        DemandPlanningProjection demandPlanningProjection = criaDemandPlanningProjection(
                demandPlan,
                unidadeMedidaProjection,
                clusterEParametrosProjection,
                parametrosDemandPlanProjection,
                calendario,
                dfuProjection,
                false,
                null,
                null);

        return demandPlanningProjection;

    }

    /**
     * Ponto unico de construcao da projection principal de Demand Planning.
     *
     * <p>O Community devolve a projection conservadora, que bloqueia escrita em
     * colunas Enterprise como `New Products` e `Uplift`. Overlays privados podem
     * sobrescrever este metodo para retornar uma subclass com capabilities ja
     * migradas, reaproveitando toda a montagem de escopo, calendario, UOM e
     * parametros feita pela factory Community.</p>
     */
    protected DemandPlanningProjection criaDemandPlanningProjection(
            DemandPlan demandPlan,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            ParametrosDemandPlanProjection parametrosDemandPlanProjection,
            Calendario calendario,
            FiltroDFUProjection dfuProjection,
            boolean consolidacaoDemandaClientes,
            PerfilExecucaoSupplyPlan.ModoPropagacaoDemanda modoPropagacaoDemanda,
            VersaoMalha versaoMalha) {

        return new DemandPlanningProjection(
                demandPlan,
                unidadeMedidaProjection,
                clusterEParametrosProjection,
                parametrosDemandPlanProjection,
                calendario,
                dfuProjection,
                consolidacaoDemandaClientes,
                modoPropagacaoDemanda,
                versaoMalha);

    }

    public DemandPlanningProjection getDemandPlanningProjectionCompleto(
            DemandPlan demandPlan,
            FiltroDFUProjection dfuProjection,
            boolean efetuaCatchUpVendas) {


        return getDemandPlanningProjectionCompleto(
                demandPlan,
                dfuProjection,
                unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache(),
                clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache(),
                parametrosDemandPlanningProjectionFactory.getParametrosDemandPlanProjectionDeCache(
                                demandPlan.getPerfilExecucaoDemandPlan()),
                efetuaCatchUpVendas);

    }

    public DemandPlanningProjection getDemandPlanningProjectionCompleto(
            DemandPlan demandPlan,
            FiltroDFUProjection dfuProjection,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection parametrosProjection,
            ParametrosDemandPlanProjection parametrosDemandPlanProjection,
            boolean efetuaCatchUpVendas) {

        DemandPlanningProjection demandPlanningProjection = getDemandPlanningProjectionVazio(
                demandPlan, dfuProjection,
                unidadeMedidaProjection,
                parametrosProjection,
                parametrosDemandPlanProjection,
                efetuaCatchUpVendas);

        populaDemandPlanningProjectionComDemandPlan(demandPlanningProjection);
        populaDemandPlanningProjectionComHistoricoDemandPlan(demandPlanningProjection);

        return demandPlanningProjection;

    }

    public DemandPlanningProjection getDemandPlanningProjectionCompletoComDadosLag(
            DemandPlan demandPlan,
            int lagPeriodos,
            FiltroDFUProjection dfuProjection,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection parametrosProjection,
            ParametrosDemandPlanProjection parametrosDemandPlanProjection,
            boolean efetuaCatchUpVendas) {

        validaLagPeriodosDemandPlanProjection(lagPeriodos);

        DemandPlanningProjection demandPlanningProjection = getDemandPlanningProjectionVazio(
                demandPlan, dfuProjection,
                unidadeMedidaProjection,
                parametrosProjection,
                parametrosDemandPlanProjection,
                efetuaCatchUpVendas);

        populaDemandPlanningProjectionComDemandPlanParaLagPeriodos(demandPlanningProjection, lagPeriodos);

        return demandPlanningProjection;

    }

    public DemandPlanningProjection getDemandPlanningProjectionCompleto(
            DemandPlan demandPlan,
            Set<Location> locations,
            Set<Produto> materiais,
            boolean efetuaCatchUpVendas) {


        return getDemandPlanningProjectionCompleto(
                demandPlan,
                locations,
                materiais,
                unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache(),
                clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache(),
                parametrosDemandPlanningProjectionFactory.getParametrosDemandPlanProjectionDeCache(
                                demandPlan.getPerfilExecucaoDemandPlan()),
                efetuaCatchUpVendas);

    }

    public DemandPlanningProjection getDemandPlanningProjectionCompleto(
            DemandPlan demandPlan,
            LocationProjection locationProjection,
            MaterialProjection materialProjection,
            boolean efetuaCatchUpVendas) {


        return getDemandPlanningProjectionCompleto(
                demandPlan,
                locationProjection,
                materialProjection,
                unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache(),
                clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache(),
                parametrosDemandPlanningProjectionFactory.getParametrosDemandPlanProjectionDeCache(
                                demandPlan.getPerfilExecucaoDemandPlan()),
                efetuaCatchUpVendas);

    }


    public DemandPlanningProjection getDemandPlanningProjectionCompleto(
            DemandPlan demandPlan,
            LocationProjection locationProjection,
            MaterialProjection materialProjection,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            ParametrosDemandPlanProjection parametrosDemandPlanProjection,
            boolean efetuaCatchUpVendas) {


        return getDemandPlanningProjectionCompleto(
                demandPlan,
                locationProjection.getLocationsAtivas(),
                materialProjection.getMateriaisAtivos(),
                unidadeMedidaProjection,
                clusterEParametrosProjection,
                parametrosDemandPlanProjection,
                efetuaCatchUpVendas);

    }


    public DemandPlanningProjection getDemandPlanningProjectionCompleto(
            DemandPlan demandPlan,
            Set<Location> locations,
            Set<Produto> materiais,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            ParametrosDemandPlanProjection parametrosDemandPlanProjection,
            boolean efetuaCatchUpVendas) {

        DemandPlanningProjection demandPlanningProjection = getDemandPlanningProjectionVazio(
                demandPlan,
                locations, materiais,
                unidadeMedidaProjection,
                clusterEParametrosProjection,
                parametrosDemandPlanProjection,
                efetuaCatchUpVendas);

        populaDemandPlanningProjectionComDemandPlan(demandPlanningProjection);
        populaDemandPlanningProjectionComHistoricoDemandPlan(demandPlanningProjection);

        return demandPlanningProjection;
    }

    public DemandPlanningProjection getDemandPlanningProjectionCompletoComDadosLag(
            DemandPlan demandPlan, Set<Location> locations, Set<Produto> materiais,
            int lagPeriodos,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection parametrosProjection,
            ParametrosDemandPlanProjection parametrosDemandPlanProjection,
            boolean efetuaCatchUpVendas) {

        validaLagPeriodosDemandPlanProjection(lagPeriodos);

        DemandPlanningProjection demandPlanningProjection = getDemandPlanningProjectionVazio(
                demandPlan,
                locations, materiais,
                unidadeMedidaProjection,
                parametrosProjection,
                parametrosDemandPlanProjection,
                efetuaCatchUpVendas);

        populaDemandPlanningProjectionComDemandPlanParaLagPeriodos(demandPlanningProjection, lagPeriodos);

        return demandPlanningProjection;
    }

    /**
     * Monta um projection de demand planning carregando somente os períodos relevantes para a análise.
     * Esse caminho evita extrações completas do plano quando o chamador já possui um filtro temporal definido.
     */
    public DemandPlanningProjection getDemandPlanningProjectionCompletoComDadosNoRangeDatas(
            DemandPlan demandPlan,
            Set<Location> locations,
            Set<Produto> materiais,
            LocalDateTime dataReferenciaInicial,
            LocalDateTime dataReferenciaFinal,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection parametrosProjection,
            ParametrosDemandPlanProjection parametrosDemandPlanProjection,
            boolean efetuaCatchUpVendas) {

        DemandPlanningProjection demandPlanningProjection = getDemandPlanningProjectionVazio(
                demandPlan,
                locations,
                materiais,
                unidadeMedidaProjection,
                parametrosProjection,
                parametrosDemandPlanProjection,
                efetuaCatchUpVendas);

        populaDemandPlanningProjectionComDemandPlanNoRangeDatas(
                demandPlanningProjection,
                dataReferenciaInicial,
                dataReferenciaFinal);

        return demandPlanningProjection;
    }

    /**
     * Monta a projection lendo somente os períodos explicitamente selecionados.
     *
     * <p>Diferente de uma faixa, esta variante preserva uma seleção esparsa de
     * períodos e não materializa os períodos intermediários no banco.</p>
     */
    public DemandPlanningProjection getDemandPlanningProjectionCompletoComDadosNasDatas(
            DemandPlan demandPlan,
            FiltroDFUProjection dfuProjection,
            Collection<LocalDateTime> dataReferenciaCollection,
            boolean efetuaCatchUpVendas) {

        if (dataReferenciaCollection == null || dataReferenciaCollection.isEmpty()) {
            throw new IllegalArgumentException(
                    "Demand Plan projection selected-period snapshot requires reference dates.");
        }

        DemandPlanningProjection demandPlanningProjection = getDemandPlanningProjectionVazio(
                demandPlan,
                dfuProjection,
                unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache(),
                clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache(),
                parametrosDemandPlanningProjectionFactory.getParametrosDemandPlanProjectionDeCache(
                        demandPlan.getPerfilExecucaoDemandPlan()),
                efetuaCatchUpVendas);

        populaDemandPlanningProjectionComDemandPlanNasDatas(
                demandPlanningProjection,
                dataReferenciaCollection);

        return demandPlanningProjection;

    }

    public DemandPlanningProjection getDemandPlanningProjectionCompleto(
            DemandPlan demandPlan, Location location, Set<Produto> materiais,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection parametrosProjection,
            ParametrosDemandPlanProjection parametrosDemandPlanProjection,
            boolean efetuaCatchUpVendas) {


        Set<Location> locationSet = new HashSet<>();
        locationSet.add(location);

        return getDemandPlanningProjectionCompleto(
                demandPlan,
                locationSet, materiais,
                unidadeMedidaProjection,
                parametrosProjection,
                parametrosDemandPlanProjection,
                efetuaCatchUpVendas);

    }

    public DemandPlanningProjection getDemandPlanningProjectionCompleto(
            DemandPlan demandPlan, Location location, Set<Produto> materiais,
            boolean efetuaCatchUpVendas) {


        return getDemandPlanningProjectionCompleto(
                demandPlan,
                location,
                materiais,
                unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache(),
                clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache(),
                parametrosDemandPlanningProjectionFactory.getParametrosDemandPlanProjectionDeCache(
                                demandPlan.getPerfilExecucaoDemandPlan()),
                efetuaCatchUpVendas);

    }


    /**
     * Valida o plano que ancora uma projection de Demand Planning.
     *
     * <p>A factory pode receber projections auxiliares ja materializadas por
     * services chamadores, mas o plano em si nunca deve ser implicito. Ele e
     * usado para calendario, chave das consultas JPA e criacao de linhas novas
     * dentro da projection.</p>
     */
    /**
     * Valida o plano quando o overload precisa buscar parametros por perfil.
     */
    /**
     * Valida o calendario explicito usado para construir uma projection vazia.
     */
    /**
     * Valida a projection de unidades antes que calculos de Demand/Supply Plan
     * leiam conversoes ou unidades padrao.
     */
    /**
     * Valida a projection central de clusters/parametros antes de criar filtros
     * DFU ou acessar unidades e calendarios derivados.
     */
    /**
     * Valida os parametros Demand Planning usados pela projection.
     *
     * <p>Overloads que calculam calendario tambem chamam a variante com
     * perfil obrigatorio. A variante base existe para projections ja recebidas
     * com calendario explicito, mas ainda exige parametros globais para manter
     * o snapshot estrutural consistente.</p>
     */
    /**
     * Valida os parametros Demand Planning quando eles tambem dirigem o
     * calendario do Demand Plan.
     */
    /**
     * Valida o filtro DFU que define o escopo material/location da projection.
     */
    /**
     * Valida uma location escalar antes de transforma-la em conjunto DFU.
     */
    /**
     * Valida projection de locations recebida de services chamadores antes de
     * ler o subconjunto ativo.
     */
    /**
     * Valida projection de materiais recebida de services chamadores antes de
     * ler o subconjunto ativo.
     */

    public void populaDemandPlanningProjectionComDemandPlan(DemandPlanningProjection demandPlanningProjection) {

        validaDemandPlanningProjectionParaRepository(
                demandPlanningProjection,
                "Demand Plan projection line snapshot");

        FiltroDFUProjection dfuProjection = demandPlanningProjection.getFiltroDfuProjection();

        Collection<DemandPlanItem> demandPlanItems = validaDemandPlanItemRepositoryResult(
                demandPlanItemRepository.customFindByDemandPlanItemKeyDemandPlanIdAndDemandPlanItemKeyLocationInAndDemandPlanItemKeyProdutoIn(
                        demandPlanningProjection.getDemandPlan().getId(),
                        dfuProjection.getLocations(),
                        dfuProjection.getMateriais()),
                "Demand Plan projection line snapshot")
                .stream()
                .filter(demandPlanItem -> dfuProjection.contemCombinacaoLocationMaterial(demandPlanItem.getLocation(), demandPlanItem.getProduto()))
                .collect(Collectors.toList());

        populaDemandPlanningProjectionComDemandPlanItems(demandPlanningProjection, demandPlanItems);

    }

    public void populaDemandPlanningProjectionComDemandPlanParaLagPeriodos(DemandPlanningProjection demandPlanningProjection, int lagPeriodos) {

        validaLagPeriodosDemandPlanProjection(lagPeriodos);
        validaDemandPlanningProjectionParaRepository(
                demandPlanningProjection,
                "Demand Plan projection lag line snapshot");

        FiltroDFUProjection dfuProjection = demandPlanningProjection.getFiltroDfuProjection();

        Calendario calendario = demandPlanningProjection.getCalendario();
        LocalDateTime dataHorarioFinalPeriodoLagPeriodos = calendario
                .getUltimaDataHorarioPeriodo(
                        calendario.getPosicaoPeriodoPresente() + lagPeriodos);

        Collection<DemandPlanItem> demandPlanItems = validaDemandPlanItemRepositoryResult(
                demandPlanItemRepository.customFindByDemandPlanItemKeyDemandPlanIdAndDemandPlanItemKeyLocationInAndDemandPlanItemKeyProdutoInAndDemandPlanItemKeyDataReferencia(
                        demandPlanningProjection.getDemandPlan().getId(),
                        dfuProjection.getLocations(),
                        dfuProjection.getMateriais(),
                        dataHorarioFinalPeriodoLagPeriodos),
                "Demand Plan projection lag line snapshot")
                .stream()
                .filter(demandPlanItem -> dfuProjection.contemCombinacaoLocationMaterial(demandPlanItem.getLocation(), demandPlanItem.getProduto()))
                .collect(Collectors.toList());

        populaDemandPlanningProjectionComDemandPlanItems(demandPlanningProjection, demandPlanItems);

    }

    /**
     * Valida o lag antes de qualquer projection/cache/repository.
     *
     * <p>O lag e um deslocamento relativo no calendario do plano. Valor
     * negativo significaria buscar periodo anterior ao ponto de referencia da
     * rodada e deve falhar na borda publica, inclusive quando o chamador usa
     * diretamente o populator incremental.</p>
     */
    private static void validaLagPeriodosDemandPlanProjection(int lagPeriodos) {

        if (lagPeriodos < 0) {
            throw new IllegalArgumentException(
                    "Demand Plan projection lag line snapshot requires non-negative lag periods.");
        }

    }

    /**
     * Popula apenas as linhas de demand plan compreendidas no intervalo solicitado.
     */
    public void populaDemandPlanningProjectionComDemandPlanNoRangeDatas(
            DemandPlanningProjection demandPlanningProjection,
            LocalDateTime dataReferenciaInicial,
            LocalDateTime dataReferenciaFinal) {

        validaDemandPlanningProjectionParaRepository(
                demandPlanningProjection,
                "Demand Plan projection date-range line snapshot");
        if (dataReferenciaInicial == null || dataReferenciaFinal == null) {
            throw new IllegalArgumentException(
                    "Demand Plan projection date-range line snapshot requires start and end reference dates.");
        }
        if (dataReferenciaInicial.isAfter(dataReferenciaFinal)) {
            throw new IllegalArgumentException(
                    "Demand Plan projection date-range line snapshot requires start date before or equal to end date.");
        }

        FiltroDFUProjection dfuProjection = demandPlanningProjection.getFiltroDfuProjection();

        Collection<DemandPlanItem> demandPlanItems = validaDemandPlanItemRepositoryResult(
                demandPlanItemRepository
                .customFindByDemandPlanItemKeyDemandPlanIdAndDemandPlanItemKeyLocationInAndDemandPlanItemKeyProdutoInAndDemandPlanItemKeyDataReferenciaBetween(
                        demandPlanningProjection.getDemandPlan().getId(),
                        dfuProjection.getLocations(),
                        dfuProjection.getMateriais(),
                        dataReferenciaInicial,
                        dataReferenciaFinal),
                "Demand Plan projection date-range line snapshot")
                .stream()
                .filter(demandPlanItem -> dfuProjection.contemCombinacaoLocationMaterial(
                        demandPlanItem.getLocation(),
                        demandPlanItem.getProduto()))
                .collect(Collectors.toList());

        populaDemandPlanningProjectionComDemandPlanItems(demandPlanningProjection, demandPlanItems);
    }

    /** Popula a projection somente com os fechamentos de período selecionados. */
    public void populaDemandPlanningProjectionComDemandPlanNasDatas(
            DemandPlanningProjection demandPlanningProjection,
            Collection<LocalDateTime> dataReferenciaCollection) {

        validaDemandPlanningProjectionParaRepository(
                demandPlanningProjection,
                "Demand Plan projection selected-period line snapshot");
        if (dataReferenciaCollection == null || dataReferenciaCollection.isEmpty()
                || dataReferenciaCollection.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Demand Plan projection selected-period line snapshot requires non-null reference dates.");
        }

        FiltroDFUProjection dfuProjection = demandPlanningProjection.getFiltroDfuProjection();
        Collection<DemandPlanItem> demandPlanItems = validaDemandPlanItemRepositoryResult(
                demandPlanItemRepository
                        .customFindByDemandPlanItemKeyDemandPlanIdAndDemandPlanItemKeyLocationInAndDemandPlanItemKeyProdutoInAndDemandPlanItemKeyDataReferenciaIn(
                                demandPlanningProjection.getDemandPlan().getId(),
                                dfuProjection.getLocations(),
                                dfuProjection.getMateriais(),
                                dataReferenciaCollection),
                "Demand Plan projection selected-period line snapshot")
                .stream()
                .filter(demandPlanItem -> dfuProjection.contemCombinacaoLocationMaterial(
                        demandPlanItem.getLocation(),
                        demandPlanItem.getProduto()))
                .collect(Collectors.toList());

        populaDemandPlanningProjectionComDemandPlanItems(demandPlanningProjection, demandPlanItems);

    }

    public void populaDemandPlanningProjectionComHistoricoDemandPlan(DemandPlanningProjection demandPlanningProjection) {

        validaDemandPlanningProjectionParaRepository(
                demandPlanningProjection,
                "Demand Plan historical line snapshot");

        FiltroDFUProjection dfuProjection = demandPlanningProjection.getFiltroDfuProjection();

        Collection<HistoricoDemandPlanItem> historicoDemandPlanItems = validaHistoricoDemandPlanItemRepositoryResult(
                historicoDemandPlanItemRepository.customFindByHistoricoDemandPlanItemKeyDemandPlanIdAndHistoricoDemandPlanItemKeyLocationInAndHistoricoDemandPlanItemKeyProdutoIn(
                        demandPlanningProjection.getDemandPlan().getId(),
                        dfuProjection.getLocations(),
                        dfuProjection.getMateriais()),
                "Demand Plan historical line snapshot")
                .stream()
                .filter(historicoDemandPlanItem -> dfuProjection.contemCombinacaoLocationMaterial(historicoDemandPlanItem.getLocation(), historicoDemandPlanItem.getProduto()))
                .collect(Collectors.toList());

        populaDemandPlanningProjectionComHistoricoDemandPlanItems(demandPlanningProjection, historicoDemandPlanItems);

    }

    public void populaDemandPlanningProjectionComDemandPlanItems(DemandPlanningProjection demandPlanningProjection, Collection<DemandPlanItem> demandPlanItems) {

        validaDemandPlanningProjectionParaPopulacao(
                demandPlanningProjection,
                "Demand Plan projection population");
        validaDemandPlanItemInput(
                demandPlanItems,
                "Demand Plan projection population");

        /*
         * A populacao pode receber linhas vindas de repository ou de um fluxo
         * transicional ja materializado. Depois da validacao, a indexacao em
         * paralelo continua segura porque cada linha tem chave funcional
         * completa para o BI em memoria.
         */
        demandPlanItems.parallelStream().forEach(demandPlanningProjection::addDemandPlanItem);

    }

    public void populaDemandPlanningProjectionComHistoricoDemandPlanItems(DemandPlanningProjection demandPlanningProjection, Collection<HistoricoDemandPlanItem> historicoDemandPlanItems) {

        validaDemandPlanningProjectionParaPopulacao(
                demandPlanningProjection,
                "Demand Plan historical projection population");
        validaHistoricoDemandPlanItemInput(
                historicoDemandPlanItems,
                "Demand Plan historical projection population");

        /*
         * Historico tambem e indexado em paralelo; por isso validamos item e
         * chave antes da chamada ao BI, evitando falhas concorrentes pouco
         * rastreaveis.
         */
        historicoDemandPlanItems.parallelStream().forEach(demandPlanningProjection::addHistoricoDemandPlanItem);

    }

    private void validaDemandPlanningProjectionParaRepository(
            DemandPlanningProjection demandPlanningProjection,
            String projectionDescription) {

        validaDemandPlanningProjectionParaPopulacao(
                demandPlanningProjection,
                projectionDescription);

        if (demandPlanningProjection.getDemandPlan() == null
                || demandPlanningProjection.getDemandPlan().getId() == null
                || demandPlanningProjection.getFiltroDfuProjection() == null
                || demandPlanningProjection.getFiltroDfuProjection().getLocations() == null
                || demandPlanningProjection.getFiltroDfuProjection().getMateriais() == null) {
            throw new IllegalArgumentException(
                    projectionDescription
                            + " requires Demand Plan id and DFU filter.");
        }

    }

    private void validaDemandPlanningProjectionParaPopulacao(
            DemandPlanningProjection demandPlanningProjection,
            String projectionDescription) {

        if (demandPlanningProjection == null) {
            throw new IllegalArgumentException(
                    projectionDescription
                            + " requires target projection.");
        }

    }

    private Collection<DemandPlanItem> validaDemandPlanItemRepositoryResult(
            Collection<DemandPlanItem> demandPlanItemCollection,
            String projectionDescription) {

        return validaDemandPlanItemCollection(
                demandPlanItemCollection,
                projectionDescription,
                "repository returned",
                IllegalStateException::new);

    }

    private Collection<DemandPlanItem> validaDemandPlanItemInput(
            Collection<DemandPlanItem> demandPlanItemCollection,
            String projectionDescription) {

        return validaDemandPlanItemCollection(
                demandPlanItemCollection,
                projectionDescription,
                "received",
                IllegalArgumentException::new);

    }

    private Collection<DemandPlanItem> validaDemandPlanItemCollection(
            Collection<DemandPlanItem> demandPlanItemCollection,
            String projectionDescription,
            String sourceDescription,
            java.util.function.Function<String, RuntimeException> exceptionFactory) {

        if (demandPlanItemCollection == null) {
            throw exceptionFactory.apply(
                    projectionDescription
                            + " "
                            + sourceDescription
                            + " null Demand Plan line collection.");
        }

        demandPlanItemCollection.forEach(demandPlanItem -> {
            if (demandPlanItem == null) {
                throw exceptionFactory.apply(
                        projectionDescription
                                + " "
                                + sourceDescription
                                + " null Demand Plan line.");
            }
            if (demandPlanItem.getKey() == null
                    || demandPlanItem.getDemandPlan() == null
                    || demandPlanItem.getLocation() == null
                    || demandPlanItem.getProduto() == null
                    || demandPlanItem.getDataReferencia() == null) {
                throw exceptionFactory.apply(
                        projectionDescription
                                + " "
                                + sourceDescription
                                + " Demand Plan line without demand plan, location, material or reference date.");
            }
        });

        return demandPlanItemCollection;

    }

    private Collection<HistoricoDemandPlanItem> validaHistoricoDemandPlanItemRepositoryResult(
            Collection<HistoricoDemandPlanItem> historicoDemandPlanItemCollection,
            String projectionDescription) {

        return validaHistoricoDemandPlanItemCollection(
                historicoDemandPlanItemCollection,
                projectionDescription,
                "repository returned",
                IllegalStateException::new);

    }

    private Collection<HistoricoDemandPlanItem> validaHistoricoDemandPlanItemInput(
            Collection<HistoricoDemandPlanItem> historicoDemandPlanItemCollection,
            String projectionDescription) {

        return validaHistoricoDemandPlanItemCollection(
                historicoDemandPlanItemCollection,
                projectionDescription,
                "received",
                IllegalArgumentException::new);

    }

    private Collection<HistoricoDemandPlanItem> validaHistoricoDemandPlanItemCollection(
            Collection<HistoricoDemandPlanItem> historicoDemandPlanItemCollection,
            String projectionDescription,
            String sourceDescription,
            java.util.function.Function<String, RuntimeException> exceptionFactory) {

        if (historicoDemandPlanItemCollection == null) {
            throw exceptionFactory.apply(
                    projectionDescription
                            + " "
                            + sourceDescription
                            + " null historical line collection.");
        }

        Set<HistoricoDemandPlanItem.HistoricoDemandPlanItemKey> chavesHistoricoDemandPlanItem =
                new HashSet<>();

        /*
         * O snapshot historico pode chegar como List para preservar
         * cardinalidade ate esta validation. A chave duplicada indica
         * inconsistencia do snapshot antes do indice paralelo da projection.
         */
        historicoDemandPlanItemCollection.forEach(historicoDemandPlanItem -> {
            if (historicoDemandPlanItem == null) {
                throw exceptionFactory.apply(
                        projectionDescription
                                + " "
                                + sourceDescription
                                + " null historical line.");
            }
            if (historicoDemandPlanItem.getKey() == null
                    || historicoDemandPlanItem.getDemandPlan() == null
                    || historicoDemandPlanItem.getLocation() == null
                    || historicoDemandPlanItem.getProduto() == null
                    || historicoDemandPlanItem.getDataReferencia() == null) {
                throw exceptionFactory.apply(
                        projectionDescription
                                + " "
                                + sourceDescription
                                + " historical line without demand plan, location, material or reference date.");
            }
            if (!chavesHistoricoDemandPlanItem.add(
                    historicoDemandPlanItem.getKey())) {
                throw exceptionFactory.apply(
                        projectionDescription
                                + " "
                                + sourceDescription
                                + " duplicated historical line for demand plan "
                                + historicoDemandPlanItem.getDemandPlan().getId()
                                + ", location "
                                + historicoDemandPlanItem.getLocation().getId()
                                + ", material "
                                + historicoDemandPlanItem.getProduto().getId()
                                + " and reference date "
                                + historicoDemandPlanItem.getDataReferencia()
                                + ".");
            }
        });

        return historicoDemandPlanItemCollection;

    }

    /**
     * Usa a versão de malha selecionada para atualizar um novo DemandPlanningProjection com os
     * valores de demanda do nível cliente do projection original consolidados no nível location interna
     * @param demandPlanningProjectionOriginal
     * @param versaoMalha
     */
    public static DemandPlanningProjection geraNovoProjectionComConsolidacaoDemandaClientesDeProjectionOriginal(
            DemandPlanningProjection demandPlanningProjectionOriginal,
            PerfilExecucaoSupplyPlan.ModoPropagacaoDemanda modoPropagacaoDemanda,
            VersaoMalha versaoMalha,
            SupplyNetworkProjection supplyNetworkProjection) {


        ClusterEParametrosProjection clusterEParametrosProjection =
                demandPlanningProjectionOriginal.getClusterEParametrosProjection();
        UnidadeMedidaProjection unidadeMedidaProjection =
                demandPlanningProjectionOriginal.getUnidadeMedidaProjection();
        ParametrosDemandPlanProjection parametrosDemandPlanProjection =
                demandPlanningProjectionOriginal.getParametrosDemandPlanProjection();
        Calendario calendarioOriginal = demandPlanningProjectionOriginal.getCalendario();
        FiltroDFUProjection filtroDFUProjection =
                demandPlanningProjectionOriginal.getFiltroDfuProjection();

        DemandPlanningProjection demandPlanningProjectionOriginalConsolidado = new DemandPlanningProjection(
                demandPlanningProjectionOriginal.getDemandPlan(),
                unidadeMedidaProjection,
                clusterEParametrosProjection,
                parametrosDemandPlanProjection,
                calendarioOriginal,
                filtroDFUProjection,
                true,
                modoPropagacaoDemanda,
                versaoMalha);

        LocalDateTime dataHorarioReferencia = calendarioOriginal.getPrimeiraDataHorarioPeriodo(calendarioOriginal.getPosicaoPeriodoPresente());

        Map<Location, Map<Produto, Location>> mapaLocationsOrigemConsideradasPorLocationCliente = demandPlanningProjectionOriginal
                .getTodosDemandPlanItems()
                .stream()
                .map(demandPlanItem -> Pair.with(demandPlanItem.getLocation(), demandPlanItem.getProduto()))
                .filter(pair -> modoPropagacaoDemanda.verificaSeRealizaPropagacao(pair.getValue0()))
                .distinct()
                .collect(Collectors.groupingBy(
                        pair -> ((Pair<Location,Produto>) pair).getValue0(),
                        Collectors.toMap(
                                pair -> ((Pair<Location,Produto>) pair).getValue1(),
                                pair -> supplyNetworkProjection.getLocationOrigemPrioritaria(
                                                modoPropagacaoDemanda.getTipoLocationDestinoPropagacao(),
                                                versaoMalha,
                                                pair.getValue0(), pair.getValue1(),
                                                dataHorarioReferencia,
                                                null)
                                        .orElseThrow(() -> new NoResultException(
                                                "Client location " + pair.getValue0() + " does not have an internal origin within supply network " + versaoMalha.getId())))));


        demandPlanningProjectionOriginal.getLocationsComPlano()
                .parallelStream()
                .forEach(location -> {
                    for (Produto material : demandPlanningProjectionOriginal.getMateriaisComPlanoNaLocation(location)) {
                        UnidadeMedida unidadeMedidaPadraoDp = parametrosDemandPlanProjection
                                .getParametrosDemandPlanNivelClusterProjection(
                                        clusterEParametrosProjection.getClusterLocationsDeLocation(location),
                                        clusterEParametrosProjection.getClusterMateriaisDemandPlanning(material, location))
                                        .getParametrosGeraisDemandPlanningProjection()
                                        .getUnidadeMedidaDP();

                        Location locationOrigem = location;
                        if (modoPropagacaoDemanda.verificaSeRealizaPropagacao(location)) {
                            locationOrigem = FuncoesMap.getElementoDeNestedMap(
                                            mapaLocationsOrigemConsideradasPorLocationCliente,
                                            Location.class,
                                            location, material)
                                    .orElse(location);
                        }

                        for (int i = calendarioOriginal.getPosicaoPeriodoPresente(); i <= calendarioOriginal.getPosicaoPeriodoFinalFuturo(); i++) {

                            /*
                             * A projection conserva todas as colunas fisicas do
                             * Demand Plan para manter compatibilidade com schema
                             * compartilhado. No Community, Uplift e Itens Novos
                             * sao zerados tambem aqui para impedir que valores
                             * legados do plano original participem de
                             * consolidacoes usadas pelo Supply Planning.
                             */
                            double baseline = demandPlanningProjectionOriginal.getValorDemandPlanItem(
                                    i, location, material,
                                    Constantes.TipoDemanda.BASELINE, Constantes.TipoPlano.PLANO_IRRESTRITO,
                                    unidadeMedidaPadraoDp);
                            double itensNovos = 0.0;
                            double uplift = 0.0;
                            double ajusteDemanda = demandPlanningProjectionOriginal.getValorDemandPlanItem(
                                    i, location, material,
                                    Constantes.TipoDemanda.AJUSTE_DEMANDA, Constantes.TipoPlano.PLANO_IRRESTRITO,
                                    unidadeMedidaPadraoDp);

                            demandPlanningProjectionOriginalConsolidado.modificaValorDemandPlanItem(
                                    i, locationOrigem, material,
                                    baseline,
                                    Constantes.TipoDemanda.BASELINE,
                                    Constantes.TipoPlano.PLANO_IRRESTRITO,
                                    unidadeMedidaPadraoDp);
                            demandPlanningProjectionOriginalConsolidado.modificaValorDemandPlanItem(
                                    i, locationOrigem, material,
                                    itensNovos,
                                    Constantes.TipoDemanda.ITENS_NOVOS,
                                    Constantes.TipoPlano.PLANO_IRRESTRITO,
                                    unidadeMedidaPadraoDp);
                            demandPlanningProjectionOriginalConsolidado.modificaValorDemandPlanItem(
                                    i, locationOrigem, material,
                                    uplift,
                                    Constantes.TipoDemanda.UPLIFT,
                                    Constantes.TipoPlano.PLANO_IRRESTRITO,
                                    unidadeMedidaPadraoDp);
                            demandPlanningProjectionOriginalConsolidado.modificaValorDemandPlanItem(
                                    i, locationOrigem, material,
                                    ajusteDemanda,
                                    Constantes.TipoDemanda.AJUSTE_DEMANDA,
                                    Constantes.TipoPlano.PLANO_IRRESTRITO,
                                    unidadeMedidaPadraoDp);

                        }
                    }
                });

        return demandPlanningProjectionOriginalConsolidado;

    }

    /**
     * Valida a fotografia de Demand Planning antes de consolidar demanda de
     * clientes em locations operacionais.
     *
     * <p>Esse caminho e compartilhado com Supply Planning e usa mapas internos
     * da projection em paralelo. Por isso a borda falha antes de qualquer
     * stream quando falta algum componente estrutural do snapshot.</p>
     */
    public DemandPlanningProjection getDemandPlanningComConsolidacaoDemandaClientes(
            DemandPlan demandPlan,
            Location location,
            Set<Produto> materiais,
            PerfilExecucaoSupplyPlan.ModoPropagacaoDemanda modoPropagacaoDemanda,
            VersaoMalha versaoMalha,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection parametrosProjection,
            SupplyNetworkProjection supplyNetworkProjection,
            ParametrosDemandPlanProjection parametrosDemandPlanProjection,
            boolean efetuaCatchUpVendas) {

        DemandPlanningProjection demandPlanningProjection = getDemandPlanningProjectionVazio(
                demandPlan,
                location,
                materiais,
                unidadeMedidaProjection,
                parametrosProjection,
                parametrosDemandPlanProjection,
                efetuaCatchUpVendas);

        populaDemandPlanningProjectionComDemandPlan(demandPlanningProjection);

        return geraNovoProjectionComConsolidacaoDemandaClientesDeProjectionOriginal(
                demandPlanningProjection,
                modoPropagacaoDemanda,
                versaoMalha,
                supplyNetworkProjection);

    }


}
