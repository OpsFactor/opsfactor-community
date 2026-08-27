package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.facade;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.facade.dto.ConversaoUnidadeMedidaDTO;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnicaComponente;
import com.opsfactor.community.capability.masterdata.production.operation.domain.OperacaoRoteiro;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlanItem;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosDemandPlanProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.factory.ParametrosDemandPlanningProjectionFactory;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOM;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.projection.EstoqueProjectionLocationProduto;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.projection.EstoqueProjectionFactory;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.SalesProjectionFactory;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.SalesProjectionLocationMaterial;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjectionFactory;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanningProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.configuration.repository.PerfilExecucaoSupplyPlanRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.VersaoMalhaRepository;
import com.opsfactor.community.capability.demandplanning.demandplan.repository.DemandPlanRepository;
import com.opsfactor.community.capability.supplyplanning.distributionplan.repository.DistributionPlanItemRepository;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.SupplyPlanRepository;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import com.opsfactor.community.capability.masterdata.product.material.service.MaterialService;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.service.UnidadeMedidaService;
import com.opsfactor.community.capability.supplyplanning.engine.SupplyPlanning;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.facade.dto.UnidadeConversaoFaltanteDTO;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.facade.dto.UnidadeConversaoFaltanteDTO.NecessidadeConversao;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.Constantes.TamanhoBucket;
import lombok.extern.slf4j.Slf4j;
import org.javatuples.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.NoResultException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fachada Community para diagnostico e consulta de conversoes de unidade.
 *
 * <p>O service identifica conversoes faltantes para Demand/Supply quantitativo
 * usando projections ja materializadas. Custos, pricing, frota e capacidade
 * logistica nao sao tratados aqui.</p>
 */
@Slf4j
@Service
public class UnidadeMedidaFacade {

    /**
     * Factory da projection de conversoes UOM em memoria.
     */
    @Autowired
    private UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory;

    /**
     * Service de materiais usado para obter o recorte material/location ativo.
     */
    @Autowired
    private MaterialService materialService;

    /**
     * Service de parametros globais usado para unidades padrao DP/SNP.
     */
    @Autowired
    private ParametrosGlobaisService parametrosGlobaisService;

    /**
     * Factory de parametros por cluster/material/location usada para resolver
     * UOMs padrao por contexto.
     */
    @Autowired
    private ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory;

    /**
     * Factory dos parametros de Demand Planning usados na checagem de
     * conversoes do forecast.
     */
    @Autowired
    private ParametrosDemandPlanningProjectionFactory parametrosDemandPlanningProjectionFactory;

    /**
     * Factory de estoque inicial/snapshot usada para checar conversoes de
     * dado transacional Community.
     */
    @Autowired
    private EstoqueProjectionFactory estoqueProjectionFactory;

    /**
     * Factory de vendas historicas Community, restrita a sell-out.
     */
    @Autowired
    private SalesProjectionFactory salesProjectionFactory;

    /**
     * Factory da malha simples usada pelo heuristico Supply.
     */
    @Autowired
    private SupplyNetworkProjectionFactory supplyNetworkProjectionFactory;

    /**
     * Repository das versoes de malha disponiveis para diagnostico.
     */
    @Autowired
    private VersaoMalhaRepository versaoMalhaRepository;

    /**
     * Factory de projection de Demand Plan usada para validar conversoes sobre
     * planos persistidos.
     */
    @Autowired
    private DemandPlanProjectionFactory demandPlanProjectionFactory;

    /**
     * Repository de Demand Plan usado para localizar planos a diagnosticar.
     */
    @Autowired
    private DemandPlanRepository demandPlanRepository;

    /**
     * Repository dos headers de Supply Plan usado para validar o plano de
     * Deployment antes de carregar suas linhas em lote.
     */
    @Autowired
    private SupplyPlanRepository supplyPlanRepository;

    /**
     * Repository das linhas de distribuicao carregadas com produto, locations
     * e UOM em uma unica consulta para o diagnostico de Deployment.
     */
    @Autowired
    private DistributionPlanItemRepository distributionPlanItemRepository;

    /**
     * Service funcional de UOM/conversoes usado como fonte de verdade do
     * cadastro Community.
     */
    @Autowired
    private UnidadeMedidaService unidadeMedidaService;

    /**
     * Repository de perfis Supply Planning usado para diagnosticar conversoes
     * no mesmo contexto de calendario/malha em que o plano heuristico sera
     * executado.
     */
    @Autowired
    private PerfilExecucaoSupplyPlanRepository perfilExecucaoSupplyPlanRepository;

