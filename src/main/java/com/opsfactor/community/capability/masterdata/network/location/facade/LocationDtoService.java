package com.opsfactor.community.capability.masterdata.network.location.facade;

import com.opsfactor.community.capability.masterdata.network.location.facade.dto.LocationDTO;
import com.opsfactor.community.capability.masterdata.network.location.facade.mapper.LocationMapper;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import com.opsfactor.community.capability.masterdata.network.location.service.LocationService;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.NoResultException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service DTO Community para a borda publica de locations.
 *
 * <p>Caracteristicas dinamicas, visualizacao em mapa, deployment e restricoes
 * logisticas gerais pertencem ao Enterprise. A flag de restricao inbound e
 * Community, pois o heuristico de Supply a consulta para decidir se respeita o
 * lead time de recebimento. Esta classe mantem os endpoints legados de
 * consulta/salvamento simples, mas sempre converte locations sem
 * caracteristicas e rejeita campos privados antes de persistir.</p>
 */
@Service
public class LocationDtoService {

    /**
     * Service de dominio usado para consultas de locations sem a location
     * default tecnica.
     */
    @Autowired
    private LocationService locationService;

    /**
     * Repository usado somente pelo endpoint legado de salvamento simples.
     */
    @Autowired
    private LocationRepository locationRepository;

    /**
     * Mapper Community que remove caracteristicas Enterprise da resposta.
     */
    @Autowired
    private LocationMapper locationMapper;

    /**
     * Projection em memoria usada para resolver membros de clusters sem
     * consultar entidade a entidade.
     */
    @Autowired
    private ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory;

    /**
     * Lista locations sem caracteristicas para consumo do front Community.
     *
     * <p>Caracteristicas de location sao Enterprise; filtros e apresentacao
     * por caracteristica ficam fora da resposta Community.</p>
     */
    public List<LocationDTO> getListaLocationDTO() {

        List<Location> locationList = locationService.findAllWithoutDefault();
        List<LocationDTO> locationDTOList = locationMapper.converteListaEntidadeParaListaDTOSemCaracteristicasLocation(locationList);
        locationDTOList.sort(Comparator.comparing(LocationDTO::getId));

        return locationDTOList;

    }

    /**
     * Lista locations internas e pontos de transbordo para seletores
     * operacionais Community.
     */
    public List<LocationDTO> getListaLocationDTODeLocationsInternas() {

        List<Location> locationList = locationService.findAllWithoutDefault().stream()
                .filter(location -> location.getTipoLocation().equals(LocationAbstract.TipoLocation.INTERNA)
                        || location.getTipoLocation().equals(LocationAbstract.TipoLocation.PONTO_TRANSBORDO))
                .collect(Collectors.toList());

        List<LocationDTO> locationDTOList = locationMapper.converteListaEntidadeParaListaDTOSemCaracteristicasLocation(locationList);
        locationDTOList.sort(Comparator.comparing(LocationDTO::getId));

        return locationDTOList;

    }

    /**
     * Lista locations internas, pontos de transbordo e fornecedores para
     * cadastros operacionais que ainda precisam referenciar origem externa.
     */
    public List<LocationDTO> getListaLocationDTODeLocationsInternasEFornecedores() {

        List<Location> locationList = locationService.findAllWithoutDefault().stream()
                .filter(location -> location.getTipoLocation().equals(LocationAbstract.TipoLocation.INTERNA)
                        || location.getTipoLocation().equals(LocationAbstract.TipoLocation.PONTO_TRANSBORDO)
                        || location.getTipoLocation().equals(LocationAbstract.TipoLocation.FORNECEDOR))
                .collect(Collectors.toList());

        List<LocationDTO> locationDTOList = locationMapper.converteListaEntidadeParaListaDTOSemCaracteristicasLocation(locationList);
        locationDTOList.sort(Comparator.comparing(LocationDTO::getId));

        return locationDTOList;

    }

    /**
     * Resolve locations de um cluster usando a projection de parametros.
     *
     * <p>O retorno continua sem caracteristicas porque agrupamentos/filtros por
     * caracteristica sao capacidades Enterprise.</p>
     */
    public List<LocationDTO> getLocationsDeClusterLocationsId(Long clusterLocationsId) {

        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();
        return clusterEParametrosProjection.getLocationsDeClusterLocationsId(clusterLocationsId).stream()
                .map(locationMapper::convertLocationDtoWithoutCharacteristics)
                .sorted(Comparator.comparing(LocationDTO::getId))
                .collect(Collectors.toList());

    }

    /**
     * Lista locations habilitadas para o Planning Book de Supply Community.
     */
    public List<LocationDTO> getLocationDTOListParaPlanningBookSupply() {

        return locationService.findAllWithoutDefault().stream()
                .filter(location -> location.getPlanejaSupply())
                .filter(location -> !location.getTipoLocation().equals(LocationAbstract.TipoLocation.CLIENTE_FINAL)
                        && !location.getTipoLocation().equals(LocationAbstract.TipoLocation.REGIAO_COMERCIAL))
                .sorted(Comparator.comparing(Location::getId))
                .map(locationMapper::convertLocationDtoWithoutCharacteristics)
                .collect(Collectors.toList());

    }

