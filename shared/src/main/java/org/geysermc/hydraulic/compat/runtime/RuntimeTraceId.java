package org.geysermc.hydraulic.compat.runtime;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record RuntimeTraceId(@NotNull String value) {
    public RuntimeTraceId {
        if (value.isBlank()) {
            throw new IllegalArgumentException("Runtime trace id must not be blank");
        }
    }

    @NotNull
    public static RuntimeTraceId create() {
        return new RuntimeTraceId(UUID.randomUUID().toString());
    }
}
