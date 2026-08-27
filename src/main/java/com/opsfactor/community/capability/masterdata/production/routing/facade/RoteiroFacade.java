package com.opsfactor.community.capability.masterdata.production.routing.facade;

import com.opsfactor.community.capability.masterdata.production.operation.domain.OperacaoRoteiro;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.production.operation.repository.OperacaoRoteiroRepository;
import com.opsfactor.community.capability.masterdata.production.routing.repository.RoteiroRepository;
import com.opsfactor.community.capability.masterdata.production.routing.facade.dto.RoteiroDTO;
import com.opsfactor.community.capability.masterdata.production.operation.facade.dto.OperacaoRoteiroDTO;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.facade.dto.InconsistenciaReceitaProducaoDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.opsfactor.community.capability.masterdata.production.routing.facade.mapper.RoteiroAutoMapper;
import com.opsfactor.community.capability.masterdata.production.operation.facade.mapper.OperacaoRoteiroAutoMapper;

/**
 * Service front de roteiros e operacoes produtivas Community.
 *
 * <p>O Community usa estes dados apenas como master data operacional do
 * heuristico. Setup detalhado, manutencao, turnos e line scheduling pertencem
 * ao Enterprise e nao sao tratados nesta service.</p>
 */
@Service
public class RoteiroFacade {

    /**
     * Repository dos roteiros produtivos operacionais.
     */
    @Autowired
    private RoteiroRepository roteiroRepository;

    /**
     * Repository das operacoes de roteiro. Operacoes seguem no Community como
     * sequencia produtiva simples, sem line scheduling.
     */
    @Autowired
    private OperacaoRoteiroRepository operacaoRoteiroRepository;

    /**
     * Mapper de roteiro para DTO da tela.
     */
    @Autowired
    private RoteiroAutoMapper roteiroAutoMapper;

    /**
     * Mapper de operacao de roteiro para DTO da tela.
     */
    @Autowired
    private OperacaoRoteiroAutoMapper operacaoRoteiroAutoMapper;

    /**
     * Lista roteiros produtivos no contrato Community.
     *
     * <p>Roteiro e master data estrutural para o heuristico. A listagem deve
     * falhar cedo quando o snapshot nao traz id, material output ou location,
     * evitando DTOs parciais e erros tardios nas projections produtivas.</p>
     */
    public List<RoteiroDTO> getRoteiroDTOList() {

        List<Roteiro> roteiroList = roteiroRepository.customFindAllForFront();
        validaRoteiroListCarregadaCommunity(roteiroList);
        
        List<RoteiroDTO> roteiroDTOList =
                roteiroAutoMapper.converteListaEntidadeParaListaDTO(roteiroList);
        validaRoteiroDTOListCarregadaCommunity(roteiroDTOList);

        return roteiroDTOList;
        
    }

    /**
     * Lista operacoes de roteiro no contrato Community.
     *
     * <p>Operacoes precisam de chave composta, roteiro e recurso produtivo
     * antes do mapper. Esses campos sao o minimo para a SPA mostrar o cadastro
     * e para o Supply Planning materializar consumo de capacidade.</p>
     */
    public List<OperacaoRoteiroDTO> getOperacaoRoteiroDTOList() {

        List<OperacaoRoteiro> operacaoRoteiroList = operacaoRoteiroRepository.customFindAllForFront();
        validaOperacaoRoteiroListCarregadaCommunity(operacaoRoteiroList);
        
        List<OperacaoRoteiroDTO> operacaoRoteiroDTOList =
                operacaoRoteiroAutoMapper.converteListaEntidadeParaListaDTO(operacaoRoteiroList);
        validaOperacaoRoteiroDTOListCarregadaCommunity(operacaoRoteiroDTOList);

        return operacaoRoteiroDTOList;
        
    }
    
    /**
     * Executa checagens legadas de consistencia de roteiros produtivos.
     *
     * <p>O metodo permanece deprecated porque a validacao principal deve ficar
     * cada vez mais perto das entidades de roteiro/lista tecnica. Enquanto a
     * API operacional ainda expuser a lista de inconsistencias, mantemos a
     * regra explicita aqui.</p>
     *
     * @return inconsistencias encontradas no cadastro produtivo.
     */
    @Deprecated
    public List<InconsistenciaReceitaProducaoDTO> getInconsistenciaReceitaProducaoDTOList() {
        
        List<InconsistenciaReceitaProducaoDTO> listaInconsistencias = new ArrayList<>();
        List<Roteiro> roteiroList = roteiroRepository.customFindAllForConsistencyDiagnostic();
        validaRoteiroListParaInconsistenciaCommunity(roteiroList);
        
        for (Roteiro roteiro : roteiroList) {
            validaOperacoesRoteiroParaInconsistenciaCommunity(roteiro);

            if(roteiro.getOperacaoRoteiroSet().size() == 0) {
                InconsistenciaReceitaProducaoDTO inconsistencia = InconsistenciaReceitaProducaoDTO.builder()
                        .productionRoutingId(roteiro.getId())
                        .productionRoutingOutputMaterial(roteiro.getMaterialOutput().getId())
                        .inconsistency("No operations registered for routing")
                        .build();
                listaInconsistencias.add(inconsistencia);
            } else {
                OperacaoRoteiro ultimaOperacao = roteiro.getOperacaoRoteiroListOrdenadaPorPosicaoAsc().get(
                        roteiro.getOperacaoRoteiroListOrdenadaPorPosicaoAsc().size() - 1);
                
                if (roteiro.getMaterialOutput() == null) {
                    InconsistenciaReceitaProducaoDTO inconsistencia = InconsistenciaReceitaProducaoDTO.builder()
                            .productionRoutingId(roteiro.getId())
                            .lastOperationPosition(ultimaOperacao.getPosicao())
                            .inconsistency("No output material registered for routing or bill of materials registered for its last operation")
                            .build();
                    listaInconsistencias.add(inconsistencia);
                }
            }   
        }
        return listaInconsistencias;
    }

