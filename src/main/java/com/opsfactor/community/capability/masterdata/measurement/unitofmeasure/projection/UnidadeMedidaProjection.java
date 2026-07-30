package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.platform.exception.UnitOfMeasureConversionException;
import lombok.Getter;
import org.javatuples.Pair;

import jakarta.persistence.NoResultException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/**
 * Projection em memoria de unidades de medida e conversoes.
 *
 * <p>Ela carrega conversoes globais e especificas por material uma unica vez
 * para que rotinas de Demand/Supply Planning nao disparem consultas JPA durante
 * calculos em lote. Conversoes indiretas calculadas em runtime sao cacheadas no
 * proprio mapa da projection.</p>
 */
public class UnidadeMedidaProjection {
    
    /**
     * Parametros globais usados para resolver unidades padrao quando entidades
     * fisicas nao carregam unidade explicita.
     */
    @Getter
    protected ParametrosGlobais parametrosGlobais;

    /**
     * Catalogo de unidades conhecidas pelo projection.
     */
    protected Set<UnidadeMedida> unidadeMedidaSet = new HashSet<>();

    /**
     * Conversoes globais por origem/destino. A factory popula ambos os sentidos
     * para cada conversao cadastrada.
     */
    protected Map<UnidadeMedida,Map<UnidadeMedida,Double>> mapaConversoesPadrao = new ConcurrentHashMap<>();

    /**
     * Conversoes especificas por material. Conversoes indiretas encontradas por
     * recursao tambem sao armazenadas aqui como cache.
     */
    protected Map<Produto,Map<UnidadeMedida,Map<UnidadeMedida,Double>>> mapaConversoesPorProduto = new ConcurrentHashMap<>();

    /**
     * Valida material usado como chave de conversao especifica.
     *
     * <p>A factory valida conversoes carregadas do banco, mas os metodos
     * publicos desta projection tambem sao chamados diretamente por rotinas de
     * Demand/Supply e overlays Enterprise. Material sem id nao pode ser usado
     * como chave confiavel de cache/conversao.</p>
     */
    /**
     * Valida unidade de medida recebida por API publica ou calculo recursivo.
     */
    /**
     * Valida fator de conversao antes de retornar ou cachear o resultado.
     */
    private double getConversaoFinitaEPositiva(
            Double conversao,
            UnidadeMedida unidadeMedidaOrigem,
            UnidadeMedida unidadeMedidaTarget) {

        if (conversao == null
                || !Double.isFinite(conversao)
                || conversao <= 0) {
            throw new UnitOfMeasureConversionException(
                    "Invalid UOM conversion from "
                            + unidadeMedidaOrigem.getId()
                            + " to "
                            + unidadeMedidaTarget.getId());
        }

        return conversao;

    }
    
    private OptionalDouble getConversaoUnidadeMedidaRecursivo(
            UnidadeMedida unidadeMedidaOrigem,
            UnidadeMedida unidadeMedidaTarget, 
            Map<UnidadeMedida,Map<UnidadeMedida,Double>> mapaConversoes) {
        
        if (unidadeMedidaOrigem.equals(unidadeMedidaTarget)) return OptionalDouble.of(1.0);
        
        Double conversaoDireta = mapaConversoes
                .getOrDefault(unidadeMedidaOrigem, new HashMap<>())
                .getOrDefault(unidadeMedidaTarget, null);
        if (conversaoDireta != null) {
            return OptionalDouble.of(getConversaoFinitaEPositiva(
                    conversaoDireta,
                    unidadeMedidaOrigem,
                    unidadeMedidaTarget));
        }
        
        else if (mapaConversoes.containsKey(unidadeMedidaOrigem)) {
            Map<UnidadeMedida,Map<UnidadeMedida,Double>> copiaMapa = new HashMap(mapaConversoes);
            copiaMapa.remove(unidadeMedidaOrigem); // remove a unidade origem para evitar recursão infinita
            for (UnidadeMedida unidadeMedidaTargetIntermediaria : mapaConversoes.get(unidadeMedidaOrigem).keySet()) {
                OptionalDouble conversaoSecundaria = getConversaoUnidadeMedidaRecursivo(unidadeMedidaTargetIntermediaria, unidadeMedidaTarget, copiaMapa);
                if (conversaoSecundaria.isPresent()) {
                    double conversaoOrigemParaIntermediaria = getConversaoFinitaEPositiva(
                            mapaConversoes.get(unidadeMedidaOrigem).get(unidadeMedidaTargetIntermediaria),
                            unidadeMedidaOrigem,
                            unidadeMedidaTargetIntermediaria);
                    return OptionalDouble.of(
                            conversaoSecundaria.getAsDouble() *
                            conversaoOrigemParaIntermediaria);
                }
            }
        }
        return OptionalDouble.empty();
    }
    
