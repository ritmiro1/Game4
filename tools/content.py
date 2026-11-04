"""Single source of truth for every registered id in the mod.

Java code, blockstates, models, language entries, recipes, loot tables, advancements and the
validation script all read from here. If an id appears in one place it appears in all of them.
"""

MOD_ID = "hollowlight"

# --------------------------------------------------------------------------- blocks ----
CUBE_ALL = [
    "forgotten_stone", "forgotten_bricks", "cracked_forgotten_bricks",
    "drowned_stone", "drowned_bricks", "barnacled_stone",
    "living_wall", "veined_living_stone",
    "memorial_stone", "pale_memorial_tile", "tally_marks",
    "heart_stone", "heart_crystal_block",
    "rusted_grate", "root_planks", "grave_dust", "pale_crystal", "lamp_glass",
    "hollow_lantern", "memory_lamp", "memory_pedestal", "whispering_stone",
    "shrine_bricks", "shrine_lantern",
]

PILLARS = ["forgotten_pillar"]
CROSSES = ["corrupted_growth", "bioluminescent_growth"]
CUSTOM = ["memory_altar", "sealed_gate", "inscription_stone", "threshold_gate"]

BLOCKS = CUBE_ALL + PILLARS + CROSSES + CUSTOM

# per-block luminance (0 = none) used for documentation and for the validation report
LUMINANCE = {
    "corrupted_growth": 2, "heart_crystal_block": 6, "pale_crystal": 7, "lamp_glass": 9,
    "bioluminescent_growth": 10, "threshold_gate": 11, "hollow_lantern": 13,
    "shrine_lantern": 14, "memory_lamp": 15, "memory_altar": 5,
}

# ---------------------------------------------------------------------------- items ----
ITEMS = [
    "hollow_ember", "pale_scale", "living_fiber", "bone_stitch", "salt_of_hollowmere",
    "oiled_wick", "lamp_oil", "memory_shard", "seventh_seam_dust",
    "lamplighter_lantern", "cracked_lens", "lockpick", "lamplighters_journal",
    "rusted_key", "drowned_key", "heart_key",
    "miner_shiv", "rusted_pickhammer", "lantern_chain", "wickbow", "glass_bolt",
    "wax_bandage", "bitterroot_tea", "sweet_ash", "luminous_flask", "drowned_breath",
    "memory_draught",
    "torn_journal_page", "unsent_letter", "water_stained_photograph", "carved_token",
    "broken_music_box", "iver_whistle",
    "lamplighters_band", "heart_of_the_hollow",
]

# ------------------------------------------------------------------------- entities ----
ENTITIES = ["hollowed", "wickmoth", "paperface", "stitcher", "drowned_choir",
            "weeping_one", "echo", "ash_moth", "lampkeeper"]

# -------------------------------------------------------------------------- layers -----
LAYERS = [
    dict(index=1, name="layer_1", title="The Forgotten Halls", floor_y=44, ambient_light=0.055,
         fog=(20, 22, 26), sky=(0, 0, 0), water=(44, 82, 96), water_fog=(16, 32, 40),
         biome_temperature=0.4),
    dict(index=2, name="layer_2", title="The Drowned Corridors", floor_y=42, ambient_light=0.045,
         fog=(14, 30, 34), sky=(0, 0, 0), water=(30, 84, 92), water_fog=(10, 34, 40),
         biome_temperature=0.3),
    dict(index=3, name="layer_3", title="The Living Walls", floor_y=46, ambient_light=0.04,
         fog=(28, 16, 20), sky=(0, 0, 0), water=(64, 40, 44), water_fog=(24, 12, 16),
         biome_temperature=0.8),
    dict(index=4, name="layer_4", title="The Memorial Depths", floor_y=46, ambient_light=0.05,
         fog=(26, 24, 22), sky=(0, 0, 0), water=(52, 58, 62), water_fog=(20, 22, 26),
         biome_temperature=0.6),
    dict(index=5, name="layer_5", title="The Heart", floor_y=50, ambient_light=0.03,
         fog=(24, 12, 18), sky=(0, 0, 0), water=(70, 30, 40), water_fog=(22, 8, 14),
         biome_temperature=0.5),
]

# ---------------------------------------------------------------------- loot tables ----
LOOT_TABLES = [
    "chests/shrine_starter", "chests/layer_1", "chests/layer_2", "chests/layer_3",
    "chests/layer_4", "chests/layer_5", "chests/safe_room", "chests/lore_vault",
    "chests/library", "chests/chapel", "chests/trap_room", "chests/memorial",
    "chests/drowned", "chests/boss_arena", "chests/key_rusted", "chests/key_drowned",
    "chests/key_heart",
    "entities/hollowed", "entities/paperface", "entities/stitcher", "entities/drowned_choir",
    "entities/weeping_one", "entities/lampkeeper",
]

