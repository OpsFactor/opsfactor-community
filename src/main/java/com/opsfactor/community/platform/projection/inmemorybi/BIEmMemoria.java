package com.opsfactor.community.platform.projection.inmemorybi;

import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.ObjectLockingIndexedCollection;
import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.attribute.support.SimpleFunction;
import com.googlecode.cqengine.index.hash.HashIndex;
import com.googlecode.cqengine.index.navigable.NavigableIndex;
import com.googlecode.cqengine.index.unique.UniqueIndex;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.query.QueryFactory;
import com.googlecode.cqengine.resultset.ResultSet;
import com.opsfactor.community.platform.exception.InMemoryBIException;
import lombok.Getter;
import org.javatuples.Pair;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

/**
 * BI em memoria indexado por CQEngine para projections de calculo.
 *
 * <p>Rotinas Community devem consultar projections ja materializadas em vez de
 * disparar consultas JPA dentro de loops. Esta classe centraliza os indices e
 * filtros usados por essas projections.</p>
 */
public class BIEmMemoria <T> {

    @Getter
    private final Class<T> persistentClass;

    // O ObjectLockingIndexedCollection oferece proteção nas escritas de um mesmo objeto e o ConcurrentIndexedCollection apenas nas inserções de novos objetos
    IndexedCollection<T> biEmMemoria = new ObjectLockingIndexedCollection<T>();//new ConcurrentIndexedCollection<T>();
    
    public enum AttributeType {
        BOOLEAN, INTEGER, LONG, DOUBLE, FLOAT, STRING, LOCALDATE, LOCALDATETIME, OBJECT, ENUM
    }

    /**
     * Filtro tipado para consultas do BI em memória.
     * Foi introduzido como substituto progressivo do uso direto de {@link Pair},
     * mantendo os métodos legados em paralelo para compatibilidade retroativa.
     *
     * Equivalências com Pair:
     * - value0 -> nomeAtributo
     * - value1 -> valorAtributo
     */
    public static record FiltroDimensao(String nomeAtributo, Object valorAtributo) {

        public static FiltroDimensao of(String nomeAtributo, Object valorAtributo) {
            return new FiltroDimensao(nomeAtributo, valorAtributo);
        }

        public static FiltroDimensao with(String nomeAtributo, Object valorAtributo) {
            return new FiltroDimensao(nomeAtributo, valorAtributo);
        }

        public static FiltroDimensao fromPair(Pair<String, ?> pairNomeValor) {
            return new FiltroDimensao(pairNomeValor.getValue0(), pairNomeValor.getValue1());
        }

        public static FiltroDimensao[] fromPairs(Pair<String, ?>... paresNomeValor) {
            return Arrays.stream(paresNomeValor)
                    .map(FiltroDimensao::fromPair)
                    .toArray(FiltroDimensao[]::new);
        }

        public static FiltroDimensao fromEntry(Map.Entry<String, ?> entry) {
            return new FiltroDimensao(entry.getKey(), entry.getValue());
        }

        public static FiltroDimensao[] fromMap(Map<String, ?> mapaNomeValor) {
            return mapaNomeValor.entrySet().stream()
                    .map(FiltroDimensao::fromEntry)
                    .toArray(FiltroDimensao[]::new);
        }

        public Pair<String, Object> toPair() {
            return Pair.with(nomeAtributo, valorAtributo);
        }

        public String getNomeAtributo() {
            return nomeAtributo;
        }

        public Object getValorAtributo() {
            return valorAtributo;
        }

        /**
         * Métodos análogos ao Pair para facilitar migração incremental.
         */
        public String getValue0() {
            return nomeAtributo;
        }

        public Object getValue1() {
            return valorAtributo;
        }
    }

    /**
     * Helper de varargs para manter legibilidade e equivalência com Pair.with(...).
     */
    public static FiltroDimensao filtroDimensao(String nomeAtributo, Object valorAtributo) {
        return FiltroDimensao.with(nomeAtributo, valorAtributo);
    }
    
    Map<String,AttributeType> attributeTypes = new HashMap<>();

    Attribute<T, String> subclassNameAttribute;
    Map<String,Attribute<T,Boolean>> booleanAttributes =  new HashMap<>();
    Map<String,Attribute<T,Integer>> integerAttributes =  new HashMap<>();
    Map<String,Attribute<T,Long>> longAttributes =  new HashMap<>();
    Map<String,Attribute<T,Double>> doubleAttributes =  new HashMap<>();
    Map<String,Attribute<T,Float>> floatAttributes =  new HashMap<>();
    Map<String,Attribute<T,String>> stringAttributes =  new HashMap<>();
    Map<String,Attribute<T,LocalDateComparable>> localDateAttributes =  new HashMap<>();
    Map<String,Attribute<T,LocalDateTimeComparable>> localDateTimeAttributes =  new HashMap<>();
    Map<String,Attribute<T,Enum>> enumAttributes =  new HashMap<>();
    Map<String,Pair<Class,Attribute<T,?>>> objectAttributes =  new HashMap<>();

