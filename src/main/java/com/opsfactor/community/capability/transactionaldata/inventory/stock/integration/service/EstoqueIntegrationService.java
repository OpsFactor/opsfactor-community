package com.opsfactor.community.capability.transactionaldata.inventory.stock.integration.service;

import com.opsfactor.community.capability.transactionaldata.inventory.stock.integration.dto.EstoqueIntegrationDataDto;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.integration.dto.EstoqueIntegrationFiltroDto;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.integration.mapper.EstoqueIntegrationMapper;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.integration.mapper.EstoqueIntegrationSupportData;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.domain.Estoque;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.repository.EstoqueRepository;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.platform.integration.service.IntegrationServiceInterface;
import com.opsfactor.community.platform.exception.DataUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
 * Service de integracao do estoque inicial Community.
 *
 * <p>A extracao/remocao exige filtro de datas para evitar operacoes amplas em
 * dado transacional. O dado salvo continua no contrato simples de snapshot por
 * location/material/data; nenhum detalhe de lote/batch e materializado aqui.</p>
 */
@Component
public class EstoqueIntegrationService implements IntegrationServiceInterface<EstoqueIntegrationDataDto, EstoqueIntegrationDataDto.EstoquePrimaryKeyIntegrationDTO, Estoque, EstoqueIntegrationSupportData, EstoqueIntegrationMapper, EstoqueIntegrationFiltroDto> {

    /**
     * Repository do snapshot de estoque. A persistencia em batch alimenta o
     * estoque inicial do Supply Planning Community sem abrir estoque por lote
     * produtivo, ordens ou custos.
     */
    @Autowired
    private EstoqueRepository estoqueRepository;

    /**
     * Fonte das locations aceitas pelo upload. O filtro por tipo de location
     * existe apenas para extracao/remocao operacional do snapshot.
     */
    @Autowired
    private LocationRepository locationRepository;

    /**
     * Fonte dos materiais aceitos pelo upload. O nome `materialRepository`
     * explicita o contrato de borda mesmo que a entidade fisica continue
     * historicamente chamada de Produto.
     */
    @Autowired
    private ProdutoRepository materialRepository;

    /**
     * Fonte das unidades de medida do snapshot de estoque.
     */
    @Autowired
    private UnidadeMedidaRepository unidadeMedidaRepository;

    /**
     * Mapper que valida e copia o payload de estoque para a entidade
     * transacional Community.
     */
    @Autowired
    private EstoqueIntegrationMapper estoqueIntegrationMapper;

    /**
     * Devolve o mapper usado pela infraestrutura generica de carga.
     */
    @Override
    public EstoqueIntegrationMapper getMapper() {

        return estoqueIntegrationMapper;

    }

    /**
     * Salva o batch de snapshots em uma chamada ao repository e executa flush
     * para materializar eventuais ids/estados gerados ainda dentro do lote.
     */
    @Override
    public List<Estoque> saveEntityList(Collection<Estoque> entityList) {

        if (!entityList.isEmpty()) {
            validaSavedEntityCollection(
                    estoqueRepository.saveAll(entityList),
                    "Inventory saved collection",
                    entityList.size());
            estoqueRepository.flush(); // flush para carregar os valores salvos no lote de importacao
        }
        return new ArrayList<>();

    }

    /**
     * Remove os snapshots selecionados pela infraestrutura de integracao.
     */
    @Override
    public void removeEntityList(Collection<Estoque> entityList) {

        estoqueRepository.deleteAll(entityList);

    }

    /**
     * Materializa os lookups do batch em memoria para evitar N+1 no mapper.
     */
    @Override
    public EstoqueIntegrationSupportData getSupportData() {

        EstoqueIntegrationSupportData estoqueIntegrationSupportData = new EstoqueIntegrationSupportData();

        /*
         * O estoque inicial Community usa location, material e UOM como borda
         * minima do snapshot. Validar antes do mapper separa erro estrutural de
         * support data de referencia inexistente na planilha.
         */
        estoqueIntegrationSupportData.mapaLocationPorId = getMapaPorIdObrigatorio(
                locationRepository.findAll(),
                location -> location.getId(),
                "Location snapshot");
        estoqueIntegrationSupportData.mapaProdutoPorId = getMapaPorIdObrigatorio(
                materialRepository.findAll(),
                material -> material.getId(),
                "Material snapshot");
        estoqueIntegrationSupportData.mapaUomPorId = getMapaPorIdObrigatorio(
                unidadeMedidaRepository.findAll(),
                unidadeMedida -> unidadeMedida.getId(),
                "Unit of Measure snapshot");

        return estoqueIntegrationSupportData;

    }

