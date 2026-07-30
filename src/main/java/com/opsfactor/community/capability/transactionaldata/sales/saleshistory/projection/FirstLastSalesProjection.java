package com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.FuncoesMap;
import lombok.AccessLevel;
import lombok.Getter;
import org.javatuples.Pair;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Indice em memoria de primeira e ultima venda por material/location.
 *
 * <p>Esta classe nao sabe de qual documento historico a venda veio. No
 * Community, `SalesProjectionFactory` a popula exclusivamente com sell-out; no
 * Enterprise, uma factory {@code @Primary} pode complementar a populacao com
 * sell-in ou sales orders quando esses documentos forem configurados.</p>
 */
public class FirstLastSalesProjection {

    /**
     * Calendario usado para converter datas inicial/final de venda em posicoes
     * de periodo quando o caller pede recorte por bucket.
     */
    @Getter
    private Calendario calendario;

    /**
     * Primeira/ultima venda por DFU.
     */
    @Getter(AccessLevel.NONE)
    private Map<Produto, Map<Location, FirstLastByMaterialLocation>> mapaPrimeiraEUltimaVendaPorMaterialLocation = new ConcurrentHashMap<>();

    /**
     * Primeira/ultima venda por material.
     */
    @Getter(AccessLevel.NONE)
    private Map<Produto, FirstLastByMaterial> mapaPrimeiraEUltimaVendaPorMaterial = new ConcurrentHashMap<>();

    /**
     * Primeira/ultima venda por location.
     */
    @Getter(AccessLevel.NONE)
    private Map<Location, FirstLastByLocation> mapaPrimeiraEUltimaVendaPorLocation = new ConcurrentHashMap<>();

    public FirstLastSalesProjection(Calendario calendario) {

        if (calendario == null) {
            throw new IllegalArgumentException("First/last sales projection calendar is required.");
        }
        this.calendario = calendario;

    }

    public void addFirstLastByMaterialLocation(FirstLastByMaterialLocation firstLastByMaterialLocation) {

        validaFirstLastByMaterialLocation(firstLastByMaterialLocation);
        if (getFirstLastByMaterialLocation(
                firstLastByMaterialLocation.getLocation(),
                firstLastByMaterialLocation.getMaterial()).isPresent()) {
            throw new IllegalStateException(
                    "First/last sales projection already contains material/location entry material "
                            + firstLastByMaterialLocation.getMaterial().getId()
                            + " / location "
                            + firstLastByMaterialLocation.getLocation().getId()
                            + ".");
        }
        FuncoesMap.adicionaElementoAoNestedMap(
                firstLastByMaterialLocation,
                mapaPrimeiraEUltimaVendaPorMaterialLocation,
                firstLastByMaterialLocation.getMaterial(), firstLastByMaterialLocation.getLocation());

    }

    public void addFirstLastByMaterial(FirstLastByMaterial firstLastByMaterial) {

        validaFirstLastByMaterial(firstLastByMaterial);
        if (getFirstLastByMaterial(firstLastByMaterial.getMaterial()).isPresent()) {
            throw new IllegalStateException(
                    "First/last sales projection already contains material entry material "
                            + firstLastByMaterial.getMaterial().getId()
                            + ".");
        }
        FuncoesMap.adicionaElementoAoNestedMap(
                firstLastByMaterial,
                mapaPrimeiraEUltimaVendaPorMaterial,
                firstLastByMaterial.getMaterial());

    }

    public void addFirstLastByLocation(FirstLastByLocation firstLastByLocation) {

        validaFirstLastByLocation(firstLastByLocation);
        if (getFirstLastByLocation(firstLastByLocation.getLocation()).isPresent()) {
            throw new IllegalStateException(
                    "First/last sales projection already contains location entry location "
                            + firstLastByLocation.getLocation().getId()
                            + ".");
        }
        FuncoesMap.adicionaElementoAoNestedMap(
                firstLastByLocation,
                mapaPrimeiraEUltimaVendaPorLocation,
                firstLastByLocation.getLocation());

    }

