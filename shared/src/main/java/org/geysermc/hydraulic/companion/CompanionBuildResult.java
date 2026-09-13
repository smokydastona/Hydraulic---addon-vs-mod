package org.geysermc.hydraulic.companion;

import java.nio.file.Path;

/**
 * The result of building a companion's resource pack directory into a content-addressed
 * {@code .mcpack} archive.
 */
public record CompanionBuildResult(String companionId, Path mcpackPath, String sha256, long sizeBytes, boolean rebuilt) {
}
