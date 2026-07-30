package com.opsfactor.community.capability.supplyplanning.service.spi;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanningProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanProjectionFactory;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Implementacao base do recorte de catch-up de demanda.
 *
 * <p>O Community continua construindo somente a projection normal de Demand
 * Plan. Uma flag persistida por instalacao Enterprise, sem o bean privado no
 * classpath, falha antes de iniciar o calculo e nao pode ativar vendas
 * transacionais por acidente.</p>
 */
@Component
public class CommunitySupplyPlanDemandCatchUpProjectionSpi
        implements SupplyPlanDemandCatchUpProjectionSpi {

    /** Monta a projection de Demand Plan usada pelo SPI Community. */
    @Autowired
    private DemandPlanProjectionFactory demandPlanProjectionFactory;

    @Override
    public DemandPlanningProjection getDemandPlanningProjectionParaDemandaDireta(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            DemandPlan demandPlan,
            Set<Location> locations,
            Set<Produto> materiais) {

        validaCatchUpDesabilitadoNoCommunity(perfilExecucaoSupplyPlan);
        DemandPlanningProjection demandPlanningProjection =
                demandPlanProjectionFactory.getDemandPlanningProjectionVazio(
                        demandPlan,
                        locations,
                        materiais,
                        false);
        demandPlanProjectionFactory.populaDemandPlanningProjectionComDemandPlan(
                demandPlanningProjection);
        return demandPlanningProjection;

    }

    @Override
    public DemandPlanningProjection getDemandPlanningProjectionCompletaParaEstoqueInicial(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            DemandPlan demandPlan,
            Location location,
            Set<Produto> materiais) {

        validaCatchUpDesabilitadoNoCommunity(perfilExecucaoSupplyPlan);
        return demandPlanProjectionFactory.getDemandPlanningProjectionCompleto(
                demandPlan,
                location,
                materiais,
                false);

    }

    /**
     * Impede que um valor Enterprise persistido seja interpretado como feature
     * Community apenas porque compartilha a mesma linha JPA.
     */
    protected void validaCatchUpDesabilitadoNoCommunity(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) {

        if (perfilExecucaoSupplyPlan != null
                && perfilExecucaoSupplyPlan.getEnableDemandCatchUpFromPastSellout()) {
            throw new RequiresEnterpriseVersionException("Supply Planning demand catch-up");
        }

    }

}