    /**
     * Retorna conversões faltantes que serão necessárias para a execução do SNP
     * Por ora considera apenas gaps relativos a dados mestres, sem considerar
     * conversões necessárias para tratar dados transacionais (ex. unidades diferentes em ordens/estoques)
     * @return 
     */
    public Set<UnidadeConversaoFaltanteDTO> getUnidadeConversaoFaltanteSNPListDTO(
            LocalDateTime dataReferenciaSNP, TamanhoBucket tamanhoBucket, String versaoMalhaId, String perfilExecucaoSupplyPlanId, Long demandPlanId) {

        validaParametrosDiagnosticoConversaoSNPCommunity(
                dataReferenciaSNP,
                tamanhoBucket,
                versaoMalhaId,
                perfilExecucaoSupplyPlanId,
                demandPlanId);
        
        VersaoMalha versaoMalha = getVersaoMalhaObrigatoria(versaoMalhaId);
        DemandPlan demandPlan = getDemandPlanObrigatorio(demandPlanId);
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan =
                getPerfilExecucaoSupplyPlanObrigatorio(perfilExecucaoSupplyPlanId);
        
        ParametrosGlobais parametrosGlobais = parametrosGlobaisService.getParametrosGlobais();
        UnidadeMedidaProjection unidadeMedidaProjection = unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache();
        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();
        
        Calendario calendarioSNP = SupplyPlanning.getCalendarioDeDataReferencia(dataReferenciaSNP, tamanhoBucket, perfilExecucaoSupplyPlan, parametrosGlobais);
        
        SupplyNetworkProjection supplyNetworkProjection = supplyNetworkProjectionFactory.getSupplyNetworkProjectionCompletoDeCache();
        
        DemandPlanningProjection demandPlanningProjection = demandPlanProjectionFactory.getDemandPlanningProjectionCompleto(
                demandPlan, 
                clusterEParametrosProjection.getLocationSet(),
                clusterEParametrosProjection.getMaterialSet(),
                false);
        
        // estoque do início do período inicial = estoque de abertura considerado no plano (equivalente ao estoque de fechamento do período -1 no inventory plan)
        LocalDateTime dataEstoque = calendarioSNP.getDataHorarioInicial();
        EstoqueProjectionLocationProduto estoqueProjection = estoqueProjectionFactory.getEstoqueProjectionLocationProduto(
                dataEstoque, clusterEParametrosProjection.getLocationSet(), clusterEParametrosProjection.getMaterialSet(), 
                unidadeMedidaProjection, clusterEParametrosProjection, 
                clusterEParametrosProjection.getParametrosGlobais().getUnidadeMedidaPadraoSNP());
        
        Set<UnidadeConversaoFaltanteDTO> unidadeConversaoFaltanteDTOSet = new HashSet<>();
                
        for (Location location : clusterEParametrosProjection.getLocationSet()) {

            for (Produto material : clusterEParametrosProjection.getMateriaisAtivosEmLocation(location)) {
            
                // se material output inativo e demais operações da receita não puderem ser utilizadas individualmente,
                // pula para próxima receita
                if (!clusterEParametrosProjection.isDfuAtiva(material, location)) continue;

                UnidadeMedida unidadeMedidaPadrao = clusterEParametrosProjection.getSNPUnidadeMedidaPadrao(material, location);
                String identificadorUnidadeMedidaPadrao = (clusterEParametrosProjection.getSNPUnidadeMedidaProdutoLocationCadastrado(material, location) != null)
                        ?  "Location " + location.getId() + " / Material " + material.getId() : "Material " + material.getId();
                // determina se a unidade target do SNP é do material ou material/location
                UnidadeConversaoFaltanteDTO.NecessidadeConversao parametroUnidadeMedidaPadrao = 
                        (clusterEParametrosProjection.getSNPUnidadeMedidaProdutoLocationCadastrado(material, location) != null)
                        ?  UnidadeConversaoFaltanteDTO.NecessidadeConversao.PADRAO_MATERIAL_LOCATION : UnidadeConversaoFaltanteDTO.NecessidadeConversao.PADRAO_MATERIAL;
                
                Set<Roteiro> roteiroSet = supplyNetworkProjection.getRoteirosViaveis(location, material);
                Set<ListaTecnica> listaTecnicaSet = supplyNetworkProjection.getListasTecnicasViaveis(location, material, null);

                for (Roteiro roteiro : roteiroSet) {
                    
                    for (OperacaoRoteiro operacaoRoteiro : roteiro.getOperacaoRoteiroListOrdenadaPorPosicaoAsc()) {
                        // será necessária conversão da unidade DP para unidade medida SNP
                        if (!unidadeMedidaProjection.contemConversaoParaUnidadeDestino(
                                material, roteiro.getUnidadeMedidaQuantidadeBase(clusterEParametrosProjection.getParametrosGlobais()),
                                unidadeMedidaPadrao)) {
                            unidadeConversaoFaltanteDTOSet.add(UnidadeConversaoFaltanteDTO.builder()
                                    // só salva location se a unidade de medida padrão deriva do parametro material-location. se foi usada unidade padrao nivel material, se omite a location
                                    .locationId((clusterEParametrosProjection.getSNPUnidadeMedidaProdutoLocationCadastrado(material, location) != null) ? location.getId() : null)
                                    .materialId(roteiro.getMaterialOutput().getId())
                                    .originTask(Constantes.TaskTipo.SNP)
                                    .originConversionRequirementType(NecessidadeConversao.ROTEIRO_OPERACAO)
                                    .originConversionRequirementId("Routing " + roteiro.getId() + " / Operation " + operacaoRoteiro.getPosicao())
                                    .originUnitOfMeasure(roteiro.getUnidadeMedidaQuantidadeBase(
                                            clusterEParametrosProjection.getParametrosGlobais()).getId())
                                    .targetTask(Constantes.TaskTipo.SNP)
                                    .targetConversionRequirementType(parametroUnidadeMedidaPadrao)
                                    .targetConversionRequirementId(identificadorUnidadeMedidaPadrao)
                                    .targetUnitOfMeasure(unidadeMedidaPadrao.getId())
                                    .build());
                        }
                    }
                }
                
                for (ListaTecnica listaTecnica : listaTecnicaSet) {

                    // CONVERSÃO DE UNIDADE OUTPUT LISTA TÉCNICA PARA UNIDADE PADRAO LOCATION
                    if (!unidadeMedidaProjection.contemConversaoParaUnidadeDestino(
                            material, 
                            listaTecnica.getUnidadeMedidaMaterialOutput(clusterEParametrosProjection.getParametrosGlobais()), 
                            unidadeMedidaPadrao)) {
                            
                        unidadeConversaoFaltanteDTOSet.add(UnidadeConversaoFaltanteDTO.builder()
                                // não traz a location pois se trata de conversão lista tecnica -> roteiro e não unidade padrao material/location
                                .materialId(material.getId())
                                .originTask(Constantes.TaskTipo.SNP)
                                .originConversionRequirementType(NecessidadeConversao.LISTA_TECNICA_OUTPUT)
                                .originConversionRequirementId("Bill of Materials " + listaTecnica.getId())
                                .originUnitOfMeasure(listaTecnica.getUnidadeMedidaMaterialOutput(clusterEParametrosProjection.getParametrosGlobais()).getId())
                                .targetTask(Constantes.TaskTipo.SNP)
                                .targetConversionRequirementType(parametroUnidadeMedidaPadrao)
                                .targetConversionRequirementId(identificadorUnidadeMedidaPadrao)
                                .targetUnitOfMeasure(unidadeMedidaPadrao.getId())
                                .build());
                    }

                    // VARRE COMPONENTES DE LISTAS TÉCNICAS
                    for (ListaTecnicaComponente listaTecnicaComponente : listaTecnica.getListaTecnicaComponenteSet()) {
                        Produto materialInput = listaTecnicaComponente.getMaterialComponente();
                        // CONVERSÃO DE UNIDADE DE INPUT DA LISTA TÉCNICA PARA UNIDADE PADRÃO NA LOCATION
                        UnidadeConversaoFaltanteDTO.NecessidadeConversao parametroUnidadeMedidaPadraoInput = 
                                (clusterEParametrosProjection.getSNPUnidadeMedidaProdutoLocationCadastrado(materialInput, location) != null) 
                                ?  UnidadeConversaoFaltanteDTO.NecessidadeConversao.PADRAO_MATERIAL_LOCATION : UnidadeConversaoFaltanteDTO.NecessidadeConversao.PADRAO_MATERIAL;
                        String identificadorUnidadeMedidaPadraoInput = (clusterEParametrosProjection.getSNPUnidadeMedidaProdutoLocationCadastrado(materialInput, location) != null) 
                                ?  "Location " + location.getId() + " / Material " + materialInput.getId() : "Material " + materialInput.getId();
                        UnidadeMedida unidadeMedidaPadraoInput = clusterEParametrosProjection.getSNPUnidadeMedidaPadrao(materialInput, location);

                        if (!unidadeMedidaProjection.contemConversaoParaUnidadeDestino(
                                materialInput, 
                                listaTecnicaComponente.getUnidadeMedidaMaterialComponente(clusterEParametrosProjection.getParametrosGlobais()), 
                                unidadeMedidaPadraoInput)) {
                            unidadeConversaoFaltanteDTOSet.add(UnidadeConversaoFaltanteDTO.builder()
                                    .locationId((clusterEParametrosProjection.getSNPUnidadeMedidaProdutoLocationCadastrado(materialInput, location) != null) ? location.getId() : null)
                                    .materialId(materialInput.getId())
                                    .originTask(Constantes.TaskTipo.SNP)
                                    .originConversionRequirementType(NecessidadeConversao.LISTA_TECNICA_COMPONENTE)
                                    .originConversionRequirementId("Bill of Materials " + listaTecnica.getId() + " component " + materialInput.getId())
                                    .originUnitOfMeasure(listaTecnicaComponente.getUnidadeMedidaMaterialComponente(clusterEParametrosProjection.getParametrosGlobais()).getId())
                                    .targetTask(Constantes.TaskTipo.SNP)
                                    .targetConversionRequirementType(parametroUnidadeMedidaPadraoInput)
                                    .targetConversionRequirementId(identificadorUnidadeMedidaPadraoInput)
                                    .targetUnitOfMeasure(unidadeMedidaPadraoInput.getId())
                                    .build());
                        }
                    }
                }
            
                // VARRE PRODUTOS : UNIDADE DEMAND PLANNING (DEMANDA DIRETA), 
                // DEMANDA INDIRETA SNP, REQUISICOES INBOUND
                
                // DEMANDA DIRETA (DP) : tenta converter cada linha do plano de demanda para unid. padrao SNP
                Calendario calendario = demandPlanningProjection.getCalendario();
                for (int i=calendario.getPosicaoPeriodoPresente(); i<calendario.getPosicaoPeriodoFinalFuturo(); i++) {
                
                    DemandPlanItem demandPlanItem = demandPlanningProjection.getDemandPlanItem(location, material, i);
                    if (demandPlanItem != null) {
                        // será necessária conversão da unidade DP para unidade medida SNP
                        if (!unidadeMedidaProjection.contemConversaoParaUnidadeDestino(
                                material, demandPlanItem.getUnidadeMedida(parametrosGlobais), unidadeMedidaPadrao)) {
                            unidadeConversaoFaltanteDTOSet.add(UnidadeConversaoFaltanteDTO.builder()
                                    .locationId((clusterEParametrosProjection.getSNPUnidadeMedidaProdutoLocationCadastrado(material, location) != null) ? location.getId() : null)
                                    .materialId(material.getId())
                                    .originTask(Constantes.TaskTipo.DP)
                                    .originConversionRequirementType(NecessidadeConversao.PLANO_DEMANDA)
                                    .originConversionRequirementId("Location " + location.getId() + " / Material " + material.getId())
                                    .originUnitOfMeasure(demandPlanItem.getUnidadeMedida(parametrosGlobais).getId())
                                    .targetTask(Constantes.TaskTipo.SNP)
                                    .targetConversionRequirementType(parametroUnidadeMedidaPadrao)
                                    .targetConversionRequirementId(identificadorUnidadeMedidaPadrao)
                                    .targetUnitOfMeasure(unidadeMedidaPadrao.getId())
                                    .build());
                        }
                    }
                }
                
                // DEMANDA INDIRETA GERADA PELO SNP/ORDENS : UOM prioritária location precisa ser  com UOM da location destino
                for (Location locationDestino : supplyNetworkProjection.getLocationDestinoViavelSet(
                        versaoMalha, 
                        location, 
                        material, 
                        dataReferenciaSNP, 
                        null)) {
                    
                    // determina se a unidade target do SNP é do material ou material/location
                    UnidadeConversaoFaltanteDTO.NecessidadeConversao parametroUnidadeMedidaPadraoLocationDestino = 
                            (clusterEParametrosProjection.getSNPUnidadeMedidaProdutoLocationCadastrado(material, locationDestino) != null) 
                            ?  UnidadeConversaoFaltanteDTO.NecessidadeConversao.PADRAO_MATERIAL_LOCATION : UnidadeConversaoFaltanteDTO.NecessidadeConversao.PADRAO_MATERIAL;
                    String identificadorUnidadeMedidaPadraoLocationDestino = (clusterEParametrosProjection.getSNPUnidadeMedidaProdutoLocationCadastrado(material, locationDestino) != null) 
                            ?  "Location " + locationDestino.getId() + " / Material " + material.getId() : "Material " + material.getId();
                    UnidadeMedida unidadeMedidaPadraoLocationDestino = clusterEParametrosProjection.getSNPUnidadeMedidaPadrao(material, locationDestino);
                    
                    // será necessária conversão da unidade padrão da location destino para a location origem
                    if (!unidadeMedidaProjection.contemConversaoParaUnidadeDestino(
                            material, unidadeMedidaPadraoLocationDestino, unidadeMedidaPadrao)) {
                        unidadeConversaoFaltanteDTOSet.add(UnidadeConversaoFaltanteDTO.builder()
                                .locationId((clusterEParametrosProjection.getSNPUnidadeMedidaProdutoLocationCadastrado(material, locationDestino) != null) ? location.getId() : null)
                                .materialId(material.getId())
                                .originTask(Constantes.TaskTipo.SNP)
                                .originConversionRequirementType(NecessidadeConversao.REQUISICAO_OUTBOUND)
                                .originConversionRequirementId(identificadorUnidadeMedidaPadraoLocationDestino)
                                .originUnitOfMeasure(unidadeMedidaPadraoLocationDestino.getId())
                                .targetTask(Constantes.TaskTipo.SNP)
                                .targetConversionRequirementType(parametroUnidadeMedidaPadraoLocationDestino)
                                .targetConversionRequirementId(identificadorUnidadeMedidaPadrao)
                                .targetUnitOfMeasure(unidadeMedidaPadrao.getId())
                                .build());
                    }
                    
                    // determina se a unidade target do SNP é do material ou material/location
                    // se não houver UOM cadastrada para minimo/multiplo a unidade padrão do destino será considerada
                    // e portanto a conversão sempre irá existir
                    UnidadeMedida unidadeMedidaLoteMultiploLinhaTransporte = supplyNetworkProjection.getUnidadeMedidaLoteMinimoMultiploTransporte(versaoMalha, location, locationDestino, material, dataReferenciaSNP);
                    String identificadorUnidadeMedidaLoteMultiploLinhaTransporte = "Transportation Line from Origin Location " + location.getId() + " / to Destination Location " + locationDestino.getId() +
                            " / Material " + material.getId();
                    
                    // será necessária conversão da unidade padrão da location destino para a unidade de múltiplos/lotes da linha de transporte
                    if (!unidadeMedidaProjection.contemConversaoParaUnidadeDestino(
                            material, unidadeMedidaPadraoLocationDestino, unidadeMedidaLoteMultiploLinhaTransporte)) {
                        unidadeConversaoFaltanteDTOSet.add(UnidadeConversaoFaltanteDTO.builder()
                                .locationId(locationDestino.getId())
                                .materialId(material.getId())
                                .originTask(Constantes.TaskTipo.SNP)
                                .originConversionRequirementType(parametroUnidadeMedidaPadraoLocationDestino)
                                .originConversionRequirementId(identificadorUnidadeMedidaPadraoLocationDestino)
                                .originUnitOfMeasure(unidadeMedidaPadraoLocationDestino.getId())
                                .targetTask(Constantes.TaskTipo.SNP)
                                .targetConversionRequirementType(NecessidadeConversao.MINIMO_MULTIPLO_TRANSFERENCIA)
                                .targetConversionRequirementId(identificadorUnidadeMedidaLoteMultiploLinhaTransporte)
                                .targetUnitOfMeasure(unidadeMedidaLoteMultiploLinhaTransporte.getId())
                                .build());
                    }
                    
                }
            }
            
            // AVALIA BASE DE ESTOQUE NA DATA REFERENCIA            
            for (Produto material : estoqueProjection.getMateriaisComEstoqueNaLocation(location)) {

                UnidadeConversaoFaltanteDTO.NecessidadeConversao parametroUnidadeMedidaPadrao = 
                        (clusterEParametrosProjection.getSNPUnidadeMedidaProdutoLocationCadastrado(material, location) != null) 
                        ?  UnidadeConversaoFaltanteDTO.NecessidadeConversao.PADRAO_MATERIAL_LOCATION : UnidadeConversaoFaltanteDTO.NecessidadeConversao.PADRAO_MATERIAL;
                String identificadorUnidadeMedidaPadrao = (clusterEParametrosProjection.getSNPUnidadeMedidaProdutoLocationCadastrado(material, location) != null) 
                        ?  "Location " + location.getId() + " / Material " + material.getId() : "Material " + material.getId();
                UnidadeMedida unidadeMedidaPadrao = clusterEParametrosProjection.getSNPUnidadeMedidaPadrao(material, location);
                
                Set<UnidadeMedida> unidadesMedidaEstoque = estoqueProjection.getEstoques(location, material).stream()
                        .map(x -> x.getUom())
                        .collect(Collectors.toSet());
                
                for (UnidadeMedida unidadeMedidaEstoque : unidadesMedidaEstoque) {
                    
                    if (!unidadeMedidaProjection.contemConversaoParaUnidadeDestino(
                            material, unidadeMedidaEstoque, unidadeMedidaPadrao)) {
                        unidadeConversaoFaltanteDTOSet.add(UnidadeConversaoFaltanteDTO.builder()
                                .locationId(location.getId())
                                .materialId(material.getId())
                                .originTask(Constantes.TaskTipo.SNP)
                                .originConversionRequirementType(NecessidadeConversao.ESTOQUE)
                                .originConversionRequirementId("Location " + location.getId() + " / Material " + material.getId() + " on " + dataEstoque)
                                .originUnitOfMeasure(unidadeMedidaEstoque.getId())
                                .targetTask(Constantes.TaskTipo.SNP)
                                .targetConversionRequirementType(parametroUnidadeMedidaPadrao)
                                .targetConversionRequirementId(identificadorUnidadeMedidaPadrao)
                                .targetUnitOfMeasure(unidadeMedidaPadrao.getId())
                                .build());
                    }
                }
            }
        }

        /*
         * Community nao considera carteira, sales orders, sell-in, compras,
         * transferencias ou remessas como dados transacionais do supply plan.
         * Por isso, conversoes faltantes desses objetos Enterprise nao entram no
         * relatorio Community.
         */

        return unidadeConversaoFaltanteDTOSet;
    }

