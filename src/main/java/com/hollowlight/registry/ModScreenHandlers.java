package com.hollowlight.registry;

import com.hollowlight.Hollowlight;
import com.hollowlight.gui.MemoryAltarScreenHandler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;

/** Screen handler types. Client-side screen registration lives in the client initialiser. */
public final class ModScreenHandlers {

    public static final ScreenHandlerType<MemoryAltarScreenHandler> MEMORY_ALTAR =
            new ScreenHandlerType<>(MemoryAltarScreenHandler::new, FeatureFlags.VANILLA_FEATURES);

    private ModScreenHandlers() {
    }

    public static void bootstrap() {
        Registry.register(Registries.SCREEN_HANDLER, Hollowlight.id("memory_altar"), MEMORY_ALTAR);
    }
}
