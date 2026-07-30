package com.opsfactor.community.platform.bi.facade;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.supplyplanning.distributionplan.repository.DistributionPlanItemRepository;
import com.opsfactor.community.platform.bi.facade.dto.CommunityMaterialFlowsDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityMaterialFlowsLocationAndColorDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Constrói a matriz agregada de transferências do Supply Plan para a
 * visualização Community de material flows.
 *
 * <p>A leitura é intencionalmente limitada ao snapshot persistido de
 * {@link DistributionPlanItem}. Não há conversão de unidade, preço,
 * histórico nem dados Enterprise: o gráfico representa a soma física das
 * ordens planejadas e firmes irrestritas por par origem/destino.</p>
 */
@Service
public class CommunityMaterialFlowsService {

    /** Sequência visual histórica; locations além da paleta usam cinza. */
    private static final String[] COLOR_SEQUENCE = {
            "#b2182b", "#d6604d", "#f4a582", "#fddbc7", "#f7f7f7", "#d1e5f0",
            "#92c5de", "#4393c3", "#2166ac", "#053061", "#CCCCCC"
    };

    private static final String FALLBACK_COLOR = "#CCCCCC";

    /** Lê em lote as linhas persistidas do Supply Plan selecionado. */
    @Autowired
    private DistributionPlanItemRepository distributionPlanItemRepository;

    /**
     * Carrega todas as transferências de um plano e publica uma matriz
     * origem→destino alinhada à lista de locations retornada.
     */
    public CommunityMaterialFlowsDTO getMaterialFlows(Long supplyPlanId) {

        Collection<DistributionPlanItem> distributionPlanItems =
                distributionPlanItemRepository.customFindBySupplyPlanId(supplyPlanId);
        Map<Location, Double> totalOutboundFlowByLocation = new HashMap<>();
        Map<Location, Map<Location, Double>> flowByOriginAndDestination = new HashMap<>();
        Set<Location> locations = new HashSet<>();

        for (DistributionPlanItem distributionPlanItem : distributionPlanItems) {
            Location originLocation = distributionPlanItem.getLocationOrigem();
            Location destinationLocation = distributionPlanItem.getLocationDestino();
            double unconstrainedFlow = distributionPlanItem.getQuantidadeOrdemPlanejadaIrrestrita()
                    + distributionPlanItem.getQuantidadeOrdemFirmeIrrestrita();

            /*
             * Mesmo uma linha de quantidade zero participa da matriz. Isso
             * preserva nós da malha que o usuário espera visualizar, ainda
             * que não tenham recebido volume nesta execução.
             */
            locations.add(originLocation);
            locations.add(destinationLocation);
            totalOutboundFlowByLocation.merge(originLocation, unconstrainedFlow, Double::sum);
            flowByOriginAndDestination
                    .computeIfAbsent(originLocation, ignored -> new HashMap<>())
                    .merge(destinationLocation, unconstrainedFlow, Double::sum);
        }

        List<Location> orderedLocations = locations.stream()
                .sorted(Comparator
                        .comparingDouble((Location location) ->
                                totalOutboundFlowByLocation.getOrDefault(location, 0.0d))
                        .reversed()
                        /* O legado ordenava pelo outbound; o desempate por ID
                         * torna o JSON repetível sem mudar essa prioridade. */
                        .thenComparing(Location::getId))
                .toList();

        CommunityMaterialFlowsDTO materialFlows = new CommunityMaterialFlowsDTO();
        addLocationsAndColors(materialFlows, orderedLocations);
        addFlowMatrix(materialFlows, orderedLocations, flowByOriginAndDestination);
        return materialFlows;

    }

    /** Mantém as posições de nodes e cores alinhadas às linhas/colunas. */
    private void addLocationsAndColors(
            CommunityMaterialFlowsDTO materialFlows,
            List<Location> orderedLocations) {

        for (int position = 0; position < orderedLocations.size(); position++) {
            Location location = orderedLocations.get(position);
            String color = position < COLOR_SEQUENCE.length
                    ? COLOR_SEQUENCE[position]
                    : FALLBACK_COLOR;
            materialFlows.locationAndColorList.add(
                    new CommunityMaterialFlowsLocationAndColorDTO(location.getId(), color));
        }

    }

    /**
     * Produz uma matriz quadrada, com zero explícito para pares sem
     * transferência no plano selecionado.
     */
    private void addFlowMatrix(
            CommunityMaterialFlowsDTO materialFlows,
            List<Location> orderedLocations,
            Map<Location, Map<Location, Double>> flowByOriginAndDestination) {

        for (Location originLocation : orderedLocations) {
            List<Double> flowRow = new ArrayList<>();
            Map<Location, Double> flowByDestination = flowByOriginAndDestination.get(originLocation);
            for (Location destinationLocation : orderedLocations) {
                flowRow.add(flowByDestination == null
                        ? 0.0d
                        : flowByDestination.getOrDefault(destinationLocation, 0.0d));
            }
            materialFlows.flowData.add(flowRow);
        }

    }
}