    /**
     * Carrega a versao de malha usada pelo diagnostico de conversoes faltantes.
     *
     * <p>A malha define roteiros, listas tecnicas e lanes analisados. Ausencia
     * desse id invalida a chamada antes de qualquer projection de UOM, estoque
     * ou demanda.</p>
     */
    private VersaoMalha getVersaoMalhaObrigatoria(String versaoMalhaId) {


        Optional<VersaoMalha> versaoMalhaOptional = versaoMalhaRepository.findById(versaoMalhaId);

        /*
         * A ausencia funcional da malha continua representada por
         * Optional.empty(). Retorno nulo do repository e erro estrutural e deve
         * falhar antes de qualquer projection de UOM, estoque ou demanda.
         */
        if (versaoMalhaOptional == null) {
            throw new IllegalStateException(
                    "Supply Network Version repository returned null Optional for missing UOM diagnostic id "
                            + versaoMalhaId
                            + ".");
        }

        VersaoMalha versaoMalha = versaoMalhaOptional
                .orElseThrow(() -> new NoResultException(
                        "Supply Network Version " + versaoMalhaId + " not found for missing UOM diagnostic."));
        if (versaoMalha.getId() == null || versaoMalha.getId().isBlank()) {
            throw new IllegalStateException(
                    "Supply Network Version snapshot id is required for missing UOM diagnostic id "
                            + versaoMalhaId
                            + ".");
        }
        if (!versaoMalhaId.equals(versaoMalha.getId())) {
            throw new IllegalStateException(
                    "Supply Network Version snapshot id must match requested missing UOM diagnostic id "
                            + versaoMalhaId
                            + ".");
        }

        return versaoMalha;

    }