    HashIndex<String,T> subclassNameIndex;
    Map<String,NavigableIndex<Boolean,T>> booleanIndexes =  new HashMap<>();
    Map<String,NavigableIndex<Integer,T>> integerIndexes =  new HashMap<>();
    Map<String,NavigableIndex<Long,T>> longIndexes =  new HashMap<>();
    Map<String,NavigableIndex<Double,T>> doubleIndexes =  new HashMap<>();
    Map<String,NavigableIndex<Float,T>> floatIndexes =  new HashMap<>();
    Map<String,NavigableIndex<String,T>> stringIndexes =  new HashMap<>();
    Map<String,HashIndex<String,T>> stringHashIndexes =  new HashMap<>();
    Map<String,UniqueIndex<String,T>> stringUniqueIndexes =  new HashMap<>();
    Map<String,NavigableIndex<LocalDateComparable,T>> localDateIndexes =  new HashMap<>();
    Map<String,NavigableIndex<LocalDateTimeComparable,T>> localDateTimeIndexes =  new HashMap<>();
    Map<String,HashIndex<Enum,T>> enumIndexes =  new HashMap<>();
    /*
     * Índices de dimensões OBJECT usam HashIndex por padrão.
     *
     * O uso atual dessas dimensões no BI é por igualdade exata
     * (QueryFactory.equal) e listagem de chaves distintas. Para esses casos,
     * HashIndex preserva a semântica e evita o custo de ordenação exigido por
     * NavigableIndex. Esse custo aparece principalmente em projections grandes,
     * nas quais cada inserção reindexa objetos de domínio complexos e aciona
     * compareTo em classes como VersaoProducao, Roteiro e ListaTecnica. Se algum
     * fluxo futuro precisar de range/maior/menor sobre OBJECT, esse campo deverá
     * ganhar uma estratégia explícita de índice por atributo em vez de voltar o
     * padrão global para NavigableIndex.
     */
    Map<String,Pair<Class,HashIndex<?,T>>> objectIndexes =  new HashMap<>();

    public BIEmMemoria(Class<T> classeObjetoArmazenado) {
        this.persistentClass = classeObjetoArmazenado;

        // cria o atributo e índice usados para indexar os objetos adicionados pela sua subclasse
        subclassNameAttribute = QueryFactory.attribute(
                persistentClass,
                String.class,
                "ClassAttribute",
                new SimpleFunction<T, String>() {
                    @Override
                    public String apply(T t) {
                        return t.getClass().getSimpleName(); // obtém o nome da subclasse que extende T
                    }
                });
        subclassNameIndex = HashIndex.onAttribute(subclassNameAttribute);
        biEmMemoria.addIndex(subclassNameIndex);
    }
    public boolean hasAttribute(String nomeAtributo) {
        return attributeTypes.containsKey(nomeAtributo);
    }
    
    public boolean contains(T element) {
        return biEmMemoria.contains(element);
    }
    
    public void addBooleanAttribute(String nomeAtributo, SimpleFunction<T,Boolean> extracaoValorBooleanDeT, boolean criarIndice) {
       
        Attribute<T,Boolean> booleanAttribute = QueryFactory.nullableAttribute(persistentClass, Boolean.class, nomeAtributo, extracaoValorBooleanDeT);
        booleanAttributes.put(nomeAtributo, booleanAttribute);
        attributeTypes.put(nomeAtributo, AttributeType.BOOLEAN);
        
        if (criarIndice) {
            NavigableIndex<Boolean,T> index = NavigableIndex.onAttribute(booleanAttribute);
            booleanIndexes.put(nomeAtributo, index);
            biEmMemoria.addIndex(index);
        }
        
    }
    public void addIntegerAttribute(String nomeAtributo, SimpleFunction<T,Integer> extracaoValorIntegerDeT, boolean criarIndice) {
        
        Attribute<T,Integer> integerAttribute = QueryFactory.nullableAttribute(persistentClass, Integer.class, nomeAtributo, extracaoValorIntegerDeT);
        integerAttributes.put(nomeAtributo, integerAttribute);
        attributeTypes.put(nomeAtributo, AttributeType.INTEGER);
        
        if (criarIndice) {
            NavigableIndex<Integer,T> index = NavigableIndex.onAttribute(integerAttribute);
            integerIndexes.put(nomeAtributo, index);
            biEmMemoria.addIndex(index);
        }
        
    }
    public void addLongAttribute(String nomeAtributo, SimpleFunction<T,Long> extracaoValorLongDeT, boolean criarIndice) {
        
        Attribute<T,Long> longAttribute = QueryFactory.nullableAttribute(persistentClass, Long.class, nomeAtributo, extracaoValorLongDeT);
        longAttributes.put(nomeAtributo, longAttribute);
        attributeTypes.put(nomeAtributo, AttributeType.LONG);
        
        if (criarIndice) {
            NavigableIndex<Long,T> index = NavigableIndex.onAttribute(longAttribute);
            longIndexes.put(nomeAtributo, index);
            biEmMemoria.addIndex(index);
        }
        
    }
    public void addDoubleAttribute(String nomeAtributo, SimpleFunction<T,Double> extracaoValorDoubleDeT, boolean criarIndice) {
        
        Attribute<T,Double> doubleAttribute = QueryFactory.nullableAttribute(persistentClass, Double.class, nomeAtributo, extracaoValorDoubleDeT);
        doubleAttributes.put(nomeAtributo, doubleAttribute);
        attributeTypes.put(nomeAtributo, AttributeType.DOUBLE);
        
        if (criarIndice) {
            NavigableIndex<Double,T> index = NavigableIndex.onAttribute(doubleAttribute);
            doubleIndexes.put(nomeAtributo, index);
            biEmMemoria.addIndex(index);
        }
        
    }
    public void addFloatAttribute(String nomeAtributo, SimpleFunction<T,Float> extracaoValorFloatDeT, boolean criarIndice) {
        
        Attribute<T,Float> floatAttribute = QueryFactory.nullableAttribute(persistentClass, Float.class, nomeAtributo, extracaoValorFloatDeT);
        floatAttributes.put(nomeAtributo, floatAttribute);
        attributeTypes.put(nomeAtributo, AttributeType.FLOAT);
        
        if (criarIndice) {
            NavigableIndex<Float,T> index = NavigableIndex.onAttribute(floatAttribute);
            floatIndexes.put(nomeAtributo, index);
            biEmMemoria.addIndex(index);
        }
        
    }
    public void addStringAttribute(String nomeAtributo, SimpleFunction<T,String> extracaoValorStringDeT, boolean criarIndice) {
        
        Attribute<T,String> stringAttribute = QueryFactory.nullableAttribute(persistentClass, String.class, nomeAtributo, extracaoValorStringDeT);
        stringAttributes.put(nomeAtributo, stringAttribute);
        attributeTypes.put(nomeAtributo, AttributeType.STRING);
        
        if (criarIndice) {
            NavigableIndex<String,T> index = NavigableIndex.onAttribute(stringAttribute);
            stringIndexes.put(nomeAtributo, index);
            biEmMemoria.addIndex(index);
        }
        
    }

