package de.ellpeck.nyx.blocks;

import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Meteoric glass. Unlike vanilla glass it always drops itself (handled by its
 * loot table rather than a code override now).
 */
public class Glass extends GlassBlock {

    public Glass(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
