package org.geysermc.hydraulic.companion;

/**
 * How a declared companion capability is actually supported given Geyser's real
 * protocol boundary: Geyser can deliver Bedrock resource packs to a connecting
 * session, but it does not install or execute Bedrock behavior-pack scripts on
 * the client. A capability score must never be faked by treating an
 * unimplemented bridge as supported.
 */
public enum CompanionCapabilityStatus {
    /**
     * Fully implemented by the companion's own client-side Script API using
     * standard Bedrock world/entity/inventory state that Geyser already
     * translates. No Java-side runtime bridge is required.
     */
    CLIENT_LOCAL_SUPPORTED,

    /**
     * Backed by a real, implemented Hydraulic Java-side runtime signal (currently
     * the {@code phlodgate_bridge} scoreboard objective) that Geyser translates
     * to the Bedrock protocol.
     */
    SERVER_SIGNAL_SUPPORTED,

    /**
     * Declared as requiring a Java-side runtime bridge, but no such bridge is
     * implemented. Reported honestly as unsupported instead of assumed to work.
     */
    UNSUPPORTED_NO_BRIDGE
}
