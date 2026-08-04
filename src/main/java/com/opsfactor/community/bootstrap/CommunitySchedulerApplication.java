package com.opsfactor.community.bootstrap;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Configuracao modular do scheduler Community.
 *
 * <p>No Community o scheduler e usado apenas como camada de historico e
 * execucao imediata sincronizada. A classe continua existindo para que o
 * bootstrap web inclua entidades, repositories e services do pacote
 * {@code com.opsfactor.community.platform.scheduler}, mas nao habilita recorrencia, filas,
 * workers batch ou execucao por linha de comando.</p>
 */
@SpringBootApplication
public class CommunitySchedulerApplication {

}
