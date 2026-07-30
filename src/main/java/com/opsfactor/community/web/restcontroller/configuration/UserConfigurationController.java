package com.opsfactor.community.web.restcontroller.configuration;

import com.opsfactor.community.capability.configuration.user.facade.dto.ConfiguracaoUsuarioDTO;
import com.opsfactor.community.capability.configuration.user.facade.dto.UserInterfacePreferencesDTO;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;
import com.opsfactor.community.capability.configuration.facade.ConfiguracaoUsuarioFacade;
import com.opsfactor.community.capability.configuration.facade.ConfiguredViewFacade;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewDTO;
import com.opsfactor.community.platform.security.login.CommunitySecurityConstants;
import com.opsfactor.community.platform.security.login.AuthenticationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Controller Community para views e preferencias simples do usuario.
 *
 * <p>Mesmo com login simplificado e role unica ADMIN, o Community preserva
 * views pessoais do Planning Book para Demand/Supply. A service layer limita
 * essas views ao nivel material/location e bloqueia workflow, agregacao,
 * filtros DFU e selecao livre de KFs Enterprise.</p>
 */
@Slf4j
@RestController
public class UserConfigurationController {

    /**
     * Fachada das views de Planning Book. O service concentra as validacoes de
     * nivel material/location, KFs padrao e bloqueios de agregacao Enterprise.
     */
    @Autowired
    private ConfiguredViewFacade configuredViewFrontService;

    /**
     * Fachada das preferencias simples de usuario usadas pela SPA Community.
     * Configuracoes dependentes de permissao, tenant ou features Enterprise
     * devem entrar por services/overlays especificos.
     */
    @Autowired
    private ConfiguracaoUsuarioFacade configuracaoUsuarioFrontService;

    /**
     * Porta central de seguranca Community.
     *
     * <p>O controller usa este service para ler usuario e authorities ja
     * validados, evitando acesso direto ao `SecurityContextHolder` e mantendo a
     * semantica simples de `ROLE_ADMIN` em um unico ponto.</p>
     */
    @Autowired
    private AuthenticationService authenticationService;