    /**
     * Salva o cadastro simples de location mantido por compatibilidade legada.
     *
     * <p>O Community persiste descricao, status ativo, flags operacionais
     * basicas, a restricao inbound e o override de demanda indireta no safety
     * stock. Campos de mapa, deployment, demais restricoes logisticas e
     * caracteristicas falham explicitamente antes do save.</p>
     */
    @Deprecated
    public void saveLocationDTO(LocationDTO locationDTO) {

        validaPayloadLocationDTOCommunity(locationDTO);

        /*
         * Este endpoint e legado, mas permanece exposto. Deployment e
         * restricoes logisticas gerais continuam Enterprise. A restricao
         * inbound e o override de demanda indireta no safety stock sao
         * persistidos porque os motores Community os consomem. O override
         * aceita null para restaurar a heranca de ParametrosGlobais.
         */
        validaLocationDTOCommunity(locationDTO);

        Location location = locationRepository.findById(locationDTO.id)
                .orElse(new Location(locationDTO.id));

        location.setDescricao(locationDTO.description);
        location.setAtivo(locationDTO.active);
        location.setConsideraRestricaoLinhaInbound(locationDTO.applyInboundConstraints);
        location.setIncluiDemandaIndiretaNoSafetyStock(
                locationDTO.safetyStockConsiderIndirectDemand);
        location.setConsideraRestricaoProducao(locationDTO.applyProductionConstraints);
        location.setPlanejaSupply(locationDTO.showInSupplyPlanningBook);
        location.setPlanejaProducao(locationDTO.showInProductionPlanningBook);
        atualizaEscalaresEnterpriseDeLocation(location, locationDTO);

        /*
         * Endpoint legado nao retorna DTO, mas quem chama assume que a location
         * simples ficou disponivel para cadastros e planning books. Validamos
         * a fotografia salva para nao mascarar repository/stub que devolva
         * entidade nula ou chave diferente da solicitada.
         */
        Location locationSalva = locationRepository.save(location);
        validaLocationSalvaCommunity(locationDTO.id, locationSalva);

    }

    /**
     * Valida a fotografia salva pelo endpoint legado de Location.
     *
     * <p>Descricao e flags operacionais podem ser nulas conforme payload. A
     * chave da location, porem, precisa voltar igual ao DTO porque e usada por
     * malha, planning book e perfis de execucao Community.</p>
     */
    private void validaLocationSalvaCommunity(
            String locationId,
            Location locationSalva) {

        if (locationSalva == null
                || locationSalva.getId() == null
                || locationSalva.getId().isBlank()
                || !locationId.equals(locationSalva.getId())) {
            throw new IllegalStateException(
                    "Community location save returned invalid snapshot.");
        }

    }

    /**
     * Valida o payload minimo do endpoint legado de location.
     *
     * <p>O id e a chave funcional da location. Payload nulo ou id vazio deve
     * falhar como erro de request antes dos gates Enterprise e antes de
     * qualquer repository.</p>
     */
    private void validaPayloadLocationDTOCommunity(LocationDTO locationDTO) {

        if (locationDTO == null) {
            throw new IllegalArgumentException("Location payload is required.");
        }
        if (locationDTO.id == null || locationDTO.id.isBlank()) {
            throw new NoResultException("Empty location Id");
        }

    }

    /**
     * Bloqueia campos Enterprise ainda presentes no DTO compartilhado.
     */
    protected void validaLocationDTOCommunity(LocationDTO locationDTO) {

        /*
         * A coluna compartilhada da entidade nao torna deployment uma
         * capability Community: o contrato DTO continua inteiramente fechado
         * para esse campo, inclusive quando o payload tenta desabilita-lo.
         */
        validaCampoEnterprisePreenchido(locationDTO.showInDeployment, "Location deployment visibility");
        validaCampoEnterpriseDesabilitado(locationDTO.applyLogisticsConstraints, "Location logistics constraints");
        validaCoordenadasGeograficasLocation(locationDTO);
        if (locationDTO.characteristicValues != null && !locationDTO.characteristicValues.isEmpty()) {
            throw new RequiresEnterpriseVersionException("Location characteristics");
        }

    }

    /**
     * Mantem coordenadas fora da borda Community.
     *
     * <p>O overlay Enterprise substitui somente esta guarda antes de gravar
     * latitude/longitude, preservando os demais gates desta validacao.</p>
     */
    protected void validaCoordenadasGeograficasLocation(LocationDTO locationDTO) {

        validaCampoEnterprisePreenchido(locationDTO.latitude, "Location geographic coordinates");
        validaCampoEnterprisePreenchido(locationDTO.longitude, "Location geographic coordinates");

    }

    /**
     * Ponto de extensao para escalares Enterprise da mesma tabela Location.
     *
     * <p>Community nao grava nenhum escalar adicional. O hook evita que o
     * overlay replique o endpoint legado e garante que suas validacoes e os
     * campos Community continuem centralizados neste service.</p>
     */
    protected void atualizaEscalaresEnterpriseDeLocation(
            Location location,
            LocationDTO locationDTO) {

        // Community nao possui escalares adicionais de Location.

    }

    /**
     * Rejeita flags booleanas Enterprise ativadas por payload manual/legado.
     */
    private void validaCampoEnterpriseDesabilitado(Boolean valor, String nomeRecurso) {

        if (Boolean.TRUE.equals(valor)) {
            throw new RequiresEnterpriseVersionException(nomeRecurso);
        }

    }

    /**
     * Rejeita campos Enterprise preenchidos quando o Community nao publica a
     * capacidade correspondente.
     */
    private void validaCampoEnterprisePreenchido(Object valor, String nomeRecurso) {

        if (valor != null) {
            throw new RequiresEnterpriseVersionException(nomeRecurso);
        }

    }

}
