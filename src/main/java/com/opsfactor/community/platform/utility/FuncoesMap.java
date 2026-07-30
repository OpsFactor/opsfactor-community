package com.opsfactor.community.platform.utility;

import org.javatuples.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilitarios para mapas indexados usados por projections em memoria.
 *
 * <p>Projections Community costumam materializar estruturas nested para evitar
 * consultas repetidas ao banco durante calculos de demanda e supply. Estes
 * helpers centralizam flatten, merges e transformacoes sem depender de beans
 * Spring.</p>
 */
public abstract class FuncoesMap {
    
    public static Stream<?> flattenMapToStream(Object o) {
        // se próximo nível é outro mapa, fazer chamada recursiva
        if (o instanceof Map<?, ?>) {
            return ((Map<?, ?>) o).values().stream().flatMap(FuncoesMap::flattenMapToStream);
        }
        // se valores do mapa não são mapas, extrai-los como uma stream
        return Stream.of(o);
    }

    public static <T> Stream<T> flattenMapToStream(Object o, Class<T> classeObjeto) {
        if (o == null) return Stream.empty();
        // se próximo nível é outro mapa, fazer chamada recursiva
        if (o instanceof Map<?, ?>) {
            return ((Map<?, ?>) o).values().stream().flatMap(x -> flattenMapToStream(x, classeObjeto));
        }
        // se a folha já é do tipo esperado, preserva-a como elemento atômico
        if (classeObjeto.isInstance(o)) {
            return Stream.of(classeObjeto.cast(o));
        }
        // se a folha é uma coleção de elementos do tipo esperado, continua o flatten
        if (o instanceof Collection<?>) {
            return ((Collection<?>) o).stream().flatMap(x -> flattenMapToStream(x, classeObjeto));
        }
        // se valores do mapa não são mapas, extrai-los como uma stream
        return Stream.of(classeObjeto.cast(o));
    }
    
    public static Set<?> flattenMapToSet(Object o) {
        
        return flattenMapToStream(o).collect(Collectors.toSet());
        
    }
    
    public static <T> Set<T> flattenMapToSet(Object o, Class<T> classeObjeto) {
        
        return flattenMapToStream(o, classeObjeto).collect(Collectors.toSet());
        
    }
    
    public static List<?> flattenMapToList(Object o) {
        
        return flattenMapToStream(o).collect(Collectors.toList());
        
    }
    
    public static <T> List<T> flattenMapToList(Object o, Class<T> classeObjeto) {
        
        return flattenMapToStream(o, classeObjeto).collect(Collectors.toList());
        
    }
    
    public static <T> Queue<T> flattenMapToQueue(Object o, Class<T> classeObjeto) {
        
        return flattenMapToStream(o, classeObjeto).collect(Collectors.toCollection(LinkedList::new));
        
    }
    
    /**
     * Retorna stream de pares:
     * 1) Lista de objetos da chave indexadora, em sequência
     * 2) Objeto referenciado no último nível do mapa
     * @param map
     * @return 
     */
    public static Stream<Pair<List<?>, ?>> flattenMapToKeyListAndValueStream(Map<?,?> map) {
        
        return map.entrySet().stream().flatMap(entry -> FuncoesMap.flattenMapToKeyListAndValueStream(entry, new ArrayList<>()));
                
    }
    private static Stream<Pair<List<?>, ?>> flattenMapToKeyListAndValueStream(Map.Entry<?,?> entry, List<Object> listaChavesAcumuladas) {
        
        listaChavesAcumuladas.add(entry.getKey());
        
        if (entry.getValue() instanceof Map<?, ?>) {
            return ((Map<?,?>) entry.getValue()).entrySet().stream().flatMap(nestedEntry -> FuncoesMap.flattenMapToKeyListAndValueStream(nestedEntry, new ArrayList(listaChavesAcumuladas)));
        } else {
            return Stream.of(Pair.with(listaChavesAcumuladas, entry.getValue()));
        }
        
    }
    
    public static <T> Stream<Pair<List<?>, T>> flattenMapToKeyListAndValueStream(Map<?,?> map, Class<T> classeObjeto) {
        
        return map.entrySet().stream().flatMap(entry -> FuncoesMap.flattenMapToKeyListAndValueStream(entry, new ArrayList<>(), classeObjeto));
                
    }
    private static <T> Stream<Pair<List<?>, T>> flattenMapToKeyListAndValueStream(Map.Entry<?,?> entry, List<Object> listaChavesAcumuladas, Class<T> classeObjeto) {
        
        listaChavesAcumuladas.add(entry.getKey());
        
        if (entry.getValue() instanceof Map<?, ?>) {
            return ((Map<?,?>) entry.getValue()).entrySet().stream().flatMap(nestedEntry -> FuncoesMap.flattenMapToKeyListAndValueStream(nestedEntry, new ArrayList(listaChavesAcumuladas), classeObjeto));
        } else {
            return Stream.of(Pair.with(listaChavesAcumuladas, (T) entry.getValue()));
        }
        
    }
    
    /**
     * Transforma todos os sub-mapas em mapas UnmodifiableMaps
     * Interrompe o processamento 
     * @param map 
     */
    public static void convertToNestedUnmodifiableMap(Map map) {
        map = Collections.unmodifiableMap(map);
        for (Object value : map.values()) {
            if (value instanceof Map) {
                convertToNestedUnmodifiableMap((Map) value);
            } else if (value instanceof Set) {
                value = FuncoesCollections.convertToNestedUnmodifiableSet((Set) value);
            } else if (value instanceof List) {
                value = FuncoesCollections.convertToNestedUnmodifiableList((List) value);
            } else if (value instanceof Collection) {
                value = FuncoesCollections.convertToNestedUnmodifiableCollection((Collection) value);
            }
        }        
    }
    
