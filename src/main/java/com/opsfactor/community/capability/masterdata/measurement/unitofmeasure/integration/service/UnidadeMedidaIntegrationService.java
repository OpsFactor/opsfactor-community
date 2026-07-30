package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.service;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.dto.UnidadeMedidaDataUploadDTO;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.mapper.UnidadeMedidaIntegrationAutoMapper;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.platform.integration.service.IntegrationPersistenceValidation;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Integracao Community de unidades de medida.
 *
 * <p>UOM e master data essencial para Demand/Supply Community. O service
 * aceita apenas id, descricao e delete fisico do cadastro de unidade; regras
 * Enterprise de capacidade logistica, custos ou precificacao nao aparecem
 * neste payload.</p>
 */
@Component
public class UnidadeMedidaIntegrationService {

    /**
     * Repository do cadastro de unidades de medida.
     */
    @Autowired
    private UnidadeMedidaRepository unidadeMedidaRepository;

    /**
     * Mapper de arquivo/DTO da integracao manual de unidades de medida.
     */
    @Autowired
    private UnidadeMedidaIntegrationAutoMapper unidadeMedidaDataUploadAutoMapper;

    /**
     * Lista unidades de medida persistidas no formato DTO de integracao.
     */
    public List<UnidadeMedidaDataUploadDTO> getDTOList() {

        List<UnidadeMedida> unidadeMedidaList =
                unidadeMedidaRepository.findAll();
        return unidadeMedidaDataUploadAutoMapper.converteColecaoEntidadesParaListaDTO(unidadeMedidaList);

    }

    /**
     * Gera o arquivo de exportacao da integracao de unidades de medida.
     */
    public List<List<Object>> getFile() {

        List<UnidadeMedida> unidadeMedidaList =
                unidadeMedidaRepository.findAll();
        return unidadeMedidaDataUploadAutoMapper.converteColecaoEntidadesParaArquivo(unidadeMedidaList);

    }

    /**
     * Converte todo o arquivo para uma lista de DTOs que serao salvos.
     *
     * <p>Usado somente para carga manual de arquivos. Integracoes JSON devem
     * chamar {@link #saveDTOList(List)} diretamente.</p>
     */
    public String saveFile(ProcessedFile processedFile) throws DataUploadException {

        // converte as linhas do processedFile para uma lista de DTOs
        // ignora linhas do cabeçalho
        return saveDTOList(
                processedFile.getDTOListFromProcessedFile(
                        UnidadeMedidaDataUploadDTO.getNumeroLinhasCabecalho(),
                        UnidadeMedidaDataUploadDTO.getNumeroColunas(),
                        linhaArquivo -> unidadeMedidaDataUploadAutoMapper.converteLinhaArquivoParaDTO(linhaArquivo.getRowAsStringListWithEmptyFieldsAsEmptyStrings())));

    }

    /**
     * Persiste um snapshot de unidades de medida enviado pela integracao.
     *
     * <p>O metodo separa salvamentos e remocoes em colecoes para usar batch de
     * repository, evitando save/delete item a item.</p>
     */
    @Transactional
    public String saveDTOList(List<UnidadeMedidaDataUploadDTO> dtoList) throws DataUploadException {

        validaDtoList(dtoList);

        Map<String, UnidadeMedida> entidadesExistentesPorId =
                getUnidadeMedidaMapObrigatorio(unidadeMedidaRepository.findAll());

        List<UnidadeMedida> entidadesASalvar = new ArrayList<>();
        List<UnidadeMedida> entidadesARemover = new ArrayList<>();
        for (UnidadeMedidaDataUploadDTO dto : dtoList) {

            // busca unidade medida (entidade) existente ou cria nova unidade medida
            UnidadeMedida unidadeMedida = Optional.ofNullable(entidadesExistentesPorId.get(dto.id))
                    .orElse(new UnidadeMedida(dto.id));

            unidadeMedida.setDescricao(dto.description);

            // verifica se coluna 'delete' está preenchida e , caso afirmativo ,
            // procede com a tentativa remoção da entidade. caso contrário, segue com
            // tentativa de salvar a entidade
            if (dto.delete != null && dto.delete.equalsIgnoreCase("D")) {
                entidadesARemover.add(unidadeMedida);
            } else {
                entidadesASalvar.add(unidadeMedida);
            }

        }

        try {
            IntegrationPersistenceValidation.validaSavedEntityCollection(
                    unidadeMedidaRepository.saveAll(entidadesASalvar),
                    "Unit of Measure saved collection",
                    entidadesASalvar.size());
            unidadeMedidaRepository.flush();
        } catch (DataAccessException dataAccessException) {
            throw new DataUploadException(
                    "Error saving Units of Measure : " + dataAccessException.toString(),
                    dataAccessException);
        }

        try {
            unidadeMedidaRepository.deleteAll(entidadesARemover);
            unidadeMedidaRepository.flush();
        } catch (DataAccessException dataAccessException) {
            throw new DataUploadException(
                    "Error removing Units of Measure : " + dataAccessException.toString(),
                    dataAccessException);
        }

        return "Units of Measure uploaded";

    }