    /**
     * Tamanho de lote historico para cargas de estoque.
     */
    @Override
    public int getBatchSize() {

        return 5000;

    }

    /**
     * Busca apenas o envelope seguro de snapshots do batch atual. A chave
     * composta completa continua sendo reconciliada pela infraestrutura generica
     * de integracao, mas o repository evita carregar todo o historico.
     */
    @Override
    public Collection<Estoque> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
            Collection<EstoqueIntegrationDataDto.EstoquePrimaryKeyIntegrationDTO> dtoBatchList) {

        Collection<EstoqueIntegrationDataDto.EstoquePrimaryKeyIntegrationDTO> estoquePrimaryKeyDtoCollection =
                validaEstoquePrimaryKeyCollection(dtoBatchList);

        if (estoquePrimaryKeyDtoCollection.isEmpty()) {
            return List.of();
        }

        /*
         * O matching exato entre DTO e entidade acontece no fluxo generico de
         * integracao pela chave composta. Aqui buscamos apenas o menor envelope
         * seguro para o lote atual, evitando carregar todo o estoque historico
         * em cargas grandes de snapshot inicial.
         */
        Set<String> locationIds = estoquePrimaryKeyDtoCollection.stream()
                .map(estoquePrimaryKeyIntegrationDTO -> estoquePrimaryKeyIntegrationDTO.locationId)
                .collect(Collectors.toSet());
        Set<String> materialIds = estoquePrimaryKeyDtoCollection.stream()
                .map(estoquePrimaryKeyIntegrationDTO -> estoquePrimaryKeyIntegrationDTO.materialId)
                .collect(Collectors.toSet());
        LocalDateTime dataInicial = estoquePrimaryKeyDtoCollection.stream()
                .map(estoquePrimaryKeyIntegrationDTO -> estoquePrimaryKeyIntegrationDTO.referenceDate)
                .min(LocalDateTime::compareTo)
                .orElseThrow(() -> new DataUploadException(
                        "Stock upload batch must include at least one reference date"));
        LocalDateTime dataFinal = estoquePrimaryKeyDtoCollection.stream()
                .map(estoquePrimaryKeyIntegrationDTO -> estoquePrimaryKeyIntegrationDTO.referenceDate)
                .max(LocalDateTime::compareTo)
                .orElseThrow(() -> new DataUploadException(
                        "Stock upload batch must include at least one reference date"));

        return estoqueRepository.customFindUploadBatchEnvelope(
                dataInicial,
                dataFinal,
                locationIds,
                materialIds);

    }

    /**
     * Mensagem publica mantida por compatibilidade com a API de upload.
     */
    @Override
    public String getSaveSuccessMessage() {

        return "Stock data saved";

    }

    /**
     * Retorna todos os snapshots apenas para fluxos full-load explicitamente
     * solicitados. Extracoes operacionais devem preferir o filtro por data.
     */
    @Override
    public Collection<Estoque> getAllPersistedEntities() {

        return estoqueRepository.findAll();

    }

    /**
     * Extrai estoque por janela obrigatoria de datas. A exigencia protege a
     * API Community contra leituras amplas de dado transacional por engano.
     */
    @Override
    public Collection<Estoque> getFilteredPersistedEntities(EstoqueIntegrationFiltroDto filtroDto) {

        validaJanelaDatasEstoque(
                filtroDto,
                "Stock extraction must be filtered by start/end dates");
        if (filtroDto.locationType == null || filtroDto.locationType.isEmpty()) {
            return estoqueRepository.findByEstoqueCompositeKeyDataReferenciaBetween(
                    filtroDto.startDate.atStartOfDay(),
                    filtroDto.endDate.plusDays(1).atStartOfDay().minusSeconds(1));
        } else {
            return estoqueRepository.findByEstoqueCompositeKeyDataReferenciaBetweenAndEstoqueCompositeKeyLocationTipoLocationIn(
                    filtroDto.startDate.atStartOfDay(),
                    filtroDto.endDate.plusDays(1).atStartOfDay().minusSeconds(1),
                    filtroDto.locationType);
        }

    }

    /**
     * Remove estoque somente por janela obrigatoria de datas, com filtro
     * opcional de tipo de location. Remocao sem periodo completo e bloqueada
     * para evitar limpeza acidental de toda a serie transacional.
     */
    @Override
    public void removeFilteredPersistedEntities(EstoqueIntegrationFiltroDto filtroDto) {

        validaJanelaDatasEstoque(
                filtroDto,
                "Stock deletion must be filtered by start/end dates");
        if (filtroDto.locationType == null || filtroDto.locationType.isEmpty()) {
            estoqueRepository.removeByEstoqueCompositeKeyDataReferenciaBetween(
                    filtroDto.startDate.atStartOfDay(),
                    filtroDto.endDate.plusDays(1).atStartOfDay().minusSeconds(1));
        } else {
            estoqueRepository.removeByEstoqueCompositeKeyDataReferenciaBetweenAndEstoqueCompositeKeyLocationTipoLocationIn(
                    filtroDto.startDate.atStartOfDay(),
                    filtroDto.endDate.plusDays(1).atStartOfDay().minusSeconds(1),
                    filtroDto.locationType);
        }

    }

    /**
     * Valida a janela fechada usada por extracao e overwrite de snapshots de
     * estoque.
     *
     * <p>Um intervalo invertido nao pode ser tratado como resultado vazio:
     * isso esconderia um erro de integracao e faria uma remocao solicitada
     * parecer bem-sucedida sem alterar a fotografia persistida.</p>
     */
    private void validaJanelaDatasEstoque(
            EstoqueIntegrationFiltroDto filtroDto,
            String mensagemAusenciaPeriodo) {

        if (filtroDto == null || filtroDto.startDate == null || filtroDto.endDate == null) {
            throw new DataUploadException(mensagemAusenciaPeriodo);
        }
        if (filtroDto.endDate.isBefore(filtroDto.startDate)) {
            throw new DataUploadException(
                    "End date must be after or equal to start date.");
        }

    }

    /**
     * Valida as chaves compostas do snapshot recebido antes de calcular o
     * envelope de consulta.
     *
     * <p>O service reduz o batch a conjuntos de locations/materiais e intervalo
     * de datas. Por isso a chave completa repetida precisa falhar antes dessa
     * agregacao, preservando o indice da linha que veio duplicada no arquivo/API.</p>
     */
    private static Collection<EstoqueIntegrationDataDto.EstoquePrimaryKeyIntegrationDTO> validaEstoquePrimaryKeyCollection(
            Collection<EstoqueIntegrationDataDto.EstoquePrimaryKeyIntegrationDTO> estoquePrimaryKeyDtoCollection) {

        if (estoquePrimaryKeyDtoCollection == null) {
            throw new DataUploadException("Stock upload primary key collection is required.");
        }

        Map<String, Map<String, Set<LocalDateTime>>> datasPorLocationEMaterial =
                new HashMap<>();
        int indice = 0;
        for (EstoqueIntegrationDataDto.EstoquePrimaryKeyIntegrationDTO estoquePrimaryKeyIntegrationDTO
                : estoquePrimaryKeyDtoCollection) {
            if (estoquePrimaryKeyIntegrationDTO == null) {
                throw new DataUploadException(
                        "Stock upload primary key collection item at index " + indice + " is required.");
            }
            if (estoquePrimaryKeyIntegrationDTO.locationId == null
                    || estoquePrimaryKeyIntegrationDTO.materialId == null
                    || estoquePrimaryKeyIntegrationDTO.referenceDate == null) {
                throw new DataUploadException("Stock upload primary key must include location, material and reference date");
            }
            if (!datasPorLocationEMaterial
                    .computeIfAbsent(
                            estoquePrimaryKeyIntegrationDTO.locationId,
                            ignored -> new HashMap<>())
                    .computeIfAbsent(
                            estoquePrimaryKeyIntegrationDTO.materialId,
                            ignored -> new HashSet<>())
                    .add(estoquePrimaryKeyIntegrationDTO.referenceDate)) {
                throw new DataUploadException(
                        "Stock upload primary key collection item at index "
                                + indice
                                + " has duplicated key locationId "
                                + estoquePrimaryKeyIntegrationDTO.locationId
                                + " / materialId "
                                + estoquePrimaryKeyIntegrationDTO.materialId
                                + " / referenceDate "
                                + estoquePrimaryKeyIntegrationDTO.referenceDate
                                + ".");
            }
            indice++;
        }

        return estoquePrimaryKeyDtoCollection;

    }

}
