package com.opsfactor.community.capability.configuration.user.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Preferência persistida de apresentação e edição de uma Key Figure em uma
 * Configured View.
 *
 * <p>A relação é deliberadamente filha -> view, lazy e sem coleção inversa na
 * entidade Community. A leitura de várias views usa o repository batch e a
 * sincronização é feita explicitamente pelo service, evitando cascade,
 * orphan-removal e carregamento lazy por view.</p>
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(of = "key")
public class ConfiguredViewKeyFigure {

    @EmbeddedId
    @NonNull
    private Key key;

    /** Posição solicitada na grade; ausência preserva o default legado 100. */
    private Integer position;

    /** Override opcional de edição; ausência preserva o default legado true. */
    private Boolean allowChanges;

    public String getKeyFigureId() {

        return key.getKeyFigureId();

    }

    public boolean getAllowChanges() {

        return allowChanges == null || allowChanges;

    }

    public Boolean getAllowChangesCadastrado() {

        return allowChanges;

    }

    public int getPosition() {

        return position == null ? 100 : position;

    }

    public Integer getPositionCadastrado() {

        return position;

    }

    /**
     * Chave persistida por view e identificador público da Key Figure.
     */
    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @RequiredArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {

        @NonNull
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        private ConfiguredView configuredView;

        @NonNull
        private String keyFigureId;

    }

}
