package com.opsfactor.community.capability.masterdata.network.location.integration.mapper;

import com.opsfactor.community.capability.masterdata.network.location.integration.dto.LocationIntegrationDataDto;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract.TipoLocation;
import com.opsfactor.community.platform.exception.MissingDependencyDataUploadException;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.MetodosUtilidade;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapper Community do cadastro operacional de locations.
 *
 * <p>Locations continuam no Community porque participam de Demand/Supply
 * Planning material/location. Campos ligados a mapa, deployment, visibility,
 * filtros/agregadores e caracteristicas dinamicas sao mantidos fora do arquivo
 * publico e rejeitados quando chegam por JSON.</p>
 */
@Component
public class LocationIntegrationMapper implements IntegrationMapperInterface<LocationIntegrationDataDto, LocationIntegrationDataDto.LocationPrimaryKeyIntegrationDTO,Location, LocationIntegrationSupportData> {

    /**
     * Headers publicados para upload/download de locations Community.
     *
     * <p>A lista e imutavel porque estes nomes formam contrato publico de
     * arquivo. Coordenadas, UOM de expedicao, prazo de atendimento e
     * caracteristicas pertencem ao Enterprise.</p>
     */
    public static final List<String> processedFileHeaders = List.of(
        "Location Id",
        "Description",
        "Active (True/False or 1/0) : Default = True if empty",
        "Location Type : 'Internal', 'End Client', 'Supplier', 'Commercial Region' or 'Transshipment Point'. Default = 'Internal'",
        "Country",
        "State",
        "City",
        "Reference Location for Product-Location parameter mirroring",
        "Available in Production Planning Book : true/false or 0/1",
        "Available in Supply Planning Book : true/false or 0/1",
        "Finite production capacity (for constrained plan. default = true) : true/false or 0/1",
        "Default UOM for supply planning (SNP)",
        "Safety Stocks considers indirect demand (default = true) : true/false or 0/1"
    );

    /**
     * Retorna a ordem oficial das colunas processadas em arquivo.
     */
    public List<String> getProcessedFileHeaders() {

        return processedFileHeaders;

    }

    
    @Override
    public LocationIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(Location location) {
        
        return LocationIntegrationDataDto.builder()
                .description(location.getDescricao())
                .active(location.getAtivoCadastrado())
                .locationType(location.getTipoLocationCadastrada())
                .country(location.getPais())
                .state(location.getEstado())
                .city(location.getCidade())
                .latitude(null)
                .longitude(null)
                .availableInProductionPlanningBook(location.getPlanejaProducaoCadastrado())
                .availableInSupplyPlanningBook(location.getPlanejaSupplyCadastrado())
                .finiteProductionCapacity(location.getConsideraRestricaoProducaoCadastrado())
                .defaultSNPUomId((location.getUnidadeMedidaSnpCadastrado() == null) ? null : location.getUnidadeMedidaSnpCadastrado().getId())
                .expeditionUomId(null)
                .economicGroupId(null)
                .referenceLocationForProductLocationParameters(
                        location.getReferenceLocationForProductLocationParameters() == null
                                ? null
                                : location.getReferenceLocationForProductLocationParameters().getId())
                .safetyStockConsiderIndirectDemand(location.getIncluiDemandaIndiretaNoSafetyStockCadastrado())
                .orderFulfillmentTimeDays(null)
                .valueByCharacteristic(new HashMap<>())
                .build();
        
    }

    @Override
    public LocationIntegrationDataDto.LocationPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(Location location) {
        return new LocationIntegrationDataDto.LocationPrimaryKeyIntegrationDTO(
                location.getId());
    }

