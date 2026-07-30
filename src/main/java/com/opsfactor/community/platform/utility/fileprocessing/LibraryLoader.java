package com.opsfactor.community.platform.utility.fileprocessing;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.javatuples.Pair;

/**
 * Utilitario para localizar e carregar bibliotecas nativas no classpath.
 *
 * <p>No recorte Community esta classe permanece como suporte generico herdado
 * para componentes que ainda dependem de JNI. Ela nao deve introduzir
 * acoplamento com provedores Enterprise; callers especificos devem decidir
 * qual biblioteca carregar.</p>
 */
@Slf4j
public class LibraryLoader {
    
    public enum TipoCaminho {
        CAMINHO_ABSOLUTO, USER_HOME, JAR_EM_EXECUCAO, AUTOMATICO
    }
    
    public enum TipoOS {
        LINUX, WINDOWS
    }
    
    /**
     * 
     * @param tipoCaminhoJarAExtrair CAMINHO_ABSOLUTO, USER_HOME, JAR_EM_EXECUCAO, AUTOMATICO
     * @param caminhoArquivoJarAExtrair ex. USER_HOME : '/.m2/repositories/com/opsfactor/exemplo.jar' ex. JAR_EM_EXECUCAO '/BOOT_INFO/etcetcetc/arquivo.jar'
     * @param nomeBibliotecaACarregarSemExtensao ex. 'libxpto' , sem extensão .so ou .dll
     * @param baseDiretorioTemporario qualquer string, ex. 'base_biblioteca_xpto' . se criará automaticamente uma pasta no diretório temp com a base concatenada a um código numérico. será buscado em toda a estrutura do arquivo jar
     * @throws IOException 
     */
    public static void carregaBibliotecaEmJarNoClasspath(
            TipoCaminho tipoCaminhoJarAExtrair, String caminhoArquivoJarAExtrair, 
            String nomeBibliotecaACarregarSemExtensao, String baseDiretorioTemporario) throws IOException {
        
        try {
            log.info("Buscando biblioteca " + nomeBibliotecaACarregarSemExtensao + " no java.library.path");
            System.loadLibrary(nomeBibliotecaACarregarSemExtensao);
            log.info("Biblioteca " + nomeBibliotecaACarregarSemExtensao + " carregada a partir do java.library.path");
            return;
        } catch (UnsatisfiedLinkError e) {
            log.info("Biblioteca " + nomeBibliotecaACarregarSemExtensao + " não encontrada no java.library.path. Buscando na pasta temporária " + getCaminhoCompletoDiretorioTemporario(baseDiretorioTemporario));
        }
        
        FileSystem fs = FileSystems.getDefault();
        
        // caso arquivo já tenha sido copiado na pasta temporária, encerra carga de biblioteca
        // necessário pois a máquina virtual só é encerrada esporadicamente, ficando os arquivos
        // disponíveis até uma reinicialização
        Path caminhoArquivoTemporario = fs.getPath(getCaminhoCompletoDiretorioTemporario(baseDiretorioTemporario))
                .resolve(System.mapLibraryName(nomeBibliotecaACarregarSemExtensao));
        if (caminhoArquivoTemporario.toFile().isFile()) {
            log.info("Biblioteca " + caminhoArquivoTemporario.toAbsolutePath() + " já se localiza na pasta temporária. Carregando a biblioteca");
            System.load(caminhoArquivoTemporario.toString());
            return;
        }
        
        log.info("Biblioteca " + nomeBibliotecaACarregarSemExtensao+ " não encontrada na pasta temporária. Iniciando carga da biblioteca de arquivo jar " + caminhoArquivoJarAExtrair);
        
        if (tipoCaminhoJarAExtrair.equals(TipoCaminho.AUTOMATICO)) {
            if (classPathDentroDeArquivoJarEmExecucao() && caminhoArquivoJarAExtrair.contains("BOOT_INFO")) {
                tipoCaminhoJarAExtrair = TipoCaminho.JAR_EM_EXECUCAO;
            } else if (fs.getPath(caminhoArquivoJarAExtrair).toFile().isFile()) {
                tipoCaminhoJarAExtrair = TipoCaminho.CAMINHO_ABSOLUTO;
            } else {
                tipoCaminhoJarAExtrair = TipoCaminho.USER_HOME;
            }
        }        
        log.info("Tipo de caminho considerado: "  + tipoCaminhoJarAExtrair.toString());
        
        // adiciona '/' ao início do caminho do jar caso não haja
        // ex : /.m2/repository/com/opsfactor/exemplo/arquivo.jar
        String caminhoArquivoJarAExtrairTratado;
        
        switch (tipoCaminhoJarAExtrair) {
            case CAMINHO_ABSOLUTO:
                caminhoArquivoJarAExtrairTratado = caminhoArquivoJarAExtrair;
                break;
            case USER_HOME:
                caminhoArquivoJarAExtrairTratado = adicionaSeparadorInicialSeNecessario(caminhoArquivoJarAExtrair);
                // ex: C:\Users\Usuario\.m2\repository\com\opsfactor\exemplo\arquivo.jar
                // ex: /home/usuario/.m2/repository/com/opsfactor/exemplo/arquivo.jar
                caminhoArquivoJarAExtrairTratado = SystemUtils.getUserHome().getAbsolutePath() 
                        + caminhoArquivoJarAExtrairTratado;
                break;
            case JAR_EM_EXECUCAO:
                caminhoArquivoJarAExtrairTratado = adicionaSeparadorInicialSeNecessario(caminhoArquivoJarAExtrair);
                // primeiro extrai o jar dentro do jar para uma pasta temporária
                // e depois se retorna o caminho para o segundo jar na pasta temporária
                Path tempJarFolderPath = fs.getPath(getCaminhoCompletoDiretorioTemporario(baseDiretorioTemporario + "_originalJar"));
                
                Path tempJarFilePath = fs.getPath(tempJarFolderPath.toString(), "originalJar.jar");
                
                Path caminhoArquivoJarEmExecucao = Paths.get(LibraryLoader.class.getProtectionDomain().getPermissions()
                        .elements().nextElement().getName()).getParent();
                
                // zipfile = arquivo jar em execução
                try (ZipFile zipFile = new ZipFile(caminhoArquivoJarEmExecucao.toString())) {
                    // busca o jar desejado dentro do jar em execução
                    ZipEntry zipEntry = zipFile.getEntry(caminhoArquivoJarAExtrairTratado);
                    if (zipEntry == null) {
                        throw new IllegalStateException(
                                "Nested jar " + caminhoArquivoJarAExtrairTratado
                                        + " not found inside executable jar " + caminhoArquivoJarEmExecucao);
                    }

                    try (InputStream inputStream = zipFile.getInputStream(zipEntry)) {
                        // copia arquivo no novo diretório
                        Files.copy(inputStream, tempJarFilePath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    
                    tempJarFilePath.toFile().deleteOnExit();
                    
                    caminhoArquivoJarAExtrairTratado = tempJarFilePath.toString();

                } catch(IOException e) {
                    throw new IllegalStateException(
                            "Error unzipping file " + caminhoArquivoJarEmExecucao,
                            e);
                }
                break;
            default: 
                throw new IllegalStateException(
                        "Unsupported native library jar path type: " + tipoCaminhoJarAExtrair);
        }
        log.info("Caminho tratado para arquivo .jar " + caminhoArquivoJarAExtrairTratado);
        
        Path jarPath = fs.getPath(caminhoArquivoJarAExtrairTratado);

        // retorna e cria o diretório temporário caso não exista
        Path tempLibraryFolderPath = fs.getPath(getCaminhoCompletoDiretorioTemporario(baseDiretorioTemporario));
        // arquivo dentro do diretório temporário
        Path tempLibraryFilePath = fs.getPath(tempLibraryFolderPath.toString(), System.mapLibraryName(nomeBibliotecaACarregarSemExtensao));

        // extrai arquivo jar para diretório temporário
        Pair<ZipFile,ZipEntry> pairZipFileZipEntry = buscaArquivoEmJar(
                jarPath.toString(), System.mapLibraryName(nomeBibliotecaACarregarSemExtensao));
        if (pairZipFileZipEntry == null) {
            log.error("Biblioteca não encontrada em arquivo .jar");
            return;
        }
        
        extraiZipEntryParaDestino(pairZipFileZipEntry, tempLibraryFilePath);

        // apaga diretório e arquivo temporários quando a máquina virtual for encerrada
        tempLibraryFolderPath.toFile().deleteOnExit();
        tempLibraryFilePath.toFile().deleteOnExit();
            
        log.info("Biblioteca copiada no caminho " + tempLibraryFilePath.toString());
        System.load(tempLibraryFilePath.toString());
        log.info("Biblioteca carregada");

    }
    
    /**
     * Busca arquivo no classpath:
     * 1) Verifica se arquivo está diretamente indicado no classpath
     * 2) Abre diretórios do classpath para buscar arquivo
     * 3) Se classpath contém arquivos .jar, abre esses arquivos para buscar o arquivo desejado
     * Nesse último caso os arquivos .jar sáo abertos e caso o arquivo desejado seja encontrado
     * será copiado em um diretório temporário, cujo caminho será retornado
     * @param nomeArquivoComExtensao
     * @param baseDiretorioTemporario usado caso o arquivo desejado esteja dentro de um .jar no classpath. neste caso
     * o arquivo desejado será extraído para o diretório temporário
     * @return
     * @throws IOException 
     */
    public static String getCaminhoParaArquivoNoClassPath(
            String nomeArquivoComExtensao,
            String baseDiretorioTemporario) throws IOException {
        String classPath = System.getProperty("java.class.path");
        /*
         * O classpath completo pode ter milhares de caracteres em execucoes
         * Maven/IDE. Mantemos em debug para diagnostico fino sem poluir logs
         * normais de carga de bibliotecas JNI.
         */
        log.debug("Classpath : " + classPath);
        
        StringTokenizer st = new StringTokenizer(classPath, System.getProperty("path.separator"));
        while (st.hasMoreTokens()) {
            String entry = st.nextToken().trim();
            if (entry.isEmpty()) {
              continue;
            }
            Path path = Paths.get(entry);
            
            // se diretório, buscar recursivamente
            if (path.toFile().isDirectory()) {
                String caminhoArquivoEncontrado = getTargetFileRecursive(
                        path.toFile(), nomeArquivoComExtensao);
                
                if (caminhoArquivoEncontrado != null) return caminhoArquivoEncontrado;
            // se arquivo, verificar se há match
            } else {
                // se for o arquivo que estamos buscando, retorná-lo
                if (path.toFile().getName().equals(nomeArquivoComExtensao)) {
                    return path.toAbsolutePath().toString();
                // se for um outro arquivo .jar, abri-lo para buscar internamente
                } else if (path.toString().endsWith(".jar")) {
                    // obtém o registro do arquivo desejado dentro do arquivo .jar no classpath
                    Pair<ZipFile,ZipEntry> pairZipFileZipEntry = buscaArquivoEmJar(
                            path.toString(), nomeArquivoComExtensao);
                    // arquivo desejado não encontrado dentro do arquivo .jar
                    if (pairZipFileZipEntry == null) continue;
                    // copia o arquivo desejado em um diretório temporário
                    FileSystem fs = FileSystems.getDefault();
                    Path pathArquivoExtraidoNoDiretorioTemporario = fs.getPath(
                            getCaminhoCompletoDiretorioTemporario(baseDiretorioTemporario),
                            nomeArquivoComExtensao);
                    
                    if (!pathArquivoExtraidoNoDiretorioTemporario.toFile().isFile()) {
                        log.info("Arquivo desejado " + nomeArquivoComExtensao + " encontrado dentro do arquivo " + path.getFileName().toString() + " . Realizando extração para " + pathArquivoExtraidoNoDiretorioTemporario.toAbsolutePath().toString());
                        extraiZipEntryParaDestino(pairZipFileZipEntry, pathArquivoExtraidoNoDiretorioTemporario);
                    } else {
                        log.info("Arquivo desejado " + nomeArquivoComExtensao + " já se encontra em " + pathArquivoExtraidoNoDiretorioTemporario.getFileName().toString() + " . Não há necessidasde de extrair de " + path.getFileName().toString());
                    }

                    pathArquivoExtraidoNoDiretorioTemporario.toFile().deleteOnExit();
                    
                    return pathArquivoExtraidoNoDiretorioTemporario.toAbsolutePath().toString();
                    
                }                
            }
        }
        return null;
    }

    /**
     * Normaliza o caminho interno usado nas opcoes USER_HOME e JAR_EM_EXECUCAO.
     *
     * <p>O contrato antigo aceitava caminhos relativos como
     * {@code .m2/repository/...} e caminhos ja enraizados como
     * {@code /.m2/repository/...}. A verificacao precisa usar o primeiro
     * caractere real; `substring(0, 0)` sempre retorna string vazia e prefixava
     * caminhos ja iniciados por separador.</p>
     */
    static String adicionaSeparadorInicialSeNecessario(String caminhoArquivoJarAExtrair) {

        if (caminhoArquivoJarAExtrair == null || caminhoArquivoJarAExtrair.isBlank()) {
            throw new IllegalArgumentException("Native library jar path is required.");
        }
        if (caminhoArquivoJarAExtrair.startsWith("/") || caminhoArquivoJarAExtrair.startsWith("\\")) {
            return caminhoArquivoJarAExtrair;
        }
        return "/" + caminhoArquivoJarAExtrair;

    }
                
    public static TipoOS getTipoOS() {
        
        String libraryName = System.mapLibraryName("x");

        if (libraryName.endsWith("dll")) {
            log.info("OS type: WINDOWS");
            return TipoOS.WINDOWS;
        } else if (libraryName.endsWith("so")) {
            log.info("OS type: LINUX");
            return TipoOS.LINUX;
        } else {
            throw new IllegalStateException(
                    "Unable to determine native library resource prefix. This environment may not be supported.");
        }

    }
    
    /**
     * Exemplo, para um diretório base 'bibliotecaXPTO' se retornará para
     * Windows : c:\\users\nomeusuario\temp\bibliotecaXPTO'
     * Linux : '\temp\bibliotecaXPTO'
     * @param baseDiretorioTemporario
     * @return 
     */
    public static String getCaminhoCompletoDiretorioTemporario(String baseDiretorioTemporario) {
        File diretorioTemporarioOS = new File(System.getProperty("java.io.tmpdir"));
        File subDiretorioTemporarioBiblioteca = new File(diretorioTemporarioOS, baseDiretorioTemporario);
        subDiretorioTemporarioBiblioteca.mkdir();
        subDiretorioTemporarioBiblioteca.deleteOnExit();
        return subDiretorioTemporarioBiblioteca.getAbsolutePath();
    }
    
    /**
     * Verifica se o classpath está dentro de um arquivo .jar sendo executado
     * ou se está rodando diretamente a partir de classes compiladas
     * @return 
     */
    public static boolean classPathDentroDeArquivoJarEmExecucao() {
        if (SystemUtils.getUserHome().getAbsolutePath().contains("BOOT_INFO")) {
            return true;
        }
        return false;
    }
    
    /**
     * Filtra arquivos em um diretório que tenham o mesmo nome daquele solicitado
     */
    private static String[] getTargetFiles(File directory, String nomeArquivoComExtensao){
        if(directory == null){
            return new String[0];
        }

        String[] files = directory.list(new FilenameFilter(){
            @Override
            public boolean accept(File dir, String name) {
                return name.equals(nomeArquivoComExtensao);
            }
        });

        return files == null ? new String[0] : files;
    }
    
    /**
     * Faz uma busca recursiva pelo arquivo em um determinado diretório
     */
    private static String getTargetFileRecursive(File path, String nomeArquivoComExtensao) {

        // se path for diretório e arquivo estiver dentro dele,
        // retorna e encerra o método
        if (path.isDirectory()) {
            String[] files = getTargetFiles(path, nomeArquivoComExtensao);
            if (files.length > 0) {
                return path.getAbsolutePath() + "/" + nomeArquivoComExtensao;
            }
        }
        
        // circula cada diretório e para cada um
        // gera chamada recursiva
        File[] childFiles = path.listFiles();
        if (childFiles == null) {
            return null;
        }

        for (File file: childFiles) {
            if(file.isDirectory()){
                String caminhoArquivoEncontrado = getTargetFileRecursive(file, nomeArquivoComExtensao);
                if (caminhoArquivoEncontrado != null) {
                    return caminhoArquivoEncontrado;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Busca nomeArquivoBuscadoComExtensao em caminhoArquivoJar
     * @param caminhoArquivoJar ex. /temp/arquivo.jar
     * @param nomeArquivoBuscadoComExtensao ex. biblioteca.so ex. biblioteca.dll ex. outroarquivojar.jar
     * @return Pair<ZipFile,ZipEntry> ou nulo, caso o arquivo buscado não exista dentro do arquivo .jar
     * @throws IOException 
     */
    private static Pair<ZipFile,ZipEntry> buscaArquivoEmJar(String caminhoArquivoJar, String nomeArquivoBuscadoComExtensao) throws IOException {
        ZipFile zipFile = new ZipFile(caminhoArquivoJar);
        Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
        while (zipEntries.hasMoreElements()) {
            ZipEntry zipEntry = zipEntries.nextElement();
            if (!zipEntry.isDirectory() && zipEntry.getName().contains(
                    nomeArquivoBuscadoComExtensao)) {
                return Pair.with(zipFile, zipEntry);
            }
        }
        return null;
    } 
    
    private static void extraiZipEntryParaDestino(Pair<ZipFile,ZipEntry> pairZipFileZipEntry, Path caminhoCompletoDestino) {
        ZipFile zipFile = pairZipFileZipEntry.getValue0();
        ZipEntry zipEntry = pairZipFileZipEntry.getValue1();
        try (zipFile; InputStream inputStream = zipFile.getInputStream(zipEntry)) {
            // copia arquivo no novo diretório
            Files.copy(inputStream, caminhoCompletoDestino);

        } catch(IOException e) {
            throw new IllegalStateException(
                    "Error unzipping file " + zipFile.getName(),
                    e);
        }
    }

    /**
     * A introspecção de bibliotecas JNI carregadas deixou de ser acessível a partir do Java 9.
     * O método preserva o contrato antigo do utilitário e assume que a chamada ativa deve tentar
     * carregar a biblioteca, deixando a JVM falhar caso ela já esteja carregada de forma incompatível.
     */
    public static boolean checaSeBibliotecaJaFoiCarregada(String biblioteca) {
        
        return false;
        
    }

    
    
}
