package com.opsfactor.community.platform.security.login.facade;

import com.opsfactor.community.platform.security.login.CommunitySecurityConstants;
import com.opsfactor.community.platform.security.login.facade.dto.UserDTO;
import com.opsfactor.community.platform.security.login.model.User;
import com.opsfactor.community.platform.security.login.model.UserRole;
import com.opsfactor.community.platform.security.login.model.UserRole.UserRoleCompositeKey;
import com.opsfactor.community.platform.security.login.repository.UserRepository;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Fachada Community de manutencao de usuarios simples.
 *
 * <p>A edicao Community nao possui roles por dominio, tenant, SSO, lockout por
 * IP ou escopos granulares. Toda criacao/atualizacao normaliza roles para
 * {@code ROLE_ADMIN}, rejeitando payloads Enterprise antes de persistir, e as
 * leituras ocultam roles legadas que eventualmente existam na base.</p>
 */
@Service
public class UserFacade {

    /**
     * Repository Community de usuários. O @Autowired fica explícito para
     * diferenciar beans Spring de atributos puramente locais do service.
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Encoder definido pela configuração de segurança Community. Manter o uso
     * do bean evita divergência futura entre gravação de senha e autenticação.
     */
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * Lista usuarios Community visiveis para administracao.
     *
     * <p>Roles Enterprise persistidas em bases transicionais nao sao expostas
     * ao front: o DTO mostra apenas {@code ROLE_ADMIN} quando essa role existe
     * no usuario.</p>
     */
    public List<UserDTO> getUserDTOList() {

        List<User> userList = userRepository.findAll();
        validaUserListCarregadaCommunity(userList);

        List<UserDTO> userDTOList = userList.stream()
                .sorted(Comparator.comparing(x -> x.getId()))
                .map(x -> UserDTO.builder()
                        .id(x.getId())
                        .firstName(x.getFirstName())
                        .lastName(x.getLastName())
                        .email(x.getEmail())
                        .active(x.getActive())
                        .userRoles(getCommunityUserRoles(x))
                        .build())
                .collect(Collectors.toList());
        validaUserDTOListListagemCommunity(userDTOList);
        return userDTOList;
        
    }
    
    /**
     * Cria ou atualiza um usuario Community.
     *
     * <p>Na criacao, a senha e obrigatoria e nao pode ser branca. Na
     * atualizacao, somente {@code null} preserva o hash existente; texto vazio
     * ou branco e rejeitado para nunca trocar uma credencial por senha vazia.
     * Roles nulas ou vazias sao normalizadas para {@code ROLE_ADMIN}; qualquer
     * outra role falha antes de atualizar a entidade.</p>
     */
    @Transactional
    public void saveUserDTO(UserDTO userDTO) {

        validaUserDTOCommunity(userDTO);
        normalizaUserRolesCommunity(userDTO);
        
        User user = userRepository.findById(userDTO.id).orElse(null);
        boolean usuarioNovo = user == null;
        validaSenhaCommunity(userDTO.password, usuarioNovo);

        if (usuarioNovo) {
            User novoUsuario = new User();
            novoUsuario.setId(userDTO.id);
            User novoUsuarioSalvo = userRepository.save(novoUsuario);
            user = validaUsuarioSalvoCommunity(
                    userDTO.id,
                    novoUsuarioSalvo,
                    "Community user creation save returned invalid snapshot.");
        }

        validaUserRolesSnapshotUsuarioCommunity(user, "Community user loaded for save");
        
        user.setEmail(userDTO.email);
        user.setActive(userDTO.active);
        user.setFirstName(userDTO.firstName);
        user.setLastName(userDTO.lastName);
        if (userDTO.password != null) {
            user.setPassword(passwordEncoder.encode(userDTO.password));
        }
                
        user.getUserRoles().removeIf(x -> !userDTO.userRoles.contains(x.getUserRoleType()));
        for (String userRole : userDTO.userRoles) {
            if (user.getUserRoles().stream().anyMatch(x -> x.getUserRoleType().equals(userRole))) continue;
            UserRole userRoleEntity = new UserRole(new UserRoleCompositeKey(user, userRole));
            if (!user.getUserRoles().contains(userRoleEntity)) {
                user.getUserRoles().add(userRoleEntity);
            }
        }

        /*
         * O endpoint nao devolve DTO, mas a tela assume que o usuario foi
         * criado/atualizado apos a chamada. Validar o retorno salvo diferencia
         * sucesso real de repository/stub quebrado, principalmente porque o
         * Community possui apenas uma role funcional e nao tera outro fluxo de
         * seguranca para reconciliar um usuario parcial.
         */
        User userSalvo = userRepository.save(user);
        User userSalvoValidado = validaUsuarioSalvoCommunity(
                userDTO.id,
                userSalvo,
                "Community user save returned invalid snapshot.");
        validaUserRolesSnapshotUsuarioCommunity(userSalvoValidado, "Community user saved snapshot");
        
    }

