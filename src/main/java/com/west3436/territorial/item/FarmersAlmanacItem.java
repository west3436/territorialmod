package com.west3436.territorial.item;

import com.west3436.territorial.network.ModNetworking;
import com.west3436.territorial.network.OpenAlmanacPacket;
import com.west3436.territorial.util.AlmanacContentGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Farmer's Almanac item that displays all current Territorial rules
 * in an easy-to-understand format.
 * 
 * When right-clicked, it generates a written book containing:
 * - Plant growth rules (with actual crop names)
 * - Animal breeding rules (with actual animal names)
 * - Mineral vein rules (if applicable)
 * - Biome names, coordinate ranges, and temperature ranges in readable format
 */
public class FarmersAlmanacItem extends Item {
    
    public FarmersAlmanacItem(Properties properties) {
        super(properties.stacksTo(1));
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide && level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            // Generate the almanac book content dynamically
            ItemStack bookStack = AlmanacContentGenerator.generateAlmanacBook(serverLevel);

            // Send the book data to the client to open the book screen
            ModNetworking.sendToPlayer(new OpenAlmanacPacket(bookStack.getTag()), serverPlayer);

            return InteractionResultHolder.success(itemStack);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> components, TooltipFlag flag) {
        super.appendHoverText(stack, level, components, flag);
        components.add(Component.literal("Right-click to view current Territorial rules")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
