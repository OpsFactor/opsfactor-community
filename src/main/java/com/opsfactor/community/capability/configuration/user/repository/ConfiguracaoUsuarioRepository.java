package com.opsfactor.community.capability.configuration.user.repository;


import com.opsfactor.community.capability.configuration.user.domain.ConfiguracaoUsuario;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository JPA de ConfiguracaoUsuarioRepository.
 */
@Repository
public interface ConfiguracaoUsuarioRepository extends JpaRepository<ConfiguracaoUsuario,ConfiguracaoUsuario.ConfiguracaoUsuarioCompositeKey> {
	
    public List<ConfiguracaoUsuario> findByConfiguracaoUsuarioCompositeKeyUserId(String userId);
    
    public List<ConfiguracaoUsuario> findByConfiguracaoUsuarioCompositeKeyUserIdAndConfiguracaoUsuarioCompositeKeyTemaIn(String userId, Set<String> temas);
    public List<ConfiguracaoUsuario> findByConfiguracaoUsuarioCompositeKeyUserIdAndConfiguracaoUsuarioCompositeKeyTema(String userId, String tema);
        
}
