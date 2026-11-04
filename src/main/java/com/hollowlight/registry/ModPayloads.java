package com.hollowlight.registry;

import com.hollowlight.Hollowlight;
import com.hollowlight.sanity.SanityManager;
import com.hollowlight.story.LoreCatalog;
import com.hollowlight.story.StoryManager;
import com.hollowlight.world.labyrinth.LabyrinthMapData;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Networking for the custom screens. Everything is a single string or float payload — the
 * journal and the lens map are display-only views built on the client from data the server
 * has already decided, so there is no way for a client to invent lore or map cells.
 */
public final class ModPayloads {

    // ---------------------------------------------------------------- server → client ----

    public record JournalDataPayload(String payload) implements CustomPayload {
        public static final CustomPayload.Id<JournalDataPayload> ID =
                new CustomPayload.Id<>(Hollowlight.id("journal_data"));
        public static final PacketCodec<RegistryByteBuf, JournalDataPayload> CODEC =
                PacketCodec.tuple(PacketCodecs.STRING, JournalDataPayload::payload, JournalDataPayload::new);

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record MapDataPayload(String payload) implements CustomPayload {
        public static final CustomPayload.Id<MapDataPayload> ID =
                new CustomPayload.Id<>(Hollowlight.id("map_data"));
        public static final PacketCodec<RegistryByteBuf, MapDataPayload> CODEC =
                PacketCodec.tuple(PacketCodecs.STRING, MapDataPayload::payload, MapDataPayload::new);

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record OpenScreenPayload(String screen) implements CustomPayload {
        public static final CustomPayload.Id<OpenScreenPayload> ID =
                new CustomPayload.Id<>(Hollowlight.id("open_screen"));
        public static final PacketCodec<RegistryByteBuf, OpenScreenPayload> CODEC =
                PacketCodec.tuple(PacketCodecs.STRING, OpenScreenPayload::screen, OpenScreenPayload::new);

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SanityPayload(float sanity, float ambientTwist) implements CustomPayload {
        public static final CustomPayload.Id<SanityPayload> ID =
                new CustomPayload.Id<>(Hollowlight.id("sanity"));
        public static final PacketCodec<RegistryByteBuf, SanityPayload> CODEC =
                PacketCodec.tuple(PacketCodecs.FLOAT, SanityPayload::sanity,
                        PacketCodecs.FLOAT, SanityPayload::ambientTwist,
                        SanityPayload::new);

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    // ---------------------------------------------------------------- client → server ----

    public record RequestMapPayload(String reason) implements CustomPayload {
        public static final CustomPayload.Id<RequestMapPayload> ID =
                new CustomPayload.Id<>(Hollowlight.id("request_map"));
        public static final PacketCodec<RegistryByteBuf, RequestMapPayload> CODEC =
                PacketCodec.tuple(PacketCodecs.STRING, RequestMapPayload::reason, RequestMapPayload::new);

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record EndingChoicePayload(String choice) implements CustomPayload {
        public static final CustomPayload.Id<EndingChoicePayload> ID =
                new CustomPayload.Id<>(Hollowlight.id("ending_choice"));
        public static final PacketCodec<RegistryByteBuf, EndingChoicePayload> CODEC =
                PacketCodec.tuple(PacketCodecs.STRING, EndingChoicePayload::choice, EndingChoicePayload::new);

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    private ModPayloads() {
    }

    /** Registers the codecs and the serverbound handlers. Called from the initialiser. */
    public static void registerServerbound() {
        PayloadTypeRegistry.playS2C().register(JournalDataPayload.ID, JournalDataPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MapDataPayload.ID, MapDataPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenScreenPayload.ID, OpenScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SanityPayload.ID, SanityPayload.CODEC);

        PayloadTypeRegistry.playC2S().register(RequestMapPayload.ID, RequestMapPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(EndingChoicePayload.ID, EndingChoicePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(RequestMapPayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    if (context.player() != null) {
                        sendMap(context.player());
                    }
                }));

        ServerPlayNetworking.registerGlobalReceiver(EndingChoicePayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    if (context.player() != null) {
                        com.hollowlight.world.ending.EndingSequence.choose(context.player(), payload.choice());
                    }
                }));
    }

    /** Sends the journal contents and asks the client to open the journal screen. */
    public static void openJournal(ServerPlayerEntity player) {
        syncJournal(player);
        ServerPlayNetworking.send(player, new OpenScreenPayload("journal"));
    }

    public static void openLens(ServerPlayerEntity player) {
        sendMap(player);
        ServerPlayNetworking.send(player, new OpenScreenPayload("map"));
    }

    public static void openEndingChoice(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new OpenScreenPayload("ending"));
    }

    public static void syncJournal(ServerPlayerEntity player) {
        StringBuilder builder = new StringBuilder();
        for (String id : StoryManager.journalLines(player)) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(id);
        }
        builder.append("\n#total=").append(LoreCatalog.size());
        ServerPlayNetworking.send(player, new JournalDataPayload(builder.toString()));
    }

    public static void sendMap(ServerPlayerEntity player) {
        StringBuilder builder = new StringBuilder();
        builder.append("layer=").append(LabyrinthMapData.currentLayer(player)).append(';');
        builder.append("visited=");
        int count = 0;
        for (String cell : StoryManager.story(player).visitedCells()) {
            if (count++ > 0) {
                builder.append(',');
            }
            builder.append(cell);
        }
        ServerPlayNetworking.send(player, new MapDataPayload(builder.toString()));
    }

    public static void syncSanity(ServerPlayerEntity player) {
        float sanity = SanityManager.sanity(player);
        ServerPlayNetworking.send(player, new SanityPayload(sanity, SanityManager.ambientTwist(player)));
    }

    public static Identifier journalId() {
        return JournalDataPayload.ID.id();
    }
}
