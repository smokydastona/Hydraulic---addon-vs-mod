package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.server.level.ServerPlayer;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.hydraulic.HydraulicImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Resolves runtime targets in the current Java level for a Bedrock-backed Geyser session. */
public final class GeyserSessionRuntimeTargetSource implements RuntimeTargetDiscovery.TargetSource {
    private final GeyserSession session;

    public GeyserSessionRuntimeTargetSource(@NotNull GeyserSession session) {
        this.session = session;
    }

    @Override
    @Nullable
    public RuntimeTargetDiscovery.Target targetAt(@NotNull RuntimeTargetDiscovery.Position position) {
        try {
            ServerPlayer player = HydraulicImpl.instance().server().getPlayerList().getPlayer(this.session.javaUuid());
            if (player == null) {
                return null;
            }
            return new MinecraftRuntimeTargetSource(player.level()).targetAt(position);
        } catch (IllegalStateException ignored) {
            return null;
        }
    }
}
