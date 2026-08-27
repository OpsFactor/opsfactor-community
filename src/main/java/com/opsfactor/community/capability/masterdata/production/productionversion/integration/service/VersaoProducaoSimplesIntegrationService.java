package com.opsfactor.community.capability.masterdata.production.productionversion.integration.service;

import com.opsfactor.community.capability.masterdata.production.productionversion.integration.dto.VersaoProducaoSimplesIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.production.productionversion.integration.mapper.VersaoProducaoSimplesIntegrationMapper;
import com.opsfactor.community.capability.masterdata.production.productionversion.integration.mapper.VersaoProducaoSimplesIntegrationSupportData;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducaoSimples;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.repository.ListaTecnicaRepository;
import com.opsfactor.community.capability.masterdata.production.routing.repository.RoteiroRepository;
import com.opsfactor.community.capability.masterdata.production.productionversion.repository.VersaoProducaoSimplesRepository;
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
 * Service de integracao da versao de producao simples Community.
 *
 * <p>Este service persiste apenas a relacao location/material/roteiro/BOM usada
 * pelo heuristico. Parallel routing/output e escolha avancada de versoes devem
 * ser implementados no Enterprise, nao por extensao silenciosa deste contrato.</p>
 */
@Service
public class VersaoProducaoSimplesIntegrationService implements IntegrationServiceInterface<VersaoProducaoSimplesIntegrationDataDto, VersaoProducaoSimplesIntegrationDataDto.VersaoProducaoSimplesPrimaryKeyIntegrationDTO, VersaoProducaoSimples, VersaoProducaoSimplesIntegrationSupportData, VersaoProducaoSimplesIntegrationMapper, EmptyIntegrationDataFilter> {

    /**
     * Service de locations usado para resolver a location da versao simples.
     */
    @Autowired
    private LocationService locationService;

    /**
     * Repository dos roteiros operacionais referenciados pela versao simples.
     */
    @Autowired
    private RoteiroRepository roteiroRepository;

    /**
     * Repository das BOMs operacionais referenciadas pela versao simples.
     */
    @Autowired
    private ListaTecnicaRepository listaTecnicaRepository;

    /**
     * Repository da relacao Community location/material/roteiro/BOM.
     */
    @Autowired
    private VersaoProducaoSimplesRepository versaoProducaoSimplesRepository;

    /**
     * Mapper da integracao de versao de producao simples.
     */
    @Autowired
    private VersaoProducaoSimplesIntegrationMapper versaoProducaoSimplesIntegrationMapper;

    /**
     * Retorna o mapper tipado usado pelo fluxo generico de integracao.
     */
    @Override
    public VersaoProducaoSimplesIntegrationMapper getMapper() {

        return versaoProducaoSimplesIntegrationMapper;

    }

    /**
     * Persiste versoes simples em lote.
     */
    @Override
    public List<VersaoProducaoSimples> saveEntityList(Collection<VersaoProducaoSimples> entityList) {

        if (!entityList.isEmpty()) {
            validaSavedEntityCollection(
                    versaoProducaoSimplesRepository.saveAll(entityList),
                    "Simple Production Version saved collection",
                    entityList.size());
        }
        return new ArrayList<>();

    }

    /**
     * Remove versoes simples em lote usando delete batch.
     */
    @Override
    public void removeEntityList(Collection<VersaoProducaoSimples> entityList) {

        if (!entityList.isEmpty()) {
            versaoProducaoSimplesRepository.deleteInBatch(entityList);
        }

    }

    /**
     * Mensagem publica de sucesso da carga de versoes simples.
     */
    @Override
    public String getSaveSuccessMessage() {

        return "Simple Production Version data uploaded";

    }

    /**
     * Carrega support data por id para resolver referencias da versao simples
     * sem lookup por linha.
     */
    @Override
    public VersaoProducaoSimplesIntegrationSupportData getSupportData() {

        VersaoProducaoSimplesIntegrationSupportData supportData = new VersaoProducaoSimplesIntegrationSupportData();

        /*
         * A versao simples costura cadastros produtivos que precisam ser
         * coerentes entre si. A validacao acontece no support data para que
         * snapshots quebrados de repository/service falhem antes do mapper
         * tentar resolver dependencias de cada linha.
         */
        supportData.mapaVersaoProducaoPorId = getMapaPorIdObrigatorio(
                versaoProducaoSimplesRepository.findAll(),
                versaoProducao -> versaoProducao.getId(),
                "Simple Production Version snapshot");
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
     * Batch padrao da carga de versoes simples.
     */
    @Override
    public int getBatchSize() {

        return 1000;

    }

    /**
     * Busca somente versoes simples presentes no lote recebido.
     */
    @Override
    public Collection<VersaoProducaoSimples> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(Collection<VersaoProducaoSimplesIntegrationDataDto.VersaoProducaoSimplesPrimaryKeyIntegrationDTO> dtoBatchList) {

        validaVersaoProducaoSimplesPrimaryKeyCollection(dtoBatchList);

        if (dtoBatchList.isEmpty()) {
            return List.of();
        }

        Set<String> idsInBatch = dtoBatchList.stream()
                .map(versaoProducaoSimplesPrimaryKeyIntegrationDTO -> versaoProducaoSimplesPrimaryKeyIntegrationDTO.id)
                .collect(Collectors.toSet());

        return versaoProducaoSimplesRepository.findAllById(idsInBatch);

    }

    /**
     * Valida chaves da versao simples antes do lookup por id.
     */
    private void validaVersaoProducaoSimplesPrimaryKeyCollection(
            Collection<VersaoProducaoSimplesIntegrationDataDto.VersaoProducaoSimplesPrimaryKeyIntegrationDTO> dtoBatchList) {

        if (dtoBatchList == null) {
            throw new DataUploadException("Simple production version primary key collection is required.");
        }

        Set<String> idsVersaoProducaoSimples = new HashSet<>();
        int index = 0;
        for (VersaoProducaoSimplesIntegrationDataDto.VersaoProducaoSimplesPrimaryKeyIntegrationDTO versaoProducaoSimplesPrimaryKeyIntegrationDTO : dtoBatchList) {
            if (versaoProducaoSimplesPrimaryKeyIntegrationDTO == null) {
                throw new DataUploadException(
                        "Simple production version primary key collection item at index "
                                + index
                                + " is required.");
            }
            if (versaoProducaoSimplesPrimaryKeyIntegrationDTO.id == null
                    || versaoProducaoSimplesPrimaryKeyIntegrationDTO.id.isBlank()) {
                throw new DataUploadException(
                        "Simple production version upload primary key must include simple production version id");
            }
            if (!idsVersaoProducaoSimples.add(versaoProducaoSimplesPrimaryKeyIntegrationDTO.id)) {
                throw new DataUploadException(
                        "Simple production version primary key collection item at index "
                                + index
                                + " has duplicated id "
                                + versaoProducaoSimplesPrimaryKeyIntegrationDTO.id
                                + ".");
            }
            index++;
        }

    }

    /**
     * Exporta todas as versoes simples persistidas.
     */
    @Override
    public Collection<VersaoProducaoSimples> getAllPersistedEntities() {

        return versaoProducaoSimplesRepository.customFindAllForIntegrationExport();

    }

}
