package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.hydraulic.compat.ir.CompiledCompatibilityPlan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Factory for creating transfer system bridges (item, fluid, energy) based on compiled compatibility plans.
 * This factory supports both metadata-based compatibility checks and real runtime adapters for live inventory objects.
 */
public final class TransferBridgeFactory {
    private TransferBridgeFactory() {
    }

    public static boolean supportsItemTransfer(@NotNull Identifier blockIdentifier, @NotNull CompatibilityRegistry compatibilityRegistry) {
        CompiledCompatibilityPlan plan = compatibilityRegistry.dispatchTable().block(blockIdentifier);
        return BridgeAdapterSupport.supportsItemTransfer(plan);
    }

    public static boolean supportsFluidTransfer(@NotNull Identifier blockIdentifier, @NotNull CompatibilityRegistry compatibilityRegistry) {
        CompiledCompatibilityPlan plan = compatibilityRegistry.dispatchTable().block(blockIdentifier);
        return BridgeAdapterSupport.supportsFluidTransfer(plan);
    }

    public static boolean supportsEnergyTransfer(@NotNull Identifier blockIdentifier, @NotNull CompatibilityRegistry compatibilityRegistry) {
        CompiledCompatibilityPlan plan = compatibilityRegistry.dispatchTable().block(blockIdentifier);
        return BridgeAdapterSupport.supportsEnergyTransfer(plan);
    }

    @Nullable
    public static ItemTransferBridge createItemTransfer(@NotNull Identifier blockIdentifier, @NotNull CompatibilityRegistry compatibilityRegistry) {
        CompiledCompatibilityPlan plan = compatibilityRegistry.dispatchTable().block(blockIdentifier);
        return BridgeAdapterSupport.supportsItemTransfer(plan) ? new MetadataBackedItemTransferBridge(plan) : null;
    }

    @Nullable
    public static ItemTransferBridge createItemTransfer(@NotNull CompiledCompatibilityPlan plan, @NotNull Object runtimeInventory) {
        if (!BridgeAdapterSupport.supportsItemTransfer(plan)) {
            return null;
        }
        RuntimeInventoryAdapter adapter = RuntimeInventoryAdapter.from(runtimeInventory);
        if (adapter == null || !supportsInventoryPlan(plan, adapter)) {
            return null;
        }
        return new RuntimeBackedItemTransferBridge(plan, adapter);
    }

    @Nullable
    public static ItemTransferBridge createItemTransfer(@NotNull Identifier blockIdentifier, @NotNull CompatibilityRegistry compatibilityRegistry, @NotNull Object runtimeInventory) {
        CompiledCompatibilityPlan plan = compatibilityRegistry.dispatchTable().block(blockIdentifier);
        if (!BridgeAdapterSupport.supportsItemTransfer(plan)) {
            return null;
        }
        return createItemTransfer(plan, runtimeInventory);
    }

    @Nullable
    public static FluidTransferBridge createFluidTransfer(@NotNull Identifier blockIdentifier, @NotNull CompatibilityRegistry compatibilityRegistry) {
        CompiledCompatibilityPlan plan = compatibilityRegistry.dispatchTable().block(blockIdentifier);
        return BridgeAdapterSupport.supportsFluidTransfer(plan) ? new MetadataBackedFluidTransferBridge(plan) : null;
    }

    @Nullable
    public static FluidTransferBridge createFluidTransfer(@NotNull CompiledCompatibilityPlan plan, @NotNull Object runtimeTank) {
        if (!BridgeAdapterSupport.supportsFluidTransfer(plan)) {
            return null;
        }
        RuntimeFluidAdapter adapter = RuntimeFluidAdapter.from(runtimeTank);
        if (adapter == null || !supportsFluidPlan(plan, adapter)) {
            return null;
        }
        return new RuntimeBackedFluidTransferBridge(plan, adapter);
    }