    /**
     * Registra um atributo textual usando {@link HashIndex}.
     * <p>
     * O {@code NavigableIndex} continua sendo o padrão para atributos string que
     * podem participar de consultas ordenadas/range. Este método cobre chaves
     * técnicas usadas apenas por igualdade exata, como a chave primária composta
     * gerada pelo {@code BIProjectionAnotacoes}; nesses casos o hash evita o
     * custo de comparação ordenada do índice navegável.
     */
    public void addStringAttributeHashIndex(String nomeAtributo, SimpleFunction<T,String> extracaoValorStringDeT, boolean criarIndice) {

        Attribute<T,String> stringAttribute = QueryFactory.nullableAttribute(persistentClass, String.class, nomeAtributo, extracaoValorStringDeT);
        stringAttributes.put(nomeAtributo, stringAttribute);
        attributeTypes.put(nomeAtributo, AttributeType.STRING);

        if (criarIndice) {
            HashIndex<String,T> index = HashIndex.onAttribute(stringAttribute);
            stringHashIndexes.put(nomeAtributo, index);
            biEmMemoria.addIndex(index);
        }

    }

    /**
     * Registra um atributo textual único usando {@link UniqueIndex}.
     * <p>
     * Este índice é reservado para chaves técnicas de identidade, como a chave
     * primária composta do {@code BIProjectionAnotacoes}. Diferente do
     * {@link HashIndex}, o {@code UniqueIndex} guarda no CQEngine um único
     * objeto por chave e falha explicitamente se houver duplicidade. Assim o BI
     * preserva a semântica de chave primária e evita alocar conjuntos/iteradores
     * para cada consulta quente de {@code getOrAdd}.
     */
    public void addStringAttributeUniqueIndex(String nomeAtributo, SimpleFunction<T,String> extracaoValorStringDeT, boolean criarIndice) {

        Attribute<T,String> stringAttribute = QueryFactory.nullableAttribute(persistentClass, String.class, nomeAtributo, extracaoValorStringDeT);
        stringAttributes.put(nomeAtributo, stringAttribute);
        attributeTypes.put(nomeAtributo, AttributeType.STRING);

        if (criarIndice) {
            UniqueIndex<String,T> index = UniqueIndex.onAttribute(stringAttribute);
            stringUniqueIndexes.put(nomeAtributo, index);
            biEmMemoria.addIndex(index);
        }

    }
    public void addLocalDateAttribute(String nomeAtributo, SimpleFunction<T,LocalDate> extracaoValorLocalDateDeT, boolean criarIndice) {
        
        Attribute<T,LocalDateComparable> localDateAttribute = QueryFactory.nullableAttribute(persistentClass, LocalDateComparable.class, nomeAtributo, (SimpleFunction < T, LocalDateComparable >) t -> new LocalDateComparable(extracaoValorLocalDateDeT.apply(t)));
        localDateAttributes.put(nomeAtributo, localDateAttribute);
        attributeTypes.put(nomeAtributo, AttributeType.LOCALDATE);
        
        if (criarIndice) {
            NavigableIndex<LocalDateComparable,T> index = NavigableIndex.onAttribute(localDateAttribute);
            localDateIndexes.put(nomeAtributo, index);
            biEmMemoria.addIndex(index);
        }
        
    }
    public void addLocalDateTimeAttribute(String nomeAtributo, SimpleFunction<T,LocalDateTime> extracaoValorLocalDateTimeDeT, boolean criarIndice) {
        
        Attribute<T,LocalDateTimeComparable> localDateTimeAttribute = QueryFactory.nullableAttribute(persistentClass, LocalDateTimeComparable.class, nomeAtributo, (SimpleFunction < T, LocalDateTimeComparable >) t -> new LocalDateTimeComparable(extracaoValorLocalDateTimeDeT.apply(t)));
        localDateTimeAttributes.put(nomeAtributo, localDateTimeAttribute);
        attributeTypes.put(nomeAtributo, AttributeType.LOCALDATETIME);
        
        if (criarIndice) {
            NavigableIndex<LocalDateTimeComparable,T> index = NavigableIndex.onAttribute(localDateTimeAttribute);
            localDateTimeIndexes.put(nomeAtributo, index);
            biEmMemoria.addIndex(index);
        }
        
    }
    /**
     * Registra uma dimensão de objeto consultada por igualdade.
     *
     * <p>O tipo não precisa implementar {@link Comparable}: dimensões de
     * entidade usam {@link HashIndex}, cuja semântica depende de
     * {@code equals/hashCode}, não de ordenação. Consultas de range exigem um
     * atributo e um índice específicos.</p>
     */
    public <A> void addObjectAttribute(String nomeAtributo, Class<A> classeAtributo, SimpleFunction<T,A> extracaoValorObjetoDeT, boolean criarIndice) {
        
        // https://github.com/npgall/cqengine/blob/master/documentation/LambdaAttributes.md#specifying-generic-types-explicitly
        Attribute<T,A> objectAttribute = QueryFactory.nullableAttribute(persistentClass, classeAtributo, nomeAtributo, extracaoValorObjetoDeT);
        objectAttributes.put(nomeAtributo, Pair.with(classeAtributo, objectAttribute));
        attributeTypes.put(nomeAtributo, AttributeType.OBJECT);


        if (criarIndice) {
            /*
             * OBJECT é indexado por hash porque o BI usa esses atributos para
             * igualdade exata. Mantê-los em árvore navegável força comparação
             * ordenada a cada inserção, mesmo quando nenhuma consulta de range é
             * executada. No Supply Plan canônico isso domina o custo de
             * addElementoNoBI em dimensões de produção com compareTo caro.
             */
            HashIndex<A,T> index = HashIndex.onAttribute(objectAttribute);
            objectIndexes.put(nomeAtributo, Pair.with(classeAtributo, index));
            biEmMemoria.addIndex(index);
        }
        
    }
    public void addEnumAttribute(String nomeAtributo, SimpleFunction<T,Enum> extracaoValorEnumDeT, boolean criarIndice) {
        
        Attribute<T,Enum> enumAttribute = QueryFactory.nullableAttribute(persistentClass, Enum.class, nomeAtributo, extracaoValorEnumDeT);
        enumAttributes.put(nomeAtributo, enumAttribute);
        attributeTypes.put(nomeAtributo, AttributeType.ENUM);
        
        if (criarIndice) {
            HashIndex<Enum,T> index = HashIndex.onAttribute(enumAttribute);
            enumIndexes.put(nomeAtributo, index);
            biEmMemoria.addIndex(index);
        }
        
    }
    