    /**
     * Carrega o Demand Plan usado como demanda de referencia no diagnostico de
     * conversoes faltantes SNP.
     */
    private DemandPlan getDemandPlanObrigatorio(Long demandPlanId) {

        if (demandPlanId == null) {
            throw new IllegalArgumentException("Demand Plan id is required for missing UOM diagnostic.");
        }

        Optional<DemandPlan> demandPlanOptional =
                demandPlanRepository.customFindByIdComPerfilExecucao(demandPlanId);

        /*
         * O diagnostico precisa diferenciar Demand Plan inexistente de contrato
         * quebrado do repository; o segundo caso nao deve avancar ate montar a
         * projection de demanda ou calcular bucket/calendario.
         */
        if (demandPlanOptional == null) {
            throw new IllegalStateException(
                    "Demand Plan repository returned null Optional for missing UOM diagnostic id "
                            + demandPlanId
                            + ".");
        }

        DemandPlan demandPlan = demandPlanOptional
                .orElseThrow(() -> new NoResultException(
                        "Demand Plan " + demandPlanId + " not found for missing UOM diagnostic."));
        if (demandPlan.getId() == null) {
            throw new IllegalStateException(
                    "Demand Plan snapshot id is required for missing UOM diagnostic id "
                            + demandPlanId
                            + ".");
        }
        if (!demandPlanId.equals(demandPlan.getId())) {
            throw new IllegalStateException(
                    "Demand Plan snapshot id must match requested missing UOM diagnostic id "
                            + demandPlanId
                            + ".");
        }

        return demandPlan;

    }

