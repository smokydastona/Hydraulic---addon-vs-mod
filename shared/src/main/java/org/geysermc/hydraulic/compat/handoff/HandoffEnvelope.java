package org.geysermc.hydraulic.compat.handoff;

import org.geysermc.hydraulic.cache.ArtifactCache;
import org.geysermc.hydraulic.compat.CompatibilityReport;
import org.geysermc.hydraulic.compat.ContentInventory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Envelope for compatibility report handoff operations.
 *
 * This envelope carries the compatibility report along with necessary metadata
 * for transport, processing, and deduplication. The envelope is designed to be
 * provider-neutral, allowing different transport backends (GitHub, custom APIs, etc.)
 * to consume the same envelope format.
 */
public record HandoffEnvelope(
    @NotNull String envelopeId,
    @NotNull String compatibilityFingerprint,
    @NotNull ArtifactCache.CompatibilityManifest manifest,
    @NotNull ContentInventory inventory,
    @NotNull CompatibilityReport report,
    @NotNull String hydraulicVersion,
    @NotNull String targetMinecraftVersion,
    @NotNull String targetBedrockVersion,
    @NotNull String geyserVersion,
    @NotNull Map<String, String> metadata,
    long createdEpochMillis
) {
    @NotNull
    public static HandoffEnvelope create(
        @NotNull ArtifactCache.CompatibilityManifest manifest,
        @NotNull ContentInventory inventory,
        @NotNull CompatibilityReport report,
        @NotNull String hydraulicVersion,
        @NotNull String targetMinecraftVersion,
        @NotNull String targetBedrockVersion,
        @NotNull String geyserVersion
    ) {
        return new HandoffEnvelope(
            java.util.UUID.randomUUID().toString(),
            manifest.startupKey().value(),
            manifest,
            inventory,
            report,
            hydraulicVersion,
            targetMinecraftVersion,
            targetBedrockVersion,
            geyserVersion,
            Map.of(
                "mod_count", Integer.toString(manifest.modCount()),
                "metadata_fingerprint", manifest.metadataFingerprint(),
                "engine_fingerprint", manifest.engineFingerprint(),
                "adapter_catalog_fingerprint", manifest.adapterCatalogFingerprint()
            ),
            System.currentTimeMillis()
        );
    }

    /**
     * Returns a simplified identifier for logging and tracking.
     */
    @NotNull
    public String trackingId() {
        return this.envelopeId().substring(0, 8);
    }
}