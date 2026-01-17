package com.west3436.territorial.event;

import com.west3436.territorial.Config;
import com.west3436.territorial.TerritorialMod;
import com.west3436.territorial.config.ConfigLoader;
import com.west3436.territorial.item.ModItems;
import com.west3436.territorial.util.AlmanacContentGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Handler for player join events.
 * Sends warning messages to players when they join if there are invalid rules in the configuration.
 * Can also give the Farmer's Almanac to new players if configured.
 */
@Mod.EventBusSubscriber(modid = TerritorialMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerJoinHandler {
    private static final Logger LOGGER = TerritorialMod.LOGGER;
    private static final String TERRITORIAL_DATA_TAG = "territorial_data";
    private static final String RECEIVED_ALMANAC_TAG = "received_almanac";
    
    /**
     * Handles player login events.
     * Sends a chat warning to the player if invalid rules were detected during configuration loading.
     * Gives the Farmer's Almanac to new players if configured to do so.
     * 
     * @param event the player logged in event
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Get invalid rule count once to avoid race condition
            int invalidCount = ConfigLoader.getInvalidRuleCount();
            
            // Check if there are invalid rules
            if (invalidCount > 0) {
                // Build warning message parts
                String pluralSuffix = invalidCount > 1 ? "s" : "";
                String ruleText = String.format(" invalid rule%s in the configuration. ", pluralSuffix);
                
                // Send warning message to player
                Component warningMessage = Component.literal("⚠ Warning: ")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                        .append(Component.literal("Territorial mod detected ")
                                .withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(String.valueOf(invalidCount))
                                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                        .append(Component.literal(ruleText)
                                .withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal("Check server logs for details.")
                                .withStyle(ChatFormatting.GRAY));
                
                player.sendSystemMessage(warningMessage);
                
                LOGGER.debug("Sent invalid rule warning to player: {}", player.getName().getString());
            }
            
            // Give Farmer's Almanac on first join if configured
            if (Config.giveAlmanacOnFirstJoin) {
                giveAlmanacIfFirstJoin(player);
            }
        }
    }
    
    /**
     * Gives the Farmer's Almanac to a player if they haven't received it before.
     */
    private static void giveAlmanacIfFirstJoin(ServerPlayer player) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag territorialData = persistentData.getCompound(TERRITORIAL_DATA_TAG);
        
        // Check if player has already received the almanac
        if (!territorialData.getBoolean(RECEIVED_ALMANAC_TAG)) {
            // Give the player a Farmer's Almanac
            ItemStack almanac = new ItemStack(ModItems.FARMERS_ALMANAC.get());
            
            // Try to add to player inventory
            if (player.getInventory().add(almanac)) {
                // Mark that player has received the almanac
                territorialData.putBoolean(RECEIVED_ALMANAC_TAG, true);
                persistentData.put(TERRITORIAL_DATA_TAG, territorialData);
                
                // Send a message to the player
                Component message = Component.literal("You received a ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal("Farmer's Almanac")
                                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                        .append(Component.literal("! Right-click it to learn about this world's territorial rules.")
                                .withStyle(ChatFormatting.GRAY));
                
                player.sendSystemMessage(message);
                
                LOGGER.debug("Gave Farmer's Almanac to player: {}", player.getName().getString());
            } else {
                LOGGER.warn("Failed to add Farmer's Almanac to player inventory: {}", player.getName().getString());
            }
        }
    }
}
