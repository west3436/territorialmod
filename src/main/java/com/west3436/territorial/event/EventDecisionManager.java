package com.west3436.territorial.event;

import com.west3436.territorial.Config;
import com.west3436.territorial.TerritorialMod;
import com.west3436.territorial.config.AnimalBreedingConfig;
import com.west3436.territorial.config.EventRuleConfig;
import com.west3436.territorial.config.PlantGrowthConfig;
import com.west3436.territorial.util.RuleCache;
import com.west3436.territorial.util.RuleMatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Central manager for evaluating event rules and making decisions.
 * Determines whether events should be allowed based on configured rules.
 */
public class EventDecisionManager {
    private static final Logger LOGGER = TerritorialMod.LOGGER;
    private static final RuleCache cache = new RuleCache();
    
    // Performance metrics - using AtomicLong for thread-safe concurrent access
    private static final AtomicLong totalEvaluations = new AtomicLong(0);
    private static final AtomicLong totalEvaluationTimeNanos = new AtomicLong(0);
    private static final AtomicLong cacheHits = new AtomicLong(0);
    private static final AtomicLong cacheMisses = new AtomicLong(0);
    
    /**
     * Determines if an event should be allowed to occur based on configured rules.
     * Uses caching for performance optimization.
     * 
     * <p>This is the core decision-making method that evaluates all applicable rules
     * and returns whether the event should proceed. Rules are matched based on
     * specificity, with the most specific matching rule taking precedence.</p>
     * 
     * <p><b>Decision Logic:</b></p>
     * <ul>
     *   <li>If control is disabled for the event type, always returns true</li>
     *   <li>If no rules are configured, default is to allow (returns true)</li>
     *   <li>If rules match, the most specific rule determines the outcome</li>
     *   <li>Cache is used to avoid re-evaluating the same context</li>
     * </ul>
     * 
     * <p><b>Thread Safety:</b> This method is thread-safe. The cache uses
     * concurrent data structures internally.</p>
     * 
     * @param context the event context containing location, biome, and entity type information
     * @return true if the event should be allowed, false if it should be blocked
     */
    public static boolean canEventOccur(EventContext context) {
        long startTime = System.nanoTime();
        totalEvaluations.incrementAndGet();
        
        // Validate context
        if (context == null) {
            LOGGER.warn("canEventOccur called with null context - allowing event");
            return true; // Allow by default if no context
        }
        
        // Check cache first
        Boolean cached = cache.get(context);
        if (cached != null) {
            cacheHits.incrementAndGet();
            if (Config.debugLogging) {
                long elapsedNanos = System.nanoTime() - startTime;
                LOGGER.info("Cache hit for event: {} -> {} ({}μs)", context, cached ? "ALLOW" : "DENY", elapsedNanos / 1000);
            }
            return cached;
        }
        
        cacheMisses.incrementAndGet();
        
        // Check global toggles
        switch (context.getEventType()) {
            case PLANT_GROWTH:
                if (!Config.enablePlantGrowthControl) {
                    return true; // Control disabled, allow all
                }
                break;
            case ANIMAL_BREEDING:
                if (!Config.enableAnimalBreedingControl) {
                    return true; // Control disabled, allow all
                }
                break;
        }
        
        // Get applicable rules for this event type
        List<? extends EventRuleConfig> rules = getRulesForEventType(context.getEventType());
        
        if (rules.isEmpty()) {
            if (Config.debugLogging) {
                LOGGER.info("No rules configured for event: {} - allowing by default", context);
            }
            return true; // No rules configured, allow by default
        }
        
        // Find all matching rules and entity-applicable rules
        List<EventRuleConfig> matchingRules = new ArrayList<>();
        List<EventRuleConfig> entityApplicableRules = new ArrayList<>();
        
        for (EventRuleConfig rule : rules) {
            if (!rule.isEnabled()) {
                continue; // Skip disabled rules
            }
            
            // Check if the entity type matches (regardless of dimension/biome/coords)
            if (entityTypeMatches(rule, context)) {
                entityApplicableRules.add(rule);
                if (Config.debugLogging) {
                    LOGGER.info("Entity type matches for rule with dimensions={}, dimensionsBlacklist={}", 
                            rule.getDimensions(), rule.getDimensionsBlacklist());
                }
                
                // Now check if ALL criteria match
                if (ruleMatches(rule, context)) {
                    matchingRules.add(rule);
                    if (Config.debugLogging) {
                        LOGGER.info("Rule fully matches (all criteria)");
                    }
                }
            }
        }
        
        // If no rules match, check if we should deny based on whitelist/blacklist logic
        if (matchingRules.isEmpty()) {
            // If there are rules applicable to this entity type with dimension restrictions,
            // check if we should deny based on whitelist or blacklist logic
            
            // First check for dimension whitelists (higher precedence)
            for (EventRuleConfig rule : entityApplicableRules) {
                List<String> dimensions = rule.getDimensions();
                if (dimensions != null && !dimensions.isEmpty()) {
                    // Whitelist exists and we're not in it (since no rules matched)
                    if (Config.debugLogging) {
                        LOGGER.info("No matching rules for event: {} but dimension whitelist exists - denying", context);
                    }
                    cache.put(context, false);
                    return false;
                }
            }
            
            // Then check for blacklisted dimensions
            // Only compute currentDimension if there are potential blacklist rules
            if (!entityApplicableRules.isEmpty()) {
                String currentDimension = context.getDimension().location().toString();
                if (Config.debugLogging) {
                    LOGGER.info("Checking blacklists for dimension: {}, entity applicable rules: {}", 
                            currentDimension, entityApplicableRules.size());
                }
                for (EventRuleConfig rule : entityApplicableRules) {
                    List<String> dimensionsBlacklist = rule.getDimensionsBlacklist();
                    if (dimensionsBlacklist != null && !dimensionsBlacklist.isEmpty()) {
                        if (Config.debugLogging) {
                            LOGGER.info("Rule has blacklist: {}, checking if contains: {}", 
                                    dimensionsBlacklist, currentDimension);
                        }
                        if (dimensionsBlacklist.contains(currentDimension)) {
                            if (Config.debugLogging) {
                                LOGGER.info("No matching rules for event: {} but in blacklisted dimension - denying", context);
                            }
                            cache.put(context, false);
                            return false;
                        }
                    }
                }
            }
            
            // No dimension restrictions prevent this event
            if (Config.debugLogging) {
                LOGGER.info("No matching rules for event: {} - allowing", context);
            }
            cache.put(context, true);
            return true;
        }
        
        // Sort by specificity (most specific first)
        matchingRules.sort(Comparator.comparingInt(EventRuleConfig::getSpecificity).reversed());
        
        // Get the most specific rule's decision
        EventRuleConfig mostSpecific = matchingRules.get(0);
        boolean allowed = mostSpecific.isAllow();
        
        if (Config.debugLogging) {
            long elapsedNanos = System.nanoTime() - startTime;
            LOGGER.info("Event decision for {}: {} (matched {} rules, most specific: specificity={}, time: {}μs)",
                    context, allowed ? "ALLOW" : "DENY", matchingRules.size(), mostSpecific.getSpecificity(), elapsedNanos / 1000);
        }
        
        // Cache the result
        cache.put(context, allowed);
        
        // Update performance metrics
        long elapsedNanos = System.nanoTime() - startTime;
        totalEvaluationTimeNanos.addAndGet(elapsedNanos);
        
        return allowed;
    }
    