    /**
     * Gera passo a passo das conversões para relatório de detalhamento do racional de cálculo
     * @param unidadeMedidaOrigem
     * @param unidadeMedidaTarget
     * @param mapaConversoes
     * @param passoAPassoAcumulado
     * @return 
     */
    private Pair<Double,String> getPassoAPassoConversaoUnidadeMedidaRecursivo(
            UnidadeMedida unidadeMedidaOrigem,
            UnidadeMedida unidadeMedidaTarget, 
            Map<UnidadeMedida,Map<UnidadeMedida,Double>> mapaConversoes,
            String passoAPassoAcumulado) {
        
        List<String> etapasConversao = new ArrayList<>();
        
        if (unidadeMedidaOrigem.equals(unidadeMedidaTarget)) {
            return Pair.with(
                    1.0,
                    unidadeMedidaOrigem.getId() + " * 1.0 -> " + unidadeMedidaTarget.getId());
        }

        Double conversaoDireta = mapaConversoes
                .getOrDefault(unidadeMedidaOrigem, new HashMap<>())
                .getOrDefault(unidadeMedidaTarget, null);
        if (conversaoDireta != null) {
            double conversaoDiretaValidada = getConversaoFinitaEPositiva(
                    conversaoDireta,
                    unidadeMedidaOrigem,
                    unidadeMedidaTarget);
            return Pair.with(
                    conversaoDiretaValidada,
                    unidadeMedidaOrigem.getId() + " * " + conversaoDiretaValidada + " -> " + unidadeMedidaTarget.getId());
        }
        
        else if (mapaConversoes.containsKey(unidadeMedidaOrigem)) {
            Map<UnidadeMedida,Map<UnidadeMedida,Double>> copiaMapa = new HashMap(mapaConversoes);
            copiaMapa.remove(unidadeMedidaOrigem); // remove a unidade origem para evitar recursão infinita
            
            String copiaPassoAPassoAcumulado = new String(passoAPassoAcumulado);
            
            for (UnidadeMedida unidadeMedidaTargetIntermediaria : mapaConversoes.get(unidadeMedidaOrigem).keySet()) {
                Pair<Double,String> conversaoSecundaria = getPassoAPassoConversaoUnidadeMedidaRecursivo(unidadeMedidaTargetIntermediaria, unidadeMedidaTarget, copiaMapa, copiaPassoAPassoAcumulado);
                if (conversaoSecundaria != null) {     
                    
                    double conversaoValorOrigemParaValorIntermediario = getConversaoFinitaEPositiva(
                            mapaConversoes.get(unidadeMedidaOrigem).get(unidadeMedidaTargetIntermediaria),
                            unidadeMedidaOrigem,
                            unidadeMedidaTargetIntermediaria);
                    
                    copiaPassoAPassoAcumulado = unidadeMedidaOrigem.getId() + " * " + conversaoValorOrigemParaValorIntermediario + " -> " + conversaoSecundaria.getValue1();
                    
                    return Pair.with(conversaoSecundaria.getValue0() * conversaoValorOrigemParaValorIntermediario, copiaPassoAPassoAcumulado);
                }
            }
        }
        return null;
    }
    