    /**
     * Adiciona elemento ao mapa SEM SUBSTITUIR um eventual elemento existente
     * Usa um supplier ao invés do valor a ser inserido, permitindo uma avaliação 'lazy' do supplier,
     * reduzindo processamento no caso do mapeamento já existir
     * @param <K>
     * @param <V>
     * @param objetoAInserir
     * @param mapa
     * @param chavesMapa 
     */
    public static <K,V> void adicionaElementoAoNestedMap(Supplier<Object> supplier, Map<K,V> mapa, Object... chavesMapa) {
        if (chavesMapa.length == 1) {
            mapa.putIfAbsent((K) chavesMapa[0], (V) supplier.get());
            return;
        }
        // ex. Map<A, Map<B, Map<C, D>>> possui 2 chaves recursivas e 1 chave final. posicoes 0, 1 e 2. ultimaPosicaoChavesMapaRecursiva = 3 - 2 = B, na posição 1
        // a chave # 3 (C, na posição 2) é a chave final
        int ultimaPosicaoChavesMapaRecursiva = chavesMapa.length - 2;
        adicionaElementoAoMapaRecursivo(supplier, (Map<K,Map>) mapa, 0, ultimaPosicaoChavesMapaRecursiva, chavesMapa);
    }
    private static <K,V extends Map> void adicionaElementoAoMapaRecursivo(Supplier<Object> supplier, Map<K,V> mapa, int posicaoAtualChavesMapa, int ultimaPosicaoChavesMapaRecursiva, Object... chavesMapa) {
        K chaveAtual = (K) chavesMapa[posicaoAtualChavesMapa];
        if (posicaoAtualChavesMapa >= ultimaPosicaoChavesMapaRecursiva) {
            Object ultimaChave = chavesMapa[ultimaPosicaoChavesMapaRecursiva + 1];
            
            if (mapa instanceof ConcurrentHashMap) {
                mapa.computeIfAbsent((K) chaveAtual, x -> (V) new ConcurrentHashMap<>())
                        // computeIfAbsent no lugar de putIfAbsent, pois ao passar como função somente se faz 
                        // a chamada de supplier.get() se realmente precisar
                        .computeIfAbsent(ultimaChave, x -> supplier.get());
            } else {
                mapa.computeIfAbsent((K) chaveAtual, x -> (V) new HashMap<>())
                        // computeIfAbsent no lugar de putIfAbsent, pois ao passar como função somente se faz 
                        // a chamada de supplier.get() se realmente precisar
                        .computeIfAbsent(ultimaChave, x -> supplier.get());
            }
        } else {
            if (mapa instanceof ConcurrentHashMap) {
                adicionaElementoAoMapaRecursivo(
                        supplier,
                        mapa.computeIfAbsent((K) chaveAtual, x -> (V) new ConcurrentHashMap<>()),
                        posicaoAtualChavesMapa + 1,
                        ultimaPosicaoChavesMapaRecursiva,
                        chavesMapa);
            } else {
                adicionaElementoAoMapaRecursivo(
                        supplier,
                        mapa.computeIfAbsent((K) chaveAtual, x -> (V) new HashMap<>()),
                        posicaoAtualChavesMapa + 1,
                        ultimaPosicaoChavesMapaRecursiva,
                        chavesMapa);
            }
        }
    }
    /**
     * Adiciona elemento ao mapa SEM SUBSTITUIR um eventual elemento existente
     * @param <K>
     * @param <V>
     * @param objetoAInserir
     * @param mapa
     * @param chavesMapa 
     */
    public static <K,V> void adicionaElementoAoNestedMap(Object objetoAInserir, Map<K,V> mapa, Object... chavesMapa) {
        if (chavesMapa.length == 1) {
            mapa.putIfAbsent((K) chavesMapa[0], (V) objetoAInserir);
            return;
        }
        // ex. Map<A, Map<B, Map<C, D>>> possui 2 chaves recursivas e 1 chave final. posicoes 0, 1 e 2. ultimaPosicaoChavesMapaRecursiva = 3 - 2 = B, na posição 1
        // a chave # 3 (C, na posição 2) é a chave final
        int ultimaPosicaoChavesMapaRecursiva = chavesMapa.length - 2;
        adicionaElementoAoMapaRecursivo(objetoAInserir, (Map<K,Map>) mapa, 0, ultimaPosicaoChavesMapaRecursiva, (K) chavesMapa[0], chavesMapa);
    }
    private static <K,V extends Map,O> void adicionaElementoAoMapaRecursivo(O objetoAInserir, Map<K,V> mapa, int posicaoAtualChavesMapa, int ultimaPosicaoChavesMapaRecursiva, K chaveAtual, Object... chavesMapa) {
        
        if (posicaoAtualChavesMapa >= ultimaPosicaoChavesMapaRecursiva) {
            Object chaveFinal = chavesMapa[ultimaPosicaoChavesMapaRecursiva + 1];
            
            if (mapa instanceof ConcurrentHashMap) {
                adicionaElementoFinalAoMapaRecursivo(
                        objetoAInserir, 
                        mapa.computeIfAbsent((K) chaveAtual, x -> (V) new ConcurrentHashMap<>()),
                        chaveFinal);
            } else {
                adicionaElementoFinalAoMapaRecursivo(
                        objetoAInserir, 
                        mapa.computeIfAbsent((K) chaveAtual, x -> (V) new HashMap<>()),
                        chaveFinal);
            }
        } else {
            if (mapa instanceof ConcurrentHashMap) {
                adicionaElementoAoMapaRecursivo(
                        objetoAInserir,
                        mapa.computeIfAbsent((K) chaveAtual, x -> (V) new ConcurrentHashMap<>()),
                        posicaoAtualChavesMapa + 1,
                        ultimaPosicaoChavesMapaRecursiva,
                        chavesMapa[posicaoAtualChavesMapa + 1],
                        chavesMapa);
            } else {
                adicionaElementoAoMapaRecursivo(
                        objetoAInserir,
                        mapa.computeIfAbsent((K) chaveAtual, x -> (V) new HashMap<>()),
                        posicaoAtualChavesMapa + 1,
                        ultimaPosicaoChavesMapaRecursiva,
                        chavesMapa[posicaoAtualChavesMapa + 1],
                        chavesMapa);
            }
            
        }
    }
    
    private static <K,V> void adicionaElementoFinalAoMapaRecursivo(V objetoAInserir, Map<K,V> mapa, K chaveFinal) {
            mapa.putIfAbsent(chaveFinal, objetoAInserir);
    }
    
