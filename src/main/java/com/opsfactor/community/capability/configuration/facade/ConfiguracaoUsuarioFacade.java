package com.opsfactor.community.capability.configuration.facade;

import com.opsfactor.community.capability.configuration.user.facade.dto.ConfiguracaoUsuarioDTO;
import com.opsfactor.community.capability.configuration.user.facade.mapper.ConfiguracaoUsuarioAutoMapper;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguracaoUsuario;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.opsfactor.community.capability.configuration.user.repository.ConfiguracaoUsuarioRepository;

/**
 * Service de preferencias simples de usuario consumidas pelo front Community.
 *
 * <p>O contrato armazena configuracoes chave/valor por usuario e tema. Ele nao
 * representa politica de seguranca, tenant, SSO ou permissoes granulares.</p>
 */
@Service
public class ConfiguracaoUsuarioFacade {

    /**
     * Repository de configuracoes simples por usuario. O @Autowired fica
     * explicito para separar beans Spring de estado local.
     */
    @Autowired
    private ConfiguracaoUsuarioRepository configuracaoUsuarioRepository;

    /**
     * Mapper MapStruct responsavel pela conversao entidade/DTO.
     */
    @Autowired
    private ConfiguracaoUsuarioAutoMapper configuracaoUsuarioAutoMapper;

    /**
     * Lista preferencias simples de um tema para o usuario informado.
     *
     * <p>O Community nao consulta permissoes nem tenant nesta borda; portanto
     * usuario e tema precisam chegar explicitos para que a query JPA seja
     * sempre uma leitura de chave funcional, nao uma busca acidental com
     * parametro nulo.</p>
     */
    public List<ConfiguracaoUsuarioDTO> getConfiguredViewDTOList(String userId, String tema) {

        validaChaveConsultaConfiguracaoUsuarioCommunity(userId, tema);
        List<ConfiguracaoUsuario> configuracaoUsuarioList =
                configuracaoUsuarioRepository.findByConfiguracaoUsuarioCompositeKeyUserIdAndConfiguracaoUsuarioCompositeKeyTema(
                        userId,
                        tema);
        validaConfiguracaoUsuarioListCarregadaCommunity(
                configuracaoUsuarioList,
                userId,
                tema);

        List<ConfiguracaoUsuarioDTO> configuracaoUsuarioDTOList =
                configuracaoUsuarioAutoMapper.converteListEntidades(configuracaoUsuarioList);
        validaConfiguracaoUsuarioDTOListListagemCommunity(
                configuracaoUsuarioDTOList,
                userId,
                tema);
        return configuracaoUsuarioDTOList;

    }
    
    /**
     * Persiste preferencias simples reenviadas pela SPA.
     *
     * <p>O `userId` do payload e sempre sobrescrito pelo usuario autenticado
     * recebido do controller. A chave funcional de cada item Community e
     * composta por usuario autenticado, escopo/tema e parametro; valor nulo e
     * permitido para representar uma preferencia limpa ou ainda nao definida.</p>
     */
    public void saveConfigurationViewDTOList(String userId, List<ConfiguracaoUsuarioDTO> listaDTOs) {

        validaPayloadConfiguracaoUsuarioCommunity(userId, listaDTOs);

        /*
         * Flush vazio vindo da SPA nao precisa acionar mapper nem repository:
         * nao ha chave a canonizar e nenhuma preferencia a persistir.
         */
        if (listaDTOs.isEmpty()) {
            return;
        }

        for (ConfiguracaoUsuarioDTO configuracao : listaDTOs) {
            configuracao.userId = userId;
        }

        // A UI pode reenviar o mesmo parametro mais de uma vez no mesmo POST durante sincronizacoes
        // proximas entre eventos de coluna. Mantemos apenas a ultima ocorrencia de cada chave
        // composta para persistir uma visao canonica por usuario/tema/parametro.
        Map<String, ConfiguracaoUsuarioDTO> configuracoesCanonicasPorChave = new LinkedHashMap<>();
        for (ConfiguracaoUsuarioDTO configuracaoUsuarioDTO : listaDTOs) {
            configuracoesCanonicasPorChave.put(
                    getChaveCanonicaConfiguracao(configuracaoUsuarioDTO),
                    configuracaoUsuarioDTO);
        }

        List<ConfiguracaoUsuario> configuracoesUsuario =
                configuracaoUsuarioAutoMapper.converteListDTOs(configuracoesCanonicasPorChave.values());
        List<ConfiguracaoUsuario> configuracoesUsuarioSalvas =
                configuracaoUsuarioRepository.saveAll(configuracoesUsuario);
        validaConfiguracoesUsuarioSalvasCommunity(
                configuracoesUsuarioSalvas,
                configuracoesUsuario.size());
    }

