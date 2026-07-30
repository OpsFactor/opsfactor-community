package com.opsfactor.community.capability.configuration.user.repository;


import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository JPA de ConfiguredViewRepository.
 */
@Repository
public interface ConfiguredViewRepository extends JpaRepository<ConfiguredView,ConfiguredView.ConfiguredViewCompositeKey> {
	
    @Query("SELECT DISTINCT cv FROM ConfiguredView cv "
            + "WHERE cv.configuredViewCompositeKey.userId = :userId "
            + "AND cv.configuredViewCompositeKey.nomeView = :nomeView "
            + "AND cv.configuredViewCompositeKey.tipoView = :tipoView")
    public Optional<ConfiguredView> findByConfiguredViewCompositeKeyUserIdAndConfiguredViewCompositeKeyNomeViewAndConfiguredViewCompositeKeyTipoView(
            String userId, String nomeView, ConfiguredView.TipoView tipoView);
    
    /**
     * Remove a view configurada do usuario pelo nome e tipo informados.
     */
    public void removeByConfiguredViewCompositeKeyUserIdAndConfiguredViewCompositeKeyNomeViewAndConfiguredViewCompositeKeyTipoView(
            String userId, String nomeView, ConfiguredView.TipoView tipoView);
    
    /**
     * Carrega a fotografia administrativa de views de um usuario para um
     * Planning Book, incluindo a UOM configurada quando houver override.
     *
     * <p>O {@code LEFT JOIN FETCH} preserva views sem UOM propria: nesses
     * casos o mapper continua resolvendo a UOM global pelo tipo da view, sem
     * excluir a linha da listagem. Como a conversao de cada view le a UOM,
     * o fetch evita um select lazy adicional por item.</p>
     */
    @Query("SELECT DISTINCT cv FROM ConfiguredView cv "
            + "LEFT JOIN FETCH cv.unidadeMedidaView "
            + "WHERE cv.configuredViewCompositeKey.userId = :userId "
            + "AND cv.configuredViewCompositeKey.tipoView = :tipoView")
    public List<ConfiguredView> findByConfiguredViewCompositeKeyUserIdAndConfiguredViewCompositeKeyTipoView(
            String userId, ConfiguredView.TipoView tipoView);
    
    @Query("SELECT DISTINCT cv FROM ConfiguredView cv "
            + "WHERE cv.configuredViewCompositeKey.userId IN :userIds")
    public List<ConfiguredView> customFindByUserIdIn(
            Collection<String> userIds);
    
    @Query("SELECT DISTINCT cv FROM ConfiguredView cv")
    public List<ConfiguredView> customFindAll();

    @Query("SELECT DISTINCT cv FROM ConfiguredView cv "
            + "WHERE cv.configuredViewCompositeKey.tipoView = :tipoView")
    public List<ConfiguredView> customFindByConfiguredViewCompositeKeyTipoView(
            ConfiguredView.TipoView tipoView);
    
}
