package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.hydraulic.HydraulicImpl;
import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CompatibilityRuntimeDiagnostics {
    private static final Logger LOGGER = LoggerFactory.getLogger("HydraulicCompatibilityRuntime");
    private static final Set<String> WARNED_UNSUPPORTED_MENUS = ConcurrentHashMap.newKeySet();
    private static final Set<String> WARNED_UNSUPPORTED_BLOCK_ENTITIES = ConcurrentHashMap.newKeySet();

    private CompatibilityRuntimeDiagnostics() {
    }

    public static void reportUnsupportedMenuOpen(@NotNull ContainerType containerType, @Nullable String title, @Nullable String javaIdentifier) {
        String warningKey = (javaIdentifier != null ? javaIdentifier : containerType.name()) + '|' + normalizeTitle(title);
        if (!WARNED_UNSUPPORTED_MENUS.add(warningKey)) {
            return;
        }

        LOGGER.warn(UnsupportedMenuDiagnosticFormatter.format(containerType.name(), title, javaIdentifier, currentRegistry()));
    }

    public static void reportUnsupportedBlockEntityData(
        @NotNull GeyserSession session,
        @NotNull BlockEntityType blockEntityType,
        @NotNull Vector3i position
    ) {
        CompatibilityRegistry compatibilityRegistry = currentRegistry();
        String javaIdentifier = resolveJavaBlockEntityIdentifier(session, position);
        String warningKey = (javaIdentifier != null ? javaIdentifier : blockEntityType.name()) + '|' + positionKey(position);
        if (!WARNED_UNSUPPORTED_BLOCK_ENTITIES.add(warningKey)) {
            return;
        }

        String message = UnsupportedBlockEntityDiagnosticFormatter.format(
            blockEntityType.name(),
            javaIdentifier,
            positionString(position),
            compatibilityRegistry
        );
        if (message != null) {
            LOGGER.warn(message);
        }
    }

    @NotNull
    public static String unsupportedMenuReason(
        @NotNull ContainerType containerType,
        @Nullable String title,
        @NotNull CompatibilityRegistry compatibilityRegistry
    ) {
        return UnsupportedMenuDiagnosticFormatter.format(containerType.name(), title, null, compatibilityRegistry);
    }

    @NotNull
    static String unsupportedMenuReason(
        @NotNull String containerTypeName,
        @Nullable String title,
        @NotNull CompatibilityRegistry compatibilityRegistry
    ) {
        return UnsupportedMenuDiagnosticFormatter.format(containerTypeName, title, null, compatibilityRegistry);
    }

    @NotNull
    public static CompatibilityRegistry currentRegistry() {
        try {
            return HydraulicImpl.instance().getPackManager().compatibilityRegistry();
        } catch (IllegalStateException ignored) {
            return CompatibilityRegistry.empty();
        }
    }

    @Nullable
    static String resolveJavaBlockEntityIdentifier(@NotNull GeyserSession session, @NotNull Vector3i position) {
        try {
            ServerPlayer player = HydraulicImpl.instance().server().getPlayerList().getPlayer(session.javaUuid());
            if (player == null) {
                return null;
            }

            BlockEntity blockEntity = player.level().getBlockEntity(BlockPos.containing(position.getX(), position.getY(), position.getZ()));
            if (blockEntity == null) {
                return null;
            }

            Identifier identifier = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
            return identifier != null ? identifier.toString() : null;
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    @Nullable
    public static String resolveJavaMenuIdentifier(@NotNull GeyserSession session, int containerId) {
        try {
            ServerPlayer player = HydraulicImpl.instance().server().getPlayerList().getPlayer(session.javaUuid());
            if (player == null) {
                return null;
            }

            AbstractContainerMenu menu = player.containerMenu;
            if (menu == null || menu.containerId != containerId || menu.getType() == null) {
                return null;
            }

            Identifier identifier = BuiltInRegistries.MENU.getKey(menu.getType());
            return identifier != null ? identifier.toString() : null;
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    @Nullable
    public static String resolveJavaEntityIdentifier(@NotNull GeyserSession session, @NotNull Entity entity) {
        try {
            ServerPlayer player = HydraulicImpl.instance().server().getPlayerList().getPlayer(session.javaUuid());
            if (player == null) {
                return null;
            }

            net.minecraft.world.entity.Entity javaEntity = player.level().getEntity(entity.getEntityId());
            if (javaEntity == null || javaEntity.getType() == null) {
                return null;
            }

            Identifier identifier = BuiltInRegistries.ENTITY_TYPE.getKey(javaEntity.getType());
            return identifier != null ? identifier.toString() : null;
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    @NotNull
    private static String positionKey(@NotNull Vector3i position) {
        return position.getX() + "," + position.getY() + "," + position.getZ();
    }

    @NotNull
    private static String positionString(@NotNull Vector3i position) {
        return "(" + position.getX() + ", " + position.getY() + ", " + position.getZ() + ")";
    }

    @NotNull
    private static String normalizeTitle(@Nullable String title) {
        return title == null ? "" : title.trim().toLowerCase();
    }
}