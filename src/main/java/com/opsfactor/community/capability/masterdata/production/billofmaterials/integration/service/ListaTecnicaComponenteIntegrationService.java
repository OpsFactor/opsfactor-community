package com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.service;

import com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.dto.ListaTecnicaComponenteIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.mapper.ListaTecnicaComponenteIntegrationMapper;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.mapper.ListaTecnicaComponenteIntegrationSupportData;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnicaComponente;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.repository.ListaTecnicaComponenteRepository;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.repository.ListaTecnicaRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.capability.masterdata.product.material.service.MaterialService;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.platform.integration.service.IntegrationServiceInterface;
import com.opsfactor.community.platform.exception.DataUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.opsfactor.community.platform.integration.service.IntegrationSupportDataValidation.getMapaPorIdObrigatorio;

/**
 * Service de integracao dos componentes de lista tecnica Community.
 *
 * <p>O Community aceita somente a estrutura basica material output -> materiais
 * componentes, quantidade e UOM. Regras Enterprise de co-produtos, outputs
 * paralelos, custos e politicas avancadas de producao nao entram neste fluxo.</p>
 */
@Component
public class ListaTecnicaComponenteIntegrationService implements IntegrationServiceInterface<ListaTecnicaComponenteIntegrationDataDto, ListaTecnicaComponenteIntegrationDataDto.ListaTecnicaComponentePrimaryKeyIntegrationDTO, ListaTecnicaComponente,ListaTecnicaComponenteIntegrationSupportData,ListaTecnicaComponenteIntegrationMapper,EmptyIntegrationDataFilter> {

    /**
     * Service de materiais usado para resolver componentes da BOM.
     */
    @Autowired
    private MaterialService materialService;

    /**
     * Repository de unidades de medida usadas nas quantidades dos componentes.
     */
    @Autowired
    private UnidadeMedidaRepository unidadeMedidaRepository;

    /**
     * Repository das BOMs pai referenciadas pelos componentes.
     */
    @Autowired
    private ListaTecnicaRepository listaTecnicaRepository;

    /**
     * Repository dos componentes de BOM.
     */
    @Autowired
    private ListaTecnicaComponenteRepository listaTecnicaComponenteRepository;

    /**
     * Mapper da integracao de componentes de BOM Community.
     */
    @Autowired
    private ListaTecnicaComponenteIntegrationMapper listaTecnicaComponenteIntegrationMapper;

    /**
     * Retorna o mapper tipado usado pelo fluxo generico de integracao.
     */
    @Override
    public ListaTecnicaComponenteIntegrationMapper getMapper() {

        return listaTecnicaComponenteIntegrationMapper;

    }

    /**
     * Persiste componentes de BOM em lote e força flush para a importacao
     * corrente trabalhar com valores atualizados.
     */
    @Override
    public List<ListaTecnicaComponente> saveEntityList(Collection<ListaTecnicaComponente> entityList) {

        if (!entityList.isEmpty()) {
            validaSavedEntityCollection(
                    listaTecnicaComponenteRepository.saveAll(entityList),
                    "BOM Component saved collection",
                    entityList.size());
            listaTecnicaComponenteRepository.flush(); // flush para carregar os valores salvos no lote de importacao
        }
        return new ArrayList<>();

    }

    /**
     * Remove componentes de BOM em lote quando marcados para exclusao.
     */
    @Override
    public void removeEntityList(Collection<ListaTecnicaComponente> entityList) {

        if (!entityList.isEmpty()) {
            listaTecnicaComponenteRepository.deleteAll(entityList);
        }

    }

    /**
     * Mensagem publica de sucesso da carga de componentes de BOM.
     */
    @Override
    public String getSaveSuccessMessage() {

        return "Bill of Materials Components Data Saved";

    }

    /**
     * Carrega support data por id para validar material, BOM e UOM sem lookup
     * por linha.
     */
    @Override
    public ListaTecnicaComponenteIntegrationSupportData getSupportData() {

        ListaTecnicaComponenteIntegrationSupportData supportData = new ListaTecnicaComponenteIntegrationSupportData();

        /*
         * Componentes de BOM dependem de tres snapshots: BOM pai, material
         * componente e UOM. Validar antes da indexacao evita que dados de apoio
         * quebrados avancem para o mapper como erro generico de stream ou NPE.
         */
        supportData.mapaMaterialPorId = getMapaPorIdObrigatorio(
                materialService.getMateriais(false),
                material -> material.getId(),
                "Material snapshot");
        supportData.mapaListaTecnicaPorId = getMapaPorIdObrigatorio(
                listaTecnicaRepository.findAll(),
                listaTecnica -> listaTecnica.getId(),
                "Bill of Materials snapshot");
        supportData.mapaUnidadeMedidaPorId = getMapaPorIdObrigatorio(
                unidadeMedidaRepository.findAll(),
                unidadeMedida -> unidadeMedida.getId(),
                "Unit of Measure snapshot");

        return supportData;

    }

