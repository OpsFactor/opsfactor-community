package com.opsfactor.community.capability.masterdata.production.routing.integration.service;

import com.opsfactor.community.capability.masterdata.production.routing.integration.dto.RoteiroIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.production.routing.integration.mapper.RoteiroIntegrationMapper;
import com.opsfactor.community.capability.masterdata.production.routing.integration.mapper.RoteiroIntegrationSupportData;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.production.routing.repository.RoteiroRepository;
import com.opsfactor.community.capability.masterdata.network.location.service.LocationService;
import com.opsfactor.community.capability.masterdata.product.material.service.MaterialService;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.service.UnidadeMedidaService;
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
 * Service de integracao dos roteiros operacionais Community.
 *
 * <p>O contrato cobre apenas o cadastro de roteiros consumido pelo Supply
 * Planning heuristico. Setup detalhado, turnos, manutencao, custos e line
 * scheduling pertencem ao Enterprise.</p>
 */
@Component
public class RoteiroIntegrationService implements IntegrationServiceInterface<RoteiroIntegrationDataDto, RoteiroIntegrationDataDto.RoteiroPrimaryKeyIntegrationDTO, Roteiro, RoteiroIntegrationSupportData,RoteiroIntegrationMapper,EmptyIntegrationDataFilter> {

    /**
     * Service de locations usado para resolver a location de producao do
     * roteiro sem consultas por linha.
     */
    @Autowired
    private LocationService locationService;

    /**
     * Service de materiais usado para resolver o material output do roteiro.
     */
    @Autowired
    private MaterialService materialService;

    /** Unidades usadas pela quantidade-base do cabeçalho do roteiro. */
    @Autowired
    private UnidadeMedidaService unidadeMedidaService;

    /**
     * Repository do cadastro operacional de roteiros.
     */
    @Autowired
    private RoteiroRepository roteiroRepository;

    /**
     * Mapper da integracao de roteiros Community.
     */
    @Autowired
    private RoteiroIntegrationMapper roteiroIntegrationMapper;

    /**
     * Retorna o mapper tipado usado pelo fluxo generico de integracao.
     */
    @Override
    public RoteiroIntegrationMapper getMapper() {

        return roteiroIntegrationMapper;

    }

    /**
     * Persiste roteiros em lote e força flush para que operacoes e versoes
     * simples importadas depois enxerguem os novos roteiros.
     */
    @Override
    public List<Roteiro> saveEntityList(Collection<Roteiro> entityList) {

        if (!entityList.isEmpty()) {
            validaSavedEntityCollection(
                    roteiroRepository.saveAll(entityList),
                    "Routing saved collection",
                    entityList.size());
            roteiroRepository.flush(); // flush para carregar os valores salvos no lote de importacao
        }
        return new ArrayList<>();

    }

    /**
     * Remove roteiros em lote quando marcados para exclusao.
     */
    @Override
    public void removeEntityList(Collection<Roteiro> entityList) {

        if (!entityList.isEmpty()) {
            roteiroRepository.deleteAll(entityList);
        }

    }

    /**
     * Mensagem publica de sucesso da carga de roteiros.
     */
    @Override
    public String getSaveSuccessMessage() {

        return "Routing Data Saved";

    }

    /**
     * Carrega locations e materiais em mapas por id para o mapper validar o
     * payload sem lookup unitario.
     */
    @Override
    public RoteiroIntegrationSupportData getSupportData() {

        RoteiroIntegrationSupportData supportData = new RoteiroIntegrationSupportData();

        /*
         * O roteiro Community e a primeira metade da combinacao produtiva
         * roteiro/BOM usada pela versao simples. Validar locations e materiais
         * aqui evita que snapshot quebrado vire erro tardio no mapper.
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
                unidadeMedidaService.getUnidadeMedidaList(),
                unidadeMedida -> unidadeMedida.getId(),
                "Unit of Measure snapshot");

        return supportData;

    }

    /**
     * Batch padrao da carga de roteiros.
     */
    @Override
    public int getBatchSize() {

        return 1000;

    }

    /**
     * Busca apenas roteiros cujos ids aparecem no lote.
     */
    @Override
    public Collection<Roteiro> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(Collection<RoteiroIntegrationDataDto.RoteiroPrimaryKeyIntegrationDTO> dtoBatchList) {

        validaRoteiroPrimaryKeyCollection(dtoBatchList);

        if (dtoBatchList.isEmpty()) {
            return List.of();
        }

        Set<String> idsInBatch = dtoBatchList.stream()
                .map(dto -> dto.id)
                .collect(Collectors.toSet());

        return roteiroRepository.findAllById(idsInBatch);

    }

    /**
     * Valida chaves de roteiro recebidas antes do lookup em lote.
     */
    private void validaRoteiroPrimaryKeyCollection(
            Collection<RoteiroIntegrationDataDto.RoteiroPrimaryKeyIntegrationDTO> dtoBatchList) {

        if (dtoBatchList == null) {
            throw new DataUploadException("Routing primary key collection is required.");
        }

        Set<String> idsRoteiro = new HashSet<>();
        int index = 0;
        for (RoteiroIntegrationDataDto.RoteiroPrimaryKeyIntegrationDTO roteiroPrimaryKeyIntegrationDTO : dtoBatchList) {
            if (roteiroPrimaryKeyIntegrationDTO == null) {
                throw new DataUploadException(
                        "Routing primary key collection item at index " + index + " is required.");
            }
            if (roteiroPrimaryKeyIntegrationDTO.id == null
                    || roteiroPrimaryKeyIntegrationDTO.id.isBlank()) {
                throw new DataUploadException("Routing upload primary key must include routing id");
            }
            if (!idsRoteiro.add(roteiroPrimaryKeyIntegrationDTO.id)) {
                throw new DataUploadException(
                        "Routing primary key collection item at index "
                                + index
                                + " has duplicated id "
                                + roteiroPrimaryKeyIntegrationDTO.id
                                + ".");
            }
            index++;
        }

    }

    /**
     * Exporta todos os roteiros persistidos.
     */
    @Override
    public Collection<Roteiro> getAllPersistedEntities() {

        return roteiroRepository.findAll();

    }

}