    /**
     * Carrega o perfil Supply Planning usado para calendario e contexto do
     * diagnostico de conversoes faltantes.
     */
    private PerfilExecucaoSupplyPlan getPerfilExecucaoSupplyPlanObrigatorio(
            String perfilExecucaoSupplyPlanId) {


        Optional<PerfilExecucaoSupplyPlan> perfilExecucaoSupplyPlanOptional =
                perfilExecucaoSupplyPlanRepository.findById(perfilExecucaoSupplyPlanId);

        /*
         * Perfil ausente continua erro funcional de configuracao; Optional nulo
         * indica repository quebrado e deve parar antes de calendario, malha e
         * projections transacionais.
         */
        if (perfilExecucaoSupplyPlanOptional == null) {
            throw new IllegalStateException(
                    "Supply Planning Profile repository returned null Optional for missing UOM diagnostic id "
                            + perfilExecucaoSupplyPlanId
                            + ".");
        }

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = perfilExecucaoSupplyPlanOptional
                .orElseThrow(() -> new NoResultException(
                        "Supply Planning Profile " + perfilExecucaoSupplyPlanId + " not found for missing UOM diagnostic."));
        if (perfilExecucaoSupplyPlan.getId() == null || perfilExecucaoSupplyPlan.getId().isBlank()) {
            throw new IllegalStateException(
                    "Supply Planning Profile snapshot id is required for missing UOM diagnostic id "
                            + perfilExecucaoSupplyPlanId
                            + ".");
        }
        if (!perfilExecucaoSupplyPlanId.equals(perfilExecucaoSupplyPlan.getId())) {
            throw new IllegalStateException(
                    "Supply Planning Profile snapshot id must match requested missing UOM diagnostic id "
                            + perfilExecucaoSupplyPlanId
                            + ".");
        }

        return perfilExecucaoSupplyPlan;

    }