    /**
     * Batch padrao da carga de componentes de BOM.
     */
    @Override
    public int getBatchSize() {

        return 1000;

    }

    /**
     * Busca os componentes das BOMs presentes no lote para reconciliar snapshot
     * por lista tecnica.
     */
    @Override
    public Collection<ListaTecnicaComponente> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(Collection<ListaTecnicaComponenteIntegrationDataDto.ListaTecnicaComponentePrimaryKeyIntegrationDTO> dtoBatchList) {

        validaListaTecnicaComponentePrimaryKeyCollection(dtoBatchList);

        if (dtoBatchList.isEmpty()) {
            return List.of();
        }

        Set<String> bomIdsInBatch = dtoBatchList.stream()
                .map(dto -> dto.bomId)
                .collect(Collectors.toSet());

        return listaTecnicaComponenteRepository.findAllByListaTecnicaComponenteCompositeKeyListaTecnicaIdIn(bomIdsInBatch);

    }

    /**
     * Valida a chave composta BOM/material componente antes de reduzi-la para
     * a lista de BOMs usadas no lookup.
     *
     * <p>O repository busca todos os componentes das BOMs afetadas para
     * reconciliar inclusoes, updates e exclusoes. Isso nao torna duplicidades
     * do mesmo componente validas no payload recebido.</p>
     */
    private void validaListaTecnicaComponentePrimaryKeyCollection(
            Collection<ListaTecnicaComponenteIntegrationDataDto.ListaTecnicaComponentePrimaryKeyIntegrationDTO> dtoBatchList) {

        if (dtoBatchList == null) {
            throw new DataUploadException("Bill of Materials component primary key collection is required.");
        }

        Map<String, Set<String>> materiaisComponentesPorListaTecnica = new HashMap<>();
        int index = 0;
        for (ListaTecnicaComponenteIntegrationDataDto.ListaTecnicaComponentePrimaryKeyIntegrationDTO listaTecnicaComponentePrimaryKeyIntegrationDTO : dtoBatchList) {
            if (listaTecnicaComponentePrimaryKeyIntegrationDTO == null) {
                throw new DataUploadException(
                        "Bill of Materials component primary key collection item at index "
                                + index
                                + " is required.");
            }
            if (listaTecnicaComponentePrimaryKeyIntegrationDTO.bomId == null
                    || listaTecnicaComponentePrimaryKeyIntegrationDTO.bomId.isBlank()
                    || listaTecnicaComponentePrimaryKeyIntegrationDTO.componentMaterialId == null
                    || listaTecnicaComponentePrimaryKeyIntegrationDTO.componentMaterialId.isBlank()) {
                throw new DataUploadException(
                        "Bill of Materials component upload primary key must include BOM and component material");
            }

            if (!materiaisComponentesPorListaTecnica
                    .computeIfAbsent(
                            listaTecnicaComponentePrimaryKeyIntegrationDTO.bomId,
                            ignored -> new HashSet<>())
                    .add(listaTecnicaComponentePrimaryKeyIntegrationDTO.componentMaterialId)) {
                throw new DataUploadException(
                        "Bill of Materials component primary key collection item at index "
                                + index
                                + " has duplicated key bomId "
                                + listaTecnicaComponentePrimaryKeyIntegrationDTO.bomId
                                + " / componentMaterialId "
                                + listaTecnicaComponentePrimaryKeyIntegrationDTO.componentMaterialId
                                + ".");
            }
            index++;
        }

    }

    /**
     * Exporta todos os componentes de BOM persistidos.
     */
    @Override
    public Collection<ListaTecnicaComponente> getAllPersistedEntities() {

        return listaTecnicaComponenteRepository.findAll();

    }

    /**
     * Remove todos os componentes de BOM. Mantido para rotinas operacionais que
     * precisam limpar a estrutura antes de recarregar snapshot completo.
     */
    public void removeAll() {

        listaTecnicaComponenteRepository.deleteAll();

    }

}
