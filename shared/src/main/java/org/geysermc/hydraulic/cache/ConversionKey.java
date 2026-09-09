package org.geysermc.hydraulic.cache;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public record ConversionKey(
    @NotNull String algorithm,
    @NotNull String modId,
    @NotNull String modVersion,
    @NotNull String hydraulicVersion,
    @NotNull String minecraftVersion,
    @NotNull String resourceFingerprint,
    int indexedFileCount,
    long indexedTotalSizeBytes,
    @NotNull String metadataFingerprint,
    @NotNull String dependencyFingerprint,
    int dependentModCount
) {
    public ConversionKey {
        algorithm = algorithm == null ? "" : algorithm;
        modId = modId == null ? "" : modId;
        modVersion = modVersion == null ? "" : modVersion;
        hydraulicVersion = hydraulicVersion == null ? "" : hydraulicVersion;
        minecraftVersion = minecraftVersion == null ? "" : minecraftVersion;
        resourceFingerprint = resourceFingerprint == null ? "" : resourceFingerprint;
        metadataFingerprint = metadataFingerprint == null ? "" : metadataFingerprint;
        dependencyFingerprint = dependencyFingerprint == null ? "" : dependencyFingerprint;
    }

    @NotNull
    public String packUuid() {
        return UUID.nameUUIDFromBytes(this.stableValue().getBytes(StandardCharsets.UTF_8)).toString();
    }

    @NotNull
    public String stableValue() {
        return String.join("|",
            this.algorithm,
            this.modId,
            this.modVersion,
            this.hydraulicVersion,
            this.minecraftVersion,
            this.resourceFingerprint,
            Integer.toString(this.indexedFileCount),
            Long.toString(this.indexedTotalSizeBytes),
            this.metadataFingerprint,
            this.dependencyFingerprint,
            Integer.toString(this.dependentModCount)
        );
    }
}