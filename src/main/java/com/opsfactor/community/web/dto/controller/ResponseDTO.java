package com.opsfactor.community.web.dto.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Permite a criação de response entities passando apenas uma mensagem de erro mas 
 * retornando um JSON com o conteúdo da mensagem + status http
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class ResponseDTO {
    
    private String message;
    private int status;

    /**
     * Resultado operacional explícito quando a resposta se refere a uma task.
     * Outros endpoints preservam o contrato histórico e não serializam este
     * campo quando ele não se aplica.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ProcessExecutionOutcome processExecutionOutcome;

    /** Preserva o construtor binário usado pelo contrato histórico. */
    public ResponseDTO(String message, int status) {

        this(message, status, null);

    }
        
    public static ResponseEntity<ResponseDTO> getResponseEntity(String message, HttpStatus status) {
        
        return ResponseEntity
                .status(status)
                .body(new ResponseDTO(message, status.value()));
        
    }

    /**
     * Cria a resposta de uma task com a semântica que o front precisa para
     * apresentar conclusão ou aceite de processamento em background.
     */
    public static ResponseEntity<ResponseDTO> getProcessExecutionResponseEntity(
            String message,
            HttpStatus status,
            ProcessExecutionOutcome processExecutionOutcome) {

        return ResponseEntity
                .status(status)
                .body(new ResponseDTO(message, status.value(), processExecutionOutcome));

    }
    
    public static ResponseEntity<?> getResponseEntity(Object body, HttpStatus status) {
        
        if (body instanceof String) return getResponseEntity ((String) body, status);
        
        return ResponseEntity
                .status(status)
                .body(body);
        
    }

}
