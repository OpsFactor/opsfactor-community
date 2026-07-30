package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.service;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.dto.ConversaoUnidadeProdutoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.mapper.ConversaoUnidadeProdutoIntegrationMapper;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.mapper.ConversaoUnidadeProdutoIntegrationSupportData;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.ConversaoUnidadeProduto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.ConversaoUnidadeProdutoRepository;
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
 * Service de integracao de conversoes de unidade por material.
 *
 * <p>Conversao material-especifica fica no Community porque projections de
 * Demand/Supply precisam converter quantidades fisicas com seguranca. O service
 * nao expõe preco, custo ou qualquer regra economica Enterprise.</p>
 */
@Component
@Slf4j
public class ConversaoUnidadeProdutoIntegrationService implements IntegrationServiceInterface<ConversaoUnidadeProdutoIntegrationDataDto, ConversaoUnidadeProdutoIntegrationDataDto.ConversaoUnidadeProdutoPrimaryKeyIntegrationDTO, ConversaoUnidadeProduto, ConversaoUnidadeProdutoIntegrationSupportData, ConversaoUnidadeProdutoIntegrationMapper,EmptyIntegrationDataFilter> {

    /**
     * Repository das conversoes material-especificas persistidas.
     */
    @Autowired
    private ConversaoUnidadeProdutoRepository conversaoUnidadeProdutoRepository;

    /**
     * Repository de materiais usado para validar e indexar support data da
     * carga.
     */
    @Autowired
    private ProdutoRepository produtoRepository;

    /**
     * Repository de UOMs usado para validar origem/destino das conversoes.
     */
    @Autowired
    private UnidadeMedidaRepository unidadeMedidaRepository;

    /**
     * Mapper do contrato CSV/JSON de conversao por material.
     */
    @Autowired
    private ConversaoUnidadeProdutoIntegrationMapper conversaoUnidadeProdutoIntegrationMapper;


    /**
     * Retorna o mapper responsavel pela conversao da linha de integracao para
     * a entidade de conversao material-especifica.
     */
    @Override
    public ConversaoUnidadeProdutoIntegrationMapper getMapper() {

        return conversaoUnidadeProdutoIntegrationMapper;

    }

    /**
     * Persiste conversoes por material em lote e valida o snapshot salvo antes
     * de devolver controle para a infraestrutura de upload.
     */
    @Override
    public List<ConversaoUnidadeProduto> saveEntityList(Collection<ConversaoUnidadeProduto> entityList) {

        if (!entityList.isEmpty()) {
            validaSavedEntityCollection(
                    conversaoUnidadeProdutoRepository.saveAll(entityList),
                    "Material Unit Conversion saved collection",
                    entityList.size());
        }
        return new ArrayList<>();

    }

    /**
     * Remove conversoes material-especificas em lote.
     */
    @Override
    public void removeEntityList(Collection<ConversaoUnidadeProduto> entityList) {

        if (!entityList.isEmpty()) {
            conversaoUnidadeProdutoRepository.deleteAll(entityList);
        }

    }

    /**
     * Carrega support data de materiais e UOMs para que o mapper resolva
     * referencias por id sem round-trip por linha.
     */
    @Override
    public ConversaoUnidadeProdutoIntegrationSupportData getSupportData() {

        return ConversaoUnidadeProdutoIntegrationSupportData.builder()
                /*
                 * Conversao material-especifica precisa resolver material e
                 * UOMs antes do mapper. Snapshot quebrado deve falhar aqui, nao
                 * como MissingDependency de uma linha especifica.
                 */
                .materialPorId(getMapaPorIdObrigatorio(
                        produtoRepository.findAll(),
                        Produto::getId,
                        "Material snapshot"))
                .uomPorId(getMapaPorIdObrigatorio(
                        unidadeMedidaRepository.findAll(),
                        UnidadeMedida::getId,
                        "Unit of Measure snapshot"))
                .build();

    }

    /**
     * Batch padrao da integracao de conversoes por material.
     */
    @Override
    public int getBatchSize() {

        return 1000;

    }

