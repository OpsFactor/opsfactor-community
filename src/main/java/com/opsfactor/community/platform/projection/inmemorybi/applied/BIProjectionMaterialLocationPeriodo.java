package com.opsfactor.community.platform.projection.inmemorybi.applied;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.platform.projection.inmemorybi.BIEmMemoria;
import com.opsfactor.community.platform.calendar.Calendario;
import lombok.Getter;
import org.javatuples.Pair;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * BI em memoria indexado por material, location e periodo.
 *
 * <p>Esta projection e a base do Planning Book Community. Ela substitui a
 * antiga projection por agrupamentos de caracteristicas, porque o Community
 * nao expoe group-by dinamico nem filtros por caracteristica. O objetivo aqui
 * e ser pequeno e explicito: armazenar linhas de key figure e recupera-las por
 * DFU/periodo/outros atributos simples. Um ajuste que alcance varias DFUs nao
 * transforma esta fotografia folha em uma arvore visual agregada.</p>
 */
@Getter
public class BIProjectionMaterialLocationPeriodo<T> {

    private final BIEmMemoria<T> biEmMemoria;

    public BIProjectionMaterialLocationPeriodo(
            Calendario calendario,
            Function<T, Produto> funcaoExtracaoMaterial,
            Function<T, Location> funcaoExtracaoLocation,
            Function<T, TemporalAccessor> funcaoExtracaoLocalDateReferencia,
            Class<T> classeMaterialLocationPeriodo,
            boolean criaIndiceLocations,
            boolean criaIndiceMateriais) {

        biEmMemoria = new BIEmMemoria(classeMaterialLocationPeriodo);

        biEmMemoria.addIntegerAttribute(
                "periodo",
                linha -> {
                    TemporalAccessor temporalAccessor = funcaoExtracaoLocalDateReferencia.apply(linha);

                    return (temporalAccessor instanceof LocalDate)
                            ? calendario.getPosicaoPeriodo((LocalDate) temporalAccessor)
                            : calendario.getPosicaoPeriodo((LocalDateTime) temporalAccessor);
                },
                true);

        biEmMemoria.addObjectAttribute(
                "location",
                Location.class,
                funcaoExtracaoLocation::apply,
                criaIndiceLocations);

        biEmMemoria.addObjectAttribute(
                "material",
                Produto.class,
                funcaoExtracaoMaterial::apply,
                criaIndiceMateriais);

    }

    public void addDadoAoBI(T t) {

        biEmMemoria.addElementoNoBI(t);

    }

    /**
     * Extrai linhas do BI por material/location opcionais e filtros adicionais
     * de atributos simples, como periodo e KeyFigure.
     */
    public Stream<T> getValores(
            @Nullable Produto material,
            @Nullable Location location,
            Pair<String, Object>... filtrosAdicionais) {

        Map<String, Object> mapaAttributeKeyValuePairs = new HashMap<>();
        if (location != null) mapaAttributeKeyValuePairs.put("location", location);
        if (material != null) mapaAttributeKeyValuePairs.put("material", material);

        Arrays.stream(filtrosAdicionais).forEach(parChaveAtributo -> mapaAttributeKeyValuePairs.put(
                parChaveAtributo.getValue0(),
                parChaveAtributo.getValue1()));

        return biEmMemoria.getWhereEquals(mapaAttributeKeyValuePairs).stream();

    }

    /**
     * Extrai linhas filtrando apenas atributos adicionais do BI, sem restringir
     * material/location. Usado para totalizacoes e operacoes internas.
     */
    public Stream<T> getValores(Pair<String, Object>... filtrosAdicionais) {

        return biEmMemoria.getWhereEquals(filtrosAdicionais).stream();

    }

    public Stream<T> getTodosValores() {

        return biEmMemoria.getStreamTodasLinhas();

    }

}
