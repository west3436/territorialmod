package com.west3436.territorial.event;

import com.west3436.territorial.Config;
import com.west3436.territorial.TerritorialMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Handler for animal breeding events.
 * Controls where animals can breed based on configured rules.
 */
@Mod.EventBusSubscriber(modid = TerritorialMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AnimalBreedingHandler {
    private static final Logger LOGGER = TerritorialMod.LOGGER;
    
    /**
     * Handles baby entity spawn events (triggered by breeding).
     * Queries the EventDecisionManager to determine if breeding should be allowed
     * based on configured rules for biome, temperature, and coordinates.
     * 
     * <p>This method will cancel the baby spawn event (preventing breeding) if the
     * EventDecisionManager determines that breeding is not allowed at the specified location.</p>
     * 
     * <p><b>Parent Validation:</b></p>
     * <ul>
     *   <li>At least one parent must be present to make a decision</li>
     *   <li>Uses the first non-null parent for location and type determination</li>
     *   <li>Both parents are assumed to be at similar locations</li>
     * </ul>
     * 
     * <p><b>Edge Cases:</b></p>
     * <ul>
     *   <li>If control is disabled globally, all breeding is allowed</li>
     *   <li>If both parents are null, breeding is allowed</li>
     *   <li>If level, position, or animal type is null, breeding is allowed</li>
     *   <li>Failures are logged but don't crash the game</li>
     * </ul>
     * 
     * @param event the baby entity spawn event, must not be null
     */
    @SubscribeEvent
    public static void onBabyEntitySpawn(BabyEntitySpawnEvent event) {
        // Skip if control is disabled globally
        if (!Config.enableAnimalBreedingControl) {
            if (Config.debugLogging) {
                try {
                    Mob parentA = event.getParentA();
                    Mob parentB = event.getParentB();
                    Entity parent = parentA != null ? parentA : parentB;
                    if (parent != null && parent.level() != null) {
                        Level level = parent.level();
                        BlockPos pos = parent.blockPosition();
                        ResourceLocation animalType = BuiltInRegistries.ENTITY_TYPE.getKey(parent.getType());
                        LOGGER.info("Animal breeding event: {} at {} - ALLOWED (control disabled)", animalType, pos);
                    }
                } catch (Exception e) {
                    // Ignore errors in debug logging
                }
            }
            return;
        }
        
        try {
            // Get parent entities
            Mob parentA = event.getParentA();
            Mob parentB = event.getParentB();
            
            // We need at least one parent to determine location and type
            if (parentA == null && parentB == null) {
                LOGGER.debug("Baby entity spawn event with no parents - allowing breeding");
                return;
            }
            
            // Use the first non-null parent for location and type
            Entity parent = parentA != null ? parentA : parentB;
            
            // Validate parent data
            if (parent.level() == null) {
                LOGGER.warn("Parent entity has no level - allowing breeding");
                return;
            }
            
            Level level = parent.level();
            BlockPos pos = parent.blockPosition();
            
            if (pos == null) {
                LOGGER.warn("Parent entity has no position - allowing breeding");
                return;
            }
            
            // Get the animal type from the entity
            ResourceLocation animalType = BuiltInRegistries.ENTITY_TYPE.getKey(parent.getType());
            
            // Validate animal type was found
            if (animalType == null) {
                LOGGER.warn("Could not determine animal type for entity {} - allowing breeding", parent.getType());
                return;
            }
            
            // Query the decision manager
            boolean allowed = EventDecisionManager.canAnimalBreed(level, pos, animalType);
            
            if (Config.debugLogging) {
                String biomeInfo = "unknown";
                try {
                    biomeInfo = level.getBiome(pos).unwrapKey()
                        .map(key -> key.location().toString())
                        .orElse("unknown");
                } catch (Exception e) {
                    // Could not get biome info, use "unknown"
                }
                
                LOGGER.info("Animal breeding event: {} at {} in biome {} - {}", 
                    animalType, pos, biomeInfo, allowed ? "ALLOWED" : "BLOCKED");
            }
            
            if (!allowed) {
                // Block the breeding by canceling the event
                event.setCanceled(true);
                
                // Log at info level for important decisions (when debug is off)
                if (!Config.debugLogging) {
                    LOGGER.info("Blocked animal breeding: {} at {}", animalType, pos);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error processing animal breeding event - allowing breeding to proceed", e);
        }
    }
}
