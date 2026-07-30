package com.opsfactor.community.web.restcontroller.runtime;

import com.opsfactor.community.platform.runtime.facade.dto.RuntimeInfoDTO;
import com.opsfactor.community.platform.runtime.facade.RuntimeInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint aberto para os front-ends Community/Enterprise descobrirem a edicao
 * em execucao antes do login.
 *
 * <p>O Community responde `edition = community`. Quando o overlay Enterprise
 * estiver no classpath, a implementacao `@Primary` de `RuntimeInfoService`
 * troca somente os metadados, mantendo este controller e a URL estaveis.</p>
 *
 * <p>O front compartilhado deve chamar este endpoint uma vez no bootstrap ou
 * logo apos definir o backend alvo, guardar o resultado em store global e
 * reutiliza-lo nas paginas da SPA. O endpoint nao depende de usuario, tenant ou
 * cadastro funcional, justamente para evitar chamadas repetidas por tela e
 * regras divergentes no front.</p>
 */
@RestController
public class RuntimeInfoController {

    /**
     * SPI de runtime info. A injecao explicita deixa claro que a diferenca
     * Community/Enterprise vem do bean disponivel no contexto Spring, nao de
     * comparacao de enum ou variavel de ambiente.
     */
    @Autowired
    private RuntimeInfoService runtimeInfoService;

    /**
     * Endpoint aberto chamado pelo front antes do login para decidir o
     * logotipo/branding, listas de opcoes habilitadas e marcacoes visuais de
     * recursos Enterprise.
     *
     * <p>A resposta e estavel para a aplicacao em execucao; refreshes manuais
     * so fazem sentido em hard reload, troca de ambiente/backend ou novo login.</p>
     */
    @GetMapping("api/open/runtime-info")
    public RuntimeInfoDTO getRuntimeInfo() {

        return runtimeInfoService.getRuntimeInfo();

    }

}
