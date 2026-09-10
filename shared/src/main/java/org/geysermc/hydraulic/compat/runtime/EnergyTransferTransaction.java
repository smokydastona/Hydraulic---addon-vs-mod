package org.geysermc.hydraulic.compat.runtime;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Atomic energy transfer transaction over one executable runtime bridge. */
public final class EnergyTransferTransaction {
    private final TransferBridgeFactory.EnergyTransferBridge bridge;
    private final List<EnergyTransferRequest> requests = new ArrayList<>();

    public EnergyTransferTransaction(@NotNull TransferBridgeFactory.EnergyTransferBridge bridge) {
        if (!bridge.executable()) {
            throw new IllegalArgumentException("Energy transactions require an executable bridge");
        }
        this.bridge = bridge;
    }

    @NotNull
    public EnergyTransferTransaction add(@NotNull EnergyTransferRequest request) {
        this.requests.add(request);
        return this;
    }

    @NotNull
    public TransferResult execute() {
        if (this.requests.isEmpty()) {
            return TransferResult.rejected("transaction has no operations");
        }

        List<TransferBridgeFactory.OperationResult> simulations = new ArrayList<>(this.requests.size());
        Map<net.minecraft.resources.Identifier, Integer> predictedEnergy = new HashMap<>();
        for (EnergyTransferRequest request : this.requests) {
            TransferBridgeFactory.OperationResult result = operate(request, true);
            simulations.add(result);
            if (!isComplete(result, request)) {
                return new TransferResult(false, 0, result.status(), result.failureReason(), simulations, StateChangeSet.empty());
            }
            int current = predictedEnergy.computeIfAbsent(
                request.blockIdentifier(),
                identifier -> this.bridge.getEnergyStored(identifier)
            );
            int predicted = request.direction() == TransferDirection.INSERT
                ? current + request.amount()
                : current - request.amount();
            if (predicted < 0 || predicted > this.bridge.getMaxEnergy(request.blockIdentifier())) {
                return new TransferResult(
                    false,
                    0,
                    TransferBridgeFactory.OperationStatus.REJECTED,
                    "energy transaction exceeds virtual storage bounds",
                    simulations,
                    StateChangeSet.empty()
                );
            }
            predictedEnergy.put(request.blockIdentifier(), predicted);
        }

        List<CommittedOperation> committed = new ArrayList<>(this.requests.size());
        List<TransferBridgeFactory.OperationResult> results = new ArrayList<>(this.requests.size());
        List<StateChangeSet.FieldChange> changes = new ArrayList<>(this.requests.size());
        int moved = 0;
        for (EnergyTransferRequest request : this.requests) {
            int before = this.bridge.getEnergyStored(request.blockIdentifier());
            TransferBridgeFactory.OperationResult result = operate(request, false);
            results.add(result);
            if (!isComplete(result, request)) {
                rollback(committed);
                return new TransferResult(false, 0, result.status(), result.failureReason(), results, StateChangeSet.empty());
            }
            committed.add(new CommittedOperation(request, result.moved()));
            int after = this.bridge.getEnergyStored(request.blockIdentifier());
            if (before != after) {
                changes.add(new StateChangeSet.FieldChange(request.blockIdentifier(), "energy.amount", before, after));
            }
            moved += result.moved();
        }

        return new TransferResult(true, moved, TransferBridgeFactory.OperationStatus.COMPLETED, null, results, new StateChangeSet(changes));
    }

    @NotNull
    public TransferResult execute(@NotNull DirtyStateTracker dirtyStateTracker) {
        TransferResult result = execute();
        if (result.committed()) {
            dirtyStateTracker.record(result.stateChanges());
        }
        return result;
    }

    private TransferBridgeFactory.OperationResult operate(@NotNull EnergyTransferRequest request, boolean simulate) {
        if (request.direction() == TransferDirection.INSERT) {
            return this.bridge.receiveEnergyResult(request.blockIdentifier(), request.amount(), request.side(), simulate);
        }
        return this.bridge.extractEnergyResult(request.blockIdentifier(), request.amount(), request.side(), simulate);
    }

    private static boolean isComplete(
        @NotNull TransferBridgeFactory.OperationResult result,
        @NotNull EnergyTransferRequest request
    ) {
        return result.successful() && result.moved() == request.amount();
    }

    private void rollback(@NotNull List<CommittedOperation> committed) {
        for (int index = committed.size() - 1; index >= 0; index--) {
            CommittedOperation operation = committed.get(index);
            EnergyTransferRequest request = operation.request();
            operate(new EnergyTransferRequest(
                request.blockIdentifier(),
                request.direction() == TransferDirection.INSERT ? TransferDirection.EXTRACT : TransferDirection.INSERT,
                operation.moved(),
                request.side()
            ), false);
        }
    }

    private record CommittedOperation(@NotNull EnergyTransferRequest request, int moved) {
    }
}
