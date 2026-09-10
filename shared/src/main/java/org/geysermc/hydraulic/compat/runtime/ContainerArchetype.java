package org.geysermc.hydraulic.compat.runtime;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum ContainerArchetype {
    CHEST,
    DOUBLE_CHEST,
    FURNACE,
    CRAFTING,
    PROCESSOR,
    MACHINE,
    STORAGE,
    ENERGY_MACHINE,
    FLUID_MACHINE,
    CUSTOM_GRID,
    UNKNOWN;

    @NotNull
    public static ContainerArchetype parse(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        try {
            return value.trim().replace('-', '_').toUpperCase(Locale.ROOT).equals("UNKNOWN")
                ? UNKNOWN
                : valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return UNKNOWN;
        }
    }
}