    /**
     * Adiciona elemento ao mapa SUBSTITUINDO um eventual elemento existente
     * @param <K>
     * @param <V>
     * @param objetoAInserir
     * @param mapa
     * @param chavesMapa 
     */
    public static <K,V> void replaceElementoNoNestedMap(Object objetoAInserir, Map<K,V> mapa, Object... chavesMapa) {
        if (chavesMapa.length == 1) {
            mapa.put((K) chavesMapa[0], (V) objetoAInserir);
            return;
        }
        // ex. Map<A, Map<B, Map<C, D>>> possui 2 chaves recursivas e 1 chave final. posicoes 0, 1 e 2. ultimaPosicaoChavesMapaRecursiva = 3 - 2 = B, na posição 1
        // a chave # 3 (C, na posição 2) é a chave final
        int ultimaPosicaoChavesMapaRecursiva = chavesMapa.length - 2;
        replaceElementoNoMapaRecursivo(objetoAInserir, (Map<K,Map>) mapa, 0, ultimaPosicaoChavesMapaRecursiva, (K) chavesMapa[0], chavesMapa);
    }
    private static <K,V extends Map,O> void replaceElementoNoMapaRecursivo(O objetoAInserir, Map<K,V> mapa, int posicaoAtualChavesMapa, int ultimaPosicaoChavesMapaRecursiva, K chaveAtual, Object... chavesMapa) {
        
        if (posicaoAtualChavesMapa >= ultimaPosicaoChavesMapaRecursiva) {
            Object chaveFinal = chavesMapa[ultimaPosicaoChavesMapaRecursiva + 1];
            
            if (mapa instanceof ConcurrentHashMap) {
                replaceElementoFinalNoMapaRecursivo(
                        objetoAInserir, 
                        mapa.computeIfAbsent((K) chaveAtual, x -> (V) new ConcurrentHashMap<>()),
                        chaveFinal);
            } else {
                replaceElementoFinalNoMapaRecursivo(
                        objetoAInserir, 
                        mapa.computeIfAbsent((K) chaveAtual, x -> (V) new HashMap<>()),
                        chaveFinal);
            }
        } else {
            if (mapa instanceof ConcurrentHashMap) {
                replaceElementoNoMapaRecursivo(
                        objetoAInserir,
                        mapa.computeIfAbsent((K) chaveAtual, x -> (V) new ConcurrentHashMap<>()),
                        posicaoAtualChavesMapa + 1,
                        ultimaPosicaoChavesMapaRecursiva,
                        chavesMapa[posicaoAtualChavesMapa + 1],
                        chavesMapa);
            } else {
                replaceElementoNoMapaRecursivo(
                        objetoAInserir,
                        mapa.computeIfAbsent((K) chaveAtual, x -> (V) new HashMap<>()),
                        posicaoAtualChavesMapa + 1,
                        ultimaPosicaoChavesMapaRecursiva,
                        chavesMapa[posicaoAtualChavesMapa + 1],
                        chavesMapa);
            }
            
        }
    }
    private static <K,V> void replaceElementoFinalNoMapaRecursivo(V objetoAInserir, Map<K,V> mapa, K chaveFinal) {
            mapa.put(chaveFinal, objetoAInserir);
    }
    
    public static <K,V> void removeElementoNoNestedMap(Map<K,V> mapa, Object... chavesMapa) {
        if (chavesMapa.length == 1) {
            mapa.remove((K) chavesMapa[0]);
            return;
        }
        // ex. Map<A, Map<B, Map<C, D>>> possui 2 chaves recursivas e 1 chave final. posicoes 0, 1 e 2. ultimaPosicaoChavesMapaRecursiva = 3 - 2 = B, na posição 1
        // a chave # 3 (C, na posição 2) é a chave final
        int ultimaPosicaoChavesMapaRecursiva = chavesMapa.length - 2;
        removeElementoNoMapaRecursivo((Map<K,Map>) mapa, 0, ultimaPosicaoChavesMapaRecursiva, (K) chavesMapa[0], chavesMapa);
    }
    private static <K,V extends Map,O> void removeElementoNoMapaRecursivo(Map<K,V> mapa, int posicaoAtualChavesMapa, int ultimaPosicaoChavesMapaRecursiva, K chaveAtual, Object... chavesMapa) {
        
        // último nível do mapa
        if (posicaoAtualChavesMapa >= ultimaPosicaoChavesMapaRecursiva) {
            Object chaveFinal = chavesMapa[ultimaPosicaoChavesMapaRecursiva + 1];
            
            removeElementoFinalNoMapaRecursivo(
                    mapa.get((K) chaveAtual),
                    chaveFinal);
        // outros níveis do mapa : se encontrar a chave 'filho' , nova recursão
        } else {
            if (mapa.containsKey((K) chaveAtual)) {
                removeElementoNoMapaRecursivo(
                        mapa.get((K) chaveAtual),
                        posicaoAtualChavesMapa + 1,
                        ultimaPosicaoChavesMapaRecursiva,
                        chavesMapa[posicaoAtualChavesMapa + 1],
                        chavesMapa);            
            }
        }

        if (mapa.values().size() > 0) {
            return;
        } else {
            // se com a remoção do filho único não sobraram mais filhos, também remover o pai
            mapa.remove((K) chaveAtual);
        }
            
    }
    private static <K,V> void removeElementoFinalNoMapaRecursivo(Map<K,V> mapa, K chaveFinal) {
        if (mapa == null) return;
        mapa.remove(chaveFinal);
    }
    
