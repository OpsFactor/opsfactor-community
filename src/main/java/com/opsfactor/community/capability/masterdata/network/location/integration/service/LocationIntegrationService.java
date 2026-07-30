package com.opsfactor.community.capability.masterdata.network.location.integration.service;

import com.opsfactor.community.capability.masterdata.network.location.integration.dto.LocationIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.location.integration.dto.LocationIntegrationFiltroDto;
import com.opsfactor.community.capability.masterdata.network.location.integration.mapper.LocationIntegrationMapper;
import com.opsfactor.community.capability.masterdata.network.location.integration.mapper.LocationIntegrationSupportData;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.organization.economicgroup.domain.EconomicGroup;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.organization.economicgroup.repository.EconomicGroupRepository;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.platform.integration.service.IntegrationServiceInterface;
import com.opsfactor.community.platform.exception.DataUploadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.opsfactor.community.platform.integration.service.IntegrationSupportDataValidation.getMapaPorIdObrigatorio;

/**
 * Service de integracao do cadastro basico de locations Community.
 *
 * <p>Este contrato cobre somente campos operacionais usados por Demand/Supply
 * Planning abertos. Dados GIS, mapa, last mile, warehouses e demais atributos
 * logisticos Enterprise nao devem entrar neste service.</p>
 */
@Component
@Slf4j
public class LocationIntegrationService implements IntegrationServiceInterface<LocationIntegrationDataDto, LocationIntegrationDataDto.LocationPrimaryKeyIntegrationDTO, Location, LocationIntegrationSupportData, LocationIntegrationMapper, LocationIntegrationFiltroDto> {

    /**
     * Repository operacional de locations Community. Escopos avançados,
     * caracteristicas e filtros Enterprise nao entram nesta carga.
     */
    @Autowired
    private LocationRepository locationRepository;

    /**
     * Repository de unidade de medida usado para resolver UOMs padrão de
     * capacidade/operacao mantidas na location.
     */
    @Autowired
    private UnidadeMedidaRepository unidadeMedidaRepository;

    /**
     * Cabeçalhos compartilhados de grupo econômico. O Community mantém a FK
     * unidirecional na Location, enquanto o overlay Enterprise decide se a
     * borda de Data Upload pode editá-la.
     */
    @Autowired
    private EconomicGroupRepository economicGroupRepository;

    /**
     * Mapper de integracao que converte linhas publicas de location para a
     * entidade compartilhada.
     */
    @Autowired
    private LocationIntegrationMapper locationIntegrationMapper;


    @Override
    public LocationIntegrationMapper getMapper() {
        return locationIntegrationMapper;
    }

    @Override
    public List<Location> saveEntityList(Collection<Location> entityList) {
        if (!entityList.isEmpty()) {
            validaSavedEntityCollection(
                    locationRepository.saveAll(entityList),
                    "Location saved collection",
                    entityList.size());
        }
        return new ArrayList<>();
    }

    @Override
    public void removeEntityList(Collection<Location> entityList) {
        if (!entityList.isEmpty()) {
            /*
             * Caracteristicas de location nao existem como carga nem como
             * repository Community. Se houver valores dependentes no banco, a
             * base esta fora do contrato desta edicao e deve falhar de forma
             * explicita em vez de esconder uma dependencia Enterprise.
             */
            locationRepository.deleteAll(entityList);
        }
    }

    @Override
    public LocationIntegrationSupportData getSupportData() {
        return LocationIntegrationSupportData.builder()
                /*
                 * UOMs sao support data para capacidades/operacoes basicas da
                 * location. Snapshot quebrado falha aqui, antes de o mapper
                 * confundir o problema com referencia inexistente na planilha.
                 */
                .unidadeMedidaMap(getMapaPorIdObrigatorio(
                        unidadeMedidaRepository.findAll(),
                        UnidadeMedida::getId,
                        "Unit of Measure snapshot"))
                .economicGroupMap(getMapaPorIdObrigatorio(
                        economicGroupRepository.findAll(),
                        EconomicGroup::getId,
                        "Economic Group snapshot"))
                .locationMap(getMapaPorIdObrigatorio(
                        locationRepository.findAll(),
                        Location::getId,
                        "Location reference snapshot"))
                .build();
    }

    @Override
    public int getBatchSize() {
        return 1000;
    }

