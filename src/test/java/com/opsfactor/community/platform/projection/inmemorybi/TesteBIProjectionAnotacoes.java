package com.opsfactor.community.platform.projection.inmemorybi;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.platform.projection.inmemorybi.applied.annotation.AtributoBiProjection;
import com.opsfactor.community.platform.projection.inmemorybi.applied.annotation.BIProjectionAnotacoes;
import com.opsfactor.community.platform.projection.inmemorybi.applied.annotation.ChavePrimariaBiProjection;
import lombok.Getter;
import lombok.Setter;
import org.javatuples.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.stream.Collectors;

public class TesteBIProjectionAnotacoes {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    @ChavePrimariaBiProjection
    public @interface ChavePrimariaTeste {
    }

    @Getter
    @Setter
    public class TesteClasseAnotada {

        @AtributoBiProjection
        public Location locationOrigem;

        @AtributoBiProjection
        public Integer numeroUnidades;

        @AtributoBiProjection
        public String descricao;

        @AtributoBiProjection
        public String getDescricaoAPartirDeMetodo() {
            return descricao;
        }

    }

    @Getter
    @Setter
    public class TesteClasseAnotada2 extends TesteClasseAnotada {

        @AtributoBiProjection
        public Location locationDestino;

        public Integer integerAIgnorar;

    }

    @Getter
    @Setter
    public class TesteClasseChaveComposta {

        @ChavePrimariaTeste
        public String codigo;

        @ChavePrimariaTeste
        public Integer periodo;

        @AtributoBiProjection
        public String descricao;

    }

    @Test
    public void testeCargaEExtracaoDadosBIProjectionAnotacoes() {

        TesteClasseAnotada2 t1 = new TesteClasseAnotada2();
        t1.setNumeroUnidades(3);
        t1.setDescricao("DescricaoT1");
        t1.setLocationOrigem(new Location("OrigemT1"));
        t1.setLocationDestino(new Location("DestinoT1"));

        TesteClasseAnotada t2 = new TesteClasseAnotada();
        t2.setNumeroUnidades(7);
        t2.setDescricao("DescricaoT2");
        t2.setLocationOrigem(new Location("OrigemT2"));

        BIProjectionAnotacoes<TesteClasseAnotada> biProjectionAnotacoes = new BIProjectionAnotacoes(TesteClasseAnotada.class);

        // alternativa para definição dos atributos : usando as próprias instâncias
//        biProjectionAnotacoes.addAttributes(t1);
//        biProjectionAnotacoes.addAttributes(t2);

        // definição de atributos através das classes que estão sendo carregadas (todas precisam extender T de BIEmMemoria<T>)
        biProjectionAnotacoes.addAttributesFromAnnotatedFields(TesteClasseAnotada.class);
        biProjectionAnotacoes.addAttributesFromAnnotatedFields(TesteClasseAnotada2.class);

        // adiciona dados ao BI
        biProjectionAnotacoes.addElementoNoBI(t1);
        biProjectionAnotacoes.addElementoNoBI(t2);

        List<TesteClasseAnotada> a = biProjectionAnotacoes.getWhereEquals(Pair.with("descricao", "DescricaoT1")).stream().collect(Collectors.toList());
        List<TesteClasseAnotada> b = biProjectionAnotacoes.getWhereEquals(Pair.with("locationOrigem", new Location("OrigemT2"))).stream().collect(Collectors.toList());
        List<TesteClasseAnotada> c = biProjectionAnotacoes.getWhereEquals(Pair.with("locationDestino", new Location("DestinoT1"))).stream().collect(Collectors.toList());
        List<TesteClasseAnotada> d = biProjectionAnotacoes.getWhereEquals(TesteClasseAnotada.class).stream().collect(Collectors.toList());
        List<TesteClasseAnotada> e = biProjectionAnotacoes.getWhereEquals(TesteClasseAnotada2.class).stream().collect(Collectors.toList());
        List<TesteClasseAnotada> f = biProjectionAnotacoes.getWhereEquals(TesteClasseAnotada2.class, Pair.with("descricao", "DescricaoT2")).stream().collect(Collectors.toList());
        List<TesteClasseAnotada> g = biProjectionAnotacoes.getWhereEquals(Pair.with("descricaoAPartirDeMetodo", "DescricaoT2")).stream().collect(Collectors.toList());

        Assertions.assertTrue(!a.isEmpty() && a.size() == 1);
        Assertions.assertTrue(!b.isEmpty() && b.size() == 1);
        Assertions.assertTrue(!c.isEmpty() && c.size() == 1);
        Assertions.assertTrue(!d.isEmpty() && d.size() == 1);
        Assertions.assertTrue(!e.isEmpty() && e.size() == 1);
        Assertions.assertTrue(f.isEmpty());
        Assertions.assertTrue(!g.isEmpty() && g.size() == 1);

        Assertions.assertEquals(t1, a.get(0));
        Assertions.assertEquals(t2, b.get(0));
        Assertions.assertEquals(t1, c.get(0));
        Assertions.assertEquals(t2, d.get(0));
        Assertions.assertEquals(t1, e.get(0));
        Assertions.assertEquals(t2, g.get(0));

    }

    @Test
    public void testeBuscaPorChavePrimariaCompostaBIProjectionAnotacoes() {

        TesteClasseChaveComposta elemento = new TesteClasseChaveComposta();
        elemento.setCodigo("SKU_A");
        elemento.setPeriodo(202601);
        elemento.setDescricao("Descricao operacional fora da chave");

        BIProjectionAnotacoes<TesteClasseChaveComposta> biProjectionAnotacoes =
                new BIProjectionAnotacoes<>(TesteClasseChaveComposta.class, true);
        biProjectionAnotacoes.addElementoNoBI(elemento);

        BIProjectionAnotacoes.ResultadoBuscaChavePrimaria<TesteClasseChaveComposta> resultadoCompleto =
                biProjectionAnotacoes.getByChavePrimariaCompostaIfPossible(
                        TesteClasseChaveComposta.class,
                        BIEmMemoria.FiltroDimensao.with("codigo", "SKU_A"),
                        BIEmMemoria.FiltroDimensao.with("periodo", 202601));
        BIProjectionAnotacoes.ResultadoBuscaChavePrimaria<TesteClasseChaveComposta> resultadoParcial =
                biProjectionAnotacoes.getByChavePrimariaCompostaIfPossible(
                        TesteClasseChaveComposta.class,
                        BIEmMemoria.FiltroDimensao.with("codigo", "SKU_A"));
        BIProjectionAnotacoes.ResultadoBuscaChavePrimaria<TesteClasseChaveComposta> resultadoComFiltroExtra =
                biProjectionAnotacoes.getByChavePrimariaCompostaIfPossible(
                        TesteClasseChaveComposta.class,
                        BIEmMemoria.FiltroDimensao.with("codigo", "SKU_A"),
                        BIEmMemoria.FiltroDimensao.with("periodo", 202601),
                        BIEmMemoria.FiltroDimensao.with("descricao", "Descricao operacional fora da chave"));

        Assertions.assertTrue(resultadoCompleto.buscaAplicavel());
        Assertions.assertTrue(resultadoCompleto.valorEncontrado().isPresent());
        Assertions.assertEquals(elemento, resultadoCompleto.valorEncontrado().get());
        Assertions.assertFalse(resultadoParcial.buscaAplicavel());
        Assertions.assertFalse(resultadoComFiltroExtra.buscaAplicavel());

    }
    
}
