package com.opsfactor.community.capability.masterdata.network.location.service;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.cluster.repository.location.ClusterLocationsRepository;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.LinhaTransporteRepository;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.NoResultException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Service de cadastro basico de locations e cluster default.
 *
 * <p>Caracteristicas, mapa, deployment e restricoes logisticas avancadas ficam
 * fora deste service Community. O objetivo aqui e garantir o cadastro
 * operacional minimo usado por Demand/Supply heuristico.</p>
 */
@Service
@Slf4j
public class LocationService {

    /**
     * Repository JPA de locations.
     */
    @Autowired
    private LocationRepository locationRepository;

    /**
     * Repository de clusters de locations, usado para garantir o cluster
     * default necessario para Demand Planning.
     */
    @Autowired
    private ClusterLocationsRepository clusterLocationsRepository;

    /**
     * Repository da malha simples usada para descobrir origem/destino.
     */
    @Autowired
    private LinhaTransporteRepository linhaTransporteRepository;

    /**
     * JdbcTemplate usado somente para bootstrap transicional do cluster default
     * antes de reler a entidade pelo repository.
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Garante o cluster default de locations no bootstrap do contexto Spring.
     */
    @PostConstruct
    private void createDefaultClusterLocationAfterStartup() {

        createClusterLocationDefault();

    }

    /**
     * Traz todas as locations que não possuem destino. Candidatos a execução do DP
     * @return
     */
    public Collection<Location> getLocationsSemDestino(VersaoMalha versaoMalha) {
        Collection<Location> locations = findAllWithoutDefault();
        for (Location location : locations) {
            if (location.getLinhasTransporteOndeOrigem(versaoMalha).size() > 0) {
                locations.remove(location);
            }
        }
        return locations;
    }

    public List<Location> getLocations() {
        return locationRepository.findAll();
    }

    public List<LinhaTransporte> getLinhasTranpostorte(){
        return (List<LinhaTransporte>) linhaTransporteRepository.findAll();
    }

    public ClusterLocations createClusterLocationDefault(){
        Optional<ClusterLocations> optionalClusterLocations = clusterLocationsRepository.findById(Constantes.CLUSTER_LOCATION_PADRAO_ID);

        return optionalClusterLocations.orElseGet(() -> {

            /*
             * O bootstrap Community ainda usa insert direto porque o cluster
             * default precisa nascer com id fixo antes de algumas rotinas
             * legadas de configuracao relerem a entidade pelo repository.
             */
            jdbcTemplate.update("insert into cluster_locations (id,descricao,padrao,prioridade) values (?,?,?,?)",
                    Constantes.CLUSTER_LOCATION_PADRAO_ID,
                    Constantes.CLUSTER_LOCATION_PADRAO_DESCRICAO,
                    true,
                    999999);
            log.info("Cluster Locations default criada");
            return clusterLocationsRepository.findById(Constantes.CLUSTER_LOCATION_PADRAO_ID)
                    .orElseThrow(() -> new NoResultException("Default cluster location was not created"));

        });
    }
    public Location createLocationDefault() {
        Optional<Location> optLocation = locationRepository.findById(Constantes.LOCATION_PADRAO_ID);

        return optLocation.orElseGet(() -> {

            Location location = new Location();
            location.setId(Constantes.LOCATION_PADRAO_ID);
            location.setDescricao(Constantes.LOCATION_PADRAO_DESCRICAO);
            save(location);
            log.info("Location default criada");
            return locationRepository.findById(Constantes.LOCATION_PADRAO_ID)
                    .orElseThrow(() -> new NoResultException("Default location was not created"));

        });
    }

    public List<Location> findAllWithoutDefault() {
        /*
         * Community usa esta lista como cadastro base de location. Valores de
         * caracteristicas nao sao carregados aqui para evitar expor ou ativar
         * implicitamente filtros/agregacoes Enterprise.
         */
        return new ArrayList<>(locationRepository.customFindAllSemDefault());
    }

    public List<Location> findAll(){
        return locationRepository.findAll();
    }

    public Location getLocation(String locationId) {
        if (locationId == null) throw new NoResultException("Empty Location Id");
        return locationRepository.findById(locationId).orElseThrow(() -> new NoResultException("Location " + locationId + " not found"));
    }

    /**
     * Persiste uma location individual usada por fluxos internos de bootstrap
     * ou cadastros simples do Community.
     *
     * <p>O retorno do repository e tratado como snapshot salvo e validado antes
     * de ser devolvido ao caller. Isso evita que uma borda de dominio assuma
     * sucesso quando o repository devolveu entidade nula, sem id ou com id
     * divergente do cadastro solicitado.</p>
     */
    public Location save(Location location) {

        validaLocationParaSaveCommunity(location);
        Location locationSalva = locationRepository.save(location);
        validaLocationSalvaCommunity(location.getId(), locationSalva);
        return locationSalva;

    }

    /**
     * Persiste locations em lote para callers internos do modelo Community.
     *
     * <p>Mesmo sem retornar as entidades salvas, validamos entrada e retorno do
     * repository para evitar sucesso silencioso quando um batch de locations
     * estiver estruturalmente quebrado.</p>
     */
    public void saveAll(List<Location> locations) {

        validaLocationsParaSaveAllCommunity(locations);
        List<Location> locationsSalvas = locationRepository.saveAll(locations);
        validaLocationsSalvasCommunity(locationsSalvas);

    }

    private void validaLocationsParaSaveAllCommunity(List<Location> locations) {

        if (locations == null) {
            throw new IllegalArgumentException("Location collection to save is required.");
        }
        int indiceLocation = 0;
        for (Location location : locations) {
            if (location == null) {
                throw new IllegalArgumentException(
                        "Location to save at index "
                                + indiceLocation
                                + " is required.");
            }
            if (location.getId() == null || location.getId().isBlank()) {
                throw new IllegalArgumentException(
                        "Location to save at index "
                                + indiceLocation
                                + " must have an id.");
            }
            indiceLocation++;
        }

    }

    private void validaLocationParaSaveCommunity(Location location) {

        if (location == null) {
            throw new IllegalArgumentException("Location to save is required.");
        }
        if (location.getId() == null || location.getId().isBlank()) {
            throw new IllegalArgumentException("Location to save must have an id.");
        }

    }

    private void validaLocationsSalvasCommunity(List<Location> locationsSalvas) {

        if (locationsSalvas == null) {
            throw new IllegalArgumentException("Saved location collection is required.");
        }
        int indiceLocation = 0;
        for (Location location : locationsSalvas) {
            if (location == null) {
                throw new IllegalArgumentException(
                        "Saved location item at index "
                                + indiceLocation
                                + " is required.");
            }
            if (location.getId() == null || location.getId().isBlank()) {
                throw new IllegalArgumentException(
                        "Saved location item at index "
                                + indiceLocation
                                + " must have an id.");
            }
            indiceLocation++;
        }

    }

    private void validaLocationSalvaCommunity(
            String locationIdEsperado,
            Location locationSalva) {

        if (locationSalva == null) {
            throw new IllegalArgumentException("Saved location is required.");
        }
        if (locationSalva.getId() == null || locationSalva.getId().isBlank()) {
            throw new IllegalArgumentException("Saved location must have an id.");
        }
        if (!locationIdEsperado.equals(locationSalva.getId())) {
            throw new IllegalArgumentException(
                    "Saved location id "
                            + locationSalva.getId()
                            + " does not match requested location id "
                            + locationIdEsperado
                            + ".");
        }

    }

}
