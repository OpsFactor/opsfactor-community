package com.opsfactor.community.platform.projection.inmemorybi.applied.annotation;

import com.opsfactor.community.platform.projection.inmemorybi.BIEmMemoria;
import jakarta.annotation.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * BI em memória que indexa todos os campos marcados com @AtributoBiProjection
 * @param <T>
 */
public class BIProjectionAnotacoes<T> extends BIEmMemoria<T> {

    public static final String NOME_ATRIBUTO_CHAVE_PRIMARIA_COMPOSTA = "chavePrimariaCompostaBi";
    private static final String VALOR_NULO_CHAVE_PRIMARIA_COMPOSTA = "<null>";

    /*
     * Cacheia a resolução refletiva por classe concreta.
     *
     * Em projections com muitas classes irmãs, o mesmo BI executa filtros
     * repetidos sobre os mesmos nomes de dimensão. Sem cache, cada lookup faz a
     * varredura da hierarquia e dispara NoSuchFieldException/NoSuchMethodException
     * em massa até encontrar o membro correto, o que degrada muito o tempo de
     * montagem da projection.
     */
    private final Map<Class<?>, Map<String, Optional<Field>>> cacheCampoPorClasseENome = new ConcurrentHashMap<>();
    private final Map<Class<?>, Map<String, Optional<Method>>> cacheMetodoPorClasseENome = new ConcurrentHashMap<>();
    private final Map<Class<?>, Optional<Method>> cacheMetodoIdPorClasse = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Field>> cacheCamposChavePrimariaPorClasse = new ConcurrentHashMap<>();
    private final boolean criaIndiceChavePrimariaComposta;

    /**
     * Resultado da tentativa de busca por chave primária composta.
     * <p>
     * {@code buscaAplicavel=false} significa que o projection não tem índice
     * composto ativo, a classe não declara chave primária ou os filtros não
     * cobrem exatamente os campos da chave. Nesse caso o caller deve seguir pelo
     * fluxo normal de consulta por atributos individuais.
     */
    public record ResultadoBuscaChavePrimaria<C>(boolean buscaAplicavel, Optional<C> valorEncontrado) {

        public static <C> ResultadoBuscaChavePrimaria<C> naoAplicavel() {
            return new ResultadoBuscaChavePrimaria<>(false, Optional.empty());
        }

        public static <C> ResultadoBuscaChavePrimaria<C> aplicavel(Optional<C> valorEncontrado) {
            return new ResultadoBuscaChavePrimaria<>(true, valorEncontrado);
        }
    }

    public BIProjectionAnotacoes (Class<T> classeObjetoArmazenado) {
        super(classeObjetoArmazenado);
        this.criaIndiceChavePrimariaComposta = false;
        addAttributesFromAnnotatedFields(classeObjetoArmazenado);
    }

    /**
     * Cria um projection anotado com opção de índice primário composto.
     * <p>
     * O índice composto é uma otimização para lookups de identidade completa:
     * classe concreta + todos os campos anotados direta ou indiretamente com
     * {@link ChavePrimariaBiProjection}. Ele é desligado por padrão para não
     * acrescentar atributo técnico nem custo de geração de chave em BIs que só
     * fazem consultas analíticas/parciais.
     */
    public BIProjectionAnotacoes (Class<T> classeObjetoArmazenado, boolean criaIndiceChavePrimariaComposta) {
        super(classeObjetoArmazenado);
        this.criaIndiceChavePrimariaComposta = criaIndiceChavePrimariaComposta;
        if (criaIndiceChavePrimariaComposta) {
            addStringAttributeUniqueIndex(
                    NOME_ATRIBUTO_CHAVE_PRIMARIA_COMPOSTA,
                    this::geraChavePrimariaComposta,
                    true);
        }
        addAttributesFromAnnotatedFields(classeObjetoArmazenado);
    }

    /**
     * Adiciona atributos de campos e métodos anotados com @AtributoBiProjection
     * @param classes classe referência, que deve extender T de BiEmMemoria<T>
     */
    public void addAttributesFromAnnotatedFields(Collection<Class<? extends T>> classes) {
        for (Class<? extends T> classe : classes) {
            addAttributesFromAnnotatedFields(classe);
        }
    }

