package com.west3436.territorial.event;

import com.west3436.territorial.Config;
import com.west3436.territorial.TerritorialMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Handler for plant growth events.
 * Controls where crops and plants can grow based on configured rules.
 */
@Mod.EventBusSubscriber(modid = TerritorialMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlantGrowthHandler {
    private static final Logger LOGGER = TerritorialMod.LOGGER;
    
    /**
     * Helper method to safely get biome information for debug logging.
     * 
     * @param level the level (world)
     * @param pos the position
     * @return biome ID string, or "unknown" if not available
     */
    private static String getBiomeInfo(Level level, BlockPos pos) {
        try {
            return level.getBiome(pos).unwrapKey()
                .map(key -> key.location().toString())
                .orElse("unknown");
        } catch (Exception e) {
            // Could not get biome info, use "unknown"
            return "unknown";
        }
    }
    
    /**
     * Handles crop growth events before they occur.
     * Queries the EventDecisionManager to determine if growth should be allowed
     * based on configured rules for biome, temperature, and coordinates.
     * 
     * <p>This method will block the crop growth event by setting its result to DENY
     * if the EventDecisionManager determines that growth is not allowed at the 
     * specified location.</p>
     * 
     * <p><b>Edge Cases:</b></p>
     * <ul>
     *   <li>If control is disabled globally, all growth is allowed</li>
     *   <li>If level or position is null, event proceeds normally</li>
     *   <li>If crop type cannot be determined, event proceeds normally</li>
     *   <li>Failures are logged but don't crash the game</li>
     * </ul>
     * 
     * @param event the crop growth event, must not be null
     */
    @SubscribeEvent
    public static void onCropGrow(BlockEvent.CropGrowEvent.Pre event) {
        // Skip if control is disabled globally
        if (!Config.enablePlantGrowthControl) {
            if (Config.debugLogging) {
                try {
                    Level level = (Level) event.getLevel();
                    BlockPos pos = event.getPos();
                    BlockState state = event.getState();
                    if (level != null && pos != null && state != null) {
                        Block block = state.getBlock();
                        ResourceLocation cropType = BuiltInRegistries.BLOCK.getKey(block);
                        LOGGER.info("Plant growth event: {} at {} - ALLOWED (control disabled)", cropType, pos);
                    }
                } catch (Exception e) {
                    // Ignore errors in debug logging
                }
            }
            return;
        }
        
        try {
            // Validate event data
            if (event.getLevel() == null || !(event.getLevel() instanceof Level)) {
                LOGGER.warn("Plant growth event received with invalid level - allowing growth");
                return;
            }
            
            Level level = (Level) event.getLevel();
            BlockPos pos = event.getPos();
            BlockState state = event.getState();
            
            // Validate position and state
            if (pos == null || state == null) {
                LOGGER.warn("Plant growth event received with null position or state - allowing growth");
                return;
            }
            
            // Get the crop type from the block
            Block block = state.getBlock();
            ResourceLocation cropType = BuiltInRegistries.BLOCK.getKey(block);
            
            // Validate crop type was found
            if (cropType == null) {
                LOGGER.warn("Could not determine crop type for block {} - allowing growth", block);
                return;
            }
            
            // Query the decision manager
            boolean allowed = EventDecisionManager.canPlantGrow(level, pos, cropType);
            
            if (Config.debugLogging) {
                String biomeInfo = getBiomeInfo(level, pos);
                LOGGER.info("Plant growth event: {} at {} in biome {} - {}", 
                    cropType, pos, biomeInfo, allowed ? "ALLOWED" : "BLOCKED");
            }
            
            if (!allowed) {
                // Block the growth using setResult instead of setCanceled
                // CropGrowEvent.Pre is not cancelable, but it has a Result that can be set
                event.setResult(Event.Result.DENY);
                
                // Log at info level for important decisions (when debug is off)
                if (!Config.debugLogging) {
                    LOGGER.info("Blocked plant growth: {} at {}", cropType, pos);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error processing plant growth event - allowing growth to proceed", e);
        }
    }
    
    /**
     * Handles bonemeal usage on blocks to prevent accelerated growth in restricted areas.
     * This complements the onCropGrow handler by blocking player-initiated bonemeal usage,
     * which bypasses the natural growth event system.
     * 
     * <p>This method intercepts right-click events with bonemeal and checks if the target
     * block is a BonemealableBlock (growable plant). If the plant growth is not allowed at 
     * the location based on configured rules, the bonemeal usage is canceled.</p>
     * 
     * <p><b>Performance:</b> The handler includes early returns to minimize processing:</p>
     * <ul>
     *   <li>Exits immediately if not using bonemeal</li>
     *   <li>Exits immediately if target is not a BonemealableBlock</li>
     *   <li>Only queries decision manager for valid growable plants</li>
     * </ul>
     * 
     * <p><b>Edge Cases:</b></p>
     * <ul>
     *   <li>If control is disabled globally, all bonemeal usage is allowed</li>
     *   <li>Only blocks bonemeal on BonemealableBlock instances (crops, saplings, etc.)</li>
     *   <li>If level or position is null, bonemeal usage proceeds normally</li>
     *   <li>If crop type cannot be determined, bonemeal usage proceeds normally</li>
     *   <li>Failures are logged but don't crash the game</li>
     * </ul>
     * 
     * @param event the player right-click block event, must not be null
     */
    @SubscribeEvent
    public static void onBonemealUse(PlayerInteractEvent.RightClickBlock event) {
        // Skip if control is disabled globally
        if (!Config.enablePlantGrowthControl) {
            return;
        }
        
        try {
            // Check if the item being used is bonemeal
            ItemStack heldItem = event.getItemStack();
            if (heldItem.isEmpty() || !heldItem.is(Items.BONE_MEAL)) {
                return; // Not bonemeal, ignore
            }
            
            Level level = event.getLevel();
            BlockPos pos = event.getPos();
            
            // Validate level and position
            if (level == null || pos == null) {
                return;
            }
            
            BlockState state = level.getBlockState(pos);
            if (state == null) {
                return;
            }
            
            Block block = state.getBlock();
            
            // Only check blocks that can actually be bonemealed (growable plants)
            if (!(block instanceof BonemealableBlock)) {
                return; // Not a growable block, ignore
            }
            
            // Get the crop type from the block
            ResourceLocation cropType = BuiltInRegistries.BLOCK.getKey(block);
            
            // Validate crop type was found
            if (cropType == null) {
                LOGGER.warn("Could not determine crop type for block {} - allowing bonemeal", block);
                return;
            }
            
            // Query the decision manager
            boolean allowed = EventDecisionManager.canPlantGrow(level, pos, cropType);
            
            if (Config.debugLogging) {
                String biomeInfo = getBiomeInfo(level, pos);
                LOGGER.info("Bonemeal usage on plant: {} at {} in biome {} - {}", 
                    cropType, pos, biomeInfo, allowed ? "ALLOWED" : "BLOCKED");
            }
            
            if (!allowed) {
                // Block the bonemeal usage by canceling the event
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
                
                // Log at info level for important decisions (when debug is off)
                if (!Config.debugLogging) {
                    LOGGER.info("Blocked bonemeal usage on plant: {} at {}", cropType, pos);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error processing bonemeal usage event - allowing bonemeal to proceed", e);
        }
    }
}
