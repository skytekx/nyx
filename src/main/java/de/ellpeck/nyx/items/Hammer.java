package de.ellpeck.nyx.items;

import de.ellpeck.nyx.Nyx;
import de.ellpeck.nyx.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The meteor hammer: hold-and-release to launch yourself skyward, then slam down
 * dealing AoE damage on landing (the slam is handled in
 * {@link de.ellpeck.nyx.events.Events}). Also mines a 3x3 area.
 *
 * <p><b>Port note:</b> the 1.12 version reused Actually Additions' verbose
 * client/server block-break sync; this port uses {@code Level#destroyBlock} on
 * the server, which is the supported modern equivalent and avoids the fragile
 * hand-rolled packet dance.
 */
public class Hammer extends Pickaxe {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180);

    public Hammer(Tier material) {
        super(material, 7, -3.2F, new Item.Properties().durability(5500));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entityLiving, int timeLeft) {
        if (!entityLiving.onGround())
            return;
        int useTime = this.getUseDuration(stack) - timeLeft;
        if (useTime < 20)
            return;
        // we have to be looking up
        if (entityLiving.getXRot() > -10)
            return;
        float modifier = Mth.clamp((useTime - 20) / 5F, 1, 2.5F);
        Vec3 motion = entityLiving.getDeltaMovement();
        entityLiving.setDeltaMovement(
                motion.x - modifier * Mth.sin(entityLiving.getYRot() * DEG_TO_RAD),
                motion.y + 0.625 * modifier,
                motion.z + modifier * Mth.cos(entityLiving.getYRot() * DEG_TO_RAD));
        entityLiving.hasImpulse = true;
        entityLiving.getPersistentData().putLong(Nyx.ID + ":leap_start", level.getGameTime());

        if (!level.isClientSide) {
            level.playSound(null, entityLiving.blockPosition(), Registration.HAMMER_START_SOUND.get(), SoundSource.PLAYERS, 0.5F, 1);
            ((ServerLevel) level).sendParticles(ParticleTypes.FLAME, entityLiving.getX(), entityLiving.getY() + entityLiving.getEyeHeight(), entityLiving.getZ(), 30, 0.25, 0.25, 0.25, 0.05);
        }
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public boolean onBlockStartBreak(ItemStack itemstack, BlockPos pos, Player player) {
        if (player.isCrouching())
            return false;
        Level level = player.level();
        if (level.isClientSide)
            return true;
        BlockHitResult ray = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        Direction side = ray.getType() == HitResult.Type.BLOCK ? ray.getDirection() : Direction.UP;
        this.breakBlocks(itemstack, 1, level, pos, side, player);
        return true;
    }

    private void breakBlocks(ItemStack stack, int radius, Level level, BlockPos center, Direction side, Player player) {
        int xRange = radius;
        int yRange = radius;
        int zRange = 0;
        if (side.getAxis() == Direction.Axis.Y) {
            zRange = radius;
            yRange = 0;
        }
        if (side.getAxis() == Direction.Axis.X) {
            xRange = 0;
            zRange = radius;
        }

        float mainHardness = level.getBlockState(center).getDestroySpeed(level, center);
        // break the middle block
        if (!this.harvest(level, center, stack, player, false, mainHardness))
            return;

        if (mainHardness >= 0.2F) {
            for (int x = center.getX() - xRange; x <= center.getX() + xRange; x++) {
                for (int y = center.getY() - yRange; y <= center.getY() + yRange; y++) {
                    for (int z = center.getZ() - zRange; z <= center.getZ() + zRange; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (pos.equals(center))
                            continue;
                        this.harvest(level, pos, stack, player, true, mainHardness);
                    }
                }
            }
        }
    }

    private boolean harvest(Level level, BlockPos pos, ItemStack stack, Player player, boolean extra, float mainHardness) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir())
            return false;
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0)
            return false;
        if (extra) {
            if (hardness > mainHardness + 5.0F)
                return false;
            if (state.hasBlockEntity())
                return false;
            if (!player.hasCorrectToolForDrops(state) && this.getDestroySpeed(stack, state) <= 1.0F)
                return false;
        }
        if (!player.getAbilities().instabuild)
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        return level.destroyBlock(pos, !player.getAbilities().instabuild, player);
    }
}