    @Nullable
    public static FluidTransferBridge createFluidTransfer(@NotNull Identifier blockIdentifier, @NotNull CompatibilityRegistry compatibilityRegistry, @NotNull Object runtimeTank) {
        CompiledCompatibilityPlan plan = compatibilityRegistry.dispatchTable().block(blockIdentifier);
        if (!BridgeAdapterSupport.supportsFluidTransfer(plan)) {
            return null;
        }
        return createFluidTransfer(plan, runtimeTank);
    }

    @Nullable
    public static EnergyTransferBridge createEnergyTransfer(@NotNull Identifier blockIdentifier, @NotNull CompatibilityRegistry compatibilityRegistry) {
        CompiledCompatibilityPlan plan = compatibilityRegistry.dispatchTable().block(blockIdentifier);
        return BridgeAdapterSupport.supportsEnergyTransfer(plan) ? new MetadataBackedEnergyTransferBridge(plan) : null;
    }

    @Nullable
    public static EnergyTransferBridge createEnergyTransfer(@NotNull CompiledCompatibilityPlan plan, @NotNull Object runtimeStorage) {
        if (!BridgeAdapterSupport.supportsEnergyTransfer(plan)) {
            return null;
        }
        RuntimeEnergyAdapter adapter = RuntimeEnergyAdapter.from(runtimeStorage);
        if (adapter == null || !supportsEnergyPlan(plan, adapter)) {
            return null;
        }
        return new RuntimeBackedEnergyTransferBridge(plan, adapter);
    }

    @Nullable
    public static EnergyTransferBridge createEnergyTransfer(@NotNull Identifier blockIdentifier, @NotNull CompatibilityRegistry compatibilityRegistry, @NotNull Object runtimeStorage) {
        CompiledCompatibilityPlan plan = compatibilityRegistry.dispatchTable().block(blockIdentifier);
        if (!BridgeAdapterSupport.supportsEnergyTransfer(plan)) {
            return null;
        }
        return createEnergyTransfer(plan, runtimeStorage);
    }

    public interface ItemTransferBridge {
        default boolean executable() {
            return false;
        }

        boolean canInsert(@NotNull Identifier blockIdentifier);
        boolean canExtract(@NotNull Identifier blockIdentifier);
        @Nullable String inventoryType(@NotNull Identifier blockIdentifier);

        default int slotCount(@NotNull Identifier blockIdentifier) {
            return 0;
        }

        default @Nullable ItemStackView itemAt(@NotNull Identifier blockIdentifier, int slot) {
            return null;
        }

        default int insert(@NotNull Identifier blockIdentifier, @NotNull ItemStackView item, int slot, @Nullable String side, boolean simulate) {
            return 0;
        }

        default int extract(@NotNull Identifier blockIdentifier, @NotNull ItemStackView item, int slot, @Nullable String side, boolean simulate) {
            return 0;
        }
    }

    public interface FluidTransferBridge {
        default boolean executable() {
            return false;
        }

        boolean canInsertFluid(@NotNull Identifier blockIdentifier);
        boolean canExtractFluid(@NotNull Identifier blockIdentifier);
        @Nullable String tankType(@NotNull Identifier blockIdentifier);

        default int tankCount(@NotNull Identifier blockIdentifier) {
            return 0;
        }

        default int tankCapacity(@NotNull Identifier blockIdentifier, int tank) {
            return 0;
        }

        default @Nullable FluidStackView tankAt(@NotNull Identifier blockIdentifier, int tank) {
            return null;
        }

        default int insertFluid(@NotNull Identifier blockIdentifier, @NotNull FluidStackView fluid, int tank, @Nullable String side, boolean simulate) {
            return 0;
        }

        default int extractFluid(@NotNull Identifier blockIdentifier, @NotNull FluidStackView fluid, int tank, @Nullable String side, boolean simulate) {
            return 0;
        }
    }

    public interface EnergyTransferBridge {
        default boolean executable() {
            return false;
        }

        boolean canReceiveEnergy(@NotNull Identifier blockIdentifier);
        boolean canProvideEnergy(@NotNull Identifier blockIdentifier);
        @Nullable String energyType(@NotNull Identifier blockIdentifier);

