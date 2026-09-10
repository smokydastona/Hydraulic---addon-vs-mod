package org.geysermc.hydraulic.compat.runtime;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface SyncTransport {
    @NotNull
    List<SyncDeliveryResult> deliver(@NotNull List<EncodedSyncChange> changes);
}
