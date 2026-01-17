package com.west3436.territorial.config;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Configuration for animal breeding control rules.
 * Extends EventRuleConfig with animal-specific filtering.
 */
public class AnimalBreedingConfig extends EventRuleConfig {
    
    /**
     * List of animal types this rule applies to.
     * Supports exact IDs (e.g., "minecraft:cow", "minecraft:sheep")
     * Supports wildcards (e.g., "minecraft:*", "*" for all animals)
     * Empty list means applies to all animal types.
     */
    private List<String> animalTypes;
    
    public AnimalBreedingConfig() {
        super();
        this.animalTypes = Collections.emptyList();
    }
    
    public List<String> getAnimalTypes() {
        return animalTypes != null ? animalTypes : Collections.emptyList();
    }
    
    public void setAnimalTypes(List<String> animalTypes) {
        this.animalTypes = animalTypes;
    }
    
    @Override
    public int getSpecificity() {
        int score = super.getSpecificity();
        
        // Specific animal types are more specific than wildcard
        if (animalTypes != null && !animalTypes.isEmpty()) {
            boolean hasWildcard = animalTypes.stream().anyMatch(a -> a.equals("*") || a.endsWith(":*"));
            if (!hasWildcard) {
                score += 2; // Specific animal types
            } else {
                score += 1; // Wildcard animal types
            }
        }
        
        return score;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AnimalBreedingConfig that = (AnimalBreedingConfig) o;
        return Objects.equals(animalTypes, that.animalTypes);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), animalTypes);
    }
    
    @Override
    public String toString() {
        return "AnimalBreedingConfig{" +
                "animalTypes=" + animalTypes +
                ", " + super.toString() +
                '}';
    }
}
