package com.west3436.territorial.config;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.mojang.logging.LogUtils;
import com.west3436.territorial.Config;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles loading and parsing of configuration files for event rules.
 * 
 * <p>This class manages the lifecycle of configuration files, including:</p>
 * <ul>
 *   <li>Loading configuration from disk on mod initialization</li>
 *   <li>Creating default configuration files if none exist</li>
 *   <li>Hot-reloading configuration when requested</li>
 *   <li>Validating rule configurations</li>
 *   <li>Clearing caches when configuration changes</li>
 * </ul>
 * 
 * <p><b>Configuration Format:</b> Uses TOML format for rule definitions.
 * The configuration file is located at {@code config/territorial-rules.toml}.</p>
 * 
 * <p><b>Default Behavior:</b> If no configuration file exists, a default
 * configuration with examples is created. If no rules are defined, all
 * events are allowed by default.</p>
 * 
 * <p><b>Error Handling:</b> Configuration errors are logged but do not
 * prevent the mod from loading. In case of errors, the mod operates with
 * default settings (all events allowed).</p>
 * 
 * @see Config
 * @see EventRuleConfig
 */
public class ConfigLoader {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CONFIG_DIRECTORY = "config";
    private static final String CONFIG_FILE_NAME = "territorial-rules.toml";
    
    /**
     * Counter for invalid rules detected during configuration loading.
     * Uses AtomicInteger for thread-safe operations.
     */
    private static final java.util.concurrent.atomic.AtomicInteger invalidRuleCount = new java.util.concurrent.atomic.AtomicInteger(0);
    
    /**
     * Loads configuration from the config directory.
     * 
     * <p>This method performs the following steps:</p>
     * <ol>
     *   <li>Checks if configuration file exists</li>
     *   <li>Creates default configuration if missing</li>
     *   <li>Parses and validates the configuration</li>
     *   <li>Logs a summary of loaded rules</li>
     * </ol>
     * 
     * <p>If any errors occur during loading, they are logged and the mod
     * continues with default settings (all events allowed).</p>
     * 
     * @throws Exception propagated from file operations (caught internally)
     */
    public static void loadConfiguration() {
        LOGGER.info("Loading Territorial configuration...");
        
        // Reset invalid rule count
        invalidRuleCount.set(0);
        
        try {
            Path configPath = getConfigPath();
            
            // Create default config if it doesn't exist
            if (!Files.exists(configPath)) {
                LOGGER.info("Configuration file not found - creating default configuration");
                createDefaultConfiguration(configPath);
            } else {
                LOGGER.info("Found existing configuration file: {}", configPath);
            }
            
            // Parse and load configuration
            parseConfiguration(configPath);
            
            LOGGER.info("Territorial configuration loaded successfully");
            logConfigurationSummary();
            
        } catch (IOException e) {
            LOGGER.error("I/O error while loading Territorial configuration: {}", e.getMessage(), e);
            LOGGER.warn("Using default settings (all events allowed)");
        } catch (Exception e) {
            LOGGER.error("Unexpected error while loading Territorial configuration", e);
            LOGGER.warn("Using default settings (all events allowed)");
        }
    }
    
    /**
     * Reloads configuration from disk.
     * Called when configuration hot-reload is triggered.
     */
    public static void reloadConfiguration() {
        LOGGER.info("Reloading Territorial configuration...");

        // Clear existing rules
        Config.setPlantGrowthRules(new ArrayList<>());
        Config.setAnimalBreedingRules(new ArrayList<>());

        // Clear the rule evaluation cache
        try {
            Class<?> managerClass = Class.forName("com.west3436.territorial.event.EventDecisionManager");
            managerClass.getMethod("clearCache").invoke(null);
            LOGGER.debug("Rule evaluation cache cleared on config reload");
        } catch (Exception e) {
            LOGGER.debug("Could not clear cache (EventDecisionManager may not be loaded yet)");
        }
        
        // Load fresh configuration
        loadConfiguration();
        
        // Refresh Dynmap markers if available
        try {
            Class<?> integrationManager = Class.forName("com.west3436.territorial.integration.ModIntegrationManager");
            integrationManager.getMethod("refreshDynmapMarkers").invoke(null);
            LOGGER.debug("Dynmap markers refreshed after config reload");
        } catch (Exception e) {
            LOGGER.debug("Could not refresh Dynmap markers (integration may not be available)");
        }
    }
    
    /**
     * Gets the path to the configuration file.
     */
    private static Path getConfigPath() {
        return Paths.get(CONFIG_DIRECTORY, CONFIG_FILE_NAME);
    }
    