    /**
     * Valida o snapshot mínimo do diagnóstico de inconsistências de roteiros.
     *
     * <p>Ao contrário da listagem administrativa, o diagnóstico não depende
     * da location. Mantém-se a exigência histórica do material de saída antes
     * de iniciar a checagem, mas esta separação evita inicializar relações
     * lazy que não participam do diagnóstico.</p>
     */
    private void validaRoteiroListParaInconsistenciaCommunity(List<Roteiro> roteiroList) {

        if (roteiroList == null) {
            throw new IllegalStateException("Production Routing list snapshot is required.");
        }

        for (int index = 0; index < roteiroList.size(); index++) {
            Roteiro roteiro = roteiroList.get(index);

            if (roteiro == null) {
                throw new IllegalStateException(
                        "Production Routing at index " + index + " is required in list snapshot.");
            }
            if (roteiro.getId() == null || roteiro.getId().isBlank()) {
                throw new IllegalStateException(
                        "Production Routing at index " + index + " has no id in list snapshot.");
            }
            if (roteiro.getMaterialOutput() == null
                    || roteiro.getMaterialOutput().getId() == null
                    || roteiro.getMaterialOutput().getId().isBlank()) {
                throw new IllegalStateException(
                        "Production Routing at index " + index + " has no output material in list snapshot.");
            }
        }

    }

    /**
     * Valida o conjunto interno de operacoes usado pelo diagnostico legado.
     *
     * <p>A listagem normal de operacoes valida snapshots vindos diretamente do
     * repository de `OperacaoRoteiro`. Este diagnostico, entretanto, usa o set
     * carregado dentro do proprio roteiro. Antes de ordenar e pegar a ultima
     * operacao, precisamos garantir que esse set nao esta nulo e que suas
     * operacoes possuem posicao funcional.</p>
     */
    private void validaOperacoesRoteiroParaInconsistenciaCommunity(Roteiro roteiro) {

        Set<OperacaoRoteiro> operacaoRoteiroSet = roteiro.getOperacaoRoteiroSet();
        if (operacaoRoteiroSet == null) {
            throw new IllegalStateException(
                    "Production Routing "
                            + roteiro.getId()
                            + " operation set is required for consistency diagnostic.");
        }

        int index = 0;
        for (OperacaoRoteiro operacaoRoteiro : operacaoRoteiroSet) {
            if (operacaoRoteiro == null) {
                throw new IllegalStateException(
                        "Production Routing "
                                + roteiro.getId()
                                + " operation "
                                + index
                                + " is required for consistency diagnostic.");
            }
            if (operacaoRoteiro.getOperacaoRoteiroCompositeKey() == null
                    || operacaoRoteiro.getOperacaoRoteiroCompositeKey().getPosicao() == null) {
                throw new IllegalStateException(
                        "Production Routing "
                                + roteiro.getId()
                                + " operation "
                                + index
                                + " position is required for consistency diagnostic.");
            }
            index++;
        }

    }

    /**
     * Valida o snapshot de roteiros carregado para listagem/consistencia.
     *
     * <p>Lista vazia e valida. Cada roteiro carregado, entretanto, precisa
     * trazer id, location e material output, que sao as dimensoes funcionais
     * usadas tanto pelo DTO quanto pelas projections do Supply Planning.</p>
     */
    private void validaRoteiroListCarregadaCommunity(List<Roteiro> roteiroList) {

        if (roteiroList == null) {
            throw new IllegalStateException("Production Routing list snapshot is required.");
        }

        for (int index = 0; index < roteiroList.size(); index++) {
            Roteiro roteiro = roteiroList.get(index);

            if (roteiro == null) {
                throw new IllegalStateException(
                        "Production Routing at index " + index + " is required in list snapshot.");
            }
            if (roteiro.getId() == null || roteiro.getId().isBlank()) {
                throw new IllegalStateException(
                        "Production Routing at index " + index + " has no id in list snapshot.");
            }
            if (roteiro.getLocation() == null
                    || roteiro.getLocation().getId() == null
                    || roteiro.getLocation().getId().isBlank()) {
                throw new IllegalStateException(
                        "Production Routing at index " + index + " has no location in list snapshot.");
            }
            if (roteiro.getMaterialOutput() == null
                    || roteiro.getMaterialOutput().getId() == null
                    || roteiro.getMaterialOutput().getId().isBlank()) {
                throw new IllegalStateException(
                        "Production Routing at index " + index + " has no output material in list snapshot.");
            }
        }

    }