    public Set<Boolean> getChavesIndiceBoolean(String nomeAtributo) {
        
        if (!booleanIndexes.containsKey(nomeAtributo)) {
            throw getIndiceNaoConfiguradoException(nomeAtributo, "boolean");
        }
        
        Set<Boolean> set = new HashSet<>();
        booleanIndexes.get(nomeAtributo).getDistinctKeys(QueryFactory.noQueryOptions())
                .forEach(x -> set.add(x));
        
        return set;
        
    }
    public Set<Integer> getChavesIndiceInteger(String nomeAtributo) {
        
        if (!integerIndexes.containsKey(nomeAtributo)) {
            throw getIndiceNaoConfiguradoException(nomeAtributo, "integer");
        }
        
        Set<Integer> set = new HashSet<>();
        integerIndexes.get(nomeAtributo).getDistinctKeys(QueryFactory.noQueryOptions())
                .forEach(x -> set.add(x));
        
        return set;
        
    }
    public Set<Long> getChavesIndiceLong(String nomeAtributo) {
        
        if (!longIndexes.containsKey(nomeAtributo)) {
            throw getIndiceNaoConfiguradoException(nomeAtributo, "long");
        }
        
        Set<Long> set = new HashSet<>();
        longIndexes.get(nomeAtributo).getDistinctKeys(QueryFactory.noQueryOptions())
                .forEach(x -> set.add(x));
        
        return set;
        
    }
    public Set<Float> getChavesIndiceFloat(String nomeAtributo) {
        
        if (!floatIndexes.containsKey(nomeAtributo)) {
            throw getIndiceNaoConfiguradoException(nomeAtributo, "float");
        }
        
        Set<Float> set = new HashSet<>();
        floatIndexes.get(nomeAtributo).getDistinctKeys(QueryFactory.noQueryOptions())
                .forEach(x -> set.add(x));
        
        return set;
        
    }
    public Set<Double> getChavesIndiceDouble(String nomeAtributo) {
        
        if (!doubleIndexes.containsKey(nomeAtributo)) {
            throw getIndiceNaoConfiguradoException(nomeAtributo, "double");
        }
        
        Set<Double> set = new HashSet<>();
        doubleIndexes.get(nomeAtributo).getDistinctKeys(QueryFactory.noQueryOptions())
                .forEach(x -> set.add(x));
        
        return set;
        
    }
    public Set<String> getChavesIndiceString(String nomeAtributo) {
        
        if (!stringIndexes.containsKey(nomeAtributo)) {
            throw getIndiceNaoConfiguradoException(nomeAtributo, "string");
        }
        
        Set<String> set = new HashSet<>();
        stringIndexes.get(nomeAtributo).getDistinctKeys(QueryFactory.noQueryOptions())
                .forEach(x -> set.add(x));
        
        return set;
        
    }
    public Set<LocalDate> getChavesIndiceLocalDate(String nomeAtributo) {
        
        if (!localDateIndexes.containsKey(nomeAtributo)) {
            throw getIndiceNaoConfiguradoException(nomeAtributo, "Date");
        }
        
        Set<LocalDate> set = new HashSet<>();
        localDateIndexes.get(nomeAtributo).getDistinctKeys(QueryFactory.noQueryOptions())
                .forEach(x -> set.add(x.localDate));
        
        return set;
        
    }
    public Set<LocalDateTime> getChavesIndiceLocalDateTime(String nomeAtributo) {
        
        if (!localDateTimeIndexes.containsKey(nomeAtributo)) {
            throw getIndiceNaoConfiguradoException(nomeAtributo, "Date/Time");
        }
        
        Set<LocalDateTime> set = new HashSet<>();
        localDateTimeIndexes.get(nomeAtributo).getDistinctKeys(QueryFactory.noQueryOptions())
                .forEach(x -> set.add(x.localDateTime));
        
        return set;
        
    }
    public Set<Enum> getChavesIndiceEnum(String nomeAtributo) {
        
        if (!enumIndexes.containsKey(nomeAtributo)) {
            throw getIndiceNaoConfiguradoException(nomeAtributo, "enum");
        }
        
        Set<Enum> set = new HashSet<>();
        enumIndexes.get(nomeAtributo).getDistinctKeys(QueryFactory.noQueryOptions())
                .forEach(x -> set.add(x));
        
        return set;
        
    }
    public <A> Set<A> getChavesIndiceObject(String nomeAtributo, Class<A> classeObjeto) {

        if (!objectIndexes.containsKey(nomeAtributo)) {
            throw getIndiceNaoConfiguradoException(nomeAtributo, "object");
        }

        Set<A> set = new HashSet<>();

        Pair<Class,HashIndex<?,T>> parClasseEIndice = objectIndexes.get(nomeAtributo);

        Class classe = parClasseEIndice.getValue0();
        if (!classe.isAssignableFrom(classeObjeto)) {
            throw new IllegalArgumentException(
                    "Class " + classeObjeto.getSimpleName()
                            + " does not match object index class " + classe.getSimpleName());
        }

        /*
         * A listagem de chaves distintas também é suportada pelo HashIndex.
         * Esse é o único uso conhecido que depende do índice OBJECT além de
         * igualdade exata, portanto a troca não remove semântica atualmente
         * exercida pelo BI.
         */
        HashIndex<?,T> index = parClasseEIndice.getValue1();
                
        index.getDistinctKeys(QueryFactory.noQueryOptions())
                .forEach(x -> set.add(classeObjeto.cast(x)));
        
        return set;
        
    }
    private Query<T> getParteQuery(Class<? extends T> subClasseIndexada) {
        Query<T> query = QueryFactory.equal(subclassNameAttribute, subClasseIndexada.getSimpleName());
        return query;
    }


