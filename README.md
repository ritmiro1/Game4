# Hollowlight — The Lamplighter's Descent

A psychological horror / labyrinth-exploration mod for **Minecraft 1.21.4 (Fabric, Java 21)**.

> Beneath a shrine that is not for water, a woman has been walking down for eleven years.
> She sells the labyrinth a memory at a time — her voice, her face, the sound of her brother's
> laugh — to keep a lamp burning for a boy who has been dead almost that whole time.
> You are the first person to follow her down. Bring a lantern. Listen for a voice calling a
> name that is not yours.

**Content warning:** grief, implied child death, self-erasure, sustained low-level dread,
whispering voices, and darkness. There are no jump-scare stings louder than a footstep, no
screen shake, no flashing lights, and no gore. The horror is architectural.

---

## Download

**Compiled and packaged by GitHub Actions on JDK 21** (`./gradlew build`, Minecraft 1.21.4,
Yarn 1.21.4+build.8, Fabric Loader 0.16.9+, Fabric API 0.119.4+1.21.4):

- jar: <https://github.com/ritmiro1/Game4/releases/download/v1.0.3/hollowlight-1.0.2.jar>
  (3,196,644 bytes)
- release page: <https://github.com/ritmiro1/Game4/releases/tag/v1.0.3>
- workflow: <https://github.com/ritmiro1/Game4/actions>

Install: put the jar and **Fabric API 0.119.4+1.21.4** into `.minecraft/mods`, select the
Fabric 1.21.4 profile, launch. The mod list should read `hollowlight 1.0.2`.

Version history, because it matters when something breaks:

| build | what happened |
| --- | --- |
| 1.0.0 | crashed at launch: the labyrinth chunk generator was registered twice |
| 1.0.1 | black screen: a block model used the illegal face name `"all"`, and the drowned choir's model tree was built one nesting level too shallow (`Can't find part head`), which aborted the resource reload |
| 1.0.2 | those are fixed, and `tools/check_assets.py` now fails the build on that whole family of data errors |

The jar compiles and gets through mod initialisation and resource loading. World generation, mob
behaviour, sound and the GUIs have still never been watched running in a real game session.

---

## Requirements

| Dependency | Version |
|---|---|
| Minecraft | 1.21.4 |
| Fabric Loader | 0.16.9 or newer |
| Fabric API | 0.119.4+1.21.4 or newer |
| Java | 21 |

## Installation

1. Install Fabric Loader for 1.21.4.
2. Put `fabric-api-0.119.4+1.21.4.jar` in `mods/`.
3. Put `hollowlight-1.0.0.jar` in `mods/`.
4. Launch. Find the shrine: it generates once per world, on the surface, a few hundred blocks
   from spawn, on the flattest ground it can find.

## Playing it

| Control | Action |
|---|---|
| `J` | Open the Lamplighter's Journal (lore, chapters, fragment counter) |
| `M` | Open the Cracked Lens — a map of the cells you have walked |
| Right-click the lantern | Light it, place it, or take it back |
| Right-click a lore item | Read it; it is filed in the journal |
| Right-click a Memory Altar | Open the altar, offer two items, receive what the memory is worth |

The journal and the map are the two interfaces you will live in. Both are drawn by hand in
`gui/` and fed by server payloads (`ModPayloads`), so a client cannot invent lore or map cells.

## Content

**Five labyrinth layers** — The Forgotten Halls, The Drowned Corridors, The Living Walls, The
Memorial Depths, The Heart — each a full dimension with its own palette, fog, ambience bed and
music. One surface shrine with a sealed threshold in the overworld.

**Procedural maze, deterministic per seed.** `LabyrinthLayout` builds a 40×40 cell maze with a
braided spanning tree (`braidChance`, default 0.45) so loops always exist, then places twelve
kinds of handcrafted room: landing, descent, safe room, lore vault, memorial room, library, trap
room, observation chamber, chapel, flooded garden, boss arena and final chamber. Room interiors
are furnished by `RoomFurnisher` — forty-odd rooms' worth of arrangement logic, from the chapel's
candles to the kitchen that should not exist this deep.

**31 blocks.** Including the memory altar, sealed and threshold gates, inscribed stone,
whispering stone, tally marks, living walls, heart crystal, nine light sources and three
growths.

**35 items.** Lantern and lamp oil, five keys and a lockpick, a wickbow that fires glass bolts,
miner's shiv and pickhammer, wax bandages and bitterroot tea, and eleven story objects —
including Iver's tin whistle and the broken music box, both of which are required for specific
things to happen.

**9 entities.** The hollowed, the wickmoth, paperface (only moves when unobserved), the
stitcher, the drowned choir, the weeping one (never hostile — killing it is a choice the game
remembers), the echo, the ash moth, and **the Lamplighter**, a three-phase boss who asks a
question before she fights and whose third phase is her forgetting how to speak.

**53 lore fragments** across six chapters, from the dedication stone at the surface to what Mara
wrote by touch at the bottom of the world. Fragments are found as items, inscriptions, room
details and audio; each one is filed, counts toward the journal total, and gates advancement
trees and altar trades.

**32 advancements** in four trees (depth, story, endings, hidden), all triggered by real game
events rather than placeholder criteria.

**40 recipes** plus the altar's 20 memory trades; **23 loot tables**; **50 sound files** (7 music
tracks, 6 ambience beds, all four layer-specific, plus ambience, entity, interaction and horror
cues) — every one synthesised from scratch by `tools/gen_sounds.py` (sine partials, filtered
noise, convolution-free reverb taps), no sampled audio anywhere.

**Sanity.** 0–100, shown as a lantern glyph in the HUD. Darkness and the things in it drain it;
light, safe rooms, lamps, tea and certain objects restore it. At 60 you hear things, at 40 you see
things, at 25 the dark gets heavy, at 12 the dark gets closer. It never kills, never applies
damage, never locks a door, and always recovers — see *Design rules* below.

**Config** at `config/hollowlight.json`, written on first launch: sanity multipliers, horror event
interval, apparition distance, labyrinth size, floor Y, braid chance, ceiling scale, entity spawn
multiplier, music/ambience/HUD toggles, and a master horror switch. Every field is documented in
`config/ModConfig.java`.

## Design rules the code enforces

* **Horror is psychological.** `HorrorDirector` never spawns on top of a player, never plays a
  sound above 0.9 volume, and always fades in. The loudest thing in the mod is a magma trap.
* **Sanity cannot softlock.** No darkness is ever absolute: every corridor is lit by the palette,
  and safe rooms restore sanity passively. Sanity loss only ever makes entities find you sooner.
* **The labyrinth is navigable.** Every layer is generated from a braided maze (no unopenable
  dead ends), corridors are 4 blocks high and 5 wide, room doors are always cut, and the Lens
  records visited cells so a player can always retrace.
* **No flashing, no shake, no strobe.** Vignette and fog change over seconds, not frames.
* **Nothing is duplicated.** Altar trades consume their inputs in `onTakeItem` only when the
  output is taken; every recipe is acyclic (checked automatically).

## Repository layout

```
src/main/java/com/hollowlight/     99 sources
  world/labyrinth/                 layout, maze, palette, rooms, chunk generator
  story/                           lore catalogue, flags, player state, advancements
  sanity/, horror/, crafting/      systems
  entity/, entity/model, renderer/ nine mobs and their generated models
  gui/, client/                    journal, lens, altar, HUD, particles
