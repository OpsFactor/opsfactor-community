package com.opsfactor.community.bootstrap.devtools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Launches one local Vite development server as a direct child of an IntelliJ
 * Java run configuration.
 *
 * <p>The Community edition of IntelliJ IDEA cannot own an NPM configuration.
 * Keeping Node as a child of this JVM makes the Vite console visible in the
 * IDE and guarantees that stopping the configuration also terminates Vite.</p>
 */
public final class ViteDevelopmentServerLauncher {

    private static final String NODE_EXECUTABLE = "node.exe";
    private static final String VITE_CLI_RELATIVE_PATH = "node_modules/vite/bin/vite.js";

    private ViteDevelopmentServerLauncher() {

    }

    /**
     * Starts Vite with an explicit frontend directory, backend proxy target
     * and local port.
     *
     * @param arguments frontend directory, backend target URL and port
     * @throws IOException when Node or the local Vite CLI cannot be started
     * @throws InterruptedException when the launcher is interrupted while
     *                              awaiting Vite termination
     */
    public static void main(String[] arguments) throws IOException, InterruptedException {

        if (arguments.length != 3) {
            throw new IllegalArgumentException("Expected frontend directory, API proxy target and Vite port.");
        }

        Path frontendDirectory = Path.of(arguments[0]).toAbsolutePath().normalize();
        Path viteCliPath = frontendDirectory.resolve(VITE_CLI_RELATIVE_PATH);
        String apiProxyTarget = arguments[1];
        String port = arguments[2];

        validateFrontendRuntime(frontendDirectory, viteCliPath, port);

        Process viteProcess = startVite(frontendDirectory, viteCliPath, apiProxyTarget, port);
        Runtime.getRuntime().addShutdownHook(new Thread(
                () -> stopVite(viteProcess),
                "vite-development-server-shutdown"
        ));

        int viteExitCode = viteProcess.waitFor();
        if (viteExitCode != 0) {
            throw new IllegalStateException("Vite exited with code " + viteExitCode + ".");
        }

    }

    /**
     * Fails before starting a detached process when dependencies were not
     * installed, an invalid frontend directory was configured or the port is
     * not numeric.
     */
    private static void validateFrontendRuntime(Path frontendDirectory, Path viteCliPath, String port) {

        if (!Files.isDirectory(frontendDirectory)) {
            throw new IllegalArgumentException("Frontend directory does not exist: " + frontendDirectory);
        }
        if (!Files.isRegularFile(viteCliPath)) {
            throw new IllegalStateException("Vite is not installed. Run npm install once in: " + frontendDirectory);
        }
        try {
            int vitePort = Integer.parseInt(port);
            if (vitePort < 1 || vitePort > 65535) {
                throw new IllegalArgumentException("Vite port must be between 1 and 65535.");
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Vite port must be numeric: " + port, exception);
        }

    }

    /**
     * Starts the Vite CLI as a direct Node child. The strict port option avoids
     * a Community process silently occupying Enterprise's development port.
     */
    private static Process startVite(
            Path frontendDirectory,
            Path viteCliPath,
            String apiProxyTarget,
            String port
    ) throws IOException {

        ProcessBuilder viteProcessBuilder = new ProcessBuilder(
                NODE_EXECUTABLE,
                viteCliPath.toString(),
                "--host", "127.0.0.1",
                "--port", port,
                "--strictPort"
        );
        viteProcessBuilder.directory(frontendDirectory.toFile());
        viteProcessBuilder.inheritIO();

        Map<String, String> environment = viteProcessBuilder.environment();
        environment.put("VITE_API_PROXY_TARGET", apiProxyTarget);
        environment.put("VITE_PUBLIC_BASE", "/app/");

        return viteProcessBuilder.start();

    }

    /**
     * Gives Vite a short graceful shutdown window, then forcibly releases the
     * development port if it did not exit on its own.
     */
    private static void stopVite(Process viteProcess) {

        if (!viteProcess.isAlive()) {
            return;
        }

        viteProcess.destroy();
        try {
            if (!viteProcess.waitFor(3, TimeUnit.SECONDS)) {
                viteProcess.destroyForcibly();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            viteProcess.destroyForcibly();
        }

    }

}
