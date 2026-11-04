package com.hollowlight.crafting;

import com.hollowlight.registry.ModBlocks;
import com.hollowlight.registry.ModItems;
import com.hollowlight.story.StoryFlags;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * Every memory altar recipe in the mod, in the order the journal lists them.
 *
 * <p>These are the twenty-odd recipes that are not workbench recipes: restoration, binding,
 * bottling memory, and the small domestic crafts that people who live underground invent.</p>
 */
public final class AltarRecipes {

    private static final List<AltarRecipe> RECIPES = List.of(
            // --- light ---------------------------------------------------------------
            new AltarRecipe(List.of(new AltarRecipe.Ingredient(ModItems.LAMP_OIL, 1),
                    new AltarRecipe.Ingredient(ModItems.OILED_WICK, 1)),
                    new ItemStack(ModItems.LAMPLIGHTERS_LANTERN), "", "altar_inspected"),
            new AltarRecipe(List.of(new AltarRecipe.Ingredient(ModItems.LAMP_OIL, 2),
                    new AltarRecipe.Ingredient(ModItems.HOLLOW_EMBER, 1)),
                    new ItemStack(ModItems.LUMINOUS_FLASK, 2), "", "altar_inspected"),
            new AltarRecipe(List.of(new AltarRecipe.Ingredient(ModItems.PALE_SCALE, 1),
                    new AltarRecipe.Ingredient(Items.GLASS, 2)),
                    new ItemStack(ModBlocks.LAMP_GLASS.asItem(), 4), "", ""),
            // --- the weapons ---------------------------------------------------------
            new AltarRecipe(List.of(new AltarRecipe.Ingredient(ModItems.GLASS_BOLT, 4),
                    new AltarRecipe.Ingredient(ModItems.LIVING_FIBER, 1)),
                    new ItemStack(ModItems.WICKBOW), "", "altar_inspected"),
            new AltarRecipe(List.of(new AltarRecipe.Ingredient(ModItems.BONE_STITCH, 2),
                    new AltarRecipe.Ingredient(ModItems.HOLLOW_EMBER, 1)),
                    new ItemStack(ModItems.GLASS_BOLT, 8), "", ""),
            new AltarRecipe(List.of(new AltarRecipe.Ingredient(ModItems.SALT_OF_HOLLOWMERE, 2),
                    new AltarRecipe.Ingredient(Items.STICK, 1)),
                    new ItemStack(ModItems.MINER_SHIV), "", ""),
            // --- medicine and comfort ------------------------------------------------
            new AltarRecipe(List.of(new AltarRecipe.Ingredient(ModItems.LIVING_FIBER, 2),
                    new AltarRecipe.Ingredient(Items.PAPER, 1)),
                    new ItemStack(ModItems.WAX_BANDAGE, 3), "", ""),
            new AltarRecipe(List.of(new AltarRecipe.Ingredient(ModItems.BITTERROOT_TEA, 1),
                    new AltarRecipe.Ingredient(ModItems.MEMORY_SHARD, 1)),
                    new ItemStack(ModItems.MEMORY_DRAUGHT), "", ""),
            new AltarRecipe(List.of(new AltarRecipe.Ingredient(ModItems.SALT_OF_HOLLOWMERE, 1),
                    new AltarRecipe.Ingredient(Items.SUGAR, 1)),
                    new ItemStack(ModItems.SWEET_ASH, 2), "", ""),
            new AltarRecipe(List.of(new AltarRecipe.Ingredient(ModItems.PALE_SCALE, 2),
                    new AltarRecipe.Ingredient(Items.KELP, 1)),
                    new ItemStack(ModItems.DROWNED_BREATH, 1), "", ""),
            // --- restoration (the altar's real purpose) ------------------------------
            new AltarRecipe(List.of(new AltarRecipe.Ingredient(ModItems.BROKEN_MUSIC_BOX, 1),
                    new AltarRecipe.Ingredient(ModItems.MEMORY_SHARD, 2),
                    new AltarRecipe.Ingredient(ModItems.OILED_WICK, 1)),
                    new ItemStack(ModItems.BROKEN_MUSIC_BOX), StoryFlags.MUSIC_BOX_RESTORED, "music_box_tune"),
            new AltarRecipe(List.of(new AltarRecipe.Ingredient(ModItems.SEVENTH_SEAM_DUST, 1),
                    new AltarRecipe.Ingredient(Items.BOWL, 1)),
                    new ItemStack(ModItems.MEMORY_SHARD, 2), "", ""),
            new AltarRecipe(List.of(new AltarRecipe.Ingredient(ModItems.CARVED_TOKEN, 1),
                    new AltarRecipe.Ingredient(ModItems.SALT_OF_HOLLOWMERE, 2)),
                    new ItemStack(ModItems.RUSTED_KEY), "", "carved_token"),
            new AltarRecipe(List.of(new AltarRecipe.Ingredient(ModItems.UNSENT_LETTER, 1),
                    new AltarRecipe.Ingredient(ModItems.PALE_SCALE, 1),
                    new AltarRecipe.Ingredient(ModItems.MEMORY_SHARD, 1)),
                    new ItemStack(ModItems.DROWNED_KEY), "", "letter_to_iver"),
            new AltarRecipe(List.of(new AltarRecipe.Ingredient(ModItems.TORN_JOURNAL_PAGE, 3),
                    new AltarRecipe.Ingredient(ModItems.HOLLOW_EMBER, 2)),
                    new ItemStack(ModItems.LAMPLIGHTERS_JOURNAL), "", ""),
            new AltarRecipe(List.of(new AltarRecipe.Ingredient(ModItems.MEMORY_SHARD, 3),
                    new AltarRecipe.Ingredient(ModItems.BONE_STITCH, 1),
                    new AltarRecipe.Ingredient(Items.GOLD_INGOT, 1)),
                    new ItemStack(ModItems.LAMPLIGHTERS_BAND), StoryFlags.WEEPING_SPARED, "lamplighters_band"),
            // --- keys and mechanisms -------------------------------------------------
            new AltarRecipe(List.of(new AltarRecipe.Ingredient(ModItems.RUSTED_KEY, 1),
                    new AltarRecipe.Ingredient(ModItems.BONE_STITCH, 2),
                    new AltarRecipe.Ingredient(ModItems.PALE_SCALE, 1)),
                    new ItemStack(ModItems.LOCKPICK), "", ""),
            new AltarRecipe(List.of(new AltarRecipe.Ingredient(ModItems.HOLLOW_EMBER, 3),
                    new AltarRecipe.Ingredient(ModItems.SALT_OF_HOLLOWMERE, 1)),
                    new ItemStack(ModItems.LAMP_OIL, 2), "", ""),
            new AltarRecipe(List.of(new AltarRecipe.Ingredient(ModItems.LIVING_FIBER, 3),
                    new AltarRecipe.Ingredient(ModItems.HOLLOW_EMBER, 1)),
                    new ItemStack(ModItems.OILED_WICK, 2), "", ""),
            new AltarRecipe(List.of(new AltarRecipe.Ingredient(ModItems.MEMORY_SHARD, 1),
                    new AltarRecipe.Ingredient(ModItems.SEVENTH_SEAM_DUST, 1),
                    new AltarRecipe.Ingredient(ModItems.BONE_STITCH, 1)),
                    new ItemStack(ModItems.HEART_KEY), "", "iver_last_carving")
    );

    private AltarRecipes() {
    }

    public static List<AltarRecipe> all() {
        return RECIPES;
    }

    public static AltarRecipe find(List<ItemStack> slots) {
        for (AltarRecipe recipe : RECIPES) {
            if (recipe.matches(slots)) {
                return recipe;
            }
        }
        return null;
    }
}