        default int getEnergyStored(@NotNull Identifier blockIdentifier) {
            return 0;
        }

        default int getMaxEnergy(@NotNull Identifier blockIdentifier) {
            return 0;
        }

        default int receiveEnergy(@NotNull Identifier blockIdentifier, int amount, @Nullable String side, boolean simulate) {
            return 0;
        }

        default int extractEnergy(@NotNull Identifier blockIdentifier, int amount, @Nullable String side, boolean simulate) {
            return 0;
        }
    }

    private static final class MetadataBackedItemTransferBridge implements ItemTransferBridge {
        private final CompiledCompatibilityPlan plan;

        private MetadataBackedItemTransferBridge(@NotNull CompiledCompatibilityPlan plan) {
            this.plan = plan;
        }

        @Override
        public boolean canInsert(@NotNull Identifier blockIdentifier) {
            return Boolean.parseBoolean(plan.inventoryFacts().getOrDefault("can_insert", "false"));
        }

        @Override
        public boolean canExtract(@NotNull Identifier blockIdentifier) {
            return Boolean.parseBoolean(plan.inventoryFacts().getOrDefault("can_extract", "false"));
        }

        @Override
        @Nullable
        public String inventoryType(@NotNull Identifier blockIdentifier) {
            return plan.inventoryFacts().get("inventory_type");
        }
    }

    private static final class MetadataBackedFluidTransferBridge implements FluidTransferBridge {
        private final CompiledCompatibilityPlan plan;

        private MetadataBackedFluidTransferBridge(@NotNull CompiledCompatibilityPlan plan) {
            this.plan = plan;
        }

        @Override
        public boolean canInsertFluid(@NotNull Identifier blockIdentifier) {
            return Boolean.parseBoolean(plan.inventoryFacts().getOrDefault("can_insert_fluid", "false"));
        }

        @Override
        public boolean canExtractFluid(@NotNull Identifier blockIdentifier) {
            return Boolean.parseBoolean(plan.inventoryFacts().getOrDefault("can_extract_fluid", "false"));
        }

        @Override
        @Nullable
        public String tankType(@NotNull Identifier blockIdentifier) {
            return plan.inventoryFacts().get("tank_type");
        }
    }

    private static final class MetadataBackedEnergyTransferBridge implements EnergyTransferBridge {
        private final CompiledCompatibilityPlan plan;

        private MetadataBackedEnergyTransferBridge(@NotNull CompiledCompatibilityPlan plan) {
            this.plan = plan;
        }

        @Override
        public boolean canReceiveEnergy(@NotNull Identifier blockIdentifier) {
            return Boolean.parseBoolean(plan.inventoryFacts().getOrDefault("can_receive_energy", "false"));
        }

        @Override
        public boolean canProvideEnergy(@NotNull Identifier blockIdentifier) {
            return Boolean.parseBoolean(plan.inventoryFacts().getOrDefault("can_provide_energy", "false"));
        }

        @Override
        @Nullable
        public String energyType(@NotNull Identifier blockIdentifier) {
            return plan.inventoryFacts().get("energy_type");
        }
    }

    private static boolean supportsInventoryPlan(@NotNull CompiledCompatibilityPlan plan, @NotNull RuntimeInventoryAdapter adapter) {
        return (!booleanFact(plan, "can_insert") || adapter.supportsInsertion())
            && (!booleanFact(plan, "can_extract") || adapter.supportsExtraction());
    }

    private static boolean supportsFluidPlan(@NotNull CompiledCompatibilityPlan plan, @NotNull RuntimeFluidAdapter adapter) {
        return (!booleanFact(plan, "can_insert_fluid") || adapter.supportsInsertion())
            && (!booleanFact(plan, "can_extract_fluid") || adapter.supportsExtraction());
    }

    private static boolean supportsEnergyPlan(@NotNull CompiledCompatibilityPlan plan, @NotNull RuntimeEnergyAdapter adapter) {
        return (!booleanFact(plan, "can_receive_energy") || adapter.canReceive())
            && (!booleanFact(plan, "can_provide_energy") || adapter.canExtract());
    }

