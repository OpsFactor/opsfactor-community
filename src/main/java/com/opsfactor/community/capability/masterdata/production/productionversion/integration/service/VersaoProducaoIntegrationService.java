package com.opsfactor.community.capability.masterdata.production.productionversion.integration.service;

import com.opsfactor.community.capability.masterdata.production.productionversion.integration.dto.VersaoProducaoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.production.productionversion.integration.mapper.VersaoProducaoIntegrationMapper;
import com.opsfactor.community.capability.masterdata.production.productionversion.integration.mapper.VersaoProducaoIntegrationSupportData;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.repository.ListaTecnicaRepository;
import com.opsfactor.community.capability.masterdata.production.routing.repository.RoteiroRepository;
import com.opsfactor.community.capability.masterdata.production.productionversion.repository.VersaoProducaoRepository;
import com.opsfactor.community.capability.masterdata.network.location.service.LocationService;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.platform.integration.service.IntegrationServiceInterface;
import com.opsfactor.community.platform.exception.DataUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.opsfactor.community.platform.integration.service.IntegrationSupportDataValidation.getMapaPorIdObrigatorio;

/**
 * Service de integração da entidade única de versão de produção.
 *
 * <p>O contrato referencia {@link VersaoProducao}, {@code Roteiro} e
 * {@code ListaTecnica} pelas abstrações gerais. O Community limita os subtipos
 * disponíveis dos mestres; o service não precisa conhecer essa especialização.</p>
 */
@Service
public class VersaoProducaoIntegrationService implements IntegrationServiceInterface<VersaoProducaoIntegrationDataDto, VersaoProducaoIntegrationDataDto.VersaoProducaoPrimaryKeyIntegrationDTO, VersaoProducao, VersaoProducaoIntegrationSupportData, VersaoProducaoIntegrationMapper, EmptyIntegrationDataFilter> {

    /**
     * Service de locations usado para resolver a location da versão.
     */
    @Autowired
    private LocationService locationService;

    /**
     * Repository dos roteiros operacionais referenciados pela versão.
     */
    @Autowired
    private RoteiroRepository roteiroRepository;

    /**
     * Repository das BOMs operacionais referenciadas pela versão.
     */
    @Autowired
    private ListaTecnicaRepository listaTecnicaRepository;

    /**
     * Repository da relacao Community location/material/roteiro/BOM.
     */
    @Autowired
    private VersaoProducaoRepository versaoProducaoRepository;

    /**
     * Mapper da integração de versão de produção.
     */
    @Autowired
    private VersaoProducaoIntegrationMapper versaoProducaoIntegrationMapper;

    /**
     * Retorna o mapper tipado usado pelo fluxo generico de integracao.
     */
    @Override
    public VersaoProducaoIntegrationMapper getMapper() {

        return versaoProducaoIntegrationMapper;

    }

    /**
     * Persiste versões de produção em lote.
     */
    @Override
    public List<VersaoProducao> saveEntityList(Collection<VersaoProducao> entityList) {

        if (!entityList.isEmpty()) {
            validaSavedEntityCollection(
                    versaoProducaoRepository.saveAll(entityList),
                    "Production Version saved collection",
                    entityList.size());
        }
        return new ArrayList<>();

    }

    /**
     * Remove versões de produção em lote usando delete batch.
     */
    @Override
    public void removeEntityList(Collection<VersaoProducao> entityList) {

        if (!entityList.isEmpty()) {
            versaoProducaoRepository.deleteInBatch(entityList);
        }

    }

    /**
     * Mensagem pública de sucesso da carga de versões.
     */
    @Override
    public String getSaveSuccessMessage() {

        return "Production Version data uploaded";

    }

    /**
     * Carrega support data por id para resolver referências da versão
     * sem lookup por linha.
     */
    @Override
    public VersaoProducaoIntegrationSupportData getSupportData() {

        VersaoProducaoIntegrationSupportData supportData = new VersaoProducaoIntegrationSupportData();

        /*
         * A versão costura cadastros produtivos que precisam ser
         * coerentes entre si. A validacao acontece no support data para que
         * snapshots quebrados de repository/service falhem antes do mapper
         * tentar resolver dependencias de cada linha.
         */
        supportData.mapaVersaoProducaoPorId = getMapaPorIdObrigatorio(
                versaoProducaoRepository.findAll(),
                versaoProducao -> versaoProducao.getId(),
                "Production Version snapshot");
        supportData.mapaLocationPorId = getMapaPorIdObrigatorio(
                locationService.findAllWithoutDefault(),
                location -> location.getId(),
                "Location snapshot");
        supportData.mapaRoteiroPorId = getMapaPorIdObrigatorio(
                roteiroRepository.findAll(),
                roteiro -> roteiro.getId(),
                "Routing snapshot");
        supportData.mapaListaTecnicaPorId = getMapaPorIdObrigatorio(
                listaTecnicaRepository.findAll(),
                listaTecnica -> listaTecnica.getId(),
                "Bill of Materials snapshot");
        return supportData;

    }

    /**
     * Batch padrão da carga de versões.
     */
    @Override
    public int getBatchSize() {

        return 1000;

    }

    /**
     * Busca as versões presentes no lote recebido.
     */
    @Override
    public Collection<VersaoProducao> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(Collection<VersaoProducaoIntegrationDataDto.VersaoProducaoPrimaryKeyIntegrationDTO> dtoBatchList) {

        validaVersaoProducaoPrimaryKeyCollection(dtoBatchList);

        if (dtoBatchList.isEmpty()) {
            return List.of();
        }

        Set<String> idsInBatch = dtoBatchList.stream()
                .map(versaoProducaoPrimaryKeyIntegrationDTO -> versaoProducaoPrimaryKeyIntegrationDTO.id)
                .collect(Collectors.toSet());

        return versaoProducaoRepository.findAllById(idsInBatch);

    }

    /**
     * Valida chaves da versão antes do lookup por id.
     */
    private void validaVersaoProducaoPrimaryKeyCollection(
            Collection<VersaoProducaoIntegrationDataDto.VersaoProducaoPrimaryKeyIntegrationDTO> dtoBatchList) {

        if (dtoBatchList == null) {
            throw new DataUploadException("Production version primary key collection is required.");
        }

        Set<String> idsVersaoProducao = new HashSet<>();
        int index = 0;
        for (VersaoProducaoIntegrationDataDto.VersaoProducaoPrimaryKeyIntegrationDTO versaoProducaoPrimaryKeyIntegrationDTO : dtoBatchList) {
            if (versaoProducaoPrimaryKeyIntegrationDTO == null) {
                throw new DataUploadException(
                        "Production version primary key collection item at index "
                                + index
                                + " is required.");
            }
            if (versaoProducaoPrimaryKeyIntegrationDTO.id == null
                    || versaoProducaoPrimaryKeyIntegrationDTO.id.isBlank()) {
                throw new DataUploadException(
                        "Production version upload primary key must include production version id");
            }
            if (!idsVersaoProducao.add(versaoProducaoPrimaryKeyIntegrationDTO.id)) {
                throw new DataUploadException(
                        "Production version primary key collection item at index "
                                + index
                                + " has duplicated id "
                                + versaoProducaoPrimaryKeyIntegrationDTO.id
                                + ".");
            }
            index++;
        }

    }

    /**
     * Exporta todas as versões persistidas.
     */
    @Override
    public Collection<VersaoProducao> getAllPersistedEntities() {

        return versaoProducaoRepository.customFindAllForIntegrationExport();

    }

}