    /**
     * Retorna as views de Demand Planning Book associadas ao usuario indicado.
     *
     * <p>No Community existe apenas ROLE_ADMIN, mas o endpoint conserva o
     * parametro de usuario para compatibilidade com o front compartilhado e
     * com o overlay Enterprise.</p>
     */
    @GetMapping("api/secured/configuration/user/view/demandplanningbook/{userId}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<ConfiguredViewDTO>> getConfiguredViewDTOListDemandPlanning(@PathVariable("userId") String userId) {

        try {
            List<ConfiguredViewDTO> configuredViewDTOList = configuredViewFrontService.getConfiguredViewDTOListDemandPlanningBook(userId);
            return ResponseEntity.ok(configuredViewDTOList);
        } catch (RuntimeException e) {
            log.error("Error listing Demand Planning Book views for user {}", userId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Alias legado para consultar views por tipo de Planning Book.
     *
     * <p>A borda apenas decide entre as leituras Demand e Supply ja existentes.
     * Ela nao consulta repositories nem remonta projections, preservando as
     * regras de visibilidade por usuario concentradas na service layer.</p>
     */
    @PostMapping("api/secured/configuration/user/view/list")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<ConfiguredViewDTO>> getConfiguredViewDTOList(
            @RequestBody ConfiguredViewDTO configuredViewDTO) {

        validaChaveConfiguredViewParaConsulta(configuredViewDTO);

        try {
            String userIdRequisitante = getUserIdAutenticadoCommunity();
            boolean usuarioPodeExtrairVisaoOutrosUsuarios = usuarioAutenticadoAdminCommunity();
            List<ConfiguredViewDTO> configuredViewDTOList;

            if (configuredViewDTO.viewType == ConfiguredView.TipoView.DEMANDPLANNINGBOOK) {
                if (!userIdRequisitante.equals(configuredViewDTO.userId)
                        && !usuarioPodeExtrairVisaoOutrosUsuarios) {
                    configuredViewDTOList = List.of();
                } else {
                    configuredViewDTOList = configuredViewFrontService
                            .getConfiguredViewDTOListDemandPlanningBook(configuredViewDTO.userId);
                }
            } else if (configuredViewDTO.viewType == ConfiguredView.TipoView.SUPPLYPLANNINGBOOK) {
                configuredViewDTOList = configuredViewFrontService.getConfiguredViewDTOListSupplyPlanningBook(
                        userIdRequisitante,
                        configuredViewDTO.userId,
                        usuarioPodeExtrairVisaoOutrosUsuarios);
            } else {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Configured View type is not supported by Community.");
            }

            return ResponseEntity.ok(configuredViewDTOList);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Error listing {} Planning Book views for user {}",
                    configuredViewDTO.viewType,
                    configuredViewDTO.userId,
                    e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Alias legado para criar uma view inicial de Planning Book.
     *
     * <p>O contrato compartilhado envia {@code userId}, {@code viewName} e
     * {@code viewType} no corpo. A borda apenas seleciona a operacao tipada
     * que ja cria as views Community com suas key figures e niveis permitidos;
     * nao persiste o DTO generico nem aceita configuracoes Enterprise.</p>
     */
    @PostMapping("api/secured/configuration/user/view/new")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> createConfiguredView(@RequestBody ConfiguredViewDTO configuredViewDTO) {

        validaChaveConfiguredViewParaCriacao(configuredViewDTO);

        try {
            if (configuredViewDTO.viewType == ConfiguredView.TipoView.DEMANDPLANNINGBOOK) {
                configuredViewFrontService.createConfiguredViewDTODemandPlanningBook(
                        configuredViewDTO.userId,
                        configuredViewDTO.viewName);
            } else if (configuredViewDTO.viewType == ConfiguredView.TipoView.SUPPLYPLANNINGBOOK) {
                configuredViewFrontService.createConfiguredViewSupplyPlanningBook(
                        configuredViewDTO.userId,
                        configuredViewDTO.viewName);
            } else {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Configured View type is not supported by Community.");
            }

            return ResponseEntity.ok("View Successfully Created");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Error creating {} Planning Book view {} for user {}",
                    configuredViewDTO.viewType,
                    configuredViewDTO.viewName,
                    configuredViewDTO.userId,
                    e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }
    
    /**
     * Cria uma view inicial de Demand Planning Book para o usuario informado.
     */
    @GetMapping("api/secured/configuration/user/view/demandplanningbook/new/{userId}/{viewName}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> createConfiguredViewDTOListDemandPlanning(@PathVariable("userId") String userId, @PathVariable("viewName") String viewName) {

        try {
            configuredViewFrontService.createConfiguredViewDTODemandPlanningBook(userId, viewName);
            return ResponseEntity.ok("View Successfully Created");
        } catch (RuntimeException e) {
            log.error("Error creating Demand Planning Book view {} for user {}", viewName, userId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }
    
    /**
     * Retorna as views de Demand Planning Book do usuario logado.
     */
    @GetMapping("api/secured/configuration/user/view/demandplanningbook")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<ConfiguredViewDTO>> getConfiguredViewDTOListDemandPlanning() {

        try {
            String userIdSolicitante = getUserIdAutenticadoCommunity();
            List<ConfiguredViewDTO> configuredViewDTOList = configuredViewFrontService.getConfiguredViewDTOListDemandPlanningBook(userIdSolicitante);
            return ResponseEntity.ok(configuredViewDTOList);
        } catch (RuntimeException e) {
            log.error("Error listing Demand Planning Book views for current user", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }
    
    /**
     * Retorna as views de Supply Planning Book associadas ao usuario indicado.
     *
     * <p>A permissao para consultar visoes de outros usuarios continua
     * reduzida ao papel ADMIN no Community. Politicas mais granulares ficam
     * reservadas ao Enterprise.</p>
     */
    @GetMapping("api/secured/configuration/user/view/supplyplanningbook/{userId}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<ConfiguredViewDTO>> getConfiguredViewDTOListSupplyPlanning(@PathVariable("userId") String userIdAExtrair) {

        try {
            /*
             * Community possui apenas ROLE_ADMIN, mas a service de views ainda
             * recebe esta flag porque o Enterprise pode especializar a regra de
             * visibilidade por usuario/role mantendo o contrato REST legado.
             */
            boolean usuarioPodeExtrairVisaoOutrosUsuarios = usuarioAutenticadoAdminCommunity();
            String userIdRequisitante = getUserIdAutenticadoCommunity();
            List<ConfiguredViewDTO> configuredViewDTOList = configuredViewFrontService.getConfiguredViewDTOListSupplyPlanningBook(
                    userIdRequisitante, userIdAExtrair, usuarioPodeExtrairVisaoOutrosUsuarios);
            return ResponseEntity.ok(configuredViewDTOList);
        } catch (RuntimeException e) {
            log.error("Error listing Supply Planning Book views for user {}", userIdAExtrair, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }   
    
    /**
     * Cria uma view inicial de Supply Planning Book para o usuario informado.
     */
    @GetMapping("api/secured/configuration/user/view/supplyplanningbook/new/{userId}/{viewName}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> createConfiguredViewDTOListSupplyPlanning(@PathVariable("userId") String userId, @PathVariable("viewName") String viewName) {

        try {
            configuredViewFrontService.createConfiguredViewSupplyPlanningBook(userId, viewName);
            return ResponseEntity.ok("View Successfully Created");
        } catch (RuntimeException e) {
            log.error("Error creating Supply Planning Book view {} for user {}", viewName, userId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }
    
    /**
     * Retorna as views de Supply Planning Book do usuario logado.
     */
    @GetMapping("api/secured/configuration/user/view/supplyplanningbook")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<ConfiguredViewDTO>> getConfiguredViewDTOListSupplyPlanning() {

        try {
            String userIdRequisitante = getUserIdAutenticadoCommunity();
            List<ConfiguredViewDTO> configuredViewDTOList = configuredViewFrontService.getConfiguredViewDTOListSupplyPlanningBook(userIdRequisitante);
            return ResponseEntity.ok(configuredViewDTOList);
        } catch (RuntimeException e) {
            log.error("Error listing Supply Planning Book views for current user", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }
    
    /**
     * Salva a configuracao de uma view de Planning Book.
     *
     * <p>O DTO compartilhado pode carregar envelopes de agrupamento e selecao
     * de KFs, mas os services Community devem manter a visao em nivel
     * material/location e bloquear configuracoes Enterprise.</p>
     */
    @PostMapping("api/secured/configuration/user/view")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> postConfiguredViewDTOList(@RequestBody ConfiguredViewDTO configuredViewDTO) {

        try {
            String userId = getUserIdAutenticadoCommunity();
            /*
             * A permissao fica intencionalmente binaria na Community. Qualquer
             * granularidade adicional deve ser introduzida por overlay
             * Enterprise, nao por novas roles neste controller.
             */
            boolean usuarioPodeModificarVisaoOutrosUsuarios = usuarioAutenticadoAdminCommunity();
            configuredViewFrontService.saveConfiguredViewDTO(configuredViewDTO, userId, usuarioPodeModificarVisaoOutrosUsuarios);
            return ResponseEntity.ok("User View Saved");
        } catch (RuntimeException e) {
            log.error("Error saving Planning Book user view", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }
    
    /**
     * Remove uma view de Planning Book cadastrada.
     */
    @PostMapping("api/secured/configuration/user/view/delete")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> removeConfiguredViewDTO(@RequestBody ConfiguredViewDTO configuredViewDTO) {

        try {
            configuredViewFrontService.removeConfiguredView(configuredViewDTO);
            return ResponseEntity.ok("User View Removed");
        } catch (RuntimeException e) {
            log.error("Error removing Planning Book user view", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Lista preferencias simples do usuario logado para um tema da SPA.
     */
    @GetMapping("api/secured/configuration/user/userconfigs/{tema}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<ConfiguracaoUsuarioDTO>> getConfiguracaoUsuarioDTOList(@PathVariable("tema") String tema) {

        try {
            String userId = getUserIdAutenticadoCommunity();
            List<ConfiguracaoUsuarioDTO> configuracaoUsuarioDTOList = configuracaoUsuarioFrontService.getConfiguredViewDTOList(userId, tema);
            return ResponseEntity.ok(configuracaoUsuarioDTOList);
        } catch (RuntimeException e) {
            log.error("Error listing user configuration for theme {}", tema, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }
    
    /**
     * Salva preferencias simples do usuario logado para a SPA.
     */
    @PostMapping("api/secured/configuration/user/userconfigs")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> postConfiguracaoUsuarioDTOList(
            @RequestBody List<ConfiguracaoUsuarioDTO> configuracoesUsuario) {

        try {
            String userId = getUserIdAutenticadoCommunity();
            configuracaoUsuarioFrontService.saveConfigurationViewDTOList(userId, configuracoesUsuario);
            return ResponseEntity.ok("User Configurations Saved");
        } catch (RuntimeException e) {
            log.error("Error saving user configurations", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Carrega as preferencias visuais tipadas da interface do usuario logado.
     */
    @GetMapping("api/secured/configuration/user/interface/preferences")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<UserInterfacePreferencesDTO> getUserInterfacePreferencesDTO() {

        try {
            String userId = getUserIdAutenticadoCommunity();
            UserInterfacePreferencesDTO userInterfacePreferencesDTO =
                    configuracaoUsuarioFrontService.getUserInterfacePreferencesDTO(userId);
            return ResponseEntity.ok(userInterfacePreferencesDTO);
        } catch (RuntimeException e) {
            log.error("Error loading user interface preferences", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Salva as preferencias visuais tipadas da interface do usuario logado.
     */
    @PostMapping("api/secured/configuration/user/interface/preferences")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<UserInterfacePreferencesDTO> postUserInterfacePreferencesDTO(
            @RequestBody UserInterfacePreferencesDTO userInterfacePreferencesDTO) {

        try {
            String userId = getUserIdAutenticadoCommunity();
            UserInterfacePreferencesDTO savedUserInterfacePreferencesDTO =
                    configuracaoUsuarioFrontService.saveUserInterfacePreferencesDTO(
                            userId,
                            userInterfacePreferencesDTO);
            return ResponseEntity.ok(savedUserInterfacePreferencesDTO);
        } catch (RuntimeException e) {
            log.error("Error saving user interface preferences", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Le o usuario autenticado pelo service central de seguranca.
     */
    private String getUserIdAutenticadoCommunity() {

        return authenticationService.getAuthenticatedUserId();

    }

    /**
     * Valida a chave minima que o alias legado usa para consultar uma User
     * View, antes de selecionar a leitura tipada correspondente.
     */
    private void validaChaveConfiguredViewParaConsulta(ConfiguredViewDTO configuredViewDTO) {

        if (configuredViewDTO == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Configured View request is required.");
        }
        if (configuredViewDTO.userId == null || configuredViewDTO.userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User must be informed.");
        }
        if (configuredViewDTO.viewType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "View type must be informed.");
        }

    }

    /**
     * Valida a chave minima do alias legado de criacao antes de delegar a
     * inicializacao para uma das services tipadas de Planning Book.
     */
    private void validaChaveConfiguredViewParaCriacao(ConfiguredViewDTO configuredViewDTO) {

        validaChaveConfiguredViewParaConsulta(configuredViewDTO);
        if (configuredViewDTO.viewName == null || configuredViewDTO.viewName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "View name must be informed.");
        }

    }

    /**
     * Verifica a unica authority funcional da edicao Community.
     */
    private boolean usuarioAutenticadoAdminCommunity() {

        return authenticationService.currentUserHasAnyRole(List.of(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE));

    }
    
}
