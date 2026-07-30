package com.opsfactor.community.capability.masterdata.product.material.integration.mapper;

import com.opsfactor.community.capability.masterdata.product.material.integration.dto.ProdutoIntegrationDataDto;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.exception.MissingDependencyDataUploadException;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.MetodosUtilidade;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Mapper Community do cadastro operacional de materiais.
 *
 * <p>A classe mantem o nome fisico `Produto` porque a entidade legada ainda usa
 * esse conceito, mas o contrato publico novo deve ser lido como material.
 * Caracteristicas dinamicas, filtros e agregadores sao Enterprise.</p>
 */
@Component
public class ProdutoIntegrationMapper implements IntegrationMapperInterface<ProdutoIntegrationDataDto, ProdutoIntegrationDataDto.ProdutoPrimaryKeyIntegrationDTO,Produto, ProdutoIntegrationSupportData> {

    /**
     * Headers publicados para upload/download de materiais Community.
     *
     * <p>A lista e imutavel para impedir que uma carga operacional passe a
     * expor caracteristicas dinamicas Enterprise sem service/projection
     * correspondente no Community.</p>
     */
    public static final List<String> processedFileHeaders = List.of(
        "Id",
        "Description",
        "Active (True/False or 1/0) : Default = True if empty",
        "Lifecycle Stage ('Not Launched', 'New', 'Regular', 'Discontinued')",
        "Introduction Date",
        "Discontinuation Date",
        "Default Unit of Measure (SNP)",
        "Sales Unit of Measure (DP)",
        "Default Transfer Unit of Measure (Supply Planning)",
        "Operational Model ('MTS', 'MTO') : Default = MTS if empty");

    /**
     * Retorna a ordem oficial das colunas processadas em arquivo.
     */
    public List<String> getProcessedFileHeaders() {

        return processedFileHeaders;

    }

    @Override
    public ProdutoIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(Produto produto) {
        
        return ProdutoIntegrationDataDto.builder()
                .description(produto.getDescricao())
                .active(produto.getAtivoCadastrado())
                .lifecycleStage(produto.getEstagioCicloVidaCadastrado())
                .operationalModel(produto.getModeloOperacionalCadastrado())
                .introductionDate(produto.getDataIntroducao())
                .discontinuationDate(produto.getDataDescontinuacao())
                .defaultUomId((produto.getUnidadeMedidaPadraoCadastrado() == null) ? null : produto.getUnidadeMedidaPadraoCadastrado().getId())
                .salesUomId((produto.getUnidadeMedidaVendasCadastrado() == null) ? null : produto.getUnidadeMedidaVendasCadastrado().getId())
                .transferUomId((produto.getUnidadeMedidaTransferenciaCadastrado() == null) ? null : produto.getUnidadeMedidaTransferenciaCadastrado().getId())
                .valueByCharacteristic(new HashMap<>())
                .build();
        
    }

    @Override
    public ProdutoIntegrationDataDto.ProdutoPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(Produto produto) {
        return new ProdutoIntegrationDataDto.ProdutoPrimaryKeyIntegrationDTO(
                produto.getId());
    }

