package com.opsfactor.community.capability.supplyplanning.productionplan.service;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.production.productionresource.projection.BIProjectionCapacidadeProdutiva;
import com.opsfactor.community.capability.supplyplanning.productionplan.repository.CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository;
import com.opsfactor.community.platform.calendar.Calendario;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persiste e consulta o snapshot de capacidade efetiva usado pelo Supply Plan Community.
 *
 * <p>O recorte Community salva somente capacidade produtiva por recurso. Capacidade
 * logistica por location, data ou deposito pertence ao Enterprise porque depende
 * de warehouses, restricoes inbound/outbound/armazenagem e analises avançadas de
 * restricao que nao existem no modulo publico.</p>
 */
@Service
public class CapacidadeEfetivaSupplyPlanService {

    /**
     * Repository do snapshot de capacidade produtiva efetiva por recurso e
     * periodo do Supply Plan.
     */
    @Autowired
    private CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository capacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository;

    /**
     * Remove o snapshot produtivo existente de um Supply Plan antes de
     * recalcular e gravar nova fotografia.
     */
    @Transactional
    public void removeBySupplyPlanId(Long supplyPlanId) {

        validaSupplyPlanIdRemocaoCapacidadeEfetivaCommunity(supplyPlanId);

        capacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository
                .removeBySupplyPlanId(supplyPlanId);

    }

    /**
     * Salva a fotografia de capacidade produtiva efetiva considerada pela
     * rodada heuristica Community.
     */
    @Transactional
    public void salvaCapacidadesEfetivasSupplyPlan(
            SupplyPlan supplyPlan,
            Calendario calendario,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            SupplyNetworkProjection supplyNetworkProjection,
            BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva) {

        validaEntradaPersistenciaCapacidadeEfetivaCommunity(
                supplyPlan,
                calendario,
                perfilExecucaoSupplyPlan,
                supplyNetworkProjection,
                biProjectionCapacidadeProdutiva);

        /*
         * O snapshot antigo so pode ser removido depois da validacao estrutural
         * minima. Assim uma chamada interna com calendario, perfil ou projections
         * corrompidos falha sem apagar a fotografia de capacidade efetiva que
         * ainda pode alimentar relatorios do ultimo Supply Plan valido.
         */
        removeBySupplyPlanId(supplyPlan.getId());

        List<CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan> capacidadesProdutivas =
                montaCapacidadesProdutivas(
                        supplyPlan,
                        calendario,
                        perfilExecucaoSupplyPlan,
                        supplyNetworkProjection,
                        biProjectionCapacidadeProdutiva);

        List<CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan> capacidadesProdutivasSalvas =
                capacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository.saveAll(capacidadesProdutivas);
        validaCapacidadesProdutivasSalvasCommunity(
                capacidadesProdutivasSalvas,
                capacidadesProdutivas.size());

    }

    public Map<RecursoProdutivo, Map<Integer, CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan>>
    getMapaCapacidadeProdutivaPorRecursoPeriodo(SupplyPlan supplyPlan, Calendario calendario) {

        validaEntradaConsultaCapacidadeEfetivaCommunity(
                supplyPlan,
                calendario);

        Map<RecursoProdutivo, Map<Integer, CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan>> mapaCapacidadePorRecursoPeriodo =
                new HashMap<>();
        List<CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan> capacidadesProdutivas =
                capacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository.customFindBySupplyPlan(supplyPlan);
        validaCapacidadesProdutivasCarregadasCommunity(capacidadesProdutivas);

        for (CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan capacidade :
                capacidadesProdutivas) {
            int periodo = calendario.getPosicaoPeriodo(capacidade.getDataReferencia());
            mapaCapacidadePorRecursoPeriodo
                    .computeIfAbsent(capacidade.getRecursoProdutivo(), recursoProdutivo -> new HashMap<>())
                    .put(periodo, capacidade);
        }
        return mapaCapacidadePorRecursoPeriodo;

    }

