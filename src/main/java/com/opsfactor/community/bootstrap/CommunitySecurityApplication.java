package com.opsfactor.community.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootstrap auxiliar do modulo de seguranca Community.
 *
 * <p>O executavel web importa {@code CustomHttpSecurityConfig} diretamente para
 * manter explicito que o Community publica apenas login/senha simples. Esta
 * classe existe para testes ou execucoes locais isoladas do modulo security.</p>
 */
@SpringBootApplication
public class CommunitySecurityApplication {

    public static void main(String[] args) {

        SpringApplication.run(CommunitySecurityApplication.class, args);

    }

}

