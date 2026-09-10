package org.geysermc.hydraulic.compat.runtime;

import org.jetbrains.annotations.NotNull;

public final class DirtyStateTracker {
    private StateChangeSet pending = StateChangeSet.empty();

    public synchronized void record(@NotNull StateChangeSet changes) {
        this.pending = this.pending.merge(changes);
    }

    @NotNull
    public synchronized StateChangeSet drain() {
        StateChangeSet drained = this.pending;
        this.pending = StateChangeSet.empty();
        return drained;
    }

    public synchronized boolean dirty() {
        return !this.pending.changes().isEmpty();
    }
}
