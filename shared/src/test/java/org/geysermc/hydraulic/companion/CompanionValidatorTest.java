package org.geysermc.hydraulic.companion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionValidatorTest {
    private final CompanionValidator validator = new CompanionValidator();

    @Test
    void validCompanionPackageParsesManifestAndCapabilities(@TempDir Path tempDir) throws IOException {
        Path companionDir = tempDir.resolve("phlodgate");
        Files.createDirectories(companionDir.resolve("resource_pack"));
        Files.createDirectories(companionDir.resolve("behavior_pack"));

        Files.writeString(companionDir.resolve("companion.json"), """
            {
              "id": "phlodgate",
              "name": "Phlodgate Add-On",
              "version": "0.3.0",
              "resourcePack": "resource_pack",
              "behaviorPack": "behavior_pack",
              "executionMode": "client_global_behavior_pack",
              "capabilities": [
                { "id": "companion_detection_signal", "description": "signal", "requiresServerBridge": true },
                { "id": "jei_inventory_search", "description": "search", "requiresServerBridge": false }
              ]
            }
            """);
        writeBedrockManifest(companionDir.resolve("resource_pack").resolve("manifest.json"), "resources");
        writeBedrockManifest(companionDir.resolve("behavior_pack").resolve("manifest.json"), "data");

        CompanionPackage result = validator.validate(companionDir);

        assertTrue(result.isValid(), () -> "issues: " + result.issues());
        assertEquals("phlodgate", result.id());
        assertEquals(2, result.manifest().capabilities().size());
        assertEquals(companionDir.resolve("resource_pack"), result.resourcePackPath());
        assertEquals(companionDir.resolve("behavior_pack"), result.behaviorPackPath());
    }

    @Test
    void missingCompanionJsonIsRejected(@TempDir Path tempDir) throws IOException {
        Path companionDir = tempDir.resolve("broken");
        Files.createDirectories(companionDir);

        CompanionPackage result = validator.validate(companionDir);

        assertFalse(result.isValid());
        assertNull(result.manifest());
        assertTrue(result.issues().stream().anyMatch(i -> i.level() == CompanionValidationIssue.Level.ERROR));
    }

    @Test
    void missingResourcePackDirectoryIsRejected(@TempDir Path tempDir) throws IOException {
        Path companionDir = tempDir.resolve("no-rp");
        Files.createDirectories(companionDir);
        Files.writeString(companionDir.resolve("companion.json"), """
            { "id": "no-rp", "resourcePack": "resource_pack" }
            """);

        CompanionPackage result = validator.validate(companionDir);

        assertFalse(result.isValid());
        assertTrue(result.issues().stream().anyMatch(i -> i.message().contains("Resource pack directory")));
    }

    @Test
    void invalidBedrockManifestIsRejected(@TempDir Path tempDir) throws IOException {
        Path companionDir = tempDir.resolve("bad-manifest");
        Files.createDirectories(companionDir.resolve("resource_pack"));
        Files.writeString(companionDir.resolve("companion.json"), """
            { "id": "bad-manifest", "resourcePack": "resource_pack" }
            """);
        Files.writeString(companionDir.resolve("resource_pack").resolve("manifest.json"), "{ \"header\": {} }");

        CompanionPackage result = validator.validate(companionDir);

        assertFalse(result.isValid());
        assertTrue(result.issues().stream().anyMatch(i -> i.message().contains("header.uuid")));
    }

    private static void writeBedrockManifest(Path path, String moduleType) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, """
            {
              "format_version": 2,
              "header": { "name": "Test", "uuid": "11111111-1111-1111-1111-111111111111", "version": [1, 0, 0] },
              "modules": [ { "type": "%s", "uuid": "22222222-2222-2222-2222-222222222222", "version": [1, 0, 0] } ]
            }
            """.formatted(moduleType));
    }
}
