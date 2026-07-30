package com.opsfactor.community.capability.lowlevelcode.facade;

import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.VersaoMalhaRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Contratos Community da visualizacao tecnica de Low Level Code.
 *
 * <p>Os testes deixam factories e mappers ausentes de proposito. Ids e lookups
 * obrigatorios precisam falhar antes de montar projections de parametros,
 * malha, materiais ou DFUs.</p>
 */
public class LowLevelCodeFacadeCommunityContractTest {

    @Test
    public void lowLevelCodeEntrypointsShouldRejectMissingRequiredParametersBeforeRepositories() {

        LowLevelCodeFacade lowLevelCodeFrontService = new LowLevelCodeFacade();

        IllegalArgumentException missingNetworkException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> lowLevelCodeFrontService.getLowLevelCodePorDFU(
                        " ",
                        LocalDateTime.of(2026, 1, 1, 0, 0)));
        Assertions.assertEquals(
                "Supply Network Version is null or empty",
                missingNetworkException.getMessage());

        IllegalArgumentException missingReferenceDateException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> lowLevelCodeFrontService.getLowLevelCodePorDFU(
                        "NETWORK",
                        null));
        Assertions.assertEquals(
                "Low Level Code reference date is required",
                missingReferenceDateException.getMessage());

        IllegalArgumentException missingMaterialException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> lowLevelCodeFrontService.getLowLevelCodeDTO(
                        "NETWORK",
                        null));
        Assertions.assertEquals(
                "Material Id is null or empty",
                missingMaterialException.getMessage());

        IllegalArgumentException missingCircularDateException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> lowLevelCodeFrontService.getDFUMalhaCircularDTOSet(
                        "NETWORK",
                        null));
        Assertions.assertEquals(
                "Low Level Code circular-network reference date is required",
                missingCircularDateException.getMessage());

    }

    private static VersaoMalhaRepository getVersaoMalhaRepositoryStub(
            Optional<VersaoMalha> versaoMalhaOptional) {

        return (VersaoMalhaRepository) Proxy.newProxyInstance(
                VersaoMalhaRepository.class.getClassLoader(),
                new Class<?>[]{VersaoMalhaRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return versaoMalhaOptional;
                    }
                    if ("toString".equals(method.getName())) {
                        return "VersaoMalhaRepositoryStub";
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao esperado no proxy de teste: " + method.getName());
                });

    }

    private static ProdutoRepository getProdutoRepositoryStub(
            Optional<Produto> materialOptional) {

        return (ProdutoRepository) Proxy.newProxyInstance(
                ProdutoRepository.class.getClassLoader(),
                new Class<?>[]{ProdutoRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return materialOptional;
                    }
                    if ("toString".equals(method.getName())) {
                        return "ProdutoRepositoryStub";
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao esperado no proxy de teste: " + method.getName());
                });

    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

}
