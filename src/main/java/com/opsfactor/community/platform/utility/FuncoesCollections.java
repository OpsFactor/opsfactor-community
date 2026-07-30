package com.opsfactor.community.platform.utility;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiFunction;

/**
 * Utilitarios de colecoes compartilhados por projections e rotinas Community.
 *
 * <p>Esta classe permanece sem estado para que possa ser usada em calculos de
 * lote sem criar dependencias Spring nem esconder mutacoes em singletons.</p>
 */
public abstract class FuncoesCollections {
    
    public static <V> Stack<V> criaStack(V... objetos) {
        
        Stack stack = new Stack();
        for (V v : objetos) {
            stack.push(v);
        }
        return stack;
        
    }
    
    public static <V> Map<V,Integer> getMapaComPosicaoNaLista(List<V> lista) {
        
        Map<V,Integer> mapaPosicaoPorElemento = new HashMap<>();
        
        for (int i=0; i<lista.size(); i++) {
            
            mapaPosicaoPorElemento.put(lista.get(i), i);
            
        }
        
        return mapaPosicaoPorElemento;
        
    }
    
    public static <V, C> Comparator<V> criaMultiLevelComparator(
            List<C> caracteristicasOrdenadasPorPrioridade,
            BiFunction<V, C, String> funcaoExtratoraDeValorPorCaracteristica) {
        
        Comparator<V> comparator = new Comparator<V>() {
            
            @Override
            public int compare(V v1, V v2) {
                
                for (C c : caracteristicasOrdenadasPorPrioridade) {
                    
                    int comparacaoAtual = funcaoExtratoraDeValorPorCaracteristica.apply(v1, c)
                            .compareTo(funcaoExtratoraDeValorPorCaracteristica.apply(v2, c));
                    
                    if (comparacaoAtual != 0) return comparacaoAtual;
                    
                }
                
                return 0;
                
            }
            
        };
                
        return comparator;
                
    }
    
    /**
     * Converte recursivamente para UnmodifiableList
     * @param list
     * @return 
     */
    public static List convertToNestedUnmodifiableList(List list) {
        list = Collections.unmodifiableList(list);
        for (Object value : list) {
            if (value instanceof Map) {
                FuncoesMap.convertToNestedUnmodifiableMap((Map) value);
            } else if (value instanceof Set) {
                convertToNestedUnmodifiableSet((Set) value);
            } else if (value instanceof List) {
                convertToNestedUnmodifiableList((List) value);
            } else if (value instanceof Collection) {
                convertToNestedUnmodifiableCollection((Collection) value);
            }
        }     
        return list;
    }
    
    /**
     * Converte recursivamente para UnmodifiableSet
     * @param set
     * @return 
     */
    public static Set convertToNestedUnmodifiableSet(Set set) {
        set = Collections.unmodifiableSet(set);
        for (Object value : set) {
            if (value instanceof Map) {
                FuncoesMap.convertToNestedUnmodifiableMap((Map) value);
            } else if (value instanceof Set) {
                convertToNestedUnmodifiableSet((Set) value);
            } else if (value instanceof List) {
                convertToNestedUnmodifiableList((List) value);
            } else if (value instanceof Collection) {
                convertToNestedUnmodifiableCollection((Collection) value);
            }
        }        
        return set;
    }
    
    /**
     * Converte recursivamente para UnmodifiableCollection
     * @param collection
     * @return 
     */
    public static Collection convertToNestedUnmodifiableCollection(Collection collection) {
        collection = Collections.unmodifiableCollection(collection);
        for (Object value : collection) {
            if (value instanceof Map) {
                FuncoesMap.convertToNestedUnmodifiableMap((Map) value);
            } else if (value instanceof Set) {
                convertToNestedUnmodifiableSet((Set) value);
            } else if (value instanceof List) {
                convertToNestedUnmodifiableList((List) value);
            } else if (value instanceof Collection) {
                convertToNestedUnmodifiableCollection((Collection) value);
            }
        }
        return collection;
    }
    
}
