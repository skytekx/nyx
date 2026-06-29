package de.ellpeck.nyx.blocks;

import de.ellpeck.nyx.Registration;
import de.ellpeck.nyx.items.LunarWaterBottle;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Map;

/**
 * A cauldron filled with lunar water (levels 1-3). Bottling or bucketing it
 * yields lunar water; standing in it extinguishes fire and applies the lunar
 * water effect. Ported from a {@code BlockCauldron} subclass to a
 * {@link LayeredCauldronBlock} (the post-1.17 cauldron model).
 */
public class LunarWaterCauldron extends LayeredCauldronBlock {

    public LunarWaterCauldron(BlockBehaviour.Properties properties) {
        super(properties, p -> false, Map.of());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty())
            return InteractionResult.PASS;
        int fillLevel = state.getValue(LEVEL);

        if (stack.is(Items.BUCKET)) {
            if (fillLevel == 3 && !level.isClientSide) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    ItemStack bucket = new ItemStack(Registration.LUNAR_WATER_BUCKET.get());
                    if (stack.isEmpty()) {
                        player.setItemInHand(hand, bucket);
                    } else if (!player.getInventory().add(bucket)) {
                        player.drop(bucket, false);
                    }
                }
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1, 1);
                setLevel(level, pos, state, 0);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        } else if (stack.is(Items.GLASS_BOTTLE)) {
            if (fillLevel > 0 && !level.isClientSide) {
                if (!player.getAbilities().instabuild) {
                    player.awardStat(Stats.USE_CAULDRON);
                    stack.shrink(1);
                    ItemStack bottle = new ItemStack(Registration.LUNAR_WATER_BOTTLE.get());
                    if (stack.isEmpty()) {
                        player.setItemInHand(hand, bottle);
                    } else if (!player.getInventory().add(bottle)) {
                        player.drop(bottle, false);
                    }
                }
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1, 1);
                setLevel(level, pos, state, fillLevel - 1);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        int i = state.getValue(LEVEL);
        double f = pos.getY() + (6.0F + 3 * i) / 16.0D;
        if (!level.isClientSide && i > 0 && entity.getBoundingBox().minY <= f) {
            boolean did = false;
            if (entity.isOnFire()) {
                entity.clearFire();
                did = true;
            }
            if (entity instanceof LivingEntity living && LunarWaterBottle.applyLunarWater(living))
                did = true;
            if (did)
                setLevel(level, pos, state, i - 1);
        }
    }

    private static void setLevel(Level level, BlockPos pos, BlockState state, int newLevel) {
        if (newLevel <= 0) {
            level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
        } else {
            level.setBlockAndUpdate(pos, state.setValue(LEVEL, newLevel));
        }
    }
}