    /**
     * Retorna valor para se converter da unidade origem para a unidade destino
     * Qtd na unidade destino = valor * qtd na unidade origem
     * @param material
     * @param unidadeMedidaOrigem
     * @param unidadeMedidaTarget
     * @return 
     */
    public OptionalDouble getOptionalConversaoParaUnidadeDestino(Produto material, UnidadeMedida unidadeMedidaOrigem,
            UnidadeMedida unidadeMedidaTarget) {
        
        String idUnidadeMedidaOrigem = unidadeMedidaOrigem.getId();
        String idUnidadeMedidaTarget = unidadeMedidaTarget.getId();
        
        if (idUnidadeMedidaOrigem.equals(idUnidadeMedidaTarget)) return OptionalDouble.of(1.0);
        
        // tenta buscar conversão direta (definição por material ou conversão padrão)
        OptionalDouble conversaoDireta = getConversaoDiretaParaUnidadeDestino(material, unidadeMedidaOrigem, unidadeMedidaTarget);
        if (conversaoDireta.isPresent()) {
            return conversaoDireta;
        }

        // caso não encontre:
        // 1) busca recursivamente uma conversão indireta
        // 2) armazena nova conversão no nível material (cálculo pesado, então é mantido em memória)
        
        // traz referência ao mapa, que será atualizado com conversões padrão
        Map<UnidadeMedida,Map<UnidadeMedida,Double>> mapaConversoesMaterial = new HashMap(mapaConversoesPorProduto.getOrDefault(material, new HashMap<>()));
        
        // adiciona conversões padrão à lista, caso já não tenham sido especificadas no nível material
        for (UnidadeMedida unidadeOrigemConversaoPadrao : mapaConversoesPadrao.keySet()) {
            for (UnidadeMedida unidadeTargetConversaoPadrao : mapaConversoesPadrao.get(unidadeOrigemConversaoPadrao).keySet()) {
                if (!mapaConversoesMaterial.containsKey(unidadeOrigemConversaoPadrao)) {
                    mapaConversoesMaterial.put(unidadeOrigemConversaoPadrao, new HashMap());
                }
                if (!mapaConversoesMaterial.get(unidadeOrigemConversaoPadrao).containsKey(unidadeTargetConversaoPadrao)) {
                    mapaConversoesMaterial.get(unidadeOrigemConversaoPadrao).put(unidadeTargetConversaoPadrao, mapaConversoesPadrao.get(unidadeOrigemConversaoPadrao).get(unidadeTargetConversaoPadrao));
                }
            }
        }
        
        OptionalDouble conversaoIndireta = getConversaoUnidadeMedidaRecursivo(unidadeMedidaOrigem, unidadeMedidaTarget, mapaConversoesMaterial);
        
        // se conversão indireta encontrada, a adiciona ao mapa para evitar futuro recálculo
        // na próxima execução entra como conversão direta
        if (conversaoIndireta.isPresent()) {
            mapaConversoesPorProduto
                    .computeIfAbsent(material, x -> new ConcurrentHashMap<>())
                    .computeIfAbsent(unidadeMedidaOrigem, x -> new ConcurrentHashMap<>())
                    .computeIfAbsent(unidadeMedidaTarget, x -> conversaoIndireta.getAsDouble());
        }

        // optional : empty ou calculado
        return conversaoIndireta;
        
    }
    
    /**
     * Gera passo a passo das conversões para relatório de detalhamento do racional de cálculo
     * @param material
     * @param unidadeMedidaOrigem
     * @param unidadeMedidaTarget
     * @return 
     */
    public Pair<Double,String> getPassoAPassoConversaoParaUnidadeDestino(Produto material, UnidadeMedida unidadeMedidaOrigem,
            UnidadeMedida unidadeMedidaTarget) {
        
        
        if (unidadeMedidaOrigem.equals(unidadeMedidaTarget)) return Pair.with(1.0, unidadeMedidaOrigem.getId() + " * 1.0 -> " + unidadeMedidaTarget.getId() + ". Total Conversion = 1.0");
        
        // tenta buscar conversão direta (definição por material ou conversão padrão)
        OptionalDouble conversaoDireta = getConversaoDiretaParaUnidadeDestino(material, unidadeMedidaOrigem, unidadeMedidaTarget);
        if (conversaoDireta.isPresent())  {
            return Pair.with(conversaoDireta.getAsDouble(), unidadeMedidaOrigem.getId() + " * " + conversaoDireta.getAsDouble() + " -> " + unidadeMedidaTarget.getId() + ". Total Conversion = " + conversaoDireta.getAsDouble());
            
        }
        
        // caso não encontre:
        // 1) busca recursivamente uma conversão indireta
        // 2) armazena nova conversão no nível material (cálculo pesado, então é mantido em memória)
        
        // traz referência ao mapa, que será atualizado com conversões padrão
        Map<UnidadeMedida,Map<UnidadeMedida,Double>> mapaConversoesMaterial = new ConcurrentHashMap(mapaConversoesPorProduto.getOrDefault(material, new HashMap<>()));
        
        // adiciona conversões padrão à lista, caso já não tenham sido especificadas no nível material
        for (UnidadeMedida unidadeOrigemConversaoPadrao : mapaConversoesPadrao.keySet()) {
            for (UnidadeMedida unidadeTargetConversaoPadrao : mapaConversoesPadrao.get(unidadeOrigemConversaoPadrao).keySet()) {
                if (!mapaConversoesMaterial.containsKey(unidadeOrigemConversaoPadrao)) {
                    mapaConversoesMaterial.put(unidadeOrigemConversaoPadrao, new ConcurrentHashMap());
                }
                if (!mapaConversoesMaterial.get(unidadeOrigemConversaoPadrao).containsKey(unidadeTargetConversaoPadrao)) {
                    mapaConversoesMaterial.get(unidadeOrigemConversaoPadrao).put(unidadeTargetConversaoPadrao, mapaConversoesPadrao.get(unidadeOrigemConversaoPadrao).get(unidadeTargetConversaoPadrao));
                }
            }
        }
        
        Pair<Double,String> conversaoIndireta = getPassoAPassoConversaoUnidadeMedidaRecursivo(unidadeMedidaOrigem, unidadeMedidaTarget, mapaConversoesMaterial, "");
        
        // se conversão indireta encontrada, a adiciona ao mapa para evitar futuro recálculo
        // na próxima execução entra como conversão direta
        if (conversaoIndireta != null) {
            mapaConversoesPorProduto
                    .computeIfAbsent(material, x -> new ConcurrentHashMap<>())
                    .computeIfAbsent(unidadeMedidaOrigem, x -> new ConcurrentHashMap<>())
                    .computeIfAbsent(unidadeMedidaTarget, x -> conversaoIndireta.getValue0());
        }
        // optional : empty ou calculado
        return conversaoIndireta;
        
    }

    
    /**
     * Retorna valor para se converter da unidade origem para a unidade destino
     * Qtd na unidade destino = valor * qtd na unidade origem
     * @return
     */
    public double getConversaoParaUnidadeDestino(Produto material, UnidadeMedida unidadeMedidaOrigem,
            UnidadeMedida unidadeMedidaTarget) {
        
        OptionalDouble conversao = getOptionalConversaoParaUnidadeDestino(material, unidadeMedidaOrigem, unidadeMedidaTarget);
        if (!conversao.isPresent()) throw new UnitOfMeasureConversionException("No conversion available from " + unidadeMedidaOrigem.getId() + " to " + unidadeMedidaTarget.getId() + " for material " + material.getId());

        return conversao.getAsDouble();
        
    }
        