    private List<CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan> montaCapacidadesProdutivas(
            SupplyPlan supplyPlan,
            Calendario calendario,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            SupplyNetworkProjection supplyNetworkProjection,
            BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva) {

        ParametrosGlobais parametrosGlobais = supplyNetworkProjection
                .getClusterEParametrosProjection()
                .getParametrosGlobais();
        Set<Location> locationsConsideradas =
                perfilExecucaoSupplyPlan.getLocationsConsideradas(supplyNetworkProjection.getClusterEParametrosProjection());
        validaLocationsConsideradasCapacidadeEfetivaCommunity(locationsConsideradas);

        List<CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan> capacidadesProdutivas = new ArrayList<>();

        for (Location location : locationsConsideradas) {
            Set<RecursoProdutivo> recursosProdutivosAtivos =
                    supplyNetworkProjection.getRecursoProdutivoAtivoSet(location);
            validaRecursosProdutivosAtivosCapacidadeEfetivaCommunity(
                    location,
                    recursosProdutivosAtivos);

            for (RecursoProdutivo recursoProdutivo : recursosProdutivosAtivos) {
                PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva =
                        perfilExecucaoSupplyPlan.getTipoCapacidadeProdutiva(location);
                if (tipoCapacidadeProdutiva == null) {
                    throw new IllegalArgumentException(
                            "Production capacity type is required for Community effective capacity snapshot.");
                }
                UnidadeMedida unidadeMedidaCapacidade = tipoCapacidadeProdutiva
                        .equals(PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva.QUANTIDADE_POR_UOM)
                        ? recursoProdutivo.getUnidadeMedidaCapacidadeEmUom(parametrosGlobais)
                        : null;

                for (int periodo = calendario.getPosicaoPeriodoPresente(); periodo <= calendario.getPosicaoPeriodoFinalFuturo(); periodo++) {
                    double capacidadeEfetiva = biProjectionCapacidadeProdutiva.getCapacidadeEmQuantidadeOuHorasEmPosicaoPeriodo(
                            periodo,
                            recursoProdutivo,
                            BIProjectionCapacidadeProdutiva.MasterOrPlanningData.MASTER_DATA);
                    CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan capacidade =
                            new CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan(
                                    new CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan.CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanCompositeKey(
                                            supplyPlan,
                                            recursoProdutivo,
                                            getDataReferenciaPeriodo(calendario, periodo)));
                    capacidade.setTipoCapacidadeProdutiva(tipoCapacidadeProdutiva);
                    capacidade.setCapacidadeEfetiva(capacidadeEfetiva);
                    capacidade.setUnidadeMedidaCapacidade(unidadeMedidaCapacidade);
                    capacidadesProdutivas.add(capacidade);
                }
            }
        }

        return capacidadesProdutivas;

    }

    private LocalDateTime getDataReferenciaPeriodo(Calendario calendario, int periodo) {
        return calendario.getUltimaDataHorarioPeriodo(periodo);
    }

    private void validaEntradaPersistenciaCapacidadeEfetivaCommunity(
            SupplyPlan supplyPlan,
            Calendario calendario,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            SupplyNetworkProjection supplyNetworkProjection,
            BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva) {

        validaSupplyPlanCapacidadeEfetivaCommunity(supplyPlan);
        if (calendario == null) {
            throw new IllegalArgumentException(
                    "Calendar is required to persist Community effective production capacity snapshot.");
        }
        if (perfilExecucaoSupplyPlan == null) {
            throw new IllegalArgumentException(
                    "Supply Planning execution profile is required to persist Community effective production capacity snapshot.");
        }
        if (supplyNetworkProjection == null) {
            throw new IllegalArgumentException(
                    "Supply network projection is required to persist Community effective production capacity snapshot.");
        }
        if (supplyNetworkProjection.getClusterEParametrosProjection() == null) {
            throw new IllegalArgumentException(
                    "Cluster and parameters projection is required to persist Community effective production capacity snapshot.");
        }
        if (supplyNetworkProjection.getClusterEParametrosProjection().getParametrosGlobais() == null) {
            throw new IllegalArgumentException(
                    "Global parameters are required to persist Community effective production capacity snapshot.");
        }
        if (biProjectionCapacidadeProdutiva == null) {
            throw new IllegalArgumentException(
                    "Production capacity BI projection is required to persist Community effective production capacity snapshot.");
        }

    }

    /**
     * Valida a chave destrutiva usada para remover a fotografia anterior.
     */
    private void validaSupplyPlanIdRemocaoCapacidadeEfetivaCommunity(Long supplyPlanId) {

        if (supplyPlanId == null) {
            throw new IllegalArgumentException(
                    "Supply Plan id is required to remove Community effective production capacity snapshot.");
        }

    }

    private void validaEntradaConsultaCapacidadeEfetivaCommunity(
            SupplyPlan supplyPlan,
            Calendario calendario) {

        validaSupplyPlanCapacidadeEfetivaCommunity(supplyPlan);
        if (calendario == null) {
            throw new IllegalArgumentException(
                    "Calendar is required to load Community effective production capacity snapshot.");
        }

    }

    private void validaSupplyPlanCapacidadeEfetivaCommunity(SupplyPlan supplyPlan) {

        if (supplyPlan == null) {
            throw new IllegalArgumentException(
                    "Supply Plan is required for Community effective production capacity snapshot.");
        }
        if (supplyPlan.getId() == null) {
            throw new IllegalArgumentException(
                    "Supply Plan id is required for Community effective production capacity snapshot.");
        }

    }

    private void validaLocationsConsideradasCapacidadeEfetivaCommunity(Collection<Location> locationsConsideradas) {

        if (locationsConsideradas == null) {
            throw new IllegalArgumentException(
                    "Considered locations are required for Community effective production capacity snapshot.");
        }
        int indiceLocation = 0;
        for (Location location : locationsConsideradas) {
            if (location == null) {
                throw new IllegalArgumentException(
                        "Considered location at index "
                                + indiceLocation
                                + " is required for Community effective production capacity snapshot.");
            }
            indiceLocation++;
        }

    }

