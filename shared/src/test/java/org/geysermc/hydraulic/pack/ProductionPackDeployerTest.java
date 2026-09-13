package org.geysermc.hydraulic.pack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionPackDeployerTest {
    @TempDir
    Path tempDir;

    @Test
    void deploysPacksAndGeneratesDistributionManifest() throws IOException {
        Path storageDir = this.tempDir.resolve("storage");
        Path destDir = this.tempDir.resolve("geyser_packs");
        Files.createDirectories(storageDir.resolve("testmod"));

        Path packFile = storageDir.resolve("testmod").resolve("testmod.mcpack");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(packFile))) {
            zip.putNextEntry(new ZipEntry("manifest.json"));
            zip.write("""
                {
                  "format_version": 2,
                  "header": {
                    "name": "Test Mod Resource Pack",
                    "uuid": "11111111-2222-3333-4444-555555555555",
                    "version": [1, 0, 0]
                  },
                  "modules": []
                }
                """.getBytes());
            zip.closeEntry();
        }

        ProductionPackDeployer deployer = new ProductionPackDeployer(
            LoggerFactory.getLogger("ProductionPackDeployerTest"),
            storageDir,
            destDir
        );

        ProductionPackDeployer.DeploymentResult result = deployer.deploy();

        assertEquals(1, result.count());
        assertTrue(result.totalSizeBytes() > 0);
        assertTrue(Files.isRegularFile(destDir.resolve("testmod.mcpack")));
        assertTrue(Files.isRegularFile(destDir.resolve("pack-distribution-manifest.json")));

        ProductionPackDeployer.DeployedPackInfo info = result.deployedPacks().getFirst();
        assertEquals("testmod", info.modId());
        assertEquals("Test Mod Resource Pack", info.packName());
        assertEquals("11111111-2222-3333-4444-555555555555", info.packUuid());
        assertNotNull(info.sha256());
    }
}
