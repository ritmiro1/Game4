package com.hollowlight.registry;

import com.hollowlight.Hollowlight;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;

/**
 * The single creative tab.
 *
 * <p>Blocks come first because that is what the player will be holding most of the time: the
 * stone of a place they have not been yet. Everything else follows in the order it becomes
 * meaningful — light, keys, then the things people left behind.</p>
 */
public final class ModItemGroups {

    public static final RegistryKey<ItemGroup> MAIN =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, Hollowlight.id("main"));

    private ModItemGroups() {
    }

    public static void bootstrap() {
        Registry.register(Registries.ITEM_GROUP, MAIN, build());
    }

    private static ItemGroup build() {
        return FabricItemGroup.builder()
                .icon(() -> new ItemStack(ModItems.LAMPLIGHTERS_LANTERN))
                .displayName(Text.translatable("itemGroup.hollowlight.main"))
                .entries((context, entries) -> {
                    ModBlocks.blockItems().forEach(entries::add);
                    ModItems.all().forEach(entries::add);
                })
                .build();
    }
}
