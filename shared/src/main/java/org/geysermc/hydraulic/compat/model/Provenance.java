package org.geysermc.hydraulic.compat.model;

import org.jetbrains.annotations.Nullable;

public record Provenance(
    String source,
    @Nullable String analyzer,
    @Nullable String reference,
    boolean overridden
) {
}