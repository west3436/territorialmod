package com.west3436.territorial.config;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Configuration for plant growth control rules.
 * Extends EventRuleConfig with crop-specific filtering.
 */
public class PlantGrowthConfig extends EventRuleConfig {
    
    /**
     * List of crop types this rule applies to.
     * Supports exact IDs (e.g., "minecraft:wheat", "minecraft:carrots")
     * Supports wildcards (e.g., "minecraft:*", "*" for all crops)
     * Empty list means applies to all crop types.
     */
    private List<String> cropTypes;
    
    public PlantGrowthConfig() {
        super();
        this.cropTypes = Collections.emptyList();
    }
    
    public List<String> getCropTypes() {
        return cropTypes != null ? cropTypes : Collections.emptyList();
    }
    
    public void setCropTypes(List<String> cropTypes) {
        this.cropTypes = cropTypes;
    }
    
    @Override
    public int getSpecificity() {
        int score = super.getSpecificity();
        
        // Specific crop types are more specific than wildcard
        if (cropTypes != null && !cropTypes.isEmpty()) {
            boolean hasWildcard = cropTypes.stream().anyMatch(c -> c.equals("*") || c.endsWith(":*"));
            if (!hasWildcard) {
                score += 2; // Specific crop types
            } else {
                score += 1; // Wildcard crop types
            }
        }
        
        return score;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PlantGrowthConfig that = (PlantGrowthConfig) o;
        return Objects.equals(cropTypes, that.cropTypes);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cropTypes);
    }
    
    @Override
    public String toString() {
        return "PlantGrowthConfig{" +
                "cropTypes=" + cropTypes +
                ", " + super.toString() +
                '}';
    }
}