    /**
     * Protege o lifecycle da credencial Community antes de qualquer escrita.
     *
     * <p>Uma conta nova sem senha jamais conseguiria autenticar. Para contas
     * existentes, {@code null} e o unico sinal aceito para preservar o hash;
     * vazio ou branco precisa falhar para nao habilitar senha vazia por
     * acidente de serializacao do front.</p>
     */
    private void validaSenhaCommunity(String password, boolean usuarioNovo) {

        if (usuarioNovo && (password == null || password.isBlank())) {
            throw new IllegalArgumentException("Community user password is required when creating a user.");
        }
        if (!usuarioNovo && password != null && password.isBlank()) {
            throw new IllegalArgumentException(
                    "Community user password must be null to preserve the existing password or non-blank to replace it.");
        }

    }

    /**
     * Valida a fotografia salva de usuario Community.
     *
     * <p>Senha, e-mail e nomes podem ser nulos por regra cadastral simples. O
     * id, por outro lado, e a chave do login HTTP Basic e precisa voltar igual
     * ao payload salvo. Snapshot nulo ou id divergente indicam quebra do
     * repository/JPA/stub e nao devem ser aceitos como sucesso de tela.</p>
     */
    private User validaUsuarioSalvoCommunity(
            String userId,
            User userSalvo,
            String mensagemErro) {

        if (userSalvo == null
                || userSalvo.getId() == null
                || userSalvo.getId().isBlank()
                || !userId.equals(userSalvo.getId())) {
            throw new IllegalStateException(mensagemErro);
        }

        return userSalvo;

    }

    /**
     * Valida a fotografia de usuarios carregada para a tela Community.
     *
     * <p>Lista vazia e valida para bootstrap. Usuario nulo, id ausente ou lista
     * de roles nula indicam repository/snapshot quebrado; roles Enterprise
     * legadas podem existir na lista, mas serao filtradas no DTO final.</p>
     */
    private void validaUserListCarregadaCommunity(List<User> userList) {

        if (userList == null) {
            throw new IllegalStateException("Community user repository returned null list.");
        }

        for (int indiceUsuario = 0; indiceUsuario < userList.size(); indiceUsuario++) {
            User user = userList.get(indiceUsuario);
            if (user == null) {
                throw new IllegalStateException(
                        "Community user at index " + indiceUsuario + " is required in repository snapshot.");
            }
            if (user.getId() == null || user.getId().isBlank()) {
                throw new IllegalStateException(
                        "Community user at index " + indiceUsuario + " has no id in repository snapshot.");
            }
            if (user.getUserRoles() == null) {
                throw new IllegalStateException(
                        "Community user at index " + indiceUsuario + " has no role snapshot.");
            }
            validaUserRolesSnapshotUsuarioCommunity(
                    user,
                    "Community user at index " + indiceUsuario);
        }

    }

    /**
     * Valida a lista de roles associada a um usuario Community.
     *
     * <p>A lista pode conter roles Enterprise legadas em bases transicionais,
     * mas a estrutura JPA precisa estar integra antes de listagem ou mutacao.
     * Falhar aqui deixa claro que o problema esta no snapshot de seguranca, nao
     * em uma regra funcional de role nao permitida.</p>
     */
    private void validaUserRolesSnapshotUsuarioCommunity(User user, String contextoOperacional) {

        if (user.getUserRoles() == null) {
            throw new IllegalStateException(contextoOperacional + " has no role snapshot.");
        }

        for (int indiceRole = 0; indiceRole < user.getUserRoles().size(); indiceRole++) {
            UserRole userRole = user.getUserRoles().get(indiceRole);
            if (userRole == null) {
                throw new IllegalStateException(
                        contextoOperacional
                                + " has null role at index "
                                + indiceRole
                                + " in role snapshot.");
            }
            if (userRole.getUserRoleCompositeKey() == null) {
                throw new IllegalStateException(
                        contextoOperacional
                                + " has role at index "
                                + indiceRole
                                + " without composite key in role snapshot.");
            }
            if (userRole.getUserRoleType() == null || userRole.getUserRoleType().isBlank()) {
                throw new IllegalStateException(
                        contextoOperacional
                                + " has role at index "
                                + indiceRole
                                + " without role type in role snapshot.");
            }
        }

    }