    private static boolean booleanFact(@NotNull CompiledCompatibilityPlan plan, @NotNull String key) {
        return Boolean.parseBoolean(plan.inventoryFacts().getOrDefault(key, "false"));
    }

    public record ItemStackView(String itemId, int count) {
        public ItemStackView {
            if (itemId == null || itemId.isBlank()) {
                itemId = "minecraft:air";
            }
            count = Math.max(0, count);
        }

        public boolean matches(@Nullable ItemStackView other) {
            return other != null && itemId.equals(other.itemId());
        }

        public boolean isEmpty() {
            return count <= 0 || "minecraft:air".equals(itemId);
        }
    }

    public record FluidStackView(String fluidId, int amount) {
        public FluidStackView {
            if (fluidId == null || fluidId.isBlank()) {
                fluidId = "minecraft:empty";
            }
            amount = Math.max(0, amount);
        }
    }

    private interface RuntimeInventoryAdapter {
        int slotCount();
        boolean supportsInsertion();
        boolean supportsExtraction();
        @Nullable ItemStackView getItem(int slot);
        int insertItem(@NotNull ItemStackView item, int slot, @Nullable String side, boolean simulate);
        int extractItem(@NotNull ItemStackView item, int slot, @Nullable String side, boolean simulate);

        static @Nullable RuntimeInventoryAdapter from(@Nullable Object runtimeTarget) {
            if (runtimeTarget == null) {
                return null;
            }
            if (runtimeTarget instanceof RuntimeInventoryAdapter adapter) {
                return adapter;
            }
            if (!hasMethod(runtimeTarget, "insertItem", "insert", "addItem")
                && !hasMethod(runtimeTarget, "extractItem", "extract", "takeItem")) {
                return null;
            }
            return new ReflectiveRuntimeInventoryAdapter(runtimeTarget);
        }
    }

    private interface RuntimeFluidAdapter {
        int tankCount();
        boolean supportsInsertion();
        boolean supportsExtraction();
        int tankCapacity(int tank);
        @Nullable FluidStackView getFluid(int tank);
        int insertFluid(@NotNull FluidStackView fluid, int tank, @Nullable String side, boolean simulate);
        int extractFluid(@NotNull FluidStackView fluid, int tank, @Nullable String side, boolean simulate);

        static @Nullable RuntimeFluidAdapter from(@Nullable Object runtimeTarget) {
            if (runtimeTarget == null) {
                return null;
            }
            if (runtimeTarget instanceof RuntimeFluidAdapter adapter) {
                return adapter;
            }
            if (!hasMethod(runtimeTarget, "fill", "insertFluid")
                && !hasMethod(runtimeTarget, "drain", "extractFluid")) {
                return null;
            }
            return new ReflectiveRuntimeFluidAdapter(runtimeTarget);
        }
    }

    private interface RuntimeEnergyAdapter {
        boolean canReceive();
        boolean canExtract();
        int energyStored();
        int maxEnergy();
        int receiveEnergy(int amount, @Nullable String side, boolean simulate);
        int extractEnergy(int amount, @Nullable String side, boolean simulate);

        static @Nullable RuntimeEnergyAdapter from(@Nullable Object runtimeTarget) {
            if (runtimeTarget == null) {
                return null;
            }
            if (runtimeTarget instanceof RuntimeEnergyAdapter adapter) {
                return adapter;
            }
            if (!hasMethod(runtimeTarget, "receiveEnergy")
                && !hasMethod(runtimeTarget, "extractEnergy")) {
                return null;
            }
            return new ReflectiveRuntimeEnergyAdapter(runtimeTarget);
        }
    }

    private static final class RuntimeBackedItemTransferBridge implements ItemTransferBridge {
        private final CompiledCompatibilityPlan plan;
        private final RuntimeInventoryAdapter inventory;

