package org.geysermc.hydraulic.compat.runtime;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class SyncDispatcher {
    private final DirtyStateTracker dirtyStateTracker;
    private final SyncPlanner planner;
    private final SyncEncoder encoder;
    private final SyncTransport transport;

    public SyncDispatcher(
        @NotNull DirtyStateTracker dirtyStateTracker,
        @NotNull SyncPlanner planner,
        @NotNull SyncEncoder encoder,
        @NotNull SyncTransport transport
    ) {
        this.dirtyStateTracker = dirtyStateTracker;
        this.planner = planner;
        this.encoder = encoder;
        this.transport = transport;
    }

    @NotNull
    public List<SyncDeliveryResult> flush() {
        if (!this.dirtyStateTracker.dirty()) {
            return List.of();
        }
        SyncBatch batch = this.planner.plan(this.dirtyStateTracker.drain());
        if (batch.changes().isEmpty()) {
            return List.of();
        }
        return this.transport.deliver(this.encoder.encode(batch));
    }
}
