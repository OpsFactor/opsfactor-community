package com.opsfactor.community.capability.configuration.domain;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommunityNewProductStatusTest {

    @Test
    void produtoComDataIntroducaoNaoFicaNovoQuandoJanelaCommunityEZero() {

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        Produto produto = new Produto();
        produto.setDataIntroducao(LocalDateTime.of(2026, 1, 10, 0, 0));

        assertEquals(
                Constantes.StatusProduto.NAO_LANCADO,
                produto.getStatusProduto(LocalDateTime.of(2026, 1, 9, 23, 59), parametrosGlobais));
        assertEquals(
                Constantes.StatusProduto.REGULAR,
                produto.getStatusProduto(LocalDateTime.of(2026, 1, 10, 0, 0), parametrosGlobais));

    }

    @Test
    void locationComDataIntroducaoNaoFicaNovaQuandoJanelaCommunityEZero() {

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        Location location = new Location();
        location.setDataIntroducao(LocalDate.of(2026, 1, 10));

        assertEquals(
                Constantes.StatusProduto.NAO_LANCADO,
                location.getStatusLocation(LocalDate.of(2026, 1, 9), parametrosGlobais));
        assertEquals(
                Constantes.StatusProduto.REGULAR,
                location.getStatusLocation(LocalDate.of(2026, 1, 10), parametrosGlobais));

    }

    @Test
    void parametroMaterialLocationNaoCriaJanelaDeProdutoNovoNoCommunity() {

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        Produto produto = new Produto();
        produto.setId("MAT-1");
        Location location = new Location("LOC-1");
        ParametrosProdutoLocation parametrosProdutoLocation = new ParametrosProdutoLocation(
                new ParametrosProdutoLocation.ParametrosProdutoLocationCompositeKey(produto, location));
        parametrosProdutoLocation.setDataIntroducao(LocalDateTime.of(2026, 1, 10, 0, 0));

        assertEquals(
                Constantes.StatusProduto.NAO_LANCADO,
                parametrosProdutoLocation.getStatusProduto(LocalDateTime.of(2026, 1, 9, 23, 59), parametrosGlobais));
        assertEquals(
                Constantes.StatusProduto.REGULAR,
                parametrosProdutoLocation.getStatusProduto(LocalDateTime.of(2026, 1, 10, 0, 0), parametrosGlobais));

    }

}
