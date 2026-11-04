package com.hollowlight;

import com.hollowlight.entity.ModEntityModels;
import com.hollowlight.entity.renderer.AshMothRenderer;
import com.hollowlight.entity.renderer.DrownedChoirRenderer;
import com.hollowlight.entity.renderer.EchoRenderer;
import com.hollowlight.entity.renderer.HollowedRenderer;
import com.hollowlight.entity.renderer.LampkeeperRenderer;
import com.hollowlight.entity.renderer.PaperfaceRenderer;
import com.hollowlight.entity.renderer.StitcherRenderer;
import com.hollowlight.entity.renderer.WeepingOneRenderer;
import com.hollowlight.entity.renderer.WickmothRenderer;
import com.hollowlight.gui.ClientUiState;
import com.hollowlight.gui.EndingChoiceScreen;
import com.hollowlight.gui.JournalScreen;
import com.hollowlight.gui.LabyrinthMapScreen;
import com.hollowlight.gui.MemoryAltarScreen;
import com.hollowlight.gui.SanityHud;
import com.hollowlight.registry.ModEntities;
import com.hollowlight.registry.ModParticles;
import com.hollowlight.registry.ModPayloads;
import com.hollowlight.registry.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Client entry point: renderers, screens, particles, key bindings and the HUD.
 *
 * <p>The client never decides anything about the story. It receives the list of discovered
 * fragments, the set of visited map cells and the current sanity value from the server, and it
 * draws them. That separation is what keeps a modified client from inventing lore, and it also
 * keeps the screens simple enough to be tested by eye at every GUI scale.</p>
 */
public final class HollowlightClient implements ClientModInitializer {

    private static KeyBinding journalKey;
    private static KeyBinding mapKey;

    @Override
    public void onInitializeClient() {
        ModEntityModels.registerLayers();

        EntityRendererRegistry.register(ModEntities.HOLLOWED, HollowedRenderer::new);
        EntityRendererRegistry.register(ModEntities.WICKMOTH, WickmothRenderer::new);
        EntityRendererRegistry.register(ModEntities.PAPERFACE, PaperfaceRenderer::new);
        EntityRendererRegistry.register(ModEntities.STITCHER, StitcherRenderer::new);
        EntityRendererRegistry.register(ModEntities.DROWNED_CHOIR, DrownedChoirRenderer::new);
        EntityRendererRegistry.register(ModEntities.WEEPING_ONE, WeepingOneRenderer::new);
        EntityRendererRegistry.register(ModEntities.ECHO, EchoRenderer::new);
        EntityRendererRegistry.register(ModEntities.ASH_MOTH, AshMothRenderer::new);
        EntityRendererRegistry.register(ModEntities.LAMPKEEPER, LampkeeperRenderer::new);

        HandledScreens.register(ModScreenHandlers.MEMORY_ALTAR, MemoryAltarScreen::new);

        ParticleFactoryRegistry.getInstance().register(ModParticles.LAMP_DUST,
                com.hollowlight.client.HollowlightParticles.dustProvider());
        ParticleFactoryRegistry.getInstance().register(ModParticles.PALE_MOTE,
                com.hollowlight.client.HollowlightParticles.moteProvider());
        ParticleFactoryRegistry.getInstance().register(ModParticles.WHISPER_SMOKE,
                com.hollowlight.client.HollowlightParticles.smokeProvider());
        ParticleFactoryRegistry.getInstance().register(ModParticles.MEMORY_GLINT,
                com.hollowlight.client.HollowlightParticles.glintProvider());

        ClientPlayNetworking.registerGlobalReceiver(ModPayloads.JournalDataPayload.ID,
                (payload, context) -> ClientUiState.setDiscovered(payload.payload()));
        ClientPlayNetworking.registerGlobalReceiver(ModPayloads.MapDataPayload.ID,
                (payload, context) -> ClientUiState.setMap(payload.payload()));
        ClientPlayNetworking.registerGlobalReceiver(ModPayloads.SanityPayload.ID,
                (payload, context) -> ClientUiState.setSanity(payload.sanity(), payload.ambientTwist()));
        ClientPlayNetworking.registerGlobalReceiver(ModPayloads.OpenScreenPayload.ID,
                (payload, context) -> context.client().execute(() -> {
                    MinecraftClient client = context.client();
                    switch (payload.screen()) {
                        case "journal" -> client.setScreen(new JournalScreen());
                        case "map" -> client.setScreen(new LabyrinthMapScreen());
                        case "ending" -> client.setScreen(new EndingChoiceScreen());
                        default -> {
                            // Unknown screen ids are ignored rather than crashing a client.
                        }
                    }
                }));

        HudRenderCallback.EVENT.register((context, tickCounter) ->
                SanityHud.render(context, tickCounter.getTickDelta(true)));

        journalKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.hollowlight.journal",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "category.hollowlight"));
        mapKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.hollowlight.map",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M, "category.hollowlight"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }
            while (journalKey.wasPressed()) {
                client.setScreen(new JournalScreen());
            }
            while (mapKey.wasPressed()) {
                ClientPlayNetworking.send(new ModPayloads.RequestMapPayload("key"));
            }
        });

        Hollowlight.LOGGER.info("Hollowlight client ready. Nine presences registered.");
    }
}
