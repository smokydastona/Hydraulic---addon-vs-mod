package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Atomic transaction coordinator for mixed item, fluid, and energy operations. */
public final class MultiResourceTransaction {
    private static final int MAX_STACK_SIZE = 64;

    private final List<ResourceOperation> operations = new ArrayList<>();

    @NotNull
    public MultiResourceTransaction addItem(
        @NotNull TransferBridgeFactory.ItemTransferBridge bridge,
        @NotNull TransferRequest request
    ) {
        if (!bridge.executable()) {
            throw new IllegalArgumentException("Multi-resource item operations require an executable bridge");
        }
        this.operations.add(new ItemOperation(bridge, request));
        return this;
    }

    @NotNull
    public MultiResourceTransaction addFluid(
        @NotNull TransferBridgeFactory.FluidTransferBridge bridge,
        @NotNull FluidTransferRequest request
    ) {
        if (!bridge.executable()) {
            throw new IllegalArgumentException("Multi-resource fluid operations require an executable bridge");
        }
        this.operations.add(new FluidOperation(bridge, request));
        return this;
    }

    @NotNull
    public MultiResourceTransaction addEnergy(
        @NotNull TransferBridgeFactory.EnergyTransferBridge bridge,
        @NotNull EnergyTransferRequest request
    ) {
        if (!bridge.executable()) {
            throw new IllegalArgumentException("Multi-resource energy operations require an executable bridge");
        }
        this.operations.add(new EnergyOperation(bridge, request));
        return this;
    }

    @NotNull
    public TransferResult execute() {
        if (this.operations.isEmpty()) {
            return TransferResult.rejected("transaction has no operations");
        }

        SimulationState simulationState = new SimulationState();
        List<TransferBridgeFactory.OperationResult> simulations = new ArrayList<>(this.operations.size());
        for (ResourceOperation operation : this.operations) {
            TransferBridgeFactory.OperationResult result = operation.simulate(simulationState);
            simulations.add(result);
            if (!operation.complete(result)) {
                return new TransferResult(false, 0, result.status(), result.failureReason(), simulations, StateChangeSet.empty());
            }
        }

        List<CommittedOperation> committed = new ArrayList<>(this.operations.size());
        List<TransferBridgeFactory.OperationResult> results = new ArrayList<>(this.operations.size());
        StateChangeSet stateChanges = StateChangeSet.empty();
        int moved = 0;
        for (ResourceOperation operation : this.operations) {
            Snapshot before = operation.snapshot();
            TransferBridgeFactory.OperationResult result = operation.commit();
            results.add(result);
            if (!operation.complete(result)) {
                rollback(committed);
                return new TransferResult(false, 0, result.status(), result.failureReason(), results, StateChangeSet.empty());
            }
            committed.add(new CommittedOperation(operation, result.moved()));
            stateChanges = stateChanges.merge(operation.stateChanges(before));
            moved += result.moved();
        }

        return new TransferResult(true, moved, TransferBridgeFactory.OperationStatus.COMPLETED, null, results, stateChanges);
    }

    @NotNull
    public TransferResult execute(@NotNull DirtyStateTracker dirtyStateTracker) {
        TransferResult result = execute();
        if (result.committed()) {
            dirtyStateTracker.record(result.stateChanges());
        }
        return result;
    }

    private void rollback(@NotNull List<CommittedOperation> committed) {
        for (int index = committed.size() - 1; index >= 0; index--) {
            CommittedOperation operation = committed.get(index);
            operation.operation().rollback(operation.moved());
        }
    }

    private interface ResourceOperation {
        @NotNull TransferBridgeFactory.OperationResult simulate(@NotNull SimulationState simulationState);
        @NotNull TransferBridgeFactory.OperationResult commit();
        boolean complete(@NotNull TransferBridgeFactory.OperationResult result);
        @NotNull Snapshot snapshot();
        @NotNull StateChangeSet stateChanges(@NotNull Snapshot before);
        void rollback(int moved);
    }