    /**
     * Retorna o snapshot persistido usado pela infraestrutura de reconcile por
     * chave primaria de integracao.
     */
    @Override
    public Collection<ConversaoUnidadeProduto> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(Collection<ConversaoUnidadeProdutoIntegrationDataDto.ConversaoUnidadeProdutoPrimaryKeyIntegrationDTO> dtoBatchList) {

        validaConversaoUnidadeProdutoPrimaryKeyCollection(dtoBatchList);

        if (dtoBatchList.isEmpty()) {
            return List.of();
        }

        return conversaoUnidadeProdutoRepository.findAll();

    }

    /**
     * Valida a chave material/origem/destino antes de carregar o snapshot
     * completo de conversoes por material.
     */
    private void validaConversaoUnidadeProdutoPrimaryKeyCollection(
            Collection<ConversaoUnidadeProdutoIntegrationDataDto.ConversaoUnidadeProdutoPrimaryKeyIntegrationDTO> dtoBatchList) {

        if (dtoBatchList == null) {
            throw new DataUploadException("Material Unit Conversion primary key collection is required.");
        }

        Map<String, Map<String, Set<String>>> unidadesDestinoPorMaterialEUnidadeOrigem =
                new HashMap<>();
        int index = 0;
        for (ConversaoUnidadeProdutoIntegrationDataDto.ConversaoUnidadeProdutoPrimaryKeyIntegrationDTO conversaoUnidadeProdutoPrimaryKeyIntegrationDTO : dtoBatchList) {
            if (conversaoUnidadeProdutoPrimaryKeyIntegrationDTO == null) {
                throw new DataUploadException(
                        "Material Unit Conversion primary key collection item at index "
                                + index
                                + " is required.");
            }
            if (conversaoUnidadeProdutoPrimaryKeyIntegrationDTO.materialId == null
                    || conversaoUnidadeProdutoPrimaryKeyIntegrationDTO.materialId.isBlank()
                    || conversaoUnidadeProdutoPrimaryKeyIntegrationDTO.originUomId == null
                    || conversaoUnidadeProdutoPrimaryKeyIntegrationDTO.originUomId.isBlank()
                    || conversaoUnidadeProdutoPrimaryKeyIntegrationDTO.targetUomId == null
                    || conversaoUnidadeProdutoPrimaryKeyIntegrationDTO.targetUomId.isBlank()) {
                throw new DataUploadException(
                        "Material Unit Conversion upload primary key must include material, origin UOM and target UOM");
            }

            if (!unidadesDestinoPorMaterialEUnidadeOrigem
                    .computeIfAbsent(
                            conversaoUnidadeProdutoPrimaryKeyIntegrationDTO.materialId,
                            ignored -> new HashMap<>())
                    .computeIfAbsent(
                            conversaoUnidadeProdutoPrimaryKeyIntegrationDTO.originUomId,
                            ignored -> new HashSet<>())
                    .add(conversaoUnidadeProdutoPrimaryKeyIntegrationDTO.targetUomId)) {
                throw new DataUploadException(
                        "Material Unit Conversion primary key collection item at index "
                                + index
                                + " has duplicated key materialId "
                                + conversaoUnidadeProdutoPrimaryKeyIntegrationDTO.materialId
                                + " / originUomId "
                                + conversaoUnidadeProdutoPrimaryKeyIntegrationDTO.originUomId
                                + " / targetUomId "
                                + conversaoUnidadeProdutoPrimaryKeyIntegrationDTO.targetUomId
                                + ".");
            }
            index++;
        }

    }

    /**
     * Mensagem publica exibida apos salvamento da carga.
     */
    @Override
    public String getSaveSuccessMessage() {

        return "Unit of Measure Conversion by Material data saved";

    }

    /**
     * Exporta todo o cadastro de conversoes material-especificas.
     */
    @Override
    public Collection<ConversaoUnidadeProduto> getAllPersistedEntities() {

        return conversaoUnidadeProdutoRepository.findAll();

    }

}
