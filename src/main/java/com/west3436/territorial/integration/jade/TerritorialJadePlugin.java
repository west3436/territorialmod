package com.west3436.territorial.integration.jade;

import com.west3436.territorial.Config;
import com.west3436.territorial.TerritorialMod;
import com.west3436.territorial.event.EventDecisionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade plugin for Territorial mod integration.
 * Displays breeding/growth status when looking at mobs or plants.
 *
 * <p>This plugin is automatically discovered by Jade's plugin loading system
 * via the @WailaPlugin annotation.</p>
 */
@WailaPlugin
public class TerritorialJadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        if (!Config.enableJadeIntegration) {
            TerritorialMod.LOGGER.info("Jade integration is disabled in config");
            return;
        }

        // Register block provider for growable plants
        PlantGrowthProvider plantProvider = new PlantGrowthProvider();
        registration.registerBlockComponent(plantProvider, CropBlock.class);
        registration.registerBlockComponent(plantProvider, StemBlock.class);
        registration.registerBlockComponent(plantProvider, SweetBerryBushBlock.class);

        // Register entity provider for breedable animals
        AnimalBreedingProvider animalProvider = new AnimalBreedingProvider();
        registration.registerEntityComponent(animalProvider, Animal.class);

        TerritorialMod.LOGGER.info("Jade integration registered - plant growth and animal breeding providers active");
    }

    /**
     * Block component provider for plant growth status.
     * Shows whether plants can grow at the current location based on Territorial rules.
     */
    public static class PlantGrowthProvider implements IBlockComponentProvider {
        private static final ResourceLocation UID = new ResourceLocation(TerritorialMod.MODID, "plant_growth");

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!Config.enablePlantGrowthControl) {
                return; // Plant control disabled, don't show anything
            }

            Level level = accessor.getLevel();
            BlockPos pos = accessor.getPosition();
            BlockState blockState = accessor.getBlockState();

            if (level == null || pos == null || blockState == null) {
                return;
            }

            Block block = blockState.getBlock();

            // Check if this is a growable block
            if (!(block instanceof BonemealableBlock) &&
                !(block instanceof CropBlock) &&
                !(block instanceof StemBlock) &&
                !(block instanceof SweetBerryBushBlock)) {
                return;
            }

            // Get the block's registry name
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
            if (blockId == null) {
                return;
            }

            // Check if growth is allowed
            boolean canGrow = EventDecisionManager.canPlantGrow(level, pos, blockId);

            // Add tooltip text
            Component statusText;
            if (canGrow) {
                statusText = Component.translatable("territorial.jade.growth_allowed")
                    .withStyle(style -> style.withColor(0x55FF55)); // Green
            } else {
                statusText = Component.translatable("territorial.jade.growth_blocked")
                    .withStyle(style -> style.withColor(0xFF5555)); // Red
            }

            tooltip.add(statusText);
        }

        @Override
        public int getDefaultPriority() {
            return 1000; // Show after default info
        }
    }

    /**
     * Entity component provider for animal breeding status.
     * Shows whether animals can breed at the current location based on Territorial rules.
     */
    public static class AnimalBreedingProvider implements IEntityComponentProvider {
        private static final ResourceLocation UID = new ResourceLocation(TerritorialMod.MODID, "animal_breeding");

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (!Config.enableAnimalBreedingControl) {
                return; // Breeding control disabled, don't show anything
            }

            Level level = accessor.getLevel();
            Entity entity = accessor.getEntity();

            if (level == null || entity == null) {
                return;
            }

            // Only show for animals that can breed
            if (!(entity instanceof Animal)) {
                return;
            }

            // Get the entity's registry name
            ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (entityId == null) {
                return;
            }

            BlockPos pos = entity.blockPosition();

            // Check if breeding is allowed
            boolean canBreed = EventDecisionManager.canAnimalBreed(level, pos, entityId);

            // Add tooltip text
            Component statusText;
            if (canBreed) {
                statusText = Component.translatable("territorial.jade.breeding_allowed")
                    .withStyle(style -> style.withColor(0x55FF55)); // Green
            } else {
                statusText = Component.translatable("territorial.jade.breeding_blocked")
                    .withStyle(style -> style.withColor(0xFF5555)); // Red
            }

            tooltip.add(statusText);
        }

        @Override
        public int getDefaultPriority() {
            return 1000; // Show after default info
        }
    }
}