    private OptionalDouble getConversaoDiretaParaUnidadeDestino(
            Produto material, 
            UnidadeMedida unidadeMedidaOrigem,UnidadeMedida unidadeMedidaTarget) {
        
        // primeiro tenta buscar conversão específica para material
        Double conversaoDireta = mapaConversoesPorProduto
                .getOrDefault(material, new HashMap<>())
                .getOrDefault(unidadeMedidaOrigem, new HashMap<>())
                .getOrDefault(unidadeMedidaTarget, null);
        
        if (conversaoDireta != null) {
            return OptionalDouble.of(getConversaoFinitaEPositiva(
                    conversaoDireta,
                    unidadeMedidaOrigem,
                    unidadeMedidaTarget));
        }
        
        return getConversaoPadraoDiretaParaUnidadeDestino(unidadeMedidaOrigem, unidadeMedidaTarget);
        
    }

    /**
     * Mesmo que getConversaoDiretaParaUnidadeDestino mas sem considerar o material, usando apenas conversões padrão
     * @param unidadeMedidaOrigem
     * @param unidadeMedidaTarget
     * @return
     */
    private OptionalDouble getConversaoPadraoDiretaParaUnidadeDestino(
            UnidadeMedida unidadeMedidaOrigem,UnidadeMedida unidadeMedidaTarget) {

        // caso não encontre, tenta buscar conversão padrão
        Double conversaoDireta = mapaConversoesPadrao
                .getOrDefault(unidadeMedidaOrigem, new HashMap<>())
                .getOrDefault(unidadeMedidaTarget, null);

        return (conversaoDireta == null) ?
                OptionalDouble.empty()
                : OptionalDouble.of(getConversaoFinitaEPositiva(
                        conversaoDireta,
                        unidadeMedidaOrigem,
                        unidadeMedidaTarget));

    }

    public boolean contemConversaoParaUnidadeDestino(
            Produto material, 
            UnidadeMedida unidadeMedidaOrigem,
            UnidadeMedida unidadeMedidaTarget) {
     
        OptionalDouble conversao = getOptionalConversaoParaUnidadeDestino(material, unidadeMedidaOrigem, unidadeMedidaTarget);
        return (conversao.isPresent()) ? true : false;
        
    }
    
