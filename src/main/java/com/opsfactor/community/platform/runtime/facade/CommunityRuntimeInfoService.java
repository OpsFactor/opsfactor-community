package com.opsfactor.community.platform.runtime.facade;

import com.opsfactor.community.capability.demandplanning.forecast.configuration.DemandPlanningModelCatalog;
import com.opsfactor.community.capability.demandplanning.planningbook.domain.DemandPlanningPlanningBookCatalog;
import com.opsfactor.community.capability.supplyplanning.service.SupplyPlanningExecutionModelCatalog;
import com.opsfactor.community.capability.supplyplanning.planningbook.domain.SupplyPlanningPlanningBookCatalog;
import com.opsfactor.community.platform.runtime.facade.dto.RuntimeInfoDTO;
import org.springframework.stereotype.Service;

/**
 * Implementacao padrao do runtime info quando somente os artefatos Community
 * estao presentes no classpath.
 *
 * <p>Esta classe nao deve ser `@Primary`: a prioridade pertence ao overlay
 * Enterprise quando ele existir. O logo superior do Community permanece
 * `OpsFactor`; customizacao de logo por cliente e recurso Enterprise.</p>
 */
@Service
public class CommunityRuntimeInfoService implements RuntimeInfoService {

    /**
     * Publica apenas os valores que possuem runtime Community real.
     *
     * <p>Modelos estatisticos, split de forecast, documento historico e KFs do
     * Planning Book vem dos catalogos Community para manter runtime info,
     * OpenAPI e validacoes de service alinhados.</p>
     */
    @Override
    public RuntimeInfoDTO getRuntimeInfo() {

        return new RuntimeInfoDTO(
                "community",
                DemandPlanningModelCatalog.getDpModelosEstatisticosOpenApiCommunity(),
                RuntimeInfoDTO.buildRuntimeInfoOptionList(
                        DemandPlanningModelCatalog.getDpModelosEstatisticosOpenApiRuntimeOptions(),
                        DemandPlanningModelCatalog.getDpModelosEstatisticosOpenApiCommunity(),
                        DemandPlanningModelCatalog.getDpModelosEstatisticosOpenApiCommunity()),
                DemandPlanningModelCatalog.getDpModelosSplitOpenApiCommunity(),
                RuntimeInfoDTO.buildRuntimeInfoOptionList(
                        DemandPlanningModelCatalog.getDpModelosSplitOpenApiRuntimeOptions(),
                        DemandPlanningModelCatalog.getDpModelosSplitOpenApiCommunity(),
                        DemandPlanningModelCatalog.getDpModelosSplitOpenApiCommunity()),
                DemandPlanningModelCatalog.getDpModelosTratamentoStockoutOpenApiCommunity(),
                RuntimeInfoDTO.buildRuntimeInfoOptionList(
                        DemandPlanningModelCatalog.getDpModelosTratamentoStockoutOpenApiRuntimeOptions(),
                        DemandPlanningModelCatalog.getDpModelosTratamentoStockoutOpenApiCommunity(),
                        DemandPlanningModelCatalog.getDpModelosTratamentoStockoutOpenApiCommunity()),
                DemandPlanningModelCatalog.getDpModelosLimpezaHistoricoOpenApiCommunity(),
                RuntimeInfoDTO.buildRuntimeInfoOptionList(
                        DemandPlanningModelCatalog.getDpModelosLimpezaHistoricoOpenApiRuntimeOptions(),
                        DemandPlanningModelCatalog.getDpModelosLimpezaHistoricoOpenApiCommunity(),
                        DemandPlanningModelCatalog.getDpModelosLimpezaHistoricoOpenApiCommunity()),
                DemandPlanningModelCatalog.getDpModelosUpliftOpenApiCommunity(),
                RuntimeInfoDTO.buildRuntimeInfoOptionList(
                        DemandPlanningModelCatalog.getDpModelosUpliftOpenApiRuntimeOptions(),
                        DemandPlanningModelCatalog.getDpModelosUpliftOpenApiCommunity(),
                        DemandPlanningModelCatalog.getDpModelosUpliftOpenApiCommunity()),
                DemandPlanningModelCatalog.getTiposDocumentoHistoricoOpenApiCommunity(),
                RuntimeInfoDTO.buildRuntimeInfoOptionList(
                        DemandPlanningModelCatalog.getTiposDocumentoHistoricoOpenApiRuntimeOptions(),
                        DemandPlanningModelCatalog.getTiposDocumentoHistoricoOpenApiCommunity(),
                        DemandPlanningModelCatalog.getTiposDocumentoHistoricoOpenApiCommunity()),
                SupplyPlanningExecutionModelCatalog.getModosExecucaoSupplyPlanOpenApiCommunity(),
                RuntimeInfoDTO.buildRuntimeInfoOptionList(
                        SupplyPlanningExecutionModelCatalog.getModosExecucaoSupplyPlanOpenApiRuntimeOptions(),
                        SupplyPlanningExecutionModelCatalog.getModosExecucaoSupplyPlanOpenApiCommunity(),
                SupplyPlanningExecutionModelCatalog.getModosExecucaoSupplyPlanOpenApiCommunity()),
                DemandPlanningPlanningBookCatalog.getKeyFiguresVisiveisDemandPlanningBookCommunity(),
                DemandPlanningPlanningBookCatalog.getKeyFiguresSelecionaveisDemandPlanningBookCommunity(),
                DemandPlanningPlanningBookCatalog.getKeyFiguresEditaveisDemandPlanningBookCommunity(),
                SupplyPlanningPlanningBookCatalog.getKeyFiguresVisiveisSupplyPlanningBookCommunity(),
                SupplyPlanningPlanningBookCatalog.getKeyFiguresSelecionaveisSupplyPlanningBookCommunity(),
                SupplyPlanningPlanningBookCatalog.getKeyFiguresEditaveisSupplyPlanningBookCommunity());

    }

}
