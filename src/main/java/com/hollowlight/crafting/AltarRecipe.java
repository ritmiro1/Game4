package com.hollowlight.crafting;

import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * A memory altar recipe: up to three ingredients in any arrangement, one result.
 *
 * <p>The altar is the mod's only bespoke crafting station. Its recipes are kept in code rather
 * than in JSON because each one also carries a line of story text and, in two cases, a story
 * flag that must be granted when the item is restored.</p>
 *
 * @param ingredients required items and counts
 * @param result      the item produced
 * @param storyFlag   flag granted when crafted, or empty
 * @param lore        fragment revealed on the first craft, or empty
 */
public record AltarRecipe(List<Ingredient> ingredients, ItemStack result, String storyFlag, String lore) {

    /** One required ingredient. */
    public record Ingredient(Item item, int count) {
    }

    public AltarRecipe {
        ingredients = List.copyOf(ingredients);
    }

    public boolean matches(List<ItemStack> slots) {
        for (Ingredient ingredient : ingredients) {
            int available = 0;
            for (ItemStack stack : slots) {
                if (!stack.isEmpty() && stack.isOf(ingredient.item())) {
                    available += stack.getCount();
                }
            }
            if (available < ingredient.count()) {
                return false;
            }
        }
        // No extra ingredient types may be present, so recipes cannot be ambiguous.
        for (ItemStack stack : slots) {
            if (stack.isEmpty()) {
                continue;
            }
            boolean wanted = ingredients.stream().anyMatch(ingredient -> ingredient.item() == stack.getItem());
            if (!wanted) {
                return false;
            }
        }
        return true;
    }

    public void consume(List<ItemStack> slots) {
        for (Ingredient ingredient : ingredients) {
            int remaining = ingredient.count();
            for (ItemStack stack : slots) {
                if (remaining <= 0) {
                    break;
                }
                if (!stack.isEmpty() && stack.isOf(ingredient.item())) {
                    int taken = Math.min(remaining, stack.getCount());
                    stack.decrement(taken);
                    remaining -= taken;
                }
            }
        }
    }
}
