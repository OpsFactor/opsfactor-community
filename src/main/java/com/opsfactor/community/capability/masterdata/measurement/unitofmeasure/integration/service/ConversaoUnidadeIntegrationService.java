package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.service;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.dto.ConversaoUnidadeIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.mapper.ConversaoUnidadeIntegrationMapper;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.mapper.ConversaoUnidadeIntegrationSupportData;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.ConversaoUnidade;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.ConversaoUnidadeRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.platform.integration.service.IntegrationServiceInterface;
import com.opsfactor.community.platform.exception.DataUploadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.opsfactor.community.platform.integration.service.IntegrationSupportDataValidation.getMapaPorIdObrigatorio;

/**
 * Integracao Community de conversoes padrao entre unidades de medida.
 *
 * <p>Estas conversoes sao usadas por Demand/Supply Community para transformar
 * quantidades fisicas. Conversoes especificas por material existem em service
 * proprio; custos, peso/volume logistico e pricing nao fazem parte deste
 * payload.</p>
 */
@Component
@Slf4j
public class ConversaoUnidadeIntegrationService implements IntegrationServiceInterface<ConversaoUnidadeIntegrationDataDto, ConversaoUnidadeIntegrationDataDto.ConversaoUnidadePrimaryKeyIntegrationDTO, ConversaoUnidade, ConversaoUnidadeIntegrationSupportData, ConversaoUnidadeIntegrationMapper,EmptyIntegrationDataFilter> {

    /**
     * Repository das conversoes padrao persistidas.
     */
    @Autowired
    private ConversaoUnidadeRepository conversaoUnidadeRepository;

    /**
     * Repository usado para montar support data e validar ids de unidade.
     */
    @Autowired
    private UnidadeMedidaRepository unidadeMedidaRepository;

    /**
     * Mapper da integracao de conversao padrao entre unidades.
     */
    @Autowired
    private ConversaoUnidadeIntegrationMapper conversaoUnidadeIntegrationMapper;

    /**
     * Retorna o mapper tipado exigido pelo contrato generico de integracao.
     */
    @Override
    public ConversaoUnidadeIntegrationMapper getMapper() {

        return conversaoUnidadeIntegrationMapper;

    }

    /**
     * Persiste conversoes em lote para evitar save individual por linha.
     */
    @Override
    public List<ConversaoUnidade> saveEntityList(Collection<ConversaoUnidade> entityList) {

        if (!entityList.isEmpty()) {
            validaSavedEntityCollection(
                    conversaoUnidadeRepository.saveAll(entityList),
                    "Unit Conversion saved collection",
                    entityList.size());
        }
        return new ArrayList<>();

    }

    /**
     * Remove conversoes em lote quando o payload marcar delete.
     */
    @Override
    public void removeEntityList(Collection<ConversaoUnidade> entityList) {

        if (!entityList.isEmpty()) {
            conversaoUnidadeRepository.deleteAll(entityList);
        }

    }

    /**
     * Monta mapa de unidades de medida por id para o mapper resolver chaves sem
     * consultar o banco por linha.
     */
    @Override
    public ConversaoUnidadeIntegrationSupportData getSupportData() {

        return ConversaoUnidadeIntegrationSupportData.builder()
                /*
                 * Conversoes padrao dependem apenas do cadastro de UOMs. A
                 * validacao aqui distingue snapshot quebrado de UOM inexistente
                 * referenciada por uma linha do arquivo.
                 */
                .uomPorId(getMapaPorIdObrigatorio(
                        unidadeMedidaRepository.findAll(),
                        UnidadeMedida::getId,
                        "Unit of Measure snapshot"))
                .build();

    }

    /**
     * Batch padrao da integracao de conversoes de unidade.
     */
    @Override
    public int getBatchSize() {

        return 1000;

    }

    /**
     * Retorna o snapshot persistido necessario para reconcile por chave.
     */
    @Override
    public Collection<ConversaoUnidade> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(Collection<ConversaoUnidadeIntegrationDataDto.ConversaoUnidadePrimaryKeyIntegrationDTO> dtoBatchList) {

        validaConversaoUnidadePrimaryKeyCollection(dtoBatchList);

        if (dtoBatchList.isEmpty()) {
            return List.of();
        }

        return conversaoUnidadeRepository.findAll();

    }

    /**
     * Valida a chave origem/destino antes de carregar o snapshot completo.
     *
     * <p>Conversoes globais ainda precisam de `findAll()` para reconcile por
     * chave composta, mas lote vazio ou chave quebrada nao devem provocar uma
     * leitura completa desnecessaria nem esconder duplicidade do payload.</p>
     */
    private void validaConversaoUnidadePrimaryKeyCollection(
            Collection<ConversaoUnidadeIntegrationDataDto.ConversaoUnidadePrimaryKeyIntegrationDTO> dtoBatchList) {

        if (dtoBatchList == null) {
            throw new DataUploadException("Default Unit Conversion primary key collection is required.");
        }

        Map<String, Set<String>> unidadesDestinoPorUnidadeOrigem = new HashMap<>();
        int index = 0;
        for (ConversaoUnidadeIntegrationDataDto.ConversaoUnidadePrimaryKeyIntegrationDTO conversaoUnidadePrimaryKeyIntegrationDTO : dtoBatchList) {
            if (conversaoUnidadePrimaryKeyIntegrationDTO == null) {
                throw new DataUploadException(
                        "Default Unit Conversion primary key collection item at index "
                                + index
                                + " is required.");
            }
            if (conversaoUnidadePrimaryKeyIntegrationDTO.originUomId == null
                    || conversaoUnidadePrimaryKeyIntegrationDTO.originUomId.isBlank()
                    || conversaoUnidadePrimaryKeyIntegrationDTO.targetUomId == null
                    || conversaoUnidadePrimaryKeyIntegrationDTO.targetUomId.isBlank()) {
                throw new DataUploadException(
                        "Default Unit Conversion upload primary key must include origin and target UOM");
            }

            if (!unidadesDestinoPorUnidadeOrigem
                    .computeIfAbsent(
                            conversaoUnidadePrimaryKeyIntegrationDTO.originUomId,
                            ignored -> new HashSet<>())
                    .add(conversaoUnidadePrimaryKeyIntegrationDTO.targetUomId)) {
                throw new DataUploadException(
                        "Default Unit Conversion primary key collection item at index "
                                + index
                                + " has duplicated key originUomId "
                                + conversaoUnidadePrimaryKeyIntegrationDTO.originUomId
                                + " / targetUomId "
                                + conversaoUnidadePrimaryKeyIntegrationDTO.targetUomId
                                + ".");
            }
            index++;
        }

    }

    /**
     * Mensagem publica exibida apos salvamento da integracao.
     */
    @Override
    public String getSaveSuccessMessage() {

        return "Default Unit of Measure Conversion data saved";

    }

    /**
     * Exporta todo o cadastro de conversoes padrao.
     */
    @Override
    public Collection<ConversaoUnidade> getAllPersistedEntities() {

        return conversaoUnidadeRepository.findAll();

    }

}
