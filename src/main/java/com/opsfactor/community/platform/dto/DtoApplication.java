package com.opsfactor.community.platform.dto;

import com.opsfactor.community.platform.model.ModelApplication;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;

import java.util.TimeZone;

/**
 * Bootstrap auxiliar do modulo DTO para testes e execucoes locais isoladas.
 *
 * <p>A aplicacao final Community roda pelo modulo web. Este bootstrap existe
 * para materializar o scan de DTOs, mappers e model quando uma rotina de teste
 * ou ferramenta precisa subir apenas a camada de contratos sem servidor HTTP.</p>
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        /*
         * A auto-referencia do pacote dto e intencional: testes de mapper podem
         * subir este bootstrap diretamente e ainda precisam encontrar beans do
         * proprio modulo, alem das entidades/repositories do model.
         */
        "com.opsfactor.community.capability",
        "com.opsfactor.community.platform.model",
        "com.opsfactor.community.platform.dto"
})
public class DtoApplication {

    public static void main(String[] args) {

        new SpringApplicationBuilder()
                .sources(DtoApplication.class, ModelApplication.class)
                .run(args);

    }

    /**
     * Mantem JVM, Hibernate e calendario operacional alinhados ao timezone
     * padrao documentado nos defaults Community/Enterprise.
     */
    @PostConstruct
    void started() {

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

    }

}