    /**
     * Adiciona atributos de campos e métodos anotados com @AtributoBiProjection
     * @param classes classe referência, que deve extender T de BiEmMemoria<T>
     */
    public void addAttributesFromAnnotatedFields(Class<? extends T>... classes) {
        Arrays.stream(classes)
                .forEach(classe -> addAttributesFromAnnotatedFields(classe));
    }

    /**
     * Adiciona atributos de campos e métodos anotados com @AtributoBiProjection
     * @param classe classe referência, que deve extender T de BiEmMemoria<T>
     */
    public void addAttributesFromAnnotatedFields(Class<? extends T> classe) {

        Class<?> classeIterada = classe;

        // Percorre toda a hierarquia de classes até a classe base.
        // Isso é essencial porque as dimensões usadas para indexação no BI podem
        // estar distribuídas entre a classe concreta e as superclasses abstratas.
        // Se lermos sempre apenas a classe original, parte dos atributos anotados
        // deixa de ser registrada e os filtros por dimensão passam a falhar
        // silenciosamente.
        while (classeIterada != null) {
            // Processa somente os campos declarados na classe atualmente iterada.
            // Usar classeIterada aqui evita reprocessar sempre a mesma classe e
            // garante que a hierarquia inteira seja indexada.
            for (Field field : classeIterada.getDeclaredFields()) {
                if (isElementoAnotadoCom(field.getAnnotations(), AtributoBiProjection.class)) {
                    addAttributeFromAnnotatedField(field);
                }
            }

            // Processa somente os métodos declarados na classe atualmente iterada.
            // Isso inclui getters anotados em superclasses, que também precisam
            // participar da indexação por dimensão.
            for (Method method : classeIterada.getDeclaredMethods()) {
                if (isElementoAnotadoCom(method.getAnnotations(), AtributoBiProjection.class)) {
                    addAttributeFromAnnotatedMethod(method);
                }
            }

            // Move para a classe base (superclasse)
            classeIterada = classeIterada.getSuperclass();
        }

    }

    /**
     * Adiciona atributos de campos e métodos anotados com @AtributoBiProjection
     * @param instanciaT variável usada para extrair a classe de referência, que deve extender T de BiEmMemoria<T>
     */
    public <C extends T> void addAttributesFromAnnotatedFields(C instanciaT) {

        Class<? extends T> classeDaInstanciaT = (Class<? extends T>) instanciaT.getClass();

        addAttributesFromAnnotatedFields(classeDaInstanciaT);

    }

    public boolean hasIndiceChavePrimariaComposta() {
        return criaIndiceChavePrimariaComposta;
    }

    /**
     * Tenta buscar um registro pela chave primária composta, sem impor custo aos
     * projections que não habilitaram esse índice.
     * <p>
     * A busca só é aplicável quando os filtros recebidos cobrem exatamente os
     * campos anotados com {@link ChavePrimariaBiProjection} para a classe
     * concreta informada. Essa regra preserva a semântica das buscas parciais:
     * se o caller filtrou apenas parte da identidade ou adicionou um filtro
     * extra, ele continua no fluxo normal de interseção dos índices individuais.
     */
    public <C extends T> ResultadoBuscaChavePrimaria<C> getByChavePrimariaCompostaIfPossible(
            Class<C> classeConcreta,
            BIEmMemoria.FiltroDimensao... filtrosDimensao) {

        if (!criaIndiceChavePrimariaComposta) {
            return ResultadoBuscaChavePrimaria.naoAplicavel();
        }

        List<Field> camposChavePrimaria = getCamposChavePrimariaComposta(classeConcreta);
        if (camposChavePrimaria.isEmpty()) {
            return ResultadoBuscaChavePrimaria.naoAplicavel();
        }

        Map<String, Object> valorFiltroPorNomeAtributo = getValorFiltroPorNomeAtributo(filtrosDimensao);
        if (!filtrosCobremExatamenteChavePrimaria(camposChavePrimaria, valorFiltroPorNomeAtributo)) {
            return ResultadoBuscaChavePrimaria.naoAplicavel();
        }

        String chavePrimariaComposta = geraChavePrimariaComposta(classeConcreta, camposChavePrimaria, valorFiltroPorNomeAtributo);
        Optional<C> registroEncontrado = getWhereEqualsStringUniqueIndex(
                NOME_ATRIBUTO_CHAVE_PRIMARIA_COMPOSTA,
                chavePrimariaComposta)
                .map(classeConcreta::cast);

        return ResultadoBuscaChavePrimaria.aplicavel(registroEncontrado);
    }

