package com.opsfactor.community.web.restcontroller.admin;

import com.opsfactor.community.platform.cache.CachingService;
import com.opsfactor.community.platform.security.login.CommunitySecurityConstants;
import com.opsfactor.community.platform.security.login.facade.dto.UserDTO;
import com.opsfactor.community.platform.security.login.facade.UserFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

/**
 * Controller administrativo da edicao Community.
 *
 * <p>Esta superficie cobre somente usuarios simples, role ADMIN, criacao
 * opcional do primeiro usuario por propriedades externas e utilitarios tecnicos
 * de cache/memoria. Unlock de IP, lockout, tenant, SSO/OAuth/SAML e roles por
 * dominio pertencem ao Enterprise e nao devem aparecer como endpoints aqui.</p>
 */
@Slf4j
@RestController
public class AdminRestController {

    /**
     * Usuario opcional para bootstrap de instalacao Community inicial.
     *
     * <p>Deve vir de propriedade externa e so e usado pelo endpoint aberto de
     * criacao do primeiro usuario; nao representa usuario fixo versionado.</p>
     */
    @Value("${defaultuser:#{null}}")
    private String defaultUser;

    /**
     * Senha opcional do usuario de bootstrap inicial.
     *
     * <p>Assim como o usuario, precisa vir de configuracao externa. O valor e
     * enviado imediatamente ao `UserFrontService`, que usa o
     * `PasswordEncoder` Community para persistir hash.</p>
     */
    @Value("${defaultuser.password:#{null}}")
    private String defaultUserPassword;

    /**
     * Fachada Community de usuarios simples.
     *
     * <p>Este bean centraliza normalizacao para `ROLE_ADMIN`, persistencia de
     * senha e ocultacao de roles Enterprise eventualmente presentes em base
     * transicional.</p>
     */
    @Autowired
    private UserFacade userFrontService;

    /**
     * Service tecnico de cache usado apenas pelo endpoint administrativo de
     * limpeza manual. Nao carrega politica de seguranca ou tenant.</p>
     */
    @Autowired
    private CachingService cachingService;

    /**
     * Lista usuarios disponiveis para configuracao de views.
     *
     * <p>O parametro permanece no path por compatibilidade com o front legado,
     * mas no Community nao ha permissao por tipo de view: todos os usuarios
     * retornados sao administradores simples.</p>
     */
    @GetMapping("api/secured/user/configuredview/{configuredViewType}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<UserDTO>> getUserDTO(@PathVariable("configuredViewType") String configuredViewTypeString) {

        try {
            /*
             * Community possui seguranca propositalmente simples: todos os
             * usuarios visiveis para configuracao de planning books sao
             * usuarios administrativos. Roles por dominio ficam no Enterprise.
             */
            return ResponseEntity.ok(userFrontService.getUserDTOList());
        } catch (RuntimeException e) {
            log.error("Error listing Community users for configured view {}", configuredViewTypeString, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Retorna a unica role selecionavel no Community.
     */
    @GetMapping("api/secured/user/rolelist")
    @Secured("ROLE_ADMIN")
    public List<String> getUserRoleTypeList() {

        /*
         * Nao expor a enum completa no Community: varios valores existem no
         * codigo legado para compatibilidade de anotacoes e controllers, mas
         * nao podem ser selecionados nem persistidos nesta edicao.
         */
        return List.of(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE);

    }

    /**
     * Lista usuarios simples do Community.
     */
    @GetMapping("api/secured/user")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<UserDTO>> getUserDTOList() {

        try {
            List<UserDTO> userDTOList = userFrontService.getUserDTOList();
            return ResponseEntity.ok(userDTOList);
        } catch (RuntimeException e) {
            log.error("Error listing Community users", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Cria ou atualiza um usuario Community.
     */
    @PostMapping("api/secured/user")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> saveUserDTO(@RequestBody UserDTO userDTO) {

        try {
            userFrontService.saveUserDTO(userDTO);
            return ResponseEntity.ok("User data saved");
        } catch (RuntimeException e) {
            log.error("Error saving Community user {}", userDTO == null ? null : userDTO.id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Bootstrap aberto do primeiro usuario Community.
     *
     * <p>Este endpoint nao substitui cadastro administrativo normal. Ele existe
     * apenas para instalacoes vazias criarem o primeiro `ROLE_ADMIN` quando as
     * propriedades externas `defaultuser` e `defaultuser.password` estiverem
     * preenchidas.</p>
     */
    @PostMapping("api/open/createdefaultuser")
    public ResponseEntity<String> createDefaultUser() {

        try {
            /*
             * Bootstrap Community para primeira instalacao local/self-hosted.
             * O endpoint permanece aberto porque ainda nao existe usuario para
             * autenticar a primeira chamada, mas ele so cria algo quando:
             * - defaultuser e defaultuser.password foram configurados
             *   externamente; e
             * - nao ha nenhum usuario persistido.
             */
            if (defaultUser == null || defaultUserPassword == null) {
                throw getDefaultUserCreationNotAllowedException();
            }
            
            if (!userFrontService.getUserDTOList().isEmpty()) {
                throw getUsersAlreadyCreatedException();
            }
            
            UserDTO userDTO = UserDTO.builder()
                    .id(defaultUser)
                    .password(defaultUserPassword)
                    .active(true)
                    .userRoles(Set.of(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE))
                    .build();
            
            userFrontService.saveUserDTO(userDTO);
            return ResponseEntity.ok("Default user created");
        } catch (IllegalStateException illegalStateException) {
            log.warn("Community default user bootstrap rejected: {}", illegalStateException.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(illegalStateException.getMessage());
        } catch (RuntimeException e) {
            log.error("Error creating Community default user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
        
    }

    /**
     * Retorna uso de memoria JVM em MB para diagnostico operacional simples.
     */
    @GetMapping("api/secured/usedmemory")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> getUsedMemory() {

        try {
            return ResponseEntity.ok("Used memory (MB): " + ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024));
        } catch (RuntimeException e) {
            log.error("Error reading JVM used memory", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Limpa todos os caches locais conhecidos pela aplicacao Community.
     */
    @GetMapping("api/secured/clearallcaches")
    @Secured("ROLE_ADMIN")
    public void clearAllCaches() {

        cachingService.evictAllCacheValues();

    }

    private IllegalStateException getDefaultUserCreationNotAllowedException() {

        return new IllegalStateException(
                "Default user creation not allowed: configure defaultuser and defaultuser.password externally before calling the bootstrap endpoint");

    }

    private IllegalStateException getUsersAlreadyCreatedException() {

        return new IllegalStateException(
                "Users have already been created; use the secured Community user administration endpoint instead");

    }

}
