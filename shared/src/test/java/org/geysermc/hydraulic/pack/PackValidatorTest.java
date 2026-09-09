package org.geysermc.hydraulic.pack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackValidatorTest {
    @TempDir
    Path tempDir;

    @Test
    void validatesPackAndReportsWarnings() throws IOException {
        Path pack = this.tempDir.resolve("examplemod.mcpack");
        writeZip(pack,
            entry("manifest.json", """
                {"header":{"name":"Example","uuid":"123e4567-e89b-12d3-a456-426614174000","version":[1,0,0]},"modules":[{"type":"resources","uuid":"123e4567-e89b-12d3-a456-426614174001","version":[1,0,0]}]}
                """),
            entry("blocks/example.json", "{}")
        );

        PackValidationReport.ModValidation validation = new PackValidator().validate(pack);

        assertTrue(validation.created());
        assertTrue(validation.valid());
        assertEquals(0, validation.errorCount());
        assertEquals(1, validation.warningCount());
        assertEquals("pack.icon.missing", validation.warnings().get(0).code());
    }

    @Test
    void reportsErrorsForMissingOutput() {
        PackValidationReport.ModValidation validation = new PackValidator().validate(this.tempDir.resolve("missing.mcpack"));

        assertFalse(validation.created());
        assertFalse(validation.valid());
        assertEquals(1, validation.errorCount());
        assertEquals("pack.output.missing", validation.errors().get(0).code());
        assertEquals(1, validation.manualActionCount());
    }

    @Test
    void reportsErrorsForInvalidGeneratedJson() throws IOException {
        Path pack = this.tempDir.resolve("broken.mcpack");
        writeZip(pack,
            entry("manifest.json", """
                {"header":{"name":"Example","uuid":"123e4567-e89b-12d3-a456-426614174000","version":[1,0,0]},"modules":[{"type":"resources","uuid":"123e4567-e89b-12d3-a456-426614174001","version":[1,0,0]}]}
                """),
            entry("items/example.json", "{broken")
        );

        PackValidationReport.ModValidation validation = new PackValidator().validate(pack);

        assertFalse(validation.valid());
        assertTrue(validation.errors().stream().anyMatch(message -> message.code().equals("pack.json.invalid")));
    }

    private static void writeZip(Path output, ZipContent... contents) throws IOException {
        try (ZipOutputStream stream = new ZipOutputStream(Files.newOutputStream(output))) {
            for (ZipContent content : contents) {
                stream.putNextEntry(new ZipEntry(content.path()));
                stream.write(content.content().getBytes(StandardCharsets.UTF_8));
                stream.closeEntry();
            }
        }
    }

    private static ZipContent entry(String path, String content) {
        return new ZipContent(path, content);
    }

    private record ZipContent(String path, String content) {
    }
}