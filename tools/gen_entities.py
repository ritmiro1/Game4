"""Entity textures, entity model classes, renderers and model-layer registration.

One spec per entity drives three artefacts so the UV layout of the texture can never drift away
from the cuboids in the Java model: for every box, the texture sheet region is computed with the
same layout Minecraft uses, painted here, and emitted as the `uv(...)` call there.
"""
from __future__ import annotations

import json
import math
import os
import random

from PIL import Image, ImageDraw

from artlib import mix, shade

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                    "assets", "hollowlight", "textures", "entity")
JAVA = os.path.join(os.path.dirname(__file__), "..", "src", "main", "java", "com", "hollowlight",
                    "entity")
MODEL_DIR = os.path.join(JAVA, "model")
RENDERER_DIR = os.path.join(JAVA, "renderer")
for d in (ROOT, MODEL_DIR, RENDERER_DIR):
    os.makedirs(d, exist_ok=True)


class Box:
    """A cuboid: position relative to its part's pivot, size, and its texture region."""

    def __init__(self, dx, dy, dz, w, h, d, base, style="plain", accent=None):
        self.dx, self.dy, self.dz = dx, dy, dz
        self.w, self.h, self.d = w, h, d
        self.base = base
        self.style = style
        self.accent = accent
        self.u = self.v = 0
        # Integer dimensions used for the texture sheet: a 2.5-block-wide box still needs a
        # whole number of texels per face.
        self.iw = max(1, int(round(w)))
        self.ih = max(1, int(round(h)))
        self.id_ = max(1, int(round(d)))

    @property
    def region_width(self):
        return 2 * (self.iw + self.id_)

    @property
    def region_height(self):
        return self.ih + self.id_

    def allocate(self, packer):
        self.u, self.v = packer.place(self.region_width, self.region_height)


class Part:
    def __init__(self, name, parent, pivot, box, rotation=None, about=None):
        self.name = name
        self.parent = parent
        self.pivot = pivot
        self.box = box
        self.rotation = rotation or (0.0, 0.0, 0.0)
        self.about = about  # optional ModelTransform pivot override


class SheetPacker:
    """Deterministic shelf packer for UV regions."""

    def __init__(self, width, height):
        self.width = width
        self.height = height
        self.x = 0
        self.y = 0
        self.row_height = 0

    def place(self, w, h):
        if self.x + w > self.width:
            self.x = 0
            self.y += self.row_height + 1
            self.row_height = 0
        if self.y + h > self.height:
            raise RuntimeError("entity texture sheet overflow")
        position = (self.x, self.y)
        self.x += w + 1
        self.row_height = max(self.row_height, h)
        return position


# ------------------------------------------------------------------- texture painting ----

def paint_box(img, box, rng):
    """Paint the six faces of one cuboid in Minecraft's box-UV layout."""
    d = ImageDraw.Draw(img)
    u, v, w, h, dep = box.u, box.v, box.iw, box.ih, box.id_

    def face(x, y, fw, fh, tone, style=None):
        for yy in range(y, y + fh):
            t = (yy - y) / max(1, fh - 1)
            colour = mix(shade(box.base, tone + 10), shade(box.base, tone - 12), t)
            for xx in range(x, x + fw):
                n = rng.randint(-9, 9)
                img.putpixel((xx, yy), (*shade(colour, n), 255))
        if style:
            style(x, y, fw, fh)

    # west, north, east, south, top, bottom
    face(u, v + dep, dep, h, -14)
    face(u + dep, v + dep, w, h, 0, lambda x, y, fw, fh: front_detail(img, box, x, y, fw, fh, rng))
    face(u + dep + w, v + dep, dep, h, 8)
    face(u + dep + w + dep, v + dep, w, h, -6)
    face(u + dep + w, v, w, dep, 16)
    face(u + dep, v, w, dep, -26)


