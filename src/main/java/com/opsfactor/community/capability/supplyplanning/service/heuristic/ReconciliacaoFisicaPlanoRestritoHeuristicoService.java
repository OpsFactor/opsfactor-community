package com.opsfactor.community.capability.supplyplanning.service.heuristic;

import com.opsfactor.community.capability.lowlevelcode.engine.LowLevelCode;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjectionFactory;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.supplyplanning.engine.SupplyPlanning;
import com.opsfactor.community.capability.supplyplanning.engine.constrained.ConstrainedPlanningHeuristicoRotinas;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningBiProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Fecha os fluxos restritos depois do nivelamento de capacidade do heurístico.
 *
 * <p>A fotografia central conserva os cortes de produção já decididos. Cada
 * redução de transferência atinge a mesma linha vista como outbound na origem
 * e inbound no destino, sem consultas ao banco dentro dos loops.</p>
 */
@Service
public class ReconciliacaoFisicaPlanoRestritoHeuristicoService {

    private static final double TOLERANCIA_QUANTIDADE = 0.000000001;

    /**
     * Propaga faltas na ordem dos períodos e, dentro de cada período, dos
     * fornecedores para os consumidores da malha. A restrição de insumos é
     * aplicada antes de reduzir expedições e demanda direta.
     */
    public void reconcilia(
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            SupplyNetworkProjection supplyNetworkProjection,
            LowLevelCode lowLevelCode,
            SupplyPlanningBiProjection supplyPlanningBiProjection) {

        Calendario calendario = supplyPlan.getCalendarioDoSupplyPlan(
                supplyNetworkProjection.getClusterEParametrosProjection().getParametrosGlobais());
        int ultimoLowLevelCode = lowLevelCode.getUltimoLowLevelCode().getAsInt();
        MaterialProjection materialProjection = lowLevelCode.getMaterialProjection();

        for (int posicaoPeriodo = calendario.getPosicaoPeriodoPresente();
                posicaoPeriodo <= calendario.getPosicaoPeriodoFinalFuturo();
                posicaoPeriodo++) {

            // O consumo de um componente pode mudar depois do corte do produto
            // acabado. Percorremos novamente até a fotografia parar de mudar.
            int limitePassagens = Math.max(2, ultimoLowLevelCode * 2 + 2);
            boolean alterou;
            int numeroPassagens = 0;
            do {
                numeroPassagens++;
                if (numeroPassagens > limitePassagens) {
                    throw new IllegalStateException("Constrained heuristic physical flows did not converge "
                            + "in period " + posicaoPeriodo + ".");
                }

                double assinaturaAnterior = assinaturaQuantidadesRestritas(supplyPlanningBiProjection);
                limitaProducaoPorInsumos(
                        perfilExecucaoSupplyPlan,
                        supplyNetworkProjection,
                        lowLevelCode,
                        materialProjection,
                        supplyPlanningBiProjection,
                        posicaoPeriodo,
                        ultimoLowLevelCode);
                limitaSaidasEDemanda(
                        supplyNetworkProjection,
                        lowLevelCode,
                        supplyPlanningBiProjection,
                        posicaoPeriodo,
                        ultimoLowLevelCode);
                alterou = Math.abs(assinaturaAnterior
                        - assinaturaQuantidadesRestritas(supplyPlanningBiProjection))
                        > TOLERANCIA_QUANTIDADE;
            } while (alterou);
        }

        validaSaldoFisico(
                supplyNetworkProjection,
                lowLevelCode,
                supplyPlanningBiProjection,
                calendario,
                ultimoLowLevelCode);

    }

    /** Reduz produção dependente quando o estoque de um insumo foi cortado. */
    private void limitaProducaoPorInsumos(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            SupplyNetworkProjection supplyNetworkProjection,
            LowLevelCode lowLevelCode,
            MaterialProjection materialProjection,
            SupplyPlanningBiProjection supplyPlanningBiProjection,
            int posicaoPeriodo,
            int ultimoLowLevelCode) {

        for (int posicaoLowLevelCode = ultimoLowLevelCode;
                posicaoLowLevelCode > 0;
                posicaoLowLevelCode--) {
            for (Location location : lowLevelCode.getLocationsLowLevelCode(posicaoLowLevelCode)) {
                if (!perfilExecucaoSupplyPlan.getConsideraRestricaoProducao(location)) {
                    continue;
                }
                Set<Produto> materiaisOutput = lowLevelCode.getMateriaisLowLevelCodeEmLocation(
                        posicaoLowLevelCode, location);
                if (materiaisOutput.isEmpty()) {
                    continue;
                }
                SupplyPlanningProjection projectionOutput = supplyPlanningBiProjection
                        .getSupplyPlanningProjection(location,
                                MaterialProjectionFactory.getProjectionSetMateriais(
                                        materiaisOutput,
                                        supplyNetworkProjection.getClusterEParametrosProjection()));
                if (projectionOutput.getProductionPlanLinhaOutput(posicaoPeriodo).isEmpty()) {
                    continue;
                }
                Set<Produto> materiaisLocation = materialProjection.getMateriaisAtivosEmLocation(location);
                SupplyPlanningProjection projectionInsumos = supplyPlanningBiProjection
                        .getSupplyPlanningProjection(location,
                                MaterialProjectionFactory.getProjectionSetMateriais(
                                        materiaisLocation,
                                        supplyNetworkProjection.getClusterEParametrosProjection()));
                ConstrainedPlanningHeuristicoRotinas
                        .restringeSugestoesEOrdensProducaoNoProjectionPorDisponibilidadeInsumos(
                                projectionOutput,
                                projectionInsumos,
                                posicaoPeriodo);
            }
        }

    }

