package com.opsfactor.community.capability.masterdata.production.operation.integration.service;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.production.operation.domain.OperacaoRoteiro;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.production.operation.repository.OperacaoRoteiroRepository;
import com.opsfactor.community.capability.masterdata.production.productionresource.repository.RecursoProdutivoRepository;
import com.opsfactor.community.capability.masterdata.production.routing.repository.RoteiroRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import com.opsfactor.community.platform.integration.service.IntegrationPersistenceValidation;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.utility.FuncoesMap;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.opsfactor.community.platform.integration.service.IntegrationSupportDataValidation.getMapaPorIdObrigatorio;

/**
 * Carga e exportacao das operacoes basicas de roteiro usadas pelo Supply Planning Community.
 *
 * <p>Este contrato descreve somente consumo tecnico de capacidade produtiva:
 * recurso, quantidade base, unidade de medida e horas por quantidade base. Setup
 * detalhado, manutencao, custos de recurso, turnos e line scheduling pertencem
 * ao OpsFactor Enterprise e nao aparecem neste arquivo de carga.</p>
 */
@Component
@Slf4j
public class OperacaoRoteiroIntegrationService {

    /**
     * Repository de UOMs usadas na quantidade base da operacao.
     */
    @Autowired
    private UnidadeMedidaRepository unidadeMedidaRepository;

    /**
     * Repository de recursos produtivos usados pelas operacoes do roteiro.
     */
    @Autowired
    private RecursoProdutivoRepository recursoProdutivoRepository;

    /**
     * Repository dos roteiros pai das operacoes.
     */
    @Autowired
    private RoteiroRepository roteiroRepository;

    /**
     * Repository das operacoes de roteiro persistidas.
     */
    @Autowired
    private OperacaoRoteiroRepository operacaoRoteiroRepository;

    /**
     * Parametros globais usados para resolver a UOM default quando a operacao
     * nao possui unidade propria.
     */
    @Autowired
    private ParametrosGlobaisService parametrosGlobaisService;

    /**
     * Exporta as operacoes de roteiro no layout Community.
     *
     * <p>O arquivo contem apenas recurso, sequencia, quantidade base, UOM e
     * horas por quantidade base. Setup detalhado, turnos, custos e line
     * scheduling nao fazem parte deste contrato.</p>
     */
    public List<List<Object>> getFile() {

        ParametrosGlobais parametrosGlobais = parametrosGlobaisService.getParametrosGlobais();

        List<List<Object>> linhasArquivo = new ArrayList<>();

        List<Object> linhaHeader = new ArrayList<>();
        linhaHeader.add("Routing Id");
        linhaHeader.add("Operation Sequence (Integer number)");
        linhaHeader.add("Production Resource Id");
        linhaHeader.add("Base Quantity");
        linhaHeader.add("Base Quantity UOM");
        linhaHeader.add("Hours by Base Quantity");

        linhasArquivo.add(linhaHeader);

        for (Roteiro roteiro : roteiroRepository.findAll()) {
            for (OperacaoRoteiro operacaoRoteiro : roteiro.getOperacaoRoteiroListOrdenadaPorPosicaoAsc()) {
                List<Object> linhaDados = new ArrayList<>();
                linhaDados.add(roteiro.getId());
                linhaDados.add(operacaoRoteiro.getPosicao());
                linhaDados.add(operacaoRoteiro.getRecursoProdutivo().getId());
                linhaDados.add(operacaoRoteiro.getQuantidadeBase());
                linhaDados.add(operacaoRoteiro.getUnidadeMedida(parametrosGlobais).getId());
                linhaDados.add(operacaoRoteiro.getHorasPorQuantidadeBase());
                linhasArquivo.add(linhaDados);
            }
        }
        return linhasArquivo;

    }

