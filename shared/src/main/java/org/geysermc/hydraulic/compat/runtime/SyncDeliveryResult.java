package org.geysermc.hydraulic.compat.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record SyncDeliveryResult(
    @NotNull EncodedSyncChange change,
    @NotNull SyncDeliveryStatus status,
    @Nullable String reason
) {
    public SyncDeliveryResult {
        if ((status == SyncDeliveryStatus.TRANSPORT_FAILED
            || status == SyncDeliveryStatus.TARGET_UNAVAILABLE
            || status == SyncDeliveryStatus.STALE_STATE
            || status == SyncDeliveryStatus.UNSUPPORTED
            || status == SyncDeliveryStatus.ENCODING_FAILED)
            && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("Failed synchronization delivery results require a reason");
        }
    }

    public boolean successfulHandoff() {
        return this.status == SyncDeliveryStatus.QUEUED
            || this.status == SyncDeliveryStatus.SENT
            || this.status == SyncDeliveryStatus.APPLIED;
    }
}
