package org.geysermc.hydraulic.companion;

/**
 * The resolved support result for a single declared {@link CompanionCapability}.
 */
public record CompanionCapabilityResult(CompanionCapability capability, CompanionCapabilityStatus status, String reason) {
}
