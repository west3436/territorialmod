package com.west3436.territorial.config;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Base configuration class for event rules.
 * Contains common fields for biome-based, coordinate-based, and temperature-based filtering.
 */
public class EventRuleConfig {
    
    /**
     * Whether this rule is enabled
     */
    private boolean enabled;
    
    /**
     * Whether to allow or deny the event when this rule matches
     */
    private boolean allow;
    
    /**
     * List of biome IDs that this rule applies to.
     * Supports exact matches (e.g., "minecraft:desert")
     * Can be empty to match all biomes.
     */
    private List<String> biomes;
    
    /**
     * Minimum temperature for biomes this rule applies to.
     * Vanilla range is typically -2.0 to 2.0.
     * Null means no minimum temperature restriction.
     */
    private Float temperatureMin;
    
    /**
     * Maximum temperature for biomes this rule applies to.
     * Vanilla range is typically -2.0 to 2.0.
     * Null means no maximum temperature restriction.
     */
    private Float temperatureMax;
    
    /**
     * Minimum X coordinate (inclusive). Null means unbounded.
     */
    private Integer xMin;
    
    /**
     * Maximum X coordinate (inclusive). Null means unbounded.
     */
    private Integer xMax;
    
    /**
     * Minimum Y coordinate (inclusive). Null means unbounded.
     */
    private Integer yMin;
    
    /**
     * Maximum Y coordinate (inclusive). Null means unbounded.
     */
    private Integer yMax;
    
    /**
     * Minimum Z coordinate (inclusive). Null means unbounded.
     */
    private Integer zMin;
    
    /**
     * Maximum Z coordinate (inclusive). Null means unbounded.
     */
    private Integer zMax;
    
    /**
     * List of dimension IDs this rule applies to (whitelist).
     * Supports exact dimension IDs (e.g., "minecraft:overworld", "minecraft:the_nether")
     * Empty or null means applies to all dimensions (unless dimensionsBlacklist is specified).
     * Cannot be used together with dimensionsBlacklist.
     */
    private List<String> dimensions;
    
    /**
     * List of dimension IDs this rule does NOT apply to (blacklist).
     * Supports exact dimension IDs (e.g., "minecraft:the_nether")
     * Empty or null means no dimensions are blacklisted.
     * Cannot be used together with dimensions.
     */
    private List<String> dimensionsBlacklist;
    
    /**
     * @deprecated Use {@link #dimensions} instead. Kept for backward compatibility.
     * Single dimension ID this rule applies to (e.g., "minecraft:overworld").
     * Will be automatically converted to dimensions list.
     */
    @Deprecated
    private String dimension;
    
    public EventRuleConfig() {
        this.enabled = true;
        this.allow = true;
        this.biomes = Collections.emptyList();
        this.dimensions = Collections.emptyList();
        this.dimensionsBlacklist = Collections.emptyList();
    }
    
    // Getters
    public boolean isEnabled() {
        return enabled;
    }
    
    public boolean isAllow() {
        return allow;
    }
    
    public List<String> getBiomes() {
        return biomes != null ? biomes : Collections.emptyList();
    }
    
    public Float getTemperatureMin() {
        return temperatureMin;
    }
    
    public Float getTemperatureMax() {
        return temperatureMax;
    }
    
    public Integer getXMin() {
        return xMin;
    }
    
    public Integer getXMax() {
        return xMax;
    }
    
    public Integer getYMin() {
        return yMin;
    }
    
    public Integer getYMax() {
        return yMax;
    }
    
    public Integer getZMin() {
        return zMin;
    }
    
    public Integer getZMax() {
        return zMax;
    }
    
    public String getDimension() {
        // Backward compatibility: return first dimension if dimensions list is not empty
        if (dimensions != null && !dimensions.isEmpty()) {
            return dimensions.get(0);
        }
        return dimension;
    }
    
    public List<String> getDimensions() {
        return dimensions != null ? dimensions : Collections.emptyList();
    }
    
    public List<String> getDimensionsBlacklist() {
        return dimensionsBlacklist != null ? dimensionsBlacklist : Collections.emptyList();
    }
    
    // Setters
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public void setAllow(boolean allow) {
        this.allow = allow;
    }
    
    public void setBiomes(List<String> biomes) {
        this.biomes = biomes;
    }
    
    public void setTemperatureMin(Float temperatureMin) {
        this.temperatureMin = temperatureMin;
    }
    
    public void setTemperatureMax(Float temperatureMax) {
        this.temperatureMax = temperatureMax;
    }
    
    public void setXMin(Integer xMin) {
        this.xMin = xMin;
    }
    
    public void setXMax(Integer xMax) {
        this.xMax = xMax;
    }
    
    public void setYMin(Integer yMin) {
        this.yMin = yMin;
    }
    
    public void setYMax(Integer yMax) {
        this.yMax = yMax;
    }
    