    /**
     * Valida a chave da consulta antes do repository.
     */
    private void validaChaveConsultaConfiguracaoUsuarioCommunity(String userId, String tema) {

        if (isBlank(userId)) {
            throw new IllegalArgumentException("User configuration user id is required.");
        }
        if (isBlank(tema)) {
            throw new IllegalArgumentException("User configuration theme is required.");
        }

    }

    /**
     * Valida a fotografia carregada do repository antes do mapper.
     *
     * <p>Lista vazia e valida para usuario/tema sem preferencias salvas. Itens
     * presentes precisam pertencer exatamente ao usuario e tema solicitados,
     * porque esta borda nao aplica permissoes Enterprise nem filtros adicionais
     * depois da query.</p>
     */
    private void validaConfiguracaoUsuarioListCarregadaCommunity(
            List<ConfiguracaoUsuario> configuracaoUsuarioList,
            String userId,
            String tema) {

        if (configuracaoUsuarioList == null) {
            throw new IllegalStateException(
                    "User configuration repository returned null collection for user "
                            + userId
                            + " and theme "
                            + tema
                            + ".");
        }

        for (int indiceConfiguracao = 0; indiceConfiguracao < configuracaoUsuarioList.size(); indiceConfiguracao++) {
            ConfiguracaoUsuario configuracaoUsuario = configuracaoUsuarioList.get(indiceConfiguracao);
            if (configuracaoUsuario == null) {
                throw new IllegalStateException(
                        "User configuration entry at index "
                                + indiceConfiguracao
                                + " is required in repository snapshot.");
            }
            if (configuracaoUsuario.getConfiguracaoUsuarioCompositeKey() == null) {
                throw new IllegalStateException(
                        "User configuration entry at index "
                                + indiceConfiguracao
                                + " must have a primary key in repository snapshot.");
            }
            validaConfiguracaoUsuarioKeyCommunity(
                    configuracaoUsuario.getConfiguracaoUsuarioCompositeKey().getUserId(),
                    configuracaoUsuario.getConfiguracaoUsuarioCompositeKey().getTema(),
                    configuracaoUsuario.getConfiguracaoUsuarioCompositeKey().getParametro(),
                    userId,
                    tema,
                    indiceConfiguracao,
                    "repository snapshot");
        }

    }

    /**
     * Valida a fotografia DTO devolvida pelo mapper antes do retorno para a SPA.
     *
     * <p>`parameterValue` pode ser nulo porque uma preferencia pode representar
     * configuracao limpa. Usuario, tema e parametro, por outro lado, formam a
     * chave funcional e precisam permanecer presentes depois do mapper.</p>
     */
    private void validaConfiguracaoUsuarioDTOListListagemCommunity(
            List<ConfiguracaoUsuarioDTO> configuracaoUsuarioDTOList,
            String userId,
            String tema) {

        if (configuracaoUsuarioDTOList == null) {
            throw new IllegalStateException(
                    "User configuration mapper returned null DTO collection for user "
                            + userId
                            + " and theme "
                            + tema
                            + ".");
        }

        for (int indiceConfiguracao = 0; indiceConfiguracao < configuracaoUsuarioDTOList.size(); indiceConfiguracao++) {
            ConfiguracaoUsuarioDTO configuracaoUsuarioDTO = configuracaoUsuarioDTOList.get(indiceConfiguracao);
            if (configuracaoUsuarioDTO == null) {
                throw new IllegalStateException(
                        "User configuration DTO at index "
                                + indiceConfiguracao
                                + " is required in mapper snapshot.");
            }
            validaConfiguracaoUsuarioKeyCommunity(
                    configuracaoUsuarioDTO.userId,
                    configuracaoUsuarioDTO.scope,
                    configuracaoUsuarioDTO.parameter,
                    userId,
                    tema,
                    indiceConfiguracao,
                    "mapper snapshot");
        }

    }

