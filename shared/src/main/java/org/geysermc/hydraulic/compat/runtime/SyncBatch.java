package org.geysermc.hydraulic.compat.runtime;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record SyncBatch(@NotNull List<SyncChange> changes) {
    public SyncBatch {
        changes = List.copyOf(changes);
    }

    public boolean empty() {
        return this.changes.isEmpty();
    }
}
