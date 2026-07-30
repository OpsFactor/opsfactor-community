package com.opsfactor.community.capability.configuration.user.domain;

import java.io.Serializable;
import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;

/**
 * Preferencia simples de usuario persistida como chave valor.
 *
 * <p>A seguranca Community e deliberadamente basica e trabalha apenas com
 * usuario/senha e papel administrativo. Esta entidade guarda configuracoes de
 * interface e operacao por usuario, sem modelar SSO, permissoes avancadas ou
 * funcionalidades de desbloqueio Enterprise.</p>
 */
@Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
@EqualsAndHashCode(of="configuracaoUsuarioCompositeKey")
@NoArgsConstructor 
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
public class ConfiguracaoUsuario {
    
    @EmbeddedId
    @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
    private ConfiguracaoUsuarioCompositeKey configuracaoUsuarioCompositeKey;

    @Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
    @NoArgsConstructor
    @RequiredArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class ConfiguracaoUsuarioCompositeKey implements Serializable {

        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private String userId;
        
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private String tema;
        
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private String parametro;

    }
    
    private String valorParametro;
    
    public String getUserId() {
        return getConfiguracaoUsuarioCompositeKey().getUserId();
    }
    
    public String getTema() {
        return getConfiguracaoUsuarioCompositeKey().getTema();
    }
    
    public String getParametro() {
        return getConfiguracaoUsuarioCompositeKey().getParametro();
    }
        
}