# --------------------------------------------------------------------- advancements ----
ADVANCEMENTS = [
    # depth progression
    ("depth/root", "The Dedication Stone", "Find the shrine that is not for water.", "hollowlight:block/shrine_bricks", "", False),
    ("depth/the_forgotten_halls", "The Ninth Step", "Go down one step further than anyone came back from.", "hollowlight:block/forgotten_bricks", "depth/root", False),
    ("depth/the_drowned_corridors", "Where the River Went", "Wade into the halls that remember the winter.", "hollowlight:block/drowned_bricks", "depth/the_forgotten_halls", False),
    ("depth/the_living_walls", "Something Is Warm Down Here", "Reach the walls that answer when you lean on them.", "hollowlight:block/living_wall", "depth/the_drowned_corridors", False),
    ("depth/the_memorial_depths", "A Window Underground", "Find the rooms she gave away whole.", "hollowlight:block/pale_memorial_tile", "depth/the_living_walls", False),
    ("depth/the_heart", "The Quiet Before the Answer", "Stand in the Heart of the Hollow.", "hollowlight:block/heart_stone", "depth/the_memorial_depths", False),
    # story
    ("story/where_the_lamp_ends", "Where the Lamp Ends", "Read the dedication and understand it is a warning.", "hollowlight:item/lamplighter_lantern", "depth/root", False),
    ("story/nine_names", "Nine Names, Struck", "Read the first page of Mara's journal.", "hollowlight:item/torn_journal_page", "depth/root", False),
    ("story/a_lamp_is_a_promise", "A Lamp Is a Promise", "Learn what her father taught her about light.", "hollowlight:block/hollow_lantern", "story/nine_names", False),
    ("story/the_first_trade", "The First Trade", "Discover what she gave the labyrinth first.", "hollowlight:block/inscription_stone", "story/nine_names", False),
    ("story/what_it_wants", "What It Wants", "Read the terms of the bargain.", "hollowlight:item/unsent_letter", "story/the_first_trade", False),
    ("story/the_sound_of_his_laugh", "The Sound of His Laugh", "Learn the second price she paid.", "hollowlight:item/water_stained_photograph", "story/what_it_wants", False),
    ("story/four_trades", "Four Trades", "Hear how she spent her voice.", "hollowlight:block/veined_living_stone", "story/the_sound_of_his_laugh", False),
    ("story/a_window_underground", "A Window Underground", "Find the kitchen that should not exist this deep.", "hollowlight:block/root_planks", "depth/the_memorial_depths", False),
    ("story/i_am_here", "I Am Here. I Have Been Here.", "Find the first words he scratched into stone.", "hollowlight:block/tally_marks", "story/a_window_underground", False),
    ("story/you_walked_past", "You Walked Past", "Understand what the shouting was, seven years too late.", "hollowlight:block/tally_marks", "story/i_am_here", False),
    ("story/it_was_never_your_fault", "It Was Never Your Fault", "Read the last marks. He forgave her before she ever came down.", "hollowlight:block/tally_marks", "story/you_walked_past", False),
    ("story/somebody_was_looking", "Somebody Was Always Looking For Me", "Hold the proof of what happened to Iver Vane.", "hollowlight:item/iver_whistle", "story/it_was_never_your_fault", False),
    ("story/she_does_not_know_you", "She Does Not Know You", "Meet the Lamplighter and be asked a question.", "hollowlight:item/lamplighters_band", "depth/the_heart", False),
    ("story/she_is_asking_you", "She Is Asking You", "Understand that the whisper was never for the dark.", "hollowlight:item/heart_of_the_hollow", "story/she_does_not_know_you", False),
    ("story/the_last_entry", "The Last Entry", "Read what she wrote by touch at the bottom of the world.", "hollowlight:block/heart_crystal_block", "depth/the_heart", False),
    ("story/the_quiet_after", "The Quiet After", "Finish what eleven years of walking could not.", "hollowlight:item/heart_of_the_hollow", "story/she_is_asking_you", False),
    ("story/two_pillows", "A Bed Made For Two", "Find the bed in the Memorial Depths.", "hollowlight:block/memorial_stone", "depth/the_memorial_depths", True),
    ("story/a_list_of_names", "The List", "Learn why the drowned sing.", "hollowlight:block/bioluminescent_growth", "depth/the_drowned_corridors", True),
    ("story/every_day_counted", "Every Day Counted", "Count the tallies and realise they stop.", "hollowlight:block/tally_marks", "depth/the_forgotten_halls", True),
    # endings
    ("ending/extinguishing", "Extinguishing", "Put out the last lamp and let her rest.", "hollowlight:item/heart_of_the_hollow", "story/the_quiet_after", False),
    ("ending/keeping", "Keeping", "Take up the wick. Keep the lamp lit for whoever comes next.", "hollowlight:item/lamplighter_lantern", "story/the_quiet_after", False),
    ("ending/carrying_him_up", "Carrying Him Up", "Give the Heart a name it cannot keep.", "hollowlight:item/iver_whistle", "story/somebody_was_looking", True),
    # hidden
    ("hidden/you_did_not_have_to", "You Did Not Have To", "Leave the Sorrow alone. It was never going to hurt you.", "hollowlight:item/memory_shard", "depth/the_memorial_depths", True),
    ("hidden/a_thing_that_only_cried", "A Thing That Only Cried", "Kill the one resident of the labyrinth that never fought back.", "hollowlight:item/memory_shard", "depth/the_memorial_depths", True),
    ("hidden/four_notes", "Four Notes and a Fifth That Is Not There", "Restore the music box at a memory altar.", "hollowlight:item/broken_music_box", "depth/the_memorial_depths", True),
    ("hidden/two_notes_both_wrong", "Two Notes, Both Wrong", "Blow Iver's whistle somewhere it can be heard.", "hollowlight:item/iver_whistle", "story/somebody_was_looking", True),
]
