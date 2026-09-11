package com.opsfactor.community.platform.calendar;

import jakarta.persistence.Entity;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;

import static org.junit.jupiter.api.Assertions.*;

/** Valida a herança e a identidade derivada no metamodelo completo, sem conexão ou DDL. */
class CalendarProfileCommunityJpaMappingTest {

    @Test
    void calendarioIntegraMetamodeloDaEdicao() throws Exception {

        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .applySetting("hibernate.hbm2ddl.auto", "none")
                .build();
        try {
            var sources = new MetadataSources(registry);
            var resolver = new PathMatchingResourcePatternResolver();
            var readers = new CachingMetadataReaderFactory(resolver);
            for (String edition : new String[]{"community"}) {
                for (var resource : resolver.getResources("classpath*:com/opsfactor/" + edition + "/**/*.class")) {
                    var reader = readers.getMetadataReader(resource);
                    if (reader.getAnnotationMetadata().hasAnnotation(Entity.class.getName())
                            && !reader.getClassMetadata().getClassName().contains("Test")) {
                        sources.addAnnotatedClass(Class.forName(reader.getClassMetadata().getClassName(),
                                false, getClass().getClassLoader()));
                    }
                }
            }
            MetadataImplementor metadata = (MetadataImplementor) sources.buildMetadata();
            metadata.validate();
            assertNotNull(metadata.getEntityBinding("com.opsfactor.community.capability.masterdata.calendar.profile.domain.PerfilCalendarioSimples"));
            assertNotNull(metadata.getEntityBinding("com.opsfactor.community.capability.supplyplanning.supplyplan.domain.calendar.PerfilCalendarioSimplesSupplyPlan"));
            assertNull(metadata.getEntityBinding("com.opsfactor.enterprise.capability.masterdata.calendar.profile.domain.PerfilCalendarioComposto"));
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }

    }

}
