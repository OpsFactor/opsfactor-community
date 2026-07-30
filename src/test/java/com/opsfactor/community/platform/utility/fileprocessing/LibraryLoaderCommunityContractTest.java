package com.opsfactor.community.platform.utility.fileprocessing;

import org.javatuples.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Contratos de diagnostico do utilitario de carga de bibliotecas nativas.
 *
 * <p>O teste nao carrega JNI real. Ele exercita apenas a etapa de extracao do
 * zip para garantir que falhas operacionais preservem o nome do arquivo e a
 * causa original em uma excecao explicita.</p>
 */
class LibraryLoaderCommunityContractTest {

    @Test
    void adicionaSeparadorInicialSeNecessarioShouldPreserveRootedPathsAndPrefixRelativePaths() {

        Assertions.assertEquals(
                "/.m2/repository/native.jar",
                LibraryLoader.adicionaSeparadorInicialSeNecessario("/.m2/repository/native.jar"));
        Assertions.assertEquals(
                "\\.m2\\repository\\native.jar",
                LibraryLoader.adicionaSeparadorInicialSeNecessario("\\.m2\\repository\\native.jar"));
        Assertions.assertEquals(
                "/.m2/repository/native.jar",
                LibraryLoader.adicionaSeparadorInicialSeNecessario(".m2/repository/native.jar"));
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> LibraryLoader.adicionaSeparadorInicialSeNecessario(" "));

    }

    @Test
    void getTargetFileRecursiveShouldContinueScanningSiblingDirectories() throws Exception {

        FileStub emptyDirectory = new FileStub(
                "empty-directory",
                new String[0],
                new File[0]);
        FileStub directoryWithTarget = new FileStub(
                "directory-with-target",
                new String[]{"native-library.txt"},
                new File[0]);
        FileStub rootDirectory = new FileStub(
                "root-directory",
                new String[0],
                new File[]{emptyDirectory, directoryWithTarget});

        String caminhoArquivoEncontrado = invocaBuscaRecursiva(
                rootDirectory,
                "native-library.txt");

        Assertions.assertEquals(
                "directory-with-target/native-library.txt",
                caminhoArquivoEncontrado);

    }

    @Test
    void extraiZipEntryParaDestinoShouldFailExplicitlyWhenDestinationCannotBeWritten() throws Exception {

        Path zipPath = criaZipTemporarioComEntrada();

        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            ZipEntry zipEntry = zipFile.getEntry("native-library.txt");
            Path destinoSemDiretorioPai = Files.createTempDirectory("opsfactor-library-loader-test")
                    .resolve("missing-parent")
                    .resolve("native-library.txt");

            InvocationTargetException invocationTargetException = Assertions.assertThrows(
                    InvocationTargetException.class,
                    () -> invocaExtracaoZipEntryParaDestino(
                            Pair.with(zipFile, zipEntry),
                            destinoSemDiretorioPai));

            Assertions.assertInstanceOf(
                    IllegalStateException.class,
                    invocationTargetException.getCause());
            Assertions.assertTrue(
                    invocationTargetException.getCause().getMessage().contains(zipPath.getFileName().toString()));
            Assertions.assertNotNull(invocationTargetException.getCause().getCause());
        }

    }

    private static Path criaZipTemporarioComEntrada() throws Exception {

        Path zipPath = Files.createTempFile(
                "opsfactor-library-loader",
                ".zip");

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zipOutputStream.putNextEntry(new ZipEntry("native-library.txt"));
            zipOutputStream.write("native".getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }

        return zipPath;

    }

    private static String invocaBuscaRecursiva(
            File path,
            String nomeArquivoComExtensao) throws Exception {

        Method getTargetFileRecursiveMethod = LibraryLoader.class.getDeclaredMethod(
                "getTargetFileRecursive",
                File.class,
                String.class);
        getTargetFileRecursiveMethod.setAccessible(true);
        return (String) getTargetFileRecursiveMethod.invoke(
                null,
                path,
                nomeArquivoComExtensao);

    }

    private static void invocaExtracaoZipEntryParaDestino(
            Pair<ZipFile, ZipEntry> pairZipFileZipEntry,
            Path caminhoCompletoDestino) throws Exception {

        Method extraiZipEntryParaDestinoMethod = LibraryLoader.class.getDeclaredMethod(
                "extraiZipEntryParaDestino",
                Pair.class,
                Path.class);
        extraiZipEntryParaDestinoMethod.setAccessible(true);
        extraiZipEntryParaDestinoMethod.invoke(
                null,
                pairZipFileZipEntry,
                caminhoCompletoDestino);

    }

    /**
     * `File` fake minimo para testar a regra recursiva sem depender da ordem de
     * listagem do sistema operacional.
     */
    private static class FileStub extends File {

        private final String absolutePath;
        private final String[] fileNames;
        private final File[] childFiles;

        private FileStub(
                String absolutePath,
                String[] fileNames,
                File[] childFiles) {

            super(absolutePath);
            this.absolutePath = absolutePath;
            this.fileNames = fileNames;
            this.childFiles = childFiles;

        }

        @Override
        public boolean isDirectory() {

            return true;

        }

        @Override
        public String[] list(FilenameFilter filenameFilter) {

            return Arrays.stream(fileNames)
                    .filter(fileName -> filenameFilter.accept(this, fileName))
                    .toArray(String[]::new);

        }

        @Override
        public File[] listFiles() {

            return childFiles;

        }

        @Override
        public String getAbsolutePath() {

            return absolutePath;

        }

    }

}
