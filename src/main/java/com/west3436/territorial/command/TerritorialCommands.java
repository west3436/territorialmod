package com.west3436.territorial.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.west3436.territorial.Config;
import com.west3436.territorial.TerritorialMod;
import com.west3436.territorial.config.AnimalBreedingConfig;
import com.west3436.territorial.config.ConfigLoader;
import com.west3436.territorial.config.PlantGrowthConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Command handler for Territorial mod configuration management.
 * Provides commands to reload configuration and list rules.
 */
@Mod.EventBusSubscriber(modid = TerritorialMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TerritorialCommands {
    
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("territorial")
            .then(Commands.literal("reload")
                .requires(source -> source.hasPermission(2))
                .executes(TerritorialCommands::reloadConfig))
            .then(Commands.literal("rules")
                .requires(source -> source.hasPermission(2))
                .executes(context -> listRules(context, "all"))
                .then(Commands.argument("type", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        builder.suggest("all");
                        builder.suggest("plant_growth");
                        builder.suggest("animal_breeding");
                        return builder.buildFuture();
                    })
                    .executes(context -> listRules(context, 
                        StringArgumentType.getString(context, "type"))))));
    }
    
    /**
     * Reloads the configuration from disk.
     */
    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            source.sendSuccess(() -> Component.literal("Reloading Territorial configuration..."), true);
            
            ConfigLoader.reloadConfiguration();
            
            int plantRules = Config.getPlantGrowthRules().size();
            int animalRules = Config.getAnimalBreedingRules().size();
            
            source.sendSuccess(() -> Component.literal("§aConfiguration reloaded successfully!§r"), true);
            source.sendSuccess(() -> Component.literal("Loaded rules:"), false);
            source.sendSuccess(() -> Component.literal("  Plant Growth: " + plantRules), false);
            source.sendSuccess(() -> Component.literal("  Animal Breeding: " + animalRules), false);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cFailed to reload configuration: " + e.getMessage() + "§r"));
            TerritorialMod.LOGGER.error("Error in reload command", e);
            return 0;
        }
    }
    
    /**
     * Lists configured rules.
     */
    private static int listRules(CommandContext<CommandSourceStack> context, String type) {
        CommandSourceStack source = context.getSource();
        
        try {
            source.sendSuccess(() -> Component.literal("=== Territorial Rules ==="), false);
            
            if (type.equals("all") || type.equals("plant_growth")) {
                listPlantGrowthRules(source);
            }
            
            if (type.equals("all") || type.equals("animal_breeding")) {
                listAnimalBreedingRules(source);
            }
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error listing rules: " + e.getMessage()));
            TerritorialMod.LOGGER.error("Error in rules command", e);
            return 0;
        }
    }
    
    /**
     * Lists plant growth rules.
     */
    private static void listPlantGrowthRules(CommandSourceStack source) {
        var rules = Config.getPlantGrowthRules();
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("Plant Growth Rules: " + rules.size()), false);
        
        if (rules.isEmpty()) {
            source.sendSuccess(() -> Component.literal("  (no rules configured - all growth allowed)"), false);
            return;
        }
        
        int index = 1;
        for (PlantGrowthConfig rule : rules) {
            final int ruleNum = index++;
            source.sendSuccess(() -> Component.literal("  Rule " + ruleNum + ": " + 
                (rule.isEnabled() ? "§aENABLED§r" : "§7DISABLED§r") + " - " +
                (rule.isAllow() ? "ALLOW" : "DENY")), false);
            
            if (rule.getCropTypes() != null && !rule.getCropTypes().isEmpty()) {
                source.sendSuccess(() -> Component.literal("    Crops: " + String.join(", ", rule.getCropTypes())), false);
            }
            if (rule.getBiomes() != null && !rule.getBiomes().isEmpty()) {
                source.sendSuccess(() -> Component.literal("    Biomes: " + String.join(", ", rule.getBiomes())), false);
            }
        }
    }
    
    /**
     * Lists animal breeding rules.
     */
    private static void listAnimalBreedingRules(CommandSourceStack source) {
        var rules = Config.getAnimalBreedingRules();
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("Animal Breeding Rules: " + rules.size()), false);
        
        if (rules.isEmpty()) {
            source.sendSuccess(() -> Component.literal("  (no rules configured - all breeding allowed)"), false);
            return;
        }
        
        int index = 1;
        for (AnimalBreedingConfig rule : rules) {
            final int ruleNum = index++;
            source.sendSuccess(() -> Component.literal("  Rule " + ruleNum + ": " + 
                (rule.isEnabled() ? "§aENABLED§r" : "§7DISABLED§r") + " - " +
                (rule.isAllow() ? "ALLOW" : "DENY")), false);
            
            if (rule.getAnimalTypes() != null && !rule.getAnimalTypes().isEmpty()) {
                source.sendSuccess(() -> Component.literal("    Animals: " + String.join(", ", rule.getAnimalTypes())), false);
            }
            if (rule.getBiomes() != null && !rule.getBiomes().isEmpty()) {
                source.sendSuccess(() -> Component.literal("    Biomes: " + String.join(", ", rule.getBiomes())), false);
            }
        }
    }
}