    private Query<T> getParteQuery(Pair<String,Comparable<?>> parNomeEValorAtributo) {
        return getParteQuery(parNomeEValorAtributo.getValue0(), parNomeEValorAtributo.getValue1());
    }

    private Query<T> getParteQuery(FiltroDimensao filtroDimensao) {
        return getParteQuery(filtroDimensao.getValue0(), filtroDimensao.getValue1());
    }

    private Query<T> getParteQuery(String nomeAtributo, Object valorAtributo) {
        AttributeType attributeType = attributeTypes.get(nomeAtributo);

        if (attributeType == null) throw new InMemoryBIException("Attribute Type " + nomeAtributo + " not Found");

        Attribute atributo;
        switch (attributeType) {
            case BOOLEAN:
                atributo = booleanAttributes.get(nomeAtributo);
                break;
            case INTEGER:
                atributo = integerAttributes.get(nomeAtributo);
                break;
            case DOUBLE:
                atributo = doubleAttributes.get(nomeAtributo);
                break;
            case FLOAT:
                atributo = floatAttributes.get(nomeAtributo);
                break;
            case STRING:
                atributo = stringAttributes.get(nomeAtributo);
                break;
            case LOCALDATE:
                atributo = localDateAttributes.get(nomeAtributo);
                break;
            case LOCALDATETIME:
                atributo = localDateTimeAttributes.get(nomeAtributo);
                break;
            case ENUM:
                atributo = enumAttributes.get(nomeAtributo);
                break;
            default:
                Pair<Class,Attribute<T,?>> parClasseEAtributo = objectAttributes.get(nomeAtributo);
                atributo = parClasseEAtributo.getValue1();
        }

        if (valorAtributo != null) {

            Query<T> andQuery;
            switch (attributeType) {
                case BOOLEAN:
                    andQuery = QueryFactory.equal(atributo, (Boolean) valorAtributo);
                    break;
                case INTEGER:
                    andQuery = QueryFactory.equal(atributo, (Integer) valorAtributo);
                    break;
                case DOUBLE:
                    andQuery = QueryFactory.equal(atributo, (Double) valorAtributo);
                    break;
                case FLOAT:
                    andQuery = QueryFactory.equal(atributo, (Float) valorAtributo);
                    break;
                case STRING:
                    andQuery = QueryFactory.equal(atributo, (String) valorAtributo);
                    break;
                case LOCALDATE:
                    andQuery = QueryFactory.equal(atributo, new LocalDateComparable((LocalDate) valorAtributo));
                    break;
                case LOCALDATETIME:
                    andQuery = QueryFactory.equal(atributo, new LocalDateTimeComparable((LocalDateTime) valorAtributo));
                    break;
                case ENUM:
                    andQuery = QueryFactory.equal(atributo, (Enum) valorAtributo);
                    break;
                // OBJECT
                default:
                    Pair<Class,Attribute<T,?>> parClasseEAtributo = objectAttributes.get(nomeAtributo);
                    Class classe = parClasseEAtributo.getValue0();
                    andQuery = QueryFactory.equal(atributo, classe.cast(valorAtributo));
            }

            return andQuery;

        // se valor é nulo andQuery não pode ser usada. nesse caso usar not(has(atributo))
        } else {

            Query<T> hasQuery = QueryFactory.not(QueryFactory.has(atributo));

            return hasQuery;

        }
        
    }

    /**
     * Gera query recebendo ou classes ou pares Pair<String,Comparable<?>>
     * @param classeOuParesNomeEValorAtributo
     * @return
     */
    private Query<T> getEqualsQuery(Object... classeOuParesNomeEValorAtributo) {
        
        if (classeOuParesNomeEValorAtributo.length == 0) return QueryFactory.all(persistentClass);
        
        List<Query<T>> andQueryList = new ArrayList<>();
        
        for (int i=0; i<classeOuParesNomeEValorAtributo.length; i++) {

            Query<T> andQuery;
            if (classeOuParesNomeEValorAtributo[i] instanceof Class) {
                andQuery = getParteQuery((Class<? extends T>) classeOuParesNomeEValorAtributo[i]);
            } else if (classeOuParesNomeEValorAtributo[i] instanceof FiltroDimensao) {
                andQuery = getParteQuery((FiltroDimensao) classeOuParesNomeEValorAtributo[i]);
            } else {
                andQuery = getParteQuery((Pair<String,Comparable<?>>) classeOuParesNomeEValorAtributo[i]);
            }

            andQueryList.add(andQuery);
            
        }
        
        if (classeOuParesNomeEValorAtributo.length == 1) {
            return andQueryList.get(0);
        } else if (classeOuParesNomeEValorAtributo.length == 2) {
            return QueryFactory.and(andQueryList.get(0), andQueryList.get(1));
        } else if (classeOuParesNomeEValorAtributo.length == 3) {
            return QueryFactory.and(andQueryList.get(0), andQueryList.get(1), andQueryList.get(2));
        } else if (classeOuParesNomeEValorAtributo.length == 4) {
            return QueryFactory.and(andQueryList.get(0), andQueryList.get(1), andQueryList.get(2),andQueryList.get(3));
        } else if (classeOuParesNomeEValorAtributo.length == 5) {
            return QueryFactory.and(andQueryList.get(0), andQueryList.get(1), andQueryList.get(2),andQueryList.get(3), andQueryList.get(4));
        } else {
            Query<T> andQuery1 = andQueryList.get(0);
            Query<T> andQuery2 = andQueryList.get(1);
            
            andQueryList.remove(andQuery1);
            andQueryList.remove(andQuery2);
            
            return QueryFactory.and(andQuery1, andQuery2, andQueryList);
        }
        
    }

