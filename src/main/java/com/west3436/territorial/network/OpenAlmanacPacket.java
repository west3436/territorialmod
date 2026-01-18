package com.west3436.territorial.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from server to client to open the Farmer's Almanac book.
 * Contains the book's NBT data which is used to create and display the book screen.
 */
public class OpenAlmanacPacket {

    private final CompoundTag bookNbt;

    public OpenAlmanacPacket(CompoundTag bookNbt) {
        this.bookNbt = bookNbt;
    }

    public OpenAlmanacPacket(FriendlyByteBuf buf) {
        this.bookNbt = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(bookNbt);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.openAlmanacScreen(bookNbt));
        });
        ctx.get().setPacketHandled(true);
    }
}
