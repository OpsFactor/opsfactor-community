package com.opsfactor.community.capability.supplyplanning.service;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjectionFactory;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjection;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjectionFactory;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjectionFactory;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanningProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.capability.masterdata.calendar.temporalsplit.projection.SplitTemporalProjectionPorDfu;
import com.opsfactor.community.capability.masterdata.calendar.temporalsplit.projection.SplitTemporalProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.configuration.repository.PerfilExecucaoSupplyPlanRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.VersaoMalhaRepository;
import com.opsfactor.community.capability.demandplanning.demandplan.repository.DemandPlanRepository;
import com.opsfactor.community.capability.lowlevelcode.engine.LowLevelCode;
import com.opsfactor.community.capability.supplyplanning.engine.SupplyPlanning;
import com.opsfactor.community.capability.supplyplanning.service.heuristic.ConstrainedPlanService;
import com.opsfactor.community.bootstrap.CommunityWebApplication;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Teste de integracao historico do fluxo heuristico de Supply Planning.
 *
 * <p>Este teste preserva uma massa operacional ampla para validar projection,
 * malha, split temporal, demanda e persistencia de plano em conjunto. Os testes
 * de contrato Community menores cobrem os bloqueios Enterprise; aqui mantemos
 * o cenario end-to-end legado funcionando dentro do recorte heuristico.</p>
 **/
//@RunWith(SpringRunner.class)
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS) // re-cria o banco de dados + spring context após a execução desses testes (evita interferência dos dados com testes futuros)
@SpringBootTest(classes = CommunityWebApplication.class)
@Import(CommunitySupplyPlanningH2TestConfiguration.class)
public class SupplyPlanServiceTest {

    /** Service principal de Supply Planning exercitado pelo teste end-to-end. */
    @Autowired
    private SupplyPlanService supplyPlanService;

    /** Service do plano restrito heuristico Community. */
    @Autowired
    private ConstrainedPlanService constrainedPlanService;

    /** Factory da malha operacional usada pela projection de Supply. */
    @Autowired
    private SupplyNetworkProjectionFactory supplyNetworkProjectionFactory;

    /** Factory de parametros e clusters base para DFUs e Supply Planning. */
    @Autowired
    private ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory;

    /** Factory de conversoes entre unidades de medida. */
    @Autowired
    private UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory;

    /** Repository usado para persistir a versao de malha da massa do teste. */
    @Autowired
    private VersaoMalhaRepository versaoMalhaRepository;

    /** Factory de projection de Demand Plan usada como fonte de demanda. */
    @Autowired
    private DemandPlanProjectionFactory demandPlanProjectionFactory;

    /** Factory de projection de Supply Plan validada no fluxo completo. */
    @Autowired
    private SupplyPlanProjectionFactory supplyPlanProjectionFactory;

    /** Repository usado para materializar o Demand Plan da massa do teste. */
    @Autowired
    private DemandPlanRepository demandPlanRepository;

    /** Factory de split temporal por DFU usado pelo heuristico Community. */
    @Autowired
    private SplitTemporalProjectionFactory splitTemporalProjectionFactory;

    /** Factory da politica operacional de estoques usada pelo plano. */
    @Autowired
    private PoliticaEstoquesProjectionFactory politicaEstoquesProjectionFactory;

    /**
     * Repository do perfil de Supply Planning usado para montar snapshots
     * temporarios de teste com o mesmo contrato das factories produtivas.
     */
    @Autowired
    private PerfilExecucaoSupplyPlanRepository perfilExecucaoSupplyPlanRepository;

