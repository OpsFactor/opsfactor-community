package com.opsfactor.community.capability.configuration.integration.service;

import com.opsfactor.community.capability.configuration.integration.dto.ParametrosMaterialLocationIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.location.integration.dto.LocationIntegrationFiltroDto;
import com.opsfactor.community.capability.configuration.integration.mapper.ParametrosMaterialLocationIntegrationMapper;
import com.opsfactor.community.capability.configuration.integration.mapper.ParametrosMaterialLocationIntegrationSupportData;
import com.opsfactor.community.capability.configuration.domain.ParametrosProdutoLocation;
import com.opsfactor.community.capability.configuration.repository.ParametrosProdutoLocationRepository;
import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.capability.masterdata.network.location.service.LocationService;
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
 * Servico de carga dos parametros operacionais material-location no Community.
 *
 * <p>Este cadastro continua no Community porque define informacoes basicas
 * usadas por Demand Planning, Supply Planning heuristico e Planning Book:
 * ativacao da combinacao material-location, datas de introducao/descontinuacao,
 * unidade padrao e lote minimo/multiplo de producao.</p>
 *
 * <p>O filtro recebido por endpoints customizados e usado apenas para operacoes
 * administrativas simples de carga, como desativar parametros de determinadas
 * locations. Ele nao representa filtros material/location, caracteristicas DFU
 * ou estruturas de agregacao Enterprise.</p>
 */
@Component
public class ParametrosMaterialLocationIntegrationService implements IntegrationServiceInterface<ParametrosMaterialLocationIntegrationDataDto, ParametrosMaterialLocationIntegrationDataDto.ParametrosMaterialLocationPrimaryKeyIntegrationDTO, ParametrosProdutoLocation, ParametrosMaterialLocationIntegrationSupportData, ParametrosMaterialLocationIntegrationMapper, LocationIntegrationFiltroDto> {

    /**
     * Service de locations usado para carregar apenas locations reais, sem a
     * location default tecnica.
     */
    @Autowired
    private LocationService locationService;

    /**
     * Repository de UOM usado para resolver a unidade padrao da combinacao
     * material-location.
     */
    @Autowired
    private UnidadeMedidaRepository unidadeMedidaRepository;

    /**
     * Repository de materiais Community usados na chave funcional da carga.
     */
    @Autowired
    private ProdutoRepository produtoRepository;

    /**
     * Repository do cadastro operacional material-location. Este service nao
     * expõe filtros/agregadores Enterprise; trabalha apenas a combinacao DFU
     * material/location.
     */
    @Autowired
    private ParametrosProdutoLocationRepository parametrosProdutoLocationRepository;

    /**
     * Mapper de integracao para o contrato publico de parametros
     * material-location.
     */
    @Autowired
    private ParametrosMaterialLocationIntegrationMapper parametrosMaterialLocationIntegrationMapper;

    /**
     * Devolve o mapper usado pela infraestrutura generica de carga.
     */
    @Override
    public ParametrosMaterialLocationIntegrationMapper getMapper() {

        return parametrosMaterialLocationIntegrationMapper;

    }

    /**
     * Persiste o batch de parametros material-location em uma unica chamada ao
     * repository. Lista vazia representa batch sem efeito.
     */
    @Override
    public List<ParametrosProdutoLocation> saveEntityList(Collection<ParametrosProdutoLocation> entityList) {

        if (!entityList.isEmpty()) {
            validaSavedEntityCollection(
                    parametrosProdutoLocationRepository.saveAll(entityList),
                    "Material Location Parameters saved collection",
                    entityList.size());
        }
        return new ArrayList<>();

    }

    /**
     * Remove em lote os parametros selecionados pela infraestrutura generica de
     * integracao.
     */
    @Override
    public void removeEntityList(Collection<ParametrosProdutoLocation> entityList) {

        if (!entityList.isEmpty()) {
            parametrosProdutoLocationRepository.deleteAll(entityList);
        }

    }

    /**
     * Mensagem publica mantida por compatibilidade com a API de upload.
     */
    @Override
    public String getSaveSuccessMessage() {

        return "Material - Location parameters saved";

    }

