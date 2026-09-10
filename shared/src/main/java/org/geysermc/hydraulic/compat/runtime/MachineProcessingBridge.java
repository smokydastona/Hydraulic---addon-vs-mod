package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.compat.ir.CompiledCompatibilityPlan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class MachineProcessingBridge {
    private static final int MAX_STACK_SIZE = 64;

    private final CompiledCompatibilityPlan plan;
    private final TransferBridgeFactory.ItemTransferBridge inventory;
    private final int inputSlot;
    private final int outputSlot;
    private final List<MachineRecipe> recipes;
    private MachineRecipe activeRecipe;
    private int progress;

    MachineProcessingBridge(
        @NotNull CompiledCompatibilityPlan plan,
        @NotNull TransferBridgeFactory.ItemTransferBridge inventory,
        int inputSlot,
        int outputSlot,
        @NotNull List<MachineRecipe> recipes
    ) {
        this.plan = plan;
        this.inventory = inventory;
        this.inputSlot = inputSlot;
        this.outputSlot = outputSlot;
        this.recipes = List.copyOf(recipes);
    }

    public int progress() {
        return this.progress;
    }

    public boolean active() {
        return this.activeRecipe != null;
    }

    public void reset() {
        this.activeRecipe = null;
        this.progress = 0;
    }

    public int duration(@NotNull Identifier blockIdentifier) {
        MachineRecipe recipe = findRecipe(blockIdentifier);
        return recipe == null ? 0 : recipe.duration();
    }

    public boolean tick(@NotNull Identifier blockIdentifier) {
        MachineRecipe recipe = findRecipe(blockIdentifier);
        if (recipe == null) {
            reset();
            return false;
        }

        if (!recipe.equals(this.activeRecipe)) {
            this.activeRecipe = recipe;
            this.progress = 0;
        }

        TransferBridgeFactory.ItemStackView input = this.inventory.itemAt(blockIdentifier, this.inputSlot);
        if (input == null || !input.matches(recipe.input()) || input.count() < recipe.input().count()) {
            reset();
            return false;
        }

        if (this.progress < recipe.duration()) {
            this.progress++;
            return true;
        }

        TransferBridgeFactory.ItemStackView output = this.inventory.itemAt(blockIdentifier, this.outputSlot);
        if (!canAcceptOutput(blockIdentifier, output, recipe.output())
            || !this.inventory.extractResult(blockIdentifier, recipe.input(), this.inputSlot, null, true).successful()
            || !this.inventory.insertResult(blockIdentifier, recipe.output(), this.outputSlot, null, true).successful()) {
            return false;
        }

        TransferBridgeFactory.OperationResult extracted = this.inventory.extractResult(blockIdentifier, recipe.input(), this.inputSlot, null, false);
        if (!extracted.successful() || extracted.moved() != recipe.input().count()) {
            reset();
            return false;
        }

        TransferBridgeFactory.OperationResult inserted = this.inventory.insertResult(blockIdentifier, recipe.output(), this.outputSlot, null, false);
        if (!inserted.successful() || inserted.moved() != recipe.output().count()) {
            this.inventory.insert(blockIdentifier, recipe.input(), this.inputSlot, null, false);
            reset();
            return false;
        }

        reset();
        return true;
    }

    private boolean canAcceptOutput(
        @NotNull Identifier blockIdentifier,
        @Nullable TransferBridgeFactory.ItemStackView existing,
        @NotNull TransferBridgeFactory.ItemStackView output
    ) {
        if (existing == null || existing.isEmpty()) {
            return true;
        }
        return existing.matches(output) && existing.count() + output.count() <= MAX_STACK_SIZE;
    }

    @Nullable
    private MachineRecipe findRecipe(@NotNull Identifier blockIdentifier) {
        TransferBridgeFactory.ItemStackView input = this.inventory.itemAt(blockIdentifier, this.inputSlot);
        if (input == null || input.isEmpty()) {
            return null;
        }
        for (MachineRecipe recipe : this.recipes) {
            if (input.matches(recipe.input()) && input.count() >= recipe.input().count()) {
                return recipe;
            }
        }
        return null;
    }

    public record MachineRecipe(
        @NotNull TransferBridgeFactory.ItemStackView input,
        @NotNull TransferBridgeFactory.ItemStackView output,
        int duration
    ) {
        public MachineRecipe {
            if (input.isEmpty() || output.isEmpty()) {
                throw new IllegalArgumentException("Machine recipes require non-empty input and output stacks");
            }
            if (duration <= 0) {
                throw new IllegalArgumentException("Machine recipe duration must be positive");
            }
        }
    }
}
