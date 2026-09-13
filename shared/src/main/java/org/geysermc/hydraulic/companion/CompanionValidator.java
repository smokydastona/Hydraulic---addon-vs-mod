package org.geysermc.hydraulic.companion;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates a discovered companion package directory against the Phlodgate companion
 * schema and the presence of well-formed Bedrock resource/behavior pack manifests.
 * One bad companion package never prevents other companion packages from loading.
 */
public final class CompanionValidator {
    private static final Gson GSON = new Gson();

    @NotNull
    public CompanionPackage validate(@NotNull Path companionDirectory) {
        String discoveredId = companionDirectory.getFileName().toString();
        List<CompanionValidationIssue> issues = new ArrayList<>();

        Path manifestPath = companionDirectory.resolve("companion.json");
        if (!Files.isRegularFile(manifestPath)) {
            issues.add(CompanionValidationIssue.error("Missing companion.json descriptor"));
            return new CompanionPackage(discoveredId, companionDirectory, null, null, null, issues);
        }

        JsonObject json;
        try (Reader reader = Files.newBufferedReader(manifestPath, StandardCharsets.UTF_8)) {
            json = GSON.fromJson(reader, JsonObject.class);
        } catch (IOException | JsonSyntaxException e) {
            issues.add(CompanionValidationIssue.error("Failed to parse companion.json: " + e.getMessage()));
            return new CompanionPackage(discoveredId, companionDirectory, null, null, null, issues);
        }

        if (json == null) {
            issues.add(CompanionValidationIssue.error("companion.json is empty or not a JSON object"));
            return new CompanionPackage(discoveredId, companionDirectory, null, null, null, issues);
        }

        CompanionManifest manifest = CompanionManifest.fromJson(json);
        if (manifest.id() == null || manifest.id().isBlank()) {
            issues.add(CompanionValidationIssue.error("companion.json is missing a required 'id' field"));
        }

        Path resourcePackPath = companionDirectory.resolve(manifest.resourcePackDir());
        if (!Files.isDirectory(resourcePackPath)) {
            issues.add(CompanionValidationIssue.error("Resource pack directory '" + manifest.resourcePackDir() + "' does not exist"));
            resourcePackPath = null;
        } else {
            validateBedrockManifest(resourcePackPath.resolve("manifest.json"), "resource pack", issues);
        }

        Path behaviorPackPath = null;
        if (manifest.behaviorPackDir() != null) {
            Path candidate = companionDirectory.resolve(manifest.behaviorPackDir());
            if (!Files.isDirectory(candidate)) {
                issues.add(CompanionValidationIssue.warning(
                        "Declared behavior pack directory '" + manifest.behaviorPackDir() + "' does not exist; "
                                + "behavior capability reporting will be skipped for this companion"));
            } else {
                behaviorPackPath = candidate;
                validateBedrockManifest(candidate.resolve("manifest.json"), "behavior pack", issues);
            }
        }

        return new CompanionPackage(discoveredId, companionDirectory, manifest, resourcePackPath, behaviorPackPath, issues);
    }

    private void validateBedrockManifest(@NotNull Path manifestPath, @NotNull String kind, @NotNull List<CompanionValidationIssue> issues) {
        if (!Files.isRegularFile(manifestPath)) {
            issues.add(CompanionValidationIssue.error("Missing " + kind + " manifest.json at " + manifestPath));
            return;
        }

        try (Reader reader = Files.newBufferedReader(manifestPath, StandardCharsets.UTF_8)) {
            JsonObject manifestJson = GSON.fromJson(reader, JsonObject.class);
            if (manifestJson == null || !manifestJson.has("header") || !manifestJson.get("header").isJsonObject()) {
                issues.add(CompanionValidationIssue.error("Invalid " + kind + " manifest.json: missing 'header'"));
                return;
            }

            JsonObject header = manifestJson.getAsJsonObject("header");
            if (!header.has("uuid") || !header.get("uuid").isJsonPrimitive()) {
                issues.add(CompanionValidationIssue.error("Invalid " + kind + " manifest.json: missing 'header.uuid'"));
            }
            if (!header.has("version") || !header.get("version").isJsonArray()) {
                issues.add(CompanionValidationIssue.error("Invalid " + kind + " manifest.json: missing 'header.version'"));
            }
            if (!manifestJson.has("modules") || !manifestJson.get("modules").isJsonArray()
                    || manifestJson.getAsJsonArray("modules").size() == 0) {
                issues.add(CompanionValidationIssue.error("Invalid " + kind + " manifest.json: missing or empty 'modules'"));
            }
        } catch (IOException | JsonSyntaxException e) {
            issues.add(CompanionValidationIssue.error("Failed to parse " + kind + " manifest.json: " + e.getMessage()));
        }
    }
}