        private RuntimeBackedItemTransferBridge(@NotNull CompiledCompatibilityPlan plan, @NotNull RuntimeInventoryAdapter inventory) {
            this.plan = plan;
            this.inventory = inventory;
        }

        @Override
        public boolean executable() {
            return true;
        }

        @Override
        public boolean canInsert(@NotNull Identifier blockIdentifier) {
            return inventory.supportsInsertion();
        }

        @Override
        public boolean canExtract(@NotNull Identifier blockIdentifier) {
            return inventory.supportsExtraction();
        }

        @Override
        @Nullable
        public String inventoryType(@NotNull Identifier blockIdentifier) {
            return plan.inventoryFacts().getOrDefault("inventory_type", "generic");
        }

        @Override
        public int slotCount(@NotNull Identifier blockIdentifier) {
            return inventory.slotCount();
        }

        @Override
        public @Nullable ItemStackView itemAt(@NotNull Identifier blockIdentifier, int slot) {
            return inventory.getItem(slot);
        }

        @Override
        public int insert(@NotNull Identifier blockIdentifier, @NotNull ItemStackView item, int slot, @Nullable String side, boolean simulate) {
            return inventory.insertItem(item, slot, side, simulate);
        }

        @Override
        public int extract(@NotNull Identifier blockIdentifier, @NotNull ItemStackView item, int slot, @Nullable String side, boolean simulate) {
            return inventory.extractItem(item, slot, side, simulate);
        }
    }

    private static final class RuntimeBackedFluidTransferBridge implements FluidTransferBridge {
        private final CompiledCompatibilityPlan plan;
        private final RuntimeFluidAdapter runtime;

        private RuntimeBackedFluidTransferBridge(@NotNull CompiledCompatibilityPlan plan, @NotNull RuntimeFluidAdapter runtime) {
            this.plan = plan;
            this.runtime = runtime;
        }

        @Override
        public boolean executable() {
            return true;
        }

        @Override
        public boolean canInsertFluid(@NotNull Identifier blockIdentifier) {
            return runtime.supportsInsertion();
        }

        @Override
        public boolean canExtractFluid(@NotNull Identifier blockIdentifier) {
            return runtime.supportsExtraction();
        }

        @Override
        @Nullable
        public String tankType(@NotNull Identifier blockIdentifier) {
            return plan.inventoryFacts().getOrDefault("tank_type", "generic");
        }

        @Override
        public int tankCount(@NotNull Identifier blockIdentifier) {
            return runtime.tankCount();
        }

        @Override
        public int tankCapacity(@NotNull Identifier blockIdentifier, int tank) {
            return runtime.tankCapacity(tank);
        }

        @Override
        public @Nullable FluidStackView tankAt(@NotNull Identifier blockIdentifier, int tank) {
            return runtime.getFluid(tank);
        }

        @Override
        public int insertFluid(@NotNull Identifier blockIdentifier, @NotNull FluidStackView fluid, int tank, @Nullable String side, boolean simulate) {
            return runtime.insertFluid(fluid, tank, side, simulate);
        }

        @Override
        public int extractFluid(@NotNull Identifier blockIdentifier, @NotNull FluidStackView fluid, int tank, @Nullable String side, boolean simulate) {
            return runtime.extractFluid(fluid, tank, side, simulate);
        }
    }

    private static final class RuntimeBackedEnergyTransferBridge implements EnergyTransferBridge {
        private final CompiledCompatibilityPlan plan;
        private final RuntimeEnergyAdapter runtime;

        private RuntimeBackedEnergyTransferBridge(@NotNull CompiledCompatibilityPlan plan, @NotNull RuntimeEnergyAdapter runtime) {
            this.plan = plan;
            this.runtime = runtime;
        }

        @Override
        public boolean executable() {
            return true;
        }

        @Override
        public boolean canReceiveEnergy(@NotNull Identifier blockIdentifier) {
            return runtime.canReceive();
        }

        @Override
        public boolean canProvideEnergy(@NotNull Identifier blockIdentifier) {
            return runtime.canExtract();
        }

