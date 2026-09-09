package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.InventoryTranslator;
import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MenuPatchTranslatorFactory {
    private MenuPatchTranslatorFactory() {
    }

    @Nullable
    public static InventoryTranslator<?> create(
        @NotNull GeyserSession session,
        @NotNull ClientboundOpenScreenPacket packet,
        @NotNull CompatibilityRegistry compatibilityRegistry
    ) {
        String javaIdentifier = CompatibilityRuntimeDiagnostics.resolveJavaMenuIdentifier(session, packet.getContainerId());
        if (javaIdentifier == null) {
            return null;
        }

        MenuPatchTemplate template = compatibilityRegistry.mappingResolver().menuPatchTemplate(Identifier.parse(javaIdentifier));
        if (template == null) {
            return null;
        }

        return InventoryTranslator.inventoryTranslator(ContainerType.valueOf(template.fallbackContainerType()));
    }
}