def front_detail(img, box, x, y, fw, fh, rng):
    """Per-style face detailing: what makes each creature read at a glance."""
    d = ImageDraw.Draw(img)
    style = box.style
    if style == "head_eyes":
        eye_colour = box.accent or (222, 226, 168)
        ey = y + max(1, fh // 3)
        for ex in (x + max(1, fw // 4), x + fw - max(2, fw // 4) - 1):
            d.rectangle([ex, ey, ex + 1, ey + 1], fill=(*eye_colour, 255))
        mouth_y = y + int(fh * 0.72)
        d.line([(x + 1, mouth_y), (x + fw - 2, mouth_y)], fill=(*shade(box.base, -55), 255))
    elif style == "head_void":
        ey = y + max(1, fh // 3)
        d.rectangle([x + max(1, fw // 4), ey, x + fw - max(2, fw // 4), ey + max(1, fh // 5)],
                    fill=(12, 10, 12, 255))
    elif style == "head_pages":
        for row in range(0, fh, 2):
            d.line([(x, y + row), (x + fw - 1, y + row)], fill=(*shade(box.base, -34), 255))
        d.line([(x + fw // 2, y), (x + fw // 2, y + fh - 1)], fill=(*shade(box.base, -44), 255))
    elif style == "head_lantern":
        cx, cy = x + fw // 2, y + fh // 2
        for radius in range(min(fw, fh) // 2, 0, -1):
            t = 1.0 - radius / (min(fw, fh) / 2.0)
            d.ellipse([cx - radius, cy - radius, cx + radius, cy + radius],
                      fill=(*mix(shade(box.base, -10), box.accent or (255, 226, 160), t), 255))
    elif style == "ribs":
        for row in range(1, fh, 3):
            d.line([(x, y + row), (x + fw - 1, y + row)], fill=(*shade(box.base, 26), 255))
    elif style == "stitched":
        for row in range(1, fh, 4):
            d.line([(x + 1, y + row), (x + fw - 2, y + row)], fill=(*shade(box.base, -40), 255))
        for col in range(0, fw, 3):
            d.point((x + col, y + fh // 2), fill=(*shade(box.base, 40), 255))
    elif style == "glow_hole":
        cx, cy = x + fw // 2, y + fh // 2
        d.ellipse([cx - 1, cy - 1, cx + 1, cy + 1], fill=(*mix(box.base, box.accent or (255, 222, 150), 0.95), 255))
    elif style == "wet":
        for _ in range(4):
            px = rng.randrange(x, x + max(1, fw))
            py = rng.randrange(y, y + max(1, fh))
            img.putpixel((px, py), (*shade(box.base, 34), 255))
    elif style == "skirt_lamps":
        for col in range(0, fw, 2):
            for row in range(0, fh, 3):
                img.putpixel((x + col, y + row), (*mix(box.base, box.accent or (255, 210, 120), 0.8), 255))


# ------------------------------------------------------------------------- entity specs --

def build_specs():
    specs = {}

    # ---- the hollowed: a hunched miner with a lamp where his heart was -----------------
    specs["hollowed"] = dict(size=64, model_height=1.9, parts=[
        Part("body", None, (0, 24, 0), Box(-3, -12, -2, 6, 12, 4, (86, 78, 68), "glow_hole",
                                           (250, 206, 128))),
        Part("head", "body", (0, -12, 0), Box(-4, -8, -4, 8, 8, 8, (78, 72, 64), "head_void")),
        Part("arm_left", "body", (3, -11, 0), Box(0, 0, -1, 2, 13, 2, (74, 68, 60))),
        Part("arm_right", "body", (-3, -11, 0), Box(-2, 0, -1, 2, 13, 2, (74, 68, 60))),
        Part("leg_left", None, (1.5, 24, 0), Box(-1, -12, -1, 2, 12, 2, (62, 58, 52))),
        Part("leg_right", None, (-1.5, 24, 0), Box(-1, -12, -1, 2, 12, 2, (62, 58, 52))),
    ], swing=["leg_left", "leg_right", "arm_left", "arm_right"], head="head")

    # ---- the wickmoth -------------------------------------------------------------------
    specs["wickmoth"] = dict(size=32, model_height=0.6, parts=[
        Part("body", None, (0, 20, 0), Box(-1, -2, -1, 2, 2, 3, (58, 50, 44), "glow_hole",
                                           (255, 214, 128))),
        Part("wing_left", "body", (1, -2, 0), Box(0, 0, -2, 4, 1, 4, (128, 122, 112), "wet")),
        Part("wing_right", "body", (-1, -2, 0), Box(-4, 0, -2, 4, 1, 4, (128, 122, 112), "wet")),
        Part("antenna", "body", (0, -2, 0), Box(-1, -3, -1, 2, 3, 1, (96, 88, 78))),
    ], swing=[], head=None)

    # ---- the paperface ------------------------------------------------------------------
    specs["paperface"] = dict(size=64, model_height=2.2, parts=[
        Part("body", None, (0, 24, 0), Box(-3, -14, -2, 6, 14, 4, (108, 96, 80), "stitched")),
        Part("head", "body", (0, -14, 0), Box(-4, -9, -4, 8, 9, 8, (196, 186, 160), "head_pages")),
        Part("arm_left", "body", (3, -13, 0), Box(0, 0, -1, 2, 18, 2, (150, 140, 118))),
        Part("arm_right", "body", (-3, -13, 0), Box(-2, 0, -1, 2, 18, 2, (150, 140, 118))),
        Part("leg_left", None, (1.5, 24, 0), Box(-1, -10, -1, 2, 10, 2, (88, 80, 66))),
        Part("leg_right", None, (-1.5, 24, 0), Box(-1, -10, -1, 2, 10, 2, (88, 80, 66))),
    ], swing=["leg_left", "leg_right", "arm_left", "arm_right"], head="head")

    # ---- the stitcher -------------------------------------------------------------------
    specs["stitcher"] = dict(size=128, model_height=3.2, parts=[
        Part("body", None, (0, 24, 0), Box(-4, -18, -2.5, 8, 18, 5, (86, 40, 42), "ribs")),
        Part("head", "body", (0, -18, 0), Box(-3, -7, -3, 6, 7, 6, (96, 46, 48), "head_eyes",
                                              (232, 196, 138))),
        Part("arm_upper_left", "body", (4, -16, 0), Box(0, 0, -1, 2, 16, 2, (78, 36, 38))),
        Part("arm_upper_right", "body", (-4, -16, 0), Box(-2, 0, -1, 2, 16, 2, (78, 36, 38))),
        Part("arm_lower_left", "body", (4, -9, 0), Box(0, 0, -1, 2, 14, 2, (70, 32, 34))),
        Part("arm_lower_right", "body", (-4, -9, 0), Box(-2, 0, -1, 2, 14, 2, (70, 32, 34))),
        Part("leg_left", None, (2, 24, 0), Box(-1.5, -6, -1.5, 3, 6, 3, (66, 30, 32))),
        Part("leg_right", None, (-2, 24, 0), Box(-1.5, -6, -1.5, 3, 6, 3, (66, 30, 32))),
    ], swing=["leg_left", "leg_right", "arm_upper_left", "arm_upper_right"], head="head")

    # ---- the drowned choir ---------------------------------------------------------------
    specs["drowned_choir"] = dict(size=64, model_height=1.8, parts=[
        Part("body", None, (0, 24, 0), Box(-3, -12, -2, 6, 12, 4, (58, 82, 84), "wet")),
        Part("head", "body", (0, -12, 0), Box(-4, -8, -3, 8, 8, 7, (66, 92, 94), "head_void")),
        Part("jaw", "head", (0, -1, 0), Box(-3, 0, -3, 6, 3, 6, (48, 70, 72), "ribs")),
        Part("arm_left", "body", (3, -11, 0), Box(0, 0, -1, 2, 12, 2, (52, 76, 78))),
        Part("arm_right", "body", (-3, -11, 0), Box(-2, 0, -1, 2, 12, 2, (52, 76, 78))),
        Part("tail", None, (0, 24, 0), Box(-2, 0, -1, 4, 8, 2, (44, 66, 68), "wet")),
    ], swing=["arm_left", "arm_right"], head="head")

    # ---- the sorrow ---------------------------------------------------------------------
    specs["weeping_one"] = dict(size=64, model_height=1.5, parts=[
        Part("body", None, (0, 20, 0), Box(-3, -10, -2, 6, 10, 4, (108, 104, 100), "stitched")),
        Part("head", "body", (0, -10, 0), Box(-3.5, -7, -3.5, 7, 7, 7, (122, 116, 110), "head_void")),
        Part("hair", "head", (0, -7, 0), Box(-4, 0, -4, 8, 9, 1, (60, 58, 60))),
        Part("arm_left", "body", (3, -9, 0), Box(0, 0, -1, 2, 11, 2, (112, 106, 100))),
        Part("arm_right", "body", (-3, -9, 0), Box(-2, 0, -1, 2, 11, 2, (112, 106, 100))),
        Part("knee", None, (0, 20, 0), Box(-3, 0, -2, 6, 3, 4, (96, 92, 88))),
    ], swing=["arm_left", "arm_right"], head="head")

    # ---- the echo ------------------------------------------------------------------------
    specs["echo"] = dict(size=64, model_height=1.8, parts=[
        Part("body", None, (0, 24, 0), Box(-3, -18, -2, 6, 18, 4, (176, 184, 190), "wet")),
        Part("head", "body", (0, -18, 0), Box(-4, -8, -4, 8, 8, 8, (196, 202, 208), "head_void")),
        Part("arm_left", "body", (3, -17, 0), Box(0, 0, -1, 2, 15, 2, (168, 176, 182))),
        Part("arm_right", "body", (-3, -17, 0), Box(-2, 0, -1, 2, 15, 2, (168, 176, 182))),
    ], swing=["arm_left", "arm_right"], head="head")

    # ---- the ash moth ---------------------------------------------------------------------
    specs["ash_moth"] = dict(size=32, model_height=0.3, parts=[
        Part("body", None, (0, 22, 0), Box(-1, -1, -1, 2, 1, 2, (108, 100, 92))),
        Part("wing_left", "body", (1, -1, 0), Box(0, 0, -1, 3, 1, 3, (146, 138, 128))),
        Part("wing_right", "body", (-1, -1, 0), Box(-3, 0, -1, 3, 1, 3, (146, 138, 128))),
    ], swing=[], head=None)

    # ---- the lamplighter -------------------------------------------------------------------
    specs["lampkeeper"] = dict(size=128, model_height=4.2, parts=[
        Part("body", None, (0, 24, 0), Box(-5, -22, -3, 10, 22, 6, (74, 62, 58), "skirt_lamps",
                                           (255, 214, 132))),
        Part("head", "body", (0, -22, 0), Box(-5, -10, -5, 10, 10, 10, (88, 74, 66),
                                              "head_lantern", (255, 232, 170))),
        Part("arm_left", "body", (5, -20, 0), Box(0, 0, -1.5, 2.5, 20, 3, (70, 58, 54))),
        Part("arm_right", "body", (-5, -20, 0), Box(-2.5, 0, -1.5, 2.5, 20, 3, (70, 58, 54))),
        Part("arm_mid_left", "body", (4, -14, 1), Box(0, 0, -1, 2, 16, 2, (64, 54, 50))),
        Part("arm_mid_right", "body", (-4, -14, 1), Box(-2, 0, -1, 2, 16, 2, (64, 54, 50))),
        Part("skirt", None, (0, 24, 0), Box(-6, -10, -4, 12, 10, 8, (58, 48, 46), "skirt_lamps",
                                            (250, 196, 118))),
        Part("lamp", "body", (0, -30, 0), Box(-2, 0, -2, 4, 4, 4, (120, 96, 60), "head_lantern",
                                               (255, 240, 190))),
    ], swing=["arm_left", "arm_right", "arm_mid_left", "arm_mid_right"], head="head")

    return specs


# ------------------------------------------------------------------------ java emitting --

def java_float(value):
    if float(value).is_integer():
        return f"{int(value)}.0F"
    return f"{value:.1f}F"


def part_chain(part, parts):
    """Names from the root down to (and including) this part, so the constructor can walk them."""
    by_name = {p.name: p for p in parts}
    chain = [part.name]
    cursor = part
    guard = 0
    while cursor.parent is not None:
        chain.append(cursor.parent)
        cursor = by_name[cursor.parent]
        guard += 1
        if guard > 32:
            raise SystemExit("model part cycle at " + part.name)
    return list(reversed(chain))


def emit_model(name, spec):
    class_name = "".join(part.capitalize() for part in name.split("_")) + "EntityModel"
    entity_class = "".join(part.capitalize() for part in name.split("_")) + "Entity"
    parts = spec["parts"]
    top_parts = [p for p in parts if p.parent is None]

    lines = []
    lines.append("package com.hollowlight.entity.model;")
    lines.append("")
    lines.append("import com.hollowlight.entity.renderer.HollowlightEntityRenderState;")
    lines.append("import net.minecraft.client.model.ModelData;")
    lines.append("import net.minecraft.client.model.ModelPart;")
    lines.append("import net.minecraft.client.model.ModelPartBuilder;")
    lines.append("import net.minecraft.client.model.ModelPartData;")
    lines.append("import net.minecraft.client.model.ModelTransform;")
    lines.append("import net.minecraft.client.model.TexturedModelData;")
    lines.append("import net.minecraft.client.render.entity.model.EntityModel;")
    lines.append("import net.minecraft.client.render.VertexConsumer;")
    lines.append("import net.minecraft.client.util.math.MatrixStack;")
    lines.append("import net.minecraft.util.math.MathHelper;")
    lines.append("")
    lines.append("/**")
    lines.append(" * Model for {@link " + entity_class + "}.")
    lines.append(" *")
    lines.append(" * <p>Generated together with its texture (texture size " + str(spec["size"]) + "x"
                 + str(spec["size"]) + ") from a single geometry spec, so the UV")
    lines.append(" * layout of the sheet and the cuboids here are guaranteed to agree.</p>")
    lines.append(" */")
    lines.append("public class " + class_name + " extends EntityModel<HollowlightEntityRenderState> {")
    lines.append("")
    for part in parts:
        lines.append("    private final ModelPart " + part.name + ";")
    lines.append("")
    lines.append("    public " + class_name + "(ModelPart root) {")
    lines.append("        super(root);")
    for part in parts:
        chain = part_chain(part, parts)
        accessor = "root"
        for link in chain:
            accessor += '.getChild("' + link + '")'
        lines.append("        this." + part.name + " = " + accessor + ";")
    lines.append("    }")
    lines.append("")
    lines.append("    public static TexturedModelData getTexturedModelData() {")
    lines.append("        ModelData modelData = new ModelData();")
    lines.append("        ModelPartData root = modelData.getRoot();")
    def emit_part(part, accessor):
        lines.append("        ModelPartData " + part.name + " = " + accessor + ".addChild(\""
                     + part.name + "\",")
        lines.append("                " + cuboid(part) + ",")
        lines.append("                " + transform(part) + ");")
        for child in [p for p in parts if p.parent == part.name]:
            emit_part(child, part.name)

    for part in top_parts:
        emit_part(part, "root")
    lines.append("        return TexturedModelData.of(modelData, " + str(spec["size"]) + ", "
                 + str(spec["size"]) + ");")
    lines.append("    }")
    lines.append("")
    lines.append("    @Override")
    lines.append("    public void setAngles(HollowlightEntityRenderState state) {")
    lines.append("        float limbAngle = state.limbFrequency;")
    lines.append("        float limbDistance = state.limbAmplitudeMultiplier;")
    lines.append("        float animationProgress = state.age;")
    lines.append("        float headYaw = state.yawDegrees;")
    lines.append("        float headPitch = state.pitch;")
    swing = spec["swing"]
    if swing:
        for index, part_name in enumerate(swing):
            sign = 1.0 if index % 2 == 0 else -1.0
            axis = "pitch" if "arm" in part_name or "leg" in part_name else "pitch"
            field = "this." + part_name
            if "arm" in part_name:
                lines.append("        " + field + ".pitch = MathHelper.cos(limbAngle * 0.6662F + "
                             + ("0.0F" if sign > 0 else "(float) Math.PI") + ") * 0.7F * limbDistance;")
            else:
                lines.append("        " + field + ".pitch = MathHelper.cos(limbAngle * 0.6662F + "
                             + ("0.0F" if sign > 0 else "(float) Math.PI") + ") * 1.1F * limbDistance;")
    head = spec["head"]
    if head:
        lines.append("        this." + head + ".yaw = headYaw * 0.017453292F;")
        lines.append("        this." + head + ".pitch = headPitch * 0.017453292F;")
    # idle breathing / hover
    if name in ("wickmoth", "ash_moth"):
        lines.append("        this.body.pitch = MathHelper.cos(animationProgress * 0.2F) * 0.15F;")
    if name == "weeping_one":
        lines.append("        this.body.pitch = 0.5F;")
        lines.append("        this.head.pitch = 0.9F;")
    if name == "lampkeeper":
        lines.append("        this.body.pitch = MathHelper.cos(animationProgress * 0.05F) * 0.05F;")
    lines.append("    }")
    lines.append("")
    lines.append("}")

    path = os.path.join(MODEL_DIR, class_name + ".java")
    with open(path, "w", encoding="utf-8") as handle:
        handle.write("\n".join(lines) + "\n")
    return class_name


def cuboid(part):
    b = part.box
    return ("ModelPartBuilder.create().uv(" + str(b.u) + ", " + str(b.v) + ").cuboid("
            + ", ".join(java_float(v) for v in (b.dx, b.dy, b.dz, b.w, b.h, b.d)) + ")")


def transform(part):
    x, y, z = part.pivot
    rx, ry, rz = part.rotation
    if rx or ry or rz:
        return ("ModelTransform.of(" + ", ".join(java_float(v) for v in (x, y, z))
                + ", " + ", ".join(java_float(v) for v in (rx, ry, rz)) + ")")
    return "ModelTransform.pivot(" + ", ".join(java_float(v) for v in (x, y, z)) + ")"


RENDER_STATE = """package com.hollowlight.entity.renderer;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;

/**
 * One render state shared by the nine residents.
 *
 * <p>Minecraft 1.21.4 renders from immutable per-frame state objects rather than from live
 * entities, so anything an animation needs has to be copied into a state object first. The mod
 * needs very little: a few per-entity extras that the generated models read.</p>
 */
public class HollowlightEntityRenderState extends LivingEntityRenderState {

    /** 0..1, how lit this entity currently is (used to bob the Lamplighter's lantern). */
    public float glow;

    /** Current story phase for the boss; 0 for everything else. */
    public int phase;

    /** Distance to the camera, in blocks; used to thin out ambient mobs that are too close. */
}
"""


def emit_render_state():
    directory = os.path.join(MODEL_DIR, os.pardir, "renderer")
    directory = os.path.abspath(directory)
    path = os.path.join(directory, "HollowlightEntityRenderState.java")
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(RENDER_STATE)
    return "HollowlightEntityRenderState"


def emit_renderer(name, spec):
    class_name = "".join(part.capitalize() for part in name.split("_"))
    entity_class = class_name + "Entity"
    model_class = class_name + "EntityModel"
    shadow = round(min(0.9, spec["model_height"] * 0.22), 2)
    source = f"""package com.hollowlight.entity.renderer;

import com.hollowlight.entity.ModEntityModels;
import com.hollowlight.entity.{entity_class};
import com.hollowlight.entity.model.{model_class};
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

/** Renderer for {{@link {entity_class}}}. */
public class {class_name}Renderer
        extends MobEntityRenderer<{entity_class}, HollowlightEntityRenderState, {model_class}> {{

    private static final Identifier TEXTURE = Identifier.of("hollowlight", "textures/entity/{name}.png");

    public {class_name}Renderer(EntityRendererFactory.Context context) {{
        super(context, new {model_class}(context.getPart(ModEntityModels.{name.upper()})), {java_float(shadow)});
    }}

    @Override
    public HollowlightEntityRenderState createRenderState() {{
        return new HollowlightEntityRenderState();
    }}

    @Override
    public void updateRenderState({entity_class} entity, HollowlightEntityRenderState state, float tickDelta) {{
        super.updateRenderState(entity, state, tickDelta);
    }}

    @Override
    public Identifier getTexture(HollowlightEntityRenderState state) {{
        return TEXTURE;
    }}
}}
"""
    path = os.path.join(RENDERER_DIR, class_name + "Renderer.java")
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(source)
    return class_name + "Renderer"


def emit_model_registry(names):
    lines = ["package com.hollowlight.entity;", "",
             "import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;",
             "import net.minecraft.client.render.entity.model.EntityModelLayer;",
             "import net.minecraft.util.Identifier;",
             "import com.hollowlight.entity.model.AshMothEntityModel;",
             "import com.hollowlight.entity.model.DrownedChoirEntityModel;",
             "import com.hollowlight.entity.model.EchoEntityModel;",
             "import com.hollowlight.entity.model.HollowedEntityModel;",
             "import com.hollowlight.entity.model.LampkeeperEntityModel;",
             "import com.hollowlight.entity.model.PaperfaceEntityModel;",
             "import com.hollowlight.entity.model.StitcherEntityModel;",
             "import com.hollowlight.entity.model.WeepingOneEntityModel;",
             "import com.hollowlight.entity.model.WickmothEntityModel;",
             "", "/** Model layer locations for the nine residents. */",
             "public final class ModEntityModels {", ""]
    for name in names:
        lines.append(f'    public static final EntityModelLayer {name.upper()} = register("{name}");')
    lines += ["", "    private ModEntityModels() {", "    }", "",
              "    private static EntityModelLayer register(String name) {",
              '        return new EntityModelLayer(Identifier.of("hollowlight", name), "main");',
              "    }", "", "    public static void registerLayers() {"]
    for name in names:
        model_class = "".join(part.capitalize() for part in name.split("_")) + "EntityModel"
        lines.append(f"        EntityModelLayerRegistry.registerModelLayer({name.upper()}, "
                     f"{model_class}::getTexturedModelData);")
    lines += ["    }", "}", ""]
    path = os.path.join(JAVA, "ModEntityModels.java")
    with open(path, "w", encoding="utf-8") as handle:
        handle.write("\n".join(lines))


def main():
    specs = build_specs()
    names = list(specs.keys())
    for name, spec in specs.items():
        size = spec["size"]
        packer = SheetPacker(size, size)
        image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        rng = random.Random(hash(name) & 0x7FFFFFFF)
        for part in spec["parts"]:
            part.box.allocate(packer)
        for part in spec["parts"]:
            paint_box(image, part.box, rng)
        image.save(os.path.join(ROOT, name + ".png"))
        emit_model(name, spec)
        emit_renderer(name, spec)
    emit_render_state()
    emit_model_registry(names)
    print("entities:", ", ".join(names))


if __name__ == "__main__":
    main()
