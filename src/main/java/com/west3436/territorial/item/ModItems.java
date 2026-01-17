package com.west3436.territorial.item;

import com.west3436.territorial.TerritorialMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for all items in the Territorial mod.
 */
public class ModItems {
    
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TerritorialMod.MODID);
    
    /**
     * The Farmer's Almanac - A book that displays all current Territorial rules
     * in an easy-to-understand format.
     */
    public static final RegistryObject<Item> FARMERS_ALMANAC = ITEMS.register("farmers_almanac",
            () -> new FarmersAlmanacItem(new Item.Properties()));
    
    /**
     * Register all items to the event bus.
     * Must be called during mod construction.
     */
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
