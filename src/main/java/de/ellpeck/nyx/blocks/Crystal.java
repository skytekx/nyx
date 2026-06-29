package de.ellpeck.nyx.blocks;

import de.ellpeck.nyx.Config;
import de.ellpeck.nyx.Registration;
import de.ellpeck.nyx.capabilities.NyxWorld;
import de.ellpeck.nyx.lunarevents.HarvestMoon;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.IPlantable;

import javax.annotation.Nullable;

/**
 * Gleaning crystal: a light source that accelerates the growth of nearby crops
 * (consuming durability), and slowly recharges during a harvest moon. Backed by
 * a {@link Tile} block entity (formerly a {@code TileEntity}).
 */
public class Crystal extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 12, 12);

    public Crystal() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .sound(SoundType.GLASS)
                .strength(3)
                .lightLevel(s -> 15)
                .noOcclusion()
                .randomTicks());
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof Tile crystal) || crystal.durability <= 0)
            return;
        int range = 5;
        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                for (int y = -1; y <= 1; y++) {
                    BlockPos offset = pos.offset(x, y, z);
                    BlockState offState = level.getBlockState(offset);
                    if (offState.getBlock() instanceof net.minecraft.world.level.block.BonemealableBlock
                            || offState.getBlock() instanceof IPlantable) {
                        for (int i = 0; i < 5; i++)
                            offState.randomTick(level, offset, random);
                        if (level.getBlockState(offset) != offState) {
                            crystal.durability--;
                            if (crystal.durability <= 0)
                                return;
                        }
                    }
                }
            }
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter source, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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

        public int durability;

        public Tile(BlockPos pos, BlockState state) {
            super(Registration.CRYSTAL_BE.get(), pos, state);
        }

        public void tick() {
            if (this.level != null && !this.level.isClientSide && this.level.getGameTime() % 600 == 0) {
                NyxWorld data = NyxWorld.get(this.level);
                if (data != null && data.currentEvent instanceof HarvestMoon && this.level.canSeeSky(this.worldPosition.above()))
                    this.durability = Math.min(Config.crystalDurability, this.durability + Config.crystalDurability / 10);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag compound) {
            compound.putInt("durability", this.durability);
            super.saveAdditional(compound);
        }

        @Override
        public void load(CompoundTag compound) {
            this.durability = compound.getInt("durability");
            super.load(compound);
        }
    }
}