    @Override
    public Collection<Location> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(Collection<LocationIntegrationDataDto.LocationPrimaryKeyIntegrationDTO> dtoBatchList) {
        Collection<LocationIntegrationDataDto.LocationPrimaryKeyIntegrationDTO> locationPrimaryKeyCollection =
                validaLocationPrimaryKeyCollection(dtoBatchList);
        Set<String> idsInBatch = locationPrimaryKeyCollection.stream()
                .map(dto -> dto.id)
                .collect(Collectors.toSet());
        return locationRepository.findAllById(idsInBatch);
    }

    @Override
    public String getSaveSuccessMessage() {
        return "Location data saved";
    }

    @Override
    public Collection<Location> getAllPersistedEntities() {
        return locationRepository.findAll().stream()
                .filter(location -> !"0".equals(location.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Desativa locations por ids, por tipo de location ou, quando nenhum filtro
     * é informado, desativa toda a base operacional Community.
     */
    public void desativaLocationsDeFiltro(
            LocationIntegrationFiltroDto filtroLocationsADesativar) {

        if (filtroLocationsADesativar != null && filtroLocationsADesativar.id != null && !filtroLocationsADesativar.id.isEmpty()) {
            List<Location> locationsComIds = locationRepository.findAll().stream()
                            .filter(location -> filtroLocationsADesativar.id.contains(location.getId()))
                            .toList();
            if (locationsComIds.isEmpty()) return;
            locationsComIds.forEach(location -> location.setAtivo(false));
            locationRepository.saveAll(locationsComIds);
        } else if (filtroLocationsADesativar != null && filtroLocationsADesativar.locationType != null && !filtroLocationsADesativar.locationType.isEmpty()) {
            List<Location> locationTipoLocation = locationRepository.findByTipoLocationIn(
                    filtroLocationsADesativar.locationType);
            if (locationTipoLocation.isEmpty()) return;
            locationTipoLocation.forEach(location -> location.setAtivo(false));
            locationRepository.saveAll(locationTipoLocation);
        } else {
            List<Location> locations = locationRepository.findAll();
            if (locations.isEmpty()) return;
            locations.forEach(location -> location.setAtivo(false));
            locationRepository.saveAll(locations);
        }
    }

    /**
     * Valida o snapshot administrativo usado por desativacao de locations.
     *
     * <p>A desativacao pode receber filtro por id, filtro por tipo ou nenhum
     * filtro. Em todos os casos, a lista retornada pelo repository precisa
     * carregar locations reais antes de qualquer filtro em memoria ou saveAll:
     * item nulo ou id ausente indicam quebra do snapshot persistido, nao
     * ausencia funcional de filtro.</p>
     */
    /**
     * Valida chaves de location recebidas antes do lookup por id.
     *
     * <p>A carga publica continua lista/linha a linha, mesmo que o repository
     * aceite um conjunto de ids. Validar aqui evita que locations duplicadas no
     * payload sejam deduplicadas silenciosamente antes do merge generico.</p>
     */
    private static Collection<LocationIntegrationDataDto.LocationPrimaryKeyIntegrationDTO> validaLocationPrimaryKeyCollection(
            Collection<LocationIntegrationDataDto.LocationPrimaryKeyIntegrationDTO> locationPrimaryKeyCollection) {

        if (locationPrimaryKeyCollection == null) {
            throw new DataUploadException("Location primary key collection is required.");
        }

        Set<String> locationIds = new HashSet<>();
        int indice = 0;
        for (LocationIntegrationDataDto.LocationPrimaryKeyIntegrationDTO locationPrimaryKeyIntegrationDTO
                : locationPrimaryKeyCollection) {
            if (locationPrimaryKeyIntegrationDTO == null) {
                throw new DataUploadException(
                        "Location primary key collection item at index " + indice + " is required.");
            }
            if (locationPrimaryKeyIntegrationDTO.id == null || locationPrimaryKeyIntegrationDTO.id.isBlank()) {
                throw new DataUploadException("locationId is required.");
            }
            if (!locationIds.add(locationPrimaryKeyIntegrationDTO.id)) {
                throw new DataUploadException(
                        "Location primary key collection item at index "
                                + indice
                                + " has duplicated locationId "
                                + locationPrimaryKeyIntegrationDTO.id
                                + ".");
            }
            indice++;
        }

        return locationPrimaryKeyCollection;

    }

}
