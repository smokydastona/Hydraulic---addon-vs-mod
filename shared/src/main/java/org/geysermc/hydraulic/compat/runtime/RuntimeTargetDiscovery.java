package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.geysermc.geyser.session.GeyserSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RuntimeTargetDiscovery {
    private final AutomationResolver automationResolver;
    private final TargetSource targetSource;

    public RuntimeTargetDiscovery(@NotNull RuntimeDispatchTable dispatchTable, @NotNull TargetSource targetSource) {
        this(
            (target) -> dispatchTable.resourceAutomationAccess(
                target.blockIdentifier(),
                target.runtimeInventory(),
                target.runtimeTank(),
                target.runtimeStorage()
            ),
            targetSource
        );
    }

    RuntimeTargetDiscovery(@NotNull AutomationResolver automationResolver, @NotNull TargetSource targetSource) {
        this.automationResolver = automationResolver;
        this.targetSource = targetSource;
    }

    @NotNull
    public static RuntimeTargetDiscovery forGeyserSession(
        @NotNull RuntimeDispatchTable dispatchTable,
        @NotNull GeyserSession session
    ) {
        return new RuntimeTargetDiscovery(dispatchTable, new GeyserSessionRuntimeTargetSource(session));
    }

    @NotNull
    public Resolution discover(@NotNull Position position) {
        return discover(position, null);
    }

    @NotNull
    public Resolution discover(@NotNull Position position, @Nullable RuntimeTraceId traceId) {
        Target target = this.targetSource.targetAt(position);
        if (target == null) {
            return new Resolution(position, Status.TARGET_UNAVAILABLE, null, null, "No runtime target found at " + position.asKey(), traceId);
        }

        MachineBridgeFactory.ResourceAutomationAccess automation = this.automationResolver.resolve(target);
        if (automation == null) {
            return new Resolution(position, Status.CAPABILITY_UNAVAILABLE, target.blockIdentifier(), null, "No executable automation capability for " + target.blockIdentifier(), traceId);
        }
        return new Resolution(position, Status.RESOLVED, target.blockIdentifier(), automation, null, traceId);
    }

    @NotNull
    public TransferResult transferItem(
        @NotNull Position position,
        @NotNull TransferDirection direction,
        @NotNull TransferBridgeFactory.ItemStackView item,
        int slot,
        @Nullable String side,
        @Nullable DirtyStateTracker dirtyStateTracker
    ) {
        return transferItem(position, direction, item, slot, side, dirtyStateTracker, null);
    }

    @NotNull
    public TransferResult transferItem(
        @NotNull Position position,
        @NotNull TransferDirection direction,
        @NotNull TransferBridgeFactory.ItemStackView item,
        int slot,
        @Nullable String side,
        @Nullable DirtyStateTracker dirtyStateTracker,
        @Nullable RuntimeTraceId traceId
    ) {
        Resolution resolution = discover(position, traceId);
        if (!resolution.resolved()) {
            return TransferResult.rejected(resolution.reason()).withTrace(traceId);
        }
        TransferRequest request = new TransferRequest(resolution.blockIdentifier(), direction, item, slot, side);
        TransferResult result = resolution.automationAccess().transferItem(request).withTrace(traceId);
        if (dirtyStateTracker != null && result.committed()) {
            dirtyStateTracker.record(result.stateChanges());
        }
        return result;
    }

    @NotNull
    public TransferResult transferFluid(
        @NotNull Position position,
        @NotNull TransferDirection direction,
        @NotNull TransferBridgeFactory.FluidStackView fluid,
        int tank,
        @Nullable String side,
        @Nullable DirtyStateTracker dirtyStateTracker
    ) {
        return transferFluid(position, direction, fluid, tank, side, dirtyStateTracker, null);
    }

    @NotNull
    public TransferResult transferFluid(
        @NotNull Position position,
        @NotNull TransferDirection direction,
        @NotNull TransferBridgeFactory.FluidStackView fluid,
        int tank,
        @Nullable String side,
        @Nullable DirtyStateTracker dirtyStateTracker,
        @Nullable RuntimeTraceId traceId
    ) {
        Resolution resolution = discover(position, traceId);
        if (!resolution.resolved()) {
            return TransferResult.rejected(resolution.reason()).withTrace(traceId);
        }
        FluidTransferRequest request = new FluidTransferRequest(resolution.blockIdentifier(), direction, fluid, tank, side);
        TransferResult result = resolution.automationAccess().transferFluid(request).withTrace(traceId);
        if (dirtyStateTracker != null && result.committed()) {
            dirtyStateTracker.record(result.stateChanges());
        }
        return result;
    }

    @NotNull
    public TransferResult transferEnergy(
        @NotNull Position position,
        @NotNull TransferDirection direction,
        int amount,
        @Nullable String side,
        @Nullable DirtyStateTracker dirtyStateTracker
    ) {
        return transferEnergy(position, direction, amount, side, dirtyStateTracker, null);
    }

    @NotNull
    public TransferResult transferEnergy(
        @NotNull Position position,
        @NotNull TransferDirection direction,
        int amount,
        @Nullable String side,
        @Nullable DirtyStateTracker dirtyStateTracker,
        @Nullable RuntimeTraceId traceId
    ) {
        Resolution resolution = discover(position, traceId);
        if (!resolution.resolved()) {
            return TransferResult.rejected(resolution.reason()).withTrace(traceId);
        }
        EnergyTransferRequest request = new EnergyTransferRequest(resolution.blockIdentifier(), direction, amount, side);
        TransferResult result = resolution.automationAccess().transferEnergy(request).withTrace(traceId);
        if (dirtyStateTracker != null && result.committed()) {
            dirtyStateTracker.record(result.stateChanges());
        }
        return result;
    }

    public enum Status {
        RESOLVED,
        TARGET_UNAVAILABLE,
        CAPABILITY_UNAVAILABLE
    }

    public interface TargetSource {
        @Nullable Target targetAt(@NotNull Position position);
    }

    interface AutomationResolver {
        @Nullable MachineBridgeFactory.ResourceAutomationAccess resolve(@NotNull Target target);
    }

    public record Position(@NotNull String level, int x, int y, int z) {
        public Position {
            if (level.isBlank()) {
                throw new IllegalArgumentException("Runtime target level must not be blank");
            }
        }

        @NotNull
        public String asKey() {
            return this.level + ':' + this.x + ',' + this.y + ',' + this.z;
        }
    }

    public record Target(
        @NotNull Identifier blockIdentifier,
        @Nullable Object runtimeInventory,
        @Nullable Object runtimeTank,
        @Nullable Object runtimeStorage
    ) {
    }

    public record Resolution(
        @NotNull Position position,
        @NotNull Status status,
        @Nullable Identifier blockIdentifier,
        @Nullable MachineBridgeFactory.ResourceAutomationAccess automationAccess,
        @Nullable String reason,
        @Nullable RuntimeTraceId traceId
    ) {
        public Resolution(
            @NotNull Position position,
            @NotNull Status status,
            @Nullable Identifier blockIdentifier,
            @Nullable MachineBridgeFactory.ResourceAutomationAccess automationAccess,
            @Nullable String reason
        ) {
            this(position, status, blockIdentifier, automationAccess, reason, null);
        }

        public Resolution {
            if (status != Status.RESOLVED && (reason == null || reason.isBlank())) {
                throw new IllegalArgumentException("Unresolved runtime targets require a reason");
            }
            if (status == Status.RESOLVED && (blockIdentifier == null || automationAccess == null)) {
                throw new IllegalArgumentException("Resolved runtime targets require a block identifier and automation access");
            }
        }

        public boolean resolved() {
            return this.status == Status.RESOLVED;
        }
    }
}
