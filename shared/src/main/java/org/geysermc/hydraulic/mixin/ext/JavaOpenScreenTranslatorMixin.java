package org.geysermc.hydraulic.mixin.ext;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.kyori.adventure.text.Component;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.InventoryTranslator;
import org.geysermc.geyser.translator.protocol.java.inventory.JavaOpenScreenTranslator;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.hydraulic.compat.runtime.CompatibilityRuntimeDiagnostics;
import org.geysermc.hydraulic.compat.runtime.MenuPatchTranslatorFactory;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = JavaOpenScreenTranslator.class, remap = false)
public abstract class JavaOpenScreenTranslatorMixin {
    @WrapOperation(
        method = "translate(Lorg/geysermc/geyser/session/GeyserSession;Lorg/geysermc/mcprotocollib/protocol/packet/ingame/clientbound/inventory/ClientboundOpenScreenPacket;)V",
        at = @At(
            value = "INVOKE",
            target = "Lorg/geysermc/geyser/translator/inventory/InventoryTranslator;inventoryTranslator(Lorg/geysermc/mcprotocollib/protocol/data/game/inventory/ContainerType;)Lorg/geysermc/geyser/translator/inventory/InventoryTranslator;"
        )
    )
    private InventoryTranslator<?> hydraulic$reportUnsupportedMenu(
        ContainerType containerType,
        Operation<InventoryTranslator<?>> original,
        GeyserSession session,
        ClientboundOpenScreenPacket packet
    ) {
        InventoryTranslator<?> translator = original.call(containerType);
        if (translator == null) {
            CompatibilityRegistry compatibilityRegistry = CompatibilityRuntimeDiagnostics.currentRegistry();
            String javaIdentifier = CompatibilityRuntimeDiagnostics.resolveJavaMenuIdentifier(session, packet.getContainerId());
            InventoryTranslator<?> fallbackTranslator = MenuPatchTranslatorFactory.create(javaIdentifier, compatibilityRegistry);
            if (fallbackTranslator != null) {
                return fallbackTranslator;
            }

            CompatibilityRuntimeDiagnostics.reportUnsupportedMenuOpen(containerType, title(packet.getTitle(), session), javaIdentifier);
        }
        return translator;
    }

    private static String title(Component component, GeyserSession session) {
        return component == null ? "" : MessageTranslator.convertMessage(component, session.locale());
    }
}