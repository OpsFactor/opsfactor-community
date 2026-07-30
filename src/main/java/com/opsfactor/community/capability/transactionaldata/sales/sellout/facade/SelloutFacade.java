package com.opsfactor.community.capability.transactionaldata.sales.sellout.facade;

import com.opsfactor.community.capability.transactionaldata.sales.sellout.facade.dto.SelloutReportDTO;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.facade.dto.SelloutReportParametrosDTO;
import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto.FiltroMaterialLocationDeCombinacaoCaracteristicasDTO;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.facade.dto.FirstAndLastDateDTO;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.integration.dto.SelloutIntegrationFiltroDto;
import com.opsfactor.community.web.dto.template.AgGridColumnDefDTO;
import com.opsfactor.community.web.dto.template.AgGridDTO;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.facade.mapper.SelloutReportMapper;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.domain.Sellout;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjectionFactory;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.repository.SelloutRepository;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.integration.service.SelloutIntegrationService;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço de front para consulta/exportação de sell-out no Community.
 *
 * <p>O contrato Community aceita filtros simples por ids explícitos de
 * material/location. Filtros por características, agregadores ou estruturas de
 * segmentação pertencem ao Enterprise e são bloqueados antes de qualquer carga
 * de projection ou consulta ao repository.</p>
 */
@Service
public class SelloutFacade {

    /**
     * Caminho padrão para consultas sem filtro material/location explícito.
     */
    @Autowired
    private SelloutIntegrationService selloutIntegrationService;
    /**
     * BI em memória usado para resolver ids de material/location e conversões
     * de entidade antes da consulta filtrada.
     */
    @Autowired
    private ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory;
    /**
     * Projection de unidades usada pelo mapper de exportação de sell-out.
     */
    @Autowired
    private UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory;
    /**
     * Repository usado apenas quando a tela envia filtros simples por ids
     * explícitos de material/location.
     */
    @Autowired
    private SelloutRepository selloutRepository;

    public AgGridDTO getSelloutParaExportacaoAgGrid(
            SelloutReportParametrosDTO selloutReportParametrosDTO) {

        validaParametrosRelatorioSelloutCommunity(selloutReportParametrosDTO);

        /*
         * Filtros por caracteristicas sao Enterprise e devem falhar antes de
         * carregar projections ou consultar sell-out. Filtros simples por ids de
         * material/location continuam Community.
         */
        if (selloutReportParametrosDTO.materialLocationFilterDTO != null) {
            validaFiltroCaracteristicasEnterpriseCommunity(selloutReportParametrosDTO.materialLocationFilterDTO);
        }

        validaPeriodoRelatorioSelloutCommunity(selloutReportParametrosDTO);
        
        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();
        UnidadeMedidaProjection unidadeMedidaProjection = unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache();
        
        Collection<Sellout> entidadesSelloutFiltradas;
        
        if (selloutReportParametrosDTO.materialLocationFilterDTO != null) {
            MaterialProjection materialProjection = getMaterialProjectionFiltroIdsCommunity(
                    selloutReportParametrosDTO.materialLocationFilterDTO,
                    clusterEParametrosProjection);
            LocationProjection locationProjection = getLocationProjectionFiltroIdsCommunity(
                    selloutReportParametrosDTO.materialLocationFilterDTO,
                    clusterEParametrosProjection);
            
            entidadesSelloutFiltradas = selloutRepository.customFindByDataVendaBetweenMaterialInAndLocationIn(
                    selloutReportParametrosDTO.startDate.atStartOfDay(), 
                    Calendario.getUltimoSegundoData(selloutReportParametrosDTO.endDate),
                    locationProjection.getLocationsAtivas(), 
                    materialProjection.getMateriaisAtivos());
        } else {
            SelloutIntegrationFiltroDto filtroDto = new SelloutIntegrationFiltroDto();
            filtroDto.startDate = selloutReportParametrosDTO.startDate;
            filtroDto.endDate = selloutReportParametrosDTO.endDate;
            entidadesSelloutFiltradas = selloutIntegrationService.getFilteredPersistedEntities(filtroDto);
        }

        validaEntidadesSelloutRelatorioCommunity(entidadesSelloutFiltradas);

        List<SelloutReportDTO> dtosSelloutFiltrados = entidadesSelloutFiltradas.stream()
                .map(sellout -> SelloutReportMapper.convertEntityToDTO(
                        sellout, clusterEParametrosProjection,
                        unidadeMedidaProjection))
                .collect(Collectors.toList());
        
        return AgGridDTO.<SelloutReportDTO>builder()
                .data(dtosSelloutFiltrados)
                .columnDefs(getColumnDefsSelloutReport())
                .build();
        
    }