        @Override
        @Nullable
        public String energyType(@NotNull Identifier blockIdentifier) {
            return plan.inventoryFacts().getOrDefault("energy_type", "generic");
        }

        @Override
        public int getEnergyStored(@NotNull Identifier blockIdentifier) {
            return runtime.energyStored();
        }

        @Override
        public int getMaxEnergy(@NotNull Identifier blockIdentifier) {
            return runtime.maxEnergy();
        }

        @Override
        public int receiveEnergy(@NotNull Identifier blockIdentifier, int amount, @Nullable String side, boolean simulate) {
            return runtime.receiveEnergy(amount, side, simulate);
        }

        @Override
        public int extractEnergy(@NotNull Identifier blockIdentifier, int amount, @Nullable String side, boolean simulate) {
            return runtime.extractEnergy(amount, side, simulate);
        }
    }

    private static final class ReflectiveRuntimeInventoryAdapter implements RuntimeInventoryAdapter {
        private final Object target;

        private ReflectiveRuntimeInventoryAdapter(@NotNull Object target) {
            this.target = target;
        }

        @Override
        public int slotCount() {
            return asInt(invokeCompatible(target, new String[] {"getContainerSize", "size", "getSlots", "getSlotCount"}, new Object[0]));
        }

        @Override
        public boolean supportsInsertion() {
            return hasMethod(target, "insertItem", "insert", "addItem") || hasMethod(target, "canPlaceItem");
        }

        @Override
        public boolean supportsExtraction() {
            return hasMethod(target, "extractItem", "extract", "takeItem") || hasMethod(target, "canTakeItem");
        }

        @Override
        public @Nullable ItemStackView getItem(int slot) {
            Object value = invokeCompatible(target, new String[] {"getItem", "getStackInSlot"}, slot);
            if (value == null) {
                return null;
            }
            if (value instanceof ItemStackView stack) {
                return stack;
            }
            return new ItemStackView(String.valueOf(value), 1);
        }

        @Override
        public int insertItem(@NotNull ItemStackView item, int slot, @Nullable String side, boolean simulate) {
            Object result = invokeCompatible(target, new String[] {"insertItem", "insert", "addItem"}, slot, item, simulate);
            return asInt(result);
        }

        @Override
        public int extractItem(@NotNull ItemStackView item, int slot, @Nullable String side, boolean simulate) {
            Object result = invokeCompatible(target, new String[] {"extractItem", "extract", "takeItem"}, slot, item, item.count(), simulate);
            return asInt(result);
        }
    }

    private static final class ReflectiveRuntimeFluidAdapter implements RuntimeFluidAdapter {
        private final Object target;

        private ReflectiveRuntimeFluidAdapter(@NotNull Object target) {
            this.target = target;
        }

        @Override
        public int tankCount() {
            return asInt(invokeCompatible(target, new String[] {"getTanks", "size", "getTankCount"}, new Object[0]));
        }

        @Override
        public boolean supportsInsertion() {
            return hasMethod(target, "fill", "insertFluid");
        }

        @Override
        public boolean supportsExtraction() {
            return hasMethod(target, "drain", "extractFluid");
        }

        @Override
        public int tankCapacity(int tank) {
            Object result = invokeCompatible(target, new String[] {"getTankCapacity", "getCapacity"}, tank);
            return asInt(result);
        }

        @Override
        public @Nullable FluidStackView getFluid(int tank) {
            Object value = invokeCompatible(target, new String[] {"getFluidInTank"}, tank);
            if (value == null) {
                return null;
            }
            if (value instanceof FluidStackView stack) {
                return stack;
            }
            return new FluidStackView(String.valueOf(value), 0);
        }

        @Override
        public int insertFluid(@NotNull FluidStackView fluid, int tank, @Nullable String side, boolean simulate) {
            Object result = invokeCompatible(target, new String[] {"fill", "insertFluid"}, tank, fluid, simulate);
            return asInt(result);
        }

