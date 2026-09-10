package org.geysermc.hydraulic.pack;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.geysermc.pack.converter.util.JsonMappings;
import org.geysermc.pack.converter.type.texture.TextureConverter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class PackValidator {
    @NotNull
    PackValidationReport.ModValidation validate(@NotNull Path packPath) {
        return this.validate(packPath, TextureExpectations.empty());
    }

    @NotNull
    PackValidationReport.ModValidation validate(@NotNull Path packPath, @NotNull TextureExpectations textureExpectations) {
        long started = System.currentTimeMillis();
        List<PackValidationReport.ValidationMessage> errors = new ArrayList<>();
        List<PackValidationReport.ValidationMessage> warnings = new ArrayList<>();
        Set<String> manualActions = new LinkedHashSet<>();

        if (!Files.exists(packPath)) {
            errors.add(new PackValidationReport.ValidationMessage(
                "pack.output.missing",
                "Generated pack output was not written.",
                packPath.toString()
            ));
            manualActions.add("Inspect the conversion logs for this mod and confirm the generated pack path is writable.");
            return new PackValidationReport.ModValidation(
                packPath.toString(),
                false,
                false,
                System.currentTimeMillis() - started,
                errors,
                warnings,
                List.copyOf(manualActions)
            );
        }

        try (ZipFile zip = new ZipFile(packPath.toFile())) {
            Map<String, String> archiveTextureEntries = new LinkedHashMap<>();
            ZipEntry manifestEntry = zip.getEntry("manifest.json");
            if (manifestEntry == null) {
                errors.add(new PackValidationReport.ValidationMessage(
                    "pack.manifest.missing",
                    "Generated pack is missing manifest.json.",
                    "manifest.json"
                ));
                manualActions.add("Verify that pack packaging wrote a Bedrock manifest for this mod.");
            } else {
                validateManifest(zip, manifestEntry, errors, manualActions);
            }

            if (zip.getEntry("pack_icon.png") == null) {
                warnings.add(new PackValidationReport.ValidationMessage(
                    "pack.icon.missing",
                    "Generated pack does not include pack_icon.png.",
                    "pack_icon.png"
                ));
                manualActions.add("Add a pack icon if this pack is intended for inspection or distribution outside local testing.");
            }

            boolean hasContent = zip.stream()
                .filter(entry -> !entry.isDirectory())
                .map(ZipEntry::getName)
                .anyMatch(name -> !name.equals("manifest.json") && !name.equals("pack_icon.png"));
            if (!hasContent) {
                errors.add(new PackValidationReport.ValidationMessage(
                    "pack.content.empty",
                    "Generated pack contains no content files beyond manifest scaffolding.",
                    packPath.toString()
                ));
                manualActions.add("Confirm that this mod produced convertible Bedrock assets before registering the generated pack.");
            }

            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".json")) {
                    continue;
                }

                try (Reader reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                    JsonParser.parseReader(reader);
                } catch (IOException | JsonParseException e) {
                    errors.add(new PackValidationReport.ValidationMessage(
                        "pack.json.invalid",
                        "Generated JSON is not parseable: " + e.getMessage(),
                        entry.getName()
                    ));
                    manualActions.add("Inspect invalid generated JSON entries before shipping or registering this pack.");
                }
            }

            collectTextureEntries(zip, archiveTextureEntries);
            validateTextureCoverage(textureExpectations, archiveTextureEntries, errors, warnings, manualActions);
        } catch (IOException e) {
            errors.add(new PackValidationReport.ValidationMessage(
                "pack.archive.unreadable",
                "Generated pack archive could not be read: " + e.getMessage(),
                packPath.toString()
            ));
            manualActions.add("Inspect the generated pack archive and packaging logs for corruption or file locking.");
        }

        return new PackValidationReport.ModValidation(
            packPath.toString(),
            true,
            errors.isEmpty(),
            System.currentTimeMillis() - started,
            errors,
            warnings,
            List.copyOf(manualActions)
        );
    }

    @NotNull
    PackValidationReport.ModValidation conversionFailed(@NotNull Path packPath, @NotNull String code, @NotNull String message, @NotNull String manualAction) {
        return new PackValidationReport.ModValidation(
            packPath.toString(),
            false,
            false,
            0,
            List.of(new PackValidationReport.ValidationMessage(code, message, packPath.toString())),
            List.of(),
            List.of(manualAction)
        );
    }

    private void validateManifest(
        @NotNull ZipFile zip,
        @NotNull ZipEntry manifestEntry,
        @NotNull List<PackValidationReport.ValidationMessage> errors,
        @NotNull Set<String> manualActions
    ) {
        try (Reader reader = new InputStreamReader(zip.getInputStream(manifestEntry), StandardCharsets.UTF_8)) {
            JsonObject manifest = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject header = manifest.has("header") && manifest.get("header").isJsonObject() ? manifest.getAsJsonObject("header") : null;
            if (header == null) {
                errors.add(new PackValidationReport.ValidationMessage(
                    "pack.manifest.header_missing",
                    "manifest.json is missing a valid header object.",
                    "manifest.json"
                ));
                manualActions.add("Regenerate the pack manifest so Bedrock metadata includes a valid header.");
            } else {
                requireNonBlank(header, "name", errors, manualActions);
                requireNonBlank(header, "uuid", errors, manualActions);
                requireVersionArray(header, "version", errors, manualActions);
            }

            if (!manifest.has("modules") || !manifest.get("modules").isJsonArray()) {
                errors.add(new PackValidationReport.ValidationMessage(
                    "pack.manifest.modules_missing",
                    "manifest.json is missing a valid modules array.",
                    "manifest.json"
                ));
                manualActions.add("Regenerate the pack manifest so Bedrock can identify at least one module.");
                return;
            }

            JsonArray modules = manifest.getAsJsonArray("modules");
            if (modules.isEmpty()) {
                errors.add(new PackValidationReport.ValidationMessage(
                    "pack.manifest.modules_empty",
                    "manifest.json declares no modules.",
                    "manifest.json"
                ));
                manualActions.add("Regenerate the pack manifest so Bedrock can identify at least one module.");
            }
        } catch (IllegalStateException | IOException | JsonParseException e) {
            errors.add(new PackValidationReport.ValidationMessage(
                "pack.manifest.invalid",
                "manifest.json is not a valid JSON object: " + e.getMessage(),
                "manifest.json"
            ));
            manualActions.add("Inspect the generated manifest.json before shipping this pack.");
        }
    }

    private void requireNonBlank(
        @NotNull JsonObject json,
        @NotNull String field,
        @NotNull List<PackValidationReport.ValidationMessage> errors,
        @NotNull Set<String> manualActions
    ) {
        if (!json.has(field) || !json.get(field).isJsonPrimitive() || json.get(field).getAsString().isBlank()) {
            errors.add(new PackValidationReport.ValidationMessage(
                "pack.manifest.header." + field,
                "manifest.json header is missing a non-empty " + field + ".",
                "manifest.json"
            ));
            manualActions.add("Regenerate the pack manifest so required Bedrock header metadata is populated.");
        }
    }

    private void requireVersionArray(
        @NotNull JsonObject json,
        @NotNull String field,
        @NotNull List<PackValidationReport.ValidationMessage> errors,
        @NotNull Set<String> manualActions
    ) {
        if (!json.has(field) || !json.get(field).isJsonArray() || json.getAsJsonArray(field).isEmpty()) {
            errors.add(new PackValidationReport.ValidationMessage(
                "pack.manifest.header." + field,
                "manifest.json header is missing a non-empty version array.",
                "manifest.json"
            ));
            manualActions.add("Regenerate the pack manifest so required Bedrock header metadata is populated.");
        }
    }

    private static void collectTextureEntries(@NotNull ZipFile zip, @NotNull Map<String, String> archiveTextureEntries) {
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }

            String normalized = normalizePath(entry.getName());
            if (!normalized.startsWith("textures/") || !(normalized.endsWith(".png") || normalized.endsWith(".tga"))) {
                continue;
            }
            if (normalized.equals("pack_icon.png")) {
                continue;
            }

            archiveTextureEntries.put(normalized, entry.getName());
        }
    }

    private static void validateTextureCoverage(
        @NotNull TextureExpectations textureExpectations,
        @NotNull Map<String, String> archiveTextureEntries,
        @NotNull List<PackValidationReport.ValidationMessage> errors,
        @NotNull List<PackValidationReport.ValidationMessage> warnings,
        @NotNull Set<String> manualActions
    ) {
        if (textureExpectations.requiredArchiveEntries().isEmpty()) {
            return;
        }

        List<String> missingEntries = textureExpectations.requiredArchiveEntries().stream()
            .filter(expected -> !hasRequiredTextureOutput(expected, archiveTextureEntries))
            .toList();
        for (String missingEntry : missingEntries) {
            errors.add(new PackValidationReport.ValidationMessage(
                "pack.texture.required_missing",
                "Generated pack is missing a required selected texture output.",
                missingEntry
            ));
        }
        if (!missingEntries.isEmpty()) {
            manualActions.add("Inspect texture conversion and dependency selection output for missing generated textures before shipping this pack.");
        }

        List<String> unreferencedEntries = archiveTextureEntries.keySet().stream()
            .filter(entry -> !isRequiredTextureOutput(entry, textureExpectations.requiredArchiveEntries()))
            .toList();
        for (String unreferencedEntry : unreferencedEntries) {
            warnings.add(new PackValidationReport.ValidationMessage(
                "pack.texture.unreferenced_output",
                "Generated pack still contains a texture file that was not selected by the texture dependency graph.",
                archiveTextureEntries.get(unreferencedEntry)
            ));
        }
        if (!unreferencedEntries.isEmpty()) {
            manualActions.add("Review texture dependency tracking for this mod; unreferenced generated textures indicate pruning is incomplete.");
        }
    }

    private static boolean hasRequiredTextureOutput(@NotNull String expected, @NotNull Map<String, String> archiveTextureEntries) {
        return archiveTextureEntries.containsKey(expected)
            || equipmentTransformOutput(expected).map(archiveTextureEntries::containsKey).orElse(false);
    }

    private static boolean isRequiredTextureOutput(@NotNull String entry, @NotNull Set<String> requiredEntries) {
        if (requiredEntries.contains(entry)) {
            return true;
        }
        return requiredEntries.stream()
            .map(PackValidator::equipmentTransformOutput)
            .flatMap(java.util.Optional::stream)
            .anyMatch(entry::equals);
    }

    @NotNull
    private static java.util.Optional<String> equipmentTransformOutput(@NotNull String expected) {
        String normalized = normalizePath(expected);
        String entityPrefix = "textures/entity/";
        if (!normalized.startsWith(entityPrefix) || !normalized.endsWith(".png")) {
            return java.util.Optional.empty();
        }
        String namespacedPath = normalized.substring(entityPrefix.length(), normalized.length() - ".png".length());
        int namespaceSeparator = namespacedPath.indexOf('/');
        if (namespaceSeparator < 0) {
            return java.util.Optional.empty();
        }
        String namespace = namespacedPath.substring(0, namespaceSeparator);
        String source = "entity/" + namespacedPath.substring(namespaceSeparator + 1);
        if (!source.startsWith("entity/equipment/")) {
            return java.util.Optional.empty();
        }
        List<String> mapped = JsonMappings.getMapping("textures").map(source);
        if (mapped.isEmpty()) {
            return java.util.Optional.empty();
        }
        String mappedValue = mapped.getFirst();
        String mappedDirectory = mappedValue.substring(0, mappedValue.indexOf('/'));
        String mappedRemaining = mappedValue.substring(mappedValue.indexOf('/') + 1);
        String outputDirectory = TextureConverter.DIRECTORY_LOCATIONS.getOrDefault(mappedDirectory, mappedDirectory);
        return java.util.Optional.of(normalizePath("textures/" + outputDirectory + "/" + namespace + "/" + mappedRemaining + ".png"));
    }

    @NotNull
    private static String normalizePath(@NotNull String path) {
        return path.replace('\\', '/').toLowerCase(Locale.ROOT);
    }

    record TextureExpectations(@NotNull Set<String> requiredArchiveEntries) {
        TextureExpectations {
            requiredArchiveEntries = Set.copyOf(requiredArchiveEntries);
        }

        @NotNull
        static TextureExpectations empty() {
            return new TextureExpectations(Set.of());
        }

        @NotNull
        static TextureExpectations ofArchiveEntries(@NotNull Collection<String> archiveEntries) {
            Set<String> normalized = new LinkedHashSet<>();
            for (String archiveEntry : archiveEntries) {
                normalized.add(normalizePath(archiveEntry));
            }
            return new TextureExpectations(normalized);
        }
    }
}