    /**
     * Substitui/adiciona elemento ao mapa através de uma função UnaryOperator que faz uso do valor já presente no mapa
     * Se elemento não estiver no mapa, UnaryOperator<T> funcaoAtualizacaoObjeto deverá tratar o caso null
     * @param <K>
     * @param <V>
     * @param funcaoAtualizacaoObjeto
     * @param classeObjeto
     * @param mapa
     * @param chavesMapa 
     */
    public static <K,V,T> void updateElementoNoNestedMap(T valorInicialSeInexistente, UnaryOperator<T> funcaoAtualizacaoObjeto, Class<T> classeObjeto, Map<K,V> mapa, Object... chavesMapa) {
        if (chavesMapa.length == 1) {
            mapa.compute((K) chavesMapa[0], (k,t) -> (V) funcaoAtualizacaoObjeto.apply((t == null) ? valorInicialSeInexistente : (T) t));
            return;
        }
        // ex. Map<A, Map<B, Map<C, D>>> possui 2 chaves recursivas e 1 chave final. posicoes 0, 1 e 2. ultimaPosicaoChavesMapaRecursiva = 3 - 2 = B, na posição 1
        // a chave # 3 (C, na posição 2) é a chave final
        int ultimaPosicaoChavesMapaRecursiva = chavesMapa.length - 2;
        updateElementoNoMapaRecursivo(valorInicialSeInexistente, funcaoAtualizacaoObjeto, classeObjeto, (Map<K,Map>) mapa, 0, ultimaPosicaoChavesMapaRecursiva, (K) chavesMapa[0], chavesMapa);
    }
    private static <K,V extends Map,T> void updateElementoNoMapaRecursivo(T valorInicialSeInexistente, UnaryOperator<T> funcaoAtualizacaoObjeto, Class<T> classeObjeto, Map<K,V> mapa, int posicaoAtualChavesMapa, int ultimaPosicaoChavesMapaRecursiva, K chaveAtual, Object... chavesMapa) {
        
        if (posicaoAtualChavesMapa >= ultimaPosicaoChavesMapaRecursiva) {
            Object chaveFinal = chavesMapa[ultimaPosicaoChavesMapaRecursiva + 1];
            
            if (mapa instanceof ConcurrentHashMap) {
                updateElementoFinalNoMapaRecursivo(
                        valorInicialSeInexistente,
                        funcaoAtualizacaoObjeto, 
                        mapa.computeIfAbsent((K) chaveAtual, x -> (V) new ConcurrentHashMap<>()),
                        chaveFinal);
            } else {
                updateElementoFinalNoMapaRecursivo(
                        valorInicialSeInexistente,
                        funcaoAtualizacaoObjeto, 
                        mapa.computeIfAbsent((K) chaveAtual, x -> (V) new HashMap<>()),
                        chaveFinal);
            }
        } else {
            if (mapa instanceof ConcurrentHashMap) {
                updateElementoNoMapaRecursivo(
                        valorInicialSeInexistente,
                        funcaoAtualizacaoObjeto,
                        classeObjeto,
                        mapa.computeIfAbsent((K) chaveAtual, x -> (V) new ConcurrentHashMap<>()),
                        posicaoAtualChavesMapa + 1,
                        ultimaPosicaoChavesMapaRecursiva,
                        chavesMapa[posicaoAtualChavesMapa + 1],
                        chavesMapa);
            } else {
                updateElementoNoMapaRecursivo(
                        valorInicialSeInexistente,
                        funcaoAtualizacaoObjeto,
                        classeObjeto,
                        mapa.computeIfAbsent((K) chaveAtual, x -> (V) new HashMap<>()),
                        posicaoAtualChavesMapa + 1,
                        ultimaPosicaoChavesMapaRecursiva,
                        chavesMapa[posicaoAtualChavesMapa + 1],
                        chavesMapa);
            }
            
        }
    }
    private static <K,V> void updateElementoFinalNoMapaRecursivo(V valorInicialSeInexistente, UnaryOperator<V> funcaoAtualizacaoObjeto, Map<K,V> mapa, K chaveFinal) {
            mapa.compute(chaveFinal, (k,v) -> (V) funcaoAtualizacaoObjeto.apply((v == null) ? valorInicialSeInexistente : (V) v));
    }

    public static <K,V,T> void updateElementoNoNestedMapComConsumerAtualizacao(T valorInicialSeInexistente, Consumer<T> funcaoAtualizacaoObjeto, Class<T> classeObjeto, Map<K,V> mapa, Object... chavesMapa) {
        if (chavesMapa.length == 1) {
            mapa.compute((K) chavesMapa[0], (k,t) -> {
                if (t == null) {
                    t = (V) valorInicialSeInexistente;
                }
                funcaoAtualizacaoObjeto.accept((T) t);
                return t;
            });
            return;
        }
        // ex. Map<A, Map<B, Map<C, D>>> possui 2 chaves recursivas e 1 chave final. posicoes 0, 1 e 2. ultimaPosicaoChavesMapaRecursiva = 3 - 2 = B, na posição 1
        // a chave # 3 (C, na posição 2) é a chave final
        int ultimaPosicaoChavesMapaRecursiva = chavesMapa.length - 2;
        updateElementoNoMapaRecursivoComConsumerAtualizacao(valorInicialSeInexistente, funcaoAtualizacaoObjeto, classeObjeto, (Map<K,Map>) mapa, 0, ultimaPosicaoChavesMapaRecursiva, (K) chavesMapa[0], chavesMapa);
    }
    private static <K,V extends Map,T> void updateElementoNoMapaRecursivoComConsumerAtualizacao(T valorInicialSeInexistente, Consumer<T> funcaoAtualizacaoObjeto, Class<T> classeObjeto, Map<K,V> mapa, int posicaoAtualChavesMapa, int ultimaPosicaoChavesMapaRecursiva, K chaveAtual, Object... chavesMapa) {

        if (posicaoAtualChavesMapa >= ultimaPosicaoChavesMapaRecursiva) {
            Object chaveFinal = chavesMapa[ultimaPosicaoChavesMapaRecursiva + 1];

            if (mapa instanceof ConcurrentHashMap) {
                updateElementoFinalNoMapaRecursivoComConsumerAtualizacao(
                        valorInicialSeInexistente,
                        funcaoAtualizacaoObjeto,
                        mapa.computeIfAbsent((K) chaveAtual, x -> (V) new ConcurrentHashMap<>()),
                        chaveFinal);
            } else {
                updateElementoFinalNoMapaRecursivoComConsumerAtualizacao(
                        valorInicialSeInexistente,
                        funcaoAtualizacaoObjeto,
                        mapa.computeIfAbsent((K) chaveAtual, x -> (V) new HashMap<>()),
                        chaveFinal);
            }
        } else {
            if (mapa instanceof ConcurrentHashMap) {
                updateElementoNoMapaRecursivoComConsumerAtualizacao(
                        valorInicialSeInexistente,
                        funcaoAtualizacaoObjeto,
                        classeObjeto,
                        mapa.computeIfAbsent((K) chaveAtual, x -> (V) new ConcurrentHashMap<>()),
                        posicaoAtualChavesMapa + 1,
                        ultimaPosicaoChavesMapaRecursiva,
                        chavesMapa[posicaoAtualChavesMapa + 1],
                        chavesMapa);
            } else {
                updateElementoNoMapaRecursivoComConsumerAtualizacao(
                        valorInicialSeInexistente,
                        funcaoAtualizacaoObjeto,
                        classeObjeto,
                        mapa.computeIfAbsent((K) chaveAtual, x -> (V) new HashMap<>()),
                        posicaoAtualChavesMapa + 1,
                        ultimaPosicaoChavesMapaRecursiva,
                        chavesMapa[posicaoAtualChavesMapa + 1],
                        chavesMapa);
            }

        }
    }
    private static <K,V> void updateElementoFinalNoMapaRecursivoComConsumerAtualizacao(V valorInicialSeInexistente, Consumer<V> funcaoAtualizacaoObjeto, Map<K,V> mapa, K chaveFinal) {
            mapa.compute(chaveFinal, (k,v) -> {
                if (v == null) {
                    v = valorInicialSeInexistente;
                }
                funcaoAtualizacaoObjeto.accept(v);
                return v;
            });
    }