        @Override
        public int extractFluid(@NotNull FluidStackView fluid, int tank, @Nullable String side, boolean simulate) {
            Object result = invokeCompatible(target, new String[] {"drain", "extractFluid"}, tank, fluid, fluid.amount(), simulate);
            return asInt(result);
        }
    }

    private static final class ReflectiveRuntimeEnergyAdapter implements RuntimeEnergyAdapter {
        private final Object target;

        private ReflectiveRuntimeEnergyAdapter(@NotNull Object target) {
            this.target = target;
        }

        @Override
        public boolean canReceive() {
            return hasMethod(target, "canReceive") || hasMethod(target, "receiveEnergy");
        }

        @Override
        public boolean canExtract() {
            return hasMethod(target, "canExtract") || hasMethod(target, "extractEnergy");
        }

        @Override
        public int energyStored() {
            return asInt(invokeCompatible(target, new String[] {"getEnergyStored", "getStoredEnergy"}, new Object[0]));
        }

        @Override
        public int maxEnergy() {
            return asInt(invokeCompatible(target, new String[] {"getMaxEnergyStored", "getMaxEnergy"}, new Object[0]));
        }

        @Override
        public int receiveEnergy(int amount, @Nullable String side, boolean simulate) {
            Object result = invokeCompatible(target, new String[] {"receiveEnergy"}, amount, simulate);
            return asInt(result);
        }

        @Override
        public int extractEnergy(int amount, @Nullable String side, boolean simulate) {
            Object result = invokeCompatible(target, new String[] {"extractEnergy"}, amount, simulate);
            return asInt(result);
        }
    }

    private static boolean hasMethod(@NotNull Object target, @NotNull String... names) {
        return findMethod(target, names) != null;
    }

    private static @Nullable Method findMethod(@NotNull Object target, @NotNull String... names) {
        for (String name : names) {
            for (Method method : target.getClass().getMethods()) {
                if (method.getName().equals(name)) {
                    return method;
                }
            }
        }
        return null;
    }

    private static @Nullable Object invokeCompatible(@NotNull Object target, @NotNull String[] names, @Nullable Object... args) {
        for (String name : names) {
            for (Method method : target.getClass().getMethods()) {
                if (!method.getName().equals(name)) {
                    continue;
                }
                Object[] effectiveArgs = compatibleArguments(method, args);
                if (effectiveArgs == null) {
                    continue;
                }
                try {
                    return method.invoke(target, effectiveArgs);
                } catch (ReflectiveOperationException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static @Nullable Object[] compatibleArguments(@NotNull Method method, @Nullable Object... args) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (args == null || args.length == 0) {
            return parameterTypes.length == 0 ? new Object[0] : null;
        }

        if (parameterTypes.length == 0) {
            return new Object[0];
        }

        Object[] ordered = new Object[parameterTypes.length];
        boolean[] used = new boolean[args.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            Object matched = null;
            int matchedIndex = -1;
            for (int j = 0; j < args.length; j++) {
                if (used[j]) {
                    continue;
                }
                if (matchesParameterType(parameterTypes[i], args[j])) {
                    matched = args[j];
                    matchedIndex = j;
                    break;
                }
            }
            if (matched == null) {
                return null;
            }
            ordered[i] = matched;
            used[matchedIndex] = true;
        }
        return ordered;
    }

    private static boolean matchesParameterType(@NotNull Class<?> parameterType, @Nullable Object value) {
        if (value == null) {
            return !parameterType.isPrimitive();
        }
        Class<?> actualType = value.getClass();
        if (parameterType.isPrimitive()) {
            if (parameterType == int.class) {
                return value instanceof Number;
            }
            if (parameterType == boolean.class) {
                return value instanceof Boolean;
            }
            if (parameterType == long.class) {
                return value instanceof Number;
            }
            if (parameterType == double.class) {
                return value instanceof Number;
            }
            if (parameterType == float.class) {
                return value instanceof Number;
            }
            return false;
        }
        return parameterType.isAssignableFrom(actualType);
    }

    private static int asInt(@Nullable Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }
}