package com.opsfactor.community.capability.supplyplanning.supplyplan.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.DemandaDiretaConsideradaLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.DemandaDiretaConsideradaLinhaRepository;
import com.opsfactor.community.platform.calendar.Calendario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;

/**
 * Factory da demanda direta considerada pelo Supply Planning Community.
 *
 * <p>No Community, a demanda direta futura vem apenas do Demand Plan. Sell-in,
 * sales orders, carteira, pedidos de transferencia/compra/producao firmes e
 * reconciliacoes transacionais pertencem ao Enterprise e nao sao carregados
 * por esta factory.</p>
 */
@Service
public class DemandaDiretaConsideradaProjectionFactory {

    /**
     * Repository das linhas de demanda direta considerada ja persistidas para o
     * Supply Plan.
     */
    @Autowired
    private DemandaDiretaConsideradaLinhaRepository demandaDiretaConsideradaLinhaRepository;

    /**
     * Factory de unidades de medida usada para converter quantidades durante a
     * consulta da projection.
     */
    @Autowired
    private UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory;

    /**
     * Carrega a projection completa do Supply Plan.
     */
    public DemandaDiretaConsideradaProjection getDemandaDiretaConsideradaProjectionCompleto(SupplyPlan supplyPlan, Calendario calendario) {

        validaSupplyPlanECalendario(
                supplyPlan,
                calendario,
                "Direct demand considered projection");

        UnidadeMedidaProjection unidadeMedidaProjection = unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache();
        DemandaDiretaConsideradaProjection demandaDiretaConsideradaProjection = new DemandaDiretaConsideradaProjection(supplyPlan, calendario, unidadeMedidaProjection);

        /*
         * A ordenacao deixa deterministica a deduplicacao feita pela projection
         * quando ha linhas antigas do mesmo periodo salvas com horarios
         * diferentes. Antes disso validamos o snapshot retornado pelo
         * repository para que problemas estruturais falhem com mensagem de
         * fronteira, e nao como NPE dentro do Comparator.
         */
        getDemandaDiretaConsideradaLinhaRepositoryResult(
                demandaDiretaConsideradaLinhaRepository.customFindAllBySupplyPlan(supplyPlan),
                "Direct demand considered complete snapshot")
                .stream()
                .sorted(Comparator.comparing(DemandaDiretaConsideradaLinha::getDataReferencia).reversed())
                .forEach(demandaDiretaConsideradaProjection::addDemandPlanRestritoEIrrestritoLinha);

        return demandaDiretaConsideradaProjection;
    }

    /**
     * Carrega a projection restrita a uma location, evitando trafegar linhas de
     * outras locations quando a rotina trabalha em projection local.
     */
    public DemandaDiretaConsideradaProjection getDemandaDiretaConsideradaProjectionParaLocation(SupplyPlan supplyPlan, Calendario calendario, Location location) {

        validaSupplyPlanECalendario(
                supplyPlan,
                calendario,
                "Direct demand considered projection for location");
        validaLocation(
                location,
                "Direct demand considered projection for location");

        UnidadeMedidaProjection unidadeMedidaProjection = unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache();
        DemandaDiretaConsideradaProjection demandaDiretaConsideradaProjection = new DemandaDiretaConsideradaProjection(supplyPlan, calendario, unidadeMedidaProjection);

        /*
         * Mesmo filtrando por location no banco, a projection continua sendo a
         * dona da deduplicacao por material/location/periodo. A factory apenas
         * garante que o snapshot veio materializado e ordenado.
         */
        getDemandaDiretaConsideradaLinhaRepositoryResult(
                demandaDiretaConsideradaLinhaRepository.customFindAllBySupplyPlanAndLocation(supplyPlan, location),
                "Direct demand considered location snapshot")
                .stream()
                .sorted(Comparator.comparing(DemandaDiretaConsideradaLinha::getDataReferencia).reversed())
                .forEach(demandaDiretaConsideradaProjection::addDemandPlanRestritoEIrrestritoLinha);

        return demandaDiretaConsideradaProjection;
    }