    /**
     * Creates a default configuration file with examples.
     * 
     * <p>The default configuration includes:</p>
     * <ul>
     *   <li>Comprehensive comments explaining all configuration options</li>
     *   <li>Example rules for common scenarios</li>
     *   <li>Documentation of supported patterns and wildcards</li>
     * </ul>
     * 
     * @param configPath the path where the configuration file should be created
     * @throws IOException if file creation fails
     */
    private static void createDefaultConfiguration(Path configPath) throws IOException {
        LOGGER.info("Creating default Territorial configuration at: {}", configPath);
        
        // Ensure config directory exists
        Path parentDir = configPath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
            LOGGER.debug("Created config directory: {}", parentDir);
        }
        
        // Create default config content
        String defaultConfig = generateDefaultConfigContent();
        
        // Write to file
        Files.writeString(configPath, defaultConfig);
        
        LOGGER.info("Default configuration created successfully with {} bytes", defaultConfig.length());
    }
    
    /**
     * Generates default configuration file content with comprehensive examples.
     * 
     * @return TOML formatted configuration content
     */
    private static String generateDefaultConfigContent() {
        return """
                # ================================================================================
                # Territorial Mod - Event Control Configuration
                # ================================================================================
                # 
                # This file allows you to configure where certain game events can occur based on:
                # - Biome types (exact ID, namespace wildcards, or tags)
                # - Temperature ranges (hot, cold, temperate climates)
                # - Coordinate ranges (X/Y/Z boundaries)
                # - Dimensions (overworld, nether, end, or custom dimensions)
                #
                # By default (with no rules configured), ALL events are ALLOWED everywhere.
                #
                # ================================================================================
                # Configuration Format (TOML)
                # ================================================================================
                #
                # [[plant_growth.rules]]
                #     enabled = true                              # Rule is active
                #     biomes = ["minecraft:desert"]               # List of biome IDs
                #     temperature_min = 0.5                       # Optional: min temperature
                #     temperature_max = 2.0                       # Optional: max temperature
                #     x_min = -1000                               # Optional: X coordinate range
                #     x_max = 1000
                #     y_min = 60                                  # Optional: Y coordinate range
                #     y_max = 120
                #     z_min = -1000                               # Optional: Z coordinate range
                #     z_max = 1000
                #     dimension = "minecraft:overworld"           # Optional: specific dimension
                #     crop_types = ["minecraft:wheat"]            # Optional: specific crop types
                #     allow = false                               # false = block, true = allow
                #
                # [[animal_breeding.rules]]
                #     enabled = true
                #     biomes = ["minecraft:plains"]
                #     animal_types = ["minecraft:cow", "minecraft:sheep"]
                #     allow = true
                #
                # ================================================================================
                # Available Configuration Fields
                # ================================================================================
                #
                # Common to all rule types:
                #   enabled          - true/false (whether this rule is active)
                #   allow            - true (allow event) / false (block event)
                #   biomes           - list of biome IDs or patterns (see patterns below)
                #   temperature_min  - minimum biome temperature (range: -2.0 to 2.0)
                #   temperature_max  - maximum biome temperature (range: -2.0 to 2.0)
                #   x_min, x_max     - X coordinate range (inclusive)
                #   y_min, y_max     - Y coordinate range (inclusive)
                #   z_min, z_max     - Z coordinate range (inclusive)
                #   dimension        - dimension ID (e.g., "minecraft:overworld")
                #
                # Plant Growth specific:
                #   crop_types       - list of crop/plant block IDs
                #
                # Animal Breeding specific:
                #   animal_types     - list of entity type IDs
                #
                # ================================================================================
                # Supported Patterns and Wildcards
                # ================================================================================
                #
                # Biome Patterns:
                #   "minecraft:desert"        - Exact biome ID
                #   "minecraft:*"             - All biomes from minecraft namespace
                #   "*"                       - All biomes (global wildcard)
                #   "#minecraft:is_hot"       - Biome tag (all hot biomes)
                #   "#forge:is_desert"        - Forge biome tag
                #
                # Entity/Crop Patterns:
                #   "minecraft:wheat"         - Exact crop/entity type
                #   "minecraft:*"             - All minecraft crops/entities
                #   "*"                       - All types
                #
                # Temperature Values (Vanilla Minecraft):
                #   -0.5 to 0.0  - Frozen/Snowy biomes
                #    0.0 to 0.3  - Cold biomes
                #    0.3 to 0.8  - Temperate biomes
                #    0.8 to 1.5  - Warm biomes
                #    1.5 to 2.0  - Hot biomes (desert, badlands)
                #
                # ================================================================================
                # Rule Matching Priority
                # ================================================================================
                #
                # When multiple rules match the same event, the MOST SPECIFIC rule wins:
                #   1. Coordinate-based rules (highest priority)
                #   2. Temperature-based rules
                #   3. Biome-based rules
                #   4. Global rules (lowest priority)
                #
                # If no rules match, the event is ALLOWED by default.
                #
                # ================================================================================
                # Example Scenarios
                # ================================================================================
                
                # ---- Example 1: Realistic Farming - Prevent wheat in hot biomes ----
                # Wheat can only grow in temperate climates
                #
                # [[plant_growth.rules]]
                #     enabled = true
                #     temperature_min = 1.5
                #     crop_types = ["minecraft:wheat"]
                #     allow = false
                
                # ---- Example 2: Protected Spawn Area - No farming or breeding ----
                # Create a 500 block radius around spawn with no resource gathering
                #
                # [[plant_growth.rules]]
                #     enabled = true
                #     x_min = -500
                #     x_max = 500
                #     z_min = -500
                #     z_max = 500
                #     allow = false
                #
                # [[animal_breeding.rules]]
                #     enabled = true
                #     x_min = -500
                #     x_max = 500
                #     z_min = -500
                #     z_max = 500
                #     allow = false
                
                # ---- Example 3: Regional Specialization - Desert farming only ----
                # Only allow farming in desert biomes (create scarcity)
                #
                # [[plant_growth.rules]]
                #     enabled = true
                #     biomes = ["minecraft:desert"]
                #     allow = true
                #
                # [[plant_growth.rules]]
                #     enabled = true
                #     allow = false

                # ---- Example 4: High Altitude Restriction - No breeding above Y=150 ----
                # Prevent animal breeding in high mountains
                #
                # [[animal_breeding.rules]]
                #     enabled = true
                #     y_min = 150
                #     allow = false

                # ---- Example 5: Nether Farming - Allow nether wart only ----
                # Only nether wart can grow in the nether dimension
                #
                # [[plant_growth.rules]]
                #     enabled = true
                #     dimension = "minecraft:the_nether"
                #     crop_types = ["minecraft:nether_wart"]
                #     allow = true
                #
                # [[plant_growth.rules]]
                #     enabled = true
                #     dimension = "minecraft:the_nether"
                #     allow = false
                
                # ================================================================================
                # Add Your Custom Rules Below
                # ================================================================================
                # 
                # Uncomment and modify the examples above, or create your own rules.
                # 
                # Need help? See the full documentation at:
                # - USER_GUIDE.md
                # - DEVELOPER_GUIDE.md
                #
                # Use /territorial commands in-game for testing:
                # - /territorial check          - Check current location
                # - /territorial reload         - Reload this configuration
                # - /territorial rules all      - List all loaded rules
                # - /territorial debug on       - Enable debug logging
                #
                # ================================================================================
                
                """;
    }
    
    /**
     * Parses the configuration file and loads rules.
     *
     * Uses Night-Config library (already available through Forge) to parse
     * the TOML configuration file and populate the rule lists.
     */
    private static void parseConfiguration(Path configPath) throws IOException {
        if (!Files.exists(configPath)) {
            LOGGER.warn("Configuration file not found: {}", configPath);
            return;
        }

        LOGGER.debug("Parsing configuration file: {}", configPath);

        List<PlantGrowthConfig> plantRules = new ArrayList<>();
        List<AnimalBreedingConfig> animalRules = new ArrayList<>();

        try (FileConfig fileConfig = FileConfig.of(configPath)) {
            fileConfig.load();

            // Parse plant_growth.rules section
            List<CommentedConfig> plantGrowthRules = fileConfig.get("plant_growth.rules");
            if (plantGrowthRules != null) {
                for (CommentedConfig ruleConfig : plantGrowthRules) {
                    PlantGrowthConfig rule = parsePlantGrowthRule(ruleConfig);
                    if (rule != null && validateRule(rule)) {
                        plantRules.add(rule);
                    }
                }
            }

            // Parse animal_breeding.rules section
            List<CommentedConfig> animalBreedingRules = fileConfig.get("animal_breeding.rules");
            if (animalBreedingRules != null) {
                for (CommentedConfig ruleConfig : animalBreedingRules) {
                    AnimalBreedingConfig rule = parseAnimalBreedingRule(ruleConfig);
                    if (rule != null && validateRule(rule)) {
                        animalRules.add(rule);
                    }
                }
            }

            LOGGER.debug("Parsed {} plant growth rules, {} animal breeding rules",
                    plantRules.size(), animalRules.size());

        } catch (Exception e) {
            LOGGER.error("Error parsing configuration file, falling back to default settings (allow all): {}", e.getMessage(), e);
        }

        Config.setPlantGrowthRules(plantRules);
        Config.setAnimalBreedingRules(animalRules);
    }
    
    /**
     * Parses a plant growth rule from the TOML configuration.
     */
    private static PlantGrowthConfig parsePlantGrowthRule(CommentedConfig config) {
        try {
            PlantGrowthConfig rule = new PlantGrowthConfig();
            parseBaseRuleConfig(config, rule);
            
            // Parse crop_types
            List<String> cropTypes = config.get("crop_types");
            if (cropTypes != null) {
                rule.setCropTypes(new ArrayList<>(cropTypes));
            }
            
            return rule;
        } catch (Exception e) {
            LOGGER.warn("Error parsing plant growth rule: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Parses an animal breeding rule from the TOML configuration.
     */
    private static AnimalBreedingConfig parseAnimalBreedingRule(CommentedConfig config) {
        try {
            AnimalBreedingConfig rule = new AnimalBreedingConfig();
            parseBaseRuleConfig(config, rule);

            // Parse animal_types
            List<String> animalTypes = config.get("animal_types");
            if (animalTypes != null) {
                rule.setAnimalTypes(new ArrayList<>(animalTypes));
            }

            return rule;
        } catch (Exception e) {
            LOGGER.warn("Error parsing animal breeding rule: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parses the base rule configuration fields common to all rule types.
     */
    private static void parseBaseRuleConfig(CommentedConfig config, EventRuleConfig rule) {
        // Parse enabled (default: true)
        Boolean enabled = config.get("enabled");
        rule.setEnabled(enabled != null ? enabled : true);
        
        // Parse allow (default: true)
        Boolean allow = config.get("allow");
        rule.setAllow(allow != null ? allow : true);
        
        // Parse biomes
        List<String> biomes = config.get("biomes");
        if (biomes != null) {
            rule.setBiomes(new ArrayList<>(biomes));
        }
        
        // Parse temperature range
        Number tempMin = config.get("temperature_min");
        if (tempMin != null) {
            rule.setTemperatureMin(tempMin.floatValue());
        }
        
        Number tempMax = config.get("temperature_max");
        if (tempMax != null) {
            rule.setTemperatureMax(tempMax.floatValue());
        }
        
        // Parse coordinate ranges
        Number xMin = config.get("x_min");
        if (xMin != null) {
            rule.setXMin(xMin.intValue());
        }
        
        Number xMax = config.get("x_max");
        if (xMax != null) {
            rule.setXMax(xMax.intValue());
        }
        
        Number yMin = config.get("y_min");
        if (yMin != null) {
            rule.setYMin(yMin.intValue());
        }
        
        Number yMax = config.get("y_max");
        if (yMax != null) {
            rule.setYMax(yMax.intValue());
        }
        
        Number zMin = config.get("z_min");
        if (zMin != null) {
            rule.setZMin(zMin.intValue());
        }
        
        Number zMax = config.get("z_max");
        if (zMax != null) {
            rule.setZMax(zMax.intValue());
        }
        
        // Parse dimension (deprecated) and dimensions (new)
        String dimension = config.get("dimension");
        if (dimension != null) {
            rule.setDimension(dimension);
        }
        
        List<String> dimensions = config.get("dimensions");
        if (dimensions != null) {
            rule.setDimensions(new ArrayList<>(dimensions));
        }
        
        List<String> dimensionsBlacklist = config.get("dimensions_blacklist");
        if (dimensionsBlacklist != null) {
            rule.setDimensionsBlacklist(new ArrayList<>(dimensionsBlacklist));
        }
    }
    
    /**
     * Validates a loaded rule configuration.
     */
    private static boolean validateRule(EventRuleConfig rule) {
        if (!rule.validate()) {
            LOGGER.warn("Invalid rule configuration: {}", rule);
            invalidRuleCount.incrementAndGet();
            return false;
        }
        
        // Additional validation could be added here
        // For example, checking biome IDs against the registry
        
        return true;
    }
    
    /**
     * Gets the count of invalid rules detected during the last configuration load.
     * @return number of invalid rules
     */
    public static int getInvalidRuleCount() {
        return invalidRuleCount.get();
    }
    
    /**
     * Checks if there are any invalid rules.
     * @return true if invalid rules were detected
     */
    public static boolean hasInvalidRules() {
        return invalidRuleCount.get() > 0;
    }
    
    /**
     * Logs a summary of loaded configuration.
     */
    private static void logConfigurationSummary() {
        int plantRules = Config.getPlantGrowthRules().size();
        int animalRules = Config.getAnimalBreedingRules().size();

        LOGGER.info("Configuration Summary:");
        LOGGER.info("  Plant Growth Rules: {}", plantRules);
        LOGGER.info("  Animal Breeding Rules: {}", animalRules);

        if (plantRules == 0 && animalRules == 0) {
            LOGGER.info("  No rules configured - all events allowed by default");
        }
    }
}
