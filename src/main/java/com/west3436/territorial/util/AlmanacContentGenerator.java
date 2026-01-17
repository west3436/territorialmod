package com.west3436.territorial.util;

import com.west3436.territorial.Config;
import com.west3436.territorial.config.AnimalBreedingConfig;
import com.west3436.territorial.config.EventRuleConfig;
import com.west3436.territorial.config.PlantGrowthConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Utility class for generating the Farmer's Almanac book content.
 * Converts technical rule configurations into human-readable text.
 */
public class AlmanacContentGenerator {
    
    private static final int MAX_PAGE_LENGTH = 256; // Minecraft's page character limit
    private static final String TITLE = "Farmer's Almanac";
    private static final String AUTHOR = "Territorial Mod";
    
    /**
     * Generates a written book ItemStack containing all current Territorial rules.
     */
    public static ItemStack generateAlmanacBook(ServerLevel level) {
        List<String> pages = new ArrayList<>();
        
        // Title page
        pages.add(formatTitlePage());
        
        // Plant growth rules
        List<PlantGrowthConfig> plantRules = Config.getPlantGrowthRules();
        if (!plantRules.isEmpty()) {
            pages.addAll(formatPlantGrowthRules(plantRules, level));
        }
        
        // Animal breeding rules
        List<AnimalBreedingConfig> animalRules = Config.getAnimalBreedingRules();
        if (!animalRules.isEmpty()) {
            pages.addAll(formatAnimalBreedingRules(animalRules, level));
        }
        
        // No rules page
        if (pages.size() == 1) {
            pages.add("No territorial rules are currently configured.\n\nAll crops can grow and animals can breed anywhere.");
        }
        
        return createBookStack(pages);
    }
    
    private static String formatTitlePage() {
        return ChatFormatting.BOLD + TITLE + ChatFormatting.RESET + "\n\n" +
               "A comprehensive guide to the territorial rules governing this world.\n\n" +
               "This book updates automatically with the current rules.";
    }
    
    private static List<String> formatPlantGrowthRules(List<PlantGrowthConfig> rules, ServerLevel level) {
        List<String> pages = new ArrayList<>();
        StringBuilder currentPage = new StringBuilder();
        
        currentPage.append(ChatFormatting.BOLD).append("Plant Growth Rules").append(ChatFormatting.RESET).append("\n\n");
        
        int ruleNumber = 1;
        for (PlantGrowthConfig rule : rules) {
            if (!rule.isEnabled()) continue;
            
            String ruleText = formatPlantRule(rule, ruleNumber++, level);
            
            // Check if adding this rule would exceed page limit
            if (currentPage.length() + ruleText.length() > MAX_PAGE_LENGTH) {
                pages.add(currentPage.toString());
                currentPage = new StringBuilder();
            }
            
            currentPage.append(ruleText).append("\n");
        }
        
        if (currentPage.length() > 0) {
            pages.add(currentPage.toString());
        }
        
        return pages;
    }
    
    private static String formatPlantRule(PlantGrowthConfig rule, int number, ServerLevel level) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(ChatFormatting.UNDERLINE).append("Rule ").append(number).append(ChatFormatting.RESET).append("\n");
        
        // Crops
        List<String> crops = rule.getCropTypes();
        if (!crops.isEmpty() && !crops.contains("*")) {
            sb.append(ChatFormatting.GREEN).append("Crops: ").append(ChatFormatting.RESET);
            sb.append(formatCropNames(crops)).append("\n");
        } else {
            sb.append(ChatFormatting.GREEN).append("All crops\n").append(ChatFormatting.RESET);
        }
        
        // Action
        sb.append(rule.isAllow() ? ChatFormatting.GREEN + "ALLOWED" : ChatFormatting.RED + "BLOCKED");
        sb.append(ChatFormatting.RESET).append(" when:\n");
        
        // Conditions
        sb.append(formatConditions(rule, level));
        
