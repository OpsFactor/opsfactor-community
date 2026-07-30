package com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.service;

import com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.dto.ListaTecnicaIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.mapper.ListaTecnicaIntegrationMapper;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.mapper.ListaTecnicaIntegrationSupportData;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.repository.ListaTecnicaRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.capability.masterdata.network.location.service.LocationService;
import com.opsfactor.community.capability.masterdata.product.material.service.MaterialService;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.platform.integration.service.IntegrationServiceInterface;
import com.opsfactor.community.platform.exception.DataUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.opsfactor.community.platform.integration.service.IntegrationSupportDataValidation.getMapaPorIdObrigatorio;

/**
 * Service de integracao de listas tecnicas Community.
 *
 * <p>Persiste somente BOM simples com material de saida, quantidade, UOM,
 * location e prioridade. Co-produtos, outputs paralelos e custos associados
 * permanecem recortes Enterprise.</p>
 */
@Component
public class ListaTecnicaIntegrationService implements IntegrationServiceInterface<ListaTecnicaIntegrationDataDto, ListaTecnicaIntegrationDataDto.ListaTecnicaPrimaryKeyIntegrationDTO, ListaTecnica, ListaTecnicaIntegrationSupportData,ListaTecnicaIntegrationMapper,EmptyIntegrationDataFilter> {

    /**
     * Service de locations usado para resolver a location operacional da BOM.
     */
    @Autowired
    private LocationService locationService;

    /**
     * Service de materiais usado para resolver o material output da BOM.
     */
    @Autowired
    private MaterialService materialService;

    /**
     * Repository de unidades de medida usadas na quantidade output.
     */
    @Autowired
    private UnidadeMedidaRepository unidadeMedidaRepository;

    /**
     * Repository do cadastro operacional de BOM/lista tecnica.
     */
    @Autowired
    private ListaTecnicaRepository listaTecnicaRepository;

    /**
     * Mapper da integracao de BOM Community.
     */
    @Autowired
    private ListaTecnicaIntegrationMapper listaTecnicaIntegrationMapper;

    /**
     * Retorna o mapper tipado usado pelo fluxo generico de integracao.
     */
    @Override
    public ListaTecnicaIntegrationMapper getMapper() {

        return listaTecnicaIntegrationMapper;

    }

    /**
     * Persiste BOMs em lote e força flush para componentes e versoes simples
     * importados depois enxergarem as novas listas tecnicas.
     */
    @Override
    public List<ListaTecnica> saveEntityList(Collection<ListaTecnica> entityList) {

        if (!entityList.isEmpty()) {
            validaSavedEntityCollection(
                    listaTecnicaRepository.saveAll(entityList),
                    "BOM saved collection",
                    entityList.size());
            listaTecnicaRepository.flush(); // flush para carregar os valores salvos no lote de importacao
        }
        return new ArrayList<>();

    }

    /**
     * Remove BOMs em lote quando marcadas para exclusao.
     */
    @Override
    public void removeEntityList(Collection<ListaTecnica> entityList) {

        if (!entityList.isEmpty()) {
            listaTecnicaRepository.deleteAll(entityList);
        }

    }

    /**
     * Mensagem publica de sucesso da carga de BOM.
     */
    @Override
    public String getSaveSuccessMessage() {

        return "Bill of Materials Data Saved";

    }

    /**
     * Carrega support data por id para validar o payload sem lookup por linha.
     */
    @Override
    public ListaTecnicaIntegrationSupportData getSupportData() {

        ListaTecnicaIntegrationSupportData supportData = new ListaTecnicaIntegrationSupportData();

        /*
         * Esses mapas sao o snapshot de master data usado por todas as linhas
         * do arquivo. Falhar aqui mantem o erro na borda de support data, antes
         * que o mapper transforme snapshot quebrado em NullPointerException ou
         * mensagem sem contexto funcional.
         */
        supportData.mapaLocationPorId = getMapaPorIdObrigatorio(
                locationService.findAllWithoutDefault(),
                location -> location.getId(),
                "Location snapshot");
        supportData.mapaMaterialPorId = getMapaPorIdObrigatorio(
                materialService.getMateriais(false),
                material -> material.getId(),
                "Material snapshot");
        supportData.mapaUnidadeMedidaPorId = getMapaPorIdObrigatorio(
                unidadeMedidaRepository.findAll(),
                unidadeMedida -> unidadeMedida.getId(),
                "Unit of Measure snapshot");

        return supportData;

    }

    /**
     * Batch padrao da carga de BOM.
     */
    @Override
    public int getBatchSize() {

        return 1000;

    }

    /**
     * Busca apenas BOMs presentes no lote recebido.
     */
    @Override
    public Collection<ListaTecnica> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(Collection<ListaTecnicaIntegrationDataDto.ListaTecnicaPrimaryKeyIntegrationDTO> dtoBatchList) {

        validaListaTecnicaPrimaryKeyCollection(dtoBatchList);

        if (dtoBatchList.isEmpty()) {
            return List.of();
        }

        Set<String> idsInBatch = dtoBatchList.stream()
                .map(dto -> dto.id)
                .collect(Collectors.toSet());

        return listaTecnicaRepository.findAllById(idsInBatch);

    }

    /**
     * Valida chaves de BOM antes da busca em lote.
     *
     * <p>O `Set` usado pelo lookup e apenas uma otimizacao de consulta. A
     * validacao preserva a cardinalidade original do payload para que
     * duplicidades sejam erro funcional, nao deduplicacao silenciosa.</p>
     */
    private void validaListaTecnicaPrimaryKeyCollection(
            Collection<ListaTecnicaIntegrationDataDto.ListaTecnicaPrimaryKeyIntegrationDTO> dtoBatchList) {

        if (dtoBatchList == null) {
            throw new DataUploadException("Bill of Materials primary key collection is required.");
        }

        Set<String> idsListaTecnica = new HashSet<>();
        int index = 0;
        for (ListaTecnicaIntegrationDataDto.ListaTecnicaPrimaryKeyIntegrationDTO listaTecnicaPrimaryKeyIntegrationDTO : dtoBatchList) {
            if (listaTecnicaPrimaryKeyIntegrationDTO == null) {
                throw new DataUploadException(
                        "Bill of Materials primary key collection item at index " + index + " is required.");
            }
            if (listaTecnicaPrimaryKeyIntegrationDTO.id == null
                    || listaTecnicaPrimaryKeyIntegrationDTO.id.isBlank()) {
                throw new DataUploadException("Bill of Materials upload primary key must include BOM id");
            }
            if (!idsListaTecnica.add(listaTecnicaPrimaryKeyIntegrationDTO.id)) {
                throw new DataUploadException(
                        "Bill of Materials primary key collection item at index "
                                + index
                                + " has duplicated id "
                                + listaTecnicaPrimaryKeyIntegrationDTO.id
                                + ".");
            }
            index++;
        }

    }

    /**
     * Exporta todas as BOMs persistidas.
     */
    @Override
    public Collection<ListaTecnica> getAllPersistedEntities() {

        return listaTecnicaRepository.findAll();

    }

}