    /**
     * Gera a chave técnica indexada no {@code UniqueIndex} a partir do próprio
     * objeto armazenado no BI.
     */
    private String geraChavePrimariaComposta(T elemento) {

        Class<?> classeConcreta = elemento.getClass();
        List<Field> camposChavePrimaria = getCamposChavePrimariaComposta(classeConcreta);
        if (camposChavePrimaria.isEmpty()) {
            return classeConcreta.getSimpleName();
        }

        StringBuilder chavePrimariaComposta = new StringBuilder(classeConcreta.getSimpleName());
        for (Field field : camposChavePrimaria) {
            chavePrimariaComposta
                    .append("|")
                    .append(field.getName())
                    .append("=")
                    .append(normalizaValorChavePrimaria(getFieldValueByName(elemento, field.getName())));
        }

        return chavePrimariaComposta.toString();
    }

    /**
     * Gera a chave técnica equivalente a partir dos filtros recebidos pelo
     * caller. A ordem usada é sempre a ordem declarada dos campos primários na
     * classe concreta.
     */
    private String geraChavePrimariaComposta(
            Class<?> classeConcreta,
            List<Field> camposChavePrimaria,
            Map<String, Object> valorFiltroPorNomeAtributo) {

        StringBuilder chavePrimariaComposta = new StringBuilder(classeConcreta.getSimpleName());
        for (Field field : camposChavePrimaria) {
            chavePrimariaComposta
                    .append("|")
                    .append(field.getName())
                    .append("=")
                    .append(normalizaValorChavePrimaria(valorFiltroPorNomeAtributo.get(field.getName())));
        }

        return chavePrimariaComposta.toString();
    }

    /**
     * Retorna os campos de chave primária na ordem declarada.
     * <p>
     * Percorremos a classe concreta antes das superclasses para preservar a
     * leitura natural das classes finais do domínio. O cache evita repetir
     * reflexão em milhões de lookups durante a montagem do modelo canônico.
     */
    private List<Field> getCamposChavePrimariaComposta(Class<?> classeConcreta) {

        return cacheCamposChavePrimariaPorClasse.computeIfAbsent(
                classeConcreta,
                classe -> {
                    List<Field> camposChavePrimaria = new ArrayList<>();
                    Class<?> classeIterada = classe;

                    while (classeIterada != null) {
                        for (Field field : classeIterada.getDeclaredFields()) {
                            if (isElementoAnotadoCom(field.getAnnotations(), ChavePrimariaBiProjection.class)) {
                                field.setAccessible(true);
                                camposChavePrimaria.add(field);
                            }
                        }
                        classeIterada = classeIterada.getSuperclass();
                    }

                    return List.copyOf(camposChavePrimaria);
                });
    }

    private Map<String, Object> getValorFiltroPorNomeAtributo(BIEmMemoria.FiltroDimensao... filtrosDimensao) {

        Map<String, Object> valorFiltroPorNomeAtributo = new HashMap<>();
        for (BIEmMemoria.FiltroDimensao filtroDimensao : filtrosDimensao) {
            if (valorFiltroPorNomeAtributo.containsKey(filtroDimensao.nomeAtributo())) {
                throw new IllegalStateException("Filtro duplicado para atributo " + filtroDimensao.nomeAtributo());
            }
            valorFiltroPorNomeAtributo.put(
                    filtroDimensao.nomeAtributo(),
                    filtroDimensao.valorAtributo());
        }

        return valorFiltroPorNomeAtributo;
    }