    /**
     * Valida a resposta DTO devolvida para a administracao Community.
     *
     * <p>A listagem nunca deve expor senha/hash e nunca deve publicar roles
     * Enterprise como opcoes selecionadas. O front Community mostra apenas
     * {@code ROLE_ADMIN}; usuarios sem essa role aparecem com lista vazia.</p>
     */
    private void validaUserDTOListListagemCommunity(List<UserDTO> userDTOList) {

        if (userDTOList == null) {
            throw new IllegalStateException("Community user DTO list snapshot is required.");
        }

        for (int indiceUsuario = 0; indiceUsuario < userDTOList.size(); indiceUsuario++) {
            UserDTO userDTO = userDTOList.get(indiceUsuario);
            if (userDTO == null) {
                throw new IllegalStateException(
                        "Community user DTO at index " + indiceUsuario + " is required in list snapshot.");
            }
            if (userDTO.id == null || userDTO.id.isBlank()) {
                throw new IllegalStateException(
                        "Community user DTO at index " + indiceUsuario + " has no id in list snapshot.");
            }
            if (userDTO.password != null && !userDTO.password.isBlank()) {
                throw new IllegalStateException(
                        "Community user DTO at index " + indiceUsuario + " must not expose password in list snapshot.");
            }
            if (userDTO.userRoles == null) {
                throw new IllegalStateException(
                        "Community user DTO at index " + indiceUsuario + " has no role snapshot.");
            }

            Set<String> invalidRoles = userDTO.userRoles.stream()
                    .filter(userRole -> !CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE.equals(userRole))
                    .collect(Collectors.toSet());
            if (!invalidRoles.isEmpty()) {
                throw new IllegalStateException(
                        "Community user DTO at index "
                                + indiceUsuario
                                + " exposes unsupported roles: "
                                + invalidRoles
                                + ".");
            }
        }

    }

    /**
     * Valida a chave minima do usuario antes de normalizar roles ou acessar o
     * repository.
     *
     * <p>Senha nula continua permitida para atualizacao de dados cadastrais sem
     * troca de senha. O id, por outro lado, e a chave primaria do login simples
     * Community; payload sem id nao pode virar consulta `findById(null)` nem
     * criacao anonima de usuario.</p>
     */
    private void validaUserDTOCommunity(UserDTO userDTO) {

        if (userDTO == null) {
            throw new IllegalArgumentException("Community user payload is required.");
        }
        if (userDTO.id == null || userDTO.id.isBlank()) {
            throw new IllegalArgumentException("Community user id is required.");
        }

    }

    /**
     * Community não possui administração granular de roles. Se o front ainda
     * enviar payload vazio, normalizamos para ADMIN para manter a criação de
     * usuarios simples. Se enviar qualquer role Enterprise, falhamos
     * explicitamente para não persistir permissões que esta edição ignora.
     */
    private void normalizaUserRolesCommunity(UserDTO userDTO) {

        if (userDTO.userRoles == null || userDTO.userRoles.isEmpty()) {
            userDTO.userRoles = new HashSet<>(Set.of(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE));
            return;
        }

        Set<String> rolesNaoPermitidas = userDTO.userRoles.stream()
                .filter(userRole -> !CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE.equals(userRole))
                .collect(Collectors.toSet());
        if (!rolesNaoPermitidas.isEmpty()) {
            throw new IllegalArgumentException(
                    "OpsFactor Community accepts only " + CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE
                            + " user role. Invalid roles: "
                            + rolesNaoPermitidas);
        }

        userDTO.userRoles = new HashSet<>(Set.of(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE));

    }

    /**
     * Respostas Community exibem somente a permissão ADMIN. Roles legadas que
     * eventualmente existam no banco não são expostas pela API de usuários.
     */
    private Set<String> getCommunityUserRoles(User user) {

        boolean usuarioPossuiRoleAdmin = user.getUserRoles().stream()
                .anyMatch(userRole ->
                        CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE.equals(userRole.getUserRoleType()));

        return usuarioPossuiRoleAdmin ? Set.of(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE) : Set.of();

    }
        
}