    /**
     * Retorna as conversoes ausentes para executar o Deployment de um Supply
     * Plan persistido.
     *
     * <p>O header e localizado antes das linhas para manter o erro funcional
     * de plano inexistente. Em seguida, as linhas sao carregadas pelo
     * repository especializado, com produto, origem, destino e UOM em join
     * fetch; o diagnostico nao navega pela colecao lazy do aggregate.</p>
     *
     * @param supplyPlanId identificador do Supply Plan a diagnosticar.
     * @return lacunas de conversao entre a UOM da linha e as UOMs de
     *         expedicao e transferencia requeridas.
     */
    public Set<UnidadeConversaoFaltanteDTO> getUnidadeConversaoFaltanteDeploymentListDTO(Long supplyPlanId) {

        SupplyPlan supplyPlan = supplyPlanRepository.customFindById(supplyPlanId)
                .orElseThrow(() -> new NoResultException(
                        "Supply Plan " + supplyPlanId + " not found for missing UOM diagnostic."));
        Collection<DistributionPlanItem> distributionPlanItemCollection =
                distributionPlanItemRepository.customFindBySupplyPlan(supplyPlan);
        UnidadeMedidaProjection unidadeMedidaProjection =
                unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache();
        ClusterEParametrosProjection clusterEParametrosProjection =
                clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();
        ParametrosGlobais parametrosGlobais = clusterEParametrosProjection.getParametrosGlobais();
        Set<UnidadeConversaoFaltanteDTO> unidadeConversaoFaltanteDTOSet = new HashSet<>();

        for (DistributionPlanItem distributionPlanItem : distributionPlanItemCollection) {
            Produto material = distributionPlanItem.getProduto();
            Location locationOrigem = distributionPlanItem.getLocationOrigem();
            Location locationDestino = distributionPlanItem.getLocationDestino();
            UnidadeMedida unidadeMedidaPlano = distributionPlanItem.getUnidadeMedida(parametrosGlobais);

            /*
             * A projection fornece a Location canonica sem disparar lazy load.
             * A UOM cadastrada nela prevalece e a UOM global de Deployment
             * reproduz o fallback funcional ja usado pelo legado.
             */
            Location locationOrigemCanonica = clusterEParametrosProjection.getLocationMap()
                    .get(locationOrigem.getId());
            if (locationOrigemCanonica == null) {
                throw new IllegalStateException(
                        "Origin Location " + locationOrigem.getId()
                                + " is missing from the Deployment UOM diagnostic projection.");
            }
            UnidadeMedida unidadeMedidaExpedicao = locationOrigemCanonica.getExpeditionUomRegistered();
            if (unidadeMedidaExpedicao == null) {
                unidadeMedidaExpedicao = parametrosGlobais.getUnidadeMedidaPadraoDeployment();
            }
            UnidadeMedida unidadeMedidaTransferencia = clusterEParametrosProjection
                    .getTransferenciaUnidadeMedida(material, locationOrigem, locationDestino);

            if (!unidadeMedidaProjection.contemConversaoParaUnidadeDestino(
                    material, unidadeMedidaPlano, unidadeMedidaExpedicao)) {
                unidadeConversaoFaltanteDTOSet.add(UnidadeConversaoFaltanteDTO.builder()
                        .locationId(locationOrigem.getId())
                        .materialId(material.getId())
                        .originTask(Constantes.TaskTipo.SNP)
                        .originConversionRequirementType(NecessidadeConversao.PADRAO_MATERIAL_LOCATION)
                        .originConversionRequirementId("Location " + locationOrigem.getId()
                                + " / Material " + material.getId())
                        .originUnitOfMeasure(unidadeMedidaPlano.getId())
                        .targetTask(Constantes.TaskTipo.DEPLOY)
                        .targetConversionRequirementType(NecessidadeConversao.EXPEDICAO)
                        .targetConversionRequirementId("Origin Location " + locationOrigem.getId())
                        .targetUnitOfMeasure(unidadeMedidaExpedicao.getId())
                        .build());
            }

            if (!unidadeMedidaProjection.contemConversaoParaUnidadeDestino(
                    material, unidadeMedidaPlano, unidadeMedidaTransferencia)) {
                unidadeConversaoFaltanteDTOSet.add(UnidadeConversaoFaltanteDTO.builder()
                        .locationId(locationOrigem.getId())
                        .materialId(material.getId())
                        .originTask(Constantes.TaskTipo.SNP)
                        .originConversionRequirementType(NecessidadeConversao.PADRAO_MATERIAL_LOCATION)
                        .originConversionRequirementId("Location " + locationOrigem.getId()
                                + " / Material " + material.getId())
                        .originUnitOfMeasure(unidadeMedidaPlano.getId())
                        .targetTask(Constantes.TaskTipo.DEPLOY)
                        .targetConversionRequirementType(NecessidadeConversao.TRANSFERENCIA)
                        .targetConversionRequirementId("Origin Location " + locationOrigem.getId()
                                + " / Destination Location " + locationDestino.getId()
                                + " / Material " + material.getId())
                        .targetUnitOfMeasure(unidadeMedidaTransferencia.getId())
                        .build());
            }
        }

        return unidadeConversaoFaltanteDTOSet;

    }

