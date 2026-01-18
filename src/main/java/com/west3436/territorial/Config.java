package com.west3436.territorial;

import com.west3436.territorial.config.PlantGrowthConfig;
import com.west3436.territorial.config.AnimalBreedingConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Main configuration class for the Territorial mod.
 *
 * <p>Manages biome-based, coordinate-based, and temperature-based event control rules.
 * This class handles:</p>
 * <ul>
 *   <li>Global control toggles (enable/disable specific features)</li>
 *   <li>Rule storage and access (plant growth, animal breeding)</li>
 *   <li>Configuration synchronization from Forge's config system</li>
 *   <li>Thread-safe rule list management</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> Rule lists are synchronized for thread-safe access.
 * Multiple threads can safely read rules, and configuration reloads are atomic.</p>
 *
 * <p><b>Configuration Files:</b></p>
 * <ul>
 *   <li>{@code config/territorial-common.toml} - Forge config (global toggles)</li>
 *   <li>{@code config/territorial-rules.toml} - Event rules (biome/coordinate restrictions)</li>
 * </ul>
 *
 * @see com.west3436.territorial.config.ConfigLoader
 * @see com.west3436.territorial.config.EventRuleConfig
 */
@Mod.EventBusSubscriber(modid = TerritorialMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // Global toggles
    private static final ForgeConfigSpec.BooleanValue ENABLE_BIOME_CONTROL = BUILDER
            .comment("Whether to enable biome-based event control")
            .define("enableBiomeControl", true);

    private static final ForgeConfigSpec.BooleanValue ENABLE_COORDINATE_CONTROL = BUILDER
            .comment("Whether to enable coordinate-based event control")
            .define("enableCoordinateControl", true);

    private static final ForgeConfigSpec.BooleanValue ENABLE_PLANT_GROWTH_CONTROL = BUILDER
            .comment("Whether to enable plant growth control")
            .define("enablePlantGrowthControl", true);

    private static final ForgeConfigSpec.BooleanValue ENABLE_ANIMAL_BREEDING_CONTROL = BUILDER
            .comment("Whether to enable animal breeding control")
            .define("enableAnimalBreedingControl", true);

    private static final ForgeConfigSpec.BooleanValue ENABLE_DYNMAP_INTEGRATION = BUILDER
            .comment("Whether to enable Dynmap integration for visualizing rules on the map (requires Dynmap-Forge)")
            .define("enableDynmapIntegration", true);

    private static final ForgeConfigSpec.BooleanValue ENABLE_JADE_INTEGRATION = BUILDER
            .comment("Whether to enable Jade integration for showing breeding/growth status in tooltips (requires Jade)")
            .define("enableJadeIntegration", true);

    private static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("Enable debug logging for rule evaluation")
            .define("debugLogging", false);
    
    private static final ForgeConfigSpec.BooleanValue GIVE_ALMANAC_ON_FIRST_JOIN = BUILDER
            .comment("Whether to give new players a Farmer's Almanac when they first join the world")
            .define("giveAlmanacOnFirstJoin", false);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    // Runtime configuration values
    public static boolean enableBiomeControl;
    public static boolean enableCoordinateControl;
    public static boolean enablePlantGrowthControl;
    public static boolean enableAnimalBreedingControl;
    public static boolean enableDynmapIntegration;
    public static boolean enableJadeIntegration;
    public static boolean debugLogging;
    public static boolean giveAlmanacOnFirstJoin;


    // Rule lists - these will be populated from external configuration files
    private static List<PlantGrowthConfig> plantGrowthRules = Collections.synchronizedList(new ArrayList<>());
    private static List<AnimalBreedingConfig> animalBreedingRules = Collections.synchronizedList(new ArrayList<>());

    /**
     * Event handler called when configuration is loaded or reloaded.
     *
     * <p>This method synchronizes configuration values from Forge's config
     * system to the static fields used throughout the mod.</p>
     *
     * @param event the configuration event (loading or reloading)
     */
    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        enableBiomeControl = ENABLE_BIOME_CONTROL.get();
        enableCoordinateControl = ENABLE_COORDINATE_CONTROL.get();
        enablePlantGrowthControl = ENABLE_PLANT_GROWTH_CONTROL.get();
        enableAnimalBreedingControl = ENABLE_ANIMAL_BREEDING_CONTROL.get();
        enableDynmapIntegration = ENABLE_DYNMAP_INTEGRATION.get();
        enableJadeIntegration = ENABLE_JADE_INTEGRATION.get();
        debugLogging = DEBUG_LOGGING.get();
        giveAlmanacOnFirstJoin = GIVE_ALMANAC_ON_FIRST_JOIN.get();
    }

    /**
     * Gets the list of plant growth rules.
     * @return unmodifiable list of plant growth rules
     */
    public static List<PlantGrowthConfig> getPlantGrowthRules() {
        return Collections.unmodifiableList(plantGrowthRules);
    }

    /**
     * Gets the list of animal breeding rules.
     * @return unmodifiable list of animal breeding rules
     */
    public static List<AnimalBreedingConfig> getAnimalBreedingRules() {
        return Collections.unmodifiableList(animalBreedingRules);
    }

    /**
     * Sets the plant growth rules. Used by configuration loader.
     * @param rules new list of rules
     */
    public static void setPlantGrowthRules(List<PlantGrowthConfig> rules) {
        plantGrowthRules.clear();
        if (rules != null) {
            plantGrowthRules.addAll(rules);
        }
    }

    /**
     * Sets the animal breeding rules. Used by configuration loader.
     * @param rules new list of rules
     */
    public static void setAnimalBreedingRules(List<AnimalBreedingConfig> rules) {
        animalBreedingRules.clear();
        if (rules != null) {
            animalBreedingRules.addAll(rules);
        }
    }
}