src/main/resources/assets/         84 textures, 98 models, 66 item definitions, 50 ogg, lang
src/main/resources/data/           dimensions, biomes, recipes, loot, advancements, tags
tools/                             the whole asset pipeline (Python):
  artlib.py        procedural texture primitives (stone, brickwork, planks, crystal, glow)
  gen_textures.py  blocks, items, GUI panels, particles, icon
  gen_models.py    blockstates, block models, item models
  gen_entities.py  entity geometry spec → texture + model class + renderer
  synth.py         additive/subtractive synthesiser
  gen_sounds.py    50 ogg files + sounds.json
  gen_data.py      recipes, loot, advancements, worldgen, tags, lang
  gen_assets.py    1.21.4 item model definitions, particle defs, advancement background
  validate.py      structural validation (registry, assets, json, ids, lang)
  check_api.py     every MC/Fabric import against Yarn + fabric-api references
  check.py         runs both, writes VALIDATION.md
```

Everything visual and audible in the mod is generated by those scripts — there are no
third-party assets, no ripped textures, no placeholder squares.

## Testing

`VALIDATION.md` lists exactly what was machine-checked and what could not be (compilation and
in-game behaviour, because the toolchain is unavailable here). The 18 runtime categories the mod
is designed against — load, registry, textures, models, GUIs, recipes, loot, advancements, lore,
sanity, navigability, entities, boss phases, dimensions, structures, sound, save/load, config —
are each mapped to code in the sections above; the ones that can be verified statically are
verified, and the rest require the first build.

## Licence

CC BY-NC-SA 4.0 — see [LICENSE](LICENSE). You may share and adapt, not sell, keep the credit.

Code and assets by the Hollowlight authors. Built with Fabric Loom; Yarn mappings by the Fabric
community.
