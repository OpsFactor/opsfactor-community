package com.opsfactor.community.capability.transactionaldata.sales.saleshistory.service;

import com.opsfactor.community.capability.transactionaldata.sales.sellout.repository.SelloutRepository;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Testa a borda Community de metadados do historico de vendas.
 *
 * <p>O service deve consultar apenas sell-out. Sell-in e pedidos sao valores
 * aceitos no enum compartilhado para payloads transicionais, mas precisam
 * falhar antes de qualquer repository Community, pois suas entidades e
 * projections pertencem ao Enterprise.</p>
 */
class SalesServiceCommunityContractTest {

    @Test
    void selloutRepositoryShouldBeExplicitAutowiredBean() throws Exception {

        Field selloutRepositoryField = SalesService.class.getDeclaredField("selloutRepository");

        Assertions.assertNotNull(selloutRepositoryField.getAnnotation(Autowired.class));

    }

    @Test
    void getPrimeiraDataComHistoricoVendasRegistradoShouldReadCommunitySellout() {

        LocalDateTime dataPrimeiroSellout = LocalDateTime.of(2024, 1, 15, 0, 0);
        AtomicInteger chamadasPrimeiroSellout = new AtomicInteger();
        AtomicInteger chamadasUltimoSellout = new AtomicInteger();
        SelloutRepository selloutRepository = criaSelloutRepositoryProxy(
                dataPrimeiroSellout,
                null,
                chamadasPrimeiroSellout,
                chamadasUltimoSellout);
        SalesService salesService = criaSalesService(selloutRepository);

        Assertions.assertEquals(
                dataPrimeiroSellout,
                salesService.getPrimeiraDataComHistoricoVendasRegistrado(
                        Constantes.TipoDocumentoVenda.SELLOUT));
        Assertions.assertEquals(1, chamadasPrimeiroSellout.get());
        Assertions.assertEquals(0, chamadasUltimoSellout.get());

    }

    @Test
    void getUltimaDataComHistoricoVendasRegistradoShouldReadCommunitySellout() {

        LocalDateTime dataUltimoSellout = LocalDateTime.of(2024, 6, 30, 0, 0);
        AtomicInteger chamadasPrimeiroSellout = new AtomicInteger();
        AtomicInteger chamadasUltimoSellout = new AtomicInteger();
        SelloutRepository selloutRepository = criaSelloutRepositoryProxy(
                null,
                dataUltimoSellout,
                chamadasPrimeiroSellout,
                chamadasUltimoSellout);
        SalesService salesService = criaSalesService(selloutRepository);

        Assertions.assertEquals(
                dataUltimoSellout,
                salesService.getUltimaDataComHistoricoVendasRegistrado(
                        Constantes.TipoDocumentoVenda.SELLOUT));
        Assertions.assertEquals(0, chamadasPrimeiroSellout.get());
        Assertions.assertEquals(1, chamadasUltimoSellout.get());

    }

    @Test
    void getPrimeiraDataComHistoricoVendasRegistradoShouldRejectNullDocumentTypeBeforeRepository() {

        SalesService salesService = criaSalesService(null);

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> salesService.getPrimeiraDataComHistoricoVendasRegistrado(null));

    }

    @Test
    void getUltimaDataComHistoricoVendasRegistradoShouldRejectEnterpriseDocumentTypeBeforeRepository() {

        SalesService salesService = criaSalesService(null);

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> salesService.getUltimaDataComHistoricoVendasRegistrado(
                        Constantes.TipoDocumentoVenda.SELLIN));

    }

    private SelloutRepository criaSelloutRepositoryProxy(
            LocalDateTime dataPrimeiroSellout,
            LocalDateTime dataUltimoSellout,
            AtomicInteger chamadasPrimeiroSellout,
            AtomicInteger chamadasUltimoSellout) {

        return (SelloutRepository) Proxy.newProxyInstance(
                SelloutRepository.class.getClassLoader(),
                new Class<?>[] { SelloutRepository.class },
                (proxy, method, args) -> {

                    return switch (method.getName()) {
                        case "customFindPrimeiroSellout" -> {
                            chamadasPrimeiroSellout.incrementAndGet();
                            yield dataPrimeiroSellout;
                        }
                        case "customFindUltimoSellout" -> {
                            chamadasUltimoSellout.incrementAndGet();
                            yield dataUltimoSellout;
                        }
                        case "toString" -> "SelloutRepositoryProxy";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(
                                "Metodo nao esperado no teste: " + method.getName());
                    };

                });

    }

    private SalesService criaSalesService(SelloutRepository selloutRepository) {

        SalesService salesService = new SalesService();
        ReflectionTestUtils.setField(salesService, "selloutRepository", selloutRepository);
        return salesService;

    }

}
