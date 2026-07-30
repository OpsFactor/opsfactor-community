package com.opsfactor.community.platform.scheduler.services;

/**
 * Politica lexical para os nomes de classes que podem compor uma task
 * persistida pelo scheduler do produto novo.
 *
 * <p>O scheduler precisa manter o nome da classe para que um worker ou o web
 * Enterprise reconstrua uma task depois de outro processo ou restart. Esta
 * politica e aplicada antes de qualquer reflexao: o legado serve somente como
 * evidencia de migracao e jamais pode voltar a ser carregado por uma classe
 * que ainda exista no classpath.</p>
 */
public final class CanonicalScheduledTaskClassPolicy {

    private static final String COMMUNITY_PACKAGE_PREFIX = "com.opsfactor.community.";
    private static final String ENTERPRISE_PACKAGE_PREFIX = "com.opsfactor.enterprise.";

    private CanonicalScheduledTaskClassPolicy() {

    }

    /**
     * Informa se o nome pertence a uma das duas superficies executaveis do
     * produto novo, sem tentar carregar a classe.
     */
    public static boolean isCanonicalTaskClassName(String taskClassName) {

        return taskClassName != null
                && (taskClassName.startsWith(COMMUNITY_PACKAGE_PREFIX)
                || taskClassName.startsWith(ENTERPRISE_PACKAGE_PREFIX));

    }

    /**
     * Rejeita referencias vazias, externas ou legadas antes de
     * {@link Class#forName(String)} poder inicializar qualquer classe.
     */
    public static void validateCanonicalTaskClassName(String taskClassName) {

        if (taskClassName == null || taskClassName.isBlank()) {
            throw new IllegalArgumentException("Scheduled task class is required.");
        }
        if (!isCanonicalTaskClassName(taskClassName)) {
            throw new IllegalArgumentException(
                    "Scheduled task class must belong to the Community or Enterprise runtime: "
                            + taskClassName);
        }

    }

}
