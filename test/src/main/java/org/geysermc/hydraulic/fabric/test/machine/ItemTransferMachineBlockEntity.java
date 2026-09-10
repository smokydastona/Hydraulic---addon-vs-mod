package org.geysermc.hydraulic.fabric.test.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.geysermc.hydraulic.compat.runtime.TransferBridgeFactory.ItemStackView;
import org.geysermc.hydraulic.fabric.test.ModBlockEntities;
import org.jetbrains.annotations.NotNull;

/**
 * Real, persistent machine inventory. Its getContainerSize/getItem/insertItem/extractItem methods
 * are reflectively discovered by Hydraulic's {@code TransferBridgeFactory} runtime adapters, so this
 * block reports as an executable ITEM_TRANSFER/MACHINE_INVENTORY bridge rather than VISUAL_ONLY.
 */
public final class ItemTransferMachineBlockEntity extends BlockEntity {
    public static final int SLOT_COUNT = 3;
    private static final int SLOT_CAPACITY = 64;
    private static final ItemStackView EMPTY = new ItemStackView("minecraft:air", 0);

    private final ItemStackView[] slots = new ItemStackView[SLOT_COUNT];

    public ItemTransferMachineBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        super(ModBlockEntities.ITEM_TRANSFER_MACHINE, pos, state);
        for (int i = 0; i < SLOT_COUNT; i++) {
            this.slots[i] = EMPTY;
        }
    }

    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @NotNull
    public ItemStackView getItem(int slot) {
        return this.slots[slot];
    }

    public int insertItem(int slot, @NotNull ItemStackView item, @org.jetbrains.annotations.Nullable String side, boolean simulate) {
        if (slot < 0 || slot >= SLOT_COUNT || item.count() <= 0) {
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

    public int extractItem(int slot, @NotNull ItemStackView item, int maxCount, @org.jetbrains.annotations.Nullable String side, boolean simulate) {
        if (slot < 0 || slot >= SLOT_COUNT) {
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
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStackView stack = this.slots[i];
            output.putString("slot_" + i + "_item", stack.itemId());
            output.putInt("slot_" + i + "_count", stack.count());
        }
    }

    @Override
    protected void loadAdditional(@NotNull ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < SLOT_COUNT; i++) {
            String itemId = input.getStringOr("slot_" + i + "_item", "minecraft:air");
            int count = input.getIntOr("slot_" + i + "_count", 0);
            this.slots[i] = new ItemStackView(itemId, count);
        }
    }
}
