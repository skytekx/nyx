package de.ellpeck.nyx.blocks;

import de.ellpeck.nyx.capabilities.NyxWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * Hot meteoric rock: burns entities walking on it, emits depth-suspend
 * particles, and keeps the world's meteor-landing-site set in sync when mined.
 * The two block variants (normal/gleaning) share this class and differ only by
 * their loot table.
 */
public class MeteorRock extends Block {

    public MeteorRock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .sound(SoundType.STONE)
                .strength(40, 3000)
                .requiresCorrectToolForDrops());
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            NyxWorld data = NyxWorld.get(level);
            if (data != null) {
                data.meteorLandingSites.remove(pos);
                data.sendToClients();
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!entity.fireImmune() && entity instanceof LivingEntity living
                && EnchantmentHelper.getEnchantmentLevel(Enchantments.FROST_WALKER, living) <= 0)
            entity.hurt(level.damageSources().hotFloor(), 1);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource rand) {
        for (int i = 0; i < 3; i++) {
            boolean side = rand.nextBoolean();
            float x = side ? rand.nextFloat() : (rand.nextBoolean() ? 1 : 0);
            float z = !side ? rand.nextFloat() : (rand.nextBoolean() ? 1 : 0);
            float y = rand.nextBoolean() ? 1 : 0;
            level.addParticle(ParticleTypes.UNDERWATER, pos.getX() + x, pos.getY() + y, pos.getZ() + z, 0, 0, 0);
        }
    }
}
