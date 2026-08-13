package com.opsfactor.community.capability.supplyplanning.service;

import com.opsfactor.community.capability.supplyplanning.productionplan.repository.ProductionPlanLinhaDAO;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.DemandaDiretaConsideradaLinhaDAO;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * SQL JDBC exclusivo do contexto H2 usado pelo teste end-to-end histórico de
 * Supply Planning.
 *
 * <p>O runtime Community publica PostgreSQL e seus DAOs não detectam banco por
 * metadata. Esta configuração mantém a única adaptação H2 no classpath de
 * teste, substituindo os beans Community por variantes primárias apenas quando
 * importada explicitamente.</p>
 */
@TestConfiguration(proxyBeanMethods = false)
class CommunitySupplyPlanningH2TestConfiguration {

    @Bean
    @Primary
    DemandaDiretaConsideradaLinhaDAO h2DemandaDiretaConsideradaLinhaDAO() {

        return new H2DemandaDiretaConsideradaLinhaDAO();

    }

    @Bean
    @Primary
    ProductionPlanLinhaDAO h2ProductionPlanLinhaDAO() {

        return new H2ProductionPlanLinhaDAO();

    }

    /**
     * Especialização de teste para o MERGE H2 de demanda direta considerada.
     */
    public static class H2DemandaDiretaConsideradaLinhaDAO extends DemandaDiretaConsideradaLinhaDAO {

        @Override
        protected String getSqlUpsertDemandaDiretaConsideradaLinha() {

            return """
                    MERGE INTO demanda_direta_considerada_linha (
                        data_referencia,
                        quantidade_carteira_original,
                        quantidade_carteira_original_propagada_location_interna,
                        quantidade_demanda_direta_carteira_irrestrita,
                        quantidade_demanda_direta_carteira_restrita,
                        quantidade_demanda_direta_estoque_seguranca,
                        quantidade_demanda_direta_plano_demanda_irrestrita,
                        quantidade_demanda_direta_plano_demanda_restrita,
                        quantidade_plano_demanda_original,
                        quantidade_plano_demanda_original_propagada_location_interna,
                        location_id,
                        material_id,
                        supply_plan_id,
                        unidade_medida_id
                    ) KEY (
                        data_referencia,
                        location_id,
                        material_id,
                        supply_plan_id
                    ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """;

        }

    }

    /**
     * Especialização de teste para o MERGE H2 de Production Plan.
     */
    public static class H2ProductionPlanLinhaDAO extends ProductionPlanLinhaDAO {

        @Override
        protected String getSqlUpsertProductionPlanLinha() {

            return """
                    MERGE INTO production_plan_linha (
                        data_referencia,
                        quantidade_ordem_firme_producao_irrestrita,
                        quantidade_ordem_firme_producao_restrita,
                        quantidade_ordem_firme_producao_trabalho,
                        quantidade_ordem_planejada_producao_irrestrita,
                        quantidade_ordem_planejada_producao_restrita,
                        quantidade_ordem_planejada_producao_trabalho,
                        quantidade_ordem_producao_baseline,
                        quantidade_ordem_producao_baseline_atendida,
                        quantidade_sugestao_producao_baseline,
                        quantidade_sugestao_producao_baseline_atendida,
                        roteiro_id,
                        location_id,
                        lista_tecnica_id,
                        supply_plan_id,
                        versao_producao_id,
                        material_output_id,
                        unidade_medida_id
                    ) KEY (
                        data_referencia,
                        lista_tecnica_id,
                        location_id,
                        roteiro_id,
                        supply_plan_id,
                        versao_producao_id
                    ) VALUES (?,?,?,?,?,?,?,NULL,NULL,NULL,NULL,?,?,?,?,?,?,?)
                    """;

        }

    }

}
