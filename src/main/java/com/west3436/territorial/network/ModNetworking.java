package com.west3436.territorial.network;

import com.west3436.territorial.TerritorialMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Handles network communication for the Territorial mod.
 */
public class ModNetworking {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TerritorialMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private static int nextId() {
        return packetId++;
    }

    public static void register() {
        INSTANCE.messageBuilder(OpenAlmanacPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenAlmanacPacket::new)
                .encoder(OpenAlmanacPacket::encode)
                .consumerMainThread(OpenAlmanacPacket::handle)
                .add();
    }

    public static void sendToPlayer(Object packet, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