    private record ItemOperation(
        @NotNull TransferBridgeFactory.ItemTransferBridge bridge,
        @NotNull TransferRequest request
    ) implements ResourceOperation {
        @Override
        @NotNull
        public TransferBridgeFactory.OperationResult simulate(@NotNull SimulationState simulationState) {
            ItemSlotKey key = new ItemSlotKey(this.bridge, this.request.blockIdentifier(), this.request.slot());
            TransferBridgeFactory.ItemStackView current = simulationState.items.computeIfAbsent(
                key,
                ignored -> emptyIfNull(this.bridge.itemAt(this.request.blockIdentifier(), this.request.slot()))
            );
            TransferBridgeFactory.OperationResult bounds = validateVirtualItem(current);
            if (bounds != null) {
                return bounds;
            }
            TransferBridgeFactory.OperationResult result = operate(true);
            if (complete(result)) {
                simulationState.items.put(key, apply(current));
            }
            return result;
        }

        @Override
        @NotNull
        public TransferBridgeFactory.OperationResult commit() {
            return operate(false);
        }

        @Override
        public boolean complete(@NotNull TransferBridgeFactory.OperationResult result) {
            return result.successful() && result.moved() == this.request.item().count();
        }

        @Override
        @NotNull
        public Snapshot snapshot() {
            return new Snapshot(emptyIfNull(this.bridge.itemAt(this.request.blockIdentifier(), this.request.slot())));
        }

        @Override
        @NotNull
        public StateChangeSet stateChanges(@NotNull Snapshot before) {
            TransferBridgeFactory.ItemStackView after = emptyIfNull(this.bridge.itemAt(this.request.blockIdentifier(), this.request.slot()));
            if (Objects.equals(before.value(), after)) {
                return StateChangeSet.empty();
            }
            return new StateChangeSet(List.of(new StateChangeSet.FieldChange(
                this.request.blockIdentifier(),
                "inventory.slot." + this.request.slot(),
                before.value(),
                after
            )));
        }

        @Override
        public void rollback(int moved) {
            TransferRequest inverse = new TransferRequest(
                this.request.blockIdentifier(),
                this.request.direction() == TransferDirection.INSERT ? TransferDirection.EXTRACT : TransferDirection.INSERT,
                new TransferBridgeFactory.ItemStackView(this.request.item().itemId(), moved),
                this.request.slot(),
                this.request.side()
            );
            if (inverse.direction() == TransferDirection.INSERT) {
                this.bridge.insertResult(inverse.blockIdentifier(), inverse.item(), inverse.slot(), inverse.side(), false);
            } else {
                this.bridge.extractResult(inverse.blockIdentifier(), inverse.item(), inverse.slot(), inverse.side(), false);
            }
        }

        @Nullable
        private TransferBridgeFactory.OperationResult validateVirtualItem(@NotNull TransferBridgeFactory.ItemStackView current) {
            if (this.request.direction() == TransferDirection.EXTRACT) {
                if (!current.matches(this.request.item()) || current.count() < this.request.item().count()) {
                    return rejected(this.request.item().count(), "item transaction exceeds virtual slot contents");
                }
                return null;
            }
            if (!current.isEmpty() && !current.matches(this.request.item())) {
                return rejected(this.request.item().count(), "item transaction mixes incompatible slot contents");
            }
            if (current.count() + this.request.item().count() > MAX_STACK_SIZE) {
                return rejected(this.request.item().count(), "item transaction exceeds virtual slot capacity");
            }
            return null;
        }

        @NotNull
        private TransferBridgeFactory.ItemStackView apply(@NotNull TransferBridgeFactory.ItemStackView current) {
            if (this.request.direction() == TransferDirection.EXTRACT) {
                int remaining = current.count() - this.request.item().count();
                return remaining <= 0
                    ? new TransferBridgeFactory.ItemStackView("minecraft:air", 0)
                    : new TransferBridgeFactory.ItemStackView(current.itemId(), remaining);
            }
            return new TransferBridgeFactory.ItemStackView(this.request.item().itemId(), current.count() + this.request.item().count());
        }

        @NotNull
        private TransferBridgeFactory.OperationResult operate(boolean simulate) {
            if (this.request.direction() == TransferDirection.INSERT) {
                return this.bridge.insertResult(this.request.blockIdentifier(), this.request.item(), this.request.slot(), this.request.side(), simulate);
            }
            return this.bridge.extractResult(this.request.blockIdentifier(), this.request.item(), this.request.slot(), this.request.side(), simulate);
        }
    }