    /**
     * Importa operacoes de roteiro a partir do arquivo manual.
     *
     * <p>O metodo carrega roteiros, recursos, operacoes existentes e UOMs uma
     * vez no inicio, indexa em mapas e entao processa linhas sem consultar o
     * banco a cada registro.</p>
     */
    public String saveFile(ProcessedFile processedFile) throws DataUploadException {

        validaProcessedFile(processedFile);

        List<RecursoProdutivo> recursoProdutivo = recursoProdutivoRepository.findAll();
        List<Roteiro> roteiros = roteiroRepository.findAll();
        List<OperacaoRoteiro> operacoesRoteiro = operacaoRoteiroRepository.customFindAll();
        List<UnidadeMedida> unidadesMedida = unidadeMedidaRepository.findAll();

        /*
         * Essas listas sao snapshots de support data usados em memoria durante
         * todo o upload. Falhar aqui deixa claro que o problema veio da consulta
         * base, antes de `Collectors.toMap` ou getters encadeados esconderem a
         * origem em NullPointerException.
         */
        Map<String, RecursoProdutivo> mapaRecursoProdutivo = getMapaPorIdObrigatorio(
                recursoProdutivo,
                RecursoProdutivo::getId,
                "Production resource snapshot");
        Map<String, Roteiro> mapaRoteiros = getMapaPorIdObrigatorio(
                roteiros,
                Roteiro::getId,
                "Routing snapshot");
        Map<String, Map<String, OperacaoRoteiro>> mapaOperacoesRoteiro =
                getMapaOperacoesRoteiroExistentes(operacoesRoteiro);
        Map<String, UnidadeMedida> mapaUnidadesMedida = getMapaPorIdObrigatorio(
                unidadesMedida,
                UnidadeMedida::getId,
                "Unit of Measure snapshot");

        List<OperacaoRoteiro> listaObjetosASalvar = new ArrayList<>();
        List<OperacaoRoteiro> listaObjetosADeletar = new ArrayList<>();

        for (int i = 0; i < processedFile.getFileRows().size(); i++) {
            int posicaoLinha = i + 1;
            List<String> linhaAtual = processedFile.getFileRowAsStringListWithEmptyFieldsAsEmptyStrings(i);
            // 1a linha é o header e deve ser ignorada
            if (i > 0) {
                // check de # colunas
                if (linhaAtual.size() > 7) {
                    throw new DataUploadException("Number of columns does not match template file at line " + (i + 1));
                }

                String routingId = (linhaAtual.size() >= 1) ? linhaAtual.get(0) : "";
                String sequence = (linhaAtual.size() >= 2) ? linhaAtual.get(1) : "";
                String productionResourceId = (linhaAtual.size() >= 3) ? linhaAtual.get(2) : "";
                String baseQty = (linhaAtual.size() >= 4) ? linhaAtual.get(3) : "";
                String uom = (linhaAtual.size() >= 5) ? linhaAtual.get(4) : "";
                String hoursByBaseQty = (linhaAtual.size() >= 6) ? linhaAtual.get(5) : "";
                String delete = (linhaAtual.size() >= 7) ? linhaAtual.get(6) : "";

                // posição da operação deve ter sido preenchida e ser um inteiro
                if (sequence.equals("")) {
                    throw new DataUploadException("Sequence is empty at line " + (i + 1));
                }
                Integer posicaoOperacao;
                try {
                    posicaoOperacao = Integer.valueOf(sequence);
                } catch (NumberFormatException numberFormatException) {
                    throw new DataUploadException(
                            "Sequence is not a valid Integer number at line " + (i + 1) + " : no decimals should be used.",
                            numberFormatException);
                }

                // roteiro é obrigatório
                Roteiro optionalRoteiro = Optional.ofNullable(mapaRoteiros.get(routingId))
                        .orElseThrow(() -> new DataUploadException("Routing Id not found at line " + posicaoLinha));

                // recurso produtivo é obrigatório
                RecursoProdutivo optionalRecursoProdutivo = Optional.ofNullable(mapaRecursoProdutivo.get(productionResourceId))
                        .orElseThrow(() -> new DataUploadException("Production Resource Id not found at line " + posicaoLinha));

                // busca objeto em lista no banco de dados ou cria um novo
                OperacaoRoteiro operacaoRoteiro = FuncoesMap.getOrAddElementoDeNestedMap(
                        mapaOperacoesRoteiro,
                        OperacaoRoteiro.class,
                        () -> new OperacaoRoteiro(new OperacaoRoteiro.OperacaoRoteiroCompositeKey(
                                posicaoOperacao, optionalRoteiro)),
                        posicaoOperacao, routingId);

                operacaoRoteiro.setRecursoProdutivo(optionalRecursoProdutivo);

                if (!uom.equals("")) {
                    operacaoRoteiro.setUnidadeMedida(
                    Optional.ofNullable(mapaUnidadesMedida.get(uom))
                        .orElseThrow(() -> new DataUploadException("Unit of Measure " + uom + " not found at line " + posicaoLinha)));
                }

                if (!baseQty.equals("")) {
                    try {
                        operacaoRoteiro.setQuantidadeBase(Float.valueOf(baseQty));
                    } catch (NumberFormatException numberFormatException) {
                        throw new DataUploadException(
                                "Invalid base quantity at line " + (i + 1) + " : should be a float number with '.' as decimal and no thousands separator.",
                                numberFormatException);
                    }
                }

                if (!hoursByBaseQty.equals("")) {
                    try {
                        operacaoRoteiro.setHorasPorQuantidadeBase(Float.valueOf(hoursByBaseQty));
                    } catch (NumberFormatException numberFormatException) {
                        throw new DataUploadException(
                                "Invalid number of hours at line " + (i + 1) + " : should be a float number with '.' as decimal and no thousands separator.",
                                numberFormatException);
                    }
                }

                try {
                    operacaoRoteiro.valida();
                } catch (IllegalStateException illegalStateException) {
                    /*
                     * A validacao da entidade traz o contexto funcional da
                     * inconsistência. O upload apenas acrescenta a linha do
                     * arquivo, preservando a causa original para diagnostico.
                     */
                    throw new DataUploadException(
                            illegalStateException.getMessage() + " at line " + (i + 1),
                            illegalStateException);
                }

                if (delete.equals("D")) {
                    listaObjetosADeletar.add(operacaoRoteiro);
                } else {
                    listaObjetosASalvar.add(operacaoRoteiro);
                }
            }
        }

        try {
            IntegrationPersistenceValidation.validaSavedEntityCollection(
                    operacaoRoteiroRepository.saveAll(listaObjetosASalvar),
                    "Routing Operation saved collection",
                    listaObjetosASalvar.size());
        } catch (DataAccessException dataAccessException) {
            throw new DataUploadException(
                    "Error saving Routing Operations " + dataAccessException.toString(),
                    dataAccessException);
        }

        if (listaObjetosADeletar.size() > 0) {
            try {
                operacaoRoteiroRepository.deleteAll(listaObjetosADeletar);
                return "Routing Operation data uploaded and items marked for deletion removed";
            } catch (DataAccessException dataAccessException) {
                throw new DataUploadException(
                        "Error deleting Routing Operations " + dataAccessException.toString(),
                        dataAccessException);
            }
        }
        return "Routing Operation data uploaded";
    }

