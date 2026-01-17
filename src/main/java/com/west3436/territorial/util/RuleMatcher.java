package com.west3436.territorial.util;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.slf4j.Logger;

import java.util.List;

/**
 * Utility class for matching rules against game state.
 * Provides methods to check if biomes, coordinates, temperatures, and dimensions match rule criteria.
 */
public class RuleMatcher {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Checks if a biome matches any of the specified biome criteria.
     * 
     * <p><b>Supported Patterns:</b></p>
     * <ul>
     *   <li>Exact biome ID match: {@code "minecraft:desert"}</li>
     *   <li>Wildcard namespace match: {@code "minecraft:*"} (all Minecraft biomes)</li>
     *   <li>Global wildcard: {@code "*"} (all biomes)</li>
     *   <li>Biome tags: {@code "#minecraft:is_hot"} or {@code "#forge:is_desert"}</li>
     * </ul>
     * 
     * <p><b>Tag Format:</b> Tags must start with {@code #} followed by a valid
     * ResourceLocation (e.g., {@code #namespace:tag_name}). Invalid tag formats
     * are logged and skipped.</p>
     * 
     * @param biomeHolder the biome holder to check, must not be null
     * @param biomeIds list of biome IDs/patterns to match against, null or empty means match all
     * @param level the level (world) context for registry access, must not be null
     * @return true if biome matches any criteria or if biomeIds is null/empty, false otherwise
     * @throws NullPointerException if biomeHolder or level is null
     */
    public static boolean matchesBiome(Holder<Biome> biomeHolder, List<String> biomeIds, Level level) {
        if (biomeHolder == null || level == null) {
            throw new NullPointerException("biomeHolder and level must not be null");
        }
        
        if (biomeIds == null || biomeIds.isEmpty()) {
            return true; // Empty list means match all biomes
        }
        
        // Get the biome resource location with error handling
        ResourceLocation biomeLocation = null;
        try {
            biomeLocation = level.registryAccess()
                    .registryOrThrow(Registries.BIOME)
                    .getKey(biomeHolder.value());
        } catch (Exception e) {
            // Log error and return false if we can't get the biome location
            LOGGER.debug("Failed to get biome location from registry: {}", e.getMessage());
            return false;
        }
        
        if (biomeLocation == null) {
            return false;
        }
        
        String biomeId = biomeLocation.toString();
        
        for (String pattern : biomeIds) {
            if (pattern == null || pattern.isEmpty()) {
                continue;
            }
            
            // Check for tag match (starts with #)
            if (pattern.startsWith("#")) {
                String tagId = pattern.substring(1);
                ResourceLocation tagLocation = ResourceLocation.tryParse(tagId);
                if (tagLocation != null) {
                    try {
                        TagKey<Biome> tagKey = TagKey.create(Registries.BIOME, tagLocation);
                        if (biomeHolder.is(tagKey)) {
                            return true;
                        }
                    } catch (Exception e) {
                        // Log error but continue checking other patterns
                        LOGGER.debug("Failed to check biome tag '{}': {}", tagId, e.getMessage());
                    }
                } else {
                    LOGGER.debug("Invalid biome tag format: {}", pattern);
                }
                continue;
            }
            
            // Check for global wildcard
            if (pattern.equals("*")) {
                return true;
            }
            
            // Check for namespace wildcard (e.g., "minecraft:*")
            if (pattern.endsWith(":*")) {
                String namespace = pattern.substring(0, pattern.length() - 2);
                if (biomeId.startsWith(namespace + ":")) {
                    return true;
                }
            }
            // Check for exact match
            else if (pattern.equals(biomeId)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a biome's temperature falls within the specified range.
     * 
     * @param biome the biome to check
     * @param minTemp minimum temperature (inclusive), null means no minimum
     * @param maxTemp maximum temperature (inclusive), null means no maximum
     * @return true if temperature is within range (or no range specified), false otherwise
     */
    public static boolean matchesTemperature(Biome biome, Float minTemp, Float maxTemp) {
        if (minTemp == null && maxTemp == null) {
            return true; // No temperature restriction
        }
        
        float biomeTemp = biome.getBaseTemperature();
        
        if (minTemp != null && biomeTemp < minTemp) {
            return false;
        }
        
        if (maxTemp != null && biomeTemp > maxTemp) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks if a position falls within the specified coordinate ranges.
     * 
     * @param pos the block position to check
     * @param xMin minimum X coordinate (inclusive), null means unbounded
     * @param xMax maximum X coordinate (inclusive), null means unbounded
     * @param yMin minimum Y coordinate (inclusive), null means unbounded
     * @param yMax maximum Y coordinate (inclusive), null means unbounded
     * @param zMin minimum Z coordinate (inclusive), null means unbounded
     * @param zMax maximum Z coordinate (inclusive), null means unbounded
     * @return true if position is within all specified bounds, false otherwise
     */
    public static boolean matchesCoordinates(BlockPos pos, Integer xMin, Integer xMax,
                                            Integer yMin, Integer yMax,
                                            Integer zMin, Integer zMax) {
        if (xMin != null && pos.getX() < xMin) {
            return false;
        }
        if (xMax != null && pos.getX() > xMax) {
            return false;
        }
        
        if (yMin != null && pos.getY() < yMin) {
            return false;
        }
        if (yMax != null && pos.getY() > yMax) {
            return false;
        }
        
        if (zMin != null && pos.getZ() < zMin) {
            return false;
        }
        if (zMax != null && pos.getZ() > zMax) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks if a dimension matches the specified dimension criteria.
     * Supports both whitelist and blacklist approaches:
     * - If dimensions list is specified: dimension must be in the list (whitelist)
     * - If dimensionsBlacklist is specified: dimension must NOT be in the list (blacklist)
     * - If neither is specified: all dimensions match
     * 
     * @param dimension the dimension resource key to check
     * @param dimensions whitelist of dimension IDs (e.g., ["minecraft:overworld"]), null or empty means no whitelist
     * @param dimensionsBlacklist blacklist of dimension IDs, null or empty means no blacklist
     * @return true if dimension matches criteria, false otherwise
     */
    public static boolean matchesDimension(ResourceKey<Level> dimension, List<String> dimensions, List<String> dimensionsBlacklist) {
        ResourceLocation dimensionLocation = dimension.location();
        String dimensionId = dimensionLocation.toString();
        
        // Check whitelist first
        if (dimensions != null && !dimensions.isEmpty()) {
            // Whitelist mode: dimension must be in the list
            return dimensions.contains(dimensionId);
        }
        
        // Check blacklist
        if (dimensionsBlacklist != null && !dimensionsBlacklist.isEmpty()) {
            // Blacklist mode: dimension must NOT be in the list
            return !dimensionsBlacklist.contains(dimensionId);
        }
        
        // No restrictions - match all dimensions
        return true;
    }
    
    /**
     * Checks if a dimension matches the specified dimension ID.
     * 
     * @deprecated Use {@link #matchesDimension(ResourceKey, List, List)} instead.
     * @param dimension the dimension resource key to check
     * @param dimensionId the dimension ID to match (e.g., "minecraft:overworld"), null means match all
     * @return true if dimension matches (or no dimension specified), false otherwise
     */
    @Deprecated
    public static boolean matchesDimension(ResourceKey<Level> dimension, String dimensionId) {
        if (dimensionId == null || dimensionId.isEmpty()) {
            return true; // No dimension restriction
        }
        
        ResourceLocation dimensionLocation = dimension.location();
        return dimensionLocation.toString().equals(dimensionId);
    }
    
    /**
     * Checks if an entity/block type matches any of the specified type patterns.
     * Supports:
     * - Exact ID match (e.g., "minecraft:wheat")
     * - Wildcard namespace match (e.g., "minecraft:*")
     * - Global wildcard (e.g., "*")
     * 
     * @param entityType the entity/block type ID to check
     * @param typePatterns list of type patterns to match against
     * @return true if type matches any pattern, false if no match or typePatterns is empty
     */
    public static boolean matchesEntityType(ResourceLocation entityType, List<String> typePatterns) {
        if (typePatterns == null || typePatterns.isEmpty()) {
            return true; // Empty list means match all types
        }
        
        String typeId = entityType.toString();
        
        for (String pattern : typePatterns) {
            if (pattern == null || pattern.isEmpty()) {
                continue;
            }
            
            // Check for global wildcard
            if (pattern.equals("*")) {
                return true;
            }
            
            // Check for namespace wildcard (e.g., "minecraft:*")
            if (pattern.endsWith(":*")) {
                String namespace = pattern.substring(0, pattern.length() - 2);
                if (typeId.startsWith(namespace + ":")) {
                    return true;
                }
            }
            // Check for exact match
            else if (pattern.equals(typeId)) {
                return true;
            }
        }
        
        return false;
    }
}
