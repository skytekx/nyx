package de.ellpeck.nyx.entities;

import de.ellpeck.nyx.Nyx;
import de.ellpeck.nyx.Registration;
import de.ellpeck.nyx.capabilities.NyxWorld;
import de.ellpeck.nyx.lunarevents.HarvestMoon;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class FallingMeteor extends FallingStar {

    public static final EntityDataAccessor<Integer> SIZE = SynchedEntityData.defineId(FallingMeteor.class, EntityDataSerializers.INT);
    public boolean homing;
    public boolean disableMessage;
    public float speedModifier;
    public boolean spawnNoBlocks;

    public FallingMeteor(EntityType<? extends FallingMeteor> type, Level level) {
        super(type, level);
        this.entityData.set(SIZE, level.random.nextInt(3) + 1);
        this.speedModifier = 1;
        this.initTrajectory(2 * this.speedModifier);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SIZE, 1);
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        // init some stuff that /summon wouldn't have
        if (this.entityData.get(SIZE) <= 0)
            this.entityData.set(SIZE, 2);
        if (this.speedModifier <= 0)
            this.speedModifier = 1;
        if (this.trajectoryX == 0 && this.trajectoryY == 0 && this.trajectoryZ == 0)
            this.initTrajectory(2 * this.speedModifier);
    }

    @Override
    public void moveTo(double x, double y, double z, float yaw, float pitch) {
        // homing meteors should spawn higher up
        if (this.homing)
            y += 48;
        super.moveTo(x, y, z, yaw, pitch);
    }

    @Override
    protected void customUpdate() {
        if (!this.level().isClientSide) {
            // falling into the void
            if (this.getY() <= -64)
                this.discard();

            // move towards the closest player if we're homing
            if (this.homing) {
                Player player = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), 128, false);
                if (player != null && player.distanceToSqr(this) >= 32 * 32) {
                    Vec3 motion = new Vec3(player.getX() - this.getX(), player.getY() - this.getY(), player.getZ() - this.getZ()).normalize();
                    this.trajectoryX = (float) motion.x * 2 * this.speedModifier;
                    if (motion.y < 0)
                        this.trajectoryY = (float) motion.y * 2 * this.speedModifier;
                    this.trajectoryZ = (float) motion.z * 2 * this.speedModifier;
                }
            }

            if (this.collided()) {
                // if we removed trees, we want to continue flying after until we hit the ground
                if (this.removeTrees(this.blockPosition()))
                    return;

                NyxWorld data = NyxWorld.get(this.level());
                Explosion exp = this.level().explode(null, this.getX() + 0.5, this.getY() + 0.5, this.getZ() + 0.5, this.entityData.get(SIZE) * 4, Level.ExplosionInteraction.TNT);
                if (!this.spawnNoBlocks) {
                    for (BlockPos affected : exp.getToBlow()) {
                        BlockState here = this.level().getBlockState(affected);
                        if (!here.canBeReplaced() || !this.level().getBlockState(affected.below()).isCollisionShapeFullBlock(this.level(), affected.below()))
                            continue;
                        if (this.random.nextInt(2) != 0)
                            continue;
                        if (this.random.nextInt(5) == 0) {
                            this.level().setBlockAndUpdate(affected, Blocks.MAGMA_BLOCK.defaultBlockState());
                        } else {
                            if (data != null && data.currentEvent instanceof HarvestMoon && this.random.nextInt(10) == 0) {
                                this.level().setBlockAndUpdate(affected, Registration.GLEANING_METEOR_ROCK.get().defaultBlockState());
                            } else {
                                this.level().setBlockAndUpdate(affected, Registration.METEOR_ROCK.get().defaultBlockState());
                            }
                            if (data != null)
                                data.meteorLandingSites.add(affected);
                        }
                    }
                }
                if (data != null)
                    data.sendToClients();
                this.discard();

                // send "I spawned" message
                if (!this.disableMessage) {
                    Component text = Component.translatable("info." + Nyx.ID + ".meteor").withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true));
                    for (Player player : this.level().players()) {
                        SoundEvent sound;
                        float pitch;
                        double dist = player.distanceToSqr(this.getX(), this.getY(), this.getZ());
                        if (dist <= 256 * 256) {
                            if (dist > 16 * 16)
                                player.sendSystemMessage(text);
                            sound = SoundEvents.GENERIC_EXPLODE;
                            pitch = 0.15F;
                        } else {
                            sound = Registration.FALLING_METEOR_IMPACT_SOUND.get();
                            pitch = 1;
                        }
                        if (player instanceof ServerPlayer && player.level() == this.level())
                            player.playNotifySound(sound, SoundSource.AMBIENT, 0.5F, pitch);
                    }
                }
            } else {
                if (this.level().getGameTime() % 35 == 0)
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(), Registration.FALLING_METEOR_SOUND.get(), SoundSource.AMBIENT, 5, 1);
            }
        } else if (this.isLoaded()) {
            // we only want to display particles if we're loaded
            float size = this.entityData.get(SIZE) / 2F + 1;
            Vec3 motion = this.getDeltaMovement();
            for (int i = 0; i < 60; i++) {
                double x = this.getX() + Mth.nextDouble(this.random, -size, size);
                double y = this.getY() + Mth.nextDouble(this.random, -size, size);
                double z = this.getZ() + Mth.nextDouble(this.random, -size, size);
                double mX = -motion.x + this.random.nextGaussian() * 0.02;
                double mY = -motion.y + this.random.nextGaussian() * 0.02;
                double mZ = -motion.z + this.random.nextGaussian() * 0.02;

                ParticleOptions type;
                float f = this.random.nextFloat();
                if (f >= 0.65F) {
                    type = ParticleTypes.FLAME;
                } else if (f >= 0.45F) {
                    type = ParticleTypes.LAVA;
                } else if (f >= 0.3F) {
                    type = ParticleTypes.SMOKE;
                } else {
                    type = ParticleTypes.LARGE_SMOKE;
                }
                this.level().addAlwaysVisibleParticle(type, x, y, z, mX, mY, mZ);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("size", this.entityData.get(SIZE));
        compound.putBoolean("homing", this.homing);
        compound.putBoolean("disable_message", this.disableMessage);
        compound.putFloat("speed", this.speedModifier);
        compound.putBoolean("spawn_no_blocks", this.spawnNoBlocks);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(SIZE, compound.getInt("size"));
        this.homing = compound.getBoolean("homing");
        this.disableMessage = compound.getBoolean("disable_message");
        this.speedModifier = compound.getFloat("speed");
        this.spawnNoBlocks = compound.getBoolean("spawn_no_blocks");
    }

    private boolean removeTrees(BlockPos pos) {
        boolean any = false;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos offset = pos.offset(x, y, z);
                    if (offset.distToCenterSqr(this.getX(), this.getY(), this.getZ()) >= 8 * 8)
                        continue;
                    BlockState state = this.level().getBlockState(offset);
                    if (!state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS))
                        continue;
                    this.level().removeBlock(offset, false);
                    this.removeTrees(offset);
                    any = true;
                }
            }
        }
        return any;
    }

    public static FallingMeteor spawn(Level world, BlockPos pos) {
        pos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).above(Mth.nextInt(world.random, 64, 96));
        FallingMeteor meteor = Registration.FALLING_METEOR.get().create(world);
        meteor.setPos(pos.getX(), pos.getY(), pos.getZ());
        world.addFreshEntity(meteor);
        return meteor;
    }
}
