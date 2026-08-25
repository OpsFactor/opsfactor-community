package com.opsfactor.community.platform.scheduler.facade;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.opsfactor.community.web.dto.controller.ResponseDTO;
import com.opsfactor.community.web.dto.controller.ProcessExecutionOutcome;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes.ModoExecucaoProcesso;
import com.opsfactor.community.platform.scheduler.services.Task;
import com.opsfactor.community.platform.security.login.AuthenticationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Fachada web para execucoes imediatas registradas pelo {@link TaskSchedulingService}.
 *
 * <p>O OpsFactor Community usa este componente somente para rodar tarefas na
 * mesma thread da request, enriquecendo a chamada com timezone de
 * {@link ParametrosGlobaisService}, usuario do {@link AuthenticationService} e
 * retorno padronizado em {@link ResponseDTO}. Modos assíncronos, batch e filas
 * voltam no OpsFactor Enterprise por implementacoes proprias.</p>
 */
@Slf4j
@Component
public class WebControllerTaskSchedulingService {

    /**
     * Seleciona o modo de execucao para os endpoints compartilhados de
     * planejamento.
     *
     * <p>O Community fixa o contrato em {@link ModoExecucaoProcesso#SYNC},
     * independentemente de properties de infraestrutura. O overlay Enterprise
     * substitui somente este ponto para ler a property legada e continuar
     * reutilizando o mesmo controller REST.</p>
     */
    public ModoExecucaoProcesso getPlanningProcessExecutionMode() {

        return ModoExecucaoProcesso.SYNC;

    }

    /**
     * Service Community que materializa o historico tecnico e executa tarefas
     * imediatas na thread da request.
     */
    @Autowired
    private TaskSchedulingService taskSchedulingService;

    /**
     * Parametros globais usados para registrar o timezone operacional da
     * execucao. O scheduler Community nao recebe timezone por variavel externa.
     */
    @Autowired
    private ParametrosGlobaisService parametrosGlobaisService;

    /**
     * Porta central de seguranca Community para identificar o usuario da
     * request. Mantemos o acesso ao contexto de seguranca encapsulado no package
     * `com.opsfactor.community.platform.security`, onde as validacoes de autenticacao estao
     * documentadas e testadas.
     */
    @Autowired
    private AuthenticationService authenticationService;

    /**
     * Executa uma {@link Task} imediatamente e devolve resposta HTTP
     * padronizada ao controller chamador.
     *
     * <p>O modo e validado antes de acessar usuario, timezone ou scheduler para
     * que payloads assíncronos falhem como Enterprise sem efeito colateral.</p>
     */
    public <A,S,T extends Task<A,S>> ResponseEntity<ResponseDTO> runImediato(
            Class<T> taskClass, A dtoParametros, String tipoProcesso, String descricaoExecucao,
            ModoExecucaoProcesso modoExecucaoProcesso) {
        
        validaModoExecucaoProcessoCommunity(modoExecucaoProcesso);
        ParametrosGlobais parametrosGlobais = parametrosGlobaisService.getParametrosGlobais();
        
        String username = authenticationService.getAuthenticatedUserId();
        String timeZone = parametrosGlobais.getTimeZone();
        
        try {
            /*
             * Community executa a Task imediatamente. Qualquer excecao precisa
             * voltar nesta propria response, porque nao ha fila ou worker
             * posterior que possa consolidar o erro para o usuario.
             */
            taskSchedulingService.criaSalvaEExecutaScheduledTaskImediatoComTask(
                    taskClass, tipoProcesso, username, descricaoExecucao, timeZone, modoExecucaoProcesso, dtoParametros);
        } catch (ReflectiveOperationException | JsonProcessingException | RuntimeException e) {
            log.error("Error executing immediate Community task {}", tipoProcesso, e);
            return ResponseDTO.getResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        return getRespostaExecucaoProcesso(tipoProcesso, modoExecucaoProcesso);
        
    }

    /**
     * Executa uma lista de tasks imediatas no mesmo request.
     *
     * <p>O Community nao faz fan-out para fila. A iteracao e propositalmente
     * sequencial: a primeira falha interrompe a lista e devolve erro ao front,
     * evitando a falsa percepcao de sucesso parcial em processamento batch.</p>
     */
    public <A,S,T extends Task<A,S>> ResponseEntity<ResponseDTO> runImediato(
            Class<T> taskClass, List<A> dtoParametrosList, String tipoProcesso,
            Function<A,String> funcaoExtratoraDescricaoExecucao,
            ModoExecucaoProcesso modoExecucaoProcesso) {

        validaModoExecucaoProcessoCommunity(modoExecucaoProcesso);
        validaListaExecucaoImediataCommunity(dtoParametrosList);

        for (A dtoParametros : dtoParametrosList) {
            /*
             * Community roda a lista de execucoes no mesmo request. Se uma
             * execucao falhar, a resposta deve refletir imediatamente essa
             * falha em vez de devolver sucesso para uma lista parcialmente
             * processada.
             */
            ResponseEntity<ResponseDTO> responseEntity = runImediato(
                    taskClass, dtoParametros, tipoProcesso,
                    funcaoExtratoraDescricaoExecucao.apply(dtoParametros),
                    modoExecucaoProcesso);
            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                return responseEntity;
            }
        }

        return getRespostaExecucaoProcesso(tipoProcesso, modoExecucaoProcesso);

    }

