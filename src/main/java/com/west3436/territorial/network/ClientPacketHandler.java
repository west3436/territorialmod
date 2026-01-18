package com.west3436.territorial.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-side packet handling for opening screens.
 * This class is only loaded on the client to avoid NoClassDefFoundError on dedicated servers.
 */
@OnlyIn(Dist.CLIENT)
public class ClientPacketHandler {

    public static void openAlmanacScreen(CompoundTag bookNbt) {
        ItemStack bookStack = new ItemStack(Items.WRITTEN_BOOK);
        if (bookNbt != null) {
            bookStack.setTag(bookNbt);
        }
        Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.WrittenBookAccess(bookStack)));
    }
}