    /**
     * Valida o envelope de arquivo antes de acessar linhas do upload.
     *
     * <p>Arquivo nulo representa erro de chamada do controller/pipeline de
     * upload, nao arquivo vazio. Arquivo vazio continua valido e resultara em
     * nenhum objeto salvo, preservando o comportamento historico.</p>
     */
    private void validaProcessedFile(ProcessedFile processedFile) throws DataUploadException {

        if (processedFile == null) {
            throw new DataUploadException("Routing Operation upload file is required.");
        }
        if (processedFile.getFileRows() == null) {
            throw new DataUploadException("Routing Operation upload rows are required.");
        }

    }

    /**
     * Indexa operacoes de roteiro ja persistidas por sequencia e roteiro.
     *
     * <p>A chave composta e usada para reaproveitar operacoes existentes durante
     * o upload. Se algum item persistido vier sem posicao, roteiro ou id de
     * roteiro, continuar criaria uma operacao nova em paralelo ou falharia em
     * getter encadeado sem contexto funcional.</p>
     */
    private Map<String, Map<String, OperacaoRoteiro>> getMapaOperacoesRoteiroExistentes(
            List<OperacaoRoteiro> operacaoRoteiroList) throws DataUploadException {

        if (operacaoRoteiroList == null) {
            throw new DataUploadException("Routing Operation snapshot returned null.");
        }

        Map<String, Map<String, OperacaoRoteiro>> mapaOperacoesRoteiro = new LinkedHashMap<>();
        for (int indice = 0;
             indice < operacaoRoteiroList.size();
             indice++) {
            OperacaoRoteiro operacaoRoteiro = operacaoRoteiroList.get(indice);
            if (operacaoRoteiro == null) {
                throw new DataUploadException("Routing Operation snapshot returned null item at index " + indice + ".");
            }

            OperacaoRoteiro.OperacaoRoteiroCompositeKey operacaoRoteiroCompositeKey =
                    operacaoRoteiro.getOperacaoRoteiroCompositeKey();
            if (operacaoRoteiroCompositeKey == null) {
                throw new DataUploadException("Routing Operation snapshot returned item without primary key at index " + indice + ".");
            }
            if (operacaoRoteiroCompositeKey.getPosicao() == null) {
                throw new DataUploadException("Routing Operation snapshot returned item without sequence at index " + indice + ".");
            }

            Roteiro roteiro = operacaoRoteiroCompositeKey.getRoteiro();
            if (roteiro == null) {
                throw new DataUploadException("Routing Operation snapshot returned item without routing at index " + indice + ".");
            }
            if (roteiro.getId() == null || roteiro.getId().isBlank()) {
                throw new DataUploadException("Routing Operation snapshot returned item without routing id at index " + indice + ".");
            }

            String posicaoOperacao = operacaoRoteiroCompositeKey.getPosicao().toString();
            Map<String, OperacaoRoteiro> mapaOperacoesRoteiroPorRoteiro =
                    mapaOperacoesRoteiro.computeIfAbsent(
                            posicaoOperacao,
                            ignored -> new LinkedHashMap<>());
            if (mapaOperacoesRoteiroPorRoteiro.put(roteiro.getId(), operacaoRoteiro) != null) {
                throw new DataUploadException(
                        "Routing Operation snapshot returned duplicated operation "
                                + posicaoOperacao
                                + " for routing "
                                + roteiro.getId()
                                + ".");
            }
        }

        return mapaOperacoesRoteiro;

    }

}
