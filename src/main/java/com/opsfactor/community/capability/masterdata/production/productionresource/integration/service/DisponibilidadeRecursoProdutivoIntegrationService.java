package com.opsfactor.community.capability.masterdata.production.productionresource.integration.service;

import com.opsfactor.community.capability.masterdata.production.productionresource.integration.dto.DisponibilidadeRecursoProdutivoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.production.productionresource.integration.mapper.DisponibilidadeRecursoProdutivoIntegrationMapper;
import com.opsfactor.community.capability.masterdata.production.productionresource.integration.mapper.DisponibilidadeRecursoProdutivoIntegrationSupportData;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.DisponibilidadeRecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.productionresource.repository.DisponibilidadeRecursoProdutivoRepository;
import com.opsfactor.community.capability.masterdata.production.productionresource.repository.RecursoProdutivoRepository;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.platform.integration.service.IntegrationServiceInterface;
import com.opsfactor.community.platform.exception.DataUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
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
 * Service de integracao da disponibilidade diaria de recurso produtivo
 * Community.
 *
 * <p>Este service e parte do fluxo funcional minimo de Supply Planning
 * restrito: sem horas por recurso/dia, o heuristico nao consegue calcular
 * capacidade produtiva efetiva. Disponibilidade por turnos, capacidade em
 * quantidade/UOM e calendarios produtivos detalhados pertencem ao Enterprise.</p>
 */
@Component
public class DisponibilidadeRecursoProdutivoIntegrationService implements IntegrationServiceInterface<DisponibilidadeRecursoProdutivoIntegrationDataDto, DisponibilidadeRecursoProdutivoIntegrationDataDto.DisponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO, DisponibilidadeRecursoProdutivo, DisponibilidadeRecursoProdutivoIntegrationSupportData, DisponibilidadeRecursoProdutivoIntegrationMapper, EmptyIntegrationDataFilter> {

    /**
     * Repository dos recursos produtivos usados como support data.
     */
    @Autowired
    private RecursoProdutivoRepository recursoProdutivoRepository;

    /**
     * Repository da disponibilidade diaria por recurso produtivo.
     */
    @Autowired
    private DisponibilidadeRecursoProdutivoRepository disponibilidadeRecursoProdutivoRepository;

    /**
     * Mapper da integracao de disponibilidade de recurso produtivo.
     */
    @Autowired
    private DisponibilidadeRecursoProdutivoIntegrationMapper disponibilidadeRecursoProdutivoIntegrationMapper;

    /**
     * Retorna o mapper tipado usado pelo fluxo generico de integracao.
     */
    @Override
    public DisponibilidadeRecursoProdutivoIntegrationMapper getMapper() {

        return disponibilidadeRecursoProdutivoIntegrationMapper;

    }

    /**
     * Persiste disponibilidades diarias em lote.
     */
    @Override
    public List<DisponibilidadeRecursoProdutivo> saveEntityList(Collection<DisponibilidadeRecursoProdutivo> entityList) {

        if (!entityList.isEmpty()) {
            validaSavedEntityCollection(
                    disponibilidadeRecursoProdutivoRepository.saveAll(entityList),
                    "Production Resource Availability saved collection",
                    entityList.size());
        }
        return new ArrayList<>();

    }

    /**
     * Remove disponibilidades diarias em lote.
     */
    @Override
    public void removeEntityList(Collection<DisponibilidadeRecursoProdutivo> entityList) {

        if (!entityList.isEmpty()) {
            disponibilidadeRecursoProdutivoRepository.deleteInBatch(entityList);
        }

    }

    /**
     * Mensagem publica de sucesso da carga de disponibilidade produtiva.
     */
    @Override
    public String getSaveSuccessMessage() {

        return "Production Resources Availability data uploaded";

    }

