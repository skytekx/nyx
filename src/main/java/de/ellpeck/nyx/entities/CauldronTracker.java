package de.ellpeck.nyx.entities;

import de.ellpeck.nyx.Config;
import de.ellpeck.nyx.Registration;
import de.ellpeck.nyx.blocks.LunarWaterCauldron;
import de.ellpeck.nyx.capabilities.NyxWorld;
import de.ellpeck.nyx.lunarevents.BloodMoon;
import de.ellpeck.nyx.lunarevents.HarvestMoon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

/**
 * Invisible helper entity that sits on a vanilla water cauldron, counts the
 * ticks it is exposed to the night sky (per moon phase, configurable) and -
 * once ready and a blue dye is dropped in - turns it into a
 * {@link LunarWaterCauldron}. Ported from the 1.12 version; the 1.17+ cauldron
 * split means it now watches {@link Blocks#WATER_CAULDRON} and reads
 * {@link LayeredCauldronBlock#LEVEL} (1-3).
 */
public class CauldronTracker extends Entity {

    private static final EntityDataAccessor<Boolean> IS_DONE = SynchedEntityData.defineId(CauldronTracker.class, EntityDataSerializers.BOOLEAN);
    private BlockPos trackingPos;
    private int timer;

    public CauldronTracker(EntityType<? extends CauldronTracker> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setTrackingPos(BlockPos pos) {
        this.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        this.trackingPos = pos;
    }

    @Override
    public void tick() {
        if (this.level().isClientSide) {
            if (this.entityData.get(IS_DONE) && this.random.nextBoolean()) {
                double x = this.random.nextFloat() + this.getX() - 0.5F;
                double z = this.random.nextFloat() + this.getZ() - 0.5F;
                this.level().addParticle(ParticleTypes.ENTITY_EFFECT, x, this.getY() + 0.25F, z, 1, 1, 1);
            }
            return;
        }

        if (this.trackingPos == null) {
            this.discard();
            return;
        }

        BlockState state = this.level().getBlockState(this.trackingPos);
        boolean isWaterCauldron = state.is(Blocks.WATER_CAULDRON);
        if (!isWaterCauldron || state.getBlock() instanceof LunarWaterCauldron) {
            this.discard();
            return;
        }

        int level = state.getValue(LayeredCauldronBlock.LEVEL);
        if (level <= 0) {
            if (this.timer > 0) {
                this.entityData.set(IS_DONE, false);
                this.timer = 0;
            }
            return;
        }

        if (!this.entityData.get(IS_DONE)) {
            NyxWorld nyx = NyxWorld.get(this.level());
            if (nyx == null || !this.level().canSeeSky(this.trackingPos) || NyxWorld.isDaytime(this.level())) {
                this.timer = 0;
                return;
            }
            int phase = this.level().dimensionType().moonPhase(this.level().getDayTime());
            if (nyx.currentEvent instanceof HarvestMoon) {
                phase = 8;
            } else if (nyx.currentEvent instanceof BloodMoon) {
                phase = 9;
            }
            int ticksRequired = Config.lunarWaterTicks.get(phase);
            if (ticksRequired >= 0) {
                this.timer++;
                if (this.timer >= ticksRequired)
                    this.entityData.set(IS_DONE, true);
            }
        } else {
            List<ItemEntity> items = this.level().getEntitiesOfClass(ItemEntity.class, new AABB(this.trackingPos));
            for (ItemEntity item : items) {
                if (item.isRemoved())
                    continue;
                ItemStack stack = item.getItem();
                if (stack.getItem() != Items.BLUE_DYE)
                    continue;
                item.discard();

                BlockState newState = Registration.LUNAR_WATER_CAULDRON.get().defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, level);
                this.level().setBlockAndUpdate(this.trackingPos, newState);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), Registration.LUNAR_WATER_SOUND.get(), SoundSource.BLOCKS, 1, 1);
                this.discard();
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.contains("tracking_pos"))
            this.setTrackingPos(NbtUtils.readBlockPos(compound.getCompound("tracking_pos")));
        this.timer = compound.getInt("timer");
        this.entityData.set(IS_DONE, compound.getBoolean("done"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        if (this.trackingPos != null)
            compound.put("tracking_pos", NbtUtils.writeBlockPos(this.trackingPos));
        compound.putInt("timer", this.timer);
        compound.putBoolean("done", this.entityData.get(IS_DONE));
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(IS_DONE, false);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
