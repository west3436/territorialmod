package com.west3436.territorial.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
            // Create the book ItemStack from the NBT data
            ItemStack bookStack = new ItemStack(Items.WRITTEN_BOOK);
            if (bookNbt != null) {
                bookStack.setTag(bookNbt);
            }

            // Open the book screen on the client
            Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.WrittenBookAccess(bookStack)));
        });
        ctx.get().setPacketHandled(true);
    }
}
