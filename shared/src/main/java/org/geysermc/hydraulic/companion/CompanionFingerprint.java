package org.geysermc.hydraulic.companion;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic content fingerprinting for companion package directories, independent
 * of filesystem iteration order.
 */
public final class CompanionFingerprint {
    private CompanionFingerprint() {
    }

    @NotNull
    public static String sha256Directory(@NotNull Path directory) throws IOException {
        List<Path> files = new ArrayList<>();
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                files.add(file);
                return FileVisitResult.CONTINUE;
            }
        });
        files.sort((a, b) -> directory.relativize(a).toString().replace('\\', '/')
                .compareTo(directory.relativize(b).toString().replace('\\', '/')));

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Path file : files) {
                String relative = directory.relativize(file).toString().replace('\\', '/');
                digest.update(relative.getBytes(StandardCharsets.UTF_8));
                digest.update(Files.readAllBytes(file));
            }

            byte[] hash = digest.digest();
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 algorithm unavailable", e);
        }
    }
}