    /** Acesso SQL da massa de teste local; nao representa caminho funcional produtivo. */
    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeAll
    @Transactional
    public void setUpDados() {
        // ATUALIZAÇÃO PRODUTOS
        jdbcTemplate.update(
            "INSERT INTO produto (id, descricao) VALUES ('FG100','Finished Good 100');\n" +
            "INSERT INTO produto (id, descricao) VALUES ('FG101','Finished Good 101');\n" +
            "INSERT INTO produto (id, descricao) VALUES ('FG102','Finished Good 102');\n" +
            "INSERT INTO produto (id, descricao) VALUES ('FG103','Finished Good 103');\n" +
            "INSERT INTO produto (id, descricao) VALUES ('WP200','Work in Process 200');\n" +
            "INSERT INTO produto (id, descricao) VALUES ('WP201','Work in Process 201');\n" +
            "INSERT INTO produto (id, descricao) VALUES ('WP202','Work in Process 202');\n" +
            "INSERT INTO produto (id, descricao) VALUES ('RM300','Raw Material 300');\n" +
            "INSERT INTO produto (id, descricao) VALUES ('RM301','Raw Material 301');\n" +
            "INSERT INTO produto (id, descricao) VALUES ('RM302','Raw Material 302');\n" +
            "INSERT INTO produto (id, descricao) VALUES ('RM303','Raw Material 303');");
        
        // ATUALIZAÇÃO CLUSTERS LOCATIONS
        jdbcTemplate.update(
            /*
             * LocationService cria um cluster default no bootstrap Community.
             * A massa do teste usa uma faixa propria para nao depender do id
             * gerado pelo bootstrap e para manter explicita a topologia usada
             * nos cenarios de supply.
             */
            "INSERT INTO cluster_locations (id, descricao) VALUES ('101','Store Cluster');\n" +
            "INSERT INTO cluster_locations (id, descricao) VALUES ('102','DCs Cluster');\n" +
            "INSERT INTO cluster_locations (id, descricao) VALUES ('103','Factory Cluster');\n" +
            "INSERT INTO cluster_locations (id, descricao) VALUES ('104','Supplier Cluster');");
        
        // ATUALIZAÇÃO LOCATIONS
        jdbcTemplate.update(
            /*
             * Location nao possui FK direta para cluster no modelo Community.
             * A classificacao por cluster acontece via regras/projections; para
             * este teste de supply, basta materializar as locations da malha.
             */
            "INSERT INTO location (id, descricao) VALUES ('STO01','Store 01');\n" +
            "INSERT INTO location (id, descricao) VALUES ('STO02','Store 02');\n" +
            "INSERT INTO location (id, descricao) VALUES ('STO03','Store 03');\n" +
            "INSERT INTO location (id, descricao) VALUES ('STO04','Store 04');\n" +
            "INSERT INTO location (id, descricao) VALUES ('DC01','DC 01');\n" +
            "INSERT INTO location (id, descricao) VALUES ('DC02','DC 02');\n" +
            "INSERT INTO location (id, descricao) VALUES ('DC03','DC 03');\n" +
            "INSERT INTO location (id, descricao) VALUES ('FAC01','Factory 01');\n" +
            "INSERT INTO location (id, descricao) VALUES ('FAC02','Factory 02');\n" +
            "INSERT INTO location (id, descricao) VALUES ('FAC03','Factory 03');\n" +
            "INSERT INTO location (id, descricao) VALUES ('SUP01','Supplier 01');");
        
        // ATUALIZAÇÃO DAS VERSOES DE MALHA E LINHAS TRANSPORTE
        jdbcTemplate.update(
            "INSERT INTO versao_malha (id, descricao) VALUES ('VERSAO_MALHA_01','Versão Malha 01')");     
        
        jdbcTemplate.update(
            "INSERT INTO linha_transporte (versao_malha_id, location_origem_id, location_destino_id,lead_time_dias,prioridade) VALUES ('VERSAO_MALHA_01', 'DC01','STO01','1','1');\n" +
            "INSERT INTO linha_transporte (versao_malha_id, location_origem_id, location_destino_id,lead_time_dias,prioridade) VALUES ('VERSAO_MALHA_01', 'DC01','STO02','2','1');\n" +
            "INSERT INTO linha_transporte (versao_malha_id, location_origem_id, location_destino_id,lead_time_dias,prioridade) VALUES ('VERSAO_MALHA_01', 'DC01','STO03','2','1');\n" +
            "INSERT INTO linha_transporte (versao_malha_id, location_origem_id, location_destino_id,lead_time_dias,prioridade) VALUES ('VERSAO_MALHA_01', 'DC01','STO04','1','1');\n" +
            "INSERT INTO linha_transporte (versao_malha_id, location_origem_id, location_destino_id,lead_time_dias,prioridade) VALUES ('VERSAO_MALHA_01', 'DC02','STO01','2','1');\n" +
            "INSERT INTO linha_transporte (versao_malha_id, location_origem_id, location_destino_id,lead_time_dias,prioridade) VALUES ('VERSAO_MALHA_01', 'DC02','STO02','3','1');\n" +
            "INSERT INTO linha_transporte (versao_malha_id, location_origem_id, location_destino_id,lead_time_dias,prioridade) VALUES ('VERSAO_MALHA_01', 'DC02','STO03','3','1');\n" +
            "INSERT INTO linha_transporte (versao_malha_id, location_origem_id, location_destino_id,lead_time_dias,prioridade) VALUES ('VERSAO_MALHA_01', 'DC02','STO04','7','2');\n" +
            "INSERT INTO linha_transporte (versao_malha_id, location_origem_id, location_destino_id,lead_time_dias,prioridade) VALUES ('VERSAO_MALHA_01', 'DC03','STO04','1','1');\n" +
            "INSERT INTO linha_transporte (versao_malha_id, location_origem_id, location_destino_id,lead_time_dias,prioridade) VALUES ('VERSAO_MALHA_01', 'FAC01','DC01','4','1');\n" +
            "INSERT INTO linha_transporte (versao_malha_id, location_origem_id, location_destino_id,lead_time_dias,prioridade) VALUES ('VERSAO_MALHA_01', 'FAC02','DC02','10','1');\n" +
            "INSERT INTO linha_transporte (versao_malha_id, location_origem_id, location_destino_id,lead_time_dias,prioridade) VALUES ('VERSAO_MALHA_01', 'FAC03','DC03','7','1');");
        
        // ATUALIZAÇÃO DO DEMAND PLAN
        jdbcTemplate.update(
            /*
             * Split temporal e projections de Demand Plan dependem do perfil
             * de execucao. Parametros por cluster nao sao obrigatorios aqui:
             * a factory Community materializa defaults quando nao ha linhas
             * especificas cadastradas para o perfil.
             */
            "INSERT INTO perfil_execucao_demand_plan (id, descricao,tamanho_bucket,tipo_documento_venda) VALUES ('Perfil DP Teste','Perfil DP Teste','2','0');\n" +
            "INSERT INTO demand_plan (id, descricao,data_inicio_plano,tamanho_bucket,perfil_execucao_demand_plan_id) VALUES ('1','Demand Plan 01','2020-1-1','2','Perfil DP Teste');");
        jdbcTemplate.update(
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO01','FG100','2020-1-31','5');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO01','FG101','2020-1-31','5');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO01','FG102','2020-1-31','5');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO01','FG103','2020-1-31','5');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO01','FG100','2020-2-29','5');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO01','FG101','2020-2-29','5');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO01','FG102','2020-2-29','5');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO01','FG103','2020-2-29','5');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO01','FG100','2020-3-31','5');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO01','FG101','2020-3-31','5');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO01','FG102','2020-3-31','5');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO01','FG103','2020-3-31','5');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO02','FG100','2020-1-31','10');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO02','FG101','2020-1-31','10');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO02','FG102','2020-1-31','10');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO02','FG103','2020-1-31','10');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO02','FG100','2020-2-29','10');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO02','FG101','2020-2-29','10');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO02','FG102','2020-2-29','10');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO02','FG103','2020-2-29','10');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO02','FG100','2020-3-31','10');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO02','FG101','2020-3-31','10');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO02','FG102','2020-3-31','10');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO02','FG103','2020-3-31','10');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO03','FG100','2020-1-31','15');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO03','FG101','2020-1-31','15');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO03','FG102','2020-1-31','15');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO03','FG103','2020-1-31','15');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO03','FG100','2020-2-29','15');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO03','FG101','2020-2-29','15');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO03','FG102','2020-2-29','15');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO03','FG103','2020-2-29','15');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO03','FG100','2020-3-31','15');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO03','FG101','2020-3-31','15');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO03','FG102','2020-3-31','15');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO03','FG103','2020-3-31','15');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO04','FG100','2020-1-31','20');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO04','FG101','2020-1-31','20');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO04','FG102','2020-1-31','20');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO04','FG103','2020-1-31','20');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO04','FG100','2020-2-29','20');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO04','FG101','2020-2-29','20');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO04','FG102','2020-2-29','20');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO04','FG103','2020-2-29','20');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO04','FG100','2020-3-31','20');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO04','FG101','2020-3-31','20');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO04','FG102','2020-3-31','20');\n" +
            "INSERT INTO demand_plan_item (demand_plan_id,location_id, produto_id,data_referencia,quantidade_baseline) VALUES ('1','STO04','FG103','2020-3-31','20');");
        
        // ATUALIZAÇÃO DO ESTOQUE
        jdbcTemplate.update(
            "INSERT INTO estoque (location_id,produto_id, data_referencia,quantidade) VALUES ('STO01','FG100','2019-12-31','3');\n" +
            "INSERT INTO estoque (location_id,produto_id, data_referencia,quantidade) VALUES ('STO01','FG101','2019-12-31','3');\n" +
            "INSERT INTO estoque (location_id,produto_id, data_referencia,quantidade) VALUES ('STO01','FG102','2019-12-31','3');\n" +
            "INSERT INTO estoque (location_id,produto_id, data_referencia,quantidade) VALUES ('STO01','FG103','2019-12-31','3');\n" +
            "INSERT INTO estoque (location_id,produto_id, data_referencia,quantidade) VALUES ('STO02','FG100','2019-12-31','7');\n" +
            "INSERT INTO estoque (location_id,produto_id, data_referencia,quantidade) VALUES ('STO02','FG101','2019-12-31','7');\n" +
            "INSERT INTO estoque (location_id,produto_id, data_referencia,quantidade) VALUES ('STO02','FG102','2019-12-31','7');\n" +
            "INSERT INTO estoque (location_id,produto_id, data_referencia,quantidade) VALUES ('STO02','FG103','2019-12-31','7');\n" +
            "INSERT INTO estoque (location_id,produto_id, data_referencia,quantidade) VALUES ('STO03','FG100','2019-12-31','9');\n" +
            "INSERT INTO estoque (location_id,produto_id, data_referencia,quantidade) VALUES ('STO03','FG101','2019-12-31','9');\n" +
            "INSERT INTO estoque (location_id,produto_id, data_referencia,quantidade) VALUES ('STO03','FG102','2019-12-31','9');\n" +
            "INSERT INTO estoque (location_id,produto_id, data_referencia,quantidade) VALUES ('STO03','FG103','2019-12-31','9');\n" +
            "INSERT INTO estoque (location_id,produto_id, data_referencia,quantidade) VALUES ('STO04','FG100','2019-12-31','12');\n" +
            "INSERT INTO estoque (location_id,produto_id, data_referencia,quantidade) VALUES ('STO04','FG101','2019-12-31','12');\n" +
            "INSERT INTO estoque (location_id,produto_id, data_referencia,quantidade) VALUES ('STO04','FG102','2019-12-31','12');\n" +
            "INSERT INTO estoque (location_id,produto_id, data_referencia,quantidade) VALUES ('STO04','FG103','2019-12-31','12');\n" +
            "INSERT INTO estoque (location_id,produto_id, data_referencia,quantidade) VALUES ('DC01','FG100','2019-12-31','4');\n" +
            "INSERT INTO estoque (location_id,produto_id, data_referencia,quantidade) VALUES ('DC01','FG101','2019-12-31','4');\n" +
            "INSERT INTO estoque (location_id,produto_id, data_referencia,quantidade) VALUES ('DC02','FG102','2019-12-31','5');\n" +
            "INSERT INTO estoque (location_id,produto_id, data_referencia,quantidade) VALUES ('DC02','FG103','2019-12-31','5');\n" +
            "INSERT INTO estoque (location_id,produto_id, data_referencia,quantidade) VALUES ('DC03','FG100','2019-12-31','6');\n" +
            "INSERT INTO estoque (location_id,produto_id, data_referencia,quantidade) VALUES ('DC03','FG101','2019-12-31','6');");
                
        // ATUALIZAÇÃO DAS LISTAS TÉCNICAS
        jdbcTemplate.update(
            "INSERT INTO lista_tecnica (id, tipo_lista_tecnica, material_output_id,descricao, location_id, quantidade) VALUES ('LT100-FAC01','simples','FG100','LT FG100','FAC01',1);\n" +
            "INSERT INTO lista_tecnica (id, tipo_lista_tecnica, material_output_id,descricao, location_id, quantidade) VALUES ('LT100-FAC03','simples','FG100','LT FG100','FAC03',1);\n" +
            "INSERT INTO lista_tecnica (id, tipo_lista_tecnica, material_output_id,descricao, location_id, quantidade) VALUES ('LT101-FAC01','simples','FG101','LT FG101','FAC01',1);\n" +
            "INSERT INTO lista_tecnica (id, tipo_lista_tecnica, material_output_id,descricao, location_id, quantidade) VALUES ('LT101-FAC03','simples','FG101','LT FG101','FAC03',1);\n" +
            "INSERT INTO lista_tecnica (id, tipo_lista_tecnica, material_output_id,descricao, location_id, quantidade) VALUES ('LT102-FAC02','simples','FG102','LT FG102','FAC02',1);\n" +
            "INSERT INTO lista_tecnica (id, tipo_lista_tecnica, material_output_id,descricao, location_id, quantidade) VALUES ('LT103-FAC02','simples','FG103','LT FG103','FAC02',1);\n" +
//            "INSERT INTO lista_tecnica (id, material_output_id,descricao) VALUES ('LT200','WP200','LT WP200');\n" +
            "INSERT INTO lista_tecnica (id, tipo_lista_tecnica, material_output_id,descricao, location_id, quantidade) VALUES ('LT201-FAC02','simples','WP201','LT WP201','FAC02',1);\n" +
            "INSERT INTO lista_tecnica (id, tipo_lista_tecnica, material_output_id,descricao, location_id, quantidade) VALUES ('LT202-SUP01','simples','WP202','LT WP202','SUP01',1);\n" +
            "INSERT INTO lista_tecnica (id, tipo_lista_tecnica, material_output_id,descricao, location_id, quantidade) VALUES ('LT303-FAC02','simples','RM303','LT RM303','FAC02',1);\n" +
            "INSERT INTO lista_tecnica (id, tipo_lista_tecnica, material_output_id,descricao, location_id, quantidade) VALUES ('LT301-SUP01','simples','RM301','LT RM301 : sem componentes','SUP01',1);\n" +
            "INSERT INTO lista_tecnica (id, tipo_lista_tecnica, material_output_id,descricao, location_id, quantidade) VALUES ('LT302-SUP01','simples','RM302','LT RM302 : sem componentes','SUP01',1);");
        jdbcTemplate.update(
            "INSERT INTO lista_tecnica_componente (lista_tecnica_id, material_componente_id,quantidade) VALUES ('LT100-FAC01','WP200','2');\n" +
            "INSERT INTO lista_tecnica_componente (lista_tecnica_id, material_componente_id,quantidade) VALUES ('LT100-FAC03','WP200','2');\n" +
            "INSERT INTO lista_tecnica_componente (lista_tecnica_id, material_componente_id,quantidade) VALUES ('LT101-FAC01','WP200','2');\n" +
            "INSERT INTO lista_tecnica_componente (lista_tecnica_id, material_componente_id,quantidade) VALUES ('LT101-FAC03','WP200','2');\n" +
            "INSERT INTO lista_tecnica_componente (lista_tecnica_id, material_componente_id,quantidade) VALUES ('LT102-FAC02','WP201','3');\n" +
            "INSERT INTO lista_tecnica_componente (lista_tecnica_id, material_componente_id,quantidade) VALUES ('LT103-FAC02','WP202','1.2');\n" +
//            "INSERT INTO lista_tecnica_componente (lista_tecnica_id, material_componente_id,quantidade) VALUES ('LT200','RM301','1');\n" +
            "INSERT INTO lista_tecnica_componente (lista_tecnica_id, material_componente_id,quantidade) VALUES ('LT201-FAC02','RM302','1');\n" +
            "INSERT INTO lista_tecnica_componente (lista_tecnica_id, material_componente_id,quantidade) VALUES ('LT202-SUP01','RM303','2');\n" +
            "INSERT INTO lista_tecnica_componente (lista_tecnica_id, material_componente_id,quantidade) VALUES ('LT303-FAC02','RM302','1');\n" +
            "INSERT INTO lista_tecnica_componente (lista_tecnica_id, material_componente_id,quantidade) VALUES ('LT303-FAC02','RM301','0.5');");
        
        
        // ATUALIZAÇÃO DOS RECURSOS PRODUTIVOS
        jdbcTemplate.update(
            "INSERT INTO recurso_produtivo (id, location_id,descricao) VALUES ('RP001','FAC01','Resource 01 - FAC01');\n" +
            "INSERT INTO recurso_produtivo (id, location_id,descricao) VALUES ('RP002','FAC01','Resource 02 - FAC01');\n" +
            "INSERT INTO recurso_produtivo (id, location_id,descricao) VALUES ('RP003','FAC02','Resource 01 - FAC02');\n" +
            "INSERT INTO recurso_produtivo (id, location_id,descricao) VALUES ('RP004','FAC03','Resource 01 - FAC03');\n" +
            "INSERT INTO recurso_produtivo (id, location_id,descricao) VALUES ('RP005','SUP01','Resource 01 - SUP001');");
        
        // ATUALIZAÇÃO DAS RECEITAS DE PRODUÇÃO
        jdbcTemplate.update(
            "INSERT INTO roteiro (id, tipo_roteiro, descricao, prioridade, location_id, material_output_id) VALUES ('1','simples','FG100 - FAC01 - Recurso 1','1','FAC01','FG100');\n" +
            "INSERT INTO roteiro (id, tipo_roteiro, descricao, prioridade, location_id, material_output_id) VALUES ('2','simples','FG100 - FAC01 - Recurso 2','2','FAC01','FG100');\n" +
            "INSERT INTO roteiro (id, tipo_roteiro, descricao, prioridade, location_id, material_output_id) VALUES ('3','simples','FG100 - FAC03 - Recurso 1','1','FAC03','FG100');\n" +
            "INSERT INTO roteiro (id, tipo_roteiro, descricao, prioridade, location_id, material_output_id) VALUES ('4','simples','FG101 - FAC01 - Recurso 1','1','FAC01','FG101');\n" +
            "INSERT INTO roteiro (id, tipo_roteiro, descricao, prioridade, location_id, material_output_id) VALUES ('5','simples','FG101 - FAC03 - Recurso 1','1','FAC03','FG101');\n" +
            "INSERT INTO roteiro (id, tipo_roteiro, descricao, prioridade, location_id, material_output_id) VALUES ('6','simples','FG102 - FAC02 - Recurso1','1','FAC02','FG102');\n" +
            "INSERT INTO roteiro (id, tipo_roteiro, descricao, prioridade, location_id, material_output_id) VALUES ('7','simples','FG103 - FAC02 - Recurso1','1','FAC02','FG103');\n" +
            "INSERT INTO roteiro (id, tipo_roteiro, descricao, prioridade, location_id, material_output_id) VALUES ('8','simples','RM301 - SUP01 - Recurso 1','1','SUP01','RM301');\n" +
            "INSERT INTO roteiro (id, tipo_roteiro, descricao, prioridade, location_id, material_output_id) VALUES ('9','simples','RM302 - SUP01 - Recurso 1','1','SUP01','RM302');\n" +
            "INSERT INTO roteiro (id, tipo_roteiro, descricao, prioridade, location_id, material_output_id) VALUES ('10','simples','RM303 - FAC02 - Recurso 1','1','FAC02','RM303');\n" +
            "INSERT INTO roteiro (id, tipo_roteiro, descricao, prioridade, location_id, material_output_id) VALUES ('11','simples','WP202 - SUP01 - Recurso 1','1','SUP01','WP202');\n" +
            "INSERT INTO roteiro (id, tipo_roteiro, descricao, prioridade, location_id, material_output_id) VALUES ('12','simples','WP201 - FAC02 - Recurso 1','1','FAC02','WP201');");
        jdbcTemplate.update(
            "INSERT INTO operacao_roteiro (posicao,roteiro_id,recurso_produtivo_id,tempo_por_quantidade_base,unidade_tempo_operacao) VALUES ('3','1','RP001','36','H');\n" +
            "INSERT INTO operacao_roteiro (posicao,roteiro_id,recurso_produtivo_id,tempo_por_quantidade_base,unidade_tempo_operacao) VALUES ('3','2','RP002','36','H');\n" +
            "INSERT INTO operacao_roteiro (posicao,roteiro_id,recurso_produtivo_id,tempo_por_quantidade_base,unidade_tempo_operacao) VALUES ('3','3','RP004','36','H');\n" +
            "INSERT INTO operacao_roteiro (posicao,roteiro_id,recurso_produtivo_id,tempo_por_quantidade_base,unidade_tempo_operacao) VALUES ('3','4','RP001','36','H');\n" +
            "INSERT INTO operacao_roteiro (posicao,roteiro_id,recurso_produtivo_id,tempo_por_quantidade_base,unidade_tempo_operacao) VALUES ('3','5','RP004','36','H');\n" +
            "INSERT INTO operacao_roteiro (posicao,roteiro_id,recurso_produtivo_id,tempo_por_quantidade_base,unidade_tempo_operacao) VALUES ('1','6','RP003','36','H');\n" +
            "INSERT INTO operacao_roteiro (posicao,roteiro_id,recurso_produtivo_id,tempo_por_quantidade_base,unidade_tempo_operacao) VALUES ('1','7','RP003','36','H');\n" +
            "INSERT INTO operacao_roteiro (posicao,roteiro_id,recurso_produtivo_id,tempo_por_quantidade_base,unidade_tempo_operacao) VALUES ('1','1','RP002','36','H');\n" +
            "INSERT INTO operacao_roteiro (posicao,roteiro_id,recurso_produtivo_id,tempo_por_quantidade_base,unidade_tempo_operacao) VALUES ('1','2','RP002','36','H');\n" +
            "INSERT INTO operacao_roteiro (posicao,roteiro_id,recurso_produtivo_id,tempo_por_quantidade_base,unidade_tempo_operacao) VALUES ('1','3','RP004','36','H');\n" +
            "INSERT INTO operacao_roteiro (posicao,roteiro_id,recurso_produtivo_id,tempo_por_quantidade_base,unidade_tempo_operacao) VALUES ('2','4','RP002','36','H');\n" +
            "INSERT INTO operacao_roteiro (posicao,roteiro_id,recurso_produtivo_id,tempo_por_quantidade_base,unidade_tempo_operacao) VALUES ('2','5','RP004','36','H');\n" +
            "INSERT INTO operacao_roteiro (posicao,roteiro_id,recurso_produtivo_id,tempo_por_quantidade_base,unidade_tempo_operacao) VALUES ('1','8','RP005','36','H');\n" +
            "INSERT INTO operacao_roteiro (posicao,roteiro_id,recurso_produtivo_id,tempo_por_quantidade_base,unidade_tempo_operacao) VALUES ('1','9','RP005','36','H');\n" +
            "INSERT INTO operacao_roteiro (posicao,roteiro_id,recurso_produtivo_id,tempo_por_quantidade_base,unidade_tempo_operacao) VALUES ('1','10','RP003','36','H');\n" +
            "INSERT INTO operacao_roteiro (posicao,roteiro_id,recurso_produtivo_id,tempo_por_quantidade_base,unidade_tempo_operacao) VALUES ('1','11','RP005','36','H');\n" +
            "INSERT INTO operacao_roteiro (posicao,roteiro_id,recurso_produtivo_id,tempo_por_quantidade_base,unidade_tempo_operacao) VALUES ('1','12','RP003','36','H');");
        jdbcTemplate.update(
            "INSERT INTO versao_producao (id,location_id,roteiro_id,lista_tecnica_id,prioridade,ativo) VALUES ('VP-1','FAC01','1','LT100-FAC01','1',true);\n" +
            "INSERT INTO versao_producao (id,location_id,roteiro_id,lista_tecnica_id,prioridade,ativo) VALUES ('VP-2','FAC01','2','LT100-FAC01','2',true);\n" +
            "INSERT INTO versao_producao (id,location_id,roteiro_id,lista_tecnica_id,prioridade,ativo) VALUES ('VP-3','FAC03','3','LT100-FAC03','1',true);\n" +
            "INSERT INTO versao_producao (id,location_id,roteiro_id,lista_tecnica_id,prioridade,ativo) VALUES ('VP-4','FAC01','4','LT101-FAC01','1',true);\n" +
            "INSERT INTO versao_producao (id,location_id,roteiro_id,lista_tecnica_id,prioridade,ativo) VALUES ('VP-5','FAC03','5','LT101-FAC03','1',true);\n" +
            "INSERT INTO versao_producao (id,location_id,roteiro_id,lista_tecnica_id,prioridade,ativo) VALUES ('VP-6','FAC02','6','LT102-FAC02','1',true);\n" +
            "INSERT INTO versao_producao (id,location_id,roteiro_id,lista_tecnica_id,prioridade,ativo) VALUES ('VP-7','FAC02','7','LT103-FAC02','1',true);\n" +
            "INSERT INTO versao_producao (id,location_id,roteiro_id,lista_tecnica_id,prioridade,ativo) VALUES ('VP-8','SUP01','8','LT301-SUP01','1',true);\n" +
            "INSERT INTO versao_producao (id,location_id,roteiro_id,lista_tecnica_id,prioridade,ativo) VALUES ('VP-9','SUP01','9','LT302-SUP01','1',true);\n" +
            "INSERT INTO versao_producao (id,location_id,roteiro_id,lista_tecnica_id,prioridade,ativo) VALUES ('VP-10','FAC02','10','LT303-FAC02','1',true);\n" +
            "INSERT INTO versao_producao (id,location_id,roteiro_id,lista_tecnica_id,prioridade,ativo) VALUES ('VP-11','SUP01','11','LT202-SUP01','1',true);\n" +
            "INSERT INTO versao_producao (id,location_id,roteiro_id,lista_tecnica_id,prioridade,ativo) VALUES ('VP-12','FAC02','12','LT201-FAC02','1',true);");

        // PERFIL DE EXECUÇÃO SUPPLY PLANNING
        jdbcTemplate.update(
            "INSERT INTO perfil_execucao_supply_plan (id,modo_execucao,salva_inventory_plan) VALUES ('Perfil Heuristico','0','1');");
        
        // POLITICA OPERACIONAL DE ESTOQUES / SAFETY STOCK
        jdbcTemplate.update(
            /*
             * Safety stock Community vive em PoliticaEstoquesMaterialLocation.
             * A entidade ParametrosProdutoLocation guarda apenas atributos
             * operacionais de material/location e nao deve receber colunas
             * antigas de modelo de reposicao/estoque de seguranca.
             */
            "INSERT INTO politica_estoques (id, prioridade) VALUES ('POLITICA_TESTE','1');\n" +
            "INSERT INTO perfil_execucao_politica_estoques (perfil_execucao_supply_plan_id,politica_estoques_id) VALUES ('Perfil Heuristico','POLITICA_TESTE');\n" +
            "INSERT INTO politica_estoques_material_location (politica_estoques_id,location_id,material_id,modelo_reabastecimento,calculo_safety_stock,estoque_seguranca_drp_ou_target_kanban) VALUES ('POLITICA_TESTE','STO01','FG100','0','0','15');\n" +
            "INSERT INTO politica_estoques_material_location (politica_estoques_id,location_id,material_id,modelo_reabastecimento,calculo_safety_stock,estoque_seguranca_drp_ou_target_kanban) VALUES ('POLITICA_TESTE','STO01','FG101','0','0','45');");
        
    }
    
    @Test
    @Transactional
    public void lowLevelCodeTest() throws Exception{
        
        UnidadeMedidaProjection unidadeMedidaProjection = unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionComConversoes();
        
        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();
        
        VersaoMalha versaoMalha = versaoMalhaRepository.findById("VERSAO_MALHA_01").get();
        
        SupplyNetworkProjection supplyNetworkProjection = supplyNetworkProjectionFactory.getSupplyNetworkProjectionCompletoDeCache();
        
        LowLevelCode lowLevelCode = new LowLevelCode(
                supplyNetworkProjection, 
                versaoMalha,
                MaterialProjectionFactory.getMaterialProjectionCompleto(clusterEParametrosProjection),
                LocationProjectionFactory.getLocationProjectionCompleto(clusterEParametrosProjection),
                LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        lowLevelCode.atualizaMapaDFUsPorLowLevelCode();
        
    }
    
    @Test
    @Transactional
    public void safetyStockTest() throws Exception{
        
        UnidadeMedidaProjection unidadeMedidaProjection = unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionComConversoes();
        
        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();
        
        VersaoMalha versaoMalha = versaoMalhaRepository.findById("VERSAO_MALHA_01").get();
        
        SupplyNetworkProjection supplyNetworkProjection = supplyNetworkProjectionFactory.getSupplyNetworkProjectionCompletoDeCache();
        
        DemandPlan demandPlan = demandPlanRepository.customFindByIdComPerfilExecucao(1L).get();
        DemandPlanningProjection demandPlanningProjection = demandPlanProjectionFactory.getDemandPlanningProjectionCompleto(
                demandPlan,
                clusterEParametrosProjection.getLocationSet(),
                clusterEParametrosProjection.getMateriaisAtivos(),
                false);
        
        // Supply Plan temporário
        SupplyPlan supplyPlan = new SupplyPlan();
        supplyPlan.setId(1L);
        supplyPlan.setTamanhoBucket(Constantes.TamanhoBucket.MENSAL);
        supplyPlan.setDataInicioPlano(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        supplyPlan.setPerfilExecucaoSupplyPlan(
                perfilExecucaoSupplyPlanRepository.customFindById("Perfil Heuristico").get());
        
        PoliticaEstoquesProjection politicaEstoquesProjection = politicaEstoquesProjectionFactory.getPoliticaEstoquesProjection(
                supplyPlan.getCalendarioDoSupplyPlan(clusterEParametrosProjection.getParametrosGlobais()), 
                clusterEParametrosProjection, 
                supplyPlan.getPerfilExecucaoSupplyPlan());
        
        SupplyPlanningProjection supplyPlanningProjection = supplyPlanProjectionFactory.getSupplyPlanningProjectionCompleto(
                supplyPlan, 
                supplyPlan.getPerfilExecucaoSupplyPlan(), 
                new Location("STO01"),
                supplyNetworkProjection, politicaEstoquesProjection,
                MaterialProjectionFactory.getMaterialProjectionDePerfilExecucaoSupplyPlan(
                        supplyPlan.getPerfilExecucaoSupplyPlan(), clusterEParametrosProjection),
                LocationProjectionFactory.getLocationProjectionDePerfilExecucaoSupplyPlan(
                        supplyPlan.getPerfilExecucaoSupplyPlan(), clusterEParametrosProjection));
        
        SplitTemporalProjectionPorDfu splitTemporalProjectionPorDfu = splitTemporalProjectionFactory.geraSplitTemporalProjectionPorDfu(
                demandPlan, supplyPlan);
        
        SupplyPlanning.atualizaEstoqueSeguranca(
                supplyPlanningProjection,
                Constantes.TipoPlano.PLANO_IRRESTRITO);
        
    }
    
    @Test
    @Transactional
    public void supplyPlanTest() throws Exception {
        
        supplyPlanService.executeSupplyPlan(1L, null, null, "Perfil Heuristico", "VERSAO_MALHA_01", null, Constantes.TamanhoBucket.MENSAL,
                LocalDateTime.of(2020,2,1,0,0,0), "Execucao Heuristica", "UsuarioTeste");
                        
        constrainedPlanService.restringePlanoComPerfilHeuristico(1L);

    }

}