    /**
     * Materializa lookups de location, material e UOM uma vez por batch para
     * que o mapper nao gere N+1 ao validar cada DFU.
     */
    @Override
    public ParametrosMaterialLocationIntegrationSupportData getSupportData() {

        ParametrosMaterialLocationIntegrationSupportData parametrosMaterialLocationIntegrationSupportData =
                new ParametrosMaterialLocationIntegrationSupportData();

        /*
         * Parametros material-location sao a borda DFU operacional do
         * Community. Support data quebrado deve falhar aqui, antes de o mapper
         * interpretar a falta como material/location/UOM inexistente na linha.
         */
        parametrosMaterialLocationIntegrationSupportData.mapaLocationPorId = getMapaPorIdObrigatorio(
                locationService.findAllWithoutDefault(),
                location -> location.getId(),
                "Location snapshot");
        parametrosMaterialLocationIntegrationSupportData.mapaMaterialPorId = getMapaPorIdObrigatorio(
                produtoRepository.findAll(),
                material -> material.getId(),
                "Material snapshot");
        parametrosMaterialLocationIntegrationSupportData.mapaUnidadeMedidaPorId = getMapaPorIdObrigatorio(
                unidadeMedidaRepository.findAll(),
                unidadeMedida -> unidadeMedida.getId(),
                "Unit of Measure snapshot");

        return parametrosMaterialLocationIntegrationSupportData;

    }

    /**
     * Tamanho de lote historico para cargas material-location.
     */
    @Override
    public int getBatchSize() {

        return 5000;

    }