    /**
     * Valida a lista de DTOs recebida antes de qualquer consulta/persistencia.
     *
     * <p>Lista vazia continua um no-op valido. Lista nula, item nulo, id
     * ausente ou id duplicado indicam erro da chamada de integracao e devem
     * falhar antes de tocar o banco. A duplicidade precisa ser detectada aqui,
     * porque o loop de persistencia indexa entidades existentes por id e
     * poderia transformar duas linhas conflitantes em apenas uma atualizacao
     * final.</p>
     */
    private void validaDtoList(List<UnidadeMedidaDataUploadDTO> dtoList) {

        if (dtoList == null) {
            throw new DataUploadException("Unit of Measure DTO list is required.");
        }

        Set<String> idsUnidadeMedida = new HashSet<>();
        for (int index = 0;
             index < dtoList.size();
             index++) {
            UnidadeMedidaDataUploadDTO unidadeMedidaDataUploadDTO = dtoList.get(index);
            int lineNumber = index + 1;

            if (unidadeMedidaDataUploadDTO == null) {
                throw new DataUploadException("Unit of Measure DTO is null at line " + lineNumber);
            }
            if (unidadeMedidaDataUploadDTO.id == null
                    || unidadeMedidaDataUploadDTO.id.isBlank()) {
                throw new DataUploadException("ID is empty at line " + lineNumber);
            }
            if (!idsUnidadeMedida.add(unidadeMedidaDataUploadDTO.id)) {
                throw new DataUploadException(
                        "Unit of Measure DTO has duplicated id "
                                + unidadeMedidaDataUploadDTO.id
                                + " at line "
                                + lineNumber
                                + ".");
            }
        }

    }

    /**
     * Valida o snapshot de UOM retornado pelo repository.
     *
     * <p>Esse service e base para todos os outros uploads que dependem de
     * conversao fisica. Snapshot nulo, item nulo ou id ausente deve falhar com
     * mensagem funcional antes do mapper ou do lookup por id.</p>
     */
    

    /**
     * Indexa o snapshot persistido por id para evitar busca linear por DTO.
     *
     * <p>Ids duplicados no snapshot indicam quebra estrutural do repository ou
     * cache e devem falhar antes de salvar uma entidade ambigua.</p>
     */
    private Map<String, UnidadeMedida> getUnidadeMedidaMapObrigatorio(
            List<UnidadeMedida> unidadeMedidaList) {

        List<UnidadeMedida> unidadeMedidaListValidada =
                unidadeMedidaList;
        Map<String, UnidadeMedida> unidadeMedidaPorId = new LinkedHashMap<>();

        for (UnidadeMedida unidadeMedida : unidadeMedidaListValidada) {
            if (unidadeMedidaPorId.put(unidadeMedida.getId(), unidadeMedida) != null) {
                throw new DataUploadException("Unit of Measure snapshot returned duplicated id " + unidadeMedida.getId() + ".");
            }
        }

        return unidadeMedidaPorId;

    }

}
