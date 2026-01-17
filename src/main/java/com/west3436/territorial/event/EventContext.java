package com.west3436.territorial.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * Context information for event rule evaluation.
 * Contains all the data needed to determine if an event should be allowed.
 */
public class EventContext {
    private final Level level;
    private final BlockPos position;
    private final Holder<Biome> biome;
    private final ResourceLocation entityType;
    private final EventType eventType;
    
    /**
     * Creates a new event context.
     * 
     * @param level the level (world) where the event occurs
     * @param position the block position of the event
     * @param biome the biome holder at the event location
     * @param entityType the entity or block type involved (e.g., crop type, animal type)
     * @param eventType the type of event being evaluated
     */
    public EventContext(Level level, BlockPos position, Holder<Biome> biome, 
                       ResourceLocation entityType, EventType eventType) {
        this.level = level;
        this.position = position;
        this.biome = biome;
        this.entityType = entityType;
        this.eventType = eventType;
    }
    
    /**
     * Creates an event context for a plant growth event.
     */
    public static EventContext forPlantGrowth(Level level, BlockPos position, ResourceLocation cropType) {
        Holder<Biome> biome = level.getBiome(position);
        return new EventContext(level, position, biome, cropType, EventType.PLANT_GROWTH);
    }
    
    /**
     * Creates an event context for an animal breeding event.
     */
    public static EventContext forAnimalBreeding(Level level, BlockPos position, ResourceLocation animalType) {
        Holder<Biome> biome = level.getBiome(position);
        return new EventContext(level, position, biome, animalType, EventType.ANIMAL_BREEDING);
    }

    public Level getLevel() {
        return level;
    }
    
    public BlockPos getPosition() {
        return position;
    }
    
    public Holder<Biome> getBiome() {
        return biome;
    }
    
    public ResourceLocation getEntityType() {
        return entityType;
    }
    
    public EventType getEventType() {
        return eventType;
    }
    
    public ResourceKey<Level> getDimension() {
        return level.dimension();
    }
    
    @Override
    public String toString() {
        return "EventContext{" +
                "eventType=" + eventType +
                ", position=" + position +
                ", entityType=" + entityType +
                ", dimension=" + getDimension().location() +
                '}';
    }
}