    public UnidadeMedida getUnidadeMedidaFromId(String unidadeMedidaId) {
        if (unidadeMedidaId == null || unidadeMedidaId.isBlank()) {
            throw new UnitOfMeasureConversionException("UOM id is required");
        }
        return unidadeMedidaSet.stream()
                .filter(unidadeMedida -> unidadeMedida.getId().equals(unidadeMedidaId))
                .findAny().orElseThrow(() -> new NoResultException("UOM " + unidadeMedidaId + " not found"));
    }

    public <T> ToDoubleFunction<T> funcaoGetQuantidadeNaUnidadeTarget(
            ToDoubleFunction<T> extratorValorDaClasse,
            Function<T,Produto> extratorMaterialDaClasse,
            Function<T,UnidadeMedida> extratorUnidadeMedidaDaClasse, 
            UnidadeMedida unidadeMedidaTarget) {
        
        return t -> 
                extratorValorDaClasse.applyAsDouble(t)
                * getConversaoParaUnidadeDestino(
                        extratorMaterialDaClasse.apply(t), 
                        extratorUnidadeMedidaDaClasse.apply(t), 
                        unidadeMedidaTarget);
        
    }
    
    /**
     * Atualiza o valor de um campo de uma classe, convertendo para a unidade
     * de medida estabelecida no próprio objeto
     * @param <T> classe a ser atualizada
     * @param unidadeMedidaValor unidade de medida do input
     * @param extratorMaterialDaClasse necessário para se obter conversão de unidade. exemplo: distributionPlanLinha -> distributionPlanLinha.getMaterial()
     * @param extratorUnidadeMedidaDaClasse será a unidade target da conversão. exemplo: distributionPlanLinha -> distributionPlanLinha.getUnidadeMedida()
     * @param setterCampoValor setter a ser acionado. exemplo: (distributionPlanLinha,valorInput) -> distributionPlanLinha.setQuantidade(valorInput)
     */
    public <T> BiConsumer<T,Double> consumerSetQuantidadeNaUnidadeTarget(
            UnidadeMedida unidadeMedidaValor,
            Function<T,Produto> extratorMaterialDaClasse, 
            Function<T,UnidadeMedida> extratorUnidadeMedidaDaClasse,
            BiConsumer<T,Double> setterCampoValor) {
        
        return (t,valor) -> setterCampoValor.accept(t, valor * getConversaoParaUnidadeDestino(
                extratorMaterialDaClasse.apply(t), 
                unidadeMedidaValor, 
                extratorUnidadeMedidaDaClasse.apply(t)));
        
    }

    public OptionalDouble getOptionalConversaoPadraoParaUnidadeDestino(
            UnidadeMedida unidadeMedidaOrigem,
            UnidadeMedida unidadeMedidaTarget) {

        String idUnidadeMedidaOrigem = unidadeMedidaOrigem.getId();
        String idUnidadeMedidaTarget = unidadeMedidaTarget.getId();

        if (idUnidadeMedidaOrigem.equals(idUnidadeMedidaTarget)) return OptionalDouble.of(1.0);

        // tenta buscar conversão direta (apenas usando a conversão padrão, ignorando o material)
        OptionalDouble conversaoDireta = getConversaoPadraoDiretaParaUnidadeDestino(unidadeMedidaOrigem, unidadeMedidaTarget);
        if (conversaoDireta.isPresent()) {
            return conversaoDireta;
        }

        // caso não encontre:
        // 1) busca recursivamente uma conversão indireta (apenas usando conversões padrão)
        // 2) armazena nova conversão no nível material (cálculo pesado, então é mantido em memória)

        // cópia do mapa de conversões padrão
        Map<UnidadeMedida,Map<UnidadeMedida,Double>> mapaConversoesPadraoCalculadas = new HashMap(mapaConversoesPadrao);

        OptionalDouble conversaoIndireta = getConversaoUnidadeMedidaRecursivo(unidadeMedidaOrigem, unidadeMedidaTarget, mapaConversoesPadraoCalculadas);

        // optional : empty ou calculado
        return conversaoIndireta;

    }

    public double getConversaoPadraoParaUnidadeDestino(
            UnidadeMedida unidadeMedidaOrigem,
            UnidadeMedida unidadeMedidaTarget) {

        OptionalDouble conversao = getOptionalConversaoPadraoParaUnidadeDestino(unidadeMedidaOrigem, unidadeMedidaTarget);
        if (!conversao.isPresent()) throw new UnitOfMeasureConversionException("No default conversion available from " + unidadeMedidaOrigem.getId() + " to " + unidadeMedidaTarget.getId());

        return conversao.getAsDouble();

    }
    
    
}
