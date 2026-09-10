package org.geysermc.hydraulic.fabric.test;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class BarrelMenu extends AbstractContainerMenu {
    public BarrelMenu(int containerId, Inventory inventory, String ignoredLabel) {
        super(ModMenus.BARREL_MENU, containerId);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}