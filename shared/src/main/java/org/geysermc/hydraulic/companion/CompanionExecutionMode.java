package org.geysermc.hydraulic.companion;

import java.util.Locale;

/**
 * How a companion package's behavior pack is expected to reach the Bedrock client.
 */
public enum CompanionExecutionMode {
    /**
     * Only the resource pack is delivered through Geyser. There is no behavior
     * pack, or it is purely documentation/reference and not intended to run.
     */
    GEYSER_RESOURCE_PACK_ONLY,

    /**
     * The behavior pack is designed to be manually enabled by the player as a
     * Bedrock "Global Resource" on their own client, independent of any specific
     * server connection. Geyser never installs or executes it; Hydraulic only
     * documents its declared capabilities and delivers the paired resource pack.
     */
    CLIENT_GLOBAL_BEHAVIOR_PACK;

    public static CompanionExecutionMode parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return CLIENT_GLOBAL_BEHAVIOR_PACK;
        }

        try {
            return CompanionExecutionMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return CLIENT_GLOBAL_BEHAVIOR_PACK;
        }
    }
}