    public void setZMin(Integer zMin) {
        this.zMin = zMin;
    }
    
    public void setZMax(Integer zMax) {
        this.zMax = zMax;
    }
    
    /**
     * Sets the dimension field (deprecated, for backward compatibility).
     * This is called by TOML parser when the deprecated "dimension" field is used.
     * Automatically converts to the new dimensions list format.
     * 
     * @deprecated Use setDimensions() instead
     */
    @Deprecated
    public void setDimension(String dimension) {
        // Backward compatibility: convert single dimension to list
        if (dimension != null && !dimension.isEmpty()) {
            this.dimensions = Collections.singletonList(dimension);
        } else {
            // Clear dimensions when null/empty dimension is set
            this.dimensions = Collections.emptyList();
        }
        this.dimension = dimension;
    }
    
    /**
     * Sets the dimensions whitelist.
     * This is the preferred method for setting dimension filtering.
     * Clears the deprecated dimension field to maintain clean state.
     */
    public void setDimensions(List<String> dimensions) {
        this.dimensions = dimensions;
        // Always clear the deprecated field when using new field
        // This ensures only one dimension configuration method is active
        this.dimension = null;
    }
    
    public void setDimensionsBlacklist(List<String> dimensionsBlacklist) {
        this.dimensionsBlacklist = dimensionsBlacklist;
    }
    
    /**
     * Validates this configuration.
     * @return true if valid, false otherwise
     */
    public boolean validate() {
        // Validate temperature range
        if (temperatureMin != null && temperatureMax != null) {
            if (temperatureMin > temperatureMax) {
                return false;
            }
        }
        
        // Validate coordinate ranges
        if (xMin != null && xMax != null && xMin > xMax) {
            return false;
        }
        if (yMin != null && yMax != null && yMin > yMax) {
            return false;
        }
        if (zMin != null && zMax != null && zMin > zMax) {
            return false;
        }
        
        // Validate dimension fields - cannot use both whitelist and blacklist
        boolean hasDimensions = dimensions != null && !dimensions.isEmpty();
        boolean hasBlacklist = dimensionsBlacklist != null && !dimensionsBlacklist.isEmpty();
        if (hasDimensions && hasBlacklist) {
            return false; // Cannot use both whitelist and blacklist
        }
        
        return true;
    }
    
    /**
     * Calculates the specificity score of this rule.
     * Higher score means more specific rule (takes precedence).
     * @return specificity score
     */
    public int getSpecificity() {
        int score = 0;
        
        // Coordinate restrictions are most specific
        if (xMin != null || xMax != null) score += 10;
        if (yMin != null || yMax != null) score += 10;
        if (zMin != null || zMax != null) score += 10;
        
        // Temperature is more specific than just biome
        if (temperatureMin != null || temperatureMax != null) score += 5;
        
        // Biome restrictions
        if (biomes != null && !biomes.isEmpty()) score += 3;
        
        // Dimension restrictions
        if (dimensions != null && !dimensions.isEmpty()) {
            score += 2; // Whitelist is moderately specific
        } else if (dimensionsBlacklist != null && !dimensionsBlacklist.isEmpty()) {
            score += 1; // Blacklist is less specific than whitelist
        }
        
        return score;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventRuleConfig that = (EventRuleConfig) o;
        return enabled == that.enabled &&
                allow == that.allow &&
                Objects.equals(biomes, that.biomes) &&
                Objects.equals(temperatureMin, that.temperatureMin) &&
                Objects.equals(temperatureMax, that.temperatureMax) &&
                Objects.equals(xMin, that.xMin) &&
                Objects.equals(xMax, that.xMax) &&
                Objects.equals(yMin, that.yMin) &&
                Objects.equals(yMax, that.yMax) &&
                Objects.equals(zMin, that.zMin) &&
                Objects.equals(zMax, that.zMax) &&
                Objects.equals(dimensions, that.dimensions) &&
                Objects.equals(dimensionsBlacklist, that.dimensionsBlacklist);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(enabled, allow, biomes, temperatureMin, temperatureMax,
                xMin, xMax, yMin, yMax, zMin, zMax, dimensions, dimensionsBlacklist);
    }
    
    @Override
    public String toString() {
        return "EventRuleConfig{" +
                "enabled=" + enabled +
                ", allow=" + allow +
                ", biomes=" + biomes +
                ", temperatureMin=" + temperatureMin +
                ", temperatureMax=" + temperatureMax +
                ", xMin=" + xMin +
                ", xMax=" + xMax +
                ", yMin=" + yMin +
                ", yMax=" + yMax +
                ", zMin=" + zMin +
                ", zMax=" + zMax +
                ", dimensions=" + dimensions +
                ", dimensionsBlacklist=" + dimensionsBlacklist +
                '}';
    }
}
