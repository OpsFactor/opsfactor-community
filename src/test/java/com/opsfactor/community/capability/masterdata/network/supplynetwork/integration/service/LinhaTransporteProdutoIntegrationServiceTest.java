package com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.service;

import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.LinhaTransporteIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.LinhaTransporteProdutoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporteProduto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.LinhaTransporteProdutoRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.LinhaTransporteRepository;
import com.opsfactor.community.platform.exception.DataUploadException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Valida o contrato usado pela infraestrutura generica de integracao:
 * saveEntityList deve devolver as entidades persistidas pelo repositorio.
 */
public class LinhaTransporteProdutoIntegrationServiceTest {

    @Test
    public void saveEntityListShouldReturnSavedTransportationLanes() {

        LinhaTransporteIntegrationService linhaTransporteIntegrationService = new LinhaTransporteIntegrationService();
        LinhaTransporteRepository linhaTransporteRepository =
                getRepositoryProxy(
                        LinhaTransporteRepository.class,
                        false);
        ReflectionTestUtils.setField(
                linhaTransporteIntegrationService,
                "linhaTransporteRepository",
                linhaTransporteRepository);

        LinhaTransporte linhaTransporte = getLinhaTransporte();

        List<LinhaTransporte> linhasTransporteSalvas =
                linhaTransporteIntegrationService.saveEntityList(List.of(linhaTransporte));

        Assertions.assertEquals(1, linhasTransporteSalvas.size());
        Assertions.assertSame(linhaTransporte, linhasTransporteSalvas.get(0));

    }

    @Test
    public void saveEntityListShouldRejectNullSavedTransportationLaneSnapshot() {

        LinhaTransporteIntegrationService linhaTransporteIntegrationService = new LinhaTransporteIntegrationService();
        LinhaTransporteRepository linhaTransporteRepository =
                getRepositoryProxy(
                        LinhaTransporteRepository.class,
                        true);
        ReflectionTestUtils.setField(
                linhaTransporteIntegrationService,
                "linhaTransporteRepository",
                linhaTransporteRepository);

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> linhaTransporteIntegrationService.saveEntityList(List.of(getLinhaTransporte())));

