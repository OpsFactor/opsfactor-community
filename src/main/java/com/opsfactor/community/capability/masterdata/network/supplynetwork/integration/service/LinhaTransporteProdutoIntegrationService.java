package com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.service;

import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.LinhaTransporteIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.LinhaTransporteProdutoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.LinhaTransporteProdutoIntegrationOptionsDto;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.mapper.LinhaTransporteProdutoIntegrationMapper;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.mapper.LinhaTransporteProdutoIntegrationSupportData;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporteProduto;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.LinhaTransporteProdutoRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.VersaoMalhaRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.platform.integration.service.IntegrationServiceComConfiguracoesInterface;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.exception.MissingDependencyDataUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
 * Service de integracao da transportation lane por material Community.
 *
 * <p>Este service permite granularidade material-especifica da malha para o
 * Supply Planning heuristico. Quando configurado, cria automaticamente a lane
 * origem/destino base ausente, mas sempre no contrato operacional: ativa, sem
 * "enable all materials" e sem distancia/frete/mapa.</p>
 */
@Service
public class LinhaTransporteProdutoIntegrationService implements IntegrationServiceComConfiguracoesInterface<LinhaTransporteProdutoIntegrationDataDto, LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO, LinhaTransporteProduto, LinhaTransporteProdutoIntegrationSupportData, LinhaTransporteProdutoIntegrationMapper, EmptyIntegrationDataFilter, LinhaTransporteProdutoIntegrationOptionsDto> {

    /**
     * Repository da lane por material. A granularidade material/location e
     * Community porque o heuristico precisa conhecer caminhos viaveis por SKU.
     */
    @Autowired
    private LinhaTransporteProdutoRepository linhaTransporteProdutoRepository;

    /**
     * Fonte de materiais aceitos pela carga. Caracteristicas, filtros e
     * agregadores por material seguem bloqueados fora deste service.
     */
    @Autowired
    private ProdutoRepository produtoRepository;

    /**
     * Fonte das versoes de malha associadas a cada linha-material.
     */
    @Autowired
    private VersaoMalhaRepository versaoMalhaRepository;

    /**
     * Fonte das unidades opcionais de lote minimo/multiplo. O Community usa a
     * UOM operacional, mas nao calcula frete/custo logistico.
     */
    @Autowired
    private UnidadeMedidaRepository unidadeMedidaRepository;

    /**
     * Fonte das locations origem/destino. Last mile, frota e mapa permanecem no
     * Enterprise, mas a location operacional e parte do contrato Community.
     */
    @Autowired
    private LocationRepository locationRepository;

    /**
     * Mapper da linha de transporte por material. Ele limita a copia aos campos
     * aceitos no recorte aberto da malha.
     */
    @Autowired
    private LinhaTransporteProdutoIntegrationMapper linhaTransporteProdutoIntegrationMapper;

    /**
     * Service da lane origem/destino base. Usado para normalizar a referencia
     * persistida e, opcionalmente, criar lanes ausentes antes do upsert por
     * material.
     */
    @Autowired
    private LinhaTransporteIntegrationService linhaTransporteIntegrationService;

    /**
     * Devolve o mapper usado pela infraestrutura generica de carga.
     */
    @Override
    public LinhaTransporteProdutoIntegrationMapper getMapper() {

        return linhaTransporteProdutoIntegrationMapper;

    }

    /**
     * Persiste o batch atual de lane/material. Antes do `saveAll`, normaliza a
     * lane pai para a entidade persistida, evitando merge JPA ambigua quando a
     * mesma chave ja existe no banco.
     */
    @Override
    public List<LinhaTransporteProduto> saveEntityList(Collection<LinhaTransporteProduto> entityList) {

        if (!entityList.isEmpty()) {
            normalizaLinhasTransportePersistidas(entityList);
            /*
             * A infraestrutura generica usa as entidades salvas para recompor
             * as chaves do batch. Como a lane pai ja foi normalizada acima,
             * qualquer retorno nulo/quebrado do repository deve parar aqui com
             * mensagem funcional da carga material-specific.
             */
            return validaSavedEntityCollection(
                    linhaTransporteProdutoRepository.saveAll(entityList),
                    "Transportation Lane Material saved collection",
                    entityList.size());
        }
        return new ArrayList<>();

    }

