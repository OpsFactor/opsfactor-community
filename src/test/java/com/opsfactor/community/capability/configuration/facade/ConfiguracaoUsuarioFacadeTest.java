package com.opsfactor.community.capability.configuration.facade;

import com.opsfactor.community.capability.configuration.user.facade.dto.ConfiguracaoUsuarioDTO;
import com.opsfactor.community.capability.configuration.user.facade.dto.UserInterfacePreferencesDTO;
import com.opsfactor.community.capability.configuration.user.facade.mapper.ConfiguracaoUsuarioAutoMapper;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguracaoUsuario;
import com.opsfactor.community.capability.configuration.user.repository.ConfiguracaoUsuarioRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Contratos de borda das preferencias simples de usuario Community.
 *
 * <p>Os testes usam a service sem repositories injetados de proposito: payload
 * sem chave funcional deve falhar antes de qualquer chamada JPA, e payload
 * vazio deve ser tratado como no-op sem acesso a mapper/repository.</p>
 */
public class ConfiguracaoUsuarioFacadeTest {

    @Test
    public void getConfiguredViewDTOListShouldRejectMissingQueryKeyBeforeRepository() {

        ConfiguracaoUsuarioFacade configuracaoUsuarioFrontService = new ConfiguracaoUsuarioFacade();

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> configuracaoUsuarioFrontService.getConfiguredViewDTOList(null, "planning-book"));
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> configuracaoUsuarioFrontService.getConfiguredViewDTOList("admin", " "));

    }

    @Test
    public void getConfiguredViewDTOListShouldRejectBrokenRepositorySnapshotBeforeMapper()
            throws Exception {

        ConfiguracaoUsuarioFacade serviceComListaNula =
                criaConfiguracaoUsuarioFrontServiceParaListagem(
                        null,
                        List.of());

        IllegalStateException nullCollectionException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> serviceComListaNula.getConfiguredViewDTOList("admin", "planning-book"));
        Assertions.assertEquals(
                "User configuration repository returned null collection for user admin and theme planning-book.",
                nullCollectionException.getMessage());

        ConfiguracaoUsuarioFacade serviceComItemNulo =
                criaConfiguracaoUsuarioFrontServiceParaListagem(
                        Arrays.asList((ConfiguracaoUsuario) null),
                        List.of());

        IllegalStateException nullItemException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> serviceComItemNulo.getConfiguredViewDTOList("admin", "planning-book"));
        Assertions.assertEquals(
                "User configuration entry at index 0 is required in repository snapshot.",
                nullItemException.getMessage());

        ConfiguracaoUsuarioFacade serviceComChaveNula =
                criaConfiguracaoUsuarioFrontServiceParaListagem(
                        List.of(new ConfiguracaoUsuario()),
                        List.of());

        IllegalStateException nullKeyException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> serviceComChaveNula.getConfiguredViewDTOList("admin", "planning-book"));
        Assertions.assertEquals(
                "User configuration entry at index 0 must have a primary key in repository snapshot.",
                nullKeyException.getMessage());

        ConfiguracaoUsuarioFacade serviceComUsuarioDivergente =
                criaConfiguracaoUsuarioFrontServiceParaListagem(
                        List.of(getConfiguracaoUsuarioEntidade("other", "planning-book", "last-view")),
                        List.of());

        IllegalStateException wrongUserException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> serviceComUsuarioDivergente.getConfiguredViewDTOList("admin", "planning-book"));
        Assertions.assertEquals(
                "User configuration entry at index 0 does not match requested user/theme in repository snapshot.",
                wrongUserException.getMessage());

    }

    @Test
    public void getConfiguredViewDTOListShouldRejectBrokenMapperSnapshotBeforeReturning()
            throws Exception {

        List<ConfiguracaoUsuario> configuracaoUsuarioList =
                List.of(getConfiguracaoUsuarioEntidade("admin", "planning-book", "last-view"));

        ConfiguracaoUsuarioFacade serviceComDTOListNula =
                criaConfiguracaoUsuarioFrontServiceParaListagem(
                        configuracaoUsuarioList,
                        null);

        IllegalStateException nullDTOCollectionException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> serviceComDTOListNula.getConfiguredViewDTOList("admin", "planning-book"));
        Assertions.assertEquals(
                "User configuration mapper returned null DTO collection for user admin and theme planning-book.",
                nullDTOCollectionException.getMessage());

        ConfiguracaoUsuarioFacade serviceComDTOItemNulo =
                criaConfiguracaoUsuarioFrontServiceParaListagem(
                        configuracaoUsuarioList,
                        Arrays.asList((ConfiguracaoUsuarioDTO) null));

        IllegalStateException nullDTOItemException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> serviceComDTOItemNulo.getConfiguredViewDTOList("admin", "planning-book"));
        Assertions.assertEquals(
                "User configuration DTO at index 0 is required in mapper snapshot.",
                nullDTOItemException.getMessage());

        ConfiguracaoUsuarioDTO configuracaoUsuarioDTOSemParametro =
                getConfiguracaoUsuarioDTOListagem("admin", "planning-book", null);
        ConfiguracaoUsuarioFacade serviceComDTOSemParametro =
                criaConfiguracaoUsuarioFrontServiceParaListagem(
                        configuracaoUsuarioList,
                        List.of(configuracaoUsuarioDTOSemParametro));

        IllegalStateException missingParameterException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> serviceComDTOSemParametro.getConfiguredViewDTOList("admin", "planning-book"));
        Assertions.assertEquals(
                "User configuration entry at index 0 has no parameter in mapper snapshot.",
                missingParameterException.getMessage());

        ConfiguracaoUsuarioDTO configuracaoUsuarioDTOOutroTema =
                getConfiguracaoUsuarioDTOListagem("admin", "supply-book", "last-view");
        ConfiguracaoUsuarioFacade serviceComDTOTemaDivergente =
                criaConfiguracaoUsuarioFrontServiceParaListagem(
                        configuracaoUsuarioList,
                        List.of(configuracaoUsuarioDTOOutroTema));

        IllegalStateException wrongThemeException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> serviceComDTOTemaDivergente.getConfiguredViewDTOList("admin", "planning-book"));
        Assertions.assertEquals(
                "User configuration entry at index 0 does not match requested user/theme in mapper snapshot.",
                wrongThemeException.getMessage());

    }

    @Test
    public void saveConfigurationViewDTOListShouldRejectInvalidPayloadBeforeRepository() {

        ConfiguracaoUsuarioFacade configuracaoUsuarioFrontService = new ConfiguracaoUsuarioFacade();

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> configuracaoUsuarioFrontService.saveConfigurationViewDTOList(null, List.of()));
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> configuracaoUsuarioFrontService.saveConfigurationViewDTOList("admin", null));
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> configuracaoUsuarioFrontService.saveConfigurationViewDTOList(
                        "admin",
                        Arrays.asList((ConfiguracaoUsuarioDTO) null)));

        ConfiguracaoUsuarioDTO configuracaoUsuarioDTOWithoutScope = getConfiguracaoUsuarioDTO();
        configuracaoUsuarioDTOWithoutScope.scope = "";
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> configuracaoUsuarioFrontService.saveConfigurationViewDTOList(
                        "admin",
                        List.of(configuracaoUsuarioDTOWithoutScope)));

        ConfiguracaoUsuarioDTO configuracaoUsuarioDTOWithoutParameter = getConfiguracaoUsuarioDTO();
        configuracaoUsuarioDTOWithoutParameter.parameter = null;
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> configuracaoUsuarioFrontService.saveConfigurationViewDTOList(
                        "admin",
                        List.of(configuracaoUsuarioDTOWithoutParameter)));

    }

    @Test
    public void saveConfigurationViewDTOListShouldTreatEmptyPayloadAsNoopBeforeRepository() {

        ConfiguracaoUsuarioFacade configuracaoUsuarioFrontService = new ConfiguracaoUsuarioFacade();

        Assertions.assertDoesNotThrow(
                () -> configuracaoUsuarioFrontService.saveConfigurationViewDTOList(
                        "admin",
                        List.of()));

    }

    @Test
    public void saveConfigurationViewDTOListShouldRejectNullSavedSnapshot() throws Exception {

        ConfiguracaoUsuarioFacade configuracaoUsuarioFrontService = new ConfiguracaoUsuarioFacade();
        injetaMapperERepositoryParaSaveAllNulo(configuracaoUsuarioFrontService);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> configuracaoUsuarioFrontService.saveConfigurationViewDTOList(
                        "admin",
                        List.of(getConfiguracaoUsuarioDTO())));

        Assertions.assertEquals(
                "Saved user configuration collection is required.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void saveConfigurationViewDTOListShouldRejectPartialSavedSnapshot() throws Exception {

        ConfiguracaoUsuarioFacade configuracaoUsuarioFrontService =
                new ConfiguracaoUsuarioFacade();
        injetaMapperERepositoryParaSaveAllRetornando(
                configuracaoUsuarioFrontService,
                List.of(getConfiguracaoUsuarioEntidade(
                        "admin",
                        "planning-book",
                        "last-view")));

        ConfiguracaoUsuarioDTO configuracaoUsuarioDTO =
                getConfiguracaoUsuarioDTO();
        ConfiguracaoUsuarioDTO outraConfiguracaoUsuarioDTO =
                getConfiguracaoUsuarioDTO();
        outraConfiguracaoUsuarioDTO.parameter = "last-filter";

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> configuracaoUsuarioFrontService.saveConfigurationViewDTOList(
                        "admin",
                        List.of(
                                configuracaoUsuarioDTO,
                                outraConfiguracaoUsuarioDTO)));

        Assertions.assertEquals(
                "Saved user configuration collection size 1 differs from expected size 2.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void userInterfacePreferencesShouldReadDefaultAndPersistThemeThroughExistingConfigurationBatch()
            throws Exception {

        ConfiguracaoUsuarioFacade serviceComTemaPadrao =
                criaConfiguracaoUsuarioFrontServiceParaListagem(List.of(), List.of());

        Assertions.assertEquals(
                ConfiguracaoUsuarioFacade.DARK_THEME_MODE,
                serviceComTemaPadrao.getUserInterfacePreferencesDTO("admin").themeMode);

        ConfiguracaoUsuarioFacade configuracaoUsuarioFrontService =
                new ConfiguracaoUsuarioFacade();
        ConfiguracaoUsuarioRepository configuracaoUsuarioRepository =
                Mockito.mock(ConfiguracaoUsuarioRepository.class);
        ConfiguracaoUsuarioAutoMapper configuracaoUsuarioAutoMapper =
                Mockito.mock(ConfiguracaoUsuarioAutoMapper.class);
        ConfiguracaoUsuario configuracaoUsuario = getConfiguracaoUsuarioEntidade(
                "admin",
                ConfiguracaoUsuarioFacade.USER_INTERFACE_SCOPE,
                ConfiguracaoUsuarioFacade.VISUAL_THEME_MODE_PARAMETER);
        ConfiguracaoUsuarioDTO configuracaoUsuarioDTO = getConfiguracaoUsuarioDTOListagem(
                "admin",
                ConfiguracaoUsuarioFacade.USER_INTERFACE_SCOPE,
                ConfiguracaoUsuarioFacade.VISUAL_THEME_MODE_PARAMETER);
        configuracaoUsuarioDTO.parameterValue = ConfiguracaoUsuarioFacade.LIGHT_THEME_MODE;

        Mockito.when(configuracaoUsuarioAutoMapper.converteListDTOs(Mockito.any()))
                .thenReturn(List.of(configuracaoUsuario));
        Mockito.when(configuracaoUsuarioRepository.saveAll(Mockito.any()))
                .thenReturn(List.of(configuracaoUsuario));
        Mockito.when(configuracaoUsuarioRepository
                        .findByConfiguracaoUsuarioCompositeKeyUserIdAndConfiguracaoUsuarioCompositeKeyTema(
                                "admin",
                                ConfiguracaoUsuarioFacade.USER_INTERFACE_SCOPE))
                .thenReturn(List.of(configuracaoUsuario));
        Mockito.when(configuracaoUsuarioAutoMapper.converteListEntidades(List.of(configuracaoUsuario)))
                .thenReturn(List.of(configuracaoUsuarioDTO));
        setField(
                configuracaoUsuarioFrontService,
                "configuracaoUsuarioRepository",
                configuracaoUsuarioRepository);
        setField(
                configuracaoUsuarioFrontService,
                "configuracaoUsuarioAutoMapper",
                configuracaoUsuarioAutoMapper);

        UserInterfacePreferencesDTO request = new UserInterfacePreferencesDTO();
        request.themeMode = ConfiguracaoUsuarioFacade.LIGHT_THEME_MODE;

        UserInterfacePreferencesDTO response =
                configuracaoUsuarioFrontService.saveUserInterfacePreferencesDTO("admin", request);

        Assertions.assertEquals(ConfiguracaoUsuarioFacade.LIGHT_THEME_MODE, response.themeMode);
        Assertions.assertEquals(
                List.of(
                        ConfiguracaoUsuarioFacade.DARK_THEME_MODE,
                        ConfiguracaoUsuarioFacade.LIGHT_THEME_MODE),
                response.availableThemeModes);
        Mockito.verify(configuracaoUsuarioRepository).saveAll(Mockito.any());
        Mockito.verify(configuracaoUsuarioRepository)
                .findByConfiguracaoUsuarioCompositeKeyUserIdAndConfiguracaoUsuarioCompositeKeyTema(
                        "admin",
                        ConfiguracaoUsuarioFacade.USER_INTERFACE_SCOPE);

    }

    private static ConfiguracaoUsuarioDTO getConfiguracaoUsuarioDTO() {

        ConfiguracaoUsuarioDTO configuracaoUsuarioDTO = new ConfiguracaoUsuarioDTO();
        configuracaoUsuarioDTO.scope = "planning-book";
        configuracaoUsuarioDTO.parameter = "last-view";
        configuracaoUsuarioDTO.parameterValue = "Demand Planning Book";

        return configuracaoUsuarioDTO;

    }

    private static ConfiguracaoUsuarioDTO getConfiguracaoUsuarioDTOListagem(
            String userId,
            String scope,
            String parameter) {

        ConfiguracaoUsuarioDTO configuracaoUsuarioDTO = getConfiguracaoUsuarioDTO();
        configuracaoUsuarioDTO.userId = userId;
        configuracaoUsuarioDTO.scope = scope;
        configuracaoUsuarioDTO.parameter = parameter;
        return configuracaoUsuarioDTO;

    }

    private static ConfiguracaoUsuario getConfiguracaoUsuarioEntidade(
            String userId,
            String tema,
            String parametro) {

        return new ConfiguracaoUsuario(
                new ConfiguracaoUsuario.ConfiguracaoUsuarioCompositeKey(
                        userId,
                        tema,
                        parametro));

    }

    private static ConfiguracaoUsuarioFacade criaConfiguracaoUsuarioFrontServiceParaListagem(
            List<ConfiguracaoUsuario> configuracaoUsuarioList,
            List<ConfiguracaoUsuarioDTO> configuracaoUsuarioDTOList) throws Exception {

        ConfiguracaoUsuarioFacade configuracaoUsuarioFrontService =
                new ConfiguracaoUsuarioFacade();

        ConfiguracaoUsuarioRepository configuracaoUsuarioRepository =
                (ConfiguracaoUsuarioRepository) Proxy.newProxyInstance(
                        ConfiguracaoUsuarioRepository.class.getClassLoader(),
                        new Class<?>[]{ConfiguracaoUsuarioRepository.class},
                        (proxy, method, args) -> {
                            if ("findByConfiguracaoUsuarioCompositeKeyUserIdAndConfiguracaoUsuarioCompositeKeyTema".equals(method.getName())) {
                                return configuracaoUsuarioList;
                            }
                            if ("toString".equals(method.getName())) {
                                return "ConfiguracaoUsuarioRepository listagem test double";
                            }
                            if ("hashCode".equals(method.getName())) {
                                return System.identityHashCode(proxy);
                            }
                            if ("equals".equals(method.getName())) {
                                return proxy == args[0];
                            }
                            throw new AssertionError(
                                    "Repository method should not be called by listing snapshot test: "
                                            + method.getName());
                        });

        ConfiguracaoUsuarioAutoMapper configuracaoUsuarioAutoMapper =
                (ConfiguracaoUsuarioAutoMapper) Proxy.newProxyInstance(
                        ConfiguracaoUsuarioAutoMapper.class.getClassLoader(),
                        new Class<?>[]{ConfiguracaoUsuarioAutoMapper.class},
                        (proxy, method, args) -> {
                            if ("converteListEntidades".equals(method.getName())) {
                                return configuracaoUsuarioDTOList;
                            }
                            if ("toString".equals(method.getName())) {
                                return "ConfiguracaoUsuarioAutoMapper listagem test double";
                            }
                            if ("hashCode".equals(method.getName())) {
                                return System.identityHashCode(proxy);
                            }
                            if ("equals".equals(method.getName())) {
                                return proxy == args[0];
                            }
                            throw new AssertionError(
                                    "Mapper method should not be called by listing snapshot test: "
                                            + method.getName());
                        });

        setField(
                configuracaoUsuarioFrontService,
                "configuracaoUsuarioRepository",
                configuracaoUsuarioRepository);
        setField(
                configuracaoUsuarioFrontService,
                "configuracaoUsuarioAutoMapper",
                configuracaoUsuarioAutoMapper);
        return configuracaoUsuarioFrontService;

    }

    @SuppressWarnings("unchecked")
    private static void injetaMapperERepositoryParaSaveAllNulo(
            ConfiguracaoUsuarioFacade configuracaoUsuarioFrontService) throws Exception {

        injetaMapperERepositoryParaSaveAllRetornando(
                configuracaoUsuarioFrontService,
                null);

    }

    @SuppressWarnings("unchecked")
    private static void injetaMapperERepositoryParaSaveAllRetornando(
            ConfiguracaoUsuarioFacade configuracaoUsuarioFrontService,
            List<ConfiguracaoUsuario> configuracaoUsuarioListSalva) throws Exception {

        ConfiguracaoUsuarioAutoMapper configuracaoUsuarioAutoMapper =
                (ConfiguracaoUsuarioAutoMapper) Proxy.newProxyInstance(
                        ConfiguracaoUsuarioAutoMapper.class.getClassLoader(),
                        new Class<?>[]{ConfiguracaoUsuarioAutoMapper.class},
                        (proxy, method, args) -> {
                            if ("converteListDTOs".equals(method.getName())) {
                                Collection<ConfiguracaoUsuarioDTO> configuracaoUsuarioDTOs =
                                        (Collection<ConfiguracaoUsuarioDTO>) args[0];
                                return configuracaoUsuarioDTOs.stream()
                                        .map(configuracaoUsuarioDTO ->
                                                new ConfiguracaoUsuario(
                                                        new ConfiguracaoUsuario.ConfiguracaoUsuarioCompositeKey(
                                                                configuracaoUsuarioDTO.userId,
                                                                configuracaoUsuarioDTO.scope,
                                                                configuracaoUsuarioDTO.parameter)))
                                        .toList();
                            }
                            if ("toString".equals(method.getName())) {
                                return "ConfiguracaoUsuarioAutoMapper test double";
                            }
                            if ("hashCode".equals(method.getName())) {
                                return System.identityHashCode(proxy);
                            }
                            if ("equals".equals(method.getName())) {
                                return proxy == args[0];
                            }
                            throw new AssertionError(
                                    "Mapper method should not be called by saved snapshot test: "
                                            + method.getName());
                        });

        ConfiguracaoUsuarioRepository configuracaoUsuarioRepository =
                (ConfiguracaoUsuarioRepository) Proxy.newProxyInstance(
                        ConfiguracaoUsuarioRepository.class.getClassLoader(),
                        new Class<?>[]{ConfiguracaoUsuarioRepository.class},
                        (proxy, method, args) -> {
                            if ("saveAll".equals(method.getName())) {
                                return configuracaoUsuarioListSalva;
                            }
                            if ("toString".equals(method.getName())) {
                                return "ConfiguracaoUsuarioRepository test double";
                            }
                            if ("hashCode".equals(method.getName())) {
                                return System.identityHashCode(proxy);
                            }
                            if ("equals".equals(method.getName())) {
                                return proxy == args[0];
                            }
                            throw new AssertionError(
                                    "Repository method should not be called by saved snapshot test: "
                                            + method.getName());
                        });

        setField(
                configuracaoUsuarioFrontService,
                "configuracaoUsuarioAutoMapper",
                configuracaoUsuarioAutoMapper);
        setField(
                configuracaoUsuarioFrontService,
                "configuracaoUsuarioRepository",
                configuracaoUsuarioRepository);

    }

    private static void setField(
            Object target,
            String fieldName,
            Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

}
