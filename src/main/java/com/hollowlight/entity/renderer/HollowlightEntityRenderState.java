package com.hollowlight.entity.renderer;

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