    /**
     * O mapper cria a LinhaTransporte apenas com a chave, mas a persistencia da
     * filha deve apontar para a entidade persistida/canonica para evitar merge
     * inconsistente quando a lane ja existe no contexto JPA.
     *
     * <p>Falhamos explicitamente se a lane pai nao existir. A unica excecao
     * funcional e o fluxo com option `createTransportationLaneIfNotPresent`,
     * tratado antes da conversao/persistencia do batch.</p>
     */
    private void normalizaLinhasTransportePersistidas(Collection<LinhaTransporteProduto> entityList) {

        Set<LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO> transportationLaneKeys = entityList.stream()
                .map(this::getLinhaTransportePrimaryKeyIntegrationDTO)
                .collect(Collectors.toSet());

        Map<LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO, LinhaTransporte> transportationLanesPersistedByPrimaryKey =
                linhaTransporteIntegrationService.getPersistedEntityMapFromPrimaryKeyDtoCollection(transportationLaneKeys);

        entityList.forEach(linhaTransporteProduto -> {
            LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO transportationLanePrimaryKey =
                    getLinhaTransportePrimaryKeyIntegrationDTO(linhaTransporteProduto);
            LinhaTransporte linhaTransportePersistida =
                    transportationLanesPersistedByPrimaryKey.get(transportationLanePrimaryKey);

            if (linhaTransportePersistida == null) {
                throw new MissingDependencyDataUploadException(
                        "Transportation Lane for Supply Network Version "
                                + transportationLanePrimaryKey.supplyNetworkVersionId
                                + ", Origin Location "
                                + transportationLanePrimaryKey.originLocationId
                                + ", Destination Location "
                                + transportationLanePrimaryKey.destinationLocationId
                                + " not found",
                        transportationLanePrimaryKey);
            }

            linhaTransporteProduto.setLinhaTransporte(linhaTransportePersistida);
        });

    }

    /**
     * Converte a entidade filha para a chave publica da lane origem/destino
     * base, reutilizando o mesmo DTO de chave do service pai.
     */
    private LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO getLinhaTransportePrimaryKeyIntegrationDTO(
            LinhaTransporteProduto linhaTransporteProduto) {

        return new LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO(
                linhaTransporteProduto.getVersaoMalha().getId(),
                linhaTransporteProduto.getLocationOrigem().getId(),
                linhaTransporteProduto.getLocationDestino().getId());

    }

    /**
     * Remove em lote as lanes/material selecionadas pela infraestrutura de
     * integracao.
     */
    @Override
    public void removeEntityList(Collection<LinhaTransporteProduto> entityList) {

        if (!entityList.isEmpty()) {
            linhaTransporteProdutoRepository.deleteInBatch(entityList);
        }

    }

    /**
     * Mensagem publica mantida por compatibilidade com a API de upload.
     */
    @Override
    public String getSaveSuccessMessage() {

        return "Transportation Lane - Material data uploaded";

    }

    /**
     * Materializa os lookups do batch em memoria. Isso mantem a validacao do
     * mapper deterministica e evita N+1 em material, versao, locations e UOM.
     */
    @Override
    public LinhaTransporteProdutoIntegrationSupportData getSupportData() {

        LinhaTransporteProdutoIntegrationSupportData linhaTransporteProdutoIntegrationSupportData =
                new LinhaTransporteProdutoIntegrationSupportData();

        /*
         * Linha/material usa a mesma fotografia de locations para origem e
         * destino. A validacao local evita que snapshot quebrado avance para o
         * mapper ou para a criacao automatica de lane pai.
         */
        List<Location> locations = locationRepository.findAll();

        linhaTransporteProdutoIntegrationSupportData.mapaVersaoMalhaPorId = getMapaPorIdObrigatorio(
                versaoMalhaRepository.findAll(),
                versaoMalha -> versaoMalha.getId(),
                "Supply Network Version snapshot");
        linhaTransporteProdutoIntegrationSupportData.mapaLocationOrigemPorId = getMapaPorIdObrigatorio(
                locations,
                location -> location.getId(),
                "Location snapshot");
        linhaTransporteProdutoIntegrationSupportData.mapaLocationDestinoPorId = getMapaPorIdObrigatorio(
                locations,
                location -> location.getId(),
                "Location snapshot");
        linhaTransporteProdutoIntegrationSupportData.mapaProdutoPorId = getMapaPorIdObrigatorio(
                produtoRepository.findAll(),
                material -> material.getId(),
                "Material snapshot");
        linhaTransporteProdutoIntegrationSupportData.mapaUomLoteMinimoMultiploTransportePorId = getMapaPorIdObrigatorio(
                unidadeMedidaRepository.findAll(),
                unidadeMedida -> unidadeMedida.getId(),
                "Unit of Measure snapshot");

        /*
         * A chave null representa a UOM vazia na interface de transportation
         * lane/material. Assim o mapper encontra explicitamente o valor null e
         * atualiza o banco sem tratar ausencia como erro de dependencia.
         */
        linhaTransporteProdutoIntegrationSupportData.mapaUomLoteMinimoMultiploTransportePorId.put(null, null);

        return linhaTransporteProdutoIntegrationSupportData;

    }