    public Optional<FirstLastByMaterialLocation> getFirstLastByMaterialLocation(Location location, Produto material) {

        validaLocation(
                location,
                "First/last material/location lookup");
        validaMaterial(
                material,
                "First/last material/location lookup");
        return FuncoesMap.getElementoDeNestedMap(
                mapaPrimeiraEUltimaVendaPorMaterialLocation,
                FirstLastByMaterialLocation.class,
                material, location);

    }

    public Optional<FirstLastByMaterial> getFirstLastByMaterial(Produto material) {

        validaMaterial(
                material,
                "First/last material lookup");
        return FuncoesMap.getElementoDeNestedMap(
                mapaPrimeiraEUltimaVendaPorMaterial,
                FirstLastByMaterial.class,
                material);

    }

    public Optional<FirstLastByLocation> getFirstLastByLocation(Location location) {

        validaLocation(
                location,
                "First/last location lookup");
        return FuncoesMap.getElementoDeNestedMap(
                mapaPrimeiraEUltimaVendaPorLocation,
                FirstLastByLocation.class,
                location);

    }

    public Optional<LocalDate> getDataFinalVenda() {

        return mapaPrimeiraEUltimaVendaPorMaterial.values()
                .stream()
                .map(FirstLastByMaterial::getLastDateTime)
                .sorted(Comparator.reverseOrder())
                .map(localDateTime -> localDateTime.toLocalDate())
                .findFirst();

    }

    public Optional<Pair<LocalDate, LocalDate>> getDatasInicialEFinalVenda(Location location, Produto material, boolean restritoAoCalendario) {

        return getFirstLastByMaterialLocation(location, material)
                .map(firstLastByMaterialLocation -> (restritoAoCalendario) ?
                        Pair.with(
                                Calendario.getMaxData(firstLastByMaterialLocation.getFirstDateTime().toLocalDate(), calendario.getDataHorarioInicial().toLocalDate()),
                                Calendario.getMinData(firstLastByMaterialLocation.getLastDateTime().toLocalDate(), calendario.getDataHorarioFinalPassada().toLocalDate()))
                        : Pair.with(
                                firstLastByMaterialLocation.getFirstDateTime().toLocalDate(),
                                firstLastByMaterialLocation.getLastDateTime().toLocalDate()));

    }

    public Optional<Pair<LocalDate, LocalDate>> getDatasInicialEFinalVenda(Location location, boolean restritoAoCalendario) {

        return getFirstLastByLocation(location)
                .map(firstLastByMaterialLocation -> (restritoAoCalendario) ?
                        Pair.with(
                                Calendario.getMaxData(firstLastByMaterialLocation.getFirstDateTime().toLocalDate(), calendario.getDataHorarioInicial().toLocalDate()),
                                Calendario.getMinData(firstLastByMaterialLocation.getLastDateTime().toLocalDate(), calendario.getDataHorarioFinalPassada().toLocalDate()))
                        : Pair.with(
                                firstLastByMaterialLocation.getFirstDateTime().toLocalDate(),
                                firstLastByMaterialLocation.getLastDateTime().toLocalDate()));

    }

    public Optional<Pair<LocalDate, LocalDate>> getDatasInicialEFinalVenda(Produto material, boolean restritoAoCalendario) {

        return getFirstLastByMaterial(material)
                .map(firstLastByMaterialLocation -> (restritoAoCalendario) ?
                        Pair.with(
                                Calendario.getMaxData(firstLastByMaterialLocation.getFirstDateTime().toLocalDate(), calendario.getDataHorarioInicial().toLocalDate()),
                                Calendario.getMinData(firstLastByMaterialLocation.getLastDateTime().toLocalDate(), calendario.getDataHorarioFinalPassada().toLocalDate()))
                        : Pair.with(
                                firstLastByMaterialLocation.getFirstDateTime().toLocalDate(),
                                firstLastByMaterialLocation.getLastDateTime().toLocalDate()));

    }

