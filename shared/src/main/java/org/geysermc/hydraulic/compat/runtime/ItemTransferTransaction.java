package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Atomic item transfer transaction over one executable runtime bridge.
 * Simulation must fully accept every request before any mutation is committed.
 */
public final class ItemTransferTransaction {
    private final TransferBridgeFactory.ItemTransferBridge bridge;
    private final List<TransferRequest> requests = new ArrayList<>();

    public ItemTransferTransaction(@NotNull TransferBridgeFactory.ItemTransferBridge bridge) {
        if (!bridge.executable()) {
            throw new IllegalArgumentException("Item transactions require an executable bridge");
        }
        this.bridge = bridge;
    }

    @NotNull
    public ItemTransferTransaction add(@NotNull TransferRequest request) {
        this.requests.add(request);
        return this;
    }

    @NotNull
    public TransferResult execute() {
        if (this.requests.isEmpty()) {
            return TransferResult.rejected("transaction has no operations");
        }

        List<TransferBridgeFactory.OperationResult> simulations = new ArrayList<>(this.requests.size());
        for (TransferRequest request : this.requests) {
            TransferBridgeFactory.OperationResult result = operate(request, true);
            simulations.add(result);
            if (!isComplete(result, request)) {
                return new TransferResult(false, 0, result.status(), result.failureReason(), simulations);
            }
        }

        List<CommittedOperation> committed = new ArrayList<>(this.requests.size());
        List<TransferBridgeFactory.OperationResult> results = new ArrayList<>(this.requests.size());
        int moved = 0;
        for (TransferRequest request : this.requests) {
            TransferBridgeFactory.OperationResult result = operate(request, false);
            results.add(result);
            if (!isComplete(result, request)) {
                rollback(committed);
                return new TransferResult(false, 0, result.status(), result.failureReason(), results);
            }
            committed.add(new CommittedOperation(request, result.moved()));
            moved += result.moved();
        }

        return new TransferResult(true, moved, TransferBridgeFactory.OperationStatus.COMPLETED, null, results);
    }

    private TransferBridgeFactory.OperationResult operate(@NotNull TransferRequest request, boolean simulate) {
        if (request.direction() == TransferDirection.INSERT) {
            return this.bridge.insertResult(request.blockIdentifier(), request.item(), request.slot(), request.side(), simulate);
        }
        return this.bridge.extractResult(request.blockIdentifier(), request.item(), request.slot(), request.side(), simulate);
    }

    private static boolean isComplete(
        @NotNull TransferBridgeFactory.OperationResult result,
        @NotNull TransferRequest request
    ) {
        return result.successful() && result.moved() == request.item().count();
    }

    private void rollback(@NotNull List<CommittedOperation> committed) {
        for (int index = committed.size() - 1; index >= 0; index--) {
            CommittedOperation operation = committed.get(index);
            TransferRequest request = operation.request();
            TransferBridgeFactory.ItemStackView moved = new TransferBridgeFactory.ItemStackView(request.item().itemId(), operation.moved());
            TransferRequest inverse = new TransferRequest(
                request.blockIdentifier(),
                request.direction() == TransferDirection.INSERT ? TransferDirection.EXTRACT : TransferDirection.INSERT,
                moved,
                request.slot(),
                request.side()
            );
            operate(inverse, false);
        }
    }

    private record CommittedOperation(@NotNull TransferRequest request, int moved) {
    }
}