    private void validaRecursosProdutivosAtivosCapacidadeEfetivaCommunity(
            Location location,
            Collection<RecursoProdutivo> recursosProdutivosAtivos) {

        if (recursosProdutivosAtivos == null) {
            throw new IllegalArgumentException(
                    "Active production resources are required for Community effective production capacity snapshot.");
        }
        int indiceRecursoProdutivo = 0;
        for (RecursoProdutivo recursoProdutivo : recursosProdutivosAtivos) {
            if (recursoProdutivo == null) {
                throw new IllegalArgumentException(
                        "Active production resource at index "
                                + indiceRecursoProdutivo
                                + " for location "
                                + location.getId()
                                + " is required for Community effective production capacity snapshot.");
            }
            indiceRecursoProdutivo++;
        }

    }

    private void validaCapacidadesProdutivasCarregadasCommunity(
            List<CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan> capacidadesProdutivas) {

        if (capacidadesProdutivas == null) {
            throw new IllegalArgumentException(
                    "Loaded effective production capacity snapshot collection is required for Community Supply Planning.");
        }
        int indiceCapacidade = 0;
        for (CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan capacidadeProdutiva : capacidadesProdutivas) {
            validaCapacidadeProdutivaEfetivaCommunity(
                    capacidadeProdutiva,
                    indiceCapacidade,
                    "Loaded");
            indiceCapacidade++;
        }

    }

    /**
     * Valida a fotografia salva de capacidade produtiva efetiva.
     *
     * <p>O snapshot salvo e usado como dado fisico do plano restrito e por
     * consultas posteriores de capacidade. Retorno nulo, parcial ou com item
     * estruturalmente incompleto indica quebra na borda repository/persistencia
     * e deve falhar antes de a rodada seguir como materializada.</p>
     */
    private void validaCapacidadesProdutivasSalvasCommunity(
            List<CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan> capacidadesProdutivas,
            int quantidadeCapacidadesProdutivasEsperada) {

        if (capacidadesProdutivas == null) {
            throw new IllegalArgumentException(
                    "Saved effective production capacity snapshot collection is required for Community Supply Planning.");
        }
        if (capacidadesProdutivas.size() != quantidadeCapacidadesProdutivasEsperada) {
            throw new IllegalArgumentException(
                    "Saved effective production capacity snapshot size "
                            + capacidadesProdutivas.size()
                            + " differs from expected Community effective production capacity snapshot size "
                            + quantidadeCapacidadesProdutivasEsperada
                            + ".");
        }
        int indiceCapacidade = 0;
        for (CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan capacidadeProdutiva : capacidadesProdutivas) {
            validaCapacidadeProdutivaEfetivaCommunity(
                    capacidadeProdutiva,
                    indiceCapacidade,
                    "Saved");
            indiceCapacidade++;
        }

    }

    /**
     * Valida uma linha do snapshot de capacidade efetiva antes de qualquer
     * leitura por getters derivados da chave composta.
     */
    private void validaCapacidadeProdutivaEfetivaCommunity(
            CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan capacidadeProdutiva,
            int indiceCapacidade,
            String origemSnapshot) {

        if (capacidadeProdutiva == null) {
            throw new IllegalArgumentException(
                    origemSnapshot
                            + " effective production capacity snapshot item at index "
                            + indiceCapacidade
                            + " is required for Community Supply Planning.");
        }

        CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan
                .CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanCompositeKey compositeKey =
                capacidadeProdutiva.getCapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanCompositeKey();
        if (compositeKey == null) {
            throw new IllegalArgumentException(
                    origemSnapshot
                            + " effective production capacity snapshot item at index "
                            + indiceCapacidade
                            + " must have a composite key.");
        }
        if (compositeKey.getSupplyPlan() == null || compositeKey.getSupplyPlan().getId() == null) {
            throw new IllegalArgumentException(
                    origemSnapshot
                            + " effective production capacity snapshot item at index "
                            + indiceCapacidade
                            + " must have a Supply Plan id.");
        }
        if (compositeKey.getRecursoProdutivo() == null) {
            throw new IllegalArgumentException(
                    origemSnapshot
                            + " effective production capacity snapshot item at index "
                            + indiceCapacidade
                            + " must have a production resource.");
        }
        if (compositeKey.getRecursoProdutivo().getId() == null
                || compositeKey.getRecursoProdutivo().getId().isBlank()) {
            throw new IllegalArgumentException(
                    origemSnapshot
                            + " effective production capacity snapshot item at index "
                            + indiceCapacidade
                            + " must have a production resource id.");
        }
        if (compositeKey.getDataReferencia() == null) {
            throw new IllegalArgumentException(
                    origemSnapshot
                            + " effective production capacity snapshot item at index "
                            + indiceCapacidade
                            + " must have a reference date.");
        }

    }

}