    public Optional<Pair<Integer, Integer>> getPeriodosInicialEFinalVenda(Location location, Produto material, boolean restritoAoCalendario) {

        return getDatasInicialEFinalVenda(location, material, restritoAoCalendario)
                .map(pair -> Pair.with(
                        calendario.getPosicaoPeriodo((LocalDate) pair.getValue0()),
                        calendario.getPosicaoPeriodo((LocalDate) pair.getValue1())));

    }

    public Optional<Pair<Integer, Integer>> getPeriodosInicialEFinalVenda(Produto material, boolean restritoAoCalendario) {

        return getDatasInicialEFinalVenda(material, restritoAoCalendario)
                .map(pair -> Pair.with(
                        calendario.getPosicaoPeriodo((LocalDate) pair.getValue0()),
                        calendario.getPosicaoPeriodo((LocalDate) pair.getValue1())));

    }

    public Optional<Pair<Integer, Integer>> getPeriodosInicialEFinalVenda(Location location, boolean restritoAoCalendario) {

        return getDatasInicialEFinalVenda(location, restritoAoCalendario)
                .map(pair -> Pair.with(
                        calendario.getPosicaoPeriodo((LocalDate) pair.getValue0()),
                        calendario.getPosicaoPeriodo((LocalDate) pair.getValue1())));

    }

    private void validaFirstLastByMaterialLocation(FirstLastByMaterialLocation firstLastByMaterialLocation) {

        if (firstLastByMaterialLocation == null) {
            throw new IllegalArgumentException("First/last material/location entry is required.");
        }
        validaMaterial(
                firstLastByMaterialLocation.getMaterial(),
                "First/last material/location entry");
        validaLocation(
                firstLastByMaterialLocation.getLocation(),
                "First/last material/location entry");
        validaDatasFirstLast(
                firstLastByMaterialLocation.getFirstDateTime(),
                firstLastByMaterialLocation.getLastDateTime(),
                "First/last material/location entry");

    }

    private void validaFirstLastByMaterial(FirstLastByMaterial firstLastByMaterial) {

        if (firstLastByMaterial == null) {
            throw new IllegalArgumentException("First/last material entry is required.");
        }
        validaMaterial(
                firstLastByMaterial.getMaterial(),
                "First/last material entry");
        validaDatasFirstLast(
                firstLastByMaterial.getFirstDateTime(),
                firstLastByMaterial.getLastDateTime(),
                "First/last material entry");

    }

    private void validaFirstLastByLocation(FirstLastByLocation firstLastByLocation) {

        if (firstLastByLocation == null) {
            throw new IllegalArgumentException("First/last location entry is required.");
        }
        validaLocation(
                firstLastByLocation.getLocation(),
                "First/last location entry");
        validaDatasFirstLast(
                firstLastByLocation.getFirstDateTime(),
                firstLastByLocation.getLastDateTime(),
                "First/last location entry");

    }

    private void validaMaterial(Produto material, String contexto) {

        if (material == null || material.getId() == null || material.getId().isBlank()) {
            throw new IllegalArgumentException(contexto + " requires material with id.");
        }

    }

    private void validaLocation(Location location, String contexto) {

        if (location == null || location.getId() == null || location.getId().isBlank()) {
            throw new IllegalArgumentException(contexto + " requires location with id.");
        }

    }

    private void validaDatasFirstLast(
            LocalDateTime firstDateTime,
            LocalDateTime lastDateTime,
            String contexto) {

        if (firstDateTime == null || lastDateTime == null) {
            throw new IllegalArgumentException(contexto + " requires first and last date.");
        }
        /*
         * A classe e a dona do indice mutavel. Mesmo que as factories validem
         * snapshots de repository antes de chamar os adders, a protecao fica
         * tambem aqui para impedir que um caller futuro escreva uma janela
         * temporal impossivel diretamente no mapa compartilhado.
         */
        if (lastDateTime.isBefore(firstDateTime)) {
            throw new IllegalArgumentException(
                    contexto
                            + " returned last date "
                            + lastDateTime
                            + " before first date "
                            + firstDateTime
                            + ".");
        }

    }

}
