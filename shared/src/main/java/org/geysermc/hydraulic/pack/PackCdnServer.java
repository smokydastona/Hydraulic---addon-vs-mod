package org.geysermc.hydraulic.pack;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.OutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Embedded HTTP CDN Pack Server endpoint for serving large generated Bedrock resource packs (>50MB).
 */
public final class PackCdnServer {
    private final Logger logger;
    private final Path packsDirectory;
    private final int port;
    private HttpServer server;

    public PackCdnServer(@NotNull Logger logger, @NotNull Path packsDirectory, int port) {
        this.logger = logger;
        this.packsDirectory = packsDirectory;
        this.port = port;
    }

    public void start() {
        try {
            this.server = HttpServer.create(new InetSocketAddress(this.port), 0);
            this.server.createContext("/packs/", new PackHandler(this.packsDirectory, this.logger));
            this.server.setExecutor(null);
            this.server.start();
            this.logger.info("Started HTTP CDN Pack Server on port {} serving {}", this.port, this.packsDirectory);
        } catch (IOException e) {
            this.logger.error("Failed to start HTTP CDN Pack Server on port {}", this.port, e);
        }
    }

    public void stop() {
        if (this.server != null) {
            this.server.stop(0);
            this.logger.info("Stopped HTTP CDN Pack Server");
        }
    }

    private static final class PackHandler implements HttpHandler {
        private final Path packsDirectory;
        private final Logger logger;

        private PackHandler(Path packsDirectory, Logger logger) {
            this.packsDirectory = packsDirectory;
            this.logger = logger;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            Path packFile = this.packsDirectory.resolve(fileName).normalize();

            if (!packFile.startsWith(this.packsDirectory) || !Files.isRegularFile(packFile)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            long length = Files.size(packFile);
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.sendResponseHeaders(200, length);

            try (OutputStream os = exchange.getResponseBody()) {
                Files.copy(packFile, os);
            }
        }
    }
}
