package com.opsfactor.community.capability.planningbook.keyfigure.service;

import com.opsfactor.community.capability.configuration.user.projection.ConfiguredViewProjection;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureInterface;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardSupplyPlanning;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandard;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardMonetariaDemandPlanning;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardEnum;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.MetodosUtilidade;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Catalogo operacional de Key Figures conhecidas pelo OpsFactor Community.
 *
 * <p>Este service fica no model porque projections, DTOs e services de
 * Planning Book precisam resolver os mesmos identificadores persistidos sem
 * depender de uma camada web. O Community trabalha apenas com KFs padrao
 * estaticas; KFs customizadas, workflow stage, selecao dinamica de key figure
 * e apresentacao agregada pertencem ao OpsFactor Enterprise e devem falhar com
 * {@link RequiresEnterpriseVersionException} quando chegarem a esta borda.</p>
 */
@Service
public class KeyFigureService {

    /**
     * Cria a implementacao fina de uma Key Figure padrao.
     */
    public static KeyFigureStandard getKeyFigureStandardDeKeyFigureStandardEnum(KeyFigureStandardEnum keyFigureStandardEnum) {

        return switch (keyFigureStandardEnum) {
            case VENDAS_GROSS, VENDAS_NET ->
                    new KeyFigureStandardMonetariaDemandPlanning(keyFigureStandardEnum);
            default -> new KeyFigureStandard(keyFigureStandardEnum);
        };

    }

    /**
     * Resolve uma Key Figure padrao pelo identificador tecnico do enum.
     */
    public static Optional<KeyFigureStandard> getKeyFigureStandardDeId(String id) {

        Optional<KeyFigureStandardEnum> optionalKeyFigureStandardEnum = Arrays.stream(KeyFigureStandardEnum.values())
                .filter(keyFigureStandard -> keyFigureStandard.toString().equals(id))
                .findFirst();

        return optionalKeyFigureStandardEnum
                .map(KeyFigureService::getKeyFigureStandardDeKeyFigureStandardEnum);

    }

    /**
     * Retorna as KFs que compoem a demanda direta futura no Community.
     *
     * <p>No recorte Community, a demanda direta editavel nasce somente de
     * Baseline + Demand Adjustment. Uplift, New Products, carteira, workflow e
     * KFs customizadas ficam fora desta composicao para impedir que o Planning
     * Book salve uma linha que o backend aberto nao sabe recalcular.</p>
     */
    public List<KeyFigureInterface> getKeyFiguresDpQueCompoemDemandaDireta() {

        return new ArrayList<>(getKeyFiguresStandardDpQueCompoemDemandaDireta());

    }

    /**
     * Normaliza uma tentativa de ajuste no totalizador de demanda direta.
     *
     * <p>A tela pode mandar `Direct Demand` como a linha editada. No Community,
     * esse delta deve ser persistido em `Demand Adjustment`, mantendo o
     * totalizador como leitura derivada de Baseline + Adjustment. Outras KFs
     * permanecem inalteradas para que validacoes posteriores possam rejeitar o
     * que nao for editavel nesta edicao.</p>
     */
    public KeyFigureInterface getKeyFigureAjusteDemandaCommunity(KeyFigureInterface keyFigureAjuste) {

        if (keyFigureAjuste instanceof KeyFigureStandard keyFigureAjusteStandard) {
            switch (keyFigureAjusteStandard.getKeyFigureStandardEnum()) {
                case DEMANDA_DIRETA_TOTAL_DP -> {
                    return getKeyFigureStandardDeKeyFigureStandardEnum(KeyFigureStandardEnum.AJUSTE_DEMANDA);
                }
            }
        }
        return keyFigureAjuste;

    }

    /**
     * Subconjunto padrao usado pela composicao de demanda direta Community.
     */
    public List<KeyFigureStandard> getKeyFiguresStandardDpQueCompoemDemandaDireta() {

        return List.of(
                getKeyFigureStandardDeKeyFigureStandardEnum(KeyFigureStandardEnum.BASELINE),
                getKeyFigureStandardDeKeyFigureStandardEnum(KeyFigureStandardEnum.AJUSTE_DEMANDA));

    }

