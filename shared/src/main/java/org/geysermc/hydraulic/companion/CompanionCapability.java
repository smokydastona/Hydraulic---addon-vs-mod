package org.geysermc.hydraulic.companion;

/**
 * A single behavior/feature capability declared by a companion package's behavior pack
 * inside {@code companion.json}.
 *
 * @param id                   stable capability identifier (e.g. {@code companion_detection_signal})
 * @param description          human-readable description used in {@code companion-report.json}
 * @param requiresServerBridge whether this capability needs authoritative Java-side involvement
 *                             to function, as opposed to being fully self-contained inside the
 *                             companion's own client-side Script API
 */
public record CompanionCapability(String id, String description, boolean requiresServerBridge) {
}
