package com.opsfactor.community.capability.masterdata.architecture.service;

import com.opsfactor.community.capability.masterdata.network.location.facade.LocationDtoService;
import com.opsfactor.community.capability.masterdata.product.material.facade.MaterialDtoService;
import com.opsfactor.community.capability.masterdata.product.material.facade.mapper.MaterialMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Guarda de contrato para a injecao explicita dos services/mappers DTO de
 * master data Community.
 *
 * <p>Essas classes ficam na borda publica material/location. Campos que sao
 * beans Spring devem continuar anotados com {@link Autowired} para deixar claro
 * no codigo o que e dependencia de container e o que e estado local.</p>
 */
class MasterdataDtoAutowiredContractTest {

    @Test
    void locationDtoServiceShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertRequiredAutowiredFields(
                LocationDtoService.class,
                List.of(
                        "locationService",
                        "locationRepository",
                        "locationMapper",
                        "clusterEParametrosProjectionFactory"));

    }

    @Test
    void materialDtoServiceShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertRequiredAutowiredFields(
                MaterialDtoService.class,
                List.of(
                        "produtoRepository",
                        "clusterProdutosRepository",
                        "materialMapper",
                        "clusterEParametrosProjectionFactory"));

    }

    @Test
    void materialMapperShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertRequiredAutowiredFields(
                MaterialMapper.class,
                List.of("parametrosGlobaisService"));

    }

    private static void assertRequiredAutowiredFields(
            Class<?> beanClass,
            List<String> fieldNames) throws Exception {

        for (String fieldName : fieldNames) {
            Field field = beanClass.getDeclaredField(fieldName);
            Autowired autowired = field.getAnnotation(Autowired.class);

            Assertions.assertNotNull(
                    autowired,
                    beanClass.getSimpleName() + "." + fieldName + " must declare @Autowired explicitly");
            Assertions.assertTrue(
                    autowired.required(),
                    beanClass.getSimpleName() + "." + fieldName + " must be a required Spring bean");
        }

    }

}