    @Override
    public Location createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            LocationIntegrationDataDto.LocationPrimaryKeyIntegrationDTO dto,
            LocationIntegrationSupportData supportData) {
        
        return new Location(dto.id);
        
    }
    
    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            Location location,
            LocationIntegrationDataDto dto,
            LocationIntegrationSupportData supportData,
            @Nullable Map<String,MetodoAtualizacaoCampo> camposASobrecrever) {
        
        // seta campos simples
        if (isUpdateableField("description", camposASobrecrever)) {
            location.setDescricao(dto.description);
        }
        if (isUpdateableField("active", camposASobrecrever)) {
            location.setAtivo(dto.active);
        }
        if (isUpdateableField("locationType", camposASobrecrever)) {
            location.setTipoLocation(dto.locationType);
        }
        if (isUpdateableField("country", camposASobrecrever)) {
            location.setPais(dto.country);
        }
        if (isUpdateableField("state", camposASobrecrever)) {
            location.setEstado(dto.state);
        }
        if (isUpdateableField("city", camposASobrecrever)) {
            location.setCidade(dto.city);
        }
        /*
         * Latitude/longitude alimentam mapa, baricentro e visualizacoes
         * geograficas Enterprise. A entidade Community preserva os campos por
         * compatibilidade de schema, mas a API publica nao deve ler nem gravar
         * coordenadas.
         */
        if (isUpdateableField("latitude", camposASobrecrever)
                && dto.latitude != null) {
            throw new RequiresEnterpriseVersionException("Location geographic coordinates");
        }
        if (isUpdateableField("longitude", camposASobrecrever)
                && dto.longitude != null) {
            throw new RequiresEnterpriseVersionException("Location geographic coordinates");
        }
        if (isUpdateableField("availableInProductionPlanningBook", camposASobrecrever)) {
            location.setPlanejaProducao(dto.availableInProductionPlanningBook);
        }
        if (isUpdateableField("availableInSupplyPlanningBook", camposASobrecrever)) {
            location.setPlanejaSupply(dto.availableInSupplyPlanningBook);
        }
        if (isUpdateableField("finiteProductionCapacity", camposASobrecrever)) {
            location.setConsideraRestricaoProducao(dto.finiteProductionCapacity);
        }
        if (isUpdateableField("safetyStockConsiderIndirectDemand", camposASobrecrever)) {
            location.setIncluiDemandaIndiretaNoSafetyStock(dto.safetyStockConsiderIndirectDemand);
        }

        if (isUpdateableField("defaultSNPUomId", camposASobrecrever)) {
            location.setUnidadeMedidaSnp(
                    supportData.unidadeMedidaMap.getOrDefault(dto.defaultSNPUomId, null));
        }
        /*
         * Unidade de expedicao e usada pelo Deployment, que e Enterprise. O
         * layout tabular Community nao publica essa coluna; a validacao existe
         * para payloads JSON ou contratos Enterprise antigos que cheguem nesta
         * borda por engano.
         */
        if (isUpdateableField("expeditionUomId", camposASobrecrever)
                && dto.expeditionUomId != null
                && !dto.expeditionUomId.isBlank()) {
            throw new RequiresEnterpriseVersionException("Location expedition unit of measure");
        }
        /*
         * O DTO compartilha o identificador para que o overlay Enterprise use
         * a FK EconomicGroup da Location. A integração Community, porém, não
         * administra a associação: payload preenchido deve falhar em vez de
         * virar configuração sem comportamento público correspondente.
         */
        if (isUpdateableField("economicGroupId", camposASobrecrever)
                && dto.economicGroupId != null
                && !dto.economicGroupId.isBlank()) {
            throw new RequiresEnterpriseVersionException("Location economic group");
        }
        if (isUpdateableField("referenceLocationForProductLocationParameters", camposASobrecrever)) {
            /*
             * A referencia nula remove explicitamente o vinculo. Para um ID
             * informado, a fotografia batch precisa conter a location: aceitar
             * null aqui esconderia erro de master data e produziria uma
             * parametrizacao de material/location incorreta.
             */
            if (dto.referenceLocationForProductLocationParameters == null) {
                location.setReferenceLocationForProductLocationParameters(null);
            } else {
                Location referenceLocation = supportData.getLocationById(
                        dto.referenceLocationForProductLocationParameters);
                if (referenceLocation == null) {
                    throw new MissingDependencyDataUploadException(
                            "Reference Location "
                                    + dto.referenceLocationForProductLocationParameters
                                    + " not found",
                            dto);
                }
                location.setReferenceLocationForProductLocationParameters(referenceLocation);
            }
        }
        /*
         * Prazo de atendimento pertence a fluxos Enterprise de distribuicao,
         * disponibilidade e visibilidade logistica. O heuristico Community nao
         * consome esse campo; por isso payload preenchido deve falhar em vez
         * de criar uma falsa configuracao operacional.
         */
        if (isUpdateableField("orderFulfillmentTimeDays", camposASobrecrever)
                && dto.orderFulfillmentTimeDays != null) {
            throw new RequiresEnterpriseVersionException("Location order fulfillment time");
        }
        
        /*
         * Caracteristicas de location sao Enterprise porque alimentam filtros,
         * agregacoes e apresentacoes configuraveis que nao existem no
         * Community. O DTO compartilhado aceita o campo apenas para que a
         * borda Community falhe de forma explicita quando receber payload
         * Enterprise ou arquivo antigo ja convertido para JSON.
         */
        if (dto.valueByCharacteristic != null && !dto.valueByCharacteristic.isEmpty()) {
            throw new RequiresEnterpriseVersionException("Location characteristics");
        }
                
    }

    @Override
    public ProcessedFileRow convertEntityToProcessedFileRow(Location entity, LocationIntegrationSupportData supportData) {
        
        ProcessedFileRow linhaArquivo = new ProcessedFileRow();
        linhaArquivo.addContent(entity.getId());
        linhaArquivo.addContent(entity.getDescricao());
        linhaArquivo.addContent(entity.getAtivoCadastrado());
        linhaArquivo.addContent(entity.getTipoLocationCadastrada());
        linhaArquivo.addContent(entity.getPais());
        linhaArquivo.addContent(entity.getEstado());
        linhaArquivo.addContent(entity.getCidade());
        linhaArquivo.addContent(
                entity.getReferenceLocationForProductLocationParameters() == null
                        ? null
                        : entity.getReferenceLocationForProductLocationParameters().getId());
        linhaArquivo.addContent(entity.getPlanejaProducaoCadastrado());
        linhaArquivo.addContent(entity.getPlanejaSupplyCadastrado());
        linhaArquivo.addContent(entity.getConsideraRestricaoProducaoCadastrado());
        linhaArquivo.addContent((entity.getUnidadeMedidaSnpCadastrado() == null) ? null : entity.getUnidadeMedidaSnpCadastrado().getId());
        linhaArquivo.addContent(entity.getIncluiDemandaIndiretaNoSafetyStockCadastrado());
        
        return linhaArquivo;

    }

    /**
     * Implementacao customizada para manter apenas as colunas Community de
     * location. Caracteristicas dinamicas sao Enterprise e, portanto, nao
     * aparecem no template nem no arquivo processado do Community.
     *
     * @param supportData
     * @return
     */
    @Override
    public List<ProcessedFileRow> getFileHeaderRows(LocationIntegrationSupportData supportData) {
        
        ProcessedFileRow processedFileRow = new ProcessedFileRow();

        // adiciona colunas-base ao header
        for (String nomeHeader : getProcessedFileHeaders()) {
            processedFileRow.addContent(nomeHeader);
        }
        
        // retorna lista de 1 só elemento (apenas 1 linha cabeçalho)
        return List.of(processedFileRow);
        
    }

    @Override
    public LocationIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(ProcessedFileRow processedFileRow, LocationIntegrationSupportData supportData) {
                        
        LocationIntegrationDataDto dto = LocationIntegrationDataDto.builder()
                .description(processedFileRow.getColumnValueAsString(1))
                .active(processedFileRow.getColumnValueAsBoolean(2))
                .locationType(MetodosUtilidade.getValorEnumDeJsonProperty(TipoLocation.class, processedFileRow.getColumnValueAsString(3)))
                .country(processedFileRow.getColumnValueAsString(4))
                .state(processedFileRow.getColumnValueAsString(5))
                .city(processedFileRow.getColumnValueAsString(6))
                .referenceLocationForProductLocationParameters(processedFileRow.getColumnValueAsString(7))
                .availableInProductionPlanningBook(processedFileRow.getColumnValueAsBoolean(8))
                .availableInSupplyPlanningBook(processedFileRow.getColumnValueAsBoolean(9))
                .finiteProductionCapacity(processedFileRow.getColumnValueAsBoolean(10))
                .defaultSNPUomId(processedFileRow.getColumnValueAsString(11))
                .safetyStockConsiderIndirectDemand(processedFileRow.getColumnValueAsBoolean(12))
                .build();

        return dto;
        
    }

    @Override
    public LocationIntegrationDataDto.LocationPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(ProcessedFileRow processedFileRow, LocationIntegrationSupportData supportData) {
        return new LocationIntegrationDataDto.LocationPrimaryKeyIntegrationDTO(
                processedFileRow.getColumnValueAsString(0));
    }

}