    /** Reduz transferências e a demanda direta no nó que ficou sem suprimento. */
    private void limitaSaidasEDemanda(
            SupplyNetworkProjection supplyNetworkProjection,
            LowLevelCode lowLevelCode,
            SupplyPlanningBiProjection supplyPlanningBiProjection,
            int posicaoPeriodo,
            int ultimoLowLevelCode) {

        for (int posicaoLowLevelCode = ultimoLowLevelCode;
                posicaoLowLevelCode > 0;
                posicaoLowLevelCode--) {
            for (Location location : lowLevelCode.getLocationsLowLevelCode(posicaoLowLevelCode)) {
                Set<Produto> materiais = lowLevelCode.getMateriaisLowLevelCodeEmLocation(
                        posicaoLowLevelCode, location);
                if (materiais.isEmpty()) {
                    continue;
                }
                SupplyPlanningProjection projection = supplyPlanningBiProjection
                        .getSupplyPlanningProjection(location,
                                MaterialProjectionFactory.getProjectionSetMateriais(
                                        materiais,
                                        supplyNetworkProjection.getClusterEParametrosProjection()));
                ConstrainedPlanningHeuristicoRotinas.restringeDistributionPlanOutboundEDemandPlan(
                        projection,
                        posicaoPeriodo);
                supplyPlanningBiProjection.sincroniza(projection);
            }
        }

    }

    /** Soma mudanças monótonas para detectar se outra passagem é necessária. */
    private double assinaturaQuantidadesRestritas(SupplyPlanningBiProjection supplyPlanningBiProjection) {

        double producao = supplyPlanningBiProjection.getTodosProductionPlanLinhas().stream()
                .mapToDouble(linha -> linha.getQuantidadeOrdemPlanejadaProducaoRestrita()
                        + linha.getQuantidadeOrdemFirmeProducaoRestrita())
                .sum();
        double distribuicao = supplyPlanningBiProjection.getTodosDistributionPlanItems().stream()
                .mapToDouble(linha -> linha.getQuantidadeOrdemPlanejadaRestrita()
                        + linha.getQuantidadeOrdemFirmeRestrita())
                .sum();
        double atendimento = supplyPlanningBiProjection.getTodasDemandasDiretasConsideradas().stream()
                .mapToDouble(linha -> linha.getQuantidadeDemandaDiretaRestrita())
                .sum();
        return producao + distribuicao + atendimento;

    }

    /** Falha se algum recorte ainda depender de estoque negativo ocultado. */
    private void validaSaldoFisico(
            SupplyNetworkProjection supplyNetworkProjection,
            LowLevelCode lowLevelCode,
            SupplyPlanningBiProjection supplyPlanningBiProjection,
            Calendario calendario,
            int ultimoLowLevelCode) {

        for (int posicaoPeriodo = calendario.getPosicaoPeriodoPresente();
                posicaoPeriodo <= calendario.getPosicaoPeriodoFinalFuturo();
                posicaoPeriodo++) {
            for (int posicaoLowLevelCode = ultimoLowLevelCode;
                    posicaoLowLevelCode > 0;
                    posicaoLowLevelCode--) {
                for (Location location : lowLevelCode.getLocationsLowLevelCode(posicaoLowLevelCode)) {
                    Set<Produto> materiais = lowLevelCode.getMateriaisLowLevelCodeEmLocation(
                            posicaoLowLevelCode, location);
                    if (materiais.isEmpty() || location.getTipoLocation().equals(Location.TipoLocation.FORNECEDOR)) {
                        continue;
                    }
                    SupplyPlanningProjection projection = supplyPlanningBiProjection
                            .getSupplyPlanningProjection(location,
                                    MaterialProjectionFactory.getProjectionSetMateriais(
                                            materiais,
                                            supplyNetworkProjection.getClusterEParametrosProjection()));
                    for (Produto material : materiais) {
                        double saldoFisico = SupplyPlanning.getEstoqueProjetado(
                                projection,
                                posicaoPeriodo - 1,
                                posicaoPeriodo,
                                material,
                                Constantes.TipoPlano.PLANO_RESTRITO,
                                supplyNetworkProjection.getClusterEParametrosProjection()
                                        .getSNPUnidadeMedidaPadrao(material, location),
                                true, true, false, true);
                        if (saldoFisico < -TOLERANCIA_QUANTIDADE) {
                            throw new IllegalStateException("Constrained heuristic plan has negative "
                                    + "physical balance for location " + location.getId()
                                    + ", material " + material.getId()
                                    + ", period " + posicaoPeriodo + ": " + saldoFisico);
                        }
                    }
                }
            }
        }

    }

}