    /**
     * Valida a chave funcional compartilhada por entidade e DTO de preferencia.
     */
    private void validaConfiguracaoUsuarioKeyCommunity(
            String configuracaoUserId,
            String configuracaoTema,
            String configuracaoParametro,
            String userId,
            String tema,
            int indiceConfiguracao,
            String contexto) {

        if (isBlank(configuracaoUserId)) {
            throw new IllegalStateException(
                    "User configuration entry at index "
                            + indiceConfiguracao
                            + " has no user id in "
                            + contexto
                            + ".");
        }
        if (isBlank(configuracaoTema)) {
            throw new IllegalStateException(
                    "User configuration entry at index "
                            + indiceConfiguracao
                            + " has no theme in "
                            + contexto
                            + ".");
        }
        if (isBlank(configuracaoParametro)) {
            throw new IllegalStateException(
                    "User configuration entry at index "
                            + indiceConfiguracao
                            + " has no parameter in "
                            + contexto
                            + ".");
        }
        if (!userId.equals(configuracaoUserId) || !tema.equals(configuracaoTema)) {
            throw new IllegalStateException(
                    "User configuration entry at index "
                            + indiceConfiguracao
                            + " does not match requested user/theme in "
                            + contexto
                            + ".");
        }

    }

    /**
     * Valida o batch de preferencias simples antes de qualquer mutacao do DTO.
     *
     * <p>Lista vazia e no-op valido: a SPA pode enviar um flush sem alteracoes
     * durante transicoes de tela. Itens nulos ou sem escopo/parametro, por outro
     * lado, nao formam chave persistivel e devem falhar antes do mapper.</p>
     */
    private void validaPayloadConfiguracaoUsuarioCommunity(
            String userId,
            List<ConfiguracaoUsuarioDTO> listaDTOs) {

        if (isBlank(userId)) {
            throw new IllegalArgumentException("User configuration user id is required.");
        }
        if (listaDTOs == null) {
            throw new IllegalArgumentException("User configuration payload list is required.");
        }

        for (ConfiguracaoUsuarioDTO configuracaoUsuarioDTO : listaDTOs) {
            if (configuracaoUsuarioDTO == null) {
                throw new IllegalArgumentException("User configuration entry is required.");
            }
            if (isBlank(configuracaoUsuarioDTO.scope)) {
                throw new IllegalArgumentException("User configuration scope is required.");
            }
            if (isBlank(configuracaoUsuarioDTO.parameter)) {
                throw new IllegalArgumentException("User configuration parameter is required.");
            }
        }

    }

    /**
     * Gera chave textual estavel para deduplicar configuracoes no mesmo batch.
     * A PK real e composta por usuario, tema e parametro, portanto essa mesma
     * combinacao precisa aparecer no maximo uma vez por chamada.
     */
    private String getChaveCanonicaConfiguracao(ConfiguracaoUsuarioDTO configuracaoUsuarioDTO) {
        return configuracaoUsuarioDTO.userId + "::"
                + configuracaoUsuarioDTO.scope + "::"
                + configuracaoUsuarioDTO.parameter;
    }

    /**
     * Valida o retorno salvo de preferencias simples antes de encerrar o POST.
     *
     * <p>A API nao devolve as entidades salvas, mas um retorno quebrado do
     * repository indicaria falha de persistencia/cache e deve aparecer como
     * erro funcional de configuracao de usuario, nao como sucesso silencioso da
     * SPA.</p>
     */
    private void validaConfiguracoesUsuarioSalvasCommunity(
            List<ConfiguracaoUsuario> configuracoesUsuarioSalvas,
            int numeroConfiguracoesUsuarioEsperado) {

        if (configuracoesUsuarioSalvas == null) {
            throw new IllegalArgumentException("Saved user configuration collection is required.");
        }
        if (configuracoesUsuarioSalvas.size() != numeroConfiguracoesUsuarioEsperado) {
            throw new IllegalArgumentException(
                    "Saved user configuration collection size "
                            + configuracoesUsuarioSalvas.size()
                            + " differs from expected size "
                            + numeroConfiguracoesUsuarioEsperado
                            + ".");
        }
        int indiceConfiguracao = 0;
        for (ConfiguracaoUsuario configuracaoUsuario : configuracoesUsuarioSalvas) {
            if (configuracaoUsuario == null) {
                throw new IllegalArgumentException(
                        "Saved user configuration entry at index "
                                + indiceConfiguracao
                                + " is required.");
            }
            if (configuracaoUsuario.getConfiguracaoUsuarioCompositeKey() == null) {
                throw new IllegalArgumentException(
                        "Saved user configuration entry at index "
                                + indiceConfiguracao
                                + " must have a primary key.");
            }
            indiceConfiguracao++;
        }

    }

    private boolean isBlank(String value) {

        return value == null || value.isBlank();

    }
    
}
