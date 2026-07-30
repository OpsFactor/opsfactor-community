package com.opsfactor.community.capability.configuration.user.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Preferencias visuais da interface grafica para o usuario autenticado.
 *
 * <p>O DTO separa o tema efetivamente escolhido dos modos que a SPA pode
 * oferecer. A persistencia continua reduzida a uma configuracao chave/valor
 * em {@code ConfiguracaoUsuario}; nenhum estado de interface vira entidade
 * propria no Community.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserInterfacePreferencesDTO {

    /**
     * Tema visual efetivo da SPA. Os modos Community previstos sao
     * {@code dark} e {@code light}.
     */
    public String themeMode;

    /**
     * Modos visuais que a plataforma permite selecionar para o usuario.
     */
    public List<String> availableThemeModes;

}
