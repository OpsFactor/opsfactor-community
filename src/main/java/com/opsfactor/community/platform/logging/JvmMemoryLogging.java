package com.opsfactor.community.platform.logging;

/**
 * Utilitarios leves para logs de memoria da JVM.
 * <p>
 * A classe e propositalmente estatica e sem dependencia de framework para poder
 * ser usada em pontos quentes de processamento, inclusive fora do contexto
 * Spring. O objetivo e registrar uma fotografia simples da heap sem acoplar o
 * codigo de dominio a uma ferramenta de profiling.
 */
public final class JvmMemoryLogging {

    private static final long BYTES_POR_MEGABYTE = 1024L * 1024L;

    private JvmMemoryLogging() {
    }

    /**
     * Retorna um resumo compacto da heap da JVM para inclusao em logs.
     * <p>
     * Os valores seguem a semantica do {@link Runtime}: memoria usada dentro da
     * heap atualmente alocada, heap total ja alocada pela JVM e heap maxima
     * permitida. Esse formato ajuda a diferenciar uma etapa lenta por CPU de uma
     * etapa lenta por pressao de GC quando a heap fica proxima do limite.
     */
    public static String getResumoMemoriaJvm() {

        Runtime runtime = Runtime.getRuntime();
        long memoriaTotalMb = runtime.totalMemory() / BYTES_POR_MEGABYTE;
        long memoriaLivreMb = runtime.freeMemory() / BYTES_POR_MEGABYTE;
        long memoriaUsadaMb = memoriaTotalMb - memoriaLivreMb;
        long memoriaMaximaMb = runtime.maxMemory() / BYTES_POR_MEGABYTE;

        return memoriaUsadaMb
                + "MB usados / "
                + memoriaTotalMb
                + "MB alocados / "
                + memoriaMaximaMb
                + "MB max";

    }

}
