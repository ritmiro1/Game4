"""Introspect the remapped Minecraft jar and print the exact signatures the mod needs.

Runs on a machine that has the Loom cache (i.e. the CI runner), because this sandbox cannot
download Minecraft at all. Output is deliberately compact: only the members we call.
"""
from __future__ import annotations

import glob
import os
import re
import subprocess
import sys

TARGETS = {
    "net.minecraft.client.render.entity.LivingEntityRenderer": r"class |<init>|createRenderState|updateRenderState|getTexture|render",
    "net.minecraft.client.render.entity.MobEntityRenderer": r"class |<init>|createRenderState|updateRenderState|getTexture|getShadow",
    "net.minecraft.client.render.entity.EntityRenderer": r"class |abstract |createRenderState|updateRenderState|getTexture|shouldRender",
    "net.minecraft.client.render.entity.EntityRendererFactory$Context": r"getPart|getModelManager|getTextRenderer",
    "net.minecraft.client.render.entity.model.EntityModel": r"class |<init>|setAngles|render",
    "net.minecraft.client.render.entity.model.Model": r"class |render|getRootPart|getPart",
    "net.minecraft.client.render.entity.state.EntityRenderState": r"class |public ",
    "net.minecraft.client.render.entity.state.LivingEntityRenderState": r"class |public ",
    "net.minecraft.client.render.entity.state.MobEntityRenderState": r"class |public ",
    "net.minecraft.entity.attribute.EntityAttributes": r"public static final .*(HEALTH|SPEED|DAMAGE|FOLLOW|KNOCKBACK|ARMOR|STEP)",
    "net.minecraft.client.gui.DrawContext": r"drawTexture|drawText\(|fillGradient|fill\(",
    "net.minecraft.world.World": r"playSound",
    "net.minecraft.entity.LivingEntity": r"damage\(|canBreatheInWater|pushAway|setHealth",
    "net.minecraft.entity.mob.MobEntity": r"canSpawn|tryAttack|attack\(|getTarget",
    "net.minecraft.entity.Entity": r"teleport|lookAt|damage\(",
    "net.minecraft.command.argument.EntityAnchorArgumentType": r"class |public ",
    "net.minecraft.world.TeleportTarget": r"<init>|public ",
    "net.minecraft.entity.EntityType$Builder": r"build|dimensions|maxTrackingRange|makeFireImmune",
    "net.minecraft.screen.ScreenHandler": r"quickMove|canUse|onClosed|addSlot|onButtonClick|sendContentUpdates",
    "net.minecraft.entity.boss.ServerBossBar": r"class |<init>|setPercent|setName|setDarkenSky|setColor|addPlayer|removePlayer",
    "net.minecraft.entity.boss.BossBar$Color": r"class |public static final",
    "net.minecraft.entity.ai.goal.Goal": r"shouldRun|tick|canStart|setControls|start|stop",
    "net.minecraft.entity.mob.HostileEntity": r"createHostileAttributes|class ",
    "net.minecraft.entity.passive.PassiveEntity": r"createMobAttributes|class ",
    "net.minecraft.entity.PathAwareEntity": r"createMobAttributes|class ",
    "net.minecraft.entity.ai.pathing.PathNodeType": r"class ",
    "net.minecraft.item.Item$Settings": r"maxCount|maxDamage|rarity|attributeModifiers|food|component",
    "net.minecraft.component.type.AttributeModifiersComponent$Builder": r"add\(",
    "net.minecraft.block.AbstractBlock$Settings": r"luminance|strength|sounds|requiresTool|nonOpaque|noCollision|ticksRandomly|dropsNothing|mapColor",
}


def find_jar():
    """Loom keeps the remapped jar under minecraftMaven; only the named one has Yarn classes."""
    candidates = []
    for root in (os.path.expanduser("~/.gradle/caches/fabric-loom"),
                 os.path.expanduser("~/.gradle/caches/modules-2"),
                 ".gradle/loom-cache", os.path.expanduser("~/.gradle/caches/fabric-loom/1.21.4")):
        for path in glob.glob(os.path.join(root, "**", "*.jar"), recursive=True):
            name = os.path.basename(path).lower()
            if "minecraft" not in name or "source" in name or "intermediary" in name:
                continue
            if "merged" not in name:
                continue
            candidates.append(path)
    for path in sorted(candidates, key=len, reverse=True):
        probe = subprocess.run(["javap", "-cp", path, "net.minecraft.client.render.entity.LivingEntityRenderer"],
                               capture_output=True, text=True)
        if probe.returncode == 0:
            return path
    return None


def javap(jar, cls):
    try:
        result = subprocess.run(["javap", "-p", "-s", "-cp", jar, cls],
                                capture_output=True, text=True, timeout=180)
    except Exception as error:  # noqa: BLE001
        return [f"!! {cls}: {error}"]
    if result.returncode != 0:
        return [f"!! {cls}: not found"]
    return result.stdout.splitlines()


def main():
    jar = sys.argv[1] if len(sys.argv) > 1 else find_jar()
    if not jar or not os.path.isfile(jar):
        print("!! no remapped Minecraft jar found")
        found = []
        for root in (os.path.expanduser("~/.gradle/caches"), ".gradle"):
            for path in glob.glob(os.path.join(root, "**", "*.jar"), recursive=True):
                if "minecraft" in os.path.basename(path).lower():
                    found.append(path)
        print("\n".join(found[:20]) or "no minecraft jars at all")
        return 1
    print(f"## jar: {os.path.basename(jar)}\n")
    pending_descriptor = {}
    for cls, pattern in TARGETS.items():
        lines = javap(jar, cls)
        keep = []
        for index, line in enumerate(lines):
            pass
        for index, line in enumerate(lines):
            if re.search(pattern, line):
                keep.append(line.rstrip())
                if index + 1 < len(lines) and "descriptor" in lines[index + 1]:
                    keep.append("    " + lines[index + 1].strip())
        print(f"### {cls}")
        print("\n".join(keep[:60]) if keep else "(nothing matched)")
        print()
    return 0


if __name__ == "__main__":
    sys.exit(main())
