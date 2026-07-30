package com.opsfactor.community.capability.planningbook.keyfigure.projection;

import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureInterface;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandard;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardEnum;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

/**
 * Garante que o hook de materializacao privada nao cria fonte oculta no
 * Community antes da soma de Direct Demand.
 */
class KeyFigureProjectionFactoryPrivateMaterializationCommunityTest {

    @Test
    void communityPrivateMaterializationHookShouldRemainNoOp() {

        KeyFigureProjection keyFigureProjection = Mockito.mock(KeyFigureProjection.class);
        ExposedKeyFigureProjectionFactory keyFigureProjectionFactory =
                new ExposedKeyFigureProjectionFactory();

        keyFigureProjectionFactory.exposeMaterializaKeyFiguresDemandPlanningAntesDaTotalizacao(
                keyFigureProjection,
                List.of(new KeyFigureStandard(KeyFigureStandardEnum.BASELINE)));

        Mockito.verifyNoInteractions(keyFigureProjection);

    }

    private static class ExposedKeyFigureProjectionFactory extends KeyFigureProjectionFactory {

        private void exposeMaterializaKeyFiguresDemandPlanningAntesDaTotalizacao(
                KeyFigureProjection keyFigureProjection,
                List<KeyFigureInterface> keyFiguresDemandPlanningNecessarias) {

            materializaKeyFiguresDemandPlanningAntesDaTotalizacao(
                    keyFigureProjection,
                    keyFiguresDemandPlanningNecessarias);

        }

    }

}
