package com.opsfactor.community.web.dto.controller;

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
        
    public static ResponseEntity<ResponseDTO> getResponseEntity(String message, HttpStatus status) {
        
        return ResponseEntity
                .status(status)
                .body(new ResponseDTO(message, status.value()));
        
    }
    
    public static ResponseEntity<?> getResponseEntity(Object body, HttpStatus status) {
        
        if (body instanceof String) return getResponseEntity ((String) body, status);
        
        return ResponseEntity
                .status(status)
                .body(body);
        
    }

}
