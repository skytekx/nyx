package de.ellpeck.nyx.blocks;

import de.ellpeck.nyx.Nyx;
import de.ellpeck.nyx.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Invisible light-emitting "star air" placed under a landed fallen star. Its
 * block entity removes the block again once the fallen star item is gone.
 */
public class StarAir extends Block implements EntityBlock {

    public StarAir(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public boolean isAir(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tile(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide)
            return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof Tile tile)
                tile.tick();
        };
    }

    public static class Tile extends BlockEntity {

        public Tile(BlockPos pos, BlockState state) {
            super(Registration.STAR_AIR_BE.get(), pos, state);
        }

        public void tick() {
            if (this.level == null || this.level.isClientSide)
                return;
            AABB bounds = new AABB(this.worldPosition).inflate(1);
            List<ItemEntity> items = this.level.getEntitiesOfClass(ItemEntity.class, bounds,
                    e -> e.isAlive() && e.getPersistentData().getBoolean(Nyx.ID + ":fallen_star"));
            if (items.isEmpty())
                this.level.setBlockAndUpdate(this.worldPosition, Blocks.AIR.defaultBlockState());
        }
    }
}
