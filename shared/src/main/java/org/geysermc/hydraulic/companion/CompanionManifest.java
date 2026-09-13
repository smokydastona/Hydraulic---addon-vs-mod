package org.geysermc.hydraulic.companion;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parsed {@code companion.json} descriptor for a Phlodgate companion package.
 * This is a Hydraulic-specific schema; it is not a Bedrock {@code manifest.json}.
 */
public record CompanionManifest(
        @Nullable String id,
        String name,
        String version,
        CompanionExecutionMode executionMode,
        String resourcePackDir,
        @Nullable String behaviorPackDir,
        List<CompanionCapability> capabilities
) {
    @NotNull
    public static CompanionManifest fromJson(@NotNull JsonObject json) {
        String id = stringOrNull(json, "id");
        String name = stringOrDefault(json, "name", id != null ? id : "unknown");
        String version = stringOrDefault(json, "version", "0.0.0");
        CompanionExecutionMode mode = CompanionExecutionMode.parse(stringOrNull(json, "executionMode"));
        String resourcePackDir = stringOrDefault(json, "resourcePack", "resource_pack");
        String behaviorPackDir = stringOrNull(json, "behaviorPack");

        List<CompanionCapability> capabilities = new ArrayList<>();
        if (json.has("capabilities") && json.get("capabilities").isJsonArray()) {
            JsonArray array = json.getAsJsonArray("capabilities");
            for (var element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }

                JsonObject capabilityJson = element.getAsJsonObject();
                String capabilityId = stringOrNull(capabilityJson, "id");
                if (capabilityId == null || capabilityId.isBlank()) {
                    continue;
                }

                String description = stringOrDefault(capabilityJson, "description", "");
                boolean requiresBridge = capabilityJson.has("requiresServerBridge")
                        && capabilityJson.get("requiresServerBridge").isJsonPrimitive()
                        && capabilityJson.get("requiresServerBridge").getAsBoolean();
                capabilities.add(new CompanionCapability(capabilityId, description, requiresBridge));
            }
        }

        return new CompanionManifest(id, name, version, mode, resourcePackDir, behaviorPackDir, Collections.unmodifiableList(capabilities));
    }

    @Nullable
    private static String stringOrNull(@NotNull JsonObject json, @NotNull String field) {
        if (json.has(field) && json.get(field).isJsonPrimitive()) {
            return json.get(field).getAsString();
        }
        return null;
    }

    @NotNull
    private static String stringOrDefault(@NotNull JsonObject json, @NotNull String field, @NotNull String fallback) {
        String value = stringOrNull(json, field);
        return value != null ? value : fallback;
    }
}
