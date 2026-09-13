package org.geysermc.hydraulic.companion;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Classifies a companion's declared capabilities against what Hydraulic can actually
 * deliver given Geyser's real Bedrock protocol boundary: Geyser can push resource
 * packs to a connecting Bedrock session, but it does not install or execute Bedrock
 * behavior-pack scripts on the client. A capability is only ever reported as
 * server-bridge-supported when Hydraulic has a concrete, implemented Java-side
 * bridge for it; everything else that needs server involvement is reported as
 * unsupported rather than assumed to work.
 */
public final class CompanionCapabilityClassifier {
    /**
     * Capability ids that Hydraulic implements a real Java-side runtime bridge for.
     */
    private static final Set<String> KNOWN_SERVER_BRIDGE_CAPABILITIES = Set.of(
            "companion_detection_signal",
            "hydraulic_bridge_signal"
    );

    @NotNull
    public List<CompanionCapabilityResult> classify(@NotNull CompanionManifest manifest, boolean signalBridgeInstalled) {
        List<CompanionCapabilityResult> results = new ArrayList<>();
        for (CompanionCapability capability : manifest.capabilities()) {
            results.add(classifyOne(capability, signalBridgeInstalled));
        }
        return results;
    }

    @NotNull
    private CompanionCapabilityResult classifyOne(@NotNull CompanionCapability capability, boolean signalBridgeInstalled) {
        if (!capability.requiresServerBridge()) {
            return new CompanionCapabilityResult(
                    capability,
                    CompanionCapabilityStatus.CLIENT_LOCAL_SUPPORTED,
                    "Implemented entirely by the companion's own client-side Script API using standard Bedrock "
                            + "world/inventory/entity state that Geyser already translates; no Java-side runtime bridge is required."
            );
        }

        boolean known = KNOWN_SERVER_BRIDGE_CAPABILITIES.contains(capability.id().toLowerCase(Locale.ROOT));
        if (known && signalBridgeInstalled) {
            return new CompanionCapabilityResult(
                    capability,
                    CompanionCapabilityStatus.SERVER_SIGNAL_SUPPORTED,
                    "Backed by the Hydraulic-maintained '" + CompanionSignalBridge.OBJECTIVE_NAME + "' scoreboard "
                            + "objective, which Geyser translates to the Bedrock protocol like any vanilla scoreboard "
                            + "objective, matching this companion's server-signal detection contract."
            );
        }

        return new CompanionCapabilityResult(
                capability,
                CompanionCapabilityStatus.UNSUPPORTED_NO_BRIDGE,
                "This capability is declared as requiring a Java-side runtime bridge, but Geyser does not install or "
                        + "execute Bedrock behavior-pack scripts on the client, and Hydraulic does not implement an "
                        + "equivalent Java-side bridge for it yet. It will not function until one is added."
        );
    }
}
