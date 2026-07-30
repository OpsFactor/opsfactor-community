package com.opsfactor.community.capability.transactionaldata.inventory.stock.facade;

import com.opsfactor.community.capability.transactionaldata.inventory.stock.domain.Estoque;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.repository.EstoqueRepository;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.facade.dto.EstoqueDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Contratos Community do service simples de estoque inicial.
 *
 * <p>Estoque inicial e permitido no Community como dado transacional basico do
 * Supply Planning heuristico. O service deve diferenciar material sem saldo de
 * snapshot quebrado do repository antes de montar DTOs para a tela/API.</p>
 */
class EstoqueServiceCommunityContractTest {

    @Test
    void estoqueRepositoryShouldBeExplicitAutowiredBean() throws Exception {

        Field estoqueRepositoryField = EstoqueService.class.getDeclaredField("estoqueRepository");

        Assertions.assertNotNull(estoqueRepositoryField.getAnnotation(Autowired.class));

    }

    @Test
    void checkMaterialStockShouldMapValidStockSnapshot() throws Exception {

        LocalDateTime dataReferencia = LocalDateTime.of(2026, 1, 1, 0, 0);
        Estoque estoque = new Estoque(
                new Estoque.EstoqueCompositeKey(
                        new Location("LOC-1"),
                        new Produto("MAT-1"),
                        dataReferencia),
                12.5d);
        EstoqueService estoqueService =
                createEstoqueService(getEstoqueRepositoryComUltimoMaterial(List.of(estoque)));

        List<EstoqueDTO> estoqueDTOList = estoqueService.checkMaterialStock("MAT-1");

        Assertions.assertEquals(1, estoqueDTOList.size());
        Assertions.assertEquals(dataReferencia, estoqueDTOList.get(0).getReference_date());
        Assertions.assertEquals("LOC-1", estoqueDTOList.get(0).getLocation_id());
        Assertions.assertEquals("MAT-1", estoqueDTOList.get(0).getMaterial_id());
        Assertions.assertEquals(12.5d, estoqueDTOList.get(0).getQuantity());

    }

    @Test
    void apagaEstoquesEntreDatasShouldRejectInvalidWindowBeforeRepository() {

        EstoqueService estoqueService = new EstoqueService();

        IllegalArgumentException dataInicialAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> estoqueService.apagaEstoquesEntreDatas(
                        null,
                        LocalDateTime.of(2026, 1, 31, 0, 0)));
        IllegalArgumentException janelaInvertidaException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> estoqueService.apagaEstoquesEntreDatas(
                        LocalDateTime.of(2026, 2, 1, 0, 0),
                        LocalDateTime.of(2026, 1, 31, 0, 0)));

        Assertions.assertEquals(
                "Initial stock deletion start date is required.",
                dataInicialAusenteException.getMessage());
        Assertions.assertEquals(
                "Initial stock deletion start date must be before or equal to end date.",
                janelaInvertidaException.getMessage());

    }

    private static EstoqueService createEstoqueService(
            EstoqueRepository estoqueRepository) throws Exception {

        EstoqueService estoqueService = new EstoqueService();
        Field estoqueRepositoryField = EstoqueService.class.getDeclaredField("estoqueRepository");
        estoqueRepositoryField.setAccessible(true);
        estoqueRepositoryField.set(estoqueService, estoqueRepository);
        return estoqueService;

    }

    private static EstoqueRepository getEstoqueRepositoryComUltimoMaterial(
            Collection<Estoque> estoqueCollection) {

        return (EstoqueRepository) Proxy.newProxyInstance(
                EstoqueRepository.class.getClassLoader(),
                new Class<?>[]{EstoqueRepository.class},
                (proxy, method, args) -> {
                    if ("findByLastMaterial".equals(method.getName())) {
                        return estoqueCollection;
                    }
                    if ("toString".equals(method.getName())) {
                        return "EstoqueRepository ultimo material para teste Community";
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao esperado no proxy de teste: " + method.getName());
                });

    }

}
