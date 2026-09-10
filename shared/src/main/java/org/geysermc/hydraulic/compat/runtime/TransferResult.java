package org.geysermc.hydraulic.compat.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record TransferResult(
    boolean committed,
    int moved,
    @NotNull TransferBridgeFactory.OperationStatus status,
    @Nullable String failureReason,
    @NotNull List<TransferBridgeFactory.OperationResult> operations,
    @NotNull StateChangeSet stateChanges,
    @Nullable RuntimeTraceId traceId
) {
    public TransferResult(
        boolean committed,
        int moved,
        @NotNull TransferBridgeFactory.OperationStatus status,
        @Nullable String failureReason,
        @NotNull List<TransferBridgeFactory.OperationResult> operations,
        @NotNull StateChangeSet stateChanges
    ) {
        this(committed, moved, status, failureReason, operations, stateChanges, null);
    }

    public TransferResult {
        operations = List.copyOf(operations);
        if (traceId != null) {
            stateChanges = stateChanges.withTrace(traceId);
        }
    }

    public static TransferResult rejected(@NotNull String reason) {
        return new TransferResult(false, 0, TransferBridgeFactory.OperationStatus.REJECTED, reason, List.of(), StateChangeSet.empty());
    }

    @NotNull
    public TransferResult withTrace(@Nullable RuntimeTraceId traceId) {
        if (traceId == null) {
            return this;
        }
        return new TransferResult(this.committed, this.moved, this.status, this.failureReason, this.operations, this.stateChanges, traceId);
    }
}
