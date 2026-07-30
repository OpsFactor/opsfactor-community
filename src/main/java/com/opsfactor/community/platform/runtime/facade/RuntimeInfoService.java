package com.opsfactor.community.platform.runtime.facade;

import com.opsfactor.community.platform.runtime.facade.dto.RuntimeInfoDTO;

/**
 * SPI simples para metadados de runtime da edicao OpsFactor em execucao.
 *
 * <p>O Community fornece a implementacao padrao. O Enterprise deve registrar um
 * bean `@Primary` implementando esta interface para que o mesmo endpoint REST
 * passe a responder como Enterprise sem depender de properties externas.</p>
 */
public interface RuntimeInfoService {

    /**
     * Retorna os metadados de edicao e listas de funcionalidades publicadas ao
     * front compartilhado.
     *
     * <p>Este metodo nao substitui validacoes de backend. Ele apenas permite
     * que a UI marque, oculte ou bloqueie opcoes antes do submit, mantendo a
     * separacao Community/Enterprise implicita nos beans disponiveis.</p>
     */
    RuntimeInfoDTO getRuntimeInfo();

}
