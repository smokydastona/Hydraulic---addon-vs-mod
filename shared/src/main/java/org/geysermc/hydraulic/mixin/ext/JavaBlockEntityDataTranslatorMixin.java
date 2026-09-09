package org.geysermc.hydraulic.mixin.ext;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator;
import org.geysermc.geyser.translator.level.block.entity.EmptyBlockEntityTranslator;
import org.geysermc.geyser.translator.protocol.java.level.JavaBlockEntityDataTranslator;
import org.geysermc.hydraulic.compat.runtime.CompatibilityRuntimeDiagnostics;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundBlockEntityDataPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = JavaBlockEntityDataTranslator.class, remap = false)
public abstract class JavaBlockEntityDataTranslatorMixin {
    @WrapOperation(
        method = "translate(Lorg/geysermc/geyser/session/GeyserSession;Lorg/geysermc/mcprotocollib/protocol/packet/ingame/clientbound/level/ClientboundBlockEntityDataPacket;)V",
        at = @At(
            value = "INVOKE",
            target = "Lorg/geysermc/geyser/util/BlockEntityUtils;getBlockEntityTranslator(Lorg/geysermc/mcprotocollib/protocol/data/game/level/block/BlockEntityType;)Lorg/geysermc/geyser/translator/level/block/entity/BlockEntityTranslator;"
        )
    )
    private BlockEntityTranslator hydraulic$reportUnsupportedBlockEntityData(
        BlockEntityType blockEntityType,
        Operation<BlockEntityTranslator> original,
        GeyserSession session,
        ClientboundBlockEntityDataPacket packet
    ) {
        BlockEntityTranslator translator = original.call(blockEntityType);
        if (translator instanceof EmptyBlockEntityTranslator) {
            CompatibilityRuntimeDiagnostics.reportUnsupportedBlockEntityData(session, blockEntityType, packet.getPosition());
        }
        return translator;
    }
}