    public static <K,V> Optional<Object> getElementoDeNestedMap(Map<K,V> mapa, Object... chavesMapa) {
        if (chavesMapa.length == 1) return Optional.ofNullable(mapa.get((K) chavesMapa[0]));
        // ex. Map<A, Map<B, Map<C, D>>> possui 2 chaves recursivas e 1 chave final. posicoes 0, 1 e 2. ultimaPosicaoChavesMapaRecursiva = 3 - 2 = B, na posição 1
        // a chave # 3 (C, na posição 2) é a chave final
        int ultimaPosicaoChavesMapaRecursiva = chavesMapa.length - 2;
        return getElementoDeMapaRecursivo((Map<K,Map>) mapa, 0, ultimaPosicaoChavesMapaRecursiva, chavesMapa);
    }
    private static <K,V extends Map> Optional<Object> getElementoDeMapaRecursivo(Map<K,V> mapa, int posicaoAtualChavesMapa, int ultimaPosicaoChavesMapaRecursiva, Object... chavesMapa) {
        K chaveAtual = (K) chavesMapa[posicaoAtualChavesMapa];
        if (posicaoAtualChavesMapa >= ultimaPosicaoChavesMapaRecursiva) {
            Object ultimaChave = chavesMapa[ultimaPosicaoChavesMapaRecursiva + 1];
            
            if (mapa instanceof ConcurrentHashMap) {
                return Optional.ofNullable(mapa.computeIfAbsent((K) chaveAtual, x -> (V) new ConcurrentHashMap<>())
                                .get(ultimaChave));
            } else {
                return Optional.ofNullable(mapa.computeIfAbsent((K) chaveAtual, x -> (V) new HashMap<>())
                                .get(ultimaChave));
            }
        } else {
            if (mapa instanceof ConcurrentHashMap) {
                return getElementoDeMapaRecursivo(
                        mapa.computeIfAbsent((K) chaveAtual, x -> (V) new ConcurrentHashMap<>()),
                        posicaoAtualChavesMapa + 1,
                        ultimaPosicaoChavesMapaRecursiva,
                        chavesMapa);
            } else {
                return getElementoDeMapaRecursivo(
                        mapa.computeIfAbsent((K) chaveAtual, x -> (V) new HashMap<>()),
                        posicaoAtualChavesMapa + 1,
                        ultimaPosicaoChavesMapaRecursiva,
                        chavesMapa);
            }
        }
    }

    public static <K,V> OptionalDouble getDoubleDeNestedMap(Map<K,V> mapa, Object... chavesMapa) {
        if (chavesMapa.length == 1) {
            Double resultado = (Double) mapa.get((K) chavesMapa[0]);
            return (resultado == null) ? OptionalDouble.empty() : OptionalDouble.of((Double) mapa.get((K) chavesMapa[0]));
        }
        // ex. Map<A, Map<B, Map<C, D>>> possui 2 chaves recursivas e 1 chave final. posicoes 0, 1 e 2. ultimaPosicaoChavesMapaRecursiva = 3 - 2 = B, na posição 1
        // a chave # 3 (C, na posição 2) é a chave final
        int ultimaPosicaoChavesMapaRecursiva = chavesMapa.length - 2;
        return getDoubleDeMapaRecursivo((Map<K,Map>) mapa, 0, ultimaPosicaoChavesMapaRecursiva, chavesMapa);
    }
    private static <K,V extends Map> OptionalDouble getDoubleDeMapaRecursivo(Map<K,V> mapa, int posicaoAtualChavesMapa, int ultimaPosicaoChavesMapaRecursiva, Object... chavesMapa) {
        K chaveAtual = (K) chavesMapa[posicaoAtualChavesMapa];
        if (posicaoAtualChavesMapa >= ultimaPosicaoChavesMapaRecursiva) {
            Object ultimaChave = chavesMapa[ultimaPosicaoChavesMapaRecursiva + 1];

            if (mapa instanceof ConcurrentHashMap) {
                Double resultado = ((Double) mapa.getOrDefault((K) chaveAtual, (V) new ConcurrentHashMap<>())
                        .get(ultimaChave));
                return (resultado == null) ? OptionalDouble.empty() : OptionalDouble.of(resultado);
            } else {
                Double resultado = ((Double) mapa.getOrDefault((K) chaveAtual, (V) new HashMap<>())
                        .get(ultimaChave));
                return (resultado == null) ? OptionalDouble.empty() : OptionalDouble.of(resultado);
            }
        } else {
            if (mapa instanceof ConcurrentHashMap) {
                return getDoubleDeMapaRecursivo(
                        mapa.getOrDefault((K) chaveAtual, (V) new ConcurrentHashMap<>()),
                        posicaoAtualChavesMapa + 1,
                        ultimaPosicaoChavesMapaRecursiva,
                        chavesMapa);
            } else {
                return getDoubleDeMapaRecursivo(
                        mapa.getOrDefault((K) chaveAtual, (V) new HashMap<>()),
                        posicaoAtualChavesMapa + 1,
                        ultimaPosicaoChavesMapaRecursiva,
                        chavesMapa);
            }
        }
    }