    /**
     * Carrega recursos produtivos por id para validar payload sem lookup por
     * linha.
     */
    @Override
    public DisponibilidadeRecursoProdutivoIntegrationSupportData getSupportData() {

        DisponibilidadeRecursoProdutivoIntegrationSupportData supportData =
                new DisponibilidadeRecursoProdutivoIntegrationSupportData();

        /*
         * Disponibilidade diaria precisa de recurso produtivo valido antes de
         * converter linhas. Falhar no support data deixa claro que o problema e
         * estrutural do snapshot, nao uma referencia inexistente do arquivo.
         */
        supportData.mapaRecursoProdutivoPorId = getMapaPorIdObrigatorio(
                recursoProdutivoRepository.findAll(),
                recursoProdutivo -> recursoProdutivo.getId(),
                "Production resource snapshot");

        return supportData;

    }

    /**
     * Batch padrao da carga de disponibilidade produtiva.
     */
    @Override
    public int getBatchSize() {

        return 1000;

    }

    /**
     * Busca o intervalo minimo necessario para reconciliar o lote por recurso e
     * data.
     *
     * <p>A janela e expandida em um dia para preservar o comportamento legado
     * de processamento de vizinhanca sem carregar todo o historico.</p>
     */
    @Override
    public Collection<DisponibilidadeRecursoProdutivo> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
            Collection<DisponibilidadeRecursoProdutivoIntegrationDataDto.DisponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO> dtoBatchList) {

        validaDisponibilidadeRecursoProdutivoPrimaryKeyCollection(dtoBatchList);

        if (dtoBatchList.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> idsRecursosProdutivos = dtoBatchList.stream()
                .map(dto -> dto.productionResourceId)
                .collect(Collectors.toSet());
        LocalDate dataReferenciaInicial = dtoBatchList.stream()
                .map(dto -> dto.referenceDate)
                .min(LocalDate::compareTo)
                .orElseThrow(() -> new DataUploadException(
                        "Production resource availability upload batch must include at least one reference date"));
        LocalDate dataReferenciaFinal = dtoBatchList.stream()
                .map(dto -> dto.referenceDate)
                .max(LocalDate::compareTo)
                .orElseThrow(() -> new DataUploadException(
                        "Production resource availability upload batch must include at least one reference date"));

        return disponibilidadeRecursoProdutivoRepository
                .findAllByDisponibilidadeRecursoProdutivoCompositeKeyRecursoProdutivoIdInAndDisponibilidadeRecursoProdutivoCompositeKeyDataReferenciaBetween(
                        idsRecursosProdutivos,
                        dataReferenciaInicial.minusDays(1),
                        dataReferenciaFinal.plusDays(1));

    }

    /**
     * Valida o par recurso/data antes de calcular a janela de reconciliacao.
     */
    private void validaDisponibilidadeRecursoProdutivoPrimaryKeyCollection(
            Collection<DisponibilidadeRecursoProdutivoIntegrationDataDto.DisponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO> dtoBatchList) {

        if (dtoBatchList == null) {
            throw new DataUploadException("Production resource availability primary key collection is required.");
        }

        Map<String, Set<LocalDate>> datasPorRecursoProdutivo = new HashMap<>();
        int index = 0;
        for (DisponibilidadeRecursoProdutivoIntegrationDataDto.DisponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO disponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO : dtoBatchList) {
            if (disponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO == null) {
                throw new DataUploadException(
                        "Production resource availability primary key collection item at index "
                                + index
                                + " is required.");
            }
            if (disponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO.productionResourceId == null
                    || disponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO.productionResourceId.isBlank()
                    || disponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO.referenceDate == null) {
                throw new DataUploadException(
                        "Production resource availability upload primary key must include production resource and reference date");
            }

            if (!datasPorRecursoProdutivo
                    .computeIfAbsent(
                            disponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO.productionResourceId,
                            ignored -> new HashSet<>())
                    .add(disponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO.referenceDate)) {
                throw new DataUploadException(
                        "Production resource availability primary key collection item at index "
                                + index
                                + " has duplicated key productionResourceId "
                                + disponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO.productionResourceId
                                + " / referenceDate "
                                + disponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO.referenceDate
                                + ".");
            }
            index++;
        }

    }

    /**
     * Exporta todas as disponibilidades produtivas persistidas.
     */
    @Override
    public Collection<DisponibilidadeRecursoProdutivo> getAllPersistedEntities() {

        return disponibilidadeRecursoProdutivoRepository.findAll();

    }

}