        Assertions.assertEquals(
                "Transportation Lane saved collection returned null.",
                dataUploadException.getMessage());

    }

    @Test
    public void saveEntityListShouldReturnSavedTransportationLaneMaterials() {

        LinhaTransporteProdutoIntegrationService linhaTransporteProdutoIntegrationService =
                new LinhaTransporteProdutoIntegrationService();
        LinhaTransporteProdutoRepository linhaTransporteProdutoRepository =
                getRepositoryProxy(
                        LinhaTransporteProdutoRepository.class,
                        false);
        LinhaTransporte linhaTransportePersistida = getLinhaTransporte();
        Map<LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO, LinhaTransporte> linhasTransportePersistidasPorChave =
                new LinkedHashMap<>();
        linhasTransportePersistidasPorChave.put(
                getLinhaTransportePrimaryKeyIntegrationDTO(linhaTransportePersistida),
                linhaTransportePersistida);

        ReflectionTestUtils.setField(
                linhaTransporteProdutoIntegrationService,
                "linhaTransporteProdutoRepository",
                linhaTransporteProdutoRepository);
        ReflectionTestUtils.setField(
                linhaTransporteProdutoIntegrationService,
                "linhaTransporteIntegrationService",
                getLinhaTransporteIntegrationService(linhasTransportePersistidasPorChave));

        LinhaTransporteProduto linhaTransporteProduto = new LinhaTransporteProduto(
                new LinhaTransporteProduto.LinhaTransporteProdutoCompositeKey(
                        getLinhaTransporte(),
                        new Produto("MAT-001")));

        List<LinhaTransporteProduto> linhasTransporteProdutoSalvas =
                linhaTransporteProdutoIntegrationService.saveEntityList(List.of(linhaTransporteProduto));

        Assertions.assertEquals(1, linhasTransporteProdutoSalvas.size());
        Assertions.assertSame(linhaTransporteProduto, linhasTransporteProdutoSalvas.get(0));
        Assertions.assertSame(linhaTransportePersistida, linhasTransporteProdutoSalvas.get(0).getLinhaTransporte());

    }

    @Test
    public void saveEntityListShouldRejectNullSavedTransportationLaneMaterialSnapshot() {

        LinhaTransporteProdutoIntegrationService linhaTransporteProdutoIntegrationService =
                new LinhaTransporteProdutoIntegrationService();
        LinhaTransporteProdutoRepository linhaTransporteProdutoRepository =
                getRepositoryProxy(
                        LinhaTransporteProdutoRepository.class,
                        true);
        LinhaTransporte linhaTransportePersistida = getLinhaTransporte();
        Map<LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO, LinhaTransporte> linhasTransportePersistidasPorChave =
                new LinkedHashMap<>();
        linhasTransportePersistidasPorChave.put(
                getLinhaTransportePrimaryKeyIntegrationDTO(linhaTransportePersistida),
                linhaTransportePersistida);

        ReflectionTestUtils.setField(
                linhaTransporteProdutoIntegrationService,
                "linhaTransporteProdutoRepository",
                linhaTransporteProdutoRepository);
        ReflectionTestUtils.setField(
                linhaTransporteProdutoIntegrationService,
                "linhaTransporteIntegrationService",
                getLinhaTransporteIntegrationService(linhasTransportePersistidasPorChave));

        LinhaTransporteProduto linhaTransporteProduto = new LinhaTransporteProduto(
                new LinhaTransporteProduto.LinhaTransporteProdutoCompositeKey(
                        getLinhaTransporte(),
                        new Produto("MAT-001")));

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> linhaTransporteProdutoIntegrationService.saveEntityList(List.of(linhaTransporteProduto)));

        Assertions.assertEquals(
                "Transportation Lane Material saved collection returned null.",
                dataUploadException.getMessage());

    }

    @Test
    public void primaryKeyLookupShouldRejectDuplicatedTransportationLaneMaterialPrimaryKey() {

        LinhaTransporteProdutoIntegrationService linhaTransporteProdutoIntegrationService =
                new LinhaTransporteProdutoIntegrationService();
        LinhaTransporteProdutoRepository linhaTransporteProdutoRepository =
                getRepositoryProxy(
                        LinhaTransporteProdutoRepository.class,
                        false);
        LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO primeiraChave =
                new LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO(
                        "SN-001",
                        "ORIGEM",
                        "DESTINO",
                        "MAT-001");
        LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO chaveDuplicada =
                new LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO(
                        "SN-001",
                        "ORIGEM",
                        "DESTINO",
                        "MAT-001");

        ReflectionTestUtils.setField(
                linhaTransporteProdutoIntegrationService,
                "linhaTransporteProdutoRepository",
                linhaTransporteProdutoRepository);

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> linhaTransporteProdutoIntegrationService.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
                        List.of(
                                primeiraChave,
                                chaveDuplicada)));

        Assertions.assertEquals(
                "Transportation Lane Material primary key collection item at index 1 has duplicated key supplyNetworkVersionId SN-001 / originLocationId ORIGEM / destinationLocationId DESTINO / materialId MAT-001.",
                dataUploadException.getMessage());

    }

    @Test
    public void saveSuccessMessageShouldUseMaterialNaming() {

        LinhaTransporteProdutoIntegrationService linhaTransporteProdutoIntegrationService =
                new LinhaTransporteProdutoIntegrationService();

        Assertions.assertEquals(
                "Transportation Lane - Material data uploaded",
                linhaTransporteProdutoIntegrationService.getSaveSuccessMessage());

    }

    private static LinhaTransporte getLinhaTransporte() {

        return new LinhaTransporte(
                new LinhaTransporte.LinhaTransporteCompositeKey(
                        new VersaoMalha("SN-001"),
                        new Location("ORIGEM", "Origem"),
                        new Location("DESTINO", "Destino")));

    }

    private static LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO getLinhaTransportePrimaryKeyIntegrationDTO(
            LinhaTransporte linhaTransporte) {

        return new LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO(
                linhaTransporte.getVersaoMalha().getId(),
                linhaTransporte.getLocationOrigem().getId(),
                linhaTransporte.getLocationDestino().getId());

    }

    private static LinhaTransporteIntegrationService getLinhaTransporteIntegrationService(
            Map<LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO, LinhaTransporte> linhasTransportePersistidasPorChave) {

        return new LinhaTransporteIntegrationService() {

            @Override
            public Map<LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO, LinhaTransporte> getPersistedEntityMapFromPrimaryKeyDtoCollection(
                    Collection<LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO> dtoBatchList) {

                Map<LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO, LinhaTransporte> linhasTransporteEncontradasPorChave =
                        new LinkedHashMap<>();
                dtoBatchList.forEach(primaryKeyDto -> {
                    LinhaTransporte linhaTransporte = linhasTransportePersistidasPorChave.get(primaryKeyDto);
                    if (linhaTransporte != null) {
                        linhasTransporteEncontradasPorChave.put(primaryKeyDto, linhaTransporte);
                    }
                });

                return linhasTransporteEncontradasPorChave;

            }

        };

    }

    @SuppressWarnings("unchecked")
    private static <T> T getRepositoryProxy(
            Class<T> repositoryClass,
            boolean returnNullOnSaveAll) {

        return (T) Proxy.newProxyInstance(
                repositoryClass.getClassLoader(),
                new Class[]{repositoryClass},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass().equals(Object.class)) {
                        return getRespostaMetodoObject(proxy, method.getName(), args);
                    }

                    if (method.getName().equals("saveAll")) {
                        if (returnNullOnSaveAll) {
                            return null;
                        }

                        List<Object> entidadesSalvas = new ArrayList<>();
                        Iterable<?> entidades = (Iterable<?>) args[0];
                        entidades.forEach(entidadesSalvas::add);
                        return entidadesSalvas;
                    }

                    throw new UnsupportedOperationException("Metodo nao implementado no teste: " + method.getName());
                });

    }

    private static Object getRespostaMetodoObject(Object proxy, String methodName, Object[] args) {

        return switch (methodName) {
            case "toString" -> "Proxy repositorio teste " + System.identityHashCode(proxy);
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new UnsupportedOperationException("Metodo Object nao implementado no teste: " + methodName);
        };

    }

}