    private record FluidOperation(
        @NotNull TransferBridgeFactory.FluidTransferBridge bridge,
        @NotNull FluidTransferRequest request
    ) implements ResourceOperation {
        @Override
        @NotNull
        public TransferBridgeFactory.OperationResult simulate(@NotNull SimulationState simulationState) {
            FluidTankKey key = new FluidTankKey(this.bridge, this.request.blockIdentifier(), this.request.tank());
            TransferBridgeFactory.FluidStackView current = simulationState.fluids.computeIfAbsent(
                key,
                ignored -> emptyIfNull(this.bridge.tankAt(this.request.blockIdentifier(), this.request.tank()))
            );
            TransferBridgeFactory.OperationResult bounds = validateVirtualFluid(current);
            if (bounds != null) {
                return bounds;
            }
            TransferBridgeFactory.OperationResult result = operate(true);
            if (complete(result)) {
                simulationState.fluids.put(key, apply(current));
            }
            return result;
        }

        @Override
        @NotNull
        public TransferBridgeFactory.OperationResult commit() {
            return operate(false);
        }

        @Override
        public boolean complete(@NotNull TransferBridgeFactory.OperationResult result) {
            return result.successful() && result.moved() == this.request.fluid().amount();
        }

        @Override
        @NotNull
        public Snapshot snapshot() {
            return new Snapshot(emptyIfNull(this.bridge.tankAt(this.request.blockIdentifier(), this.request.tank())));
        }

        @Override
        @NotNull
        public StateChangeSet stateChanges(@NotNull Snapshot before) {
            TransferBridgeFactory.FluidStackView after = emptyIfNull(this.bridge.tankAt(this.request.blockIdentifier(), this.request.tank()));
            if (Objects.equals(before.value(), after)) {
                return StateChangeSet.empty();
            }
            return new StateChangeSet(List.of(new StateChangeSet.FieldChange(
                this.request.blockIdentifier(),
                "fluid.tank." + this.request.tank(),
                before.value(),
                after
            )));
        }

        @Override
        public void rollback(int moved) {
            FluidTransferRequest inverse = new FluidTransferRequest(
                this.request.blockIdentifier(),
                this.request.direction() == TransferDirection.INSERT ? TransferDirection.EXTRACT : TransferDirection.INSERT,
                new TransferBridgeFactory.FluidStackView(this.request.fluid().fluidId(), moved),
                this.request.tank(),
                this.request.side()
            );
            if (inverse.direction() == TransferDirection.INSERT) {
                this.bridge.insertFluidResult(inverse.blockIdentifier(), inverse.fluid(), inverse.tank(), inverse.side(), false);
            } else {
                this.bridge.extractFluidResult(inverse.blockIdentifier(), inverse.fluid(), inverse.tank(), inverse.side(), false);
            }
        }

        @Nullable
        private TransferBridgeFactory.OperationResult validateVirtualFluid(@NotNull TransferBridgeFactory.FluidStackView current) {
            if (this.request.direction() == TransferDirection.EXTRACT) {
                if (!current.fluidId().equals(this.request.fluid().fluidId()) || current.amount() < this.request.fluid().amount()) {
                    return rejected(this.request.fluid().amount(), "fluid transaction exceeds virtual tank contents");
                }
                return null;
            }
            if (current.amount() > 0 && !current.fluidId().equals(this.request.fluid().fluidId())) {
                return rejected(this.request.fluid().amount(), "fluid transaction mixes incompatible tank contents");
            }
            if (current.amount() + this.request.fluid().amount() > this.bridge.tankCapacity(this.request.blockIdentifier(), this.request.tank())) {
                return rejected(this.request.fluid().amount(), "fluid transaction exceeds virtual tank capacity");
            }
            return null;
        }

        @NotNull
        private TransferBridgeFactory.FluidStackView apply(@NotNull TransferBridgeFactory.FluidStackView current) {
            if (this.request.direction() == TransferDirection.EXTRACT) {
                int remaining = current.amount() - this.request.fluid().amount();
                return remaining <= 0
                    ? new TransferBridgeFactory.FluidStackView("minecraft:empty", 0)
                    : new TransferBridgeFactory.FluidStackView(current.fluidId(), remaining);
            }
            return new TransferBridgeFactory.FluidStackView(this.request.fluid().fluidId(), current.amount() + this.request.fluid().amount());
        }

        @NotNull
        private TransferBridgeFactory.OperationResult operate(boolean simulate) {
            if (this.request.direction() == TransferDirection.INSERT) {
                return this.bridge.insertFluidResult(this.request.blockIdentifier(), this.request.fluid(), this.request.tank(), this.request.side(), simulate);
            }
            return this.bridge.extractFluidResult(this.request.blockIdentifier(), this.request.fluid(), this.request.tank(), this.request.side(), simulate);
        }
    }

