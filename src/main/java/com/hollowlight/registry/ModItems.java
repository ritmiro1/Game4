package com.hollowlight.registry;

import com.hollowlight.Hollowlight;
import com.hollowlight.item.ArtifactItem;
import com.hollowlight.item.ConsumableItem;
import com.hollowlight.item.JournalItem;
import com.hollowlight.item.KeyTier;
import com.hollowlight.item.LabyrinthKeyItem;
import com.hollowlight.item.LanternItem;
import com.hollowlight.item.LensItem;
import com.hollowlight.item.LockpickItem;
import com.hollowlight.item.LoreItem;
import com.hollowlight.item.WeaponItem;
import com.hollowlight.item.WickbowItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Rarity;

/**
 * Every item in the mod. Thirty-five objects that a player will come to know by their weight
 * in the inventory: the light, the keys, the things people left behind, and the things that
 * were made to keep them safe and mostly didn't.
 */
public final class ModItems {

    private static final List<Item> ALL = new ArrayList<>();

    // ================================= MATERIALS ==========================================
    public static final Item HOLLOW_EMBER = register("hollow_ember", Item::new,
            new Item.Settings().maxCount(64));
    public static final Item PALE_SCALE = register("pale_scale", Item::new,
            new Item.Settings().maxCount(64));
    public static final Item LIVING_FIBER = register("living_fiber", Item::new,
            new Item.Settings().maxCount(64));
    public static final Item BONE_STITCH = register("bone_stitch", Item::new,
            new Item.Settings().maxCount(64));
    public static final Item SALT_OF_HOLLOWMERE = register("salt_of_hollowmere", Item::new,
            new Item.Settings().maxCount(64));
    public static final Item OILED_WICK = register("oiled_wick", Item::new,
            new Item.Settings().maxCount(64));
    public static final Item LAMP_OIL = register("lamp_oil", Item::new,
            new Item.Settings().maxCount(16));
    public static final Item MEMORY_SHARD = register("memory_shard",
            settings -> new LoreItem(settings, "memory_shard", "item.hollowlight.memory_shard.flavour"),
            new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON));
    public static final Item SEVENTH_SEAM_DUST = register("seventh_seam_dust", Item::new,
            new Item.Settings().maxCount(64));

    // =================================== TOOLS ============================================
    public static final Item LAMPLIGHTERS_LANTERN = register("lamplighter_lantern", LanternItem::new,
            new Item.Settings().maxCount(1).maxDamage(160).rarity(Rarity.UNCOMMON));
    public static final Item CRACKED_LENS = register("cracked_lens", LensItem::new,
            new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON));
    public static final Item LOCKPICK = register("lockpick", LockpickItem::new,
            new Item.Settings().maxCount(1).maxDamage(48));
    public static final Item LAMPLIGHTERS_JOURNAL = register("lamplighters_journal", JournalItem::new,
            new Item.Settings().maxCount(1));

    // ==================================== KEYS ============================================
    public static final Item RUSTED_KEY = register("rusted_key",
            settings -> new LabyrinthKeyItem(settings, KeyTier.RUSTED, "item.hollowlight.rusted_key.flavour"),
            new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON));
    public static final Item DROWNED_KEY = register("drowned_key",
            settings -> new LabyrinthKeyItem(settings, KeyTier.DROWNED, "item.hollowlight.drowned_key.flavour"),
            new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON));
    public static final Item HEART_KEY = register("heart_key",
            settings -> new LabyrinthKeyItem(settings, KeyTier.HEART, "item.hollowlight.heart_key.flavour"),
            new Item.Settings().maxCount(1).rarity(Rarity.RARE));

    // =================================== WEAPONS ==========================================
    public static final Item MINER_SHIV = register("miner_shiv",
            settings -> new WeaponItem(settings, "item.hollowlight.miner_shiv.flavour", false),
            weapon(260, 4.0, 1.6, Rarity.COMMON));
    public static final Item RUSTED_PICKHAMMER = register("rusted_pickhammer",
            settings -> new WeaponItem(settings, "item.hollowlight.rusted_pickhammer.flavour", true),
            weapon(420, 7.0, -2.8, Rarity.COMMON));
    public static final Item LANTERN_CHAIN = register("lantern_chain",
            settings -> new WeaponItem(settings, "item.hollowlight.lantern_chain.flavour", true),
            weapon(640, 6.0, -1.6, Rarity.UNCOMMON));
    public static final Item WICKBOW = register("wickbow", WickbowItem::new,
            new Item.Settings().maxCount(1).maxDamage(220).rarity(Rarity.UNCOMMON));
    public static final Item GLASS_BOLT = register("glass_bolt", Item::new,
            new Item.Settings().maxCount(64));

    // ================================= CONSUMABLES ========================================
    public static final Item WAX_BANDAGE = register("wax_bandage",
            settings -> new ConsumableItem(settings, 6.0F, 2.0F, List.of(), "item.hollowlight.wax_bandage.flavour", false),
            new Item.Settings().maxCount(16));
    public static final Item BITTERROOT_TEA = register("bitterroot_tea",
            settings -> new ConsumableItem(settings, 1.0F, 8.0F,
                    List.of(ConsumableItem.effect(ConsumableItem.REGEN, 10, 0)), "item.hollowlight.bitterroot_tea.flavour", false),
            new Item.Settings().maxCount(16));
    public static final Item SWEET_ASH = register("sweet_ash",
            settings -> new ConsumableItem(settings, 0.0F, 14.0F,
                    List.of(ConsumableItem.effect(ConsumableItem.RESIST, 5, 0)), "item.hollowlight.sweet_ash.flavour", false),
            new Item.Settings().maxCount(16));
    public static final Item LUMINOUS_FLASK = register("luminous_flask",
            com.hollowlight.item.FlaskItem::new, new Item.Settings().maxCount(8));
    public static final Item DROWNED_BREATH = register("drowned_breath",
            settings -> new ConsumableItem(settings, 0.0F, 4.0F,
                    List.of(ConsumableItem.effect(ConsumableItem.BREATH, 90, 0),
                            ConsumableItem.effect(ConsumableItem.GRACE, 25, 0)),
                    "item.hollowlight.drowned_breath.flavour", false),
            new Item.Settings().maxCount(8));
    public static final Item MEMORY_DRAUGHT = register("memory_draught",
            settings -> new ConsumableItem(settings, 4.0F, 35.0F,
                    List.of(ConsumableItem.effect(ConsumableItem.SLOW, 3, 0)), "item.hollowlight.memory_draught.flavour", true),
            new Item.Settings().maxCount(4).rarity(Rarity.RARE));

    // ==================================== LORE ============================================
    public static final Item TORN_JOURNAL_PAGE = register("torn_journal_page",
            settings -> new LoreItem(settings, "page_the_first_descent", "item.hollowlight.torn_journal_page.flavour"),
            new Item.Settings().maxCount(16));
    public static final Item UNSENT_LETTER = register("unsent_letter",
            settings -> new LoreItem(settings, "letter_to_iver", "item.hollowlight.unsent_letter.flavour"),
            new Item.Settings().maxCount(16));
    public static final Item WATER_STAINED_PHOTOGRAPH = register("water_stained_photograph",
            settings -> new LoreItem(settings, "photograph", "item.hollowlight.water_stained_photograph.flavour"),
            new Item.Settings().maxCount(16));
    public static final Item CARVED_TOKEN = register("carved_token",
            settings -> new LoreItem(settings, "carved_token", "item.hollowlight.carved_token.flavour"),
            new Item.Settings().maxCount(16));
    public static final Item BROKEN_MUSIC_BOX = register("broken_music_box",
            com.hollowlight.item.MusicBoxItem::new,
            new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON));
    public static final Item IVER_WHISTLE = register("iver_whistle",
            com.hollowlight.item.WhistleItem::new,
            new Item.Settings().maxCount(1).rarity(Rarity.RARE));

    // ================================== ARTIFACTS =========================================
    public static final Item LAMPLIGHTERS_BAND = register("lamplighters_band",
            settings -> new ArtifactItem(settings, ArtifactItem.Kind.ANCHOR,
                    "item.hollowlight.lamplighters_band.flavour"),
            new Item.Settings().maxCount(1).rarity(Rarity.RARE));
    public static final Item HEART_OF_THE_HOLLOW = register("heart_of_the_hollow",
            settings -> new ArtifactItem(settings, ArtifactItem.Kind.FINAL,
                    "item.hollowlight.heart_of_the_hollow.flavour"),
            new Item.Settings().maxCount(1).rarity(Rarity.EPIC));

    private ModItems() {
    }

    private static Item.Settings weapon(int durability, double damage, double speedDelta, Rarity rarity) {
        AttributeModifiersComponent modifiers = AttributeModifiersComponent.builder()
                .add(EntityAttributes.ATTACK_DAMAGE,
                        new EntityAttributeModifier(Hollowlight.id("base_attack_damage"), damage,
                                EntityAttributeModifier.Operation.ADD_VALUE),
                        AttributeModifierSlot.MAINHAND)
                .add(EntityAttributes.ATTACK_SPEED,
                        new EntityAttributeModifier(Hollowlight.id("base_attack_speed"), speedDelta,
                                EntityAttributeModifier.Operation.ADD_VALUE),
                        AttributeModifierSlot.MAINHAND)
                .build();
        return new Item.Settings().maxCount(1).maxDamage(durability).rarity(rarity)
                .attributeModifiers(modifiers);
    }

    private static Item register(String name, Function<Item.Settings, Item> factory, Item.Settings settings) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Hollowlight.id(name));
        Item item = Registry.register(Registries.ITEM, key, factory.apply(settings.registryKey(key)));
        ALL.add(item);
        return item;
    }

    public static List<Item> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static void bootstrap() {
        Hollowlight.LOGGER.debug("Registered {} items", ALL.size());
    }
}