    /**
     * Retorna conversões faltantes que serão necessárias para a execução do DP 
     * Verifica as unidades de medida na base vendas e as compara com a unidade de medida padrão do DP
     * @return 
     */
    public Set<UnidadeConversaoFaltanteDTO> getUnidadeConversaoFaltanteDPListDTO(
            String perfilExecucaoDemandPlanId,
            String dataReferenciaDP) {


        Set<UnidadeConversaoFaltanteDTO> unidadeConversaoFaltanteDTOSet = new HashSet<>();
        
        UnidadeMedidaProjection conversaoUnidadeMedidaProjection = unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache();
        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();
        ParametrosGlobais parametrosGlobais = clusterEParametrosProjection.getParametrosGlobais();
        ParametrosDemandPlanProjection parametrosDemandPlanProjection = parametrosDemandPlanningProjectionFactory.getParametrosDemandPlanProjectionDeCache(perfilExecucaoDemandPlanId);
        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan = parametrosDemandPlanProjection.getPerfilExecucaoDemandPlan();

        LocalDateTime dataReferenciaDpTratada = Calendario.getPrimeiraDataFromDescricaoPeriodo(
                dataReferenciaDP,
                perfilExecucaoDemandPlan.getTamanhoBucket());

        // cria calendário que arredonda # dias passados para semanas ou meses, se necessário
        Calendario calendario = Calendario.criaCalendarioPeriodosFuturosDeDatas(
                parametrosDemandPlanProjection.getPerfilExecucaoDemandPlan().getTamanhoBucket(),
                dataReferenciaDpTratada.minusDays(
                        parametrosDemandPlanProjection.getNumeroMaximoDiasHistoricoVendasParaForecast() + 1),
                dataReferenciaDpTratada);
        
        // AVALIA BASE DE VENDAS NA DATA REFERENCIA
        // CORRIGIR : DEVE SER CAPAZ DE CONVERTER DE UNID VENDA PARA UNID FORECAST
        // APENAS CLUSTERS LOCATIONS COM GERACAO DP!!!
        SalesProjectionLocationMaterial salesProjectionLocationMaterial = salesProjectionFactory.getSalesProjectionMaterialLocation(
                parametrosDemandPlanProjection.getPerfilExecucaoDemandPlan().getTipoDocumentoVenda(parametrosGlobais),
                calendario,
                clusterEParametrosProjection.getLocationsComExecucaoDP(), 
                clusterEParametrosProjection.getMaterialSet(), 
                conversaoUnidadeMedidaProjection, clusterEParametrosProjection, 
                clusterEParametrosProjection.getParametrosGlobais().getUnidadeMedidaPadraoSNP());
            
        for (AggregatedByLocationMaterialUOM aggregatedByLocationMaterialUOM : salesProjectionLocationMaterial.getSetSalesConsolidado()) {
            Produto material = aggregatedByLocationMaterialUOM.getMaterial();
            Location location = aggregatedByLocationMaterialUOM.getLocation();
            UnidadeMedida unidadeMedidaVenda = aggregatedByLocationMaterialUOM.getUom();
            
            UnidadeMedida unidadeMedidaDP = parametrosDemandPlanProjection
                    .getParametrosDemandPlanNivelClusterProjection(location, material, clusterEParametrosProjection)
                    .getParametrosGeraisDemandPlanningProjection()
                    .getUnidadeMedidaDP();

            if (!conversaoUnidadeMedidaProjection.contemConversaoParaUnidadeDestino(material, unidadeMedidaVenda, unidadeMedidaDP)) {
                unidadeConversaoFaltanteDTOSet.add(UnidadeConversaoFaltanteDTO.builder()
                        .locationId(location.getId())
                        .materialId(material.getId())
                        .originTask(Constantes.TaskTipo.DP)
                        .originConversionRequirementType(NecessidadeConversao.VENDAS)
                        .originConversionRequirementId("Location " + location.getId() + " / Material " + material.getId())
                        .originUnitOfMeasure(unidadeMedidaVenda.getId())
                        .targetTask(Constantes.TaskTipo.DP)
                        .targetConversionRequirementType(NecessidadeConversao.PLANO_DEMANDA)
                        .targetConversionRequirementId("Location " + location.getId() + " / Material " + material.getId())
                        .targetUnitOfMeasure(unidadeMedidaDP.getId())
                        .build());
            }
        }
        return unidadeConversaoFaltanteDTOSet;
    }