    public static <K,V,T> Optional<T> getElementoDeNestedMap(Map<K,V> mapa, Class<T> classeObjeto, Object... chavesMapa) {
        if (chavesMapa.length == 1) return Optional.ofNullable((T) mapa.get((K) chavesMapa[0]));
        // ex. Map<A, Map<B, Map<C, D>>> possui 2 chaves recursivas e 1 chave final. posicoes 0, 1 e 2. ultimaPosicaoChavesMapaRecursiva = 3 - 2 = B, na posição 1
        // a chave # 3 (C, na posição 2) é a chave final
        int ultimaPosicaoChavesMapaRecursiva = chavesMapa.length - 2;
        return getElementoDeMapaRecursivo((Map<K,Map>) mapa, classeObjeto, 0, ultimaPosicaoChavesMapaRecursiva, chavesMapa);
    }
    private static <K,V extends Map,T> Optional<T> getElementoDeMapaRecursivo(Map<K,V> mapa, Class<T> classeObjeto, int posicaoAtualChavesMapa, int ultimaPosicaoChavesMapaRecursiva, Object... chavesMapa) {
        K chaveAtual = (K) chavesMapa[posicaoAtualChavesMapa];
        if (posicaoAtualChavesMapa >= ultimaPosicaoChavesMapaRecursiva) {
            Object ultimaChave = chavesMapa[ultimaPosicaoChavesMapaRecursiva + 1];
            
            if (mapa instanceof ConcurrentHashMap) {
                return Optional.ofNullable((T) mapa.getOrDefault((K) chaveAtual, (V) new ConcurrentHashMap<>())
                                .get(ultimaChave));
            } else {
                return Optional.ofNullable((T) mapa.getOrDefault((K) chaveAtual, (V) new HashMap<>())
                                .get(ultimaChave));
            }
        } else {
            if (mapa instanceof ConcurrentHashMap) {
                return getElementoDeMapaRecursivo(
                        mapa.getOrDefault((K) chaveAtual, (V) new ConcurrentHashMap<>()),
                        classeObjeto,
                        posicaoAtualChavesMapa + 1,
                        ultimaPosicaoChavesMapaRecursiva,
                        chavesMapa);
            } else {
                return getElementoDeMapaRecursivo(
                        mapa.getOrDefault((K) chaveAtual, (V) new HashMap<>()),
                        classeObjeto,
                        posicaoAtualChavesMapa + 1,
                        ultimaPosicaoChavesMapaRecursiva,
                        chavesMapa);
            }
        }
    }
    
    public static long getNumeroElementosDeNestedMap(Map<?,?> mapa) {
        
        return mapa.values().stream()
                .mapToLong(value -> {
                    if (value instanceof Map) {
                        return getNumeroElementosDeNestedMap((Map) value);
                    } else {
                        return 1;
                    }
                })
                .sum();
        
    }
    
    public static <K,V> Object getOrAddElementoDeNestedMap(Map<K,V> mapa, Supplier supplierCriacaoNovoObjeto, Object... chavesMapa) {
        if (chavesMapa.length == 1) return mapa.computeIfAbsent((K) chavesMapa[0], x -> (V) supplierCriacaoNovoObjeto.get());
        // ex. Map<A, Map<B, Map<C, D>>> possui 2 chaves recursivas e 1 chave final. posicoes 0, 1 e 2. ultimaPosicaoChavesMapaRecursiva = 3 - 2 = B, na posição 1
        // a chave # 3 (C, na posição 2) é a chave final
        int ultimaPosicaoChavesMapaRecursiva = chavesMapa.length - 2;
        return getOrAddElementoDeMapaRecursivo((Map<K,Map>) mapa, 0, ultimaPosicaoChavesMapaRecursiva, supplierCriacaoNovoObjeto, chavesMapa);
    }
    private static <K,V extends Map> Object getOrAddElementoDeMapaRecursivo(Map<K,V> mapa, int posicaoAtualChavesMapa, int ultimaPosicaoChavesMapaRecursiva, Supplier supplierCriacaoNovoObjeto, Object... chavesMapa) {
        K chaveAtual = (K) chavesMapa[posicaoAtualChavesMapa];
        if (posicaoAtualChavesMapa >= ultimaPosicaoChavesMapaRecursiva) {
            Object ultimaChave = chavesMapa[ultimaPosicaoChavesMapaRecursiva + 1];
            
            if (mapa instanceof ConcurrentHashMap) {
                return mapa.computeIfAbsent((K) chaveAtual, x -> (V) new ConcurrentHashMap<>())
                                .computeIfAbsent(ultimaChave, x -> supplierCriacaoNovoObjeto.get());
            } else {
                return mapa.computeIfAbsent((K) chaveAtual, x -> (V) new HashMap<>())
                                .computeIfAbsent(ultimaChave, x -> supplierCriacaoNovoObjeto.get());
            }
        } else {
            if (mapa instanceof ConcurrentHashMap) {
                return getOrAddElementoDeMapaRecursivo(
                        mapa.computeIfAbsent((K) chaveAtual, x -> (V) new ConcurrentHashMap<>()),
                        posicaoAtualChavesMapa + 1,
                        ultimaPosicaoChavesMapaRecursiva,
                        supplierCriacaoNovoObjeto,
                        chavesMapa);
            } else {
                return getOrAddElementoDeMapaRecursivo(
                        mapa.computeIfAbsent((K) chaveAtual, x -> (V) new HashMap<>()),
                        posicaoAtualChavesMapa + 1,
                        ultimaPosicaoChavesMapaRecursiva,
                        supplierCriacaoNovoObjeto,
                        chavesMapa);
            }
            
        }
    }
    
