package com.west3436.territorial.integration;

import com.west3436.territorial.TerritorialMod;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/**
 * Manages optional mod integrations for map visualization.
 * Detects which mods are loaded and initializes appropriate integrations.
 */
public class ModIntegrationManager {
    private static final Logger LOGGER = TerritorialMod.LOGGER;

    // Mod IDs for integration
    private static final String DYNMAP_MOD_ID = "dynmap";

    // Integration instances
    private static DynmapIntegration dynmapIntegration = null;

    // Integration status
    private static boolean dynmapAvailable = false;
    private static boolean initialized = false;

    /**
     * Initializes all available mod integrations.
     * Should be called during mod setup phase.
     */
    public static void initialize() {
        if (initialized) {
            LOGGER.warn("ModIntegrationManager already initialized, skipping");
            return;
        }

        LOGGER.info("Initializing mod integrations...");

        // Check for Dynmap
        if (ModList.get().isLoaded(DYNMAP_MOD_ID)) {
            try {
                dynmapIntegration = new DynmapIntegration();
                dynmapIntegration.initialize();
                dynmapAvailable = true;
                LOGGER.info("Dynmap integration initialized successfully");
            } catch (Exception e) {
                LOGGER.error("Failed to initialize Dynmap integration", e);
                dynmapAvailable = false;
            }
        } else {
            LOGGER.info("Dynmap not detected, skipping integration");
        }

        initialized = true;
        logIntegrationStatus();
    }

    /**
     * Logs the current status of all integrations.
     */
    private static void logIntegrationStatus() {
        StringBuilder status = new StringBuilder("Territorial integrations: ");
        status.append("Dynmap [").append(dynmapAvailable ? "ACTIVE" : "INACTIVE").append("]");
        LOGGER.info(status.toString());
    }

    /**
     * Checks if Dynmap integration is available.
     *
     * @return true if Dynmap is loaded and integration is active
     */
    public static boolean isDynmapAvailable() {
        return dynmapAvailable;
    }

    /**
     * Gets the Dynmap integration instance.
     *
     * @return the Dynmap integration, or null if not available
     */
    public static DynmapIntegration getDynmapIntegration() {
        return dynmapIntegration;
    }

    /**
     * Refreshes Dynmap markers when configuration changes.
     * Should be called after config reload.
     */
    public static void refreshDynmapMarkers() {
        if (dynmapAvailable && dynmapIntegration != null) {
            try {
                dynmapIntegration.updateMarkers();
                LOGGER.info("Dynmap markers refreshed");
            } catch (Exception e) {
                LOGGER.error("Failed to refresh Dynmap markers", e);
            }
        }
    }
}