    /**
     * Tamanho de lote historico da carga de lane/material.
     */
    @Override
    public int getBatchSize() {

        return 1000;

    }

    /**
     * Busca registros material-especificos ja persistidos para reconciliar o
     * batch atual em uma unica consulta por conjuntos de chaves.
     */
    @Override
    public Collection<LinhaTransporteProduto> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
            Collection<LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO> dtoBatchList) {

        validaLinhaTransporteProdutoPrimaryKeyCollection(dtoBatchList);

        if (dtoBatchList.isEmpty()) {
            return List.of();
        }

        Set<String> versaoMalhaIds = dtoBatchList.stream()
                .map(dto -> dto.supplyNetworkVersionId)
                .collect(Collectors.toSet());
        Set<String> locationOrigemIds = dtoBatchList.stream()
                .map(dto -> dto.originLocationId)
                .collect(Collectors.toSet());
        Set<String> locationDestinoIds = dtoBatchList.stream()
                .map(dto -> dto.destinationLocationId)
                .collect(Collectors.toSet());
        Set<String> materialIds = dtoBatchList.stream()
                .map(dto -> dto.materialId)
                .collect(Collectors.toSet());

        return linhaTransporteProdutoRepository.findByLinhaTransporteProdutoCompositeKeyVersaoIdInAndLinhaTransporteProdutoCompositeKeyProdutoIdInAndLinhaTransporteProdutoCompositeKeyLocationOrigemIdInAndLinhaTransporteProdutoCompositeKeyLocationDestinoIdIn(versaoMalhaIds, materialIds, locationOrigemIds, locationDestinoIds);

    }

    /**
     * Valida a chave da lane/material antes de criar os conjuntos usados no
     * envelope de consulta.
     *
     * <p>A consulta por `IN` em cada dimensao pode trazer um envelope maior que
     * o batch. Isso e intencional para reconcile; o que nao pode acontecer e o
     * payload esconder duas linhas com a mesma chave funcional completa.</p>
     */
    private void validaLinhaTransporteProdutoPrimaryKeyCollection(
            Collection<LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO> dtoBatchList) {

        if (dtoBatchList == null) {
            throw new DataUploadException("Transportation Lane Material primary key collection is required.");
        }

        Map<String, Map<String, Map<String, Set<String>>>>
                materiaisPorVersaoOrigemEDestino = new HashMap<>();
        int index = 0;
        for (LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO linhaTransporteProdutoPrimaryKeyIntegrationDTO : dtoBatchList) {
            if (linhaTransporteProdutoPrimaryKeyIntegrationDTO == null) {
                throw new DataUploadException(
                        "Transportation Lane Material primary key collection item at index "
                                + index
                                + " is required.");
            }
            if (linhaTransporteProdutoPrimaryKeyIntegrationDTO.supplyNetworkVersionId == null
                    || linhaTransporteProdutoPrimaryKeyIntegrationDTO.supplyNetworkVersionId.isBlank()
                    || linhaTransporteProdutoPrimaryKeyIntegrationDTO.originLocationId == null
                    || linhaTransporteProdutoPrimaryKeyIntegrationDTO.originLocationId.isBlank()
                    || linhaTransporteProdutoPrimaryKeyIntegrationDTO.destinationLocationId == null
                    || linhaTransporteProdutoPrimaryKeyIntegrationDTO.destinationLocationId.isBlank()
                    || linhaTransporteProdutoPrimaryKeyIntegrationDTO.materialId == null
                    || linhaTransporteProdutoPrimaryKeyIntegrationDTO.materialId.isBlank()) {
                throw new DataUploadException(
                        "Transportation Lane Material upload primary key must include supply network version, origin location, destination location and material");
            }

            if (!materiaisPorVersaoOrigemEDestino
                    .computeIfAbsent(
                            linhaTransporteProdutoPrimaryKeyIntegrationDTO.supplyNetworkVersionId,
                            ignored -> new HashMap<>())
                    .computeIfAbsent(
                            linhaTransporteProdutoPrimaryKeyIntegrationDTO.originLocationId,
                            ignored -> new HashMap<>())
                    .computeIfAbsent(
                            linhaTransporteProdutoPrimaryKeyIntegrationDTO.destinationLocationId,
                            ignored -> new HashSet<>())
                    .add(linhaTransporteProdutoPrimaryKeyIntegrationDTO.materialId)) {
                throw new DataUploadException(
                        "Transportation Lane Material primary key collection item at index "
                                + index
                                + " has duplicated key supplyNetworkVersionId "
                                + linhaTransporteProdutoPrimaryKeyIntegrationDTO.supplyNetworkVersionId
                                + " / originLocationId "
                                + linhaTransporteProdutoPrimaryKeyIntegrationDTO.originLocationId
                                + " / destinationLocationId "
                                + linhaTransporteProdutoPrimaryKeyIntegrationDTO.destinationLocationId
                                + " / materialId "
                                + linhaTransporteProdutoPrimaryKeyIntegrationDTO.materialId
                                + ".");
            }
            index++;
        }

    }

    /**
     * Retorna todos os registros para operacoes full-load da infraestrutura de
     * integracao.
     */
    @Override
    public Collection<LinhaTransporteProduto> getAllPersistedEntities() {

        return linhaTransporteProdutoRepository.findAll();

    }

    /**
     * Executa a acao opcional de criacao de lane pai no inicio do batch e, em
     * seguida, delega o fluxo padrao de upsert/delete para a interface generica.
     */
    @Override
    public void persistDTOBatchList(
            List<LinhaTransporteProdutoIntegrationDataDto> dtoBatchList,
            Map<LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO, LinhaTransporteProduto> currentlyPersistedEntitiesByPrimaryKey,
            LinhaTransporteProdutoIntegrationSupportData supportData,
            Integer initialBatchPosition,
            Map<String, IntegrationMapperInterface.MetodoAtualizacaoCampo> metodoAtualizacaoPorCampo,
            LinhaTransporteProdutoIntegrationOptionsDto options) {

        if (options != null && Boolean.TRUE.equals(options.createTransportationLaneIfNotPresent)) {
            criaTransportationLanesOrigemDestinoAusentes(dtoBatchList);
        }

        IntegrationServiceComConfiguracoesInterface.super.persistDTOBatchList(
                dtoBatchList,
                currentlyPersistedEntitiesByPrimaryKey,
                supportData,
                initialBatchPosition,
                metodoAtualizacaoPorCampo,
                options);

    }

    /**
     * Para esta integração, as options são tratadas em lote no
     * {@link #persistDTOBatchList} para evitar tentativa repetida de criação de
     * transportation lane a cada linha.
     */
    @Override
    public void executaAcaoColateralOptionsNaConversaoDtoEmEntidade(
            LinhaTransporteProdutoIntegrationDataDto dto,
            LinhaTransporteProduto entity,
            LinhaTransporteProdutoIntegrationOptionsDto options,
            LinhaTransporteProdutoIntegrationSupportData supportData) {

    }

    /**
     * Cria transportation lanes origem/destino ausentes para os registros do
     * batch atual. As lanes criadas ficam ativas e com "enable all materials"
     * igual a false.
     *
     * <p>Registros marcados para delete nao devem criar lane pai nova: nesse
     * caso a intencao operacional e remover/desativar dado existente, nao abrir
     * um novo caminho de suprimento.</p>
     */
    private void criaTransportationLanesOrigemDestinoAusentes(List<LinhaTransporteProdutoIntegrationDataDto> dtoBatchList) {

        Set<LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO> transportationLaneKeys = dtoBatchList.stream()
                .filter(dto -> dto.delete == null || !dto.delete.trim().equalsIgnoreCase("D"))
                .map(dto -> new LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO(
                        dto.primaryKeyDto.supplyNetworkVersionId,
                        dto.primaryKeyDto.originLocationId,
                        dto.primaryKeyDto.destinationLocationId))
                .collect(Collectors.toCollection(HashSet::new));

        if (transportationLaneKeys.isEmpty()) {
            return;
        }

        Map<LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO, ?> transportationLanesPersistedByPrimaryKey =
                linhaTransporteIntegrationService.getPersistedEntityMapFromPrimaryKeyDtoCollection(transportationLaneKeys);

        List<LinhaTransporteIntegrationDataDto> transportationLanesToCreate = transportationLaneKeys.stream()
                .filter(transportationLanePrimaryKey -> !transportationLanesPersistedByPrimaryKey.containsKey(transportationLanePrimaryKey))
                .map(transportationLanePrimaryKey -> LinhaTransporteIntegrationDataDto.builder()
                        .primaryKeyDto(transportationLanePrimaryKey)
                        .active(true)
                        .enableAllMaterials(false)
                        .build())
                .collect(Collectors.toList());

        if (!transportationLanesToCreate.isEmpty()) {
            linhaTransporteIntegrationService.saveDTOList(transportationLanesToCreate);
        }

    }

    /**
     * Desativa lanes/material por filtro operacional. O metodo permanece no
     * Community porque e uma limpeza funcional da malha simples; filtros por
     * caracteristica/agregador e regras logisticas avancadas continuam fora
     * deste service.
     */
    public void desativaLinhaTransporteProdutoDeFiltro(
            LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoDeactivationFilterIntegrationDTO filtroLinhaTransporteProdutoADesativar) {

        if (filtroLinhaTransporteProdutoADesativar == null) {
            throw new IllegalArgumentException("Transportation lane/material deactivation filter is required");
        }

        List<LinhaTransporteProduto> linhasTransporteProduto =
                new ArrayList<>((filtroLinhaTransporteProdutoADesativar.supplyNetworkVersionId == null || filtroLinhaTransporteProdutoADesativar.supplyNetworkVersionId.isEmpty()) ? linhaTransporteProdutoRepository.customFindAll() : linhaTransporteProdutoRepository.customFindByVersaoMalhaIdIn(filtroLinhaTransporteProdutoADesativar.supplyNetworkVersionId));
        if (linhasTransporteProduto.isEmpty()) {
            return;
        }

        if (filtroLinhaTransporteProdutoADesativar.originLocationType != null && !filtroLinhaTransporteProdutoADesativar.originLocationType.isEmpty()) {
            linhasTransporteProduto = linhasTransporteProduto.stream()
                    .filter(linhaTransporteProduto -> filtroLinhaTransporteProdutoADesativar.originLocationType.contains(linhaTransporteProduto.getLocationOrigem().getTipoLocation()))
                    .toList();
            if (linhasTransporteProduto.isEmpty()) {
                return;
            }
        }

        if (filtroLinhaTransporteProdutoADesativar.destinationLocationType != null && !filtroLinhaTransporteProdutoADesativar.destinationLocationType.isEmpty()) {
            linhasTransporteProduto = linhasTransporteProduto.stream()
                    .filter(linhaTransporteProduto -> filtroLinhaTransporteProdutoADesativar.destinationLocationType.contains(linhaTransporteProduto.getLocationDestino().getTipoLocation()))
                    .toList();
            if (linhasTransporteProduto.isEmpty()) {
                return;
            }
        }

        linhasTransporteProduto.forEach(linhaTransporteProduto -> linhaTransporteProduto.setAtivo(false));
        validaSavedEntityCollection(
                linhaTransporteProdutoRepository.saveAll(linhasTransporteProduto),
                "Transportation Lane Material deactivation saved collection",
                linhasTransporteProduto.size());

    }

    /**
     * Valida o snapshot administrativo de lane/material antes de filtros e
     * desativacao.
     *
     * <p>O filtro pode operar por versao de malha, tipo de origem e tipo de
     * destino. Portanto cada linha carregada precisa trazer a chave composta
     * completa: lane pai, versao, origem, destino e material. Falhar aqui evita
     * que snapshot quebrado vire NPE durante o filtro por tipo ou sucesso falso
     * no `saveAll` de desativacao.</p>
     */
    

}
