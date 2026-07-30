package com.opsfactor.community.capability.configuration.user.repository;

import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

/**
 * Contrato da fotografia de Configured Views publicada pelas listagens
 * Community de Planning Book.
 */
class ConfiguredViewRepositoryQueryContractTest {

    @Test
    void userPlanningBookSnapshotShouldFetchConfiguredUnitOfMeasureWithoutExcludingGlobalFallback()
            throws Exception {

        Method method = ConfiguredViewRepository.class.getDeclaredMethod(
                "findByConfiguredViewCompositeKeyUserIdAndConfiguredViewCompositeKeyTipoView",
                String.class,
                ConfiguredView.TipoView.class);

        Assertions.assertEquals(List.class, method.getReturnType());

        String query = method.getAnnotation(Query.class).value();
        Assertions.assertTrue(query.contains("SELECT DISTINCT cv FROM ConfiguredView cv"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH cv.unidadeMedidaView"));
        Assertions.assertTrue(query.contains("WHERE cv.configuredViewCompositeKey.userId = :userId"));
        Assertions.assertTrue(query.contains("AND cv.configuredViewCompositeKey.tipoView = :tipoView"));

    }

}