    public static <K,V,T> T getOrAddElementoDeNestedMap(Map<K,V> mapa, Class<T> classeObjeto, Supplier supplierCriacaoNovoObjeto, Object... chavesMapa) {
        if (chavesMapa.length == 1) return (T) mapa.computeIfAbsent((K) chavesMapa[0], x -> (V) supplierCriacaoNovoObjeto.get());
        // ex. Map<A, Map<B, Map<C, D>>> possui 2 chaves recursivas e 1 chave final. posicoes 0, 1 e 2. ultimaPosicaoChavesMapaRecursiva = 3 - 2 = B, na posição 1
        // a chave # 3 (C, na posição 2) é a chave final
        int ultimaPosicaoChavesMapaRecursiva = chavesMapa.length - 2;
        return getOrAddElementoDeMapaRecursivo((Map<K,Map>) mapa, classeObjeto, 0, ultimaPosicaoChavesMapaRecursiva, supplierCriacaoNovoObjeto, chavesMapa);
    }
    private static <K,V extends Map,T> T getOrAddElementoDeMapaRecursivo(Map<K,V> mapa, Class<T> classeObjeto, int posicaoAtualChavesMapa, int ultimaPosicaoChavesMapaRecursiva, Supplier supplierCriacaoNovoObjeto, Object... chavesMapa) {
        K chaveAtual = (K) chavesMapa[posicaoAtualChavesMapa];
        if (posicaoAtualChavesMapa >= ultimaPosicaoChavesMapaRecursiva) {
            Object ultimaChave = chavesMapa[ultimaPosicaoChavesMapaRecursiva + 1];
            
            if (mapa instanceof ConcurrentHashMap) {
                return (T) mapa.computeIfAbsent((K) chaveAtual, x -> (V) new ConcurrentHashMap<>())
                                .computeIfAbsent(ultimaChave, x -> supplierCriacaoNovoObjeto.get());
            } else {
                return (T) mapa.computeIfAbsent((K) chaveAtual, x -> (V) new HashMap<>())
                                .computeIfAbsent(ultimaChave, x -> supplierCriacaoNovoObjeto.get());
            }
        } else {
            if (mapa instanceof ConcurrentHashMap) {
                return (T) getOrAddElementoDeMapaRecursivo(
                        mapa.computeIfAbsent((K) chaveAtual, x -> (V) new ConcurrentHashMap<>()),
                        classeObjeto,
                        posicaoAtualChavesMapa + 1,
                        ultimaPosicaoChavesMapaRecursiva,
                        supplierCriacaoNovoObjeto,
                        chavesMapa);
            } else {
                return (T) getOrAddElementoDeMapaRecursivo(
                        mapa.computeIfAbsent((K) chaveAtual, x -> (V) new HashMap<>()),
                        classeObjeto,
                        posicaoAtualChavesMapa + 1,
                        ultimaPosicaoChavesMapaRecursiva,
                        supplierCriacaoNovoObjeto,
                        chavesMapa);
            }
        }
    }    
    
    /**
     * Retorna nested map a partir de coleção de objetos e respectivos campos
     * Gera erro se houverem 2 objetos finais idênticos para o mesmo agrupamento
     * @param <T>
     * @param collection
     * @param funcoesExtracaoCamposMapa exemplo : produto -> produto.getCategoria(), produto -> produto.getSubcategoria()
     * @return nested map, por exemplo: Map<Categoria,Map<Subcategoria,Produto>>
     */
    public static <T> Map<?,?> getNestedMapDeCollectionSemGrouping(Collection<T> collection, Function<T,?>... funcoesExtracaoCamposMapa) {
            return getNestedMapDeCollection(collection, false, funcoesExtracaoCamposMapa);
    }
    /**
     * Retorna nested map a partir de coleção de objetos e respectivos campos
     * Duplicatas são agrupadas em Set no último nível do mapa
     * @param <T>
     * @param collection
     * @param funcoesExtracaoCamposMapa exemplo : produto -> produto.getCategoria(), produto -> produto.getSubcategoria()
     * @return nested map, por exemplo: Map<Categoria,Map<Subcategoria,Set<Produto>>>
     */
    public static <T> Map<?,?> getNestedMapDeCollectionComGrouping(Collection<T> collection, Function<T,?>... funcoesExtracaoCamposMapa) {

        validaFuncoesExtracaoCamposMapa(funcoesExtracaoCamposMapa);
        return getNestedMapDeCollection(collection, true, funcoesExtracaoCamposMapa);

    }
    private static <T> Map<?,?> getNestedMapDeCollection(Collection<T> collection, boolean permiteAgrupamento, Function<T,?>... funcoesExtracaoCamposMapa) {
            return (Map<?,?>) collection.stream().collect(getNestedMapDeCollectionRecursivo(permiteAgrupamento, 0, funcoesExtracaoCamposMapa));
    }
    private static <T> Collector<T,?,?> getNestedMapDeCollectionRecursivo(
            boolean permiteAgrupamento,
            int posicaoAtualFuncoesExtracaoCampoMapa, Function<T,?>... funcoesExtracaoCamposMapa) {
        Function<T,?> funcaoExtracaoAtual = funcoesExtracaoCamposMapa[posicaoAtualFuncoesExtracaoCampoMapa];
        
        if (posicaoAtualFuncoesExtracaoCampoMapa < funcoesExtracaoCamposMapa.length - 1) {
            
            return Collectors.groupingBy(
                    funcaoExtracaoAtual,
                    getNestedMapDeCollectionRecursivo(
                            permiteAgrupamento,
                            posicaoAtualFuncoesExtracaoCampoMapa + 1,
                            funcoesExtracaoCamposMapa));
            
        } else if (posicaoAtualFuncoesExtracaoCampoMapa == funcoesExtracaoCamposMapa.length - 1) {
            
            if (permiteAgrupamento) {
                return Collectors.groupingBy(funcaoExtracaoAtual, Collectors.toSet());
            } else {
                return Collectors.toMap(funcaoExtracaoAtual, x -> x);
            }
            
        } else {
            throw getEstadoInconsistenteDeRecursaoNestedMapException(
                    posicaoAtualFuncoesExtracaoCampoMapa,
                    funcoesExtracaoCamposMapa.length);
        }
        
    }
    