    @Override
    public Produto createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            ProdutoIntegrationDataDto.ProdutoPrimaryKeyIntegrationDTO dto,
            ProdutoIntegrationSupportData supportData) {
        
        return new Produto(dto.id);
        
    }
    
    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            Produto produto,
            ProdutoIntegrationDataDto dto,
            ProdutoIntegrationSupportData supportData,
            @Nullable Map<String,MetodoAtualizacaoCampo> camposASobrecrever) {
        
        // seta campos simples ------------------------------------------------------------------------
        if (isUpdateableField("description", camposASobrecrever)) {
            produto.setDescricao(dto.description);
        }
        if (isUpdateableField("active", camposASobrecrever)) {
            produto.setAtivo(dto.active);
        }
        if (isUpdateableField("lifecycleStage", camposASobrecrever)) {
            produto.setEstagioCicloVida(dto.lifecycleStage);
        }
        if (isUpdateableField("operationalModel", camposASobrecrever)) {
            produto.setModeloOperacional(dto.operationalModel);
        }
        if (isUpdateableField("introductionDate", camposASobrecrever)) {
            produto.setDataIntroducao(dto.introductionDate);
        }
        if (isUpdateableField("discontinuationDate", camposASobrecrever)) {
            produto.setDataDescontinuacao(dto.discontinuationDate);
        }
        // seta campos que fazem referência a unidades de medida ------------------------------------------------------------------------
        if (isUpdateableField("defaultUomId", camposASobrecrever)) {
            if (dto.defaultUomId == null) {
                produto.setUnidadeMedidaPadrao(null);
            } else {
                UnidadeMedida unidadeMedidaDefault = Optional.ofNullable(
                        supportData.unidadeMedidaMap
                                .get(dto.defaultUomId))
                        .orElseThrow(() -> new MissingDependencyDataUploadException("Default Unit of Measure " + dto.defaultUomId + " not found", dto));
                produto.setUnidadeMedidaPadrao(unidadeMedidaDefault);
            }
        }
        
        if (isUpdateableField("salesUomId", camposASobrecrever)) {
            if (dto.salesUomId == null) {
                produto.setUnidadeMedidaVendas(null);
            } else {
                UnidadeMedida unidadeMedidaVendas = Optional.ofNullable(
                        supportData.unidadeMedidaMap
                                .get(dto.salesUomId))
                        .orElseThrow(() -> new MissingDependencyDataUploadException("Sales Unit of Measure " + dto.salesUomId + " not found", dto));
                produto.setUnidadeMedidaVendas(unidadeMedidaVendas);
            }
        }
        
        if (isUpdateableField("transferUomId", camposASobrecrever)) {
            if (dto.transferUomId == null) {
                produto.setUnidadeMedidaTransferencia(null);
            } else {
                UnidadeMedida unidadeMedidaTransferencia = Optional.ofNullable(
                        supportData.unidadeMedidaMap
                                .get(dto.transferUomId))
                        .orElseThrow(() -> new MissingDependencyDataUploadException("Transfer Unit of Measure " + dto.transferUomId + " not found", dto));
                produto.setUnidadeMedidaTransferencia(unidadeMedidaTransferencia);
            }
        }

        /*
         * O DTO compartilhado precisa reconhecer os dois campos para que a
         * borda Community produza erro funcional, em vez de ignorar um
         * payload Enterprise de COGS por material.
         */
        if (dto.unitCogs != null || dto.unitCogsUnitOfMeasureId != null) {
            throw new RequiresEnterpriseVersionException("Material unit COGS");
        }
        
        /*
         * Caracteristicas de material sao Enterprise porque alimentam filtros,
         * agregacoes e apresentacoes configuraveis que nao existem no
         * Community. O DTO compartilhado aceita o campo apenas para que a
         * borda Community falhe de forma explicita quando receber payload
         * Enterprise ou arquivo antigo ja convertido para JSON.
         */
        if (dto.valueByCharacteristic != null && !dto.valueByCharacteristic.isEmpty()) {
            throw new RequiresEnterpriseVersionException("Material characteristics");
        }
                
    }

    @Override
    public ProcessedFileRow convertEntityToProcessedFileRow(Produto entity, ProdutoIntegrationSupportData supportData) {
        
        ProcessedFileRow linhaArquivo = new ProcessedFileRow();
        linhaArquivo.addContent(entity.getId());
        linhaArquivo.addContent(entity.getDescricao());
        linhaArquivo.addContent(entity.getAtivoCadastrado());
        linhaArquivo.addContent(entity.getEstagioCicloVidaCadastrado());
        linhaArquivo.addContent(entity.getDataIntroducao());
        linhaArquivo.addContent(entity.getDataDescontinuacao());
        linhaArquivo.addContent((entity.getUnidadeMedidaPadraoCadastrado() == null) ? null : entity.getUnidadeMedidaPadraoCadastrado().getId());
        linhaArquivo.addContent((entity.getUnidadeMedidaVendasCadastrado() == null) ? null : entity.getUnidadeMedidaVendasCadastrado().getId());
        linhaArquivo.addContent((entity.getUnidadeMedidaTransferenciaCadastrado() == null) ? null : entity.getUnidadeMedidaTransferenciaCadastrado().getId());
        linhaArquivo.addContent(entity.getModeloOperacionalCadastrado());
        
        return linhaArquivo;

    }

    /**
     * Implementacao customizada para manter apenas as colunas Community de
     * material. Caracteristicas dinamicas sao Enterprise e, portanto, nao
     * aparecem no template nem no arquivo processado do Community.
     *
     * @param supportData
     * @return
     */
    @Override
    public List<ProcessedFileRow> getFileHeaderRows(ProdutoIntegrationSupportData supportData) {
        
        ProcessedFileRow processedFileRow = new ProcessedFileRow();

        // adiciona colunas-base ao header
        for (String nomeHeader : getProcessedFileHeaders()) {
            processedFileRow.addContent(nomeHeader);
        }
        
        // retorna lista de 1 só elemento (apenas 1 linha cabeçalho)
        return List.of(processedFileRow);
        
    }

    @Override
    public ProdutoIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(ProcessedFileRow processedFileRow, ProdutoIntegrationSupportData supportData) {
        
        String id = processedFileRow.getColumnValueAsString(0);
        String description = processedFileRow.getColumnValueAsString(1);
        Boolean active = processedFileRow.getColumnValueAsBoolean(2);
        Constantes.StatusProduto lifecycleStage = MetodosUtilidade.getValorEnumDeJsonProperty(Constantes.StatusProduto.class, processedFileRow.getColumnValueAsString(3));
        LocalDateTime introductionDate = processedFileRow.getColumnValueAsLocalDateTime(4);
        LocalDateTime discontinuationDate = processedFileRow.getColumnValueAsLocalDateTime(5);
        String defaultUomId = processedFileRow.getColumnValueAsString(6);
        String salesUomId = processedFileRow.getColumnValueAsString(7);
        String transferUomId = processedFileRow.getColumnValueAsString(8);
        Constantes.SNPModeloOperacional operationalModel = MetodosUtilidade.getValorEnumDeJsonProperty(
                Constantes.SNPModeloOperacional.class,
                processedFileRow.getColumnValueAsString(9));
        
        // requer campo id no mínimo
        if (id == null) throw new DataUploadException("Id cannot be empty");
                
        ProdutoIntegrationDataDto dto = ProdutoIntegrationDataDto.builder()
                .description(description)
                .active(active)
                .lifecycleStage(lifecycleStage)
                .operationalModel(operationalModel)
                .introductionDate(introductionDate) 
                .discontinuationDate(discontinuationDate) 
                .defaultUomId(defaultUomId) 
                .salesUomId(salesUomId) 
                .transferUomId(transferUomId)
                .build();                

        return dto;
        
    }

    @Override
    public ProdutoIntegrationDataDto.ProdutoPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(ProcessedFileRow processedFileRow, ProdutoIntegrationSupportData supportData) {
        return new ProdutoIntegrationDataDto.ProdutoPrimaryKeyIntegrationDTO(
                processedFileRow.getColumnValueAsString(0));
    }

}
