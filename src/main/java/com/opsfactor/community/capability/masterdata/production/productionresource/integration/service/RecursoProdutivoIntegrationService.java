package com.opsfactor.community.capability.masterdata.production.productionresource.integration.service;

import com.opsfactor.community.capability.masterdata.production.productionresource.integration.dto.RecursoProdutivoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.production.productionresource.integration.mapper.RecursoProdutivoIntegrationMapper;
import com.opsfactor.community.capability.masterdata.production.productionresource.integration.mapper.RecursoProdutivoIntegrationSupportData;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.productionresource.repository.RecursoProdutivoRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.capability.masterdata.network.location.service.LocationService;
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
 * Service de integracao de recursos produtivos Community.
 *
 * <p>O Community aceita apenas dados operacionais de capacidade em horas por
 * dia. Custos, manutencao, turnos, capacidade por quantidade/UOM e line
 * scheduling ficam no Enterprise.</p>
 */
@Component
public class RecursoProdutivoIntegrationService implements IntegrationServiceInterface<RecursoProdutivoIntegrationDataDto, RecursoProdutivoIntegrationDataDto.RecursoProdutivoPrimaryKeyIntegrationDTO, RecursoProdutivo, RecursoProdutivoIntegrationSupportData,RecursoProdutivoIntegrationMapper,EmptyIntegrationDataFilter> {

    /**
     * Service de locations usado para montar support data sem expor filtros ou
     * agregadores Enterprise.
     */
    @Autowired
    private LocationService locationService;

    /**
     * Repository do cadastro operacional de recursos produtivos.
     */
    @Autowired
    private RecursoProdutivoRepository recursoProdutivoRepository;

    /**
     * Repository de unidades de medida aceitas no payload operacional.
     */
    @Autowired
    private UnidadeMedidaRepository unidadeMedidaRepository;

    /**
     * Mapper da integracao de recursos produtivos Community.
     */
    @Autowired
    private RecursoProdutivoIntegrationMapper recursoProdutivoIntegrationMapper;

    /**
     * Retorna o mapper tipado usado pelo fluxo generico de integracao.
     */
    @Override
    public RecursoProdutivoIntegrationMapper getMapper() {

        return recursoProdutivoIntegrationMapper;

    }

    /**
     * Persiste recursos produtivos em lote e força flush para que a importacao
     * seguinte veja os valores atualizados no mesmo ciclo operacional.
     */
    @Override
    public List<RecursoProdutivo> saveEntityList(Collection<RecursoProdutivo> entityList) {

        if (!entityList.isEmpty()) {
            validaSavedEntityCollection(
                    recursoProdutivoRepository.saveAll(entityList),
                    "Production Resource saved collection",
                    entityList.size());
            recursoProdutivoRepository.flush(); // flush para carregar os valores salvos no lote de importacao
        }
        return new ArrayList<>();

    }

    /**
     * Remove recursos produtivos em lote quando marcados para exclusao.
     */
    @Override
    public void removeEntityList(Collection<RecursoProdutivo> entityList) {

        if (!entityList.isEmpty()) {
            recursoProdutivoRepository.deleteAll(entityList);
        }

    }

    /**
     * Mensagem publica de sucesso da carga de recursos produtivos.
     */
    @Override
    public String getSaveSuccessMessage() {

        return "Production Resource data Saved";

    }

    /**
     * Carrega support data em mapas por id para evitar lookup no banco por
     * linha de arquivo.
     */
    @Override
    public RecursoProdutivoIntegrationSupportData getSupportData() {

        RecursoProdutivoIntegrationSupportData supportData = new RecursoProdutivoIntegrationSupportData();

        /*
         * Recursos produtivos dependem de location e UOM para alimentar a
         * capacidade heuristica. Validar o support data antes da indexacao
         * evita erro generico de stream quando o snapshot base esta quebrado.
         */
        supportData.mapaLocationPorId = getMapaPorIdObrigatorio(
                locationService.findAllWithoutDefault(),
                location -> location.getId(),
                "Location snapshot");
        supportData.mapaUnidadeMedidaPorId = getMapaPorIdObrigatorio(
                unidadeMedidaRepository.findAll(),
                unidadeMedida -> unidadeMedida.getId(),
                "Unit of Measure snapshot");

        return supportData;

    }

    /**
     * Batch padrao da carga de recursos produtivos.
     */
    @Override
    public int getBatchSize() {

        return 1000;

    }

    /**
     * Busca somente recursos produtivos presentes no lote recebido.
     */
    @Override
    public Collection<RecursoProdutivo> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(Collection<RecursoProdutivoIntegrationDataDto.RecursoProdutivoPrimaryKeyIntegrationDTO> dtoBatchList) {

        validaRecursoProdutivoPrimaryKeyCollection(dtoBatchList);

        if (dtoBatchList.isEmpty()) {
            return List.of();
        }

        Set<String> idsInBatch = dtoBatchList.stream()
                .map(dto -> dto.id)
                .collect(Collectors.toSet());

        return recursoProdutivoRepository.findAllById(idsInBatch);

    }

    /**
     * Valida a chave de recurso produtivo antes de reduzi-la para `Set`.
     *
     * <p>Sem essa checagem, duas linhas com a mesma chave seriam colapsadas
     * antes do lookup no repository, escondendo erro de payload logo na borda
     * de integracao.</p>
     */
    private void validaRecursoProdutivoPrimaryKeyCollection(
            Collection<RecursoProdutivoIntegrationDataDto.RecursoProdutivoPrimaryKeyIntegrationDTO> dtoBatchList) {

        if (dtoBatchList == null) {
            throw new DataUploadException("Production resource primary key collection is required.");
        }

        Set<String> idsRecursoProdutivo = new HashSet<>();
        int index = 0;
        for (RecursoProdutivoIntegrationDataDto.RecursoProdutivoPrimaryKeyIntegrationDTO recursoProdutivoPrimaryKeyIntegrationDTO : dtoBatchList) {
            if (recursoProdutivoPrimaryKeyIntegrationDTO == null) {
                throw new DataUploadException(
                        "Production resource primary key collection item at index " + index + " is required.");
            }
            if (recursoProdutivoPrimaryKeyIntegrationDTO.id == null
                    || recursoProdutivoPrimaryKeyIntegrationDTO.id.isBlank()) {
                throw new DataUploadException(
                        "Production resource upload primary key must include production resource id");
            }
            if (!idsRecursoProdutivo.add(recursoProdutivoPrimaryKeyIntegrationDTO.id)) {
                throw new DataUploadException(
                        "Production resource primary key collection item at index "
                                + index
                                + " has duplicated id "
                                + recursoProdutivoPrimaryKeyIntegrationDTO.id
                                + ".");
            }
            index++;
        }

    }

    /**
     * Exporta todos os recursos produtivos persistidos.
     */
    @Override
    public Collection<RecursoProdutivo> getAllPersistedEntities() {

        return recursoProdutivoRepository.findAll();

    }

}