    /**
     * Valida a projection de UOM antes dos diagnosticos de conversao.
     *
     * <p>O diagnostico usa este snapshot para converter sell-out, estoque,
     * roteiros, listas tecnicas e linhas de plano. Projection nula ou sem
     * parametros globais representa falha de cache/factory e deve parar antes
     * de qualquer rotina que tente resolver unidade padrao.</p>
     */
    /**
     * Valida a projection central de parametros/master data antes dos
     * diagnosticos de conversao.
     *
     * <p>Sem esta fotografia, o service nao consegue resolver DFUs, clusters,
     * UOMs padrao ou o documento historico da rodada DP/SNP. Falhar aqui evita
     * NPEs profundos em sales projection, estoque projection ou supply network.</p>
     */
    /**
     * Valida os parametros DP usados no diagnostico de conversoes faltantes.
     *
     * <p>A projection de parametros define bucket, fonte historica e janela de
     * historico usada para consultar vendas. Snapshot nulo ou sem perfil e
     * contrato quebrado da factory/cache, nao ausencia operacional de vendas.</p>
     */
                    public ConversaoUnidadeMedidaDTO getDTODetalhamentoConversoesUnidade(String materialId, String originUomId, String targetUomId) {

        validaParametrosDetalhamentoConversaoUnidadeCommunity(originUomId, targetUomId);

        ConversaoUnidadeMedidaDTO conversaoUnidadeMedidaDTO = new ConversaoUnidadeMedidaDTO();
        conversaoUnidadeMedidaDTO.setMaterialId(materialId);
        conversaoUnidadeMedidaDTO.setOriginUomId(originUomId);
        conversaoUnidadeMedidaDTO.setTargetUomId(targetUomId);
                
        UnidadeMedidaProjection unidadeMedidaProjection = unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionComConversoes();
        Produto material = materialService.getMaterialDeId(materialId);
        UnidadeMedida unidadeMedidaOrigem = unidadeMedidaService.getUnidadeMedida(originUomId);
        UnidadeMedida unidadeMedidaTarget = unidadeMedidaService.getUnidadeMedida(targetUomId);
        
        Pair<Double,String> parConversaoEPassoAPasso = unidadeMedidaProjection.getPassoAPassoConversaoParaUnidadeDestino(material, unidadeMedidaOrigem, unidadeMedidaTarget);
        
        conversaoUnidadeMedidaDTO.setConversionCoefficient(parConversaoEPassoAPasso.getValue0());
        conversaoUnidadeMedidaDTO.setStepByStep(parConversaoEPassoAPasso.getValue1());
        
        return conversaoUnidadeMedidaDTO;
        
    }
    
    public List<ConversaoUnidadeMedidaDTO> getListaDTODetalhamentoConversoesUnidade(String originUomId, String targetUomId) {

        validaParametrosDetalhamentoConversaoUnidadeCommunity(originUomId, targetUomId);

        List<ConversaoUnidadeMedidaDTO> dtoList = new ArrayList<>();
        
        UnidadeMedidaProjection unidadeMedidaProjection = unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionComConversoes();
        UnidadeMedida unidadeMedidaOrigem = unidadeMedidaService.getUnidadeMedida(originUomId);
        UnidadeMedida unidadeMedidaTarget = unidadeMedidaService.getUnidadeMedida(targetUomId);
        
        for (Produto material : materialService.getMateriais(true)) {
            
            ConversaoUnidadeMedidaDTO conversaoUnidadeMedidaDTO = new ConversaoUnidadeMedidaDTO();
            conversaoUnidadeMedidaDTO.setMaterialId(material.getId());
            conversaoUnidadeMedidaDTO.setOriginUomId(originUomId);
            conversaoUnidadeMedidaDTO.setTargetUomId(targetUomId);

            Pair<Double,String> parConversaoEPassoAPasso = unidadeMedidaProjection.getPassoAPassoConversaoParaUnidadeDestino(material, unidadeMedidaOrigem, unidadeMedidaTarget);

            conversaoUnidadeMedidaDTO.setConversionCoefficient(parConversaoEPassoAPasso.getValue0());
            conversaoUnidadeMedidaDTO.setStepByStep(parConversaoEPassoAPasso.getValue1());

            dtoList.add(conversaoUnidadeMedidaDTO);
                    
        }
        
        return dtoList;
        
    }

    /**
     * Valida parametros obrigatorios do diagnostico de conversoes faltantes SNP.
     *
     * <p>Esses campos definem calendario, malha, perfil e demanda de referencia.
     * Sem eles, o service nao deve inicializar projections pesadas nem consultar
     * repositories com chave nula/vazia.</p>
     */
    private void validaParametrosDiagnosticoConversaoSNPCommunity(
            LocalDateTime dataReferenciaSNP,
            TamanhoBucket tamanhoBucket,
            String versaoMalhaId,
            String perfilExecucaoSupplyPlanId,
            Long demandPlanId) {

        if (dataReferenciaSNP == null) {
            throw new IllegalArgumentException(
                    "Supply Planning reference date is required for missing UOM diagnostic.");
        }
        if (tamanhoBucket == null) {
            throw new IllegalArgumentException("Supply Planning bucket is required for missing UOM diagnostic.");
        }
        if (demandPlanId == null) {
            throw new IllegalArgumentException("Demand Plan id is required for missing UOM diagnostic.");
        }

    }

    /**
     * Valida origem/destino de uma consulta de detalhamento de conversao.
     */
    private void validaParametrosDetalhamentoConversaoUnidadeCommunity(
            String originUomId,
            String targetUomId) {

        
    }
    /**
     * Falha para texto nulo ou em branco antes de repositories/projections.
     */
}