        return sb.toString();
    }
    
    private static List<String> formatAnimalBreedingRules(List<AnimalBreedingConfig> rules, ServerLevel level) {
        List<String> pages = new ArrayList<>();
        StringBuilder currentPage = new StringBuilder();
        
        currentPage.append(ChatFormatting.BOLD).append("Animal Breeding Rules").append(ChatFormatting.RESET).append("\n\n");
        
        int ruleNumber = 1;
        for (AnimalBreedingConfig rule : rules) {
            if (!rule.isEnabled()) continue;
            
            String ruleText = formatAnimalRule(rule, ruleNumber++, level);
            
            // Check if adding this rule would exceed page limit
            if (currentPage.length() + ruleText.length() > MAX_PAGE_LENGTH) {
                pages.add(currentPage.toString());
                currentPage = new StringBuilder();
            }
            
            currentPage.append(ruleText).append("\n");
        }
        
        if (currentPage.length() > 0) {
            pages.add(currentPage.toString());
        }
        
        return pages;
    }
    
    private static String formatAnimalRule(AnimalBreedingConfig rule, int number, ServerLevel level) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(ChatFormatting.UNDERLINE).append("Rule ").append(number).append(ChatFormatting.RESET).append("\n");
        
        // Animals
        List<String> animals = rule.getAnimalTypes();
        if (!animals.isEmpty() && !animals.contains("*")) {
            sb.append(ChatFormatting.GOLD).append("Animals: ").append(ChatFormatting.RESET);
            sb.append(formatAnimalNames(animals)).append("\n");
        } else {
            sb.append(ChatFormatting.GOLD).append("All animals\n").append(ChatFormatting.RESET);
        }
        
        // Action
        sb.append(rule.isAllow() ? ChatFormatting.GREEN + "ALLOWED" : ChatFormatting.RED + "BLOCKED");
        sb.append(ChatFormatting.RESET).append(" when:\n");
        
        // Conditions
        sb.append(formatConditions(rule, level));
        
        return sb.toString();
    }
    
    private static String formatConditions(EventRuleConfig rule, ServerLevel level) {
        StringBuilder sb = new StringBuilder();
        boolean hasCondition = false;
        
        // Biomes
        List<String> biomes = rule.getBiomes();
        if (!biomes.isEmpty()) {
            sb.append("• In biomes: ");
            sb.append(formatBiomeNames(biomes, level)).append("\n");
            hasCondition = true;
        }
        
        // Temperature
        Float tempMin = rule.getTemperatureMin();
        Float tempMax = rule.getTemperatureMax();
        if (tempMin != null || tempMax != null) {
            sb.append("• Temperature: ");
            if (tempMin != null && tempMax != null) {
                sb.append(tempMin).append(" to ").append(tempMax);
            } else if (tempMin != null) {
                sb.append("at least ").append(tempMin);
            } else {
                sb.append("at most ").append(tempMax);
            }
            sb.append("\n");
            hasCondition = true;
        }
        
        // Coordinates
        if (rule.getXMin() != null || rule.getXMax() != null) {
            sb.append("• X: ").append(formatCoordRange(rule.getXMin(), rule.getXMax())).append("\n");
            hasCondition = true;
        }
        if (rule.getYMin() != null || rule.getYMax() != null) {
            sb.append("• Y: ").append(formatCoordRange(rule.getYMin(), rule.getYMax())).append("\n");
            hasCondition = true;
        }
        if (rule.getZMin() != null || rule.getZMax() != null) {
            sb.append("• Z: ").append(formatCoordRange(rule.getZMin(), rule.getZMax())).append("\n");
            hasCondition = true;
        }
        
        // Dimensions
        List<String> dimensions = rule.getDimensions();
        if (!dimensions.isEmpty()) {
            sb.append("• In dimensions: ");
            sb.append(formatDimensionNames(dimensions)).append("\n");
            hasCondition = true;
        }
        
        List<String> blacklist = rule.getDimensionsBlacklist();
        if (!blacklist.isEmpty()) {
            sb.append("• Not in dimensions: ");
            sb.append(formatDimensionNames(blacklist)).append("\n");
            hasCondition = true;
        }
        
        if (!hasCondition) {
            sb.append("• Anywhere\n");
        }
        
        return sb.toString();
    }
    
    private static String formatCropNames(List<String> cropIds) {
        return formatResourceIds(cropIds, false);
    }
    
    private static String formatAnimalNames(List<String> animalIds) {
        return formatResourceIds(animalIds, false);
    }
    
    /**
     * Formats a list of resource IDs into human-readable names.
     * @param ids The resource IDs to format
     * @param replaceUnderscores Whether to replace underscores with spaces
     * @return Comma-separated list of formatted names
     */
    private static String formatResourceIds(List<String> ids, boolean replaceUnderscores) {
        List<String> names = new ArrayList<>();
        for (String id : ids) {
            String formatted;
            if (id.contains(":")) {
                String[] parts = id.split(":");
                if (parts.length == 2) {
                    formatted = parts[1];
                } else {
                    formatted = id;
                }
            } else {
                formatted = id;
            }
            
            if (replaceUnderscores) {
                formatted = formatted.replace("_", " ");
            }
            names.add(capitalize(formatted));
        }
        return String.join(", ", names);
    }
    
    private static String formatBiomeNames(List<String> biomeIds, ServerLevel level) {
        List<String> names = new ArrayList<>();
        for (String id : biomeIds) {
            try {
                ResourceLocation resourceLocation = new ResourceLocation(id);
                Optional<ResourceKey<Biome>> biomeKey = level.registryAccess()
                        .registry(Registries.BIOME)
                        .flatMap(registry -> {
                            Biome biome = registry.get(resourceLocation);
                            if (biome == null) {
                                return Optional.empty();
                            }
                            return registry.getResourceKey(biome);
                        });
                
                if (biomeKey.isPresent()) {
                    String biomeName = biomeKey.get().location().getPath();
                    names.add(capitalize(biomeName.replace("_", " ")));
                } else {
                    // Fallback to formatting the ID directly
                    names.add(formatResourceIds(Collections.singletonList(id), true));
                }
            } catch (Exception e) {
                // If parsing fails, use the raw ID
                names.add(id);
            }
        }
        return String.join(", ", names);
    }
    
    private static String formatDimensionNames(List<String> dimensionIds) {
        return formatResourceIds(dimensionIds, true);
    }
    
    private static String formatCoordRange(Integer min, Integer max) {
        if (min != null && max != null) {
            return min + " to " + max;
        } else if (min != null) {
            return "at least " + min;
        } else if (max != null) {
            return "at most " + max;
        }
        return "any";
    }
    
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        if (str.length() == 1) {
            return str.toUpperCase();
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
    
    private static ItemStack createBookStack(List<String> pages) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        
        // Set title and author
        tag.putString("title", TITLE);
        tag.putString("author", AUTHOR);
        tag.putBoolean("resolved", true);
        tag.putInt("generation", 0);
        
        // Add pages
        ListTag pagesTag = new ListTag();
        for (String pageContent : pages) {
            // Wrap each page in JSON text component format
            String jsonPage = Component.Serializer.toJson(Component.literal(pageContent));
            pagesTag.add(StringTag.valueOf(jsonPage));
        }
        tag.put("pages", pagesTag);
        
        return book;
    }
}