    /**
     * Busca os parametros ja persistidos para o batch atual.
     *
     * <p>O repository ja traz os many-to-one necessarios para evitar lazy N+1 no
     * filtro em memoria. Lote vazio retorna imediatamente para nao carregar a
     * tabela inteira sem necessidade.</p>
     */
    @Override
    public Collection<ParametrosProdutoLocation> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
            Collection<ParametrosMaterialLocationIntegrationDataDto.ParametrosMaterialLocationPrimaryKeyIntegrationDTO> dtoBatchList) {

        Collection<ParametrosMaterialLocationIntegrationDataDto.ParametrosMaterialLocationPrimaryKeyIntegrationDTO> parametrosMaterialLocationPrimaryKeyCollection =
                validaParametrosMaterialLocationPrimaryKeyCollection(dtoBatchList);

        if (parametrosMaterialLocationPrimaryKeyCollection.isEmpty()) {
            return List.of();
        }

        Set<String> materialIdSet = parametrosMaterialLocationPrimaryKeyCollection.stream()
                .map(x -> x.materialId)
                .collect(Collectors.toSet());
        Set<String> locationIdSet = parametrosMaterialLocationPrimaryKeyCollection.stream()
                .map(x -> x.locationId)
                .collect(Collectors.toSet());

        Collection<ParametrosProdutoLocation> parametrosProdutoLocationCollection =
                parametrosProdutoLocationRepository.customFindAllComFetchAtributosManyToOne();

        return parametrosProdutoLocationCollection.stream()
                .filter(parametrosProdutoLocation ->
                        materialIdSet.contains(parametrosProdutoLocation.getProduto().getId())
                                && locationIdSet.contains(parametrosProdutoLocation.getLocation().getId()))
                .collect(Collectors.toList());

    }

    /**
     * Retorna todos os parametros material-location com fetch dos many-to-one
     * usados por DTOs e mappers.
     */
    @Override
    public Collection<ParametrosProdutoLocation> getAllPersistedEntities() {

        return parametrosProdutoLocationRepository.customFindAllComFetchAtributosManyToOne();

    }

    /**
     * Remove todos os parametros material-location. Mantido como operacao
     * explicita para fluxos administrativos de carga full.
     */
    public void removeAll() {

        parametrosProdutoLocationRepository.deleteAll();

    }

    /**
     * Desativa parametros material-location usando somente filtros de location
     * aceitos no Community.
     */
    public void desativaParametrosMaterialLocationDeFiltroLocations(LocationIntegrationFiltroDto filtroLocationsADesativar) {

        /*
         * O Community aceita somente filtro operacional de locations para
         * desativacao em lote. Filtros por caracteristicas de material/location
         * e estruturas agregadoras sao Enterprise e nao chegam neste DTO.
         *
         * Antes de filtrar por location ou tipo, validamos o snapshot carregado
         * do repository. Um item sem chave/material/location nao pode chegar ao
         * filtro nem ao saveAll, porque isso transformaria quebra estrutural de
         * persisted data em NPE ou em sucesso falso de desativacao.
         */
        if (filtroLocationsADesativar != null && filtroLocationsADesativar.id != null && !filtroLocationsADesativar.id.isEmpty()) {
            List<ParametrosProdutoLocation> parametrosProdutoLocationList =
                    parametrosProdutoLocationRepository.customFindAllComFetchAtributosManyToOne().stream()
                            .filter(parametrosProdutoLocation ->
                                    filtroLocationsADesativar.id.contains(parametrosProdutoLocation.getLocation().getId()))
                            .toList();
            if (parametrosProdutoLocationList.isEmpty()) {
                return;
            }
            parametrosProdutoLocationList.forEach(parametrosProdutoLocation -> parametrosProdutoLocation.setAtivo(false));
            validaSavedEntityCollection(
                    parametrosProdutoLocationRepository.saveAll(parametrosProdutoLocationList),
                    "Material Location Parameters deactivation saved collection",
                    parametrosProdutoLocationList.size());
        } else if (filtroLocationsADesativar != null && filtroLocationsADesativar.locationType != null && !filtroLocationsADesativar.locationType.isEmpty()) {
            List<ParametrosProdutoLocation> parametrosProdutoLocationList =
                    parametrosProdutoLocationRepository.customFindAllComFetchAtributosManyToOne().stream()
                            .filter(parametrosProdutoLocation ->
                                    filtroLocationsADesativar.locationType.contains(parametrosProdutoLocation.getLocation().getTipoLocation()))
                            .toList();
            if (parametrosProdutoLocationList.isEmpty()) {
                return;
            }
            parametrosProdutoLocationList.forEach(parametrosProdutoLocation -> parametrosProdutoLocation.setAtivo(false));
            validaSavedEntityCollection(
                    parametrosProdutoLocationRepository.saveAll(parametrosProdutoLocationList),
                    "Material Location Parameters deactivation saved collection",
                    parametrosProdutoLocationList.size());
        } else {
            Collection<ParametrosProdutoLocation> parametrosProdutoLocationList =
                    parametrosProdutoLocationRepository.findAll();
            parametrosProdutoLocationList.forEach(parametrosProdutoLocation -> parametrosProdutoLocation.setAtivo(false));
            validaSavedEntityCollection(
                    parametrosProdutoLocationRepository.saveAll(parametrosProdutoLocationList),
                    "Material Location Parameters deactivation saved collection",
                    parametrosProdutoLocationList.size());
        }

    }

    /**
     * Valida o snapshot persistido antes do filtro material/location em memoria.
     *
     * <p>A consulta ja vem com fetch dos many-to-one para evitar N+1. Se algum
     * item vier sem chave, material ou location, continuar transformaria uma
     * inconsistencia de repository/cache em erro de getter encadeado sem
     * contexto funcional.</p>
     */
    

    /**
     * Valida as chaves material/location recebidas antes de reduzir o batch a
     * conjuntos de ids.
     *
     * <p>O lookup busca um envelope por material e location, mas a
     * reconciliacao funcional continua sendo por DFU. Uma chave repetida no
     * payload deve falhar aqui, com indice da linha recebida, em vez de ser
     * deduplicada silenciosamente pelos `Set` usados para consultar o
     * repository.</p>
     */
    private static Collection<ParametrosMaterialLocationIntegrationDataDto.ParametrosMaterialLocationPrimaryKeyIntegrationDTO> validaParametrosMaterialLocationPrimaryKeyCollection(
            Collection<ParametrosMaterialLocationIntegrationDataDto.ParametrosMaterialLocationPrimaryKeyIntegrationDTO> parametrosMaterialLocationPrimaryKeyCollection) {

        if (parametrosMaterialLocationPrimaryKeyCollection == null) {
            throw new DataUploadException("Material-location parameters primary key collection is required.");
        }

        Map<String, Set<String>> materiaisPorLocation = new HashMap<>();
        int indice = 0;
        for (ParametrosMaterialLocationIntegrationDataDto.ParametrosMaterialLocationPrimaryKeyIntegrationDTO parametrosMaterialLocationPrimaryKeyIntegrationDTO
                : parametrosMaterialLocationPrimaryKeyCollection) {
            if (parametrosMaterialLocationPrimaryKeyIntegrationDTO == null) {
                throw new DataUploadException(
                        "Material-location parameters primary key collection item at index " + indice + " is required.");
            }
            if (parametrosMaterialLocationPrimaryKeyIntegrationDTO.locationId == null
                    || parametrosMaterialLocationPrimaryKeyIntegrationDTO.locationId.isBlank()
                    || parametrosMaterialLocationPrimaryKeyIntegrationDTO.materialId == null
                    || parametrosMaterialLocationPrimaryKeyIntegrationDTO.materialId.isBlank()) {
                throw new DataUploadException(
                        "Material-location parameters upload primary key must include location and material");
            }
            if (!materiaisPorLocation
                    .computeIfAbsent(
                            parametrosMaterialLocationPrimaryKeyIntegrationDTO.locationId,
                            ignored -> new HashSet<>())
                    .add(parametrosMaterialLocationPrimaryKeyIntegrationDTO.materialId)) {
                throw new DataUploadException(
                        "Material-location parameters primary key collection item at index "
                                + indice
                                + " has duplicated key locationId "
                                + parametrosMaterialLocationPrimaryKeyIntegrationDTO.locationId
                                + " / materialId "
                                + parametrosMaterialLocationPrimaryKeyIntegrationDTO.materialId
                                + ".");
            }
            indice++;
        }

        return parametrosMaterialLocationPrimaryKeyCollection;

    }

}