    private Query<T> getEqualsQuery(Class<? extends T>... subClassesAFiltrar) {

        if (subClassesAFiltrar.length == 0) return QueryFactory.all(persistentClass);

        List<Query<T>> andQueryList = new ArrayList<>();

        for (int i=0; i<subClassesAFiltrar.length; i++) {
            Query<T> andQuery = getParteQuery((Class<? extends T>) subClassesAFiltrar[i]);
            andQueryList.add(andQuery);
        }

        if (subClassesAFiltrar.length == 1) {
            return andQueryList.get(0);
        } else if (subClassesAFiltrar.length == 2) {
            return QueryFactory.and(andQueryList.get(0), andQueryList.get(1));
        } else if (subClassesAFiltrar.length == 3) {
            return QueryFactory.and(andQueryList.get(0), andQueryList.get(1), andQueryList.get(2));
        } else if (subClassesAFiltrar.length == 4) {
            return QueryFactory.and(andQueryList.get(0), andQueryList.get(1), andQueryList.get(2),andQueryList.get(3));
        } else if (subClassesAFiltrar.length == 5) {
            return QueryFactory.and(andQueryList.get(0), andQueryList.get(1), andQueryList.get(2),andQueryList.get(3), andQueryList.get(4));
        } else {
            Query<T> andQuery1 = andQueryList.get(0);
            Query<T> andQuery2 = andQueryList.get(1);

            andQueryList.remove(andQuery1);
            andQueryList.remove(andQuery2);

            return QueryFactory.and(andQuery1, andQuery2, andQueryList);
        }

    }

    private Query<T> getEqualsQuery(Pair<String,Comparable<?>>... paresNomeEValorAtributo) {
        
        if (paresNomeEValorAtributo.length == 0) return QueryFactory.all(persistentClass);
        
        List<Query<T>> andQueryList = new ArrayList<>();
        
        for (int i=0; i<paresNomeEValorAtributo.length; i++) {
            
            Query<T> andQuery = getParteQuery(paresNomeEValorAtributo[i]);
            
            andQueryList.add(andQuery);
            
        }
        
        if (paresNomeEValorAtributo.length == 1) {
            return andQueryList.get(0);
        } else if (paresNomeEValorAtributo.length == 2) {
            return QueryFactory.and(andQueryList.get(0), andQueryList.get(1));
        } else {
            Query<T> andQuery1 = andQueryList.get(0);
            Query<T> andQuery2 = andQueryList.get(1);
            
            andQueryList.remove(andQuery1);
            andQueryList.remove(andQuery2);
            
            return QueryFactory.and(andQuery1, andQuery2, andQueryList);
        }
        
    }

    private Query<T> getEqualsQuery(FiltroDimensao... filtrosDimensao) {

        if (filtrosDimensao.length == 0) return QueryFactory.all(persistentClass);

        List<Query<T>> andQueryList = new ArrayList<>();

        for (FiltroDimensao filtroDimensao : filtrosDimensao) {
            Query<T> andQuery = getParteQuery(filtroDimensao);
            andQueryList.add(andQuery);
        }

        if (filtrosDimensao.length == 1) {
            return andQueryList.get(0);
        } else if (filtrosDimensao.length == 2) {
            return QueryFactory.and(andQueryList.get(0), andQueryList.get(1));
        } else {
            Query<T> andQuery1 = andQueryList.get(0);
            Query<T> andQuery2 = andQueryList.get(1);

            andQueryList.remove(andQuery1);
            andQueryList.remove(andQuery2);

            return QueryFactory.and(andQuery1, andQuery2, andQueryList);
        }

    }

    public ResultSet<T> getAllRecords() {
        return biEmMemoria.retrieve(QueryFactory.all(persistentClass));
    }
    
    public <V> ResultSet<T> getWhereEquals(Map<String,V> paresNomeEValorAtributo) {
        
        return biEmMemoria.retrieve(getEqualsQuery(
                /*(Pair<String,Object>[])*/ paresNomeEValorAtributo.entrySet().stream()
                        .map(entry -> Pair.with(entry.getKey(), entry.getValue()))
                        .toArray()));
        
    }

    public ResultSet<T> getWhereEquals(Map<String,Comparable<?>> paresNomeEValorAtributo, Class... classesAFiltrar) {

        Object[] objetosAFiltrar = new Object[paresNomeEValorAtributo.size() + classesAFiltrar.length];

        int posicaoArray = 0;
        for (Map.Entry entry : paresNomeEValorAtributo.entrySet()) {
            objetosAFiltrar[posicaoArray] = Pair.with(entry.getKey(), entry.getValue());
            posicaoArray++;
        }

        for (Class classeAFiltrar : classesAFiltrar) {
            objetosAFiltrar[posicaoArray] = classeAFiltrar;
            posicaoArray++;
        }

        return biEmMemoria.retrieve(getEqualsQuery(objetosAFiltrar));

    }

    public <V> ResultSet<T> getWhereEquals(Class... classesAFiltrar) {
        return biEmMemoria.retrieve(getEqualsQuery(classesAFiltrar));
    }

    public ResultSet<T> getWhereEquals(Collection<Pair<String,Comparable<?>>> paresNomeEValorAtributo) {
        
        return biEmMemoria.retrieve(getEqualsQuery(/*(Pair<String,Object>[])*/ paresNomeEValorAtributo.stream().toArray()));
        
    }

    public ResultSet<T> getWhereEquals(FiltroDimensao... filtrosDimensao) {
        return biEmMemoria.retrieve(getEqualsQuery(filtrosDimensao));
    }

