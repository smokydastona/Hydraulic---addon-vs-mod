package org.geysermc.hydraulic.companion;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds a content-addressed {@code .mcpack} archive from a companion's resource pack
 * directory, reusing an existing cached build when the directory contents are unchanged.
 */
public final class CompanionPackBuilder {
    private final Logger logger;
    private final Path cacheDirectory;

    public CompanionPackBuilder(@NotNull Logger logger, @NotNull Path cacheDirectory) {
        this.logger = logger;
        this.cacheDirectory = cacheDirectory;
    }

    @NotNull
    public CompanionBuildResult build(@NotNull String companionId, @NotNull Path resourcePackDirectory) throws IOException {
        Files.createDirectories(this.cacheDirectory);
        String fingerprint = CompanionFingerprint.sha256Directory(resourcePackDirectory);
        String shortHash = fingerprint.substring(0, Math.min(16, fingerprint.length()));
        Path mcpackPath = this.cacheDirectory.resolve(companionId + "_" + shortHash + ".mcpack");

        if (Files.isRegularFile(mcpackPath)) {
            return new CompanionBuildResult(companionId, mcpackPath, fingerprint, Files.size(mcpackPath), false);
        }

        removeStaleBuilds(companionId);

        Path tempPath = this.cacheDirectory.resolve(companionId + "_" + shortHash + ".mcpack.tmp");
        List<Path> files = new ArrayList<>();
        Files.walkFileTree(resourcePackDirectory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                files.add(file);
                return FileVisitResult.CONTINUE;
            }
        });
        files.sort((a, b) -> resourcePackDirectory.relativize(a).toString().replace('\\', '/')
                .compareTo(resourcePackDirectory.relativize(b).toString().replace('\\', '/')));

        try (OutputStream fileOut = Files.newOutputStream(tempPath);
             ZipOutputStream zipOut = new ZipOutputStream(fileOut)) {
            for (Path file : files) {
                String relative = resourcePackDirectory.relativize(file).toString().replace('\\', '/');
                ZipEntry entry = new ZipEntry(relative);
                entry.setTime(0L);
                zipOut.putNextEntry(entry);
                Files.copy(file, zipOut);
                zipOut.closeEntry();
            }
        } catch (IOException e) {
            Files.deleteIfExists(tempPath);
            throw e;
        }

        Files.move(tempPath, mcpackPath, StandardCopyOption.REPLACE_EXISTING);
        long size = Files.size(mcpackPath);
        this.logger.info("Built companion resource pack '{}' ({} bytes, fingerprint {})", companionId, size, shortHash);
        return new CompanionBuildResult(companionId, mcpackPath, fingerprint, size, true);
    }

    private void removeStaleBuilds(@NotNull String companionId) throws IOException {
        if (!Files.isDirectory(this.cacheDirectory)) {
            return;
        }

        try (var stream = Files.list(this.cacheDirectory)) {
            stream.filter(path -> {
                String name = path.getFileName().toString();
                return name.startsWith(companionId + "_") && name.endsWith(".mcpack");
            }).forEach(stale -> {
                try {
                    Files.deleteIfExists(stale);
                } catch (IOException e) {
                    this.logger.warn("Failed to remove stale companion pack {}", stale, e);
                }
            });
        }
    }
}