    /**
     * Gets the list of rules for a specific event type.
     *
     * @param eventType the type of event to get rules for, must not be null
     * @return list of applicable rules, never null (may be empty)
     */
    private static List<? extends EventRuleConfig> getRulesForEventType(EventType eventType) {
        switch (eventType) {
            case PLANT_GROWTH:
                return Config.getPlantGrowthRules();
            case ANIMAL_BREEDING:
                return Config.getAnimalBreedingRules();
            default:
                return new ArrayList<>();
        }
    }
    
    /**
     * Checks if a rule matches the given event context.
     * 
     * <p>This method evaluates all applicable criteria including:</p>
     * <ul>
     *   <li>Dimension matching</li>
     *   <li>Biome matching (if biome control is enabled)</li>
     *   <li>Temperature range (if biome control is enabled)</li>
     *   <li>Coordinate ranges (if coordinate control is enabled)</li>
     *   <li>Entity/crop/vein type matching</li>
     * </ul>
     * 
     * @param rule the rule to evaluate, must not be null
     * @param context the event context to match against, must not be null
     * @return true if all criteria match, false if any criterion fails
     */
    private static boolean ruleMatches(EventRuleConfig rule, EventContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getPosition();
        Holder<Biome> biome = context.getBiome();
        ResourceLocation entityType = context.getEntityType();
        
        // Check dimension
        if (!RuleMatcher.matchesDimension(context.getDimension(), rule.getDimensions(), rule.getDimensionsBlacklist())) {
            return false;
        }
        
        // Check biome (if Config.enableBiomeControl is true)
        if (Config.enableBiomeControl) {
            if (!RuleMatcher.matchesBiome(biome, rule.getBiomes(), level)) {
                return false;
            }
            
            // Check temperature
            if (!RuleMatcher.matchesTemperature(biome.value(), rule.getTemperatureMin(), rule.getTemperatureMax())) {
                return false;
            }
        }
        
        // Check coordinates (if Config.enableCoordinateControl is true)
        if (Config.enableCoordinateControl) {
            if (!RuleMatcher.matchesCoordinates(pos, 
                    rule.getXMin(), rule.getXMax(),
                    rule.getYMin(), rule.getYMax(),
                    rule.getZMin(), rule.getZMax())) {
                return false;
            }
        }
        
        // Check entity type (specific to each event type)
        if (!entityTypeMatches(rule, context)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks if the entity type in the context matches the rule's entity type patterns.
     * This is a helper method used to determine if a rule is applicable to a specific entity type,
     * regardless of dimension, biome, or coordinate restrictions.
     *
     * @param rule the rule to check
     * @param context the event context containing the entity type
     * @return true if the entity type matches, false otherwise (or if event type is not recognized)
     */
    private static boolean entityTypeMatches(EventRuleConfig rule, EventContext context) {
        ResourceLocation entityType = context.getEntityType();

        if (context.getEventType() == EventType.PLANT_GROWTH && rule instanceof PlantGrowthConfig) {
            PlantGrowthConfig plantRule = (PlantGrowthConfig) rule;
            return RuleMatcher.matchesEntityType(entityType, plantRule.getCropTypes());
        } else if (context.getEventType() == EventType.ANIMAL_BREEDING && rule instanceof AnimalBreedingConfig) {
            AnimalBreedingConfig animalRule = (AnimalBreedingConfig) rule;
            return RuleMatcher.matchesEntityType(entityType, animalRule.getAnimalTypes());
        }

        // Unknown event type or mismatched rule type - treat as no match
        return false;
    }
    
    /**
     * Convenience method for checking if a plant growth event should occur.
     * 
     * @param level the level (world) where the plant is growing, must not be null
     * @param pos the position of the plant, must not be null
     * @param cropType the type of crop/plant, must not be null
     * @return true if growth should be allowed, false if it should be blocked
     * @throws NullPointerException if any parameter is null
     */
    public static boolean canPlantGrow(Level level, BlockPos pos, ResourceLocation cropType) {
        if (level == null || pos == null || cropType == null) {
            throw new NullPointerException("level, pos, and cropType must not be null");
        }
        EventContext context = EventContext.forPlantGrowth(level, pos, cropType);
        return canEventOccur(context);
    }
    
    /**
     * Convenience method for checking if an animal breeding event should occur.
     *
     * @param level the level (world) where breeding is occurring, must not be null
     * @param pos the position of the breeding, must not be null
     * @param animalType the type of animal, must not be null
     * @return true if breeding should be allowed, false if it should be blocked
     * @throws NullPointerException if any parameter is null
     */
    public static boolean canAnimalBreed(Level level, BlockPos pos, ResourceLocation animalType) {
        if (level == null || pos == null || animalType == null) {
            throw new NullPointerException("level, pos, and animalType must not be null");
        }
        EventContext context = EventContext.forAnimalBreeding(level, pos, animalType);
        return canEventOccur(context);
    }

    /**
     * Generic convenience method for checking if an event should occur.
     * Used by commands and testing.
     */
    public static boolean canEventOccur(EventType eventType, Level level, BlockPos pos, ResourceLocation entityType) {
        EventContext context;
        switch (eventType) {
            case PLANT_GROWTH:
                context = EventContext.forPlantGrowth(level, pos, entityType);
                break;
            case ANIMAL_BREEDING:
                context = EventContext.forAnimalBreeding(level, pos, entityType);
                break;
            default:
                return true;
        }
        return canEventOccur(context);
    }
    
    /**
     * Clears the rule evaluation cache.
     * Should be called when configuration is reloaded.
     */
    public static void clearCache() {
        cache.clear();
        if (Config.debugLogging) {
            LOGGER.info("Rule evaluation cache cleared");
        }
    }
    
    /**
     * Gets the current cache size for debugging/monitoring.
     * 
     * @return number of cached entries
     */
    public static int getCacheSize() {
        return cache.size();
    }
    
    /**
     * Gets performance statistics for rule evaluation.
     * 
     * @return formatted string with performance metrics
     */
    public static String getPerformanceStats() {
        long evals = totalEvaluations.get();
        if (evals == 0) {
            return "No evaluations performed yet";
        }
        
        double avgTimeMillis = (totalEvaluationTimeNanos.get() / (double) evals) / 1_000_000.0;
        double cacheHitRate = (cacheHits.get() / (double) evals) * 100.0;
        
        return String.format(
            "Performance Stats:\n" +
            "  Total Evaluations: %d\n" +
            "  Avg Evaluation Time: %.3fms (%.1fμs)\n" +
            "  Cache Hits: %d (%.1f%%)\n" +
            "  Cache Misses: %d\n" +
            "  Cache Size: %d entries",
            evals,
            avgTimeMillis,
            avgTimeMillis * 1000.0,
            cacheHits.get(),
            cacheHitRate,
            cacheMisses.get(),
            getCacheSize()
        );
    }
    
    /**
     * Resets performance statistics.
     * Useful for starting a fresh performance test.
     */
    public static void resetPerformanceStats() {
        totalEvaluations.set(0);
        totalEvaluationTimeNanos.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        
        if (Config.debugLogging) {
            LOGGER.info("Performance statistics reset");
        }
    }
    
    /**
     * Logs current performance statistics.
     */
    public static void logPerformanceStats() {
        LOGGER.info(getPerformanceStats());
    }
}