    /**
     * Consulta diretamente um índice textual único.
     * <p>
     * O método existe para evitar o caminho genérico do {@code CollectionQueryEngine}
     * quando o caller já sabe que está procurando por uma chave técnica única.
     * Para filtros comuns ou combinações de dimensões, continuar usando
     * {@link #getWhereEquals(FiltroDimensao...)} preserva o comportamento
     * analítico do BI.
     */
    public Optional<T> getWhereEqualsStringUniqueIndex(String nomeAtributo, String valorAtributo) {

        UniqueIndex<String,T> index = stringUniqueIndexes.get(nomeAtributo);
        if (index == null) {
            throw getIndiceNaoConfiguradoException(nomeAtributo, "string unique");
        }

        Attribute<T,String> atributo = stringAttributes.get(nomeAtributo);
        if (atributo == null) {
            throw new InMemoryBIException("Attribute " + nomeAtributo + " not Found");
        }

        ResultSet<T> resultSet = index.retrieve(
                QueryFactory.equal(atributo, valorAtributo),
                QueryFactory.noQueryOptions());

        try {
            Iterator<T> iterator = resultSet.iterator();
            if (!iterator.hasNext()) {
                return Optional.empty();
            }

            T elementoEncontrado = iterator.next();
            if (iterator.hasNext()) {
                throw new IllegalStateException("Mais de um registro encontrado para indice unico " + nomeAtributo);
            }

            return Optional.of(elementoEncontrado);
        } finally {
            resultSet.close();
        }

    }

    /**
     * Variante tipada com {@link FiltroDimensao} para evitar uso direto de Pair.
     * O nome distinto do método evita conflito de type erasure com os métodos legados.
     */
    public ResultSet<T> getWhereEqualsFiltrosDimensao(Collection<FiltroDimensao> filtrosDimensao) {
        return biEmMemoria.retrieve(getEqualsQuery(filtrosDimensao.toArray(new FiltroDimensao[0])));
    }

    public ResultSet<T> getWhereEquals(Collection<Pair<String,Comparable<?>>> paresNomeEValorAtributo, Class... classesAFiltrar) {

        Object[] objetosAFiltrar = new Object[paresNomeEValorAtributo.size() + classesAFiltrar.length];

        int posicaoArray = 0;
        for (Pair<String,Comparable<?>> parAtributoValor : paresNomeEValorAtributo) {
            objetosAFiltrar[posicaoArray] = parAtributoValor;
            posicaoArray++;
        }

        for (Class classeAFiltrar : classesAFiltrar) {
            objetosAFiltrar[posicaoArray] = classeAFiltrar;
            posicaoArray++;
        }

        return biEmMemoria.retrieve(getEqualsQuery(objetosAFiltrar));

    }

    /**
     * Variante tipada com {@link FiltroDimensao} + filtro por subclasse.
     * O nome distinto do método evita conflito de type erasure com os métodos legados.
     */
    public ResultSet<T> getWhereEqualsFiltrosDimensao(Collection<FiltroDimensao> filtrosDimensao, Class... classesAFiltrar) {
        Object[] objetosAFiltrar = new Object[filtrosDimensao.size() + classesAFiltrar.length];

        int posicaoArray = 0;
        for (FiltroDimensao filtroDimensao : filtrosDimensao) {
            objetosAFiltrar[posicaoArray] = filtroDimensao;
            posicaoArray++;
        }

        for (Class classeAFiltrar : classesAFiltrar) {
            objetosAFiltrar[posicaoArray] = classeAFiltrar;
            posicaoArray++;
        }

        return biEmMemoria.retrieve(getEqualsQuery(objetosAFiltrar));
    }

    public ResultSet<T> getWhereEquals(Pair<String,Comparable<?>>... paresNomeEValorAtributo) {

        return biEmMemoria.retrieve(getEqualsQuery(paresNomeEValorAtributo));
        
    }

    public ResultSet<T> getWhereEquals(Object... paresNomeEValorAtributoEClassesAFiltrar) {

        return biEmMemoria.retrieve(getEqualsQuery(paresNomeEValorAtributoEClassesAFiltrar));

    }

    public Double getWhereEqualsConsolidadoEmDouble(ToDoubleFunction<T> funcaoExtratoraValorDouble, Map<String,Object> paresNomeEValorAtributo) {
        
        return getWhereEquals(paresNomeEValorAtributo).stream()
                .mapToDouble(funcaoExtratoraValorDouble)
                .sum();
        
    }

    public Double getWhereEqualsConsolidadoEmDouble(ToDoubleFunction<T> funcaoExtratoraValorDouble, Map<String,Object> paresNomeEValorAtributo, Class... classesAFiltrar) {

        return getWhereEquals(paresNomeEValorAtributo).stream()
                .mapToDouble(funcaoExtratoraValorDouble)
                .sum();

    }

    public Double getWhereEqualsConsolidadoEmDouble(ToDoubleFunction<T> funcaoExtratoraValorDouble, Collection<Pair<String,Object>> paresNomeEValorAtributo) {
        
        return getWhereEquals(paresNomeEValorAtributo).stream()
                .mapToDouble(funcaoExtratoraValorDouble)
                .sum();
        
    }

    public Double getWhereEqualsConsolidadoEmDouble(ToDoubleFunction<T> funcaoExtratoraValorDouble, Collection<Pair<String,Object>> paresNomeEValorAtributo, Class... classesAFiltrar) {

        return getWhereEquals(paresNomeEValorAtributo, classesAFiltrar).stream()
                .mapToDouble(funcaoExtratoraValorDouble)
                .sum();

    }

    public Double getWhereEqualsConsolidadoEmDouble(ToDoubleFunction<T> funcaoExtratoraValorDouble, Pair<String,Object>... paresNomeEValorAtributo) {
        
        return getWhereEquals(paresNomeEValorAtributo).stream()
                .mapToDouble(funcaoExtratoraValorDouble)
                .sum();

    }

