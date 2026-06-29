package de.ellpeck.nyx.entities;

import de.ellpeck.nyx.Nyx;
import de.ellpeck.nyx.Registration;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class FallingStar extends Entity {

    protected float trajectoryX;
    protected float trajectoryY;
    protected float trajectoryZ;

    public FallingStar(EntityType<? extends FallingStar> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.initTrajectory(1);
    }

    @Override
    public void tick() {
        this.customUpdate();
        if (!this.level().isClientSide)
            this.move(MoverType.SELF, new Vec3(this.trajectoryX, this.trajectoryY, this.trajectoryZ));
        super.tick();
    }

    protected void customUpdate() {
        if (!this.isLoaded()) {
            this.discard();
            return;
        }

        if (!this.level().isClientSide) {
            if (this.collided()) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), Registration.FALLING_STAR_IMPACT_SOUND.get(), SoundSource.AMBIENT, 10, 1);

                ItemEntity item = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), new ItemStack(Registration.FALLEN_STAR.get()));
                item.getPersistentData().putBoolean(Nyx.ID + ":fallen_star", true);
                this.level().addFreshEntity(item);
                this.discard();
            } else {
                if (this.tickCount % 40 == 0)
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(), Registration.FALLING_STAR_SOUND.get(), SoundSource.AMBIENT, 5, 1);
            }
        } else {
            for (int i = 0; i < 2; i++) {
                Vec3 motion = this.getDeltaMovement();
                double mX = -motion.x + this.random.nextGaussian() * 0.05;
                double mY = -motion.y + this.random.nextGaussian() * 0.05;
                double mZ = -motion.z + this.random.nextGaussian() * 0.05;
                this.level().addAlwaysVisibleParticle(ParticleTypes.FIREWORK, this.getX(), this.getY(), this.getZ(), mX, mY, mZ);
            }
        }
    }

    protected boolean collided() {
        return this.horizontalCollision || this.verticalCollision || this.onGround();
    }

    public boolean isLoaded() {
        // entities are only updated if they're at least 32 blocks away from unloaded chunks
        return this.level().isAreaLoaded(this.blockPosition(), 34);
    }

    protected void initTrajectory(float multiplier) {
        this.trajectoryX = Mth.nextFloat(this.random, 0.5F, 1.25F) * multiplier;
        if (this.random.nextBoolean())
            this.trajectoryX *= -1;
        this.trajectoryY = Mth.nextFloat(this.random, -0.85F, -0.5F) * multiplier;
        this.trajectoryZ = Mth.nextFloat(this.random, 0.5F, 1.25F) * multiplier;
        if (this.random.nextBoolean())
            this.trajectoryZ *= -1;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        this.trajectoryX = compound.getFloat("trajectory_x");
        this.trajectoryY = compound.getFloat("trajectory_y");
        this.trajectoryZ = compound.getFloat("trajectory_z");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putFloat("trajectory_x", this.trajectoryX);
        compound.putFloat("trajectory_y", this.trajectoryY);
        compound.putFloat("trajectory_z", this.trajectoryZ);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