    private record EnergyOperation(
        @NotNull TransferBridgeFactory.EnergyTransferBridge bridge,
        @NotNull EnergyTransferRequest request
    ) implements ResourceOperation {
        @Override
        @NotNull
        public TransferBridgeFactory.OperationResult simulate(@NotNull SimulationState simulationState) {
            EnergyKey key = new EnergyKey(this.bridge, this.request.blockIdentifier());
            int current = simulationState.energy.computeIfAbsent(
                key,
                ignored -> this.bridge.getEnergyStored(this.request.blockIdentifier())
            );
            int predicted = this.request.direction() == TransferDirection.INSERT
                ? current + this.request.amount()
                : current - this.request.amount();
            if (predicted < 0 || predicted > this.bridge.getMaxEnergy(this.request.blockIdentifier())) {
                return rejected(this.request.amount(), "energy transaction exceeds virtual storage bounds");
            }
            TransferBridgeFactory.OperationResult result = operate(true);
            if (complete(result)) {
                simulationState.energy.put(key, predicted);
            }
            return result;
        }

        @Override
        @NotNull
        public TransferBridgeFactory.OperationResult commit() {
            return operate(false);
        }

        @Override
        public boolean complete(@NotNull TransferBridgeFactory.OperationResult result) {
            return result.successful() && result.moved() == this.request.amount();
        }

        @Override
        @NotNull
        public Snapshot snapshot() {
            return new Snapshot(this.bridge.getEnergyStored(this.request.blockIdentifier()));
        }

        @Override
        @NotNull
        public StateChangeSet stateChanges(@NotNull Snapshot before) {
            int after = this.bridge.getEnergyStored(this.request.blockIdentifier());
            if (Objects.equals(before.value(), after)) {
                return StateChangeSet.empty();
            }
            return new StateChangeSet(List.of(new StateChangeSet.FieldChange(
                this.request.blockIdentifier(),
                "energy.amount",
                before.value(),
                after
            )));
        }

        @Override
        public void rollback(int moved) {
            EnergyTransferRequest inverse = new EnergyTransferRequest(
                this.request.blockIdentifier(),
                this.request.direction() == TransferDirection.INSERT ? TransferDirection.EXTRACT : TransferDirection.INSERT,
                moved,
                this.request.side()
            );
            if (inverse.direction() == TransferDirection.INSERT) {
                this.bridge.receiveEnergyResult(inverse.blockIdentifier(), inverse.amount(), inverse.side(), false);
            } else {
                this.bridge.extractEnergyResult(inverse.blockIdentifier(), inverse.amount(), inverse.side(), false);
            }
        }

        @NotNull
        private TransferBridgeFactory.OperationResult operate(boolean simulate) {
            if (this.request.direction() == TransferDirection.INSERT) {
                return this.bridge.receiveEnergyResult(this.request.blockIdentifier(), this.request.amount(), this.request.side(), simulate);
            }
            return this.bridge.extractEnergyResult(this.request.blockIdentifier(), this.request.amount(), this.request.side(), simulate);
        }
    }

    private static TransferBridgeFactory.ItemStackView emptyIfNull(@Nullable TransferBridgeFactory.ItemStackView value) {
        return value == null ? new TransferBridgeFactory.ItemStackView("minecraft:air", 0) : value;
    }

    private static TransferBridgeFactory.FluidStackView emptyIfNull(@Nullable TransferBridgeFactory.FluidStackView value) {
        return value == null ? new TransferBridgeFactory.FluidStackView("minecraft:empty", 0) : value;
    }

    @NotNull
    private static TransferBridgeFactory.OperationResult rejected(int requested, @NotNull String reason) {
        return new TransferBridgeFactory.OperationResult(
            requested,
            0,
            true,
            TransferBridgeFactory.OperationStatus.REJECTED,
            null
        );
    }

    private record Snapshot(@Nullable Object value) {
    }

    private record CommittedOperation(@NotNull ResourceOperation operation, int moved) {
    }

    private record ItemSlotKey(
        @NotNull TransferBridgeFactory.ItemTransferBridge bridge,
        @NotNull Identifier blockIdentifier,
        int slot
    ) {
    }

    private record FluidTankKey(
        @NotNull TransferBridgeFactory.FluidTransferBridge bridge,
        @NotNull Identifier blockIdentifier,
        int tank
    ) {
    }

    private record EnergyKey(
        @NotNull TransferBridgeFactory.EnergyTransferBridge bridge,
        @NotNull Identifier blockIdentifier
    ) {
    }

    private static final class SimulationState {
        private final Map<ItemSlotKey, TransferBridgeFactory.ItemStackView> items = new HashMap<>();
        private final Map<FluidTankKey, TransferBridgeFactory.FluidStackView> fluids = new HashMap<>();
        private final Map<EnergyKey, Integer> energy = new HashMap<>();
    }
}