    private boolean filtrosCobremExatamenteChavePrimaria(
            List<Field> camposChavePrimaria,
            Map<String, Object> valorFiltroPorNomeAtributo) {

        if (valorFiltroPorNomeAtributo.size() != camposChavePrimaria.size()) {
            return false;
        }

        for (Field field : camposChavePrimaria) {
            if (!valorFiltroPorNomeAtributo.containsKey(field.getName())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Normaliza valores de dimensão para uma representação barata e estável.
     * <p>
     * Entidades de domínio costumam ter {@code getId()}; usar esse identificador
     * evita acionar comparadores caros como {@code VersaoProducao.compareTo} no
     * caminho quente de lookup. Quando não há id disponível, caímos para uma
     * representação baseada em classe e hashCode, que continua determinística
     * para o mesmo valor lógico dentro da execução.
     */
    private String normalizaValorChavePrimaria(Object valor) {

        if (valor == null) {
            return VALOR_NULO_CHAVE_PRIMARIA_COMPOSTA;
        }
        if (valor instanceof Enum<?> valorEnum) {
            return valorEnum.getDeclaringClass().getSimpleName() + "#" + valorEnum.name();
        }
        if (valor instanceof CharSequence
                || valor instanceof Number
                || valor instanceof Boolean
                || valor instanceof TemporalAccessor) {
            return String.valueOf(valor);
        }

        Object idObjeto = getIdObjetoSeDisponivel(valor);
        if (idObjeto != null) {
            return valor.getClass().getSimpleName() + "#" + idObjeto;
        }

        return valor.getClass().getSimpleName() + "#" + valor.hashCode();
    }

    @Nullable
    private Object getIdObjetoSeDisponivel(Object valor) {

        Method metodoId = cacheMetodoIdPorClasse.computeIfAbsent(
                        valor.getClass(),
                        classe -> {
                            try {
                                Method method = classe.getMethod("getId");
                                method.setAccessible(true);
                                return Optional.of(method);
                            } catch (NoSuchMethodException noSuchMethodException) {
                                return Optional.empty();
                            }
                        })
                .orElse(null);

        if (metodoId == null) {
            return null;
        }

        try {
            return metodoId.invoke(valor);
        } catch (ReflectiveOperationException | IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Falha ao invocar getId para normalizar dimensao BI OBJECT em "
                            + valor.getClass().getName(),
                    exception);
        }
    }

    private boolean isElementoAnotadoCom(
            Annotation[] annotations,
            Class<? extends Annotation> annotationTypeBuscada) {

        for (Annotation annotation : annotations) {
            if (isAnnotationTypeAnotadaCom(
                    annotation.annotationType(),
                    annotationTypeBuscada,
                    new HashSet<>())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Resolve meta-anotações em profundidade.
     * <p>
     * Isso permite que uma anotação de domínio reaproveite
     * {@link ChavePrimariaBiProjection} ou {@link AtributoBiProjection} como
     * meta-anotação, sem acoplar o módulo do BI aos detalhes do domínio.
     */
    private boolean isAnnotationTypeAnotadaCom(
            Class<? extends Annotation> annotationType,
            Class<? extends Annotation> annotationTypeBuscada,
            Set<Class<? extends Annotation>> annotationTypesVisitadas) {

        if (annotationType.equals(annotationTypeBuscada)) {
            return true;
        }
        if (!annotationTypesVisitadas.add(annotationType)) {
            return false;
        }
        if (annotationType.isAnnotationPresent(annotationTypeBuscada)) {
            return true;
        }

        for (Annotation metaAnnotation : annotationType.getAnnotations()) {
            String pacoteMetaAnnotation = metaAnnotation.annotationType().getPackageName();
            if (pacoteMetaAnnotation.startsWith("java.lang.annotation")) {
                continue;
            }
            if (isAnnotationTypeAnotadaCom(
                    metaAnnotation.annotationType(),
                    annotationTypeBuscada,
                    annotationTypesVisitadas)) {
                return true;
            }
        }

        return false;
    }

    private <V extends Comparable<V>> void addAttributeFromAnnotatedField(Field field) {
        field.setAccessible(true); // Necessário para acessar campos privados
        String nomeAtributo = field.getName();
        Class<V> classeField = (Class<V>) field.getType();

        if (!hasAttribute(nomeAtributo)) {
            /*
             * O atributo precisa ser extraído pelo nome do campo na classe real da
             * instância, e não pelo java.lang.reflect.Field originalmente recebido.
             *
             * Motivo:
             * - o BI registra o atributo apenas uma vez por nome;
             * - em várias famílias de projection temos classes irmãs com os mesmos
             *   nomes de dimensão (ex.: location/material/periodo);
             * - se o extrator ficar preso ao Field da primeira classe registrada,
             *   a leitura nas demais classes irmãs retorna IllegalArgumentException
             *   e o BI passa a indexar null silenciosamente.
             *
             * Isso fazia com que buscas por dimensão encontrassem apenas a primeira
             * classe registrada daquela família, quebrando cálculos que dependem
             * de múltiplos registros com as mesmas dimensões lógicas.
             */
            addAttributeBasedOnValueType(nomeAtributo, classeField, t -> (V) getFieldValueByName(t, nomeAtributo));
        }
    }

    private <V extends Comparable<V>> void addAttributeFromAnnotatedMethod(Method method) {
        method.setAccessible(true); // Necessário para acessar campos privados
        String nomeMetodo = method.getName();

        // Verifica se o método começa com 'get' ou 'is'
        if (nomeMetodo.startsWith("get") || nomeMetodo.startsWith("is")) {
            // Remove o 'get' ou 'is'
            nomeMetodo = nomeMetodo.startsWith("get") ? nomeMetodo.substring(3) : nomeMetodo.substring(2);
        }

        // Converte a primeira letra para minúscula
        nomeMetodo = nomeMetodo.substring(0, 1).toLowerCase() + nomeMetodo.substring(1);

        Class<V> classeRetornoMetodo = (Class<V>) method.getReturnType();

        if (!hasAttribute(nomeMetodo)) {
            /*
             * O mesmo racional dos campos vale para métodos/getters anotados:
             * o método refletido precisa ser resolvido na classe concreta da
             * instância. Caso contrário, um getter definido em uma classe irmã da
             * primeira classe registrada deixa de ser utilizável para indexação.
             */
            addAttributeBasedOnValueType(nomeMetodo, classeRetornoMetodo, t -> (V) getMethodValueByName(t, method.getName()));
        }
    }

    // Adiciona o atributo com base no tipo do valor
    private <V extends Comparable<V>> void addAttributeBasedOnValueType(String nomeAtributo, V valor, Function<T, V> extratorValor) {
        if (valor instanceof Integer) {
            addIntegerAttribute(nomeAtributo, t -> (Integer) extratorValor.apply(t), true);
        } else if (valor instanceof String) {
            addStringAttribute(nomeAtributo, t -> (String) extratorValor.apply(t), true);
        } else if (valor instanceof Double) {
            addDoubleAttribute(nomeAtributo, t -> (Double) extratorValor.apply(t), true);
        } else if (valor instanceof Boolean) {
            addBooleanAttribute(nomeAtributo, t -> (Boolean) extratorValor.apply(t), true);
        } else if (valor instanceof Enum) {
            addEnumAttribute(nomeAtributo, t -> (Enum) extratorValor.apply(t), true);
        } else {
            // Caso seja um objeto genérico, armazena como OBJECT
            addObjectAttribute(nomeAtributo, (Class<V>) valor.getClass(), t -> extratorValor.apply(t), true);
        }
    }

    private <V extends Comparable<V>> void addAttributeBasedOnValueType(String nomeAtributo, Class<V> classeValorExtraido, Function<T, V> extratorValor) {
        if (classeValorExtraido == Integer.class) {
            addIntegerAttribute(nomeAtributo, t -> (Integer) extratorValor.apply(t), true);
        } else if (classeValorExtraido == String.class) {
            addStringAttribute(nomeAtributo, t -> (String) extratorValor.apply(t), true);
        } else if (classeValorExtraido == Double.class) {
            addDoubleAttribute(nomeAtributo, t -> (Double) extratorValor.apply(t), true);
        } else if (classeValorExtraido == Boolean.class) {
            addBooleanAttribute(nomeAtributo, t -> (Boolean) extratorValor.apply(t), true);
        } else if (classeValorExtraido.isEnum()) {
            addEnumAttribute(nomeAtributo, t -> (Enum<?>) extratorValor.apply(t), true);
        } else {
            // Caso seja um objeto genérico, armazena como OBJECT
            addObjectAttribute(nomeAtributo, classeValorExtraido, t -> extratorValor.apply(t), true);
        }
    }

    // Métodos utilitários para extrair os valores

    // Extrai valor de um campo refletido diretamente.
    @Nullable
    private Object getFieldValue(T t, Field field) {
        try {
            return field.get(t);
        } catch (IllegalArgumentException e) {
            // campo não disponível na classe (ex. campo de uma classe 'filha' que não está na classe 'pai')
            return null;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "Falha ao acessar campo BI "
                            + field.getName()
                            + " em "
                            + t.getClass().getName(),
                    e);
        }
    }

    /*
     * Extrai valor do campo procurando o mesmo nome na classe concreta da
     * instância e em sua hierarquia. Isso permite reutilizar o mesmo atributo BI
     * entre classes irmãs que repetem o nome da dimensão.
     */
    @Nullable
    private Object getFieldValueByName(T t, String nomeCampo) {
        // tenta obter do cache e, se necessário, busca na classe
        Field field = getCachedField(t.getClass(), nomeCampo);

        if (field == null) {
            return null;
        }

        try {
            return field.get(t);
        } catch (IllegalAccessException illegalAccessException) {
            throw new IllegalStateException(
                    "Falha ao acessar campo BI "
                            + nomeCampo
                            + " em "
                            + t.getClass().getName(),
                    illegalAccessException);
        }
    }

    // Extrai valor de um método refletido diretamente.
    @Nullable
    private Object getMethodValue(T t, Method method) {
        try {
            return method.invoke(t);
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Falha ao invocar metodo BI "
                            + method.getName()
                            + " em "
                            + t.getClass().getName(),
                    e);
        }
    }

    /*
     * Extrai valor do getter procurando o mesmo nome de método na classe
     * concreta da instância e em sua hierarquia.
     */
    @Nullable
    private Object getMethodValueByName(T t, String nomeMetodo) {
        // tenta obter do cache e, se necessário, busca na classe
        Method method = getCachedMethod(t.getClass(), nomeMetodo);

        if (method == null) {
            return null;
        }

        try {
            return method.invoke(t);
        } catch (ReflectiveOperationException | IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Falha ao invocar metodo BI "
                            + nomeMetodo
                            + " em "
                            + t.getClass().getName(),
                    exception);
        }
    }

    /**
     * Resolve e cacheia o campo na hierarquia da classe concreta.
     * O cache também memoriza miss para evitar exceções repetidas quando o
     * atributo não existe naquela família de classes.
     */
    @Nullable
    private Field getCachedField(Class<?> classeConcreta, String nomeCampo) {
        Map<String, Optional<Field>> cacheCamposDaClasse = cacheCampoPorClasseENome.computeIfAbsent(
                classeConcreta,
                classe -> new ConcurrentHashMap<>());

        Optional<Field> fieldOptional = cacheCamposDaClasse.computeIfAbsent(
                nomeCampo,
                nomeCampoBuscado -> {
                    Class<?> classeIterada = classeConcreta;

                    while (classeIterada != null) {
                        try {
                            Field field = classeIterada.getDeclaredField(nomeCampoBuscado);
                            field.setAccessible(true);
                            return Optional.of(field);
                        } catch (NoSuchFieldException noSuchFieldException) {
                            classeIterada = classeIterada.getSuperclass();
                        }
                    }

                    return Optional.empty();
                });

        return fieldOptional.orElse(null);
    }

    /**
     * Resolve e cacheia o getter na hierarquia da classe concreta seguindo a
     * mesma estratégia usada para os campos.
     */
    @Nullable
    private Method getCachedMethod(Class<?> classeConcreta, String nomeMetodo) {
        Map<String, Optional<Method>> cacheMetodosDaClasse = cacheMetodoPorClasseENome.computeIfAbsent(
                classeConcreta,
                classe -> new ConcurrentHashMap<>());

        Optional<Method> methodOptional = cacheMetodosDaClasse.computeIfAbsent(
                nomeMetodo,
                nomeMetodoBuscado -> {
                    Class<?> classeIterada = classeConcreta;

                    while (classeIterada != null) {
                        try {
                            Method method = classeIterada.getDeclaredMethod(nomeMetodoBuscado);
                            method.setAccessible(true);
                            return Optional.of(method);
                        } catch (NoSuchMethodException noSuchMethodException) {
                            classeIterada = classeIterada.getSuperclass();
                        }
                    }

                    return Optional.empty();
                });

        return methodOptional.orElse(null);
    }

}