    /**
     * Mantem o texto de resposta observado pelo front legado conforme o modo
     * efetivo de execucao. O Community somente chega ao ramo {@code SYNC}; o
     * overlay Enterprise pode reutilizar a mesma fachada para {@code ASYNC} e
     * {@code BATCH}, que ja foram apenas agendados no retorno HTTP.
     */
    protected ResponseEntity<ResponseDTO> getRespostaExecucaoProcesso(
            String tipoProcesso,
            ModoExecucaoProcesso modoExecucaoProcesso) {

        if (ModoExecucaoProcesso.SYNC.equals(modoExecucaoProcesso)) {
            return ResponseDTO.getProcessExecutionResponseEntity(
                    tipoProcesso + " executed successfully",
                    HttpStatus.OK,
                    ProcessExecutionOutcome.COMPLETED);
        }

        return ResponseDTO.getProcessExecutionResponseEntity(
                tipoProcesso
                        + " executing in background. The process status can be followed in Processes -> Process Status",
                HttpStatus.OK,
                ProcessExecutionOutcome.ACCEPTED_FOR_BACKGROUND_PROCESSING);

    }

    /**
     * Valida a lista de payloads antes de criar qualquer historico de task.
     *
     * <p>No Community, execucoes em lista ainda acontecem item a item dentro da
     * mesma request. Lista vazia ou item nulo nao representa uma task
     * funcional; aceitar isso geraria sucesso sem trabalho ou erro tecnico na
     * lambda de descricao do controller.</p>
     */
    private <A> void validaListaExecucaoImediataCommunity(List<A> dtoParametrosList) {

        if (dtoParametrosList == null) {
            throw new IllegalArgumentException(
                    "Immediate Community task payload list is required");
        }

        if (dtoParametrosList.isEmpty()) {
            throw new IllegalArgumentException("Immediate Community task payload list cannot be empty");
        }

        for (int indiceDTOParametros = 0; indiceDTOParametros < dtoParametrosList.size(); indiceDTOParametros++) {
            if (dtoParametrosList.get(indiceDTOParametros) == null) {
                throw new IllegalArgumentException(
                        "Immediate Community task payload list cannot contain null value at index "
                                + indiceDTOParametros);
            }
        }

    }

    /**
     * Executa uma rotina leve representada por {@link Supplier} e registra o
     * status como tarefa imediata.
     *
     * <p>Este caminho cobre acoes de controller que precisam de historico na
     * tela de Process Status, mas nao justificam uma classe {@link Task}
     * dedicada.</p>
     */
    public ResponseEntity<ResponseDTO> runImediatoSync(
            Supplier<String> supplierExecucaoIntegracao, String nomeMetodoIntegracao) {

        try {
            String username = authenticationService.getAuthenticatedUserId();
            String timeZone = parametrosGlobaisService.getParametrosGlobais().getTimeZone();
            String mensagemOutput = taskSchedulingService.criaSalvaEExecutaScheduledTaskImediatoSincronoComSupplier(
                    supplierExecucaoIntegracao, nomeMetodoIntegracao, username, null, timeZone);
            return ResponseDTO.getProcessExecutionResponseEntity(
                    mensagemOutput,
                    HttpStatus.OK,
                    ProcessExecutionOutcome.COMPLETED);
        } catch (RuntimeException e) {
            // Supplier sync nao declara checked exception; qualquer falha aqui
            // ja foi convertida pelo scheduler em historico de Process Status.
            log.error("Error executing immediate Community integration {}", nomeMetodoIntegracao, e);
            return ResponseDTO.getResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    /**
     * Community executa tarefas imediatamente no mesmo fluxo de request.
     * Background processing, filas e batch runners pertencem ao OpsFactor Enterprise.
     */
    protected void validaModoExecucaoProcessoCommunity(ModoExecucaoProcesso modoExecucaoProcesso) {

        if (modoExecucaoProcesso == null) {
            throw new IllegalArgumentException("Modo de execucao do processo nao pode ser nulo");
        }

        /*
         * Depois de validada a presenca do parametro, qualquer modo diferente de
         * SYNC deixa de ser problema de payload e passa a representar um recurso
         * Enterprise ainda ausente do runtime Community.
         */
        if (!ModoExecucaoProcesso.SYNC.equals(modoExecucaoProcesso)) {
            throw new RequiresEnterpriseVersionException("Asynchronous or batch process execution");
        }

    }

}