    /**
     * Valida o snapshot de operacoes de roteiro carregado para listagem.
     *
     * <p>As quantidades e tempos possuem defaults no dominio. A chave da
     * operacao, o roteiro e o recurso produtivo nao possuem default funcional:
     * se qualquer um estiver ausente, o cadastro nao pode alimentar o front nem
     * as projections produtivas Community.</p>
     */
    private void validaOperacaoRoteiroListCarregadaCommunity(
            List<OperacaoRoteiro> operacaoRoteiroList) {

        if (operacaoRoteiroList == null) {
            throw new IllegalStateException("Production Routing Operation list snapshot is required.");
        }

        for (int index = 0; index < operacaoRoteiroList.size(); index++) {
            OperacaoRoteiro operacaoRoteiro = operacaoRoteiroList.get(index);

            if (operacaoRoteiro == null) {
                throw new IllegalStateException(
                        "Production Routing Operation at index " + index + " is required in list snapshot.");
            }
            if (operacaoRoteiro.getOperacaoRoteiroCompositeKey() == null
                    || operacaoRoteiro.getOperacaoRoteiroCompositeKey().getPosicao() == null) {
                throw new IllegalStateException(
                        "Production Routing Operation at index " + index + " has no position in list snapshot.");
            }
            if (operacaoRoteiro.getRoteiro() == null
                    || operacaoRoteiro.getRoteiro().getId() == null
                    || operacaoRoteiro.getRoteiro().getId().isBlank()) {
                throw new IllegalStateException(
                        "Production Routing Operation at index " + index + " has no routing in list snapshot.");
            }
            if (operacaoRoteiro.getRecursoProdutivo() == null
                    || operacaoRoteiro.getRecursoProdutivo().getId() == null
                    || operacaoRoteiro.getRecursoProdutivo().getId().isBlank()) {
                throw new IllegalStateException(
                        "Production Routing Operation at index " + index + " has no production resource in list snapshot.");
            }
        }

    }

    /**
     * Valida a fotografia DTO de roteiros produtivos devolvida pelo mapper.
     *
     * <p>A entidade carregada ja foi validada, mas a SPA passa a depender do
     * DTO para editar e exibir o cadastro. Lista nula, item nulo ou chave
     * estrutural perdida pelo mapper devem falhar antes de retornar resposta
     * parcial.</p>
     */
    private void validaRoteiroDTOListCarregadaCommunity(List<RoteiroDTO> roteiroDTOList) {

        if (roteiroDTOList == null) {
            throw new IllegalStateException("Production Routing DTO list snapshot is required.");
        }

        for (int index = 0; index < roteiroDTOList.size(); index++) {
            RoteiroDTO roteiroDTO = roteiroDTOList.get(index);

            if (roteiroDTO == null) {
                throw new IllegalStateException(
                        "Production Routing DTO at index " + index + " is required in list snapshot.");
            }
            if (isBlank(roteiroDTO.getId())) {
                throw new IllegalStateException(
                        "Production Routing DTO at index " + index + " has no id in list snapshot.");
            }
            if (isBlank(roteiroDTO.getLocationId())) {
                throw new IllegalStateException(
                        "Production Routing DTO at index " + index + " has no location in list snapshot.");
            }
            if (isBlank(roteiroDTO.getOutputMaterialId())) {
                throw new IllegalStateException(
                        "Production Routing DTO at index " + index + " has no output material in list snapshot.");
            }
        }

    }

    /**
     * Valida a fotografia DTO de operacoes produtivas devolvida pelo mapper.
     */
    private void validaOperacaoRoteiroDTOListCarregadaCommunity(
            List<OperacaoRoteiroDTO> operacaoRoteiroDTOList) {

        if (operacaoRoteiroDTOList == null) {
            throw new IllegalStateException("Production Routing Operation DTO list snapshot is required.");
        }

        for (int index = 0; index < operacaoRoteiroDTOList.size(); index++) {
            OperacaoRoteiroDTO operacaoRoteiroDTO = operacaoRoteiroDTOList.get(index);

            if (operacaoRoteiroDTO == null) {
                throw new IllegalStateException(
                        "Production Routing Operation DTO at index " + index + " is required in list snapshot.");
            }
            if (isBlank(operacaoRoteiroDTO.getRoutingId())) {
                throw new IllegalStateException(
                        "Production Routing Operation DTO at index " + index + " has no routing in list snapshot.");
            }
            if (operacaoRoteiroDTO.getOperationPosition() == null) {
                throw new IllegalStateException(
                        "Production Routing Operation DTO at index " + index + " has no position in list snapshot.");
            }
            if (isBlank(operacaoRoteiroDTO.getProductionResourceId())) {
                throw new IllegalStateException(
                        "Production Routing Operation DTO at index " + index + " has no production resource in list snapshot.");
            }
        }

    }

    private boolean isBlank(String value) {

        return value == null || value.isBlank();

    }
    
}
