package org.geysermc.hydraulic.fabric.test.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.geysermc.hydraulic.compat.runtime.TransferBridgeFactory.ItemStackView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared real, persistent slot storage exposing the getContainerSize/getItem/insertItem/extractItem
 * shape that Hydraulic's {@code TransferBridgeFactory} reflectively adapts into an executable
 * ITEM_TRANSFER/MACHINE_INVENTORY bridge, without implementing any Hydraulic-specific interface.
 */
public abstract class ReflectiveItemStorageBlockEntity extends BlockEntity {
    private static final int SLOT_CAPACITY = 64;
    private static final ItemStackView EMPTY = new ItemStackView("minecraft:air", 0);

    private final ItemStackView[] slots;

    protected ReflectiveItemStorageBlockEntity(
        @NotNull BlockEntityType<?> type,
        @NotNull BlockPos pos,
        @NotNull BlockState state,
        int slotCount
    ) {
        super(type, pos, state);
        this.slots = new ItemStackView[slotCount];
        for (int i = 0; i < slotCount; i++) {
            this.slots[i] = EMPTY;
        }
    }

    public int getContainerSize() {
        return this.slots.length;
    }

    @NotNull
    public ItemStackView getItem(int slot) {
        return this.slots[slot];
    }

    public int insertItem(int slot, @NotNull ItemStackView item, @Nullable String side, boolean simulate) {
        if (slot < 0 || slot >= this.slots.length || item.count() <= 0) {
            return 0;
        }
        ItemStackView current = this.slots[slot];
        if (!current.isEmpty() && !current.matches(item)) {
            return 0;
        }
        int moved = Math.min(item.count(), SLOT_CAPACITY - current.count());
        if (moved <= 0) {
            return 0;
        }
        if (!simulate) {
            this.slots[slot] = new ItemStackView(item.itemId(), current.count() + moved);
            this.setChanged();
        }
        return moved;
    }

    public int extractItem(int slot, @NotNull ItemStackView item, int maxCount, @Nullable String side, boolean simulate) {
        if (slot < 0 || slot >= this.slots.length) {
            return 0;
        }
        ItemStackView current = this.slots[slot];
        if (current.isEmpty() || !current.matches(item)) {
            return 0;
        }
        int moved = Math.min(maxCount, current.count());
        if (moved <= 0) {
            return 0;
        }
        if (!simulate) {
            int remaining = current.count() - moved;
            this.slots[slot] = remaining == 0 ? EMPTY : new ItemStackView(current.itemId(), remaining);
            this.setChanged();
        }
        return moved;
    }

    @Override
    protected void saveAdditional(@NotNull ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < this.slots.length; i++) {
            ItemStackView stack = this.slots[i];
            output.putString("slot_" + i + "_item", stack.itemId());
            output.putInt("slot_" + i + "_count", stack.count());
        }
    }

    @Override
    protected void loadAdditional(@NotNull ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < this.slots.length; i++) {
            String itemId = input.getStringOr("slot_" + i + "_item", "minecraft:air");
            int count = input.getIntOr("slot_" + i + "_count", 0);
            this.slots[i] = new ItemStackView(itemId, count);
        }
    }
}
