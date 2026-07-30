package com.opsfactor.community.capability.planningbook.keyfigure.domain;

import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.MetodosUtilidade;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Key figure standard tipada por plano para permitir que a mesma KF base
 * seja exibida em múltiplas variações do supply planning book.
 */
@EqualsAndHashCode(callSuper = true, of = "tipoPlano")
public class KeyFigureStandardSupplyPlanning extends KeyFigureStandard {

    private static final Set<Constantes.TipoPlano> TIPOS_PLANO_PERMITIDOS_SUPPLY_PLANNING_BOOK = EnumSet.of(
            Constantes.TipoPlano.PLANO_IRRESTRITO,
            Constantes.TipoPlano.PLANO_RESTRITO,
            Constantes.TipoPlano.PLANO_TRABALHO);
    /*
     * Subconjunto funcional do Supply Planning Book Community. KFs de carteira,
     * ordens firmes, estoque em transito, writeoff/batch aging, custos e KFs
     * customizadas pertencem ao Enterprise e devem ser rejeitadas antes da
     * projection.
     */
    private static final Set<KeyFigureStandardEnum> KEY_FIGURES_PERMITIDAS_SUPPLY_PLANNING_BOOK = EnumSet.of(
            KeyFigureStandardEnum.DEMANDA_TOTAL,
            KeyFigureStandardEnum.DEMANDA_DIRETA_TOTAL_SNP,
            KeyFigureStandardEnum.DEMANDA_DIRETA_PLANO_DEMANDA_SNP,
            KeyFigureStandardEnum.DEMANDA_INDIRETA_TOTAL,
            KeyFigureStandardEnum.ESTOQUE_SEGURANCA,
            KeyFigureStandardEnum.ESTOQUE,
            KeyFigureStandardEnum.PRODUCAO_PLANEJADA,
            KeyFigureStandardEnum.INBOUND_PLANEJADO);

    @Getter
    private final Constantes.TipoPlano tipoPlano;

    /**
     * Constrói a key figure tipada do supply planning a partir do identificador
     * persistido na view ou da descrição apresentada ao usuário.
     * Se o tipo de plano não vier explícito, assume-se `PLANO_TRABALHO`,
     * mantendo compatibilidade com configurações antigas do supply book.
     */
    public KeyFigureStandardSupplyPlanning(String idOuDescricao) {
        this(
                getKeyFigureStandardEnumSupplyPlanning(idOuDescricao),
                getTipoPlanoSupplyPlanning(idOuDescricao));
    }

    public KeyFigureStandardSupplyPlanning(
            KeyFigureStandardEnum keyFigureStandardEnum,
            Constantes.TipoPlano tipoPlano) {
        super(keyFigureStandardEnum);

        if (!TIPOS_PLANO_PERMITIDOS_SUPPLY_PLANNING_BOOK.contains(tipoPlano)) {
            throw new IllegalArgumentException("Tipo plano " + tipoPlano + " não permitido para key figure do supply planning book");
        }
        if (!KEY_FIGURES_PERMITIDAS_SUPPLY_PLANNING_BOOK.contains(keyFigureStandardEnum)) {
            throw new IllegalArgumentException("Key Figure " + keyFigureStandardEnum + " não aceita tipo plano no supply planning book");
        }

        this.tipoPlano = tipoPlano;
    }

    @Override
    public String getId() {
        return MetodosUtilidade.getValorJsonPropertyDeEnum(getKeyFigureStandardEnum())
                + "-"
                + MetodosUtilidade.getValorJsonPropertyDeEnum(tipoPlano);
    }

    @Override
    public String getDescricao() {
        return MetodosUtilidade.getValorJsonPropertyDeEnum(getKeyFigureStandardEnum())
                + " ("
                + MetodosUtilidade.getValorJsonPropertyDeEnum(tipoPlano)
                + ")";
    }

    @Override
    public boolean getPadraoPermiteEdicao() {
        return tipoPlano.equals(Constantes.TipoPlano.PLANO_TRABALHO)
                && super.getPadraoPermiteEdicao();
    }

    private static KeyFigureStandardEnum getKeyFigureStandardEnumSupplyPlanning(String idOuDescricao) {
        String identificadorSemTipoPlano = getIdentificadorKeyFigureSemTipoPlano(idOuDescricao);

        return Arrays.stream(KeyFigureStandardEnum.values())
                .filter(KEY_FIGURES_PERMITIDAS_SUPPLY_PLANNING_BOOK::contains)
                .filter(keyFigureStandardEnum ->
                        keyFigureStandardEnum.name().equals(identificadorSemTipoPlano)
                                || MetodosUtilidade.getValorJsonPropertyDeEnum(keyFigureStandardEnum).equals(identificadorSemTipoPlano))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Key Figure " + idOuDescricao + " não aceita tipo plano no supply planning book"));
    }

    private static Constantes.TipoPlano getTipoPlanoSupplyPlanning(String idOuDescricao) {
        return getOptionalTipoPlanoExplicito(idOuDescricao)
                .orElse(Constantes.TipoPlano.PLANO_TRABALHO);
    }

    /**
     * Remove apenas o sufixo de plano explicitamente informado.
     * Se a string não trouxer sufixo, ela já representa a key figure base.
     */
    private static String getIdentificadorKeyFigureSemTipoPlano(String idOuDescricao) {
        if (idOuDescricao == null) {
            throw new IllegalArgumentException("Id/descrição da Key Figure do supply planning não pode ser nulo");
        }

        Optional<Constantes.TipoPlano> optionalTipoPlano = getOptionalTipoPlanoExplicito(idOuDescricao);
        if (optionalTipoPlano.isEmpty()) {
            return idOuDescricao;
        }

        Constantes.TipoPlano tipoPlano = optionalTipoPlano.orElseThrow(() -> new IllegalArgumentException(
                "Tipo de plano não identificado na Key Figure " + idOuDescricao));
        String descricaoTipoPlano = MetodosUtilidade.getValorJsonPropertyDeEnum(tipoPlano);
        String sufixoId = "-" + descricaoTipoPlano;
        if (idOuDescricao.endsWith(sufixoId)) {
            return idOuDescricao.substring(0, idOuDescricao.length() - sufixoId.length());
        }

        String sufixoDescricao = " (" + descricaoTipoPlano + ")";
        if (idOuDescricao.endsWith(sufixoDescricao)) {
            return idOuDescricao.substring(0, idOuDescricao.length() - sufixoDescricao.length());
        }

        throw new IllegalArgumentException("Não foi possível separar o tipo de plano da Key Figure " + idOuDescricao);
    }

    private static Optional<Constantes.TipoPlano> getOptionalTipoPlanoExplicito(String idOuDescricao) {
        for (Constantes.TipoPlano tipoPlanoIterado : TIPOS_PLANO_PERMITIDOS_SUPPLY_PLANNING_BOOK) {
            String descricaoTipoPlano = MetodosUtilidade.getValorJsonPropertyDeEnum(tipoPlanoIterado);
            if (idOuDescricao.endsWith("-" + descricaoTipoPlano)
                    || idOuDescricao.endsWith(" (" + descricaoTipoPlano + ")")) {
                return Optional.of(tipoPlanoIterado);
            }
        }

        return Optional.empty();
    }
}