    /**
     * 
     * @param <T>
     * @param collection
     * @param reduceBinaryOperator exemplo (produtoA,produtoB) -> produtoA.toString() + produtoB.toString()
     * @param funcoesExtracaoCamposMapa exemplo : produto -> produto.getCategoria(), produto -> produto.getSubcategoria()
     * @return Map<Categoria,Map<Subcategoria,String>>, onde a string é a concatenação dos nomes dos produtos
     */
    public static <T> Map<?,?> getNestedMapDeCollectionComReduce(
            Collection<T> collection, 
            BinaryOperator<T> reduceBinaryOperator,
            Function<T,?>... funcoesExtracaoCamposMapa) {
        
        validaFuncoesExtracaoCamposMapa(funcoesExtracaoCamposMapa);
        return (Map<?,?>) collection.stream().collect(getNestedMapDeCollectionRecursivoComReduce(reduceBinaryOperator, 0, funcoesExtracaoCamposMapa));
            
    }
    private static <T> Collector<T,?,?> getNestedMapDeCollectionRecursivoComReduce(
            BinaryOperator<T> reduceBinaryOperator,
            int posicaoAtualFuncoesExtracaoCampoMapa, Function<T,?>... funcoesExtracaoCamposMapa) {
        Function<T,?> funcaoExtracaoAtual = funcoesExtracaoCamposMapa[posicaoAtualFuncoesExtracaoCampoMapa];
        
        if (posicaoAtualFuncoesExtracaoCampoMapa < funcoesExtracaoCamposMapa.length - 1) {
            
            return Collectors.groupingBy(
                    funcaoExtracaoAtual,
                    getNestedMapDeCollectionRecursivoComReduce(
                            reduceBinaryOperator,
                            posicaoAtualFuncoesExtracaoCampoMapa + 1,
                            funcoesExtracaoCamposMapa));
            
        } else if (posicaoAtualFuncoesExtracaoCampoMapa == funcoesExtracaoCamposMapa.length - 1) {
            
            return Collectors.toMap(funcaoExtracaoAtual, x -> x, reduceBinaryOperator);
            
        } else {
            throw getEstadoInconsistenteDeRecursaoNestedMapException(
                    posicaoAtualFuncoesExtracaoCampoMapa,
                    funcoesExtracaoCamposMapa.length);
        }
        
    }
    
    public static <T> Map<?,?> getNestedMapDeCollectionComTotalizadorDouble(
            Collection<T> collection, 
            ToDoubleFunction<T> toDoubleFunction,
            Function<T,?>... funcoesExtracaoCamposMapa) {
        
        validaFuncoesExtracaoCamposMapa(funcoesExtracaoCamposMapa);
        return (Map<?,?>) collection.stream().collect(getNestedMapDeCollectionRecursivoComTotalizadorDouble(toDoubleFunction, 0, funcoesExtracaoCamposMapa));
            
    }
    private static <T> Collector<T,?,?> getNestedMapDeCollectionRecursivoComTotalizadorDouble(
            ToDoubleFunction<T> toDoubleFunction,
            int posicaoAtualFuncoesExtracaoCampoMapa, Function<T,?>... funcoesExtracaoCamposMapa) {
        Function<T,?> funcaoExtracaoAtual = funcoesExtracaoCamposMapa[posicaoAtualFuncoesExtracaoCampoMapa];
        
        if (posicaoAtualFuncoesExtracaoCampoMapa < funcoesExtracaoCamposMapa.length - 1) {
            
            return Collectors.groupingBy(
                    funcaoExtracaoAtual,
                    getNestedMapDeCollectionRecursivoComTotalizadorDouble(
                            toDoubleFunction,
                            posicaoAtualFuncoesExtracaoCampoMapa + 1,
                            funcoesExtracaoCamposMapa));
            
        } else if (posicaoAtualFuncoesExtracaoCampoMapa == funcoesExtracaoCamposMapa.length - 1) {
            
            return Collectors.groupingBy(funcaoExtracaoAtual, 
                    Collectors.summingDouble(toDoubleFunction));
            
        } else {
            throw getEstadoInconsistenteDeRecursaoNestedMapException(
                    posicaoAtualFuncoesExtracaoCampoMapa,
                    funcoesExtracaoCamposMapa.length);
        }
        
    }
    
    /**
     * Se key = nulo retorna nulo
     * Se valor não for encontrado no mapa, lança notFoundException
     * Caso valor seja encontrado, retorna o valor
     */
    public static <K,V,E extends RuntimeException> V getFromMapOrThrowExceptionIfNotFound(Map<K,V> map, K key, boolean throwExceptionIfKeyIsNull, E notFoundException) {
        if (key == null && !throwExceptionIfKeyIsNull) return null;
        if (key == null && throwExceptionIfKeyIsNull) throw new IllegalArgumentException("Key must not be null when throwExceptionIfKeyIsNull = true");
        V value = map.getOrDefault(key, null);
        if (value == null) throw notFoundException;
        return value;
    }

    /**
     * Garante que os builders de nested map tenham ao menos uma dimensao de
     * indexacao. Sem essa validacao a primeira chamada recursiva falharia com
     * erro de array, escondendo que o problema foi um argumento invalido do
     * consumidor.
     */
    private static void validaFuncoesExtracaoCamposMapa(Function<?,?>[] funcoesExtracaoCamposMapa) {

        if (funcoesExtracaoCamposMapa == null || funcoesExtracaoCamposMapa.length == 0) {
            throw new IllegalArgumentException("At least one nested map extraction function must be provided");
        }

    }

    /**
     * Falha defensiva para estados que nao devem ser alcancados depois da
     * validacao de entrada. Se acontecer, ha erro no proprio algoritmo
     * recursivo, nao uma capability ausente da edicao Community.
     */
    private static IllegalStateException getEstadoInconsistenteDeRecursaoNestedMapException(
            int posicaoAtualFuncoesExtracaoCampoMapa,
            int quantidadeFuncoesExtracaoCamposMapa) {

        return new IllegalStateException(
                "Nested map recursion index " + posicaoAtualFuncoesExtracaoCampoMapa
                        + " exceeded extraction function count " + quantidadeFuncoesExtracaoCamposMapa);

    }
        
}