    public Double getWhereEqualsConsolidadoEmDouble(ToDoubleFunction<T> funcaoExtratoraValorDouble, Object... paresNomeEValorAtributoEFiltrosClasse) {

        return getWhereEquals(paresNomeEValorAtributoEFiltrosClasse).stream()
                .mapToDouble(funcaoExtratoraValorDouble)
                .sum();

    }

    public Float getWhereEqualsConsolidadoEmFloat(Function<T,Float> funcaoExtratoraValorFloat, Map<String,Object> paresNomeEValorAtributo) {
        
        return getWhereEquals(paresNomeEValorAtributo).stream()
                .map(funcaoExtratoraValorFloat)
                .reduce(0f, (a,b) -> a + b);
        
    }
    
    public Float getWhereEqualsConsolidadoEmFloat(Function<T,Float> funcaoExtratoraValorFloat, Collection<Pair<String,Object>> paresNomeEValorAtributo) {
        
        return getWhereEquals(paresNomeEValorAtributo).stream()
                .map(funcaoExtratoraValorFloat)
                .reduce(0f, (a,b) -> a + b);
        
    }
    
    public Float getWhereEqualsConsolidadoEmFloat(Function<T,Float> funcaoExtratoraValorFloat, Pair<String,Object>... paresNomeEValorAtributo) {
        
        return getWhereEquals(paresNomeEValorAtributo).stream()
                .map(funcaoExtratoraValorFloat)
                .reduce(0f, (a,b) -> a + b);
        
    }
        
    public void addElementoNoBI(T t) {
        biEmMemoria.add(t);
    }

    public void addElementosNoBI(Collection<T> collectionT) {
        biEmMemoria.addAll(collectionT);
    }
    
    public void removeElementoDoBI(T t) {
        biEmMemoria.remove(t);
    }

    public void removeElementosDoBI(Collection<T> collectionT) {
        biEmMemoria.removeAll(collectionT);
    }

    /**
     *
     * @param funcaoVerificaSeRemoveElemento se true, elemento deverá ser removido do BI
     */
    public void removeElementosDoBISeCondicao(Function<T,Boolean> funcaoVerificaSeRemoveElemento) {
        List<T> valoresFiltrados = biEmMemoria
                .stream()
                .filter(t -> funcaoVerificaSeRemoveElemento.apply(t))
                .toList();
        biEmMemoria.removeAll(valoresFiltrados);
    }

    public int getNumeroTotalElementos() {
        return biEmMemoria.size();
    }
    
    public Stream<T> getStreamTodasLinhas() {
        return getAllRecords().stream();
    }
    
    public void setValorTopDown(
            Double valor, 
            ToDoubleFunction<T> funcaoExtratoraValorDouble,
            ObjDoubleConsumer<T> consumerSetterValorDouble,
            Pair<String,Object>... paresNomeEValorAtributo) {
        
        ResultSet<T> resultSetObjetosArmazenadosBI = getWhereEquals(paresNomeEValorAtributo);
        
        if (resultSetObjetosArmazenadosBI.isEmpty()) return;
        
        double valorAcumulado = resultSetObjetosArmazenadosBI.stream()
                .mapToDouble(funcaoExtratoraValorDouble)
                .sum();
        
        // se já houver valor acumulado existente, realiza um split proporcional aos valores pré-existentes
        if (valorAcumulado != 0) {
            resultSetObjetosArmazenadosBI.stream()
                    .forEach(t -> {
                        double valorAtual = funcaoExtratoraValorDouble.applyAsDouble(t);
                        double novoValor = (valor / valorAcumulado) * valorAtual;
                        consumerSetterValorDouble.accept(t, novoValor);
                    });
        } else {
            int numeroRegistrosEncontrados = resultSetObjetosArmazenadosBI.size();
            resultSetObjetosArmazenadosBI.stream()
                    .forEach(t -> {
                        double novoValor = valor / numeroRegistrosEncontrados;
                        consumerSetterValorDouble.accept(t, novoValor);
                    });
        }
        
    }
    
    public IndexedCollection<T> getIndexedCollection() {
        return biEmMemoria;
    }
        
    public Attribute<T,Boolean> getBooleanAttribute(String attributeName) {
        return booleanAttributes.get(attributeName);
    }
    public Attribute<T,Long> getLongAttribute(String attributeName) {
        return longAttributes.get(attributeName);
    }
    public Attribute<T,Integer> getIntegerAttribute(String attributeName) {
        return integerAttributes.get(attributeName);
    }
    public Attribute<T,Double> getDoubleAttribute(String attributeName) {
        return doubleAttributes.get(attributeName);
    }
    public Attribute<T,Float> getFloatAttribute(String attributeName) {
        return floatAttributes.get(attributeName);
    }
    public Attribute<T,String> getStringAttribute(String attributeName) {
        return stringAttributes.get(attributeName);
    }
    public Attribute<T,LocalDateComparable> getLocalDateAttribute(String attributeName) {
        return localDateAttributes.get(attributeName);
    }
    public Attribute<T,LocalDateTimeComparable> getLocalDateTimeAttribute(String attributeName) {
        return localDateTimeAttributes.get(attributeName);
    }
    public Attribute<T,Enum> getEnumAttribute(String attributeName) {
        return enumAttributes.get(attributeName);
    }
    public Pair<Class, Attribute<T,?>> getObjectClassAndAttribute(String attributeName) {
        return objectAttributes.get(attributeName);
    }
    public Attribute<T,?> getObjectAttribute(String attributeName) {
        return getObjectClassAndAttribute(attributeName).getValue1();
    }

    /**
     * Falha quando o caller pede chaves ou lookup direto de um indice que nao
     * foi criado para o atributo. Isso representa configuracao inconsistente do
     * BI/projection ou uso incorreto da API, nao capability ausente de uma
     * edicao da plataforma.
     */
    private IllegalStateException getIndiceNaoConfiguradoException(String nomeAtributo, String tipoIndice) {

        return new IllegalStateException(
                "Attribute " + nomeAtributo + " does not have a " + tipoIndice + " index");

    }

     
}
