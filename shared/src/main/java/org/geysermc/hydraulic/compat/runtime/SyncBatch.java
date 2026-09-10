package org.geysermc.hydraulic.compat.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record SyncBatch(@NotNull List<SyncChange> changes, @Nullable RuntimeTraceId traceId) {
    public SyncBatch(@NotNull List<SyncChange> changes) {
        this(changes, traceId(changes));
    }

    public SyncBatch {
        changes = List.copyOf(changes);
    }

    public boolean empty() {
        return this.changes.isEmpty();
    }

    @Nullable
    private static RuntimeTraceId traceId(@NotNull List<SyncChange> changes) {
        RuntimeTraceId traceId = null;
        for (SyncChange change : changes) {
            if (change.traceId() == null) {
                continue;
            }
            if (traceId == null) {
                traceId = change.traceId();
            } else if (!traceId.equals(change.traceId())) {
                return null;
            }
        }
        return traceId;
    }
}
