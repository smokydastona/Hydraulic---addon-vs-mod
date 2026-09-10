package org.geysermc.hydraulic.compat.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record TransferResult(
    boolean committed,
    int moved,
    @NotNull TransferBridgeFactory.OperationStatus status,
    @Nullable String failureReason,
    @NotNull List<TransferBridgeFactory.OperationResult> operations
) {
    public TransferResult {
        operations = List.copyOf(operations);
    }

    public static TransferResult rejected(@NotNull String reason) {
        return new TransferResult(false, 0, TransferBridgeFactory.OperationStatus.REJECTED, reason, List.of());
    }
}
