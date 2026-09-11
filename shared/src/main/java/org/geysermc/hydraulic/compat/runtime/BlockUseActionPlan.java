package org.geysermc.hydraulic.compat.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record BlockUseActionPlan(
    @NotNull Action action,
    int slot,
    int count,
    @Nullable String side
) {
    private static final int MAX_TRANSFER_COUNT = 64;

    public BlockUseActionPlan {
        if (slot < 0) {
            throw new IllegalArgumentException("Block-use transfer slot must not be negative");
        }
        if (count <= 0 || count > MAX_TRANSFER_COUNT) {
            throw new IllegalArgumentException("Block-use transfer count must be between 1 and " + MAX_TRANSFER_COUNT);
        }
        if (side != null && side.isBlank()) {
            side = null;
        }
    }

    @Nullable
    public static BlockUseActionPlan from(@NotNull Map<String, String> facts) {
        Action action = Action.parse(facts.get("interaction.block_use.action"));
        if (action == null) {
            return null;
        }
        Integer slot = nonNegativeInteger(facts.get("interaction.block_use.slot"));
        Integer count = positiveInteger(facts.getOrDefault("interaction.block_use.count", "1"));
        if (slot == null || count == null || count > MAX_TRANSFER_COUNT) {
            return null;
        }
        return new BlockUseActionPlan(action, slot, count, facts.get("interaction.block_use.side"));
    }

    @Nullable
    private static Integer nonNegativeInteger(@Nullable String value) {
        Integer parsed = integer(value);
        return parsed == null || parsed < 0 ? null : parsed;
    }

    @Nullable
    private static Integer positiveInteger(@Nullable String value) {
        Integer parsed = integer(value);
        return parsed == null || parsed <= 0 ? null : parsed;
    }

    @Nullable
    private static Integer integer(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public enum Action {
        INSERT_HELD_ITEM;

        @Nullable
        private static Action parse(@Nullable String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }
}