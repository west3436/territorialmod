package com.west3436.territorial;

import com.mojang.logging.LogUtils;
import com.west3436.territorial.config.ConfigLoader;
import com.west3436.territorial.integration.DynmapIntegration;
import com.west3436.territorial.integration.ModIntegrationManager;
import com.west3436.territorial.item.ModItems;
import com.west3436.territorial.network.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Main mod class for Territorial.
 *
 * <p>Territorial is a Minecraft Forge mod that allows control of game events
 * based on biome types, coordinates, and environmental conditions. The mod
 * can control:</p>
 * <ul>
 *   <li>Plant growth - Control where crops can grow</li>
 *   <li>Animal breeding - Manage where animals can breed</li>
 * </ul>
 *
 * <p>This class handles mod initialization, configuration loading, and
 * integration with other mods.</p>
 *
 * @author west3436
 * @version 1.0.0
 */
// The value here should match an entry in the META-INF/mods.toml file
@Mod(TerritorialMod.MODID)
public class TerritorialMod
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "territorial";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Main mod constructor.
     * 
     * <p>Initializes the mod by:</p>
     * <ol>
     *   <li>Registering for mod lifecycle events</li>
     *   <li>Registering for game events</li>
     *   <li>Registering the configuration spec</li>
     * </ol>
     */
    public TerritorialMod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register items
        ModItems.register(modEventBus);

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register creative tab contents
        modEventBus.addListener(this::addCreativeTabContents);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        
        LOGGER.info("Territorial mod constructor initialized");
    }

    /**
     * Common setup handler called during mod initialization.
     *
     * <p>This method is called on both client and server sides during the
     * FMLCommonSetupEvent phase. It loads the configuration rules and
     * initializes mod integrations.</p>
     *
     * @param event the common setup event
     */
    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("Starting Territorial mod common setup...");

        // Register networking
        ModNetworking.register();

        // Load configuration rules
        event.enqueueWork(() -> {
            try {
                LOGGER.info("Loading Territorial configuration rules...");
                ConfigLoader.loadConfiguration();
                LOGGER.info("Configuration rules loaded successfully");

                // Initialize mod integrations (Dynmap, etc.)
                LOGGER.info("Initializing mod integrations...");
                ModIntegrationManager.initialize();
                LOGGER.info("Mod integrations initialized");

            } catch (Exception e) {
                LOGGER.error("Error during Territorial setup - mod may not function correctly", e);
            }
        });

        LOGGER.info("Territorial mod initialized - ready to control events based on biome or coordinates");
    }

    /**
     * Adds mod items to the appropriate creative tabs.
     *
     * @param event the creative tab contents event
     */
    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.FARMERS_ALMANAC);
        }
    }

    /**
     * Handler for server starting event.
     * 
     * <p>Reloads configuration when the server starts, in case it was
     * modified while the server was stopped.</p>
     * 
     * @param event the server starting event
     */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("Territorial mod starting on server - reloading configuration...");
        
        try {
            // Get the server's level name for Dynmap world name resolution
            MinecraftServer server = event.getServer();
            if (server.getWorldData() != null) {
                String levelName = server.getWorldData().getLevelName();
                if (levelName != null && !levelName.isEmpty()) {
                    DynmapIntegration.setServerLevelName(levelName);
                }
            }
            
            // Reload configuration on server start (in case it changed)
            ConfigLoader.reloadConfiguration();
            LOGGER.info("Configuration reloaded successfully on server start");
        } catch (Exception e) {
            LOGGER.error("Error reloading configuration on server start", e);
        }
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("Territorial mod client setup");
            LOGGER.info("Player: {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
