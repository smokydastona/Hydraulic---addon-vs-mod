package org.geysermc.hydraulic.mixin.ext;

import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.bedrock.BedrockInventoryTransactionTranslator;
import org.geysermc.hydraulic.compat.runtime.BedrockRuntimeActionRouter;
import org.geysermc.hydraulic.compat.runtime.CompatibilityRuntimeDiagnostics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BedrockInventoryTransactionTranslator.class, remap = false)
public abstract class BedrockInventoryTransactionTranslatorMixin {
    @Inject(
        method = "translate(Lorg/geysermc/geyser/session/GeyserSession;Lorg/cloudburstmc/protocol/bedrock/packet/InventoryTransactionPacket;)V",
        at = @At("HEAD")
    )
    private void hydraulic$routeRuntimeTargetDiscovery(
        GeyserSession session,
        InventoryTransactionPacket packet,
        CallbackInfo callbackInfo
    ) {
        BedrockRuntimeActionRouter.route(session, packet, CompatibilityRuntimeDiagnostics.currentRegistry());
    }
}