    /**
     * Resolve uma Key Figure conhecida a partir do id persistido.
     *
     * <p>O metodo aceita tanto KFs standard simples quanto KFs tipadas do
     * Supply Planning Book, como `Planned Production-Working Plan`. Qualquer id
     * que nao represente uma KF padrao conhecida e tratado como custom key
     * figure Enterprise, pois o Community nao possui cadastro/execucao dinamica
     * dessas series.</p>
     */
    public KeyFigureInterface getKeyFigureDeId(String id) {

        Optional<KeyFigureStandard> optionalKeyFigureStandard = getKeyFigureStandardDeDescricao(id);

        return optionalKeyFigureStandard
                .map(keyFigureStandard -> (KeyFigureInterface) keyFigureStandard)
                .orElseThrow(() -> new RequiresEnterpriseVersionException("Custom key figures"));

    }

    /**
     * Resolve uma Key Figure padrao pelo id ou descricao publica.
     *
     * <p>Supply Planning Book pode persistir a mesma KF em diferentes planos,
     * por isso a tentativa tipada vem antes do parser sem tipo de plano. Se a
     * string nao pertencer ao subconjunto tipado permitido, seguimos para o
     * catalogo standard simples.</p>
     */
    public static Optional<KeyFigureStandard> getKeyFigureStandardDeDescricao(String descricao) {

        try {
            KeyFigureStandardSupplyPlanning keyFigureStandardSupplyPlanning = new KeyFigureStandardSupplyPlanning(descricao);
            if (keyFigureStandardSupplyPlanning.getId().equals(descricao)
                    || keyFigureStandardSupplyPlanning.getDescricao().equals(descricao)) {
                return Optional.of(keyFigureStandardSupplyPlanning);
            }
        } catch (IllegalArgumentException exception) {
            // Se a descrição não representar uma key figure tipada de supply, o fluxo
            // segue para o parser standard sem contaminar outros contextos do sistema.
        }

        return getKeyFigureStandardDeDescricaoSemTipoPlano(descricao);

    }

    /**
     * Resolve KFs standard simples pelo nome tecnico ou pelo label JSON.
     */
    public static Optional<KeyFigureStandard> getKeyFigureStandardDeDescricaoSemTipoPlano(String descricao) {

        Optional<KeyFigureStandardEnum> optionalKeyFigureStandardEnum = Arrays.stream(KeyFigureStandardEnum.values())
                .filter(keyFigureStandard -> keyFigureStandard.name().equals(descricao) || MetodosUtilidade.getValorJsonPropertyDeEnum(keyFigureStandard).equals(descricao))
                .findFirst();

        return optionalKeyFigureStandardEnum
                .map(KeyFigureService::getKeyFigureStandardDeKeyFigureStandardEnum);

    }

    /**
     * Resolve uma Key Figure conhecida a partir da descricao apresentada na UI.
     */
    public KeyFigureInterface getKeyFigureDeDescricao(String descricao) {

        Optional<KeyFigureStandard> optionalKeyFigureStandard = getKeyFigureStandardDeDescricao(descricao);

        return optionalKeyFigureStandard
                .map(keyFigureStandard -> (KeyFigureInterface) keyFigureStandard)
                .orElseThrow(() -> new RequiresEnterpriseVersionException("Custom key figures"));

    }

    /**
     * Retorna as KFs que compoem a demanda direta para uma view configurada.
     *
     * <p>O parametro fica na assinatura por compatibilidade com o fluxo
     * historico e com overlays Enterprise. No Community, views nao podem
     * selecionar KFs dinamicas, entao a projection da view nao altera o
     * subconjunto Baseline + Demand Adjustment.</p>
     */
    public List<KeyFigureInterface> getKeyFiguresQueCompoemDemandaDireta(ConfiguredViewProjection configuredViewProjection) {

        return getKeyFiguresDpQueCompoemDemandaDireta();

    }

}