    /**
     * Valida a presenca do payload raiz antes de qualquer outra leitura.
     *
     * <p>Quando o payload existe, filtros Enterprise ainda sao bloqueados antes
     * da validacao de periodo para preservar a mensagem funcional de edicao.
     * Por isso esta etapa valida apenas o objeto raiz.</p>
     */
    private void validaParametrosRelatorioSelloutCommunity(
            SelloutReportParametrosDTO selloutReportParametrosDTO) {

        if (selloutReportParametrosDTO == null) {
            throw new IllegalArgumentException("Sell-out report parameters are required");
        }

    }

    /**
     * Valida o periodo solicitado para exportacao de sell-out.
     *
     * <p>O Community nao possui fallback de datas para relatorios historicos:
     * periodo ausente e erro de contrato da tela/API e deve falhar antes de
     * carregar snapshots ou consultar o repository.</p>
     */
    private void validaPeriodoRelatorioSelloutCommunity(
            SelloutReportParametrosDTO selloutReportParametrosDTO) {

        if (selloutReportParametrosDTO.startDate == null) {
            throw new IllegalArgumentException("Sell-out report start date is required");
        }
        if (selloutReportParametrosDTO.endDate == null) {
            throw new IllegalArgumentException("Sell-out report end date is required");
        }
        if (selloutReportParametrosDTO.startDate.isAfter(selloutReportParametrosDTO.endDate)) {
            throw new IllegalArgumentException("Sell-out report start date must be before or equal to end date");
        }

    }

    /**
     * Valida a projection central antes de resolver ids ou converter entidades.
     */
    /**
     * Valida a projection de unidade usada pelo mapper de sell-out.
     */
    /**
     * Valida a colecao retornada pelo caminho de consulta de sell-out.
     *
     * <p>Colecao vazia e ausencia operacional valida de vendas no periodo; item
     * nulo indica snapshot/retorno de repository quebrado e nao pode chegar ao
     * mapper do relatorio.</p>
     */
    private void validaEntidadesSelloutRelatorioCommunity(
            Collection<Sellout> entidadesSelloutFiltradas) {

        if (entidadesSelloutFiltradas == null) {
            throw new IllegalArgumentException("Sell-out report rows collection is required");
        }
        int indiceSellout = 0;
        for (Sellout sellout : entidadesSelloutFiltradas) {
            if (sellout == null) {
                throw new IllegalArgumentException("Sell-out report rows cannot contain null item at index "
                        + indiceSellout);
            }
            indiceSellout++;
        }

    }
    
    /**
     * Valida o filtro usado pela tela/exportacao de sell-out no Community.
     *
     * Filtros por caracteristicas de material/location e estruturas de
     * agregacao pertencem ao Enterprise. O Community aceita apenas listas
     * explicitas de ids de material e location para manter a consulta simples e
     * sem depender do subdominio de caracteristicas.
     */
    private void validaFiltroCaracteristicasEnterpriseCommunity(
            FiltroMaterialLocationDeCombinacaoCaracteristicasDTO filtroMaterialLocationDeCombinacaoCaracteristicasDTO) {

        if (temFiltroCaracteristicaPreenchido(filtroMaterialLocationDeCombinacaoCaracteristicasDTO.valuesByMaterialCharacteristicId)) {
            throw new RequiresEnterpriseVersionException("Material characteristic filters");
        }

        if (temFiltroCaracteristicaPreenchido(filtroMaterialLocationDeCombinacaoCaracteristicasDTO.valuesByLocationCharacteristicId)) {
            throw new RequiresEnterpriseVersionException("Location characteristic filters");
        }

    }

