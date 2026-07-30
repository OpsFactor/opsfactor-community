package com.opsfactor.community.platform.utility;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsfactor.community.platform.utility.Logger.LoggerImplementation;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.Normalizer;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class MetodosUtilidade {
    private static int lastCounter = 0;

    public static void counter(int counter, int size, int porcentagem) {
        if (((counter * 100 / size * 100) / 100) != lastCounter) {
            lastCounter = (counter * 100 / size * 100) / 100;
            if (lastCounter % 10 == 0 && lastCounter % porcentagem == 0) LoggerImplementation.print(lastCounter + "%");
            else if (lastCounter % 5 == 0) LoggerImplementation.print(lastCounter + "%");
        }
    }

    public static void exportaMatrizCSV(String arquivo, int[][] matrizInput) throws IOException {
        BufferedWriter br = new BufferedWriter(new FileWriter(arquivo));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < matrizInput.length; i++) {
            for (int j = 0; j < matrizInput[0].length; j++) {
                sb.append(matrizInput[i][j]);
                sb.append(";");
            }
            sb.append("\n");
        }
        br.write(sb.toString());
        br.close();
    }

    public static void exportaMatrizComValoresPorColuna(String arquivo, double[][] matrizInput,
                                                        List<String> valorI, List<String> valorJ,
                                                        String descricaoColunaI, String descricaoColunaJ,
                                                        String descricaoConteudoMatriz) throws IOException {
        BufferedWriter br = new BufferedWriter(new FileWriter(arquivo));
        StringBuilder sb = new StringBuilder();
        // header do CSV
        sb.append(descricaoColunaI + ";" + descricaoColunaJ + ";" + descricaoConteudoMatriz + "\n");
        for (int i = 0; i < matrizInput.length; i++) {
            for (int j = 0; j < matrizInput[0].length; j++) {
                sb.append(valorI.get(i));
                sb.append(";");
                sb.append(valorJ.get(j));
                sb.append(";");
                sb.append(matrizInput[i][j]);
                sb.append("\n");
            }
        }
        br.write(sb.toString());
        br.close();
    }

    /**
     * Cada elemento do array será Max(valor do array, valorMinimo)
     * @param array
     * @param valorMinimo
     * @return 
     */
    public static float[] atualizaArrayComValorMinimo(float[] array, float valorMinimo) {
        for (int i = 0,length = array.length; i < length; i++) {
            array[i] = Math.max(array[i], valorMinimo);
        }
        return array;
    }

    /**
     * Cada elemento do array será Max(valor do array, valorMinimo)
     * @param array
     * @param valorMinimo
     * @return
     */
    public static double[] atualizaArrayComValorMinimo(double[] array, double valorMinimo) {
        for (int i = 0,length = array.length; i < length; i++) {
            array[i] = Math.max(array[i], valorMinimo);
        }
        return array;
    }

    public static void exportaArrayCSV(String arquivo, int[] arrayInput) throws IOException {
        BufferedWriter br = new BufferedWriter(new FileWriter(arquivo));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arrayInput.length; i++) {
            sb.append(arrayInput[i]);
            sb.append("\n");
        }
        br.write(sb.toString());
        br.close();
    }

    // 
    // pode ser usado para inverter mapa de SKUs ou mapa de semanas

    /**
     * Função para trazer um array de SKUs/codigo subs de um mapa (função
     * inversa do mapa)
     *
     * @param mapa
     * @return retorna array onde a chave é o valor e o índice, o resultado do
     * mapa
     */
    public static int[] getMapKeys(Map<Integer, Integer> mapa) {
        int[] skus = new int[mapa.size()];
        for (int key : mapa.keySet()) {
            if (mapa.get(key) >= 0) {
                skus[mapa.get(key)] = key;
            }
        }
        return skus;
    }

    public static int[] getValorMaximoLinhas(int[][] matriz) {
        int[] valorMaximo = new int[matriz.length];
        for (int i = 0; i < matriz.length; i++) {
            for (int j = 0; j < matriz[0].length; j++) {
                valorMaximo[i] = Math.max(valorMaximo[i], matriz[i][j]);
            }
        }
        return valorMaximo;
    }

    public static int[] getValorMaximoLinhas(Integer[][] matriz) {
        int[] valorMaximo = new int[matriz.length];
        for (int i = 0; i < matriz.length; i++) {
            for (int j = 0; j < matriz[0].length; j++) {
                if (matriz[i][j] != null) {
                    valorMaximo[i] = Math.max(valorMaximo[i], matriz[i][j]);
                }
            }
        }
        return valorMaximo;
    }

    public static int[] getValorMaximoColunas(int[][] matriz) {
        int[] valorMaximo = new int[matriz[0].length];
        for (int i = 0; i < matriz[0].length; i++) {
            for (int j = 0; j < matriz.length; j++) {
                valorMaximo[i] = Math.max(valorMaximo[i], matriz[j][i]);
            }
        }
        return valorMaximo;
    }

    public static int getValorMaximoArray(int[] array) {
        int valorMaximo = array[0];
        for (int i = 0; i < array.length; i++) {
            if (array[i] > valorMaximo) {
                valorMaximo = array[i];
            }
        }
        return valorMaximo;
    }

    public static float getValorMaximoArray(float[] array) {
        float valorMaximo = array[0];
        for (int i = 0; i < array.length; i++) {
            if (array[i] > valorMaximo) {
                valorMaximo = array[i];
            }
        }
        return valorMaximo;
    }

    // atualiza o próprio objeto mas também retorna referência a ele mesmo
    public static int[] setaArray(int[] vetor, int valor) {
        int i = 0;
        while (i < vetor.length) {
            vetor[i] = valor;
            i++;
        }
        return vetor;
    }

    // atualiza o próprio objeto mas também retorna referência a ele mesmo
    public static double[] setaArray(double[] vetor, double valor) {
        int i = 0;
        while (i < vetor.length) {
            vetor[i] = valor;
            i++;
        }
        return vetor;
    }

    // atualiza o próprio objeto mas também retorna referência a ele mesmo
    public static Double[] setaArray(Double[] vetor, Double valor) {
        int i = 0;
        while (i < vetor.length) {
            vetor[i] = valor;
            i++;
        }
        return vetor;
    }

    // atualiza o próprio objeto mas também retorna referência a ele mesmo
    public static boolean[] setaArray(boolean[] vetor, boolean valor) {
        int i = 0;
        while (i < vetor.length) {
            vetor[i] = valor;
            i++;
        }
        return vetor;
    }


    // atualiza o próprio objeto mas também retorna referência a ele mesmo
    public static Integer[] setaArray(Integer[] vetor, int valor) {
        int i = 0;
        while (i < vetor.length) {
            vetor[i] = valor;
            i++;
        }
        return vetor;
    }

    public static float[][] setaMatriz(float[][] matriz, float valor) {
        int i = 0;
        int j = 0;
        while (i < matriz.length) {
            while (j < matriz[0].length) {
                matriz[i][j] = valor;
                j++;
            }
            i++;
        }
        return matriz;
    }

    // atualiza o próprio objeto mas também retorna referência a ele mesmo
    public static int[][] setaMatriz(int[][] matriz, int valor) {
        int i = 0;
        int j = 0;
        while (i < matriz.length) {
            j = 0;
            while (j < matriz[0].length) {
                matriz[i][j] = valor;
                j++;
            }
            i++;

        }
        return matriz;
    }

    // atualiza o próprio objeto mas também retorna referência a ele mesmo
    public static double[][] setaMatriz(double[][] matriz, double valor) {
        int i = 0;
        int j = 0;
        while (i < matriz.length) {
            j = 0;
            while (j < matriz[0].length) {
                matriz[i][j] = valor;
                j++;
            }
            i++;
        }
        return matriz;
    }

    public static int contaMenorQue(double[] array, double valorMaximo) {
        int contagem = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i] < valorMaximo) {
                contagem++;
            }
        }
        return contagem;
    }

    // atualiza o próprio objeto mas também retorna referência a ele mesmo
    public static Double[][] setaMatriz(Double[][] matriz, Double valor) {
        int i = 0;
        int j = 0;
        while (i < matriz.length) {
            j = 0;
            while (j < matriz[0].length) {
                matriz[i][j] = valor;
                j++;
            }
            i++;
        }
        return matriz;
    }


    // atualiza o próprio objeto mas também retorna referência a ele mesmo
    public static boolean[][] setaMatriz(boolean[][] matriz, boolean valor) {
        int i = 0;
        int j = 0;
        while (i < matriz.length) {
            j = 0;
            while (j < matriz[0].length) {
                matriz[i][j] = valor;
                j++;
            }
            i++;

        }
        return matriz;
    }

    public static int somaArray(int[] vetor) {
        int soma = 0;
        for (int i = 0; i < vetor.length; i++) {
            soma += vetor[i];
        }
        return soma;
    }

    public static float somaArray(float[] vetor) {
        float soma = 0;
        for (float v : vetor) {
            soma += v;
        }
        return soma;
    }

    public static double somaArray(double[] vetor) {
        double soma = 0;
        for (int i = 0; i < vetor.length; i++) {
            soma += vetor[i];
        }
        return soma;
    }
    
    public static int[] somaArrays(int[] array1, int[] array2) {
        int[] arrayOutput = new int[array1.length];
        for (int i = 0; i < array1.length; i++) {
            arrayOutput[i] = array1[i] + array2[i];
        }
        return arrayOutput;
    }
    
    public static float[] somaArrays(float[] array1, float[] array2) {
        float[] arrayOutput = new float[array1.length];
        for (int i = 0; i < array1.length; i++) {
            arrayOutput[i] = array1[i] + array2[i];
        }
        return arrayOutput;
    }

    public static double[] somaArrays(double[] array1, double[] array2) {
        double[] arrayOutput = new double[array1.length];
        for (int i = 0; i < array1.length; i++) {
            arrayOutput[i] = array1[i] + array2[i];
        }
        return arrayOutput;
    }
    
    public static int[][] somaMatrizes(int[][] matriz1, int[][] matriz2) {
        int linhas = matriz1.length;
        int colunas = matriz1[0].length;
        
        int[][] matrizOutput = new int[linhas][colunas];
        
        for (int i=0; i<linhas; i++) {
            for (int j=0; j<colunas; j++) {
                matrizOutput[i][j] = matriz1[i][j] + matriz2[i][j];
            }
        }
        return matrizOutput;
    }
    
    public static float[][] somaMatrizes(float[][] matriz1, float[][] matriz2) {
        int linhas = matriz1.length;
        int colunas = matriz1[0].length;
        
        float[][] matrizOutput = new float[linhas][colunas];
        
        for (int i=0; i<linhas; i++) {
            for (int j=0; j<colunas; j++) {
                matrizOutput[i][j] = matriz1[i][j] + matriz2[i][j];
            }
        }
        return matrizOutput;
    }
    
    public static double[][] somaMatrizes(double[][] matriz1, double[][] matriz2) {
        int linhas = matriz1.length;
        int colunas = matriz1[0].length;
        
        double[][] matrizOutput = new double[linhas][colunas];
        
        for (int i=0; i<linhas; i++) {
            for (int j=0; j<colunas; j++) {
                matrizOutput[i][j] = matriz1[i][j] + matriz2[i][j];
            }
        }
        return matrizOutput;
    }


    /**
     * extrai por ex. a venda semanal de uma matriz [semana][sku] ou
     * [semana][sub]
     *
     * @param matriz        int[semanas][skus]
     * @param posicaoSKUSub
     * @return array com por ex. a venda semanal para um dado SKU
     */
    public static int[] extraiArraySemanal(int[][] matriz, int posicaoSKUSub) {
        int[] arraySemanal = new int[matriz.length];
        for (int i = 0; i < matriz.length; i++) {
            arraySemanal[i] = matriz[i][posicaoSKUSub];
        }
        return arraySemanal;
    }

    /**
     * extrai por ex. a venda semanal de uma matriz [semana][sku] ou
     * [semana][sub]
     *
     * @param matriz        float[semanas][skus]
     * @param posicaoSKUSub
     * @return array com por ex. a venda semanal para um dado SKU
     */
    public static float[] extraiArraySemanal(float[][] matriz, int posicaoSKUSub) {
        float[] arraySemanal = new float[matriz.length];
        for (int i = 0; i < matriz.length; i++) {
            arraySemanal[i] = matriz[i][posicaoSKUSub];
        }
        return arraySemanal;
    }

    /**
     * extrai por ex. a venda semanal de uma matriz [semana][sku] ou
     * [semana][sub]
     *
     * @param matriz        float[semanas][skus]
     * @param posicaoSKUSub
     * @return array com por ex. a venda semanal para um dado SKU
     */
    public static double[] extraiArraySemanal(double[][] matriz, int posicaoSKUSub) {
        double[] arraySemanal = new double[matriz.length];
        for (int i = 0; i < matriz.length; i++) {
            arraySemanal[i] = matriz[i][posicaoSKUSub];
        }
        return arraySemanal;
    }


    /**
     * extrai por ex. a venda semanal de uma matriz [semana][sku] ou
     * [semana][sub] Não utilizado pois precisa de uma matriz Integer ou Float ,
     * não compatível com primitivas (muito mais rápidas)
     *
     * @param matriz        matriz Integer[semana][sku/sub] ou Float[semana][sku/sub]
     * @param posicaoSKUSub
     * @return array Integer ou Float com por ex. a venda semanal para um dado
     * SKU
     */
    public static <T extends Number> T[] extraiArraySemanal2(T[][] matriz, int posicaoSKUSub) {
        T[] arraySemanal;
        if (matriz[0][0].getClass() == int.class) {
            arraySemanal = (T[]) new Integer[matriz.length];
        } else {
            arraySemanal = (T[]) new Float[matriz.length];
        }
        for (int i = 0; i < matriz.length; i++) {
            arraySemanal[i] = matriz[i][posicaoSKUSub];
        }
        return (T[]) arraySemanal;
    }

    /**
     * atualiza na matriz [semanas][sku/sub] um array [sku/sub]
     *
     * @param matriz        int[semanas][skus]
     * @param skuSub        array com os dados semanais do sku ou sub
     * @param posicaoSKUSub posição do sku ou da sub
     * @return
     */
    public static int[][] atualizaSKUSubMatriz(int[][] matriz, int[] skuSub, int posicaoSKUSub) {
        for (int i = 0; i < matriz.length; i++) {
            matriz[i][posicaoSKUSub] = skuSub[i];
        }
        return matriz;
    }

    /**
     * atualiza na matriz [semanas][sku/sub] um array [sku/sub]
     *
     * @param matriz        float[semanas][skus]
     * @param skuSub        array com os dados semanais do sku ou sub
     * @param posicaoSKUSub posição do sku ou da sub
     * @return
     */
    public static float[][] atualizaSKUSubMatriz(float[][] matriz, float[] skuSub, int posicaoSKUSub) {
        for (int i = 0; i < matriz.length; i++) {
            matriz[i][posicaoSKUSub] = skuSub[i];
        }
        return matriz;
    }

    public static ArrayList<ArrayList<String>> adicionaColunaArrayList(ArrayList<ArrayList<String>> arrayList, String primeiraLinha, String demaisLinhas) {
        // adiciona 1a linha : nome da coluna
        arrayList.get(0).add(primeiraLinha);
        for (int i = 1; i < arrayList.size(); i++) {
            arrayList.get(i).add(demaisLinhas);
        }
        return arrayList;
    }

    public static ArrayList<ArrayList<String>> substituiValoresArrayList(ArrayList<ArrayList<String>> arrayList, String nomeColuna, ArrayList<String> valorAntigo, String novoValor) {
        int numeroColuna = arrayList.get(0).indexOf(nomeColuna);
        // adiciona 1a linha : nome da coluna
        for (int i = 1; i < arrayList.size(); i++) {
            if (valorAntigo.contains(arrayList.get(i).get(numeroColuna))) {
                ArrayList<String> novaLinha = arrayList.get(i);
                novaLinha.set(numeroColuna, novoValor);
                arrayList.set(i, novaLinha);
            }
        }
        return arrayList;
    }

    public static ArrayList<ArrayList<String>> removeValoresArrayList(ArrayList<ArrayList<String>> arrayList, String nomeColuna, ArrayList<String> valorAntigo, String novoValor) {
        int numeroColuna = arrayList.get(0).indexOf(nomeColuna);
        // adiciona 1a linha : nome da coluna
        for (int i = 1; i < arrayList.size(); i++) {
            if (valorAntigo.contains(arrayList.get(i).get(numeroColuna))) {
                arrayList.remove(i);
            }
        }
        return arrayList;
    }

    /**
     * Copia array de forma a manter referências independentes
     *
     * @param arrayInput
     * @return
     */
    public static int[] copiaArray(int[] arrayInput) {
        int[] arrayOutput = new int[arrayInput.length];
        System.arraycopy(arrayInput, 0, arrayOutput, 0, arrayInput.length);
        return arrayOutput;
    }

    /**
     * Copia array de forma a manter referências independentes
     *
     * @param arrayInput
     * @return
     */
    public static double[] copiaArray(double[] arrayInput) {
        double[] arrayOutput = new double[arrayInput.length];
        System.arraycopy(arrayInput, 0, arrayOutput, 0, arrayInput.length);
        return arrayOutput;
    }

    /**
     * Copia array de forma a manter referências independentes
     *
     * @param arrayInput
     * @return
     */
    public static float[] copiaArray(float[] arrayInput) {
        float[] arrayOutput = new float[arrayInput.length];
        System.arraycopy(arrayInput, 0, arrayOutput, 0, arrayInput.length);
        return arrayOutput;
    }

    /**
     * Copia array de forma a manter referências independentes
     *
     * @param arrayInput
     * @return
     */
    public static Integer[] copiaArray(Integer[] arrayInput) {
        Integer[] arrayOutput = new Integer[arrayInput.length];
        System.arraycopy(arrayInput, 0, arrayOutput, 0, arrayInput.length);
        return arrayOutput;
    }

    /**
     * Copia array de forma a manter referências independentes
     *
     * @param arrayInput
     * @return
     */
    public static int[][] copiaArray(int[][] arrayInput) {
        int[][] arrayOutput = new int[arrayInput.length][arrayInput[0].length];
        for (int i = 0; i < arrayInput.length; i++) {
            System.arraycopy(arrayInput[i], 0, arrayOutput[i], 0, arrayInput[0].length);
        }
        return arrayOutput;
    }

    /**
     * Copia array de forma a manter referências independentes
     *
     * @param arrayInput
     * @return
     */
    public static float[][] copiaArray(float[][] arrayInput) {
        float[][] arrayOutput = new float[arrayInput.length][arrayInput[0].length];
        for (int i = 0; i < arrayInput.length; i++) {
            System.arraycopy(arrayInput[i], 0, arrayOutput[i], 0, arrayInput[0].length);
        }
        return arrayOutput;
    }

    /**
     * Copia array de forma a manter referências independentes
     *
     * @param arrayInput
     * @return
     */
    public static Integer[][] copiaArray(Integer[][] arrayInput) {
        Integer[][] arrayOutput = new Integer[arrayInput.length][arrayInput[0].length];
        for (int i = 0; i < arrayInput.length; i++) {
            System.arraycopy(arrayInput[i], 0, arrayOutput[i], 0, arrayInput[0].length);
        }
        return arrayOutput;
    }

    /**
     * Função usada para definir primeira semana onde houve venda ou estoque a
     * partir de uma matriz [semana][produtoDevolucao/cluster]. Caso não seja encontrada
     * uma semana, retorna null para o produtoDevolucao/cluster
     *
     * @param input
     * @return para cada j no input [i][j] , define o 1o i para o qual [i][j] >
     * 0. se não encontrar, retorna null
     */
    public static Integer[] calculaPrimeiroPeriodoMaiorZero(float[][] input) {

        Integer[] output = new Integer[input[0].length];
        for (int j = 0; j < output.length; j++) {
            output[j] = null;
        }
        // valor padrão é -1, caso não tenha sido identificada semana com venda

        for (int j = 0; j < input[0].length; j++) {
            boolean continuaBuscando = true;
            for (int i = 0; i < input.length && continuaBuscando; i++) {
                if (input[i][j] > 0) {
                    output[j] = i;
                    continuaBuscando = false;
                }
            }
        }
        return output;
    }

    /**
     * Função usada para definir primeira semana onde houve venda ou estoque a
     * partir de um array [semana]. Caso não seja encontrada uma semana, retorna
     * -1 para o produtoDevolucao/cluster
     *
     * @param input
     * @return o 1o i para o qual [i] > 0
     */
    public static Integer calculaPrimeiroPeriodoMaiorZero(float[] input) {

        Integer output = null;

        boolean continuaBuscando = true;
        for (int i = 0; i < input.length && continuaBuscando; i++) {
            if (input[i] > 0) {
                output = i;
                continuaBuscando = false;
            }
        }
        return output;
    }

    /**
     * Função usada para definir o último período onde houve venda ou estoque a
     * partir de um array de vendas por período. Caso não seja encontrado um
     * período com vendas, retorna null
     *
     * @param input
     * @return o 1o i para o qual [i] > 0. se não achar, retorna null
     */
    public static Integer calculaUltimoPeriodoMaiorZero(float[] input) {

        Integer output = null;

        boolean continuaBuscando = true;
        for (int i = input.length - 1; i >= 0 && continuaBuscando; i--) {
            if (input[i] > 0) {
                output = i;
                continuaBuscando = false;
            }
        }
        return output;
    }

    /**
     * Função usada para definir o último período onde houve venda ou estoque a
     * partir de uma matriz [periodo][produtoDevolucao] de vendas por período. Caso não
     * seja encontrado um período com vendas, retorna null
     *
     * @param input
     * @return para cada j no input [i][j] , define o 1o i para o qual [i][j] >
     * 0. retorna null se não encontrar.
     */
    public static Integer[] calculaUltimoPeriodoMaiorZero(float[][] input) {

        Integer[] output = new Integer[input[0].length];
        for (int j = 0; j < output.length; j++) {
            output[j] = null;
        }
        // valor padrão é -1, caso não tenha sido identificada semana com venda

        for (int j = 0; j < input[0].length; j++) {
            boolean continuaBuscando = true;
            for (int i = 0; i < input.length && continuaBuscando; i++) {
                if (input[i][j] > 0) {
                    output[j] = i;
                    continuaBuscando = false;
                }
            }
        }
        return output;
    }

    /**
     * Dada uma lista de observações e uma observação, retorna o percentil da
     * observação Função inversa da disponível em Excel : PERCENTILE.EXC Retorna
     * nulo caso listaObservacoes seja nula ou vazia.
     *
     * @param listaObservacoes
     * @param observacao
     * @return
     */
    public static Float getPercentilDaObservacao(List<Float> listaObservacoes, float observacao) {

        if (listaObservacoes == null || listaObservacoes.isEmpty()) {
            return null;
        }

        List<Float> listaObservacoesClone = new ArrayList<>();
        listaObservacoesClone.addAll(listaObservacoes);

        int tamanhoLista = listaObservacoes.size();
        int posicaoObservacao = tamanhoLista - 1;
        Collections.sort(listaObservacoesClone);
        if (observacao >= listaObservacoesClone.get(posicaoObservacao)) { // se maior que valor da última posição da lista
            return 1f;
        } else if (observacao < listaObservacoesClone.get(0)) {
            return 0f;
        }

        // encontra a posição do primeiro elemento menor ou igual à observação
        while (posicaoObservacao - 1 >= 0 && listaObservacoesClone.get(posicaoObservacao) > observacao) {
            posicaoObservacao--;
        }

        float valorAnterior = listaObservacoesClone.get(posicaoObservacao);
        float proximoValor = listaObservacoesClone.get(posicaoObservacao + 1);
        float fatorInterpolacao = (observacao - valorAnterior) / (proximoValor - valorAnterior);

        return ((fatorInterpolacao) * (posicaoObservacao + 1) + (1 - fatorInterpolacao) * (posicaoObservacao)) / (tamanhoLista - 1);

    }




    public static String renomeiaArquivoEnviado(String filename) {
        String[] arqParsed = filename.split("\\u002e");
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < arqParsed.length; i++) {
            String str = arqParsed[i];
            if (i == 0) {
                stringBuilder.append(str + "[ENV]");
            } else {
                stringBuilder.append("\u002e" + str);
            }
        }
        return stringBuilder.toString();
    }

    public static Float ifNullZero(Float valor) {
        return valor == null ? 0 : valor;
    }

    public static float round(float number, int decimalPlace) {
        float scaleFactor = (float) Math.pow(10, decimalPlace);
        return Math.round(number * scaleFactor) / scaleFactor;
    }

    public static double round(double number, int decimalPlace) {
        double scaleFactor = Math.pow(10, decimalPlace);
        return Math.round(number * scaleFactor) / scaleFactor;
    }

    public static float[] roundArray(float[] number, int decimalPlace) {
        float[] numbers = new float[number.length];
        for (int i = 0, numberLength = number.length; i < numberLength; i++) {
            numbers[i] = round(number[i], decimalPlace);
        }
        return numbers;
    }

    public static double[] roundArray(double[] number, int decimalPlace) {
        double[] numbers = new double[number.length];
        for (int i = 0, numberLength = number.length; i < numberLength; i++) {
            numbers[i] = round(number[i], decimalPlace);
        }
        return numbers;
    }

    public static int fatorial(int n) {

        if (n < 0) {
            throw new IllegalArgumentException("Factorial input must be zero or positive.");
        }
        if (n == 0 || n == 1) {
            return 1;
        }
        return n * fatorial(n - 1);

    }

    public static String removeSpecialsChars(String chars) {
        String textoEditado = chars;
        if (textoEditado != null) {
            textoEditado = Normalizer.normalize(textoEditado, Normalizer.Form.NFD);
            textoEditado = textoEditado.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
            return textoEditado;
        }
        return textoEditado;
    }
    
    public static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValue(Map<K, V> unsortMap) {

        List<Map.Entry<K, V>> list =
                new LinkedList<Map.Entry<K, V>>(unsortMap.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;

    }
    
    public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
        Map<Object, Boolean> map = new ConcurrentHashMap<>();
        return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
    
    public static Map<String,String> converteMapaParaStrings(Map<?,?> mapaInput) {
        Map<String,String> mapaOutput = new HashMap<>();
        for (Object object : mapaInput.keySet()) {
            mapaOutput.put(object.toString(), mapaInput.get(object).toString());
        }
        return mapaOutput;
    }

    public static double[] getMatrizLinearizada(double[][] matriz) {
        double[] array = new double[matriz.length * matriz[0].length];
        for (int i=0; i< matriz.length; i++) {
            for (int j=0; j < matriz[0].length; j++) {
                array[(i*matriz[0].length) + j] = matriz[i][j];
            }
        }
        return array;
    }
    
    public static double getSomaElementosMatriz(double[][] matriz, boolean somaEmModulo) {
        double acumulado = 0;
        for (int i=0; i< matriz.length; i++) {
            for (int j=0; j < matriz[0].length; j++) {
                if (somaEmModulo) {
                    acumulado += Math.abs(matriz[i][j]);
                } else {
                    acumulado += matriz[i][j];
                }
            }
        }
        return acumulado;
    }
    
    public static <T, Y> Map<T, Set<Y>> inverteMapa(Map<Y,T> mapaInput) {
        Map<T, Set<Y>> mapaOutput = mapaInput.entrySet()
               .stream()
               .collect(Collectors.groupingBy(Map.Entry::getValue, 
                       Collectors.mapping(Map.Entry::getKey, Collectors.toSet())));
        
        return mapaOutput;
    }
    
    public static <T, Y> Map<Y, T> inverteMapaComCollection(Map<T,Collection<Y>> mapaInput) {
        Map<Y,T> mapaOutput = new HashMap();
        for (Entry entry : mapaInput.entrySet()) {
            for (Y y : (Collection<Y>) entry.getValue()) {
                mapaOutput.put(y, (T) entry.getKey());
            }
        }
        return mapaOutput;
    }
    
    public static void setaDiagonal(double[][] matriz, double valor) {
        for (int i = 0; i < matriz.length; i++) {
            if (i < matriz[i].length) {
                matriz[i][i] = valor;
            }
        }
    }
    
    public static float[] converteArrayDoubleParaFloat(double[] arrayDouble) {
        float[] arrayFloat = new float[arrayDouble.length];
        for (int i=0; i<arrayDouble.length; i++) {
            arrayFloat[i] = (float) arrayDouble[i];
        }
        return arrayFloat;
    }
    
    public static double[] converteArrayFloatParaDouble(float[] arrayFloat) {
        double[] arrayDouble = new double[arrayFloat.length];
        for (int i=0; i<arrayFloat.length; i++) {
            arrayDouble[i] = (double) arrayFloat[i];
        }
        return arrayDouble;
    }
    
    /**
     * Converte valores textuais usados em arquivos de integracao para boolean.
     *
     * <p>O contrato aceito e propositalmente pequeno: `1`, `0`, `true` e
     * `false`, ignorando espacos laterais. Valores nulos, vazios ou qualquer
     * outro texto indicam dado invalido no arquivo e devem falhar de forma
     * explicita para que o caller consiga apontar a coluna correta.</p>
     */
    public static boolean converteStringParaBoolean(String booleanComoString) {

        if (booleanComoString == null || booleanComoString.isBlank()) {
            throw new IllegalArgumentException("Boolean value must be provided as 0/1 or true/false.");
        }

        String booleanComoStringNormalizado = booleanComoString.trim();

        if (booleanComoStringNormalizado.equals("1")) return true;
        else if (booleanComoStringNormalizado.equals("0")) return false;
        else if (booleanComoStringNormalizado.equalsIgnoreCase("true")) return true;
        else if (booleanComoStringNormalizado.equalsIgnoreCase("false")) return false;
        else throw new IllegalArgumentException("String " + booleanComoString + " is not a boolean value: should be 0/1 or true/false");

    }
    
    /**
     * Exemplo: enum X { @JsonProperty("campoA") A, @JsonProperty("campoB") B}
     * getValorStringDeEnum(X.class, "campoA") retorna X.A
     * @param classeEnum
     * @param valorJsonPropertyEnum
     * @return 
     */
    public static <T extends Enum<T>> Optional<T> getOptionalValorEnumDeJsonProperty(Class<T> classeEnum, String valorJsonPropertyEnum) {
        if (classeEnum == null || valorJsonPropertyEnum == null) return Optional.empty();
        Field[] fields = classeEnum.getFields();
        for (int i=0; i<fields.length; i++) {
            // se constante do enum não tiver @JsonProperty, checa o valor da constante
            if (fields[i].getAnnotation(JsonProperty.class) == null) {
                if (fields[i].getName().equalsIgnoreCase(valorJsonPropertyEnum)) return Optional.of(Enum.valueOf(classeEnum, fields[i].getName())); 
            // se possuir @JsonProperty, checa o valor da anotação
            } else if (fields[i].getAnnotation(JsonProperty.class).value().equalsIgnoreCase(valorJsonPropertyEnum)) {
                return Optional.of(Enum.valueOf(classeEnum, fields[i].getName()));
            }
        }
        return Optional.empty();
    }
    
    /**
     * Exemplo: enum X { @JsonProperty("campoA") A, @JsonProperty("campoB") B}
     * getValorStringDeEnum(X.class, "campoA") retorna X.A
     * Mesmo que getOptional, mas retorna erro se não encontrar match para String valorJsonPropertyEnum
     * @param classeEnum
     * @param valorJsonPropertyEnum
     * @return 
     */
    public static <T extends Enum<T>> T getValorEnumDeJsonProperty(Class<T> classeEnum, String valorJsonPropertyEnum) {
        if (classeEnum == null || valorJsonPropertyEnum == null) return null;
        Field[] fields = classeEnum.getFields();
        for (int i=0; i<fields.length; i++) {
            // se constante do enum não tiver @JsonProperty, checa o valor da constante
            if (fields[i].getAnnotation(JsonProperty.class) == null) {
                if (fields[i].getName().equalsIgnoreCase(valorJsonPropertyEnum)) return Enum.valueOf(classeEnum, fields[i].getName()); 
            // se possuir @JsonProperty, checa o valor da anotação
            } else if (fields[i].getAnnotation(JsonProperty.class).value().equalsIgnoreCase(valorJsonPropertyEnum)) {
                return Enum.valueOf(classeEnum, fields[i].getName());
            }
        }
        /*
         * Se nao encontrar nenhum dos dois, o valor recebido nao condiz com
         * nenhuma anotacao @JsonProperty nem com o nome de constante. Isso e
         * argumento invalido do caller, nao ausencia de implementacao.
         */
        throw new IllegalArgumentException("Invalid value " + valorJsonPropertyEnum + ". Should be one of " +
                Arrays.stream(fields)
                        .map(x -> x.getName())
                        .collect(Collectors.joining(",")));
    }
    
    public static <T extends Enum<T>> String getValorJsonPropertyDeEnum(T enumValue) {
        if (enumValue == null) return null;
        Field[] fields = enumValue.getDeclaringClass().getFields();
        for (int i=0; i<fields.length; i++) {
            if (fields[i].getName().equalsIgnoreCase(enumValue.name())) {
                JsonProperty annotation = fields[i].getAnnotation(JsonProperty.class);
                if (annotation == null) return enumValue.name();
                return fields[i].getAnnotation(JsonProperty.class).value();
            }
        }
        return enumValue.toString();
    }
    
    public <T extends Number> int getRank (T valor, Collection<T> valores) {
        List<T> listaOrdenada = valores.stream().sorted().collect(Collectors.toList());
        for (int i = 0; i < valores.size(); i++) {
            if (valor.doubleValue() <= listaOrdenada.get(i).doubleValue()) return i+1;
        }
        return valores.size() + 1;
    }
    
    /**
     * Distribui um valor entre objetos de uma collection usando a participacao
     * proporcional de um campo de referencia.
     *
     * <p>Este helper e usado em rotinas de planejamento para ajustar linhas
     * operacionais sem acoplar a classe utilitaria ao tipo concreto de entidade
     * ou projection.</p>
     *
     * @param novoValor valor a ser distribuído entre os objetos T da lista
     * @param lista ex; List<DistributionPlanItem>
     * @param getterCampoReferenciaProporcao ex: distributionPlanItem -> distributionPlanItem.getQuantidadeRequisicaoAtendimentoCarteira()
     * @param setterCampoAAtualizar  ex: (distributionPlanItem, valorAtualizacao) -> distributionPlanItem.setQuantidadeRequisicaoAtendimentoCarteira((float) valorAtualizacao.floatValue())
     */
    public static <T> void setaValorProporcional(
            double novoValor,
            Collection<T> lista, 
            ToDoubleFunction<T> getterCampoReferenciaProporcao,
            BiConsumer<T,Double> setterCampoAAtualizar) {

        // soma campos referência para se chegar ao denominador da proporção
        double valorAcumulado = lista.stream()
                .mapToDouble(getterCampoReferenciaProporcao)
                .sum();

        // valor percentual da participação de cada objeto T
        ToDoubleFunction<T> funcaoValorASetar = x -> novoValor * getterCampoReferenciaProporcao.applyAsDouble(x) / valorAcumulado;
        
        // se totalizador > 0, chama o setter atualizando a proporção do valor
        if (Math.abs(valorAcumulado) > 0) {
            lista.stream().forEach(x -> setterCampoAAtualizar.accept(x, funcaoValorASetar.applyAsDouble(x)));
        // se totalizador = 0, seta valor 0 para todos os elementos da lista
        } else {
            lista.stream().forEach(x -> setterCampoAAtualizar.accept(x, 0.0));
        }
        
    }
    
    /**
     * Para uma collection de determinada classe (ex. DistributionPlanItem) , se atualiza um campo com um valor seguindo a proporção
     * de valores para um outro campo
     * @param novoValor valor a ser distribuído entre os objetos T da lista
     * @param lista ex; List<DistributionPlanItem>
     * @param getterCampoReferenciaProporcao ex: distributionPlanItem -> distributionPlanItem.getQuantidadeRequisicaoAtendimentoCarteira()
     * @param getterCampoAAtualizar necessário para se calcular o novo valor como 'modificacao + valor atual do campo a atualizar'
     * @param setterCampoAAtualizar  ex: (distributionPlanItem, valorAtualizacao) -> distributionPlanItem.setQuantidadeRequisicaoAtendimentoCarteira((float) valorAtualizacao.floatValue())
     */
    public static <T> void modificaValorProporcional(
            double modificacao,
            Collection<T> lista, 
            ToDoubleFunction<T> getterCampoReferenciaProporcao,
            ToDoubleFunction<T> getterCampoAAtualizar,
            BiConsumer<T,Double> setterCampoAAtualizar) {

        // soma campos referência para se chegar ao denominador da proporção
        double valorAcumulado = lista.stream()
                .mapToDouble(getterCampoReferenciaProporcao)
                .sum();
        
        // valor percentual da participação de cada objeto T
        ToDoubleFunction<T> funcaoValorASetar = x -> (modificacao * getterCampoReferenciaProporcao.applyAsDouble(x) / valorAcumulado) + getterCampoAAtualizar.applyAsDouble(x);
        
        // se totalizador > 0, chama o setter atualizando a proporção do valor
        if (Math.abs(valorAcumulado) > 0) {
            lista.stream().forEach(x -> setterCampoAAtualizar.accept(x, funcaoValorASetar.applyAsDouble(x)));
        // se totalizador = 0, seta valor 0 para todos os elementos da lista
        } else {
            lista.stream().forEach(x -> setterCampoAAtualizar.accept(x, 0.0));
        }
        
    }
    
    public static Stream<Integer> getIntRangeAsStream(int posicaoInicial, int posicaoFinal) {
        return Stream.iterate(posicaoInicial, x -> x + 1)
                .limit(posicaoFinal - posicaoInicial + 1);
    }
    
    
    public static <T> float somaFloatSobreRangeInt(
        int posicaoInicial, int posicaoFinal, T t,
        BiFunction<Integer, T, Float> funcao) {

        return getIntRangeAsStream(posicaoInicial, posicaoFinal)
                .map(i -> funcao.apply(i, t))
                .reduce(0F, (x1, x2) -> x1 + x2);
        
    }

    public static <T> double somaDoubleSobreRangeInt(
        int posicaoInicial, int posicaoFinal, T t,
        BiFunction<Integer, T, Double> funcao) {

        return getIntRangeAsStream(posicaoInicial, posicaoFinal)
                .mapToDouble(i -> funcao.apply(i, t))
                .sum();

    }

    public static String getStackTraceAsString(Exception t) {

        StringBuilder stackTraceAsString = new StringBuilder(t.toString());
        for (StackTraceElement stackTraceElement : t.getStackTrace()) {
            stackTraceAsString
                    .append(System.lineSeparator())
                    .append("\tat ")
                    .append(stackTraceElement);
        }

        return stackTraceAsString.toString();
        
    }
    
    public static Boolean isValidJson(String jsonText) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.readTree(jsonText);
            return true; // JSON is valid
        } catch (JsonParseException | JsonMappingException e) {
            // Exception occurred, JSON is not valid
            return false;
        } catch (IOException e) {
            // Other IO exception occurred
            log.error("Erro de IO ao validar JSON", e);
            return false;
        }
    }
    
    public static Map<String,String> getMapFromJson(String jsonText) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> map = mapper.readValue(jsonText, Map.class);
        return map;
    }

    /**
     * Exemplos para 4 digitos significativos:
     * 12345.678 → 12350
     * 0.001234567 → 0.001235
     * 12.34567 → 12.35
     * 0.00098765 → 0.0009877
     * @param numero
     * @param numeroDigitos
     * @return
     */
    public static String getNumeroComNumeroDeDigitosSignificativos(double numero, int numeroDigitos) {
        if (numero == 0.0) return "0";
        BigDecimal bd = new BigDecimal(numero, new MathContext(numeroDigitos)); // # dígitos significativos
        return bd.stripTrailingZeros().toPlainString();          // tira zeros desnecessários
    }

}
