package com.hollowlight.gui;

import com.hollowlight.crafting.AltarRecipe;
import com.hollowlight.crafting.AltarRecipes;
import com.hollowlight.registry.ModScreenHandlers;
import com.hollowlight.registry.ModSounds;
import com.hollowlight.story.StoryManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;

/**
 * The memory altar's container. Three inputs, one result, and the rule that matters: taking the
 * result consumes exactly the ingredients the recipe asked for and nothing else. Recipes are
 * matched against the combined contents of the input slots, so a player can put one item in each
 * slot or a stack in a single slot and both work.
 */
public class MemoryAltarScreenHandler extends ScreenHandler {

    private static final int INPUT_SLOTS = 3;

    private final SimpleInventory inputs = new SimpleInventory(INPUT_SLOTS);
    private final SimpleInventory output = new SimpleInventory(1);
    private final PlayerInventory playerInventory;
    private ItemStack lastResult = ItemStack.EMPTY;

    public MemoryAltarScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(ModScreenHandlers.MEMORY_ALTAR, syncId);
        this.playerInventory = playerInventory;

        for (int index = 0; index < INPUT_SLOTS; index++) {
            this.addSlot(new Slot(inputs, index, 26 + index * 26, 20));
        }
        this.addSlot(new Slot(output, 0, 134, 20) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }

            @Override
            public void onTakeItem(PlayerEntity player, ItemStack stack) {
                MemoryAltarScreenHandler.this.consumeIngredients(player, stack);
                super.onTakeItem(player, stack);
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }

        inputs.addListener(inventory -> updateResult());
        output.addListener(inventory -> {
        });
    }

    private List<ItemStack> inputSnapshot() {
        List<ItemStack> stacks = new ArrayList<>(INPUT_SLOTS);
        for (int index = 0; index < INPUT_SLOTS; index++) {
            stacks.add(inputs.getStack(index));
        }
        return stacks;
    }

    private void updateResult() {
        AltarRecipe recipe = AltarRecipes.find(inputSnapshot());
        ItemStack result = recipe == null ? ItemStack.EMPTY : recipe.result().copy();
        if (!ItemStack.areEqual(result, lastResult)) {
            lastResult = result;
            output.setStack(0, result);
            sendContentUpdates();
        }
    }

    private void consumeIngredients(PlayerEntity player, ItemStack taken) {
        List<ItemStack> stacks = inputSnapshot();
        AltarRecipe recipe = AltarRecipes.find(stacks);
        if (recipe == null) {
            return;
        }
        recipe.consume(stacks);
        for (int index = 0; index < INPUT_SLOTS; index++) {
            inputs.setStack(index, stacks.get(index));
        }
        if (player instanceof ServerPlayerEntity serverPlayer) {
            if (!recipe.storyFlag().isEmpty()) {
                StoryManager.grant(serverPlayer, recipe.storyFlag());
            }
            if (!recipe.lore().isEmpty()) {
                StoryManager.discover(serverPlayer, recipe.lore());
            }
            player.getWorld().playSound(null, player.getBlockPos(), ModSounds.ALTAR_ACTIVATE,
                    SoundCategory.BLOCKS, 0.9F, 1.1F);
        }
        lastResult = ItemStack.EMPTY;
        updateResult();
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        // Never swallow items: hand anything left on the altar back to the player.
        for (int index = 0; index < INPUT_SLOTS; index++) {
            ItemStack stack = inputs.getStack(index);
            if (!stack.isEmpty()) {
                if (!player.getInventory().insertStack(stack)) {
                    player.dropItem(stack, false);
                }
                inputs.setStack(index, ItemStack.EMPTY);
            }
        }
        ItemStack result = output.getStack(0);
        if (!result.isEmpty()) {
            if (!player.getInventory().insertStack(result)) {
                player.dropItem(result, false);
            }
            output.setStack(0, ItemStack.EMPTY);
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        ItemStack moved = ItemStack.EMPTY;
        Slot source = this.slots.get(slot);
        if (source != null && source.hasStack()) {
            ItemStack stack = source.getStack();
            moved = stack.copy();
            if (slot < INPUT_SLOTS + 1) {
                if (!this.insertItem(stack, INPUT_SLOTS + 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                boolean inserted = this.insertItem(stack, 0, INPUT_SLOTS, false);
                if (!inserted) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                source.setStack(ItemStack.EMPTY);
            } else {
                source.markDirty();
            }
        }
        return moved;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    public Inventory inputs() {
        return inputs;
    }
}