    /**
     * Deriva uma projection de location a partir de uma projection completa ja
     * carregada em memoria. Esse caminho evita nova consulta ao banco quando o
     * fluxo ja possui o snapshot completo.
     */
    public DemandaDiretaConsideradaProjection getDemandaDiretaConsideradaProjectionParaLocation(
            SupplyPlan supplyPlan,
            Location location,
            Calendario calendario,
            DemandaDiretaConsideradaProjection demandaDiretaConsideradaProjectionCompleta) {

        validaSupplyPlanECalendario(
                supplyPlan,
                calendario,
                "Direct demand considered projection derived for location");
        validaLocation(
                location,
                "Direct demand considered projection derived for location");
        if (demandaDiretaConsideradaProjectionCompleta == null) {
            throw new IllegalArgumentException(
                    "Direct demand considered projection derived for location requires loaded complete projection.");
        }

        UnidadeMedidaProjection unidadeMedidaProjection = unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache();
        DemandaDiretaConsideradaProjection demandaDiretaConsideradaProjection = new DemandaDiretaConsideradaProjection(supplyPlan, calendario, unidadeMedidaProjection);

        /*
         * Este caminho reaproveita um snapshot completo ja carregado. Ainda
         * assim validamos a colecao retornada pela projection de origem para
         * manter a mesma garantia estrutural do caminho via repository.
         */
        getDemandaDiretaConsideradaLinhaProjectionResult(
                demandaDiretaConsideradaProjectionCompleta.getDemandaDiretaConsideradaLinha(location),
                "Direct demand considered in-memory location snapshot")
                .stream()
                .sorted(Comparator.comparing(DemandaDiretaConsideradaLinha::getDataReferencia).reversed())
                .forEach(demandaDiretaConsideradaProjection::addDemandPlanRestritoEIrrestritoLinha);

        return demandaDiretaConsideradaProjection;
    }

    private void validaSupplyPlanECalendario(
            SupplyPlan supplyPlan,
            Calendario calendario,
            String projectionDescription) {

        if (supplyPlan == null || calendario == null) {
            throw new IllegalArgumentException(
                    projectionDescription
                            + " requires supply plan and calendar.");
        }

    }

    private void validaLocation(
            Location location,
            String projectionDescription) {

        if (location == null) {
            throw new IllegalArgumentException(
                    projectionDescription
                            + " requires location.");
        }

    }

    private Collection<DemandaDiretaConsideradaLinha> getDemandaDiretaConsideradaLinhaRepositoryResult(
            Collection<DemandaDiretaConsideradaLinha> demandaDiretaConsideradaLinhaCollection,
            String projectionDescription) {

        return validaDemandaDiretaConsideradaLinhaCollection(
                demandaDiretaConsideradaLinhaCollection,
                projectionDescription,
                "repository returned");

    }

    private Collection<DemandaDiretaConsideradaLinha> getDemandaDiretaConsideradaLinhaProjectionResult(
            Collection<DemandaDiretaConsideradaLinha> demandaDiretaConsideradaLinhaCollection,
            String projectionDescription) {

        return validaDemandaDiretaConsideradaLinhaCollection(
                demandaDiretaConsideradaLinhaCollection,
                projectionDescription,
                "source projection returned");

    }

    /**
     * Valida uma fotografia de demanda direta antes da ordenacao e indexacao em
     * memoria.
     *
     * <p>Colecao vazia e ausencia operacional valida de demanda direta
     * persistida. Colecao nula, item nulo ou chave funcional incompleta indicam
     * snapshot quebrado: a projection depende de supply plan, location,
     * material e data de referencia para deduplicar por periodo.</p>
     */
    private Collection<DemandaDiretaConsideradaLinha> validaDemandaDiretaConsideradaLinhaCollection(
            Collection<DemandaDiretaConsideradaLinha> demandaDiretaConsideradaLinhaCollection,
            String projectionDescription,
            String sourceDescription) {

        if (demandaDiretaConsideradaLinhaCollection == null) {
            throw new IllegalStateException(
                    projectionDescription
                            + " "
                            + sourceDescription
                            + " null collection.");
        }

        demandaDiretaConsideradaLinhaCollection.forEach(demandaDiretaConsideradaLinha -> {
            if (demandaDiretaConsideradaLinha == null) {
                throw new IllegalStateException(
                        projectionDescription
                                + " "
                                + sourceDescription
                                + " null line.");
            }
            if (demandaDiretaConsideradaLinha.getDemandaDiretaConsideradaLinhaCompositeKey() == null
                    || demandaDiretaConsideradaLinha.getSupplyPlan() == null
                    || demandaDiretaConsideradaLinha.getLocation() == null
                    || demandaDiretaConsideradaLinha.getMaterial() == null
                    || demandaDiretaConsideradaLinha.getDataReferencia() == null) {
                throw new IllegalStateException(
                        projectionDescription
                                + " "
                                + sourceDescription
                                + " line without supply plan, location, material or reference date.");
            }
        });

        return demandaDiretaConsideradaLinhaCollection;

    }

}
