package com.opsfactor.community.capability.configuration.user.facade.mapper;

import com.opsfactor.community.capability.configuration.user.domain.ConfiguracaoUsuario;
import com.opsfactor.community.capability.configuration.user.facade.dto.ConfiguracaoUsuarioDTO;
import java.util.Collection;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct das preferencias simples de usuario.
 */
// spring : mapper pode ser extraído com @Autowired. uses : outros mappers dos quais este depende
@Mapper(componentModel = "spring")
public interface ConfiguracaoUsuarioAutoMapper {
    
    @Mapping(source = "configuracaoUsuarioCompositeKey.userId", target = "userId")
    @Mapping(source = "configuracaoUsuarioCompositeKey.tema", target = "scope")
    @Mapping(source = "configuracaoUsuarioCompositeKey.parametro", target = "parameter")
    @Mapping(source = "valorParametro", target = "parameterValue")
    public ConfiguracaoUsuarioDTO converte(ConfiguracaoUsuario configuracaoUsuario);
    
    @Mapping(source = "userId", target = "configuracaoUsuarioCompositeKey.userId")
    @Mapping(source = "scope", target = "configuracaoUsuarioCompositeKey.tema")
    @Mapping(source = "parameter", target = "configuracaoUsuarioCompositeKey.parametro")
    @Mapping(source = "parameterValue", target = "valorParametro")
    public ConfiguracaoUsuario converte(ConfiguracaoUsuarioDTO configuracaoUsuarioDTO);
    
    public List<ConfiguracaoUsuario> converteListDTOs(Collection<ConfiguracaoUsuarioDTO> collectionDTOs);
    public List<ConfiguracaoUsuarioDTO> converteListEntidades(Collection<ConfiguracaoUsuario> collectionEntidades);
    

}
