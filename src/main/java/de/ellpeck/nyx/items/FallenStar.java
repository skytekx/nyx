package de.ellpeck.nyx.items;

import de.ellpeck.nyx.Nyx;
import de.ellpeck.nyx.Registration;
import de.ellpeck.nyx.capabilities.NyxWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;

public class FallenStar extends Item {

    public FallenStar() {
        super(new Item.Properties());
    }

    @Override
    public boolean onEntityItemUpdate(net.minecraft.world.item.ItemStack stack, ItemEntity entityItem) {
        if (entityItem.level().isClientSide) {
            if (entityItem.level().random.nextFloat() >= 0.7F) {
                double mX = entityItem.level().random.nextGaussian() * 0.05;
                double mY = entityItem.level().random.nextFloat() * 0.4;
                double mZ = entityItem.level().random.nextGaussian() * 0.05;
                entityItem.level().addAlwaysVisibleParticle(ParticleTypes.FIREWORK, entityItem.getX(), entityItem.getY() + 0.5F, entityItem.getZ(), mX, mY, mZ);
            }
            return false;
        }

        // This tag is set to true only by stars spawned by falling
        if (!entityItem.getPersistentData().getBoolean(Nyx.ID + ":fallen_star"))
            return false;

        if (NyxWorld.isDaytime(entityItem.level())) {
            entityItem.discard();
            return true;
        }

        String lastOnGround = Nyx.ID + ":last_on_ground";
        if (entityItem.onGround() && !entityItem.getPersistentData().getBoolean(lastOnGround)) {
            entityItem.getPersistentData().putBoolean(lastOnGround, true);
            this.placeStarAir(entityItem);
        }
        return false;
    }

    private void placeStarAir(ItemEntity entityItem) {
        BlockPos pos = entityItem.blockPosition();
        if (entityItem.level().getBlockState(pos).canBeReplaced()) {
            entityItem.level().setBlockAndUpdate(pos, Registration.STAR_AIR.get().defaultBlockState());
            return;
        }

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos offset = pos.offset(x, y, z);
                    if (entityItem.level().getBlockState(offset).canBeReplaced()) {
                        entityItem.level().setBlockAndUpdate(offset, Registration.STAR_AIR.get().defaultBlockState());
                        return;
                    }
                }
            }
        }
    }
}
