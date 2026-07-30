package com.opsfactor.community.capability.configuration.user.repository;

import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredViewKeyFigure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Repository batch das preferências de Key Figure por Configured View.
 */
@Repository
public interface ConfiguredViewKeyFigureRepository extends JpaRepository<ConfiguredViewKeyFigure, ConfiguredViewKeyFigure.Key> {

    /**
     * Carrega a fotografia das Key Figures de várias views de uma vez.
     *
     * <p>O fetch da view evita lazy loading durante o agrupamento do DTO e da
     * projection. A Key Figure em si é apenas um id escalar, resolvido pela
     * edição Community/Enterprise apropriada depois desta consulta.</p>
     */
    @Query("SELECT cvkf FROM ConfiguredViewKeyFigure cvkf "
            + "JOIN FETCH cvkf.key.configuredView "
            + "WHERE cvkf.key.configuredView IN :configuredViews")
    List<ConfiguredViewKeyFigure> findAllByConfiguredViewIn(Collection<ConfiguredView> configuredViews);

    /**
     * Remove em lote a fotografia de Key Figures de uma view antes de remover
     * sua entidade pai.
     *
     * <p>A relacao e propositalmente unidirecional filho -> view e nao usa
     * cascade/orphan removal no agregado Community. Portanto a exclusao da
     * view precisa eliminar explicitamente as filhas para respeitar a chave
     * estrangeira sem carregar uma colecao lazy.</p>
     */
    @Modifying
    @Query("DELETE FROM ConfiguredViewKeyFigure cvkf WHERE cvkf.key.configuredView = :configuredView")
    void deleteAllByConfiguredView(@Param("configuredView") ConfiguredView configuredView);

}