    private boolean temFiltroCaracteristicaPreenchido(
            Map<String, ? extends Collection<String>> valoresPorCaracteristicaId) {

        return valoresPorCaracteristicaId != null
                && valoresPorCaracteristicaId
                        .values()
                        .stream()
                        .anyMatch(valores -> valores != null && !valores.isEmpty());

    }

    private MaterialProjection getMaterialProjectionFiltroIdsCommunity(
            FiltroMaterialLocationDeCombinacaoCaracteristicasDTO filtroMaterialLocationDeCombinacaoCaracteristicasDTO,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        if (filtroMaterialLocationDeCombinacaoCaracteristicasDTO.materialIds == null
                || filtroMaterialLocationDeCombinacaoCaracteristicasDTO.materialIds.isEmpty()) {
            return MaterialProjectionFactory.getMaterialProjectionCompleto(clusterEParametrosProjection);
        }

        Set<Produto> materiais = filtroMaterialLocationDeCombinacaoCaracteristicasDTO
                .materialIds
                .stream()
                .map(clusterEParametrosProjection::getMaterialPersistido)
                .collect(Collectors.toSet());

        return MaterialProjectionFactory.getProjectionSetMateriais(materiais, clusterEParametrosProjection);

    }

    private LocationProjection getLocationProjectionFiltroIdsCommunity(
            FiltroMaterialLocationDeCombinacaoCaracteristicasDTO filtroMaterialLocationDeCombinacaoCaracteristicasDTO,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        if (filtroMaterialLocationDeCombinacaoCaracteristicasDTO.locationIds == null
                || filtroMaterialLocationDeCombinacaoCaracteristicasDTO.locationIds.isEmpty()) {
            return LocationProjectionFactory.getLocationProjectionCompleto(clusterEParametrosProjection);
        }

        Set<Location> locations = filtroMaterialLocationDeCombinacaoCaracteristicasDTO
                .locationIds
                .stream()
                .map(clusterEParametrosProjection::getLocationPersistida)
                .collect(Collectors.toSet());

        return LocationProjectionFactory.getProjectionSetLocations(locations, clusterEParametrosProjection);

    }

    private List<AgGridColumnDefDTO> getColumnDefsSelloutReport() {
        
        List<AgGridColumnDefDTO> columnDefs = new ArrayList<>();
        
        columnDefs.add(AgGridColumnDefDTO.builder()
                .headerName("Document Id")
                .field("documentId")
                .editable(false)
                .filter(AgGridColumnDefDTO.FilterType.TEXTO)
                .build());
        
        columnDefs.add(AgGridColumnDefDTO.builder()
                .headerName("Reference Date")
                .field("referenceDate")
                .editable(false)
                .filter(AgGridColumnDefDTO.FilterType.GERAL)
                .build());
        
        columnDefs.add(AgGridColumnDefDTO.builder()
                .headerName("Origin Location Id")
                .field("originLocationId")
                .editable(false)
                .filter(AgGridColumnDefDTO.FilterType.TEXTO)
                .build());

        columnDefs.add(AgGridColumnDefDTO.builder()
                .headerName("Material Id")
                .field("materialId")
                .editable(false)
                .filter(AgGridColumnDefDTO.FilterType.TEXTO)
                .build());

        columnDefs.add(AgGridColumnDefDTO.builder()
                .headerName("Sellout document UOM")
                .field("uomId")
                .editable(false)
                .filter(AgGridColumnDefDTO.FilterType.TEXTO)
                .build());
        
        columnDefs.add(AgGridColumnDefDTO.builder()
                .headerName("Quantity (in sellout document UOM)")
                .field("quantity")
                .editable(false)
                .filter(AgGridColumnDefDTO.FilterType.NUMERO)
                .build());

        return columnDefs;
        
    }

    public FirstAndLastDateDTO getSelloutFirstAndLastDateDTO() {

        return new FirstAndLastDateDTO(
                selloutRepository.customFindPrimeiroSellout(),
                selloutRepository.customFindUltimoSellout());

    }
    
}
